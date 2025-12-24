package com.example.beautyhub.buyer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.OrderDetailSubOrderAdapter;
import com.example.beautyhub.databinding.ActivityOrderDetailsBinding;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Order;
import com.example.beautyhub.models.OrderItem;
import com.example.beautyhub.models.ShippingAddress;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailsActivity extends AppCompatActivity {

    private ActivityOrderDetailsBinding binding;
    private String orderId;
    private DatabaseReference orderRef;
    private Order currentOrder;
    private FirebaseUser currentUser;
    private static final double SHIPPING_FEE_WEST_MY = 5.00;
    private static final double SHIPPING_FEE_EAST_MY = 10.00;

    private static final String TAG = "OrderDetailsActivity";

    private final ActivityResultLauncher<Intent> addReviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Review submitted. Updating details...", Toast.LENGTH_SHORT).show();
                    loadOrderDetails();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("ORDER_ID");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Error: Order ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);

        setupToolbar();
        setupButtonListeners();
        loadOrderDetails();
    }

    private void setupToolbar() {
        binding.toolbarOrderDetail.setNavigationOnClickListener(v -> finish());
    }

    private void loadOrderDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.contentLayout.setVisibility(View.GONE);

        orderRef.get().addOnCompleteListener(task -> {
            binding.progressBar.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    currentOrder = snapshot.getValue(Order.class);
                    if (currentOrder != null) {
                        currentOrder.setOrderId(snapshot.getKey());
                        displayOrderData(currentOrder);
                        updateUIBasedOnStatus(currentOrder.getStatus());
                        binding.contentLayout.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(OrderDetailsActivity.this, "Failed to parse order data.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(OrderDetailsActivity.this, "Order not found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Exception exception = task.getException();
                Log.e("OrderDetails", "Failed to load order details: " + (exception != null ? exception.getMessage() : "Unknown error"));
                Toast.makeText(OrderDetailsActivity.this, "Failed to load details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayOrderData(Order order) {
        // Display Order ID
        if (order.getOrderId() != null && order.getOrderId().length() > 10) {
            binding.tvDetailOrderId.setText(String.format("Order #%s", order.getOrderId().substring(1, 10).toUpperCase()));
        } else {
            binding.tvDetailOrderId.setText(String.format("Order #%s", order.getOrderId()));
        }

        // Display Order Status
        binding.tvDetailOrderStatus.setText(order.getStatus());

        // Display Order Date - CHANGED: using getOrderDate() instead of getTimestamp()
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String formattedDate = sdf.format(new Date(order.getOrderDate()));
        binding.tvDetailOrderDate.setText(String.format("Placed on: %s", formattedDate));

        // Display Shipping Address
        ShippingAddress address = order.getShippingAddress();
        if (address != null) {
            String fullAddress = address.getRecipientName() + "\n" +
                    address.getStreet() + "\n" +
                    address.getCity() + ", " + address.getState() + " " + address.getZipcode();
            binding.tvDetailShippingAddress.setText(fullAddress);
            binding.tvDetailPhoneNumber.setText(String.format("Phone: %s", address.getPhoneNumber()));
        }

        // Display Payment Summary
        displayPaymentSummary(order);

        // Load seller names and setup RecyclerView
        if (order.getSubOrders() != null && !order.getSubOrders().isEmpty()) {
            loadSellerNamesAndSetupRecyclerView(new ArrayList<>(order.getSubOrders().values()));
        } else if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) { // CHANGED: using getOrderItems() instead of getItems()
            // Handle simple orders (without sub-orders)
            setupSimpleOrderRecyclerView(order);
        }
    }

    private void displayPaymentSummary(Order order) {
        double subtotal = calculateSubtotal(order);
        double totalPaid = order.getTotalAmount();

        // --- Logik Baharu Bermula Di Sini ---
        long sellerCount = 1; // Lalai kepada 1 jika tiada sub-pesanan
        if (order.getSubOrders() != null && !order.getSubOrders().isEmpty()) {
            sellerCount = order.getSubOrders().size();
        }

        double shippingFeePerSeller = SHIPPING_FEE_WEST_MY; // Lalai
        ShippingAddress address = order.getShippingAddress();
        if (address != null && "East Malaysia".equals(address.getZone())) {
            shippingFeePerSeller = SHIPPING_FEE_EAST_MY;
        }

        double shippingFee = sellerCount * shippingFeePerSeller;
        // --- Logik Baharu Berakhir Di Sini ---

        // Kira diskaun
        double discount = (subtotal + shippingFee) - totalPaid;

        // Paparkan nilai
        binding.tvPaymentSubtotal.setText(String.format(Locale.US, "RM %.2f", subtotal));
        binding.tvPaymentShipping.setText(String.format(Locale.US, "RM %.2f", shippingFee));
        binding.tvPaymentTotal.setText(String.format(Locale.US, "RM %.2f", totalPaid));

        // Display discount if exists
        if (discount > 0) {
            binding.layoutDetailDiscount.setVisibility(View.VISIBLE);
            binding.tvPaymentDiscount.setText(String.format(Locale.US, "- RM %.2f", discount));
        } else {
            binding.layoutDetailDiscount.setVisibility(View.GONE);
        }

        // Display payment method
        if (order.getPaymentMethod() != null && !order.getPaymentMethod().isEmpty()) {
            binding.tvPaymentMethod.setText(order.getPaymentMethod().toUpperCase());
        } else {
            binding.tvPaymentMethod.setText("N/A");
        }
    }

    private double calculateSubtotal(Order order) {
        double subtotal = 0.0;

        // Calculate from sub-orders
        if (order.getSubOrders() != null && !order.getSubOrders().isEmpty()) {
            for (Order subOrder : order.getSubOrders().values()) {
                if (subOrder.getOrderItems() != null) { // CHANGED: using getOrderItems() instead of getItems()
                    for (OrderItem item : subOrder.getOrderItems()) {
                        subtotal += item.getPrice() * item.getQuantity();
                    }
                }
            }
        }
        // Calculate from simple order items
        else if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) { // CHANGED: using getOrderItems() instead of getItems()
            for (OrderItem item : order.getOrderItems()) {
                subtotal += item.getPrice() * item.getQuantity();
            }
        }

        return subtotal;
    }

    private void loadSellerNamesAndSetupRecyclerView(ArrayList<Order> subOrders) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        List<Task<DataSnapshot>> sellerNameTasks = new ArrayList<>();

        for (Order subOrder : subOrders) {
            if (subOrder.getSellerId() != null && !subOrder.getSellerId().isEmpty()) {
                sellerNameTasks.add(usersRef.child(subOrder.getSellerId()).child("username").get());
            } else {
                sellerNameTasks.add(Tasks.forResult(null));
            }
        }

        Tasks.whenAllSuccess(sellerNameTasks).addOnSuccessListener(results -> {
            for (int i = 0; i < subOrders.size(); i++) {
                if (results.get(i) instanceof DataSnapshot) {
                    DataSnapshot snapshot = (DataSnapshot) results.get(i);
                    String sellerName = snapshot.getValue(String.class);
                    subOrders.get(i).setSellerName(sellerName != null ? sellerName : "Unknown Seller");
                }
            }
            setupRecyclerView(subOrders);
        }).addOnFailureListener(e -> {
            Log.e("OrderDetails", "Failed to load seller names.", e);
            setupRecyclerView(subOrders);
        });
    }

    private void setupRecyclerView(ArrayList<Order> subOrders) {
        OrderDetailSubOrderAdapter adapter = new OrderDetailSubOrderAdapter(this, subOrders, orderId, addReviewLauncher);
        binding.rvOrderDetailItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderDetailItems.setNestedScrollingEnabled(false);
        binding.rvOrderDetailItems.setAdapter(adapter);
    }

    private void setupSimpleOrderRecyclerView(Order order) {
        // Create a fake sub-order list for simple orders
        ArrayList<Order> fakeSubOrders = new ArrayList<>();
        Order fakeSubOrder = new Order();
        fakeSubOrder.setOrderItems(order.getOrderItems()); // CHANGED: using setOrderItems() instead of setItems()
        fakeSubOrder.setSellerId(order.getSellerId());
        fakeSubOrder.setSellerName(order.getSellerName());
        fakeSubOrders.add(fakeSubOrder);

        // Load seller name for this single order
        loadSellerNamesAndSetupRecyclerView(fakeSubOrders);
    }

    private void setupButtonListeners() {
        // Cancel Order Button
        binding.btnCancelOrder.setOnClickListener(v -> handleCancelOrder());

        // Order Again Button
        binding.btnOrderAgain.setOnClickListener(v -> handleOrderAgain());
    }

    private void handleCancelOrder() {
        if (currentOrder == null) return;

        // Check if order can be cancelled
        String status = currentOrder.getStatus().toLowerCase(Locale.ROOT);
        if (status.equals("completed") || status.equals("shipped") || status.equals("cancelled")) {
            Toast.makeText(this, "This order cannot be cancelled.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order? This action cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelEntireOrder(currentOrder))
                .setNegativeButton("No", null)
                .show();
    }

    private void handleOrderAgain() {
        if (currentOrder == null || currentUser == null) {
            Toast.makeText(this, "Cannot process request. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if order is completed
        String status = currentOrder.getStatus().toLowerCase(Locale.ROOT);
        if (!status.equals("completed") && !status.equals("cancelled")) {
            Toast.makeText(this, "You can only re-order completed or cancelled orders.", Toast.LENGTH_SHORT).show();
            return;
        }

        orderAgain();
    }

    private void cancelEntireOrder(Order orderToCancel) {
        binding.progressBar.setVisibility(View.VISIBLE);

        if (orderToCancel.getSubOrders() != null && !orderToCancel.getSubOrders().isEmpty()) {
            // Cancel all sub-orders
            for (String subOrderId : orderToCancel.getSubOrders().keySet()) {
                orderRef.child("subOrders").child(subOrderId).child("status").setValue("Cancelled");
            }
        }

        // Update main order status
        orderRef.child("status").setValue("Cancelled")
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(OrderDetailsActivity.this, "Order cancelled successfully.", Toast.LENGTH_SHORT).show();
                    loadOrderDetails(); // Refresh UI
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(OrderDetailsActivity.this, "Failed to cancel order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void orderAgain() {
        binding.progressBar.setVisibility(View.VISIBLE);
        List<CartItem> allItems = new ArrayList<>();

        // Process sub-orders (for orders with multiple sellers)
        if (currentOrder.getSubOrders() != null && !currentOrder.getSubOrders().isEmpty()) {
            for (Order subOrder : currentOrder.getSubOrders().values()) {
                if (subOrder.getOrderItems() != null) {
                    for (OrderItem orderItem : subOrder.getOrderItems()) {
                        CartItem cartItem = new CartItem();
                        // Set CartItem properties from OrderItem
                        cartItem.setProductId(orderItem.getProductId());
                        cartItem.setProductName(orderItem.getProductName());
                        cartItem.setQuantity(orderItem.getQuantity());
                        cartItem.setPrice(orderItem.getPrice());
                        cartItem.setImageUrl(orderItem.getProductImageUrl());
                        cartItem.setSellerId(subOrder.getSellerId());
                        // Set variant information if available
                        if (orderItem.getVariantId() != null && !orderItem.getVariantId().isEmpty()) {
                            cartItem.setVariantId(orderItem.getVariantId());
                            cartItem.setVariantName(orderItem.getVariantName());
                            cartItem.setHasVariants(true);
                        }
                        allItems.add(cartItem);
                    }
                }
            }
        }
        // Process simple order (for single seller orders)
        else if (currentOrder.getOrderItems() != null && !currentOrder.getOrderItems().isEmpty()) {
            for (OrderItem orderItem : currentOrder.getOrderItems()) {
                CartItem cartItem = new CartItem();
                // Set CartItem properties from OrderItem
                cartItem.setProductId(orderItem.getProductId());
                cartItem.setProductName(orderItem.getProductName());
                cartItem.setQuantity(orderItem.getQuantity());
                cartItem.setPrice(orderItem.getPrice());
                cartItem.setImageUrl(orderItem.getProductImageUrl());
                cartItem.setSellerId(currentOrder.getSellerId());
                // Set variant information if available
                if (orderItem.getVariantId() != null && !orderItem.getVariantId().isEmpty()) {
                    cartItem.setVariantId(orderItem.getVariantId());
                    cartItem.setVariantName(orderItem.getVariantName());
                    cartItem.setHasVariants(true);
                }
                allItems.add(cartItem);
            }
        }

        if (allItems.isEmpty()) {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "No items found in this order.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add items to cart in Firebase
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Carts").child(currentUser.getUid());
        List<Task<Void>> updateTasks = new ArrayList<>();

        for (CartItem item : allItems) {
            String newCartItemId = cartRef.push().getKey();
            if (newCartItemId != null) {
                item.setCartItemId(newCartItemId);
                Task<Void> task = cartRef.child(newCartItemId).setValue(item);
                updateTasks.add(task);
            }
        }

        Tasks.whenAll(updateTasks)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "All items added to your cart!", Toast.LENGTH_LONG).show();

                    // Navigate to cart
                    Intent intent = new Intent(OrderDetailsActivity.this, CartActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to add items to cart: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUIBasedOnStatus(String status) {
        int colorResId;
        boolean showCancelButton;
        boolean showOrderAgainButton;

        switch (status.toLowerCase(Locale.ROOT)) {
            case "pending":
            case "processing":
                colorResId = R.color.status_pending;
                showCancelButton = true;
                showOrderAgainButton = false;
                break;
            case "shipped":
                colorResId = R.color.status_shipped;
                showCancelButton = false;
                showOrderAgainButton = false;
                break;
            case "completed":
                colorResId = R.color.status_completed;
                showCancelButton = false;
                showOrderAgainButton = true;
                break;
            case "cancelled":
            case "refunded":
                colorResId = R.color.status_cancelled;
                showCancelButton = false;
                showOrderAgainButton = true;
                break;
            default:
                colorResId = R.color.status_pending;
                showCancelButton = false;
                showOrderAgainButton = true;
                break;
        }

        // Update status badge color
        if (binding.tvDetailOrderStatus.getBackground() instanceof GradientDrawable) {
            GradientDrawable background = (GradientDrawable) binding.tvDetailOrderStatus.getBackground().mutate();
            background.setColor(ContextCompat.getColor(this, colorResId));
        }

        // Show/hide buttons based on status
        binding.btnCancelOrder.setVisibility(showCancelButton ? View.VISIBLE : View.GONE);
        binding.btnOrderAgain.setVisibility(showOrderAgainButton ? View.VISIBLE : View.GONE);
    }

    // Helper method to get readable status
    private String getReadableStatus(String status) {
        switch (status.toLowerCase(Locale.ROOT)) {
            case "pending": return "Order Placed";
            case "processing": return "Processing";
            case "shipped": return "Shipped";
            case "completed": return "Delivered";
            case "cancelled": return "Cancelled";
            case "refunded": return "Refunded";
            default: return status;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh order details when returning to activity
        if (orderId != null) {
            loadOrderDetails();
        }
    }
}