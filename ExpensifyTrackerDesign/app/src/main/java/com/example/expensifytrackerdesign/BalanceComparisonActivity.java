package com.example.expensifytrackerdesign;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Activity that displays a 6-month balance comparison using a BarChart and a list.
 * It provides AI-powered analysis of the user's financial trends.
 */
public class BalanceComparisonActivity extends AppCompatActivity {

    private BarChart barChart;
    private RecyclerView rvBalanceHistory;
    private TextView tvAiAnalysis;
    private AppDatabase db;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance_comparison);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        barChart = findViewById(R.id.barChart);
        rvBalanceHistory = findViewById(R.id.rvBalanceHistory);
        tvAiAnalysis = findViewById(R.id.tvAiAnalysis);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .allowMainThreadQueries()
                .build();

        userId = getSharedPreferences("app", MODE_PRIVATE).getInt("userId", -1);

        if (userId != -1) {
            loadBalanceHistory();
        }
    }

    /**
     * Loads the balance history for the last 6 months based on the latest transaction found.
     * Calculates the net balance (income - expense) for each month.
     */
    private void loadBalanceHistory() {
        List<Transaction> allTransactions = db.transactionDao().getAll(userId);
        if (allTransactions.isEmpty()) return;

        // Find the latest transaction date to start the history from
        long latestDate = 0;
        for (Transaction t : allTransactions) {
            if (t.date > latestDate) latestDate = t.date;
        }

        List<MonthBalance> history = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // Start from the latest transaction date found, or current date, whichever is later
        long now = System.currentTimeMillis();
        cal.setTimeInMillis(Math.max(latestDate, now));
        cal.set(Calendar.DAY_OF_MONTH, 1);

        for (int i = 0; i < 6; i++) {
            int targetMonth = cal.get(Calendar.MONTH);
            int targetYear = cal.get(Calendar.YEAR);

            double income = 0;
            double expense = 0;

            for (Transaction t : allTransactions) {
                Calendar tCal = Calendar.getInstance();
                tCal.setTimeInMillis(t.date);
                if (tCal.get(Calendar.MONTH) == targetMonth && tCal.get(Calendar.YEAR) == targetYear) {
                    if ("income".equalsIgnoreCase(t.type)) {
                        income += t.amount;
                    } else {
                        expense += t.amount;
                    }
                }
            }

            String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
            history.add(new MonthBalance(monthName, income - expense));
            cal.add(Calendar.MONTH, -1);
        }

        List<MonthBalance> filteredHistory = new ArrayList<>();
        for (MonthBalance mb : history) {
            if (mb.balance != 0) {
                filteredHistory.add(mb);
            }
        }

        // For the chart: Chronological order (Feb -> Mar -> Apr -> May)
        Collections.reverse(filteredHistory);
        setupBarChart(filteredHistory);

        // For the list: Most recent first (May -> Apr -> Mar -> Feb)
        List<MonthBalance> listHistory = new ArrayList<>(filteredHistory);
        Collections.reverse(listHistory);
        setupRecyclerView(listHistory);

        updateAiAnalysis(filteredHistory);
    }

    /**
     * Checks if AI features are globally enabled in the application settings.
     * @return true if AI is enabled, false otherwise.
     */
    private boolean isAiEnabled() {
        return getSharedPreferences("app", MODE_PRIVATE).getBoolean("ai_enabled", true);
    }

    /**
     * Sends the historical balance data to Gemini AI to generate a brief summary and recommendation.
     * Updates the UI text box with the result.
     * @param history The list of monthly balances to analyze.
     */
    private void updateAiAnalysis(List<MonthBalance> history) {
        if (!isAiEnabled()) {
            tvAiAnalysis.setText("AI features are disabled in Settings. Please enable them to see analysis.");
            tvAiAnalysis.setAlpha(0.5f);
            return;
        }
        tvAiAnalysis.setAlpha(1.0f);

        if (history.isEmpty()) {
            tvAiAnalysis.setText("No data available for AI analysis.");
            return;
        }

        tvAiAnalysis.setText("Analyzing...");

        StringBuilder prompt = new StringBuilder();
        prompt.append("Historical Balance Data (RM):\n");
        for (MonthBalance mb : history) {
            prompt.append("- ").append(mb.monthName).append(": ").append(mb.balance).append("\n");
        }
        prompt.append("\nTask: Provide a 1-sentence analysis of the balance trend and a 1-sentence recommendation. Max 30 words total.");

        new Thread(() -> {
            try {
                Client client = Client.builder()
                        .apiKey(BuildConfig.GEMINI_API_KEY)
                        .build();

                String model = "gemini-3.1-flash-lite-preview";
                List<Content> contents = ImmutableList.of(
                        Content.builder()
                                .role("user")
                                .parts(ImmutableList.of(Part.fromText(prompt.toString())))
                                .build()
                );

                GenerateContentResponse result = client.models.generateContent(model, contents, null);

                final String output;
                if (result.candidates().isPresent() && !result.candidates().get().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    List<Part> parts = result.candidates().get().get(0).content().get().parts().get();
                    for (Part part : parts) {
                        sb.append(part.text().orElse(""));
                    }
                    output = sb.toString();
                } else {
                    output = "No analysis generated.";
                }

                runOnUiThread(() -> tvAiAnalysis.setText(output));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvAiAnalysis.setText("AI analysis temporarily unavailable."));
            }
        }).start();
    }

    private void setupBarChart(List<MonthBalance> history) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            entries.add(new BarEntry(i, (float) history.get(i).balance));
            labels.add(history.get(i).monthName);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Balance");
        dataSet.setColor(Color.parseColor("#F4A940"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void setupRecyclerView(List<MonthBalance> history) {
        rvBalanceHistory.setLayoutManager(new LinearLayoutManager(this));
        // Show most recent first in list
        rvBalanceHistory.setAdapter(new BalanceHistoryAdapter(history));
    }

    private static class MonthBalance {
        String monthName;
        double balance;

        MonthBalance(String monthName, double balance) {
            this.monthName = monthName;
            this.balance = balance;
        }
    }

    private static class BalanceHistoryAdapter extends RecyclerView.Adapter<BalanceHistoryAdapter.ViewHolder> {
        private final List<MonthBalance> data;

        BalanceHistoryAdapter(List<MonthBalance> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_detail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MonthBalance item = data.get(position);
            holder.tvName.setText(item.monthName);
            holder.tvAmount.setText(String.format(Locale.US, "RM%.2f", item.balance));
            holder.tvAmount.setTextColor(item.balance >= 0 ? Color.parseColor("#2E7D32") : Color.RED);
            holder.vColor.setBackgroundColor(Color.parseColor("#F4A940"));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvAmount;
            View vColor;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCategoryName);
                tvAmount = itemView.findViewById(R.id.tvCategoryAmount);
                vColor = itemView.findViewById(R.id.vColorIndicator);
            }
        }
    }
}