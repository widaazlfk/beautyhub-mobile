package com.example.beautyhub.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.models.Product;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageProductsActivity extends AppCompatActivity {

    private RecyclerView rvProducts;
    private EditText etSearchProducts;
    private ChipGroup chipGroupCategories;
    private ProductAdapter productAdapter;
    private List<Product> allProductsList;
    private LinearLayout emptyProductsState;

    private DatabaseReference productsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_manage_products);

        initViews();
        setupRecyclerView();
        setupFilterChips();
        setupSearch();
        fetchProductsFromFirebase();
    }

    private void initViews() {
        rvProducts = findViewById(R.id.rv_products);
        etSearchProducts = findViewById(R.id.et_search_products);
        chipGroupCategories = findViewById(R.id.chipGroup_categories);
        emptyProductsState = findViewById(R.id.empty_products_state);
        // Rujukan kepada ic_filter_products dan fabAddProduct telah dibuang
    }

    private void setupRecyclerView() {
        allProductsList = new ArrayList<>();
        // Menggunakan this::showProductActionMenu sebagai callback apabila item diklik
        productAdapter = new ProductAdapter(new ArrayList<>(), this::showProductActionMenu);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setAdapter(productAdapter);
    }

    private void fetchProductsFromFirebase() {
        productsRef = FirebaseDatabase.getInstance().getReference("Products");
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allProductsList.clear();
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    Product product = productSnapshot.getValue(Product.class);
                    if (product != null) {
                        product.setProductId(productSnapshot.getKey());
                        allProductsList.add(product);
                    }
                }
                // Tapis dan paparkan data selepas dimuat turun
                filterProducts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageProductsActivity.this, "Failed to load products: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilterChips() {
        // Menggunakan setOnCheckedStateChangeListener yang lebih moden untuk ChipGroup
        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> filterProducts());
    }

    private void setupSearch() {
        etSearchProducts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Fungsi setupFab() telah dibuang kerana FAB tidak lagi wujud

    private void filterProducts() {
        String searchQuery = etSearchProducts.getText().toString().toLowerCase().trim();
        List<String> selectedFilters = getSelectedFilters();

        List<Product> filteredList = new ArrayList<>();

        for (Product product : allProductsList) {
            boolean matchesSearch = product.getName().toLowerCase().contains(searchQuery) ||
                    product.getBrand().toLowerCase().contains(searchQuery);

            // Logik penapisan berdasarkan cip yang dipilih
            boolean matchesFilter = selectedFilters.isEmpty() ||
                    selectedFilters.contains("All Products") ||
                    (selectedFilters.contains("Skincare") && "Skincare".equalsIgnoreCase(product.getCategory())) ||
                    (selectedFilters.contains("Makeup") && "Makeup".equalsIgnoreCase(product.getCategory()));

            if (matchesSearch && matchesFilter) {
                filteredList.add(product);
            }
        }
        productAdapter.updateList(filteredList);
        updateEmptyState();
    }

    private List<String> getSelectedFilters() {
        List<String> selectedFilters = new ArrayList<>();
        int checkedChipId = chipGroupCategories.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = chipGroupCategories.findViewById(checkedChipId);
            selectedFilters.add(chip.getText().toString());
        } else {
            // Jika tiada cip yang dipilih (walaupun mustahil dengan selectionRequired=true), anggap "All Products"
            selectedFilters.add("All Products");
        }
        return selectedFilters;
    }

    private void updateEmptyState() {
        if (productAdapter.getItemCount() == 0) {
            emptyProductsState.setVisibility(View.VISIBLE);
            rvProducts.setVisibility(View.GONE);
        } else {
            emptyProductsState.setVisibility(View.GONE);
            rvProducts.setVisibility(View.VISIBLE);
        }
    }

    // Fungsi showAddProductDialog() telah dibuang

    private void showProductActionMenu(Product product) {
        final CharSequence[] options = {"Edit Product", "Toggle Status", "Delete Product", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle("Manage: " + product.getName())
                .setItems(options, (dialog, item) -> {
                    String selectedOption = options[item].toString();
                    switch (selectedOption) {
                        case "Edit Product":
                            showEditProductDialog(product);
                            break;
                        case "Toggle Status":
                            toggleProductStatus(product);
                            break;
                        case "Delete Product":
                            deleteProduct(product);
                            break;
                        case "Cancel":
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void showEditProductDialog(Product product) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.a_dialog_edit_product, null);

        final EditText etEditName = dialogView.findViewById(R.id.et_edit_product_name);
        final EditText etEditBrand = dialogView.findViewById(R.id.et_edit_product_brand);
        final EditText etEditPrice = dialogView.findViewById(R.id.et_edit_product_price);
        final EditText etEditStock = dialogView.findViewById(R.id.et_edit_product_stock);

        etEditName.setText(product.getName());
        etEditBrand.setText(product.getBrand());
        etEditPrice.setText(String.valueOf(product.getPrice()));
        etEditStock.setText(String.valueOf(product.getStock()));

        new AlertDialog.Builder(this)
                .setTitle("Edit Product")
                .setView(dialogView)
                .setPositiveButton("Save Changes", (dialog, which) -> {
                    String newName = etEditName.getText().toString().trim();
                    String newBrand = etEditBrand.getText().toString().trim();
                    String newPriceStr = etEditPrice.getText().toString().trim();
                    String newStockStr = etEditStock.getText().toString().trim();

                    if (newName.isEmpty() || newBrand.isEmpty() || newPriceStr.isEmpty() || newStockStr.isEmpty()) {
                        Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DatabaseReference productRef = FirebaseDatabase.getInstance().getReference("Products").child(product.getProductId());
                    productRef.child("name").setValue(newName);
                    productRef.child("brand").setValue(newBrand);
                    productRef.child("price").setValue(Double.parseDouble(newPriceStr));
                    productRef.child("stock").setValue(Integer.parseInt(newStockStr))
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void toggleProductStatus(Product product) {
        DatabaseReference productNode = FirebaseDatabase.getInstance().getReference("Products").child(product.getProductId());
        boolean newStatus = !product.isActive();
        productNode.child("active").setValue(newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Status changed to " + (newStatus ? "Active" : "Inactive"), Toast.LENGTH_SHORT).show());
    }

    private void deleteProduct(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete '" + product.getName() + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    DatabaseReference productNode = FirebaseDatabase.getInstance().getReference("Products").child(product.getProductId());
                    productNode.removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Product deleted.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
