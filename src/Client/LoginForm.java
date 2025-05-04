package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginForm extends JFrame {
    private JTextField serverField;
    private JTextField portField;
    private JTextField usernameField;
    private ChatClient chatClient;

    public LoginForm() {
        setTitle("Chat Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        panel.add(new JLabel("Server:"));
        serverField = new JTextField("localhost");
        panel.add(serverField);
        
        panel.add(new JLabel("Port:"));
        portField = new JTextField("5555");
        panel.add(portField);
        
        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);
        
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(this::performLogin);
        panel.add(new JLabel()); // Empty cell for layout
        panel.add(loginButton);
        
        add(panel);
    }

    private void performLogin(ActionEvent e) {
        String server = serverField.getText();
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid port number", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String username = usernameField.getText();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        chatClient = new ChatClient(new ChatForm());
        if (chatClient.connect(server, port, username)) {
            dispose(); // Close login form
            chatClient.getChatForm().setVisible(true); // Show chat form
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}