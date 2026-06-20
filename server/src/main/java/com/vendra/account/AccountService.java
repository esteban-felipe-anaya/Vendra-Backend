package com.vendra.account;

import com.vendra.common.ApiException;
import com.vendra.domain.Courier;
import com.vendra.domain.Profile;
import com.vendra.domain.Shop;
import com.vendra.repo.CourierRepository;
import com.vendra.repo.ProfileRepository;
import com.vendra.repo.ShopRepository;
import com.vendra.security.AuthUser;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the authenticated principal to its profile / shop / courier rows for row-scoping. */
@Service
public class AccountService {

  private final ProfileRepository profiles;
  private final ShopRepository shops;
  private final CourierRepository couriers;

  public AccountService(
      ProfileRepository profiles, ShopRepository shops, CourierRepository couriers) {
    this.profiles = profiles;
    this.shops = shops;
    this.couriers = couriers;
  }

  public UUID currentUserId() {
    return UUID.fromString(AuthUser.id());
  }

  /**
   * Returns the caller's profile. The DB trigger creates it at signup; we self-heal if a token
   * arrives before replication by inserting from the JWT claims.
   */
  @Transactional
  public Profile currentProfile() {
    UUID id = currentUserId();
    return profiles
        .findById(id)
        .orElseGet(
            () -> {
              Profile p = new Profile();
              p.setId(id);
              p.setRole(AuthUser.role());
              return profiles.save(p);
            });
  }

  public Shop currentShop() {
    List<Shop> owned = shops.findByOwnerId(currentUserId());
    if (owned.isEmpty()) {
      throw ApiException.forbidden("No shop is linked to this account");
    }
    return owned.get(0);
  }

  public Shop requireShop(String shopId) {
    Shop s = shops.findById(shopId).orElseThrow(() -> ApiException.notFound("Shop"));
    if (!s.getOwnerId().equals(currentUserId())) {
      throw ApiException.forbidden("Not your shop");
    }
    return s;
  }

  public Courier currentCourier() {
    return couriers
        .findByProfileId(currentUserId())
        .orElseThrow(() -> ApiException.forbidden("No courier profile linked to this account"));
  }
}
