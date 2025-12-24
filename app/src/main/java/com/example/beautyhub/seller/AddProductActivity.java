package com.example.beautyhub.seller;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.cloudinary.Url;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.beautyhub.R;
import com.example.beautyhub.admin.LogHelper;
import com.example.beautyhub.databinding.SActivityAddProductBinding;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Variant;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AddProductActivity extends AppCompatActivity {

    private SActivityAddProductBinding binding;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private DatabaseReference sellerRef;

    private final List<Uri> imageUris = new ArrayList<>();
    private final List<String> uploadedImageUrls = new ArrayList<>();
    private final List<Variant> variantList = new ArrayList<>(); // Senarai untuk simpan varian
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<CropImageContractOptions> imageCropperLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SActivityAddProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            MediaManager.init(this);
        } catch (Exception e) {
            Log.e("AddProductActivity", "Cloudinary init failed", e);
        }

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        if (currentUser == null) {
            Toast.makeText(this, "Authentication Error. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        sellerRef = databaseReference.child("Users").child(currentUser.getUid());

        initImageLaunchers();
        setupToolbar();
        setupListeners();
        fetchCategoriesFromFirebase();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Adding Product");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
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
                        if (!imageUris.contains(croppedImageUri)) {
                            imageUris.add(croppedImageUri);
                            displaySelectedImages();
                        } else {
                            Toast.makeText(this, "Image already selected.", Toast.LENGTH_SHORT).show();
                        }
                    } else if (!result.isSuccessful()) {
                        Exception error = result.getError();
                        if (error != null) {
                            Log.e("AddProductActivity", "Image Cropping Error", error);
                        }
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
        binding.btnAddVariant.setOnClickListener(v -> showAddVariantDialog());
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

    private void showAddVariantDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_variant, null);
        builder.setView(dialogView);

        final EditText etVariantName = dialogView.findViewById(R.id.et_variant_name);
        final EditText etVariantPrice = dialogView.findViewById(R.id.et_variant_price);
        final EditText etVariantStock = dialogView.findViewById(R.id.et_variant_stock);

        builder.setTitle("Add Variant");
        builder.setPositiveButton("Add", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String name = etVariantName.getText().toString().trim();
                String priceStr = etVariantPrice.getText().toString().trim();
                String stockStr = etVariantStock.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    etVariantName.setError("Variant name is required");
                    return;
                }
                if (TextUtils.isEmpty(stockStr)) {
                    etVariantStock.setError("Stock is required");
                    return;
                }

                double priceModifier = 0.0;
                if (!TextUtils.isEmpty(priceStr)) {
                    try {
                        priceModifier = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        etVariantPrice.setError("Invalid price format");
                        return;
                    }
                }

                int stock = Integer.parseInt(stockStr);

                Variant newVariant = new Variant(name, priceModifier, stock);
                variantList.add(newVariant);
                displayVariants();

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void displayVariants() {
        binding.variantsContainer.removeAllViews();
        for (Variant variant : variantList) {
            View variantView = LayoutInflater.from(this).inflate(R.layout.s_item_variant, binding.variantsContainer, false);

            TextView variantName = variantView.findViewById(R.id.variant_name);
            TextView variantPrice = variantView.findViewById(R.id.variant_price);
            TextView variantStock = variantView.findViewById(R.id.variant_stock);
            ImageView removeButton = variantView.findViewById(R.id.btn_remove_variant);

            variantName.setText(variant.getName());
            variantPrice.setText(String.format("+ RM%.2f", variant.getPriceModifier()));
            variantStock.setText("Stock: " + variant.getStock());

            removeButton.setOnClickListener(v -> {
                variantList.remove(variant);
                displayVariants();
            });

            binding.variantsContainer.addView(variantView);
        }
    }

    private void displaySelectedImages() {
        binding.imageContainer.removeAllViews();
        for (Uri uri : new ArrayList<>(imageUris)) {
            MaterialCardView cardView = (MaterialCardView) LayoutInflater.from(this)
                    .inflate(R.layout.s_item_image_preview, binding.imageContainer, false);
            ImageView imageView = cardView.findViewById(R.id.image_preview);
            ImageView removeButton = cardView.findViewById(R.id.btn_remove_image);
            Glide.with(this).load(uri).into(imageView);
            removeButton.setOnClickListener(v -> {
                imageUris.remove(uri);
                displaySelectedImages();
            });
            binding.imageContainer.addView(cardView);
        }
        binding.imageContainer.addView(binding.btnAddImage);
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

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(stockStr) ||
                TextUtils.isEmpty(description) || TextUtils.isEmpty(category) || TextUtils.isEmpty(brand)) {
            Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUris.isEmpty()) {
            Toast.makeText(this, "Please add at least one product image.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Double.parseDouble(priceStr);
            Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for price and stock.", Toast.LENGTH_SHORT).show();
            return;
        }


        progressDialog.show();
        uploadImagesAndSaveProduct();
    }

    private void uploadImagesAndSaveProduct() {
        progressDialog.setMessage("Uploading images...");
        uploadedImageUrls.clear();

        if (imageUris.isEmpty()) {
            fetchSellerDetailsAndSaveProduct();
            return;
        }

        AtomicInteger uploadCounter = new AtomicInteger(0);
        String unsignedPreset = "beautyhub_unsigned_preset";

        // GANTIKAN "YOUR_CLOUD_NAME" DENGAN CLOUD NAME ANDA YANG SEBENAR
        String cloudName = "dvvktb9jr";

        for (Uri uri : imageUris) {
            // Menambah konfigurasi cloud_name secara eksplisit
            MediaManager.get().upload(uri)
                    .option("cloud_name", cloudName)
                    .unsigned(unsignedPreset)
                    .callback(new UploadCallback() {
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = (String) resultData.get("secure_url");
                            if (url != null) {
                                uploadedImageUrls.add(url);
                            }
                            if (uploadCounter.incrementAndGet() == imageUris.size()) {
                                fetchSellerDetailsAndSaveProduct();
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            progressDialog.dismiss();
                            Log.e("AddProductActivity", "Image upload failed: " + error.getDescription());
                            Toast.makeText(AddProductActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        }

                        @Override public void onStart(String requestId) {}
                        @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();
        }
    }


    private void fetchSellerDetailsAndSaveProduct() {
        progressDialog.setMessage("Saving product details...");
        sellerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sellerName = snapshot.child("username").getValue(String.class);
                    String sellerProfileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    saveProductToDatabase(sellerName, sellerProfileImageUrl);
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(AddProductActivity.this, "Could not find seller details. Please update your profile.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Log.e("AddProductActivity", "Failed to get seller details.", error.toException());
                Toast.makeText(AddProductActivity.this, "Failed to get seller details: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProductToDatabase(String sellerName, String sellerProfileImageUrl) {
        DatabaseReference productsRef = databaseReference.child("Products");
        String productId = productsRef.push().getKey();

        if (productId == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to create a unique product ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ▼▼▼ 1. Ambil nilai harga diskaun dari EditText ▼▼▼
        String discountPriceStr = binding.etDiscountPrice.getText().toString().trim();
        double discountPrice = 0.0;
        if (!TextUtils.isEmpty(discountPriceStr)) {
            try {
                discountPrice = Double.parseDouble(discountPriceStr);
            } catch (NumberFormatException e) {
                progressDialog.dismiss();
                Toast.makeText(this, "Invalid format for discount price.", Toast.LENGTH_SHORT).show();
                return; // Hentikan proses jika format salah
            }
        }

        Product newProduct = new Product();
        newProduct.setProductId(productId);
        newProduct.setName(binding.etProductName.getText().toString().trim());
        newProduct.setPrice(Double.parseDouble(binding.etProductPrice.getText().toString().trim()));
        newProduct.setDescription(binding.etProductDescription.getText().toString().trim());
        newProduct.setStock(Integer.parseInt(binding.etStock.getText().toString().trim()));
        newProduct.setCategory(binding.actvCategory.getText().toString().trim());
        newProduct.setBrand(binding.etBrand.getText().toString().trim());
        newProduct.setIngredients(binding.etIngredients.getText().toString().trim());
        newProduct.setSkinType(binding.etSkinType.getText().toString().trim());
        newProduct.setActive(true);
        newProduct.setCreationTimestamp(System.currentTimeMillis());
        newProduct.setImageUrls(uploadedImageUrls);
        newProduct.setVariants((Map<String, Variant>) variantList);
        newProduct.setSellerId(currentUser.getUid());
        newProduct.setSellerName(sellerName);
        newProduct.setSellerProfileImageUrl(sellerProfileImageUrl);
        newProduct.setDiscountPrice(discountPrice);

        // Set nilai lalai untuk medan baharu (pilihan, tapi amalan baik)
        newProduct.setSoldCount(0);
        newProduct.setAverageRating(0.0f);

        productsRef.child(productId).setValue(newProduct)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        LogHelper.logCurrentUserAction("ADD_PRODUCT", "Product added: " + newProduct.getName(), "Seller");
                        Toast.makeText(AddProductActivity.this, "Product added successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Log.e("AddProductActivity", "Failed to save product.", task.getException());
                        Toast.makeText(AddProductActivity.this, "Failed to save product: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
