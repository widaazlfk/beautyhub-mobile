package com.example.beautyhub.adapters;

import android.content.Context;
import android.graphics.Paint;
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
import com.example.beautyhub.models.Product;
import java.util.List;
import java.util.Locale;

public class SellerProductAdapter extends RecyclerView.Adapter<SellerProductAdapter.SellerProductViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final OnProductActionListener listener;

    public interface OnProductActionListener {
        void onProductClick(Product product);
        void onEditClick(Product product);
        void onDeleteClick(Product product, int position);
    }

    public SellerProductAdapter(Context context, List<Product> productList, OnProductActionListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SellerProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure this matches your XML filename
        View view = LayoutInflater.from(context).inflate(R.layout.item_seller_product, parent, false);
        return new SellerProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SellerProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product, listener, position);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    // --- Helper Methods for List Management ---

    public void removeItem(int position) {
        if (position >= 0 && position < productList.size()) {
            productList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, productList.size());
        }
    }

    public void updateList(List<Product> newList) {
        productList.clear();
        productList.addAll(newList);
        notifyDataSetChanged();
    }

    // --- ViewHolder Class ---

    static class SellerProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage, btnEdit, btnDelete;
        TextView tvProductName, tvProductPrice, tvDiscountPrice, tvStockStatus;

        public SellerProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.iv_product_image);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
            tvDiscountPrice = itemView.findViewById(R.id.tv_product_discount_price); // Added for consistency
            tvStockStatus = itemView.findViewById(R.id.tv_stock_status);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(final Product product, final OnProductActionListener listener, final int position) {
            tvProductName.setText(product.getName());

            // Price Logic: Handling Normal Price vs Discount Price
            if (product.getDiscountPrice() > 0 && product.getDiscountPrice() < product.getPrice()) {
                tvDiscountPrice.setVisibility(View.VISIBLE);
                tvDiscountPrice.setText(String.format(Locale.US, "RM %.2f", product.getDiscountPrice()));

                // Strike-through original price
                tvProductPrice.setText(String.format(Locale.US, "RM %.2f", product.getPrice()));
                tvProductPrice.setPaintFlags(tvProductPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvProductPrice.setTextSize(12f);
            } else {
                tvDiscountPrice.setVisibility(View.GONE);
                tvProductPrice.setText(String.format(Locale.US, "RM %.2f", product.getPrice()));
                tvProductPrice.setPaintFlags(tvProductPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvProductPrice.setTextSize(14f);
            }

            // Update Stock Status with Colors
            updateStockStatus(product.getStock());

            // Load Image with Glide
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(product.getImageUrls().get(0))
                        .placeholder(R.drawable.product_placeholder)
                        .error(R.drawable.product_placeholder)
                        .centerCrop()
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.product_placeholder);
            }

            // Click Listeners
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onProductClick(product));
                btnEdit.setOnClickListener(v -> listener.onEditClick(product));
                btnDelete.setOnClickListener(v -> listener.onDeleteClick(product, position));
            }
        }

        private void updateStockStatus(int stock) {
            Context context = itemView.getContext();
            if (stock > 10) {
                tvStockStatus.setText("Stock: " + stock);
                tvStockStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                // Optional: tvStockStatus.setBackgroundResource(R.drawable.bg_stock_high);
            } else if (stock > 0) {
                tvStockStatus.setText("Low Stock: " + stock);
                tvStockStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
                // Optional: tvStockStatus.setBackgroundResource(R.drawable.bg_stock_low);
            } else {
                tvStockStatus.setText("Out of Stock");
                tvStockStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                // Optional: tvStockStatus.setBackgroundResource(R.drawable.bg_stock_out);
            }
        }
    }
}