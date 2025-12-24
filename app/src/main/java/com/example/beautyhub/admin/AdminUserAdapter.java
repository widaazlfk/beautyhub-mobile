package com.example.beautyhub.admin;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
// Pastikan import merujuk kepada model 'User' (tunggal) yang betul.
import com.example.beautyhub.models.User;

import java.util.List;
import java.util.Locale;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    // 1. Kemas kini Interface untuk menggunakan model 'User' (tunggal).
    public interface OnUserActionsListener {
        void onUserClick(User user);
        void onUserMenuClick(User user, View anchorView);
    }

    private final Context context;
    private List<User> userList;
    private final OnUserActionsListener listener;

    // 2. Pastikan constructor menggunakan 'User' (tunggal).
    public AdminUserAdapter(Context context, List<User> userList, OnUserActionsListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Pastikan nama fail reka letak ini betul.
        View view = LayoutInflater.from(context).inflate(R.layout.a_item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        // Pembolehubah 'user' kini adalah dari jenis 'User'.
        User user = userList.get(position);
        holder.bind(user, listener, context);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // Kaedah untuk mengemas kini senarai pengguna.
    public void updateList(List<User> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar, ivUserActionMenu;
        TextView tvUsername, tvEmail, tvRole, tvStatus, tvRegistrationDate;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            // Inisialisasi semua view dari reka letak.
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            ivUserActionMenu = itemView.findViewById(R.id.iv_user_action_menu);
            tvUsername = itemView.findViewById(R.id.tv_user_username);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvRole = itemView.findViewById(R.id.tv_user_role);
            tvStatus = itemView.findViewById(R.id.tv_user_status);
            tvRegistrationDate = itemView.findViewById(R.id.tv_user_registration_date);
        }

        // 3. Kemas kini kaedah bind untuk menggunakan model 'User' dan kaedah yang betul.
        public void bind(final User user, final OnUserActionsListener listener, Context context) {
            // Guna kaedah getter yang betul dari model 'User'.
            tvUsername.setText(user.getUsername());
            tvEmail.setText(user.getEmail());
            tvRole.setText(user.getUserType().toUpperCase(Locale.ROOT));

            // Tetapkan status (Aktif/Digantung) berdasarkan boolean 'isSuspended'.
            // in your AdminUserAdapter.java file

            // ... inside the bind(...) method of UserViewHolder
            if (user.isSuspended()) {
                tvStatus.setText("SUSPENDED");
                // ↓↓↓ CHANGE THIS LINE ↓↓↓
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error_color)); // Warna Merah
            } else {
                tvStatus.setText("ACTIVE");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_accent));
            }
            //...


            // Paparkan tarikh pendaftaran (sebagai String).
            if (user.getRegistrationDate() != null && !user.getRegistrationDate().isEmpty()) {
                tvRegistrationDate.setText("Joined: " + user.getRegistrationDate());
                tvRegistrationDate.setVisibility(View.VISIBLE);
            } else {
                tvRegistrationDate.setVisibility(View.GONE);
            }

            // Tetapkan listener klik.
            itemView.setOnClickListener(v -> listener.onUserClick(user));
            ivUserActionMenu.setOnClickListener(v -> listener.onUserMenuClick(user, v));
        }
    }
}
