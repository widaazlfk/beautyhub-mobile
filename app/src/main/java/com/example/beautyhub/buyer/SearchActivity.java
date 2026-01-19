package com.example.beautyhub.buyer;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.BuyerProductAdapter;
import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.models.CartItem;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.seller.SellerProfileActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchActivity extends AppCompatActivity implements BuyerProductAdapter.OnProductInteractionListener {

    private MaterialToolbar toolbar;
    private EditText searchInput;
    private ImageView micIcon;
    private RecyclerView searchResultsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptySearchText;

    private BuyerProductAdapter productAdapter;
    private List<Product> productList;
    private DatabaseReference productsRef;
    private DatabaseReference favoritesRef;
    private FirebaseUser currentUser;

    private static final int VOICE_SEARCH_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupRecyclerView();
        setupListeners();
        showKeyboardAndFocus();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_search);
        searchInput = findViewById(R.id.search_input);
        micIcon = findViewById(R.id.iv_mic);
        searchResultsRecyclerView = findViewById(R.id.rv_search_results);
        progressBar = findViewById(R.id.progress_bar_search);
        emptySearchText = findViewById(R.id.empty_search_text);

        productsRef = FirebaseDatabase.getInstance().getReference("Products");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Gunakan "Favourites" untuk selaras dengan BuyerActivity anda
            favoritesRef = FirebaseDatabase.getInstance().getReference("Favourites").child(currentUser.getUid());
        }
    }

    private void setupRecyclerView() {
        productList = new ArrayList<>();
        productAdapter = new BuyerProductAdapter(this, productList, this);
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        searchResultsRecyclerView.setAdapter(productAdapter);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> {
            hideKeyboard();
            finish();
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchInput.getText().toString().trim();
                hideKeyboard();
                if (!query.isEmpty()) {
                    performComprehensiveSearch(query);
                } else {
                    clearResults();
                }
                return true;
            }
            return false;
        });

        if (micIcon != null) {
            micIcon.setOnClickListener(v -> startVoiceSearch());
        }
    }

    private void performComprehensiveSearch(String searchText) {
        String query = searchText.toLowerCase().trim();
        updateUiForLoading();

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                productList.clear();

                List<String> skincareKeys = Arrays.asList("cleanser", "serum", "moisturizer", "sunscreen", "toner", "face mask", "skincare");
                List<String> makeupKeys = Arrays.asList("foundation", "lipstick", "mascara", "eyeliner", "eyeshadow", "powder", "makeup");

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);

                    if (product != null && product.isActive() && product.hasStock()) {
                        product.setProductId(snapshot.getKey());

                        String pName = (product.getName() != null) ? product.getName().toLowerCase() : "";
                        String pBrand = (product.getBrand() != null) ? product.getBrand().toLowerCase() : "";
                        String pSeller = (product.getSellerName() != null) ? product.getSellerName().toLowerCase() : "";
                        String pCategory = (product.getCategory() != null) ? product.getCategory().toLowerCase() : "";
                        String pDesc = (product.getDescription() != null) ? product.getDescription().toLowerCase() : "";

                        boolean matchesText = pName.contains(query) ||
                                pBrand.contains(query) ||
                                pSeller.contains(query) ||
                                pDesc.contains(query);

                        boolean matchesCategory = pCategory.contains(query);

                        if (query.equals("makeup") && makeupKeys.contains(pCategory)) matchesCategory = true;
                        if (query.equals("skincare") && skincareKeys.contains(pCategory)) matchesCategory = true;

                        if (matchesText || matchesCategory) {
                            productList.add(product);
                        }
                    }
                }

                productAdapter.notifyDataSetChanged();
                updateUiWithResults();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SearchActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUiForLoading() {
        progressBar.setVisibility(View.VISIBLE);
        emptySearchText.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
    }

    private void updateUiWithResults() {
        progressBar.setVisibility(View.GONE);
        if (productList.isEmpty()) {
            emptySearchText.setVisibility(View.VISIBLE);
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else {
            emptySearchText.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void clearResults() {
        productList.clear();
        productAdapter.notifyDataSetChanged();
        emptySearchText.setVisibility(View.GONE);
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search products, brands...");

        try {
            startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Voice search not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_SEARCH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                searchInput.setText(spokenText);
                performComprehensiveSearch(spokenText);
            }
        }
    }

    private void showKeyboardAndFocus() {
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onBuyNowClick(Product product) {
        if (currentUser != null) {
            if (product.getStock() <= 0) {
                Toast.makeText(this, "Out of stock", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, CheckoutActivity.class);
            ArrayList<CartItem> items = new ArrayList<>();
            String img = (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) ? product.getImageUrls().get(0) : "";

            CartItem item = new CartItem(product.getProductId(), product.getName(), product.getFinalPrice(), 1, img, product.getSellerProfileImageUrl());
            item.setSellerId(product.getSellerId());
            item.setSellerName(product.getSellerName());

            items.add(item);
            intent.putParcelableArrayListExtra("CHECKOUT_ITEMS", items);
            intent.putExtra("SOURCE", "BUY_NOW");
            startActivity(intent);
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    @Override
    public void onAddToCartClick(Product product) {
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        // Logik ringkas tambah ke Cart (selaraskan dengan logic BuyerActivity anda jika perlu)
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Carts").child(currentUser.getUid());
        cartRef.child(product.getSellerId()).child(product.getProductId()).setValue(true)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSellerClick(String sellerId) {
        if (sellerId != null && !sellerId.isEmpty()) {
            Intent intent = new Intent(this, SellerProfileActivity.class);
            intent.putExtra("SELLER_ID", sellerId);
            startActivity(intent);
        }
    }

    @Override
    public void onFavouriteClick(Product product, boolean isFavourite) {
        if (currentUser == null) {
            Toast.makeText(this, "Please login to manage favorites", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        String productId = product.getProductId();
        // favoritesRef sudah di-init di initViews() merujuk ke Favourites/uid
        favoritesRef.child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Jika sudah wujud (toggle off), buang
                    favoritesRef.child(productId).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(SearchActivity.this, "Removed from Favourites", Toast.LENGTH_SHORT).show());
                } else {
                    // Jika belum wujud (toggle on), simpan
                    favoritesRef.child(productId).setValue(true)
                            .addOnSuccessListener(aVoid -> Toast.makeText(SearchActivity.this, "Added to Favourites", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SearchActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
} 