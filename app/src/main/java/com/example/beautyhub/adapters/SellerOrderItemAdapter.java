package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.models.OrderItem;

import java.util.List;
import java.util.Locale;

public class SellerOrderItemAdapter extends RecyclerView.Adapter<SellerOrderItemAdapter.ItemViewHolder> {

    private final Context context;
    private final List<OrderItem> itemList;

    public SellerOrderItemAdapter(Context context, List<OrderItem> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.s_item_order_detail, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        OrderItem item = itemList.get(position);
        if (item != null) {
            holder.bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProductImage;
        private final TextView tvProductName, tvProductPrice, tvProductQuantity;
        private final View btnAddReview;

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
            tvProductQuantity.setText(String.format(Locale.US, "x%d", item.getQuantity()));

            // PENGAMBILAN GAMBAR - getImageUrls() memulangkan String URL
            String imageUrl = item.getImageUrls();

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.product_placeholder)
                        .error(R.drawable.product_placeholder)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.product_placeholder);
            }

            if (btnAddReview != null) {
                btnAddReview.setVisibility(View.GONE);
            }
        }
    }
}
