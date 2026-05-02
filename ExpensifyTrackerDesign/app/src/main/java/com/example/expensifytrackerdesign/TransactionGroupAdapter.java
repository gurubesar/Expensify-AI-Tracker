package com.example.expensifytrackerdesign;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Advanced adapter that groups transactions by date with headers (e.g., "Today", "Yesterday").
 * Supports pagination through a "Next Page" footer button.
 */
public class TransactionGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 2;

    private List<Object> items = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private int visibleCount = 10; // Initial number of transactions to show
    private boolean showFooter = true;
    
    private SimpleDateFormat headerFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
    private SimpleDateFormat itemTimeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    
    private OnTransactionLongClickListener longClickListener;

    /**
     * Interface to handle long-press events on transaction items.
     */
    public interface OnTransactionLongClickListener {
        void onLongClick(Transaction transaction);
    }

    public void setOnTransactionLongClickListener(OnTransactionLongClickListener listener) {
        this.longClickListener = listener;
    }

    /**
     * Updates the data source for the adapter and resets pagination if necessary.
     */
    public void setTransactions(List<Transaction> transactions) {
        this.allTransactions = transactions;
        if (showFooter) {
            this.visibleCount = 10; // Reset pagination for new data
        } else {
            this.visibleCount = transactions.size(); // Show everything if footer is disabled
        }
        updateDisplayItems();
    }

    /**
     * Enables or disables the pagination footer.
     */
    public void setShowFooter(boolean showFooter) {
        this.showFooter = showFooter;
    }

    /**
     * Increases the number of visible transactions and refreshes the list.
     */
    public void loadMore() {
        visibleCount += 10;
        updateDisplayItems();
    }

    /**
     * Logic to transform the flat list of Transactions into a grouped list with Headers and Footer.
     */
    private void updateDisplayItems() {
        items.clear();
        if (allTransactions.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Subset of transactions based on visibleCount
        List<Transaction> subset = allTransactions.subList(0, Math.min(visibleCount, allTransactions.size()));

        String lastDate = "";
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        for (Transaction t : subset) {
            String dateStr = headerFormat.format(new Date(t.date));
            if (!dateStr.equals(lastDate)) {
                // Determine header text: "Today", "Yesterday", or "Day, Date"
                String headerText = dateStr;
                Calendar tCal = Calendar.getInstance();
                tCal.setTimeInMillis(t.date);
                
                if (isSameDay(tCal, today)) {
                    headerText = "Today";
                } else if (isSameDay(tCal, yesterday)) {
                    headerText = "Yesterday";
                } else {
                    headerText = dayFormat.format(new Date(t.date)) + ", " + dateStr;
                }
                
                items.add(headerText);
                lastDate = dateStr;
            }
            items.add(t);
        }

        // Add footer if more transactions are available to load
        if (showFooter && visibleCount < allTransactions.size()) {
            items.add(Boolean.TRUE); // Using a Boolean as a sentinel for footer
        }
        
        notifyDataSetChanged();
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof String) return TYPE_HEADER;
        if (item instanceof Transaction) return TYPE_ITEM;
        return TYPE_FOOTER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_footer, parent, false);
            return new FooterViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeader.setText((String) items.get(position));
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).btnNext.setOnClickListener(v -> loadMore());
        } else {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            Transaction t = (Transaction) items.get(position);
            
            // Standard transaction item binding
            itemHolder.tvTitle.setText(t.title != null && !t.title.isEmpty() ? t.title : t.type);
            
            if ("income".equalsIgnoreCase(t.type)) {
                itemHolder.tvAmount.setText(String.format(Locale.getDefault(), "+RM%.2f", t.amount));
                itemHolder.tvAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            } else {
                itemHolder.tvAmount.setText(String.format(Locale.getDefault(), "-RM%.2f", t.amount));
                itemHolder.tvAmount.setTextColor(android.graphics.Color.parseColor("#E53935"));
            }
            
            // For items, we only show the time
            itemHolder.tvDate.setText(itemTimeFormat.format(new Date(t.date)));
            itemHolder.tvCategory.setText(t.category);

            if (t.note != null && !t.note.trim().isEmpty()) {
                itemHolder.tvNote.setText(t.note);
                itemHolder.tvNote.setVisibility(View.VISIBLE);
            } else {
                itemHolder.tvNote.setVisibility(View.GONE);
            }

            itemHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onLongClick(t);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvDateHeader);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        android.widget.Button btnNext;
        FooterViewHolder(View itemView) {
            super(itemView);
            btnNext = itemView.findViewById(R.id.btnNextPage);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAmount, tvDate, tvCategory, tvNote;
        ItemViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvExpenseTitle);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            tvDate = itemView.findViewById(R.id.tvExpenseDate);
            tvCategory = itemView.findViewById(R.id.tvExpenseCategory);
            tvNote = itemView.findViewById(R.id.tvExpenseNote);
        }
    }
}
