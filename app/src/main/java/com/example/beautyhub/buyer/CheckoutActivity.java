package com.example.beautyhub.buyer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

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

    private boolean isFromBuyNow = false;
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
    }

    private void setupListeners() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        binding.btnPlaceOrder.setOnClickListener(v -> initiateOrderProcess());
        binding.layoutAddressDetails.setOnClickListener(v -> openAddressSelection());
        binding.layoutNoAddress.setOnClickListener(v -> openAddressSelection());

        binding.rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> displayOrderSummary());

        binding.chipGroupDiscounts.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            if (selectedChip == null || "No Discount".equals(selectedChip.getText().toString())) {
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
        startActivityForResult(intent, 1001);
    }

    private void initiateOrderProcess() {
        if (userShippingAddress == null) {
            Toast.makeText(this, "Please select a shipping address.", Toast.LENGTH_SHORT).show();
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
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            userShippingAddress = data.getParcelableExtra("SELECTED_ADDRESS");
            updateShippingAddressUI();
            displayOrderSummary();
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
            Toast.makeText(this, "Some items are out of stock.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        updateStockForItems().addOnCompleteListener(stockTask -> {
            if (stockTask.isSuccessful()) {
                saveOrderToFirebase(paymentMethod);
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to update stock.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Task<Void> updateStockForItems() {
        List<Task<Void>> stockUpdateTasks = new ArrayList<>();
        for (CartItem item : selectedItems) {
            TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
            database.getReference("Products").child(item.getProductId()).runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    Product product = mutableData.getValue(Product.class);
                    if (product == null || product.getStock() < item.getQuantity())
                        return Transaction.abort();
                    mutableData.child("stock").setValue(product.getStock() - item.getQuantity());
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    if (error != null) tcs.trySetException(error.toException());
                    else if (!committed) tcs.trySetException(new Exception("Stock update failed."));
                    else tcs.setResult(null);
                }
            });
            stockUpdateTasks.add(tcs.getTask());
        }
        return Tasks.whenAll(stockUpdateTasks);
    }

    private void saveOrderToFirebase(String paymentMethod) {
        DatabaseReference ordersRef = database.getReference("Orders");
        Map<String, List<CartItem>> itemsBySeller = selectedItems.stream()
                .collect(Collectors.groupingBy(CartItem::getSellerId));

        double shippingFeePerSeller = binding.rbCashOnDelivery.isChecked() ? 0.0 :
                ("East Malaysia".equalsIgnoreCase(userShippingAddress.getZone()) ? SHIPPING_FEE_EAST_MY : SHIPPING_FEE_WEST_MY);

        List<Task<Void>> allTasks = new ArrayList<>();
        ArrayList<String> newOrderIds = new ArrayList<>();

        for (Map.Entry<String, List<CartItem>> entry : itemsBySeller.entrySet()) {
            String sellerId = entry.getKey();
            List<CartItem> sellerItems = entry.getValue();
            String subOrderId = ordersRef.push().getKey();
            if (subOrderId == null) continue;

            newOrderIds.add(subOrderId);
            double sellerSubtotal = 0;
            List<OrderItem> orderItems = new ArrayList<>();

            for (CartItem item : sellerItems) {
                sellerSubtotal += (item.getPrice() * item.getQuantity());
                orderItems.add(new OrderItem(
                        item.getProductId(), item.getName(), item.getQuantity(),
                        item.getPrice(), item.getImageUrls(), item.getSellerProfileImageUrl(),
                        item.getSellerId(), item.getSellerName()
                ));
            }

            Order splitOrder = new Order(
                    subOrderId, currentUser.getUid(), currentUser.getDisplayName(),
                    userShippingAddress, (sellerSubtotal + shippingFeePerSeller) - discountAmount,
                    "Pending", System.currentTimeMillis(), paymentMethod, discountAmount
            );
            splitOrder.setOrderItems(orderItems);
            splitOrder.setSellerId(sellerId);
            splitOrder.setSellerName(sellerItems.get(0).getSellerName());
            splitOrder.setOrderSource(isFromBuyNow ? "buy_now" : "cart");

            // --- 1. NOTIFICATION DATA ---
            String firstProductName = sellerItems.get(0).getName();
            String productSummary = (sellerItems.size() > 1)
                    ? firstProductName + " and " + (sellerItems.size() - 1) + " others"
                    : firstProductName;

            String sellerNameForNotif = (sellerItems.get(0).getSellerName() != null)
                    ? sellerItems.get(0).getSellerName() : "BeautyHub Seller";

            String productImgForNotif = (sellerItems.get(0).getImageUrls() != null)
                    ? sellerItems.get(0).getImageUrls() : "";

            // --- 2. BUYER NOTIFICATION ---
            DatabaseReference buyerNotifRef = database.getReference("Notifications").child(currentUser.getUid());
            String buyerNotifId = buyerNotifRef.push().getKey();
            if (buyerNotifId != null) {
                NotificationModel buyerNotif = new NotificationModel(buyerNotifId, "Order Placed Successfully", "",
                        System.currentTimeMillis(), "Order", productSummary, sellerNameForNotif, productImgForNotif, subOrderId, true);
                allTasks.add(buyerNotifRef.child(buyerNotifId).setValue(buyerNotif));
            }

            // --- 3. SELLER NOTIFICATION ---
            DatabaseReference sellerNotifRef = database.getReference("Notifications").child(sellerId);
            String sellerNotifId = sellerNotifRef.push().getKey();
            if (sellerNotifId != null) {
                String buyerName = (userShippingAddress != null) ? userShippingAddress.getRecipientName() : "A Customer";
                NotificationModel sellerNotif = new NotificationModel(sellerNotifId, "New Order Received!", "",
                        System.currentTimeMillis(), "NewOrder", productSummary, buyerName, productImgForNotif, subOrderId, true);
                allTasks.add(sellerNotifRef.child(sellerNotifId).setValue(sellerNotif));
            }

            // --- 4. SAVE ORDER ---
            allTasks.add(ordersRef.child(subOrderId).setValue(splitOrder));
        }

        Tasks.whenAll(allTasks).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                handlePostOrderCleanup(newOrderIds, paymentMethod);
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to place orders.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handlePostOrderCleanup(ArrayList<String> orderIds, String paymentMethod) {
        // 1. Remove from Cart
        if (!isFromBuyNow) {
            for (CartItem item : selectedItems) {
                database.getReference("Cart").child(currentUser.getUid()).child(item.getCartItemId()).removeValue();
            }
        }

        // 2. Remove used Reward
        int checkedId = binding.chipGroupDiscounts.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = findViewById(checkedId);
            if (chip != null && chip.getTag() instanceof Reward) {
                Reward usedReward = (Reward) chip.getTag();
                userRef.child("redeemedRewards").child(usedReward.getRewardId()).removeValue();
            }
        }

        // 3. Add Points Transaction
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

        // 4. Final Redirect
        if (progressDialog.isShowing()) progressDialog.dismiss();
        Intent intent = new Intent(this, OrderSuccessActivity.class);
        intent.putStringArrayListExtra("ORDER_IDS", orderIds);
        intent.putExtra("ORDER_COUNT", orderIds.size());
        intent.putExtra("PAYMENT_METHOD", paymentMethod);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadProductsData() {
        productViewModel.getProductsAsMap().observe(this, map -> {
            this.productCache = map;
            enhanceItems();
            setupRecyclerView();
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
        if (userShippingAddress != null) adapter.setUserZone(userShippingAddress.getZone());
        binding.rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCheckoutItems.setAdapter(adapter);
        displayOrderSummary();
    }

    @Override
    public void onQuantityChanged(int position, int newQuantity) {
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

        if (binding.rvCheckoutItems.getAdapter() instanceof CheckoutItemsAdapter) {
            ((CheckoutItemsAdapter) binding.rvCheckoutItems.getAdapter()).setShippingEnabled(!binding.rbCashOnDelivery.isChecked());
        }
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
                binding.tvCurrentPoints.setText(String.format(Locale.US, "Points: %d", currentUserPoints));

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
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void populateRewards() {
        binding.chipGroupDiscounts.removeAllViews();
        Chip none = new Chip(this);
        none.setText("No Discount");
        none.setCheckable(true);
        none.setChecked(true);
        binding.chipGroupDiscounts.addView(none);

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
            binding.tvRecipientNameAndPhone.setText(String.format("%s | %s", userShippingAddress.getRecipientName(), userShippingAddress.getPhoneNumber()));
            binding.tvAddressLine.setText(String.format("%s, %s, %s", userShippingAddress.getStreet(), userShippingAddress.getCity(), userShippingAddress.getState()));
        } else {
            binding.layoutAddressDetails.setVisibility(View.GONE);
            binding.layoutNoAddress.setVisibility(View.VISIBLE);
        }
    }

    private void setupAddMoreItems() {
        binding.tvAddMoreItems.setVisibility(isFromBuyNow ? View.GONE : View.VISIBLE);
        binding.tvAddMoreItems.setOnClickListener(v -> finish());
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}