package com.example.beautyhub.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.models.NotificationModel;
import com.example.beautyhub.buyer.OrderDetailsActivity; // Pastikan path ini betul
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.HolderNotification> {

    private final Context context;
    private final List<NotificationModel> notificationList;

    public NotificationAdapter(Context context, List<NotificationModel> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public HolderNotification onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification_row, parent, false);
        return new HolderNotification(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderNotification holder, int position) {
        NotificationModel model = notificationList.get(position);

        // 1. Set Teks
        holder.titleTv.setText(model.getTitle());

        if (model.getProductName() != null && !model.getProductName().isEmpty()) {
            holder.productNameTv.setVisibility(View.VISIBLE);
            holder.productNameTv.setText(model.getProductName());
            holder.productNameTv.setTextColor(Color.parseColor("#800000"));
        } else {
            holder.productNameTv.setVisibility(View.GONE);
        }

        if (model.getSellerName() != null && !model.getSellerName().isEmpty()) {
            holder.sellerNameTv.setVisibility(View.VISIBLE);
            holder.sellerNameTv.setText(model.getSellerName());
        } else {
            holder.sellerNameTv.setVisibility(View.GONE);
        }

        // 2. Glide Gambar
        Glide.with(context)
                .load(model.getProductImageUrl())
                .placeholder(R.drawable.product_placeholder)
                .error(R.drawable.product_placeholder)
                .centerCrop()
                .into(holder.productImageIv);

        // 3. Format Masa
        long time = model.getTimestamp();
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        holder.timeTv.setText(timeAgo);

        // 4. Logik Unread UI
        if (model.isUnread()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF0F0"));
            holder.unreadIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
            holder.unreadIndicator.setVisibility(View.GONE);
        }

        // 5. LOGIK KLIK (Navigasi ke Order Details)
        // 5. LOGIK KLIK (Navigasi Berbeza untuk Buyer & Seller)
        holder.itemView.setOnClickListener(v -> {
            // A. Mark as Read di Firebase
            if (model.isUnread()) {
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null) {
                    FirebaseDatabase.getInstance().getReference("Notifications")
                            .child(uid)
                            .child(model.getId())
                            .child("unread").setValue(false);
                }
                model.setUnread(false);
                notifyItemChanged(position);
            }

            // B. Tentukan Halaman Navigasi
            Intent intent;
            if ("NewOrder".equals(model.getType())) {
                // Jika notifikasi jenis NewOrder, hantar ke SellerOrderDetailsActivity
                intent = new Intent(context, com.example.beautyhub.seller.SellerOrderDetailActivity.class);
            } else {
                // Selain itu (Contoh: "Order"), hantar ke Buyer OrderDetailsActivity
                intent = new Intent(context, com.example.beautyhub.buyer.OrderDetailsActivity.class);
            }

            intent.putExtra("ORDER_ID", model.getOrderId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class HolderNotification extends RecyclerView.ViewHolder {
        TextView titleTv, timeTv, productNameTv, sellerNameTv;
        ImageView productImageIv;
        View unreadIndicator;

        public HolderNotification(@NonNull View itemView) {
            super(itemView);
            titleTv = itemView.findViewById(R.id.notification_title);
            timeTv = itemView.findViewById(R.id.notification_time);
            productNameTv = itemView.findViewById(R.id.notification_product_name);
            sellerNameTv = itemView.findViewById(R.id.notification_seller_info);
            productImageIv = itemView.findViewById(R.id.notification_product_image);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }
    }
}