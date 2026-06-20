package com.vendra.catalog;

import com.vendra.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Public catalog — no auth required (open browse). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalog", description = "Public browse: categories, shops, products, reviews")
public class CatalogController {

  private final CatalogService catalog;

  public CatalogController(CatalogService catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/categories")
  @Operation(summary = "List active categories")
  public List<CategoryDto> categories() {
    return catalog.listCategories();
  }

  @GetMapping("/shops")
  @Operation(summary = "List approved shops")
  public List<ShopDto> shops() {
    return catalog.listShops();
  }

  @GetMapping("/shops/{slug}")
  @Operation(summary = "Get a shop by slug")
  public ShopDto shop(@PathVariable String slug) {
    return catalog.getShop(slug);
  }

  @GetMapping("/products")
  @Operation(summary = "Browse products (filter by category, shop, search query)")
  public List<ProductDto> products(
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String shop,
      @RequestParam(required = false) String q) {
    return catalog.listProducts(category, shop, q);
  }

  @GetMapping("/products/{id}")
  @Operation(summary = "Get product detail with images, variants and reviews")
  public ProductDetailDto product(@PathVariable String id) {
    return catalog.getProduct(id);
  }

  @GetMapping("/products/{id}/reviews")
  @Operation(summary = "List reviews for a product")
  public List<ReviewDto> reviews(@PathVariable String id) {
    return catalog.listReviews(id);
  }
}
