package com.example.beautyhub.ui;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.beautyhub.data.ProductRepository;
import com.example.beautyhub.models.Product;
import com.example.beautyhub.models.Variant;
import java.util.List;

public class ProductViewModel extends AndroidViewModel {

    private final ProductRepository productRepository;

    // LiveData untuk UI
    private final MutableLiveData<List<Product>> productsLiveData = new MutableLiveData<>();
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
    public LiveData<Product> getProduct() { return productLiveData; }
    public LiveData<Boolean> getLoading() { return loadingLiveData; } // FIXED: Changed from isLoading() to getLoading()
    public LiveData<String> getError() { return errorLiveData; }
    public LiveData<String> getSuccessMessage() { return successMessageLiveData; }

    // ==================== PUBLIC METHODS ====================

    // Di dalam C:/Users/widaa/beautyhub/app/src/main/java/com/example/beautyhub/ui/ProductViewModel.java

    public void loadAllProducts() {
        loadingLiveData.setValue(true);
        productRepository.getAllProducts(new ProductRepository.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                // ▼▼▼ TAMBAH LOG INI ▼▼▼
                android.util.Log.d("ViewModelDebug", "onSuccess dipanggil. Bilangan produk diterima: " + products.size());
                if (!products.isEmpty()) {
                    android.util.Log.d("ViewModelDebug", "Produk pertama: " + products.get(0).getName());
                }
                // ▲▲▲ TAMBAH LOG INI ▲▲▲

                loadingLiveData.postValue(false);
                productsLiveData.postValue(products);
            }

            @Override
            public void onError(String message) {
                // ▼▼▼ TAMBAH LOG INI ▼▼▼
                android.util.Log.e("ViewModelDebug", "onError dipanggil. Mesej: " + message);
                // ▲▲▲ TAMBAH LOG INI ▲▲▲

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

    public void addSellerProduct(Product product, List<Variant> variants) {
        loadingLiveData.setValue(true);
        productRepository.addSellerProduct(product, variants, new ProductRepository.StringCallback() {
            @Override
            public void onSuccess(String productId) {
                loadingLiveData.postValue(false);
                successMessageLiveData.postValue("Product added successfully!");
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