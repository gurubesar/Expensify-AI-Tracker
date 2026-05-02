package com.example.expensifytrackerdesign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.google.common.collect.ImmutableList;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;


/**
 * dashboardActivity is the main landing screen of the application.
 * It displays a summary of the user's finances, including:
 * - Current total balance (with a toggle to hide/show).
 * - A PieChart of expenses by category.
 * - Monthly spending progress vs. set limit.
 * - Top 5 transaction categories.
 * - AI-generated financial advice and predictions using Gemini.
 */
public class dashboardActivity extends AppCompatActivity {

    // UI Components
    TextView txtGreeting, tvName, tvSpendingLimit, tvSpentAmount, tvTotalBalance, tvBalanceChange, tvAiAdvice, tvAiPrediction;
    LinearLayout llCategorySummary;
    ImageView profileImage, ivToggleBalance;
    ProgressBar progressBar;
    PieChart pieChart;
    ImageButton btnDashboard, btnAddIncome, btnAddExpense, btnSpending, btnScanner, btnAiChat;
    SwipeRefreshLayout swipeRefreshLayout;
    AppDatabase db;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView spinnerMonth;

    int userId;
    boolean isBalanceHidden = false;
    double currentTotalBalance = 0;
    int selectedMonth; // Current selected month (0-11)
    int selectedYear; // Current selected year
    Map<String, Double> lastExpenseCategoryTotals = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize Room Database
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries() // Using main thread for simplicity in this dashboard
                .build();

        // Retrieve the logged-in user's ID
        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        // Bind UI components to their XML counterparts
        txtGreeting = findViewById(R.id.tvGreeting);
        tvName = findViewById(R.id.tvName);
        tvSpendingLimit = findViewById(R.id.textView32);
        tvSpentAmount = findViewById(R.id.textView30);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvBalanceChange = findViewById(R.id.tvBalanceChange);
        tvAiAdvice = findViewById(R.id.tvAiAdvice);
        tvAiPrediction = findViewById(R.id.tvAiPrediction);
        progressBar = findViewById(R.id.progressBar);

        // AI Advisor card click listener
        MaterialCardView aiAdvisorCard = findViewById(R.id.aiAdvisorCard);
        aiAdvisorCard.setOnClickListener(v -> {
            Intent intent = new Intent(dashboardActivity.this, AiChatActivity.class);
            startActivity(intent);
        });

        profileImage = findViewById(R.id.imageView2);
        pieChart = findViewById(R.id.pieChart);
        ivToggleBalance = findViewById(R.id.ivToggleBalance);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        spinnerMonth = findViewById(R.id.spinnerMonth);

        // Make the chart card clickable using an overlay
        View pieChartOverlay = findViewById(R.id.pieChartOverlay);
        pieChartOverlay.setOnClickListener(v -> {
            if (lastExpenseCategoryTotals != null && !lastExpenseCategoryTotals.isEmpty()) {
                Intent intent = new Intent(dashboardActivity.this, ChartDetailActivity.class);
                intent.putExtra("categoryTotals", new HashMap<>(lastExpenseCategoryTotals));
                startActivity(intent);
            }
        });

        // Make the balance card clickable to show history comparison
        MaterialCardView balanceCard = findViewById(R.id.balanceCard);
        balanceCard.setOnClickListener(v -> {
            Intent intent = new Intent(dashboardActivity.this, BalanceComparisonActivity.class);
            startActivity(intent);
        });

        // Setup the month selection dropdown
        setupMonthSpinner();

