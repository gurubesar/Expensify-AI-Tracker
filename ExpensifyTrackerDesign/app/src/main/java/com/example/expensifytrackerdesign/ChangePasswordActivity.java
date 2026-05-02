package com.example.expensifytrackerdesign;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.room.Room;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for users to change their account password.
 * <p>
 * This class handles the logic for verifying the user's current password
 * and updating it in the local database after ensuring the new password
 * and confirmation match.
 */
public class ChangePasswordActivity extends Activity {

    /** Room Database instance for accessing user data. */
    private AppDatabase db;

    /** Executor service for background database operations. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** The current logged-in user's ID. */
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Database and session initialization
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .fallbackToDestructiveMigration()
                .build();

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        // Ensure user is properly logged in
        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // UI Initialization
        ImageButton btnBack = findViewById(R.id.btnBack);
        EditText etOldPassword = findViewById(R.id.etOldPassword);
        EditText etNewPassword = findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnSavePassword = findViewById(R.id.btnSavePassword);

        // Set up click listeners
        btnBack.setOnClickListener(v -> finish());

        // Update password logic
        btnSavePassword.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            // Validate input fields
            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify new password confirmation
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Process update in background thread
            executor.execute(() -> {
                User user = db.userDao().getUserById(userId);
                if (user != null) {
                    // Check if current password is correct
                    if (user.password.equals(oldPass)) {
                        user.password = newPass;
                        db.userDao().update(user);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Incorrect old password", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });
    }
}