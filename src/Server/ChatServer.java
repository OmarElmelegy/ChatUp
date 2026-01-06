package Server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatServer is the main server application for the multi-client chat system.
 * It listens for incoming client connections on a specified port and manages
 * all connected clients through ClientHandler threads.
 * 
 * <p>
 * Key features:
 * <ul>
 * <li>Accepts multiple simultaneous client connections</li>
 * <li>Broadcasts messages to all connected clients</li>
 * <li>Supports private messaging between users</li>
 * <li>Maintains a list of active clients</li>
 * </ul>
 * 
 * @author ChatSystem Team
 * @version 1.0
 * @see ClientHandler
 */
public class ChatServer {
    /** The port number on which the server listens for connections */
    static final int PORT = 5001;

    /** List of all currently connected client handlers */
    private static List<ClientHandler> clients = new ArrayList<>();

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
                client.sendMessage(message);
            }
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
                client.sendMessage(senderName + " (Whisper): " + msg);
                sender.sendMessage("You whispered to " + targetName + ": " + msg); // Confirmation
                found = true;
                break; // Stop looking
            }
        }
        if (!found) {
            sender.sendMessage("Error: User '" + targetName + "' not found.");
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
        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            server.bind(new java.net.InetSocketAddress(PORT));
            System.out.println("Server started on port " + PORT + ". Waiting for connections...");

            while (true) {
                Socket client = server.accept();
                System.out.println("New Client connected: " + client.getInetAddress());

                ClientHandler handler = new ClientHandler(client);
                new Thread(handler).start();
                // REMOVED: Don't try to get username here - it's not available yet!
                // The ClientHandler will print the username when it's ready
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}