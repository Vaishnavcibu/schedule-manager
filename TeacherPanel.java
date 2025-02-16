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
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

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

        initializeUI();
        loadAppointments();
    }

    private void initializeUI() {
        configureTableRenderers();
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);
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

        TableColumn actionColumn = table.getColumnModel().getColumn(3);

        actionColumn.setCellRenderer(new TableCellRenderer() {
            private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            private final JButton acceptBtn = createActionButton("Accept", Color.GREEN);
            private final JButton rejectBtn = createActionButton("Reject", Color.RED);

            {
                panel.add(acceptBtn);
                panel.add(rejectBtn);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                return panel;
            }
        });

        actionColumn.setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            private final JButton acceptBtn = createActionButton("Accept", Color.GREEN);
            private final JButton rejectBtn = createActionButton("Reject", Color.RED);
            private int currentAppointmentId;

            {
                acceptBtn.addActionListener(e -> {
                    updateAppointmentStatus(currentAppointmentId, "approved");
                    fireEditingStopped();
                });
                rejectBtn.addActionListener(e -> {
                    updateAppointmentStatus(currentAppointmentId, "declined");
                    fireEditingStopped();
                });
                panel.add(acceptBtn);
                panel.add(rejectBtn);
            }

            @Override
            public Object getCellEditorValue() {
                return currentAppointmentId;
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                currentAppointmentId = (Integer) value;
                return panel;
            }
        });
    }

    private JButton createActionButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEtchedBorder());
        return btn;
    }

    private void loadAppointments() {
        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                model.setRowCount(0);
                String sql = "SELECT a.id, s.name AS student_name, a.time, a.status "
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
                            rs.getInt("id")
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

    private void updateAppointmentStatus(int appointmentId, String newStatus) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                String sql = "UPDATE appointments SET status = ? WHERE id = ?";
                try (Connection con = Database.getConnection();
                     PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, newStatus);
                    stmt.setInt(2, appointmentId);
                    stmt.executeUpdate();
                    loadAppointments();
                } catch (SQLException ex) {
                    showError("Update Error", "Failed to update status: " + ex.getMessage());
                }
                return null;
            }
        };
        worker.execute();
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
