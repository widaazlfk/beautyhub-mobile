package com.example.beautyhub.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beautyhub.R;
// ▼▼▼ PERUBAHAN 1: Tukar import dari CartItem kepada OrderItem ▼▼▼
import com.example.beautyhub.models.OrderItem;
import com.example.beautyhub.models.Order;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SellerOrderAdapter extends RecyclerView.Adapter<SellerOrderAdapter.OrderViewHolder> {

    private final Context context;
    private final List<Order> orderList;
    private final OnOrderItemClickListener listener;

    public interface OnOrderItemClickListener {
        void onOrderItemClick(Order order);
    }

    public SellerOrderAdapter(Context context, List<Order> orderList, OnOrderItemClickListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.s_item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        if (order != null) {
            holder.bind(order, listener);
        }
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        // Pastikan ID ini sepadan dengan s_item_order.xml
        private final TextView tvOrderId, tvOrderStatus, tvOrderDate, tvOrderItemsPreview, tvOrderTotal;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            // Inisialisasi TextViews di dalam constructor, amalan terbaik
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvOrderItemsPreview = itemView.findViewById(R.id.tv_order_items_preview);
            tvOrderTotal = itemView.findViewById(R.id.tv_order_total);
        }

        void bind(final Order order, final OnOrderItemClickListener listener) {
            // Set ID Pesanan
            tvOrderId.setText(String.format("Order #%s", getShortOrderId(order.getOrderId())));

            // Set Tarikh Pesanan
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            tvOrderDate.setText(dateFormat.format(new Date(order.getTimestamp())));

            // Set Jumlah Harga (menggunakan format mata wang Malaysia)
            tvOrderTotal.setText(String.format(Locale.US, "RM %.2f", order.getTotalPayment()));

            // Set Pratonton Item
            tvOrderItemsPreview.setText(generateItemsPreview(order.getItems()));

            // Set Status Pesanan
            String status = order.getStatus();
            tvOrderStatus.setText(status);
            updateStatusBackground(status);

            // Set listener untuk keseluruhan item view
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderItemClick(order);
                }
            });
        }

        // ▼▼▼ PERUBAHAN 2: Kemas kini kaedah untuk menggunakan OrderItem ▼▼▼
        private String generateItemsPreview(List<OrderItem> items) {
            if (items == null || items.isEmpty()) {
                return "No items in this order.";
            }

            StringBuilder preview = new StringBuilder();
            int count = 0;
            for (OrderItem item : items) {
                if (count < 2) { // Tunjuk 2 item pertama
                    preview.append(String.format(Locale.US, "%dx %s\n", item.getQuantity(), item.getProductName()));
                }
                count++;
            }
            if (items.size() > 2) {
                preview.append(String.format(Locale.US, "...and %d more", items.size() - 2));
            }
            // Buang newline terakhir jika ada
            return preview.toString().trim();
        }

        private String getShortOrderId(String orderId) {
            if (orderId != null && orderId.length() > 7) {
                return orderId.substring(orderId.length() - 7).toUpperCase();
            }
            return orderId != null ? orderId.toUpperCase() : "";
        }

        // ▼▼▼ PERUBAHAN 3: Kaedah kemas kini latar belakang yang lebih mantap ▼▼▼
        private void updateStatusBackground(String status) {
            if (status == null) status = "pending";

            int colorResId;
            switch (status.toLowerCase()) {
                case "shipped": colorResId = R.color.status_shipped; break;
                case "completed": case "delivered": colorResId = R.color.status_completed; break;
                case "cancelled": colorResId = R.color.status_cancelled; break;
                case "pending": default: colorResId = R.color.status_pending; break;
            }

            // Cara yang lebih selamat untuk menukar warna drawable tanpa bergantung pada jenis fail
            if (tvOrderStatus.getBackground() instanceof GradientDrawable) {
                GradientDrawable background = (GradientDrawable) tvOrderStatus.getBackground().mutate();
                background.setColor(ContextCompat.getColor(context, colorResId));
            } else {
                // Sebagai fallback, tetapkan warna latar belakang sahaja
                tvOrderStatus.setBackgroundColor(ContextCompat.getColor(context, colorResId));
            }
        }
    }
}
