package com.example.expensifytrackerdesign;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a User in the system.
 */
@Entity
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String username;
    public String email;
    public String password;

    /**
     * Default monthly spending limit for the user.
     */
    public double monthlySpendingLimit;
    public String gender;
    public String phoneNumber;
    public String countryRegion;
    public boolean notification;
}