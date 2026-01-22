package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * ClientHandler manages communication with a single connected client.
 * Each client connection runs in its own thread to handle concurrent users.
 * 
 * <p>
 * This class is responsible for:
 * <ul>
 * <li>Authenticating users with password verification (SHA-256)</li>
 * <li>Registering new users with hashed password storage</li>
 * <li>Reading text messages and files from the client using binary
 * protocol</li>
 * <li>Processing special commands (/list, /w, bye)</li>
 * <li>Processing authentication commands (CHECK_USER, VERIFY_PASSWORD,
 * REGISTER_PASSWORD)</li>
 * <li>Broadcasting public messages to all users</li>
 * <li>Broadcasting files to all users (excluding sender)</li>
 * <li>Handling private messages between users</li>
 * <li>Persisting message history to SQLite database</li>
 * <li>Managing client connection lifecycle</li>
 * <li>Enforcing file size limits (50MB maximum)</li>
 * </ul>
 * 
 * <p>
 * Binary Protocol:
 * <ul>
 * <li>msgType_TEXT (1): Text message with UTF-8 string content</li>
 * <li>msgType_FILE (2): File transfer with filename, size, and binary data</li>
 * </ul>
 * 
 * <p>
 * Authentication Protocol:
 * <ul>
 * <li>CHECK_USER: Client requests username existence check</li>
 * <li>USER_EXISTS / USER_NEW: Server response for username check</li>
 * <li>VERIFY_PASSWORD: Client sends password for authentication</li>
 * <li>REGISTER_PASSWORD: Client sends password for new account</li>
 * <li>PASSWORD_CORRECT / PASSWORD_INCORRECT: Server password verification
 * response</li>
 * </ul>
 * 
 * @author ChatSystem Team
 * @version 2.2
 * @see ChatServer
 * @see DatabaseManager
 */
public class ClientHandler implements Runnable {
    /** The socket connection to the client */
    private Socket clientSocket;

    /** Output stream for sending messages to the client */
    private DataOutputStream output;

    /** The username of the connected client */
    private String username;

    /** Defining time formatter object with the correct format */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Defining the msgType bytes at the first sector of the new protocl messages
     */
    private static final byte msgType_TEXT = 1;
    private static final byte msgType_FILE = 2;

    /**
     * Constructs a new ClientHandler for the given socket connection.
     * 
     * @param socket the client's socket connection
     */
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    /**
     * Sends a message to this specific client.
     * Used by the server to deliver messages to this client.
     * 
     * @param message the message to send
     */
    public void sendText(String message) throws IOException {
        output.writeByte(msgType_TEXT);
        output.writeUTF(message);
        output.flush();
    }

    /**
     * Sends a file to this specific client.
     * Used by the server to deliver files to this client.
     * 
     * @param file the file to send
     */
    public void sendFile(String fileName, byte[] data) throws IOException {
        output.writeByte(msgType_FILE);
        output.writeUTF(fileName);
        output.writeLong(data.length);
        output.write(data);
        output.flush();
    }

