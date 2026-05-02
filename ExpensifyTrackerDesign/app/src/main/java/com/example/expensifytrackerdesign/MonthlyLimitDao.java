package com.example.expensifytrackerdesign;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

/**
 * Data Access Object (DAO) for MonthlyLimit-related database operations.
 */
@Dao
public interface MonthlyLimitDao {
    
    /**
     * Inserts or replaces a custom monthly limit.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MonthlyLimit limit);

    /**
     * Updates an existing monthly limit.
     */
    @Update
    void update(MonthlyLimit limit);

    /**
     * Retrieves the custom limit for a user for a specific month and year.
     */
    @Query("SELECT * FROM monthly_limits WHERE userId = :userId AND month = :month AND year = :year LIMIT 1")
    MonthlyLimit getLimit(int userId, int month, int year);
}
