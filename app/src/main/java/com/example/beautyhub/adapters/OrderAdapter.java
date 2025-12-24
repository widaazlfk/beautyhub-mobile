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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final Context context;
    private final List<Order> orderList;
    private final OnOrderItemClickListener listener;

    public interface OnOrderItemClickListener {
        void onOrderItemClick(Order order);
    }

    public OrderAdapter(Context context, List<Order> orderList, OnOrderItemClickListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
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
        return orderList.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderStatus, tvOrderDate, tvOrderItemsPreview, tvOrderTotal;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvOrderItemsPreview = itemView.findViewById(R.id.tv_order_items_preview);
            tvOrderTotal = itemView.findViewById(R.id.tv_order_total);
        }

        void bind(final Order order, final OnOrderItemClickListener clickListener) {
            tvOrderId.setText(String.format("Order #%s", getShortOrderId(order.getOrderId())));
            tvOrderStatus.setText(order.getStatus());
            updateStatusBackground(order.getStatus());

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            tvOrderDate.setText(sdf.format(new Date(order.getTimestamp())));

            tvOrderTotal.setText(String.format(Locale.US, "RM %.2f", order.getTotalPayment()));

            // ▼▼▼ PERUBAHAN 2: Logik baharu untuk pratonton item dari sub-pesanan ▼▼▼
            // Kumpul semua item dari semua sub-pesanan ke dalam satu senarai.
            List<OrderItem> allItems = new ArrayList<>();
            if (order.getSubOrders() != null && !order.getSubOrders().isEmpty()) {
                for (Order subOrder : order.getSubOrders().values()) {
                    if (subOrder.getItems() != null) {
                        allItems.addAll(subOrder.getItems());
                    }
                }
            }

            // Bina string pratonton item
            StringBuilder itemsPreview = new StringBuilder();
            if (!allItems.isEmpty()) {
                int count = 0;
                for (OrderItem item : allItems) {
                    if (count < 2) { // Tunjuk 2 item pertama
                        itemsPreview.append(String.format("%dx - %s\n", item.getQuantity(), item.getProductName()));
                    }
                    count++;
                }
                if (allItems.size() > 2) {
                    itemsPreview.append(String.format("...and %d more item(s)", allItems.size() - 2));
                }
            } else {
                itemsPreview.append("No items in this order.");
            }
            tvOrderItemsPreview.setText(itemsPreview.toString().trim());

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onOrderItemClick(order);
                }
            });
        }

        private void updateStatusBackground(String status) {
            if (status == null) status = "pending";

            int colorResId;
            switch (status.toLowerCase()) {
                case "shipped": colorResId = R.color.status_shipped; break;
                case "completed": case "delivered": colorResId = R.color.status_completed; break;
                case "cancelled": colorResId = R.color.status_cancelled; break;
                case "pending": default: colorResId = R.color.status_pending; break;
            }

            if (tvOrderStatus.getBackground() instanceof GradientDrawable) {
                GradientDrawable background = (GradientDrawable) tvOrderStatus.getBackground().mutate();
                background.setColor(ContextCompat.getColor(context, colorResId));
            } else {
                tvOrderStatus.setBackgroundColor(ContextCompat.getColor(context, colorResId));
            }
        }

        private String getShortOrderId(String orderId) {
            if (orderId != null && orderId.length() > 7) {
                return orderId.substring(orderId.length() - 7).toUpperCase();
            }
            return orderId != null ? orderId.toUpperCase() : "";
        }
    }
}
