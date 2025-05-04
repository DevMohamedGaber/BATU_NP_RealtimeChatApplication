package Server;

import Shared.User;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class ChatServer {
    private final int port;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private final Map<SocketChannel, User> clients = new HashMap<>();
    private final Map<SocketChannel, StringBuilder> clientBuffers = new HashMap<>();
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final ServerForm serverForm;
    private boolean running = true;

    public ChatServer(int port, ServerForm serverForm) {
        this.port = port;
        this.serverForm = serverForm;
    }

    public void start() throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        serverForm.log("Server started on port " + port + ". Waiting for connections...");

        while (running) {
            try {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    
                    if (!key.isValid()) continue;
                    
                    if (key.isAcceptable()) {
                        acceptClient(key);
                    } else if (key.isReadable()) {
                        readMessage(key);
                    }
                }
            } catch (IOException e) {
                serverForm.log("Server error: " + e.getMessage());
            }
        }
    }

    public void stop() throws IOException {
        running = false;
        broadcastSystemMessage("[SERVER] Server is shutting down");
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        selector.wakeup();
        for (SocketChannel channel : clients.keySet()) {
            try {
                if (channel.isOpen()) {
                    channel.close();
                }
            } catch (IOException e) {
                serverForm.log("Error closing client channel: " + e.getMessage());
            }
        }
        serverSocketChannel.close();
        selector.close();
    }

    private void acceptClient(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        User guestUser = new User("Guest" + clientChannel.hashCode());
        clients.put(clientChannel, guestUser);
        serverForm.log("New client connected: " + guestUser);
        updateUserList(); // Send updated list to all clients
    }

    private void readMessage(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        User user = clients.get(clientChannel);
        user.updateLastActive();

        // Get or create buffer for this client
        StringBuilder messageBuffer = clientBuffers.computeIfAbsent(clientChannel, k -> new StringBuilder());

        buffer.clear();
        try {
            int bytesRead = clientChannel.read(buffer);
            if (bytesRead == -1) {
                disconnectClient(clientChannel);
                return;
            }

            buffer.flip();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            messageBuffer.append(new String(bytes));

            // Process complete messages
            int newlineIndex;
            while ((newlineIndex = messageBuffer.indexOf("\n")) != -1) {
                String completeMessage = messageBuffer.substring(0, newlineIndex).trim();
                messageBuffer.delete(0, newlineIndex + 1);

                if (!completeMessage.isEmpty()) {
                    processClientMessage(clientChannel, user, completeMessage);
                }
            }
        } catch (IOException e) {
            disconnectClient(clientChannel);
        } finally {
            // Update or remove the buffer
            if (messageBuffer.length() == 0) {
                clientBuffers.remove(clientChannel);
            }
        }
    }
    private void processClientMessage(SocketChannel channel, User user, String message) throws IOException {
        if (message.startsWith("/register ")) {
            handleRegistration(channel, user, message);
        } else if (message.startsWith("/status ")) {
            handleStatusChange(user, message);
        } else {
            broadcastMessage(user, message);
        }
    }
    private void handleRegistration(SocketChannel channel, User user, String message) throws IOException {
        String username = message.substring(10).trim();
        if (username.isEmpty()) {
            channel.write(ByteBuffer.wrap("Invalid username\n".getBytes()));
            return;
        }

        boolean usernameTaken = clients.values().stream()
            .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));

        if (usernameTaken) {
            channel.write(ByteBuffer.wrap("Username already taken\n".getBytes()));
        } else {
            user.setUsername(username);
            broadcastSystemMessage(username + " has joined the chat");
            updateUserList();
            serverForm.updateClientList(clients);
        }
    }
    private void handleStatusChange(User user, String message) throws IOException {
        String newStatus = message.substring(8).trim();
        user.setStatus(newStatus);
        broadcastSystemMessage(user.getUsername() + " is now " + newStatus);
        updateUserList();
        serverForm.updateClientList(clients);
    }
    private void broadcastMessage(User sender, String message) throws IOException {
        String formattedMessage = sender.getUsername() + ": " + message + "\n";
        serverForm.log("Broadcasting: " + formattedMessage.trim());

        for (SocketChannel channel : clients.keySet()) {
            if (channel.isConnected()) {
                channel.write(ByteBuffer.wrap(formattedMessage.getBytes()));
            }
        }
    }

    private void broadcastSystemMessage(String message) throws IOException {
        String formattedMessage = "[System] " + message + "\n";
        serverForm.log(formattedMessage.trim());

        for (SocketChannel channel : clients.keySet()) {
            if (channel.isConnected()) {
                channel.write(ByteBuffer.wrap(formattedMessage.getBytes()));
            }
        }
    }

    private void disconnectClient(SocketChannel clientChannel) throws IOException {
        User user = clients.get(clientChannel);
        clients.remove(clientChannel);
        try {
            clientChannel.close();
        } catch (IOException e) {
            serverForm.log("Error closing client channel: " + e.getMessage());
        }
        broadcastSystemMessage(user.getUsername() + " has left the chat");
        updateUserList();
        serverForm.updateClientList(clients); // Explicitly update server form
        serverForm.log("Client disconnected: " + user);
    }
    private void updateUserList() throws IOException {
        StringBuilder userList = new StringBuilder("/userlist");
        for (User user : clients.values()) {
            userList.append(",").append(user.getUsername())
                   .append(":").append(user.getStatus());
        }
        // Send to ALL clients
        for (SocketChannel channel : clients.keySet()) {
            if (channel.isConnected()) {
                channel.write(ByteBuffer.wrap((userList.toString() + "\n").getBytes()));
            }
        }
    }
}