package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.PriceComparisonAdapter;
import com.example.beautyhub.adapters.ProductImageAdapter;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.ProductListing;
import com.example.beautyhub.models.Variant;
import com.example.beautyhub.ui.ProductViewModel;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity implements
        PriceComparisonAdapter.OnItemActionListener {

    private static final String TAG = "ProductDetailActivity";

    // --- Views ---
    private ViewPager2 viewPagerProductImages;
    private TextView tvProductName, tvProductDescription, tvProductPrice, tvProductDiscountPrice;
    private TextView tvProductBrand, tvProductCategory, tvProductSkinType, tvProductIngredients;
    private TextView tvProductStock, tvProductRating, tvProductSoldCount;
    private ImageView iconFavourite;
    private CollapsingToolbarLayout collapsingToolbar;
    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewComparison;
    private TextView labelSellers;

    // NEW: Action buttons
    private MaterialButton btnAddToCart, btnBuyNow;

    // NEW: Variant views
    private TextView tvVariantSelector;
    private View variantSelectionLayout;

    // --- Data & Firebase ---
    private String productId;
    private Product currentProduct;
    private FirebaseUser currentUser;
    private DatabaseReference favouritesRef, listingsRef;
    private ValueEventListener favouritesListener, listingsListener;
    private Query listingsQuery;
    private PriceComparisonAdapter comparisonAdapter;
    private List<ProductListing> listingList;
    private boolean isFavourite = false;

    // NEW: ViewModel untuk products
    private ProductViewModel productViewModel;

    // NEW: Untuk variant selection
    private Variant selectedVariant;
    private List<Variant> availableVariants = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        productId = getIntent().getStringExtra("PRODUCT_ID");

        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "Product ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        initViews();
        setupToolbar();
        loadProductWithViewModel();
        loadPriceComparisons();

        if (currentUser != null) {
            listenToFavouriteStatus();
        }
    }

    private void initViews() {
        viewPagerProductImages = findViewById(R.id.vp_product_images);
        tvProductName = findViewById(R.id.tv_detail_product_name);
        tvProductDescription = findViewById(R.id.tv_detail_product_description);
        tvProductPrice = findViewById(R.id.tv_product_price);
        tvProductDiscountPrice = findViewById(R.id.tv_product_discount_price);
        tvProductBrand = findViewById(R.id.tv_product_brand);
        tvProductCategory = findViewById(R.id.tv_product_category);
        tvProductSkinType = findViewById(R.id.tv_product_skin_type);
        tvProductIngredients = findViewById(R.id.tv_product_ingredients);
        tvProductStock = findViewById(R.id.tv_product_stock);
        tvProductRating = findViewById(R.id.tv_product_rating);
        tvProductSoldCount = findViewById(R.id.tv_product_sold_count);

        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        toolbar = findViewById(R.id.toolbar_product_detail);
        iconFavourite = findViewById(R.id.icon_favourite_detail);

        labelSellers = findViewById(R.id.label_sellers);
        recyclerViewComparison = findViewById(R.id.recycler_view_price_comparison);
        recyclerViewComparison.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComparison.setNestedScrollingEnabled(false);

        btnAddToCart = findViewById(R.id.btn_add_to_cart_detail);
        btnBuyNow = findViewById(R.id.btn_buy_now_detail);

        tvVariantSelector = findViewById(R.id.tv_variant_selector);
        variantSelectionLayout = findViewById(R.id.layout_variant_selection);

        listingList = new ArrayList<>();
        comparisonAdapter = new PriceComparisonAdapter(this, listingList, this);
        recyclerViewComparison.setAdapter(comparisonAdapter);

        setupButtonListeners();
    }

    private void setupButtonListeners() {
        btnAddToCart.setOnClickListener(v -> {
            if (currentProduct == null) return;

            if (currentProduct.hasVariants() && selectedVariant == null) {
                showVariantSelectionDialog(true);
                return;
            }
            addToCart(currentProduct, selectedVariant);
        });

        btnBuyNow.setOnClickListener(v -> {
            if (currentProduct == null) return;

            if (currentProduct.hasVariants() && selectedVariant == null) {
                showVariantSelectionDialog(false);
                return;
            }
            proceedToBuyNow(currentProduct, selectedVariant);
        });

        if (variantSelectionLayout != null) {
            variantSelectionLayout.setOnClickListener(v -> {
                if (currentProduct != null && currentProduct.hasVariants()) {
                    showVariantSelectionDialog(true);
                }
            });
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        iconFavourite.setOnClickListener(v -> toggleFavouriteStatus());
    }

    // ▼▼▼ KOD YANG DIPERBAIKI ▼▼▼
    private void loadProductWithViewModel() {
        // Panggil kaedah dalam ViewModel untuk mula memuatkan data
        productViewModel.loadProductById(productId);

        // Perhatikan 'productLiveData' untuk mendapatkan data produk
        productViewModel.getProduct().observe(this, product -> {
            if (product != null) {
                currentProduct = product;
                displayProductDetails(product);
                setupVariants(product);
                updateButtonStates();
            }
        });

        // Perhatikan 'errorLiveData' untuk mengendalikan ralat
        productViewModel.getError().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(ProductDetailActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                // Anda mungkin mahu menutup activity jika produk gagal dimuatkan
                // finish();
            }
        });

        // (Pilihan) Perhatikan 'loadingLiveData' untuk menunjukkan/menyembunyikan progress bar
        productViewModel.getLoading().observe(this, isLoading -> {
            // Jika anda mempunyai ProgressBar, anda boleh mengawalnya di sini
            // Misalnya: findViewById(R.id.your_progress_bar).setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }
    // ▲▲▲ KOD YANG DIPERBAIKI ▲▲▲

    private void displayProductDetails(Product product) {
        tvProductName.setText(product.getName());
        collapsingToolbar.setTitle(product.getName());
        tvProductDescription.setText(product.getDescription());

        // Harga
        if (product.hasDiscount()) {
            tvProductPrice.setText(String.format(Locale.US, "RM%.2f", product.getPrice()));
            tvProductPrice.setPaintFlags(tvProductPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tvProductDiscountPrice.setText(String.format(Locale.US, "RM%.2f", product.getDiscountPrice()));
            tvProductDiscountPrice.setVisibility(View.VISIBLE);
        } else {
            tvProductPrice.setText(String.format(Locale.US, "RM%.2f", product.getPrice()));
            tvProductPrice.setPaintFlags(tvProductPrice.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            tvProductDiscountPrice.setVisibility(View.GONE);
        }

        // Info lain
        tvProductBrand.setText(product.getBrand() != null ? product.getBrand() : "N/A");
        tvProductCategory.setText(product.getCategory() != null ? product.getCategory() : "N/A");
        tvProductSkinType.setText(product.getSkinType() != null ? product.getSkinType() : "N/A");
        tvProductIngredients.setText(product.getIngredients() != null ? product.getIngredients() : "N/A");

        updateStockDisplay(product);

        // Rating & Sold
        if (product.getAverageRating() > 0) {
            tvProductRating.setVisibility(View.VISIBLE);
            tvProductRating.setText(String.format(Locale.US, "%.1f", product.getAverageRating()));
        } else {
            tvProductRating.setVisibility(View.GONE);
        }

        if (product.getSoldCount() > 0) {
            tvProductSoldCount.setVisibility(View.VISIBLE);
            tvProductSoldCount.setText(String.format(Locale.US, "%,d sold", product.getSoldCount()));
        } else {
            tvProductSoldCount.setVisibility(View.GONE);
        }

        // Imej
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            ProductImageAdapter imageAdapter = new ProductImageAdapter(this, product.getImageUrls());
            viewPagerProductImages.setAdapter(imageAdapter);
        }

        if (product.isPreloaded()) {
            findViewById(R.id.badge_official).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.badge_official).setVisibility(View.GONE);
        }
    }

    private void setupVariants(Product product) {
        if (product.hasVariants()) {
            availableVariants.clear();
            availableVariants.addAll(product.getVariants());

            if (variantSelectionLayout != null) {
                variantSelectionLayout.setVisibility(View.VISIBLE);
                tvVariantSelector.setText("Select Variant");

                selectedVariant = null; // Reset pilihan
                for (Variant variant : availableVariants) {
                    if (variant.getStock() > 0) {
                        selectedVariant = variant;
                        updateVariantDisplay();
                        break;
                    }
                }
            }
        } else {
            if (variantSelectionLayout != null) {
                variantSelectionLayout.setVisibility(View.GONE);
            }
            selectedVariant = null;
        }
    }

    private void updateVariantDisplay() {
        if (selectedVariant != null) {
            tvVariantSelector.setText(selectedVariant.getDisplayNameWithPrice());

            if (currentProduct != null) {
                double finalPrice = currentProduct.getPriceWithVariant(selectedVariant);
                tvProductPrice.setText(String.format(Locale.US, "RM%.2f", finalPrice));

                if (currentProduct.hasDiscount()) {
                    tvProductDiscountPrice.setText(String.format(Locale.US, "RM%.2f",
                            currentProduct.getDiscountPrice() + selectedVariant.getPriceModifier()));
                }
            }
        }
    }

    private void updateStockDisplay(Product product) {
        if (product.hasVariants()) {
            int totalStock = 0;
            int availableVariantsCount = 0;
            for (Variant variant : product.getVariants()) {
                totalStock += variant.getStock();
                if (variant.getStock() > 0) {
                    availableVariantsCount++;
                }
            }
            if (totalStock > 0) {
                tvProductStock.setText(String.format("%d variants available", availableVariantsCount));
                tvProductStock.setTextColor(getResources().getColor(R.color.stock_in));
            } else {
                tvProductStock.setText("Out of stock");
                tvProductStock.setTextColor(getResources().getColor(R.color.stock_out));
            }
        } else {
            if (product.getStock() > 0) {
                tvProductStock.setText(String.format("%d in stock", product.getStock()));
                tvProductStock.setTextColor(getResources().getColor(R.color.stock_in));
            } else {
                tvProductStock.setText("Out of stock");
                tvProductStock.setTextColor(getResources().getColor(R.color.stock_out));
            }
        }
    }

    private void updateButtonStates() {
        if (currentProduct == null) return;
        boolean hasStock = currentProduct.hasStock();
        btnAddToCart.setEnabled(hasStock);
        btnBuyNow.setEnabled(hasStock);
        btnAddToCart.setAlpha(hasStock ? 1f : 0.5f);
        btnBuyNow.setAlpha(hasStock ? 1f : 0.5f);
    }

    private void loadPriceComparisons() {
        listingsRef = FirebaseDatabase.getInstance().getReference("ProductListings");
        listingsQuery = listingsRef.orderByChild("productId").equalTo(productId);

        listingsListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listingList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ProductListing listing = snapshot.getValue(ProductListing.class);
                        if (listing != null) {
                            listingList.add(listing);
                        }
                    }
                }
                Collections.sort(listingList, (l1, l2) -> Double.compare(l1.getPrice(), l2.getPrice()));
                comparisonAdapter.notifyDataSetChanged();

                if (listingList.isEmpty()) {
                    labelSellers.setVisibility(View.GONE);
                } else {
                    labelSellers.setVisibility(View.VISIBLE);
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load price comparisons: " + databaseError.getMessage());
                Toast.makeText(ProductDetailActivity.this, "Failed to load price list.", Toast.LENGTH_SHORT).show();
            }
        };
        listingsQuery.addValueEventListener(listingsListener);
    }

    @Override
    public void onAddToCartClicked(ProductListing listing) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add items to cart.", Toast.LENGTH_SHORT).show();
            return;
        }

        String listingId = listing.getSellerId() + "_" + listing.getProductId();
        DatabaseReference cartItemRef = FirebaseDatabase.getInstance().getReference("Carts")
                .child(currentUser.getUid()).child(listingId);

        cartItemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot cartSnapshot) {
                if (cartSnapshot.exists()) {
                    Integer currentQuantity = cartSnapshot.child("quantity").getValue(Integer.class);
                    cartItemRef.child("quantity").setValue((currentQuantity != null ? currentQuantity : 0) + 1)
                            .addOnSuccessListener(aVoid -> Toast.makeText(ProductDetailActivity.this, "Quantity updated in cart", Toast.LENGTH_SHORT).show());
                } else {
                    FirebaseDatabase.getInstance().getReference("Products").child(listing.getProductId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot productSnapshot) {
                                    Product productInfo = productSnapshot.getValue(Product.class);
                                    if (productInfo != null) {
                                        String imageUrl = (productInfo.getImageUrls() != null && !productInfo.getImageUrls().isEmpty())
                                                ? productInfo.getImageUrls().get(0) : "";

                                        CartItem newCartItem = new CartItem(
                                                listing.getProductId(),
                                                productInfo.getName(),
                                                listing.getPrice(),
                                                1,
                                                imageUrl,
                                                listing.getSellerId(),
                                                "Seller Name Placeholder"
                                        );
                                        cartItemRef.setValue(newCartItem)
                                                .addOnSuccessListener(aVoid -> Toast.makeText(ProductDetailActivity.this, "Added to cart!", Toast.LENGTH_SHORT).show());
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(ProductDetailActivity.this, "Failed to get product info.", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProductDetailActivity.this, "Failed to update cart.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addToCart(Product product, Variant variant) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add items to cart.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Carts")
                .child(currentUser.getUid());

        String cartItemId = product.getProductId();
        if (variant != null && variant.getId() != null) {
            cartItemId = product.getProductId() + "_" + variant.getId();
        }

        DatabaseReference cartItemRef = cartRef.child(cartItemId);

        cartItemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                        ? product.getImageUrls().get(0) : "";

                double finalPrice = product.getFinalPrice();
                String variantName = "";
                String variantId = "";

                if (variant != null) {
                    finalPrice = product.getPriceWithVariant(variant);
                    variantName = variant.getName();
                    variantId = variant.getId();
                }

                String sellerId = product.getSellerId();
                String sellerName = product.getSellerName();

                if (sellerId == null || sellerId.isEmpty()) {
                    sellerId = product.isPreloaded() ? "system" : "unknown_seller";
                }

                if (sellerName == null || sellerName.isEmpty()) {
                    sellerName = product.isPreloaded() ? "BeautyHub Official Store" : "Unknown Seller";
                }

                if (snapshot.exists()) {
                    Integer currentQuantity = snapshot.child("quantity").getValue(Integer.class);
                    cartItemRef.child("quantity").setValue((currentQuantity != null ? currentQuantity : 0) + 1);
                    cartItemRef.child("variantName").setValue(variantName);
                    cartItemRef.child("variantId").setValue(variantId);
                    cartItemRef.child("price").setValue(finalPrice);
                    cartItemRef.child("sellerId").setValue(sellerId);
                    cartItemRef.child("sellerName").setValue(sellerName);

                    Toast.makeText(ProductDetailActivity.this, "Quantity updated in cart", Toast.LENGTH_SHORT).show();
                } else {
                    CartItem newCartItem = new CartItem(
                            product.getProductId(),
                            product.getName(),
                            finalPrice,
                            1,
                            imageUrl
                    );

                    newCartItem.setVariantId(variantId);
                    newCartItem.setVariantName(variantName);
                    newCartItem.setSellerId(sellerId);
                    newCartItem.setSellerName(sellerName);
                    newCartItem.setFromJson(product.isPreloaded());
                    newCartItem.setHasVariants(product.hasVariants());

                    cartItemRef.setValue(newCartItem);
                    Toast.makeText(ProductDetailActivity.this, "Added to cart!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProductDetailActivity.this, "Failed to add to cart.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Add to cart failed: " + error.getMessage());
            }
        });
    }

    private void proceedToBuyNow(Product product, Variant variant) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to proceed with your purchase.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (variant != null && variant.getStock() <= 0) {
            Toast.makeText(this, "Selected variant is out of stock", Toast.LENGTH_SHORT).show();
            return;
        } else if (variant == null && product.getStock() <= 0) {
            Toast.makeText(this, "Product is out of stock", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                ? product.getImageUrls().get(0) : "";

        double finalPrice = product.getFinalPrice();
        String variantName = "";
        String variantId = "";

        if (variant != null) {
            finalPrice = product.getPriceWithVariant(variant);
            variantName = variant.getName();
            variantId = variant.getId();
        }

        String sellerId = product.getSellerId();
        String sellerName = product.getSellerName();

        if (sellerId == null || sellerId.isEmpty()) {
            sellerId = product.isPreloaded() ? "system" : "unknown_seller";
        }

        if (sellerName == null || sellerName.isEmpty()) {
            sellerName = product.isPreloaded() ? "BeautyHub Official Store" : "Unknown Seller";
        }

        CartItem buyNowItem = new CartItem(
                product.getProductId(),
                product.getName(),
                finalPrice,
                1,
                imageUrl
        );

        buyNowItem.setVariantId(variantId);
        buyNowItem.setVariantName(variantName);
        buyNowItem.setSellerId(sellerId);
        buyNowItem.setSellerName(sellerName);
        buyNowItem.setFromJson(product.isPreloaded());

        ArrayList<CartItem> itemsForCheckout = new ArrayList<>();
        itemsForCheckout.add(buyNowItem);

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("SOURCE", "BUY_NOW");
        intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", itemsForCheckout);
        startActivity(intent);
    }

    private void showVariantSelectionDialog(boolean isAddToCart) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Variant");

        List<String> variantNames = new ArrayList<>();
        for (Variant variant : availableVariants) {
            variantNames.add(variant.getDisplayNameWithPrice() + " - " + variant.getStockStatus());
        }

        builder.setItems(variantNames.toArray(new String[0]), (dialog, which) -> {
            selectedVariant = availableVariants.get(which);

            if (selectedVariant.getStock() <= 0) {
                Toast.makeText(this, "This variant is out of stock", Toast.LENGTH_SHORT).show();
                return;
            }

            updateVariantDisplay();

            if (isAddToCart) {
                addToCart(currentProduct, selectedVariant);
            } else {
                proceedToBuyNow(currentProduct, selectedVariant);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void listenToFavouriteStatus() {
        if (currentUser == null) return;
        favouritesRef = FirebaseDatabase.getInstance().getReference("Favourites")
                .child(currentUser.getUid()).child(productId);

        favouritesListener = favouritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavourite = snapshot.exists() && snapshot.getValue(Boolean.class) == Boolean.TRUE;
                updateFavouriteIcon();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "listenToFavouriteStatus:onCancelled", error.toException());
            }
        });
    }

    private void toggleFavouriteStatus() {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to manage favourites.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFavourite) {
            favouritesRef.removeValue().addOnSuccessListener(aVoid ->
                    Toast.makeText(this, "Removed from Favourites", Toast.LENGTH_SHORT).show()
            );
        } else {
            favouritesRef.setValue(true).addOnSuccessListener(aVoid ->
                    Toast.makeText(this, "Added to Favourites", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void updateFavouriteIcon() {
        if (isFavourite) {
            iconFavourite.setImageResource(R.drawable.ic_favourite_filled);
        } else {
            iconFavourite.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (favouritesRef != null && favouritesListener != null) {
            favouritesRef.removeEventListener(favouritesListener);
        }
        if (listingsRef != null && listingsListener != null) {
            listingsQuery.removeEventListener(listingsListener);
        }
    }
}
