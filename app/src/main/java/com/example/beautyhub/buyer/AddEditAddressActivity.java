package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beautyhub.R;
import com.example.beautyhub.models.ShippingAddress;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AddEditAddressActivity extends AppCompatActivity {

    // Deklarasi komponen UI
    private MaterialToolbar toolbar;
    private TextInputEditText etRecipientName, etPhoneNumber, etStreet, etCity, etState, etZipCode, etAddressType;
    private SwitchMaterial switchDefaultAddress; // <-- TAMBAH Switch
    private Button btnSaveChanges;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Untuk menyimpan data alamat yang sedang diedit
    private ShippingAddress addressToEdit;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_address);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeViews();
        setupToolbar();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EDIT_ADDRESS")) {
            addressToEdit = intent.getParcelableExtra("EDIT_ADDRESS");
            if (addressToEdit != null) {
                isEditMode = true;
                populateFieldsForEdit();
            }
        } else {
            // Jika dalam mod tambah, set tajuk di sini
            toolbar.setTitle("Add New Address");
        }

        btnSaveChanges.setOnClickListener(v -> saveAddress());
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_add_edit_address);
        etRecipientName = findViewById(R.id.et_recipient_name);
        etPhoneNumber = findViewById(R.id.et_phone_number);
        etStreet = findViewById(R.id.et_street);
        etCity = findViewById(R.id.et_city);
        etState = findViewById(R.id.et_state);
        etZipCode = findViewById(R.id.et_zip_code);
        etAddressType = findViewById(R.id.et_address_type);
        switchDefaultAddress = findViewById(R.id.switch_default_address); // <-- Ikat (Bind) Switch
        btnSaveChanges = findViewById(R.id.btn_save_changes);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void populateFieldsForEdit() {
        toolbar.setTitle("Edit Address"); // Tukar tajuk
        etRecipientName.setText(addressToEdit.getRecipientName());
        etPhoneNumber.setText(addressToEdit.getPhoneNumber());
        etStreet.setText(addressToEdit.getStreet());
        etCity.setText(addressToEdit.getCity());
        etState.setText(addressToEdit.getState());
        etZipCode.setText(addressToEdit.getZipcode());
        etAddressType.setText(addressToEdit.getAddressType());
        switchDefaultAddress.setChecked(addressToEdit.isDefault()); // <-- Set status Switch
    }

    private void saveAddress() {
        // Dapatkan teks dari setiap input field
        String recipientName = etRecipientName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String street = etStreet.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String state = etState.getText().toString().trim();
        String zipcode = etZipCode.getText().toString().trim();
        String addressType = etAddressType.getText().toString().trim();
        boolean isDefault = switchDefaultAddress.isChecked(); // <-- Dapatkan status dari Switch

        // ▼▼▼ TAMBAHAN: Validasi input ▼▼▼
        if (recipientName.isEmpty() || phoneNumber.isEmpty() || street.isEmpty() || city.isEmpty() || state.isEmpty() || zipcode.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validasi nombor telefon mesti bermula dengan '0'
        if (!phoneNumber.startsWith("0")) {
            etPhoneNumber.setError("Phone number must start with '0'");
            etPhoneNumber.requestFocus();
            return;
        }
        // ▲▲▲ AKHIR TAMBAHAN ▲▲▲


        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to save an address.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userAddressesRef = mDatabase.child("Users").child(currentUser.getUid()).child("addresses");

        String addressId;
        if (isEditMode) {
            addressId = addressToEdit.getAddressId();
        } else {
            addressId = userAddressesRef.push().getKey();
        }

        if (addressId == null) {
            Toast.makeText(this, "Could not get address ID. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cipta atau kemas kini objek ShippingAddress
        ShippingAddress addressData = new ShippingAddress();
        addressData.setAddressId(addressId);
        addressData.setRecipientName(recipientName);
        addressData.setPhoneNumber(phoneNumber);
        addressData.setStreet(street);
        addressData.setCity(city);
        addressData.setState(state);
        addressData.setZipcode(zipcode);
        addressData.setAddressType(addressType);
        addressData.setDefault(isDefault); // <-- Guna nilai dari Switch

        // ▼▼▼ PERUBAHAN: Logik untuk menguruskan hanya satu alamat default ▼▼▼
        if (isDefault) {
            // Jika pengguna set alamat ini sebagai default, kita perlu pastikan alamat lain tidak lagi default
            unsetOtherDefaultAddresses(userAddressesRef, addressId, () -> {
                // Selepas selesai, barulah kita simpan alamat baru/yang dikemas kini
                saveFinalAddress(userAddressesRef, addressId, addressData);
            });
        } else {
            // Jika pengguna tidak set sebagai default, terus simpan
            saveFinalAddress(userAddressesRef, addressId, addressData);
        }
        // ▲▲▲ AKHIR PERUBAHAN ▲▲▲
    }

    // Kaedah untuk membuang status 'default' dari alamat lain
    private void unsetOtherDefaultAddresses(DatabaseReference userAddressesRef, String currentAddressId, Runnable onComplete) {
        userAddressesRef.orderByChild("default").equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot addressSnapshot : snapshot.getChildren()) {
                    // Jangan sentuh alamat yang sedang kita edit/simpan sekarang
                    if (!addressSnapshot.getKey().equals(currentAddressId)) {
                        addressSnapshot.getRef().child("default").setValue(false);
                    }
                }
                onComplete.run(); // Jalankan callback selepas selesai
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddEditAddressActivity.this, "Failed to update default status: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                onComplete.run(); // Teruskan walaupun gagal
            }
        });
    }

    // Kaedah untuk menyimpan alamat
    private void saveFinalAddress(DatabaseReference ref, String addressId, ShippingAddress addressData) {
        ref.child(addressId).setValue(addressData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddEditAddressActivity.this, "Address saved successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Kembali ke skrin sebelumnya
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddEditAddressActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
