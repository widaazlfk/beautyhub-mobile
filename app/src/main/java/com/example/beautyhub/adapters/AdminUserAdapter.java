package com.example.beautyhub.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.models.User;

import java.util.List;
import java.util.Locale;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private final Context context;
    private List<User> userList;
    private final OnUserActionListener listener;

    public interface OnUserActionListener {
        void onActionClick(User user);
    }

    public AdminUserAdapter(Context context, List<User> userList, OnUserActionListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.a_item_user, parent, false);
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
        this.userList = newList;
        notifyDataSetChanged();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvUsername, tvEmail, tvRole, tvStatus, tvRegistrationDate;
        private final ImageView ivActionMenu;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_user_username);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvRole = itemView.findViewById(R.id.tv_user_role);
            tvStatus = itemView.findViewById(R.id.tv_user_status);
            tvRegistrationDate = itemView.findViewById(R.id.tv_user_registration_date);
            ivActionMenu = itemView.findViewById(R.id.iv_user_action_menu);
        }

        public void bind(final User user, final OnUserActionListener listener) {
            // Sediakan nilai default jika data null
            tvUsername.setText(user.getUsername() != null ? user.getUsername() : "No Name");
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "No Email");

            // Pemeriksaan selamat untuk 'userType' bagi mengelak crash
            String role = user.getUserType();
            int roleColor;

            if (role != null) {
                tvRole.setText(role.toUpperCase(Locale.ROOT));
                switch (role) {
                    case "Admin":
                        roleColor = ContextCompat.getColor(context, R.color.role_admin_color);
                        break;
                    case "Seller":
                        roleColor = ContextCompat.getColor(context, R.color.role_seller_color);
                        break;
                    default: // Untuk "Buyer" atau lain-lain
                        roleColor = ContextCompat.getColor(context, R.color.role_customer_color);
                        break;
                }
            } else {
                tvRole.setText("N/A"); // Jika tiada userType
                roleColor = ContextCompat.getColor(context, R.color.grey);
            }

            // Tetapkan warna latar belakang
            GradientDrawable roleBackground = (GradientDrawable) tvRole.getBackground().mutate();
            roleBackground.setColor(roleColor);

            // Pemeriksaan selamat untuk status 'suspended'
            if (Boolean.TRUE.equals(user.isSuspended())) {
                tvStatus.setText("SUSPENDED");
                tvStatus.setTextColor(Color.parseColor("#D32F2F")); // Merah
            } else {
                tvStatus.setText("ACTIVE");
                tvStatus.setTextColor(Color.parseColor("#388E3C")); // Hijau
            }

            // Paparkan tarikh pendaftaran
            String registrationDate = user.getRegistrationDate();
            if (registrationDate != null && !registrationDate.isEmpty()) {
                tvRegistrationDate.setText("Joined: " + registrationDate);
            } else {
                tvRegistrationDate.setText("Joined: N/A");
            }

            ivActionMenu.setOnClickListener(v -> listener.onActionClick(user));
        }
    }
}
