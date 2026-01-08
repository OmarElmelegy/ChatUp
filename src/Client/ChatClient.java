package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * ChatClient is the client application for connecting to the chat server.
 * It handles user input, sends messages to the server, and displays
 * messages from other users.
 * 
 * <p>
 * Features:
 * <ul>
 * <li>Connects to the chat server on startup</li>
 * <li>Prompts user for a username</li>
 * <li>Sends user messages to the server</li>
 * <li>Receives and displays messages from other users via ServerListener</li>
 * <li>Supports commands: /list (list users), /w (whisper/private message), bye
 * (quit)</li>
 * </ul>
 * 
 * @author ChatSystem Team
 * @version 1.0
 * @see ServerListener
 */
public class ChatClient {

    /** The port number to connect to on the server */
    static final int PORT = 5001;

    /**
     * Main method that starts the chat client.
     * Establishes connection to the server, handles username registration,
     * and manages the message send/receive loop.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {

        System.setProperty("javax.net.ssl.trustStore", "keystore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password123");

        try {
            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) sslFactory.createSocket("127.0.0.1", PORT);

            socket.startHandshake();

            System.out.println("Connected to SECURE Server successfully!");
            // ... inside main, after connecting ...

            // 1. Setup Network IO
            PrintWriter networkOutput = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader networkInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ServerListener listener = new ServerListener(socket);
            Scanner consoleInput = new Scanner(System.in);

            System.out.print("Enter your username:    ");
            String username = consoleInput.nextLine().trim();

            networkOutput.println(username);

            String welcomeMessage = networkInput.readLine();
            System.out.println(welcomeMessage);

            // Now start the listener thread
            new Thread(listener).start();

            // 2. Setup Keyboard Input (Scanner)

            String userMessage;

            System.out.println("Connected! Type your messages below (type 'bye' to quit):");

            // 3. The Loop
            while (true) {
                // A. Read from your Keyboard
                System.out.print("> ");
                userMessage = consoleInput.nextLine();

                // B. Send to Server
                networkOutput.println(userMessage);

                // C. Stop if we said bye
                if (userMessage.equals("bye")) {
                    break;
                }
            }

            socket.close();
            consoleInput.close();
        } catch (IOException e) {
            e.getMessage();
        }
    }

}
