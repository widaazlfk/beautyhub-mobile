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
import androidx.appcompat.app.AlertDialog; // ▼▼▼ Import AlertDialog ▼▼▼
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
    private TextView btnEditCart; // ▼▼▼ Tambah rujukan untuk butang Edit ▼▼▼
    private Button btnDeleteSelected; // ▼▼▼ Butang baru untuk mod suntingan ▼▼▼

    // Firebase & Data
    private CartParentAdapter parentAdapter;
    private List<CartSeller> sellerList;
    private DatabaseReference cartRef;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;

    // Cache untuk data produk
    private ProductViewModel productViewModel;
    private Map<String, Product> productCache;

    // ▼▼▼ Flag untuk mod suntingan ▼▼▼
    private boolean isInEditMode = false;

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

    private void initViews() {
        rvCartParent = findViewById(R.id.rv_cart_parent);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        tvTotalItemsCount = findViewById(R.id.tv_total_items_count);
        btnCheckout = findViewById(R.id.btn_checkout);
        emptyCartView = findViewById(R.id.empty_cart_view);
        checkboxSelectAll = findViewById(R.id.checkbox_select_all_items);
        progressBar = findViewById(R.id.progress_bar_cart);
        bottomCheckoutBar = findViewById(R.id.bottom_checkout_bar);
        btnEditCart = findViewById(R.id.btn_edit_cart); // ▼▼▼ Inisialisasi butang Edit ▼▼▼

        // ▼▼▼ Inisialisasi butang Delete (re-use butang Checkout) ▼▼▼
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

        // ▼▼▼ Listener untuk butang Edit/Done ▼▼▼
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
        // Listener ini kini dikawal oleh mod suntingan
        btnCheckout.setOnClickListener(v -> {
            if (isInEditMode) {
                // Dalam mod suntingan, butang ini berfungsi sebagai "Delete"
                confirmDeleteSelectedItems();
            } else {
                // Dalam mod biasa, butang ini berfungsi sebagai "Checkout"
                proceedToCheckout();
            }
        });

        checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                toggleSelectAll(isChecked);
            }
        });
    }

    // ▼▼▼ Kaedah untuk mengawal mod suntingan ▼▼▼
    private void toggleEditMode() {
        isInEditMode = !isInEditMode;
        parentAdapter
                .setEditMode(isInEditMode); // Beritahu adapter tentang perubahan mod

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
            btnDeleteSelected.setBackgroundColor(getResources().getColor(R.color.md_theme_primary)); // Kembali ke warna asal
            updateTotalPrice(); // Kemas kini UI untuk mod biasa
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

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("SOURCE", "CART");
        intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", itemsForCheckout);
        startActivity(intent);
    }

    // ▼▼▼ Kaedah untuk mengesahkan dan memadam item terpilih ▼▼▼
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
            // Laluan untuk memadam: /Carts/{userId}/{sellerId}/{cartItemId}
            updates.put("/" + item.getSellerId() + "/" + item.getCartItemId(), null);
        }

        cartRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Selected items removed.", Toast.LENGTH_SHORT).show();
                // Keluar dari mod suntingan secara automatik selepas memadam
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
            // Teruskan muat troli walaupun data produk gagal dimuat
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
                        // ▼▼▼ FIX: Add robust deserialization ▼▼▼
                        try {
                            // Check if the snapshot contains a valid object (Map) before deserializing
                            if (itemSnapshot.getValue() instanceof Map) {
                                CartItem cartItem = itemSnapshot.getValue(CartItem.class);

                                // Ensure the deserialized object is valid and has a product ID
                                if (cartItem != null && cartItem.getProductId() != null) {
                                    cartItem.setCartItemId(itemSnapshot.getKey());
                                    cartItem.setSellerId(sellerId);
                                    enhanceCartItemWithProductInfo(cartItem);
                                    sellerItems.add(cartItem);
                                } else {
                                    Log.w("CartActivity", "Skipping null or invalid cart item: " + itemSnapshot.getKey());
                                }
                            } else {
                                // Log unexpected data types (like Boolean) to help with debugging
                                Log.w("CartActivity", "Skipping unexpected data type at: " + itemSnapshot.getKey() + " | Type: " + (itemSnapshot.getValue() != null ? itemSnapshot.getValue().getClass().getSimpleName() : "null"));
                            }
                        } catch (Exception e) {
                            // Catch any other exceptions during parsing
                            Log.e("CartActivity", "Failed to parse cart item " + itemSnapshot.getKey(), e);
                        }
                        // ▲▲▲ END FIX ▲▲▲
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
        // Kosongkan senarai lama sebelum mengisi yang baru
        sellerList.clear();

        if (itemsBySeller.isEmpty()) {
            // Jika tiada item, panggil completeCartLoading dengan senarai kosong
            completeCartLoading(new ArrayList<>());
            return;
        }

        // Gunakan senarai baru untuk mengelakkan masalah serentak (concurrency issues)
        List<CartSeller> newSellerList = new ArrayList<>();

        // Ulang melalui setiap entri (setiap penjual) dalam Map
        for (Map.Entry<String, List<CartItem>> entry : itemsBySeller.entrySet()) {
            String sellerId = entry.getKey();
            List<CartItem> items = entry.getValue();

            String sellerName = "Unknown Seller";
            String sellerProfileImageUrl = null;

            // Pastikan senarai item untuk penjual ini tidak kosong
            if (!items.isEmpty()) {
                // Ambil productId dari item pertama dalam senarai untuk mendapatkan maklumat penjual
                CartItem firstItem = items.get(0);
                Product productInfo = productCache.get(firstItem.getProductId());

                if (productInfo != null) {
                    // Jika maklumat produk ditemui dalam cache, ambil nama dan URL gambar profil penjual
                    sellerName = productInfo.getSellerName();
                    sellerProfileImageUrl = productInfo.getSellerProfileImageUrl();
                }
            }

            // Cipta objek CartSeller baru dengan maklumat yang betul dan tambahkannya ke senarai baru
            newSellerList.add(new CartSeller(sellerId, sellerName, sellerProfileImageUrl, items, false));
        }

        // Panggil kaedah untuk melengkapkan proses pemuatan
        completeCartLoading(newSellerList);
    }

    private void completeCartLoading(List<CartSeller> newSellerList) {
        newSellerList.sort((o1, o2) -> o1.getSellerName().compareToIgnoreCase(o2.getSellerName()));
        sellerList.clear();
        sellerList.addAll(newSellerList);
        parentAdapter.notifyDataSetChanged();
        updateUIForEmptyCart(sellerList.isEmpty());
        updateTotalPrice();
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
            seller.setSelected(isChecked); // Pilih/nyahpilih penjual
            for (CartItem item : seller.getCartItems()) {
                if (item.isAvailable() || isInEditMode) { // Benarkan pemilihan item tidak tersedia semasa mod suntingan
                    item.setSelected(isChecked);
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

        for (CartSeller seller : sellerList) {
            int selectedItemsInSeller = 0;
            for (CartItem item : seller.getCartItems()) {
                if (item.isAvailable()) {
                    availableItemCount++;
                    if (item.isSelected()) {
                        total += item.getPrice() * item.getQuantity();
                        selectedCount++;
                        selectedItemsInSeller++;
                    } else {
                        allAvailableItemsSelected = false;
                    }
                } else {
                    // Dalam mod biasa, pastikan item tidak tersedia tidak dipilih
                    if (!isInEditMode) {
                        item.setSelected(false);
                    }
                }
            }
            // Tentukan sama ada header penjual patut dipilih
            boolean allSellerItemsSelected = !seller.getCartItems().isEmpty();
            for(CartItem item : seller.getCartItems()) {
                if(isInEditMode) { // Dalam mod edit, semua item boleh dipilih
                    if(!item.isSelected()) allSellerItemsSelected = false;
                } else { // Dalam mod biasa, hanya item yang tersedia diambil kira
                    if(item.isAvailable() && !item.isSelected()) allSellerItemsSelected = false;
                }
            }
            seller.setSelected(allSellerItemsSelected);
        }
        if (availableItemCount == 0 && !isInEditMode) allAvailableItemsSelected = false;

        tvTotalPrice.setText(String.format(Locale.US, "RM%.2f", total));
        tvTotalItemsCount.setText(String.format("Total (%d)", selectedCount));

        if (isInEditMode) {
            btnDeleteSelected.setEnabled(selectedCount > 0);
            btnDeleteSelected.setText(String.format("Delete (%d)", selectedCount));
        } else {
            btnCheckout.setEnabled(selectedCount > 0);
            btnCheckout.setText(String.format("Checkout (%d)", selectedCount));
        }

        // Kemas kini checkbox "Select All" tanpa mencetuskan listenernya
        checkboxSelectAll.setOnCheckedChangeListener(null);
        checkboxSelectAll.setChecked(allAvailableItemsSelected);
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
            // Anda boleh memaparkan Toast atau Snackbar di sini jika perlu
            // Contoh: Toast.makeText(this, unavailableCount + " product(s) are unavailable.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSellerHeaderClicked(String sellerId) {
        if (!isInEditMode) {
            Intent intent = new Intent(this, SellerProfileActivity.class);
            intent.putExtra("SELLER_ID", sellerId);
            startActivity(intent);
        }
    }

    @Override
    public void onQuantityChanged(String sellerId, String cartItemId, int newQuantity) {
        if (newQuantity <= 0) {
            // Jika kuantiti 0 atau kurang, padam item tersebut
            CartItem itemToDelete = findCartItem(sellerId, cartItemId);
            if (itemToDelete != null) {
                ArrayList<CartItem> items = new ArrayList<>();
                items.add(itemToDelete);
                // Tunjuk dialog pengesahan sebelum memadam
                new AlertDialog.Builder(this)
                        .setTitle("Remove Item")
                        .setMessage("Do you want to remove '" + itemToDelete.getName() + "' from your cart?")
                        .setPositiveButton("Remove", (dialog, which) -> deleteSelectedItems(items))
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            // Jika batal, set semula kuantiti kepada 1 dalam UI
                            parentAdapter.notifyDataSetChanged();
                        })
                        .show();
            }
        } else {
            // Kemas kini kuantiti dalam Firebase
            cartRef.child(sellerId).child(cartItemId).child("quantity").setValue(newQuantity)
                    .addOnFailureListener(e -> Toast.makeText(CartActivity.this, "Failed to update quantity.", Toast.LENGTH_SHORT).show());
        }
    }

    // Kaedah bantuan untuk mencari item dalam senarai
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
        // Kaedah ini mungkin tidak lagi digunakan jika anda membuang butang delete individu,
        // tetapi adalah baik untuk menyimpannya jika diperlukan pada masa hadapan.
        cartRef.child(sellerId).child(cartItemId).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(CartActivity.this, "Item removed", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(CartActivity.this, "Failed to remove item", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onItemSelectedChanged() {
        updateTotalPrice();
    }
}
