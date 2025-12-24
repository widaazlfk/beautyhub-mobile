package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.BuyerCategoryAdapter;
import com.example.beautyhub.adapters.BuyerProductAdapter;
import com.example.beautyhub.adapters.PromoCarouselAdapter;
import com.example.beautyhub.admin.LogHelper;
import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.info.AboutUsActivity;
import com.example.beautyhub.info.ContactUsActivity;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Category;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Variant;
import com.example.beautyhub.seller.SellerProfileActivity;
import com.example.beautyhub.ui.ProductViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BuyerActivity extends AppCompatActivity implements
        BuyerCategoryAdapter.OnCategoryClickListener,
        BuyerProductAdapter.OnProductInteractionListener { // ← CHANGED: Implement the new interface

    private static final String TAG = "BuyerActivity";
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;

    // Views
    private RecyclerView newestProductsRecyclerView;
    private ProgressBar productsProgressBar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private ImageView iconMenu, searchButton;
    private FrameLayout cartIconLayout;
    private TextView tvViewAllProducts;
    private TextView cartBadge;
    private ViewPager2 promoCarousel;
    private RecyclerView categoriesRecyclerView;

    // Adapters
    private BuyerProductAdapter productAdapter;
    private BuyerCategoryAdapter categoryAdapter;
    private PromoCarouselAdapter promoCarouselAdapter;

    // Data Lists
    private List<Product> productList;
    private List<Category> categoryList;
    private List<Integer> promoImageList;

    // Firebase Listener for Cart
    private DatabaseReference cartRef;
    private ValueEventListener cartListener;
    private FirebaseUser currentUser;

    // ViewModel untuk products
    private ProductViewModel productViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.b_activity_buyer);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize ViewModel
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        initViews();
        setupNavigation();
        setupPromoCarousel();
        setupCategories();
        setupNewestProducts();

        fetchCategoriesFromFirebase();
        fetchProductsWithViewModel();
        setupCartBadge();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        iconMenu = findViewById(R.id.icon_menu);
        searchButton = findViewById(R.id.icon_search);
        cartIconLayout = findViewById(R.id.cart_icon_layout);
        cartBadge = findViewById(R.id.cart_badge);
        promoCarousel = findViewById(R.id.promo_carousel);
        categoriesRecyclerView = findViewById(R.id.rv_categories);
        tvViewAllProducts = findViewById(R.id.tv_view_all_products);
        newestProductsRecyclerView = findViewById(R.id.newest_products_grid);
        productsProgressBar = findViewById(R.id.products_progress_bar);
    }

    private void setupNavigation() {
        iconMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_about_us) {
                startActivity(new Intent(BuyerActivity.this, AboutUsActivity.class));
            } else if (itemId == R.id.nav_contact_us) {
                startActivity(new Intent(BuyerActivity.this, ContactUsActivity.class));
            } else if (itemId == R.id.nav_logout) {
                showLogoutConfirmation();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        searchButton.setOnClickListener(v -> startActivity(new Intent(BuyerActivity.this, SearchActivity.class)));
        cartIconLayout.setOnClickListener(v -> startActivity(new Intent(BuyerActivity.this, CartActivity.class)));

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_shop) {
                startActivity(new Intent(this, ShopActivity.class));
                return true;
            } else if (itemId == R.id.nav_favourite) {
                startActivity(new Intent(this, FavouriteActivity.class));
                return true;
            } else if (itemId == R.id.nav_rewards) {
                startActivity(new Intent(this, MyRewardsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
                return true;
            }
            return false;
        });

        tvViewAllProducts.setOnClickListener(v -> startActivity(new Intent(this, ShopActivity.class)));
    }

    private void setupPromoCarousel() {
        promoImageList = new ArrayList<>();
        promoImageList.add(R.drawable.promo_placeholder_1);
        promoImageList.add(R.drawable.promo_placeholder_2);
        promoImageList.add(R.drawable.promo_placeholder_3);
        promoCarouselAdapter = new PromoCarouselAdapter(promoImageList);
        promoCarousel.setAdapter(promoCarouselAdapter);
    }

    private void setupCategories() {
        categoryList = new ArrayList<>();
        categoryAdapter = new BuyerCategoryAdapter(this, categoryList, this);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoriesRecyclerView.setAdapter(categoryAdapter);
    }

    private void fetchCategoriesFromFirebase() {
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance().getReference("Categories");
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Category category = dataSnapshot.getValue(Category.class);
                    if (category != null) {
                        categoryList.add(category);
                    }
                }
                categoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BuyerActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNewestProducts() {
        productList = new ArrayList<>();
        // Pass this activity as the listener
        productAdapter = new BuyerProductAdapter(this, productList, this);
        newestProductsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        newestProductsRecyclerView.setAdapter(productAdapter);
        newestProductsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void fetchProductsWithViewModel() {
        productsProgressBar.setVisibility(View.VISIBLE);
        newestProductsRecyclerView.setVisibility(View.GONE);

        // Observer untuk products dari ViewModel
        productViewModel.getProducts().observe(this, products -> {
            productList.clear();

            // Filter: hanya active products dengan stock > 0
            for (Product product : products) {
                // ▼▼▼ PERUBAHAN DI SINI ▼▼▼
                // Gunakan kaedah hasStock() dari model Product yang sudah pintar
                if (product.isActive() && product.hasStock()) {
                    productList.add(product);
                }
                // ▲▲▲ AKHIR PERUBAHAN ▲▲▲

                // Limit to 10 newest products
                if (productList.size() >= 10) {
                    break;
                }
            }

            productAdapter.updateProductList(productList);
            productsProgressBar.setVisibility(View.GONE);
            newestProductsRecyclerView.setVisibility(View.VISIBLE);
        });

        // Observer untuk error
        productViewModel.getError().observe(this, error -> {
            productsProgressBar.setVisibility(View.GONE);
            if (error != null && !error.isEmpty()) {
                Toast.makeText(BuyerActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // Observer untuk loading
        productViewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                productsProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Load products melalui ViewModel
        productViewModel.loadAllProducts();
    }


    private void setupCartBadge() {
        if (currentUser == null) {
            cartBadge.setVisibility(View.GONE);
            return;
        }
        String userId = currentUser.getUid();
        cartRef = FirebaseDatabase.getInstance().getReference("Carts").child(userId);
        cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long itemCount = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        CartItem item = itemSnapshot.getValue(CartItem.class);
                        if (item != null) {
                            itemCount += item.getQuantity();
                        }
                    }
                }

                if (itemCount > 0) {
                    cartBadge.setText(String.valueOf(itemCount));
                    cartBadge.setVisibility(View.VISIBLE);
                } else {
                    cartBadge.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read cart data for badge.", error.toException());
            }
        };
        cartRef.addValueEventListener(cartListener);
    }

    // --- Category Listener Implementation ---
    @Override
    public void onCategoryClick(Category category) {
        Intent intent = new Intent(this, ShopActivity.class);
        intent.putExtra("CATEGORY_NAME", category.getCategoryName());
        startActivity(intent);
    }

    // --- Product Interaction Listener Implementation ---

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onBuyNowClick(Product product, Variant variant) {
        if (currentUser != null) {
            // Check stock berdasarkan variant
            if (variant != null && variant.getStock() <= 0) {
                Toast.makeText(this, "Selected variant is out of stock", Toast.LENGTH_SHORT).show();
                return;
            } else if (variant == null && product.getStock() <= 0) {
                Toast.makeText(this, "Product is out of stock", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, CheckoutActivity.class);
            ArrayList<CartItem> itemsToCheckout = new ArrayList<>();

            String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                    ? product.getImageUrls().get(0) : "";

            // Calculate final price based on variant
            double finalPrice = product.getFinalPrice();
            if (variant != null) {
                finalPrice = product.getPriceWithVariant(variant);
            }

            // Create CartItem dengan variant info
            CartItem item = new CartItem(
                    product.getProductId(),
                    product.getName(),
                    finalPrice,
                    1,
                    imageUrl
            );

            // Set variant info jika ada
            if (variant != null) {
                item.setVariantId(variant.getId());
                item.setVariantName(variant.getName());
            }

            // Set seller info
            item.setSellerId(product.getSellerId());
            item.setSellerName(product.getSellerName());

            itemsToCheckout.add(item);
            intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", itemsToCheckout);
            intent.putExtra("SOURCE", "BUY_NOW");
            startActivity(intent);

        } else {
            Toast.makeText(this, "Please log in to proceed with your purchase", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    @Override
    public void onAddToCartClick(Product product, Variant variant) {
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Generate unique cart item ID (include variant ID if exists)
            String cartItemId = product.getProductId();
            if (variant != null && variant.getId() != null) {
                cartItemId = product.getProductId() + "_" + variant.getId();
            }

            DatabaseReference cartItemRef = FirebaseDatabase.getInstance().getReference("Carts")
                    .child(userId).child(cartItemId);

            cartItemRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    double finalPrice = product.getFinalPrice();
                    if (variant != null) {
                        finalPrice = product.getPriceWithVariant(variant);
                    }

                    if (snapshot.exists()) {
                        // Item already in cart, update quantity
                        CartItem cartItem = snapshot.getValue(CartItem.class);
                        if (cartItem != null) {
                            cartItem.setQuantity(cartItem.getQuantity() + 1);
                            cartItemRef.setValue(cartItem);
                            Toast.makeText(BuyerActivity.this, "Quantity updated in cart", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // New item, add to cart
                        String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                                ? product.getImageUrls().get(0) : "";

                        CartItem newCartItem = new CartItem(
                                product.getProductId(),
                                product.getName(),
                                finalPrice,
                                1,
                                imageUrl
                        );

                        // Set variant info jika ada
                        if (variant != null) {
                            newCartItem.setVariantId(variant.getId());
                            newCartItem.setVariantName(variant.getName());
                        }

                        // Set seller info
                        newCartItem.setSellerId(product.getSellerId());
                        newCartItem.setSellerName(product.getSellerName());

                        cartItemRef.setValue(newCartItem);
                        Toast.makeText(BuyerActivity.this, "Added to cart", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(BuyerActivity.this, "Failed to update cart.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Please log in to add items to your cart", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFavouriteClick(Product product, boolean isFavourite) {
        if (currentUser != null) {
            // Toggle favourite status
            DatabaseReference favRef = FirebaseDatabase.getInstance().getReference("Favourites")
                    .child(currentUser.getUid())
                    .child(product.getProductId());

            if (isFavourite) {
                favRef.setValue(true).addOnSuccessListener(aVoid ->
                        Toast.makeText(BuyerActivity.this, "Added to favourites", Toast.LENGTH_SHORT).show());
            } else {
                favRef.removeValue().addOnSuccessListener(aVoid ->
                        Toast.makeText(BuyerActivity.this, "Removed from favourites", Toast.LENGTH_SHORT).show());
            }
        } else {
            Toast.makeText(this, "Please log in to manage favourites", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    @Override
    public void onSellerClick(String sellerId) {
        if ("system".equals(sellerId)) {
            Toast.makeText(this, "This is an official BeautyHub product", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SellerProfileActivity.class);
        intent.putExtra("SELLER_ID", sellerId);
        startActivity(intent);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    LogHelper.logCurrentUserAction("LOGOUT", "User logged out successfully.", "Buyer");
                    startActivity(new Intent(BuyerActivity.this, LoginActivity.class));
                    finishAffinity();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartRef != null && cartListener != null) {
            cartRef.removeEventListener(cartListener);
        }
    }
}