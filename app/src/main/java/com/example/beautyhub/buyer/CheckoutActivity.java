package com.example.beautyhub.buyer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.CheckoutItemsAdapter;
import com.example.beautyhub.databinding.ActivityCheckoutBinding;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Order;
import com.example.beautyhub.models.OrderItem;
import com.example.beautyhub.models.NotificationModel;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Reward;
import com.example.beautyhub.models.ShippingAddress;
import com.example.beautyhub.ui.ProductViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CheckoutActivity extends AppCompatActivity implements CheckoutItemsAdapter.OnQuantityChangeListener {

    private ActivityCheckoutBinding binding;
    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private DatabaseReference cartsRef;

    private ProductViewModel productViewModel;
    private ArrayList<CartItem> selectedItems = new ArrayList<>();
    private List<Reward> availableRewards = new ArrayList<>();
    private int currentUserPoints = 0;
    private ShippingAddress userShippingAddress;

    private double subtotal = 0.0;
    private double discountAmount = 0.0;
    private double totalPayment = 0.0;
    private static final double SHIPPING_FEE_WEST_MY = 5.00;
    private static final double SHIPPING_FEE_EAST_MY = 10.00;
    private static final int PAYMENT_REQUEST_CODE = 2002;
    private static final int ADDRESS_SELECTION_REQUEST_CODE = 1001;

    private boolean isFromBuyNow = false;
    private boolean shouldClearCartAfterOrder = false;
    private Map<String, Product> productCache = new HashMap<>();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to checkout.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        Intent intent = getIntent();
        isFromBuyNow = "BUY_NOW".equals(intent.getStringExtra("SOURCE"));
        selectedItems = intent.getParcelableArrayListExtra("CHECKOUT_ITEMS");
        shouldClearCartAfterOrder = intent.getBooleanExtra("CLEAR_CART_AFTER_ORDER", false);

        if (selectedItems == null || selectedItems.isEmpty()) {
            Toast.makeText(this, "Error: No items selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeActivity();
        loadProductsData();
    }

    private void initializeActivity() {
        userRef = database.getReference("Users").child(currentUser.getUid());
        cartsRef = database.getReference("Carts").child(currentUser.getUid());
        setupToolbar();
        setupListeners();
        setupAddMoreItems();
        loadUserDetailsAndRewards();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarCheckout);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Checkout");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.toolbarCheckout.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        binding.btnPlaceOrder.setOnClickListener(v -> initiateOrderProcess());

        // Address section click listeners
        binding.layoutAddressDetails.setOnClickListener(v -> openAddressSelection());
        binding.layoutNoAddress.setOnClickListener(v -> openAddressSelection());
        binding.layoutChangeAddress.setOnClickListener(v -> openAddressSelection());

        binding.rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> displayOrderSummary());

        binding.chipGroupDiscounts.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            if (selectedChip == null || selectedChip.getId() == R.id.chip_no_discount) {
                discountAmount = 0.0;
            } else {
                Reward selectedReward = (Reward) selectedChip.getTag();
                if (selectedReward != null) {
                    discountAmount = selectedReward.getDiscountValue();
                }
            }
            displayOrderSummary();
        });
    }

    private void openAddressSelection() {
        Intent intent = new Intent(this, ShippingAddressListActivity.class);
        intent.putExtra("SELECT_MODE", true);
        intent.putExtra("CURRENT_ADDRESS", userShippingAddress);
        startActivityForResult(intent, ADDRESS_SELECTION_REQUEST_CODE);
    }

    private void initiateOrderProcess() {
        if (userShippingAddress == null) {
            Toast.makeText(this, "Please select a shipping address.", Toast.LENGTH_SHORT).show();
            openAddressSelection();
            return;
        }

        if (binding.rbOnlineBanking.isChecked()) {
            Intent intent = new Intent(this, MockPaymentActivity.class);
            intent.putExtra("TOTAL_AMOUNT", totalPayment);
            startActivityForResult(intent, PAYMENT_REQUEST_CODE);
        } else if (binding.rbCashOnDelivery.isChecked()) {
            placeOrder("Cash on Delivery");
        } else {
            Toast.makeText(this, "Please select a payment method.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADDRESS_SELECTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                userShippingAddress = data.getParcelableExtra("SELECTED_ADDRESS");
                if (userShippingAddress != null) {
                    updateShippingAddressUI();
                    displayOrderSummary();
                    Toast.makeText(this, "Shipping address updated", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                if (userShippingAddress == null) {
                    Toast.makeText(this, "Please select a shipping address to continue.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == PAYMENT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                placeOrder("Online Banking");
            } else {
                Toast.makeText(this, "Payment Failed or Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void placeOrder(String paymentMethod) {
        validateStockForAllItems();
        if (!binding.btnPlaceOrder.isEnabled()) {
            Toast.makeText(this, "Some items are out of stock or unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userShippingAddress == null) {
            Toast.makeText(this, "Please select a shipping address.", Toast.LENGTH_SHORT).show();
            openAddressSelection();
            return;
        }

        progressDialog.show();
        progressDialog.setMessage("Validating stock...");

        validateRealTimeStock().addOnCompleteListener(validationTask -> {
            if (!validationTask.isSuccessful() || !validationTask.getResult()) {
                progressDialog.dismiss();
                Toast.makeText(this, "Stock validation failed. Some items are out of stock.", Toast.LENGTH_LONG).show();
                return;
            }

            progressDialog.setMessage("Updating stock...");
            updateStockForItems().addOnCompleteListener(stockTask -> {
                if (stockTask.isSuccessful()) {
                    saveOrderToFirebase(paymentMethod);
                } else {
                    progressDialog.dismiss();
                    Log.e("CheckoutActivity", "Stock update failed: " + stockTask.getException());
                    Toast.makeText(this, "Failed to update stock. Please try again.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private Task<Boolean> validateRealTimeStock() {
        List<Task<Boolean>> validationTasks = new ArrayList<>();

        for (CartItem item : selectedItems) {
            TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();

            database.getReference("Products").child(item.getProductId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Product product = snapshot.getValue(Product.class);
                            if (product == null) {
                                tcs.setResult(false);
                                return;
                            }
                            // Check if stock is sufficient
                            tcs.setResult(product.getStock() >= item.getQuantity());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            tcs.setResult(false);
                        }
                    });

            validationTasks.add(tcs.getTask());
        }

        return Tasks.whenAllSuccess(validationTasks)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        return false;
                    }

                    // Tasks.whenAllSuccess returns List<Object>, so we iterate and cast
                    List<Object> results = task.getResult();
                    for (Object result : results) {
                        if (result instanceof Boolean && !((Boolean) result)) {
                            return false;
                        }
                    }
                    return true;
                });
    }

    private Task<Void> updateStockForItems() {
        List<Task<Void>> tasks = new ArrayList<>();
        for (CartItem item : selectedItems) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Products").child(item.getProductId());

            TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
            ref.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData currentData) {
                    // Gunakan Map atau class Product, pastikan field "stock" wujud
                    Long currentStock = currentData.child("stock").getValue(Long.class);
                    if (currentStock == null) return Transaction.success(currentData); // Atau abort jika wajib ada

                    if (currentStock < item.getQuantity()) {
                        return Transaction.abort(); // Stok tak cukup
                    }

                    currentData.child("stock").setValue(currentStock - item.getQuantity());
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                    if (committed) tcs.setResult(null);
                    else tcs.setException(error != null ? error.toException() : new Exception("Stock update failed"));
                }
            });
            tasks.add(tcs.getTask());
        }
        return Tasks.whenAll(tasks);
    }

    private void saveOrderToFirebase(String paymentMethod) {
        progressDialog.setMessage("Finalizing order...");
        DatabaseReference ordersRef = database.getReference("Orders");
        long timestamp = System.currentTimeMillis();

        // List untuk simpan semua Task Firebase dan Order ID
        List<Task<Void>> tasks = new ArrayList<>();
        ArrayList<String> orderIdsList = new ArrayList<>();

        Map<String, List<CartItem>> itemsBySeller = new HashMap<>();
        for (CartItem item : selectedItems) {
            itemsBySeller.computeIfAbsent(item.getSellerId(), k -> new ArrayList<>()).add(item);
        }

        userRef.child("recipientName").get().addOnSuccessListener(snapshot -> {
                    String buyerName = snapshot.getValue(String.class);
                    if (buyerName == null) buyerName = "A Buyer";

                    // 1. Kumpulkan item mengikut SellerId

                    for (Map.Entry<String, List<CartItem>> entry : itemsBySeller.entrySet()) {
                        String sellerId = entry.getKey();
                        List<CartItem> sellerItems = entry.getValue();

                        // Jana Order ID unik
                        String orderId = ordersRef.push().getKey();
                        orderIdsList.add(orderId);

                        // 2. Ambil Maklumat Seller
                        String sellerName = "Unknown Store";
                        if (!sellerItems.isEmpty()) {
                            sellerName = sellerItems.get(0).getSellerName();
                        }

                        // 3. Kira Kos
                        double sellerSubtotal = 0;
                        for (CartItem item : sellerItems) {
                            sellerSubtotal += (item.getPrice() * item.getQuantity());
                        }
                        double shipping = userShippingAddress.getState().toLowerCase().contains("sabah") ||
                                userShippingAddress.getState().toLowerCase().contains("sarawak") ?
                                SHIPPING_FEE_EAST_MY : SHIPPING_FEE_WEST_MY;

                        // 4. Sediakan List OrderItems (Sama format JSON anda)
                        List<OrderItem> currentOrderItems = new ArrayList<>();
                        for (CartItem item : sellerItems) {
                            OrderItem orderItem = new OrderItem();
                            orderItem.setProductId(item.getProductId());
                            orderItem.setProductName(item.getName()); // Map ke 'productName
                            orderItem.setQuantity(item.getQuantity());
                            orderItem.setPrice(item.getPrice());
                            orderItem.setImageUrls(item.getImageUrls()); // Ambil dari Cart (plural)
                            orderItem.setSellerId(sellerId);
                            orderItem.setSellerName(sellerName);
                            orderItem.setReviewed(false);
                            orderItem.setFromJson(false);
                            orderItem.setSellerProfileImageUrl(item.getSellerProfileImageUrl());
                            orderItem.setOfficialStore(false);


                            currentOrderItems.add(orderItem);
                        }

                        // 5. Bina Objek Order (Ikut struktur JSON yang anda beri)
                        Order order = new Order();
                        order.setOrderId(orderId);
                        order.setUserId(currentUser.getUid());
                        order.setSellerId(sellerId);
                        order.setSellerName(sellerName);
                        order.setPaymentMethod(paymentMethod);
                        order.setTotalAmount(sellerSubtotal + shipping);
                        order.setOrderDate(timestamp);
                        order.setShippingAddress(userShippingAddress);
                        order.setOrderItems(currentOrderItems); // Masukkan list item ke dalam order
                        order.setStatus("Pending");
                        order.setOrderSource(isFromBuyNow ? "buy_now" : "cart");
                        order.setDiscountAmount(0); // Set default jika tiada
                        order.setOfficialStore(false);

                        // 6. Simpan ke Firebase
                        // 6. Simpan ke Firebase
                        tasks.add(ordersRef.child(orderId).setValue(order));

                        // 7. Notifikasi
                        // Ambil nama produk pertama untuk ringkasan notifikasi
                        String firstProductName = currentOrderItems.get(0).getProductName();
                        // Ambil gambar produk pertama penjual ini
                        String firstProductImage = currentOrderItems.get(0).getImageUrls();

                        if (currentOrderItems.size() > 1) {
                            firstProductName += " (and " + (currentOrderItems.size() - 1) + " more)";
                        }

                        // Di dalam loop itemsBySeller
                        double orderTotal = sellerSubtotal + shipping;

// TAMBAHKAN "ORDER_NEW" di hujung
                        sendNotificationToSeller(sellerId, orderId, buyerName, firstProductName, firstProductImage, "ORDER_NEW");

                        sendNotificationToBuyer(orderId, orderTotal, sellerName, firstProductName, firstProductImage);
                    }});
        // 8. Tunggu semua siap & panggil handlePostOrderCleanup
        Tasks.whenAll(tasks).addOnSuccessListener(aVoid -> {
            handlePostOrderCleanup(orderIdsList, paymentMethod);
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Order failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    private void sendNotificationToBuyer(String orderId, double amount, String sellerName, String productName, String imageUrl) {
        DatabaseReference notifRef = database.getReference("Notifications").child(currentUser.getUid());
        String id = notifRef.push().getKey();

        NotificationModel notification = new NotificationModel();
        notification.setId(id);
        notification.setOrderId(orderId);
        notification.setTitle("Order Placed Successfully! ✅");

        // Mesej yang lebih spesifik mengikut pesanan seller tersebut
        notification.setMessage("You ordered " + productName + " from " + sellerName + ". Total: RM" + String.format(Locale.getDefault(), "%.2f", amount));

        notification.setSellerName(sellerName);
        notification.setProductName(productName);
        notification.setProductImageUrl(imageUrl); // Guna gambar produk dari seller ini

        notification.setType("ORDER_PLACED");
        notification.setTimestamp(System.currentTimeMillis());
        notification.setUnread(true);

        if (id != null) {
            notifRef.child(id).setValue(notification);
        }
    }
    private void sendNotificationToSeller(String sellerId, String orderId, String buyerName, String productName, String imageUrl, String type) {
        DatabaseReference notifyRef = database.getReference("Notifications").child(sellerId);
        String id = notifyRef.push().getKey();

        NotificationModel notification = new NotificationModel();
        notification.setId(id);
        notification.setOrderId(orderId);

        // Gunakan nama dari shipping address jika ada
        String nameToDisplay = buyerName;
        if (userShippingAddress != null && userShippingAddress.getRecipientName() != null) {
            nameToDisplay = userShippingAddress.getRecipientName();
        }

        if (type.equals("ORDER_COMPLETED")) {
            notification.setTitle("Order Completed! ✅");
            notification.setMessage("Customer " + nameToDisplay + " has confirmed receiving " + productName + ". Funds are being processed.");
            notification.setType("ORDER_COMPLETED");
        } else {
            notification.setTitle("New Order Received! 🛍️");
            notification.setMessage("Customer " + nameToDisplay + " has ordered " + productName + ". Please ship it soon.");
            notification.setType("ORDER_NEW");
        }

        notification.setBuyerName(nameToDisplay);
        notification.setProductName(productName);
        notification.setProductImageUrl(imageUrl);
        notification.setTimestamp(System.currentTimeMillis());
        notification.setUnread(true);

        if (id != null) {
            notifyRef.child(id).setValue(notification);
        }
    }

    private double calculateSellerSubtotal(List<CartItem> items) {
        double sub = 0;
        for (CartItem item : items) sub += (item.getPrice() * item.getQuantity());
        return sub;
    }
    private void handlePostOrderCleanup(ArrayList<String> orderIds, String paymentMethod) {
        // 1. Bersihkan Cart jika perlu
        if (!isFromBuyNow && shouldClearCartAfterOrder) {
            removeOrderedItemsFromCart();
        }

        // 2. Buang reward yang telah digunakan (Kekalkan logik anda)
        int checkedId = binding.chipGroupDiscounts.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = binding.chipGroupDiscounts.findViewById(checkedId);
            if (chip != null && chip.getTag() instanceof Reward) {
                Reward usedReward = (Reward) chip.getTag();
                userRef.child("redeemedRewards").child(usedReward.getRewardId()).removeValue();
            }
        }

        // 3. Tambah point (Kekalkan logik anda)
        userRef.child("points").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Integer current = mutableData.getValue(Integer.class);
                if (current == null) current = 0;
                mutableData.setValue(current + 5);
                return Transaction.success(mutableData);
            }
            @Override public void onComplete(@Nullable DatabaseError e, boolean c, @Nullable DataSnapshot d) {}
        });

        // 4. Tutup progress dialog
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        // 5. NAVIGASI KE OrderSuccessActivity
        Intent intent = new Intent(this, OrderSuccessActivity.class);

        // PENTING: Pastikan orderIds tidak null sebelum hantar
        if (orderIds == null) {
            orderIds = new ArrayList<>();
        }

        // Hantar sebagai ArrayList
        intent.putStringArrayListExtra("ORDER_IDS", orderIds);

        // Hantar String tunggal sebagai backup (Ambil yang pertama)
        if (!orderIds.isEmpty()) {
            intent.putExtra("ORDER_ID", orderIds.get(0));
        } else {
            intent.putExtra("ORDER_ID", "N/A");
        }

        intent.putExtra("PAYMENT_METHOD", paymentMethod);
        intent.putExtra("TOTAL_AMOUNT", totalPayment);

        // Guna flag ini supaya user tidak boleh 'back' ke Checkout semula
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    public void onQuantityChanged(CartItem item, int newQuantity) {
        // Find item in selectedItems and update
        for (CartItem selected : selectedItems) {
            if (selected.getProductId().equals(item.getProductId())) {
                selected.setQuantity(newQuantity);
                break;
            }
        }
        displayOrderSummary();
    }
    private void removeOrderedItemsFromCart() {
        if (cartsRef == null || selectedItems.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        for (CartItem item : selectedItems) {
            if (item.getSellerId() != null && item.getCartItemId() != null) {
                updates.put("/" + item.getSellerId() + "/" + item.getCartItemId(), null);
            }
        }

        if (!updates.isEmpty()) {
            cartsRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("CheckoutActivity", "Successfully removed ordered items from cart");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CheckoutActivity", "Failed to remove ordered items from cart", e);
                    });
        }
    }

    private void rollbackStockUpdates(ArrayList<CartItem> items) {
        for (CartItem item : items) {
            database.getReference("Products").child(item.getProductId())
                    .runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                            Product product = mutableData.getValue(Product.class);
                            if (product != null) {
                                int newStock = product.getStock() + item.getQuantity();
                                mutableData.child("stock").setValue(newStock);
                                Log.d("StockRollback", "Rolled back stock for: " + item.getName());
                            }
                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                            if (error != null) {
                                Log.e("StockRollback", "Failed to rollback: " + error.getMessage());
                            }
                        }
                    });
        }
    }

    private void loadProductsData() {
        productViewModel.getProductsAsMap().observe(this, map -> {
            this.productCache = map;
            enhanceItems();
            setupRecyclerView();
            displayOrderSummary();
        });
        productViewModel.loadAllProducts();
    }

    private void enhanceItems() {
        for (CartItem item : selectedItems) {
            Product p = productCache.get(item.getProductId());
            if (p != null) {
                item.setName(p.getName());
                if (p.getImageUrls() != null && !p.getImageUrls().isEmpty())
                    item.setImageUrls(p.getImageUrls().get(0));
                item.setSellerId(p.getSellerId());
                item.setSellerName(p.getSellerName());
                item.setSellerProfileImageUrl(p.getSellerProfileImageUrl());
            }
        }
    }

    private void setupRecyclerView() {
        CheckoutItemsAdapter adapter = new CheckoutItemsAdapter(this, selectedItems, this);
        adapter.setProductCache(productCache);
        binding.rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCheckoutItems.setAdapter(adapter);
        binding.rvCheckoutItems.setNestedScrollingEnabled(false);
    }

    @Override
    public void onQuantityChanged(int position, int newQuantity) {
        if (newQuantity < 1) {
            Toast.makeText(this, "Quantity must be at least 1", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedItems.get(position).setQuantity(newQuantity);
        displayOrderSummary();
        validateStockForAllItems();
    }

    private void displayOrderSummary() {
        subtotal = 0.0;
        for (CartItem item : selectedItems) subtotal += item.getPrice() * item.getQuantity();

        double totalShipping = 0.0;
        if (userShippingAddress != null && !binding.rbCashOnDelivery.isChecked()) {
            Map<String, List<CartItem>> itemsBySeller = selectedItems.stream()
                    .collect(Collectors.groupingBy(CartItem::getSellerId));
            double shippingPerSeller = "East Malaysia".equalsIgnoreCase(userShippingAddress.getZone()) ? SHIPPING_FEE_EAST_MY : SHIPPING_FEE_WEST_MY;
            totalShipping = shippingPerSeller * itemsBySeller.size();
        }

        totalPayment = Math.max(0, subtotal + totalShipping - discountAmount);

        // Update UI
        binding.tvSubtotal.setText(String.format(Locale.US, "RM %.2f", subtotal));
        binding.tvShippingFee.setText(String.format(Locale.US, "RM %.2f", totalShipping));
        binding.tvTotalPayment.setText(String.format(Locale.US, "RM %.2f", totalPayment));
        binding.tvTotalPaymentBottom.setText(String.format(Locale.US, "RM %.2f", totalPayment));

        if (discountAmount > 0) {
            binding.layoutDiscount.setVisibility(View.VISIBLE);
            binding.tvDiscountAmount.setText(String.format(Locale.US, "- RM %.2f", discountAmount));
        } else {
            binding.layoutDiscount.setVisibility(View.GONE);
        }

        // Update button text dengan jumlah item
        int totalItems = selectedItems.stream().mapToInt(CartItem::getQuantity).sum();
        binding.btnPlaceOrder.setText(String.format("Place Order (%d items)", totalItems));
    }

    private void validateStockForAllItems() {
        boolean allInStock = true;
        for (CartItem item : selectedItems) {
            Product p = productCache.get(item.getProductId());
            if (p == null || p.getStock() < item.getQuantity()) {
                allInStock = false;
                break;
            }
        }
        binding.btnPlaceOrder.setEnabled(allInStock);
        binding.btnPlaceOrder.setAlpha(allInStock ? 1.0f : 0.5f);
    }

    private void loadUserDetailsAndRewards() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer pts = snapshot.child("points").getValue(Integer.class);
                currentUserPoints = (pts != null) ? pts : 0;
                binding.tvCurrentPoints.setText(String.format(Locale.US, "You have %,d points", currentUserPoints));

                if (userShippingAddress == null) {
                    for (DataSnapshot s : snapshot.child("shippingAddress").getChildren()) {
                        ShippingAddress a = s.getValue(ShippingAddress.class);
                        if (a != null && a.isDefault()) {
                            userShippingAddress = a;
                            break;
                        }
                    }
                }

                availableRewards.clear();
                DataSnapshot redeemedSnapshot = snapshot.child("redeemedRewards");
                for (DataSnapshot s : redeemedSnapshot.getChildren()) {
                    Reward r = s.getValue(Reward.class);
                    if (r != null) {
                        r.setRewardId(s.getKey());
                        availableRewards.add(r);
                    }
                }

                updateShippingAddressUI();
                populateRewards();
                displayOrderSummary();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CheckoutActivity", "Failed to load user details", error.toException());
            }
        });
    }

    private void populateRewards() {
        // Clear existing chips kecuali "No Discount"
        int childCount = binding.chipGroupDiscounts.getChildCount();
        for (int i = childCount - 1; i > 0; i--) {
            binding.chipGroupDiscounts.removeViewAt(i);
        }

        // Add reward chips
        for (Reward r : availableRewards) {
            Chip chip = new Chip(this);
            chip.setText(String.format(Locale.US, "Voucher RM %.2f", r.getDiscountValue()));
            chip.setTag(r);
            chip.setCheckable(true);
            binding.chipGroupDiscounts.addView(chip);
        }
    }

    private void updateShippingAddressUI() {
        if (userShippingAddress != null) {
            binding.layoutAddressDetails.setVisibility(View.VISIBLE);
            binding.layoutNoAddress.setVisibility(View.GONE);

            binding.tvRecipientNameAndPhone.setText(String.format("%s | %s",
                    userShippingAddress.getRecipientName(),
                    userShippingAddress.getPhoneNumber()));

            binding.tvAddressLine.setText(String.format("%s, %s, %s, %s %s",
                    userShippingAddress.getStreet(),
                    userShippingAddress.getCity(),
                    userShippingAddress.getState(),
                    userShippingAddress.getZipcode(),
                    userShippingAddress.getZone()));
        } else {
            binding.layoutAddressDetails.setVisibility(View.GONE);
            binding.layoutNoAddress.setVisibility(View.VISIBLE);
        }
    }

    private void setupAddMoreItems() {
        binding.tvAddMoreItems.setVisibility(isFromBuyNow ? View.GONE : View.VISIBLE);
        binding.tvAddMoreItems.setOnClickListener(v -> {
            // Kembali ke CartActivity
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Tanya user jika mereka mahu keluar dari checkout
        if (!isFromBuyNow) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cancel Checkout")
                    .setMessage("Are you sure you want to cancel checkout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}