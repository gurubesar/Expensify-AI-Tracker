package com.example.expensifytrackerdesign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AddExpenseActivity allows users to record their spending.
 * Users can select a date from the calendar, choose a category, and enter an amount and note.
 */
public class AddExpenseActivity extends AppCompatActivity {
    // Navigation and Tab elements
    private ImageButton btnDashboard, btnAiChat, btnAddExpense, btnSpending, btnScanner, btnFastPick;
    private TextView tabExpense, tabIncome;
    
    // UI components for data input
    private CalendarView calendarView;
    private EditText amountExpense, etNote;
    private Button btnAddExpenseSubmit;
    private Spinner spinnerCategory;
    private ImageButton btnAddCategory;
    private ImageView btnProfile;
    
    // Data fields
    private String selectedCategory = "Health";
    private List<String> categories = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private long selectedDate;
    private AppDatabase db;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addexpense);

        // Database initialization (allow main thread for simplicity in this task)
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();

        // Retrieve the current logged-in user's ID
        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components from XML layout
        calendarView = findViewById(R.id.calendarView);
        amountExpense = findViewById(R.id.AmountTitle);
        etNote = findViewById(R.id.etNote);
        btnAddExpenseSubmit = findViewById(R.id.btnaddexpense);
        btnFastPick = findViewById(R.id.btnFastPick);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnProfile = findViewById(R.id.btnProfile);
        ImageButton backButton = findViewById(R.id.imageButton);

        // Populate the category dropdown
        setupCategorySpinner();

        // Default to current date
        Calendar cal = Calendar.getInstance();
        selectedDate = cal.getTimeInMillis();
        
        // Show DatePickerDialog on click to allow fast month/year jumping
        btnFastPick.setOnClickListener(v -> showDatePicker());
        
        // Also allow clicking individual dates
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = calendar.getTimeInMillis();
        });

        // Setup click listeners for UI actions
        backButton.setOnClickListener(v -> finish());

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(AddExpenseActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Dialog for adding a custom expense category
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

        btnAddExpenseSubmit.setOnClickListener(v -> saveExpense());

        // Check for pre-filled data from ScanActivity
        if (getIntent().getBooleanExtra("FROM_SCAN", false)) {
            String preTitle = getIntent().getStringExtra("PREFILL_TITLE");
            double preAmount = getIntent().getDoubleExtra("PREFILL_AMOUNT", 0.0);
            String preCat = getIntent().getStringExtra("PREFILL_CATEGORY");
            String preDate = getIntent().getStringExtra("PREFILL_DATE");

            if (preAmount > 0) amountExpense.setText(String.valueOf(preAmount));
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
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    long time = sdf.parse(preDate).getTime();
                    calendarView.setDate(time);
                    selectedDate = time;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Initialize and setup Bottom Navigation buttons
        btnDashboard = findViewById(R.id.btnDashboard);
        btnAiChat = findViewById(R.id.btnAiChat);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnSpending = findViewById(R.id.btnSpending);
        btnScanner = findViewById(R.id.btnScanner);

        tabExpense = findViewById(R.id.tabExpense);
        tabIncome = findViewById(R.id.tabIncome);

        // Switch to Income entry tab
        tabIncome.setOnClickListener(v -> {
            Intent intent = new Intent(AddExpenseActivity.this, AddIncomeActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0); // Disable transition for a tab-like feel
        });

        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(AddExpenseActivity.this, dashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnAiChat.setOnClickListener(v -> {
            Intent intent = new Intent(AddExpenseActivity.this, AiChatActivity.class);
            startActivity(intent);
        });

        btnSpending.setOnClickListener(v -> {
            Intent intent = new Intent(AddExpenseActivity.this, SpendingActivity.class);
            startActivity(intent);
            finish();
        });

        btnScanner.setOnClickListener(v -> {
            Intent intent = new Intent(AddExpenseActivity.this, ScanActivity.class);
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
     * Standardizes category names to Title Case.
     */
    private String normalizeCategory(String input) {
        if (input == null || input.trim().isEmpty()) return "Other";
        String trimmed = input.trim();
        if (trimmed.length() == 1) return trimmed.toUpperCase();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    /**
     * Prepares the category spinner with default values and any previously used categories.
     */
    private void setupCategorySpinner() {
        categories.clear();
        categories.add("Health");
        categories.add("Transport");
        categories.add("Electric");
        categories.add("Food");
        categories.add("Entertainment");

        // Load existing categories from the database to include custom ones created earlier
        List<Transaction> all = db.transactionDao().getAll(userId);
        Set<String> existingCats = new HashSet<>();
        for (Transaction t : all) {
            if ("expense".equalsIgnoreCase(t.type) && t.category != null) {
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
     * Validates input and saves the expense transaction to the database.
     */
    private void saveExpense() {
        String amountStr = amountExpense.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            Transaction t = new Transaction();
            t.userId = userId;
            t.title = selectedCategory; 
            t.amount = amount;
            t.type = "expense";
            t.category = selectedCategory;
            t.note = note;
            t.date = selectedDate;

            db.transactionDao().insert(t);
            Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show();
            
            // Clear input fields but remain on the screen for further entries
            amountExpense.setText("");
            etNote.setText("");
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
        }
    }
}
