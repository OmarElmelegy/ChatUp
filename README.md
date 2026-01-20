# ChatSystem - Multi-Client Chat Application

A Java-based client-server chat system that supports multiple concurrent users with real-time messaging, private messaging (whispers), and user management features. The system uses SSL/TLS encryption for secure communications and includes both command-line and JavaFX GUI client interfaces.

## 📋 Features

- **SSL/TLS Security**: Encrypted communication between clients and server
- **Multi-Client Support**: Multiple users can connect simultaneously
- **Real-Time Messaging**: Instant message broadcasting to all connected users
- **Message Persistence**: SQLite database stores chat history for new users joining
- **File Transfer**: Send and receive files securely between users with a dedicated File button
- **Binary Protocol**: Efficient message and file transmission using DataInputStream/DataOutputStream
- **Private Messaging**: Whisper functionality for one-to-one communication using `/w <username> <message>`
- **User List**: View all currently connected users with `/list` command
- **Thread-Based Architecture**: Each client connection runs in its own thread for concurrent handling
- **Username System**: Users register with unique usernames upon connection
- **Join/Leave Notifications**: Server broadcasts when users enter or exit the chat
- **Dual Client Interfaces**: Both command-line (ChatClient) and GUI (ClientGUI) options
- **Timestamps**: All messages display with precise timestamps
- **Modern JavaFX GUI**: Dark-themed graphical interface with styled components
- **Silent Close**: GUI closes gracefully without error dialogs when user clicks X button
- **Graceful Shutdown**: Server properly closes all client connections when shutting down
- **Configurable Connection**: Clients can connect to custom hosts and ports
- **Auto-Reconnect Notifications**: Clients are notified when server closes unexpectedly
- **Thread-Safe Operations**: Synchronized output streams prevent data corruption

## 🏗️ Architecture

### Class Diagram

The diagram below shows the clear separation between client-side and server-side components:

![ChatSystem Class Diagram](/diagrams/ChatSystem%20Class%20Diagram.png)

**Server Side (Green)**:
- `ChatServer`: Main server that accepts connections and routes messages
- `ClientHandler`: Manages individual client connections in separate threads

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
- **ChatServer**: Main server application that accepts SSL client connections and manages message routing
- **ClientHandler**: Handles individual client connections in separate threads, processes commands and messages
- **DatabaseManager**: Manages SQLite database for persistent message storage and retrieval

**Client Side:**
- **ChatClient**: Command-line client application that connects to the server and sends user input
- **ClientGUI**: JavaFX-based graphical client with modern dark-themed UI for chat interaction
- **ServerListener**: Runs in a separate thread on the CLI client side to continuously receive messages from the server

### UI Design

The JavaFX GUI client features:
- **Dark Theme**: Modern dark color scheme (#1e1e1e background, #0d7377 accent colors)
- **Styled Components**: Custom-styled text areas, input fields, and buttons
- **Header Bar**: Shows connection information (server host and port)
- **Message Display**: Monospace font for clear message readability with timestamps
- **Interactive Buttons**: Send and File buttons with hover effects for better user experience
- **File Transfer UI**: File chooser dialog for sending files, save dialog for receiving files
- **Welcome Screen**: Displays available commands and usage instructions
- **Responsive Layout**: Automatically adjusts to window resizing
- **Silent Close**: No error dialogs when user intentionally closes the window

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
- **Protocol**: TCP/IP using Java Sockets with SSL/TLS encryption
- **Message Protocol**: Binary protocol using DataInputStream/DataOutputStream with message type byte (1=TEXT, 2=FILE)
- **Database**: SQLite (chat.db) for persistent message storage
- **Security**: SSL/TLS using SSLSocket and SSLServerSocket
- **Threading Model**: One thread per client connection + listener thread on CLI client side
- **GUI Framework**: JavaFX 23.0.1 for graphical client with custom dark theme
- **Concurrency**: Thread-safe client list management with proper synchronization
- **File Transfer**: Supports files up to 50MB with streaming upload/download
- **Time Formatting**: yyyy-MM-dd HH:mm:ss format for message timestamps
- **Shutdown Handling**: Graceful server shutdown with client notification via shutdown hooks
- **UI Design**: Modern dark theme with custom-styled components and hover effects
- **Error Handling**: Intentional close detection to prevent false error dialogs

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

- No username uniqueness validation (duplicate usernames are allowed)
- SSL keystore password is hardcoded (should use environment variables in production)
- Private messages are not stored in the database

## ✨ Recent Updates

### Version 2.0 (January 2026)
- ✅ **Message Persistence**: SQLite database stores chat history for new users
- ✅ **Thread Synchronization**: Fixed race conditions in output stream operations
- ✅ **File Transfer**: Send and receive files securely between users
- ✅ **Binary Protocol**: Implemented efficient DataInputStream/DataOutputStream protocol
- ✅ **Silent Close**: Graceful window closure without error dialogs
- ✅ **Enhanced UI**: Added File button with matching style and hover effects
- ✅ **File Size Limit**: Server enforces 50MB file size limit for security
- ✅ **Background File Transfer**: Non-blocking file uploads to prevent UI freezing
- ✅ **Save Dialog**: Interactive file save dialog when receiving files

### Version 1.1 (January 2026)
- ✅ **Enhanced UI**: Modern dark-themed GUI with styled components
- ✅ **Graceful Shutdown**: Server properly notifies and closes all clients on shutdown
- ✅ **Configurable Connection**: Support for custom server host and port via system properties
- ✅ **Auto-Reconnect Handling**: Clients display alerts when server closes unexpectedly
- ✅ **Improved Diagrams**: Class and sequence diagrams now use orthogonal (rectangular) lines
- ✅ **Better Error Handling**: Enhanced connection error messages and user notifications
- ✅ **Welcome Screen**: GUI displays command help on startup

---

**Version**: 2.0  
**Last Updated**: January 20, 2026

**Key Features**: SSL/TLS Encryption • File Transfer • Binary Protocol • Message Persistence • Modern Dark UI • Multi-Client Support • Private Messaging • Graceful Shutdown
