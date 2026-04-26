package io.github.dt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class PairTests {

  @Test
  void of01() {
    Assertions.assertEquals(new Pair<>(1, "x"), Pair.of(1, "x"));
  }

  @Test
  void mapFirst01() {
    var pair = Pair.of(2, "x");

    var mapped = pair.mapFirst(x -> x * 10);

    Assertions.assertEquals(Pair.of(20, "x"), mapped);
    Assertions.assertEquals(Pair.of(2, "x"), pair);
  }

  @Test
  void mapSecond01() {
    var pair = Pair.of(2, "x");

    var mapped = pair.mapSecond(String::toUpperCase);

    Assertions.assertEquals(Pair.of(2, "X"), mapped);
    Assertions.assertEquals(Pair.of(2, "x"), pair);
  }

  @Test
  void toString01() {
    Assertions.assertEquals("(left, right)", Pair.of("left", "right").toString());
  }
}
