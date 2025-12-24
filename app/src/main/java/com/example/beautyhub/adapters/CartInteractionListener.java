package com.example.beautyhub.adapters;

public interface CartInteractionListener {
    void onQuantityChanged(String cartItemId, int newQuantity);
    void onItemRemoved(String cartItemId);
}
