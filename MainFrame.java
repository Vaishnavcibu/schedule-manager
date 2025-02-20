import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public MainFrame() {
        setTitle("School Schedule Manager");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        add(mainPanel);
    }

    public void initializePanels(String userRole, int userId) {
        AdminPanel adminPanel = new AdminPanel();
        StudentPanel studentPanel = new StudentPanel(userId);
        TeacherPanel teacherPanel = new TeacherPanel(userId);

        mainPanel.add(adminPanel, "Admin");
        mainPanel.add(studentPanel, "Student");
        mainPanel.add(teacherPanel, "Teacher");

        showPanel(userRole);
    }

    public void showPanel(String panelName) {
        cardLayout.show(mainPanel, panelName);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            Login login = new Login(mainFrame);
            login.setVisible(true);
        });
    }
}
