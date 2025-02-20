import java.awt.*;
import java.sql.*;
import javax.swing.*;

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
        setLayout(new GridBagLayout());
        getContentPane().setBackground(Color.decode("#CAF0F8")); // Light Blue Background

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Username Label
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(createLabel("Username:"), gbc);

        // Username Field
        gbc.gridx = 1;
        txtUsername = createTextField();
        add(txtUsername, gbc);

        // Password Label
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(createLabel("Password:"), gbc);

        // Password Field
        gbc.gridx = 1;
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Arial", Font.PLAIN, 14));
        add(txtPassword, gbc);

        // Show Password Checkbox
        gbc.gridx = 1;
        gbc.gridy = 2;
        showPasswordCheckbox = new JCheckBox("Show Password");
        showPasswordCheckbox.setBackground(Color.decode("#CAF0F8"));
        showPasswordCheckbox.setFont(new Font("Arial", Font.PLAIN, 12));
        showPasswordCheckbox.addActionListener(e -> togglePasswordVisibility());
        add(showPasswordCheckbox, gbc);

        // Role Label
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(createLabel("Role:"), gbc);

        // Role Dropdown
        gbc.gridx = 1;
        roleComboBox = new JComboBox<>(new String[]{"Admin", "Teacher", "Student"});
        roleComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        add(roleComboBox, gbc);

        // Login Button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        btnLogin = createStyledButton("Login");
        add(btnLogin, gbc);

        // Login Action
        btnLogin.addActionListener(e -> authenticateUser());
    }

    // Styled Label
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        return label;
    }

    // Styled TextField
    private JTextField createTextField() {
        JTextField textField = new JTextField(15);
        textField.setFont(new Font("Arial", Font.PLAIN, 14));
        return textField;
    }

    // Toggle Password Visibility
    private void togglePasswordVisibility() {
        if (showPasswordCheckbox.isSelected()) {
            txtPassword.setEchoChar((char) 0);
        } else {
            txtPassword.setEchoChar('*');
        }
    }

    // Custom Styled Button
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.decode("#0077B6")); // Dark Blue
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.decode("#00B4D8")); // Lighter Blue on Hover
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.decode("#0077B6"));
            }
        });

        return button;
    }

    // Authenticate User
    private void authenticateUser() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        String role = roleComboBox.getSelectedItem().toString();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Error", JOptionPane.ERROR_MESSAGE);
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
                dispose();

                // Redirect Based on Role
                mainFrame.showPanel(role);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
