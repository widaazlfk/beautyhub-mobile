package com.example.beautyhub.info;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.beautyhub.R;
import com.google.android.material.appbar.MaterialToolbar; // Tambah import ini

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        // 1. Inisialisasi Toolbar dan fungsi Back Button
        setupToolbar();

        // Dapatkan TextView untuk versi aplikasi
        TextView tvAppVersion = findViewById(R.id.tv_app_version);

        // Panggil method untuk menetapkan teks versi secara dinamik
        setAppVersion(tvAppVersion);
    }

    /**
     * Method untuk menguruskan Toolbar dan butang kembali.
     */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_about);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            // Logik apabila butang back diklik
            toolbar.setNavigationOnClickListener(v -> {
                onBackPressed(); // Menutup aktiviti semasa dan kembali ke halaman sebelumnya
            });
        }
    }

    /**
     * Method ini mendapatkan versi aplikasi dari Gradle dan memaparkannya.
     * @param textView TextView untuk memaparkan versi.
     */
    private void setAppVersion(TextView textView) {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            textView.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("AboutUsActivity", "Could not get package version", e);
            textView.setText("Version 1.0.0");
        }
    }
}