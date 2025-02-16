import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AdminPanel extends JPanel {
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private UserListPanel userListPanel;

    public AdminPanel() {
        setLayout(new BorderLayout());

        // Sidebar (Navigation)
        JPanel sidebar = new JPanel(new GridLayout(5, 1, 5, 5));
        JButton btnDashboard = new JButton("Dashboard");
        JButton btnAddUser = new JButton("Add User");
        JButton btnUserList = new JButton("User List");

        sidebar.add(btnDashboard);
        sidebar.add(btnAddUser);
        sidebar.add(btnUserList);
        add(sidebar, BorderLayout.WEST);

        // Content Panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        userListPanel = new UserListPanel();
        AddUserPanel addUserPanel = new AddUserPanel(userListPanel);
        DashboardPanel dashboardPanel = new DashboardPanel();

        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(addUserPanel, "AddUser");
        contentPanel.add(userListPanel, "UserList");

        add(contentPanel, BorderLayout.CENTER);

        // Button Actions
        btnDashboard.addActionListener(e -> cardLayout.show(contentPanel, "Dashboard"));
        btnAddUser.addActionListener(e -> cardLayout.show(contentPanel, "AddUser"));
        btnUserList.addActionListener(e -> {
            userListPanel.loadUsers(); // Refresh users from MySQL
            cardLayout.show(contentPanel, "UserList");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminPanel().setVisible(true));
    }
}

// ** Database Connection Utility **
class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/sheduledb";
    private static final String USER = "root";
    private static final String PASSWORD = "rootpassword";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

// ** Dashboard Panel **
class DashboardPanel extends JPanel {
    public DashboardPanel() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Admin Dashboard", JLabel.CENTER);
        add(label, BorderLayout.CENTER);
    }
}

// ** Add User Panel (Insert into MySQL) **
class AddUserPanel extends JPanel {
    private JTextField txtName, txtRole;
    private JButton btnRegister;
    private UserListPanel userListPanel;

    public AddUserPanel(UserListPanel userListPanel) {
        this.userListPanel = userListPanel;
        setLayout(new GridLayout(3, 2, 10, 10));

        add(new JLabel("Name:"));
        txtName = new JTextField();
        add(txtName);

        add(new JLabel("Role (Student/Teacher):"));
        txtRole = new JTextField();
        add(txtRole);

        btnRegister = new JButton("Register");
        add(btnRegister);

        btnRegister.addActionListener(e -> addUserToDatabase());
    }

    private void addUserToDatabase() {
        String name = txtName.getText();
        String role = txtRole.getText();

        if (name.isEmpty() || role.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }

        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement("INSERT INTO users (name, role, status) VALUES (?, ?, 'Inactive')")) {
            stmt.setString(1, name);
            stmt.setString(2, role);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User added successfully!");
            userListPanel.loadUsers(); // Refresh user list
            txtName.setText(""); // Clear input fields
            txtRole.setText("");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding user.");
        }
    }
}

// ** User List Panel (CRUD Operations) **
class UserListPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtName, txtRole;
    private JButton btnEdit, btnSave, btnDelete;
    private int selectedUserId = -1;

    public UserListPanel() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new Object[]{"ID", "Name", "Role", "Status"}, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Panel for Editing
        JPanel editPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        editPanel.setBorder(BorderFactory.createTitledBorder("Edit User"));

        editPanel.add(new JLabel("Name:"));
        txtName = new JTextField();
        editPanel.add(txtName);

        editPanel.add(new JLabel("Role (Student/Teacher):"));
        txtRole = new JTextField();
        editPanel.add(txtRole);

        btnEdit = new JButton("Edit Selected");
        btnSave = new JButton("Save Changes");
        btnDelete = new JButton("Delete");

        editPanel.add(btnEdit);
        editPanel.add(btnSave);
        editPanel.add(btnDelete);

        add(editPanel, BorderLayout.SOUTH);

        // Button Actions
        btnEdit.addActionListener(e -> loadSelectedUser());
        btnSave.addActionListener(e -> updateUser());
        btnDelete.addActionListener(e -> deleteUser());

        loadUsers(); // Load users on startup
    }

    public void loadUsers() {
        model.setRowCount(0); // Clear existing table data
        try (Connection con = Database.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getString("role"), rs.getString("status")});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading users.");
        }
    }

    private void loadSelectedUser() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            selectedUserId = (int) model.getValueAt(selectedRow, 0);
            txtName.setText(model.getValueAt(selectedRow, 1).toString());
            txtRole.setText(model.getValueAt(selectedRow, 2).toString());
        } else {
            JOptionPane.showMessageDialog(this, "Please select a user to edit.");
        }
    }

    private void updateUser() {
        if (selectedUserId == -1) return;
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement("UPDATE users SET name = ?, role = ? WHERE id = ?")) {
            stmt.setString(1, txtName.getText());
            stmt.setString(2, txtRole.getText());
            stmt.setInt(3, selectedUserId);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User updated successfully!");
            loadUsers();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void deleteUser() {
        if (selectedUserId == -1) return;
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement("DELETE FROM users WHERE id = ?")) {
            stmt.setInt(1, selectedUserId);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User deleted successfully!");
            loadUsers();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}