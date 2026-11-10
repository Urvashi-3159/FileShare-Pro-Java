# FileShare Pro — Java Computer Networking (Final Academic Build)

Java Swing desktop application using TCP sockets and a multithreaded server.

## Implemented
- Register and login with per-user file storage
- Password hashing with PBKDF2-HMAC-SHA256 and random salts
- TCP client/server architecture
- Upload, list, download and delete files
- Separate storage folder for each account
- Multiple simultaneous client connections
- Upload/download progress bars
- File size display, input validation and basic transfer error handling
- No external libraries required

## Run on Windows (JDK 17+; Java 21 recommended)
Open terminal in the project root:

```bat
mkdir out
javac -d out src\main\java\com\fileshare\*.java
```

Terminal 1:
```bat
java -cp out com.fileshare.FileServer
```

Terminal 2:
```bat
java -cp out com.fileshare.ClientGUI
```

In the GUI, choose the same server IP and port `5000`. Register a username (3-24 letters/digits/_/-) and password (at least 8 characters), or log in.

## Storage
- `users.db` stores usernames, random salts and password hashes (not plaintext passwords).
- `server_files/<username>/` stores each user's files.

Back up both `users.db` and `server_files` if preserving accounts and files.

## Test checklist
1. Start server and GUI.
2. Register a new user.
3. Upload a small file; observe progress.
4. Refresh and confirm file name and size.
5. Select it and download to another folder.
6. Delete the server copy and refresh.
7. Start a second client window and register another user; verify the first user's files are not listed.

## Computer networking concepts for demonstration
TCP connection-oriented communication, IP address, port number, client-server model, `Socket`, `ServerSocket`, byte streams, `DataInputStream`, `DataOutputStream`, multithreading, concurrent client handling and transfer timeouts.

## Important security note
This is a college/LAN demonstration, not production-ready. Passwords are hashed, but network traffic is NOT encrypted with TLS, and this simple protocol does not yet provide hardened session tokens/rate limiting. Use only on a trusted private network; never expose port 5000 publicly or transfer confidential files.
