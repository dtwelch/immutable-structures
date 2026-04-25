package io.github.dt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResultTests {

  @Test
  public void get01() {
    Assertions.assertEquals(42, Result.ok(42).get());
  }

  @Test
  public void get02() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> Result.err(42).get());
  }

  @Test
  public void map01() {
    Assertions.assertEquals(Result.ok(43), Result.ok(42).map(x -> x + 1));
  }

  @Test
  public void map02() {
    Assertions.assertEquals(Result.err(42), Result.<Integer, Integer>err(42).map(x -> x + 1));
  }

  @Test
  public void map03() {
    var result = Result.ok(42).map(x -> x + 1).map(x -> x + 1).map(x -> x + 1);
    Assertions.assertEquals(Result.ok(45), result);
  }

  @Test
  public void flatMap01() {
    var result = Result.ok(42).flatMap(x -> Result.ok(x + 1));
    Assertions.assertEquals(Result.ok(43), result);
  }

  @Test
  public void flatMap02() {
    var result = Result.<Integer, Integer>err(42).flatMap(x -> Result.ok(x + 1));
    Assertions.assertEquals(Result.err(42), result);
  }

  @Test
  public void flatMap03() {
    var result = Result.ok(42).flatMap(x -> Result.ok(x + 1)).flatMap(x -> Result.ok(x + 1));
    Assertions.assertEquals(Result.ok(44), result);
  }

  @Test
  public void flatmapCallChain01() {
    var r = Result.ok(42).map(x -> x);
    Assertions.assertEquals(Result.ok(42), r);
  }

  @Test
  public void flatmapCallChain02() {
    var r =
        Result.ok(42).flatMap(a -> Result.ok(21).flatMap(b -> Result.ok(11).map(c -> a + b + c)));
    Assertions.assertEquals(Result.ok(74), r);
  }

  @Test
  public void flatmapCallChain03() {
    var r =
        Result.<Integer, Integer>ok(42)
            .flatMap(
                a ->
                    Result.<Integer, Integer>err(82)
                        .flatMap(b -> Result.<Integer, Integer>ok(11).map(_ -> a + b)));
    Assertions.assertEquals(Result.err(82), r);
  }

  @Test
  public void seqM01() {
    Assertions.assertEquals(Result.ok(VList.empty()), Result.sequence(VList.empty()));
  }

  @Test
  public void seqM02() {
    var a = Result.ok(1);
    var b = Result.ok(2);
    var list = VList.of(a, b);
    Assertions.assertEquals(Result.ok(VList.of(1, 2)), Result.sequence(list));
  }

  @Test
  public void seqM03() {
    var list = VList.of(Result.ok(1), Result.ok(2), Result.ok(3));
    Assertions.assertEquals(Result.ok(VList.of(1, 2, 3)), Result.sequence(list));
  }

  @Test
  public void seqM04() {
    var list = VList.of(Result.ok(1), Result.ok(2), Result.ok(3), Result.ok(4));
    Assertions.assertEquals(Result.ok(VList.of(1, 2, 3, 4)), Result.sequence(list));
  }

  @Test
  public void traverse01() {
    var actual = Result.traverse(VList.<Integer>empty(), x -> Result.ok(x + 1));
    Assertions.assertEquals(Result.ok(VList.empty()), actual);
  }

  @Test
  public void traverse02() {
    var actual = Result.traverse(VList.of(1), x -> Result.ok(x + 1));
    Assertions.assertEquals(Result.ok(VList.of(2)), actual);
  }

  @Test
  public void traverse03() {
    var actual = Result.traverse(VList.of(1, 2), x -> Result.ok(x + 1));
    Assertions.assertEquals(Result.ok(VList.of(2, 3)), actual);
  }

  @Test
  public void traverse04() {
    var actual = Result.traverse(VList.of(1, 2, 3), x -> Result.ok(x + 1));
    Assertions.assertEquals(Result.ok(VList.of(2, 3, 4)), actual);
  }

  @Test
  public void traverse05() {
    var actual =
        Result.traverse(
            VList.of(1, 2, 3),
            x -> {
              if (x == 1) {
                return Result.err("one");
              } else {
                return Result.ok(x);
              }
            });
    Assertions.assertEquals(Result.err("one"), actual);
  }

  @Test
  public void traverse06() {
    var actual =
        Result.traverse(
            VList.of(1, 2, 3),
            x -> {
              if (x == 2) {
                return Result.err("two");
              } else {
                return Result.ok(x);
              }
            });
    Assertions.assertEquals(Result.err("two"), actual);
  }

  @Test
  public void traverse07() {
    var actual =
        Result.traverse(
            VList.of(1, 2, 3),
            x -> {
              if (x == 3) {
                return Result.err("three");
              } else {
                return Result.ok(x);
              }
            });
    Assertions.assertEquals(Result.err("three"), actual);
  }

  @Test
  public void traverse08() {
    var actual = Result.traverse(VList.of(1, 2, 3), _ -> Result.ok(42));
    Assertions.assertEquals(Result.ok(VList.of(42, 42, 42)), actual);
  }
}
