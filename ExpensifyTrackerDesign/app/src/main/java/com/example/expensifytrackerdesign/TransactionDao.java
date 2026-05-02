package com.example.expensifytrackerdesign;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) for Transaction-related database operations.
 */
@Dao
public interface TransactionDao {

    /**
     * Inserts a new financial transaction into the database.
     */
    @Insert
    void insert(Transaction t);

    /**
     * Retrieves all transactions associated with a specific user.
     */
    @Query("SELECT * FROM `Transaction` WHERE userId = :userId")
    List<Transaction> getAll(int userId);

    /**
     * Retrieves the most recent transactions for a specific user, sorted by date.
     */
    @Query("SELECT * FROM `Transaction` WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    List<Transaction> getRecentTransactions(int userId, int limit);

    /**
     * Deletes a specific transaction from the database.
     */
    @androidx.room.Delete
    void delete(Transaction t);
}