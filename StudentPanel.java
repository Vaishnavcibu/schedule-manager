import java.awt.*;
import java.awt.event.*;
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

    public StudentPanel(int studentId) {
        this.studentId = studentId;
        this.teacherMap = new HashMap<>();
        setLayout(new BorderLayout());

        // Sidebar
        JPanel sidebar = new JPanel(new GridLayout(2, 1, 5, 5));
        sidebar.setBackground(Color.decode("#03045E"));
        JButton btnRequestPanel = createStyledButton("Request Appointment");
        JButton btnTeacherStatus = createStyledButton("Teacher Status");
        sidebar.add(btnRequestPanel);
        sidebar.add(btnTeacherStatus);
        add(sidebar, BorderLayout.WEST);

        // Content Panel
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(createRequestPanel(), "RequestPanel");
        contentPanel.add(createStatusPanel(), "StatusPanel");
        add(contentPanel, BorderLayout.CENTER);

        btnRequestPanel.addActionListener(e -> cardLayout.show(contentPanel, "RequestPanel"));
        btnTeacherStatus.addActionListener(e -> {
            loadTeacherStatus();
            cardLayout.show(contentPanel, "StatusPanel");
        });
    }

    private JPanel createRequestPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Request Appointment"));

        formPanel.add(new JLabel("Select Teacher:"));
        teacherComboBox = new JComboBox<>();
        formPanel.add(teacherComboBox);

        formPanel.add(new JLabel("Time:"));
        txtTime = new JTextField();
        formPanel.add(txtTime);

        formPanel.add(new JLabel());
        btnRequestAppointment = new JButton("Request Appointment");
        formPanel.add(btnRequestAppointment);

        panel.add(formPanel, BorderLayout.CENTER);
        btnRequestAppointment.addActionListener(e -> requestAppointment());
        loadAvailableTeachers();
        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Teacher Status"));

        String[] columnNames = {"Teacher Name", "Time Allotted", "Status"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
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
            JOptionPane.showMessageDialog(this, "Error loading teachers.");
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
            JOptionPane.showMessageDialog(this, "Appointment request sent!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error sending appointment request.");
        }
    }

    private void loadTeacherStatus() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Teacher Name", "Time Allotted", "Status"}, 0);
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT t.name, a.time, a.status FROM appointments a JOIN teachers t ON a.teacher_id = t.id WHERE a.student_id = ?")) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getString("name"), rs.getString("time"), rs.getString("status")});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading teacher status.");
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.decode("#0077B6"));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Color.decode("#00B4D8"));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.decode("#0077B6"));
            }
        });
        return button;
    }
}
