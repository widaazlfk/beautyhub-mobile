package com.example.beautyhub.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.beautyhub.R;
import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.models.User;
import com.google.android.material.appbar.MaterialToolbar;
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

public class AdminProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;

    private TextView tvAdminName, tvAdminEmail;
    // ▼▼▼ UBAH SUAI DI SINI: etPhone dibuang kerana tidak wujud dalam model User ▼▼▼
    private TextInputEditText etFullName, etEmail;
    private com.google.android.material.button.MaterialButton btnUpdateProfile, btnLogout;
    private LinearLayout btnChangePassword;
    private ImageView ivProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_admin_profile);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_admin_profile);
        toolbar.setNavigationOnClickListener(v -> finish());

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        }

        initViews();
        loadAdminInfo();
        setupListeners();
    }

    private void initViews() {
        tvAdminName = findViewById(R.id.tv_admin_name);
        tvAdminEmail = findViewById(R.id.tv_admin_email);
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        // ▼▼▼ UBAH SUAI DI SINI: Rujukan kepada et_phone dibuang ▼▼▼
        // etPhone = findViewById(R.id.et_phone);
        btnUpdateProfile = findViewById(R.id.btn_update_profile);
        btnLogout = findViewById(R.id.btn_logout);
        btnChangePassword = findViewById(R.id.btn_change_password);
        ivProfile = findViewById(R.id.iv_profile);
    }

    private void loadAdminInfo() {
        if (userRef != null) {
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User adminUsers = snapshot.getValue(User.class);
                        if (adminUsers != null) {
                            // ▼▼▼ UBAH SUAI DI SINI ▼▼▼
                            tvAdminName.setText(adminUsers.getUsername()); // Guna getUsername()
                            tvAdminEmail.setText(adminUsers.getEmail());
                            etFullName.setText(adminUsers.getUsername()); // Guna getUsername()
                            etEmail.setText(adminUsers.getEmail());
                            // Rujukan kepada etPhone dibuang
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(AdminProfileActivity.this, "Failed to load admin data.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        btnUpdateProfile.setOnClickListener(v -> updateAdminProfile());
        btnChangePassword.setOnClickListener(v -> {
            Toast.makeText(this, "Change Password clicked!", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateAdminProfile() {
        // ▼▼▼ UBAH SUAI DI SINI ▼▼▼
        String username = etFullName.getText().toString().trim(); // Nama variabel ditukar
        String email = etEmail.getText().toString().trim();
        // Rujukan kepada phone dibuang

        if (username.isEmpty() || email.isEmpty()) { // Semak username
            Toast.makeText(this, "Username and email cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userRef != null) {
            Map<String, Object> profileUpdates = new HashMap<>();
            profileUpdates.put("username", username); // Guna "username"
            profileUpdates.put("email", email);
            // Rujukan kepada "phone" dibuang

            userRef.updateChildren(profileUpdates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(AdminProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(AdminProfileActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show());
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> logoutAdmin())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logoutAdmin() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
