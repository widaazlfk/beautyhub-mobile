package com.example.beautyhub.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ShippingAddressActivity extends AppCompatActivity implements ShippingAddressAdapter.OnAddressActionsListener {

    private RecyclerView rvAddressList;
    private MaterialButton btnAddNewAddress;
    private ProgressBar progressBar;
    private TextView tvNoAddresses;

    private ShippingAddressAdapter adapter;
    private List<ShippingAddress> addressList;
    private DatabaseReference addressRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.b_activity_shipping_address);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Laluan yang betul ke alamat pengguna di Firebase
        addressRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUser.getUid())
                .child("addresses");

        initViews();
        setupToolbar();
        setupRecyclerView();

        // Butang ini akan membuka borang AddEditAddressActivity untuk alamat BARU
        btnAddNewAddress.setOnClickListener(v -> {
            Intent intent = new Intent(ShippingAddressActivity.this, AddEditAddressActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Muat turun alamat setiap kali aktiviti ini dipaparkan semula (selepas menambah/mengedit)
        loadAddresses();
    }

    private void initViews() {
        rvAddressList = findViewById(R.id.rv_address_list);
        btnAddNewAddress = findViewById(R.id.btn_add_new_address);
        progressBar = findViewById(R.id.progress_bar);
        tvNoAddresses = findViewById(R.id.tv_no_addresses);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_shipping_address);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        addressList = new ArrayList<>();
        // 'this' merujuk kepada ShippingAddressActivity yang mengimplementasi OnAddressActionsListener
        adapter = new ShippingAddressAdapter(this, addressList, this, null);
        rvAddressList.setLayoutManager(new LinearLayoutManager(this));
        rvAddressList.setAdapter(adapter);
    }

    private void loadAddresses() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoAddresses.setVisibility(View.GONE);
        rvAddressList.setVisibility(View.GONE);

        addressRef.addValueEventListener(new ValueEventListener() {
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
                }

                progressBar.setVisibility(View.GONE);
                if (addressList.isEmpty()) {
                    tvNoAddresses.setVisibility(View.VISIBLE);
                    rvAddressList.setVisibility(View.GONE);
                } else {
                    tvNoAddresses.setVisibility(View.GONE);
                    rvAddressList.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ShippingAddressActivity.this, "Failed to load addresses.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Implementasi dari OnAddressActionsListener ---

    @Override
    public void onEdit(ShippingAddress address) {
        // Apabila 'Edit' pada item ditekan, buka AddEditAddressActivity dan hantar ID alamat
        Intent intent = new Intent(ShippingAddressActivity.this, AddEditAddressActivity.class);
        intent.putExtra("ADDRESS_ID", address.getAddressId());
        startActivity(intent);
    }

    @Override
    public void onDelete(ShippingAddress address) {
        // Logik untuk memadam alamat dari Firebase
        if (address != null && address.getAddressId() != null) {
            addressRef.child(address.getAddressId()).removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Address deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete address", Toast.LENGTH_SHORT).show());
        }
    }
}
