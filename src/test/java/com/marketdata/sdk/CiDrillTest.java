package com.marketdata.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CiDrillTest {

  @Test
  void userNameIsSdkBot() {
    assertThat(System.getProperty("user.name")).isEqualTo("sdk-bot");
  }
}
