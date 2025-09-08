package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 5000;
    private static final String SECRET_KEY = "secret123";
    public static Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        int port = PORT;
        String key = SECRET_KEY;

        if (args.length >= 1) port = Integer.parseInt(args[0]);
        if (args.length >= 2) key = args[1];

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, key);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcast(String message, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude) {
                client.sendMessage(message);
            }
        }
    }

    static void remove(ClientHandler client) {
        clients.remove(client);
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String key;
    private String name;

    public ClientHandler(Socket socket, String key) {
        this.socket = socket;
        this.key = key;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            out.println("Enter your nickname:");
            name = in.readLine();
            ChatServer.broadcast(name + " joined the chat.", this);

            String msg;
            while ((msg = in.readLine()) != null) {
                String decrypted = decrypt(msg);
                if (decrypted.startsWith("/pm")) {
                    String[] parts = decrypted.split(" ", 3);
                    if (parts.length >= 3) {
                        privateMessage(parts[1], name + " (PM): " + parts[2]);
                    }
                } else {
                    ChatServer.broadcast(name + ": " + decrypted, this);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ChatServer.remove(this);
            ChatServer.broadcast(name + " left the chat.", this);
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void privateMessage(String targetName, String message) {
        for (ClientHandler client : ChatServer.clients) {
            if (client.name.equals(targetName)) {
                client.sendMessage(message);
                break;
            }
        }
    }

    public void sendMessage(String msg) {
        out.println(encrypt(msg));
    }

    private String encrypt(String msg) {
        return Base64.getEncoder().encodeToString((msg + key).getBytes());
    }

    private String decrypt(String msg) {
        try {
            String decoded = new String(Base64.getDecoder().decode(msg));
            return decoded.replace(key, "");
        } catch (Exception e) {
            return msg;
        }
    }
}

