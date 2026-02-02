package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.BuyerProductAdapter;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.seller.SellerProfileActivity;
import com.example.beautyhub.ui.ProductViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity implements
        FilterBottomSheetDialog.FilterListener,
        BuyerProductAdapter.OnProductInteractionListener {

    private static final String TAG = "ShopActivity";

    // UI Components
    private RecyclerView allProductsRecyclerView;
    private ProgressBar progressBar;
    private TextView tvNoProducts;
    private SearchView searchView;
    private View cartIconLayout;
    private TextView cartBadge;

    // Adapter and Data List
    private BuyerProductAdapter buyerProductAdapter;
    private List<Product> allProductsList;

    // Filter State Variables
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";
    private String currentBrandFilter = "";
    private float currentMinPrice = 10f;
    private float currentMaxPrice = 200f;
    private String currentSkinTypeFilter = "All";
    private String currentIngredientFilter = "";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private ValueEventListener favouritesListener;
    private DatabaseReference favouritesRef;
    private DatabaseReference cartRef;
    private ValueEventListener cartListener;

    // ViewModel untuk produk
    private ProductViewModel productViewModel;

    // Cache untuk semua produk
    private List<Product> allProductsCache = new ArrayList<>();
    private List<String> currentSubCategoryFilter = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        initViews();
        setupToolbar();
        setupRecyclerView();
        handleIncomingIntent();
        setupCartBadge();

        loadProductsWithViewModel();

        if (currentUser != null) {
            listenToFavourites();
        }
    }

    private void initViews() {
        allProductsRecyclerView = findViewById(R.id.rv_all_products);
        progressBar = findViewById(R.id.progress_bar_shop);
        tvNoProducts = findViewById(R.id.tv_no_products_shop);
        searchView = findViewById(R.id.search_view_shop);
        cartIconLayout = findViewById(R.id.btn_cart_icon);
        cartBadge = findViewById(R.id.cart_badge_shop);
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
                Log.w(TAG, "Failed to read cart data for badge.", error.toException());
                cartBadge.setVisibility(View.GONE);
            }
        };
        cartRef.addValueEventListener(cartListener);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_shop);
        toolbar.setNavigationOnClickListener(v -> finish());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query.trim().toLowerCase();
                filterProducts();
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty() && !currentSearchQuery.isEmpty()) {
                    currentSearchQuery = "";
                    filterProducts();
                }
                return true;
            }
        });

        findViewById(R.id.btn_cart_icon).setOnClickListener(v ->
                startActivity(new Intent(ShopActivity.this, CartActivity.class)));

        // FILTER BUTTON - FIXED
        // DI SHOP ACTIVITY - ganti showNow() dengan show()
        findViewById(R.id.btn_filter_icon).setOnClickListener(v -> {
            FilterBottomSheetDialog filterDialog = FilterBottomSheetDialog.newInstance(
                    currentCategoryFilter,
                    new ArrayList<>(currentSubCategoryFilter),
                    currentBrandFilter,
                    currentMinPrice,
                    currentMaxPrice,
                    currentSkinTypeFilter,
                    currentIngredientFilter
            );

            // GANTI: showNow() -> show()
            filterDialog.show(getSupportFragmentManager(), FilterBottomSheetDialog.TAG);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (favouritesRef != null && favouritesListener != null) {
            favouritesRef.removeEventListener(favouritesListener);
        }
        if (cartRef != null && cartListener != null) {
            cartRef.removeEventListener(cartListener);
        }
    }

    private void setupRecyclerView() {
        allProductsList = new ArrayList<>();
        buyerProductAdapter = new BuyerProductAdapter(this, allProductsList, this);
        allProductsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        allProductsRecyclerView.setAdapter(buyerProductAdapter);
    }

    private void loadProductsWithViewModel() {
        progressBar.setVisibility(View.VISIBLE);
        allProductsRecyclerView.setVisibility(View.GONE);
        tvNoProducts.setVisibility(View.GONE);

        productViewModel.getProducts().observe(this, products -> {
            allProductsCache.clear();
            allProductsCache.addAll(products);
            filterProducts();
        });

        productViewModel.getError().observe(this, error -> {
            progressBar.setVisibility(View.GONE);
            if (error != null && !error.isEmpty()) {
                Toast.makeText(ShopActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        productViewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        productViewModel.loadAllProducts();
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        MaterialToolbar toolbar = findViewById(R.id.toolbar_shop);

        if (intent.hasExtra("SUB_CATEGORIES")) {
            currentSubCategoryFilter = intent.getStringArrayListExtra("SUB_CATEGORIES");
            String parentCategoryName = intent.getStringExtra("PARENT_CATEGORY_NAME");

            if (parentCategoryName != null) {
                toolbar.setTitle(parentCategoryName);
                currentCategoryFilter = "All";
            }
        } else if (intent.hasExtra("CATEGORY_NAME")) {
            String categoryFromIntent = intent.getStringExtra("CATEGORY_NAME");
            if (categoryFromIntent != null && !categoryFromIntent.isEmpty()) {
                currentCategoryFilter = categoryFromIntent;
                toolbar.setTitle(categoryFromIntent);
            }
        }
    }

    private void filterProducts() {
        allProductsList.clear();

        for (Product product : allProductsCache) {
            if (product.isActive() && product.hasStock() && matchesAllFilters(product)) {
                allProductsList.add(product);
            }
        }
        updateUiAfterFilter();
    }

    private boolean matchesAllFilters(Product product) {
        boolean matchesSearch = currentSearchQuery.isEmpty() ||
                (product.getName() != null && product.getName().toLowerCase().contains(currentSearchQuery)) ||
                (product.getBrand() != null && product.getBrand().toLowerCase().contains(currentSearchQuery)) ||
                (product.getSellerName() != null && product.getSellerName().toLowerCase().contains(currentSearchQuery));

        boolean matchesCategory;
        if (currentSubCategoryFilter != null && !currentSubCategoryFilter.isEmpty()) {
            matchesCategory = product.getCategory() != null && currentSubCategoryFilter.contains(product.getCategory());
        } else {
            matchesCategory = currentCategoryFilter.equalsIgnoreCase("All") ||
                    (product.getCategory() != null && product.getCategory().equalsIgnoreCase(currentCategoryFilter));
        }

        boolean matchesBrand = currentBrandFilter.isEmpty() ||
                (product.getBrand() != null && product.getBrand().equalsIgnoreCase(currentBrandFilter));

        boolean matchesPrice = product.getFinalPrice() >= currentMinPrice && product.getFinalPrice() <= currentMaxPrice;

        boolean matchesSkinType = currentSkinTypeFilter.equalsIgnoreCase("All") ||
                (product.getSkinType() != null && product.getSkinType().toLowerCase().contains(currentSkinTypeFilter.toLowerCase()));

        boolean matchesIngredient = currentIngredientFilter.isEmpty() ||
                (product.getIngredients() != null && product.getIngredients().toLowerCase().contains(currentIngredientFilter.toLowerCase()));

        return matchesSearch && matchesCategory && matchesBrand && matchesPrice && matchesSkinType && matchesIngredient;
    }

    private void updateUiAfterFilter() {
        buyerProductAdapter.updateProductList(allProductsList);

        if (allProductsList.isEmpty()) {
            tvNoProducts.setVisibility(View.VISIBLE);
            allProductsRecyclerView.setVisibility(View.GONE);
        } else {
            tvNoProducts.setVisibility(View.GONE);
            allProductsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void listenToFavourites() {
        if (currentUser == null) return;
        favouritesRef = FirebaseDatabase.getInstance().getReference("Favourites").child(currentUser.getUid());

        favouritesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> favouriteIds = new ArrayList<>();
                for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                    if (Boolean.TRUE.equals(idSnapshot.getValue(Boolean.class))) {
                        favouriteIds.add(idSnapshot.getKey());
                    }
                }
                if (buyerProductAdapter != null) {
                    buyerProductAdapter.setFavouriteProductIds(favouriteIds);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to listen to favourites: " + error.getMessage());
            }
        };
        favouritesRef.addValueEventListener(favouritesListener);
    }

    @Override
    public void onFilterApplied(String category, List<String> subCategories, String brand,
                                float minPrice, float maxPrice, String skinType, String ingredient) {
        if (subCategories != null && !subCategories.isEmpty()) {
            this.currentCategoryFilter = "All";
            this.currentSubCategoryFilter = subCategories;
        } else {
            this.currentCategoryFilter = category;
            this.currentSubCategoryFilter.clear();
        }

        this.currentBrandFilter = brand;
        this.currentMinPrice = minPrice;
        this.currentMaxPrice = maxPrice;
        this.currentSkinTypeFilter = skinType;
        this.currentIngredientFilter = ingredient;

        if (!category.equals("All")) {
            ((MaterialToolbar) findViewById(R.id.toolbar_shop)).setTitle(category);
        } else if (getIntent().hasExtra("PARENT_CATEGORY_NAME")) {
            ((MaterialToolbar) findViewById(R.id.toolbar_shop)).setTitle(getIntent().getStringExtra("PARENT_CATEGORY_NAME"));
        } else {
            ((MaterialToolbar) findViewById(R.id.toolbar_shop)).setTitle("Shop");
        }

        filterProducts();
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onAddToCartClick(Product product) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add items to your cart.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!product.hasStock()) {
            Toast.makeText(this, "This item is out of stock.", Toast.LENGTH_SHORT).show();
            return;
        }

        addToCartDirectly(product, 1);
    }

    @Override
    public void onBuyNowClick(Product product) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to purchase.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!product.hasStock()) {
            Toast.makeText(this, "This item is out of stock.", Toast.LENGTH_SHORT).show();
            return;
        }

        proceedToCheckout(product, 1);
    }

    private void addToCartDirectly(Product product, int quantity) {
        String sellerId = product.getSellerId();
        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "Cannot add to cart: Seller info is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartItemRef = FirebaseDatabase.getInstance()
                .getReference("Carts")
                .child(currentUser.getUid())
                .child(sellerId)
                .child(product.getProductId());

        cartItemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer currentQuantity = snapshot.child("quantity").getValue(Integer.class);
                    int newQuantity = (currentQuantity != null ? currentQuantity : 0) + quantity;

                    if (newQuantity > product.getStock()) {
                        Toast.makeText(ShopActivity.this, "Maximum stock reached!", Toast.LENGTH_SHORT).show();
                        cartItemRef.child("quantity").setValue(product.getStock());
                    } else {
                        cartItemRef.child("quantity").setValue(newQuantity);
                        Toast.makeText(ShopActivity.this, "Cart updated!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) ?
                            product.getImageUrls().get(0) : "";

                    CartItem cartItem = new CartItem(
                            product.getProductId(),
                            product.getName(),
                            product.getFinalPrice(),
                            quantity,
                            imageUrl,
                            product.getSellerProfileImageUrl()
                    );
                    cartItem.setSellerId(sellerId);
                    cartItem.setSellerName(product.getSellerName());
                    cartItem.setCartItemId(product.getProductId());

                    cartItemRef.setValue(cartItem)
                            .addOnSuccessListener(aVoid -> Toast.makeText(ShopActivity.this, "Added to cart!", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ShopActivity.this, "Failed to add to cart.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void proceedToCheckout(Product product, int quantity) {
        String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) ?
                product.getImageUrls().get(0) : "";

        CartItem buyNowItem = new CartItem(
                product.getProductId(),
                product.getName(),
                product.getFinalPrice(),
                quantity,
                imageUrl,
                product.getSellerProfileImageUrl()
        );
        buyNowItem.setSellerId(product.getSellerId());
        buyNowItem.setSellerName(product.getSellerName());
        buyNowItem.setCartItemId(product.getProductId());

        ArrayList<CartItem> itemsForCheckout = new ArrayList<>();
        itemsForCheckout.add(buyNowItem);

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("SOURCE", "BUY_NOW");
        intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", itemsForCheckout);
        startActivity(intent);
    }

    @Override
    public void onFavouriteClick(Product product, boolean isFavourite) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to manage favourites.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference favRef = FirebaseDatabase.getInstance().getReference("Favourites")
                .child(currentUser.getUid())
                .child(product.getProductId());

        if (isFavourite) {
            favRef.setValue(true);
        } else {
            favRef.removeValue();
        }
    }

    @Override
    public void onSellerClick(String sellerId) {
        if (sellerId == null || sellerId.isEmpty() || sellerId.startsWith("json_")) {
            Toast.makeText(this, "This is an official store.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, SellerProfileActivity.class);
        intent.putExtra("SELLER_ID", sellerId);
        startActivity(intent);
    }
}