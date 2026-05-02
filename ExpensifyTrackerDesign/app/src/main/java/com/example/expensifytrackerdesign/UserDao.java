package com.example.expensifytrackerdesign;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

/**
 * Data Access Object (DAO) for User-related database operations.
 */
@Dao
public interface UserDao {

    /**
     * Inserts or updates a User in the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    /**
     * Updates User information in the database.
     */
    @Update
    void update(User user);

    /**
     * Authenticates a user based on username and password.
     */
    @Query("SELECT * FROM User WHERE username = :u AND password = :p")
    User login(String u, String p);

    /**
     * Retrieves a User by their unique username.
     */
    @Query("SELECT * FROM User WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);

    /**
     * Retrieves a User by their unique numeric ID.
     */
    @Query("SELECT * FROM User WHERE id = :id LIMIT 1")
    User getUserById(int id);
}