package com.cism.backend.controller.system;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.system.cart.CartRequest;
import com.cism.backend.dto.system.cart.CartResponse;
import com.cism.backend.service.system.CartService;

@RestController
@RequestMapping("/api/customer/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add-item-to-cart")
    public ResponseEntity<Api<CartResponse>> addItemToCartController(@RequestBody CartRequest entity) throws Exception {
        System.out.println("entity: " + entity);
        CartResponse success = cartService.addItemToCart(entity);
        return ResponseEntity.ok(Api.ok("Item added to cart", "CART_ADD_SUCCESS", success));
    }

    @GetMapping("/get-cart")
    public ResponseEntity<Api<List<CartResponse>>> getCartController() throws Exception {
        List<CartResponse> success = cartService.getCart();
        return ResponseEntity.ok(Api.ok("Cart retrieved successfully", "CART_GET_SUCCESS", success));
    }

    @DeleteMapping("/delete-cart-item/{cartId}")
    public ResponseEntity<Api<String>> deleteCartItemController(@PathVariable Long cartId) throws Exception {
        String success = cartService.deleteCartItem(cartId);
        return ResponseEntity.ok(Api.ok("Cart item deleted", "CART_DELETE_SUCCESS", success));
    }

    @PatchMapping("/update-cart-item/{cartId}")
    public ResponseEntity<Api<CartResponse>> updateCartItemQuantityController(
            @PathVariable Long cartId,
            @RequestParam Integer quantity) throws Exception {
        CartResponse success = cartService.updateCartItemQuantity(cartId, quantity);
        return ResponseEntity.ok(Api.ok("Cart item quantity updated", "CART_UPDATE_SUCCESS", success));
    }

    @DeleteMapping("/delete-all-cart")
    public ResponseEntity<Api<String>> deleteCartAllController() throws Exception {
        String success = cartService.deleteAllCart();
        return ResponseEntity.ok(Api.ok("Cart deleted successfully", "CART_DELETE_ALL_SUCCESS", success));
    }

}
