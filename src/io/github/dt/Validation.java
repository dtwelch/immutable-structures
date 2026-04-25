package io.github.dt;

import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

/// Validation represents either a successful result or a failure. The [Success] type
/// wraps a successfully
/// computed value of type `T`; a [Failure] encapsulates a list of failures of type `E`.
///
/// Functions similar to the 'Either' datatype in scala or haskell -- where the left constructor's
/// contents are semigroup-like (can be concatenated/accumulated).
///
/// @param <T> the type of some successfully computed result.
/// @param <E> the type of errors that are accumulated.
///
/// NOTE: this + associated tests is a near direct port of the github.com/flix
/// compiler Validation type
public sealed interface Validation<T, E> {
  record Success<T, E>(T t) implements Validation<T, E> {
    @Override
    public VList<E> errors() {
      return VList.empty();
    }
  }

  record Failure<T, E>(VList<E> errors) implements Validation<T, E> {}

  /// Returns as an [Result.Ok] only if there are no errors; returns [Result.Err] otherwise.
  default Result<T, VList<E>> toResult() {
    return switch (this) {
      case Validation.Success(var t) -> Result.ok(t);
      case Validation.Failure(var errors) -> Result.err(errors);
    };
  }

  VList<E> errors();

  // companion operations

  // don't use this, use successNil(), successNone() for versions with
  // non-wildcard types
  final class Lazy {

    private Lazy() {}

    private static final Validation<?, ?> SuccessNil = new Success<>(VList.of());

    private static final Validation<?, ?> SuccessNone = new Success<>(Maybe.none());
  }

  @SuppressWarnings("unchecked")
  static <T, E> Validation<VList<T>, E> successNil() {
    return (Validation<VList<T>, E>) Lazy.SuccessNil;
  }

  @SuppressWarnings("unchecked")
  static <T, E> Validation<Maybe<T>, E> successNone() {
    return (Validation<Maybe<T>, E>) Lazy.SuccessNone;
  }

  static <T, E> Validation<T, E> success(T value) {
    return new Validation.Success<>(value);
  }

  /// Creates an [Validation.Failure] that contains the given `error`.
  static <T, E> Validation<T, E> fail(E error) {
    return Validation.fail(VList.of(error));
  }

  static <T, E> Validation<T, E> fail(VList<E> errors) {
    return new Validation.Failure<>(errors);
  }

  /// Sequences the given list of validations `xs`.
  static <T, E> Validation<VList<T>, E> sequence(Iterable<Validation<T, E>> xs) {
    var acc = Validation.<T, E>successNil();
    for (var x : xs) {
      acc =
          switch (Pair.of(x, acc)) {
            case Pair(Success(var curVal), Success(var accVal)) ->
                success(VList.cons(curVal, accVal));
            case Pair(Success(_), Failure(var accErrors)) -> fail(accErrors);
            case Pair(Failure(var curErrors), Success(_)) -> fail(curErrors);
            case Pair(Failure(var currErrors), Failure(var accErrors)) ->
                fail(currErrors.append(accErrors));
          };
    }
    return acc;
  }

  /// Traverses `xs` applying the function `f` to each element.
  static <T, S, E> Validation<VList<S>, E> traverse(
      Iterable<T> xs, Function<T, Validation<S, E>> f) {
    return fastTraverse(xs, f);
  }

  // todo todo (traverseNel potentially)

  static <T, S, E> Validation<Maybe<S>, E> traverseOpt(
      Maybe<T> o, Function<T, Validation<S, E>> f) {
    return switch (o) {
      case Maybe.None<T> _ -> successNone();
      case Maybe.Some(var x) ->
          switch (f.apply(x)) {
            case Success(var t) -> success(Maybe.of(t));
            case Failure(var errs) -> fail(errs);
          };
    };
  }

