package com.example.expensifytrackerdesign;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for viewing a complete breakdown of all spending and income categories.
 * <p>
 * This class aggregates transactions by their category for a selected month
 * and displays them in a summary list, showing the total amount spent or earned per category.
 */
public class AllCategoriesActivity extends AppCompatActivity {

    /** Container for the category summary items. */
    private LinearLayout llAllCategories;

    /** Dropdown menu for selecting the month to filter categories. */
    private com.google.android.material.textfield.MaterialAutoCompleteTextView spinnerMonthAll;

    /** Room Database instance. */
    private AppDatabase db;

    /** Current logged-in user ID. */
    private int userId;

    /** The month selected by the user for filtering. */
    private int selectedMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_categories);

        // UI Initialization
        llAllCategories = findViewById(R.id.llAllCategories);
        spinnerMonthAll = findViewById(R.id.spinnerMonthAll);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Database and session initialization
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();

        userId = getSharedPreferences("app", MODE_PRIVATE).getInt("userId", -1);

        // Initial setup
        setupMonthSpinner();
        loadAllCategories();
    }

    /**
     * Initializes the month selection dropdown with month names and sets the default selection.
     */
    private void setupMonthSpinner() {
        String[] months = {"January", "February", "March", "April", "May", "June", 
                          "July", "August", "September", "October", "November", "December"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, months);
        spinnerMonthAll.setAdapter(adapter);

        // Get month passed from dashboard or fallback to current month
        int dashboardMonth = getIntent().getIntExtra("selectedMonth", -1);
        if (dashboardMonth != -1) {
            selectedMonth = dashboardMonth;
        } else {
            selectedMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH);
        }
        spinnerMonthAll.setText(months[selectedMonth], false);

        // Update categories when a new month is selected
        spinnerMonthAll.setOnItemClickListener((parent, view, position, id) -> {
            selectedMonth = position;
            loadAllCategories();
        });
    }

    /**
     * Normalizes a category string to ensure consistent grouping.
     * <p>
     * Converts the category to title case (e.g., "food" -> "Food").
     * @param input The category name to normalize.
     * @return The normalized category string.
     */
    private String normalizeCategory(String input) {
        if (input == null || input.trim().isEmpty()) return "Other";
        String trimmed = input.trim();
        if (trimmed.length() == 1) return trimmed.toUpperCase();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    /**
     * Retrieves all transactions for the user and aggregates them by category.
     * <p>
     * Filters transactions based on the selected month, calculates totals for each
     * normalized category, and dynamically populates the UI container.
     */
    private void loadAllCategories() {
        if (userId == -1) return;

        llAllCategories.removeAllViews();
        List<Transaction> allTransactions = db.transactionDao().getAll(userId);
        List<Transaction> filteredTransactions = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        // Filter transactions for the selected month and current year
        for (Transaction t : allTransactions) {
            cal.setTimeInMillis(t.date);
            if (cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == currentYear) {
                filteredTransactions.add(t);
            }
        }

        // Map categories to total amounts and types
        Map<String, Double> categoryTotals = new HashMap<>();
        Map<String, String> categoryTypes = new HashMap<>();

        for (Transaction t : filteredTransactions) {
            String cat = normalizeCategory(t.category);
            categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + t.amount);
            categoryTypes.put(cat, t.type);
        }

        // Sort categories by amount descending
        List<Map.Entry<String, Double>> list = new ArrayList<>(categoryTotals.entrySet());
        Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        // Inflate and add summary views to the layout
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, Double> entry : list) {
            View view = inflater.inflate(R.layout.item_category_summary, llAllCategories, false);
            TextView tvName = view.findViewById(R.id.tvCategoryName);
            TextView tvType = view.findViewById(R.id.tvCategoryType);
            TextView tvAmount = view.findViewById(R.id.tvCategoryAmount);

            tvName.setText(entry.getKey());
            String type = categoryTypes.get(entry.getKey());
            tvType.setText(type != null ? type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase() : "");
            tvAmount.setText(String.format(Locale.US, "RM%.2f", entry.getValue()));

            llAllCategories.addView(view);
        }
    }
}
