package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    void addItemToCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> listCartItems();

    void cleanCart();

}
