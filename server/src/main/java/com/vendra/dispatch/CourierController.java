package com.vendra.dispatch;

import com.vendra.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Courier mode: availability, dispatch offers, active delivery flow, location pings. */
@RestController
@RequestMapping("/api/v1/courier")
@PreAuthorize("hasRole('courier')")
@Tag(name = "Courier", description = "Dispatch offers and delivery flow")
public class CourierController {

  private final DispatchService dispatch;

  public CourierController(DispatchService dispatch) {
    this.dispatch = dispatch;
  }

  @PutMapping("/availability")
  @Operation(summary = "Set availability: offline | available | busy")
  public MessageResponse availability(@Valid @RequestBody AvailabilityRequest req) {
    dispatch.setAvailability(req.availability());
    return new MessageResponse("Availability set to " + req.availability());
  }

  @GetMapping("/offers")
  @Operation(summary = "List open delivery offers (when available)")
  public List<DeliveryDto> offers() {
    return dispatch.offers();
  }

  @GetMapping("/deliveries")
  @Operation(summary = "List my deliveries (active + past)")
  public List<DeliveryDto> deliveries() {
    return dispatch.myDeliveries();
  }

  @PostMapping("/offers/{deliveryId}/accept")
  @Operation(summary = "Accept an offer (first-accept-wins)")
  public DeliveryDto accept(@PathVariable String deliveryId) {
    return dispatch.accept(deliveryId);
  }

  @PostMapping("/deliveries/{deliveryId}/transition")
  @Operation(summary = "Advance delivery: picked_up | on_the_way | delivered")
  public DeliveryDto transition(
      @PathVariable String deliveryId, @Valid @RequestBody TransitionRequest req) {
    return dispatch.transition(deliveryId, req.to());
  }

  @PostMapping("/deliveries/{deliveryId}/location")
  @Operation(summary = "Post a courier location ping (drives the customer live map)")
  public MessageResponse ping(
      @PathVariable String deliveryId, @Valid @RequestBody LocationPingRequest req) {
    dispatch.ping(deliveryId, req);
    return new MessageResponse("ok");
  }
}
