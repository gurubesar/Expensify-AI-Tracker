package com.example.expensifytrackerdesign;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

/**
 * SignUpActivity allows a new user to create an account by providing a username, email, and password.
 * It ensures the account is unique and stores it in the local Room database.
 */
public class SignUpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);


        Button backbutton,btnSignUp;
        EditText etUsername ,etEmail,etPassword,etConfirm;

        // Initialize UI components
        backbutton = findViewById(R.id.backbutton);
        etUsername = findViewById(R.id.usernameInput);
        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        etConfirm = findViewById(R.id.cpasswordInput);
        btnSignUp = findViewById(R.id.btnSignUp);

        // DATABASE SETUP - Initialize the local Room database
        AppDatabase db = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "app-db"
        ).fallbackToDestructiveMigration().allowMainThreadQueries().build();


        // Handle sign up process: validate inputs and insert into DB
        btnSignUp.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirm = etConfirm.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.equals(confirm)) {
                // Check if username already exists in the database
                User existingUser = db.userDao().getUserByUsername(username);
                if (existingUser != null) {
                    Toast.makeText(this, "User already exists!", Toast.LENGTH_SHORT).show();
                } else {
                    // Create and save the new user entity
                    User user = new User();
                    user.username = username;
                    user.email = email;
                    user.password = password;

                    db.userDao().insert(user);
                    Toast.makeText(this, "User registered!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            }
        });

        // Click listeners to exit the activity and return to previous screen
        findViewById(R.id.backbutton).setOnClickListener(v -> finish());
        findViewById(R.id.imageView).setOnClickListener(v -> finish());
    }
}
