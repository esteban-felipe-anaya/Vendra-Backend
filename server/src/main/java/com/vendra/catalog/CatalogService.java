package com.vendra.catalog;

import com.vendra.common.ApiException;
import com.vendra.domain.Product;
import com.vendra.domain.Shop;
import com.vendra.dto.Dtos.*;
import com.vendra.repo.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Public, open catalog browse — categories, shops, products, product detail, reviews. */
@Service
public class CatalogService {

  private final CategoryRepository categories;
  private final ShopRepository shops;
  private final ProductRepository products;
  private final ProductImageRepository images;
  private final ProductVariantRepository variants;
  private final ReviewRepository reviews;
  private final ProfileRepository profiles;

  public CatalogService(
      CategoryRepository categories,
      ShopRepository shops,
      ProductRepository products,
      ProductImageRepository images,
      ProductVariantRepository variants,
      ReviewRepository reviews,
      ProfileRepository profiles) {
    this.categories = categories;
    this.shops = shops;
    this.products = products;
    this.images = images;
    this.variants = variants;
    this.reviews = reviews;
    this.profiles = profiles;
  }

  public List<CategoryDto> listCategories() {
    return categories.findByIsActiveTrueOrderBySortOrderAsc().stream()
        .map(c -> new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getIcon(), c.getImageUrl(), c.getSortOrder()))
        .toList();
  }

  public List<ShopDto> listShops() {
    return shops.findByStatus("approved").stream().map(this::toShopDto).toList();
  }

  public ShopDto getShop(String slug) {
    Shop s = shops.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Shop"));
    return toShopDto(s);
  }

  /** Browse products with optional category / shop / search filters. */
  public List<ProductDto> listProducts(String categoryId, String shopId, String q) {
    Map<String, String> shopNames =
        shops.findAll().stream().collect(Collectors.toMap(Shop::getId, Shop::getName));
    return products.findByIsActiveTrue().stream()
        .filter(p -> categoryId == null || categoryId.equals(p.getCategoryId()))
        .filter(p -> shopId == null || shopId.equals(p.getShopId()))
        .filter(p -> q == null || p.getName().toLowerCase().contains(q.toLowerCase()))
        .map(p -> toProductDto(p, shopNames.get(p.getShopId())))
        .sorted(Comparator.comparingInt(ProductDto::soldCount).reversed())
        .toList();
  }

  public ProductDetailDto getProduct(String id) {
    Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
    String shopName = shops.findById(p.getShopId()).map(Shop::getName).orElse(null);
    List<String> imgs =
        images.findByProductIdOrderBySortOrderAsc(id).stream().map(i -> i.getUrl()).toList();
    List<VariantDto> vars =
        variants.findByProductId(id).stream()
            .map(v -> new VariantDto(v.getId(), v.getName(), v.getSku(),
                v.getPrice() != null ? v.getPrice() : p.getPrice(), v.getStock()))
            .toList();
    List<ReviewDto> revs =
        reviews.findByProductId(id).stream().map(this::toReviewDto).toList();
    return new ProductDetailDto(toProductDto(p, shopName), p.getDescription(), imgs, vars, revs);
  }

  public List<ReviewDto> listReviews(String productId) {
    return reviews.findByProductId(productId).stream().map(this::toReviewDto).toList();
  }

  ShopDto toShopDto(Shop s) {
    return new ShopDto(s.getId(), s.getName(), s.getSlug(), s.getDescription(), s.getLogoUrl(),
        s.getBannerUrl(), s.getStatus(), s.getRatingAvg(), s.getRatingCount());
  }

  ProductDto toProductDto(Product p, String shopName) {
    return new ProductDto(p.getId(), p.getShopId(), shopName, p.getCategoryId(), p.getName(),
        p.getSlug(), p.getPrice(), p.getCurrency(), p.getStock(), p.getImageUrl(),
        p.getRatingAvg(), p.getRatingCount(), p.getSoldCount());
  }

  ReviewDto toReviewDto(com.vendra.domain.Review r) {
    var prof = profiles.findById(r.getUserId());
    return new ReviewDto(r.getId(), r.getProductId(), r.getUserId().toString(),
        prof.map(p -> p.getFullName()).orElse("Customer"),
        prof.map(p -> p.getAvatarUrl()).orElse(null),
        r.getRating(), r.getTitle(), r.getBody(), r.getCreatedAt());
  }
}
