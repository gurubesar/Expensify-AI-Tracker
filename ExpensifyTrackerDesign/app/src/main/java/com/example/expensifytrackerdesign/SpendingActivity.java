package com.example.expensifytrackerdesign;

import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * SpendingActivity provides a detailed view of the user's spending and income for a selected month.
 * It features a circular progress indicator for the monthly limit and a grouped transaction list.
 */
public class SpendingActivity extends AppCompatActivity {

    private TextView tvSpentAmountInside, tvIncomeAmountInside, tvLimitAmountInside, tvLimitStatus, tvTransactionMonth;
    private CircularProgressIndicator spendingProgress;
    private View circleContainer;
    private ImageButton btnDashboard, btnAiChat, btnAddExpense, btnSpending, btnScanner;
    private Button btnSeeSpending;
    private AppDatabase db;
    private int userId;
    private int selectedMonth, selectedYear;

    private RecyclerView rvMonthlyTransactions;
    private TransactionGroupAdapter adapter;
    private List<Transaction> monthlyTransactions = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spending);

        // Initialize Room DB
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();

        // Get current user session
        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        // UI Initialization
        tvSpentAmountInside = findViewById(R.id.tvSpentAmountInside);
        tvIncomeAmountInside = findViewById(R.id.tvIncomeAmountInside);
        tvLimitAmountInside = findViewById(R.id.tvLimitAmountInside);
        tvTransactionMonth = findViewById(R.id.tvTransactionMonth);
        tvLimitStatus = findViewById(R.id.textView20);
        spendingProgress = findViewById(R.id.spendingProgress);
        circleContainer = findViewById(R.id.circleContainer);
        btnSeeSpending = findViewById(R.id.btnSeeSpending);
        ImageButton backButton = findViewById(R.id.imageButton);

        // Transactions RecyclerView setup with grouping by date
        rvMonthlyTransactions = findViewById(R.id.rvMonthlyTransactions);
        rvMonthlyTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionGroupAdapter();
        adapter.setTransactions(monthlyTransactions);
        adapter.setShowFooter(false); // No pagination on this summary screen
        rvMonthlyTransactions.setAdapter(adapter);

        // Swipe-to-refresh
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadSpendingData();
            swipeRefreshLayout.setRefreshing(false);
        });

        // Initialize with current month and year
        Calendar cal = Calendar.getInstance();
        selectedMonth = cal.get(Calendar.MONTH);
        selectedYear = cal.get(Calendar.YEAR);

        // Click on month text to change month
        tvTransactionMonth.setOnClickListener(v -> showMonthYearPicker());

        // Navigation (Back button hidden in XML as requested, but logic remains for compatibility)
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Navigate to secondary spending details screen with shared element transition
        btnSeeSpending.setOnClickListener(v -> {
            Intent intent = new Intent(SpendingActivity.this, Spending2Activity.class);
            
            // Create pairs for shared elements
            // We animate the circle and the header text
            Pair<View, String> pair1 = Pair.create(circleContainer, "spending_circle");
            Pair<View, String> pair2 = Pair.create(findViewById(R.id.tvMonthlyHeader), "spending_header");
            Pair<View, String> pair3 = Pair.create(tvLimitStatus, "spending_text");
            
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, pair1, pair2, pair3);
            startActivity(intent, options.toBundle());
        });

        // Bottom Navigation Logic
        btnDashboard = findViewById(R.id.btnDashboard);
        btnAiChat = findViewById(R.id.btnAiChat);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnSpending = findViewById(R.id.btnSpending);
        btnScanner = findViewById(R.id.btnScanner);

        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(SpendingActivity.this, dashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnAiChat.setOnClickListener(v -> {
            Intent intent = new Intent(SpendingActivity.this, AiChatActivity.class);
            startActivity(intent);
        });

        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(SpendingActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        btnScanner.setOnClickListener(v -> {
            Intent intent = new Intent(SpendingActivity.this, ScanActivity.class);
            startActivity(intent);
        });

        loadSpendingData();
    }

    /**
     * Displays a dialog for the user to select a different month and year for viewing transactions.
     */
    private void showMonthYearPicker() {
        final String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_month_year_picker, null);
        android.widget.NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        android.widget.NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(months);
        monthPicker.setValue(selectedMonth);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear - 5);
        yearPicker.setMaxValue(currentYear + 5);
        yearPicker.setValue(selectedYear);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Month & Year")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedMonth = monthPicker.getValue();
                    selectedYear = yearPicker.getValue();
                    loadSpendingData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Fetches transactions for the selected month and updates all UI components.
     */
    private void loadSpendingData() {
        if (userId != -1) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, selectedMonth);
            cal.set(Calendar.YEAR, selectedYear);
            
            String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            tvTransactionMonth.setText(monthName + " " + selectedYear);

            // Fetch specific limit for this month/year
            MonthlyLimit mLimit = db.monthlyLimitDao().getLimit(userId, selectedMonth, selectedYear);
            double limit = (mLimit != null) ? mLimit.limit : 0;
            tvLimitAmountInside.setText(String.format(Locale.getDefault(), "RM%.2f", limit));

            // Accumulate Income and Expense for the selected period
            List<Transaction> all = db.transactionDao().getAll(userId);
            double totalExpense = 0;
            double totalIncome = 0;
            monthlyTransactions.clear();

            for (Transaction t : all) {
                Calendar tCal = Calendar.getInstance();
                tCal.setTimeInMillis(t.date);
                if (tCal.get(Calendar.MONTH) == selectedMonth && tCal.get(Calendar.YEAR) == selectedYear) {
                    monthlyTransactions.add(t);
                    if ("expense".equalsIgnoreCase(t.type)) {
                        totalExpense += t.amount;
                    } else if ("income".equalsIgnoreCase(t.type)) {
                        totalIncome += t.amount;
                    }
                }
            }

            // Update the transaction list (sorted by latest)
            monthlyTransactions.sort((o1, o2) -> Long.compare(o2.date, o1.date));
            adapter.setTransactions(monthlyTransactions);

            tvSpentAmountInside.setText(String.format(Locale.getDefault(), "RM%.2f", totalExpense));
            tvIncomeAmountInside.setText(String.format(Locale.getDefault(), "RM%.2f", totalIncome));

            // Handle progress animation and status text
            if (limit > 0) {
                int targetProgress = (int) ((totalExpense / limit) * 100);
                animateProgress(targetProgress);
                
                double percentage = (totalExpense / limit) * 100;
                tvLimitStatus.setText(String.format(Locale.getDefault(), "Spent RM%.2f | Income RM%.2f\n%.1f%% of limit used", totalExpense, totalIncome, percentage));
            } else {
                spendingProgress.setProgress(0);
                tvLimitStatus.setText(String.format(Locale.getDefault(), "Spent RM%.2f | Income RM%.2f\nNo limit set", totalExpense, totalIncome));
            }
        }
    }

    /**
     * Smoothly animates the circular progress bar.
     */
    private void animateProgress(int targetProgress) {
        ValueAnimator animator = ValueAnimator.ofInt(0, Math.min(targetProgress, 100));
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            spendingProgress.setProgress(progress);
        });
        animator.start();
    }
}
