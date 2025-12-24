package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.CartParentAdapter;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.CartSeller;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.User;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CartActivity extends AppCompatActivity implements CartParentAdapter.ParentCartListener {

    // Views
    private RecyclerView rvCartParent;
    private TextView tvTotalPrice, tvTotalItemsCount;
    private Button btnCheckout;
    private LinearLayout emptyCartView;
    private CheckBox checkboxSelectAll;
    private ProgressBar progressBar;
    private View bottomCheckoutBar;

    // Firebase & Data
    private CartParentAdapter parentAdapter;
    private List<CartSeller> sellerList;
    private DatabaseReference cartRef;
    private ValueEventListener cartListener;
    private FirebaseUser currentUser;

    // NEW: Untuk handle products dari JSON
    private ProductViewModel productViewModel;
    private Map<String, Product> productCache;
    private Map<String, CartItem> cartItemCache; // Untuk quick lookup cart items

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view your cart.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize ViewModel dan cache
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        productCache = new HashMap<>();
        cartItemCache = new HashMap<>();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        // Load products dulu, kemudian load cart
        loadAllProductsFirst();
    }

    private void initViews() {
        rvCartParent = findViewById(R.id.rv_cart_parent);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        tvTotalItemsCount = findViewById(R.id.tv_total_items_count);
        btnCheckout = findViewById(R.id.btn_checkout);
        emptyCartView = findViewById(R.id.empty_cart_view);
        checkboxSelectAll = findViewById(R.id.checkbox_select_all_items);
        progressBar = findViewById(R.id.progress_bar_cart);
        bottomCheckoutBar = findViewById(R.id.bottom_checkout_bar);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_cart);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Cart");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        sellerList = new ArrayList<>();
        parentAdapter = new CartParentAdapter(this, sellerList, this);
        parentAdapter.setProductCache(productCache); // Pass product cache ke adapter
        rvCartParent.setLayoutManager(new LinearLayoutManager(this));
        rvCartParent.setAdapter(parentAdapter);
        rvCartParent.setItemAnimator(null);
    }

    private void setupListeners() {
        btnCheckout.setOnClickListener(v -> {
            ArrayList<CartItem> itemsForCheckout = getSelectedItems();
            if (itemsForCheckout.isEmpty()) {
                Toast.makeText(this, "Please select at least one item.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate stock sebelum checkout
            if (!validateStockBeforeCheckout(itemsForCheckout)) {
                return;
            }

            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putExtra("SOURCE", "CART");
            intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", itemsForCheckout);
            startActivity(intent);
        });

        checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                for (CartSeller seller : sellerList) {
                    for (CartItem item : seller.getCartItems()) {
                        item.setSelected(isChecked);
                    }
                }
                parentAdapter.notifyDataSetChanged();
                updateTotalPrice();
            }
        });
    }

    private void loadAllProductsFirst() {
        setLoading(true);

        productViewModel.getProducts().observe(this, products -> {
            // Cache semua products untuk quick lookup
            productCache.clear();
            for (Product product : products) {
                productCache.put(product.getProductId(), product);
            }

            // Sekarang baru load cart items
            loadCartItems();
        });

        productViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "Failed to load products: " + error, Toast.LENGTH_SHORT).show();
            }
            // Tetap cuba load cart walaupun products fail
            loadCartItems();
        });

        productViewModel.loadAllProducts();
    }

    private void loadCartItems() {
        cartRef = FirebaseDatabase.getInstance().getReference("Carts").child(currentUser.getUid());

        cartListener = cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    sellerList.clear();
                    parentAdapter.notifyDataSetChanged();
                    updateUIForEmptyCart(true);
                    setLoading(false);
                    return;
                }

                Map<String, List<CartItem>> itemsBySeller = new LinkedHashMap<>();
                cartItemCache.clear();

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    CartItem item = itemSnapshot.getValue(CartItem.class);
                    if (item != null && item.getSellerId() != null) {
                        item.setCartItemId(itemSnapshot.getKey());
                        cartItemCache.put(item.getCartItemId(), item);

                        // Enhance cart item dengan product info dari cache
                        enhanceCartItemWithProductInfo(item);

                        itemsBySeller.computeIfAbsent(item.getSellerId(), k -> new ArrayList<>()).add(item);
                    }
                }

                if (itemsBySeller.isEmpty()) {
                    updateUIForEmptyCart(true);
                    setLoading(false);
                } else {
                    groupItemsBySeller(itemsBySeller);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartActivity.this, "Failed to load cart.", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    private void enhanceCartItemWithProductInfo(CartItem cartItem) {
        String productId = cartItem.getProductId();
        Product product = productCache.get(productId);

        if (product != null) {
            // Set flag untuk product dari JSON
            cartItem.setFromJson(product.isPreloaded());

            // Update product name jika berbeza
            if (!product.getName().equals(cartItem.getProductName())) {
                cartItem.setProductName(product.getName());
            }

            // Update price jika product dari JSON (harga mungkin berubah)
            if (product.isPreloaded()) {
                double finalPrice = product.getFinalPrice();

                // Adjust untuk variant jika ada
                if (cartItem.getVariantId() != null && !cartItem.getVariantId().isEmpty()) {
                    Variant variant = product.getVariantById(cartItem.getVariantId());
                    if (variant != null) {
                        finalPrice = product.getPriceWithVariant(variant);
                        cartItem.setVariantName(variant.getName());
                        cartItem.setVariantSku(variant.getSku());
                    }
                }

                // Only update price jika berbeza lebih dari 1%
                if (Math.abs(cartItem.getPrice() - finalPrice) > 0.01) {
                    cartItem.setPrice(finalPrice);
                    // Update di Firebase juga
                    cartRef.child(cartItem.getCartItemId()).child("price").setValue(finalPrice);
                }
            }

            // Update image jika cart item tak ada image
            if ((cartItem.getImageUrl() == null || cartItem.getImageUrl().isEmpty())
                    && product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                cartItem.setImageUrl(product.getImageUrls().get(0));
                cartRef.child(cartItem.getCartItemId()).child("imageUrl").setValue(product.getImageUrls().get(0));
            }

            // Update seller info jika cart item tak ada
            if (cartItem.getSellerName() == null || cartItem.getSellerName().isEmpty()) {
                cartItem.setSellerName(product.getSellerName());
                cartRef.child(cartItem.getCartItemId()).child("sellerName").setValue(product.getSellerName());
            }
        } else {
            // Product tidak ditemui dalam cache (mungkin dah delete)
            cartItem.setProductName(cartItem.getProductName() + " (Product unavailable)");
        }
    }

    private void groupItemsBySeller(Map<String, List<CartItem>> itemsBySeller) {
        sellerList.clear();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        AtomicInteger counter = new AtomicInteger(itemsBySeller.size());

        for (Map.Entry<String, List<CartItem>> entry : itemsBySeller.entrySet()) {
            String sellerId = entry.getKey();
            List<CartItem> items = entry.getValue();

            // Check jika seller adalah "system" (products dari JSON)
            if ("system".equals(sellerId)) {
                // Untuk system/JSON products, guna default info
                sellerList.add(new CartSeller(
                        sellerId,
                        "BeautyHub Official Store",
                        "https://res.cloudinary.com/.../beautyhub_logo.jpg",
                        items
                ));

                if (counter.decrementAndGet() == 0) {
                    completeCartLoading();
                }
            } else {
                // Untuk seller biasa, fetch dari Firebase
                usersRef.child(sellerId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                        String sellerName = "Unknown Seller";
                        String sellerProfileUrl = null;
                        if (userSnapshot.exists()) {
                            User seller = userSnapshot.getValue(User.class);
                            if (seller != null) {
                                sellerName = seller.getUsername();
                                sellerProfileUrl = seller.getProfileImage();
                            }
                        }
                        sellerList.add(new CartSeller(sellerId, sellerName, sellerProfileUrl, items));

                        if (counter.decrementAndGet() == 0) {
                            completeCartLoading();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        sellerList.add(new CartSeller(sellerId, "Error Loading Name", null, items));
                        if (counter.decrementAndGet() == 0) {
                            completeCartLoading();
                        }
                    }
                });
            }
        }
    }

    private void completeCartLoading() {
        parentAdapter.notifyDataSetChanged();
        updateUIForEmptyCart(false);
        updateTotalPrice();
        setLoading(false);

        // Show warning untuk products yang unavailable
        showUnavailableProductsWarning();
    }

    private void showUnavailableProductsWarning() {
        int unavailableCount = 0;
        for (CartSeller seller : sellerList) {
            for (CartItem item : seller.getCartItems()) {
                Product product = productCache.get(item.getProductId());
                if (product == null || !product.isActive()) {
                    unavailableCount++;
                } else if (product.hasVariants() && item.getVariantId() != null) {
                    // Check jika variant masih available
                    Variant variant = product.getVariantById(item.getVariantId());
                    if (variant == null || variant.getStock() <= 0) {
                        unavailableCount++;
                    }
                } else if (!product.hasVariants() && product.getStock() <= 0) {
                    unavailableCount++;
                }
            }
        }

        if (unavailableCount > 0) {
            Toast.makeText(this,
                    unavailableCount + " item(s) in your cart are currently unavailable",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateUIForEmptyCart(boolean isEmpty) {
        emptyCartView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvCartParent.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        bottomCheckoutBar.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            rvCartParent.setVisibility(View.GONE);
            bottomCheckoutBar.setVisibility(View.GONE);
            emptyCartView.setVisibility(View.GONE);
        }
    }

    private void updateTotalPrice() {
        double total = 0;
        int selectedCount = 0;
        int totalItemCountInCart = 0;
        boolean allItemsSelected = true;

        if (sellerList.isEmpty()) {
            allItemsSelected = false;
        } else {
            for (CartSeller seller : sellerList) {
                for (CartItem item : seller.getCartItems()) {
                    totalItemCountInCart++;
                    if (item.isSelected()) {
                        total += item.getPrice() * item.getQuantity();
                        selectedCount++;
                    } else {
                        allItemsSelected = false;
                    }
                }
            }
        }

        if (totalItemCountInCart == 0) {
            allItemsSelected = false;
        }

        tvTotalPrice.setText(String.format(Locale.US, "RM%.2f", total));
        tvTotalItemsCount.setText(String.format(Locale.US, "Total (%d)", selectedCount));
        btnCheckout.setEnabled(selectedCount > 0);
        btnCheckout.setText(String.format("Checkout (%d)", selectedCount));

        checkboxSelectAll.setOnCheckedChangeListener(null);
        checkboxSelectAll.setChecked(allItemsSelected);
        checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                for (CartSeller seller : sellerList) {
                    for (CartItem item : seller.getCartItems()) {
                        item.setSelected(isChecked);
                    }
                }
                parentAdapter.notifyDataSetChanged();
                updateTotalPrice();
            }
        });
    }

    private ArrayList<CartItem> getSelectedItems() {
        ArrayList<CartItem> selectedItems = new ArrayList<>();
        for (CartSeller seller : sellerList) {
            for (CartItem item : seller.getCartItems()) {
                if (item.isSelected()) {
                    selectedItems.add(item);
                }
            }
        }
        return selectedItems;
    }

    private boolean validateStockBeforeCheckout(ArrayList<CartItem> items) {
        for (CartItem item : items) {
            Product product = productCache.get(item.getProductId());
            if (product == null || !product.isActive()) {
                Toast.makeText(this,
                        item.getDisplayName() + " is no longer available",
                        Toast.LENGTH_SHORT).show();
                return false;
            }

            if (product.hasVariants() && item.getVariantId() != null) {
                // Check variant stock
                Variant variant = product.getVariantById(item.getVariantId());
                if (variant == null) {
                    Toast.makeText(this,
                            "Selected variant for " + item.getDisplayName() + " is unavailable",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (variant.getStock() < item.getQuantity()) {
                    Toast.makeText(this,
                            "Only " + variant.getStock() + " left for " + item.getDisplayName(),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else if (!product.hasVariants()) {
                // Check product stock
                if (product.getStock() < item.getQuantity()) {
                    Toast.makeText(this,
                            "Only " + product.getStock() + " left for " + item.getDisplayName(),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartRef != null && cartListener != null) {
            cartRef.removeEventListener(cartListener);
        }
    }

    // --- Implementasi dari ParentCartListener ---

    @Override
    public void onSellerHeaderClicked(String sellerId) {
        if ("system".equals(sellerId)) {
            Toast.makeText(this, "Official Store - All products are verified", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SellerProfileActivity.class);
        intent.putExtra("SELLER_ID", sellerId);
        startActivity(intent);
    }

    @Override
    public void onQuantityChanged(String cartItemId, int newQuantity) {
        if (cartItemId == null) return;

        // Check stock limit sebelum update quantity
        if (newQuantity > 0) {
            CartItem item = cartItemCache.get(cartItemId);
            if (item != null) {
                Product product = productCache.get(item.getProductId());
                if (product != null) {
                    int maxQuantity = getMaxQuantityForItem(product, item.getVariantId());
                    if (newQuantity > maxQuantity) {
                        Toast.makeText(this,
                                "Maximum quantity is " + maxQuantity + " for this item",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        }

        if (newQuantity <= 0) {
            onItemDeleted(cartItemId);
        } else {
            cartRef.child(cartItemId).child("quantity").setValue(newQuantity);
        }
    }

    @Override
    public void onVariantChanged(String cartItemId, String newVariantId) {
        CartItem item = cartItemCache.get(cartItemId);
        if (item == null) return;

        Product product = productCache.get(item.getProductId());
        if (product == null) return;

        Variant variant = product.getVariantById(newVariantId);
        if (variant == null) return;

        // Update cart item dengan variant baru
        item.setVariantId(newVariantId);
        item.setVariantName(variant.getName());
        item.setVariantSku(variant.getSku());

        // Update price berdasarkan variant
        double newPrice = product.getPriceWithVariant(variant);
        item.setPrice(newPrice);

        // Update di Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("variantId", newVariantId);
        updates.put("variantName", variant.getName());
        updates.put("price", newPrice);

        cartRef.child(cartItemId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Variant updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update variant", Toast.LENGTH_SHORT).show();
                });
    }

    private int getMaxQuantityForItem(Product product, String variantId) {
        if (variantId != null && !variantId.isEmpty()) {
            Variant variant = product.getVariantById(variantId);
            return variant != null ? variant.getStock() : 0;
        } else {
            return product.getStock();
        }
    }

    @Override
    public void onItemDeleted(String cartItemId) {
        if (cartItemId == null) return;

        new android.app.AlertDialog.Builder(this)
                .setTitle("Remove Item")
                .setMessage("Are you sure you want to remove this item from cart?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    cartRef.child(cartItemId).removeValue();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onItemSelectedChanged() {
        updateTotalPrice();
    }
}