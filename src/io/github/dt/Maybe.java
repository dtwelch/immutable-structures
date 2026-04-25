package io.github.dt;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public sealed interface Maybe<A> {
  static <T> Maybe<T> of(T value) {
    return (value == null) ? none() : new Some<>(value);
  }

  @SuppressWarnings("unchecked")
  static <T> Maybe<T> none() {
    return (None<T>) None.Instance;
  }

  default A getOrElse(A other) {
    return isEmpty() ? other : get();
  }

  default A getOrElse(Supplier<A> supplier) {
    return isEmpty() ? supplier.get() : get();
  }

  /// Returns the maybe's value if it is nonempty, or null if it is empty.
  default A orNull() {
    return isEmpty() ? null : get();
  }

  default A get() {
    return switch (this) {
      case Some(var x) -> x;
      case None<?> _ -> throw new UnsupportedOperationException("get called on None");
    };
  }

  default boolean isEmpty() {
    return this instanceof Maybe.None<A>;
  }

  default boolean nonEmpty() {
    return this instanceof Maybe.Some<A>;
  }

  default boolean isDefined() {
    return nonEmpty();
  }

  default boolean contains(A item) {
    return switch (this) {
      case Some(var x) -> x.equals(item);
      case None<A> _ -> false;
    };
  }

  /// Returns true only if this maybe is empty or contains a wrapped item that satisfies predicate
  /// `p`; returns [Maybe.None] otherwise.
  default boolean forall(Predicate<A> p) {
    return isEmpty() || p.test(this.get());
  }

  default <B> Maybe<B> map(Function<A, B> f) {
    return switch (this) {
      case Maybe.None<?> _ -> none();
      case Some(var x) -> new Some<>(f.apply(x));
    };
  }

  default <B> Maybe<B> flatMap(Function<A, Maybe<B>> f) {
    return switch (this) {
      case Maybe.None<?> _ -> Maybe.none();
      case Maybe.Some(var x) -> f.apply(x);
    };
  }

  /// Returns true only if this maybe is nonempty *and* the provided predicate `p` holds when
  /// applied to the contents within.
  default boolean exists(Predicate<A> p) {
    return switch (this) {
      case Some(var x) -> p.test(x);
      default -> false;
    };
  }

  default <U> void foreach(Function<A, U> f) {
    if (!isEmpty()) {
      f.apply(this.get());
    }
  }

  final class None<A> implements Maybe<A> {

    public static final None<?> Instance = new None<>();

    private None() {}

    @Override
    public A get() {
      throw new NoSuchElementException("option is empty");
    }

    @Override
    public int hashCode() {
      return 1;
    }
  }

  record Some<A>(A value) implements Maybe<A> {
    @Override
    public A get() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      return switch (o) {
        case Some(var ov) -> this.value.equals(ov);
        default -> false;
      };
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value);
    }
  }
}
