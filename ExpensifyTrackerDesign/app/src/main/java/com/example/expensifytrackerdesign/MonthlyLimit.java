package com.example.expensifytrackerdesign;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a custom spending limit for a specific month and year.
 */
@Entity(tableName = "monthly_limits",
        indices = {@Index(value = {"userId", "month", "year"}, unique = true)})
public class MonthlyLimit {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    /**
     * ID of the User who owns this limit.
     */
    public int userId;
    
    /**
     * Target month (0-11).
     */
    public int month;
    
    /**
     * Target year (e.g., 2024).
     */
    public int year;
    
    /**
     * Spending limit amount in RM.
     */
    public double limit;
}
