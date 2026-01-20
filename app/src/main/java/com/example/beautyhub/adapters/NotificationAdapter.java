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

        // --- 1. SET TITLE DENGAN SHORTENED ORDER ID ---
        String originalTitle = model.getTitle();
        String processedTitle = originalTitle;

        if (originalTitle != null && originalTitle.contains("#")) {
            try {
                int hashIndex = originalTitle.indexOf("#");
                String prefix = originalTitle.substring(0, hashIndex + 1);
                String idPart = originalTitle.substring(hashIndex + 1).trim();

                if (!idPart.isEmpty()) {
                    String cleanId = idPart.replace("-", "");
                    String shortId = cleanId.substring(0, Math.min(cleanId.length(), 8)).toUpperCase();
                    processedTitle = prefix + " " + shortId;
                }
            } catch (Exception e) {
                processedTitle = originalTitle;
            }
        }
        holder.titleTv.setText(processedTitle);

        // --- 2. DISPLAY NAMA PRODUK & INFO SELLER/BUYER ---
        if (model.getProductName() != null && !model.getProductName().isEmpty()) {
            holder.productNameTv.setVisibility(View.VISIBLE);
            holder.productNameTv.setText(model.getProductName());
        } else {
            holder.productNameTv.setVisibility(View.GONE);
        }

        if ("ORDER_NEW".equals(model.getType())) {
            holder.sellerNameTv.setText("Buyer: " + (model.getBuyerName() != null ? model.getBuyerName() : "Customer"));
        } else {
            holder.sellerNameTv.setText(model.getSellerName() != null ? model.getSellerName() : "");
        }

        // --- 3. GAMBAR PRODUK ---
        Glide.with(context)
                .load(model.getProductImageUrl())
                .placeholder(R.drawable.product_placeholder)
                .error(R.drawable.product_placeholder)
                .centerCrop()
                .into(holder.productImageIv);

        // --- 4. FORMAT MASA ---
        long time = model.getTimestamp();
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        holder.timeTv.setText(timeAgo);

        // --- 5. UNREAD INDICATOR & BACKGROUND ---
        // Item akan berwarna merah cair jika belum diklik
        if (model.isUnread()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF0F0"));
            holder.unreadIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
            holder.unreadIndicator.setVisibility(View.GONE);
        }

        // --- 6. LOGIK KLIK: TANDA SEBAGAI BACA HANYA PADA ITEM YANG DITEKAN ---
        holder.itemView.setOnClickListener(v -> {
            if (model.isUnread()) {
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null && model.getId() != null) {
                    // Update Firebase: Tukar unread kepada false
                    FirebaseDatabase.getInstance().getReference("Notifications")
                            .child(uid)
                            .child(model.getId())
                            .child("unread").setValue(false);
                }
                // Update UI secara lokal serta-merta
                model.setUnread(false);
                notifyItemChanged(position);
            }

            // Navigasi ke Order Details yang sepadan
            Intent intent;
            String type = model.getType();
            String orderId = model.getOrderId() != null ? model.getOrderId() : "";

            if ("ORDER_NEW".equals(type) || "ORDER_COMPLETED".equals(type) || "ORDER_CANCELLED_BY_BUYER".equals(type)) {
                intent = new Intent(context, com.example.beautyhub.seller.SellerOrderDetailActivity.class);
            } else {
                intent = new Intent(context, com.example.beautyhub.buyer.OrderDetailsActivity.class);
            }

            intent.putExtra("ORDER_ID", orderId);
            context.startActivity(intent);
        });
    } // Penutup onBindViewHolder yang betul

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