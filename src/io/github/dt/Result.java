package io.github.dt;

import java.util.ArrayList;
import java.util.function.Function;

/// A result that is either [Ok] or [Err].
///
/// @param <T> the type of the value.
/// @param <E> the type of the error.
public sealed interface Result<T, E> {
  record Ok<T, E>(T t) implements Result<T, E> {}

  record Err<T, E>(E e) implements Result<T, E> {}

  static <T, E> Result<T, E> ok(T t) {
    return new Ok<>(t);
  }

  static <T, E> Result<T, E> err(E e) {
    return new Err<>(e);
  }

  /// O(1) - returns the wrapped error value.
  ///
  /// @throws IllegalArgumentException if this is [Ok].
  default E getError() {
    return switch (this) {
      case Ok(_) -> throw new IllegalArgumentException("getError() called on non error instance");
      case Err(var e) -> e;
    };
  }

  /// O(1) - returns the wrapped success value.
  ///
  /// @throws IllegalArgumentException if this is [Err].
  default T get() {
    return switch (this) {
      case Ok(var v) -> v;
      case Err(_) -> throw new IllegalArgumentException("get() called on error instance");
    };
  }

  /// O(1) - returns true only if this result is [Ok].
  default boolean isOk() {
    return this instanceof Result.Ok<T, E>;
  }

  /// O(1) - returns true only if this result is [Err].
  default boolean isError() {
    return this instanceof Result.Err<T, E>;
  }

  /// O(1) - maps `f` over the wrapped success value if present.
  default <U> Result<U, E> map(Function<T, U> f) {
    return switch (this) {
      case Ok(var t) -> ok(f.apply(t));
      case Err(var e) -> err(e);
    };
  }

  /// O(1) - flatmaps `f` over the wrapped success value if present.
  default <B> Result<B, E> flatMap(Function<T, Result<B, E>> f) {
    return switch (this) {
      case Ok(var t) -> f.apply(t);
      case Err(var e) -> err(e);
    };
  }

  /// O(1) - converts this result to a [Maybe], discarding any error value.
  default Maybe<T> toMaybe() {
    return switch (this) {
      case Ok(var t) -> Maybe.of(t);
      case Err(_) -> Maybe.none();
    };
  }

  /// O(n) - evaluates `xs` from left to right, collecting success values into a list.
  ///
  /// Returns the first error value encountered, if any.
  static <T, E> Result<VList<T>, E> sequence(VList<Result<T, E>> xs) {
    var result = new ArrayList<T>();
    for (var res : xs) {
      switch (res) {
        case Ok(var r) -> result.add(r);
        case Err(var err) -> {
          return err(err);
        }
      }
    }
    return ok(VList.from(result));
  }

  /// O(n + cost(f)) - applies `f` to each element in `xs`.
  ///
  /// Returns the first error produced by `f`, or the collected success values otherwise.
  static <T, S, E> Result<VList<S>, E> traverse(Iterable<T> xs, Function<T, Result<S, E>> f) {
    var result = new ArrayList<S>();
    for (T x : xs) {
      switch (f.apply(x)) {
        // case 1: ok, add to the list
        case Ok(var ok) -> result.add(ok);
        // case 2: error, short-circuit
        case Err(var e) -> {
          return err(e);
        }
      }
    }
    return ok(VList.from(result));
  }
}
