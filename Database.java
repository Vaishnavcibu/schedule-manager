import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/sheduledb";
    private static final String USER = "root"; // Change if you have a different username
    private static final String PASSWORD = "rootpassword"; // Add your MySQL password if required

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
