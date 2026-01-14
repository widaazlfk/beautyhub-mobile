package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.adapters.PriceComparisonAdapter;
import com.example.beautyhub.databinding.ActivityComparisonBinding;
import com.example.beautyhub.models.ProductComparison;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ComparisonActivity extends AppCompatActivity {

    private ActivityComparisonBinding binding;
    private PriceComparisonAdapter adapter;
    private List<ProductComparison> fullList = new ArrayList<>();
    private List<ProductComparison> displayList = new ArrayList<>();
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComparisonBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbRef = FirebaseDatabase.getInstance().getReference("Products");

        setupUI();
        loadData();
    }

    private void setupUI() {
        // --- KOD UNTUK BUTTON BACK ---
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new PriceComparisonAdapter(displayList);
        binding.rvSimilarProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSimilarProducts.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    toggleUI(true);
                    displayList.clear();
                    adapter.notifyDataSetChanged();
                } else {
                    searchProduct(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.rvSimilarProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                hideKeyboard();
            }
        });
    }

    private void searchProduct(String query) {
        displayList.clear();
        ProductComparison watsons = null;
        ProductComparison guardian = null;
        ProductComparison mainProduct = null;

        String q = query.toLowerCase();

        for (ProductComparison p : fullList) {
            if (p.getName() != null && p.getName().toLowerCase().contains(q)) {
                if (mainProduct == null) mainProduct = p;

                String seller = (p.getSellerName() != null) ? p.getSellerName() : "";

                if (seller.equalsIgnoreCase("Watsons Pharmacy")) {
                    watsons = p;
                } else if (seller.equalsIgnoreCase("Guardian Malaysia")) {
                    guardian = p;
                } else {
                    displayList.add(p);
                }
            }
        }

        if (watsons != null || guardian != null || !displayList.isEmpty()) {
            toggleUI(false);
            updateComparisonTable(watsons, guardian);

            if (watsons != null) updateAnalysisUI(watsons);
            else if (guardian != null) updateAnalysisUI(guardian);
            else if (mainProduct != null) updateAnalysisUI(mainProduct);
        } else {
            toggleUI(true);
        }
        adapter.notifyDataSetChanged();
    }

    private void updateComparisonTable(ProductComparison w, ProductComparison g) {
        binding.badgeWatsonsWinner.setVisibility(View.INVISIBLE);
        binding.badgeGuardianWinner.setVisibility(View.INVISIBLE);
        binding.ivWatsons.setImageResource(R.drawable.product_placeholder);
        binding.ivGuardian.setImageResource(R.drawable.product_placeholder);
        binding.tvPriceWatsons.setText("RM 0.00");
        binding.tvPriceGuardian.setText("RM 0.00");

        if (w != null) {
            binding.tvPriceWatsons.setText("RM " + String.format("%.2f", w.getPrice()));
            if (w.getImageUrls() != null && !w.getImageUrls().isEmpty()) {
                Glide.with(this).load(w.getImageUrls().get(0)).into(binding.ivWatsons);
            }
            binding.layoutWatsons.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProductDetailActivity.class);
                intent.putExtra("productId", w.getProductId());
                startActivity(intent);
            });
        }

        if (g != null) {
            binding.tvPriceGuardian.setText("RM " + String.format("%.2f", g.getPrice()));
            if (g.getImageUrls() != null && !g.getImageUrls().isEmpty()) {
                Glide.with(this).load(g.getImageUrls().get(0)).into(binding.ivGuardian);
            }
            binding.layoutGuardian.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProductDetailActivity.class);
                intent.putExtra("productId", g.getProductId());
                startActivity(intent);
            });
        }

        if (w != null && g != null && w.getPrice() > 0 && g.getPrice() > 0) {
            if (w.getPrice() < g.getPrice()) {
                binding.badgeWatsonsWinner.setVisibility(View.VISIBLE);
            } else if (g.getPrice() < w.getPrice()) {
                binding.badgeGuardianWinner.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ProductComparison p = ds.getValue(ProductComparison.class);
                    if (p != null) {
                        // SET PRODUCT ID DARI KEY FIREBASE
                        p.setProductId(ds.getKey());
                        fullList.add(p);
                    }
                }
                binding.progressBar.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleUI(boolean isEmpty) {
        if (isEmpty) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.nestedScrollView.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.nestedScrollView.setVisibility(View.VISIBLE);
        }
    }

    private void updateAnalysisUI(ProductComparison main) {
        binding.chipGroupIngredients.removeAllViews();
        if (main.getIngredients() != null) {
            String[] parts = main.getIngredients().split(",\\s*");
            for (int i = 0; i < Math.min(parts.length, 5); i++) {
                Chip chip = new Chip(this);
                chip.setText(parts[i]);
                binding.chipGroupIngredients.addView(chip);
            }
        }
    }

    private void hideKeyboard() {
        View v = this.getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}