import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

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

class UserListPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtName;
    private JComboBox<String> cmbRole;
    private JButton btnEdit, btnDelete, btnSave;

    public UserListPanel() {
        setLayout(new BorderLayout());
        model = new DefaultTableModel(new String[]{"ID", "Name", "Role", "Status"}, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel editPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        editPanel.add(new JLabel("Edit User"), gbc);

        gbc.gridy = 1;
        editPanel.add(new JLabel("Name:"), gbc);
        txtName = new JTextField(15);
        gbc.gridx = 1;
        editPanel.add(txtName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        editPanel.add(new JLabel("Role:"), gbc);
        cmbRole = new JComboBox<>(new String[]{"Student", "Teacher"});
        gbc.gridx = 1;
        editPanel.add(cmbRole, gbc);

        add(editPanel, BorderLayout.SOUTH);

        JPanel btnPanel = new JPanel();
        btnEdit = new JButton("Edit Selected");
        btnSave = new JButton("Save Changes");
        btnDelete = new JButton("Delete");
        btnPanel.add(btnEdit);
        btnPanel.add(btnSave);
        btnPanel.add(btnDelete);
        add(btnPanel, BorderLayout.NORTH);

        btnEdit.addActionListener(e -> editUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnSave.addActionListener(e -> saveChanges());
    }

    public void loadUsers() {
        model.setRowCount(0);
        try (Connection con = Database.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, role, status FROM users")) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getString("id"), rs.getString("name"), rs.getString("role"), rs.getString("status")});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void editUser() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to edit.");
            return;
        }
        txtName.setText((String) model.getValueAt(selectedRow, 1));
        cmbRole.setSelectedItem(model.getValueAt(selectedRow, 2));
    }

    private void saveChanges() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to save changes.");
            return;
        }
        String id = (String) model.getValueAt(selectedRow, 0);
        String newName = txtName.getText();
        String newRole = (String) cmbRole.getSelectedItem();
        if (!newName.trim().isEmpty()) {
            try (Connection con = Database.getConnection();
                 PreparedStatement stmt = con.prepareStatement("UPDATE users SET name=?, role=? WHERE id=?")) {
                stmt.setString(1, newName);
                stmt.setString(2, newRole);
                stmt.setString(3, id);
                stmt.executeUpdate();
                loadUsers();
                JOptionPane.showMessageDialog(this, "User updated successfully!");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating user.");
            }
        }
    }

    private void deleteUser() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.");
            return;
        }
        String id = (String) model.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection con = Database.getConnection();
                 PreparedStatement stmt = con.prepareStatement("DELETE FROM users WHERE id=?")) {
                stmt.setString(1, id);
                stmt.executeUpdate();
                loadUsers();
                JOptionPane.showMessageDialog(this, "User deleted successfully!");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error deleting user.");
            }
        }
    }
}
