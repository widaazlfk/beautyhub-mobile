package com.example.beautyhub.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.databinding.BItemProductGrid2Binding;
import com.example.beautyhub.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final Context context;
    private List<Product> productsList;
    private final boolean isSellerView;
    private final OnProductClickListener listener;
    private final NumberFormat currencyFormat;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onAddToCartClick(Product product);
        void onBuyNowClick(Product product);
        void onSellerClick(String sellerId);
        void onWishlistClick(Product product);
        void onEditClick(Product product);
        void onDeleteClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> productsList, boolean isSellerView, OnProductClickListener listener) {
        this.context = context;
        this.productsList = productsList;
        this.isSellerView = isSellerView;
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
        this.currencyFormat.setMaximumFractionDigits(2);
    }

    public void updateList(List<Product> newList) {
        ProductDiffCallback diffCallback = new ProductDiffCallback(this.productsList, newList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        this.productsList.clear();
        this.productsList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        BItemProductGrid2Binding binding = BItemProductGrid2Binding.inflate(inflater, parent, false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product currentProduct = productsList.get(position);
        holder.bind(currentProduct, listener);
    }

    @Override
    public int getItemCount() {
        return productsList != null ? productsList.size() : 0;
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        private final BItemProductGrid2Binding binding;

        public ProductViewHolder(@NonNull BItemProductGrid2Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final Product product, final OnProductClickListener listener) {
            binding.tvProductName.setText(product.getName());

            checkIfFavourite(product);
            displayPrice(product);
            displayRatingAndSoldCount(product);
            displaySellerInfo(product);
            displayProductImages(product);
            displayStockStatus(product);
            setupClickListeners(product, listener);
            handleViewBasedOnRole();
        }

        private void handleViewBasedOnRole() {
            if (isSellerView) {
                // Seller View Actions
                binding.btnAddToCart.setVisibility(View.GONE);
                binding.btnBuyNow.setVisibility(View.GONE);
                binding.icFavourite.setVisibility(View.GONE);
                binding.layoutSellerInfo.setVisibility(View.GONE); // Hide own info
                binding.layoutSellerActions.setVisibility(View.VISIBLE); // Show Edit/Delete
            } else {
                // Buyer View Actions
                binding.btnAddToCart.setVisibility(View.VISIBLE);
                binding.btnBuyNow.setVisibility(View.VISIBLE);
                binding.icFavourite.setVisibility(View.VISIBLE);
                binding.layoutSellerInfo.setVisibility(View.VISIBLE); // Show shop info
                binding.layoutSellerActions.setVisibility(View.GONE); // Hide Edit/Delete
            }
        }

        private void checkIfFavourite(Product product) {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid == null || product.getProductId() == null) {
                binding.icFavourite.setImageResource(R.drawable.ic_favourite);
                return;
            }

            DatabaseReference favRef = FirebaseDatabase.getInstance().getReference("Favourites")
                    .child(uid)
                    .child(product.getProductId());

            favRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        binding.icFavourite.setImageResource(R.drawable.ic_favourite_filled);
                    } else {
                        binding.icFavourite.setImageResource(R.drawable.ic_favourite);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        private void displayPrice(Product product) {
            if (product.hasDiscount()) {
                binding.tvProductDiscountPrice.setVisibility(View.VISIBLE);
                binding.tvProductDiscountPrice.setText(currencyFormat.format(product.getDiscountPrice()));
                binding.tvProductPrice.setText(currencyFormat.format(product.getPrice()));
                binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                binding.tvProductDiscountPrice.setVisibility(View.GONE);
                binding.tvProductPrice.setText(currencyFormat.format(product.getPrice()));
                binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }

        private void displayRatingAndSoldCount(Product product) {
            binding.rbProductRating.setVisibility(product.getAverageRating() > 0 ? View.VISIBLE : View.GONE);
            if (product.getAverageRating() > 0) binding.rbProductRating.setRating(product.getAverageRating());

            binding.tvSoldCount.setVisibility(product.getSoldCount() > 0 ? View.VISIBLE : View.GONE);
            if (product.getSoldCount() > 0) {
                binding.tvSoldCount.setText(String.format(Locale.getDefault(), "(%d sold)", product.getSoldCount()));
            }
        }

        private void displaySellerInfo(Product product) {
            // Only show if not in Seller View and info exists
            if (!isSellerView && product.getSellerName() != null) {
                binding.layoutSellerInfo.setVisibility(View.VISIBLE);
                binding.tvSellerName.setText(product.getSellerName());
                Glide.with(context)
                        .load(product.getSellerProfileImageUrl())
                        .placeholder(R.drawable.ic_profile)
                        .into(binding.ivSellerProfile);
            } else {
                binding.layoutSellerInfo.setVisibility(View.GONE);
            }
        }

        private void displayProductImages(Product product) {
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                binding.rvProductImages.setVisibility(View.VISIBLE);
                ProductImageAdapter imageAdapter = new ProductImageAdapter(context, product.getImageUrls());
                binding.rvProductImages.setAdapter(imageAdapter);
            } else {
                binding.rvProductImages.setVisibility(View.GONE);
            }
        }

        private void displayStockStatus(Product product) {
            boolean outOfStock = product.getStock() <= 0;
            binding.tvStockStatus.setVisibility(outOfStock ? View.VISIBLE : View.GONE);
            if (!isSellerView) {
                binding.btnAddToCart.setEnabled(!outOfStock);
                binding.btnBuyNow.setEnabled(!outOfStock);
            }
        }

        private void setupClickListeners(final Product product, final OnProductClickListener listener) {
            binding.getRoot().setOnClickListener(v -> listener.onProductClick(product));
            binding.icFavourite.setOnClickListener(v -> listener.onWishlistClick(product));
            binding.btnAddToCart.setOnClickListener(v -> listener.onAddToCartClick(product));
            binding.btnBuyNow.setOnClickListener(v -> listener.onBuyNowClick(product));
            binding.btnEditProduct.setOnClickListener(v -> listener.onEditClick(product));
            binding.btnDeleteProduct.setOnClickListener(v -> listener.onDeleteClick(product));
            binding.layoutSellerInfo.setOnClickListener(v -> {
                if (product.getSellerId() != null) listener.onSellerClick(product.getSellerId());
            });
        }
    }

    private static class ProductDiffCallback extends DiffUtil.Callback {
        private final List<Product> oldList;
        private final List<Product> newList;

        public ProductDiffCallback(List<Product> oldList, List<Product> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }
        @Override public boolean areItemsTheSame(int o, int n) {
            return oldList.get(o).getProductId().equals(newList.get(n).getProductId());
        }
        @Override public boolean areContentsTheSame(int o, int n) {
            return oldList.get(o).equals(newList.get(n));
        }
    }
}