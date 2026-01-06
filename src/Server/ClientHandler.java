package Server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 * ClientHandler manages communication with a single connected client.
 * Each client connection runs in its own thread to handle concurrent users.
 * 
 * <p>
 * This class is responsible for:
 * <ul>
 * <li>Reading messages from the client</li>
 * <li>Processing special commands (/list, /w, bye)</li>
 * <li>Broadcasting public messages to all users</li>
 * <li>Handling private messages between users</li>
 * <li>Managing client connection lifecycle</li>
 * </ul>
 * 
 * @author ChatSystem Team
 * @version 1.0
 * @see ChatServer
 */
public class ClientHandler implements Runnable {
    /** The socket connection to the client */
    private Socket clientSocket;

    /** Output stream for sending messages to the client */
    private PrintWriter output;

    /** The username of the connected client */
    private String username;

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
    public void sendMessage(String message) {
        output.println(message);
    }

    /**
     * Gets the username of this client.
     * 
     * @return the client's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Main execution method for the client handler thread.
     * Handles the complete lifecycle of a client connection:
     * <ul>
     * <li>Reads and registers the username</li>
     * <li>Sends welcome message</li>
     * <li>Broadcasts join notification</li>
     * <li>Processes incoming messages and commands</li>
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
     */
    public void run() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // Initialize the class-level output writer
            output = new PrintWriter(clientSocket.getOutputStream(), true);

            // A. REGISTER: Add myself to the list
            ChatServer.addClient(this);

            // Read username first and send it back in the broadcast
            String message = input.readLine();
            this.username = message;
            output.println("Welcome, " + username + "!");

            ChatServer.broadcast("SERVER: " + username + " has joined the chat!", this);

            while ((message = input.readLine()) != null) {

                if (message.equals("/list")) {
                    ArrayList<String> listofUsers = new ArrayList<>();
                    for (ClientHandler client : ChatServer.getClients()) {
                        listofUsers.add(client.getUsername());
                    }

                    this.sendMessage("List of users currently connected are: " + listofUsers);

                } else if (message.startsWith("/w ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length < 3) {
                        this.sendMessage("Usage: /w <username> <message>");
                    } else {
                        String targetuserName = parts[1];
                        String contentofMessage = parts[2];
                        ChatServer.sendPrivateMessage(username, targetuserName, contentofMessage, this);
                    }
                } else if (message.equals("bye")) {
                    break;

                } else {
                    System.out.println(this.username + " says: " + message);
                    ChatServer.broadcast(this.username + ": " + message, this);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // C. UNREGISTER: Remove myself when I leave (Important!)
            ChatServer.removeClient(this);
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}