package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * ClientGUI is a JavaFX-based graphical user interface for the chat client.
 * It provides a modern, user-friendly interface for connecting to the secure
 * chat server,
 * sending messages, viewing chat history with timestamps, and transferring
 * files.
 * 
 * <p>
 * Key features:
 * <ul>
 * <li>SSL/TLS encrypted connection to the server</li>
 * <li>Password authentication with SHA-256 hashed passwords</li>
 * <li>User registration for new users with password confirmation</li>
 * <li>Returning user login with password verification</li>
 * <li>User-friendly login dialog for username entry</li>
 * <li>Real-time message display with timestamps</li>
 * <li>Text input field with send button</li>
 * <li>File transfer support with file chooser dialog</li>
 * <li>Binary protocol for efficient message and file transmission</li>
 * <li>Support for all server commands (/list, /w, bye)</li>
 * <li>Non-blocking message reception using background threads</li>
 * <li>Automatic connection error handling with intentional close detection</li>
 * <li>Silent graceful shutdown when user closes the window</li>
 * </ul>
 * 
 * @author ChatSystem Team
 * @version 2.2
 * @see ChatClient
 * @see ServerListener
 */
public class ClientGUI extends Application {

    /** output stream for sending messages to the server */
    private DataOutputStream output;

    /** Reader for receiving messages from the server */
    private DataInputStream input;

    /** Secure socket connection to the server */
    private SSLSocket socket;

    /**
     * The hostname to connect to (default: localhost, can be set via
     * -Dserver.host=hostname)
     */
    private static final String SERVER_HOST = System.getProperty("server.host", "127.0.0.1");

    /**
     * The port number to connect to on the server (default: 5001, can be set via
     * -Dserver.port=port)
     */
    private static final int PORT = Integer.parseInt(System.getProperty("server.port", "5001"));

    /** Formatter for displaying message timestamps */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** The username of the current user */
    private String username;

    /** Flag to track if we're intentionally closing the connection */
    private volatile boolean intentionalClose = false;

    /**
     * Defining the msgType bytes at the first sector of the new protocl messages
     */
    private static final byte msgType_TEXT = 1;
    private static final byte msgType_FILE = 2;

