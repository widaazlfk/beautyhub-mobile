package com.example.beautyhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.models.Product; // Pastikan path model ini benar

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MyProductsAdapter extends RecyclerView.Adapter<MyProductsAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final OnProductActionClickListener listener;

    // Interface untuk memberitahu Activity saat tombol ditekan
    public interface OnProductActionClickListener {
        void onEditClick(Product product);
        void onDeleteClick(Product product);
    }

    public MyProductsAdapter(Context context, List<Product> productList, OnProductActionClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_seller, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.productName.setText(product.getName());
        holder.productStock.setText("Stock: " + product.getStock());

        // Format harga
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
        holder.productPrice.setText(currencyFormat.format(product.getPrice()));

        // Muat gambar pertama dari daftar gambar produk
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrls().get(0))
                    .placeholder(R.drawable.product_placeholder)
                    .into(holder.productImage);
        } else {
            // Jika tidak ada gambar, tampilkan placeholder
            holder.productImage.setImageResource(R.drawable.product_placeholder);
        }

        // Set listener untuk tombol
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(product);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, productStock;
        Button btnEdit, btnDelete;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            // Menghubungkan view dengan ID dari item_product_seller.xml
            productImage = itemView.findViewById(R.id.iv_product_image_seller);
            productName = itemView.findViewById(R.id.tv_product_name_seller);
            productPrice = itemView.findViewById(R.id.tv_product_price_seller);
            productStock = itemView.findViewById(R.id.tv_product_stock_seller);
            btnEdit = itemView.findViewById(R.id.btn_edit_product);
            btnDelete = itemView.findViewById(R.id.btn_delete_product);
        }
    }
}
