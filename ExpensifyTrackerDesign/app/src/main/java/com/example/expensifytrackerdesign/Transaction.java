package com.example.expensifytrackerdesign;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a financial Transaction (either Income or Expense).
 */
@Entity
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * ID of the User who owns this transaction.
     */
    public int userId;

    public String title;
    public double amount;
    
    /**
     * Type of transaction: "Income" or "Expense".
     */
    public String type; 
    
    public String category;
    public String note;
    
    /**
     * Timestamp of the transaction in milliseconds.
     */
    public long date;
}