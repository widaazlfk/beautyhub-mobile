package com.example.beautyhub.seller;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
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
import com.canhub.cropper.CropImageView;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.beautyhub.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditSellerProfileActivity extends AppCompatActivity {

    // Deklarasi UI
    private MaterialToolbar toolbar;
    private TextInputEditText etSellerName, etSellerEmail, etSellerPhone, etSellerAddress; // Tambah phone & address
    private MaterialButton btnSaveProfile;
    private CircleImageView profileImage;
    private TextView tvChangePhoto;
    private ProgressDialog progressDialog;

    // Firebase
    private DatabaseReference userRef;
    private FirebaseUser currentUser;

    // Launcher untuk image cropper
    private ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s_activity_edit_seller_profile);

        initViews();
        setupLaunchers();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());

        loadCurrentData();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_edit_profile);
        etSellerName = findViewById(R.id.et_seller_name);
        etSellerEmail = findViewById(R.id.et_seller_email);
        etSellerPhone = findViewById(R.id.et_seller_phone); // Baru
        etSellerAddress = findViewById(R.id.et_seller_address); // Baru
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        profileImage = findViewById(R.id.profile_image_edit);
        tvChangePhoto = findViewById(R.id.tv_change_photo);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Updating Profile");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
    }

    private void setupLaunchers() {
        galleryPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                startImageCrop();
            } else {
                Toast.makeText(this, "Permission to access gallery is required.", Toast.LENGTH_SHORT).show();
            }
        });

        cropImageLauncher = registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful()) {
                Uri croppedImageUri = result.getUriContent();
                if (croppedImageUri != null) {
                    profileImage.setImageURI(croppedImageUri); // Pamerkan gambar yang di-crop
                    // Simpan URI untuk dimuat naik kemudian bila user tekan "Save"
                    uploadImageToCloudinary(croppedImageUri);
                }
            } else {
                Toast.makeText(this, "Image cropping cancelled.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCurrentData() {
        progressDialog.setMessage("Loading data...");
        progressDialog.show();
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentName = snapshot.child("storeName").getValue(String.class);
                    String currentEmail = snapshot.child("email").getValue(String.class);
                    String currentPhone = snapshot.child("phone").getValue(String.class);
                    String currentAddress = snapshot.child("address").getValue(String.class);
                    String currentImageUrl = snapshot.child("profileImage").getValue(String.class);

                    etSellerName.setText(currentName);
                    etSellerEmail.setText(currentEmail);
                    etSellerPhone.setText(currentPhone);
                    etSellerAddress.setText(currentAddress);

                    if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                        Glide.with(EditSellerProfileActivity.this)
                                .load(currentImageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .into(profileImage);
                    }
                }
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(EditSellerProfileActivity.this, "Failed to load current data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());

        View.OnClickListener changePhotoListener = v -> checkPermissionAndStartCrop();
        profileImage.setOnClickListener(changePhotoListener);
        tvChangePhoto.setOnClickListener(changePhotoListener);

        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void checkPermissionAndStartCrop() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startImageCrop();
        } else {
            galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void startImageCrop() {
        CropImageOptions cropOptions = new CropImageOptions();
        cropOptions.guidelines = CropImageView.Guidelines.ON;
        cropOptions.aspectRatioX = 1;
        cropOptions.aspectRatioY = 1;
        cropOptions.fixAspectRatio = true;

        CropImageContractOptions options = new CropImageContractOptions(null, cropOptions);
        cropOptions.imageSourceIncludeGallery = true; // Ini akan buka galeri
        cropOptions.imageSourceIncludeCamera = true; // (Opsyen) benarkan juga kamera
        cropImageLauncher.launch(options);
    }

    private void saveProfileChanges() {
        String newName = etSellerName.getText().toString().trim();
        String newPhone = etSellerPhone.getText().toString().trim();
        String newAddress = etSellerAddress.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            etSellerName.setError("Store name cannot be empty");
            return;
        }

        progressDialog.show();
        updateFirebaseDatabase(newName, newPhone, newAddress);
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        progressDialog.setMessage("Uploading image...");
        progressDialog.show();
        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                userRef.child("profileImage").setValue(imageUrl).addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if(task.isSuccessful()){
                        Toast.makeText(EditSellerProfileActivity.this, "Photo updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EditSellerProfileActivity.this, "Failed to save photo URL.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                progressDialog.dismiss();
                Toast.makeText(EditSellerProfileActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onStart(String requestId) {}
            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void updateFirebaseDatabase(String newName, String newPhone, String newAddress) {
        progressDialog.setMessage("Saving data...");

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("storeName", newName);
        profileUpdates.put("phone", newPhone);
        profileUpdates.put("address", newAddress);
        // Kita tidak update gambar di sini kerana ia diuruskan secara berasingan

        userRef.updateChildren(profileUpdates).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(EditSellerProfileActivity.this, "Profile details updated successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Kembali ke skrin profil
            } else {
                Toast.makeText(EditSellerProfileActivity.this, "Failed to update details.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
