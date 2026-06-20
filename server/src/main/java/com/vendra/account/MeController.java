package com.vendra.account;

import com.vendra.account.AccountDtos.AddressRequest;
import com.vendra.account.AccountDtos.UpdateProfileRequest;
import com.vendra.common.ApiException;
import com.vendra.domain.Address;
import com.vendra.domain.Notification;
import com.vendra.domain.Product;
import com.vendra.domain.Profile;
import com.vendra.domain.Shop;
import com.vendra.domain.WishlistItem;
import com.vendra.dto.Dtos.AddressDto;
import com.vendra.dto.Dtos.MessageResponse;
import com.vendra.dto.Dtos.NotificationDto;
import com.vendra.dto.Dtos.ProductDto;
import com.vendra.dto.Dtos.ProfileDto;
import com.vendra.shop.ShopDtos;
import com.vendra.repo.AddressRepository;
import com.vendra.repo.NotificationRepository;
import com.vendra.repo.ProductRepository;
import com.vendra.repo.ShopRepository;
import com.vendra.repo.WishlistItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The authenticated user's own profile, notifications, addresses and wishlist. */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Me", description = "Current user profile, notifications, addresses, wishlist")
public class MeController {

  private final AccountService account;
  private final NotificationRepository notifications;
  private final AddressRepository addresses;
  private final WishlistItemRepository wishlist;
  private final ProductRepository products;
  private final ShopRepository shops;
  private final com.vendra.repo.ProfileRepository profiles;

  public MeController(
      AccountService account,
      NotificationRepository notifications,
      AddressRepository addresses,
      WishlistItemRepository wishlist,
      ProductRepository products,
      ShopRepository shops,
      com.vendra.repo.ProfileRepository profiles) {
    this.account = account;
    this.notifications = notifications;
    this.addresses = addresses;
    this.wishlist = wishlist;
    this.products = products;
    this.shops = shops;
    this.profiles = profiles;
  }

  // ---------- profile ----------
  @GetMapping
  @Operation(summary = "Get my profile")
  public ProfileDto me() {
    return toProfileDto(account.currentProfile());
  }

  @PatchMapping
  @Operation(summary = "Update my profile (fullName, phone, avatarUrl)")
  @Transactional
  public ProfileDto updateMe(@Valid @RequestBody UpdateProfileRequest req) {
    Profile p = account.currentProfile();
    if (req.fullName() != null) {
      p.setFullName(req.fullName());
    }
    if (req.phone() != null) {
      p.setPhone(req.phone());
    }
    if (req.avatarUrl() != null) {
      p.setAvatarUrl(req.avatarUrl());
    }
    return toProfileDto(profiles.save(p));
  }

  // ---------- notifications ----------
  @GetMapping("/notifications")
  @Operation(summary = "List my notifications (newest first)")
  public List<NotificationDto> notifications() {
    return notifications.findByUserIdOrderByCreatedAtDesc(account.currentUserId()).stream()
        .map(MeController::toNotificationDto)
        .toList();
  }

  @PostMapping("/notifications/{id}/read")
  @Operation(summary = "Mark a notification read")
  @Transactional
  public MessageResponse markRead(@PathVariable String id) {
    Notification n = notifications.findById(id).orElseThrow(() -> ApiException.notFound("Notification"));
    if (!n.getUserId().equals(account.currentUserId())) {
      throw ApiException.forbidden("Not your notification");
    }
    n.setRead(true);
    notifications.save(n);
    return new MessageResponse("Notification marked read");
  }

  // ---------- addresses ----------
  @GetMapping("/addresses")
  @Operation(summary = "List my saved addresses")
  public List<AddressDto> addresses() {
    return addresses.findByUserId(account.currentUserId()).stream()
        .map(MeController::toAddressDto)
        .toList();
  }

  @PostMapping("/addresses")
  @Operation(summary = "Add a saved address")
  @Transactional
  public AddressDto createAddress(@Valid @RequestBody AddressRequest req) {
    Address a = new Address();
    a.setUserId(account.currentUserId());
    applyAddress(a, req);
    return toAddressDto(addresses.save(a));
  }

