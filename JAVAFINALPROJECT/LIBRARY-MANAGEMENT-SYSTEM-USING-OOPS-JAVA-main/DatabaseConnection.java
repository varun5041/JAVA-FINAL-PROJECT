import java.sql.*;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConnection {
    private static Connection connection = null;
    private static final String PROPERTIES_FILE = "database.properties";
    
    private static String dbUrl;
    private static String dbUsername;
    private static String dbPassword;
    
    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        try {
            Properties props = new Properties();
            InputStream inputStream = DatabaseConnection.class.getClassLoader()
                    .getResourceAsStream(PROPERTIES_FILE);
            
            if (inputStream == null) {
                // Fallback to default values if properties file not found
                dbUrl = "jdbc:mysql://127.0.0.1:3306/library_db";
                dbUsername = "root";
                dbPassword = "Varun5041@";
                System.out.println("Using default database configuration. Create database.properties for custom settings.");
                return;
            }
            
            props.load(inputStream);
            dbUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true");
            dbUsername = props.getProperty("db.username", "root");
            dbPassword = props.getProperty("db.password", "");
            inputStream.close();
        } catch (Exception e) {
            System.err.println("Error loading database properties: " + e.getMessage());
            // Use defaults (matching the fallback values)
            dbUrl = "jdbc:mysql://127.0.0.1:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true";
            dbUsername = "root";
            dbPassword = "Varun5041@";
        }
    }
    
    public static Connection getConnection() throws SQLException {
        // Always create a new connection to avoid stale data
        // (For production, consider connection pooling)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            // Ensure autocommit is on for immediate visibility
            conn.setAutoCommit(true);
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Please add mysql-connector-j to your classpath.", e);
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            throw e;
        }
    }
    
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    // Note: Table creation/initialization has been removed from code.
    // Use an external SQL schema file to set up the database instead.
}
