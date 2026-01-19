package com.example.beautyhub.seller;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View; // Pastikan import View yang betul
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.databinding.ActivitySellerProductDetailBinding;
import com.example.beautyhub.models.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SellerProductDetailActivity extends AppCompatActivity {

    private ActivitySellerProductDetailBinding binding;
    private String productId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inisialisasi View Binding
        binding = ActivitySellerProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productId = getIntent().getStringExtra("PRODUCT_ID");

        if (productId == null) {
            Toast.makeText(this, "Product ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        loadProductDetails();

        // Klik butang Edit
        binding.btnEditProduct.setOnClickListener(v -> {
            if (currentProduct != null) {
                Intent intent = new Intent(this, AddProductActivity.class);
                intent.putExtra("PRODUCT_ID", productId);
                startActivity(intent);
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarSellerDetail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Product Details");
        }
        binding.toolbarSellerDetail.setNavigationOnClickListener(v -> finish());
    }

    private void loadProductDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Products").child(productId);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentProduct = snapshot.getValue(Product.class);
                if (currentProduct != null) {
                    displayData(currentProduct);
                } else {
                    Toast.makeText(SellerProductDetailActivity.this, "Product no longer exists", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SellerProductDetailActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayData(Product product) {
        // Maklumat Asas
        binding.tvProductName.setText(product.getName());
        binding.tvProductPrice.setText(String.format("RM %.2f", product.getPrice()));
        binding.tvProductStock.setText("Stock: " + product.getStock());
        binding.tvProductDescription.setText(product.getDescription());
        binding.tvCategory.setText(product.getCategory());

        // Maklumat Tambahan (Brand, Ingredients, Skin Type)
        if (product.getBrand() != null && !product.getBrand().isEmpty()) {
            binding.tvBrand.setVisibility(View.VISIBLE);
            binding.tvBrand.setText("Brand: " + product.getBrand());
        } else {
            binding.tvBrand.setVisibility(View.GONE);
        }

        if (product.getIngredients() != null && !product.getIngredients().isEmpty()) {
            binding.tvIngredients.setVisibility(View.VISIBLE);
            binding.tvIngredients.setText(product.getIngredients());
        } else {
            binding.tvIngredients.setVisibility(View.GONE);
        }

        if (product.getSkinType() != null && !product.getSkinType().isEmpty()) {
            binding.tvSkinType.setVisibility(View.VISIBLE);
            binding.tvSkinType.setText("Skin Type: " + product.getSkinType());
        } else {
            binding.tvSkinType.setVisibility(View.GONE);
        }

        // Paparan Harga Diskaun (Logic)
        if (product.getDiscountPrice() > 0) {
            binding.tvDiscountPrice.setVisibility(View.VISIBLE);
            binding.tvDiscountPrice.setText(String.format("RM %.2f", product.getDiscountPrice()));

            // Tambah kesan potong (strike-through) pada harga asal
            binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            binding.tvDiscountPrice.setVisibility(View.GONE);
            // Buang kesan potong jika tiada diskaun
            binding.tvProductPrice.setPaintFlags(binding.tvProductPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        // Paparan Imej menggunakan Glide
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            Glide.with(this)
                    .load(product.getImageUrls().get(0))
                    .placeholder(R.drawable.product_placeholder)
                    .into(binding.ivProductImage);
        }
    }
}