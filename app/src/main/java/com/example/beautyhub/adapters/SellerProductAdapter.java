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
import com.example.beautyhub.models.Product;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SellerProductAdapter extends RecyclerView.Adapter<SellerProductAdapter.SellerProductViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final OnProductActionListener listener;

    public interface OnProductActionListener {
        void onProductClick(Product product);
        void onEditClick(Product product);
        void onDeleteClick(Product product, int position);
    }

    public SellerProductAdapter(Context context, List<Product> productList, OnProductActionListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SellerProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_seller_product, parent, false);
        return new SellerProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SellerProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product, listener, position);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < productList.size()) {
            productList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, productList.size());
        }
    }

    public void updateList(List<Product> newList) {
        productList.clear();
        productList.addAll(newList);
        notifyDataSetChanged();
    }

    public void addItem(Product product) {
        productList.add(product);
        notifyItemInserted(productList.size() - 1);
    }

    public void updateItem(int position, Product product) {
        productList.set(position, product);
        notifyItemChanged(position);
    }

    static class SellerProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage, btnEdit, btnDelete;
        TextView tvProductName, tvProductPrice, tvStockStatus;

        public SellerProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.iv_product_image);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
            tvStockStatus = itemView.findViewById(R.id.tv_stock_status);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(final Product product, final OnProductActionListener listener, final int position) {
            tvProductName.setText(product.getName());

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
            tvProductPrice.setText(format.format(product.getPrice()));

            updateStockStatus(product.getStock());

            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(product.getImageUrls().get(0))
                        .placeholder(R.drawable.product_placeholder)
                        .error(R.drawable.product_placeholder)
                        .centerCrop()
                        .into(ivProductImage);
            }

            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onProductClick(product));
                btnEdit.setOnClickListener(v -> listener.onEditClick(product));
                btnDelete.setOnClickListener(v -> listener.onDeleteClick(product, position));
            }
        }

        private void updateStockStatus(int stock) {
            if (stock > 10) {
                tvStockStatus.setText("Stock: " + stock);
                tvStockStatus.setBackgroundResource(R.drawable.bg_stock_high);
                tvStockStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.green_600));
            } else if (stock > 0) {
                tvStockStatus.setText("Low Stock: " + stock);
                tvStockStatus.setBackgroundResource(R.drawable.bg_stock_low);
                tvStockStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.orange_600));
            } else {
                tvStockStatus.setText("Out of Stock");
                tvStockStatus.setBackgroundResource(R.drawable.bg_stock_out);
                tvStockStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.red_500));
            }
        }
    }
}