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
    private TextInputEditText etSellerName, etSellerEmail;
    private MaterialButton btnSaveProfile;
    private CircleImageView profileImage;
    private TextView tvChangePhoto;
    private ProgressDialog progressDialog;

    // Firebase
    private DatabaseReference userRef;
    private FirebaseUser currentUser;

    // URI untuk gambar yang dipilih
    private Uri imageUri;

    // Launcher untuk meminta kebenaran akses galeri
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(this, "Permission to access gallery is required to change photo.", Toast.LENGTH_SHORT).show();
                }
            });

    // Launcher untuk membuka galeri dan menerima hasil gambar
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    // Paparkan gambar yang baru dipilih
                    profileImage.setImageURI(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s_activity_edit_seller_profile);

        initViews(); // Inisialisasi semua elemen UI

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
        etSellerEmail = findViewById(R.id.et_seller_email); // EditText untuk e-mel
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        profileImage = findViewById(R.id.profile_image_edit);
        tvChangePhoto = findViewById(R.id.tv_change_photo);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Updating Profile");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
    }

    private void loadCurrentData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentName = snapshot.child("name").getValue(String.class);
                    String currentEmail = snapshot.child("email").getValue(String.class);
                    String currentImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    etSellerName.setText(currentName);
                    etSellerEmail.setText(currentEmail);

                    if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                        Glide.with(EditSellerProfileActivity.this)
                                .load(currentImageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditSellerProfileActivity.this, "Failed to load current data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());

        // Listener untuk gambar profil dan teks "Change Photo"
        View.OnClickListener changePhotoListener = v -> checkPermissionAndOpenGallery();
        profileImage.setOnClickListener(changePhotoListener);
        tvChangePhoto.setOnClickListener(changePhotoListener);

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etSellerName.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                etSellerName.setError("Name cannot be empty");
                return;
            }
            progressDialog.show();
            saveProfileChanges(newName);
        });
    }

    private void checkPermissionAndOpenGallery() {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            // Minta kebenaran dari pengguna
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void saveProfileChanges(String newName) {
        if (imageUri != null) {
            // Jika ada gambar baru, muat naik ke Cloudinary terlebih dahulu
            uploadImageToCloudinary(newName);
        } else {
            // Jika hanya nama yang ditukar, terus kemas kini Firebase
            updateFirebaseDatabase(newName, null);
        }
    }

    private void uploadImageToCloudinary(String newName) {
        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                progressDialog.setMessage("Uploading image...");
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                // Boleh digunakan untuk tunjuk progress bar, tapi kita biarkan kosong untuk sekarang
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                // Gambar berjaya dimuat naik, dapatkan URL
                String imageUrl = (String) resultData.get("secure_url");
                // Kemas kini pangkalan data dengan nama dan URL gambar baru
                updateFirebaseDatabase(newName, imageUrl);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                progressDialog.dismiss();
                Toast.makeText(EditSellerProfileActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                // Dibiarkan kosong
            }
        }).dispatch();
    }

    private void updateFirebaseDatabase(String newName, String newImageUrl) {
        progressDialog.setMessage("Saving data...");

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("name", newName);

        // Hanya tambah URL gambar jika ada yang baru
        if (newImageUrl != null) {
            profileUpdates.put("profileImageUrl", newImageUrl);
        }

        userRef.updateChildren(profileUpdates).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(EditSellerProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Kembali ke skrin profil
            } else {
                Toast.makeText(EditSellerProfileActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
