import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    static final int PORT = 5001;

    private static List<ClientHandler> clients = new ArrayList<>();

    public static List<ClientHandler> getClients() {
        return clients;
    }

    public static void addClient(ClientHandler client) {
        clients.add(client);
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static void sendPrivateMessage(String senderuserName, String targetuserName, String contentofMessage,
            ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(targetuserName) && client != sender) {
                client.sendMessage(senderuserName + " Says: " + contentofMessage);
            }
        }
    }

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