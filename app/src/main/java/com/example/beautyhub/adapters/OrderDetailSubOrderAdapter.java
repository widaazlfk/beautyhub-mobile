package com.example.beautyhub.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.buyer.ShopViewActivity;
import com.example.beautyhub.models.OrderItem;
import com.example.beautyhub.models.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderDetailSubOrderAdapter extends RecyclerView.Adapter<OrderDetailSubOrderAdapter.SubOrderViewHolder> {

    private final Context context;
    private final List<Order> subOrderList;
    private final String mainOrderId;
    private final ActivityResultLauncher<Intent> addReviewLauncher;

    public OrderDetailSubOrderAdapter(Context context, List<Order> subOrderList, String mainOrderId, String mainOrderStatus, ActivityResultLauncher<Intent> addReviewLauncher) {
        this.context = context;
        this.subOrderList = subOrderList;
        this.mainOrderId = mainOrderId;
        this.addReviewLauncher = addReviewLauncher;
    }

    @NonNull
    @Override
    public SubOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Pastikan layout item_sub_order mempunyai TextView untuk status (tv_sub_order_status)
        View view = LayoutInflater.from(context).inflate(R.layout.item_sub_order, parent, false);
        return new SubOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubOrderViewHolder holder, int position) {
        Order subOrder = subOrderList.get(position);
        holder.bind(subOrder);
    }

    @Override
    public int getItemCount() {
        return subOrderList.size();
    }

    class SubOrderViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout layoutSellerProfile;
        private final TextView tvSellerName, tvSubOrderStatus; // Tambah tvSubOrderStatus
        private final RecyclerView rvItems;

        public SubOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSellerProfile = itemView.findViewById(R.id.layout_seller_profile);
            tvSellerName = itemView.findViewById(R.id.tv_sub_order_seller_name);
            tvSubOrderStatus = itemView.findViewById(R.id.tv_sub_order_status); // Pastikan ID ini ada di XML
            rvItems = itemView.findViewById(R.id.rv_sub_order_items);

            layoutSellerProfile.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Order subOrder = subOrderList.get(position);
                    String sellerId = subOrder.getSellerId();
                    if (sellerId != null && !sellerId.isEmpty()) {
                        Intent intent = new Intent(context, ShopViewActivity.class);
                        intent.putExtra("SELLER_ID", sellerId);
                        context.startActivity(intent);
                    }
                }
            });
        }

        public void bind(Order subOrder) {
            // 1. Papar Nama Penjual
            if (subOrder.getSellerName() != null) {
                tvSellerName.setText("Seller: " + subOrder.getSellerName());
                layoutSellerProfile.setVisibility(View.VISIBLE);
            }

            // 2. PAPAR STATUS SPESIFIK PENJUAL (Penting untuk Multiple Seller)
            String currentSubStatus = subOrder.getStatus();
            if (currentSubStatus != null) {
                tvSubOrderStatus.setText(currentSubStatus);
                tvSubOrderStatus.setVisibility(View.VISIBLE);

                // Anda boleh tukar warna teks mengikut status di sini
                if (currentSubStatus.equalsIgnoreCase("Shipped")) {
                    tvSubOrderStatus.setTextColor(context.getResources().getColor(R.color.status_shipped));
                } else if (currentSubStatus.equalsIgnoreCase("Completed")) {
                    tvSubOrderStatus.setTextColor(context.getResources().getColor(R.color.status_completed));
                }
            }

            // 3. Set Item Adapter menggunakan status Sub-Order
            if (subOrder.getOrderItems() != null) {
                ArrayList<OrderItem> items = new ArrayList<>(subOrder.getOrderItems());

                // Guna subOrder.getStatus() dan bukannya mainOrderStatus
                OrderDetailItemAdapter itemsAdapter = new OrderDetailItemAdapter(
                        context,
                        items,
                        mainOrderId,
                        currentSubStatus, // <--- Ini kunci penyelesaiannya
                        addReviewLauncher
                );
                rvItems.setLayoutManager(new LinearLayoutManager(context));
                rvItems.setAdapter(itemsAdapter);
                rvItems.setNestedScrollingEnabled(false);
            }
        }
    }
}
