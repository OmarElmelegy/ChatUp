package Server;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * ChatServer is the main server application for the multi-client chat system.
 * It listens for incoming client connections on a specified port and manages
 * all connected clients through ClientHandler threads.
 * 
 * <p>
 * Key features:
 * <ul>
 * <li>SSL/TLS encrypted connections for secure communication</li>
 * <li>Accepts multiple simultaneous client connections</li>
 * <li>Broadcasts text messages to all connected clients</li>
 * <li>Broadcasts files to all connected clients</li>
 * <li>Supports private messaging between users</li>
 * <li>Maintains a list of active clients</li>
 * <li>Persistent message storage using SQLite database</li>
 * <li>Graceful shutdown with client notification</li>
 * </ul>
 * 
 * @author ChatSystem Team
 * @version 2.0
 * @see ClientHandler
 * @see DatabaseManager
 */
public class ChatServer {
    /** The port number on which the server listens for connections */
    static final int PORT = 5001;

    /** List of all currently connected client handlers */
    private static List<ClientHandler> clients = new ArrayList<>();

    /** Server socket instance for managing shutdown */
    private static SSLServerSocket serverSocket;

    /**
     * Gets the list of all connected clients.
     * 
     * @return the list of ClientHandler instances representing connected clients
     */
    public static List<ClientHandler> getClients() {
        return clients;
    }

    /**
     * Adds a new client to the list of connected clients.
     * This method is called when a new client successfully connects.
     * 
     * @param client the ClientHandler instance to add
     */
    public static void addClient(ClientHandler client) {
        clients.add(client);
    }

    /**
     * Removes a client from the list of connected clients.
     * This method is called when a client disconnects.
     * 
     * @param client the ClientHandler instance to remove
     */
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    /**
     * Broadcasts a message to all connected clients except the sender.
     * 
     * @param message the message to broadcast
     * @param sender  the ClientHandler of the sender (will not receive the message)
     */
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                try {
                    client.sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Broadcasts a message to all connected clients including the sender.
     * 
     * @param message the message to broadcast to everyone
     */
    public static void broadcastAll(String message) {
        for (ClientHandler client : clients) {
            try {
                client.sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Shuts down the server gracefully by notifying all clients and closing
     * connections.
     * 
     * @param exitAfterShutdown if true, calls System.exit(0) after cleanup
     */
    public static void shutdown(boolean exitAfterShutdown) {
        System.out.println("\nShutting down server...");
        broadcastAll("SERVER: Server is shutting down. All connections will be closed.");

        // Give clients time to receive the message
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Close all client connections
        for (ClientHandler client : new ArrayList<>(clients)) {
            try {
                client.closeConnection();
            } catch (Exception e) {
                System.err.println("Error closing client connection: " + e.getMessage());
            }
        }

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        System.out.println("Server shutdown complete.");

        if (exitAfterShutdown) {
            System.exit(0);
        }
    }

    /**
     * Sends a private message from one user to another.
     * Only the target user receives the message, along with a confirmation to the
     * sender.
     * 
     * @param senderName the username of the sender
     * @param targetName the username of the recipient
     * @param msg        the message content
     * @param sender     the ClientHandler of the sender
     */
    public static void sendPrivateMessage(String senderName, String targetName, String msg, ClientHandler sender) {
        boolean found = false;
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(targetName)) {
                String timestamp = LocalTime.now().format(ClientHandler.getFormatter());

                try {
                    client.sendText("[" + timestamp + "] " + senderName + " (Whisper): " + msg);
                    sender.sendText("[" + timestamp + "] You whispered to " + targetName + ": " + msg); // Confirmation

                } catch (IOException e) {
                    e.printStackTrace();
                }
                found = true;
                break; // Stop looking
            }
        }
        if (!found) {
            try {
                sender.sendText("Error: User '" + targetName + "' not found.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Broadcasts a file to all connected clients except the sender.
     * Notifies each recipient that a file is incoming before sending the file data.
     * 
     * @param filename the name of the file being sent
     * @param content  the binary content of the file as a byte array
     * @param sender   the ClientHandler of the sender (will not receive the file)
     */
    public static void broadcastFile(String filename, byte[] content, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (!client.getUsername().equals(sender.getUsername())) {
                try {
                    client.sendText("Incoming file from " + sender.getUsername());
                    client.sendFile(filename, content);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Main method that starts the chat server.
     * Creates a ServerSocket and continuously accepts incoming client connections.
     * Each client connection is handled by a new ClientHandler thread.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {

        System.setProperty("javax.net.ssl.keyStore", "keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password123");

        // Add shutdown hook to handle Ctrl+C gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown(false); // Don't call System.exit from within shutdown hook
        }));

        try {
            SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            serverSocket = (SSLServerSocket) sslFactory.createServerSocket(PORT);
            System.out.println("SECURE Server started on port " + PORT + ". Waiting for connections...");
            System.out.println("Press Ctrl+C to stop the server.");

            DatabaseManager.createNewTable();

            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("New Client connected: " + client.getInetAddress());

                ClientHandler handler = new ClientHandler(client);
                new Thread(handler).start();
                // REMOVED: Don't try to get username here - it's not available yet!
                // The ClientHandler will print the username when it's ready
            }
        } catch (java.net.BindException e) {
            System.err.println("\n╔════════════════════════════════════════════════════════════╗");
            System.err.println("║  ERROR: Port " + PORT + " is already in use!                    ║");
            System.err.println("║                                                            ║");
            System.err.println("║  Another instance of ChatServer may already be running.   ║");
            System.err.println("║  Please stop the existing server or choose a different    ║");
            System.err.println("║  port number.                                              ║");
            System.err.println("╚════════════════════════════════════════════════════════════╝\n");
            System.exit(1);
        } catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed()) {
                System.out.println("Server socket closed.");
            } else {
                System.err.println("Error starting server: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}