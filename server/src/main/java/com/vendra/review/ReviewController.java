package com.vendra.review;

import com.vendra.account.AccountService;
import com.vendra.common.ApiException;
import com.vendra.domain.OrderItem;
import com.vendra.domain.Product;
import com.vendra.domain.Profile;
import com.vendra.domain.Review;
import com.vendra.domain.SubOrder;
import com.vendra.dto.Dtos.CreateReviewRequest;
import com.vendra.dto.Dtos.ReviewDto;
import com.vendra.repo.OrderItemRepository;
import com.vendra.repo.OrderRepository;
import com.vendra.repo.ProductRepository;
import com.vendra.repo.ProfileRepository;
import com.vendra.repo.ReviewRepository;
import com.vendra.repo.SubOrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Customer product reviews — gated to verified, delivered purchases. */
@RestController
@RequestMapping("/api/v1/reviews")
@PreAuthorize("hasRole('customer')")
@Tag(name = "Reviews", description = "Customer reviews of purchased products")
public class ReviewController {

  private final AccountService account;
  private final OrderRepository orders;
  private final SubOrderRepository subOrders;
  private final OrderItemRepository orderItems;
  private final ReviewRepository reviews;
  private final ProductRepository products;
  private final ProfileRepository profiles;

  public ReviewController(
      AccountService account,
      OrderRepository orders,
      SubOrderRepository subOrders,
      OrderItemRepository orderItems,
      ReviewRepository reviews,
      ProductRepository products,
      ProfileRepository profiles) {
    this.account = account;
    this.orders = orders;
    this.subOrders = subOrders;
    this.orderItems = orderItems;
    this.reviews = reviews;
    this.products = products;
    this.profiles = profiles;
  }

  @PostMapping
  @Operation(summary = "Review a product you have received (delivered)")
  @Transactional
  public ReviewDto create(@Valid @RequestBody CreateReviewRequest req) {
    if (req.rating() < 1 || req.rating() > 5) {
      throw ApiException.badRequest("rating must be between 1 and 5");
    }
    UUID userId = account.currentUserId();
    Product product =
        products.findById(req.productId()).orElseThrow(() -> ApiException.notFound("Product"));

    if (!hasDeliveredItem(userId, product.getId())) {
      throw ApiException.forbidden("You can only review products from a delivered order");
    }

    // Upsert: one review per (product, user).
    Review review =
        reviews.findByProductId(product.getId()).stream()
            .filter(r -> userId.equals(r.getUserId()))
            .findFirst()
            .orElseGet(
                () -> {
                  Review r = new Review();
                  r.setProductId(product.getId());
                  r.setUserId(userId);
                  return r;
                });
    review.setRating(req.rating());
    review.setTitle(req.title());
    review.setBody(req.body());
    Review saved = reviews.save(review);

    recomputeRating(product);

    return toReviewDto(saved);
  }

  /**
   * True if the customer has a DELIVERED sub-order containing this product. We iterate the
   * customer's orders, find delivered sub-orders, then scan their order_items for the productId.
   */
  private boolean hasDeliveredItem(UUID userId, String productId) {
    List<com.vendra.domain.Order> myOrders = orders.findByCustomerIdOrderByCreatedAtDesc(userId);
    for (var order : myOrders) {
      for (SubOrder sub : subOrders.findByOrderId(order.getId())) {
        if (!"delivered".equalsIgnoreCase(sub.getStatus())) {
          continue;
        }
        for (OrderItem item : orderItems.findBySubOrderId(sub.getId())) {
          if (productId.equals(item.getProductId())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void recomputeRating(Product product) {
    List<Review> all = reviews.findByProductId(product.getId());
    int count = all.size();
    if (count == 0) {
      product.setRatingAvg(BigDecimal.ZERO);
      product.setRatingCount(0);
    } else {
      int sum = all.stream().mapToInt(Review::getRating).sum();
      BigDecimal avg =
          BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
      product.setRatingAvg(avg);
      product.setRatingCount(count);
    }
    products.save(product);
  }

  private ReviewDto toReviewDto(Review r) {
    Profile prof = profiles.findById(r.getUserId()).orElse(null);
    return new ReviewDto(
        r.getId(),
        r.getProductId(),
        r.getUserId().toString(),
        prof != null ? prof.getFullName() : "Customer",
        prof != null ? prof.getAvatarUrl() : null,
        r.getRating(),
        r.getTitle(),
        r.getBody(),
        r.getCreatedAt());
  }
}
