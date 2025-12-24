package com.example.beautyhub.buyer;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Variant; // Import the Variant model
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

// Implement the listener interface
public class SearchActivity extends AppCompatActivity implements BuyerProductAdapter.OnProductInteractionListener {

    // UI Components
    private MaterialToolbar toolbar;
    private EditText searchInput;
    private ImageView micIcon;
    private RecyclerView searchResultsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptySearchText;

    // Firebase & Adapters
    private BuyerProductAdapter productAdapter;
    private List<Product> productList;
    private DatabaseReference productsRef;

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
    }

    private void setupRecyclerView() {
        productList = new ArrayList<>();
        // Pass 'this' as the third argument for the listener
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
        String lowercasedQuery = searchText.toLowerCase();
        updateUiForLoading();

        HashSet<Product> uniqueProducts = new HashSet<>();
        // Corrected the search count to match the number of search fields
        final int[] searchCounter = {3};
        Runnable onSearchComplete = () -> {
            productList.clear();
            productList.addAll(uniqueProducts);
            productAdapter.notifyDataSetChanged(); // Use notifyDataSetChanged for updates
            updateUiWithResults();
        };

        // Search across different fields
        searchByField("name_lowercase", lowercasedQuery, uniqueProducts, searchCounter, onSearchComplete);
        searchByField("category_lowercase", lowercasedQuery, uniqueProducts, searchCounter, onSearchComplete);
        searchByField("brand_lowercase", lowercasedQuery, uniqueProducts, searchCounter, onSearchComplete);
    }

    private void searchByField(String field, String query, HashSet<Product> uniqueProducts, int[] counter, Runnable onComplete) {
        Query searchQuery = productsRef.orderByChild(field)
                .startAt(query)
                .endAt(query + "\uf8ff");

        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);
                    // Ensure the product is valid before adding
                    if (product != null && product.isActive() && product.getStock() > 0) {
                        product.setProductId(snapshot.getKey());
                        uniqueProducts.add(product);
                    }
                }
                counter[0]--;
                if (counter[0] == 0) {
                    onComplete.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                counter[0]--;
                if (counter[0] == 0) {
                    onComplete.run();
                }
                Toast.makeText(SearchActivity.this, "Search on " + field + " failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for products, brands, or categories...");

        try {
            startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Sorry, your device doesn't support voice input", Toast.LENGTH_SHORT).show();
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
                searchInput.setSelection(spokenText.length());
                hideKeyboard();
                performComprehensiveSearch(spokenText);
            }
        }
    }

    // --- Implementation of BuyerProductAdapter.OnProductInteractionListener ---

    @Override
    public void onProductClick(Product product) {
        // Example: Navigate to ProductDetailActivity
        // Intent intent = new Intent(this, ProductDetailActivity.class);
        // intent.putExtra("PRODUCT_ID", product.getProductId());
        // startActivity(intent);
        Toast.makeText(this, "Clicked on: " + product.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBuyNowClick(Product product, Variant variant) {
        // Handle buy now logic
        String variantInfo = variant != null ? " with variant " + variant.getName() : "";
        Toast.makeText(this, "Buy now: " + product.getName() + variantInfo, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddToCartClick(Product product, Variant variant) {
        // Handle add to cart logic
        String variantInfo = variant != null ? " with variant " + variant.getName() : "";
        Toast.makeText(this, "Added to cart: " + product.getName() + variantInfo, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFavouriteClick(Product product, boolean isFavourite) {
        // Handle favourite toggle logic
        String message = isFavourite ? "Added to favourites: " : "Removed from favourites: ";
        Toast.makeText(this, message + product.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSellerClick(String sellerId) {
        // Handle seller profile click
        Toast.makeText(this, "Clicked on seller ID: " + sellerId, Toast.LENGTH_SHORT).show();
    }


    // --- UI Helper Functions ---
    private void showKeyboardAndFocus() {
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void updateUiForLoading() {
        progressBar.setVisibility(View.VISIBLE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        emptySearchText.setVisibility(View.GONE);
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
        emptySearchText.setVisibility(View.VISIBLE);
        emptySearchText.setText("Start typing to search for products");
        searchResultsRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }
}