  // T = input type in the iterable
  // f = a mapping function that maps a given input element i : T, to a
  // validation
  //     consisting of a some result of type R and any errors (of type E)
  // NOTE: private static ok in interfaces since jdk9 (didn't realize)
  private static <T, S, E> Validation<VList<S>, E> fastTraverse(
      Iterable<T> xs, Function<T, Validation<S, E>> f) {
    // check if the sequence is empty.
    if (!xs.iterator().hasNext()) {
      return Validation.success(VList.empty());
    }

    // two mutable arrays to hold the intermediate results.
    var successValues = new ArrayList<S>();
    var failureStream = new ArrayList<VList<E>>();

    // flag to signal errors
    var isFatal = false;

    // apply f to each element and collect the results.
    for (T x : xs) {
      var res = f.apply(x);
      switch (res) {
        case Success<S, E> s -> successValues.add(s.t);
        case Failure<S, E> s -> {
          failureStream.add(s.errors);
          isFatal = true;
        }
      }
    }

    // check whether we were successful or not
    if (isFatal) {
      var acc = VList.<E>empty();
      for (var fail : failureStream) {
        acc = acc.append(fail);
      }
      return fail(acc);
    } else {
      return success(VList.from(successValues));
    }
  }

  /// Returns the validation inside `t1`; preserves all errors.
  private static <U, E> Validation<U, E> flatten(Validation<Validation<U, E>, E> t1) {
    return switch (t1) {
      case Success(Success(var t)) -> Validation.success((U) t);
      case Success(Failure(var e)) -> Validation.fail(e);
      case Failure(VList<E> errors) -> Validation.fail(errors);
    };
  }

  /// Applies function `f` to the val inside `t`.
  private static <T1, U, E> Validation<U, E> ap(
      Validation<Function<T1, U>, E> f, Validation<T1, E> t1) {
    return switch (Pair.of(f, t1)) {
      case Pair(Success(var g), Success(var v)) -> success(g.apply(v));
      case Pair(Success(_), Failure(var e2)) -> fail(e2);
      case Pair(Failure(var e1), Success(_)) -> fail(e1);
      case Pair(Failure(var e1), Failure(var e2)) -> fail(e1.append(e2));
    };
  }

  /// Returns `f` with the last parameter curried.
  static <T1, T2, T3> Function<T1, Function<T2, T3>> curry(BiFunction<T1, T2, T3> f) {
    return (T1 t1) -> (T2 t2) -> f.apply(t1, t2);
  }

  /// Returns `f` with the last parameter curried.
  static <T1, T2, T3, T4> BiFunction<T1, T2, Function<T3, T4>> curry(Function3<T1, T2, T3, T4> f) {
    return (T1 t1, T2 t2) -> (T3 t3) -> f.apply(t1, t2, t3);
  }

  /// Returns `f` with the last parameter curried.
  static <T1, T2, T3, T4, T5> Function3<T1, T2, T3, Function<T4, T5>> curry(
      Function4<T1, T2, T3, T4, T5> f) {
    return (T1 t1, T2 t2, T3 t3) -> (T4 t4) -> f.apply(t1, t2, t3, t4);
  }

  /// Returns `f` with the last parameter curried.
  static <T1, T2, T3, T4, T5, T6> Function4<T1, T2, T3, T4, Function<T5, T6>> curry(
      Function5<T1, T2, T3, T4, T5, T6> f) {
    return (T1 t1, T2 t2, T3 t3, T4 t4) -> (T5 t5) -> f.apply(t1, t2, t3, t4, t5);
  }

  /// Returns `f` with the last parameter curried.
  static <T1, T2, T3, T4, T5, T6, T7> Function5<T1, T2, T3, T4, T5, Function<T6, T7>> curry(
      Function6<T1, T2, T3, T4, T5, T6, T7> f) {
    return (T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) -> (T6 t6) -> f.apply(t1, t2, t3, t4, t5, t6);
  }

  /// Maps over `t1`.
  static <T1, U, E> Validation<U, E> mapN(Validation<T1, E> t1, Function<T1, U> f) {
    return switch (t1) {
      case Success(var v1) -> success(f.apply(v1));
      case Failure(var errors) -> fail(errors);
    };
  }

  /// Maps over `t1` and `t2`.
  static <T1, T2, U, E> Validation<U, E> mapN(
      Validation<T1, E> t1, Validation<T2, E> t2, BiFunction<T1, T2, U> f) {
    return ap(mapN(t1, curry(f)), t2);
  }

  /// Maps over `t1`, `t2`, and `t3`.
  static <T1, T2, T3, U, E> Validation<U, E> mapN(
      Validation<T1, E> t1,
      Validation<T2, E> t2,
      Validation<T3, E> t3,
      Function3<T1, T2, T3, U> f) {
    return ap(mapN(t1, t2, curry(f)), t3);
  }

