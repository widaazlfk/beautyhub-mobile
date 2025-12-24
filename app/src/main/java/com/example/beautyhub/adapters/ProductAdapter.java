package com.example.beautyhub.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.databinding.BItemProductGrid2Binding;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Variant;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final Context context;
    private List<Product> productsList;
    private final OnProductClickListener listener;
    private final NumberFormat currencyFormat;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onAddToCartClick(Product product, Variant selectedVariant);
        void onBuyNowClick(Product product, Variant selectedVariant);
        void onSellerClick(String sellerId);
        void onWishlistClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> productsList, OnProductClickListener listener) {
        this.context = context;
        this.productsList = productsList;
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
        this.currencyFormat.setMaximumFractionDigits(2);
    }

    public void updateList(List<Product> newList) {
        this.productsList = newList;
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

            displayPrice(product);
            displayRatingAndSoldCount(product);
            displaySellerInfo(product);
            displayProductImages(product);
            displayStockStatus(product);

            setupClickListeners(product, listener);
        }

        private void displayPrice(Product product) {
            if (product.hasDiscount()) {
                // Show discounted price prominently
                binding.tvProductDiscountPrice.setVisibility(View.VISIBLE);
                binding.tvProductDiscountPrice.setText(currencyFormat.format(product.getDiscountPrice()));

                // Show original price with strikethrough
                binding.tvProductPrice.setVisibility(View.VISIBLE);
                binding.tvProductPrice.setText(currencyFormat.format(product.getPrice()));
                binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            } else {
                // No discount, hide discount view and show normal price
                binding.tvProductDiscountPrice.setVisibility(View.GONE);
                binding.tvProductPrice.setVisibility(View.VISIBLE);
                binding.tvProductPrice.setText(currencyFormat.format(product.getPrice()));
                // Remove strikethrough flag if it was set previously
                binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }

        private void displayRatingAndSoldCount(Product product) {
            if (product.getAverageRating() > 0) {
                binding.rbProductRating.setVisibility(View.VISIBLE);
                binding.rbProductRating.setRating(product.getAverageRating());
            } else {
                binding.rbProductRating.setVisibility(View.GONE);
            }

            if (product.getSoldCount() > 0) {
                binding.tvSoldCount.setVisibility(View.VISIBLE);
                binding.tvSoldCount.setText(String.format(Locale.getDefault(), "(%s sold)", formatSoldCount(product.getSoldCount())));
            } else {
                binding.tvSoldCount.setVisibility(View.GONE);
            }
        }

        private String formatSoldCount(int count) {
            if (count >= 1000) {
                return (count / 1000) + "." + ((count % 1000) / 100) + "k";
            }
            return String.valueOf(count);
        }


        private void displaySellerInfo(Product product) {
            if (product.getSellerName() != null && !product.getSellerName().isEmpty()) {
                binding.layoutSellerInfo.setVisibility(View.VISIBLE);
                binding.tvSellerName.setText(product.getSellerName());

                if (product.getSellerProfileImageUrl() != null && !product.getSellerProfileImageUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(product.getSellerProfileImageUrl())
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(binding.ivSellerProfile);
                } else {
                    binding.ivSellerProfile.setImageResource(R.drawable.ic_profile);
                }
            } else {
                binding.layoutSellerInfo.setVisibility(View.GONE);
            }
        }

        private void displayProductImages(Product product) {
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                binding.rvProductImages.setVisibility(View.VISIBLE);
                ProductImageAdapter imageAdapter = new ProductImageAdapter(itemView.getContext(), product.getImageUrls());
                binding.rvProductImages.setAdapter(imageAdapter);
            } else {
                binding.rvProductImages.setVisibility(View.GONE);
            }
        }

        // Dalam ProductAdapter.java -> ProductViewHolder

        private void displayStockStatus(Product product) {
            // Menggunakan fungsi hasStock() yang telah anda buat, ia lebih kemas
            boolean isOutOfStock = !product.hasStock();

            // Logik sedia ada untuk melumpuhkan butang
            binding.btnAddToCart.setEnabled(!isOutOfStock);
            binding.btnBuyNow.setEnabled(!isOutOfStock);
            binding.btnAddToCart.setAlpha(isOutOfStock ? 0.5f : 1.0f);
            binding.btnBuyNow.setAlpha(isOutOfStock ? 0.5f : 1.0f);

            // Bahagian baru untuk mengawal label "Out of Stock"
            if (isOutOfStock) {
                binding.tvStockStatus.setVisibility(View.VISIBLE); // Tunjukkan label
            } else {
                binding.tvStockStatus.setVisibility(View.GONE); // Sembunyikan label
            }
        }



        private void setupClickListeners(final Product product, final OnProductClickListener listener) {
            itemView.setOnClickListener(v -> listener.onProductClick(product));
            binding.icFavourite.setOnClickListener(v -> listener.onWishlistClick(product));

            binding.btnAddToCart.setOnClickListener(v -> {
                if (product.hasVariants()) {
                    showVariantSelectionDialog(product, listener, true);
                } else {
                    listener.onAddToCartClick(product, null);
                }
            });

            binding.btnBuyNow.setOnClickListener(v -> {
                if (product.hasVariants()) {
                    showVariantSelectionDialog(product, listener, false);
                } else {
                    listener.onBuyNowClick(product, null);
                }
            });

            binding.layoutSellerInfo.setOnClickListener(v -> {
                if (product.getSellerId() != null) {
                    listener.onSellerClick(product.getSellerId());
                }
            });
        }

        private void showVariantSelectionDialog(final Product product, final OnProductClickListener listener, final boolean isForCart) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setTitle("Select Variant");

            List<String> variantDisplayNames = product.getVariants().stream()
                    .map(variant -> String.format(Locale.getDefault(), "%s - %s", variant.getName(), currencyFormat.format(variant.getPriceModifier())))
                    .collect(Collectors.toList());

            builder.setItems(variantDisplayNames.toArray(new String[0]), (dialog, which) -> {
                Variant selectedVariant = product.getVariants().get(which);
                if (selectedVariant.getStock() <= 0) {
                    Toast.makeText(itemView.getContext(), "This variant is out of stock", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isForCart) {
                    listener.onAddToCartClick(product, selectedVariant);
                } else {
                    listener.onBuyNowClick(product, selectedVariant);
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.create().show();
        }
    }
}
