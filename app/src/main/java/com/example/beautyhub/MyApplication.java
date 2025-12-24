package com.example.beautyhub;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Sediakan konfigurasi untuk Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "de5ctrpk5");
        config.put("api_key", "818495986498182");
        config.put("api_secret", "Wmu5PvFCBF8Sk0NGJyb1354g00w");
        // config.put("secure", "true"); // Pilihan: untuk sentiasa guna URL HTTPS

        // Inisialisasi MediaManager sekali sahaja untuk seluruh aplikasi
        MediaManager.init(this, config);
    }
}
    