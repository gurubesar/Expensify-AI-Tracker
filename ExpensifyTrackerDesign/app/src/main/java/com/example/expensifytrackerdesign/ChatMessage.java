package com.example.expensifytrackerdesign;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a single message in the AI Chat.
 */
@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    /**
     * Text content of the message.
     */
    private String message;
    
    /**
     * True if the message was sent by the user; False if by the AI Advisor.
     */
    private boolean isUser;
    
    /**
     * Timestamp of the message in milliseconds.
     */
    private long timestamp;

    public ChatMessage(String message, boolean isUser, long timestamp) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
