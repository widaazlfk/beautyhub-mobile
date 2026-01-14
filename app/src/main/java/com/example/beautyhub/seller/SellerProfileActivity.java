package com.example.beautyhub.seller;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.beautyhub.R;
import com.example.beautyhub.adapters.ProductAdapter;
import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SellerProfileActivity extends AppCompatActivity {

    private CircleImageView ivSellerProfile;
    private TextView tvChangeImage, tvRatingValue, tvStoreDescValue, tvPhoneValue, tvAddressValue, tvNoProducts;
    private EditText etSellerNameHeader;
    private ImageButton ibEditNameHeader;
    private RatingBar ratingBarSeller;
    private RelativeLayout itemStoreDesc, itemPhone, itemAddress;

    private FirebaseAuth mAuth;
    private DatabaseReference sellerRef;
    private DatabaseReference productsRef;
    private ProgressDialog progressDialog;

    private RecyclerView rvSellerProducts;
    private ProductAdapter productAdapter;
    private List<Product> productList;
    private String targetSellerId;

    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri croppedImageUri = result.getUriContent();
                    if (croppedImageUri != null) {
                        uploadProfileImageToCloudinary(croppedImageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);

        mAuth = FirebaseAuth.getInstance();

        // 1. Get Seller ID from Intent (if opened by Buyer)
        targetSellerId = getIntent().getStringExtra("SELLER_ID");

        // 2. If no ID in Intent, default to the logged-in user's UID
        if (targetSellerId == null && mAuth.getCurrentUser() != null) {
            targetSellerId = mAuth.getCurrentUser().getUid();
        }

        if (targetSellerId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize Firebase References using targetSellerId
        sellerRef = FirebaseDatabase.getInstance().getReference("Users").child(targetSellerId);
        productsRef = FirebaseDatabase.getInstance().getReference("Products");

        initViews();
        setupToolbar();
        setupClickListeners();
        loadSellerData();
    }

    private void initViews() {
        ivSellerProfile = findViewById(R.id.iv_seller_profile);
        tvChangeImage = findViewById(R.id.tv_change_image);
        etSellerNameHeader = findViewById(R.id.et_seller_name_header);
        ibEditNameHeader = findViewById(R.id.ib_edit_name_header);
        ratingBarSeller = findViewById(R.id.rating_bar_seller);
        tvRatingValue = findViewById(R.id.tv_rating_value);

        itemStoreDesc = findViewById(R.id.item_store_desc);
        tvStoreDescValue = findViewById(R.id.tv_store_desc_value);
        itemPhone = findViewById(R.id.item_phone);
        tvPhoneValue = findViewById(R.id.tv_phone_value);
        itemAddress = findViewById(R.id.item_address);
        tvAddressValue = findViewById(R.id.tv_address_value);

        tvNoProducts = findViewById(R.id.tv_no_products_seller);
        rvSellerProducts = findViewById(R.id.rv_seller_products);
        rvSellerProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvSellerProducts.setNestedScrollingEnabled(false);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        productList = new ArrayList<>();
        setupAdapter();
    }

    private void setupAdapter() {
        // Only show edit/delete icons if the logged-in user owns this profile
        boolean isMyProfile = targetSellerId.equals(mAuth.getUid());

        ProductAdapter.OnProductClickListener productClickListener = new ProductAdapter.OnProductClickListener() {
            @Override public void onProductClick(Product product) { /* Open Product Detail */ }
            @Override public void onEditClick(Product product) { /* Open Edit Activity */ }
            @Override public void onDeleteClick(Product product) { confirmDeleteProduct(product); }
            @Override public void onAddToCartClick(Product product) {}
            @Override public void onBuyNowClick(Product product) {}
            @Override public void onSellerClick(String sellerId) {}
            @Override public void onWishlistClick(Product product) {}
        };

        productAdapter = new ProductAdapter(this, productList, isMyProfile, productClickListener);
        rvSellerProducts.setAdapter(productAdapter);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_seller_profile);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupClickListeners() {
        boolean isMyProfile = targetSellerId.equals(mAuth.getUid());

        if (isMyProfile) {
            // Owner can edit profile
            tvChangeImage.setOnClickListener(v -> startImageCrop());
            ivSellerProfile.setOnClickListener(v -> startImageCrop());
            ibEditNameHeader.setOnClickListener(v -> toggleNameEditMode());

            itemStoreDesc.setOnClickListener(v -> showEditDialog("Store Description", tvStoreDescValue.getText().toString(), "shopDescription", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE));
            itemPhone.setOnClickListener(v -> showEditDialog("Phone Number", tvPhoneValue.getText().toString(), "phone", InputType.TYPE_CLASS_PHONE));
            itemAddress.setOnClickListener(v -> showEditDialog("Store Address", tvAddressValue.getText().toString(), "address", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE));
        } else {
            // Visitors (Buyers) cannot edit
            tvChangeImage.setVisibility(View.GONE);
            ibEditNameHeader.setVisibility(View.GONE);
            etSellerNameHeader.setEnabled(false);
            etSellerNameHeader.setFocusable(false);
        }
    }

    private void loadSellerData() {
        sellerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User seller = snapshot.getValue(User.class);
                if (seller != null) {
                    updateUI(seller);
                    loadSellerProducts(targetSellerId);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUI(User seller) {
        etSellerNameHeader.setText(seller.getUsername());
        tvStoreDescValue.setText(TextUtils.isEmpty(seller.getShopDescription()) ? "No description provided" : seller.getShopDescription());
        tvPhoneValue.setText(TextUtils.isEmpty(seller.getPhone()) ? "No phone number" : seller.getPhone());
        tvAddressValue.setText(TextUtils.isEmpty(seller.getAddress()) ? "No address provided" : seller.getAddress());

        if (!isDestroyed() && seller.getProfileImage() != null && !seller.getProfileImage().isEmpty()) {
            Glide.with(this)
                    .load(seller.getProfileImage())
                    .placeholder(R.drawable.ic_profile)
                    .into(ivSellerProfile);
        }
    }

    private void loadSellerProducts(String sellerId) {
        productsRef.orderByChild("sellerId").equalTo(sellerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Product product = ds.getValue(Product.class);
                    if (product != null) productList.add(product);
                }

                if (productList.isEmpty()) {
                    tvNoProducts.setVisibility(View.VISIBLE);
                    rvSellerProducts.setVisibility(View.GONE);
                } else {
                    tvNoProducts.setVisibility(View.GONE);
                    rvSellerProducts.setVisibility(View.VISIBLE);
                }
                productAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startImageCrop() {
        CropImageOptions options = new CropImageOptions();
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.fixAspectRatio = true;
        cropImageLauncher.launch(new CropImageContractOptions(null, options));
    }

    private void uploadProfileImageToCloudinary(Uri uri) {
        progressDialog.setMessage("Updating profile image...");
        progressDialog.show();

        MediaManager.get().upload(uri)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        sellerRef.child("profileImage").setValue(imageUrl)
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(SellerProfileActivity.this, "Profile image updated", Toast.LENGTH_SHORT).show();
                                });
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        progressDialog.dismiss();
                        Toast.makeText(SellerProfileActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void toggleNameEditMode() {
        if (etSellerNameHeader.isEnabled()) {
            String newName = etSellerNameHeader.getText().toString().trim();
            if (!TextUtils.isEmpty(newName)) {
                sellerRef.child("username").setValue(newName);
            }
            etSellerNameHeader.setEnabled(false);
            ibEditNameHeader.setImageResource(R.drawable.ic_edit);
        } else {
            etSellerNameHeader.setEnabled(true);
            etSellerNameHeader.requestFocus();
            ibEditNameHeader.setImageResource(R.drawable.ic_check);
        }
    }

    private void showEditDialog(String title, String currentVal, String dbKey, int inputType) {
        final EditText input = new EditText(this);
        input.setInputType(inputType);
        input.setText(currentVal);
        input.setPadding(50, 40, 50, 40);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Update " + title)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String val = input.getText().toString().trim();
                    sellerRef.child(dbKey).setValue(val);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteProduct(Product product) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    productsRef.child(product.getProductId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
