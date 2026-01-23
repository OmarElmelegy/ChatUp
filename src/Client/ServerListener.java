package Client;

import java.io.DataInputStream;
import java.io.IOException;
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
 * <p>
 * The listener uses a binary protocol to receive:
 * <ul>
 * <li>Text messages (msgType_TEXT = 1)</li>
 * <li>File transfers (msgType_FILE = 2)</li>
 * </ul>
 * 
 * @author ChatSystem Team
 * @version 2.2
 * @see ChatClient
 */
public class ServerListener implements Runnable {

    /** The socket connection to the server */
    private Socket socket;

    /**
     * Defining the msgType bytes at the first sector of the new protocol messages
     */
    private static final byte msgType_TEXT = 1;

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
     * Uses binary protocol to receive text messages (and potentially file
     * transfers).
     */
    public void run() {
        try {
            DataInputStream networkInput = new DataInputStream(socket.getInputStream());

            while (true) {
                byte messageType = networkInput.readByte();

                switch (messageType) {
                    case msgType_TEXT:
                        String message = networkInput.readUTF();
                        System.out.println(message);
                        System.out.print("> ");
                        break;

                    default:
                        System.err.println("Unknown message type received: " + messageType);
                        break;
                }
            }

        } catch (IOException e) {
            System.err.println("\n[Connection Lost]");
        }

    }
}
