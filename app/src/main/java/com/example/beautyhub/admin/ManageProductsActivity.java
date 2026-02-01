package com.example.beautyhub.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.models.Product;
import com.google.android.material.appbar.MaterialToolbar;
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
    // ChipGroup sudah dibuang
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
        setupSearch();
        // setupFilterChips() sudah dibuang
        fetchProductsFromFirebase();
    }

    private void initViews() {
        rvProducts = findViewById(R.id.rv_products);
        etSearchProducts = findViewById(R.id.et_search_products);
        emptyProductsState = findViewById(R.id.empty_products_state);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        allProductsList = new ArrayList<>();
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

                        String sellerId = product.getSellerId();
                        if (sellerId != null) {
                            FirebaseDatabase.getInstance().getReference("Users").child(sellerId)
                                    .child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                            if (userSnapshot.exists()) {
                                                product.setSellerName(userSnapshot.getValue(String.class));
                                            }
                                            filterProducts(); // Kemaskini list apabila nama seller dimuatkan
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                        }
                        allProductsList.add(product);
                    }
                }
                filterProducts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageProductsActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
            }
        });
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

    private void filterProducts() {
        String query = etSearchProducts.getText().toString().toLowerCase().trim();

        List<Product> filteredList = new ArrayList<>();
        for (Product product : allProductsList) {
            // Logik Carian Sahaja (Nama, Brand, Seller)
            String name = product.getName() != null ? product.getName().toLowerCase() : "";
            String brand = product.getBrand() != null ? product.getBrand().toLowerCase() : "";
            String seller = product.getSellerName() != null ? product.getSellerName().toLowerCase() : "";

            if (name.contains(query) || brand.contains(query) || seller.contains(query)) {
                filteredList.add(product);
            }
        }

        productAdapter.updateList(filteredList);
        updateEmptyState();
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

        final EditText etName = dialogView.findViewById(R.id.et_edit_product_name);
        final EditText etBrand = dialogView.findViewById(R.id.et_edit_product_brand);
        final AutoCompleteTextView actvCategory = dialogView.findViewById(R.id.et_edit_product_category);
        final EditText etSkinType = dialogView.findViewById(R.id.et_edit_product_skintype);
        final EditText etIngredients = dialogView.findViewById(R.id.et_edit_product_ingredients);

        fetchCategoriesForDialog(actvCategory);

        etName.setText(product.getName());
        etBrand.setText(product.getBrand());
        actvCategory.setText(product.getCategory(), false);
        etSkinType.setText(product.getSkinType());
        etIngredients.setText(product.getIngredients());

        new AlertDialog.Builder(this)
                .setTitle("Edit Product Information")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String brand = etBrand.getText().toString().trim();
                    String category = actvCategory.getText().toString().trim();
                    String skinType = etSkinType.getText().toString().trim();
                    String ingredients = etIngredients.getText().toString().trim();

                    if (name.isEmpty() || brand.isEmpty()) {
                        Toast.makeText(this, "Name and Brand are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Products")
                            .child(product.getProductId());

                    java.util.HashMap<String, Object> updates = new java.util.HashMap<>();
                    updates.put("name", name);
                    updates.put("brand", brand);
                    updates.put("category", category);
                    updates.put("skinType", skinType);
                    updates.put("ingredients", ingredients);

                    ref.updateChildren(updates).addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Product updated successfully!", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchCategoriesForDialog(AutoCompleteTextView actv) {
        FirebaseDatabase.getInstance().getReference("Categories")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> categories = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String name = ds.child("categoryName").getValue(String.class);
                            if (name == null) name = ds.getValue(String.class);
                            if (name != null) categories.add(name);
                        }
                        if (!categories.isEmpty()) {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    ManageProductsActivity.this,
                                    android.R.layout.simple_dropdown_item_1line,
                                    categories);
                            actv.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void toggleProductStatus(Product product) {
        DatabaseReference productNode = FirebaseDatabase.getInstance().getReference("Products").child(product.getProductId());
        boolean newStatus = !product.isActive();
        productNode.child("active").setValue(newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show());
    }

    private void deleteProduct(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Delete this product?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseDatabase.getInstance().getReference("Products")
                            .child(product.getProductId()).removeValue();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}