package com.example.expensifytrackerdesign;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

/**
 * Data Access Object (DAO) for AI chat history database operations.
 */
@Dao
public interface ChatMessageDao {
    
    /**
     * Inserts a single chat message into the history.
     */
    @Insert
    void insert(ChatMessage message);

    /**
     * Retrieves all chat messages, ordered chronologically.
     */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    List<ChatMessage> getAllMessages();

    /**
     * Clears all chat history from the database.
     */
    @Query("DELETE FROM chat_messages")
    void deleteAll();
}
