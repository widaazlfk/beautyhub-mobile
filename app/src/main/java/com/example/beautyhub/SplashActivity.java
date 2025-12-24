package com.example.beautyhub;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.auth.LoginActivity;
import com.example.beautyhub.buyer.BuyerActivity;
// --- ▼▼▼ PERBAIKAN 1: Import kelas model yang betul ▼▼▼ ---
import com.example.beautyhub.models.User; // Guna User.class bukan Users.class
import com.example.beautyhub.seller.SellerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * SplashActivity ialah skrin pertama yang dipaparkan apabila aplikasi dilancarkan.
 * Tugas utamanya adalah untuk menyemak status log masuk pengguna dan menghalakan
 * mereka ke skrin yang betul (Log Masuk atau Papan Pemuka).
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2000; // 2 saat
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Menggunakan Handler untuk melengahkan proses semakan, memberi masa untuk logo dipaparkan.
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserSession, SPLASH_DELAY_MS);
    }

    /**
     * Menyemak sesi pengguna semasa dari Firebase Auth.
     */
    private void checkUserSession() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Jika ada sesi pengguna aktif, dapatkan butiran pengguna dari Realtime Database.
            Log.d(TAG, "Sesi pengguna aktif ditemui untuk UID: " + currentUser.getUid() + ". Mengambil jenis pengguna...");
            fetchUserTypeAndRedirect(currentUser.getUid());
        } else {
            // Jika tiada sesi aktif, halakan pengguna ke skrin log masuk.
            Log.d(TAG, "Tiada sesi pengguna aktif. Menghala ke LoginActivity.");
            navigateToLogin();
        }
    }

    /**
     * Mengambil peranan ('userType') pengguna dari Realtime Database berdasarkan userId.
     * @param userId UID pengguna dari Firebase Auth.
     */
    private void fetchUserTypeAndRedirect(String userId) {
        // --- ▼▼▼ PERBAIKAN 2: Guna rujukan pangkalan data yang betul ("Users") ▼▼▼ ---
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // --- ▼▼▼ PERBAIKAN 3: Guna kelas model yang betul (User.class) ▼▼▼ ---
                    User user = dataSnapshot.getValue(User.class);
                    // Pastikan objek pengguna dan jenis pengguna tidak kosong.
                    if (user != null && user.getUserType() != null) {
                        Log.d(TAG, "Jenis pengguna ditemui: " + user.getUserType());
                        navigateToDashboard(user.getUserType());
                    } else {
                        // Kes di mana data pengguna ada tetapi rosak atau tidak lengkap.
                        Log.e(TAG, "Data pengguna rosak atau 'userType' tiada. Memaksa log masuk semula.");
                        forceLogoutAndRedirectToLogin();
                    }
                } else {
                    // Kes yang jarang berlaku: Sesi Auth wujud tetapi tiada rekod dalam database.
                    Log.e(TAG, "Data pengguna tidak ditemui dalam database. Memaksa log masuk semula.");
                    forceLogoutAndRedirectToLogin();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Jika terdapat ralat semasa menyambung ke database, lebih selamat untuk log masuk semula.
                Log.e(TAG, "Ralat database semasa mengambil jenis pengguna: " + databaseError.getMessage());
                navigateToLogin();
            }
        });
    }

    /**
     * Menghalakan pengguna ke papan pemuka yang betul berdasarkan 'userType'.
     * @param userType Peranan pengguna, contohnya "SELLER" atau "BUYER".
     */
    private void navigateToDashboard(String userType) {
        Intent intent;
        // Gunakan equalsIgnoreCase untuk perbandingan yang lebih selamat
        if ("SELLER".equalsIgnoreCase(userType)) {
            intent = new Intent(SplashActivity.this, SellerActivity.class);
        } else { // Anggap peranan selain "SELLER" adalah "BUYER" sebagai lalai.
            intent = new Intent(SplashActivity.this, BuyerActivity.class);
        }
        startActivityWithFlags(intent);
    }

    /**
     * Menghalakan pengguna ke skrin Log Masuk.
     */
    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivityWithFlags(intent);
    }

    /**
     * Melancarkan Intent baharu dan membersihkan semua aktiviti sebelumnya.
     * @param intent Intent yang hendak dilancarkan.
     */
    private void startActivityWithFlags(Intent intent) {
        // Bendera ini penting untuk memastikan pengguna tidak boleh kembali ke SplashActivity.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Tutup SplashActivity secara kekal.
    }

    /**
     * Mengelog keluar pengguna dari Firebase Auth dan menghalakan ke skrin log masuk.
     * Berguna untuk mengendalikan data yang tidak konsisten.
     */
    private void forceLogoutAndRedirectToLogin() {
        FirebaseAuth.getInstance().signOut();
        navigateToLogin();
    }
}
