package com.example.expensifytrackerdesign;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Detailed spending activity that provides a deeper breakdown of transactions and monthly limits.
 * <p>
 * This activity allows users to select specific months, view grouped transactions, 
 * and manage individual transactions (such as voiding/deleting them).
 * It also synchronizes with the main dashboard and other core features via bottom navigation.
 */
public class Spending2Activity extends AppCompatActivity {
    
    /** Navigation buttons for the bottom navigation bar. */
    private ImageButton btnDashboard, btnAiChat, btnAddExpense, btnSpending, btnScanner;
    
    /** RecyclerView for displaying the list of transactions. */
    private RecyclerView rvExpenses;
    
    /** UI elements for displaying financial summaries. */
    private TextView tvSpentAmountInside, tvIncomeAmountInside, tvLimitAmountInside, tvLimitStatus, tvTransactionMonth;
    
    /** Circular progress indicator showing spending relative to the monthly limit. */
    private CircularProgressIndicator spendingProgress;
    
    /** Room database instance. */
    private AppDatabase db;
    
    /** Current user ID from shared preferences. */
    private int userId;
    
    /** Currently selected month and year for filtering data. */
    private int selectedMonth, selectedYear;
    
    /** List of transactions for the selected period. */
    private List<Transaction> expenseList = new ArrayList<>();
    
    /** Adapter for the transaction RecyclerView. */
    private TransactionGroupAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spending2);

        // Database initialization
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();

        // Session management
        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        // RecyclerView Setup
        rvExpenses = findViewById(R.id.rvExpenses);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionGroupAdapter();
        rvExpenses.setAdapter(adapter);

        // Swipe-to-refresh implementation
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadSpendingData();
            swipeRefreshLayout.setRefreshing(false);
        });

        // Long-click listener for transaction deletion (voiding)
        adapter.setOnTransactionLongClickListener(t -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Void Transaction")
                    .setMessage("Are you sure you want to delete this transaction?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        db.transactionDao().delete(t);
                        loadSpendingData();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        ImageButton backButton = findViewById(R.id.imageButton);
        backButton.setOnClickListener(v -> finish());
        
        // Bottom Navigation Setup
        btnDashboard = findViewById(R.id.btnDashboard);
        btnAiChat = findViewById(R.id.btnAiChat);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnSpending = findViewById(R.id.btnSpending);
        btnScanner = findViewById(R.id.btnScanner);

        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(Spending2Activity.this, dashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnAiChat.setOnClickListener(v -> {
            Intent intent = new Intent(Spending2Activity.this, AiChatActivity.class);
            startActivity(intent);
        });

        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(Spending2Activity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        btnSpending.setOnClickListener(v -> {
            // Already in spending view
        });

        btnScanner.setOnClickListener(v -> {
            Intent intent = new Intent(Spending2Activity.this, ScanActivity.class);
            startActivity(intent);
        });

        // Initial data load
        loadSpendingData();
    }



    /**
     * Fetches and calculates spending data for the selected month.
     * <p>
     * This method retrieves all transactions for the current user, filters them by
     * the selected month and year, and updates the summary statistics and transaction list.
     */
    private void loadSpendingData() {
        if (userId == -1) return;

        List<Transaction> all = db.transactionDao().getAll(userId);
        expenseList.clear();
        expenseList.addAll(all); // Add all transactions to the list for the RecyclerView
        
        // Sort by date descending for chronological display
        expenseList.sort((o1, o2) -> Long.compare(o2.date, o1.date));
        adapter.setTransactions(expenseList);
    }


}
