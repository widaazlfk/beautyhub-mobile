package com.example.beautyhub.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// ▼▼▼ 1. IMPORT LogHelper ▼▼▼
import com.example.beautyhub.admin.LogHelper;
// ▲▲▲ AKHIR IMPORT ▲▲▲

import com.example.beautyhub.admin.AdminDashboardActivity;
import com.example.beautyhub.buyer.BuyerActivity;
import com.example.beautyhub.databinding.ActivityRegisterBinding;
import com.example.beautyhub.models.User;
import com.example.beautyhub.seller.SellerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Creating Account");
        progressDialog.setMessage("Please wait, we are setting up your account...");
        progressDialog.setCancelable(false);

        binding.registerButton.setOnClickListener(v -> validateAndRegisterUser());

        binding.loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void validateAndRegisterUser() {
        String username = binding.name.getText().toString().trim();
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        String confirmPassword = binding.confirmPassword.getText().toString().trim();

        int selectedUserTypeId = binding.userTypeRadioGroup.getCheckedRadioButtonId();
        String userType;
        if (selectedUserTypeId == binding.radioBuyer.getId()) {
            userType = "Buyer";
        } else if (selectedUserTypeId == binding.radioSeller.getId()) {
            userType = "Seller";
        } else {
            Toast.makeText(this, "Please select a user type (Buyer/Seller)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            binding.name.setError("Username is required");
            binding.name.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            binding.email.setError("Email is required");
            binding.email.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.setError("Please enter a valid email");
            binding.email.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.password.setError("Password is required");
            binding.password.requestFocus();
            return;
        }

        if (password.length() < 6) {
            binding.password.setError("Password must be at least 6 characters long");
            binding.password.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.confirmPassword.setError("Passwords do not match");
            binding.confirmPassword.requestFocus();
            return;
        }

        progressDialog.show();
        registerUserWithFirebase(username, email, password, userType);
    }

    private void registerUserWithFirebase(String username, String email, String password, String userType) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveAdditionalUserInfo(firebaseUser, username, email, userType);
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "Failed to get user details after registration.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        progressDialog.dismiss();
                        String errorMessage = Objects.requireNonNull(task.getException()).getMessage();
                        Toast.makeText(RegisterActivity.this, "Authentication failed: " + errorMessage, Toast.LENGTH_LONG).show();

                        // ▼▼▼ If the error is about email already in use, set error on the email field ▼▼▼
                        if (errorMessage != null && errorMessage.contains("email address is already in use")) {
                            binding.email.setError("This email is already registered. Please use a different email or log in.");
                            binding.email.requestFocus();
                        }
                        // ▲▲▲ END OF CHANGE ▲▲▲
                    }
                });
    }

    private void saveAdditionalUserInfo(FirebaseUser firebaseUser, String username, String email, String userType) {
        String uid = firebaseUser.getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String registrationDate = sdf.format(new Date());

        // Menggunakan HashMap untuk memastikan kunci (keys) tepat seperti dalam Rules
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);                  // Wajib ada (Line 17 Rules)
        userMap.put("username", username);        // Wajib ada (Line 13 Rules)
        userMap.put("email", email);              // Wajib ada (Line 14 Rules)
        userMap.put("userType", userType);        // Wajib ada (Line 15 Rules) - "Buyer" atau "Seller"
        userMap.put("registrationDate", registrationDate); // Wajib ada (Line 16 Rules)

        databaseReference.setValue(userMap).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

                // Log Action
                String logMessage = "New " + userType + " registered with email: " + email;
                LogHelper.logAction(uid, logMessage);

                redirectToDashboard(userType);
            } else {
                // Jika masih error, paparkan error yang spesifik dari Firebase
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(RegisterActivity.this, "Database Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    private void redirectToDashboard(String userType) {
        if (userType == null) {
            // ... fallback ...
            return;
        }

        Intent intent;
        // Gunakan equalsIgnoreCase untuk perbandingan yang lebih selamat
        if (userType.equalsIgnoreCase("Admin")) { // Akan terima "Admin", "admin", "ADMIN"
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if (userType.equalsIgnoreCase("Seller")) { // Akan terima "Seller", "seller", "SELLER"
            intent = new Intent(this, SellerActivity.class);
        } else { // Termasuk "Buyer" dan sebarang kes lalai lain
            intent = new Intent(this, BuyerActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
