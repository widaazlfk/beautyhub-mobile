package com.example.beautyhub.adapters;

import android.content.Context;
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
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Variant;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckoutItemsAdapter extends RecyclerView.Adapter<CheckoutItemsAdapter.ViewHolder> {

    private final Context context;
    private final List<CartItem> cartItems;

    // NEW: Product cache untuk stock validation
    private Map<String, Product> productCache;

    // Antara muka untuk callback ke Activity
    public interface OnQuantityChangeListener {
        void onQuantityChanged(int position, int newQuantity);
    }
    private OnQuantityChangeListener quantityChangeListener;

    public CheckoutItemsAdapter(Context context, List<CartItem> cartItems) {
        this.context = context;
        this.cartItems = cartItems;
        if (context instanceof OnQuantityChangeListener) {
            this.quantityChangeListener = (OnQuantityChangeListener) context;
        }
    }

    // NEW: Set product cache
    public void setProductCache(Map<String, Product> productCache) {
        this.productCache = productCache;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_checkout_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        if (item == null) return;

        // NEW: Display product name dengan variant jika ada
        holder.itemName.setText(item.getDisplayName());

        // NEW: Display variant info jika ada
        if (item.getVariantName() != null && !item.getVariantName().isEmpty()) {
            holder.tvVariantInfo.setVisibility(View.VISIBLE);
            holder.tvVariantInfo.setText(item.getVariantName());
        } else {
            holder.tvVariantInfo.setVisibility(View.GONE);
        }

        // Calculate total price untuk item ini
        double itemTotal = item.getPrice() * item.getQuantity();
        holder.itemPrice.setText(String.format(Locale.US, "RM %.2f", itemTotal));
        holder.itemQuantity.setText(String.format("x %d", item.getQuantity()));

        // Display seller info
        if (item.getSellerName() != null && !item.getSellerName().isEmpty()) {
            holder.sellerName.setText("Sold by: " + item.getSellerName());
            holder.sellerName.setVisibility(View.VISIBLE);

            // NEW: Show official badge untuk "system" seller
            if ("system".equals(item.getSellerId())) {
                holder.tvOfficialBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvOfficialBadge.setVisibility(View.GONE);
            }
        } else {
            holder.sellerName.setVisibility(View.GONE);
            holder.tvOfficialBadge.setVisibility(View.GONE);
        }

        // Load image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.product_placeholder)
                    .error(R.drawable.product_placeholder)
                    .into(holder.itemImage);
        } else {
            holder.itemImage.setImageResource(R.drawable.product_placeholder);
        }

        // NEW: Quantity controls
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        holder.btnIncrease.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() + 1;

            // Check stock limit
            if (productCache != null) {
                Product product = productCache.get(item.getProductId());
                if (product != null) {
                    int maxQuantity = getMaxQuantity(product, item.getVariantId());
                    if (newQuantity > maxQuantity) {
                        showStockLimitToast(maxQuantity, item.getDisplayName());
                        return;
                    }
                }
            }

            item.setQuantity(newQuantity);
            holder.tvQuantity.setText(String.valueOf(newQuantity));
            notifyItemChanged(position);

            if (quantityChangeListener != null) {
                quantityChangeListener.onQuantityChanged(position, newQuantity);
            }
        });

        holder.btnDecrease.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() - 1;
            if (newQuantity >= 1) {
                item.setQuantity(newQuantity);
                holder.tvQuantity.setText(String.valueOf(newQuantity));
                notifyItemChanged(position);

                if (quantityChangeListener != null) {
                    quantityChangeListener.onQuantityChanged(position, newQuantity);
                }
            }
        });

        // NEW: Check stock status
        checkStockStatus(holder, item);
    }

    // NEW: Helper method untuk dapatkan max quantity
    private int getMaxQuantity(Product product, String variantId) {
        if (variantId != null && !variantId.isEmpty()) {
            Variant variant = product.getVariantById(variantId);
            return variant != null ? variant.getStock() : 0;
        } else {
            return product.getStock();
        }
    }

    // NEW: Show stock limit toast
    private void showStockLimitToast(int maxQuantity, String itemName) {
        String message;
        if (maxQuantity <= 0) {
            message = itemName + " is out of stock";
        } else {
            message = "Maximum " + maxQuantity + " available for " + itemName;
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    // NEW: Check stock status dan update UI
    private void checkStockStatus(ViewHolder holder, CartItem item) {
        if (productCache == null) return;

        Product product = productCache.get(item.getProductId());
        if (product == null || !product.isActive()) {
            // Product unavailable
            holder.tvStockStatus.setVisibility(View.VISIBLE);
            holder.tvStockStatus.setText("Product unavailable");
            holder.tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.stock_out));
            holder.btnIncrease.setEnabled(false);
            holder.btnDecrease.setEnabled(false);
            return;
        }

        int availableStock = getMaxQuantity(product, item.getVariantId());

        if (availableStock <= 0) {
            // Out of stock
            holder.tvStockStatus.setVisibility(View.VISIBLE);
            holder.tvStockStatus.setText("Out of stock");
            holder.tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.stock_out));
            holder.btnIncrease.setEnabled(false);
            holder.btnDecrease.setEnabled(false);
        } else if (availableStock < item.getQuantity()) {
            // Quantity exceeds available stock
            holder.tvStockStatus.setVisibility(View.VISIBLE);
            holder.tvStockStatus.setText("Only " + availableStock + " available");
            holder.tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.stock_warning));
            holder.btnIncrease.setEnabled(false);
            holder.btnDecrease.setEnabled(true);
        } else if (availableStock <= 5) {
            // Low stock
            holder.tvStockStatus.setVisibility(View.VISIBLE);
            holder.tvStockStatus.setText("Low stock: " + availableStock + " left");
            holder.tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.stock_low));
            holder.btnIncrease.setEnabled(true);
            holder.btnDecrease.setEnabled(true);
        } else {
            // In stock
            holder.tvStockStatus.setVisibility(View.GONE);
            holder.btnIncrease.setEnabled(true);
            holder.btnDecrease.setEnabled(true);
        }

        // Disable increase button jika quantity sudah mencapai max
        if (availableStock <= item.getQuantity()) {
            holder.btnIncrease.setEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName, itemPrice, itemQuantity, sellerName;

        // NEW: Views untuk variant info dan quantity controls
        TextView tvVariantInfo, tvQuantity, tvStockStatus, tvOfficialBadge;
        View btnIncrease, btnDecrease;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.iv_checkout_item_image);
            itemName = itemView.findViewById(R.id.tv_checkout_item_name);
            itemPrice = itemView.findViewById(R.id.tv_checkout_item_price);
            itemQuantity = itemView.findViewById(R.id.tv_checkout_item_quantity_total);
            sellerName = itemView.findViewById(R.id.tv_checkout_item_seller_name);

            // NEW: Initialize new views
            tvVariantInfo = itemView.findViewById(R.id.tv_checkout_item_variant);
            tvQuantity = itemView.findViewById(R.id.tv_checkout_quantity);
            tvStockStatus = itemView.findViewById(R.id.tv_checkout_stock_status);
            tvOfficialBadge = itemView.findViewById(R.id.tv_checkout_official_badge);
            btnIncrease = itemView.findViewById(R.id.btn_checkout_increase);
            btnDecrease = itemView.findViewById(R.id.btn_checkout_decrease);
        }
    }
}