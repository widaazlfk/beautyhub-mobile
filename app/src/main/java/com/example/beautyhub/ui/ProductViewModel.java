package com.example.beautyhub.ui;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.beautyhub.data.ProductRepository;
import com.example.beautyhub.models.Product;
// --- PERUBAHAN 1: Padam import Variant ---
// import com.example.beautyhub.models.Variant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductViewModel extends AndroidViewModel {

    private final ProductRepository productRepository;

    // LiveData untuk UI
    private final MutableLiveData<List<Product>> productsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Product>> productsAsMapLiveData = new MutableLiveData<>();
    private final MutableLiveData<Product> productLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> successMessageLiveData = new MutableLiveData<>();

    public ProductViewModel(Application application) {
        super(application);
        productRepository = new ProductRepository(application);
    }

    // ==================== GETTERS ====================
    public LiveData<List<Product>> getProducts() { return productsLiveData; }
    public LiveData<Map<String, Product>> getProductsAsMap() { return productsAsMapLiveData; }
    public LiveData<Product> getProduct() { return productLiveData; }
    public LiveData<Boolean> getLoading() { return loadingLiveData; }
    public LiveData<String> getError() { return errorLiveData; }
    public LiveData<String> getSuccessMessage() { return successMessageLiveData; }

    // ==================== PUBLIC METHODS ====================

    public void loadAllProducts() {
        loadingLiveData.setValue(true);
        productRepository.getAllProducts(new ProductRepository.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                // Cipta Map daripada senarai produk
                Map<String, Product> productMap = new HashMap<>();
                for (Product product : products) {
                    // Pastikan productId tidak null sebelum dimasukkan ke dalam map
                    if (product.getProductId() != null) {
                        productMap.put(product.getProductId(), product);
                    }
                }

                // Hantar data ke kedua-dua LiveData
                productsLiveData.postValue(products);
                productsAsMapLiveData.postValue(productMap);
                loadingLiveData.postValue(false);

                android.util.Log.d("ViewModelDebug", "onSuccess: Products loaded into List and Map. Count: " + products.size());
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("ViewModelDebug", "onError: " + message);
                loadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }


    public void loadProductById(String productId) {
        loadingLiveData.setValue(true);
        productRepository.getProductById(productId, new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess(Product product) {
                loadingLiveData.postValue(false);
                productLiveData.postValue(product);
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }

    // --- PERUBAHAN 2: Permudahkan kaedah addSellerProduct ---
    public void addSellerProduct(Product product) {
        loadingLiveData.setValue(true);
        // Panggil kaedah repository yang telah dikemas kini
        productRepository.addSellerProduct(product, new ProductRepository.StringCallback() {
            @Override
            public void onSuccess(String productId) {
                loadingLiveData.postValue(false);
                successMessageLiveData.postValue("Product added successfully!");
                // Anda mungkin mahu memuat semula senarai produk di sini jika perlu
                loadAllProducts();
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue("Failed to add product: " + message);
            }
        });
    }

    public void loadProductsBySeller(String sellerId) {
        loadingLiveData.setValue(true);
        productRepository.getProductsBySeller(sellerId, new ProductRepository.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                loadingLiveData.postValue(false);
                productsLiveData.postValue(products);
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }

    public void searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadAllProducts();
            return;
        }

        loadingLiveData.setValue(true);
        productRepository.searchProducts(query, new ProductRepository.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                loadingLiveData.postValue(false);
                productsLiveData.postValue(products);
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }

    public void loadProductsByCategory(String category) {
        loadingLiveData.setValue(true);
        productRepository.getProductsByCategory(category, new ProductRepository.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                loadingLiveData.postValue(false);
                productsLiveData.postValue(products);
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }

    public void loadFeaturedProducts(int limit) {
        loadingLiveData.setValue(true);
        productRepository.getFeaturedProducts(limit, new ProductRepository.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                loadingLiveData.postValue(false);
                productsLiveData.postValue(products);
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }

    public void updateStock(String productId, int newStock) {
        loadingLiveData.setValue(true);
        productRepository.updateStock(productId, newStock, new ProductRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                loadingLiveData.postValue(false);
                successMessageLiveData.postValue("Stock updated successfully!");
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue("Failed to update stock: " + message);
            }
        });
    }
}
