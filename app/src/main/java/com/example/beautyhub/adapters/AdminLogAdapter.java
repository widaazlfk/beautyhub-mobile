package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
// ▼▼▼ GUNAKAN MODEL AdminLog ▼▼▼
import com.example.beautyhub.models.AdminLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminLogAdapter extends RecyclerView.Adapter<AdminLogAdapter.LogViewHolder> {

    private final Context context;
    // ▼▼▼ GUNAKAN List<AdminLog> ▼▼▼
    private final List<AdminLog> logList;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault());

    // ▼▼▼ KEMAS KINI CONSTRUCTOR ▼▼▼
    public AdminLogAdapter(Context context, List<AdminLog> logList) {
        this.context = context;
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.log_item, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        // ▼▼▼ GUNAKAN OBJEK AdminLog ▼▼▼
        AdminLog log = logList.get(position);

        if (log == null) {
            return;
        }

        holder.tvLogAction.setText(log.getAction());
        holder.tvLogDetails.setText(log.getDetails());

        String formattedDate = sdf.format(new Date(log.getTimestamp()));
        holder.tvLogTimestamp.setText(formattedDate);

        setLogIcon(holder.ivLogIcon, log.getAction());
    }

    private void setLogIcon(ImageView imageView, String action) {
        int iconResId;
        int colorResId;

        if (action == null) action = "";

        if (action.contains("Login")) {
            iconResId = R.drawable.ic_login;
            colorResId = R.color.log_color_login;
        } else if (action.contains("Logout")) {
            iconResId = R.drawable.ic_logout;
            colorResId = R.color.log_color_logout;
        } else if (action.contains("Registration")) {
            iconResId = R.drawable.ic_person_add;
            colorResId = R.color.log_color_register;
        } else if (action.contains("Suspended") || action.contains("Deleted")) {
            iconResId = R.drawable.ic_block;
            colorResId = R.color.log_color_delete;
        } else if (action.contains("Reactivated") || action.contains("Changed")) {
            iconResId = R.drawable.ic_edit;
            colorResId = R.color.log_color_edit;
        } else if (action.contains("Product")) {
            iconResId = R.drawable.ic_product;
            colorResId = R.color.log_color_product;
        } else if (action.contains("Order")) {
            iconResId = R.drawable.ic_order;
            colorResId = R.color.log_color_order;
        } else {
            iconResId = R.drawable.ic_log_default;
            colorResId = R.color.grey;
        }

        imageView.setImageResource(iconResId);
        imageView.setColorFilter(ContextCompat.getColor(context, colorResId), android.graphics.PorterDuff.Mode.SRC_IN);
    }

    @Override
    public int getItemCount() {
        return logList != null ? logList.size() : 0;
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        ImageView ivLogIcon;
        TextView tvLogAction, tvLogDetails, tvLogTimestamp;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLogIcon = itemView.findViewById(R.id.iv_log_icon);
            tvLogAction = itemView.findViewById(R.id.tv_log_action);
            tvLogDetails = itemView.findViewById(R.id.tv_log_details);
            tvLogTimestamp = itemView.findViewById(R.id.tv_log_timestamp);
        }
    }
}
