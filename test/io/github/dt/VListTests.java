package io.github.dt;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public final class VListTests {

  @Test
  public void emptyAppendEmptyShouldReturnEmpty() {
    var list1 = VList.<Integer>empty();
    var list2 = VList.<Integer>empty();
    var appended = list1.append(list2);
    var expected = VList.<Integer>empty();
    Assertions.assertEquals(expected, appended);
  }

  @Test
  public void headOnEmptyShouldThrow() {
    var empty = VList.empty();
    Assertions.assertThrows(IllegalArgumentException.class, empty::head);
  }

  @Test
  public void tailOnEmptyShouldThrow() {
    var empty = VList.empty();
    Assertions.assertThrows(IllegalArgumentException.class, empty::tail);
  }

  @Test
  public void headMaybeOnEmptyShouldReturnNone() {
    var list = VList.<String>empty();
    Assertions.assertTrue(list.headMaybe().isEmpty());
  }

  @Test
  public void headMaybeOnNonEmptyShouldReturnSome() {
    var list = VList.of("hello", "world");
    Assertions.assertEquals(Maybe.of("hello"), list.headMaybe());
  }

  @Test
  public void emptyAppendNonEmptyShouldReturnNonEmpty() {
    var list1 = VList.<Integer>empty();
    var list2 = VList.of(1, 2, 3);
    var appended = list1.append(list2);
    var expected = VList.of(1, 2, 3);
    Assertions.assertEquals(expected, appended);
  }

  @Test
  public void nonEmptyAppendEmptyShouldReturnNonEmpty() {
    var list1 = VList.of(1, 2, 3);
    var list2 = VList.<Integer>empty();
    var appended = list1.append(list2);
    var expected = VList.of(1, 2, 3);
    Assertions.assertEquals(expected, appended);
  }

  @Test
  public void nonEmptyAppendNonEmptyShouldReturnCombinedList() {
    var list1 = VList.of(1, 2, 3);
    var list2 = VList.of(4, 5, 6);
    var appended = list1.append(list2);
    var expected = VList.of(1, 2, 3, 4, 5, 6);
    Assertions.assertEquals(expected, appended);
  }

  @Test
  public void appendShouldUpdateSize() {
    var list1 = VList.of(1, 2, 3);
    var list2 = VList.of(4, 5);
    var appended = list1.append(list2);
    var expectedSize = 5;
    Assertions.assertEquals(expectedSize, appended.length());
  }

  @Test
  public void appendEmptyToEmptyShouldMaintainSize() {
    var list1 = VList.<Integer>empty();
    var list2 = VList.<Integer>empty();
    var appended = list1.append(list2);
    var expectedSize = 0;
    Assertions.assertEquals(expectedSize, appended.length());
  }

  @Test
  public void appendShouldBeImmutable() {
    var list1 = VList.of(1, 2, 3);
    var list2 = VList.of(4, 5, 6);
    var appended = list1.append(list2);
    var expected1 = VList.of(1, 2, 3);
    var expected2 = VList.of(4, 5, 6);
    Assertions.assertEquals(expected1, list1);
    Assertions.assertEquals(expected2, list2);
    Assertions.assertEquals(VList.of(1, 2, 3, 4, 5, 6), appended);
  }

  @Test
  public void appendDifferentOrderShouldMaintainCombinedOrder() {
    var list1 = VList.of(3, 2, 1);
    var list2 = VList.of(6, 5, 4);
    var appended = list1.append(list2);
    var expected = VList.of(3, 2, 1, 6, 5, 4);
    Assertions.assertEquals(expected, appended);
  }

  @Test
  public void appendLargeListsShouldHaveCorrectSize() {
    var list1 = VList.<Integer>empty();
    for (var i = 0; i < 1000; i++) {
      list1 = list1.append(VList.of(i));
    }
    var list2 = VList.<Integer>empty();
    for (var i = 1000; i < 2000; i++) {
      list2 = list2.append(VList.of(i));
    }
    var appended = list1.append(list2);
    var expectedSize = 2000;
    Assertions.assertEquals(expectedSize, appended.length());
  }

  @Test
  public void takeOnEmptyShouldReturnEmpty() {
    Assertions.assertEquals(VList.empty(), VList.<Integer>empty().take(3));
  }

  @Test
  public void takeNonPositiveShouldReturnEmpty() {
    Assertions.assertEquals(VList.empty(), VList.of(1, 2, 3).take(0));
  }

  @Test
  public void takeShouldReturnPrefix() {
    Assertions.assertEquals(VList.of(1, 2), VList.of(1, 2, 3, 4).take(2));
  }

  @Test
  public void takePastLengthShouldReturnWholeList() {
    Assertions.assertEquals(VList.of(1, 2, 3), VList.of(1, 2, 3).take(10));
  }

  @Test
  public void prependAll_withEmptyPrefix_returnsOriginalList() {
    var xs = VList.of(1, 2, 3);
    var prefix = VList.<Integer>empty();

    var result = xs.prependAll(prefix);

    Assertions.assertEquals(
        xs, result, "Prepending an empty prefix should return the original list");
  }

  @Test
  public void prependAll_onEmptyList_returnsPrefix() {
    var xs = VList.<Integer>empty();
    var prefix = VList.of(1, 2, 3);

    var result = xs.prependAll(prefix);

    Assertions.assertEquals(
        prefix, result, "Prepending onto an empty list should yield the prefix");
  }

  @Test
  public void prependAll_preservesOrderOfBothLists() {
    var prefix = VList.of(1, 2);
    var xs = VList.of(3, 4);

    var result = xs.prependAll(prefix);

    var expected = VList.of(1, 2, 3, 4);
    Assertions.assertEquals(expected, result, "Elements should appear as prefix ++ xs");
  }

  @Test
  public void prependAll_doesNotMutateOperands() {
    var prefix = VList.of(1, 2);
    var xs = VList.of(3, 4);

    var result = xs.prependAll(prefix);

    Assertions.assertEquals(VList.of(1, 2), prefix, "prefix should remain unchanged");
    Assertions.assertEquals(VList.of(3, 4), xs, "xs should remain unchanged");

    Assertions.assertEquals(VList.of(1, 2, 3, 4), result, "Result should be prefix ++ xs");
  }

  @Test
  public void reverseEmptyShouldReturnEmpty() {
    var empty = VList.<Integer>empty();
    Assertions.assertEquals(empty, empty.reverse());
  }

  @Test
  public void reverseSingleElementShouldBeItself() {
    var list = VList.of(42);
    Assertions.assertEquals(list, list.reverse());
  }

  @Test
  public void reverseShouldInvertOrder() {
    var list = VList.of(1, 2, 3);
    var expected = VList.of(3, 2, 1);
    Assertions.assertEquals(expected, list.reverse());
  }

  @Test
  public void filterEmptyShouldReturnEmpty() {
    var list = VList.<Integer>empty();
    Assertions.assertEquals(list, list.filter(x -> x % 2 == 0));
  }

  @Test
  public void filterNoneShouldReturnEmpty() {
    var list = VList.of(1, 3, 5);
    var result = list.filter(x -> x % 2 == 0);
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  public void filterSomeShouldReturnMatchingOnly() {
    var list = VList.of(1, 2, 3, 4);
    var expected = VList.of(2, 4);
    Assertions.assertEquals(expected, list.filter(x -> x % 2 == 0));
  }

  @Test
  public void foldLeftShouldAccumulateCorrectly() {
    var list = VList.of(1, 2, 3, 4);
    int sum = list.foldLeft(0, Integer::sum);
    Assertions.assertEquals(10, sum);
  }

  @Test
  public void foldRightShouldAccumulateCorrectly() {
    var list = VList.of(1, 2, 3, 4);
    int sum = list.foldRight(0, Integer::sum);
    Assertions.assertEquals(10, sum);
  }

  @Test
  public void foldLeftShouldPreserveOrder() {
    var list = VList.of("a", "b", "c");
    String concat = list.foldLeft("", (acc, x) -> acc + x);
    Assertions.assertEquals("abc", concat);
  }

  @Test
  public void foldRightShouldPreserveOrder() {
    var list = VList.of("a", "b", "c");
    String concat = list.foldRight("", (x, acc) -> x + acc);
    Assertions.assertEquals("abc", concat);
  }

  @Test
  public void mapShouldApplyFunctionCorrectly() {
    var list = VList.of(1, 2, 3);
    var doubled = list.map(x -> x * 2);
    var expected = VList.of(2, 4, 6);
    Assertions.assertEquals(expected, doubled);
  }

  @Test
  public void existsShouldFindMatchingElement() {
    var list = VList.of(1, 2, 3);
    Assertions.assertTrue(list.exists(x -> x == 2));
  }

  @Test
  public void existsShouldReturnFalseIfNotFound() {
    var list = VList.of(1, 2, 3);
    Assertions.assertFalse(list.exists(x -> x == 5));
  }

  @Test
  public void anyMatchShouldReturnTrueIfMatchExists() {
    var list = VList.of("a", "b", "c");
    Assertions.assertTrue(list.exists(s -> s.equals("b")));
  }

  @Test
  public void anyMatchShouldReturnFalseIfNoMatch() {
    var list = VList.of("a", "b", "c");
    Assertions.assertFalse(list.exists(s -> s.equals("z")));
  }

  @Test
  public void mkStringSingleElement() {
    var list = VList.of(1);
    var result = list.mkString("[", ", ", "]");
    Assertions.assertEquals("[1]", result);
  }

  @Test
  public void mkStringTwoElements() {
    var list = VList.of(1, 2);
    var result = list.mkString("[", ", ", "]");
    Assertions.assertEquals("[1, 2]", result);
  }

  @Test
  public void mkStringThreeElements() {
    var list = VList.of(1, 2, 3);
    var result = list.mkString("[", ", ", "]");
    Assertions.assertEquals("[1, 2, 3]", result);
  }

  @Test
  public void mkStringEmptyList() {
    var list = VList.empty();
    var result = list.mkString("[", ", ", "]");
    Assertions.assertEquals("[]", result);
  }

  @Test
  public void emptyListToStringShouldIncludeBrackets() {
    Assertions.assertEquals("[]", VList.empty().toString());
  }

  @Test
  public void mkStringCustomDelimiters() {
    var list = VList.of("a", "b", "c");
    var result = list.mkString("<", "|", ">");
    Assertions.assertEquals("<a|b|c>", result);
  }

  @Test
  public void mkStringOneElementCustom() {
    var list = VList.of(42);
    var result = list.mkString("{", " - ", "}");
    Assertions.assertEquals("{42}", result);
  }

  @Test
  public void splitHalf01() {
    var xs = VList.<Integer>empty();
    var halves = VList.splitHalf(xs);

    Assertions.assertEquals(xs, halves.first());
    Assertions.assertTrue(halves.second().isEmpty());
  }

  @Test
  public void splitHalf02() {
    var xs = VList.of(99);
    var halves = VList.splitHalf(xs);

    Assertions.assertEquals(xs, halves.first());
    Assertions.assertTrue(halves.second().isEmpty());
  }

  @Test
  public void splitHalf03() {
    var xs = VList.of(1, 2, 3, 4);
    var halves = VList.splitHalf(xs);

    Assertions.assertEquals(xs, halves.first().append(halves.second()));
  }

  @Test
  public void splitHalf04() {
    var xs = VList.of(1, 2, 3, 4, 5);
    var halves = VList.splitHalf(xs);

    Assertions.assertEquals(VList.of(1, 2), halves.first());
    Assertions.assertEquals(VList.of(3, 4, 5), halves.second());
    Assertions.assertEquals(xs, halves.first().append(halves.second()));
  }

  @Test
  public void splitHalf05() {
    var xs = VList.of(5, 4);
    var halves = VList.splitHalf(xs);

    Assertions.assertEquals(VList.of(5), halves.first());
    Assertions.assertEquals(VList.of(4), halves.second());
  }

  @Test
  public void sort01() {
    var xs = VList.<Integer>empty();
    Assertions.assertEquals(xs, VList.sort(xs));
  }

  @Test
  public void sort02() {
    var xs = VList.of(42);
    var sorted = VList.sort(xs);
    Assertions.assertSame(xs, sorted);
  }

  @Test
  public void sort03() {
    var xs = VList.of(1, 2, 3, 4, 5);
    Assertions.assertEquals(xs, VList.sort(xs));
  }

  @Test
  public void sort04() {
    var xs = VList.of(5, 4, 3, 2, 1);
    var expected = VList.of(1, 2, 3, 4, 5);
    Assertions.assertEquals(expected, VList.sort(xs));
  }

  @Test
  public void sort05() {
    var xs = VList.of(3, 1, 2, 3, 1, 2);
    var expected = VList.of(1, 1, 2, 2, 3, 3);
    Assertions.assertEquals(expected, VList.sort(xs));
  }

  @Test
  public void sort06() {
    var xs = VList.of(1, 2, 3, 4);
    var descendingOrd = Comparator.<Integer>reverseOrder();

    var viaStatic = VList.sort(xs, descendingOrd);
    var viaInstance = xs.sortBy(descendingOrd);
    var expected = VList.of(4, 3, 2, 1);

    Assertions.assertEquals(expected, viaStatic);
    Assertions.assertEquals(expected, viaInstance);
  }

  @Test
  public void sort07() {
    var xs = VList.of(4, 3, 2, 1);
    var _ = xs.sortBy(Comparator.naturalOrder()); // ignored
    Assertions.assertEquals(VList.of(4, 3, 2, 1), xs);
  }

  @RepeatedTest(5) // runs with fresh random data a few times
  public void sortRandomIntegersShouldMatchArraysSort() {
    var rnd = new Random();
    var n = 1_000;

    // note: seems to always run 5 times
    // System.out.println("RUNNING\n\n\n");

    var raw = rnd.ints(n, -10_000, 10_000).boxed().toArray(Integer[]::new);

    var xs = VList.from(Arrays.asList(raw));
    var sorted = VList.sort(xs);

    Arrays.sort(raw);
    var expected = VList.from(Arrays.asList(raw));

    Assertions.assertEquals(expected, sorted);
    Assertions.assertEquals(n, sorted.length());
  }

  @Test
  public void eqHash01() {
    var a = VList.of("x", "y", "z");
    var b = VList.of("x", "y", "z");
    var c = VList.of("z", "y", "x");

    Assertions.assertEquals(a, b);
    Assertions.assertEquals(a.hashCode(), b.hashCode());

    Assertions.assertNotEquals(a, c);
  }

  @Test
  public void findOnEmptyShouldReturnNone() {
    var list = VList.<Integer>empty();
    var result = list.find(x -> x == 1);
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  public void findShouldReturnFirstMatchingElement() {
    var list = VList.of(1, 2, 3, 2);
    var result = list.find(x -> x == 2);
    Assertions.assertEquals(Maybe.of(2), result);
  }

  @Test
  public void findShouldReturnNoneWhenNoMatch() {
    var list = VList.of(1, 2, 3);
    var result = list.find(x -> x == 42);
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  public void findMapShouldReturnFirstSome() {
    var list = VList.of("nope", "123", "456");

    var result =
        list.findMap(
            s -> {
              try {
                return Maybe.of(Integer.parseInt(s));
              } catch (NumberFormatException e) {
                return Maybe.none();
              }
            });

    Assertions.assertEquals(Maybe.of(123), result);
  }

  @Test
  public void findMapShouldReturnNoneWhenMapperAlwaysNone() {
    var list = VList.of("a", "b", "c");

    var result = list.findMap(s -> Maybe.<Integer>none());

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  public void findMapShouldPreferEarlierMatches() {
    var list = VList.of("first", "second", "third");

    var result =
        list.findMap(
            s ->
                switch (s) {
                  case "second" -> Maybe.of("HIT-SECOND");
                  case "third" -> Maybe.of("HIT-THIRD");
                  default -> Maybe.none();
                });

    Assertions.assertEquals(Maybe.of("HIT-SECOND"), result);
  }
}
