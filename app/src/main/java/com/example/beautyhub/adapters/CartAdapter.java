// C:/Users/widaa/beautyhub/app/src/main/java/com/example/beautyhub/adapters/CartAdapter.java

package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Variant;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartItemViewHolder> {

    private final Context context;
    private List<CartItem> cartItemList;
    private final ChildCartListener listener;
    private Map<String, Product> productCache;

    public interface ChildCartListener {
        void onQuantityChanged(String cartItemId, int newQuantity);
        void onItemDeleted(String cartItemId);
        void onItemSelectedChanged(String cartItemId, boolean isSelected);
        void onVariantChanged(String cartItemId, String newVariantId);
    }

    // ↓↓↓ FIX: Rename the constructor to match the class name "CartAdapter" ↓↓↓
    public CartAdapter(Context context, List<CartItem> cartItemList, ChildCartListener listener) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.listener = listener;
    }

    public void updateItemList(List<CartItem> newList) {
        this.cartItemList = newList;
        notifyDataSetChanged();
    }

    public List<CartItem> getItemList() {
        return cartItemList;
    }

    public void setProductCache(Map<String, Product> productCache) {
        this.productCache = productCache;
    }

    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart_item, parent, false);
        return new CartItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        CartItem cartItem = cartItemList.get(position);
        holder.bind(cartItem);
    }

    @Override
    public int getItemCount() {
        return cartItemList != null ? cartItemList.size() : 0;
    }

    public class CartItemViewHolder extends RecyclerView.ViewHolder {

        private CheckBox checkboxItem;
        private ImageView ivProductImage;
        private TextView tvProductName;
        private TextView tvProductPrice;
        private TextView tvVariantInfo;
        private TextView tvQuantity;
        private TextView btnDecrease, btnIncrease;
        private ImageView btnDelete;
        private TextView btnChangeVariant;

        private CartItem currentItem;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);

            checkboxItem = itemView.findViewById(R.id.checkbox_cart_item);
            ivProductImage = itemView.findViewById(R.id.iv_product_image);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
            tvVariantInfo = itemView.findViewById(R.id.tv_variant_info);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnChangeVariant = itemView.findViewById(R.id.btn_change_variant);
        }

        void bind(CartItem cartItem) {
            currentItem = cartItem;

            // Set product info
            tvProductName.setText(cartItem.getDisplayName());
            tvProductPrice.setText(String.format(Locale.US, "RM%.2f", cartItem.getPrice()));

            // Show variant info if it exists
            if (cartItem.getVariantName() != null && !cartItem.getVariantName().isEmpty()) {
                tvVariantInfo.setVisibility(View.VISIBLE);
                tvVariantInfo.setText(cartItem.getVariantName());
            } else {
                tvVariantInfo.setVisibility(View.GONE);
            }

            // Set quantity
            tvQuantity.setText(String.valueOf(cartItem.getQuantity()));

            // Load image
            if (cartItem.getImageUrl() != null && !cartItem.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(cartItem.getImageUrl())
                        .placeholder(R.drawable.product_placeholder)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.product_placeholder);
            }

            // Set checkbox
            checkboxItem.setChecked(cartItem.isSelected());

            // Check stock status
            checkStockStatus(cartItem);

            // Setup listeners
            setupListeners();
        }

        private void checkStockStatus(CartItem cartItem) {
            if (productCache == null) return;

            Product product = productCache.get(cartItem.getProductId());
            if (product == null || !product.isActive()) {
                // Product unavailable
                showOutOfStockWarning("Product unavailable");
                disableQuantityControls();
                return;
            }

            int maxQuantity = getMaxQuantity(product, cartItem.getVariantId());
            if (maxQuantity <= 0) {
                // Out of stock
                showOutOfStockWarning("Out of stock");
                disableQuantityControls();
            } else if (cartItem.getQuantity() > maxQuantity) {
                // Quantity exceeds stock
                showOutOfStockWarning("Only " + maxQuantity + " available");
                btnIncrease.setEnabled(false);
            } else {
                // In stock
                clearStockWarning();
                enableQuantityControls();
            }
        }

        private int getMaxQuantity(Product product, String variantId) {
            if (variantId != null && !variantId.isEmpty()) {
                Variant variant = product.getVariantById(variantId);
                return variant != null ? variant.getStock() : 0;
            } else {
                return product.getStock();
            }
        }

        private void showOutOfStockWarning(String message) {
            tvProductName.setTextColor(context.getResources().getColor(R.color.stock_out));
            if (tvVariantInfo != null) {
                tvVariantInfo.setText(message);
                tvVariantInfo.setVisibility(View.VISIBLE);
                tvVariantInfo.setTextColor(context.getResources().getColor(R.color.stock_out));
            }
        }

        private void clearStockWarning() {
            tvProductName.setTextColor(context.getResources().getColor(android.R.color.black));
            if (tvVariantInfo != null && currentItem.getVariantName() != null) {
                tvVariantInfo.setText(currentItem.getVariantName());
                tvVariantInfo.setTextColor(context.getResources().getColor(R.color.gray_dark));
            }
        }

        private void disableQuantityControls() {
            btnDecrease.setEnabled(false);
            btnIncrease.setEnabled(false);
            btnDecrease.setAlpha(0.5f);
            btnIncrease.setAlpha(0.5f);
        }

        private void enableQuantityControls() {
            btnDecrease.setEnabled(true);
            btnIncrease.setEnabled(true);
            btnDecrease.setAlpha(1f);
            btnIncrease.setAlpha(1f);
        }

        private void setupListeners() {
            // Checkbox listener
            checkboxItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    currentItem.setSelected(isChecked);
                    if (listener != null) {
                        listener.onItemSelectedChanged(currentItem.getCartItemId(), isChecked);
                    }
                }
            });

            // Quantity controls
            btnDecrease.setOnClickListener(v -> {
                int newQuantity = currentItem.getQuantity() - 1;
                if (newQuantity >= 0) {
                    if (listener != null) {
                        listener.onQuantityChanged(currentItem.getCartItemId(), newQuantity);
                    }
                }
            });

            btnIncrease.setOnClickListener(v -> {
                // Check stock limit
                if (productCache != null) {
                    Product product = productCache.get(currentItem.getProductId());
                    if (product != null) {
                        int maxQuantity = getMaxQuantity(product, currentItem.getVariantId());
                        if (currentItem.getQuantity() >= maxQuantity) {
                            Toast.makeText(context,
                                    "Maximum quantity is " + maxQuantity,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                int newQuantity = currentItem.getQuantity() + 1;
                if (listener != null) {
                    listener.onQuantityChanged(currentItem.getCartItemId(), newQuantity);
                }
            });

            // Delete button
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemDeleted(currentItem.getCartItemId());
                }
            });

            // Change variant button
            if (btnChangeVariant != null) {
                Product product = productCache != null ? productCache.get(currentItem.getProductId()) : null;
                if (product != null && product.hasVariants()) {
                    btnChangeVariant.setVisibility(View.VISIBLE);
                    btnChangeVariant.setOnClickListener(v -> {
                        showVariantSelectionDialog(product);
                    });
                } else {
                    btnChangeVariant.setVisibility(View.GONE);
                }
            }
        }

        private void showVariantSelectionDialog(Product product) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Change Variant");

            List<String> variantNames = new java.util.ArrayList<>();
            for (Variant variant : product.getVariants()) {
                variantNames.add(variant.getDisplayNameWithPrice() + " (" + variant.getStockStatus() + ")");
            }

            builder.setItems(variantNames.toArray(new String[0]), (dialog, which) -> {
                Variant selectedVariant = product.getVariants().get(which);

                if (selectedVariant.getStock() <= 0) {
                    Toast.makeText(context, "This variant is out of stock", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if variant is the same as the current one
                if (selectedVariant.getId().equals(currentItem.getVariantId())) {
                    Toast.makeText(context, "Already selected this variant", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update variant
                if (listener != null) {
                    listener.onVariantChanged(currentItem.getCartItemId(), selectedVariant.getId());
                }

                Toast.makeText(context, "Variant changed successfully", Toast.LENGTH_SHORT).show();
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
    }
}
