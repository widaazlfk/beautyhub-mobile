package com.example.beautyhub.admin;

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
// CORRECTED: Import the singular 'Product' model class
import com.example.beautyhub.models.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    // CORRECTED: The list should hold 'Product' objects
    private List<Product> productList;
    private final ProductActionListener actionListener;

    public interface ProductActionListener {
        // CORRECTED: The parameter should be the singular 'Product'
        void onProductAction(Product product);
    }

    // CORRECTED: The constructor now accepts a List of 'Product'
    public ProductAdapter(List<Product> productList, ProductActionListener actionListener) {
        this.productList = productList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.a_item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        // CORRECTED: Get a singular 'Product' object from the list
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    // CORRECTED: The update method now accepts a List of 'Product'
    public void updateList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    // ViewHolder class
    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView tvProductName, tvProductBrand, tvProductPrice, tvProductStock;
        private final TextView tvProductCategory, tvProductStatus, tvSellerName;
        private final ImageView ivActionMenu;
        private final Context context;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            this.context = itemView.getContext();

            productImage = itemView.findViewById(R.id.iv_product_image);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvProductBrand = itemView.findViewById(R.id.tv_product_brand);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
            tvProductStock = itemView.findViewById(R.id.tv_product_stock);
            tvProductCategory = itemView.findViewById(R.id.tv_product_category);
            tvProductStatus = itemView.findViewById(R.id.tv_product_status);
            tvSellerName = itemView.findViewById(R.id.tv_seller_name);
            ivActionMenu = itemView.findViewById(R.id.iv_action_menu);
        }

        // CORRECTED: The bind method now accepts a singular 'Product' object
        public void bind(final Product product) {
            tvProductName.setText(product.getName());
            tvProductBrand.setText(product.getBrand());
            tvProductPrice.setText(String.format("RM %.2f", product.getPrice()));
            tvProductCategory.setText(product.getCategory());
            tvSellerName.setText("By: " + product.getSellerName());

            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                String imageUrl = product.getImageUrls().get(0);
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.product_placeholder)
                        .error(R.drawable.product_placeholder)
                        .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.product_placeholder);
            }


            if (product.isActive()) {
                tvProductStatus.setText("ACTIVE");
                tvProductStatus.setBackgroundResource(R.drawable.badge_active);
            } else {
                tvProductStatus.setText("INACTIVE");
                tvProductStatus.setBackgroundResource(R.drawable.badge_inactive);
            }

            ivActionMenu.setOnClickListener(v -> actionListener.onProductAction(product));
            itemView.setOnClickListener(v -> actionListener.onProductAction(product));
        }
    }
}
