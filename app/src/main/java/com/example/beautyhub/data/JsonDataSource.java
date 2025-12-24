package com.example.beautyhub.data;

import android.content.Context;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Promotion;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JsonDataSource {

    private final Context context;
    private final Gson gson;

    public JsonDataSource(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public List<Product> loadBaseProducts() {
        try {
            String json = loadJsonFromAssets("base_products.json");
            Type productListType = new TypeToken<List<Product>>(){}.getType(); // Fixed: Using Gson's TypeToken
            List<Product> products = gson.fromJson(json, productListType);

            if (products != null) {
                // Mark semua sebagai preloaded
                for (Product product : products) {
                    product.setPreloaded(true);
                }
                return products;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<Promotion> loadPromotions() {
        try {
            String json = loadJsonFromAssets("promotions.json");
            Type promotionListType = new TypeToken<List<Promotion>>(){}.getType(); // Fixed: Using Gson's TypeToken
            return gson.fromJson(json, promotionListType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private String loadJsonFromAssets(String fileName) throws IOException {
        InputStream is = context.getAssets().open(fileName);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }
}