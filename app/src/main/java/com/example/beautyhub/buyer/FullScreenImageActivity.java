package com.example.beautyhub.buyer;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.beautyhub.R;
import com.example.beautyhub.adapters.FullScreenImageAdapter;
import java.util.ArrayList;

public class FullScreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URLS = "EXTRA_IMAGE_URLS";
    public static final String EXTRA_CURRENT_POSITION = "EXTRA_CURRENT_POSITION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Jadikan aktiviti skrin penuh
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_full_screen_image);

        ViewPager2 viewPager = findViewById(R.id.view_pager_full_screen);
        findViewById(R.id.btn_close).setOnClickListener(v -> finish());

        // Dapatkan data dari Intent
        ArrayList<String> imageUrls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);
        int currentPosition = getIntent().getIntExtra(EXTRA_CURRENT_POSITION, 0);

        if (imageUrls != null && !imageUrls.isEmpty()) {
            FullScreenImageAdapter adapter = new FullScreenImageAdapter(this, imageUrls);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(currentPosition, false); // Tunjuk imej yang diklik
        }
    }
}
