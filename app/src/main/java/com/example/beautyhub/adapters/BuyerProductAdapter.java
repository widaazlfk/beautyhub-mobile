package com.example.beautyhub.adapters;

import android.app.AlertDialog;
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
import com.example.beautyhub.models.Variant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BuyerProductAdapter extends RecyclerView.Adapter<BuyerProductAdapter.ProductViewHolder> {

    private final Context context;
    private List<Product> productList;
    private final OnProductInteractionListener listener;
    private final Set<String> favouriteProductIds = new HashSet<>();

    public interface OnProductInteractionListener {
        void onProductClick(Product product);
        void onBuyNowClick(Product product, Variant variant);
        void onAddToCartClick(Product product, Variant variant);
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

            // Set Rating
            if (product.getAverageRating() > 0) {
                binding.rbProductRating.setRating(product.getAverageRating());
                binding.rbProductRating.setVisibility(View.VISIBLE);
            } else {
                binding.rbProductRating.setVisibility(View.GONE);
            }

            // Set Sold Count
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
            // Set listener on the root view to handle product clicks
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });

            // Set listener for the 'Buy Now' button
            binding.btnBuyNow.setOnClickListener(v -> handleAction(product, false));

            // Set listener for the 'Add to Cart' button
            binding.btnAddToCart.setOnClickListener(v -> handleAction(product, true));

            // Set listener for the seller information section
            binding.layoutSellerInfo.setOnClickListener(v -> {
                if (listener != null && product.getSellerId() != null && !product.getSellerId().isEmpty()) {
                    listener.onSellerClick(product.getSellerId());
                }
            });

            // Set listener for the favourite icon
            binding.icFavourite.setOnClickListener(v -> {
                if (listener != null) {
                    boolean isCurrentlyFavourite = favouriteProductIds.contains(product.getProductId());
                    listener.onFavouriteClick(product, !isCurrentlyFavourite);
                }
            });
        }

        private void handleAction(Product product, boolean isAddToCart) {
            if (listener == null) return;

            if (product.hasVariants()) {
                showVariantSelectionDialog(product, isAddToCart);
            } else {
                if (isAddToCart) {
                    listener.onAddToCartClick(product, null);
                } else {
                    listener.onBuyNowClick(product, null);
                }
            }
        }

        private void showVariantSelectionDialog(Product product, boolean isAddToCart) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Select Variant");

            List<String> variantNames = new ArrayList<>();
            for (Variant variant : product.getVariants()) {
                variantNames.add(variant.getDisplayNameWithPrice()); // Assuming this method exists in Variant model
            }

            builder.setItems(variantNames.toArray(new String[0]), (dialog, which) -> {
                Variant selectedVariant = product.getVariants().get(which);

                if (selectedVariant.getStock() <= 0) {
                    Toast.makeText(context, "This variant is out of stock", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (listener != null) {
                    if (isAddToCart) {
                        listener.onAddToCartClick(product, selectedVariant);
                    } else {
                        listener.onBuyNowClick(product, selectedVariant);
                    }
                }
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void setupPriceDisplay(Product product) {
            boolean hasDiscount = product.hasDiscount();

            // Set visibility of discount price view
            binding.tvProductDiscountPrice.setVisibility(hasDiscount ? View.VISIBLE : View.GONE);

            if (hasDiscount) {
                // Set discounted price
                binding.tvProductDiscountPrice.setText(String.format(Locale.US, "RM%.2f", product.getDiscountPrice()));

                // Set original price with strikethrough
                binding.tvProductPrice.setText(String.format(Locale.US, "RM%.2f", product.getPrice()));
                binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvProductPrice.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));

                // Align original price to the end of the discount price
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) binding.tvProductPrice.getLayoutParams();
                params.startToEnd = binding.tvProductDiscountPrice.getId();
                binding.tvProductPrice.setLayoutParams(params);

            } else {
                // Set regular price
                binding.tvProductPrice.setText(String.format(Locale.US, "RM%.2f", product.getPrice()));
                binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                binding.tvProductPrice.setTextColor(ContextCompat.getColor(context, R.color.purple_700)); // Ensure you have this color

                // Align price to the start of the parent
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) binding.tvProductPrice.getLayoutParams();
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                binding.tvProductPrice.setLayoutParams(params);
            }
        }

        private void updateFavouriteIcon(Product product) {
            if (favouriteProductIds.contains(product.getProductId())) {
                binding.icFavourite.setImageResource(R.drawable.ic_favourite_filled); // Assumes you have a filled favorite icon
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
                // Optionally show a placeholder
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
