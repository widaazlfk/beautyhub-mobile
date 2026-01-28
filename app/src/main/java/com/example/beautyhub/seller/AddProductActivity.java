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

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.beautyhub.R;
import com.example.beautyhub.databinding.SActivityAddProductBinding;
import com.example.beautyhub.models.Product;
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

    private final List<Uri> newImageUris = new ArrayList<>();
    private final List<String> existingImageUrls = new ArrayList<>();
    private final List<String> uploadedImageUrls = new ArrayList<>();

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<CropImageContractOptions> imageCropperLauncher;

    private boolean isEditMode = false;
    private String editingProductId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SActivityAddProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Init Cloudinary
        try {
            if (MediaManager.get() == null) {
                MediaManager.init(this);
            }
        } catch (Exception e) {
            Log.e("AddProductActivity", "Cloudinary init failed", e);
        }

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        if (currentUser == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        if (getIntent().hasExtra("EDIT_PRODUCT")) {
            isEditMode = true;
            Product product = getIntent().getParcelableExtra("EDIT_PRODUCT");
            if (product != null) {
                editingProductId = product.getProductId();
                setupForEditMode();
                populateFieldsForEdit(product);
            }
        } else if (getIntent().hasExtra("PRODUCT_ID")) {
            isEditMode = true;
            editingProductId = getIntent().getStringExtra("PRODUCT_ID");
            setupForEditMode();
        } else {
            setupForAddMode();
        }

        initImageLaunchers();
        setupToolbar();
        setupListeners();
        fetchCategoriesFromFirebase();
    }

    private void setupForEditMode() {
        progressDialog.setTitle("Updating Product");
        binding.toolbarAddProduct.setTitle("Edit Product");
        binding.btnSaveProduct.setText("Update Product");
        if (editingProductId != null) {
            loadProductData(editingProductId);
        }
    }

    private void setupForAddMode() {
        progressDialog.setTitle("Adding Product");
        binding.toolbarAddProduct.setTitle("Add New Product");
    }

    private void loadProductData(String productId) {
        progressDialog.setMessage("Loading details...");
        progressDialog.show();

        databaseReference.child("Products").child(productId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Product product = snapshot.getValue(Product.class);
                        if (product != null) {
                            populateFieldsForEdit(product);
                        }
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Toast.makeText(AddProductActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateFieldsForEdit(Product product) {
        binding.etProductName.setText(product.getName());
        binding.etProductPrice.setText(String.valueOf(product.getPrice()));
        binding.etStock.setText(String.valueOf(product.getStock()));
        binding.etProductDescription.setText(product.getDescription());
        binding.actvCategory.setText(product.getCategory(), false);
        binding.etBrand.setText(product.getBrand());
        binding.etIngredients.setText(product.getIngredients());
        binding.etSkinType.setText(product.getSkinType());

        if (product.getDiscountPrice() > 0) {
            binding.etDiscountPrice.setText(String.valueOf(product.getDiscountPrice()));
        }

        if (product.getImageUrls() != null) {
            existingImageUrls.clear();
            existingImageUrls.addAll(product.getImageUrls());
            updateImagePreviews();
        }
    }

    private void validateAndSaveProduct() {
        String name = binding.etProductName.getText().toString().trim();
        String priceStr = binding.etProductPrice.getText().toString().trim();
        String stockStr = binding.etStock.getText().toString().trim();
        String category = binding.actvCategory.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr) ||
                TextUtils.isEmpty(stockStr) || TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newImageUris.isEmpty() && existingImageUrls.isEmpty()) {
            Toast.makeText(this, "Please add at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Saving product...");
        progressDialog.show();

        if (!newImageUris.isEmpty()) {
            uploadImagesToCloudinary();
        } else {
            saveProductToFirebase(existingImageUrls);
        }
    }

    private void uploadImagesToCloudinary() {
        uploadedImageUrls.clear();
        AtomicInteger uploadCount = new AtomicInteger(0);

        for (Uri uri : newImageUris) {
            MediaManager.get().upload(uri)
                    .callback(new UploadCallback() {
                        @Override public void onStart(String requestId) {}
                        @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            uploadedImageUrls.add((String) resultData.get("secure_url"));
                            if (uploadCount.incrementAndGet() == newImageUris.size()) {
                                List<String> allImages = new ArrayList<>(existingImageUrls);
                                allImages.addAll(uploadedImageUrls);
                                saveProductToFirebase(allImages);
                            }
                        }
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            progressDialog.dismiss();
                            Toast.makeText(AddProductActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        }
    }

    private void saveProductToFirebase(List<String> imageUrls) {
        progressDialog.setMessage("Generating ID and saving...");
        String currentUserId = currentUser.getUid();

        // Jika sedang EDIT, kita guna ID asal (contoh: prod_001)
        if (isEditMode) {
            // Nota: Untuk edit, kita biasanya dah ada sName dan sImage dari loadProductData
            // Saya letak default "BeautyHub Seller" jika data tersebut null
            proceedToSave(editingProductId, currentUserId, "BeautyHub Seller", null, imageUrls);
        } else {
            // Jika ADD NEW, cari ID terakhir dalam database (untuk buat prod_077 dan ke atas)
            databaseReference.child("Products").orderByKey().limitToLast(1)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot productsSnapshot) {
                            String newId = "prod_001"; // Default jika database kosong

                            if (productsSnapshot.exists()) {
                                for (DataSnapshot child : productsSnapshot.getChildren()) {
                                    String lastId = child.getKey();
                                    try {
                                        if (lastId != null && lastId.startsWith("prod_")) {
                                            // Ambil nombor terakhir, contoh 076 -> jadi 76
                                            int lastNumber = Integer.parseInt(lastId.replace("prod_", ""));
                                            // Tambah 1 -> jadi 77, format balik jadi prod_077
                                            newId = String.format("prod_%03d", lastNumber + 1);
                                        }
                                    } catch (Exception e) {
                                        newId = "prod_" + System.currentTimeMillis();
                                    }
                                }
                            }
                            // Kita panggil proceedToSave.
                            // Nama Seller akan diuruskan di dalam proceedToSave
                            proceedToSave(newId, currentUserId, null, null, imageUrls);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            progressDialog.dismiss();
                            Toast.makeText(AddProductActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    // Method Helper untuk simpan data setelah ID ditentukan
    private void proceedToSave(String productId, String currentUserId, String sName, String sImage, List<String> imageUrls) {
        // Tarik data seller yang terkini dari database dahulu
        databaseReference.child("Users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String dbSellerName = snapshot.child("username").getValue(String.class);
                String dbSellerImage = snapshot.child("profileImage").getValue(String.class);

                Product product = new Product();
                product.setProductId(productId);
                product.setSellerId(currentUserId);

                if (dbSellerName != null) product.setSellerName(dbSellerName);
                if (dbSellerImage != null) product.setSellerProfileImageUrl(dbSellerImage);


                // Set data dari form
                product.setName(binding.etProductName.getText().toString().trim());
                product.setDescription(binding.etProductDescription.getText().toString().trim());
                product.setBrand(binding.etBrand.getText().toString().trim());
                product.setIngredients(binding.etIngredients.getText().toString().trim());
                product.setSkinType(binding.etSkinType.getText().toString().trim());
                product.setCategory(binding.actvCategory.getText().toString().trim());
                product.setImageUrls(imageUrls);
                product.setActive(true);

                try {
                    product.setPrice(Double.parseDouble(binding.etProductPrice.getText().toString()));
                    String dStr = binding.etDiscountPrice.getText().toString().trim();
                    product.setDiscountPrice(dStr.isEmpty() ? 0 : Double.parseDouble(dStr));
                    product.setStock(Integer.parseInt(binding.etStock.getText().toString()));
                } catch (Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(AddProductActivity.this, "Wrong format of price or stock", Toast.LENGTH_SHORT).show();
                    return;
                }

                // SIMPAN KE FIREBASE
                databaseReference.child("Products").child(productId).setValue(product)
                        .addOnCompleteListener(task -> {
                            progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                Toast.makeText(AddProductActivity.this, "Product saved successfully! ID: " + productId, Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AddProductActivity.this, "Failed to save: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(AddProductActivity.this, "Gagal akses data penjual", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void initImageLaunchers() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                launchImageCropper(result.getData().getData());
            }
        });

        imageCropperLauncher = registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful() && result.getUriContent() != null) {
                newImageUris.add(result.getUriContent());
                updateImagePreviews();
            }
        });
    }

    private void updateImagePreviews() {
        binding.imageContainer.removeAllViews();
        for (String url : existingImageUrls) { addImageToPreview(url, true); }
        for (Uri uri : newImageUris) { addImageToPreview(uri, false); }
        binding.imageContainer.addView(binding.btnAddImage);
    }

    private void addImageToPreview(Object source, boolean isUrl) {
        MaterialCardView cardView = (MaterialCardView) LayoutInflater.from(this)
                .inflate(R.layout.s_item_image_preview, binding.imageContainer, false);
        ImageView imageView = cardView.findViewById(R.id.image_preview);
        ImageView btnRemove = cardView.findViewById(R.id.btn_remove_image);

        Glide.with(this).load(source).centerCrop().into(imageView);
        btnRemove.setOnClickListener(v -> {
            if (isUrl) existingImageUrls.remove((String)source);
            else newImageUris.remove((Uri)source);
            updateImagePreviews();
        });
        binding.imageContainer.addView(cardView);
    }

    private void setupToolbar() {
        binding.toolbarAddProduct.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });
        binding.actvCategory.setOnClickListener(v -> binding.actvCategory.showDropDown());
        binding.btnSaveProduct.setOnClickListener(v -> validateAndSaveProduct());
        binding.btnCancel.setOnClickListener(v -> finish());
    }

    private void launchImageCropper(Uri uri) {
        CropImageOptions cropImageOptions = new CropImageOptions();
        cropImageOptions.aspectRatioX = 1;
        cropImageOptions.aspectRatioY = 1;
        cropImageOptions.fixAspectRatio = true;
        imageCropperLauncher.launch(new CropImageContractOptions(uri, cropImageOptions));
    }

    private void fetchCategoriesFromFirebase() {
        databaseReference.child("Categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> categories = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String categoryName = ds.child("categoryName").getValue(String.class);
                    if (categoryName == null) categoryName = ds.getValue(String.class);
                    if (categoryName != null) categories.add(categoryName);
                }
                if (!categories.isEmpty()) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddProductActivity.this, android.R.layout.simple_dropdown_item_1line, categories);
                    binding.actvCategory.setAdapter(adapter);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}