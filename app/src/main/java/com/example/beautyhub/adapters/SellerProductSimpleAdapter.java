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

public class SellerProductSimpleAdapter extends RecyclerView.Adapter<SellerProductSimpleAdapter.ViewHolder> {

    private Context context;
    private List<Product> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public SellerProductSimpleAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_seller_product_simple, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateList(List<Product> newList) {
        productList.clear();
        productList.addAll(newList);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName;
        TextView tvProductPrice;
        TextView tvStock;
        TextView tvSoldCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.iv_product_image);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
            tvStock = itemView.findViewById(R.id.tv_stock);
            tvSoldCount = itemView.findViewById(R.id.tv_sold_count);
        }

        public void bind(Product product) {
            tvProductName.setText(product.getName());

            // Format price
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
            tvProductPrice.setText(format.format(product.getPrice()));

            // Stock status
            if (product.getStock() > 0) {
                tvStock.setText("In Stock: " + product.getStock());
                tvStock.setTextColor(context.getResources().getColor(R.color.green_600));
            } else {
                tvStock.setText("Out of Stock");
                tvStock.setTextColor(context.getResources().getColor(R.color.red_500));
            }

            // Sold count
            if (product.getSoldCount() > 0) {
                tvSoldCount.setText(product.getSoldCount() + " sold");
                tvSoldCount.setVisibility(View.VISIBLE);
            } else {
                tvSoldCount.setVisibility(View.GONE);
            }

            // Load image
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                Glide.with(context)
                        .load(product.getImageUrls().get(0))
                        .placeholder(R.drawable.product_placeholder)
                        .error(R.drawable.product_placeholder)
                        .into(ivProductImage);
            }
        }
    }
}