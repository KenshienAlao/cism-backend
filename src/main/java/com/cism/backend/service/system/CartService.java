package com.cism.backend.service.system;

import java.util.List;
import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cism.backend.dto.system.cart.CartRequest;
import com.cism.backend.dto.system.cart.CartResponse;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.stalls.ItemVariationsModel;
import com.cism.backend.model.stalls.StallItemModel;
import com.cism.backend.model.system.review.CartModel;
import com.cism.backend.model.users.AuthModel;
import com.cism.backend.repository.admin.CreateStallRepository;
import com.cism.backend.repository.stalls.ItemvariationsRepository;
import com.cism.backend.repository.stalls.StallItemRepository;
import com.cism.backend.repository.system.CartRepository;
import com.cism.backend.repository.users.RegisterRepository;
import com.cism.backend.util.CurrentUserLicence;

import jakarta.transaction.Transactional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private RegisterRepository registerRepository;

    @Autowired
    private CreateStallRepository createStallRepository;

    @Autowired
    private StallItemRepository stallItemRepository;

    @Autowired
    private ItemvariationsRepository itemvariationsRepository;

    @Autowired
    private CurrentUserLicence currentUserLicence;

    @Transactional
    public List<CartResponse> getCart() throws Exception {
        return cartRepository.findByUsersId(getCurrentUser().getId()).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Transactional
    public CartResponse addItemToCart(CartRequest request) throws Exception {
        if (request.quantity() <= 0) {
            throw new BadrequestException("Quantity must be greater than 0", "INVALID_QUANTITY");
        }

        AuthModel user = getCurrentUser();
        StallModel stall = createStallRepository.findById(request.stallId())
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        StallItemModel item = stallItemRepository.findById(request.stallItemId())
                .orElseThrow(() -> new BadrequestException("Item not found", "ITEM_NOT_FOUND"));

        ItemVariationsModel variation = null;
        if (request.variationId() != null && request.variationId() != 0) {
            variation = itemvariationsRepository.findById(request.variationId())
                    .orElseThrow(() -> new BadrequestException("Variation not found", "VARIATION_NOT_FOUND"));
        }

        CartModel cart = cartRepository
                .findByUsersIdAndStallItemIdAndVariationId(user.getId(), item.getId(),
                        variation != null ? variation.getId() : null)
                .orElse(null);

        if (cart != null) {
            cart.setQuantity(cart.getQuantity() + request.quantity());
        } else {
            cart = CartModel.builder()
                    .users(user)
                    .stall(stall)
                    .stallItem(item)
                    .variation(variation)
                    .quantity(request.quantity())
                    .build();
        }

        return mapToResponseDto(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateCartItemQuantity(Long cartId, Integer quantity) throws Exception {
        if (quantity <= 0) {
            throw new BadrequestException("Quantity must be greater than 0", "INVALID_QUANTITY");
        }
        CartModel cart = getCartAndVerifyOwnership(cartId, getCurrentUser());
        cart.setQuantity(quantity);
        return mapToResponseDto(cartRepository.save(cart));
    }

    @Transactional
    public String deleteCartItem(Long cartId) throws Exception {
        cartRepository.delete(getCartAndVerifyOwnership(cartId, getCurrentUser()));
        return "Cart item deleted";
    }

    @Transactional
    public String deleteAllCart() throws Exception {
        cartRepository.deleteAll(cartRepository.findByUsersId(getCurrentUser().getId()));
        return "All cart items deleted";
    }

    private AuthModel getCurrentUser() {
        String email = currentUserLicence.getCurrentUserEmail();
        return registerRepository.findByEmail(email)
                .orElseThrow(() -> new BadrequestException("User not found", "USER_NOT_FOUND"));
    }

    private CartModel getCartAndVerifyOwnership(Long cartId, AuthModel user) {
        CartModel cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new BadrequestException("Cart item not found", "CART_ITEM_NOT_FOUND"));

        if (!cart.getUsers().getId().equals(user.getId())) {
            throw new BadrequestException("Unauthorized", "UNAUTHORIZED");
        }
        return cart;
    }

    private CartResponse mapToResponseDto(CartModel entity) {
        String itemName = entity.getStallItem().getName();
        ItemVariationsModel variation = entity.getVariation();

        if (variation != null && variation.getName() != null
                && !variation.getName().isEmpty()
                && !variation.getName().equalsIgnoreCase("default")) {
            itemName += " - " + variation.getName();
        }

        String image = (variation != null && variation.getImage() != null) ? variation.getImage()
                : entity.getStallItem().getImage();

        String stallName = (entity.getStall() != null && entity.getStall().getUserList() != null
                && !entity.getStall().getUserList().isEmpty())
                        ? entity.getStall().getUserList().get(0).getName()
                        : "";

        BigDecimal price = (variation != null) ? variation.getPrice() : entity.getStallItem().getPrice();

        return new CartResponse(
                entity.getId(),
                entity.getStallItem().getId(),
                variation != null ? variation.getId() : null,
                itemName,
                price,
                image,
                stallName,
                entity.getQuantity());
    }
}