    /**
     * Gets the username of this client.
     * 
     * @return the client's username
     */
    public String getUsername() {
        return username;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * Gets the formatter for timestamp formatting.
     * 
     * @return the DateTimeFormatter instance
     */
    public static DateTimeFormatter getFormatter() {
        return formatter;
    }

    /**
     * Closes the client connection gracefully.
     * Sends a goodbye message and closes the socket.
     */
    public void closeConnection() {
        try {
            if (output != null) {
                output.writeByte(msgType_TEXT);
                output.writeUTF("SERVER: Connection closing...");
                output.flush();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing connection for " + username + ": " + e.getMessage());
        }
    }

    /**
     * Main execution method for the client handler thread.
     * Handles the complete lifecycle of a client connection:
     * <ul>
     * <li>Reads and registers the username</li>
     * <li>Sends welcome message</li>
     * <li>Broadcasts join notification</li>
     * <li>Sends message history from database</li>
     * <li>Processes incoming messages and commands</li>
     * <li>Handles file transfers with size validation</li>
     * <li>Persists public messages to database</li>
     * <li>Handles disconnection and cleanup</li>
     * </ul>
     * 
     * <p>
     * Supported commands:
     * <ul>
     * <li>/list - Show all connected users</li>
     * <li>/w &lt;username&gt; &lt;message&gt; - Send private message</li>
     * <li>bye - Disconnect from server</li>
     * </ul>
     * 
     * <p>
     * File Transfer Protocol:
     * <ul>
     * <li>Receives msgType_FILE (2) byte</li>
     * <li>Reads filename as UTF-8 string</li>
     * <li>Reads file size as long (8 bytes)</li>
     * <li>Validates size is under 50MB limit</li>
     * <li>Reads file data as byte array</li>
     * <li>Broadcasts to all other clients</li>
     * </ul>
     */
    @Override
    public void run() {
        try {
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            output = new DataOutputStream(clientSocket.getOutputStream());

            // Read initial message
            byte msgType = input.readByte();
            String initialMessage = input.readUTF();

            if (initialMessage.startsWith("CHECK_USER:")) {
                String requestedUsername = initialMessage.substring("CHECK_USER:".length());

                // Check if user exists in database
                boolean exists = DatabaseManager.userExists(requestedUsername);

                synchronized (output) {
                    output.writeByte(msgType_TEXT);

                    if (exists) {
                        // ═══ EXISTING USER PATH ═══
                        output.writeUTF("USER_EXISTS");
                        output.flush();

                        // Read password verification request
                        msgType = input.readByte(); // Read msgType
                        String passwordMessage = input.readUTF();

                        if (passwordMessage.startsWith("VERIFY_PASSWORD:")) {
                            String password = passwordMessage.substring("VERIFY_PASSWORD:".length());

                            if (DatabaseManager.verifyPassword(requestedUsername, password)) {
                                output.writeByte(msgType_TEXT);
                                output.writeUTF("PASSWORD_CORRECT");
                                output.flush();
                            } else {
                                output.writeByte(msgType_TEXT);
                                output.writeUTF("PASSWORD_INCORRECT");
                                output.flush();
                                clientSocket.close();
                                return; // End connection
                            }
                        }

                    } else {
                        // ═══ NEW USER PATH ═══
                        output.writeUTF("USER_NEW");
                        output.flush();

                        // Read password registration
                        msgType = input.readByte(); // Read msgType
                        String registerMessage = input.readUTF();

                        if (registerMessage.startsWith("REGISTER_PASSWORD:")) {
                            String password = registerMessage.substring("REGISTER_PASSWORD:".length());
                            DatabaseManager.registerUser(requestedUsername, password);
                        }
                    }

                    // Read final username confirmation (OUTSIDE if-else, for BOTH paths)
                    msgType = input.readByte();
                    this.username = input.readUTF();
                }

            } else {
                // Fallback for old clients without authentication
                this.username = initialMessage;
            }

            // ═══ REST OF THE CODE (unchanged) ═══
            ChatServer.addClient(this);
            sendText("Welcome, " + username + "!");
            ChatServer.broadcast("SERVER: " + username + " has joined the chat!", this);

            ArrayList<String> listofMessages = DatabaseManager.getAllMessages(username);
            for (String msg : listofMessages) {
                this.sendText(msg);
            }

            Boolean running = true;
            while (running) {
                msgType = input.readByte();
                String timestamp = LocalDateTime.now().format(formatter);

                switch (msgType) {
                    case msgType_TEXT:
                        String message = input.readUTF().trim();
                        if (message.equals("/list")) {
                            ArrayList<String> listofUsers = new ArrayList<>();
                            for (ClientHandler client : ChatServer.getClients()) {
                                listofUsers.add(client.getUsername());
                            }

                            this.sendText("List of users currently connected : " + listofUsers);

                        } else if (message.startsWith("/w ")) {
                            String[] parts = message.split(" ", 3);
                            if (parts.length < 3) {
                                this.sendText("Usage: /w <username> <message>");
                            } else {
                                String targetuserName = parts[1];
                                String contentofMessage = parts[2];
                                ChatServer.sendPrivateMessage(username, targetuserName, contentofMessage, this);
                            }
                        } else if (message.equals("bye")) {
                            running = false;

                        } else {

                            System.out.println("[" + timestamp + "] " + this.username + " says: " + message);
                            DatabaseManager.insertMessage(username, clientSocket.getInetAddress().getHostAddress(),
                                    "ALL", "---", message, timestamp);
                            ChatServer.broadcast("[" + timestamp + "] " + this.username + ": " + message, this);
                        }

                        break;

                    case msgType_FILE:
                        String fileName = input.readUTF();
                        long fileSize = input.readLong();
                        if (fileSize > 50_000_000) {
                            sendText("SERVER: File too large (max 50MB). Rejected.");
                            // Skip file data in chunks to avoid int overflow
                            long remaining = fileSize;
                            byte[] skipBuffer = new byte[8192];
                            while (remaining > 0) {
                                int toSkip = (int) Math.min(remaining, skipBuffer.length);
                                input.readFully(skipBuffer, 0, toSkip);
                                remaining -= toSkip;
                            }
                        } else {
                            byte[] fileData = new byte[(int) fileSize];
                            input.readFully(fileData); // Read all bytes

                            System.out.println("Received file: " + fileName);

                            message = "[File: " + fileName + "]";
                            DatabaseManager.insertMessage(username, clientSocket.getInetAddress().getHostAddress(),
                                    "ALL", "---", message, timestamp);

                            // Broadcast to others
                            ChatServer.broadcastFile(fileName, fileData, this);
                        }
                        break;

                    default:
                        System.err.println("Unknown message msgType: " + msgType);
                        break;
                }

            }
        } catch (IOException e) {
            System.err.println("Connection error with " + username + ": " + e.getMessage());
        } finally {
            // C. UNREGISTER: Remove myself when I leave (Important!)
            ChatServer.removeClient(this);
            if (username != null) {
                ChatServer.broadcast("SERVER: " + username + " has left the chat!", null);
                System.out.println(username + " has disconnected.");
            }
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}