package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import Shared.User;

public class ChatForm extends JFrame {
    private final JTextArea chatArea;
    private final JTextField messageField;
    private JLabel connectionStatus;
    private final JList<String> contactsList;
    private final DefaultListModel<String> contactsModel;
    private ChatClient chatClient;

    public ChatForm() {
        setTitle("Chat Application - User Name Here");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        
        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Chat area (center)
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        mainPanel.add(chatScroll, BorderLayout.CENTER);
        
        // Contacts list (east)
        contactsModel = new DefaultListModel<>();
        contactsList = new JList<>(contactsModel);
        JScrollPane contactsScroll = new JScrollPane(contactsList);
        contactsScroll.setPreferredSize(new Dimension(150, 0));
        mainPanel.add(contactsScroll, BorderLayout.EAST);
        
        // Message input (south)
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.addActionListener(this::sendMessage);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(this::sendMessage);
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // Status menu
        JMenuBar menuBar = new JMenuBar();
        
        JMenu statusMenu = new JMenu("Status");
        JMenuItem onlineItem = new JMenuItem("Online");
        JMenuItem awayItem = new JMenuItem("Away");
        JMenuItem busyItem = new JMenuItem("Busy");
        onlineItem.addActionListener(e -> chatClient.setStatus("Online"));
        awayItem.addActionListener(e -> chatClient.setStatus("Away"));
        busyItem.addActionListener(e -> chatClient.setStatus("Busy"));
        statusMenu.add(onlineItem);
        statusMenu.add(awayItem);
        statusMenu.add(busyItem);
        menuBar.add(statusMenu);
        
        connectionStatus = new JLabel("Connected", JLabel.RIGHT);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(connectionStatus);
        JMenu connectionMenu = new JMenu("Connection");
        JMenuItem reconnectItem = new JMenuItem("Reconnect");
        reconnectItem.addActionListener(e -> {
            if (chatClient != null && !chatClient.isConnected()) {
                chatClient.attemptReconnect();
            }
        });
        connectionMenu.add(reconnectItem);
        menuBar.add(connectionMenu);
        
        setJMenuBar(menuBar);
        
        add(mainPanel);
    }

    private void sendMessage(ActionEvent e) {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            chatClient.sendMessage(message);
            messageField.setText("");
        }
    }

    public void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
        });
    }

    public void updateUserList(List<User> users) {
        SwingUtilities.invokeLater(() -> {
            contactsModel.clear();
            for (User user : users) {
                contactsModel.addElement(user.toString());
            }
        });
    }

    public void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        setTitle("Chat Application - " + chatClient.getCurrentUser().getUsername());
    }
    public void updateConnectionStatus(boolean connected) {
    SwingUtilities.invokeLater(() -> {
        if (connected) {
            connectionStatus.setText("Connected");
            connectionStatus.setForeground(Color.GREEN);
        } else {
            connectionStatus.setText("Disconnected");
            connectionStatus.setForeground(Color.RED);
        }
    });
}
}