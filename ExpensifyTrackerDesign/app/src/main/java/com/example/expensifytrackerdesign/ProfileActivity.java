package com.example.expensifytrackerdesign;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.card.MaterialCardView;
import androidx.room.Room;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ProfileActivity displays user account information and provides access to settings.
 * It allows the user to navigate to edit their profile, manage notifications, and change/recover passwords.
 * It also handles the logout process.
 */
public class ProfileActivity extends Activity {

    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Room DB
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .fallbackToDestructiveMigration()
                .build();

        // Retrieve current user ID from preferences
        int userId = getSharedPreferences("app", MODE_PRIVATE).getInt("userId", -1);

        // Display user's name fetched from the database
        TextView tvUserName = findViewById(R.id.tvUserName);
        if (userId != -1) {
            executor.execute(() -> {
                User user = db.userDao().getUserById(userId);
                if (user != null) {
                    runOnUiThread(() -> tvUserName.setText(user.username));
                }
            });
        }

        // Back button navigation
        android.widget.ImageView backbutton;
        backbutton = findViewById(R.id.imageView14);
        backbutton.setOnClickListener(v -> onBackPressed());

        // Navigation card options
        MaterialCardView cardEditProfile = findViewById(R.id.cardEditProfile);
        MaterialCardView cardNotification = findViewById(R.id.cardNotification);
        MaterialCardView cardRecoverPassword = findViewById(R.id.cardRecoverPassword);
        MaterialCardView cardChangePassword = findViewById(R.id.cardChangePassword);
        Button btnLogOut = findViewById(R.id.btnLogOut);

        // Intent logic for different profile sub-screens
        cardEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        cardNotification.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        cardRecoverPassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, RecoverpassActivity.class);
            startActivity(intent);
        });

        cardChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // AI Toggle Setup
        SwitchMaterial switchAi = findViewById(R.id.switchAi);
        boolean isAiEnabled = getSharedPreferences("app", MODE_PRIVATE).getBoolean("ai_enabled", true);
        switchAi.setChecked(isAiEnabled);

        switchAi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences("app", MODE_PRIVATE).edit().putBoolean("ai_enabled", isChecked).apply();
        });

        // Logout: clear preferences and return to Main (landing) activity
        btnLogOut.setOnClickListener(v -> {
            getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
