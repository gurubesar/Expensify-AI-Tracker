package com.example.expensifytrackerdesign;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * RecoverpassActivity provides a screen for users to initiate password recovery.
 * This is currently a placeholder screen where the user can return to the previous screen.
 */
public class RecoverpassActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recoverpass);

        // Simple navigation listeners to return to the profile screen
        findViewById(R.id.imageButton).setOnClickListener(v -> finish());
        findViewById(R.id.btnDone).setOnClickListener(v -> finish());
    }
}
