package Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * DatabaseManager handles all database operations for the chat system.
 * It provides persistent storage for chat messages using SQLite database.
 * 
 * <p>
 * This class manages:
 * <ul>
 * <li>Database connection and initialization</li>
 * <li>Creating the messages table structure</li>
 * <li>Inserting new messages into the database</li>
 * <li>Retrieving message history for new users</li>
 * </ul>
 * 
 * <p>
 * Database Schema:
 * 
 * <pre>
 * CREATE TABLE messages (
 *   id INTEGER PRIMARY KEY,
 *   sender TEXT NOT NULL,
 *   content TEXT NOT NULL,
 *   timestamp TEXT NOT NULL
 * );
 * </pre>
 * 
 * @author ChatSystem Team
 * @version 2.0
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
                + " id integer PRIMARY KEY, \n"
                + " sender text NOT NULL, \n"
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
    public static void insertMessage(String sender, String content, String timestamp) {

        String sql = "INSERT INTO messages(sender, content, timestamp) VALUES(?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sender);
            pstmt.setString(2, content);
            pstmt.setString(3, timestamp);

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
    public static ArrayList<String> getAllMessages() {

        String sql = "SELECT sender, content, timestamp FROM messages";

        ArrayList<String> listofMessages = new ArrayList<String>();

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String sender = rs.getString("sender");
                String content = rs.getString("content");
                String timestamp = rs.getString("timestamp");
                String message = "[" + timestamp + "] " + sender + ": " + content;

                listofMessages.add(message);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving messages: " + e.getMessage());
        }

        return listofMessages;
    }
}
