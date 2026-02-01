package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.CartParentAdapter;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.CartSeller;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartActivity extends AppCompatActivity implements CartParentAdapter.ParentCartListener {

    // Views
    private RecyclerView rvCartParent;
    private TextView tvTotalPrice, tvTotalItemsCount;
    private Button btnCheckout;
    private LinearLayout emptyCartView;
    private CheckBox checkboxSelectAll;
    private ProgressBar progressBar;
    private View bottomCheckoutBar;
    private TextView btnEditCart;
    private Button btnDeleteSelected;

    // Firebase & Data
    private CartParentAdapter parentAdapter;
    private List<CartSeller> sellerList;
    private DatabaseReference cartRef;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;

    // Cache untuk data produk
    private ProductViewModel productViewModel;
    private Map<String, Product> productCache;

    // Flag untuk mod suntingan
    private boolean isInEditMode = false;

    // ▼▼▼ Tambah static flag untuk tracking order completion ▼▼▼
    private static boolean isOrderCompleted = false;

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

        database = FirebaseDatabase.getInstance();
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        productCache = new HashMap<>();
        cartRef = database.getReference("Carts").child(currentUser.getUid());

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadAllProductsFirst();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ▼▼▼ Clear cart jika order telah selesai ▼▼▼
        if (isOrderCompleted) {
            clearCartAfterOrder();
            isOrderCompleted = false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // ▼▼▼ Handle intent dari CheckoutActivity ▼▼▼
        if (intent != null && intent.hasExtra("ORDER_COMPLETED")) {
            isOrderCompleted = intent.getBooleanExtra("ORDER_COMPLETED", false);
            if (isOrderCompleted) {
                clearCartAfterOrder();
            }
        }
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
        btnEditCart = findViewById(R.id.btn_edit_cart);
        btnDeleteSelected = findViewById(R.id.btn_checkout);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_cart);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Cart");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        btnEditCart.setOnClickListener(v -> toggleEditMode());
    }

    private void setupRecyclerView() {
        sellerList = new ArrayList<>();
        parentAdapter = new CartParentAdapter(this, sellerList, this);
        parentAdapter.setProductCache(productCache);
        rvCartParent.setLayoutManager(new LinearLayoutManager(this));
        rvCartParent.setAdapter(parentAdapter);
        rvCartParent.setItemAnimator(null);
    }

    private void setupListeners() {
        btnCheckout.setOnClickListener(v -> {
            if (isInEditMode) {
                confirmDeleteSelectedItems();
            } else {
                proceedToCheckout();
            }
        });

        checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                toggleSelectAll(isChecked);
            }
        });
    }

    // ▼▼▼ Kaedah untuk clear cart selepas order ▼▼▼
    private void clearCartAfterOrder() {
        if (currentUser == null) return;

        DatabaseReference userCartRef = database.getReference("Carts").child(currentUser.getUid());

        // Hapus hanya item yang telah dipesan (yang checked)
        ArrayList<CartItem> selectedItems = getSelectedItems();
        if (!selectedItems.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            for (CartItem item : selectedItems) {
                updates.put("/" + item.getSellerId() + "/" + item.getCartItemId(), null);
            }

            userCartRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("CartActivity", "Ordered items removed from cart");
                        // Reset semua selection
                        resetAllSelections();
                        Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CartActivity", "Failed to remove ordered items", e);
                    });
        }
    }

    // ▼▼▼ Kaedah untuk reset semua selection ▼▼▼
    private void resetAllSelections() {
        for (CartSeller seller : sellerList) {
            seller.setSelected(false);
            for (CartItem item : seller.getCartItems()) {
                item.setSelected(false);
            }
        }
        checkboxSelectAll.setChecked(false);
        parentAdapter.notifyDataSetChanged();
        updateTotalPrice();
    }

    private void toggleEditMode() {
        isInEditMode = !isInEditMode;
        parentAdapter.setEditMode(isInEditMode);

        if (isInEditMode) {
            btnEditCart.setText("Done");
            tvTotalPrice.setVisibility(View.GONE);
            tvTotalItemsCount.setVisibility(View.GONE);
            btnDeleteSelected.setText("Delete");
            btnDeleteSelected.setBackgroundColor(getResources().getColor(R.color.red_500));
        } else {
            btnEditCart.setText("Edit");
            tvTotalPrice.setVisibility(View.VISIBLE);
            tvTotalItemsCount.setVisibility(View.VISIBLE);
            btnDeleteSelected.setText("Checkout");
            btnDeleteSelected.setBackgroundColor(getResources().getColor(R.color.md_theme_primary));
            updateTotalPrice();
        }
    }

    private void proceedToCheckout() {
        ArrayList<CartItem> itemsForCheckout = getSelectedItems();
        if (itemsForCheckout.isEmpty()) {
            Toast.makeText(this, "Please select at least one item.", Toast.LENGTH_SHORT).show();
            return;
        }

        String validationError = validateStockForSelectedItems(itemsForCheckout);
        if (validationError != null) {
            Toast.makeText(this, validationError, Toast.LENGTH_LONG).show();
            return;
        }

        // ▼▼▼ Simpan state selection untuk digunakan semasa clear cart ▼▼▼
        saveSelectedItemsForOrder(itemsForCheckout);

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("SOURCE", "CART");
        intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", itemsForCheckout);

        // ▼▼▼ Set flag untuk clear cart selepas order ▼▼▼
        intent.putExtra("CLEAR_CART_AFTER_ORDER", true);

        startActivity(intent);
    }

    // ▼▼▼ Simpan item yang dipilih untuk order ▼▼▼
    private void saveSelectedItemsForOrder(ArrayList<CartItem> selectedItems) {
        // Anda boleh simpan di SharedPreferences atau variable static jika perlu
        // Untuk sekarang, kita akan guna static flag
    }

    private void confirmDeleteSelectedItems() {
        ArrayList<CartItem> itemsToDelete = getSelectedItems();
        if (itemsToDelete.isEmpty()) {
            Toast.makeText(this, "Select items to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to remove " + itemsToDelete.size() + " item(s)?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSelectedItems(itemsToDelete))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSelectedItems(ArrayList<CartItem> itemsToDelete) {
        Map<String, Object> updates = new HashMap<>();
        for (CartItem item : itemsToDelete) {
            updates.put("/" + item.getSellerId() + "/" + item.getCartItemId(), null);
        }

        cartRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Selected items removed.", Toast.LENGTH_SHORT).show();
                if (isInEditMode) {
                    toggleEditMode();
                }
            } else {
                Toast.makeText(this, "Failed to remove items.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllProductsFirst() {
        setLoading(true);
        productViewModel.getProductsAsMap().observe(this, productMap -> {
            if (productMap != null) {
                this.productCache = productMap;
                Log.d("CartActivity", "Product cache loaded with " + productCache.size() + " items.");
            }
            loadCartItems();
        });
        productViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Log.e("CartActivity", "Failed to load products: " + error);
            }
            if (productCache.isEmpty()) {
                loadCartItems();
            }
        });
        productViewModel.loadAllProducts();
    }

    private void loadCartItems() {
        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, List<CartItem>> itemsBySeller = new HashMap<>();

                if (!dataSnapshot.exists()) {
                    groupItemsBySeller(itemsBySeller);
                    return;
                }

                for (DataSnapshot sellerSnapshot : dataSnapshot.getChildren()) {
                    String sellerId = sellerSnapshot.getKey();
                    if (sellerId == null) continue;

                    List<CartItem> sellerItems = new ArrayList<>();
                    for (DataSnapshot itemSnapshot : sellerSnapshot.getChildren()) {
                        try {
                            if (itemSnapshot.getValue() instanceof Map) {
                                CartItem cartItem = itemSnapshot.getValue(CartItem.class);

                                if (cartItem != null && cartItem.getProductId() != null) {
                                    cartItem.setCartItemId(itemSnapshot.getKey());
                                    cartItem.setSellerId(sellerId);
                                    // ▼▼▼ Pastikan selection tidak auto true ▼▼▼
                                    cartItem.setSelected(false);
                                    enhanceCartItemWithProductInfo(cartItem);
                                    sellerItems.add(cartItem);
                                } else {
                                    Log.w("CartActivity", "Skipping null or invalid cart item: " + itemSnapshot.getKey());
                                }
                            } else {
                                Log.w("CartActivity", "Skipping unexpected data type at: " + itemSnapshot.getKey());
                            }
                        } catch (Exception e) {
                            Log.e("CartActivity", "Failed to parse cart item " + itemSnapshot.getKey(), e);
                        }
                    }

                    if (!sellerItems.isEmpty()) {
                        itemsBySeller.put(sellerId, sellerItems);
                    }
                }
                groupItemsBySeller(itemsBySeller);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                setLoading(false);
                Log.e("CartActivity", "Failed to load cart items.", databaseError.toException());
            }
        });
    }

    private void groupItemsBySeller(Map<String, List<CartItem>> itemsBySeller) {
        sellerList.clear();

        if (itemsBySeller.isEmpty()) {
            completeCartLoading(new ArrayList<>());
            return;
        }

        List<CartSeller> newSellerList = new ArrayList<>();

        for (Map.Entry<String, List<CartItem>> entry : itemsBySeller.entrySet()) {
            String sellerId = entry.getKey();
            List<CartItem> items = entry.getValue();

            String sellerName = "Unknown Seller";
            String sellerProfileImageUrl = null;

            if (!items.isEmpty()) {
                CartItem firstItem = items.get(0);
                Product productInfo = productCache.get(firstItem.getProductId());

                if (productInfo != null) {
                    sellerName = productInfo.getSellerName();
                    sellerProfileImageUrl = productInfo.getSellerProfileImageUrl();
                }
            }

            // ▼▼▼ Pastikan seller selection tidak auto true ▼▼▼
            CartSeller cartSeller = new CartSeller(sellerId, sellerName, sellerProfileImageUrl, items, false);
            newSellerList.add(cartSeller);
        }

        completeCartLoading(newSellerList);
    }

    private void completeCartLoading(List<CartSeller> newSellerList) {
        newSellerList.sort((o1, o2) -> o1.getSellerName().compareToIgnoreCase(o2.getSellerName()));
        sellerList.clear();
        sellerList.addAll(newSellerList);
        parentAdapter.notifyDataSetChanged();
        updateUIForEmptyCart(sellerList.isEmpty());

        // ▼▼▼ Reset total price dan update UI ▼▼▼
        resetAllSelections();

        setLoading(false);
        showUnavailableProductsWarning();
    }

    private void enhanceCartItemWithProductInfo(CartItem cartItem) {
        if (cartItem == null || cartItem.getProductId() == null) return;
        Product product = productCache.get(cartItem.getProductId());

        if (product == null) {
            cartItem.setName("Product Not Found");
            cartItem.setAvailable(false);
        } else {
            cartItem.setName(product.getName());
            cartItem.setPrice(product.getFinalPrice());
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                cartItem.setImageUrls(product.getImageUrls().get(0));
            }
            cartItem.setAvailable(product.isActive() && product.hasStock());
        }
    }

    private void updateUIForEmptyCart(boolean isEmpty) {
        emptyCartView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvCartParent.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        bottomCheckoutBar.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        btnEditCart.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            rvCartParent.setVisibility(View.GONE);
            bottomCheckoutBar.setVisibility(View.GONE);
            emptyCartView.setVisibility(View.GONE);
        }
    }

    private void toggleSelectAll(boolean isChecked) {
        for (CartSeller seller : sellerList) {
            seller.setSelected(isChecked);
            for (CartItem item : seller.getCartItems()) {
                // ▼▼▼ Dalam mod biasa, hanya item tersedia boleh dipilih ▼▼▼
                if (isInEditMode || item.isAvailable()) {
                    item.setSelected(isChecked);
                } else {
                    item.setSelected(false);
                }
            }
        }
        parentAdapter.notifyDataSetChanged();
        updateTotalPrice();
    }

    private void updateTotalPrice() {
        double total = 0;
        int selectedCount = 0;
        boolean allAvailableItemsSelected = !sellerList.isEmpty();
        int availableItemCount = 0;
        int totalSelectableItems = 0;

        for (CartSeller seller : sellerList) {
            boolean allSellerItemsSelected = !seller.getCartItems().isEmpty();

            for(CartItem item : seller.getCartItems()) {
                if(isInEditMode || item.isAvailable()) {
                    totalSelectableItems++;

                    if(isInEditMode) {
                        // Dalam mod edit, semua item boleh dipilih
                        if(item.isSelected()) {
                            total += item.getPrice() * item.getQuantity();
                            selectedCount++;
                        } else {
                            allSellerItemsSelected = false;
                        }
                    } else {
                        // Dalam mod biasa, hanya item tersedia diambil kira
                        if(item.isAvailable()) {
                            availableItemCount++;
                            if(item.isSelected()) {
                                total += item.getPrice() * item.getQuantity();
                                selectedCount++;
                            } else {
                                allSellerItemsSelected = false;
                                allAvailableItemsSelected = false;
                            }
                        }
                    }
                } else {
                    // Item tidak tersedia sentiasa tidak dipilih dalam mod biasa
                    item.setSelected(false);
                    allSellerItemsSelected = false;
                }
            }
            seller.setSelected(allSellerItemsSelected);
        }

        if (availableItemCount == 0 && !isInEditMode) {
            allAvailableItemsSelected = false;
        }

        tvTotalPrice.setText(String.format(Locale.US, "RM%.2f", total));
        tvTotalItemsCount.setText(String.format("Total (%d)", selectedCount));

        if (isInEditMode) {
            btnDeleteSelected.setEnabled(selectedCount > 0);
            btnDeleteSelected.setText(selectedCount > 0 ?
                    String.format("Delete (%d)", selectedCount) : "Delete");
        } else {
            btnCheckout.setEnabled(selectedCount > 0);
            btnCheckout.setText(selectedCount > 0 ?
                    String.format("Checkout (%d)", selectedCount) : "Checkout");
        }

        checkboxSelectAll.setOnCheckedChangeListener(null);
        checkboxSelectAll.setChecked(allAvailableItemsSelected && totalSelectableItems > 0);
        checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) toggleSelectAll(isChecked);
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

    private String validateStockForSelectedItems(List<CartItem> items) {
        for (CartItem item : items) {
            Product product = productCache.get(item.getProductId());
            if (product == null || !product.isActive()) {
                return "'" + item.getName() + "' is no longer available.";
            }
            if (item.getQuantity() > product.getStock()) {
                return "Not enough stock for '" + product.getName() + "'. Only " + product.getStock() + " left.";
            }
        }
        return null;
    }

    private void showUnavailableProductsWarning() {
        int unavailableCount = 0;
        for (CartSeller seller : sellerList) {
            for (CartItem item : seller.getCartItems()) {
                if (!item.isAvailable()) {
                    unavailableCount++;
                }
            }
        }
        if (unavailableCount > 0) {
            // Anda boleh tampilkan warning jika perlu
        }
    }

    @Override
    public void onSellerHeaderClicked(String sellerId) {
        if (!isInEditMode) {
            Intent intent = new Intent(this, ShopViewActivity.class);
            intent.putExtra("SELLER_ID", sellerId);
            startActivity(intent);
        }
    }

    @Override
    public void onQuantityChanged(String sellerId, String cartItemId, int newQuantity) {
        if (newQuantity <= 0) {
            CartItem itemToDelete = findCartItem(sellerId, cartItemId);
            if (itemToDelete != null) {
                ArrayList<CartItem> items = new ArrayList<>();
                items.add(itemToDelete);
                new AlertDialog.Builder(this)
                        .setTitle("Remove Item")
                        .setMessage("Do you want to remove '" + itemToDelete.getName() + "' from your cart?")
                        .setPositiveButton("Remove", (dialog, which) -> deleteSelectedItems(items))
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            parentAdapter.notifyDataSetChanged();
                        })
                        .show();
            }
        } else {
            cartRef.child(sellerId).child(cartItemId).child("quantity").setValue(newQuantity)
                    .addOnFailureListener(e -> Toast.makeText(CartActivity.this, "Failed to update quantity.", Toast.LENGTH_SHORT).show());
        }
    }

    private CartItem findCartItem(String sellerId, String cartItemId) {
        for (CartSeller seller : sellerList) {
            if (seller.getSellerId().equals(sellerId)) {
                for (CartItem item : seller.getCartItems()) {
                    if (item.getCartItemId().equals(cartItemId)) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onItemDeleted(String sellerId, String cartItemId) {
        cartRef.child(sellerId).child(cartItemId).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(CartActivity.this, "Item removed", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(CartActivity.this, "Failed to remove item", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onItemSelectedChanged() {
        updateTotalPrice();
    }
}