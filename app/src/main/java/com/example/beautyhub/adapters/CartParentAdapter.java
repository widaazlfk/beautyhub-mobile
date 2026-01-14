package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.CartSeller;
import com.example.beautyhub.models.Product;

import java.util.List;
import java.util.Map;

public class CartParentAdapter extends RecyclerView.Adapter<CartParentAdapter.SellerViewHolder> {

    private final Context context;
    private List<CartSeller> sellerList;
    private final ParentCartListener listener;
    private Map<String, Product> productCache;

    // ▼▼▼ PEMBETULAN 1: Tambah pemboleh ubah untuk mod suntingan ▼▼▼
    private boolean isInEditMode = false;

    public interface ParentCartListener {
        void onSellerHeaderClicked(String sellerId);
        void onQuantityChanged(String sellerId, String cartItemId, int newQuantity);
        void onItemDeleted(String sellerId, String cartItemId);
        void onItemSelectedChanged();
    }

    public CartParentAdapter(Context context, List<CartSeller> sellerList, ParentCartListener listener) {
        this.context = context;
        this.sellerList = sellerList;
        this.listener = listener;
    }

    public void setProductCache(Map<String, Product> productCache) {
        this.productCache = productCache;
    }

    // ▼▼▼ PEMBETULAN 2: Tambah kaedah setEditMode ▼▼▼
    public void setEditMode(boolean isInEditMode) {
        this.isInEditMode = isInEditMode;
        notifyDataSetChanged(); // Beritahu RecyclerView untuk melukis semula semua item
    }


    @NonNull
    @Override
    public SellerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ralat asal adalah di sini, pastikan nama layout adalah betul
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_cart_seller, parent, false);
        return new SellerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SellerViewHolder holder, int position) {
        CartSeller seller = sellerList.get(position);
        holder.bind(seller);
    }

    @Override
    public int getItemCount() {
        return sellerList != null ? sellerList.size() : 0;
    }

    public class SellerViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvSellerName;
        private final ImageView ivSellerProfile;
        private final CheckBox checkboxSellerSelectAll;
        private final RecyclerView rvCartItems;
        private final View sellerHeader;
        private CartChildAdapter childAdapter; // Nama ditukar dari CartAdapter

        public SellerViewHolder(@NonNull View itemView) {
            super(itemView);
            // Pastikan ID adalah betul
            tvSellerName = itemView.findViewById(R.id.tv_seller_name_header);
            ivSellerProfile = itemView.findViewById(R.id.iv_seller_profile_header);
            checkboxSellerSelectAll = itemView.findViewById(R.id.checkbox_select_seller);
            rvCartItems = itemView.findViewById(R.id.rv_child_cart_items);
            sellerHeader = itemView.findViewById(R.id.layout_seller_header);

            rvCartItems.setLayoutManager(new LinearLayoutManager(context));
            rvCartItems.setItemAnimator(null);
        }

        void bind(final CartSeller seller) {
            tvSellerName.setText(seller.getSellerName());

            if (seller.getSellerProfileImageUrl() != null && !seller.getSellerProfileImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(seller.getSellerProfileImageUrl())
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(ivSellerProfile);
            } else {
                ivSellerProfile.setImageResource(R.drawable.ic_profile);
            }

            // Guna CartChildAdapter
            childAdapter = new CartChildAdapter(context, seller.getCartItems(), new CartChildAdapter.ChildCartListener() {
                @Override
                public void onQuantityChanged(String cartItemId, int newQuantity) {
                    if (listener != null) {
                        listener.onQuantityChanged(seller.getSellerId(), cartItemId, newQuantity);
                    }
                }

                @Override
                public void onItemDeleted(String cartItemId) {
                    if (listener != null) {
                        listener.onItemDeleted(seller.getSellerId(), cartItemId);
                    }
                }

                @Override
                public void onItemSelectedChanged(String cartItemId, boolean isSelected) {
                    updateSellerSelectAllCheckbox(seller.getCartItems());
                    if (listener != null) {
                        listener.onItemSelectedChanged();
                    }
                }
            });

            rvCartItems.setAdapter(childAdapter);
            childAdapter.setProductCache(productCache);
            // ▼▼▼ PEMBETULAN 3: Hantar status mod suntingan kepada child adapter ▼▼▼
            childAdapter.setEditMode(isInEditMode);


            sellerHeader.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSellerHeaderClicked(seller.getSellerId());
                }
            });

            checkboxSellerSelectAll.setOnCheckedChangeListener(null);
            updateSellerSelectAllCheckbox(seller.getCartItems());

            checkboxSellerSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    for (CartItem item : seller.getCartItems()) {
                        // Hanya pilih item yang tersedia jika tidak dalam mod suntingan
                        if (item.isAvailable() || isInEditMode) {
                            item.setSelected(isChecked);
                        }
                    }
                    childAdapter.notifyDataSetChanged();
                    if (listener != null) {
                        listener.onItemSelectedChanged();
                    }
                }
            });
        }

        private void updateSellerSelectAllCheckbox(List<CartItem> items) {
            if (items == null || items.isEmpty()) {
                checkboxSellerSelectAll.setChecked(false);
                return;
            }

            // Kira jumlah item yang relevan untuk pemilihan dalam mod semasa
            int relevantItemCount = 0;
            // Kira jumlah item yang telah dipilih
            int selectedItemCount = 0;

            for (CartItem item : items) {
                // Satu item dianggap 'relevan' jika:
                // 1. Kita berada dalam mod suntingan (semua item relevan).
                // 2. Kita dalam mod biasa DAN item itu tersedia.
                boolean isItemRelevant = isInEditMode || item.isAvailable();

                if (isItemRelevant) {
                    relevantItemCount++;
                    if (item.isSelected()) {
                        selectedItemCount++;
                    }
                }
            }

            // Kotak semak penjual akan ditanda jika:
            // 1. Terdapat sekurang-kurangnya satu item yang relevan.
            // 2. Jumlah item yang relevan adalah sama dengan jumlah item yang dipilih.
            checkboxSellerSelectAll.setChecked(relevantItemCount > 0 && relevantItemCount == selectedItemCount);
        }

    }
}