    /**
     * Starts the JavaFX application and initializes the chat GUI.
     * This method prompts for username, establishes a secure connection to the
     * server,
     * and creates the main chat interface.
     * 
     * @param primaryStage the primary stage for this application
     */
    @Override
    public void start(Stage primaryStage) {
        TextInputDialog userNameInput = new TextInputDialog();
        userNameInput.setTitle("Login");
        userNameInput.setHeaderText("Enter your username");
        Optional<String> usernameResult = userNameInput.showAndWait();

        if (!usernameResult.isPresent()) {
            Platform.exit();
            return;
        }

        username = usernameResult.get();

        try {
            System.setProperty("javax.net.ssl.trustStore", "keystore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "password123");

            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) sslFactory.createSocket(SERVER_HOST, PORT);
            socket.startHandshake();

            System.out.println("Connected to server at " + SERVER_HOST + ":" + PORT);

            // Initialize Streams
            output = new DataOutputStream(socket.getOutputStream());
            input = new DataInputStream(socket.getInputStream());

            // Send the handshake username immediately
            synchronized (output) {
                output.writeByte(msgType_TEXT);
                output.writeUTF("CHECK_USER:" + username);
                output.flush();
            }

            // Read response (msgType + message)
            byte responseType = input.readByte();
            String response = input.readUTF();

            String password = null;

            if (response.equals("USER_EXISTS")) {

                TextInputDialog passwordInput = new TextInputDialog();
                passwordInput.setTitle("Login");
                passwordInput.setHeaderText("Welcome back, " + username + "!");
                passwordInput.setContentText("Enter your password:");

                // Makes the password field show in dots
                passwordInput.getEditor().setPromptText("Password");

                Optional<String> passwordResult = passwordInput.showAndWait();

                if (!passwordResult.isPresent()) {
                    socket.close();
                    Platform.exit();
                    return;
                }

                password = passwordResult.get();

                // Send password for verification
                synchronized (output) {
                    output.writeByte(msgType_TEXT);
                    output.writeUTF("VERIFY_PASSWORD:" + password);
                    output.flush();
                }

                // Check if password is correct
                responseType = input.readByte();
                response = input.readUTF();

                if (response.equals("PASSWORD_INCORRECT")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Login Failed");
                    alert.setHeaderText("Incorrect Password");
                    alert.setContentText("The password you entered is incorrect.");
                    alert.showAndWait();
                    socket.close();
                    Platform.exit();
                    return;
                }
            } else if (response.equals("USER_NEW")) {
                TextInputDialog createPasswordInput = new TextInputDialog();
                createPasswordInput.setTitle("Create Account");
                createPasswordInput.setHeaderText("Welcome, " + username + "!");
                createPasswordInput.setContentText("Create a password for your account:");
                createPasswordInput.getEditor().setPromptText("Password");

                Optional<String> passwordResult = createPasswordInput.showAndWait();

                if (!passwordResult.isPresent()) {
                    socket.close();
                    Platform.exit();
                    return;
                }

                password = passwordResult.get();

                // Confirm password
                TextInputDialog confirmPasswordInput = new TextInputDialog();
                confirmPasswordInput.setTitle("Create Account");
                confirmPasswordInput.setHeaderText("Confirm Password");
                confirmPasswordInput.setContentText("Re-enter your password:");
                confirmPasswordInput.getEditor().setPromptText("Password");

                Optional<String> confirmResult = confirmPasswordInput.showAndWait();

                if (!confirmResult.isPresent() || !confirmResult.get().equals(password)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Password Mismatch");
                    alert.setContentText("Passwords do not match. Please try again.");
                    alert.showAndWait();
                    socket.close();
                    Platform.exit();
                    return;
                }

                synchronized (output) {
                    output.writeByte(msgType_TEXT);
                    output.writeUTF("REGISTER_PASSWORD:" + password);
                    output.flush();
                }
            }

            // Final handshake - send username again (now authenticated)
            synchronized (output) {
                output.writeByte(msgType_TEXT);
                output.writeUTF(username);
                output.flush();
            }

        } catch (

        Exception e) {
            // If connection fails, show error and stop
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setContentText("Could not connect to server: " + e.getMessage());
            alert.showAndWait();
            Platform.exit();
            return;
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;");

        // Header with server info
        Label headerLabel = new Label("Secure Chat - Connected to " + SERVER_HOST + ":"
                + PORT);
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-padding: 10px;");
        VBox header = new VBox(
                headerLabel);
        header.setStyle("-fx-background-color: #1e1e1e;");
        header.setAlignment(Pos.CENTER);
        root.setTop(header);

        // Chat area with styling
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-control-inner-background: #1e1e1e; " + "-fx-text-fill: #e0e0e0; "
                + "-fx-font-family: 'Consolas', 'Monaco', monospace; " + "-fx-font-size: 13px; "
                + "-fx-border-color: #404040; " + "-fx-border-width: 1px;");
        root.setCenter(chatArea);

        // Input area
        TextField inputField = new TextField();
        inputField.setPromptText("Type your message here... (Commands: /list, /w <user> <msg>, bye)");
        inputField.setStyle("-fx-background-color: #1e1e1e; " + "-fx-text-fill: #e0e0e0; "
                + "-fx-prompt-text-fill: #808080; " + "-fx-font-size: 13px; " + "-fx-border-color: #404040; "
                + "-fx-border-width: 1px 0 1px 1px; " + "-fx-padding: 8px;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendButton = new Button(
                "Send");
        sendButton.setStyle("-fx-background-color: #0d7377; " + "-fx-text-fill: white; " + "-fx-font-weight: bold; "
                + "-fx-font-size: 13px; " + "-fx-padding: 8px 20px; " + "-fx-border-color: #404040; "
                + "-fx-border-width: 1px; " + "-fx-cursor: hand;");
        sendButton.setOnMouseEntered(
                e -> sendButton.setStyle("-fx-background-color: #14ffec; " + "-fx-text-fill: #1e1e1e; "
                        + "-fx-font-weight: bold; " + "-fx-font-size: 13px; " + "-fx-padding: 8px 20px; "
                        + "-fx-border-color: #14ffec; " + "-fx-border-width: 1px; " + "-fx-cursor: hand;"));
        sendButton
                .setOnMouseExited(e -> sendButton.setStyle("-fx-background-color: #0d7377; " + "-fx-text-fill: white; "
                        + "-fx-font-weight: bold; " + "-fx-font-size: 13px; " + "-fx-padding: 8px 20px; "
                        + "-fx-border-color: #404040; " + "-fx-border-width: 1px; " + "-fx-cursor: hand;"));

        inputField.setOnAction(event -> sendButton.fire());

        Button fileButton = new Button(
                "File");
        fileButton.setStyle("-fx-background-color: #0d7377; " + "-fx-text-fill: white; " + "-fx-font-weight: bold; "
                + "-fx-font-size: 13px; " + "-fx-padding: 8px 20px; " + "-fx-border-color: #404040; "
                + "-fx-border-width: 1px; " + "-fx-cursor: hand;");
        fileButton.setOnMouseEntered(
                e -> fileButton.setStyle("-fx-background-color: #14ffec; " + "-fx-text-fill: #1e1e1e; "
                        + "-fx-font-weight: bold; " + "-fx-font-size: 13px; " + "-fx-padding: 8px 20px; "
                        + "-fx-border-color: #14ffec; " + "-fx-border-width: 1px; " + "-fx-cursor: hand;"));
        fileButton
                .setOnMouseExited(e -> fileButton.setStyle("-fx-background-color: #0d7377; " + "-fx-text-fill: white; "
                        + "-fx-font-weight: bold; " + "-fx-font-size: 13px; " + "-fx-padding: 8px 20px; "
                        + "-fx-border-color: #404040; " + "-fx-border-width: 1px; " + "-fx-cursor: hand;"));

        HBox bottomBar = new HBox(inputField, sendButton,
                fileButton);
        bottomBar.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 10px;");
        root.setBottom(bottomBar);

        // EVENTS

        // Send Button
        sendButton.setOnAction(event -> {
            String text = inputField.getText();
            if (!text.isEmpty()) {
                try {
                    synchronized (output) {
                        output.writeByte(msgType_TEXT);
                        output.writeUTF(text);
                        output.flush();

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                // Don't echo /w, bye, or /list commands locally
                if (!text.startsWith("/w") && !text.equals("bye") && !text.equals("/list")) {
                    String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
                    chatArea.appendText("[" + timestamp + "] ");
                    chatArea.appendText("Me: " + text + "\n");
                }
                inputField.clear();
            }
        });

        // File button
        fileButton.setOnAction(evt -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose file to send");
            File selected = chooser.showOpenDialog(primaryStage);
            if (selected == null)
                return;

            // Send in background to avoid blocking UI
            new Thread(() -> {
                try (FileInputStream fis = new FileInputStream(selected)) {

                    synchronized (output) {
                        output.writeByte(msgType_FILE);
                        output.writeUTF(selected.getName());
                        long length = selected.length();
                        output.writeLong(length);

                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = fis.read(buffer)) != -1) {
                            output.write(buffer, 0, read);
                        }
                        output.flush();
                    }
                    Platform.runLater(() -> chatArea.appendText("Me: Sent file " + selected.getName() + "\n"));

                } catch (IOException ex) {
                    Platform.runLater(() -> chatArea.appendText("Error sending file:" + ex.getMessage()));
                }
            }).start();
        });

