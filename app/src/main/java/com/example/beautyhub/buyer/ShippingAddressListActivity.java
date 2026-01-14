package com.example.beautyhub.buyer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.adapters.ShippingAddressAdapter;
import com.example.beautyhub.models.ShippingAddress;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ShippingAddressListActivity extends AppCompatActivity {

    private static final String TAG = "ShippingAddressList";

    // Views

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewAddresses;
    private FloatingActionButton fabAddAddress;
    private TextView tvNoAddresses;

    // Firebase
    private DatabaseReference userAddressesRef;
    private ValueEventListener addressesListener;
    private FirebaseUser currentUser;

    // Variables
    private ShippingAddressAdapter adapter;
    private final List<ShippingAddress> addressList = new ArrayList<>();
    private boolean isSelectMode = false; // Flag untuk mod memilih

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipping_address_list);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view addresses.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Periksa jika aktiviti ini dilancarkan dalam mod memilih
        isSelectMode = getIntent().getBooleanExtra("SELECT_MODE", false);

        userAddressesRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUser.getUid())
                .child("addresses");

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_shipping_address);
        recyclerViewAddresses = findViewById(R.id.recycler_view_addresses);
        fabAddAddress = findViewById(R.id.fab_add_address);
        tvNoAddresses = findViewById(R.id.tv_no_addresses);

        if (isSelectMode) {
            toolbar.setTitle("Select Address");
        } else {
            toolbar.setTitle("My Shipping Addresses");
        }
    }

    private void setupRecyclerView() {
        adapter = new ShippingAddressAdapter(this, addressList,
                // Listener untuk butang Edit dan Delete
                new ShippingAddressAdapter.OnAddressActionsListener() {
                    @Override
                    public void onEdit(ShippingAddress address) {
                        Intent intent = new Intent(ShippingAddressListActivity.this, AddEditAddressActivity.class);
                        intent.putExtra("EDIT_ADDRESS", address);
                        startActivity(intent);
                    }

                    @Override
                    public void onDelete(ShippingAddress address) {
                        deleteAddress(address.getAddressId());
                    }
                },
                // Listener untuk klik pada keseluruhan item
                address -> {
                    if (isSelectMode) {
                        // Jika dalam mod memilih, set alamat ini sebagai default dan kembali
                        setDefaultAddressAndFinish(address);
                    }
                }
        );

        recyclerViewAddresses.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAddresses.setAdapter(adapter);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        fabAddAddress.setOnClickListener(v -> {
            Intent intent = new Intent(ShippingAddressListActivity.this, AddEditAddressActivity.class);
            startActivity(intent);
        });
    }

    private void loadAddresses() {
        if (addressesListener != null) {
            userAddressesRef.removeEventListener(addressesListener);
        }
        addressesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                addressList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot addressSnapshot : snapshot.getChildren()) {
                        ShippingAddress address = addressSnapshot.getValue(ShippingAddress.class);
                        if (address != null) {
                            addressList.add(address);
                        }
                    }
                    tvNoAddresses.setVisibility(View.GONE);
                    recyclerViewAddresses.setVisibility(View.VISIBLE);
                } else {
                    tvNoAddresses.setVisibility(View.VISIBLE);
                    recyclerViewAddresses.setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load addresses: ", error.toException());
                Toast.makeText(ShippingAddressListActivity.this, "Failed to load addresses.", Toast.LENGTH_SHORT).show();
            }
        };
        userAddressesRef.addValueEventListener(addressesListener);
    }

    // Kaedah baru untuk set alamat default dan kembali
    private void setDefaultAddressAndFinish(ShippingAddress selectedAddress) {
        // Buat Intent untuk menghantar data kembali
        Intent resultIntent = new Intent();
        resultIntent.putExtra("SELECTED_ADDRESS", selectedAddress);

        // Jika alamat yang dipilih sudah pun default, terus kembali dengan data
        if (selectedAddress.isDefault()) {
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating your default address...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 1. Dapatkan alamat yang default sekarang dan tukar ke false
        userAddressesRef.orderByChild("default").equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot addressSnapshot : snapshot.getChildren()) {
                    addressSnapshot.getRef().child("default").setValue(false);
                }

                // Tandakan alamat yang baru dipilih sebagai default dalam objek itu sendiri
                selectedAddress.setDefault(true);

                // 2. Set alamat yang baru dipilih ke true di Firebase
                userAddressesRef.child(selectedAddress.getAddressId()).child("default").setValue(true)
                        .addOnCompleteListener(task -> {
                            progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                // 3. Hantar isyarat berjaya DAN data alamat, kemudian tutup skrin
                                setResult(RESULT_OK, resultIntent); // <-- INI PEMBETULANNYA
                                finish();
                            } else {
                                Toast.makeText(ShippingAddressListActivity.this, "Failed to set default address.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(ShippingAddressListActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void deleteAddress(String addressId) {
        if (addressId == null || addressId.isEmpty()) return;
        userAddressesRef.child(addressId).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(ShippingAddressListActivity.this, "Address deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ShippingAddressListActivity.this, "Failed to delete address", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (addressesListener != null) {
            userAddressesRef.removeEventListener(addressesListener);
        }
    }
}
