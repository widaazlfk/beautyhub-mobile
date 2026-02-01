package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.adapters.ImageSliderAdapter;
import com.example.beautyhub.adapters.ReviewAdapter;
import com.example.beautyhub.adapters.SimilarProductAdapter;
import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Review;
import com.example.beautyhub.databinding.ActivityProductDetailBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private ActivityProductDetailBinding binding;
    private String productId;
    private Product currentProduct;
    private FirebaseUser currentUser;

    // Database References
    private DatabaseReference productRef, favouritesRef, reviewsRef, allProductsRef;

    // Adapters
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private SimilarProductAdapter similarProductAdapter;
    private List<Product> similarProductList;

    private boolean isFavourite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "Product ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupUIComponents();
        setupToolbar();
        setupButtonListeners();

        loadProductData();
        loadProductReviews();

        if (currentUser != null) {
            checkFavouriteStatus();
        }
    }

    private void setupUIComponents() {
        // Setup Similar Products (Horizontal Scroll)
        similarProductList = new ArrayList<>();
        similarProductAdapter = new SimilarProductAdapter(this, similarProductList);
        binding.rvSimilarProducts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvSimilarProducts.setAdapter(similarProductAdapter);

        // Setup Reviews (Vertical)
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        binding.rvProductReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProductReviews.setNestedScrollingEnabled(false);
        binding.rvProductReviews.setAdapter(reviewAdapter);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarProductDetail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        binding.toolbarProductDetail.setNavigationOnClickListener(v -> finish());
    }

    private void setupButtonListeners() {
        binding.btnAddToCartDetail.setOnClickListener(v -> addToCart());
        binding.btnBuyNowDetail.setOnClickListener(v -> proceedToBuyNow());
        binding.iconFavouriteDetail.setOnClickListener(v -> toggleFavouriteStatus());
    }

    private void loadProductData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        productRef = FirebaseDatabase.getInstance().getReference("Products").child(productId);
        productRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentProduct = snapshot.getValue(Product.class);
                    if (currentProduct != null) {
                        currentProduct.setProductId(snapshot.getKey());
                        displayProductDetails(currentProduct);
                        loadSimilarProducts(currentProduct.getCategory());
                    }
                }
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void displayProductDetails(Product product) {
        if (product == null) {
            Log.e("ProductDetail", "Attempted to display a null product object.");
            return;
        }

        try {
            // 1. Maklumat Asas (Basic Info)
            binding.tvDetailProductName.setText(product.getName() != null ? product.getName() : "No Name Available");
            binding.tvDetailProductDescription.setText(product.getDescription() != null ? product.getDescription() : "No description provided.");
            binding.tvProductIngredients.setText(product.getIngredients() != null ? product.getIngredients() : "No ingredients listed.");

            // Gantikan bahagian selection anda dengan ini:
            if (binding.tvProductBrand != null) {
                if (product.getBrand() != null && !product.getBrand().isEmpty()) {
                    binding.tvProductBrand.setText(product.getBrand());
                    binding.tvProductBrand.setVisibility(View.VISIBLE);
                } else {
                    binding.tvProductBrand.setVisibility(View.GONE);
                }
            }

            if (binding.tvProductCategory != null) {
                if (product.getCategory() != null && !product.getCategory().isEmpty()) {
                    binding.tvProductCategory.setText(product.getCategory());
                    binding.tvProductCategory.setVisibility(View.VISIBLE);
                } else {
                    binding.tvProductCategory.setVisibility(View.GONE);
                }
            }

            if (binding.tvProductSkinType != null) {
                if (product.getSkinType() != null && !product.getSkinType().isEmpty()) {
                    binding.tvProductSkinType.setText( product.getSkinType());
                    binding.tvProductSkinType.setVisibility(View.VISIBLE);
                } else {
                    binding.tvProductSkinType.setVisibility(View.GONE);
                }
            }
            // 2. Logik Harga (Price Logic) - Mengelakkan formatting error jika harga null
            // 2. Logik Harga (Price Logic) - Sudah diperkukuh
            try {
                double originalPrice = product.getPrice();
                double discountPrice = product.getDiscountPrice();
                double finalPrice = product.getFinalPrice();

                // Debugging: Lihat nilai dalam Logcat (Filter: DEBUG_PRICE)
                Log.d("DEBUG_PRICE", "ID: " + productId + " | Ori: " + originalPrice + " | Disc: " + discountPrice + " | Final: " + finalPrice);

                if (product.hasDiscount() && discountPrice > 0) {
                    // Papar Harga Asal (Dicoret/Strikethrough)
                    binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    binding.tvProductPrice.setText(String.format(Locale.US, "RM %.2f", originalPrice));
                    binding.tvProductPrice.setTextColor(getResources().getColor(R.color.gray)); // Pastikan warna tak sama dengan background

                    // Papar Harga Diskaun (Warna Merah/Promo)
                    binding.tvProductDiscountPrice.setText(String.format(Locale.US, "RM %.2f", discountPrice));
                    binding.tvProductDiscountPrice.setVisibility(View.VISIBLE);
                } else {
                    // Tiada diskaun: Sembunyikan tvProductDiscountPrice, tunjuk harga asal tanpa coret
                    binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                    binding.tvProductPrice.setText(String.format(Locale.US, "RM %.2f", finalPrice));
                    binding.tvProductPrice.setTextColor(getResources().getColor(R.color.black)); // Warna gelap supaya nampak

                    binding.tvProductDiscountPrice.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                binding.tvProductPrice.setText("RM 0.00");
                Log.e("ProductDetail", "Error formatting price: " + e.getMessage());
            }

            // 3. Kad Penjual (Seller Card)
            if (product.getSellerId() != null) {
                binding.sellerInfoCard.setVisibility(View.VISIBLE);
                binding.tvSellerName.setText(product.getSellerName() != null ? product.getSellerName() : "Unknown Seller");

                Glide.with(this)
                        .load(product.getSellerProfileImageUrl())
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop() // Tambah ini supaya gambar bulat sempurna
                        .into(binding.ivSellerProfile);

                // LOGIK KLIK YANG DIPERBAIKI
                View.OnClickListener openShopListener = v -> {
                    Log.d("PRODUCT_DETAIL", "Navigating to shop: " + product.getSellerId());
                    Intent intent = new Intent(ProductDetailActivity.this, ShopViewActivity.class);
                    intent.putExtra("SELLER_ID", product.getSellerId());
                    startActivity(intent);
                };

                // Pasang listener pada keseluruhan kad
                binding.sellerInfoCard.setOnClickListener(openShopListener);

                // Backup: Pasang juga pada nama seller (kadang-kadang kad terhalang oleh elemen dalam)
                binding.tvSellerName.setOnClickListener(openShopListener);
                binding.ivSellerProfile.setOnClickListener(openShopListener);

            } else {
                binding.sellerInfoCard.setVisibility(View.GONE);
                Log.e("PRODUCT_DETAIL", "Seller ID is NULL for product: " + productId);
            }
            // 4. Slider Gambar (Image Slider)
            List<String> images = product.getImageUrls();
            if (images != null && !images.isEmpty()) {
                binding.vpProductImages.setVisibility(View.VISIBLE);
                ImageSliderAdapter imageAdapter = new ImageSliderAdapter(this, images);
                binding.vpProductImages.setAdapter(imageAdapter);
            } else {
                // Sembunyikan jika tiada gambar untuk elak ruang kosong/crash
                binding.vpProductImages.setVisibility(View.GONE);
                Log.d("ProductDetail", "No images available for this product.");
            }
            String sid = product.getSellerId();
            if (sid != null && !sid.isEmpty()) {
                binding.sellerInfoCard.setVisibility(View.VISIBLE);
                binding.tvSellerName.setText(product.getSellerName() != null ? product.getSellerName() : "Visit Store");

                Glide.with(this)
                        .load(product.getSellerProfileImageUrl())
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(binding.ivSellerProfile);

                View.OnClickListener openShopListener = v -> {
                    // Log untuk pengesahan di Logcat
                    Log.d("PRODUCT_DETAIL", "Navigating to shop with UID: " + sid);

                    // Toast untuk maklum balas visual segera
                    Toast.makeText(ProductDetailActivity.this, "Opening shop...", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ProductDetailActivity.this, ShopViewActivity.class);
                    intent.putExtra("SELLER_ID", sid);
                    startActivity(intent);
                };

                // Pasang pada 3 tempat untuk pastikan boleh ditekan
                binding.sellerInfoCard.setOnClickListener(openShopListener);
                binding.tvSellerName.setOnClickListener(openShopListener);
                binding.ivSellerProfile.setOnClickListener(openShopListener);

            } else {
                binding.sellerInfoCard.setVisibility(View.GONE);
                Log.e("PRODUCT_DETAIL", "Seller ID is NULL for product: " + productId);
            }
        } catch (Exception e) {
            Log.e("ProductDetail", "General error in displayProductDetails: " + e.getMessage());
            Toast.makeText(this, "Error loading product details", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSimilarProducts(String category) {
        allProductsRef = FirebaseDatabase.getInstance().getReference("Products");
        allProductsRef.orderByChild("category").equalTo(category).limitToFirst(10)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        similarProductList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            if (!ds.getKey().equals(productId)) {
                                Product p = ds.getValue(Product.class);
                                if (p != null) {
                                    p.setProductId(ds.getKey());
                                    similarProductList.add(p);
                                }
                            }
                        }
                        similarProductAdapter.notifyDataSetChanged();
                        binding.similarProductsLayout.setVisibility(similarProductList.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadProductReviews() {
        reviewsRef = FirebaseDatabase.getInstance().getReference("Reviews");
        reviewsRef.orderByChild("productId").equalTo(productId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Review r = ds.getValue(Review.class);
                    if (r != null && "APPROVED".equals(r.getStatus())) {
                        reviewList.add(r);
                    }
                }
                Collections.sort(reviewList, (r1, r2) -> Long.compare(r2.getTimestampLong(), r1.getTimestampLong()));
                reviewAdapter.notifyDataSetChanged();
                binding.labelReviews.setText("Reviews (" + reviewList.size() + ")");
                binding.tvNoReviews.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addToCart() {
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (currentProduct == null) return;

        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Carts")
                .child(currentUser.getUid()).child(currentProduct.getSellerId()).child(productId);

        String firstImage = (currentProduct.getImageUrls() != null && !currentProduct.getImageUrls().isEmpty())
                ? currentProduct.getImageUrls().get(0) : "";

        CartItem item = new CartItem(productId, currentProduct.getName(), currentProduct.getFinalPrice(),
                1, firstImage, currentProduct.getSellerId(), currentProduct.getSellerName(),
                currentProduct.getSellerProfileImageUrl());

        cartRef.setValue(item).addOnSuccessListener(aVoid ->
                Snackbar.make(binding.getRoot(), "Item added to cart", Snackbar.LENGTH_LONG)
                        .setAction("GO TO CART", v -> startActivity(new Intent(this, CartActivity.class))).show()
        );
    }

    private void proceedToBuyNow() {
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (currentProduct == null) return;

        ArrayList<CartItem> buyNowList = new ArrayList<>();
        String img = (currentProduct.getImageUrls() != null && !currentProduct.getImageUrls().isEmpty()) ? currentProduct.getImageUrls().get(0) : "";

        buyNowList.add(new CartItem(productId, currentProduct.getName(), currentProduct.getFinalPrice(), 1, img,
                currentProduct.getSellerId(), currentProduct.getSellerName(), currentProduct.getSellerProfileImageUrl()));

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", buyNowList);
        startActivity(intent);
    }

    private void checkFavouriteStatus() {
        favouritesRef = FirebaseDatabase.getInstance().getReference("Favourites").child(currentUser.getUid()).child(productId);
        favouritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavourite = snapshot.exists();
                binding.iconFavouriteDetail.setImageResource(isFavourite ? R.drawable.ic_favourite_filled : R.drawable.ic_favorite_border);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleFavouriteStatus() {
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFavourite) favouritesRef.removeValue();
        else favouritesRef.setValue(true);
    }
}