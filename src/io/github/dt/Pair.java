package io.github.dt;

import java.util.function.Function;

public record Pair<T, U>(T first, U second) {
  public static <A, B> Pair<A, B> of(A a, B b) {
    return new Pair<>(a, b);
  }

  /// O(1) - map the first component of this pair via `f`
  public <R> Pair<R, U> mapFirst(Function<T, R> f) {
    return new Pair<>(f.apply(first), second);
  }

  /// O(1) - map the second component of this pair via `g`
  public <R> Pair<T, R> mapSecond(Function<U, R> g) {
    return new Pair<>(first, g.apply(second));
  }

  @Override
  public String toString() {
    return String.format("(%s, %s)", first, second);
  }
}
