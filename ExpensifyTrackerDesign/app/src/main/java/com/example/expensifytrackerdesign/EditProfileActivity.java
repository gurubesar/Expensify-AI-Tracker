package com.example.expensifytrackerdesign;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.room.Room;

/**
 * EditProfileActivity provides a form for users to update their personal information.
 * It allows editing the username, email, phone number, gender, country, and monthly spending limit.
 * Changes are persisted back to the Room database.
 */
public class EditProfileActivity extends Activity {

    private EditText etUsername, etEmail, etPhone, etSpendingLimit;
    private AutoCompleteTextView genderDropdown, countryDropdown;
    private Button btnSave;
    private ImageButton backButton;
    private AppDatabase db;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editprofile);

        // Database and session initialization
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        // Bind UI components
        etUsername = findViewById(R.id.usertxt);
        etEmail = findViewById(R.id.emailtxt);
        etPhone = findViewById(R.id.editPhone);
        etSpendingLimit = findViewById(R.id.editSpendingLimit);
        genderDropdown = findViewById(R.id.genderDropdown);
        countryDropdown = findViewById(R.id.countryDropdown);
        btnSave = findViewById(R.id.btnSave);
        backButton = findViewById(R.id.imageButton);

        // Populate form with existing user data if available
        if (userId != -1) {
            User user = db.userDao().getUserById(userId);
            if (user != null) {
                etUsername.setText(user.username);
                etEmail.setText(user.email);
                etPhone.setText(user.phoneNumber);
                etSpendingLimit.setText(String.valueOf(user.monthlySpendingLimit));
                genderDropdown.setText(user.gender);
                countryDropdown.setText(user.countryRegion);
            }
        }

        // Return to profile screen without saving
        backButton.setOnClickListener(v -> finish());

        // Validate and save updated profile data to database
        btnSave.setOnClickListener(v -> {
            if (userId != -1) {
                User user = db.userDao().getUserById(userId);
                if (user != null) {
                    user.username = etUsername.getText().toString();
                    user.email = etEmail.getText().toString();
                    user.phoneNumber = etPhone.getText().toString();
                    user.gender = genderDropdown.getText().toString();
                    user.countryRegion = countryDropdown.getText().toString();
                    
                    try {
                        user.monthlySpendingLimit = Double.parseDouble(etSpendingLimit.getText().toString());
                    } catch (NumberFormatException e) {
                        user.monthlySpendingLimit = 0;
                    }

                    db.userDao().insert(user);
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }
}
