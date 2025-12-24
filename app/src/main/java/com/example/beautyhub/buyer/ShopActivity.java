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
import com.example.beautyhub.models.Variant;
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
        BuyerProductAdapter.OnProductInteractionListener,
        VariantSelectionBottomSheet.VariantSelectionListener {

    private static final String TAG = "ShopActivity";

    // UI Components
    private RecyclerView allProductsRecyclerView;
    private ProgressBar progressBar;
    private TextView tvNoProducts;
    private SearchView searchView;

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

    // ViewModel untuk produk
    private ProductViewModel productViewModel;

    // Cache untuk semua produk
    private List<Product> allProductsCache = new ArrayList<>();

    // Flag untuk membezakan tindakan dari BottomSheet
    private boolean isForBuyNow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize ViewModel
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        initViews();
        setupToolbar();
        setupRecyclerView();
        handleIncomingIntent();

        // Load produk menggunakan ViewModel
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
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_shop);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up search functionality
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

        findViewById(R.id.btn_filter_icon).setOnClickListener(v -> {
            try {
                FilterBottomSheetDialog bottomSheet = FilterBottomSheetDialog.newInstance(
                        currentCategoryFilter,
                        currentBrandFilter,
                        currentMinPrice,
                        currentMaxPrice,
                        currentSkinTypeFilter,
                        currentIngredientFilter
                );
                bottomSheet.show(getSupportFragmentManager(), FilterBottomSheetDialog.TAG);
            } catch (Exception e) {
                Log.e(TAG, "Error showing filter bottom sheet: " + e.getMessage());
                Toast.makeText(this, "Filter feature not available yet", Toast.LENGTH_SHORT).show();
            }
        });
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
        if (intent != null && intent.hasExtra("CATEGORY_NAME")) {
            String categoryFromIntent = intent.getStringExtra("CATEGORY_NAME");
            if (categoryFromIntent != null && !categoryFromIntent.isEmpty()) {
                currentCategoryFilter = categoryFromIntent;
                MaterialToolbar toolbar = findViewById(R.id.toolbar_shop);
                toolbar.setTitle(categoryFromIntent);
            }
        }
    }

    private void filterProducts() {
        allProductsList.clear();

        for (Product product : allProductsCache) {
            // --- START OF FIX ---
            // Replace the complex stock check with this single line
            boolean hasStock = product.hasStock();
            // --- END OF FIX ---

            if (product.isActive() && hasStock && matchesAllFilters(product)) {
                allProductsList.add(product);
            }
        }
        updateUiAfterFilter();
    }


    private boolean matchesAllFilters(Product product) {
        // Search filter
        boolean matchesSearch = currentSearchQuery.isEmpty() ||
                (product.getName() != null && product.getName().toLowerCase().contains(currentSearchQuery)) ||
                (product.getDescription() != null && product.getDescription().toLowerCase().contains(currentSearchQuery)) ||
                (product.getBrand() != null && product.getBrand().toLowerCase().contains(currentSearchQuery));

        // Category filter
        boolean matchesCategory = currentCategoryFilter.equalsIgnoreCase("All") ||
                (product.getCategory() != null && product.getCategory().equalsIgnoreCase(currentCategoryFilter));

        // Brand filter
        boolean matchesBrand = currentBrandFilter.isEmpty() ||
                (product.getBrand() != null && product.getBrand().equalsIgnoreCase(currentBrandFilter));

        // Price filter
        double priceToCheck = product.hasDiscount() ? product.getDiscountPrice() : product.getPrice();
        boolean matchesPrice = priceToCheck >= currentMinPrice && priceToCheck <= currentMaxPrice;

        // Skin type filter
        boolean matchesSkinType = currentSkinTypeFilter.equalsIgnoreCase("All") ||
                (product.getSkinType() != null && product.getSkinType().toLowerCase().contains(currentSkinTypeFilter.toLowerCase()));

        // Ingredient filter
        boolean matchesIngredient = currentIngredientFilter.isEmpty() ||
                (product.getIngredients() != null && product.getIngredients().toLowerCase().contains(currentIngredientFilter.toLowerCase()));

        return matchesSearch && matchesCategory && matchesBrand && matchesPrice && matchesSkinType && matchesIngredient;
    }

    private void updateUiAfterFilter() {
        buyerProductAdapter.updateProductList(allProductsList);

        if (allProductsList.isEmpty()) {
            tvNoProducts.setVisibility(View.VISIBLE);
            allProductsRecyclerView.setVisibility(View.GONE);
            tvNoProducts.setText("No products found. Try adjusting your filters.");
        } else {
            tvNoProducts.setVisibility(View.GONE);
            allProductsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void listenToFavourites() {
        if (currentUser == null) return;
        favouritesRef = FirebaseDatabase.getInstance().getReference("Favourites").child(currentUser.getUid());

        if (favouritesListener != null) {
            favouritesRef.removeEventListener(favouritesListener);
        }

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

    // FilterListener implementation
    @Override
    public void onFilterApplied(String category, String brand, float minPrice, float maxPrice, String skinType, String ingredient) {
        this.currentCategoryFilter = category;
        this.currentBrandFilter = brand;
        this.currentMinPrice = minPrice;
        this.currentMaxPrice = maxPrice;
        this.currentSkinTypeFilter = skinType;
        this.currentIngredientFilter = ingredient;

        if (!category.equals("All")) {
            ((MaterialToolbar) findViewById(R.id.toolbar_shop)).setTitle(category);
        }
        filterProducts();
        Toast.makeText(this, "Filters applied!", Toast.LENGTH_SHORT).show();
    }

    // OnProductInteractionListener implementation
    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onAddToCartClick(Product product, Variant variant) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add items to your cart.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (product.hasVariants() && variant == null) {
            try {
                showVariantSelectionDialog(product, false);
            } catch (Exception e) {
                Log.e(TAG, "Error showing variant selection: " + e.getMessage());
                Toast.makeText(this, "Please select a variant first", Toast.LENGTH_SHORT).show();
            }
        } else {
            addToCartDirectly(product, variant, 1);
        }
    }

    @Override
    public void onBuyNowClick(Product product, Variant variant) {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to purchase.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check stock
        int stock = (variant != null) ? variant.getStock() : product.getStock();
        if (stock <= 0) {
            Toast.makeText(this, "This item is out of stock.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (product.hasVariants() && variant == null) {
            try {
                showVariantSelectionDialog(product, true);
            } catch (Exception e) {
                Log.e(TAG, "Error showing variant selection: " + e.getMessage());
                Toast.makeText(this, "Please select a variant first", Toast.LENGTH_SHORT).show();
            }
        } else {
            proceedToCheckout(product, variant, 1);
        }
    }

    // VariantSelectionListener implementation
    @Override
    public void onVariantSelected(Product product, Variant variant, int quantity) {
        if (isForBuyNow) {
            proceedToCheckout(product, variant, quantity);
        } else {
            addToCartDirectly(product, variant, quantity);
        }
    }

    private void showVariantSelectionDialog(Product product, boolean isBuyNow) {
        this.isForBuyNow = isBuyNow;

        // Pass the product ID to the bottom sheet
        try {
            VariantSelectionBottomSheet bottomSheet = VariantSelectionBottomSheet.newInstance(product.getProductId());
            bottomSheet.setVariantSelectionListener(this);
            bottomSheet.setBuyNowMode(isBuyNow);
            bottomSheet.show(getSupportFragmentManager(), VariantSelectionBottomSheet.TAG);
        } catch (Exception e) {
            Log.e(TAG, "Error creating variant selection dialog: " + e.getMessage());
            // Fallback: Show an AlertDialog for variant selection
            showFallbackVariantSelection(product, isBuyNow);
        }
    }

    private void showFallbackVariantSelection(Product product, boolean isBuyNow) {
        // Fallback method if the bottom sheet is not available
        if (product.hasVariants()) {
            // Create a simple dialog for variant selection
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Select Variant");

            String[] variantNames = new String[product.getVariants().size()];
            for (int i = 0; i < product.getVariants().size(); i++) {
                Variant variant = product.getVariants().get(i);
                variantNames[i] = variant.getName() + " (Stock: " + variant.getStock() + ")";
            }

            builder.setItems(variantNames, (dialog, which) -> {
                Variant selectedVariant = product.getVariants().get(which);
                if (isBuyNow) {
                    proceedToCheckout(product, selectedVariant, 1);
                } else {
                    addToCartDirectly(product, selectedVariant, 1);
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
    }

    private void addToCartDirectly(Product product, Variant variant, int quantity) {
        if (currentUser == null) return;

        // Validate stock
        int availableStock = (variant != null) ? variant.getStock() : product.getStock();
        if (availableStock < quantity) {
            Toast.makeText(this, "Not enough stock available. Only " + availableStock + " items left.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Carts").child(currentUser.getUid());
        String cartItemId = product.getProductId();

        // If variant exists, append variant ID to cart item ID
        if (variant != null && variant.getId() != null && !variant.getId().isEmpty()) {
            cartItemId += "_" + variant.getId();
        }

        DatabaseReference cartItemRef = cartRef.child(cartItemId);

        cartItemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double finalPrice = product.getPriceWithVariant(variant);

                if (snapshot.exists()) {
                    // Item already exists, update quantity
                    int currentQuantity = snapshot.child("quantity").getValue(Integer.class);
                    cartItemRef.child("quantity").setValue(currentQuantity + quantity);
                    Toast.makeText(ShopActivity.this, "Updated cart quantity!", Toast.LENGTH_SHORT).show();
                } else {
                    // New item, create CartItem object
                    String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) ?
                            product.getImageUrls().get(0) : "";

                    CartItem cartItem = new CartItem(
                            product.getProductId(),
                            product.getName(),
                            finalPrice,
                            quantity,
                            imageUrl
                    );

                    // Set variant information if available
                    if (variant != null) {
                        cartItem.setVariantId(variant.getId());
                        cartItem.setVariantName(variant.getName());
                    }

                    // Set seller information
                    cartItem.setSellerId(product.getSellerId() != null ? product.getSellerId() : "system");
                    cartItem.setSellerName(product.getSellerName() != null ? product.getSellerName() : "BeautyHub Official Store");
                    cartItem.setFromJson(product.isPreloaded());
                    cartItem.setHasVariants(product.hasVariants());

                    cartItemRef.setValue(cartItem);
                    Toast.makeText(ShopActivity.this, "Added to cart!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ShopActivity.this, "Failed to add to cart.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Add to cart failed: " + error.getMessage());
            }
        });
    }

    private void proceedToCheckout(Product product, Variant variant, int quantity) {
        // Validate stock before proceeding
        int availableStock = (variant != null) ? variant.getStock() : product.getStock();
        if (availableStock < quantity) {
            Toast.makeText(this, "Not enough stock available. Only " + availableStock + " items left.", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) ?
                product.getImageUrls().get(0) : "";
        double finalPrice = product.getPriceWithVariant(variant);

        CartItem buyNowItem = new CartItem(
                product.getProductId(),
                product.getName(),
                finalPrice,
                quantity,
                imageUrl
        );

        // Set variant information if available
        if (variant != null) {
            buyNowItem.setVariantId(variant.getId());
            buyNowItem.setVariantName(variant.getName());
        }

        // Set seller information
        buyNowItem.setSellerId(product.getSellerId() != null ? product.getSellerId() : "system");
        buyNowItem.setSellerName(product.getSellerName() != null ? product.getSellerName() : "BeautyHub Official Store");
        buyNowItem.setFromJson(product.isPreloaded());
        buyNowItem.setHasVariants(product.hasVariants());

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
            favRef.setValue(true).addOnSuccessListener(aVoid ->
                    Toast.makeText(this, "Added to Favourites", Toast.LENGTH_SHORT).show());
        } else {
            favRef.removeValue().addOnSuccessListener(aVoid ->
                    Toast.makeText(this, "Removed from Favourites", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onSellerClick(String sellerId) {
        if ("system".equals(sellerId)) {
            Toast.makeText(this, "This is an official BeautyHub product.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, SellerProfileActivity.class);
        intent.putExtra("SELLER_ID", sellerId);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (favouritesRef != null && favouritesListener != null) {
            favouritesRef.removeEventListener(favouritesListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners
        if (favouritesRef != null && favouritesListener != null) {
            favouritesRef.removeEventListener(favouritesListener);
        }
    }
}