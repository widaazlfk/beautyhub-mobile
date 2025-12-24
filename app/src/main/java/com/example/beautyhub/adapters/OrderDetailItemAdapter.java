// Buka dan gantikan keseluruhan kandungan fail:
// C:/Users/widaa/beautyhub/app/src/main/java/com/example/beautyhub/adapters/OrderDetailItemAdapter.java

package com.example.beautyhub.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.buyer.AddReviewActivity;
// ▼▼▼ PERUBAHAN 1: Guna OrderItem, buang CartItem ▼▼▼
import com.example.beautyhub.models.OrderItem;

import java.util.ArrayList;
import java.util.Locale;

public class OrderDetailItemAdapter extends RecyclerView.Adapter<OrderDetailItemAdapter.ItemViewHolder> {

    private final Context context;
    // ▼▼▼ PERUBAHAN 2: Tukar jenis senarai kepada OrderItem ▼▼▼
    private final ArrayList<OrderItem> itemList;
    private final String mainOrderId;
    private final String orderStatus;
    private final ActivityResultLauncher<Intent> addReviewLauncher;

    // ▼▼▼ PERUBAHAN 3: Kemas kini constructor untuk menerima ArrayList<OrderItem> ▼▼▼
    public OrderDetailItemAdapter(Context context, ArrayList<OrderItem> itemList, String mainOrderId, String orderStatus, ActivityResultLauncher<Intent> addReviewLauncher) {
        this.context = context;
        this.itemList = itemList;
        this.mainOrderId = mainOrderId;
        this.orderStatus = orderStatus;
        this.addReviewLauncher = addReviewLauncher;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_detail, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        // ▼▼▼ PERUBAHAN 4: Guna OrderItem di sini ▼▼▼
        OrderItem item = itemList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProductImage;
        private final TextView tvProductName, tvProductPrice, tvProductQuantity;
        private final Button btnAddReview;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            // ID ini mesti sepadan dengan item_order_detail.xml
            ivProductImage = itemView.findViewById(R.id.iv_order_detail_item_image);
            tvProductName = itemView.findViewById(R.id.tv_order_detail_item_name);
            tvProductPrice = itemView.findViewById(R.id.tv_order_detail_item_price);
            tvProductQuantity = itemView.findViewById(R.id.tv_order_detail_item_quantity);
            btnAddReview = itemView.findViewById(R.id.btn_add_review);
        }

        // ▼▼▼ PERUBAHAN 5: Kemas kini kaedah bind untuk menerima OrderItem ▼▼▼
        void bind(OrderItem item) {
            tvProductName.setText(item.getProductName());
            tvProductPrice.setText(String.format(Locale.US, "RM %.2f", item.getPrice()));
            tvProductQuantity.setText("x" + item.getQuantity());

            if (item.getProductImageUrl() != null && !item.getProductImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(item.getProductImageUrl())
                        .placeholder(R.drawable.product_placeholder) // Pastikan drawable ini wujud
                        .error(R.drawable.product_placeholder)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.product_placeholder);
            }

            // Logik butang review
            if ("Completed".equalsIgnoreCase(orderStatus)) {
                btnAddReview.setVisibility(View.VISIBLE);
                btnAddReview.setText("Add Review");
                btnAddReview.setEnabled(true);
            } else {
                btnAddReview.setVisibility(View.GONE);
            }

            btnAddReview.setOnClickListener(v -> {
                Intent intent = new Intent(context, AddReviewActivity.class);
                intent.putExtra("PRODUCT_ID", item.getProductId());
                intent.putExtra("ORDER_ID", mainOrderId);
                intent.putExtra("PRODUCT_NAME", item.getProductName());
                intent.putExtra("PRODUCT_IMAGE_URL", item.getProductImageUrl());

                addReviewLauncher.launch(intent);
            });
        }
    }
}
