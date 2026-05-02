package com.example.expensifytrackerdesign;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.room.Room;

/**
 * WelcomeActivity serves as the login screen for the application.
 * It verifies user credentials against the local database and manages session creation.
 */
public class WelcomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnSignIn;
        EditText etUsername, etPassword;
        android.widget.ImageView backbutton;
        android.widget.CheckBox chkRemember;

        // Initialize UI components from XML
        backbutton = findViewById(R.id.imageView13);
        btnSignIn = findViewById(R.id.btnLogOut); // Note: Uses btnLogOut ID for the Sign In button from layout
        etUsername = findViewById(R.id.usernameInput);
        etPassword = findViewById(R.id.passwordInput);
        chkRemember = findViewById(R.id.chkRemember);

        // DATABASE SETUP - Initialize Room database with main thread queries for login
        AppDatabase db = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "app-db"
        ).fallbackToDestructiveMigration().allowMainThreadQueries().build();

        // Handle navigation back to the previous screen
        backbutton.setOnClickListener(v -> onBackPressed());

        // Process sign in: validate inputs, check DB, and manage session
        btnSignIn.setOnClickListener(v -> {
            String usernameInput = etUsername.getText().toString().trim();
            String passwordInput = etPassword.getText().toString().trim();

            if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify credentials against stored User entities
            User user = db.userDao().login(usernameInput, passwordInput);

            if (user != null) {
                // Persistent session management using SharedPreferences
                SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("userId", user.id);
                editor.putBoolean("rememberMe", chkRemember.isChecked());
                editor.apply();

                // Successful login leads to the dashboard
                Intent intent = new Intent(WelcomeActivity.this, dashboardActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