  /// Maps over `t1`, `t2`, `t3`, and `t4`.
  static <T1, T2, T3, T4, U, E> Validation<U, E> mapN(
      Validation<T1, E> t1,
      Validation<T2, E> t2,
      Validation<T3, E> t3,
      Validation<T4, E> t4,
      Function4<T1, T2, T3, T4, U> f) {
    return ap(mapN(t1, t2, t3, curry(f)), t4);
  }

  /// Maps over `t1`, `t2`, `t3`, `t4`, and `t5`.
  static <T1, T2, T3, T4, T5, U, E> Validation<U, E> mapN(
      Validation<T1, E> t1,
      Validation<T2, E> t2,
      Validation<T3, E> t3,
      Validation<T4, E> t4,
      Validation<T5, E> t5,
      Function5<T1, T2, T3, T4, T5, U> f) {
    return ap(mapN(t1, t2, t3, t4, curry(f)), t5);
  }

  /// Maps over `t1`, `t2`, `t3`, `t4`, `t5`, and `t6`.
  static <T1, T2, T3, T4, T5, T6, U, E> Validation<U, E> mapN(
      Validation<T1, E> t1,
      Validation<T2, E> t2,
      Validation<T3, E> t3,
      Validation<T4, E> t4,
      Validation<T5, E> t5,
      Validation<T6, E> t6,
      Function6<T1, T2, T3, T4, T5, T6, U> f) {
    return ap(mapN(t1, t2, t3, t4, t5, curry(f)), t6);
  }

  /// Flatmaps over `t1`.
  static <T1, U, E> Validation<U, E> flatMapN(
      Validation<T1, E> t1, Function<T1, Validation<U, E>> f) {
    return switch (t1) {
      case Success(var v1) -> f.apply(v1);
      case Failure(var errors) -> fail(errors);
    };
  }

  /// Flatmaps over t1 and `t2`.
  static <T1, T2, U, E> Validation<U, E> flatMapN(
      Validation<T1, E> t1, Validation<T2, E> t2, BiFunction<T1, T2, Validation<U, E>> f) {
    return flatten(ap(mapN(t1, curry(f)), t2));
  }

  /// Flatmaps over `t1`, `t2`, and `t3`.
  static <T1, T2, T3, U, E> Validation<U, E> flatMapN(
      Validation<T1, E> t1,
      Validation<T2, E> t2,
      Validation<T3, E> t3,
      Function3<T1, T2, T3, Validation<U, E>> f) {
    return flatten(ap(mapN(t1, t2, curry(f)), t3));
  }

  /// Flatmaps over `t1`, `t2`, `t3`, and `t4`.
  static <T1, T2, T3, T4, U, E> Validation<U, E> flatMapN(
      Validation<T1, E> t1,
      Validation<T2, E> t2,
      Validation<T3, E> t3,
      Validation<T4, E> t4,
      Function4<T1, T2, T3, T4, Validation<U, E>> f) {
    return flatten(ap(mapN(t1, t2, t3, curry(f)), t4));
  }

  /// Flatmaps over `t1`, `t2`, `t3`, `t4`, and `t5`.
  static <T1, T2, T3, T4, T5, U, E> Validation<U, E> flatMapN(
      Validation<T1, E> t1,
      Validation<T2, E> t2,
      Validation<T3, E> t3,
      Validation<T4, E> t4,
      Validation<T5, E> t5,
      Function5<T1, T2, T3, T4, T5, Validation<U, E>> f) {
    return flatten(ap(mapN(t1, t2, t3, t4, curry(f)), t5));
  }

  /// Flatmaps over `t1`, `t2`, `t3`, `t4`, `t5`, and `t6`.
  static <T1, T2, T3, T4, T5, T6, U, E> Validation<U, E> flatMapN(
      Validation<T1, E> t1,
      Validation<T2, E> t2,
      Validation<T3, E> t3,
      Validation<T4, E> t4,
      Validation<T5, E> t5,
      Validation<T6, E> t6,
      Function6<T1, T2, T3, T4, T5, T6, Validation<U, E>> f) {
    return flatten(ap(mapN(t1, t2, t3, t4, t5, curry(f)), t6));
  }

  public static <In, Out, E> Validation<Out, E> fold(
      Iterable<In> xs, Out zero, java.util.function.BiFunction<Out, In, Validation<Out, E>> f) {
    var acc = Validation.<Out, E>success(zero);
    for (var x : xs) {
      acc = Validation.flatMapN(acc, out -> f.apply(out, x));
    }
    return acc;
  }
}
