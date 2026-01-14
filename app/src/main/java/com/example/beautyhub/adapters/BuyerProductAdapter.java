package com.example.beautyhub.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.databinding.BItemProductGrid2Binding;
import com.example.beautyhub.models.Product;
// --- PERUBAHAN 1: Padam import Variant ---
// import com.example.beautyhub.models.Variant;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BuyerProductAdapter extends RecyclerView.Adapter<BuyerProductAdapter.ProductViewHolder> {

    private final Context context;
    private List<Product> productList;
    private final OnProductInteractionListener listener;
    private final Set<String> favouriteProductIds = new HashSet<>();

    // --- PERUBAHAN 2: Permudahkan Listener Interface ---
    public interface OnProductInteractionListener {
        void onProductClick(Product product);
        void onBuyNowClick(Product product); // Parameter Variant dibuang
        void onAddToCartClick(Product product); // Parameter Variant dibuang
        void onFavouriteClick(Product product, boolean isFavourite);
        void onSellerClick(String sellerId);
    }

    public BuyerProductAdapter(Context context, List<Product> productList, OnProductInteractionListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    public void updateProductList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    public void setFavouriteProductIds(List<String> ids) {
        this.favouriteProductIds.clear();
        if (ids != null) {
            this.favouriteProductIds.addAll(ids);
        }
        notifyDataSetChanged();
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
        Product product = productList.get(position);
        if (product != null) {
            holder.bind(product);
        }
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        private final BItemProductGrid2Binding binding;

        public ProductViewHolder(@NonNull BItemProductGrid2Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product) {
            binding.tvProductName.setText(product.getName());

            if (product.getAverageRating() > 0) {
                binding.rbProductRating.setRating(product.getAverageRating());
                binding.rbProductRating.setVisibility(View.VISIBLE);
            } else {
                binding.rbProductRating.setVisibility(View.GONE);
            }

            if (product.getSoldCount() > 0) {
                binding.tvSoldCount.setText(String.format(Locale.getDefault(), "(%s sold)", formatSoldCount(product.getSoldCount())));
                binding.tvSoldCount.setVisibility(View.VISIBLE);
            } else {
                binding.tvSoldCount.setVisibility(View.GONE);
            }

            setupPriceDisplay(product);
            setupProductImageSlider(product);
            setupSellerInfo(product);
            updateFavouriteIcon(product);
            setupClickListeners(product);
        }

        private void setupClickListeners(Product product) {
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });

            // --- PERUBAHAN 3: Permudahkan listener butang ---
            binding.btnBuyNow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBuyNowClick(product);
                }
            });

            binding.btnAddToCart.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToCartClick(product);
                }
            });

            binding.layoutSellerInfo.setOnClickListener(v -> {
                if (listener != null && product.getSellerId() != null && !product.getSellerId().isEmpty()) {
                    // Semak jika produk adalah 'preloaded'
                    if (product.isPreloaded()) {
                        // Dapatkan nama penjual, jika tiada, guna "BeautyHub" sebagai lalai
                        String sellerName = product.getSellerName() != null ? product.getSellerName() : "BeautyHub";
                        // Paparkan mesej yang lebih spesifik
                        Toast.makeText(itemView.getContext(), "Official Product from " + sellerName, Toast.LENGTH_SHORT).show();
                    } else {
                        // Jika bukan produk preloaded, benarkan navigasi ke profil penjual
                        listener.onSellerClick(product.getSellerId());
                    }
                }
            });


            binding.icFavourite.setOnClickListener(v -> {
                if (listener != null) {
                    boolean isCurrentlyFavourite = favouriteProductIds.contains(product.getProductId());
                    listener.onFavouriteClick(product, !isCurrentlyFavourite);
                    updateFavouriteIconOnClick(product.getProductId());
                }
            });
        }

        private void updateFavouriteIconOnClick(String productId) {
            boolean isNowFavourite = !favouriteProductIds.contains(productId);
            if (isNowFavourite) {
                favouriteProductIds.add(productId);
                binding.icFavourite.setImageResource(R.drawable.ic_favourite_filled);
            } else {
                favouriteProductIds.remove(productId);
                binding.icFavourite.setImageResource(R.drawable.ic_favorite_border);
            }
        }

        // --- PERUBAHAN 4: Padam kaedah handleAction dan showVariantSelectionDialog ---
        /*
        private void handleAction(Product product, boolean isAddToCart) { ... }
        private void showVariantSelectionDialog(Product product, boolean isAddToCart) { ... }
        */

        private void setupPriceDisplay(Product product) {
            boolean hasDiscount = product.hasDiscount();
            binding.tvProductDiscountPrice.setVisibility(hasDiscount ? View.VISIBLE : View.GONE);

            if (hasDiscount) {
                binding.tvProductDiscountPrice.setText(String.format(Locale.US, "RM%.2f", product.getDiscountPrice()));
                binding.tvProductPrice.setText(String.format(Locale.US, "RM%.2f", product.getPrice()));
                binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvProductPrice.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));

                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) binding.tvProductPrice.getLayoutParams();
                params.startToEnd = binding.tvProductDiscountPrice.getId();
                binding.tvProductPrice.setLayoutParams(params);
            } else {
                binding.tvProductPrice.setText(String.format(Locale.US, "RM%.2f", product.getPrice()));
                binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                binding.tvProductPrice.setTextColor(ContextCompat.getColor(context, R.color.purple_700));

                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) binding.tvProductPrice.getLayoutParams();
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                binding.tvProductPrice.setLayoutParams(params);
            }
        }

        private void updateFavouriteIcon(Product product) {
            if (favouriteProductIds.contains(product.getProductId())) {
                binding.icFavourite.setImageResource(R.drawable.ic_favourite_filled);
            } else {
                binding.icFavourite.setImageResource(R.drawable.ic_favorite_border);
            }
        }

        private void setupProductImageSlider(Product product) {
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                ProductImageAdapter imageAdapter = new ProductImageAdapter(context, product.getImageUrls());
                binding.rvProductImages.setAdapter(imageAdapter);
                binding.rvProductImages.setVisibility(View.VISIBLE);
            } else {
                binding.rvProductImages.setVisibility(View.GONE);
            }
        }

        private void setupSellerInfo(Product product) {
            if (product.getSellerName() != null && !product.getSellerName().isEmpty()) {
                binding.tvSellerName.setText(product.getSellerName());
                binding.layoutSellerInfo.setVisibility(View.VISIBLE);

                Glide.with(context)
                        .load(product.getSellerProfileImageUrl())
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(binding.ivSellerProfile);
            } else {
                binding.layoutSellerInfo.setVisibility(View.GONE);
            }
        }

        private String formatSoldCount(int count) {
            if (count >= 1000) {
                return (count / 1000) + "k";
            }
            return String.valueOf(count);
        }
    }
}
