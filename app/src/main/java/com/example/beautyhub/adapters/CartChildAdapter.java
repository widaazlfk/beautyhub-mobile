package com.example.beautyhub.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartChildAdapter extends RecyclerView.Adapter<CartChildAdapter.CartItemViewHolder> {

    private final Context context;
    private List<CartItem> cartItemList;
    private final ChildCartListener listener;
    private Map<String, Product> productCache;

    // ▼▼▼ PEMBETULAN: Tambah pemboleh ubah untuk mod suntingan ▼▼▼
    private boolean isInEditMode = false;


    // Interface listener yang ringkas
    public interface ChildCartListener {
        void onQuantityChanged(String cartItemId, int newQuantity);
        void onItemDeleted(String cartItemId);
        void onItemSelectedChanged(String cartItemId, boolean isSelected);
    }

    public CartChildAdapter(Context context, List<CartItem> cartItemList, ChildCartListener listener) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.listener = listener;
    }

    public void setProductCache(Map<String, Product> productCache) {
        this.productCache = productCache;
        notifyDataSetChanged();
    }

    // ▼▼▼ PEMBETULAN: Tambah kaedah setEditMode yang hilang ▼▼▼
    public void setEditMode(boolean isInEditMode) {
        this.isInEditMode = isInEditMode;
        // Kita boleh panggil notifyDataSetChanged() untuk memastikan UI dikemas kini jika perlu pada masa hadapan
        notifyDataSetChanged();
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

        private final CheckBox checkboxItem;
        private final ImageView ivProductImage;
        private final TextView tvProductName;
        private final TextView tvProductPrice;
        private final TextView tvQuantity;
        private final TextView btnDecrease;
        private final TextView btnIncrease;
        private final ImageView btnDelete;
        private final TextView tvStockWarning;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            // Gunakan ID dari item_cart_item.xml yang anda berikan
            checkboxItem = itemView.findViewById(R.id.checkbox_cart_item);
            ivProductImage = itemView.findViewById(R.id.iv_product_image);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            tvStockWarning = itemView.findViewById(R.id.tv_stock_warning);
        }

        void bind(CartItem item) {
            // Memaparkan data asas
            tvProductName.setText(item.getName());
            tvProductPrice.setText(String.format(Locale.US, "RM%.2f", item.getPrice()));
            tvQuantity.setText(String.valueOf(item.getQuantity()));

            // Muat gambar produk
            if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
                Glide.with(context).load(item.getImageUrls()).placeholder(R.drawable.product_placeholder).into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.product_placeholder);
            }

            boolean isAvailable = item.isAvailable();
            Product product = (productCache != null) ? productCache.get(item.getProductId()) : null; // Kekalkan untuk semakan kuantiti


            // ▼▼▼ PEMBETULAN: Logik mod suntingan ditambah di sini ▼▼▼
            // Walaupun susun atur anda tidak mempunyai butang padam yang berasingan,
            // kita boleh mengawal keterlihatan kawalan kuantiti.
            if (isInEditMode) {
                // Sembunyikan kawalan kuantiti dalam mod suntingan
                itemView.findViewById(R.id.quantity_selector_layout).setVisibility(View.GONE);
                // Pastikan butang padam sentiasa kelihatan
                btnDelete.setVisibility(View.VISIBLE);
            } else {
                // Tunjukkan kawalan kuantiti dalam mod biasa
                itemView.findViewById(R.id.quantity_selector_layout).setVisibility(View.VISIBLE);
                // Butang padam juga sentiasa kelihatan berdasarkan susun atur anda
                btnDelete.setVisibility(View.VISIBLE);
            }


            // Kemas kini UI berdasarkan ketersediaan
            if (isAvailable) {
                // Item tersedia
                tvStockWarning.setVisibility(View.GONE);
                tvProductName.setPaintFlags(tvProductName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                itemView.setAlpha(1.0f);
                checkboxItem.setEnabled(true);
                enableQuantityControls();
            } else {
                // Item tidak tersedia atau kehabisan stok
                tvStockWarning.setText("Out of stock");
                tvStockWarning.setVisibility(View.VISIBLE);
                tvProductName.setPaintFlags(tvProductName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                itemView.setAlpha(0.6f);
                item.setSelected(false); // Nyahpilih secara automatik
                checkboxItem.setEnabled(false);
                disableQuantityControls();
            }

            // Listeners
            checkboxItem.setOnCheckedChangeListener(null);
            checkboxItem.setChecked(item.isSelected());
            checkboxItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    item.setSelected(isChecked);
                    if (listener != null) {
                        listener.onItemSelectedChanged(item.getCartItemId(), isChecked);
                    }
                }
            });

            btnIncrease.setOnClickListener(v -> {
                if (product != null && item.getQuantity() >= product.getStock()) {
                    Toast.makeText(context, "Maximum stock reached", Toast.LENGTH_SHORT).show();
                } else if (listener != null) {
                    listener.onQuantityChanged(item.getCartItemId(), item.getQuantity() + 1);
                }
            });

            btnDecrease.setOnClickListener(v -> {
                if (item.getQuantity() > 1) {
                    if (listener != null) {
                        listener.onQuantityChanged(item.getCartItemId(), item.getQuantity() - 1);
                    }
                } else {
                    showDeleteConfirmationDialog(item);
                }
            });

            btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(item));
        }

        private void showDeleteConfirmationDialog(final CartItem item) {
            new AlertDialog.Builder(context)
                    .setTitle("Remove Item")
                    .setMessage("Are you sure you want to remove this item?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (listener != null) {
                            listener.onItemDeleted(item.getCartItemId());
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
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
    }
}
