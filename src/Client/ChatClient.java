package Client;

import Shared.User;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

public class ChatClient {
    private Socket socket;
    private User currentUser;
    private final ChatForm chatForm;
    private PrintWriter out;
    private BufferedReader in;
    private final List<User> userList = new ArrayList<>();
    private volatile boolean shouldReconnect = true;
    private int reconnectAttempts = 0;
    private final int MAX_RECONNECT_ATTEMPTS = 5;
    private final long RECONNECT_DELAY_MS = 5000; // 5 seconds

    public ChatClient(ChatForm chatForm) {
        this.chatForm = chatForm;
    }

    public boolean connect(String host, int port, String username) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            currentUser = new User(username);
            chatForm.setChatClient(this);
            sendMessage("/register " + username);
            
            new Thread(this::listenForMessages).start();
            chatForm.updateConnectionStatus(true);
            return true;
        } catch (IOException e) {
            chatForm.showError("Connection failed: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void setStatus(String status) {
        sendMessage("/status " + status);
    }

    public User getCurrentUser() {
        return currentUser;
    }
    
    private void listenForMessages() {
        try {
            StringBuilder messageBuffer = new StringBuilder();
            char[] readBuffer = new char[1024];
            int charsRead;

            while ((charsRead = in.read(readBuffer)) != -1) {
                messageBuffer.append(readBuffer, 0, charsRead);

                // Process all complete messages (delimited by newline)
                int newlineIndex;
                while ((newlineIndex = messageBuffer.indexOf("\n")) != -1) {
                    String completeMessage = messageBuffer.substring(0, newlineIndex).trim();
                    messageBuffer.delete(0, newlineIndex + 1);

                    if (completeMessage.startsWith("/userlist")) {
                        handleUserList(completeMessage);
                    } else {
                        chatForm.displayMessage(completeMessage);
                    }
                }
            }
            handleDisconnection();
        } catch (IOException e) {
            handleDisconnection();
        }
    }

    private void handleUserList(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.substring(10).split(",");
            userList.clear();

            for (String part : parts) {
                if (!part.isEmpty()) {
                    String[] userInfo = part.split(":");
                    if (userInfo.length >= 1) {
                        User user = new User(userInfo[0]);
                        if (userInfo.length > 1) {
                            user.setStatus(userInfo[1]);
                        }
                        // Skip guest users in display
                        if (!user.getUsername().startsWith("Guest")) {
                            userList.add(user);
                        }
                    }
                }
            }
            chatForm.updateUserList(userList);
        });
    }
    private void handleDisconnection() {
        chatForm.updateConnectionStatus(false);
        chatForm.displayMessage("[SYSTEM] Connection lost");

        if (shouldReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            attemptReconnect();
        } else {
            chatForm.showError("Failed to reconnect after " + MAX_RECONNECT_ATTEMPTS + " attempts");
        }
    }
    public void attemptReconnect() {
        reconnectAttempts++;
        chatForm.displayMessage("[SYSTEM] Attempting to reconnect (" + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")...");

        try {
            Thread.sleep(RECONNECT_DELAY_MS);

            if (connect(socket.getInetAddress().getHostName(), socket.getPort(), currentUser.getUsername())) {
                reconnectAttempts = 0;
                chatForm.displayMessage("[SYSTEM] Reconnected successfully!");
                chatForm.updateConnectionStatus(true);
                // Re-register with server
                sendMessage("/register " + currentUser.getUsername());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            chatForm.displayMessage("[SYSTEM] Reconnect failed: " + e.getMessage());
            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                attemptReconnect(); // Try again
            }
        }
    }

    // Add a method to stop reconnection attempts
    public void stopReconnectionAttempts() {
        shouldReconnect = false;
    }
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ChatForm getChatForm() {
        return chatForm;
    }
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}