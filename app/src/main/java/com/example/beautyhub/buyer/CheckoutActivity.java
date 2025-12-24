package com.example.beautyhub.buyer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Reward;
import com.example.beautyhub.models.ShippingAddress;
import com.example.beautyhub.models.Variant;
import com.example.beautyhub.ui.ProductViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.chip.Chip;
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
import java.util.stream.Collectors;

public class CheckoutActivity extends AppCompatActivity implements CheckoutItemsAdapter.OnQuantityChangeListener {

    private ActivityCheckoutBinding binding;
    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private DatabaseReference rewardsRef;

    private ProductViewModel productViewModel;
    private ArrayList<CartItem> selectedItems = new ArrayList<>();
    private List<Reward> availableRewards = new ArrayList<>();
    private ShippingAddress userShippingAddress;
    private Reward selectedReward;
    private double subtotal = 0.0;
    private double discountAmount = 0.0;
    private double totalPayment = 0.0;
    private static final double SHIPPING_FEE_WEST_MY = 5.00;
    private static final double SHIPPING_FEE_EAST_MY = 10.00;
    private boolean isFromBuyNow = false;
    private Map<String, Product> productCache = new HashMap<>();
    private boolean isStockValid = true;

    private ProgressDialog progressDialog;

    private final ActivityResultLauncher<Intent> addressSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadUserDetailsAndRewards();
                }
            }
    );

    private final ActivityResultLauncher<Intent> stripeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    String paymentDetail = "Online Banking (FPX)";
                    if (result.getData() != null && result.getData().hasExtra("PAYMENT_METHOD_DETAIL")) {
                        String bankName = result.getData().getStringExtra("PAYMENT_METHOD_DETAIL");
                        paymentDetail = "Online Banking (" + bankName + ")";
                    }
                    if (progressDialog != null) {
                        progressDialog.setMessage("Payment successful. Placing order...");
                        progressDialog.show();
                    }
                    proceedWithOrderCreation(paymentDetail);
                } else {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(this, "Payment failed or was canceled.", Toast.LENGTH_LONG).show();
                }
            }
    );

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
        String source = intent.getStringExtra("SOURCE");
        isFromBuyNow = "BUY_NOW".equals(source);
        selectedItems = intent.getParcelableArrayListExtra("CHECKOUT_ITEMS");

        if (selectedItems == null || selectedItems.isEmpty()) {
            Toast.makeText(this, "Error: No items selected for checkout.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeActivity();
        loadProductsData();
    }

    private void loadProductsData() {
        productViewModel.getProducts().observe(this, products -> {
            productCache.clear();
            for (Product product : products) {
                productCache.put(product.getProductId(), product);
            }
            validateStockForAllItems();
            enhanceCartItemsWithProductInfo();
        });

        productViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Log.e("CheckoutActivity", "Failed to load products: " + error);
            }
        });

        productViewModel.loadAllProducts();
    }

    private void validateStockForAllItems() {
        isStockValid = true;
        List<String> outOfStockItems = new ArrayList<>();

        for (CartItem item : selectedItems) {
            Product product = productCache.get(item.getProductId());

            if (product == null || !product.isActive()) {
                outOfStockItems.add(item.getProductName() + " (Product unavailable)");
                isStockValid = false;
                continue;
            }

            if (item.getVariantId() != null && !item.getVariantId().isEmpty()) {
                Variant variant = product.getVariantById(item.getVariantId());
                if (variant == null || variant.getStock() < item.getQuantity()) {
                    outOfStockItems.add(item.getDisplayName());
                    isStockValid = false;
                }
            } else {
                if (product.getStock() < item.getQuantity()) {
                    outOfStockItems.add(item.getProductName());
                    isStockValid = false;
                }
            }
        }

        updateUIForStockValidation(outOfStockItems);
    }

    private void updateUIForStockValidation(List<String> outOfStockItems) {
        if (!outOfStockItems.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Some items are unavailable:\n");
            for (String itemName : outOfStockItems) {
                errorMessage.append("• ").append(itemName).append("\n");
            }

            binding.tvStockWarning.setText(errorMessage.toString());
            binding.layoutStockWarning.setVisibility(View.VISIBLE);
            binding.btnPlaceOrder.setEnabled(false);
            binding.btnPlaceOrder.setAlpha(0.5f);

            Toast.makeText(this,
                    outOfStockItems.size() + " item(s) unavailable",
                    Toast.LENGTH_LONG).show();
        } else {
            binding.layoutStockWarning.setVisibility(View.GONE);
            binding.btnPlaceOrder.setEnabled(true);
            binding.btnPlaceOrder.setAlpha(1f);
            isStockValid = true;
        }
    }

    private void enhanceCartItemsWithProductInfo() {
        for (CartItem item : selectedItems) {
            Product product = productCache.get(item.getProductId());
            if (product != null) {
                if (product.isPreloaded() && !product.getName().equals(item.getProductName())) {
                    item.setProductName(product.getName());
                }

                if ((item.getImageUrl() == null || item.getImageUrl().isEmpty())
                        && product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                    item.setImageUrl(product.getImageUrls().get(0));
                }

                if ((item.getSellerName() == null || item.getSellerName().isEmpty())
                        && product.getSellerName() != null) {
                    item.setSellerName(product.getSellerName());
                }

                if ((item.getSellerId() == null || item.getSellerId().isEmpty())
                        && product.getSellerId() != null) {
                    item.setSellerId(product.getSellerId());
                }

                if (item.getVariantId() != null && product.hasVariants()) {
                    Variant variant = product.getVariantById(item.getVariantId());
                    if (variant != null && (item.getVariantName() == null ||
                            !item.getVariantName().equals(variant.getName()))) {
                        item.setVariantName(variant.getName());
                    }
                }
            }
        }

        if (binding.rvCheckoutItems.getAdapter() != null) {
            binding.rvCheckoutItems.getAdapter().notifyDataSetChanged();
        }
    }

    private void initializeActivity() {
        userRef = database.getReference("Users").child(currentUser.getUid());
        rewardsRef = database.getReference("Rewards");

        setupListeners();
        setupToolbar();
        setupRecyclerView();
        displayOrderSummary();
        loadUserDetailsAndRewards();
    }

    private void setupListeners() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        binding.btnPlaceOrder.setOnClickListener(v -> {
            if (!isStockValid) {
                Toast.makeText(this,
                        "Cannot proceed: Some items are unavailable",
                        Toast.LENGTH_LONG).show();
                return;
            }
            placeOrder();
        });

        binding.layoutChangeAddress.setOnClickListener(v -> {
            Intent intent = new Intent(CheckoutActivity.this, ShippingAddressListActivity.class);
            intent.putExtra("SELECT_MODE", true);
            addressSelectionLauncher.launch(intent);
        });

        if (isFromBuyNow) {
            binding.tvAddMoreItems.setVisibility(View.GONE);
        } else {
            binding.tvAddMoreItems.setVisibility(View.VISIBLE);
            binding.tvAddMoreItems.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userRef != null) {
            loadProductsData();
            loadUserDetailsAndRewards();
        }
    }

    private void setupRecyclerView() {
        CheckoutItemsAdapter checkoutAdapter = new CheckoutItemsAdapter(this, selectedItems);
        checkoutAdapter.setProductCache(productCache);
        binding.rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCheckoutItems.setAdapter(checkoutAdapter);
    }


    public void onQuantityChanged() {
        Log.d("CheckoutActivity", "Quantity changed, recalculating totals.");
        validateStockForAllItems();
        displayOrderSummary();
    }

    private void displayOrderSummary() {
        subtotal = 0.0;
        if (selectedItems != null) {
            for (CartItem item : selectedItems) {
                subtotal += item.getPrice() * item.getQuantity();
            }
        }
        applyDiscount(selectedReward);
    }

    private void updatePaymentSummary() {
        long uniqueSellers = selectedItems.stream()
                .map(CartItem::getSellerId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .count();

        double shippingFeePerSeller = SHIPPING_FEE_WEST_MY;
        if (userShippingAddress != null && "East Malaysia".equals(userShippingAddress.getZone())) {
            shippingFeePerSeller = SHIPPING_FEE_EAST_MY;
        }

        double totalShippingFee = uniqueSellers * shippingFeePerSeller;
        totalPayment = subtotal + totalShippingFee - discountAmount;
        if (totalPayment < 0) totalPayment = 0;

        binding.tvSubtotal.setText(String.format(Locale.US, "RM %.2f", subtotal));
        binding.tvShippingFee.setText(String.format(Locale.US, "RM %.2f", totalShippingFee));
        binding.tvTotalPayment.setText(String.format(Locale.US, "RM %.2f", totalPayment));
        binding.tvTotalPaymentBottom.setText(String.format(Locale.US, "RM %.2f", totalPayment));

        if (discountAmount > 0) {
            binding.layoutDiscount.setVisibility(View.VISIBLE);
            binding.tvDiscountAmount.setText(String.format(Locale.US, "- RM %.2f", discountAmount));
        } else {
            binding.layoutDiscount.setVisibility(View.GONE);
        }
    }

    private void placeOrder() {
        if (userShippingAddress == null) {
            Toast.makeText(this, "Please set a shipping address", Toast.LENGTH_SHORT).show();
            return;
        }

        for (CartItem item : selectedItems) {
            if (item.getSellerId() == null || item.getSellerId().isEmpty()) {
                Toast.makeText(this, "Error: Seller information is missing for an item.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (!isStockValid) {
            Toast.makeText(this,
                    "Cannot proceed: Some items are unavailable. Please update your cart.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        final CharSequence[] paymentOptions = {"Online Banking (FPX)", "Cash on Delivery (COD)"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Payment Method");
        builder.setItems(paymentOptions, (dialog, item) -> {
            if (paymentOptions[item].equals("Online Banking (FPX)")) {
                Intent stripeIntent = new Intent(CheckoutActivity.this, MockPaymentActivity.class);
                stripeIntent.putExtra("TOTAL_PAYMENT", totalPayment);
                stripeLauncher.launch(stripeIntent);
            } else if (paymentOptions[item].equals("Cash on Delivery (COD)")) {
                if (progressDialog != null) {
                    progressDialog.setMessage("Placing order...");
                    progressDialog.show();
                }
                proceedWithOrderCreation("COD");
            }
        });
        builder.show();
    }

    private void proceedWithOrderCreation(String paymentMethod) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.setMessage("Placing order...");
            progressDialog.show();
        }

        String currentUserId = currentUser.getUid();
        DatabaseReference rootRef = database.getReference();

        rootRef.child("Users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(CheckoutActivity.this, "User data not found. Cannot proceed.", Toast.LENGTH_SHORT).show();
                    return;
                }

                long currentPoints = snapshot.child("points").exists() ? snapshot.child("points").getValue(Long.class) : 0;
                int pointsToUse = (selectedReward != null) ? selectedReward.getPointsRequired() : 0;

                if (currentPoints < pointsToUse) {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(CheckoutActivity.this, "Not enough points for the selected discount.", Toast.LENGTH_SHORT).show();
                    return;
                }

                updateStockForOrderedItems(rootRef, currentUserId, currentPoints, pointsToUse, paymentMethod);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                Log.e("CheckoutActivity", "Failed to read user data for placing order.", error.toException());
                Toast.makeText(CheckoutActivity.this, "Failed to verify user details: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStockForOrderedItems(DatabaseReference rootRef, String userId,
                                            long currentPoints, int pointsToUse, String paymentMethod) {
        Map<String, Object> stockUpdates = new HashMap<>();

        for (CartItem item : selectedItems) {
            Product product = productCache.get(item.getProductId());
            if (product != null) {
                if (item.getVariantId() != null && !item.getVariantId().isEmpty()) {
                    String variantStockPath = "Products/" + item.getProductId() +
                            "/variants/" + item.getVariantId() + "/stock";

                    Variant variant = product.getVariantById(item.getVariantId());
                    if (variant != null) {
                        int newStock = variant.getStock() - item.getQuantity();
                        stockUpdates.put(variantStockPath, Math.max(0, newStock));
                    }
                } else {
                    String productStockPath = "Products/" + item.getProductId() + "/stock";
                    int newStock = product.getStock() - item.getQuantity();
                    stockUpdates.put(productStockPath, Math.max(0, newStock));
                }
            }
        }

        if (!stockUpdates.isEmpty()) {
            rootRef.updateChildren(stockUpdates).addOnCompleteListener(stockTask -> {
                if (stockTask.isSuccessful()) {
                    performAtomicOrderUpdate(rootRef, userId, currentPoints, pointsToUse, paymentMethod);
                } else {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(CheckoutActivity.this,
                            "Failed to update stock: " + stockTask.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            performAtomicOrderUpdate(rootRef, userId, currentPoints, pointsToUse, paymentMethod);
        }
    }

    private void performAtomicOrderUpdate(DatabaseReference rootRef, String userId,
                                          long currentPoints, int pointsToUse, String paymentMethod) {
        Map<String, List<CartItem>> itemsBySeller = selectedItems.stream()
                .collect(Collectors.groupingBy(CartItem::getSellerId));

        Map<String, Object> atomicUpdate = new HashMap<>();

        final String parentOrderId = rootRef.child("Orders").push().getKey();
        if (parentOrderId == null) {
            if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
            Toast.makeText(this, "Failed to create a unique order ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        double shippingFeePerSeller = SHIPPING_FEE_WEST_MY;
        if (userShippingAddress != null && "East Malaysia".equals(userShippingAddress.getZone())) {
            shippingFeePerSeller = SHIPPING_FEE_EAST_MY;
        }

        Map<String, Order> subOrdersMap = new HashMap<>();

        for (Map.Entry<String, List<CartItem>> entry : itemsBySeller.entrySet()) {
            String sellerId = entry.getKey();
            List<CartItem> sellerItems = entry.getValue();

            String subOrderId = rootRef.child("Orders").child(parentOrderId).child("subOrders").push().getKey();
            if (subOrderId == null) continue;

            List<OrderItem> orderItems = new ArrayList<>();
            String sellerNameForSubOrder = "";

            for (CartItem cartItem : sellerItems) {
                OrderItem orderItem = new OrderItem(
                        cartItem.getProductId(),
                        cartItem.getProductName(),
                        cartItem.getQuantity(),
                        cartItem.getPrice(),
                        cartItem.getImageUrl()
                );
                orderItem.setSubOrderId(subOrderId);

                if (cartItem.getVariantId() != null) {
                    orderItem.setVariantId(cartItem.getVariantId());
                    orderItem.setVariantName(cartItem.getVariantName());
                }

                Product product = productCache.get(cartItem.getProductId());
                if (product != null) {
                    orderItem.setFromJson(product.isPreloaded());
                    orderItem.setHasVariants(product.hasVariants());
                }

                orderItems.add(orderItem);

                if (sellerNameForSubOrder.isEmpty() && cartItem.getSellerName() != null) {
                    sellerNameForSubOrder = cartItem.getSellerName();
                }
            }

            double orderSubtotal = orderItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
            double proportionalDiscount = 0.0;
            if (subtotal > 0 && discountAmount > 0) {
                proportionalDiscount = (orderSubtotal / subtotal) * discountAmount;
            }

            double orderFinalPayment = orderSubtotal + shippingFeePerSeller - proportionalDiscount;
            if (orderFinalPayment < 0) orderFinalPayment = 0;

            Order subOrder = new Order(
                    subOrderId, userId, userShippingAddress, orderItems, orderFinalPayment,
                    "Pending", System.currentTimeMillis(), paymentMethod
            );
            subOrder.setSellerId(sellerId);
            subOrder.setSellerName(sellerNameForSubOrder);

            if ("system".equals(sellerId)) {
                subOrder.setOfficialStore(true);
            }

            subOrdersMap.put(subOrderId, subOrder);
        }

        Order parentOrder = new Order(
                parentOrderId, userId, userShippingAddress, new ArrayList<>(), totalPayment,
                "Pending", System.currentTimeMillis(), paymentMethod
        );
        parentOrder.setSubOrders(subOrdersMap);
        parentOrder.setOrderSource(isFromBuyNow ? "buy_now" : "cart");

        atomicUpdate.put("Orders/" + parentOrderId, parentOrder);

        if (!isFromBuyNow) {
            for (CartItem item : selectedItems) {
                atomicUpdate.put("Carts/" + userId + "/" + item.getCartItemId(), null);
            }
        }

        long pointsEarned = (long) totalPayment;
        long finalPoints = currentPoints - pointsToUse + pointsEarned;
        atomicUpdate.put("Users/" + userId + "/points", finalPoints);

        rootRef.updateChildren(atomicUpdate).addOnCompleteListener(task -> {
            if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(CheckoutActivity.this, "Order placed successfully!", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(CheckoutActivity.this, OrderDetailsActivity.class);
                intent.putExtra("ORDER_ID", parentOrderId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Log.e("CheckoutActivity", "Failed to place order.", task.getException());
                Toast.makeText(CheckoutActivity.this,
                        "Failed to place order: " + (task.getException() != null ?
                                task.getException().getMessage() : "Unknown error"),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupToolbar() {
        binding.toolbarCheckout.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserDetailsAndRewards() {
        if (currentUser == null) return;
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.w("CheckoutActivity", "User data does not exist for UID: " + currentUser.getUid());
                    return;
                }
                DataSnapshot addressesSnapshot = snapshot.child("addresses");
                if (addressesSnapshot.exists() && addressesSnapshot.hasChildren()) {
                    ShippingAddress defaultAddress = null;
                    for (DataSnapshot addressData : addressesSnapshot.getChildren()) {
                        ShippingAddress currentAddress = addressData.getValue(ShippingAddress.class);
                        if (currentAddress != null && currentAddress.isDefault()) {
                            defaultAddress = currentAddress;
                            break;
                        }
                    }
                    if (defaultAddress == null) {
                        DataSnapshot firstAddressSnapshot = addressesSnapshot.getChildren().iterator().next();
                        defaultAddress = firstAddressSnapshot.getValue(ShippingAddress.class);
                    }
                    userShippingAddress = defaultAddress;
                    updateAddressUI(userShippingAddress);
                } else {
                    userShippingAddress = null;
                    updateAddressUI(null);
                }
                long userPoints = snapshot.child("points").exists() ? snapshot.child("points").getValue(Long.class) : 0;
                binding.tvCurrentPoints.setText(String.format(Locale.US, "You have %d points", userPoints));
                loadAvailableRewards(userPoints);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CheckoutActivity", "Failed to load user details.", error.toException());
            }
        });
    }

    private void loadAvailableRewards(long userPoints) {
        binding.rewardsProgressBar.setVisibility(View.VISIBLE);
        rewardsRef.orderByChild("pointsRequired").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.rewardsProgressBar.setVisibility(View.GONE);
                availableRewards.clear();
                for (DataSnapshot rewardSnapshot : snapshot.getChildren()) {
                    Reward reward = rewardSnapshot.getValue(Reward.class);
                    if (reward != null && reward.isActive() && userPoints >= reward.getPointsRequired()) {
                        availableRewards.add(reward);
                    }
                }
                setupDiscountOptions();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.rewardsProgressBar.setVisibility(View.GONE);
                Log.e("CheckoutActivity", "Failed to load rewards.", error.toException());
            }
        });
    }

    private void setupDiscountOptions() {
        binding.chipGroupDiscounts.clearCheck();
        if (binding.chipGroupDiscounts.getChildCount() > 1) {
            binding.chipGroupDiscounts.removeViews(1, binding.chipGroupDiscounts.getChildCount() - 1);
        }

        if (binding.layoutDiscount != null) {
            if (availableRewards.isEmpty()) {
                binding.layoutDiscount.setVisibility(View.GONE);
            } else {
                binding.layoutDiscount.setVisibility(View.VISIBLE);
                for (Reward reward : availableRewards) {
                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filter, binding.chipGroupDiscounts, false);
                    String chipText = String.format("%s (%d pts)", reward.getTitle(), reward.getPointsRequired());
                    chip.setText(chipText);
                    chip.setTag(reward);
                    chip.setId(View.generateViewId());
                    binding.chipGroupDiscounts.addView(chip);
                }
            }
        }

        binding.chipGroupDiscounts.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID || checkedId == R.id.chip_no_discount) {
                selectedReward = null;
            } else {
                Chip checkedChip = group.findViewById(checkedId);
                if (checkedChip != null) {
                    selectedReward = (Reward) checkedChip.getTag();
                }
            }
            applyDiscount(selectedReward);
        });

        binding.chipGroupDiscounts.check(R.id.chip_no_discount);
    }

    private void applyDiscount(@Nullable Reward reward) {
        if (reward == null) {
            discountAmount = 0.0;
        } else {
            if ("percentage".equalsIgnoreCase(reward.getRewardType())) {
                discountAmount = subtotal * (reward.getDiscountValue() / 100.0);
            } else {
                discountAmount = reward.getDiscountValue();
            }
        }
        updatePaymentSummary();
    }

    private void updateAddressUI(@Nullable ShippingAddress address) {
        if (address != null) {
            String nameAndPhone = address.getRecipientName() + " | " + address.getPhoneNumber();
            binding.tvRecipientNameAndPhone.setText(nameAndPhone);
            binding.tvRecipientNameAndPhone.setVisibility(View.VISIBLE);
            String fullAddress = address.getStreet() + ", " + address.getCity() + ", " + address.getState() + " " + address.getZipcode();
            binding.tvAddressLine.setText(fullAddress);
        } else {
            binding.tvRecipientNameAndPhone.setVisibility(View.GONE);
            binding.tvAddressLine.setText("No shipping address set. Please add one.");
        }
    }

    @Override
    public void onQuantityChanged(int position, int newQuantity) {
        // This method should be called from the adapter
        if (position >= 0 && position < selectedItems.size()) {
            CartItem item = selectedItems.get(position);
            item.setQuantity(newQuantity);
            validateStockForAllItems();
            displayOrderSummary();
        }
    }
}