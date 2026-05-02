package com.example.expensifytrackerdesign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.room.Room;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity serves as the initial entry point of the application.
 * It manages automatic login via 'Remember Me' session checks and provides
 * navigation to the core onboarding flows (Sign In and Sign Up).
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Session Management: Check if a user has previously opted to stay logged in.
        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean("rememberMe", false);
        int userId = prefs.getInt("userId", -1);

        // If a valid session exists, skip the landing screen and go straight to the dashboard.
        if (rememberMe && userId != -1) {
            Intent intent = new Intent(MainActivity.this, dashboardActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Initialize UI components for the landing screen
        Button buttonSignIn, buttonSignUp;

        buttonSignIn = findViewById(R.id.buttonSignIn);
        buttonSignUp = findViewById(R.id.buttonSignUp);

        // Navigate to the Login flow
        buttonSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
        });

        // Navigate to the Account Creation flow
        buttonSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

    }
}