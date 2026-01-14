package com.example.beautyhub.seller;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.beautyhub.R;
import com.example.beautyhub.models.ProductListing;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
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

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            editTextPrice.setError("Format harga tidak sah");
            editTextPrice.requestFocus();
            return;
        }

        int stock;
        try {
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            editTextStock.setError("Format stok tidak sah");
            editTextStock.requestFocus();
            return;
        }

        int selectedConditionId = radioGroupCondition.getCheckedRadioButtonId();
        if (selectedConditionId == -1) {
            Toast.makeText(this, "Sila pilih kondisi produk", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadioButton = findViewById(selectedConditionId);
        String condition = selectedRadioButton.getText().toString();

        String currentUserId = mAuth.getCurrentUser().getUid();

        progressDialog.show(); // Tunjukkan dialog sebelum memuatkan data penjual

        // 1. Dapatkan maklumat penjual dari nod 'Users'
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Ambil username dan profileImageUrl dari profil pengguna
                    String sellerName = snapshot.child("username").getValue(String.class);
                    String sellerProfileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    // Pastikan nilai tidak null untuk mengelakkan ralat
                    if (sellerName == null) sellerName = "Unknown Seller";
                    if (sellerProfileImageUrl == null) sellerProfileImageUrl = ""; // URL kosong jika tiada

                    // 2. Panggil kaedah untuk menyimpan dengan maklumat yang lengkap
                    saveListingToFirebase(currentUserId, sellerName, sellerProfileImageUrl, price, stock, condition);

                } else {
                    progressDialog.dismiss();
                    Toast.makeText(AddListingActivity.this, "Gagal mendapatkan data penjual.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(AddListingActivity.this, "Ralat pangkalan data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveListingToFirebase(String sellerId, String sellerName, String sellerProfileImageUrl, double price, int stock, String condition) {
        String listingId = listingsRef.push().getKey();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        // Cipta objek ProductListing dengan maklumat penjual yang lengkap
        // Pastikan konstruktor dalam ProductListing.java sepadan
        ProductListing newListing = new ProductListing(
                selectedProductId,
                selectedProductName,
                sellerId,
                sellerName,
                price,
                stock,
                condition,
                currentDate,
               sellerProfileImageUrl,
                null
        );

        // Tetapkan URL gambar profil penjual menggunakan setter (ini lebih selamat)
        newListing.setSellerProfileImageUrl(sellerProfileImageUrl);

        if (listingId != null) {
            // Simpan objek di bawah nod utama "ProductListings"
            listingsRef.child(listingId).setValue(newListing)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(AddListingActivity.this, "Penyenaraian berjaya disimpan!", Toast.LENGTH_SHORT).show();
                            finish(); // Kembali ke skrin sebelumnya
                        } else {
                            Toast.makeText(AddListingActivity.this, "Gagal menyimpan: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            progressDialog.dismiss();
            Toast.makeText(AddListingActivity.this, "Gagal mencipta ID untuk penyenaraian.", Toast.LENGTH_SHORT).show();
        }
    }
}
