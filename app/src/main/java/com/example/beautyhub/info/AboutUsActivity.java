package com.example.beautyhub.info;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import com.example.beautyhub.R;
import com.google.android.material.appbar.MaterialToolbar;

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        // 1. Setup Toolbar & Back Button
        setupToolbar();

        // 2. Setup Instagram Click
        ImageButton btnInstagram = findViewById(R.id.btn_instagram);
        btnInstagram.setOnClickListener(v -> {
            String url = "https://www.instagram.com/beautyhubofficial?igsh=b2dnaGV1OXhzN2lx"; // Tukar ke username anda
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startContext(intent);
        });

        // 3. Set Version
        TextView tvAppVersion = findViewById(R.id.tv_app_version);
        setAppVersion(tvAppVersion);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_about);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            // Ikon Arrow Back dikendalikan di sini
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setAppVersion(TextView textView) {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            textView.setText("Version " + pInfo.versionName);
        } catch (Exception e) {
            textView.setText("Version 1.0.0");
        }
    }

    private void startContext(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e("AboutUs", "Browser tidak dijumpai");
        }
    }
}