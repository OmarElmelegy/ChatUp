package Server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * DatabaseManager handles all database operations for the chat system.
 * It provides persistent storage for chat messages and user accounts using
 * SQLite database.
 * 
 * <p>
 * This class manages:
 * <ul>
 * <li>Database connection and initialization</li>
 * <li>Creating the messages and users table structures</li>
 * <li>User registration with SHA-256 password hashing</li>
 * <li>User authentication with password verification</li>
 * <li>Checking username existence</li>
 * <li>Inserting new messages into the database</li>
 * <li>Retrieving message history for users</li>
 * </ul>
 * 
 * <p>
 * Database Schema:
 * 
 * <pre>
 * CREATE TABLE messages (
 *   id INTEGER PRIMARY KEY AUTOINCREMENT,
 *   sender TEXT NOT NULL,
 *   sender_ip TEXT NOT NULL,
 *   recipient TEXT NOT NULL,
 *   recipient_ip TEXT NOT NULL,
 *   content TEXT NOT NULL,
 *   timestamp TEXT NOT NULL
 * );
 * 
 * CREATE TABLE users (
 *   id INTEGER PRIMARY KEY AUTOINCREMENT,
 *   username TEXT NOT NULL UNIQUE,
 *   password_hash TEXT NOT NULL,
 *   created_at TEXT NOT NULL
 * );
 * </pre>
 * 
 * @author ChatSystem Team
 * @version 2.2
 * @see ChatServer
 * @see ClientHandler
 */
public class DatabaseManager {
    /**
     * The JDBC connection URL for SQLite database (creates chat.db file in project
     * root)
     */
    static final String url = "jdbc:sqlite:chat.db";

    /**
     * Creates the messages table if it doesn't already exist.
     * This method is called when the server starts to ensure the database
     * schema is properly initialized.
     * 
     * <p>
     * Table structure:
     * <ul>
     * <li>id: Auto-incrementing primary key</li>
     * <li>sender: Username of the message sender (NOT NULL)</li>
     * <li>content: Message content (NOT NULL)</li>
     * <li>timestamp: Message timestamp in yyyy-MM-dd HH:mm:ss format (NOT
     * NULL)</li>
     * </ul>
     */
    static void createNewTable() {
        // SQK statment for creating a new table

        String sql = "CREATE TABLE IF NOT EXISTS messages (\n"
                + " id integer PRIMARY KEY AUTOINCREMENT, \n"
                + " sender text NOT NULL, \n"
                + " sender_ip text NOT NULL, \n"
                + " recipient text NOT NULL, \n"
                + " recipient_ip text NOT NULL, \n"
                + " content text NOT NULL, \n"
                + " timestamp text NOT NULL\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement()) {
            // Excute the SQL
            stmt.execute(sql);
            System.out.println("Database: 'messages' table checked/created.");
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
    }

    /**
     * Inserts a new message into the database.
     * This method is called for all public messages (not private messages or
     * commands).
     * 
     * @param sender    the username of the message sender
     * @param content   the text content of the message
     * @param timestamp the timestamp of the message in yyyy-MM-dd HH:mm:ss format
     */
    public static void insertMessage(String sender, String sender_ip, String recipient, String recipient_ip,
            String content, String timestamp) {

        String sql = "INSERT INTO messages(sender, sender_ip, recipient, recipient_ip, content, timestamp) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sender);
            pstmt.setString(2, sender_ip);
            pstmt.setString(3, recipient);
            pstmt.setString(4, recipient_ip);
            pstmt.setString(5, content);
            pstmt.setString(6, timestamp);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error inserting message: " + e.getMessage());
        }
    }

    /**
     * Retrieves all messages from the database in chronological order.
     * This method is called when a new user joins to display message history.
     * 
     * @return an ArrayList of formatted message strings in the format:
     *         "[timestamp] sender: content"
     */
    public static ArrayList<String> getAllMessages(String username) {

        String sql = "SELECT sender, sender_ip, recipient, recipient_ip, content, timestamp FROM messages " +
                "WHERE recipient = 'ALL' OR sender = ? OR recipient = ? " +
                "ORDER BY id";

        ArrayList<String> listofMessages = new ArrayList<String>();

        try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, username);
            pstmt.setString(2, username);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String recipient = rs.getString("recipient");
                String content = rs.getString("content");
                String timestamp = rs.getString("timestamp");
                String message;

                if (!recipient.equals("ALL")) {
                    message = "[" + timestamp + "] " + sender + "(Private to " + recipient + "): " + content;
                } else {
                    message = "[" + timestamp + "] " + sender + ": " + content;
                }
                listofMessages.add(message);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving messages: " + e.getMessage());
        }

        return listofMessages;
    }

    /**
     * Creates the users table if it doesn't exist
     * Stores username and hashed password for authentication
     */

    public static void createUsersTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " username TEXT NOT NULL UNIQUE,\n"
                + " password_hash TEXT NOT NULL,\n"
                + " created_at TEXT NOT NULL\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            System.out.println("Users table created successfully");
        } catch (Exception e) {
            System.err.println("Error creating users table: " + e.getMessage());
        }

    }

    /**
     * Hashes a password using SHA-256.
     * 
     * @param password the plain text password
     * @return the hashed password as a hexadecimal string
     */

    private static String hashPassword(String password) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);

        }
    }

    /**
     * Registers a new user with username and password.
     * 
     * @param username the username to register
     * @param password the plain text password (will be hashed before storing)
     * @return true if registration successful, false if username already exists
     */
    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password_hash, created_at) VALUES(?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password)); // Hash the password!
            pstmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.executeUpdate();

            System.out.println("User registered: " + username);
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.out.println("Username already exists: " + username);
                return false;
            }
            System.err.println("Error registering user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifies if a password matches the stored hash for a user.
     * 
     * @param username the username to check
     * @param password the plain text password to verify
     * @return true if password matches, false otherwise
     */

    public static boolean verifyPassword(String username, String password) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String inputHash = hashPassword(password);
                return storedHash.equals(inputHash);
            }

            return false; // User not found
        } catch (SQLException e) {
            System.err.println("Error verifying  password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a username exists in the database.
     * 
     * @param username the username to check
     * @return true if user exists, false otherwise
     */
    public static boolean userExists(String username) {
        String sql = "SELECT username FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // true if user found
        } catch (SQLException e) {
            System.err.println("Error checking user existence: " + e.getMessage());
            return false;
        }
    }
}
