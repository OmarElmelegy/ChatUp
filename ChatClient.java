import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

    static final int PORT = 5001;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("127.0.0.1", PORT);
            System.out.println("Connected to Server successfully!");

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
