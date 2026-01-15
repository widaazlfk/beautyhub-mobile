package com.example.beautyhub.admin;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class UsersStatisticsActivity extends AppCompatActivity {

    private static final String TAG = "UsersStatisticsActivity";
    private DatabaseReference usersRef;
    private TextView tvTotalUsers, tvTotalSellers, tvTotalBuyers;
    private PieChart pieChart;

    // Untuk Senarai User Terbaru
    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<UserStat> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_statistics);

        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        initViews();
        setupToolbar();
        setupPieChart();
        loadUserData();
    }

    private void initViews() {
        tvTotalUsers = findViewById(R.id.tv_stats_total_users);
        tvTotalSellers = findViewById(R.id.tv_stats_total_sellers);
        tvTotalBuyers = findViewById(R.id.tv_stats_total_buyers);
        pieChart = findViewById(R.id.pie_chart_users);

        // Setup RecyclerView (Pastikan ID ini ada dalam XML anda)
        recyclerView = findViewById(R.id.rv_users_list);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            userList = new ArrayList<>();
            adapter = new UserAdapter(userList);
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setCenterText("User Distribution");
        pieChart.setCenterTextSize(16f);

        // Tambah Click Listener pada Carta Pai
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                PieEntry pe = (PieEntry) e;
                Toast.makeText(UsersStatisticsActivity.this,
                        pe.getLabel() + ": " + (int) pe.getValue() + " users",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected() {
            }
        });
    }

    private void loadUserData() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                long totalSellers = 0;
                long totalBuyers = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    // Pakai ROLE mengikut struktur anda yang huruf besar
                    String role = ds.child("ROLE").getValue(String.class);
                    if (role == null) role = ds.child("userType").getValue(String.class);

                    String name = ds.child("username").getValue(String.class);
                    String email = ds.child("email").getValue(String.class);

                    if ("Seller".equalsIgnoreCase(role)) totalSellers++;
                    else if ("Buyer".equalsIgnoreCase(role)) totalBuyers++;

                    // Masukkan dalam list untuk senarai di bawah
                    userList.add(new UserStat(name, role, email));
                }

                // Susun senarai (paling atas mungkin user terbaru - jika ada timestamp)
                // Buat masa ni kita biar default atau sort ikut nama
                Collections.sort(userList, (o1, o2) -> o1.name.compareToIgnoreCase(o2.name));

                tvTotalUsers.setText(String.valueOf(snapshot.getChildrenCount()));
                tvTotalSellers.setText(String.valueOf(totalSellers));
                tvTotalBuyers.setText(String.valueOf(totalBuyers));

                updatePieChartData(totalBuyers, totalSellers);
                if (adapter != null) adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void updatePieChartData(long buyers, long sellers) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (buyers > 0) entries.add(new PieEntry(buyers, "Buyer"));
        if (sellers > 0) entries.add(new PieEntry(sellers, "Seller"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{Color.parseColor("#4C4CAF"), Color.parseColor("#8a2128")});
        dataSet.setSliceSpace(3f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(data);
        pieChart.invalidate();
        pieChart.animateY(1000);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_users_statistics);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    // --- Model & Adapter ---
    private static class UserStat {
        String name, role, email;

        UserStat(String n, String r, String e) {
            name = n;
            role = r;
            email = e;
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<UserStat> list;

        UserAdapter(List<UserStat> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_order, parent, false);
            return new UserViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserStat user = list.get(position);

            // Set Nama & Email secara direct (Ini akan hilangkan format RM jika ada)
            holder.tvName.setText(user.name);
            holder.tvEmail.setText(user.email); // Dia akan tunjuk email@gmail.com, bukan RM email
            holder.tvRole.setText(user.role);

            // Tukar warna mengikut role supaya senang beza
            if ("Seller".equalsIgnoreCase(user.role)) {
                holder.tvRole.setTextColor(Color.parseColor("#8a2128")); // Merah untuk Seller
            } else {
                holder.tvRole.setTextColor(Color.parseColor("#4C4CAF")); // Biru untuk Buyer
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvRole, tvName, tvEmail;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                // 1. Mapping ID yang ada dalam XML
                tvRole = itemView.findViewById(R.id.tv_order_id);
                tvName = itemView.findViewById(R.id.tv_order_customer);
                tvEmail = itemView.findViewById(R.id.tv_order_amount);

                // 2. Sembunyikan Status (Bucu kanan atas)
                View status = itemView.findViewById(R.id.tv_order_status);
                if (status != null) status.setVisibility(View.GONE);

                // 3. Sembunyikan bahagian "Earn (10%)" (Seluruh bekas kanan)
                // Kita cari melalui TextView nilainya, kemudian ambil "Parent" (LinearLayout) dia
                View commissionValue = itemView.findViewById(R.id.tv_admin_commission);
                if (commissionValue != null && commissionValue.getParent() instanceof View) {
                    ((View) commissionValue.getParent()).setVisibility(View.GONE);
                }

                // 4. Sembunyikan Label "Total Amount"
                // Disebabkan label itu tiada ID, kita cari Parent kepada tvEmail
                // Parent kepada tvEmail ialah LinearLayout yang mengandungi tulisan "Total Amount"
                if (tvEmail != null && tvEmail.getParent() instanceof View) {
                    View container = (View) tvEmail.getParent();

                    // Kita tak boleh GONE-kan container sebab nanti Email pun hilang.
                    // Jadi kita cari anak pertama (TextView "Total Amount") dan sorokkan.
                    if (container instanceof ViewGroup) {
                        ViewGroup vg = (ViewGroup) container;
                        if (vg.getChildCount() > 0) {
                            vg.getChildAt(0).setVisibility(View.GONE); // Sorokkan label "Total Amount"
                        }
                    }
                }
            }
        }
    }
}