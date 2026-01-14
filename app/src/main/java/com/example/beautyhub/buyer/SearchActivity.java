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
import com.example.beautyhub.models.Product;
import com.example.beautyhub.seller.SellerProfileActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

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

    // Gantikan kaedah performComprehensiveSearch yang lama dengan ini
    // Gantikan kaedah performComprehensiveSearch yang lama dengan versi yang telah dikemas kini ini
    // Gantikan kaedah performComprehensiveSearch yang lama dengan versi yang lebih pintar ini
    private void performComprehensiveSearch(String searchText) {
        String lowercasedQuery = searchText.toLowerCase().trim();
        updateUiForLoading();

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<Product> foundProducts = new ArrayList<>();

                // Definisikan sub-kategori di sini
                List<String> skincareSubCategories = Arrays.asList("cleansers", "serums", "moisturizers", "sunscreens", "toners", "face masks");
                List<String> makeupSubCategories = Arrays.asList("foundations", "lipstick", "mascara", "eyeliner", "eyeshadow", "concealer", "powder", "primer", "blusher");

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);

                    if (product != null && product.isActive() && product.hasStock()) {
                        product.setProductId(snapshot.getKey());

                        boolean nameMatches = product.getName() != null && product.getName().toLowerCase().contains(lowercasedQuery);
                        boolean brandMatches = product.getBrand() != null && product.getBrand().toLowerCase().contains(lowercasedQuery);
                        boolean sellerMatches = product.getSellerName() != null && product.getSellerName().toLowerCase().contains(lowercasedQuery);

                        // --- LOGIK BARU UNTUK KATEGORI ---
                        boolean categoryMatches = false;
                        String productCategory = product.getCategory() != null ? product.getCategory().toLowerCase() : "";

                        if (!productCategory.isEmpty()) {
                            // 1. Semak padanan terus (cth: cari "serum" jumpa produk kategori "serums")
                            if (productCategory.contains(lowercasedQuery)) {
                                categoryMatches = true;
                            }
                            // 2. Semak jika pengguna mencari kategori utama "skincare"
                            else if (lowercasedQuery.equals("skincare") && skincareSubCategories.contains(productCategory)) {
                                categoryMatches = true;
                            }
                            // 3. Semak jika pengguna mencari kategori utama "makeup"
                            else if (lowercasedQuery.equals("makeup") && makeupSubCategories.contains(productCategory)) {
                                categoryMatches = true;
                            }
                        }
                        // --- AKHIR LOGIK BARU ---

                        if (nameMatches || brandMatches || sellerMatches || categoryMatches) {
                            foundProducts.add(product);
                        }
                    }
                }
                productAdapter.updateProductList(foundProducts);
                updateUiWithResults();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SearchActivity.this, "Failed to load products: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
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
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getProductId());
        startActivity(intent);
    }

    // --- PERUBAHAN 2: Kemas kini tandatangan kaedah ---
    @Override
    public void onBuyNowClick(Product product) {
        // Implementasi logik 'Beli Sekarang' di sini
        // Contoh:
        Toast.makeText(this, "Buy now: " + product.getName(), Toast.LENGTH_SHORT).show();
        // Anda boleh salin logik dari BuyerActivity jika perlu
    }

    // --- PERUBAHAN 3: Kemas kini tandatangan kaedah ---
    @Override
    public void onAddToCartClick(Product product) {
        // Implementasi logik 'Tambah ke Troli' di sini
        // Contoh:
        Toast.makeText(this, "Added to cart: " + product.getName(), Toast.LENGTH_SHORT).show();
        // Anda boleh salin logik dari BuyerActivity jika perlu
    }

    @Override
    public void onFavouriteClick(Product product, boolean isFavourite) {
        // Implementasi logik kegemaran di sini
        String message = isFavourite ? "Added to favourites: " : "Removed from favourites: ";
        Toast.makeText(this, message + product.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSellerClick(String sellerId) {
        // Implementasi klik pada profil penjual di sini
        if (sellerId == null || sellerId.isEmpty() || sellerId.startsWith("json_")) {
            Toast.makeText(this, "This is an official store.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, SellerProfileActivity.class);
        intent.putExtra("SELLER_ID", sellerId);
        startActivity(intent);
    }


    // --- UI Helper Functions ---
    private void showKeyboardAndFocus() {
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void updateUiForLoading() {
        progressBar.setVisibility(View.VISIBLE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        emptySearchText.setVisibility(View.GONE);
    }

    // Ubah sedikit kaedah updateUiWithResults
    private void updateUiWithResults() {
        progressBar.setVisibility(View.GONE);
        // Gunakan getItemCount() dari adapter sebagai sumber kebenaran
        if (productAdapter.getItemCount() == 0) {
            emptySearchText.setText("No results found"); // Beri mesej yang jelas
            emptySearchText.setVisibility(View.VISIBLE);
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else {
            emptySearchText.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        }
    }


    private void clearResults() {
        productList.clear();
        productAdapter.updateProductList(new ArrayList<>());
        emptySearchText.setVisibility(View.VISIBLE);
        emptySearchText.setText(R.string.start_typing_to_search); // Guna rujukan string
        searchResultsRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }
}
