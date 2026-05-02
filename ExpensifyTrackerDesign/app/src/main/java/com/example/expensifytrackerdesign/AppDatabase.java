package com.example.expensifytrackerdesign;
import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * AppDatabase is the main Room database class for the application.
 * It provides access to the DAOs for Users, Transactions, Monthly Limits, and Chat Messages.
 * Uses destructive migration to handle schema updates during development.
 */
@Database(entities = {User.class, Transaction.class, MonthlyLimit.class, ChatMessage.class}, version = 6)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * @return Data Access Object for User entities.
     */
    public abstract UserDao userDao();

    /**
     * @return Data Access Object for Transaction entities.
     */
    public abstract TransactionDao transactionDao();

    /**
     * @return Data Access Object for MonthlyLimit entities.
     */
    public abstract MonthlyLimitDao monthlyLimitDao();

    /**
     * @return Data Access Object for ChatMessage entities.
     */
    public abstract ChatMessageDao chatMessageDao();
}