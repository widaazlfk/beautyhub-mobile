package com.example.beautyhub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.admin.AdminDashboardActivity; // Pastikan aktiviti ini wujud
import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.buyer.BuyerActivity;   // Import yang betul
import com.example.beautyhub.seller.SellerActivity; // Import yang betul
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Tetapkan layout untuk MainActivity.
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            sendUserToLoginActivity();
        } else {
            checkUserTypeAndRedirect(currentUser.getUid());
        }
    }

    private void checkUserTypeAndRedirect(String uid) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("userType")) {
                    String userType = snapshot.child("userType").getValue(String.class);

                    if (userType == null) {
                        Log.e(TAG, "UserType is null for UID: " + uid);
                        sendUserToLoginActivity();
                        return;
                    }

                    // ▼▼▼ BAHAGIAN DIPERBETULKAN ▼▼▼
                    // Arahkan pengguna berdasarkan jenis akaun mereka
                    switch (userType) {
                        case "Buyer":
                            // Buka Buyer Activity
                            startActivity(new Intent(MainActivity.this, BuyerActivity.class));
                            break;
                        case "Seller":
                            // Buka Seller Activity
                            startActivity(new Intent(MainActivity.this, SellerActivity.class));
                            break;
                        case "Admin":
                            // Buka Admin Dashboard
                            startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
                            break;
                        default:
                            // Jika jenis pengguna tidak dikenali, log keluar
                            Toast.makeText(MainActivity.this, "Unknown user type.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            sendUserToLoginActivity();
                            break;
                    }
                    // ▲▲▲ PEMBETULAN TAMAT DI SINI ▲▲▲

                    finish(); // Tutup MainActivity supaya pengguna tidak boleh kembali ke sini

                } else {
                    // Data pengguna tidak ditemui di database
                    Log.e(TAG, "User data not found in database for UID: " + uid);
                    Toast.makeText(MainActivity.this, "Your data is not found. Please log in again.", Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                    sendUserToLoginActivity();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Failed to read user data.", Toast.LENGTH_SHORT).show();
                sendUserToLoginActivity();
            }
        });
    }

    private void sendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish(); // Tutup MainActivity
    }
}
