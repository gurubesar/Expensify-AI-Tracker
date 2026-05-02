package com.example.expensifytrackerdesign;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity that shows a detailed breakdown of expenses using a PieChart.
 * Provides AI analysis of the current spending categories.
 */
public class ChartDetailActivity extends AppCompatActivity {

    private PieChart bigPieChart;
    private RecyclerView rvCategoryDetails;
    private TextView tvAiAnalysis;
    private HashMap<String, Double> categoryTotals;
    private List<Integer> colors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_detail);

        bigPieChart = findViewById(R.id.bigPieChart);
        rvCategoryDetails = findViewById(R.id.rvCategoryDetails);
        tvAiAnalysis = findViewById(R.id.tvAiAnalysis);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Get data from intent
        categoryTotals = (HashMap<String, Double>) getIntent().getSerializableExtra("categoryTotals");

        setupPieChart();
        setupRecyclerView();
        updateAiAnalysis();
    }

    /**
     * Checks if AI features are globally enabled in the application settings.
     * @return true if AI is enabled, false otherwise.
     */
    private boolean isAiEnabled() {
        return getSharedPreferences("app", MODE_PRIVATE).getBoolean("ai_enabled", true);
    }

    /**
     * Sends the current expense category breakdown to Gemini AI to generate a brief analysis.
     * Updates the UI text box with the result.
     */
    private void updateAiAnalysis() {
        if (!isAiEnabled()) {
            tvAiAnalysis.setText("AI features are disabled in Settings. Please enable them to see analysis.");
            tvAiAnalysis.setAlpha(0.5f);
            return;
        }
        tvAiAnalysis.setAlpha(1.0f);

        if (categoryTotals == null || categoryTotals.isEmpty()) {
            tvAiAnalysis.setText("No expense data for analysis.");
            return;
        }

        tvAiAnalysis.setText("Analyzing...");

        StringBuilder prompt = new StringBuilder();
        prompt.append("Monthly Expense Breakdown (RM):\n");
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        prompt.append("\nTask: Provide a 1-sentence observation on where most money goes and a 1-sentence saving tip. Max 30 words total.");

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

    /**
     * Configures the PieChart with category data and matching colors.
     */
    private void setupPieChart() {
        if (categoryTotals == null || categoryTotals.isEmpty()) return;

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Gunakan warna yang sama seperti di Dashboard
        colors = new ArrayList<>();
        colors.add(Color.parseColor("#FFC261"));
        colors.add(Color.parseColor("#F48A44"));
        colors.add(Color.parseColor("#FAEC6F"));
        colors.add(Color.parseColor("#FFCC00"));
        colors.add(Color.parseColor("#D48811"));
        // Tambah warna tambahan jika kategori lebih banyak daripada warna asal
        colors.add(Color.parseColor("#4CAF50")); // Green
        colors.add(Color.parseColor("#F44336")); // Red
        colors.add(Color.parseColor("#2196F3")); // Blue

        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Styling agar sama dengan Dashboard
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.15f);
        dataSet.setValueLinePart2Length(0.15f);
        dataSet.setValueLineColor(Color.BLACK);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(bigPieChart));
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.BLACK);
        data.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        bigPieChart.setData(data);
        bigPieChart.setUsePercentValues(true);
        bigPieChart.getDescription().setEnabled(false);
        bigPieChart.setDrawHoleEnabled(true);
        bigPieChart.setHoleColor(Color.TRANSPARENT);
        bigPieChart.setHoleRadius(62f);
        bigPieChart.setTransparentCircleRadius(65f);
        bigPieChart.setCenterText("Expenses");
        bigPieChart.setCenterTextColor(Color.parseColor("#F48A44"));
        bigPieChart.setCenterTextSize(18f);
        bigPieChart.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        bigPieChart.setEntryLabelColor(Color.BLACK);
        bigPieChart.setEntryLabelTextSize(11f);
        bigPieChart.getLegend().setEnabled(false);
        bigPieChart.animateY(1400);
        bigPieChart.invalidate();
    }

    private void setupRecyclerView() {
        rvCategoryDetails.setLayoutManager(new LinearLayoutManager(this));
        List<Map.Entry<String, Double>> list = new ArrayList<>(categoryTotals.entrySet());
        rvCategoryDetails.setAdapter(new CategoryAdapter(list, colors));
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<Map.Entry<String, Double>> data;
        private final List<Integer> colorList;

        public CategoryAdapter(List<Map.Entry<String, Double>> data, List<Integer> colorList) {
            this.data = data;
            this.colorList = colorList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_detail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map.Entry<String, Double> entry = data.get(position);
            holder.tvCategoryName.setText(entry.getKey());
            holder.tvCategoryAmount.setText(String.format("RM%.2f", entry.getValue()));
            if (colorList != null && position < colorList.size()) {
                holder.vColorIndicator.setBackgroundColor(colorList.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategoryName, tvCategoryAmount;
            View vColorIndicator;
            ViewHolder(View view) {
                super(view);
                tvCategoryName = view.findViewById(R.id.tvCategoryName);
                tvCategoryAmount = view.findViewById(R.id.tvCategoryAmount);
                vColorIndicator = view.findViewById(R.id.vColorIndicator);
            }
        }
    }
}