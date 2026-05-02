package com.example.expensifytrackerdesign;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.view.View;
import android.widget.TextView;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.common.collect.ImmutableList;

/**
 * AiChatActivity provides an interface for the user to chat with a Gemini-powered AI Financial Advisor.
 * It integrates user transaction data to provide personalized financial insights.
 */
public class AiChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private ChatAdapter chatAdapter;
    private AppDatabase db;
    private int userId;
    
    // Executor for background tasks (DB and API calls)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Bottom Navigation buttons
    private ImageButton btnDashboard, btnAiChat, btnAddExpense, btnSpending, btnScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        // Toolbar setup - back arrow removed programmatically as requested
        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("AI Advisor Chat");
        }

        // UI Component Initialization
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        // DB initialization
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "app-db")
                .fallbackToDestructiveMigration()
                .build();

        userId = getSharedPreferences("app", MODE_PRIVATE).getInt("userId", -1);

        // Chat RecyclerView setup
        chatAdapter = new ChatAdapter(new ArrayList<>());
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        // Load existing chat messages from the database
        loadChatHistory();

        btnSend.setOnClickListener(v -> sendMessage());

        checkAiEnabled();

        // Setup Bottom Navigation logic
        btnDashboard = findViewById(R.id.btnDashboard);
        btnAiChat = findViewById(R.id.btnAiChat);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnSpending = findViewById(R.id.btnSpending);
        btnScanner = findViewById(R.id.btnScanner);

        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(AiChatActivity.this, dashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(AiChatActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        btnSpending.setOnClickListener(v -> {
            Intent intent = new Intent(AiChatActivity.this, SpendingActivity.class);
            startActivity(intent);
        });

        btnScanner.setOnClickListener(v -> {
            Intent intent = new Intent(AiChatActivity.this, ScanActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Checks if AI features are globally enabled in the application settings.
     * Shows a semi-transparent overlay to block access if disabled.
     */
    private void checkAiEnabled() {
        boolean isAiEnabled = getSharedPreferences("app", MODE_PRIVATE).getBoolean("ai_enabled", true);
        View overlay = findViewById(R.id.aiDisabledOverlay);
        if (overlay != null) {
            overlay.setVisibility(isAiEnabled ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAiEnabled();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_clear_chat) {
            clearChatHistory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Deletes all chat messages from the local database and clears the UI.
     */
    private void clearChatHistory() {
        executor.execute(() -> {
            db.chatMessageDao().deleteAll();
            mainHandler.post(() -> {
                chatAdapter.clearMessages();
                rvChat.scrollToPosition(0);
            });
        });
    }

    /**
     * Retrieves previous messages from the database to populate the chat screen.
     */
    private void loadChatHistory() {
        executor.execute(() -> {
            List<ChatMessage> messages = db.chatMessageDao().getAllMessages();
            mainHandler.post(() -> {
                for (ChatMessage m : messages) {
                    chatAdapter.addMessage(m);
                }
                rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
            });
        });
    }

    /**
     * Handles the process of sending a user message, saving it, and triggering the AI response.
     */
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        // Display user message immediately
        ChatMessage userMsg = new ChatMessage(text, true, System.currentTimeMillis());
        chatAdapter.addMessage(userMsg);
        etMessage.setText("");
        rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);

        executor.execute(() -> {
            db.chatMessageDao().insert(userMsg);
            
            // Gather financial context to send to the AI for more relevant advice
            List<Transaction> transactions = db.transactionDao().getAll(userId);
            double totalIncome = 0;
            double totalExpense = 0;
            Map<String, Double> categoryTotals = new HashMap<>();
            java.util.Set<String> monthsTracked = new java.util.HashSet<>();
            java.util.Calendar cal = java.util.Calendar.getInstance();

            for (Transaction t : transactions) {
                cal.setTimeInMillis(t.date);
                String monthKey = cal.get(java.util.Calendar.YEAR) + "-" + cal.get(java.util.Calendar.MONTH);
                monthsTracked.add(monthKey);

                if ("Income".equalsIgnoreCase(t.type)) {
                    totalIncome += t.amount;
                } else {
                    totalExpense += t.amount;
                    String cat = t.category != null ? t.category : "Other";
                    categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + t.amount);
                }
            }
            
            int monthCount = Math.max(1, monthsTracked.size());
            final double avgIncome = totalIncome / monthCount;
            final double avgExpense = totalExpense / monthCount;
            final double lifetimeIncome = totalIncome;
            final double lifetimeExpense = totalExpense;
            final int transCount = transactions.size();
            final String breakdown = categoryTotals.toString();

            // Fetch history and call Gemini after a small delay for UI smoothness
            mainHandler.postDelayed(() -> {
                executor.execute(() -> {
                    List<ChatMessage> history = db.chatMessageDao().getAllMessages();
                    // Take the last 10 messages for context
                    int start = Math.max(0, history.size() - 11);
                    List<ChatMessage> recentHistory = history.subList(start, history.size() - 1);
                    
                    mainHandler.post(() -> {
                        chatAdapter.setThinking(true);
                        rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                        callGeminiAi(text, recentHistory, avgIncome, avgExpense, lifetimeIncome, lifetimeExpense, transCount, breakdown);
                    });
                });
            }, 500);
        });
    }

    /**
     * Connects to the Gemini API using the provided financial context and chat history.
     */
    /**
     * Connects to the Gemini API using the provided financial context and chat history.
     * @param userMessage The current message from the user.
     * @param history The previous messages in the chat for context.
     * @param avgIncome The calculated average monthly income.
     * @param avgExpense The calculated average monthly expense.
     * @param lifetimeIncome The total income recorded for the user.
     * @param lifetimeExpense The total expense recorded for the user.
     * @param transCount The total number of transactions recorded.
     * @param breakdown A string representation of the user's spending categories.
     */
    private void callGeminiAi(String userMessage, List<ChatMessage> history, double avgIncome, double avgExpense, double lifetimeIncome, double lifetimeExpense, int transCount, String breakdown) {
        executor.execute(() -> {
            try {
                Client client = Client.builder()
                        .apiKey(BuildConfig.GEMINI_API_KEY)
                        .build();

                List<Content> contents = new ArrayList<>();
                
                // Construct the system instruction with user data
                StringBuilder systemPrompt = new StringBuilder();
                systemPrompt.append("You are a professional Financial Advisor AI for the 'Expensify' app.\n");
                systemPrompt.append("User's Financial Data Summary:\n");
                systemPrompt.append("- Average Monthly Income: RM").append(String.format(Locale.getDefault(), "%.2f", avgIncome)).append("\n");
                systemPrompt.append("- Average Monthly Expenses: RM").append(String.format(Locale.getDefault(), "%.2f", avgExpense)).append("\n");
                systemPrompt.append("- Total Lifetime Income: RM").append(String.format(Locale.getDefault(), "%.2f", lifetimeIncome)).append("\n");
                systemPrompt.append("- Total Lifetime Expenses: RM").append(String.format(Locale.getDefault(), "%.2f", lifetimeExpense)).append("\n");
                systemPrompt.append("- Expense Breakdown (Lifetime): ").append(breakdown).append("\n");
                systemPrompt.append("- Total Transactions recorded: ").append(transCount).append("\n\n");
                systemPrompt.append("Task: Answer the user's question using the data provided. ");
                systemPrompt.append("Be helpful, accurate, and professional. ONLY discuss financial topics, budgeting, or the user's spending data. ");
                systemPrompt.append("If the user asks something unrelated to money or finances, politely explain your role.");

                // Map history to Gemini Content objects
                String lastRole = null;
                boolean isFirst = true;
                
                for (ChatMessage m : history) {
                    String role = m.isUser() ? "user" : "model";
                    if (role.equals(lastRole)) continue; // Ensure strict user-model alternation
                    
                    String msgText = m.getMessage();
                    if (isFirst && m.isUser()) {
                        msgText = "Context: " + systemPrompt.toString() + "\n\nPrevious message: " + msgText;
                        isFirst = false;
                    }
                    
                    contents.add(Content.builder()
                            .role(role)
                            .parts(ImmutableList.of(Part.fromText(msgText)))
                            .build());
                    lastRole = role;
                }

                // Append the new message
                String currentMsgText = userMessage;
                if (contents.isEmpty()) {
                    currentMsgText = "Context: " + systemPrompt.toString() + "\n\nQuestion: " + userMessage;
                }
                
                if ("user".equals(lastRole)) {
                    if (!contents.isEmpty()) contents.remove(contents.size() - 1);
                }

                contents.add(Content.builder()
                        .role("user")
                        .parts(ImmutableList.of(Part.fromText(currentMsgText)))
                        .build());

                String model = "gemini-3.1-flash-lite-preview";
                GenerateContentResponse result = client.models.generateContent(model, contents, null);

                String output = "I'm having trouble connecting to my brain right now. Please try again.";
                if (result.candidates().isPresent() && !result.candidates().get().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    List<Part> parts = result.candidates().get().get(0).content().get().parts().get();
                    for (Part part : parts) {
                        sb.append(part.text().orElse(""));
                    }
                    output = sb.toString();
                }

                // Update UI with AI response
                String finalOutput = output;
                mainHandler.post(() -> {
                    chatAdapter.setThinking(false);
                    ChatMessage aiMsg = new ChatMessage(finalOutput, false, System.currentTimeMillis());
                    chatAdapter.addMessage(aiMsg);
                    rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                    executor.execute(() -> db.chatMessageDao().insert(aiMsg));
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    chatAdapter.setThinking(false);
                    ChatMessage errorMsg = new ChatMessage("Sorry, I encountered an error analyzing your data.", false, System.currentTimeMillis());
                    chatAdapter.addMessage(errorMsg);
                });
            }
        });
    }
}
