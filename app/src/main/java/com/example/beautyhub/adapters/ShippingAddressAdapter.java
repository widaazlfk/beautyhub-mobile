package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;
import com.example.beautyhub.models.ShippingAddress;
import com.google.android.material.chip.Chip;

import java.util.List;

public class ShippingAddressAdapter extends RecyclerView.Adapter<ShippingAddressAdapter.AddressViewHolder> {

    private final Context context;
    private final List<ShippingAddress> addressList;
    private final OnAddressActionsListener listener;
    private final OnItemClickListener itemClickListener; // Listener untuk klik item

    // Interface untuk butang Edit/Delete
    public interface OnAddressActionsListener {
        void onEdit(ShippingAddress address);
        void onDelete(ShippingAddress address);
    }

    // Interface baru untuk klik pada keseluruhan item
    public interface OnItemClickListener {
        void onItemClick(ShippingAddress address);
    }

    // Ubah suai constructor untuk menerima kedua-dua listener
    public ShippingAddressAdapter(Context context, List<ShippingAddress> addressList,
                                  OnAddressActionsListener listener, OnItemClickListener itemClickListener) {
        this.context = context;
        this.addressList = addressList;
        this.listener = listener;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shipping_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        ShippingAddress currentAddress = addressList.get(position);

        // Set listener untuk keseluruhan item, panggil itemClickListener
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(currentAddress);
            }
        });

        holder.tvRecipientName.setText(currentAddress.getRecipientName());
        holder.tvPhoneNumber.setText(currentAddress.getPhoneNumber());

        String fullAddress = currentAddress.getStreet() + ", " +
                currentAddress.getCity() + ", " +
                currentAddress.getState() + " " +
                currentAddress.getZipcode();
        holder.tvFullAddress.setText(fullAddress);

        if (currentAddress.getAddressType() == null || currentAddress.getAddressType().isEmpty()) {
            holder.chipAddressType.setVisibility(View.GONE);
        } else {
            holder.chipAddressType.setVisibility(View.VISIBLE);
            holder.chipAddressType.setText(currentAddress.getAddressType());
        }

        if (currentAddress.isDefault()) {
            holder.chipDefaultIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.chipDefaultIndicator.setVisibility(View.GONE);
        }

        // Set listener untuk butang, panggil listener biasa
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(currentAddress);
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(currentAddress);
            }
        });
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        Chip chipAddressType, chipDefaultIndicator;
        TextView tvRecipientName, tvPhoneNumber, tvFullAddress;
        Button btnEdit, btnDelete;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            chipAddressType = itemView.findViewById(R.id.chip_address_type);
            chipDefaultIndicator = itemView.findViewById(R.id.chip_default_indicator);
            tvRecipientName = itemView.findViewById(R.id.tv_recipient_name);
            tvPhoneNumber = itemView.findViewById(R.id.tv_phone_number);
            tvFullAddress = itemView.findViewById(R.id.tv_full_address);
            btnEdit = itemView.findViewById(R.id.btn_edit_address);
            btnDelete = itemView.findViewById(R.id.btn_delete_address);
        }
    }
}
