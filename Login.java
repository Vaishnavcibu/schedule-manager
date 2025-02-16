import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class Login extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> roleComboBox;
    private JButton btnLogin;
    private JCheckBox showPasswordCheckbox;
    private MainFrame mainFrame;

    public Login(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setTitle("Login - Schedule Manager");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(5, 2, 10, 10));

        add(new JLabel("Username:"));
        txtUsername = new JTextField();
        add(txtUsername);

        add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        add(txtPassword);

        showPasswordCheckbox = new JCheckBox("Show Password");
        showPasswordCheckbox.addActionListener(e -> {
            if (showPasswordCheckbox.isSelected()) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('*');
            }
        });
        add(showPasswordCheckbox);
        add(new JLabel());

        add(new JLabel("Role:"));
        roleComboBox = new JComboBox<>(new String[]{"Admin", "Teacher", "Student"});
        add(roleComboBox);

        btnLogin = new JButton("Login");
        add(btnLogin);

        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                authenticateUser();
            }
        });
    }

    private void authenticateUser() {
    String username = txtUsername.getText();
    String password = new String(txtPassword.getPassword());
    String role = roleComboBox.getSelectedItem().toString();

    if (username.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Please enter both username and password.");
        return;
    }

    try (Connection con = Database.getConnection();
         PreparedStatement stmt = con.prepareStatement("SELECT id FROM users WHERE name = ? AND password = ? AND role = ?")) {
        stmt.setString(1, username);
        stmt.setString(2, password);
        stmt.setString(3, role);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            int userId = rs.getInt("id");
            JOptionPane.showMessageDialog(this, "Login Successful!");

            mainFrame.initializePanels(role, userId);
            mainFrame.setVisible(true);
            dispose(); // Close only the login window


            // Step 3: Redirect to Admin Panel
            if (role.equalsIgnoreCase("Admin")) {
                mainFrame.showPanel("Admin");
            } else if (role.equalsIgnoreCase("Student")) {
                mainFrame.showPanel("Student");
            } else if (role.equalsIgnoreCase("Teacher")) {
                mainFrame.showPanel("Teacher");
            }

        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials. Please try again.");
        }
    } catch (SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
    }
}


public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        MainFrame mainFrame = new MainFrame();
        Login login = new Login(mainFrame);
        login.setVisible(true);
    });
}
}