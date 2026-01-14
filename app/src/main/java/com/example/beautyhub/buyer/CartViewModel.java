package com.example.beautyhub.buyer;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.CartSeller;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.User;
// --- PERUBAHAN 1: Padam import Variant ---
// import com.example.beautyhub.models.Variant;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CartViewModel extends AndroidViewModel {

    // LiveData untuk UI
    private final MutableLiveData<List<CartSeller>> cartData = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> stockValidationError = new MutableLiveData<>();
    private final MutableLiveData<Integer> unavailableItemCount = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // Firebase & Data Cache
    private final DatabaseReference cartsRef;
    private final DatabaseReference usersRef;
    private ValueEventListener cartListener;
    private final Map<String, Boolean> previousSelection = new HashMap<>();
    private Map<String, Product> productCache = new HashMap<>();

    public CartViewModel(@NonNull Application application) {
        super(application);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            cartsRef = FirebaseDatabase.getInstance().getReference("Carts").child(currentUser.getUid());
        } else {
            cartsRef = null;
        }
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
    }

    // --- Getters untuk LiveData ---
    public LiveData<List<CartSeller>> getCartData() { return cartData; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getStockValidationError() { return stockValidationError; }
    public LiveData<Integer> getUnavailableItemCount() { return unavailableItemCount; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    public void loadCart(Map<String, Product> productCache) {
        this.productCache = productCache;
        if (cartsRef == null) {
            error.setValue("User not logged in.");
            return;
        }
        isLoading.setValue(true);

        if (cartListener != null) {
            cartsRef.removeEventListener(cartListener);
        }
        setupCartRealtimeListener();
    }

    private void setupCartRealtimeListener() {
        if (cartsRef == null) return;
        cartListener = cartsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    cartData.postValue(new ArrayList<>());
                    isLoading.postValue(false);
                    return;
                }
                processCartSnapshot(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.postValue(databaseError.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    private void processCartSnapshot(DataSnapshot userCartSnapshot) {
        Map<String, List<CartItem>> itemsBySeller = new LinkedHashMap<>();

        if (cartData.getValue() != null) {
            for (CartSeller seller : cartData.getValue()) {
                for (CartItem item : seller.getCartItems()) {
                    previousSelection.put(item.getCartItemId(), item.isSelected());
                }
            }
        }

        for (DataSnapshot sellerSnapshot : userCartSnapshot.getChildren()) {
            String folderSellerId = sellerSnapshot.getKey();
            if (folderSellerId == null) continue;

            for (DataSnapshot itemSnapshot : sellerSnapshot.getChildren()) {
                try {
                    CartItem item = itemSnapshot.getValue(CartItem.class);
                    if (item != null && item.getProductId() != null) {
                        item.setCartItemId(itemSnapshot.getKey());

                        Product product = productCache.get(item.getProductId());
                        enhanceItemWithProductData(item, folderSellerId, product);

                        if (previousSelection.containsKey(item.getCartItemId())) {
                            item.setSelected(Boolean.TRUE.equals(previousSelection.get(item.getCartItemId())));
                        }
                        itemsBySeller.computeIfAbsent(folderSellerId, k -> new ArrayList<>()).add(item);
                    }
                } catch (Exception e) {
                    Log.e("CartViewModel", "Error processing item: " + e.getMessage());
                }
            }
        }
        fetchSellerDetailsAndGroup(itemsBySeller);
    }

    // --- PERUBAHAN 2: Permudahkan kaedah enhanceItemWithProductData ---
    private void enhanceItemWithProductData(CartItem cartItem, String sellerId, Product product) {
        cartItem.setSellerId(sellerId);

        if (product == null) {
            cartItem.setName(cartItem.getName() != null ? cartItem.getName() : "Product unavailable");
            cartItem.setPrice(0);
            cartItem.setAvailable(false);
            cartItem.setSellerName("Unknown Seller");
            return;
        }

        cartItem.setName(product.getName());
        cartItem.setFromJson(product.isPreloaded());
        // Ketersediaan produk kini hanya bergantung pada stok utama
        cartItem.setAvailable(product.isActive() && product.hasStock());

        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            cartItem.setImageUrls(product.getImageUrls().get(0));
        }

        // Harga item troli kini sentiasa harga akhir produk
        cartItem.setPrice(product.getFinalPrice());

        if (product.isPreloaded()) {
            cartItem.setSellerName(product.getBrand() != null ? product.getBrand() : "Official Partner");
        } else {
            // Nama penjual akan diambil kemudian dalam fetchSellerDetailsAndGroup
        }
    }

    private void fetchSellerDetailsAndGroup(Map<String, List<CartItem>> itemsBySeller) {
        List<CartSeller> newSellerList = new ArrayList<>();
        if (itemsBySeller.isEmpty()) {
            cartData.postValue(newSellerList);
            isLoading.postValue(false);
            return;
        }

        AtomicInteger pendingSellers = new AtomicInteger(itemsBySeller.size());

        for (Map.Entry<String, List<CartItem>> entry : itemsBySeller.entrySet()) {
            String sellerId = entry.getKey();
            List<CartItem> items = entry.getValue();

            if (sellerId.startsWith("json_")) {
                String sellerNameFromJson = "Official Partner";
                if (!items.isEmpty() && items.get(0).getSellerName() != null) {
                    sellerNameFromJson = items.get(0).getSellerName();
                }
                newSellerList.add(new CartSeller(sellerId, sellerNameFromJson, null, items, true));
                if (pendingSellers.decrementAndGet() == 0) {
                    sortAndPost(newSellerList);
                }
                continue;
            }

            usersRef.child(sellerId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                    String sellerName = "Unknown Seller";
                    String sellerProfileUrl = null;
                    if (userSnapshot.exists()) {
                        User sellerUser = userSnapshot.getValue(User.class);
                        if (sellerUser != null && sellerUser.getUsername() != null) {
                            sellerName = sellerUser.getUsername();
                            sellerProfileUrl = sellerUser.getProfileImage();
                        }
                    }
                    newSellerList.add(new CartSeller(sellerId, sellerName, sellerProfileUrl, items, false));
                    if (pendingSellers.decrementAndGet() == 0) {
                        sortAndPost(newSellerList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    newSellerList.add(new CartSeller(sellerId, "Seller (Error)", null, items, false));
                    if (pendingSellers.decrementAndGet() == 0) {
                        sortAndPost(newSellerList);
                    }
                }
            });
        }
    }

    private void sortAndPost(List<CartSeller> list) {
        list.sort((o1, o2) -> {
            if (o1.isFromJsonSeller() != o2.isFromJsonSeller()) {
                return o1.isFromJsonSeller() ? -1 : 1;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getSellerName(), o2.getSellerName());
        });
        calculateUnavailableItems(list);
        cartData.postValue(list);
        isLoading.postValue(false);
    }

    // --- PERUBAHAN 3: Permudahkan kaedah validateStockForItems ---
    public void validateStockForItems(List<CartItem> items) {
        if (this.productCache == null || this.productCache.isEmpty()) {
            stockValidationError.setValue("Product information is not ready. Please try again.");
            return;
        }

        StringBuilder errorMessage = new StringBuilder();
        int errorCount = 0;

        for (CartItem item : items) {
            Product product = this.productCache.get(item.getProductId());

            if (product == null || !product.isActive()) {
                errorCount++;
                errorMessage.append("• ").append(item.getName()).append(" is no longer available.\n");
                continue;
            }

            int availableStock = product.getStock(); // Terus ambil stok produk

            if (item.getQuantity() > availableStock) {
                errorCount++;
                errorMessage.append("• ").append(product.getName())
                        .append(": Only ").append(availableStock)
                        .append(" left, you have ").append(item.getQuantity()).append(".\n");
            }
        }

        if (errorCount > 0) {
            stockValidationError.setValue(errorMessage.toString());
        } else {
            stockValidationError.setValue(null); // Tiada ralat
        }
    }

    private void calculateUnavailableItems(List<CartSeller> sellers) {
        int count = 0;
        for (CartSeller seller : sellers) {
            for (CartItem item : seller.getCartItems()) {
                if (!item.isAvailable()) {
                    count++;
                }
            }
        }
        unavailableItemCount.postValue(count);
    }

    // --- PERUBAHAN 4: Permudahkan kaedah checkStockAndUpdateQuantity ---
    public void checkStockAndUpdateQuantity(String sellerId, String cartItemId, int newQuantity) {
        List<CartSeller> sellers = cartData.getValue();
        if (sellers == null) return;

        CartItem foundItem = null;
        for (CartSeller seller : sellers) {
            if (seller.getSellerId().equals(sellerId)) {
                for (CartItem item : seller.getCartItems()) {
                    if (item.getCartItemId().equals(cartItemId)) {
                        foundItem = item;
                        break;
                    }
                }
            }
            if (foundItem != null) break;
        }

        if (foundItem == null) return;

        Product product = productCache.get(foundItem.getProductId());
        if (product != null) {
            int maxQuantity = product.getStock(); // Terus guna stok produk
            if (newQuantity > maxQuantity) {
                toastMessage.setValue("Maximum quantity is " + maxQuantity);
                return;
            }
        }

        if (cartsRef != null) {
            cartsRef.child(sellerId).child(cartItemId).child("quantity").setValue(newQuantity);
        }
    }

    // --- PERUBAHAN 5: Padam kaedah updateVariant ---
    /*
    public void updateVariant(String sellerId, String cartItemId, String newVariantId) {
        // ... KESELURUHAN KAEDAH INI DIPADAM ...
    }
    */

    public void clearStockValidationError() {
        stockValidationError.setValue(null);
    }

    public void clearToastMessage() {
        toastMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (cartsRef != null && cartListener != null) {
            cartsRef.removeEventListener(cartListener);
        }
    }
}
