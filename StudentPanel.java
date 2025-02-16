import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class StudentPanel extends JPanel {
    private int studentId;
    private JComboBox<String> teacherComboBox;
    private JTextField txtTime;
    private JButton btnRequestAppointment;

    public StudentPanel(int studentId) {
        this.studentId = studentId;
        setLayout(new BorderLayout());

        // Form Panel for Requesting Appointment
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Request Appointment"));

        formPanel.add(new JLabel("Select Teacher:"));
        teacherComboBox = new JComboBox<>();
        formPanel.add(teacherComboBox);

        formPanel.add(new JLabel("Time:"));
        txtTime = new JTextField();
        formPanel.add(txtTime);

        btnRequestAppointment = new JButton("Request Appointment");
        formPanel.add(btnRequestAppointment);

        add(formPanel, BorderLayout.CENTER);

        btnRequestAppointment.addActionListener(e -> requestAppointment());

        loadAvailableTeachers();
    }

    private void loadAvailableTeachers() {
        teacherComboBox.removeAllItems();
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT id, name FROM teachers WHERE status = 'active'")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int teacherId = rs.getInt("id");
                String teacherName = rs.getString("name");
                teacherComboBox.addItem(teacherName + " (ID: " + teacherId + ")");
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

        int teacherId = Integer.parseInt(selectedTeacher.replaceAll("\\D+", "")); // Extract teacher ID

        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "INSERT INTO appointments (student_id, teacher_id, time, status) VALUES (?, ?, ?, 'pending')")) {
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
