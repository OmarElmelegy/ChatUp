# ChatUp - Multi-Client Chat Application

A Java-based client-server chat system that supports multiple concurrent users with real-time messaging, private messaging (whispers), and user management features. The system uses SSL/TLS encryption for secure communications and includes both command-line and JavaFX GUI client interfaces.

## 📋 Features

### 🔐 Security & Authentication
- SSL/TLS encrypted communication with password-protected user accounts (SHA-256 hashing)
- New user registration and returning user authentication via SQLite database
- IP address tracking for security auditing

### 💬 Messaging
- Real-time public broadcasting and private messaging (`/w <username> <message>`)
- Complete message persistence (all public and private messages stored in database)
- File transfer support (up to 50MB) with binary protocol
- Timestamped messages (yyyy-MM-dd HH:mm:ss format)

### ⚙️ Architecture & Performance
- Thread pool architecture (ExecutorService with 50 worker threads) for scalable client handling
- Thread-safe operations with synchronized client list management
- Supports up to 50 concurrent client connections
- Graceful server shutdown with client notifications

### 🖥️ User Interface
- Dual interfaces: Modern JavaFX GUI (dark theme) and command-line client
- User list display (`/list` command) and join/leave notifications
- Configurable connection settings (custom host and port)

## 🏗️ Architecture

### Class Diagram

The diagram below shows the clear separation between client-side and server-side components:

![ChatSystem Class Diagram](/diagrams/ChatSystem%20Class%20Diagram.png)

**Server Side (Green)**:
- `ChatServer`: Main server that accepts connections, manages thread pool, and routes messages
- `ClientHandler`: Manages individual client connections via thread pool

**Client Side (Blue)**:
- `ChatClient`: User-facing application that sends/receives messages
- `ServerListener`: Background thread that continuously listens for incoming messages

### Sequence Diagram

![ChatSystem Sequence Diagram](/diagrams/ChatSystem%20Sequence%20Diagram.png)

## 🖼️ Screenshots

### Multiple Users Connected
![Multiple Users Chatting](/img/MultipleUsers.png)

The modern GUI supports multiple simultaneous users with real-time message synchronization.

### User Left Notification
![User Has Left Chat](/img/UserHasLeftChat.png)

Server broadcasts when users disconnect, keeping all participants informed.

### File Transfer
![User Sending File](/img/UserSendingFile.png)

Users can send files securely using the File button, with recipient receiving a save dialog.

### Components

**Server Side:**
- **ChatServer**: Main server application that accepts SSL client connections, manages a fixed thread pool (50 threads), and routes messages
- **ClientHandler**: Handles individual client connections via thread pool, processes commands and messages
- **DatabaseManager**: Manages SQLite database for persistent storage of messages (public and private), users, and authentication

**Client Side:**
- **ChatClient**: Command-line client application that connects to the server and sends user input
- **ClientGUI**: JavaFX-based graphical client with modern dark-themed UI for chat interaction
- **ServerListener**: Runs in a separate thread on the CLI client side to continuously receive messages from the server

### UI Design

The JavaFX GUI features a modern dark theme with:
- Custom-styled components (text areas, input fields, buttons with hover effects)
- Connection header bar and monospace message display with timestamps
- File transfer dialogs (chooser for sending, save for receiving)
- Welcome screen with command instructions
- Responsive layout and silent close (no error dialogs on window close)

## 🚀 Getting Started

### Prerequisites

- Java JDK 11 or higher
- JavaFX SDK 23.0.1 (included in `lib/` directory)
- Terminal/Command Prompt access
- SSL keystore file (`keystore.jks`) for secure communication

### Compilation

**For Command-Line Client:**
```bash
cd src
javac Server/*.java Client/ChatClient.java Client/ServerListener.java
```

**For JavaFX GUI Client:**
```bash
javac --module-path lib --add-modules javafx.controls,javafx.fxml -d bin src/Server/*.java src/Client/*.java
```

### Running the Application

#### 1. Start the Server

```bash
java --module-path lib --add-modules javafx.controls,javafx.fxml -cp bin Server.ChatServer
```

The server will start listening on port **5001** with SSL/TLS enabled.

#### 2. Start Client(s)

**Option A: Command-Line Client**
```bash
java --module-path lib --add-modules javafx.controls,javafx.fxml -cp bin Client.ChatClient
```

**Option B: JavaFX GUI Client**
```bash
java --module-path lib --add-modules javafx.controls,javafx.fxml -cp bin Client.ClientGUI
```

Or press **F5** in VS Code with the ClientGUI launch configuration.

Enter your username when prompted (GUI shows a dialog, CLI prompts in terminal).

#### 3. Server Shutdown

To gracefully stop the server, press **Ctrl+C**. The server will:
- Notify all connected clients about the shutdown
- Close all client connections properly
- Clients will display a "Server Closed" message and exit automatically

## 💬 Usage

### Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/list` | Display all connected users | `/list` |
| `/w <user> <msg>` | Send a private message (whisper) | `/w Alice Hello there!` |
| `bye` | Disconnect from the chat | `bye` |
| Regular text | Send a public message to all users | `Hello everyone!` |

### Example Session

```
Connected to Server successfully!
Enter your username: Alice
Welcome, Alice!
Connected! Type your messages below (type 'bye' to quit):
> Hello everyone!
SERVER: Bob has joined the chat!
> /w Bob Hi there!
You whispered to Bob: Hi there!
> /list
List of users currently connected are: [Alice, Bob]
> bye
```

