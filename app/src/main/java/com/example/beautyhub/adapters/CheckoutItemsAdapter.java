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

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckoutItemsAdapter extends RecyclerView.Adapter<CheckoutItemsAdapter.ViewHolder> {

    private final Context context;
    private final List<CartItem> cartItems;
    private Map<String, Product> productCache;
    private String userZone = "West Malaysia";
    private boolean isShippingEnabled = true; // Logik untuk COD

    public interface OnQuantityChangeListener {
        void onQuantityChanged(int position, int newQuantity);
    }
    private final OnQuantityChangeListener quantityChangeListener;

    public CheckoutItemsAdapter(Context context, List<CartItem> cartItems, OnQuantityChangeListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.quantityChangeListener = listener;
    }

    // Fungsi untuk mematikan/menghidupkan shipping fee (digunakan oleh CheckoutActivity untuk COD)
    public void setShippingEnabled(boolean enabled) {
        this.isShippingEnabled = enabled;
        notifyDataSetChanged();
    }

    public void setUserZone(String zone) {
        this.userZone = zone;
        notifyDataSetChanged();
    }

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

        // 1. Set Maklumat Produk
        holder.itemName.setText(item.getName());
        double itemTotal = item.getPrice() * item.getQuantity();
        holder.itemPrice.setText(String.format(Locale.US, "RM %.2f", itemTotal));
        holder.itemQuantity.setText(String.format(Locale.US, "x %d", item.getQuantity()));

        // 2. Set Nama Penjual
        if (item.getSellerName() != null && !item.getSellerName().isEmpty()) {
            holder.sellerName.setText(item.getSellerName());
        } else {
            holder.sellerName.setText("BeautyHub Seller");
        }

        // 3. LOGIK SHIPPING FEE (Berubah jadi RM 0 jika isShippingEnabled = false)
        double shippingFee = isShippingEnabled ?
                ("East Malaysia".equalsIgnoreCase(userZone) ? 10.00 : 5.00) : 0.0;
        holder.tvItemShippingFee.setText(String.format(Locale.US, "RM %.2f", shippingFee));

        // 4. Load Imej
        if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrls())
                    .placeholder(R.drawable.product_placeholder)
                    .error(R.drawable.product_placeholder)
                    .centerCrop()
                    .into(holder.itemImage);
        } else {
            holder.itemImage.setImageResource(R.drawable.product_placeholder);
        }

        // 5. Kawalan Kuantiti
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        holder.btnIncrease.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() + 1;
            if (quantityChangeListener != null) {
                quantityChangeListener.onQuantityChanged(holder.getAdapterPosition(), newQuantity);
            }
        });

        holder.btnDecrease.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() - 1;
            if (newQuantity >= 1 && quantityChangeListener != null) {
                quantityChangeListener.onQuantityChanged(holder.getAdapterPosition(), newQuantity);
            }
        });

        checkStockAndBadgeStatus(holder, item);
    }

    private void checkStockAndBadgeStatus(ViewHolder holder, CartItem item) {
        if (productCache == null) {
            holder.tvStockStatus.setVisibility(View.GONE);
            holder.tvOfficialBadge.setVisibility(View.GONE);
            return;
        }

        Product product = productCache.get(item.getProductId());
        if (product == null || !product.isActive()) {
            holder.tvStockStatus.setText("Unavailable");
            holder.tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.stock_out));
            holder.tvStockStatus.setVisibility(View.VISIBLE);
            holder.btnIncrease.setEnabled(false);
            holder.btnDecrease.setEnabled(false);
            holder.tvOfficialBadge.setVisibility(View.GONE);
            return;
        }

        holder.tvOfficialBadge.setVisibility(product.isPreloaded() ? View.VISIBLE : View.GONE);

        int availableStock = product.getStock();
        if (availableStock <= 0) {
            holder.tvStockStatus.setText("Out of stock");
            holder.tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.stock_out));
            holder.tvStockStatus.setVisibility(View.VISIBLE);
            holder.btnIncrease.setEnabled(false);
        } else if (availableStock < item.getQuantity()) {
            holder.tvStockStatus.setText("Only " + availableStock + " left");
            holder.tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.stock_warning));
            holder.tvStockStatus.setVisibility(View.VISIBLE);
            holder.btnIncrease.setEnabled(false);
        } else if (availableStock <= 5) {
            holder.tvStockStatus.setText("Low stock: " + availableStock + " left");
            holder.tvStockStatus.setTextColor(ContextCompat.getColor(context, R.color.stock_low));
            holder.tvStockStatus.setVisibility(View.VISIBLE);
            holder.btnIncrease.setEnabled(true);
        } else {
            holder.tvStockStatus.setVisibility(View.GONE);
            holder.btnIncrease.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName, itemPrice, itemQuantity, sellerName;
        TextView tvQuantity, tvStockStatus, tvOfficialBadge, tvItemShippingFee;
        View btnIncrease, btnDecrease;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.iv_checkout_item_image);
            itemName = itemView.findViewById(R.id.tv_checkout_item_name);
            itemPrice = itemView.findViewById(R.id.tv_checkout_item_price);
            itemQuantity = itemView.findViewById(R.id.tv_checkout_item_quantity_total);
            sellerName = itemView.findViewById(R.id.tv_checkout_item_seller_name);
            tvQuantity = itemView.findViewById(R.id.tv_checkout_quantity);
            tvStockStatus = itemView.findViewById(R.id.tv_checkout_stock_status);
            tvOfficialBadge = itemView.findViewById(R.id.tv_checkout_official_badge);
            tvItemShippingFee = itemView.findViewById(R.id.tv_item_shipping_fee);
            btnIncrease = itemView.findViewById(R.id.btn_checkout_increase);
            btnDecrease = itemView.findViewById(R.id.btn_checkout_decrease);
        }
    }
}
