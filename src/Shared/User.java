package Shared;

import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private String status; // Online, Away, Busy, etc.
    private long lastActive; // timestamp

    public User(String username) {
        this.username = username;
        this.status = "Online";
        this.lastActive = System.currentTimeMillis();
    }
    
    // Getters and setters
   public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getLastActive() {
        return lastActive;
    }

    public void updateLastActive() {
        this.lastActive = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return username + " (" + status + ")";
    }
}