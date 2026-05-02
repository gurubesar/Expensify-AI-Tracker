package com.example.expensifytrackerdesign;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a list of individual financial transactions (Income or Expense).
 * Color codes amounts based on the transaction type.
 */
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Transaction> transactionList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private OnTransactionLongClickListener longClickListener;

    /**
     * Interface to handle long-press events on transaction items.
     */
    public interface OnTransactionLongClickListener {
        void onLongClick(Transaction transaction);
    }

    public ExpenseAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    /**
     * Registers a listener for long-click events.
     */
    public void setOnTransactionLongClickListener(OnTransactionLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Transaction t = transactionList.get(position);
        
        // Display the user-defined title or fall back to the type
        holder.tvTitle.setText(t.title != null && !t.title.isEmpty() ? t.title : t.type);
        
        // Color-coding amounts: Green for Income (+), Red for Expense (-)
        if ("income".equalsIgnoreCase(t.type)) {
            holder.tvAmount.setText(String.format(Locale.getDefault(), "+RM%.2f", t.amount));
            holder.tvAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Green
        } else {
            holder.tvAmount.setText(String.format(Locale.getDefault(), "-RM%.2f", t.amount));
            holder.tvAmount.setTextColor(android.graphics.Color.parseColor("#E53935")); // Red
        }
        
        // Set formatted date and category
        holder.tvDate.setText(dateFormat.format(new Date(t.date)));
        holder.tvCategory.setText(t.category);
        
        // Conditionally show notes
        if (t.note != null && !t.note.trim().isEmpty()) {
            holder.tvNote.setText(t.note);
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        // Forward long-click event to the registered listener
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(t);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAmount, tvDate, tvCategory, tvNote;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvExpenseTitle);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            tvDate = itemView.findViewById(R.id.tvExpenseDate);
            tvCategory = itemView.findViewById(R.id.tvExpenseCategory);
            tvNote = itemView.findViewById(R.id.tvExpenseNote);
        }
    }
}
