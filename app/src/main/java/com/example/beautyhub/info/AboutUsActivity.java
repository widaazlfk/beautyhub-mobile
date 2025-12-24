package com.example.beautyhub.info;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.beautyhub.R;

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        // Tetapkan tajuk untuk AppBar aktiviti ini
        // Anda boleh membuang baris ini jika anda menggunakan Toolbar khas
        setTitle("About Us");

        // Dapatkan TextView untuk versi aplikasi
        TextView tvAppVersion = findViewById(R.id.tv_app_version);

        // Panggil method untuk menetapkan teks versi secara dinamik
        setAppVersion(tvAppVersion);
    }

    /**
     * Method ini mendapatkan versi aplikasi dari Gradle dan memaparkannya.
     * @param textView TextView untuk memaparkan versi.
     */
    private void setAppVersion(TextView textView) {
        try {
            // Dapatkan maklumat pakej untuk aplikasi semasa
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            // Dapatkan nama versi (cth: "1.0.0")
            String version = pInfo.versionName;
            // Tetapkan teks pada TextView
            textView.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            // Tangani ralat jika maklumat pakej tidak ditemui
            Log.e("AboutUsActivity", "Could not get package version", e);
            // Tetapkan teks lalai jika berlaku ralat
            textView.setText("Version 1.0.0");
        }
    }
}
