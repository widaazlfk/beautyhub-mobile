package com.example.beautyhub.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
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
        private final ImageView ivOrderImage;
        private final TextView tvOrderId, tvOrderStatus, tvOrderDate, tvOrderItemsPreview, tvOrderTotal;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivOrderImage = itemView.findViewById(R.id.iv_order_image);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvOrderItemsPreview = itemView.findViewById(R.id.tv_order_items_preview);
            tvOrderTotal = itemView.findViewById(R.id.tv_order_total);
        }

        void bind(final Order order, final OnOrderItemClickListener listener) {
            // 1. Set Order ID (Shortened)
            String fullOrderId = order.getOrderId();
            String displayOrderId = "N/A";

            if (fullOrderId != null && !fullOrderId.isEmpty()) {
                // Logik memendekkan ID: Ambil 8 aksara pertama dan buang simbol '-'
                String cleanId = fullOrderId.replace("-", "");
                displayOrderId = cleanId.substring(0, Math.min(cleanId.length(), 8)).toUpperCase();
            }
            tvOrderId.setText(String.format("Order #%s", displayOrderId));

            // 2. Set Date
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            tvOrderDate.setText(dateFormat.format(new Date(order.getOrderDate())));

            // 3. Set Total Amount
            tvOrderTotal.setText(String.format(Locale.US, "RM %.2f", order.getTotalAmount()));

            // 4. Set Items Preview Text
            tvOrderItemsPreview.setText(generateItemsPreview(order.getOrderItems()));

            // 5. Load Image
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                OrderItem firstItem = order.getOrderItems().get(0);
                String imageUrl = firstItem.getImageUrls();

                Glide.with(context)
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.product_placeholder)
                        .error(R.drawable.product_placeholder)
                        .into(ivOrderImage);
            } else {
                ivOrderImage.setImageResource(R.drawable.product_placeholder);
            }

            // 6. Set Status and Background Color
            String status = order.getStatus();
            tvOrderStatus.setText(status != null ? status : "Pending");
            updateStatusBackground(status);

            // 7. Click Listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderItemClick(order); // Tetap hantar object 'order' dengan ID penuh
                }
            });
        }

        private String generateItemsPreview(List<OrderItem> items) {
            if (items == null || items.isEmpty()) {
                return "No items in this order.";
            }

            StringBuilder preview = new StringBuilder();
            int limit = 2;
            for (int i = 0; i < Math.min(items.size(), limit); i++) {
                OrderItem item = items.get(i);
                if (item != null) {
                    preview.append(String.format(Locale.US, "%dx %s\n", item.getQuantity(), item.getProductName()));
                }
            }

            if (items.size() > limit) {
                preview.append(String.format(Locale.US, "...and %d more", items.size() - limit));
            }
            return preview.toString().trim();
        }

        private void updateStatusBackground(String status) {
            if (status == null) status = "pending";

            int colorResId;
            switch (status.toLowerCase()) {
                case "shipped":
                    colorResId = R.color.status_shipped;
                    break;
                case "completed":
                case "delivered":
                    colorResId = R.color.status_completed;
                    break;
                case "cancelled":
                    colorResId = R.color.status_cancelled;
                    break;
                default:
                    colorResId = R.color.status_pending;
                    break;
            }

            if (tvOrderStatus.getBackground() instanceof GradientDrawable) {
                GradientDrawable background = (GradientDrawable) tvOrderStatus.getBackground().mutate();
                background.setColor(ContextCompat.getColor(context, colorResId));
            } else {
                tvOrderStatus.setBackgroundColor(ContextCompat.getColor(context, colorResId));
            }
        }
    }
}
