package io.github.dt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class ValidationTests {

  // dummy error type for these tests
  record DummyError(int n) {}

  @Test
  void map01() {
    var result = Validation.mapN(Validation.success("foo"), String::toUpperCase);
    Assertions.assertEquals(Validation.success("FOO"), result);
  }

  @Test
  void map02() {
    var one = Validation.mapN(Validation.success("foo"), String::toUpperCase);
    var result = Validation.mapN(one, s -> new StringBuilder(s).reverse().toString());
    Assertions.assertEquals(Validation.success("OOF"), result);
  }

  @Test
  void map03() {
    var one = Validation.mapN(Validation.success("foo"), String::toUpperCase);
    var two = Validation.mapN(one, s -> new StringBuilder(s).reverse().toString());
    var result = Validation.mapN(two, s -> s + s);
    Assertions.assertEquals(Validation.success("OOFOOF"), result);
  }

  @Test
  void map04() {
    var one = Validation.mapN(Validation.success("abc"), String::length);
    var result = Validation.mapN(one, len -> len < 5);
    Assertions.assertEquals(Validation.success(true), result);
  }

  @Test
  void map05() {
    var one = Validation.mapN(Validation.success("abc"), s -> s.charAt(1));
    var two = Validation.mapN(one, c -> (char) (c + 3));
    var result = Validation.mapN(two, Object::toString);
    Assertions.assertEquals(Validation.success("e"), result);
  }

  @Test
  void mapN01() {
    var result =
        Validation.mapN(
            Validation.success("foo"),
            Validation.success("foo"),
            (x, y) ->
                new StringBuilder(x).reverse().toString().toUpperCase()
                    + new StringBuilder(y).reverse().toString().toUpperCase());
    Assertions.assertEquals(Validation.success("OOFOOF"), result);
  }

  @Test
  void mapN02() {
    var ex = new RuntimeException();
    var result =
        Validation.mapN(
            Validation.fail(VList.of(ex)),
            _ -> {
              throw new AssertionError("shouldn't be called");
            });
    Assertions.assertEquals(Validation.fail(VList.of(ex)), result);
  }

  @Test
  void mapN03() {
    var result =
        Validation.mapN(
            Validation.success("foo"),
            x ->
                new StringBuilder(x).reverse().toString().toUpperCase()
                    + new StringBuilder(x).reverse().toString().toUpperCase());
    Assertions.assertEquals(Validation.success("OOFOOF"), result);
  }

  @Test
  void flatMapN01() {
    var result =
        Validation.flatMapN(Validation.success("foo"), x -> Validation.success(x.toUpperCase()));
    Assertions.assertEquals(Validation.success("FOO"), result);
  }

  @Test
  void flatMapN02() {
    var result =
        Validation.flatMapN(
            Validation.success("foo"),
            x ->
                Validation.flatMapN(
                    Validation.success(x.toUpperCase()),
                    y ->
                        Validation.flatMapN(
                            Validation.success(new StringBuilder(y).reverse().toString()),
                            z -> Validation.success(z + z))));
    Assertions.assertEquals(Validation.success("OOFOOF"), result);
  }

  @Test
  void flatMapN03() {
    var ex = new DummyError(1);
    var result =
        Validation.flatMapN(
            Validation.<String, DummyError>success("foo"), _ -> Validation.fail(VList.of(ex)));
    Assertions.assertEquals(Validation.fail(VList.of(ex)), result);
  }

  @Test
  void flatMapN05() {
    var result =
        Validation.flatMapN(
            Validation.success("foo"),
            x ->
                Validation.flatMapN(
                        Validation.success(x.toUpperCase()),
                        _ ->
                        Validation.flatMapN(
                            Validation.fail(VList.of(4, 5, 6)),
                            _ -> Validation.fail(VList.of(7, 8, 9)))));
    Assertions.assertEquals(Validation.fail(VList.of(4, 5, 6)), result);
  }

  @Test
  void traverse01() {
    var result = Validation.traverse(VList.of(1, 2, 3), x -> Validation.success(x + 1));
    Assertions.assertEquals(Validation.success(VList.from(VList.of(2, 3, 4))), result);
  }

  @Test
  void traverse02() {
    var result = Validation.traverse(VList.of(1, 2, 3), _ -> Validation.fail(VList.of(42)));
    Assertions.assertEquals(Validation.fail(VList.of(42, 42, 42)), result);
  }

  @Test
  void traverse03() {
    var result =
        Validation.traverse(
            VList.of(1, 2, 3),
            x -> x % 2 == 1 ? Validation.success(x) : Validation.fail(VList.of(2)));
    Assertions.assertEquals(Validation.fail(VList.of(2)), result);
  }

  @Test
  void foldRight01() {
    var result =
        VList.of(1, 1, 1)
            .foldRight(Validation.success(10), (x, acc) -> Validation.mapN(acc, a -> a - x));
    Assertions.assertEquals(Validation.success(7), result);
  }

  @Test
  void toResult01() {
    var t = Validation.<String, DummyError>success("abc");
    Assertions.assertEquals(Result.ok("abc"), t.toResult());
  }

  @Test
  void toResult02() {
    var ex = new DummyError(1);
    var t = Validation.<String, DummyError>fail(VList.of(ex));
    Assertions.assertEquals(Result.err(VList.of(ex)), t.toResult());
  }
}
