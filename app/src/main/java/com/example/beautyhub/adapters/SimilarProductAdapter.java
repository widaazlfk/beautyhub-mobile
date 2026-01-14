package com.example.beautyhub.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.beautyhub.R;
import com.example.beautyhub.buyer.ProductDetailActivity;
import com.example.beautyhub.models.Product;

import java.util.List;
import java.util.Locale;

public class SimilarProductAdapter extends RecyclerView.Adapter<SimilarProductAdapter.ViewHolder> {

    private final Context context;
    private final List<Product> similarProductList;

    public SimilarProductAdapter(Context context, List<Product> similarProductList) {
        this.context = context;
        this.similarProductList = similarProductList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_similar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = similarProductList.get(position);

        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.format(Locale.US, "RM%.2f", product.getFinalPrice()));

        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrls().get(0))
                    .placeholder(R.color.gray_light)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.color.gray_light); // Imej lalai
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getProductId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return similarProductList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.iv_product_image_similar);
            productName = itemView.findViewById(R.id.tv_product_name_similar);
            productPrice = itemView.findViewById(R.id.tv_product_price_similar);
        }
    }
}
