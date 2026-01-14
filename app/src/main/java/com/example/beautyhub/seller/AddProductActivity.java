package com.example.beautyhub.seller;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.beautyhub.R;
import com.example.beautyhub.admin.LogHelper;
import com.example.beautyhub.databinding.SActivityAddProductBinding;
import com.example.beautyhub.models.Product;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Objects;


public class AddProductActivity extends AppCompatActivity {

    private SActivityAddProductBinding binding;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;

    // Senarai untuk imej BARU yang dipilih dari galeri (dalam bentuk Uri)
    private final List<Uri> newImageUris = new ArrayList<>();
    // Senarai untuk URL imej LAMA yang sedia ada
    private final List<String> existingImageUrls = new ArrayList<>();
    // Senarai untuk URL imej BARU yang telah berjaya dimuat naik
    private final List<String> uploadedImageUrls = new ArrayList<>();

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<CropImageContractOptions> imageCropperLauncher;

    // Pembolehubah untuk mod edit
    private boolean isEditMode = false;
    private String editingProductId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SActivityAddProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inisialisasi Cloudinary
        try {
            if (MediaManager.get() == null) {
                MediaManager.init(this);
            }
        } catch (Exception e) {
            Log.e("AddProductActivity", "Cloudinary init failed", e);
        }

        // Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        if (currentUser == null) {
            Toast.makeText(this, "Authentication Error. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Sediakan ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Semak jika dalam mod 'Edit' atau 'Tambah'
        if (getIntent().hasExtra("PRODUCT_ID")) {
            isEditMode = true;
            editingProductId = getIntent().getStringExtra("PRODUCT_ID");
            setupForEditMode();
        } else {
            isEditMode = false;
            setupForAddMode();
        }

        // Panggil fungsi persediaan
        initImageLaunchers();
        setupToolbar();
        setupListeners();
        fetchCategoriesFromFirebase();
    }

    private void setupForEditMode() {
        progressDialog.setTitle("Updating Product");
        binding.toolbarAddProduct.setTitle("Edit Product");
        binding.btnSaveProduct.setText("Update Product");
        loadProductData(editingProductId);
    }

    private void setupForAddMode() {
        progressDialog.setTitle("Adding Product");
        binding.toolbarAddProduct.setTitle("Add New Product");
    }

