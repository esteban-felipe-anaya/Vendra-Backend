package com.vendra.shop;

import com.vendra.account.AccountService;
import com.vendra.common.ApiException;
import com.vendra.domain.Product;
import com.vendra.domain.ProductImage;
import com.vendra.domain.Shop;
import com.vendra.dto.Dtos.MessageResponse;
import com.vendra.dto.Dtos.ProductDto;
import com.vendra.repo.ProductImageRepository;
import com.vendra.repo.ProductRepository;
import com.vendra.repo.ProductVariantRepository;
import com.vendra.shop.ShopDtos.ProductImageRequest;
import com.vendra.shop.ShopDtos.ProductRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Product CRUD scoped to the authenticated merchant's own shop. */
@RestController
@RequestMapping("/api/v1/shop/products")
@PreAuthorize("hasRole('shop')")
@Tag(name = "Shop products", description = "Merchant product catalog management")
public class ShopProductController {

  private final AccountService account;
  private final ProductRepository products;
  private final ProductImageRepository images;
  private final ProductVariantRepository variants;

  public ShopProductController(
      AccountService account,
      ProductRepository products,
      ProductImageRepository images,
      ProductVariantRepository variants) {
    this.account = account;
    this.products = products;
    this.images = images;
    this.variants = variants;
  }

  @GetMapping
  @Operation(summary = "List my shop's products")
  public List<ProductDto> list() {
    Shop shop = account.currentShop();
    return products.findByShopId(shop.getId()).stream()
        .map(p -> ShopDtos.toProductDto(p, shop.getName()))
        .toList();
  }

  @PostMapping
  @Operation(summary = "Create a product in my shop")
  @Transactional
  public ProductDto create(@Valid @RequestBody ProductRequest req) {
    Shop shop = account.currentShop();
    Product p = new Product();
    p.setShopId(shop.getId());
    apply(p, req);
    p.setSlug(Slugs.slugify(req.name()));
    Product saved = products.save(p);
    saveExtraImages(saved.getId(), req.images());
    return ShopDtos.toProductDto(saved, shop.getName());
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update one of my products")
  @Transactional
  public ProductDto update(@PathVariable String id, @Valid @RequestBody ProductRequest req) {
    Shop shop = account.currentShop();
    Product p = requireOwnProduct(id, shop);
    apply(p, req);
    p.setSlug(Slugs.slugify(req.name()));
    return ShopDtos.toProductDto(products.save(p), shop.getName());
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete one of my products")
  @Transactional
  public MessageResponse delete(@PathVariable String id) {
    Shop shop = account.currentShop();
    Product p = requireOwnProduct(id, shop);
    // Clean up dependent rows to avoid orphans.
    variants.deleteAll(variants.findByProductId(p.getId()));
    images.deleteAll(images.findByProductIdOrderBySortOrderAsc(p.getId()));
    products.delete(p);
    return new MessageResponse("Product deleted");
  }

  @PostMapping("/{id}/images")
  @Operation(summary = "Append an image to one of my products")
  @Transactional
  public MessageResponse addImage(
      @PathVariable String id, @Valid @RequestBody ProductImageRequest req) {
    Shop shop = account.currentShop();
    Product p = requireOwnProduct(id, shop);
    ProductImage img = new ProductImage();
    img.setProductId(p.getId());
    img.setUrl(req.url());
    img.setSortOrder(req.sortOrder());
    images.save(img);
    if (p.getImageUrl() == null || p.getImageUrl().isBlank()) {
      p.setImageUrl(req.url());
      products.save(p);
    }
    return new MessageResponse("Image added");
  }

  private Product requireOwnProduct(String id, Shop shop) {
    Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
    if (!shop.getId().equals(p.getShopId())) {
      throw ApiException.forbidden("Not your product");
    }
    return p;
  }

  private static void apply(Product p, ProductRequest req) {
    p.setName(req.name());
    p.setCategoryId(req.categoryId());
    p.setDescription(req.description());
    p.setPrice(req.price());
    p.setStock(req.stock());
    p.setImageUrl(req.imageUrl());
    if (req.currency() != null && !req.currency().isBlank()) {
      p.setCurrency(req.currency());
    }
  }

  private void saveExtraImages(String productId, List<String> urls) {
    if (urls == null) {
      return;
    }
    int order = 0;
    for (String url : urls) {
      if (url == null || url.isBlank()) {
        continue;
      }
      ProductImage img = new ProductImage();
      img.setProductId(productId);
      img.setUrl(url);
      img.setSortOrder(order++);
      images.save(img);
    }
  }
}
