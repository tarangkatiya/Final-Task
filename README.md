Chat Application - Java

A simple real-time peer-to-peer chat system built with Java Sockets, Threads, and JavaFX. It supports group messaging, private messaging, user nicknames, and connection logs. Messages are Base64-encoded for basic encryption.

Features

Multi-client support (Server handles many clients at once).
Group chat – messages are broadcast to all connected clients.
Private chat – use /w to send a private message.
Nicknames – users pick a nickname when joining.
Logs – server terminal shows all joins, messages, and disconnects.
Basic Encryption – messages are Base64 encoded before transmission.
JavaFX GUI – user-friendly client interface.
Tools & Technologies

Java (JDK 11+ recommended)
JavaFX (for client GUI)
Socket Programming (ServerSocket, Socket)
Threads (for concurrency)
How to Run

Compile: javac Chat_Server.java 

Start Server: java Chat_Server (java Chat_Server 5000)
<img width="1920" height="1080" alt="Screenshot (297)" src="https://github.com/user-attachments/assets/d3845cac-84e2-470a-89e7-a55796fd1642" />


Start Client(s) java Chat_Client (java Chat_Client)

Enter server host (localhost if running locally).
Enter server port (e.g., 5000).
Choose a nickname.
Usage

Send a group message: type normally in the input box.
Send a private message: /w
<img width="1920" height="1080" alt="Screenshot (299)" src="https://github.com/user-attachments/assets/1e44acbb-c12b-4395-b304-b51c21450c68" />

Quit: close the client window or press X.
Security

Messages are Base64-encoded before sending over the network.
This is not strong encryption, but it prevents raw-text snooping.
For real security, integrate AES/RSA encryption.
Next Steps / Improvements

Add online user list in the client GUI.
Implement AES encryption for stronger security.

Author:- Tarang Katiyar.
Store chat logs in a database.
Add file-sharing support.
Screenshots
