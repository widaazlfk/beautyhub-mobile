package com.example.beautyhub.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log; // Tambah log untuk debug
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
import com.example.beautyhub.buyer.ProductDetailActivity;
import com.example.beautyhub.models.OrderItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class OrderDetailItemAdapter extends RecyclerView.Adapter<OrderDetailItemAdapter.ItemViewHolder> {

    private final Context context;
    private final ArrayList<OrderItem> itemList;
    private final String mainOrderId;
    private final String orderStatus;
    private final ActivityResultLauncher<Intent> addReviewLauncher;

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
        OrderItem item = itemList.get(position);
        holder.bind(item);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", item.getProductId());
            context.startActivity(intent);
        });
    }

    private void checkIfItemReviewed(String productId, Button btnReview) {
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("Reviews");
        reviewsRef.orderByChild("orderId").equalTo(mainOrderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isReviewed = false;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String pId = ds.child("productId").getValue(String.class);
                            if (productId != null && productId.equals(pId)) {
                                isReviewed = true;
                                break;
                            }
                        }

                        if (isReviewed) {
                            btnReview.setText("Reviewed");
                            btnReview.setEnabled(false);
                            btnReview.setAlpha(0.5f);
                        } else {
                            btnReview.setText("Add Review");
                            btnReview.setEnabled(true);
                            btnReview.setAlpha(1.0f);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProductImage;
        private final TextView tvProductName, tvProductPrice, tvProductQuantity;
        private final Button btnAddReview;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.iv_order_detail_item_image);
            tvProductName = itemView.findViewById(R.id.tv_order_detail_item_name);
            tvProductPrice = itemView.findViewById(R.id.tv_order_detail_item_price);
            tvProductQuantity = itemView.findViewById(R.id.tv_order_detail_item_quantity);
            btnAddReview = itemView.findViewById(R.id.btn_add_review);
        }

        void bind(OrderItem item) {
            tvProductName.setText(item.getProductName());
            tvProductPrice.setText(String.format(Locale.US, "RM %.2f", item.getPrice()));
            tvProductQuantity.setText("x" + item.getQuantity());

            // Ambil URL imej
            String imageUrl = item.getImageUrls();

            // Log untuk debug (Semak Logcat dengan filter "IMAGE_DEBUG")
            Log.d("IMAGE_DEBUG", "Product: " + item.getProductName() + " | URL: " + imageUrl);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.product_placeholder)
                        .error(R.drawable.product_placeholder)
                        .centerCrop()
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.product_placeholder);
            }

            // Logik butang review
            if ("Completed".equalsIgnoreCase(orderStatus)) {
                btnAddReview.setVisibility(View.VISIBLE);
                checkIfItemReviewed(item.getProductId(), btnAddReview);
            } else {
                btnAddReview.setVisibility(View.GONE);
            }

            // Set listener di dalam bind supaya 'item' boleh dicapai
            btnAddReview.setOnClickListener(v -> {
                Intent intent = new Intent(context, AddReviewActivity.class);
                intent.putExtra("PRODUCT_ID", item.getProductId());
                intent.putExtra("ORDER_ID", mainOrderId);
                intent.putExtra("PRODUCT_NAME", item.getProductName());
                intent.putExtra("PRODUCT_IMAGE_URL", item.getImageUrls());
                addReviewLauncher.launch(intent);
            });
        }
        }
    }