        // Background Listener, we start a new thread so we don't freeze the GUI
        new Thread(() -> {
            try {
                while (true) {
                    byte type = input.readByte();

                    switch (type) {
                        case msgType_TEXT:
                            String line = input.readUTF();
                            // CRITICAL: We are in a background thread.
                            // We must use Platform.runLater to touch the GUI.
                            Platform.runLater(() -> {
                                chatArea.appendText(line + "\n");
                            });
                            break;

                        case msgType_FILE:
                            String fileName = input.readUTF();
                            long size = input.readLong();
                            byte[] data = new byte[(int) size];
                            input.readFully(data);

                            // Save Logic: Use Platform.runLater to show file chooser
                            Platform.runLater(() -> {
                                FileChooser saveChooser = new FileChooser();
                                saveChooser.setTitle("Save File");
                                saveChooser.setInitialFileName(fileName);
                                File saveFile = saveChooser.showSaveDialog(primaryStage);

                                if (saveFile != null) {
                                    try {
                                        Path path = saveFile.toPath();
                                        Files.write(path, data);
                                        chatArea.appendText(
                                                "System: Saved file to " + saveFile.getAbsolutePath() + "\n");
                                    } catch (IOException ex) {
                                        chatArea.appendText("System: Error saving file - " + ex.getMessage() + "\n");
                                    }
                                } else {
                                    chatArea.appendText("System: File save cancelled\n");
                                }
                            });
                            break;

                        default:
                            System.err.println("Unknown message type: " + type);
                            break;
                    }
                }
            } catch (Exception e) {
                if (!intentionalClose) {
                    Platform.runLater(() -> {
                        chatArea.appendText("\n[Connection Lost]\n");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Connection Error");
                        alert.setHeaderText("Lost connection to server");
                        alert.setContentText("The connection to the server was lost unexpectedly.");
                        alert.showAndWait();
                        Platform.exit();
                    });
                }
            }

        }).start();

        Scene scene = new Scene(root, 800,
                500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Secure Chat - " + username);
        primaryStage.setOnCloseRequest(e -> {
            intentionalClose = true;
            try {
                if (output != null) {
                    try {
                        synchronized (output) {
                            output.writeByte(msgType_TEXT);
                            output.writeUTF("bye");
                            output.flush();
                        }
                    } catch (Exception ex) {
                        // Ignore errors during intentional close
                    }
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ex) {
                // Ignore errors during intentional close
            }
        });
        primaryStage.show();

        // Welcome message
        chatArea.appendText("═══════════════════════════════════════════════════════════════════════\n");
        chatArea.appendText("  Welcome to Secure Chat, " + username + "!\n");
        chatArea.appendText("═══════════════════════════════════════════════════════════════════════\n");
        chatArea.appendText("  Commands:\n");
        chatArea.appendText("    • /list              View all connected users\n");
        chatArea.appendText("    • /w <user> <msg>    Send private message (whisper)\n");
        chatArea.appendText("    • bye                Disconnect from chat\n");
        chatArea.appendText("═══════════════════════════════════════════════════════════════════════\n\n");
    }

    /**
     * Called when the application is being stopped.
     * Ensures proper cleanup of network resources by closing the socket connection.
     * 
     * @throws Exception if an error occurs during cleanup
     */
    @Override
    public void stop() throws Exception {
        intentionalClose = true;
        if (output != null) {
            try {
                synchronized (output) {
                    output.writeByte(msgType_TEXT);
                    output.writeUTF("bye");
                    output.flush();
                }
            } catch (Exception ex) {
                // Ignore errors during intentional close
            }
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Main method that launches the JavaFX GUI application.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
