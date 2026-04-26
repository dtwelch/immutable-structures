package io.github.dt;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/// An optional value that is either [Some] or [None].
public sealed interface Maybe<A> {
  static <T> Maybe<T> of(T value) {
    return (value == null) ? none() : new Some<>(value);
  }

  @SuppressWarnings("unchecked")
  static <T> Maybe<T> none() {
    return (None<T>) None.Instance;
  }

  /// O(1) - returns the wrapped value if present; otherwise returns `other`.
  default A getOrElse(A other) {
    return isEmpty() ? other : get();
  }

  /// O(1) - returns the wrapped value if present; otherwise evaluates `supplier`.
  default A getOrElse(Supplier<A> supplier) {
    return isEmpty() ? supplier.get() : get();
  }

  /// O(1) - returns the wrapped value if present; otherwise returns `null`.
  default A orNull() {
    return isEmpty() ? null : get();
  }

  /// O(1) - returns the wrapped value.
  ///
  /// @throws NoSuchElementException if this is [None].
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

  /// O(1) - returns true only if this maybe wraps a value equal to `item`.
  default boolean contains(A item) {
    return switch (this) {
      case Some(var x) -> x.equals(item);
      case None<A> _ -> false;
    };
  }

  /// O(1) - returns true if this maybe is empty or if the wrapped value satisfies `p`.
  default boolean forall(Predicate<A> p) {
    return isEmpty() || p.test(this.get());
  }

  /// O(1) - maps `f` over the wrapped value if present.
  default <B> Maybe<B> map(Function<A, B> f) {
    return switch (this) {
      case Maybe.None<?> _ -> none();
      case Some(var x) -> new Some<>(f.apply(x));
    };
  }

  /// O(1) - flatmaps `f` over the wrapped value if present.
  default <B> Maybe<B> flatMap(Function<A, Maybe<B>> f) {
    return switch (this) {
      case Maybe.None<?> _ -> Maybe.none();
      case Maybe.Some(var x) -> f.apply(x);
    };
  }

  /// O(1) - returns true only if this maybe is nonempty and the wrapped value satisfies `p`.
  default boolean exists(Predicate<A> p) {
    return switch (this) {
      case Some(var x) -> p.test(x);
      default -> false;
    };
  }

  /// O(1) - applies `f` to the wrapped value if present.
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
