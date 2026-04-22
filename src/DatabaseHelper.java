import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {
    
    // 🔥 NEW: Path to the local SQLite database file
    private static final String DB_URL = "jdbc:sqlite:financebuddy.db";

    public static Connection connect() throws SQLException {
        try {
            // Updated for SQLite
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite Driver not found: " + e.getMessage());
        }
        return DriverManager.getConnection(DB_URL);
    }

    public static void initDatabase() {
        // SQL Syntax remains the same for CREATE TABLE
        // No change needed to your table creation strings
        
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "username TEXT PRIMARY KEY, "
                + "email TEXT NOT NULL UNIQUE, "
                + "password TEXT NOT NULL)";
        
        // ... (Keep your existing table strings but change VARCHAR/DOUBLE to TEXT/REAL if desired, 
        // though SQLite is flexible and usually accepts MySQL syntax for these)

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            // ... (Execute all other table creations)
            System.out.println("SQLite Database Initialized Successfully.");
        } catch (SQLException e) {
            System.out.println("Database Initialization Error: " + e.getMessage());
        }
    }
}