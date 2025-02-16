import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import javax.swing.*;
import javax.swing.table.*;

public class TeacherPanel extends JPanel {
    private final int teacherId;
    private final JTable table;
    private final DefaultTableModel model;
    private final JButton refreshButton;
    private final JToggleButton statusToggle;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private boolean isActive = true;

    public TeacherPanel(int teacherId) {
        this.teacherId = teacherId;
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new Object[]{"Student Name", "Time", "Status", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 3 ? Integer.class : String.class;
            }
        };

        table = new JTable(model);
        table.setRowHeight(30);

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadAppointments());

        statusToggle = new JToggleButton("Active", isActive);
        statusToggle.addActionListener(e -> toggleStatus());

        initializeUI();
        loadAppointments();
    }

    private void initializeUI() {
        configureTableRenderers();
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        buttonPanel.add(statusToggle);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void toggleStatus() {
        isActive = !isActive;
        statusToggle.setText(isActive ? "Active" : "Inactive");
        statusToggle.setBackground(isActive ? Color.GREEN : Color.RED);
        updateDatabaseStatus();
    }

    private void updateDatabaseStatus() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                String sql = "UPDATE users SET status = ? WHERE id = ?";
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

    private void configureTableRenderers() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) table.getValueAt(row, 2);
                c.setBackground(getStatusColor(status));
                return c;
            }

            private Color getStatusColor(String status) {
                return switch (status.toLowerCase()) {
                    case "approved" -> new Color(144, 238, 144);
                    case "declined" -> new Color(255, 182, 193);
                    case "pending" -> Color.YELLOW;
                    default -> Color.WHITE;
                };
            }
        });
    }

    private void loadAppointments() {
        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                model.setRowCount(0);
                String sql = "SELECT s.name AS student_name, a.time, a.status "
                        + "FROM appointments a "
                        + "JOIN students s ON a.student_id = s.id "
                        + "WHERE a.teacher_id = ?";
                try (Connection con = Database.getConnection();
                     PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setInt(1, teacherId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        publish(new Object[]{
                            rs.getString("student_name"),
                            formatTimestamp(rs.getTimestamp("time")),
                            rs.getString("status"),
                            ""
                        });
                    }
                } catch (SQLException ex) {
                    showError("Database Error", "Failed to load appointments: " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    model.addRow(row);
                }
            }
        };
        worker.execute();
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp != null ? TIME_FORMAT.format(timestamp) : "N/A";
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}