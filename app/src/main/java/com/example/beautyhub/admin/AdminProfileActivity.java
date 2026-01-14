package com.example.beautyhub.admin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.beautyhub.R;
import com.example.beautyhub.auth.LoginActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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

public class AdminProfileActivity extends AppCompatActivity {

    private static final String TAG = "AdminProfileActivity";

    // Views
    private CircleImageView ivAdminProfileImage;
    private FloatingActionButton fabEditImage;
    private TextView tvAdminNameHeader, tvAdminEmailHeader;
    private TextView tvFullNameValue, tvEmailValue, tvPhoneValue;
    private RelativeLayout itemFullName, itemPhone;
    private LinearLayout btnChangePassword, btnLogout;
    private ProgressDialog progressDialog;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference adminRef;
    private ValueEventListener adminListener;

    // Launcher untuk memilih gambar dari galeri
    private ActivityResultLauncher<String> imagePickerLauncher;
    // Launcher untuk hasil dari activity cropper
    private ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_admin_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Sila log masuk semula.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adminRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());

        // Initialize views
        initViews();
        setupToolbar();
        setupImagePicker();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadAdminData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adminRef != null && adminListener != null) {
            adminRef.removeEventListener(adminListener);
        }
    }

    private void initViews() {
        ivAdminProfileImage = findViewById(R.id.iv_admin_profile_image);
        fabEditImage = findViewById(R.id.fab_edit_image);
        tvAdminNameHeader = findViewById(R.id.tv_admin_name);
        tvAdminEmailHeader = findViewById(R.id.tv_admin_email_header);
        itemFullName = findViewById(R.id.item_full_name);
        itemPhone = findViewById(R.id.item_phone);
        tvFullNameValue = findViewById(R.id.tv_full_name_value);
        tvEmailValue = findViewById(R.id.tv_email_value);
        tvPhoneValue = findViewById(R.id.tv_phone_value);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnLogout = findViewById(R.id.btn_logout);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Mengemas kini Profil");
        progressDialog.setMessage("Sila tunggu...");
        progressDialog.setCancelable(false);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_admin_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                launchCropper(uri);
            }
        });

        cropImageLauncher = registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful()) {
                Uri croppedUri = result.getUriContent();
                if (croppedUri != null) {
                    uploadImageToCloudinary(croppedUri);
                }
            } else {
                Exception error = result.getError();
                Log.e(TAG, "Image cropping failed: ", error);
                Toast.makeText(this, "Image cropping failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void launchCropper(Uri uri) {
        CropImageOptions cropOptions = new CropImageOptions();
        cropOptions.guidelines = CropImageView.Guidelines.ON;
        cropOptions.aspectRatioX = 1;
        cropOptions.aspectRatioY = 1;
        cropOptions.fixAspectRatio = true;

        CropImageContractOptions options = new CropImageContractOptions(uri, cropOptions);
        cropImageLauncher.launch(options);
    }

    private void setupListeners() {
        // Profile image edit
        fabEditImage.setOnClickListener(v -> openGallery());

        // Edit name
        itemFullName.setOnClickListener(v -> showEditDialog("Full Name",
                tvFullNameValue.getText().toString(), "username"));

        // Edit phone
        itemPhone.setOnClickListener(v -> showEditDialog("Phone Number",
                tvPhoneValue.getText().toString(), "phone"));

        // Change Password - FIXED
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Logout
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void loadAdminData() {
        adminListener = adminRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImage").getValue(String.class);

                    // Update header
                    tvAdminNameHeader.setText(name != null ? name : "Admin User");
                    tvAdminEmailHeader.setText(email != null ? email : "No Email");

                    // Update profile info section
                    tvFullNameValue.setText(name != null ? name : "Admin User");
                    tvEmailValue.setText(email != null ? email : "No Email");
                    tvPhoneValue.setText(phone != null ? phone : "Not set");

                    // Load profile image
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(AdminProfileActivity.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_admin_profile)
                                .error(R.drawable.ic_admin_profile)
                                .into(ivAdminProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Gagal memuatkan data admin: " + error.getMessage());
            }
        });
    }

    private void openGallery() {
        imagePickerLauncher.launch("image/*");
    }

    private void showEditDialog(String title, String currentValue, final String fieldKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit " + title);

        final EditText input = new EditText(this);
        input.setText(currentValue);
        input.setSelection(currentValue.length());

        if (title.contains("Phone")) {
            input.setInputType(InputType.TYPE_CLASS_PHONE);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        }

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        int margin = getResources().getDimensionPixelSize(R.dimen.dialog_horizontal_margin);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty() && !newValue.equals(currentValue)) {
                updateFieldInFirebase(fieldKey, newValue);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateFieldInFirebase(String key, String value) {
        progressDialog.show();

        adminRef.child(key).setValue(value)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(AdminProfileActivity.this,
                            titleCase(key) + " berjaya dikemas kini.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(AdminProfileActivity.this,
                            "Gagal mengemas kini: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // =============== CHANGE PASSWORD IMPLEMENTATION ===============
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        // Inflate dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        // Initialize input fields
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.et_current_password);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);

        // Setup password toggles
        TextInputLayout currentLayout = dialogView.findViewById(R.id.layout_current_password);
        TextInputLayout newLayout = dialogView.findViewById(R.id.layout_new_password);
        TextInputLayout confirmLayout = dialogView.findViewById(R.id.layout_confirm_password);

        setupPasswordToggle(currentLayout);
        setupPasswordToggle(newLayout);
        setupPasswordToggle(confirmLayout);

        builder.setPositiveButton("Change", (dialog, which) -> {
            String currentPass = etCurrentPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (validatePasswordFields(currentPass, newPass, confirmPass)) {
                changePassword(currentPass, newPass);
            }
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupPasswordToggle(TextInputLayout textInputLayout) {
        textInputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
    }

    private boolean validatePasswordFields(String current, String newPass, String confirm) {
        // Check empty fields
        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Semua ruangan perlu diisi", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check password match
        if (!newPass.equals(confirm)) {
            Toast.makeText(this, "Kata laluan baru tidak sepadan", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check minimum length
        if (newPass.length() < 6) {
            Toast.makeText(this, "Kata laluan mesti sekurang-kurangnya 6 aksara", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if new password is different
        if (current.equals(newPass)) {
            Toast.makeText(this, "Kata laluan baru mesti berbeza dengan kata laluan semasa",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void changePassword(String currentPassword, String newPassword) {
        progressDialog.setTitle("Menukar Kata Laluan");
        progressDialog.setMessage("Sila tunggu...");
        progressDialog.show();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null && user.getEmail() != null) {
            // Re-authenticate user first
            AuthCredential credential = EmailAuthProvider
                    .getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential)
                    .addOnCompleteListener(reauthTask -> {
                        if (reauthTask.isSuccessful()) {
                            // Update password
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(updateTask -> {
                                        progressDialog.dismiss();

                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(this, "Kata laluan berjaya ditukar!",
                                                    Toast.LENGTH_SHORT).show();

                                            // Log the activity
                                            logPasswordChangeActivity();
                                        } else {
                                            String errorMessage = "Gagal menukar kata laluan";
                                            if (updateTask.getException() != null) {
                                                errorMessage += ": " + updateTask.getException().getMessage();
                                            }
                                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Kata laluan semasa tidak betul",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Autentikasi gagal: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "Pengguna tidak dijumpai. Sila log masuk semula.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void logPasswordChangeActivity() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference logsRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(user.getUid())
                    .child("activityLogs");

            String logId = "log_" + System.currentTimeMillis();
            Map<String, Object> logData = new HashMap<>();
            logData.put("action", "PASSWORD_CHANGE");
            logData.put("timestamp", System.currentTimeMillis());
            logData.put("device", "Android");

            logsRef.child(logId).setValue(logData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Password change activity logged"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to log password change", e));
        }
    }
    // =============== END CHANGE PASSWORD IMPLEMENTATION ===============

    private void uploadImageToCloudinary(Uri imageUri) {
        if (imageUri == null) return;

        progressDialog.setMessage("Muat naik gambar...");
        progressDialog.show();

        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) { }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String downloadUrl = (String) resultData.get("secure_url");
                if (downloadUrl != null) {
                    saveImageUrlToFirebase(downloadUrl);
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(AdminProfileActivity.this,
                            "Gagal mendapatkan URL gambar.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                progressDialog.dismiss();
                Log.e(TAG, "Cloudinary upload error: " + error.getDescription());
                Toast.makeText(AdminProfileActivity.this,
                        "Muat naik gambar gagal: " + error.getDescription(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveImageUrlToFirebase(String downloadUrl) {
        progressDialog.setMessage("Menyimpan...");
        updateFieldInFirebase("profileImage", downloadUrl);
        progressDialog.dismiss();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Keluar")
                .setMessage("Anda pasti ingin log keluar?")
                .setPositiveButton("Log Keluar", (dialog, which) -> logoutAdmin())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void logoutAdmin() {
        mAuth.signOut();
        Toast.makeText(this, "Berjaya log keluar", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String titleCase(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}