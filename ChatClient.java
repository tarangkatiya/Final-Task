package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class ChatClient extends Application {

    private TextArea chatArea;
    private TextField inputField;
    private Button sendButton;
    private Label statusLabel;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nick;

    @Override
    public void start(Stage primaryStage) {
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        inputField = new TextField();
        inputField.setPromptText("Type message (/w <nick> <message> for private)");

        sendButton = new Button("Send");
        statusLabel = new Label("Not connected");

        HBox bottom = new HBox(8, inputField, sendButton);
        bottom.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setCenter(chatArea);
        root.setBottom(bottom);
        root.setTop(statusLabel);
        BorderPane.setMargin(chatArea, new Insets(8));

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("JavaFX Chat Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        inputField.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER) { sendMessage(); evt.consume(); }
        });
        sendButton.setOnAction(e -> sendMessage());

        connectFlow();
    }

    private void connectFlow() {
        TextInputDialog hostDialog = new TextInputDialog("localhost");
        hostDialog.setHeaderText("Server Host"); hostDialog.setContentText("Host:");
        Optional<String> hostOpt = hostDialog.showAndWait();
        if (!hostOpt.isPresent()) { Platform.exit(); return; }
        String host = hostOpt.get().trim();

        TextInputDialog portDialog = new TextInputDialog("5000");
        portDialog.setHeaderText("Server Port"); portDialog.setContentText("Port:");
        Optional<String> portOpt = portDialog.showAndWait();
        if (!portOpt.isPresent()) { Platform.exit(); return; }
        int port = Integer.parseInt(portOpt.get().trim());

        TextInputDialog nickDialog = new TextInputDialog("User" + (int)(Math.random()*1000));
        nickDialog.setHeaderText("Choose Nickname"); nickDialog.setContentText("Nickname:");
        Optional<String> nickOpt = nickDialog.showAndWait();
        if (!nickOpt.isPresent()) { Platform.exit(); return; }
        nick = nickOpt.get().trim();

        new Thread(() -> {
            try {
                appendToChat("[System] Connecting to " + host + ":" + port + " ...");
                socket = new Socket(host, port);
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                out.println("NICK:" + nick);

                String serverResp = in.readLine();
                if (serverResp != null) {
                    if (serverResp.startsWith("NICK_OK:")) {
                        nick = serverResp.substring(8);
                        setStatus("Connected as " + nick);
                        appendToChat("[System] Connected as " + nick);
                    } else if (serverResp.startsWith("INFO:")) {
                        appendToChat("[Server] " + decodeSafe(serverResp.substring(5)));
                    }
                }

                String line;
                while ((line = in.readLine()) != null) handleServerLine(line);

            } catch (IOException e) {
                appendToChat("[System] Connection failed: " + e.getMessage());
                setStatus("Disconnected");
            }
        }).start();
    }

    private void handleServerLine(String line) {
        if (line == null || line.trim().isEmpty())
            return;

        if (line.startsWith("MSG:")) {
            String rest = line.substring(4);
            int idx = rest.indexOf(':');
            if (idx > 0) {
                String from = rest.substring(0, idx);
                String text = decodeSafe(rest.substring(idx + 1));
                appendToChat(from + ": " + text);
            }
        } else if (line.startsWith("PVT:")) {
            String rest = line.substring(4);
            int idx = rest.indexOf(':');
            if (idx > 0) {
                String from = rest.substring(0, idx);
                String text = decodeSafe(rest.substring(idx + 1));
                appendToChat("[Private] " + from + ": " + text);
            }
        } else if (line.startsWith("PVT_SENT:")) {
            String rest = line.substring(9);
            int idx = rest.indexOf(':');
            if (idx > 0) {
                String target = rest.substring(0, idx);
                String text = decodeSafe(rest.substring(idx + 1));
                appendToChat("[To " + target + "] " + text);
            }
        } else if (line.startsWith("INFO:")) appendToChat("[Info] " + decodeSafe(line.substring(5)));
        else if (line.startsWith("NICK_OK:")) appendToChat("[System] Nick assigned: " + line.substring(8));
        else appendToChat("[Raw] " + line);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || out == null) return;

        try {
            if (text.startsWith("/w ") || text.startsWith("/W ")) {
                String[] parts = text.split("\\s+", 3);
                if (parts.length < 3) appendToChat("[System] Private message format: /w <nick> <message>");
                else out.println("PVT:" + parts[1] + ":" + encode(parts[2]));
            } else out.println("MSG:" + encode(text));
            inputField.clear();
        } catch (Exception e) { appendToChat("[System] Error sending: " + e.getMessage()); }
    }

    private void appendToChat(String text) {
        Platform.runLater(() -> chatArea.appendText(text + "\n"));
    }

    private void setStatus(String s) {
        Platform.runLater(() -> statusLabel.setText(s));
    }

    private static String encode(String plain) {
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeSafe(String b64) {
        try { return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException e) {
            return "<invalid base64>";
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        try {
            if (out != null) out.println("QUIT");
            if (socket != null && !socket.isClosed()) socket.close();
        }
        catch (IOException ignored) {

        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
