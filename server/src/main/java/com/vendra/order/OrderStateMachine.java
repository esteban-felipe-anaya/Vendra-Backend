package com.vendra.order;

import com.vendra.common.ApiException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sub-order lifecycle and who may drive each transition.
 *
 * <pre>
 * placed → accepted → preparing → ready → courier_assigned → picked_up → on_the_way → delivered
 *   └─────────────────────────► cancelled / rejected (per rules)
 * </pre>
 *
 * Shop drives prep (accept/preparing/ready/reject); courier drives delivery
 * (picked_up/on_the_way/delivered) once it has claimed the offer; customer may cancel early; admin
 * may force any transition for dispute resolution. {@code courier_assigned} is set by the dispatch
 * service when a courier claims the delivery, not by a manual transition here.
 */
public final class OrderStateMachine {

  private OrderStateMachine() {}

  public static final String PLACED = "placed";
  public static final String ACCEPTED = "accepted";
  public static final String PREPARING = "preparing";
  public static final String READY = "ready";
  public static final String COURIER_ASSIGNED = "courier_assigned";
  public static final String PICKED_UP = "picked_up";
  public static final String ON_THE_WAY = "on_the_way";
  public static final String DELIVERED = "delivered";
  public static final String CANCELLED = "cancelled";
  public static final String REJECTED = "rejected";

  /** Allowed next states from a given state (the "happy path" + terminal branches). */
  private static final Map<String, Set<String>> NEXT =
      Map.ofEntries(
          Map.entry(PLACED, Set.of(ACCEPTED, REJECTED, CANCELLED)),
          Map.entry(ACCEPTED, Set.of(PREPARING, CANCELLED)),
          Map.entry(PREPARING, Set.of(READY)),
          Map.entry(READY, Set.of(COURIER_ASSIGNED)),
          Map.entry(COURIER_ASSIGNED, Set.of(PICKED_UP)),
          Map.entry(PICKED_UP, Set.of(ON_THE_WAY)),
          Map.entry(ON_THE_WAY, Set.of(DELIVERED)),
          Map.entry(DELIVERED, Set.of()),
          Map.entry(CANCELLED, Set.of()),
          Map.entry(REJECTED, Set.of()));

  /** Role permitted to perform a transition INTO the given target state. */
  private static final Map<String, Set<String>> ROLE_FOR_TARGET =
      Map.ofEntries(
          Map.entry(ACCEPTED, Set.of("shop", "admin")),
          Map.entry(PREPARING, Set.of("shop", "admin")),
          Map.entry(READY, Set.of("shop", "admin")),
          Map.entry(REJECTED, Set.of("shop", "admin")),
          Map.entry(COURIER_ASSIGNED, Set.of("courier", "admin")),
          Map.entry(PICKED_UP, Set.of("courier", "admin")),
          Map.entry(ON_THE_WAY, Set.of("courier", "admin")),
          Map.entry(DELIVERED, Set.of("courier", "admin")),
          Map.entry(CANCELLED, Set.of("customer", "admin")));

  public static boolean canTransition(String from, String to) {
    return NEXT.getOrDefault(from, Set.of()).contains(to);
  }

  /** Validate a transition or throw a 409/403 with a clear message. */
  public static void assertTransition(String from, String to, String role) {
    if (!canTransition(from, to)) {
      throw ApiException.conflict("Cannot move from '" + from + "' to '" + to + "'");
    }
    Set<String> allowed = ROLE_FOR_TARGET.getOrDefault(to, Set.of("admin"));
    // admin may cancel from any non-terminal state for disputes
    if (!allowed.contains(role) && !"admin".equals(role)) {
      throw ApiException.forbidden("Role '" + role + "' may not move an order to '" + to + "'");
    }
  }

  public static List<String> terminal() {
    return List.of(DELIVERED, CANCELLED, REJECTED);
  }

  public static boolean isTerminal(String state) {
    return terminal().contains(state);
  }
}
