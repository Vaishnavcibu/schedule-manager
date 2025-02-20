import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class StudentPanel extends JPanel {
    private int studentId;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JComboBox<String> teacherComboBox;
    private JTextField txtTime;
    private JButton btnRequestAppointment;
    private Map<String, Integer> teacherMap;
    private DefaultTableModel teacherStatusModel;
    private JTable teacherStatusTable;

    public StudentPanel(int studentId) {
        this.studentId = studentId;
        this.teacherMap = new HashMap<>();
        setLayout(new BorderLayout());

        // Sidebar Navigation
        JPanel sidebar = new JPanel(new GridLayout(2, 1, 5, 5));
        sidebar.setBackground(Color.decode("#03045E"));
        
        JButton btnRequestAppointmentPanel = createStyledButton("Request Appointment");
        JButton btnTeacherStatus = createStyledButton("Teacher Status");
        
        sidebar.add(btnRequestAppointmentPanel);
        sidebar.add(btnTeacherStatus);
        add(sidebar, BorderLayout.WEST);

        // Main Content Area
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        JPanel requestAppointmentPanel = createRequestAppointmentPanel();
        JPanel teacherStatusPanel = createTeacherStatusPanel();
        
        contentPanel.add(requestAppointmentPanel, "RequestAppointment");
        contentPanel.add(teacherStatusPanel, "TeacherStatus");
        
        add(contentPanel, BorderLayout.CENTER);

        // Button Listeners
        btnRequestAppointmentPanel.addActionListener(e -> cardLayout.show(contentPanel, "RequestAppointment"));
        btnTeacherStatus.addActionListener(e -> {
            loadTeacherStatus();
            cardLayout.show(contentPanel, "TeacherStatus");
        });

        // Load available teachers
        loadAvailableTeachers();
    }

    private JPanel createRequestAppointmentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        
        formPanel.add(new JLabel("Select Teacher:"));
        teacherComboBox = new JComboBox<>();
        formPanel.add(teacherComboBox);
        
        formPanel.add(new JLabel("Time:"));
        txtTime = new JTextField();
        formPanel.add(txtTime);
        
        btnRequestAppointment = new JButton("Request Appointment");
        formPanel.add(new JLabel());
        formPanel.add(btnRequestAppointment);
        
        btnRequestAppointment.addActionListener(e -> requestAppointment());
        panel.add(formPanel, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createTeacherStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        teacherStatusModel = new DefaultTableModel(new String[]{"Teacher Name", "Time Allotted", "Status"}, 0);
        teacherStatusTable = new JTable(teacherStatusModel);
        panel.add(new JScrollPane(teacherStatusTable), BorderLayout.CENTER);
        return panel;
    }

    private void loadTeacherStatus() {
        teacherStatusModel.setRowCount(0);
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                 "SELECT t.name, a.time, a.status FROM appointments a " +
                 "JOIN teachers t ON a.teacher_id = t.id WHERE a.student_id = ?")) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                teacherStatusModel.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getString("time"),
                    rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading teacher status.");
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.decode("#0077B6"));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        return button;
    }

    private void loadAvailableTeachers() {
        teacherComboBox.removeAllItems();
        teacherMap.clear();
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT id, name FROM teachers WHERE status = 'Active'")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int teacherId = rs.getInt("id");
                String teacherName = rs.getString("name");
                teacherComboBox.addItem(teacherName);
                teacherMap.put(teacherName, teacherId);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading available teachers.");
        }
    }

    private void requestAppointment() {
        String selectedTeacher = (String) teacherComboBox.getSelectedItem();
        String time = txtTime.getText();
        if (selectedTeacher == null || time.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a teacher and enter a time.");
            return;
        }
        int teacherId = teacherMap.get(selectedTeacher);
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement("INSERT INTO appointments (student_id, teacher_id, time, status) VALUES (?, ?, ?, 'pending')")) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, teacherId);
            stmt.setString(3, time);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Appointment request sent successfully!");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error sending appointment request.");
        }
    }
}
