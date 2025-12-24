package com.example.beautyhub.admin;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beautyhub.R;

// Import model 'User' dari lokasi yang betul
import com.example.beautyhub.models.User;

import java.util.List;
import java.util.Locale;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onActionMenuClick(User user, View view);
    }

    private List<User> userList;
    private final OnUserActionListener listener;

    public UserAdapter(List<User> userList, OnUserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.a_item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateList(List<User> newList) {
        userList = newList;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUserName, tvUserEmail, tvUserRole, tvUserStatus, tvRegistrationDate;
        private final ImageView ivActionMenu;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_username);
            tvUserEmail = itemView.findViewById(R.id.tv_user_email);
            tvUserRole = itemView.findViewById(R.id.tv_user_role);
            tvUserStatus = itemView.findViewById(R.id.tv_user_status);
            tvRegistrationDate = itemView.findViewById(R.id.tv_user_registration_date);
            ivActionMenu = itemView.findViewById(R.id.iv_user_action_menu);
        }

        public void bind(final User user, final OnUserActionListener listener) {
            tvUserName.setText(user.getUsername());
            tvUserEmail.setText(user.getEmail());

            if (user.getUserType() != null) {
                tvUserRole.setText(user.getUserType().toUpperCase(Locale.ROOT));
            }

            if (user.isSuspended()) {
                tvUserStatus.setText("SUSPENDED");
                tvUserStatus.setTextColor(Color.RED);
            } else {
                tvUserStatus.setText("ACTIVE");
                tvUserStatus.setTextColor(Color.parseColor("#388E3C"));
            }

            // --- ▼▼▼ PERBAIKAN DI SINI ▼▼▼ ---
            // Terus gunakan String dari getRegistrationDate() tanpa pemformatan.
            // Ini lebih selamat dan mudah.
            String regDate = user.getRegistrationDate();
            if (regDate != null && !regDate.isEmpty()) {
                // Anda boleh memendekkan String tarikh jika perlu. Contoh ini mengambil 10 aksara pertama ("yyyy-MM-dd")
                String displayDate = regDate.length() > 10 ? regDate.substring(0, 10) : regDate;
                tvRegistrationDate.setText("Joined: " + displayDate);
                tvRegistrationDate.setVisibility(View.VISIBLE);
            } else {
                tvRegistrationDate.setVisibility(View.GONE);
            }
            // --- ▲▲▲ AKHIR PERBAIKAN ▲▲▲ ---

            ivActionMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionMenuClick(user, v);
                }
            });
        }
    }
}
