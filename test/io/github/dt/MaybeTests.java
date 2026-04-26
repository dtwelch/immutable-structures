package io.github.dt;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class MaybeTests {

  @Test
  void of01() {
    Assertions.assertEquals(Maybe.of(42), Maybe.of(42));
  }

  @Test
  void of02() {
    Assertions.assertEquals(Maybe.none(), Maybe.of(null));
  }

  @Test
  void get01() {
    Assertions.assertEquals("x", Maybe.of("x").get());
  }

  @Test
  void get02() {
    Assertions.assertThrows(java.util.NoSuchElementException.class, () -> Maybe.none().get());
  }

  @Test
  void getOrElse01() {
    Assertions.assertEquals(42, Maybe.of(42).getOrElse(99));
    Assertions.assertEquals(99, Maybe.<Integer>none().getOrElse(99));
  }

  @Test
  void getOrElse02() {
    var calls = new AtomicInteger(0);

    var some = Maybe.of(42).getOrElse(() -> {
      calls.incrementAndGet();
      return 99;
    });
    var none = Maybe.<Integer>none().getOrElse(() -> {
      calls.incrementAndGet();
      return 99;
    });

    Assertions.assertEquals(42, some);
    Assertions.assertEquals(99, none);
    Assertions.assertEquals(1, calls.get());
  }

  @Test
  void orNull01() {
    Assertions.assertEquals("x", Maybe.of("x").orNull());
    Assertions.assertNull(Maybe.<String>none().orNull());
  }

  @Test
  void contains01() {
    Assertions.assertTrue(Maybe.of("x").contains("x"));
    Assertions.assertFalse(Maybe.of("x").contains("y"));
    Assertions.assertFalse(Maybe.<String>none().contains("x"));
  }

  @Test
  void forallExists01() {
    var some = Maybe.of(4);
    var none = Maybe.<Integer>none();

    Assertions.assertTrue(some.forall(x -> x % 2 == 0));
    Assertions.assertFalse(some.exists(x -> x % 2 == 1));
    Assertions.assertTrue(none.forall(x -> x % 2 == 0));
    Assertions.assertFalse(none.exists(x -> true));
  }

  @Test
  void map01() {
    Assertions.assertEquals(Maybe.of(43), Maybe.of(42).map(x -> x + 1));
    Assertions.assertEquals(Maybe.none(), Maybe.<Integer>none().map(x -> x + 1));
  }

  @Test
  void flatMap01() {
    Assertions.assertEquals(Maybe.of(43), Maybe.of(42).flatMap(x -> Maybe.of(x + 1)));
    Assertions.assertEquals(Maybe.none(), Maybe.of(42).flatMap(_ -> Maybe.none()));
    Assertions.assertEquals(Maybe.none(), Maybe.<Integer>none().flatMap(x -> Maybe.of(x + 1)));
  }

  @Test
  void foreach01() {
    var acc = new AtomicInteger(0);

    Maybe.of(5).foreach(x -> {
      acc.addAndGet(x);
      return null;
    });
    Maybe.<Integer>none().foreach(x -> {
      acc.addAndGet(100);
      return null;
    });

    Assertions.assertEquals(5, acc.get());
  }
}
