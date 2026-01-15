package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.BuyerProductAdapter;
import com.example.beautyhub.models.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CategoryProductsActivity extends AppCompatActivity implements BuyerProductAdapter.OnProductInteractionListener {

    private RecyclerView recyclerView;
    private BuyerProductAdapter adapter;
    private List<Product> productList;
    private ProgressBar progressBar;
    private String categoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_produdcts); // Matches your filename

        // 1. Get Category ID and Name from Intent
        categoryId = getIntent().getStringExtra("CATEGORY_ID");
        String categoryName = getIntent().getStringExtra("CATEGORY_NAME"); // Optional if you passed it

        // 2. Initialize Views
        recyclerView = findViewById(R.id.rv_category_products);
        progressBar = findViewById(R.id.progress_bar);
        ImageView backBtn = findViewById(R.id.btn_back);
        TextView titleTv = findViewById(R.id.tv_category_title);

        if (categoryName != null) titleTv.setText(categoryName);
        backBtn.setOnClickListener(v -> finish());

        // 3. Setup RecyclerView
        productList = new ArrayList<>();
        adapter = new BuyerProductAdapter(this, productList, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // 4. Load Data
        fetchProductsByCategory();
    }

    private void fetchProductsByCategory() {
        progressBar.setVisibility(View.VISIBLE);

        // Filter products by categoryId field in Firebase
        FirebaseDatabase.getInstance().getReference("Products")
                .orderByChild("categoryId")
                .equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        productList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Product product = ds.getValue(Product.class);
                            if (product != null && product.isActive()) {
                                productList.add(product);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);

                        if (productList.isEmpty()) {
                            Toast.makeText(CategoryProductsActivity.this, "No products found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CategoryProductsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- Implement Adapter Callbacks ---
    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onAddToCartClick(Product product) {
        // Your logic to add to cart
        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSellerClick(String sellerId) {
        // Logic to visit seller shop
    }

    @Override
    public void onBuyNowClick(Product product) {
        // Fast checkout logic
    }

    @Override
    public void onFavouriteClick(Product product, boolean isFavourite) {
        // Toggle favourite logic
    }
}