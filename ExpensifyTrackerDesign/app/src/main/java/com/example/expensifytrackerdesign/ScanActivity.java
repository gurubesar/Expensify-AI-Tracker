package com.example.expensifytrackerdesign;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ScanActivity allows users to capture or upload receipt images, which are then processed
 * using Gemini AI to extract transaction details (Title, Amount, Category, Date).
 */
public class ScanActivity extends AppCompatActivity {
    private ImageButton btnDashboard, btnAiChat, btnAddExpense, btnSpending, btnScanner;
    private TextView btnScan, btnUpload;
    private android.widget.ProgressBar progressBar;
    private View aiDisabledOverlay;
    private AppDatabase db;
    private int userId;
    
    /**
     * API Key for Gemini AI services.
     */
    private final String API_KEY = BuildConfig.GEMINI_API_KEY;

    /**
     * Launcher for selecting an image from the gallery.
     */
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    processImageFromUri(imageUri);
                }
            }
    );

    /**
     * Launcher for capturing a new photo using the camera.
     */
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap imageBitmap = (Bitmap) result.getData().getExtras().get("data");
                    processImageFromBitmap(imageBitmap);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // Database Initialization
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();

        userId = getSharedPreferences("app", MODE_PRIVATE).getInt("userId", -1);

        // UI Setup
        ImageButton backButton = findViewById(R.id.imageButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        btnDashboard = findViewById(R.id.btnDashboard);
        btnAiChat = findViewById(R.id.btnAiChat);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnSpending = findViewById(R.id.btnSpending);
        btnScanner = findViewById(R.id.btnScanner);
        btnScan = findViewById(R.id.btnScan);
        btnUpload = findViewById(R.id.btnUpload);
        progressBar = findViewById(R.id.progressBar);
        aiDisabledOverlay = findViewById(R.id.aiDisabledOverlay);
        
        // Triggers camera for receipt scanning
        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });
        
        // Triggers gallery for receipt upload
        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // Bottom Navigation
        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(ScanActivity.this, dashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnAiChat.setOnClickListener(v -> {
            Intent intent = new Intent(ScanActivity.this, AiChatActivity.class);
            startActivity(intent);
        });

        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(ScanActivity.this, AddExpenseActivity.class);
            startActivity(intent);
            finish();
        });

        btnSpending.setOnClickListener(v -> {
            Intent intent = new Intent(ScanActivity.this, SpendingActivity.class);
            startActivity(intent);
            finish();
        });

        checkAiEnabled();
    }

    /**
     * Checks if AI features are enabled in settings. 
     * If disabled, shows a semi-transparent overlay blocking the UI.
     */
    private void checkAiEnabled() {
        boolean isAiEnabled = getSharedPreferences("app", MODE_PRIVATE).getBoolean("ai_enabled", true);
        if (aiDisabledOverlay != null) {
            aiDisabledOverlay.setVisibility(isAiEnabled ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAiEnabled();
    }

    /**
     * Decodes a URI into a Bitmap for processing.
     */
    private void processImageFromUri(Uri uri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            processImageFromBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Converts a Bitmap to a Base64 string and initiates AI processing.
     */
    private void processImageFromBitmap(Bitmap bitmap) {
        Toast.makeText(this, "Processing receipt...", Toast.LENGTH_SHORT).show();
        
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] bytes = outputStream.toByteArray();
        String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);
        
        processReceiptWithAI(base64Image, true);
    }

    /**
     * Communicates with Gemini AI to extract financial data from the provided receipt data (image or text).
     */
    private void processReceiptWithAI(String data, boolean isImage) {
        runOnUiThread(() -> {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        });
        new Thread(() -> {
            try {
                Client client = Client.builder().apiKey(API_KEY).build();
                String prompt = "Extract data from this receipt or payslip " + (isImage ? "image" : "text") + ". " +
                        "Return ONLY a JSON object with keys: \"title\", \"amount\", \"category\", \"date\", \"type\".\n" +
                        "- \"title\": Use the merchant name, employer, or main item.\n" +
                        "- \"amount\": The total amount as a plain number.\n" +
                        "- \"category\": A logical category.\n" +
                        "- \"date\": The date in YYYY-MM-DD format.\n" +
                        "- \"type\": Either \"income\" if it's a payslip/salary or \"expense\" if it's a receipt/purchase.\n" +
                        "Ensure the response is valid JSON and nothing else.";

                Part part;
                if (isImage) {
                    part = Part.fromBytes(Base64.decode(data, Base64.DEFAULT), "image/jpeg");
                } else {
                    part = Part.fromText(data);
                }
                
                List<Content> contents = ImmutableList.of(
                        Content.builder().role("user").parts(ImmutableList.of(part, Part.fromText(prompt))).build()
                );

                GenerateContentResponse result = client.models.generateContent("gemini-3.1-flash-lite-preview", contents, null);

                if (result.candidates().isPresent() && !result.candidates().get().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    List<Part> parts = result.candidates().get().get(0).content().get().parts().get();
                    for (Part p : parts) sb.append(p.text().orElse(""));
                    String jsonText = sb.toString();

                    // Robustly extract JSON from the AI response
                    int firstBrace = jsonText.indexOf("{");
                    int lastBrace = jsonText.lastIndexOf("}");
                    
                    if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                        jsonText = jsonText.substring(firstBrace, lastBrace + 1);
                    } else {
                        // If no braces found, try cleaning markdown as a fallback
                        jsonText = jsonText.replace("```json", "").replace("```", "").trim();
                    }
                    
                    final String finalJson = jsonText;
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        navigateToConfirm(finalJson);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(ScanActivity.this, "Scan failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Navigates to the appropriate add transaction activity with pre-filled data.
     */
    private void navigateToConfirm(String jsonStr) {
        android.util.Log.d("ScanActivity", "AI Response JSON: " + jsonStr);
        try {
            JsonObject json = new Gson().fromJson(jsonStr, JsonObject.class);
            String title = json.has("title") ? json.get("title").getAsString() : "";
            String type = json.has("type") ? json.get("type").getAsString().toLowerCase() : "expense";
            String category = json.has("category") ? json.get("category").getAsString() : "";
            String dateStr = json.has("date") ? json.get("date").getAsString() : "";
            
            double amount = 0.0;
            if (json.has("amount")) {
                try {
                    amount = json.get("amount").getAsDouble();
                } catch (Exception e) {
                    String amountStr = json.get("amount").getAsString().replaceAll("[^\\d.]", "");
                    if (!amountStr.isEmpty()) amount = Double.parseDouble(amountStr);
                }
            }

            Intent intent;
            if ("income".equals(type)) {
                intent = new Intent(this, AddIncomeActivity.class);
            } else {
                intent = new Intent(this, AddExpenseActivity.class);
            }

            intent.putExtra("PREFILL_TITLE", title);
            intent.putExtra("PREFILL_AMOUNT", amount);
            intent.putExtra("PREFILL_CATEGORY", category);
            intent.putExtra("PREFILL_DATE", dateStr);
            intent.putExtra("FROM_SCAN", true);
            
            startActivity(intent);
            finish();

        } catch (Exception e) {
            android.util.Log.e("ScanActivity", "Error parsing JSON: " + jsonStr, e);
            Toast.makeText(this, "Error parsing receipt data. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Saves the confirmed transaction into the Room database.
     */
    private void saveTransaction(String title, double amount, String category, String dateStr) {
        try {
            long dateMillis;
            try {
                dateMillis = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr).getTime();
            } catch (ParseException e) {
                dateMillis = System.currentTimeMillis();
            }

            Transaction t = new Transaction();
            t.userId = userId;
            t.title = title;
            t.amount = amount;
            t.category = category;
            t.type = "expense";
            t.date = dateMillis;

            db.transactionDao().insert(t);
            Toast.makeText(this, "Receipt saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
