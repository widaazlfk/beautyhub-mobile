package com.example.beautyhub.seller;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.SellerProductAdapter;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.buyer.ProductDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SellerProductActivity extends Fragment implements SellerProductAdapter.OnProductActionListener {

    private RecyclerView recyclerView;
    private SellerProductAdapter adapter;
    private List<Product> productList = new ArrayList<>();
    private ProgressBar progressBar; // Added ProgressBar for loading state

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_seller_products, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progress_bar); // Initialize ProgressBar
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Pass the correct listener interface implementation (this fragment)
        adapter = new SellerProductAdapter(getContext(), productList, this);
        recyclerView.setAdapter(adapter);

        loadProducts();

        return view;
    }

    private void loadProducts() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "You are not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String sellerId = currentUser.getUid();

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE); // Show progress bar while loading
        }

        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("Products");
        Query sellerProductsQuery = productsRef.orderByChild("sellerId").equalTo(sellerId);

        sellerProductsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                productList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);
                    if (product != null) {
                        // Set the product ID from the snapshot key
                        product.setProductId(snapshot.getKey());
                        productList.add(product);
                    }
                }
                adapter.notifyDataSetChanged(); // Notify adapter about data changes
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE); // Hide progress bar
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(getContext(), "Failed to load products: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onProductClick(Product product) {
        // Navigate to product detail
        Intent intent = new Intent(getActivity(), ProductDetailActivity.class);
        intent.putExtra("productId", product.getProductId());
        startActivity(intent);
    }

    // KOD BARU YANG BETUL
    @Override
    public void onEditClick(Product product) {
        // Buka AddProductActivity dalam mod 'Edit'
        Intent intent = new Intent(getActivity(), AddProductActivity.class);

        // Hantar ID produk dan keseluruhan objek produk
        // Objek 'Product' mesti implement 'Serializable' untuk ini berfungsi
        intent.putExtra("EDIT_PRODUCT_ID", product.getProductId());
        intent.putExtra("EDIT_PRODUCT", product);

        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Product product, int position) {
        // Show confirmation dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteProduct(product.getProductId(), position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct(String productId, int position) {
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(getContext(), "Product ID is invalid.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Delete from Firebase
        FirebaseDatabase.getInstance().getReference("Products")
                .child(productId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    // The ValueEventListener will automatically update the list.
                    // Manually removing from the adapter can cause issues with real-time updates.
                    Toast.makeText(getContext(), "Product deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
