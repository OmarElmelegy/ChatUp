package Client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * ServerListener runs in a separate thread on the client side to continuously
 * listen for incoming messages from the chat server.
 * 
 * <p>
 * This class allows the client to receive messages from other users
 * while simultaneously being able to send messages from the main thread.
 * Without this separate listener thread, the client would need to alternate
 * between sending and receiving messages.
 * 
 * @author ChatSystem Team
 * @version 1.0
 * @see ChatClient
 */
public class ServerListener implements Runnable {

    /** The socket connection to the server */
    private Socket socket;

    /**
     * Constructs a new ServerListener for the given socket.
     * 
     * @param socket the socket connection to the server
     */
    public ServerListener(Socket socket) {
        this.socket = socket;
    }

    /**
     * Main execution method for the listener thread.
     * Continuously reads messages from the server and displays them to the user.
     * This runs in a separate thread to allow simultaneous sending and receiving of
     * messages.
     */
    public void run() {
        try {
            BufferedReader networkInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;

            while ((message = networkInput.readLine()) != null) {
                System.out.println(message);
                System.out.print("> ");
            }

        } catch (IOException e) {
            System.err.println("Error reading from server: " + e.getMessage());
        }

    }
}
