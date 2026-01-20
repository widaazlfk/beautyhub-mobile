package com.example.beautyhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beautyhub.R;
import com.example.beautyhub.models.FinanceModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FinanceAdapter extends RecyclerView.Adapter<FinanceAdapter.ViewHolder> {
    private List<FinanceModel> list;
    private OnItemClickListener listener; // Tambah ini

    // Interface untuk klik
    public interface OnItemClickListener {
        void onItemClick(String orderId);
    }

    public FinanceAdapter(List<FinanceModel> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_finance_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FinanceModel model = list.get(position);
        String fullOrderId = model.getOrderId();

        // 1. Logik memendekkan ID untuk paparan (8 aksara pertama)
        String displayId = "Unknown";
        if (fullOrderId != null && !fullOrderId.isEmpty()) {
            // Ambil 8 aksara pertama, buang tanda "-" jika ada di depan, dan tukar ke Uppercase
            String cleanId = fullOrderId.startsWith("-") ? fullOrderId.substring(1) : fullOrderId;
            displayId = cleanId.length() > 8 ? cleanId.substring(0, 8).toUpperCase() : cleanId.toUpperCase();
        }

        holder.tvOrderId.setText("Order #" + displayId);

        // 2. Format Jumlah (RM)
        holder.tvAmount.setText(String.format(Locale.US, "+RM %.2f", model.getAmount()));

        // 3. Format Tarikh
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(model.getTimestamp())));

        // 4. Set klik pada item (MENGGUNAKAN ID PENUH)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && fullOrderId != null) {
                // Kita hantar fullOrderId supaya Activity boleh tarik data penuh dari Firebase
                listener.onItemClick(fullOrderId);
            }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvDate, tvAmount;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvDate = itemView.findViewById(R.id.tv_order_date);
            tvAmount = itemView.findViewById(R.id.tv_revenue_amount);
        }
    }
}