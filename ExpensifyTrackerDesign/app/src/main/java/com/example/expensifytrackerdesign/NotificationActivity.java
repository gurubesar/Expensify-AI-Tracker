package com.example.expensifytrackerdesign;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.room.Room;
import java.util.List;
import java.util.Locale;

/**
 * Activity for managing notification preferences and viewing recent transaction history.
 * <p>
 * This class provides functionality for users to toggle notification settings
 * and displays a summary of recent transactions as simulated notifications.
 */
public class NotificationActivity extends Activity {

    /** Button to return to the previous screen. */
    private ImageButton backButton;

    /** Toggle switch for notification settings. */
    private SwitchCompat switchNotification;

    /** Room Database instance for accessing user settings and transactions. */
    private AppDatabase db;

    /** The current logged-in user's ID. */
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize database and retrieve session information
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        // UI Initialization
        backButton = findViewById(R.id.imageButton);
        switchNotification = findViewById(R.id.switchNotification);

        // Load initial notification preference from the database
        if (userId != -1) {
            User user = db.userDao().getUserById(userId);
            if (user != null) {
                switchNotification.setChecked(user.notification);
            }
        }

        // Set up listeners
        backButton.setOnClickListener(v -> finish());

        // Update notification preference in the database when toggled
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userId != -1) {
                new Thread(() -> {
                    User user = db.userDao().getUserById(userId);
                    if (user != null) {
                        user.notification = isChecked;
                        db.userDao().insert(user);
                        runOnUiThread(() -> Toast.makeText(this, "Notification preference updated", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }
        });
    }
}