    private void loadProductData(String productId) {
        progressDialog.setMessage("Loading product details...");
        progressDialog.show();

        DatabaseReference productRef = databaseReference.child("Products").child(productId);
        productRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Product product = snapshot.getValue(Product.class);
                    if (product != null) {
                        populateFieldsForEdit(product);
                    }
                } else {
                    Toast.makeText(AddProductActivity.this, "Product not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(AddProductActivity.this, "Failed to load product data.", Toast.LENGTH_SHORT).show();
                Log.e("AddProductActivity", "loadProductData onCancelled: ", error.toException());
                finish();
            }
        });
    }

    private void initImageLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        launchImageCropper(selectedImageUri);
                    }
                }
        );

        imageCropperLauncher = registerForActivityResult(
                new CropImageContract(),
                result -> {
                    if (result.isSuccessful() && result.getUriContent() != null) {
                        Uri croppedImageUri = result.getUriContent();
                        if (!newImageUris.contains(croppedImageUri)) {
                            newImageUris.add(croppedImageUri);
                            updateImagePreviews();
                        } else {
                            Toast.makeText(this, "Image already selected.", Toast.LENGTH_SHORT).show();
                        }
                    } else if (result.getError() != null) {
                        Log.e("AddProductActivity", "Image Cropping Error", result.getError());
                        Toast.makeText(this, "Image cropping cancelled or failed.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupToolbar() {
        binding.toolbarAddProduct.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnAddImage.setOnClickListener(v -> openImagePicker());
        binding.btnSaveProduct.setOnClickListener(v -> validateAndSaveProduct());
        binding.btnCancel.setOnClickListener(v -> finish());
    }

    private void populateFieldsForEdit(Product product) {
        binding.etProductName.setText(product.getName());
        binding.etProductPrice.setText(String.valueOf(product.getPrice()));
        binding.etStock.setText(String.valueOf(product.getStock()));
        binding.etProductDescription.setText(product.getDescription());
        binding.actvCategory.setText(product.getCategory(), false);
        binding.etBrand.setText(product.getBrand());

        // Handle nullable fields
        if (product.getIngredients() != null) {
            binding.etIngredients.setText(product.getIngredients());
        }
        if (product.getSkinType() != null) {
            binding.etSkinType.setText(product.getSkinType());
        }
        if (product.getDiscountPrice() > 0) {
            binding.etDiscountPrice.setText(String.valueOf(product.getDiscountPrice()));
        }

        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            existingImageUrls.clear();
            existingImageUrls.addAll(product.getImageUrls());
            updateImagePreviews();
        }
    }

    private void updateImagePreviews() {
        // Simpan butang 'Add Image' sebelum memadam semua view
        binding.imageContainer.removeView(binding.btnAddImage);
        binding.imageContainer.removeAllViews();

        // 1. Papar imej sedia ada (dari URL)
        for (String imageUrl : new ArrayList<>(existingImageUrls)) {
            MaterialCardView cardView = (MaterialCardView) LayoutInflater.from(this)
                    .inflate(R.layout.s_item_image_preview, binding.imageContainer, false);
            ImageView imageView = cardView.findViewById(R.id.image_preview);
            ImageView removeButton = cardView.findViewById(R.id.btn_remove_image);

            Glide.with(this).load(imageUrl).placeholder(R.drawable.product_placeholder).centerCrop().into(imageView);
            removeButton.setOnClickListener(v -> {
                existingImageUrls.remove(imageUrl);
                updateImagePreviews();
            });
            binding.imageContainer.addView(cardView);
        }

        // 2. Papar imej baru yang dipilih (dari Uri)
        for (Uri uri : new ArrayList<>(newImageUris)) {
            MaterialCardView cardView = (MaterialCardView) LayoutInflater.from(this)
                    .inflate(R.layout.s_item_image_preview, binding.imageContainer, false);
            ImageView imageView = cardView.findViewById(R.id.image_preview);
            ImageView removeButton = cardView.findViewById(R.id.btn_remove_image);

            Glide.with(this).load(uri).placeholder(R.drawable.product_placeholder).centerCrop().into(imageView);
            removeButton.setOnClickListener(v -> {
                newImageUris.remove(uri);
                updateImagePreviews();
            });
            binding.imageContainer.addView(cardView);
        }

        // 3. Tambah semula butang 'Add Image' di hujung
        binding.imageContainer.addView(binding.btnAddImage);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private void launchImageCropper(Uri imageUri) {
        CropImageOptions options = new CropImageOptions();
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.fixAspectRatio = true;
        options.guidelines = com.canhub.cropper.CropImageView.Guidelines.ON;
        options.activityTitle = "Crop Image";
        options.toolbarColor = ContextCompat.getColor(this, R.color.maroon);
        options.toolbarBackButtonColor = ContextCompat.getColor(this, android.R.color.white);
        options.toolbarTintColor = ContextCompat.getColor(this, android.R.color.white);

        CropImageContractOptions contractOptions = new CropImageContractOptions(imageUri, options);
        imageCropperLauncher.launch(contractOptions);
    }

    private void fetchCategoriesFromFirebase() {
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance().getReference("Categories");
        categoriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> categoryNames = new ArrayList<>();
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String categoryName = categorySnapshot.child("categoryName").getValue(String.class);
                    if (categoryName != null) {
                        categoryNames.add(categoryName);
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddProductActivity.this, android.R.layout.simple_dropdown_item_1line, categoryNames);
                binding.actvCategory.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AddProductActivity", "Failed to load categories.", error.toException());
                Toast.makeText(AddProductActivity.this, "Failed to load categories.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateAndSaveProduct() {
        String name = binding.etProductName.getText().toString().trim();
        String priceStr = binding.etProductPrice.getText().toString().trim();
        String stockStr = binding.etStock.getText().toString().trim();
        String description = binding.etProductDescription.getText().toString().trim();
        String category = binding.actvCategory.getText().toString().trim();
        String brand = binding.etBrand.getText().toString().trim();
        String discountPriceStr = binding.etDiscountPrice.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(stockStr) ||
                TextUtils.isEmpty(description) || TextUtils.isEmpty(category) || TextUtils.isEmpty(brand)) {
            Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newImageUris.isEmpty() && existingImageUrls.isEmpty()) {
            Toast.makeText(this, "Please add at least one product image.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Double.parseDouble(priceStr);
            Integer.parseInt(stockStr);
            if (!TextUtils.isEmpty(discountPriceStr)) {
                Double.parseDouble(discountPriceStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for price, stock, and discount price.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        uploadNewImages();
    }

    private void uploadNewImages() {
        progressDialog.setMessage("Uploading new images...");
        uploadedImageUrls.clear();

        if (newImageUris.isEmpty()) {
            // Jika tiada imej baru untuk dimuat naik, terus simpan data
            saveProductDataToFirebase();
            return;
        }

        AtomicInteger uploadCounter = new AtomicInteger(newImageUris.size());
        String unsignedPreset = "beautyhub_unsigned_preset"; // Ganti dengan nama preset anda
        String cloudName = "dvvktb9jr"; // Ganti dengan nama cloud anda

        for (Uri uri : newImageUris) {
            String dispatch = MediaManager.get().upload(uri)
                    .option("cloud_name", cloudName)
                    .unsigned(unsignedPreset)
                    .callback(new UploadCallback() {

                        // ▼▼▼ METHOD YANG HILANG DITAMBAH DI SINI ▼▼▼
                        @Override
                        public void onStart(String requestId) {
                            // Boleh digunakan untuk log atau tindakan lain apabila muat naik bermula
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {

                        }
                        // ▲▲▲ AKHIR METHOD TAMBAHAN ▲▲▲

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = (String) resultData.get("secure_url");
                            if (url != null) {
                                uploadedImageUrls.add(url);
                            }
                            if (uploadCounter.decrementAndGet() == 0) {
                                // Semua imej baru telah dimuat naik
                                saveProductDataToFirebase();
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            progressDialog.dismiss();
                            Log.e("AddProductActivity", "Image upload failed: " + error.getDescription());
                            Toast.makeText(AddProductActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {

                        }
                    })
                    .dispatch();
        }
    }


    private void saveProductDataToFirebase() {
        progressDialog.setMessage(isEditMode ? "Updating product..." : "Saving product...");

        DatabaseReference sellerRef = databaseReference.child("Users").child(currentUser.getUid());
        sellerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sellerName = snapshot.child("username").getValue(String.class);

                    // Gabungkan senarai URL imej lama dan yang baru dimuat naik
                    List<String> finalImageUrls = new ArrayList<>(existingImageUrls);
                    finalImageUrls.addAll(uploadedImageUrls);

                    double price = Double.parseDouble(binding.etProductPrice.getText().toString());
                    String discountPriceStr = binding.etDiscountPrice.getText().toString().trim();
                    double discountPrice = TextUtils.isEmpty(discountPriceStr) ? 0 : Double.parseDouble(discountPriceStr);
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("name", binding.etProductName.getText().toString().trim());
                    productData.put("price", price);
                    productData.put("stock", Integer.parseInt(binding.etStock.getText().toString().trim()));
                    productData.put("description", binding.etProductDescription.getText().toString().trim());
                    productData.put("category", binding.actvCategory.getText().toString().trim());
                    productData.put("sellerId", currentUser.getUid());
                    productData.put("sellerName", sellerName);

// GUNAKAN INI (Sama seperti struktur data sedia ada anda)
                    productData.put("active", true);

// Field tambahan lain ikut data anda
                    productData.put("brand", binding.etBrand.getText().toString().trim());
                    productData.put("discountPrice", discountPrice);
                    productData.put("isPreloaded", false);
                    if (isEditMode) {
                        productData.put("updatedAt", ServerValue.TIMESTAMP);
                        databaseReference.child("Products").child(editingProductId).updateChildren(productData)
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(AddProductActivity.this, "Product updated successfully!", Toast.LENGTH_SHORT).show();

                                    // ▼▼▼ PEMBETULAN DI SINI ▼▼▼
                                    String logDetails = "Updated product: " + binding.etProductName.getText().toString() + " (ID: " + editingProductId + ")";
                                    LogHelper.logCurrentUserAction("Product Updated", logDetails, "Seller");
                                    // ▲▲▲ AKHIR PEMBETULAN ▲▲▲

                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(AddProductActivity.this, "Failed to update product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        String productId = databaseReference.child("Products").push().getKey();
                        if (productId == null) {
                            progressDialog.dismiss();
                            Toast.makeText(AddProductActivity.this, "Could not create product ID.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        productData.put("createdAt", ServerValue.TIMESTAMP);
                        productData.put("productId", productId);
                        databaseReference.child("Products").child(productId).setValue(productData)
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(AddProductActivity.this, "Product added successfully!", Toast.LENGTH_SHORT).show();

                                    // ▼▼▼ PEMBETULAN DI SINI ▼▼▼
                                    String logDetails = "Added new product: " + binding.etProductName.getText().toString() + " (ID: " + productId + ")";
                                    LogHelper.logCurrentUserAction("Product Added", logDetails, "Seller");
                                    // ▲▲▲ AKHIR PEMBETULAN ▲▲▲

                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(AddProductActivity.this, "Failed to add product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(AddProductActivity.this, "Seller details not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(AddProductActivity.this, "Failed to get seller details: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
