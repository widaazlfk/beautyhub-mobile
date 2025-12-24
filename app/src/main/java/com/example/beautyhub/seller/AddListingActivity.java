package com.example.beautyhub.seller;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.beautyhub.R;
import com.example.beautyhub.models.ProductListing;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddListingActivity extends AppCompatActivity {

    private TextView textViewSelectedProductName;
    private TextInputEditText editTextPrice, editTextStock;
    private RadioGroup radioGroupCondition;
    private Button buttonSaveListing;

    private String selectedProductId;
    private String selectedProductName;
    private ProgressDialog progressDialog;

    private DatabaseReference listingsRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_listing);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Dapatkan data dari Intent (dari SearchProductForSellerActivity)
        selectedProductId = getIntent().getStringExtra("SELECTED_PRODUCT_ID");
        selectedProductName = getIntent().getStringExtra("SELECTED_PRODUCT_NAME");

        // Rujukan ke Firebase
        listingsRef = FirebaseDatabase.getInstance().getReference("ProductListings");
        mAuth = FirebaseAuth.getInstance();

        // Inisialisasi UI
        textViewSelectedProductName = findViewById(R.id.text_view_selected_product_name);
        editTextPrice = findViewById(R.id.edit_text_price);
        editTextStock = findViewById(R.id.edit_text_stock);
        radioGroupCondition = findViewById(R.id.radio_group_condition);
        buttonSaveListing = findViewById(R.id.button_save_listing);

        // Paparkan nama produk yang dipilih
        textViewSelectedProductName.setText(selectedProductName);

        // Setup ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Menyimpan Penyenaraian");
        progressDialog.setMessage("Sila tunggu...");
        progressDialog.setCancelable(false);

        // Set listener untuk butang simpan
        buttonSaveListing.setOnClickListener(v -> validateAndSaveListing());
    }

    private void validateAndSaveListing() {
        String priceStr = editTextPrice.getText().toString().trim();
        String stockStr = editTextStock.getText().toString().trim();

        if (TextUtils.isEmpty(priceStr)) {
            editTextPrice.setError("Harga diperlukan");
            editTextPrice.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(stockStr)) {
            editTextStock.setError("Stok diperlukan");
            editTextStock.requestFocus();
            return;
        }

        double price = 0;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            editTextPrice.setError("Sila masukkan format harga yang betul");
            editTextPrice.requestFocus();
            return;
        }

        int stock = 0;
        try {
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            editTextStock.setError("Sila masukkan format stok yang betul");
            editTextStock.requestFocus();
            return;
        }

        int selectedConditionId = radioGroupCondition.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedConditionId);
        String condition = selectedRadioButton.getText().toString();

        String currentUserId = mAuth.getCurrentUser().getUid();

        saveListingToFirebase(currentUserId, price, stock, condition);
    }

    private void saveListingToFirebase(String sellerId, double price, int stock, String condition) {
        progressDialog.show();

        // Cipta ID unik untuk penyenaraian baru
        String listingId = listingsRef.push().getKey();

        // Dapatkan tarikh semasa
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        // Cipta objek ProductListing
        ProductListing newListing = new ProductListing(selectedProductId, sellerId, price, stock, condition, currentDate);

        if (listingId != null) {
            listingsRef.child(listingId).setValue(newListing)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(AddListingActivity.this, "Penyenaraian berjaya disimpan!", Toast.LENGTH_SHORT).show();
                            // Balik ke skrin sebelumnya atau ke papan pemuka penjual
                            finish();
                        } else {
                            Toast.makeText(AddListingActivity.this, "Gagal menyimpan: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
