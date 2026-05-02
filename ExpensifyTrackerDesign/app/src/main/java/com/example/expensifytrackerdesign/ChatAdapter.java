package com.example.expensifytrackerdesign;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

/**
 * Adapter for displaying chat messages in the AiChatActivity.
 * Supports alternating alignment for user and AI messages, and a "thinking" state.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messages;
    
    /**
     * Flag indicating if the AI is currently generating a response.
     */
    private boolean isThinking = false;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        // Special case for the "thinking" indicator at the end of the list
        if (position == messages.size() && isThinking) {
            holder.tvMessage.setText("...");
            startDotsAnimation(holder.tvMessage);
            
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.chatCard.getLayoutParams();
            params.gravity = Gravity.START;
            holder.chatCard.setCardBackgroundColor(0xFFEEEEEE); // Light grey for thinking
            holder.tvMessage.setTextColor(0xFF888888);
            holder.chatCard.setLayoutParams(params);
            return;
        }

        ChatMessage message = messages.get(position);
        holder.tvMessage.setText(message.getMessage());
        holder.tvMessage.clearAnimation(); // Stop any leftover "thinking" animation

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.chatCard.getLayoutParams();
        if (message.isUser()) {
            // Right-aligned for User messages
            params.gravity = Gravity.END;
            holder.chatCard.setCardBackgroundColor(0xFFF4B24D); // Orange for user
            holder.tvMessage.setTextColor(0xFF000000);
        } else {
            // Left-aligned for AI messages
            params.gravity = Gravity.START;
            holder.chatCard.setCardBackgroundColor(0xFFFFFFFF); // White for AI
            holder.tvMessage.setTextColor(0xFF000000);
        }
        holder.chatCard.setLayoutParams(params);
    }

    /**
     * Animates the "..." text while the AI is thinking.
     */
    private void startDotsAnimation(TextView textView) {
        final String[] dots = {".", "..", "..."};
        textView.post(new Runnable() {
            int i = 0;
            @Override
            public void run() {
                if (isThinking && textView.getText().toString().startsWith(".")) {
                    textView.setText(dots[i % 3]);
                    i++;
                    textView.postDelayed(this, 500);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        // Add one extra item for the thinking indicator if active
        return messages.size() + (isThinking ? 1 : 0);
    }

    /**
     * Toggles the thinking indicator visibility.
     */
    public void setThinking(boolean thinking) {
        if (this.isThinking == thinking) return;
        this.isThinking = thinking;
        if (thinking) {
            notifyItemInserted(messages.size());
        } else {
            notifyItemRemoved(messages.size());
        }
    }

    /**
     * Appends a new message to the list and refreshes the UI.
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    /**
     * Clears all messages from the list and refreshes the UI.
     */
    public void clearMessages() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView chatCard;
        TextView tvMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatCard = itemView.findViewById(R.id.chatCard);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}
