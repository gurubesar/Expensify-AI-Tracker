package com.example.expensifytrackerdesign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

/**
 * AddIncomeActivity allows the user to record new income transactions.
 * It features a calendar for date selection, a category spinner with an 'add new' option,
 * and handles data persistence via the Room database.
 */
public class AddIncomeActivity extends AppCompatActivity {
    private ImageButton btnDashboard, btnAiChat, btnAddExpense, btnSpending, btnScanner, btnFastPick;
    private TextView tabExpense, tabIncome;
    private CalendarView calendarView;
    private EditText amountIncome, etNote;
    private Button btnAddIncomeSubmit;
    private Spinner spinnerCategory;
    private ImageButton btnAddCategory;
    private ImageView btnProfile;
    private String selectedCategory = "Salary";
    private List<String> categories = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private long selectedDate;
    private AppDatabase db;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addincome);

        // Initialize database and user session
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind UI elements from XML
        calendarView = findViewById(R.id.calendarView);
        amountIncome = findViewById(R.id.AmountIncomeTitle);
        etNote = findViewById(R.id.etNote);
        btnAddIncomeSubmit = findViewById(R.id.btnaddincome);
        btnFastPick = findViewById(R.id.btnFastPick);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnProfile = findViewById(R.id.btnProfile);
        ImageButton backButton = findViewById(R.id.imageButton);

        // Setup the category dropdown with defaults and user-added values
        setupCategorySpinner();

        // Default to current date
        Calendar cal = Calendar.getInstance();
        selectedDate = cal.getTimeInMillis();

        // Show DatePickerDialog on click to allow fast month/year jumping
        btnFastPick.setOnClickListener(v -> showDatePicker());

        // Capture date changes from the CalendarView
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = calendar.getTimeInMillis();
        });

        // Navigation and profile button listeners
        backButton.setOnClickListener(v -> finish());

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(AddIncomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // "Add Category" dialog to allow dynamic expansion of income categories
        btnAddCategory.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add New Category");
            final EditText input = new EditText(this);
            input.setHint("Category Name");
            builder.setView(input);
            builder.setPositiveButton("Add", (dialog, which) -> {
                String newCat = input.getText().toString().trim();
                if (!newCat.isEmpty()) {
                    newCat = normalizeCategory(newCat);
                    if (!categories.contains(newCat)) {
                        categories.add(newCat);
                        categoryAdapter.notifyDataSetChanged();
                    }
                    spinnerCategory.setSelection(categories.indexOf(newCat));
                    selectedCategory = newCat;
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        // Submit the income entry to the database
        btnAddIncomeSubmit.setOnClickListener(v -> saveIncome());

        // Check for pre-filled data from ScanActivity
        if (getIntent().getBooleanExtra("FROM_SCAN", false)) {
            String preTitle = getIntent().getStringExtra("PREFILL_TITLE");
            double preAmount = getIntent().getDoubleExtra("PREFILL_AMOUNT", 0.0);
            String preCat = getIntent().getStringExtra("PREFILL_CATEGORY");
            String preDate = getIntent().getStringExtra("PREFILL_DATE");

            if (preAmount > 0) amountIncome.setText(String.valueOf(preAmount));
            if (preTitle != null && !preTitle.isEmpty()) etNote.setText(preTitle);

            if (preCat != null && !preCat.isEmpty()) {
                String normalized = normalizeCategory(preCat);
                if (!categories.contains(normalized)) {
                    categories.add(normalized);
                    categoryAdapter.notifyDataSetChanged();
                }
                spinnerCategory.setSelection(categories.indexOf(normalized));
            }

            if (preDate != null && !preDate.isEmpty()) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    long time = sdf.parse(preDate).getTime();
                    calendarView.setDate(time);
                    selectedDate = time;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Setup bottom navigation bar
        btnDashboard = findViewById(R.id.btnDashboard);
        btnAiChat = findViewById(R.id.btnAiChat);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnSpending = findViewById(R.id.btnSpending);
        btnScanner = findViewById(R.id.btnScanner);

        tabExpense = findViewById(R.id.tabExpense);
        tabIncome = findViewById(R.id.tabIncome);

        // Switch between Add Expense and Add Income views
        tabExpense.setOnClickListener(v -> {
            Intent intent = new Intent(AddIncomeActivity.this, AddExpenseActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0); // Remove transition animation for a seamless tab switch
        });

        // Standard bottom navigation actions
        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(AddIncomeActivity.this, dashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnAiChat.setOnClickListener(v -> {
            Intent intent = new Intent(AddIncomeActivity.this, AiChatActivity.class);
            startActivity(intent);
        });

        btnSpending.setOnClickListener(v -> {
            Intent intent = new Intent(AddIncomeActivity.this, SpendingActivity.class);
            startActivity(intent);
            finish();
        });

        btnScanner.setOnClickListener(v -> {
            Intent intent = new Intent(AddIncomeActivity.this, ScanActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDate);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year1, month1, dayOfMonth);
                    selectedDate = calendar.getTimeInMillis();
                    calendarView.setDate(selectedDate, true, true);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    /**
     * Standardizes category string formatting (e.g., "Investment").
     */
    private String normalizeCategory(String input) {
        if (input == null || input.trim().isEmpty()) return "Other";
        String trimmed = input.trim();
        if (trimmed.length() == 1) return trimmed.toUpperCase();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    /**
     * Loads default categories and any unique categories used in previous income transactions.
     */
    private void setupCategorySpinner() {
        categories.clear();
        categories.add("Salary");
        categories.add("Business");
        categories.add("Gift");
        categories.add("Investment");

        // Load existing categories from DB to prevent duplicates
        List<Transaction> all = db.transactionDao().getAll(userId);
        Set<String> existingCats = new HashSet<>();
        for (Transaction t : all) {
            if ("income".equalsIgnoreCase(t.type) && t.category != null) {
                existingCats.add(normalizeCategory(t.category));
            }
        }
        for (String c : existingCats) {
            if (!categories.contains(c)) categories.add(c);
        }

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = categories.get(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Validates and saves the income transaction into the Room database.
     */
    private void saveIncome() {
        String amountStr = amountIncome.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            Transaction t = new Transaction();
            t.userId = userId;
            t.title = selectedCategory; // Using category as title for simplicity
            t.amount = amount;
            t.type = "income";
            t.category = selectedCategory;
            t.note = note;
            t.date = selectedDate;

            db.transactionDao().insert(t);
            Toast.makeText(this, "Income added", Toast.LENGTH_SHORT).show();
            
            // Clear inputs for potential next entry
            amountIncome.setText("");
            etNote.setText("");
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
        }
    }
}
