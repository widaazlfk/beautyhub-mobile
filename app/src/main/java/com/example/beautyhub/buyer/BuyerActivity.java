package com.example.beautyhub.buyer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.beautyhub.adapters.BrandAdapter;
import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.info.AboutUsActivity;
import com.example.beautyhub.info.ContactUsActivity;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Category;
import com.example.beautyhub.models.NotificationModel;
import com.example.beautyhub.models.Product;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuyerActivity extends AppCompatActivity implements
        BuyerCategoryAdapter.OnCategoryClickListener,
        BuyerProductAdapter.OnProductInteractionListener {

    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;

    // Views
    private RecyclerView newestProductsRecyclerView;
    private ProgressBar productsProgressBar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private ImageView iconMenu;
    private View searchButton;
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
    private FrameLayout notificationIconLayout;
    private TextView notificationBadge;
    private DatabaseReference notificationRef;
    private ValueEventListener notificationListener;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private android.os.Handler carouselHandler = new android.os.Handler();
    private Runnable carouselRunnable;
    private RecyclerView brandsRecyclerView;
    private BrandAdapter brandAdapter;
    private List<Product> brandList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.b_activity_buyer);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize ViewModel
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        initViews();
        setupSwipeRefresh();
        setupNavigation();
        setupPromoCarousel();
        setupCategories();
        setupBrands();
        setupNewestProducts();

        fetchCategoriesFromFirebase();
        fetchProductsWithViewModel();
        setupCartBadge();
        setupNotificationBadge();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        navigationView = findViewById(R.id.navigation_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        iconMenu = findViewById(R.id.icon_menu);
        searchButton = findViewById(R.id.layout_search_trigger);
        notificationIconLayout = findViewById(R.id.notification_icon_layout);
        notificationBadge = findViewById(R.id.notification_badge);
        cartIconLayout = findViewById(R.id.cart_icon_layout);
        cartBadge = findViewById(R.id.cart_badge);
        promoCarousel = findViewById(R.id.promo_carousel);
        categoriesRecyclerView = findViewById(R.id.rv_categories);
        brandsRecyclerView = findViewById(R.id.rv_brands);
        tvViewAllProducts = findViewById(R.id.tv_view_all_products);
        newestProductsRecyclerView = findViewById(R.id.newest_products_grid);
        productsProgressBar = findViewById(R.id.products_progress_bar);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchCategoriesFromFirebase();
            productViewModel.loadAllProducts();

            new android.os.Handler().postDelayed(() -> {
                swipeRefreshLayout.setRefreshing(false);
            }, 2000);
        });
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
            } else if (itemId == R.id.nav_report) {
                startActivity(new Intent(BuyerActivity.this, ReportProblemActivity.class));
            } else if (itemId == R.id.nav_logout) {
                showLogoutConfirmation();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        searchButton.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100);
                startActivity(new Intent(BuyerActivity.this, SearchActivity.class));
            });
        });

        notificationIconLayout.setOnClickListener(v -> {
            Intent intent = new Intent(BuyerActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

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
            } else if (itemId == R.id.nav_comparison) {
                startActivity(new Intent(this, ComparisonActivity.class));
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
        promoImageList.add(R.drawable.skincare);
        promoImageList.add(R.drawable.makeip);
        promoImageList.add(R.drawable.model);

        promoCarouselAdapter = new PromoCarouselAdapter(promoImageList);
        promoCarousel.setAdapter(promoCarouselAdapter);

        carouselRunnable = () -> {
            int currentItem = promoCarousel.getCurrentItem();
            int nextItem = (currentItem + 1) % promoImageList.size();
            promoCarousel.setCurrentItem(nextItem, true);
            carouselHandler.postDelayed(carouselRunnable, 4000);
        };
        carouselHandler.postDelayed(carouselRunnable, 4000);

        promoCarousel.setPageTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        carouselHandler.removeCallbacks(carouselRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (carouselRunnable != null) {
            carouselHandler.postDelayed(carouselRunnable, 4000);
        }
    }

    private void setupCategories() {
        categoryList = new ArrayList<>();
        categoryAdapter = new BuyerCategoryAdapter(this, categoryList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        categoriesRecyclerView.setLayoutManager(layoutManager);
        categoriesRecyclerView.setAdapter(categoryAdapter);

        androidx.recyclerview.widget.SnapHelper snapHelper = new androidx.recyclerview.widget.LinearSnapHelper();
        if (categoriesRecyclerView.getOnFlingListener() == null) {
            snapHelper.attachToRecyclerView(categoriesRecyclerView);
        }
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
                        // FILTER: Hanya ambil subkategori, bukan kategori utama
                        String catName = category.getCategoryName().toLowerCase().trim();

                        // Skip kategori utama seperti "Skincare", "Makeup"
                        if (!catName.equals("skincare") && !catName.equals("makeup") &&
                                !catName.equals("haircare") && !catName.equals("bodycare")) {
                            categoryList.add(category);
                        }
                    }
                }
                categoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BuyerActivity", "Error fetch categories: " + error.getMessage());
            }
        });
    }

    private void setupBrands() {
        List<Product> brandProductList = new ArrayList<>();
        brandAdapter = new BrandAdapter(this, brandProductList, product -> {
            Intent intent = new Intent(BuyerActivity.this, ShopViewActivity.class);
            intent.putExtra("SELLER_ID", product.getSellerId());
            startActivity(intent);
        });

        brandsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        brandsRecyclerView.setAdapter(brandAdapter);
    }

    private void fetchProductsWithViewModel() {
        productsProgressBar.setVisibility(View.VISIBLE);
        newestProductsRecyclerView.setVisibility(View.GONE);

        productViewModel.getProducts().observe(this, products -> {
            if (products != null) {
                // 1. Kemaskini Grid Produk (10 produk terbaru)
                productList.clear();
                int count = 0;
                for (Product product : products) {
                    if (product.isActive() && product.hasStock()) {
                        productList.add(product);
                        count++;
                    }
                    if (count >= 10) break;
                }
                productAdapter.updateProductList(productList);

                // 2. LOGIK BRAND DENGAN LOGO
                Map<String, Product> brandLogoMap = new java.util.HashMap<>();
                for (Product p : products) {
                    if (p.getBrand() != null && !p.getBrand().isEmpty()) {
                        if (!brandLogoMap.containsKey(p.getBrand())) {
                            brandLogoMap.put(p.getBrand(), p);
                        }
                    }
                }

                List<Product> uniqueBrandLogos = new ArrayList<>(brandLogoMap.values());
                brandAdapter.updateList(uniqueBrandLogos);
            }

            productsProgressBar.setVisibility(View.GONE);
            newestProductsRecyclerView.setVisibility(View.VISIBLE);
        });

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
                    for (DataSnapshot sellerSnapshot : snapshot.getChildren()) {
                        if (sellerSnapshot.hasChildren()) {
                            itemCount += sellerSnapshot.getChildrenCount();
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
                Log.w("BuyerActivity", "Failed to read cart data for badge.", error.toException());
            }
        };
        cartRef.addValueEventListener(cartListener);
    }

    private void setupNotificationBadge() {
        if (currentUser == null) {
            notificationBadge.setVisibility(View.GONE);
            return;
        }

        String userId = currentUser.getUid();
        notificationRef = FirebaseDatabase.getInstance().getReference("Notifications").child(userId);

        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unreadCount = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Boolean isUnread = ds.child("unread").getValue(Boolean.class);
                    if (isUnread != null && isUnread) {
                        unreadCount++;
                    }
                }

                if (unreadCount > 0) {
                    notificationBadge.setVisibility(View.VISIBLE);
                    notificationBadge.setText(String.valueOf(unreadCount));
                } else {
                    notificationBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BuyerActivity", "NotificationBadge Error: " + error.getMessage());
            }
        };

        notificationRef.addValueEventListener(notificationListener);
    }

    // Implementasi OnCategoryClickListener - PERUBAHAN UTAMA DI SINI
    @Override
    public void onCategoryClick(Category category) {
        // TAMBAH LOG INI
        Log.d("DEBUG_BUYER", "onCategoryClick RECEIVED: " + category.getCategoryName());

        // TEST DENGAN TOAST
        Toast.makeText(this,
                "✓ Clicked: " + category.getCategoryName(),
                Toast.LENGTH_SHORT).show();

        // Tunggu 2 saat sebelum pergi ke ShopActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, ShopActivity.class);
            intent.putExtra("CATEGORY_NAME", category.getCategoryName());
            startActivity(intent);
        }, 2000);
    }

    private void setupNewestProducts() {
        productList = new ArrayList<>();
        productAdapter = new BuyerProductAdapter(this, productList, this);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        newestProductsRecyclerView.setLayoutManager(gridLayoutManager);
        newestProductsRecyclerView.setAdapter(productAdapter);
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onBuyNowClick(Product product) {
        if (currentUser != null) {
            if (product.getStock() <= 0) {
                Toast.makeText(this, "Product is out of stock", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, CheckoutActivity.class);
            ArrayList<CartItem> itemsToCheckout = new ArrayList<>();

            String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                    ? product.getImageUrls().get(0) : "";

            double finalPrice = product.getFinalPrice();

            CartItem item = new CartItem(
                    product.getProductId(),
                    product.getName(),
                    finalPrice,
                    1,
                    imageUrl,
                    product.getSellerProfileImageUrl()
            );

            item.setSellerId(product.getSellerId());
            item.setSellerName(product.getSellerName());
            item.setSellerProfileImageUrl(product.getSellerProfileImageUrl());

            itemsToCheckout.add(item);
            intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", itemsToCheckout);
            intent.putExtra("SOURCE", "BUY_NOW");
            startActivity(intent);

        } else {
            Toast.makeText(this, "Please log in to proceed", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    @Override
    public void onAddToCartClick(Product product) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add items to your cart", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        if (!product.hasStock()) {
            Toast.makeText(this, "Product is out of stock", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String sellerId = product.getSellerId();

        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "Cannot add item: Seller information is missing.", Toast.LENGTH_LONG).show();
            Log.e("BuyerActivity", "Attempted to add product with missing sellerId: " + product.getProductId());
            return;
        }

        DatabaseReference sellerCartRef = FirebaseDatabase.getInstance()
                .getReference("Carts")
                .child(userId)
                .child(sellerId);

        String cartItemId = product.getProductId();
        DatabaseReference cartItemRef = sellerCartRef.child(cartItemId);

        cartItemRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                CartItem currentItem = mutableData.getValue(CartItem.class);

                if (currentItem == null) {
                    String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                            ? product.getImageUrls().get(0) : null;

                    CartItem newItem = new CartItem();
                    newItem.setProductId(product.getProductId());
                    newItem.setName(product.getName());
                    newItem.setPrice(product.getFinalPrice());
                    newItem.setQuantity(1);
                    newItem.setImageUrls(imageUrl);
                    newItem.setSelected(true);
                    newItem.setSellerId(sellerId);
                    newItem.setSellerName(product.getSellerName());
                    newItem.setSellerProfileImageUrl(product.getSellerProfileImageUrl());

                    mutableData.setValue(newItem);
                } else {
                    int newQuantity = currentItem.getQuantity() + 1;

                    if (newQuantity > product.getStock()) {
                        return com.google.firebase.database.Transaction.abort();
                    }

                    currentItem.setQuantity(newQuantity);
                    mutableData.setValue(currentItem);
                }
                return com.google.firebase.database.Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(BuyerActivity.this, "Failed to add to cart: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                } else if (committed) {
                    Toast.makeText(BuyerActivity.this, "Item added to cart", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BuyerActivity.this, "Maximum quantity in cart reached", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleFavouriteClick(Product product, boolean isFavourite) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to manage your favourites", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        DatabaseReference favRef = FirebaseDatabase.getInstance().getReference("Favourites")
                .child(currentUser.getUid())
                .child(product.getProductId());

        if (isFavourite) {
            favRef.setValue(true)
                    .addOnSuccessListener(aVoid -> Log.d("BuyerActivity", product.getName() + " added to favourites."))
                    .addOnFailureListener(e -> Log.e("BuyerActivity", "Failed to add favourite", e));
        } else {
            favRef.removeValue()
                    .addOnSuccessListener(aVoid -> Log.d("BuyerActivity", product.getName() + " removed from favourites."))
                    .addOnFailureListener(e -> Log.e("BuyerActivity", "Failed to remove favourite", e));
        }
    }

    @Override
    public void onFavouriteClick(Product product, boolean isFavourite) {
        handleFavouriteClick(product, isFavourite);
    }

    @Override
    public void onSellerClick(String sellerId) {
        if (sellerId == null || sellerId.isEmpty() || sellerId.startsWith("json_")) {
            Toast.makeText(this, "This is an official store, no separate seller page.", Toast.LENGTH_SHORT).show();
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
        if (notificationRef != null && notificationListener != null) {
            notificationRef.removeEventListener(notificationListener);
        }
    }
}