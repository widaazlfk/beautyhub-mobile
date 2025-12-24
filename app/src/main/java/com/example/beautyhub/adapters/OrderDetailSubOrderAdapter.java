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
import com.example.beautyhub.seller.SellerProfileActivity;
import com.example.beautyhub.models.OrderItem;
import com.example.beautyhub.models.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderDetailSubOrderAdapter extends RecyclerView.Adapter<OrderDetailSubOrderAdapter.SubOrderViewHolder> {

    private final Context context;
    private final List<Order> subOrderList;
    private final String mainOrderId;
    private final ActivityResultLauncher<Intent> addReviewLauncher;

    public OrderDetailSubOrderAdapter(Context context, List<Order> subOrderList, String mainOrderId, ActivityResultLauncher<Intent> addReviewLauncher) {
        this.context = context;
        this.subOrderList = subOrderList;
        this.mainOrderId = mainOrderId;
        this.addReviewLauncher = addReviewLauncher;
    }

    @NonNull
    @Override
    public SubOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        private final ImageView iconStore;
        private final TextView tvSellerName;
        private final ImageView ivArrowRight;
        private final RecyclerView rvItems;

        public SubOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSellerProfile = itemView.findViewById(R.id.layout_seller_profile);
            iconStore = itemView.findViewById(R.id.icon_store);
            tvSellerName = itemView.findViewById(R.id.tv_sub_order_seller_name);
            ivArrowRight = itemView.findViewById(R.id.iv_arrow_right);
            rvItems = itemView.findViewById(R.id.rv_sub_order_items);

            // Set click listener untuk keseluruhan layout
            layoutSellerProfile.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Order subOrder = subOrderList.get(position);
                    if (subOrder.getSellerId() != null && !subOrder.getSellerId().isEmpty()) {
                        Intent intent = new Intent(context, SellerProfileActivity.class);
                        intent.putExtra("SELLER_ID", subOrder.getSellerId());
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "Seller information not available", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        public void bind(Order subOrder) {
            // Tetapkan nama penjual jika ada
            if (subOrder.getSellerName() != null && !subOrder.getSellerName().isEmpty()) {
                tvSellerName.setText("Order from: " + subOrder.getSellerName());
                tvSellerName.setVisibility(View.VISIBLE);
                layoutSellerProfile.setVisibility(View.VISIBLE);
                iconStore.setVisibility(View.VISIBLE);
                ivArrowRight.setVisibility(View.VISIBLE);
            } else {
                tvSellerName.setVisibility(View.GONE);
                layoutSellerProfile.setVisibility(View.GONE);
                iconStore.setVisibility(View.GONE);
                ivArrowRight.setVisibility(View.GONE);
            }

            // Kod untuk adapter dalaman
            if (subOrder.getItems() != null && !subOrder.getItems().isEmpty()) {
                ArrayList<OrderItem> items = new ArrayList<>(subOrder.getItems());

                OrderDetailItemAdapter itemsAdapter = new OrderDetailItemAdapter(
                        context,
                        items,
                        mainOrderId,
                        subOrder.getStatus(),
                        addReviewLauncher
                );
                rvItems.setLayoutManager(new LinearLayoutManager(context));
                rvItems.setAdapter(itemsAdapter);
                rvItems.setNestedScrollingEnabled(false);
                rvItems.setVisibility(View.VISIBLE);
            } else {
                rvItems.setVisibility(View.GONE);
            }
        }
    }
}