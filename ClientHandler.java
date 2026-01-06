import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter output; // Moved to class level!
    private String username;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    // New Helper Method for the Server to use
    public void sendMessage(String message) {
        output.println(message);
    }

    public String getUsername() {
        return username;
    }

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