package com.example.beautyhub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.buyer.BuyerActivity;
import com.example.beautyhub.seller.SellerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    private Button btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Inisialisasi butang berdasarkan ID di XML (@id/button)
        btnGetStarted = findViewById(R.id.button);

        // Apabila butang ditekan
        btnGetStarted.setOnClickListener(v -> {
            checkUserSession();
        });
    }

    private void checkUserSession() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Jika user dah pernah login, terus bawa ke Dashboard yang betul
            fetchUserTypeAndRedirect(currentUser.getUid());
        } else {
            // Jika belum login, terus ke LoginActivity
            navigateToLogin();
        }
    }

    private void fetchUserTypeAndRedirect(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Ambil role (userType) untuk tentukan skrin mana yang patut dibuka
                    String userType = dataSnapshot.child("userType").getValue(String.class);
                    if (userType != null) {
                        navigateToDashboard(userType);
                    } else {
                        navigateToLogin();
                    }
                } else {
                    navigateToLogin();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                navigateToLogin();
            }
        });
    }

    private void navigateToDashboard(String userType) {
        Intent intent;
        if ("SELLER".equalsIgnoreCase(userType)) {
            intent = new Intent(SplashActivity.this, SellerActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, BuyerActivity.class);
        }
        startActivityWithFlags(intent);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivityWithFlags(intent);
    }

    private void startActivityWithFlags(Intent intent) {
        // Flag ini supaya user tak boleh tekan 'Back' untuk kembali ke Splash
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}