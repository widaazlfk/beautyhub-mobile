package com.example.beautyhub.seller;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.beautyhub.R;

public class EditProductActivity extends AppCompatActivity {

    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We can reuse the add product layout for editing
        setContentView(R.layout.s_activity_add_product);

        // Get the product ID passed from ManageProductsActivity
        productId = getIntent().getStringExtra("productId");
        if (productId == null || productId.isEmpty()) {
            // Handle error: No product ID provided
            finish();
            return;
        }

        // Next, you would initialize views and load the product data
        // from Firebase using the productId.
    }
}
