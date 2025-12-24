package com.example.beautyhub.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.admin.LogHelper;
import com.example.beautyhub.adapters.SellerProductAdapter;
import com.example.beautyhub.buyer.ProductDetailActivity;
import com.example.beautyhub.models.Product;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManageProductsActivity extends AppCompatActivity implements SellerProductAdapter.OnProductActionListener {

    private RecyclerView productsRecyclerView;
    private ProgressBar progressBar;
    private TextView noProductsTextView;
    private FloatingActionButton fabAddProduct;
    private MaterialToolbar toolbar;

    private SellerProductAdapter productAdapter;
    private List<Product> productList;

    private FirebaseAuth mAuth;
    private DatabaseReference productsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s_activity_manage_products);

        initViews();
        setupToolbar();
        setupListeners();
        setupRecyclerView();

        // Initialize LogHelper
        LogHelper.initialize(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchSellerProducts();
    }

    private void fetchSellerProducts() {
        progressBar.setVisibility(View.VISIBLE);
        noProductsTextView.setVisibility(View.GONE);
        productsRecyclerView.setVisibility(View.GONE);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        String sellerId = currentUser.getUid();
        Query sellerProductsQuery = productsRef.orderByChild("sellerId").equalTo(sellerId);

        sellerProductsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                productList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Product product = snapshot.getValue(Product.class);
                        if (product != null) {
                            product.setProductId(snapshot.getKey());
                            productList.add(product);
                        }
                    }
                }

                Collections.reverse(productList);
                productAdapter.notifyDataSetChanged();

                progressBar.setVisibility(View.GONE);
                if (productList.isEmpty()) {
                    noProductsTextView.setVisibility(View.VISIBLE);
                    productsRecyclerView.setVisibility(View.GONE);
                } else {
                    noProductsTextView.setVisibility(View.GONE);
                    productsRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageProductsActivity.this, "Failed to load products: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        productsRecyclerView = findViewById(R.id.rv_seller_products);
        progressBar = findViewById(R.id.progress_bar_seller);
        noProductsTextView = findViewById(R.id.tv_no_products);
        fabAddProduct = findViewById(R.id.fab_add_product);
        toolbar = findViewById(R.id.toolbar_manage_products);

        mAuth = FirebaseAuth.getInstance();
        productsRef = FirebaseDatabase.getInstance().getReference("Products");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Products");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupListeners() {
        fabAddProduct.setOnClickListener(v ->
                startActivity(new Intent(ManageProductsActivity.this, AddProductActivity.class)));
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        productList = new ArrayList<>();
        productAdapter = new SellerProductAdapter(this, productList, this);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setAdapter(productAdapter);
    }

    private void showDeleteConfirmationDialog(Product product) {
        if (product == null || product.getProductId() == null) {
            Toast.makeText(this, "Cannot delete product, ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete '" + product.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteProductFromFirebase(product);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProductFromFirebase(Product product) {
        String productId = product.getProductId();
        String productName = product.getName();

        productsRef.child(productId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Log the deletion
                String logDetails = "Seller deleted product: '" + productName + "' (ID: " + productId + ")";
                LogHelper.logCurrentUserAction("Product Deleted", logDetails, "Seller");

                Toast.makeText(ManageProductsActivity.this, "Product deleted successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ManageProductsActivity.this, "Failed to delete product.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Implement OnProductActionListener methods
    @Override
    public void onProductClick(Product product) {
        // Navigate to product details
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getProductId());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Product product) {
        Intent intent = new Intent(this, AddProductActivity.class);
        intent.putExtra("EDIT_PRODUCT_ID", product.getProductId());
        intent.putExtra("EDIT_PRODUCT", product);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Product product, int position) {
        showDeleteConfirmationDialog(product);
    }
}