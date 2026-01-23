package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * ChatClient is the client application for connecting to the chat server.
 * It handles user input, sends messages to the server, and displays
 * messages from other users.
 * 
 * <p>
 * Features:
 * <ul>
 * <li>Connects to the chat server on startup with SSL/TLS encryption</li>
 * <li>Password authentication with SHA-256 hashed passwords</li>
 * <li>User registration for new users with password confirmation</li>
 * <li>Returning user login with password verification</li>
 * <li>Prompts user for a username and password</li>
 * <li>Sends user messages to the server using binary protocol</li>
 * <li>Receives and displays messages from other users via ServerListener</li>
 * <li>Supports commands: /list (list users), /w (whisper/private message), bye
 * (quit)</li>
 * <li>Binary protocol for efficient message transmission</li>
 * </ul>
 * 
 * @author ChatSystem Team
 * @version 2.2
 * @see ServerListener
 */
public class ChatClient {

    /** The port number to connect to on the server */
    static final int PORT = 5001;

    /**
     * Defining the msgType bytes at the first sector of the new protocol messages
     */
    private static final byte msgType_TEXT = 1;

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

        Scanner consoleInput = new Scanner(System.in);

        try {
            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) sslFactory.createSocket("127.0.0.1", PORT);

            socket.startHandshake();

            System.out.println("Connected to SECURE Server successfully!");

            // 1. Setup Network IO with binary protocol
            DataOutputStream networkOutput = new DataOutputStream(socket.getOutputStream());
            DataInputStream networkInput = new DataInputStream(socket.getInputStream());

            // 2. Username prompt
            System.out.print("Enter your username: ");
            String username = consoleInput.nextLine().trim();

            // 3. Send CHECK_USER handshake
            networkOutput.writeByte(msgType_TEXT);
            networkOutput.writeUTF("CHECK_USER:" + username);
            networkOutput.flush();

            // 4. Read response
            byte responseType = networkInput.readByte();
            String response = networkInput.readUTF();

            String password = null;

            if (response.equals("USER_EXISTS")) {
                // Existing user - authenticate
                System.out.println("Welcome back, " + username + "!");
                System.out.print("Enter your password: ");
                password = consoleInput.nextLine();

                // Send password for verification
                networkOutput.writeByte(msgType_TEXT);
                networkOutput.writeUTF("VERIFY_PASSWORD:" + password);
                networkOutput.flush();

                // Check if password is correct
                responseType = networkInput.readByte();
                response = networkInput.readUTF();

                if (response.equals("PASSWORD_INCORRECT")) {
                    System.out.println("ERROR: Incorrect password. Connection closing.");
                    socket.close();
                    consoleInput.close();
                    return;
                }
                System.out.println("Authentication successful!");

            } else if (response.equals("USER_NEW")) {
                // New user - register
                System.out.println("Welcome, " + username + "! Creating new account...");
                System.out.print("Create a password for your account: ");
                password = consoleInput.nextLine();

                // Confirm password
                System.out.print("Confirm your password: ");
                String confirmPassword = consoleInput.nextLine();

                if (!password.equals(confirmPassword)) {
                    System.out.println("ERROR: Passwords do not match. Connection closing.");
                    socket.close();
                    consoleInput.close();
                    return;
                }

                // Send registration password
                networkOutput.writeByte(msgType_TEXT);
                networkOutput.writeUTF("REGISTER_PASSWORD:" + password);
                networkOutput.flush();

                System.out.println("Account created successfully!");
            }

            // 5. Final handshake - send username again (now authenticated)
            networkOutput.writeByte(msgType_TEXT);
            networkOutput.writeUTF(username);
            networkOutput.flush();

            // 6. Start the listener thread for receiving messages
            ServerListener listener = new ServerListener(socket);
            new Thread(listener).start();

            // 7. Display welcome and instructions
            System.out.println("\n═══════════════════════════════════════════════════════════════════════");
            System.out.println("  Welcome to Secure Chat, " + username + "!");
            System.out.println("═══════════════════════════════════════════════════════════════════════");
            System.out.println("  Commands:");
            System.out.println("    • /list              View all connected users");
            System.out.println("    • /w <user> <msg>    Send private message (whisper)");
            System.out.println("    • bye                Disconnect from chat");
            System.out.println("═══════════════════════════════════════════════════════════════════════\n");
            System.out.println("Connected! Type your messages below:");

            // 8. Main message loop
            String userMessage;
            while (true) {
                System.out.print("> ");
                userMessage = consoleInput.nextLine();

                // Send message to server using binary protocol
                networkOutput.writeByte(msgType_TEXT);
                networkOutput.writeUTF(userMessage);
                networkOutput.flush();

                // Stop if we said bye
                if (userMessage.equals("bye")) {
                    break;
                }
            }

            socket.close();
            consoleInput.close();
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            consoleInput.close();
        }
    }

}
