package com.vendra.cart;

import com.vendra.account.AccountService;
import com.vendra.common.ApiException;
import com.vendra.domain.*;
import com.vendra.dto.Dtos.*;
import com.vendra.order.PricingService;
import com.vendra.repo.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Multi-shop cart: one open cart per customer, items may span many shops. */
@Service
public class CartService {

  private final CartRepository carts;
  private final CartItemRepository items;
  private final ProductRepository products;
  private final ProductVariantRepository variants;
  private final ShopRepository shops;
  private final AccountService account;

  public CartService(
      CartRepository carts,
      CartItemRepository items,
      ProductRepository products,
      ProductVariantRepository variants,
      ShopRepository shops,
      AccountService account) {
    this.carts = carts;
    this.items = items;
    this.products = products;
    this.variants = variants;
    this.shops = shops;
    this.account = account;
  }

  @Transactional
  Cart myCart() {
    UUID uid = account.currentUserId();
    return carts.findByUserId(uid)
        .orElseGet(
            () -> {
              Cart c = new Cart();
              c.setUserId(uid);
              return carts.save(c);
            });
  }

  @Transactional
  public CartDto add(AddToCartRequest req) {
    Cart cart = myCart();
    Product p = products.findById(req.productId()).orElseThrow(() -> ApiException.notFound("Product"));
    if (!p.isActive()) throw ApiException.badRequest("Product is unavailable");
    int available = effectiveStock(p, req.variantId());
    List<CartItem> existing = items.findByCartId(cart.getId());
    Optional<CartItem> match =
        existing.stream()
            .filter(i -> i.getProductId().equals(req.productId())
                && Objects.equals(i.getVariantId(), req.variantId()))
            .findFirst();
    int newQty = match.map(CartItem::getQuantity).orElse(0) + Math.max(1, req.quantity());
    if (newQty > available) throw ApiException.badRequest("Only " + available + " in stock");

    CartItem item = match.orElseGet(CartItem::new);
    item.setCartId(cart.getId());
    item.setProductId(p.getId());
    item.setVariantId(req.variantId());
    item.setShopId(p.getShopId());
    item.setQuantity(newQty);
    items.save(item);
    return view();
  }

  @Transactional
  public CartDto updateItem(String itemId, int quantity) {
    CartItem item = items.findById(itemId).orElseThrow(() -> ApiException.notFound("Cart item"));
    assertOwned(item);
    Product p = products.findById(item.getProductId()).orElseThrow(() -> ApiException.notFound("Product"));
    if (quantity > effectiveStock(p, item.getVariantId()))
      throw ApiException.badRequest("Insufficient stock");
    item.setQuantity(quantity);
    items.save(item);
    return view();
  }

  @Transactional
  public CartDto removeItem(String itemId) {
    CartItem item = items.findById(itemId).orElseThrow(() -> ApiException.notFound("Cart item"));
    assertOwned(item);
    items.delete(item);
    return view();
  }

  @Transactional
  public void clear() {
    items.deleteByCartId(myCart().getId());
  }

  public CartDto view() {
    Cart cart = myCart();
    Map<String, String> shopNames = new HashMap<>();
    Map<String, List<CartItemDto>> byShop = new LinkedHashMap<>();
    BigDecimal grand = BigDecimal.ZERO;
    int count = 0;

    for (CartItem i : items.findByCartId(cart.getId())) {
      Product p = products.findById(i.getProductId()).orElse(null);
      if (p == null) continue;
      BigDecimal unit = unitPrice(p, i.getVariantId());
      BigDecimal line = PricingService.lineTotal(unit, i.getQuantity());
      grand = grand.add(line);
      count += i.getQuantity();
      String shopName =
          shopNames.computeIfAbsent(p.getShopId(),
              id -> shops.findById(id).map(Shop::getName).orElse("Shop"));
      String vName = i.getVariantId() == null ? null
          : variants.findById(i.getVariantId()).map(ProductVariant::getName).orElse(null);
      CartItemDto dto =
          new CartItemDto(i.getId(), p.getId(), i.getVariantId(), p.getShopId(), shopName,
              vName == null ? p.getName() : p.getName() + " — " + vName, p.getImageUrl(),
              unit, i.getQuantity(), line, effectiveStock(p, i.getVariantId()));
      byShop.computeIfAbsent(p.getShopId(), k -> new ArrayList<>()).add(dto);
    }

    List<CartShopGroupDto> groups = new ArrayList<>();
    for (var e : byShop.entrySet()) {
      BigDecimal sub = e.getValue().stream()
          .map(CartItemDto::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
      groups.add(new CartShopGroupDto(e.getKey(), shopNames.get(e.getKey()),
          e.getValue(), PricingService.money(sub)));
    }
    return new CartDto(cart.getId(), groups, PricingService.money(grand), count);
  }

  // ---- helpers ----
  public BigDecimal unitPrice(Product p, String variantId) {
    if (variantId != null) {
      var v = variants.findById(variantId);
      if (v.isPresent() && v.get().getPrice() != null) return v.get().getPrice();
    }
    return p.getPrice();
  }

  public int effectiveStock(Product p, String variantId) {
    if (variantId != null) {
      return variants.findById(variantId).map(ProductVariant::getStock).orElse(0);
    }
    return p.getStock();
  }

  private void assertOwned(CartItem item) {
    Cart cart = myCart();
    if (!item.getCartId().equals(cart.getId())) throw ApiException.forbidden("Not your cart item");
  }
}