        // Swipe-to-refresh functionality to reload user data
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUserData();
            swipeRefreshLayout.setRefreshing(false);
        });

        llCategorySummary = findViewById(R.id.llCategorySummary);
        View dashboardtxt = findViewById(R.id.dashboardtxt);
        
        // Navigation to all categories screen
        View.OnClickListener goToAllCategories = v -> {
            Intent intent = new Intent(dashboardActivity.this, AllCategoriesActivity.class);
            intent.putExtra("selectedMonth", selectedMonth);
            startActivity(intent);
        };
        
        llCategorySummary.setOnClickListener(goToAllCategories);
        if (dashboardtxt != null) dashboardtxt.setOnClickListener(goToAllCategories);

        setRandomGreeting();
        loadUserData();

        // Profile navigation
        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(dashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Toggle balance visibility (privacy feature)
        ivToggleBalance.setOnClickListener(v -> {
            isBalanceHidden = !isBalanceHidden;
            updateBalanceDisplay();
        });

        // Initialize Bottom Navigation buttons
        btnDashboard = findViewById(R.id.btnDashboard);
        btnAiChat = findViewById(R.id.btnAiChat);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnSpending = findViewById(R.id.btnSpending);
        btnScanner = findViewById(R.id.btnScanner);

        // Set click listeners for bottom navigation
        btnAiChat.setOnClickListener(v -> {
            Intent intent = new Intent(dashboardActivity.this, AiChatActivity.class);
            startActivity(intent);
        });

        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(dashboardActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        btnSpending.setOnClickListener(v -> {
            Intent intent = new Intent(dashboardActivity.this, SpendingActivity.class);
            startActivity(intent);
        });

        btnScanner.setOnClickListener(v -> {
            Intent intent = new Intent(dashboardActivity.this, ScanActivity.class);
            startActivity(intent);
        });

        btnDashboard.setOnClickListener(v -> {
            // Already on dashboard, no action needed
        });

        // Click listeners to open the "Set Spending Limit" dialog
        findViewById(R.id.editText).setOnClickListener(v -> showSetLimitDialog());
        findViewById(R.id.textView29).setOnClickListener(v -> showSetLimitDialog());
        findViewById(R.id.textView31).setOnClickListener(v -> showSetLimitDialog());
        tvSpendingLimit.setOnClickListener(v -> showSetLimitDialog());
        tvSpentAmount.setOnClickListener(v -> showSetLimitDialog());
        progressBar.setOnClickListener(v -> showSetLimitDialog());
    }

    /**
     * Displays a dialog allowing the user to set a monthly spending limit.
     */
    private void showSetLimitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Spending Limit for " + spinnerMonth.getText().toString());

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        
        // Pre-fill with current limit if exists for the selected month/year
        MonthlyLimit current = db.monthlyLimitDao().getLimit(userId, selectedMonth, selectedYear);
        if (current != null) {
            input.setText(String.valueOf(current.limit));
            input.setSelection(input.getText().length());
        }

        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String limitStr = input.getText().toString();
            if (!limitStr.isEmpty()) {
                double limit = Double.parseDouble(limitStr);
                MonthlyLimit mLimit = new MonthlyLimit();
                if (current != null) {
                    mLimit.id = current.id;
                }
                mLimit.userId = userId;
                mLimit.month = selectedMonth;
                mLimit.year = selectedYear;
                mLimit.limit = limit;
                
                db.monthlyLimitDao().insert(mLimit);
                loadUserData(); // Refresh the dashboard with the new limit
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Configures the month selection spinner and sets its default to the current month.
     */
    private void setupMonthSpinner() {
        String[] months = {"January", "February", "March", "April", "May", "June", 
                          "July", "August", "September", "October", "November", "December"};

        java.util.Calendar cal = java.util.Calendar.getInstance();
        selectedMonth = cal.get(java.util.Calendar.MONTH);
        selectedYear = cal.get(java.util.Calendar.YEAR);
        spinnerMonth.setText(months[selectedMonth] + " " + selectedYear, false);

        // Remove the adapter to prevent the default dropdown from showing
        spinnerMonth.setAdapter(null);
        
        // Open custom picker on click for both the text and the layout
        View.OnClickListener listener = v -> showMonthYearPicker();
        spinnerMonth.setOnClickListener(listener);
        
        // Ensure the EditText is not focusable so clicks work immediately
        spinnerMonth.setFocusable(false);
        spinnerMonth.setCursorVisible(false);
        
        com.google.android.material.textfield.TextInputLayout menuMonth = findViewById(R.id.menuMonth);
        if (menuMonth != null) {
            // Disable the default end icon logic and handle clicks manually
            menuMonth.setEndIconOnClickListener(listener);
            
            // Set listener to the internal view that handles the actual box area
            menuMonth.getEditText().setOnClickListener(listener);
            
            // Make the TextInputLayout itself clickable
            menuMonth.setOnClickListener(listener);
        }
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
                    spinnerMonth.setText(months[selectedMonth] + " " + selectedYear, false);
                    loadUserData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Updates the total balance display, masking it if isBalanceHidden is true.
     */
    private void updateBalanceDisplay() {
        if (isBalanceHidden) {
            tvTotalBalance.setText("RM****");
            ivToggleBalance.setAlpha(0.5f);
        } else {
            tvTotalBalance.setText("RM" + String.format("%.2f", currentTotalBalance));
            ivToggleBalance.setAlpha(1.0f);
        }
    }

    /**
     * Ensures category strings are consistently formatted (e.g., "Food").
     */
    private String normalizeCategory(String input) {
        if (input == null || input.trim().isEmpty()) return "Other";
        String trimmed = input.trim();
        if (trimmed.length() == 1) return trimmed.toUpperCase();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    /**
     * The core logic to fetch and calculate financial data for the current user and month.
     */
    private void loadUserData() {
        if (userId != -1) {
            boolean isAiEnabled = getSharedPreferences("app", MODE_PRIVATE).getBoolean("ai_enabled", true);
            MaterialCardView aiAdvisorCard = findViewById(R.id.aiAdvisorCard);
            
            if (aiAdvisorCard != null) {
                if (!isAiEnabled) {
                    aiAdvisorCard.setAlpha(0.5f);
                    aiAdvisorCard.setClickable(false);
                    tvAiAdvice.setText("AI features are disabled");
                    tvAiPrediction.setText("Enable in Settings");
                } else {
                    aiAdvisorCard.setAlpha(1.0f);
                    aiAdvisorCard.setClickable(true);
                }
            }

            User user = db.userDao().getUserById(userId);
            if (user != null) {
                tvName.setText(user.username);
                
                List<Transaction> allTransactions = db.transactionDao().getAll(userId);
                List<Transaction> transactions = new ArrayList<>();
                
                Calendar cal = Calendar.getInstance();
                
                // Filter transactions to the selected month and selected year
                for (Transaction t : allTransactions) {
                    cal.setTimeInMillis(t.date);
                    if (cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear) {
                        transactions.add(t);
                    }
                }

                double totalIncome = 0;
                double totalExpense = 0;

                Map<String, Double> categoryTotals = new HashMap<>();
                Map<String, Double> expenseCategoryTotals = new HashMap<>();
                Map<String, String> categoryTypes = new HashMap<>();

                // Aggregate amounts by category and type
                for (Transaction t : transactions) {
                    String catKey = normalizeCategory(t.category);

                    if ("income".equalsIgnoreCase(t.type)) {
                        totalIncome += t.amount;
                    } else if ("expense".equalsIgnoreCase(t.type)) {
                        totalExpense += t.amount;
                        expenseCategoryTotals.put(catKey, expenseCategoryTotals.getOrDefault(catKey, 0.0) + t.amount);
                    }
                    
                    categoryTotals.put(catKey, categoryTotals.getOrDefault(catKey, 0.0) + t.amount);
                    categoryTypes.put(catKey, t.type);
                }

                lastExpenseCategoryTotals = expenseCategoryTotals;
                currentTotalBalance = totalIncome - totalExpense;
                updateBalanceDisplay();
                
                calculateBalanceChange(allTransactions, selectedMonth, selectedYear);
                
                updateCategorySummary(categoryTotals, categoryTypes);

                // Fetch monthly spending limit from DB or user default
                MonthlyLimit mLimit = db.monthlyLimitDao().getLimit(userId, selectedMonth, selectedYear);
                double monthlyLimitValue = (mLimit != null) ? mLimit.limit : user.monthlySpendingLimit;

                // Update Progress bar and spending text
                if (monthlyLimitValue > 0) {
                    tvSpendingLimit.setText("RM" + String.format("%.2f", monthlyLimitValue));
                    tvSpentAmount.setText("RM" + String.format("%.2f", totalExpense));
                    
                    int progress = (int) ((totalExpense / monthlyLimitValue) * 100);
                    progressBar.setProgress(Math.min(progress, 100));
                } else {
                    tvSpendingLimit.setText("RM0.00");
                    tvSpentAmount.setText("RM" + String.format("%.2f", totalExpense));
                    progressBar.setProgress(0);
                }

                // Update visual components
                updatePieChart(expenseCategoryTotals);
                if (isAiEnabled) {
                    updateAiAdvisor(totalIncome, totalExpense, expenseCategoryTotals);
                }
            }
        }
    }

    /**
     * Sends current month financial data to Gemini AI to generate a direct tip and prediction.
     */
    private void updateAiAdvisor(double income, double expense, Map<String, Double> expenses) {
        if (expense == 0) {
            tvAiAdvice.setText("Add expenses for AI tips.");
            tvAiPrediction.setText("Prediction: Data needed");
            return;
        }

        tvAiAdvice.setText("Analyzing...");

        // Construct the prompt for Gemini
        StringBuilder prompt = new StringBuilder();
        prompt.append("Financial data (RM):\n");
        prompt.append("- Income: ").append(income).append("\n");
        prompt.append("- Expense: ").append(expense).append("\n");
        prompt.append("- Breakdown: ").append(expenses.toString()).append("\n\n");
        prompt.append("Task: Provide 1 brief tip (max 10 words) and 1 brief prediction (max 10 words). Be direct. No intro/outro.");

        // Call Gemini API in a background thread
        new Thread(() -> {
            try {
                Client client = Client.builder()
                        .apiKey(BuildConfig.GEMINI_API_KEY)
                        .build();

                String model = "gemini-3.1-flash-lite-preview";
                List<Content> contents = ImmutableList.of(
                        Content.builder()
                                .role("user")
                                .parts(ImmutableList.of(
                                        Part.fromText(prompt.toString())
                                ))
                                .build()
                );

                GenerateContentConfig config = GenerateContentConfig.builder()
                        .thinkingConfig(ThinkingConfig.builder()
                                .thinkingLevel("HIGH")
                                .build())
                        .build();

                GenerateContentResponse result = client.models.generateContent(model, contents, config);

                final String output;
                if (result.candidates().isPresent() && !result.candidates().get().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    List<Part> parts = result.candidates().get().get(0).content().get().parts().get();
                    for (Part part : parts) {
                        sb.append(part.text().orElse(""));
                    }
                    output = sb.toString();
                } else {
                    output = "No advice generated.";
                }

                runOnUiThread(() -> {
                    tvAiAdvice.setText(output);
                    tvAiPrediction.setText("AI Analysis Complete");
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvAiAdvice.setText("AI Error: " + e.getMessage());
                    tvAiPrediction.setText("Prediction: Unavailable");
                });
            }
        }).start();
    }

    /**
     * Updates the Category Summary UI with the top 5 transaction categories.
     */
    private void updateCategorySummary(Map<String, Double> categoryTotals, Map<String, String> categoryTypes) {
        llCategorySummary.removeAllViews();
        
        List<Map.Entry<String, Double>> list = new ArrayList<>(categoryTotals.entrySet());
        Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        int count = 0;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, Double> entry : list) {
            if (count >= 5) break;
            
            View view = inflater.inflate(R.layout.item_category_summary, llCategorySummary, false);
            TextView tvName = view.findViewById(R.id.tvCategoryName);
            TextView tvType = view.findViewById(R.id.tvCategoryType);
            TextView tvAmount = view.findViewById(R.id.tvCategoryAmount);
            
            tvName.setText(entry.getKey());
            String type = categoryTypes.get(entry.getKey());
            tvType.setText(type != null ? type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase() : "");
            tvAmount.setText(String.format(Locale.US, "RM%.2f", entry.getValue()));
            
            llCategorySummary.addView(view);
            count++;
        }
    }

    /**
     * Configures and displays the PieChart showing the expense breakdown.
     */
    private void updatePieChart(Map<String, Double> expenseCategoryTotals) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : expenseCategoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(8, 8, 8, 8);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(62f);
        pieChart.setTransparentCircleRadius(65f);
        pieChart.setDrawCenterText(true);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, ""));
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColor(Color.LTGRAY);
            dataSet.setDrawValues(false);
            PieData data = new PieData(dataSet);
            pieChart.setData(data);
            pieChart.setCenterText("No Spending");
        } else {
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);

            ArrayList<Integer> colors = new ArrayList<>();
            colors.add(Color.parseColor("#FFC261"));
            colors.add(Color.parseColor("#F48A44"));
            colors.add(Color.parseColor("#FAEC6F"));
            colors.add(Color.parseColor("#FFCC00"));
            colors.add(Color.parseColor("#D48811"));
            dataSet.setColors(colors);

            dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            dataSet.setValueLinePart1OffsetPercentage(80.f);
            dataSet.setValueLinePart1Length(0.15f);
            dataSet.setValueLinePart2Length(0.15f);
            dataSet.setValueLineColor(Color.BLACK);

            PieData data = new PieData(dataSet);
            data.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));
            data.setValueTextSize(11f); 
            data.setValueTextColor(Color.BLACK);
            data.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            pieChart.setData(data);

            pieChart.setCenterText("Spending");
            pieChart.setCenterTextColor(Color.parseColor("#F48A44"));
            pieChart.setCenterTextSize(18f);
            pieChart.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }

        pieChart.getLegend().setEnabled(false);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(10f);
        pieChart.animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
        pieChart.invalidate();
    }

    /**
     * Calculates the percentage change in balance from the previous month relative to the selected period.
     */
    private void calculateBalanceChange(List<Transaction> allTransactions, int selMonth, int selYear) {
        Calendar cal = Calendar.getInstance();
        
        // Start of selected month
        cal.set(selYear, selMonth, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long selectedMonthStart = cal.getTimeInMillis();

        // End of selected month
        cal.add(Calendar.MONTH, 1);
        long selectedMonthEnd = cal.getTimeInMillis() - 1;

        // Start of previous month
        cal.add(Calendar.MONTH, -2);
        long prevMonthStart = cal.getTimeInMillis();
        long prevMonthEnd = selectedMonthStart - 1;

        double selectedMonthNet = 0;
        double prevMonthNet = 0;

        for (Transaction t : allTransactions) {
            double amount = "income".equalsIgnoreCase(t.type) ? t.amount : -t.amount;
            if (t.date >= selectedMonthStart && t.date <= selectedMonthEnd) {
                selectedMonthNet += amount;
            } else if (t.date >= prevMonthStart && t.date <= prevMonthEnd) {
                prevMonthNet += amount;
            }
        }

        if (prevMonthNet == 0) {
            tvBalanceChange.setText("0%");
        } else {
            double change = ((selectedMonthNet - prevMonthNet) / Math.abs(prevMonthNet)) * 100;
            String symbol = change >= 0 ? "↑" : "↓";
            tvBalanceChange.setText(symbol + " " + String.format(Locale.US, "%.1f", Math.abs(change)) + "%");
        }
    }

    /**
     * Sets a random friendly greeting for the user.
     */
    private void setRandomGreeting() {
        String[] randomTips = {
            "Great day to save some money!",
            "Ready to track your expenses?",
            "Every cent counts!",
            "Manage your money, master your life.",
            "Ready to reach your goals today?",
            "Let's keep your finances in check.",
            "Smart spending, better living!",
            "Track today, relax tomorrow!",
            "Stay disciplined with your spending.",
            "Small savings, big dreams."
        };
        
        int randomIndex = new Random().nextInt(randomTips.length);
        txtGreeting.setText(randomTips[randomIndex]);
    }
}
