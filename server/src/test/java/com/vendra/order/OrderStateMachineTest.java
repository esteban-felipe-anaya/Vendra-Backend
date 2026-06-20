package com.vendra.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vendra.common.ApiException;
import org.junit.jupiter.api.Test;

class OrderStateMachineTest {

  @Test
  void happyPathTransitionsAreValid() {
    assertThat(OrderStateMachine.canTransition("placed", "accepted")).isTrue();
    assertThat(OrderStateMachine.canTransition("accepted", "preparing")).isTrue();
    assertThat(OrderStateMachine.canTransition("preparing", "ready")).isTrue();
    assertThat(OrderStateMachine.canTransition("ready", "courier_assigned")).isTrue();
    assertThat(OrderStateMachine.canTransition("on_the_way", "delivered")).isTrue();
  }

  @Test
  void illegalJumpsAreRejected() {
    assertThat(OrderStateMachine.canTransition("placed", "delivered")).isFalse();
    assertThat(OrderStateMachine.canTransition("delivered", "placed")).isFalse();
  }

  @Test
  void shopMayNotDriveDelivery() {
    assertThatThrownBy(
            () -> OrderStateMachine.assertTransition("on_the_way", "delivered", "shop"))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void courierMayNotAcceptOnShopBehalf() {
    assertThatThrownBy(() -> OrderStateMachine.assertTransition("placed", "accepted", "courier"))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void adminMayForceCancel() {
    OrderStateMachine.assertTransition("accepted", "cancelled", "admin"); // no throw
  }

  @Test
  void terminalStatesAreTerminal() {
    assertThat(OrderStateMachine.isTerminal("delivered")).isTrue();
    assertThat(OrderStateMachine.isTerminal("cancelled")).isTrue();
    assertThat(OrderStateMachine.isTerminal("preparing")).isFalse();
  }
}
