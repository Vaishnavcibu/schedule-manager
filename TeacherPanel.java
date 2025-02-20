import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class TeacherPanel extends JPanel {
    private final int teacherId;
    private final JTable table;
    private final DefaultTableModel model;
    private final JButton refreshButton;
    private final JToggleButton statusToggle;
    private boolean isActive = true;

    public TeacherPanel(int teacherId) {
        this.teacherId = teacherId;
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.decode("#F5F5F5"));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        model = new DefaultTableModel(new Object[]{"Student Name", "Time", "Status", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };

        table = new JTable(model);
        table.setRowHeight(30);
        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditor());

        refreshButton = createButton("Refresh");
        refreshButton.addActionListener(e -> loadAppointments());

        statusToggle = new JToggleButton("Active", isActive);
        updateStatusToggleStyle();
        statusToggle.addActionListener(e -> toggleStatus());

        initializeUI();
        loadAppointments();
    }

    private void initializeUI() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(refreshButton);
        buttonPanel.add(statusToggle);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void toggleStatus() {
        isActive = !isActive;
        updateStatusToggleStyle();
        updateDatabaseStatus();
    }

    private void updateStatusToggleStyle() {
        statusToggle.setText(isActive ? "Active" : "Inactive");
        statusToggle.setBackground(isActive ? new Color(76, 175, 80) : new Color(244, 67, 54));
        statusToggle.setForeground(Color.WHITE);
    }

    private void updateDatabaseStatus() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                String sql = "UPDATE appointments SET status = ? WHERE teacher_id = ?";
                try (Connection con = Database.getConnection();
                     PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, isActive ? "active" : "inactive");
                    stmt.setInt(2, teacherId);
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    showError("Database Error", "Failed to update status: " + ex.getMessage());
                }
                return null;
            }
        };
        worker.execute();
    }

    private void loadAppointments() {
        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                model.setRowCount(0);
                String sql = "SELECT a.id, s.name AS student_name, a.time, a.status FROM appointments a JOIN students s ON a.student_id = s.id WHERE a.teacher_id = ?";

                try (Connection con = Database.getConnection();
                     PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setInt(1, teacherId);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        int appointmentId = rs.getInt("id");
                        publish(new Object[]{
                            rs.getString("student_name"),
                            rs.getString("time"),
                            rs.getString("status"),
                            new ButtonPanel(appointmentId) // Pass appointment ID to buttons
                        });
                    }
                } catch (SQLException ex) {
                    showError("Database Error", "Failed to load appointments: " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                SwingUtilities.invokeLater(() -> {
                    for (Object[] row : chunks) {
                        model.addRow(row);
                    }
                    model.fireTableDataChanged();
                });
            }
        };
        worker.execute();
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.decode("#004080"));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(120, 30));
        return button;
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private class ButtonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (Component) value;
        }
    }

    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final ButtonPanel panel;

        public ButtonEditor() {
            panel = new ButtonPanel(-1);
        }

        @Override
        public Object getCellEditorValue() {
            return panel;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return (Component) value;
        }
    }

    private class ButtonPanel extends JPanel {
        private final JButton approveButton;
        private final JButton declineButton;
        private final int appointmentId;

        public ButtonPanel(int appointmentId) {
            this.appointmentId = appointmentId;
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

            approveButton = new JButton("Approve");
            declineButton = new JButton("Decline");

            approveButton.setBackground(new Color(76, 175, 80));
            approveButton.setForeground(Color.WHITE);
            declineButton.setBackground(new Color(244, 67, 54));
            declineButton.setForeground(Color.WHITE);

            approveButton.addActionListener(e -> updateAppointmentStatus("approved"));
            declineButton.addActionListener(e -> updateAppointmentStatus("declined"));

            add(approveButton);
            add(declineButton);
        }

        private void updateAppointmentStatus(String newStatus) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    String sql = "UPDATE appointments SET status = ? WHERE id = ?";
                    try (Connection con = Database.getConnection();
                         PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setString(1, newStatus);
                        stmt.setInt(2, appointmentId);
                        stmt.executeUpdate();
                    } catch (SQLException ex) {
                        showError("Database Error", "Failed to update status: " + ex.getMessage());
                    }
                    return null;
                }

                @Override
                protected void done() {
                    loadAppointments();
                }
            };
            worker.execute();
        }
    }
}