## 📚 Documentation

Complete API documentation is available in the `docs/` directory. Open `docs/index.html` in a web browser to view the full Javadoc documentation.

To regenerate documentation:

```bash
javadoc -d docs -sourcepath src -author -version -windowtitle "ChatSystem Documentation" \
  -doctitle "ChatSystem API Documentation" \
  src/Server/*.java src/Client/*.java
```

## 🔧 Configuration

### Server Port

To change the server port, modify the `PORT` constant in `ChatServer.java`, `ChatClient.java`, and `ClientGUI.java`:

```java
static final int PORT = 5001; // Change to desired port
```

### SSL/TLS Configuration

The system uses a keystore file for SSL encryption. The keystore properties are set in the code:

**Server** (`ChatServer.java`):
```java
System.setProperty("javax.net.ssl.keyStore", "../keystore.jks");
System.setProperty("javax.net.ssl.keyStorePassword", "password123");
```

**Client** (`ChatClient.java` and `ClientGUI.java`):
```java
System.setProperty("javax.net.ssl.trustStore", "keystore.jks");
System.setProperty("javax.net.ssl.trustStorePassword", "password123");
```

## 🛠️ Technical Details

- **Port**: 5001 (default, configurable)
- **Protocol**: TCP/IP with SSL/TLS encryption using SSLSocket/SSLServerSocket
- **Message Format**: Binary protocol (DataInputStream/DataOutputStream) with type byte: 1=TEXT, 2=FILE
- **Database**: SQLite (chat.db)
  - `users`: username, password_hash (SHA-256), created_at
  - `messages`: sender, sender_ip, recipient ("ALL" or username), recipient_ip, content, timestamp
- **Threading**: Fixed thread pool (ExecutorService, 50 workers) + CLI listener thread
- **GUI**: JavaFX 23.0.1 with custom dark theme (#1e1e1e background, #0d7377 accents)
- **File Transfer**: Up to 50MB with streaming upload/download
- **Concurrency**: Thread-safe operations with synchronized client list management

## 📁 Project Structure

```
ChatApp-Java/
├── src/                         # Source code
│   ├── Server/
│   │   ├── ChatServer.java      # Server main class with SSL
│   │   ├── ClientHandler.java   # Server-side client handler
│   │   └── DatabaseManager.java # SQLite database operations
│   └── Client/
│       ├── ChatClient.java      # CLI client main class
│       ├── ClientGUI.java       # JavaFX GUI client
│       └── ServerListener.java  # Client-side message receiver (CLI)
|
├── lib/                         # JavaFX libraries
│   ├── javafx.base.jar
│   ├── javafx.controls.jar
│   ├── javafx.fxml.jar
│   ├── javafx.graphics.jar
│   └── ... (native libraries)
├── diagrams/                    # UML diagrams
│   ├── class-diagram.puml       # PlantUML class diagram source
│   └── sequence-diagram.puml    # PlantUML sequence diagram source
├── docs/                        # Javadoc documentation
│   └── index.html               # Documentation entry point
├── .vscode/                     # VS Code configuration
│   ├── settings.json            # Java and JavaFX settings
│   └── launch.json              # Run configurations
├── keystore.jks                 # SSL keystore file
├── MultipleUsers.png            # Screenshot: Multiple users chatting
├── UserHasLeftChat.png          # Screenshot: User disconnect notification
└── README.md                    # This file
```

## 🐛 Known Issues

- SSL keystore password is hardcoded (should use environment variables in production)
- File transfers are not logged in database (only file transfer notifications)
- Thread pool size is fixed at 50 (could be made configurable via config file)

## ✨ Recent Updates

### Version 2.2 (January 2026)
- ✅ Thread pool architecture with ExecutorService (50 worker threads)
- ✅ Private message persistence in database with sender/recipient tracking
- ✅ Password authentication system with SHA-256 hashing
- ✅ User registration and login with database storage
- ✅ Multi-step authentication protocol (CHECK_USER, VERIFY_PASSWORD, REGISTER_PASSWORD)
- ✅ IP address tracking for all messages (public and private)

### Version 2.1 (January 2026)
- ✅ Atomic username validation to prevent duplicates
- ✅ Thread-safe client list operations with synchronization
- ✅ Enhanced error handling and resource cleanup
- ✅ File size protection for large transfers
- ✅ Command filtering in GUI (no local echo for system commands)

### Version 2.0 (January 2026)
- ✅ SQLite database for message persistence
- ✅ Binary file transfer protocol (up to 50MB)
- ✅ Silent window close without error dialogs
- ✅ Background file transfers (non-blocking UI)
- ✅ Interactive file save dialog

### Version 1.1 (January 2026)
- ✅ Modern dark-themed JavaFX GUI
- ✅ Graceful server shutdown with client notifications
- ✅ Configurable connection settings
- ✅ Auto-reconnect handling and improved error messages

---

**Version**: 2.2  
**Last Updated**: January 23, 2026

**Key Features**: SSL/TLS Encryption • Password Authentication • SHA-256 Hashing • File Transfer • Binary Protocol • Message Persistence (Public & Private) • User Registration • Thread Pool Architecture • Thread-Safe Operations • Modern Dark UI • Multi-Client Support • Private Messaging • Graceful Shutdown
