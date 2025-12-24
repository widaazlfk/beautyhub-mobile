package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.models.CartItem; // Pastikan path model Anda benar

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CheckoutSummaryAdapter extends RecyclerView.Adapter<CheckoutSummaryAdapter.ViewHolder> {

    private final Context context;
    private final List<CartItem> itemList;

    public CheckoutSummaryAdapter(Context context, List<CartItem> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Menggunakan layout item_checkout_summary.xml yang telah Anda buat
        View view = LayoutInflater.from(context).inflate(R.layout.item_checkout_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = itemList.get(position);

        // Set data ke dalam view menggunakan ID dari layout Anda
        holder.itemName.setText(item.getProductName());
        holder.itemQuantity.setText("x " + item.getQuantity()); // Sesuai format di layout Anda "x 2"

        // Format harga sesuai mata uang Malaysia (Ringgit)
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
        holder.itemPrice.setText(currencyFormat.format(item.getPrice()));

        // Gunakan Glide untuk memuat gambar produk
        Glide.with(context)
                .load(item.getImageUrl()) // Menggunakan getter dari model CartItem
                .placeholder(R.drawable.product_placeholder) // Menggunakan placeholder dari layout Anda
                .error(R.drawable.product_placeholder) // Tampilkan placeholder jika ada error
                .into(holder.itemImage);
    }

    @Override
    public int getItemCount() {
        // Menghindari NullPointerException jika list kosong
        return itemList != null ? itemList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName, itemQuantity, itemPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // ▼▼▼ KOREKSI UTAMA ADA DI SINI ▼▼▼
            // Menghubungkan view dengan ID yang benar dari item_checkout_summary.xml
            itemImage = itemView.findViewById(R.id.iv_checkout_item_image);
            itemName = itemView.findViewById(R.id.tv_checkout_item_name);
            itemQuantity = itemView.findViewById(R.id.tv_checkout_item_quantity);
            itemPrice = itemView.findViewById(R.id.tv_checkout_item_price);
            // ▲▲▲ AKHIR DARI KOREKSI ▲▲▲
        }
    }
}
