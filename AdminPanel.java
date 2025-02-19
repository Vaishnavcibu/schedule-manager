import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;

public class AdminPanel extends JPanel {
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private UserListPanel userListPanel;

    public AdminPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.decode("#CAF0F8"));

        JPanel sidebar = new JPanel(new GridLayout(5, 1, 5, 5));
        sidebar.setBackground(Color.decode("#03045E"));

        JButton btnDashboard = createStyledButton("Dashboard");
        JButton btnAddUser = createStyledButton("Add User");
        JButton btnUserList = createStyledButton("User List");

        sidebar.add(btnDashboard);
        sidebar.add(btnAddUser);
        sidebar.add(btnUserList);
        add(sidebar, BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        userListPanel = new UserListPanel();
        AddUserPanel addUserPanel = new AddUserPanel(userListPanel);
        DashboardPanel dashboardPanel = new DashboardPanel();

        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(addUserPanel, "AddUser");
        contentPanel.add(userListPanel, "UserList");

        add(contentPanel, BorderLayout.CENTER);

        btnDashboard.addActionListener(e -> cardLayout.show(contentPanel, "Dashboard"));
        btnAddUser.addActionListener(e -> cardLayout.show(contentPanel, "AddUser"));
        btnUserList.addActionListener(e -> {
            userListPanel.loadUsers();
            cardLayout.show(contentPanel, "UserList");
        });
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.decode("#0077B6"));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Admin Panel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.add(new AdminPanel());
            frame.setVisible(true);
        });
    }
}

class AddUserPanel extends JPanel {
    private JTextField txtId, txtName;
    private JPasswordField txtPassword;
    private JComboBox<String> comboRole;
    private JButton btnRegister;
    private JCheckBox showPassword;
    private UserListPanel userListPanel;

    public AddUserPanel(UserListPanel userListPanel) {
        this.userListPanel = userListPanel;
        setLayout(new GridBagLayout());
        setBackground(Color.decode("#90E0EF"));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.decode("#CAF0F8"));
        formPanel.setOpaque(true);
        formPanel.setPreferredSize(new Dimension(500, 400));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        Dimension fieldSize = new Dimension(300, 40);

        formPanel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        txtId = new JTextField(30);
        txtId.setPreferredSize(fieldSize);
        formPanel.add(txtId, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        txtName = new JTextField(30);
        txtName.setPreferredSize(fieldSize);
        formPanel.add(txtName, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        comboRole = new JComboBox<>(new String[]{"Student", "Teacher"});
        comboRole.setPreferredSize(fieldSize);
        formPanel.add(comboRole, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        txtPassword = new JPasswordField(30);
        txtPassword.setPreferredSize(fieldSize);
        formPanel.add(txtPassword, gbc);

        showPassword = new JCheckBox("Show Password");
        showPassword.addActionListener(e -> txtPassword.setEchoChar(showPassword.isSelected() ? (char) 0 : '*'));
        gbc.gridy++;
        formPanel.add(showPassword, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        btnRegister = new JButton("Register");
        formPanel.add(btnRegister, gbc);
        
        add(formPanel);
        btnRegister.addActionListener(e -> addUserToDatabase());
    }
    
    private void addUserToDatabase() {
        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String role = comboRole.getSelectedItem().toString();
        String password = new String(txtPassword.getPassword()).trim();

        if (id.isEmpty() || name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields must be filled.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement("INSERT INTO users (id, name, role, password, status) VALUES (?, ?, ?, ?, 'Active')")) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setString(3, role);
            stmt.setString(4, password);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error registering user.", "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}