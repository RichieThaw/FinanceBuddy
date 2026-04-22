import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHelper {
    
    // 🔥 This finds the "User" folder (e.g., C:\Users\Richie)
    private static final String USER_HOME = System.getProperty("user.home");
    // 🔥 This creates the database path inside that folder
    private static final String DB_PATH = USER_HOME + File.separator + "financebuddy.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver not found: " + e.getMessage());
        }
        
        // This will now create the file in a safe place like C:\Users\Name\financebuddy.db
        return DriverManager.getConnection(DB_URL);
    }
    
    // ... rest of your code (initDatabase, etc.)

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
