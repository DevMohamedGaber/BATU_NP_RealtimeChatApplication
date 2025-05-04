package Server;

import Shared.User;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class ServerForm extends JFrame {
    private final JTextField portField;
    private final JButton startButton;
    private final JTextArea logArea;
    private final JList<String> clientsList;
    private final DefaultListModel<String> clientsModel;
    private ChatServer server;
    private boolean isRunning = false;

    public ServerForm() {
        setTitle("Chat Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        
        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Control panel (north)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Port:"));
        portField = new JTextField("5555", 10);
        controlPanel.add(portField);
        
        startButton = new JButton("Start Server");
        startButton.addActionListener(this::toggleServer);
        controlPanel.add(startButton);
        
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
        // Log area (center)
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        mainPanel.add(logScroll, BorderLayout.CENTER);
        
        // Clients list (east)
        clientsModel = new DefaultListModel<>();
        clientsList = new JList<>(clientsModel);
        JScrollPane clientsScroll = new JScrollPane(clientsList);
        clientsScroll.setPreferredSize(new Dimension(150, 0));
        mainPanel.add(clientsScroll, BorderLayout.EAST);
        
        add(mainPanel);
    }

    private void toggleServer(ActionEvent e) {
        if (!isRunning) {
            startServer();
            setTitle("Chat Server : server is [ON]");
        } else {
            stopServer();
            setTitle("Chat Server : server is [OFF]");
        }
    }

    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException ex) {
            log("Invalid port number");
            return;
        }
        
        server = new ChatServer(port, this);
        new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                log("Server error: " + e.getMessage());
            }
        }).start();
        isRunning = true;
        startButton.setText("Stop Server");
        portField.setEnabled(false);
        log("Server started on port " + port);
    }

    private void stopServer() {
        if (server != null) {
            try {
                server.stop();
                log("Server stopped");
            } catch (IOException e) {
                log("Error stopping server: " + e.getMessage());
            }
        }
        
        isRunning = false;
        startButton.setText("Start Server");
        portField.setEnabled(true);
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
        });
    }

    public void updateClientList(Map<SocketChannel, User> clients) {
        SwingUtilities.invokeLater(() -> {
            clientsModel.clear();
            for (User user : clients.values()) {
                clientsModel.addElement(user.getUsername() + " (" + user.getStatus() + ")");
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ServerForm().setVisible(true);
        });
    }
}