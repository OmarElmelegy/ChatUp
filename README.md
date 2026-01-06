# ChatSystem - Multi-Client Chat Application

A Java-based client-server chat system that supports multiple concurrent users with real-time messaging, private messaging (whispers), and user management features.

## 📋 Features

- **Multi-Client Support**: Multiple users can connect simultaneously
- **Real-Time Messaging**: Instant message broadcasting to all connected users
- **Private Messaging**: Whisper functionality for one-to-one communication using `/w <username> <message>`
- **User List**: View all currently connected users with `/list` command
- **Thread-Based Architecture**: Each client connection runs in its own thread for concurrent handling
- **Username System**: Users register with unique usernames upon connection
- **Join/Leave Notifications**: Server broadcasts when users enter or exit the chat

## 🏗️ Architecture

### Class Diagram

The diagram below shows the clear separation between client-side and server-side components:

![ChatSystem Class Diagram](diagrams/ChatSystem%20Class%20Diagram.png)

**Server Side (Green)**:
- `ChatServer`: Main server that accepts connections and routes messages
- `ClientHandler`: Manages individual client connections in separate threads

**Client Side (Blue)**:
- `ChatClient`: User-facing application that sends/receives messages
- `ServerListener`: Background thread that continuously listens for incoming messages

### Sequence Diagram

![ChatSystem Sequence Diagram](diagrams/ChatSystem%20Sequence%20Diagram.png)

### Components

- **ChatServer**: Main server application that accepts client connections and manages message routing
- **ClientHandler**: Handles individual client connections in separate threads, processes commands and messages
- **ChatClient**: Client application that connects to the server and sends user input
- **ServerListener**: Runs in a separate thread on the client side to continuously receive messages from the server

## 🚀 Getting Started

### Prerequisites

- Java JDK 11 or higher
- Terminal/Command Prompt access

### Compilation

```bash
cd src
javac ChatServer.java ClientHandler.java ChatClient.java ServerListener.java
```

### Running the Application

#### 1. Start the Server

```bash
cd src
java ChatServer
```

The server will start listening on port **5001**.

#### 2. Start Client(s)

Open separate terminal windows for each client:

```bash
cd src
java ChatClient
```

Enter your username when prompted.

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
  src/ChatServer.java src/ClientHandler.java src/ChatClient.java src/ServerListener.java
```

## 🔧 Configuration

To change the server port, modify the `PORT` constant in both `ChatServer.java` and `ChatClient.java`:

```java
static final int PORT = 5001; // Change to desired port
```

## 🛠️ Technical Details

- **Port**: 5001 (default)
- **Protocol**: TCP/IP using Java Sockets
- **Threading Model**: One thread per client connection + listener thread on client side
- **Concurrency**: Thread-safe client list management with proper synchronization

## 📁 Project Structure

```
ChatSystem/
├── src/                         # Source code
│   ├── ChatServer.java          # Server main class
│   ├── ClientHandler.java       # Server-side client handler
│   ├── ChatClient.java          # Client main class
│   └── ServerListener.java      # Client-side message receiver
├── diagrams/                    # UML diagrams
│   ├── class-diagram.puml       # PlantUML class diagram source
│   ├── sequence-diagram.puml    # PlantUML sequence diagram source
│   ├── ChatSystem Class Diagram.png
│   └── ChatSystem Sequence Diagram.png
├── docs/                        # Javadoc documentation
│   └── index.html               # Documentation entry point
└── README.md                    # This file
```

## 🐛 Known Issues

- Port must be free before starting the server (use different port if 5001 is in use)
- No username uniqueness validation (duplicate usernames are allowed)
- No message history for newly connected clients
- Server shutdown requires manual termination (Ctrl+C)

## 📝 License

This project is for educational purposes.

## 👥 Authors

ChatSystem Team - AFMS Study Project

---

**Version**: 1.0  
**Last Updated**: January 2026
