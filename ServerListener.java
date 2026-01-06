import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerListener implements Runnable {

    private Socket socket;

    public ServerListener(Socket socket) {
        this.socket = socket;
    }

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
