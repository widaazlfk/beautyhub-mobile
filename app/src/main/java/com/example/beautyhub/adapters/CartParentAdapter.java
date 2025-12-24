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

    public interface ParentCartListener {
        void onSellerHeaderClicked(String sellerId);
        void onQuantityChanged(String cartItemId, int newQuantity);
        void onItemDeleted(String cartItemId);
        void onItemSelectedChanged();
        void onVariantChanged(String cartItemId, String newVariantId);
    }

    public CartParentAdapter(Context context, List<CartSeller> sellerList, ParentCartListener listener) {
        this.context = context;
        this.sellerList = sellerList;
        this.listener = listener;
    }

    public void updateSellerList(List<CartSeller> newList) {
        this.sellerList = newList;
        notifyDataSetChanged();
    }

    public void setProductCache(Map<String, Product> productCache) {
        this.productCache = productCache;
    }

    @NonNull
    @Override
    public SellerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart_seller, parent, false);
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

        private TextView tvSellerName;
        private ImageView ivSellerProfile;
        private CheckBox checkboxSellerSelectAll;
        private RecyclerView rvCartItems;
        private View sellerHeader;

        private CartAdapter childAdapter;

        public SellerViewHolder(@NonNull View itemView) {
            super(itemView);

            tvSellerName = itemView.findViewById(R.id.tv_seller_name);
            ivSellerProfile = itemView.findViewById(R.id.iv_seller_profile);
            checkboxSellerSelectAll = itemView.findViewById(R.id.checkbox_seller_select_all);
            rvCartItems = itemView.findViewById(R.id.rv_cart_items);
            sellerHeader = itemView.findViewById(R.id.seller_header);

            // Setup child recyclerview
            childAdapter = new CartAdapter(context, new java.util.ArrayList<>(), new CartAdapter.ChildCartListener() {
                @Override
                public void onQuantityChanged(String cartItemId, int newQuantity) {
                    if (listener != null) {
                        listener.onQuantityChanged(cartItemId, newQuantity);
                    }
                }

                @Override
                public void onItemDeleted(String cartItemId) {
                    if (listener != null) {
                        listener.onItemDeleted(cartItemId);
                    }
                }

                @Override
                public void onItemSelectedChanged(String cartItemId, boolean isSelected) {
                    updateSellerSelectAllCheckbox();
                    if (listener != null) {
                        listener.onItemSelectedChanged();
                    }
                }

                @Override
                public void onVariantChanged(String cartItemId, String newVariantId) {
                    if (listener != null) {
                        listener.onVariantChanged(cartItemId, newVariantId);
                    }
                }
            });

            rvCartItems.setLayoutManager(new LinearLayoutManager(context));
            rvCartItems.setAdapter(childAdapter);
            rvCartItems.setItemAnimator(null);
        }

        void bind(CartSeller seller) {
            // Set seller info
            tvSellerName.setText(seller.getSellerName());

            if (seller.getSellerProfileImageUrl() != null && !seller.getSellerProfileImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(seller.getSellerProfileImageUrl())
                        .placeholder(R.drawable.ic_profile)
                        .into(ivSellerProfile);
            } else {
                ivSellerProfile.setImageResource(R.drawable.ic_profile);
            }

            // Show official badge for "system" seller
            if ("system".equals(seller.getSellerId())) {
                tvSellerName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_verified, 0);
            } else {
                tvSellerName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }

            // Set items
            childAdapter.updateItemList(seller.getCartItems());
            childAdapter.setProductCache(productCache); // Pass product cache ke child adapter

            // Setup seller header click
            sellerHeader.setOnClickListener(v -> {
                if (listener != null && !"system".equals(seller.getSellerId())) {
                    listener.onSellerHeaderClicked(seller.getSellerId());
                } else if ("system".equals(seller.getSellerId())) {
                    // Show toast for official store
                    android.widget.Toast.makeText(context,
                            "BeautyHub Official Store",
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            });

            // Setup seller select all checkbox
            checkboxSellerSelectAll.setOnCheckedChangeListener(null);
            boolean allSelected = areAllItemsSelected(seller.getCartItems());
            checkboxSellerSelectAll.setChecked(allSelected);

            checkboxSellerSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    for (CartItem item : seller.getCartItems()) {
                        item.setSelected(isChecked);
                    }
                    childAdapter.notifyDataSetChanged();
                    if (listener != null) {
                        listener.onItemSelectedChanged();
                    }
                }
            });
        }

        private boolean areAllItemsSelected(List<CartItem> items) {
            if (items.isEmpty()) return false;
            for (CartItem item : items) {
                if (!item.isSelected()) {
                    return false;
                }
            }
            return true;
        }

        private void updateSellerSelectAllCheckbox() {
            List<CartItem> items = childAdapter.getItemList();
            boolean allSelected = areAllItemsSelected(items);

            checkboxSellerSelectAll.setOnCheckedChangeListener(null);
            checkboxSellerSelectAll.setChecked(allSelected);
            checkboxSellerSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    for (CartItem item : items) {
                        item.setSelected(isChecked);
                    }
                    childAdapter.notifyDataSetChanged();
                    if (listener != null) {
                        listener.onItemSelectedChanged();
                    }
                }
            });
        }
    }
}