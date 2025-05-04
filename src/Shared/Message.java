package Shared;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private final String sender;
    private final String content;
    private final LocalDateTime timestamp;
    
    public Message(String sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and toString()
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", 
            timestamp.toString(), sender, content);
    }
}