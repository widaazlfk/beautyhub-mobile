package com.example.beautyhub.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.admin.LogHelper;
import com.example.beautyhub.admin.AdminDashboardActivity;
import com.example.beautyhub.buyer.BuyerActivity;
import com.example.beautyhub.databinding.ActivityLoginBinding;
import com.example.beautyhub.models.User;
import com.example.beautyhub.seller.SellerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Initialize LogHelper
        LogHelper.initialize(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Logging In");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        binding.loginButton.setOnClickListener(v -> validateAndLoginUser());

        binding.registerLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        // Forgot password functionality
        binding.forgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auto-login if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserTypeAndRedirect(currentUser.getUid(), true);
        }
    }

    private void validateAndLoginUser() {
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();

        // Clear previous errors
        binding.email.setError(null);
        binding.password.setError(null);

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
            binding.password.setError("Password must be at least 6 characters");
            binding.password.requestFocus();
            return;
        }

        progressDialog.show();
        loginUserWithFirebase(email, password);
    }

    // Buka fail: C:/Users/widaa/beautyhub/app/src/main/java/com/example/beautyhub/auth/LoginActivity.java
// Gantikan kaedah (method) loginUserWithFirebase sedia ada dengan yang ini:

    private void loginUserWithFirebase(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // --- PERUBAHAN DI SINI ---
                        // Log masuk berjaya. E-mel & kata laluan betul.
                        // Terus ke langkah seterusnya tanpa menyemak status pengesahan e-mel.

                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            checkUserTypeAndRedirect(firebaseUser.getUid(), false);
                        } else {
                            // Keadaan ini jarang berlaku, tetapi sebagai langkah keselamatan
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this,
                                    "Authentication failed. User not found after successful login.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // --- AKHIR PERUBAHAN ---
                    } else {
                        // Log masuk gagal (contoh: kata laluan salah, pengguna tidak wujud)
                        progressDialog.dismiss();
                        String errorMessage = "Login failed.";
                        if (task.getException() != null) {
                            errorMessage = getFirebaseAuthErrorMessage(task.getException().getMessage());
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }


    private String getFirebaseAuthErrorMessage(String message) {
        if (message == null) return "Login failed.";

        if (message.contains("invalid-email")) {
            return "Invalid email address format.";
        } else if (message.contains("user-not-found")) {
            return "No account found with this email.";
        } else if (message.contains("wrong-password")) {
            return "Incorrect password.";
        } else if (message.contains("user-disabled")) {
            return "This account has been disabled.";
        } else if (message.contains("too-many-requests")) {
            return "Too many login attempts. Please try again later.";
        } else if (message.contains("network-request-failed")) {
            return "Network error. Please check your internet connection.";
        } else {
            return message;
        }
    }

    private void checkUserTypeAndRedirect(String uid, boolean isAutoLogin) {
        if (isAutoLogin) {
            progressDialog.setTitle("Loading...");
            progressDialog.setMessage("Restoring your session...");
            progressDialog.show();
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.dismiss();

                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getUserType() != null) {

                        if (user.isSuspended()) {
                            mAuth.signOut();
                            progressDialog.dismiss();

                            new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                                    .setTitle("Account Suspended")
                                    .setMessage("Your account has been suspended due to a violation of our terms.\n\n" +
                                            "Please contact our support team for assistance:\n" +
                                            "📧 Email: admin@beautyhub.com\n" +
                                            "📞 WhatsApp: +60179288974")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                    .show();
                            return;
                        }

                        // Log the login action
                        String userType = user.getUserType();
                        String logDetails = String.format("%s logged in %s",
                                userType,
                                isAutoLogin ? "automatically" : "successfully");

                        // Use the correct LogHelper method
                        LogHelper.logCurrentUserAction("User Login", logDetails, userType);

                        // Redirect user based on userType

                        // Buka fail: LoginActivity.java
// Cari kaedah: checkUserTypeAndRedirect()
// Gantikan blok if-else di dalamnya dengan ini:

                        Intent intent;
// Gunakan equalsIgnoreCase untuk perbandingan yang lebih selamat
                        if (userType.equalsIgnoreCase("Admin")) {
                            // ▼▼▼ PERUBAHAN DI SINI ▼▼▼
                            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        } else if (userType.equalsIgnoreCase("Seller")) {
                            intent = new Intent(LoginActivity.this, SellerActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, BuyerActivity.class);
                        }

                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        startActivity(intent);
                        finish();


                    } else {
                        // User data is corrupted
                        handleInvalidUserData();
                    }
                } else {
                    // No user record found
                    handleInvalidUserData();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this,
                        "Database error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleInvalidUserData() {
        mAuth.signOut();
        Toast.makeText(LoginActivity.this,
                "Invalid user data. Please register again or contact support.",
                Toast.LENGTH_LONG).show();
    }

    // Add this method to handle forgot password
    public void onForgotPasswordClicked(View view) {
        String email = binding.email.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog resetDialog = new ProgressDialog(this);
        resetDialog.setMessage("Sending password reset email...");
        resetDialog.show();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    resetDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Failed to send reset email: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Optional: Add biometric login
    private void setupBiometricLogin() {
        // Implementation for biometric login (fingerprint/face recognition)
        // You can use Android's BiometricPrompt API
    }
}