  @PutMapping("/addresses/{id}")
  @Operation(summary = "Update a saved address")
  @Transactional
  public AddressDto updateAddress(@PathVariable String id, @Valid @RequestBody AddressRequest req) {
    Address a = requireOwnAddress(id);
    applyAddress(a, req);
    return toAddressDto(addresses.save(a));
  }

  @DeleteMapping("/addresses/{id}")
  @Operation(summary = "Delete a saved address")
  @Transactional
  public MessageResponse deleteAddress(@PathVariable String id) {
    Address a = requireOwnAddress(id);
    addresses.delete(a);
    return new MessageResponse("Address deleted");
  }

  private Address requireOwnAddress(String id) {
    Address a = addresses.findById(id).orElseThrow(() -> ApiException.notFound("Address"));
    if (!a.getUserId().equals(account.currentUserId())) {
      throw ApiException.forbidden("Not your address");
    }
    return a;
  }

  private static void applyAddress(Address a, AddressRequest req) {
    a.setLabel(req.label());
    a.setRecipient(req.recipient());
    a.setPhone(req.phone());
    a.setLine1(req.line1());
    a.setLine2(req.line2());
    a.setCity(req.city());
    a.setRegion(req.region());
    a.setPostalCode(req.postalCode());
    if (req.country() != null && !req.country().isBlank()) {
      a.setCountry(req.country());
    }
    a.setLat(req.lat());
    a.setLng(req.lng());
    a.setDefault(req.isDefault());
  }

  // ---------- wishlist ----------
  @GetMapping("/wishlist")
  @Operation(summary = "List my wishlist products")
  public List<ProductDto> wishlist() {
    List<WishlistItem> items = wishlist.findByUserId(account.currentUserId());
    Map<String, String> shopNames =
        shops.findAll().stream().collect(Collectors.toMap(Shop::getId, Shop::getName));
    return items.stream()
        .map(w -> products.findById(w.getProductId()).orElse(null))
        .filter(p -> p != null)
        .map(p -> ShopDtos.toProductDto(p, shopNames.get(p.getShopId())))
        .toList();
  }

  @PostMapping("/wishlist/{productId}")
  @Operation(summary = "Add a product to my wishlist")
  @Transactional
  public MessageResponse addWishlist(@PathVariable String productId) {
    Product p = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product"));
    UUID userId = account.currentUserId();
    if (wishlist.findByUserIdAndProductId(userId, p.getId()).isEmpty()) {
      WishlistItem w = new WishlistItem();
      w.setUserId(userId);
      w.setProductId(p.getId());
      wishlist.save(w);
    }
    return new MessageResponse("Added to wishlist");
  }

  @DeleteMapping("/wishlist/{productId}")
  @Operation(summary = "Remove a product from my wishlist")
  @Transactional
  public MessageResponse removeWishlist(@PathVariable String productId) {
    wishlist
        .findByUserIdAndProductId(account.currentUserId(), productId)
        .ifPresent(wishlist::delete);
    return new MessageResponse("Removed from wishlist");
  }

  // ---------- mappers ----------
  private static ProfileDto toProfileDto(Profile p) {
    return new ProfileDto(
        p.getId().toString(), p.getRole(), p.getFullName(), p.getPhone(), p.getAvatarUrl());
  }

  private static NotificationDto toNotificationDto(Notification n) {
    return new NotificationDto(
        n.getId(),
        n.getType(),
        n.getTitle(),
        n.getBody(),
        n.getRefType(),
        n.getRefId(),
        n.isRead(),
        n.getCreatedAt());
  }

  private static AddressDto toAddressDto(Address a) {
    return new AddressDto(
        a.getId(),
        a.getLabel(),
        a.getRecipient(),
        a.getPhone(),
        a.getLine1(),
        a.getLine2(),
        a.getCity(),
        a.getRegion(),
        a.getPostalCode(),
        a.getCountry(),
        a.getLat(),
        a.getLng(),
        a.isDefault());
  }
}
