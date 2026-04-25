package io.github.dt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class VHashSetTests {

  @Test
  void size01() {
    Assertions.assertEquals(0, VHashSet.<Integer>empty().size());
  }

  @Test
  void insert01() {
    var s = VHashSet.of(42);
    Assertions.assertEquals(1, s.size());
  }

  @Test
  void insert02() {
    var s = VHashSet.<Integer>empty().insert(1).insert(1);
    Assertions.assertEquals(1, s.size());
  }

  @Test
  void remove01() {
    var s = VHashSet.of(1, 2, 3);

    var removed = s.remove(2);

    Assertions.assertFalse(removed.contains(2));
    Assertions.assertTrue(removed.contains(1));
    Assertions.assertTrue(removed.contains(3));
    Assertions.assertEquals(2, removed.size());
    Assertions.assertEquals(3, s.size());
  }

  @Test
  void remove02() {
    var s = VHashSet.of(1, 2, 3);

    Assertions.assertSame(s, s.remove(99));
  }

  @Test
  void immutability01() {
    var a = VHashSet.of(1);
    var b = a.insert(2); // should not mutate a
    Assertions.assertEquals(1, a.size());
    Assertions.assertEquals(2, b.size());
  }

  @Test
  void exists01() {
    var s = VHashSet.of("a", "b", "c");
    Assertions.assertTrue(s.exists("b"::equals));
  }

  @Test
  void exists02() {
    var s = VHashSet.of("a", "b", "c");
    Assertions.assertFalse(s.exists("z"::equals));
  }

  @Test
  void forall01() {
    // vacuously true
    Assertions.assertTrue(VHashSet.<Integer>empty().forall(x -> x > 0));
  }

  @Test
  void forall02() {
    var s = VHashSet.of(2, 4, 6, 8);
    Assertions.assertTrue(s.forall(x -> x % 2 == 0));
  }

  @Test
  void forall03() {
    var s = VHashSet.of(2, 3, 4);
    Assertions.assertFalse(s.forall(x -> x % 2 == 0));
  }

  @Test
  void forallExistsLaw() {
    var s = VHashSet.of(1, 2, 3);
    Predicate<Integer> p = x -> x > 1;
    Assertions.assertEquals(!s.exists(p.negate()), s.forall(p));
  }

  @Test
  void union01() {
    var left = VHashSet.of(1, 2);
    var right = VHashSet.of(2, 3);
    var both = left.union(right);
    Assertions.assertEquals(3, both.size());
  }

  @Test
  void union02() {
    var left = VHashSet.of(1, 2);
    var right = VHashSet.of(3, 4);
    var both = left.union(right);
    Assertions.assertEquals(2, left.size()); // left unchanged
    Assertions.assertEquals(2, right.size()); // right unchanged
    Assertions.assertEquals(4, both.size());
  }

  @Test
  void union03() {
    // commutativity
    var left = VHashSet.of(1, 2, 3);
    var right = VHashSet.of(3, 4, 5);
    Assertions.assertEquals(left.union(right), right.union(left));
  }

  @Test
  void union04() {
    // associtivity
    var a = VHashSet.of(1, 2);
    var b = VHashSet.of(2, 3);
    var c = VHashSet.of(3, 4);
    Assertions.assertEquals(a.union(b).union(c), a.union(b.union(c)));
  }

  @Test
  void union05() {
    // union w/ empty is identity
    var s = VHashSet.of("a", "b");
    Assertions.assertEquals(s, s.union(VHashSet.empty()));
  }

  @Test
  void difference01() {
    var left = VHashSet.of(1, 2, 3);
    var right = VHashSet.of(2);
    var diff = left.difference(right);

    Assertions.assertEquals(3, left.size());
    Assertions.assertEquals(1, right.size());
    Assertions.assertEquals(2, diff.size());
    Assertions.assertEquals(VHashSet.of(1, 3), diff);
  }

  @Test
  void difference02() {
    var s = VHashSet.of("a", "b");
    Assertions.assertEquals(s, s.difference(VHashSet.empty()));
  }

  @Test
  void difference03() {
    var left = VHashSet.of(1, 2);
    var right = VHashSet.of(1, 2, 3);
    Assertions.assertEquals(0, left.difference(right).size());
  }

  @Test
  void difference04() {
    var left = VHashSet.of(1, 2);
    var right = VHashSet.of(3, 4);
    Assertions.assertEquals(left, left.difference(right));
  }

  @Test
  void difference05() {
    // s \ s = empty_set
    var s = VHashSet.of(1, 2, 3);
    Assertions.assertEquals(VHashSet.empty(), s.difference(s));
  }

  @Test
  void contains01() {
    Assertions.assertTrue(VHashSet.of("x").contains("x"));
  }

  @Test
  void contains02() {
    Assertions.assertFalse(VHashSet.of("x").contains("y"));
  }

  @Test
  void find01() {
    var s = VHashSet.of(10, 20);
    Assertions.assertEquals(Maybe.of(10), s.find(x -> x == 10));
  }

  @Test
  void find02() {
    Assertions.assertTrue(VHashSet.of(1, 2, 3).find(x -> x > 100).isEmpty());
  }

  @Test
  void subsetOf01() {
    Assertions.assertTrue(VHashSet.<Integer>empty().subsetOf(VHashSet.of(1)));
  }

  @Test
  void subsetOf02() {
    var s = VHashSet.of(1, 2, 3);
    Assertions.assertTrue(s.subsetOf(s));
  }

  @Test
  void subsetOf03() {
    Assertions.assertFalse(VHashSet.of(1, 2, 3).subsetOf(VHashSet.of(1, 2)));
    Assertions.assertTrue(VHashSet.of(1, 2).subsetOf(VHashSet.of(1, 2, 3)));
    Assertions.assertTrue(VHashSet.of(1).subsetOf(VHashSet.of(1)));
  }

  @Test
  void flatMap01() {
    var src = VHashSet.of(1, 2);
    var out = src.flatMap(x -> VHashSet.of(x, x + 10));
    Assertions.assertEquals(VHashSet.of(1, 2, 11, 12), out);
  }

  @Test
  void toList01() { // "round trip" test
    var s = VHashSet.of("a", "b", "c");
    var round = VHashSet.from(s.toList());
    Assertions.assertEquals(s, round);
  }

  @Test
  void iterator01() {
    var s = VHashSet.of("x", "y", "z");
    var seen = new ArrayList<>();
    for (var x : s) {
      seen.add(x);
    }
    Assertions.assertEquals(Set.of("x", "y", "z"), new HashSet<>(seen));
  }

  @Test
  void iterator02() {
    var it = VHashSet.of(1, 2).iterator();
    it.next();
    Assertions.assertThrows(UnsupportedOperationException.class, it::remove);
  }

  @Test
  void iterator03() {
    var it = VHashSet.<Integer>empty().iterator();
    Assertions.assertFalse(it.hasNext());
    Assertions.assertThrows(java.util.NoSuchElementException.class, it::next);
  }

  @Test
  void algebra01() { // (s U t) \ t = s \ t
    var s = VHashSet.of(1, 2);
    var t = VHashSet.of(2, 3);
    Assertions.assertEquals(s.union(t).difference(t), s.difference(t));
  }

  @Test
  void iteratorSnapshotImmutability() {
    var original = VHashSet.of(1, 2, 3);
    var it = original.iterator();
    var mutated = original.insert(4);

    // iterate after seeming mutation
    var seen = new ArrayList<Integer>();
    it.forEachRemaining(seen::add);

    Assertions.assertEquals(List.of(1, 2, 3).size(), seen.size());
    Assertions.assertEquals(4, mutated.size());
  }

  @Test
  void ofVarArgs01() {
    var s = VHashSet.of(1, 2, 3, 4);
    Assertions.assertEquals(4, s.size());
  }

  @Test
  void fromIterable01() {
    var s = VHashSet.from(List.of(1, 2, 3, 3)); // duplicate 3
    Assertions.assertEquals(3, s.size());
  }

  @Test
  void bulkInsert01() {
    var s = VHashSet.<Integer>empty();
    for (int i = 0; i < 1_000; i++) {
      s = s.insert(i);
    }
    Assertions.assertEquals(1_000, s.size());
  }

  @Test
  void equals01() {
    var a = VHashSet.of(1, 2, 3);
    var b = VHashSet.from(List.of(3, 2, 1));
    Assertions.assertEquals(a, b);
  }

  @Test
  void equals02() {
    var a = VHashSet.of(1, 2);
    var b = VHashSet.of(1, 2, 3);
    Assertions.assertNotEquals(a, b);
  }

  @Test
  void hashCode01() {
    var a = VHashSet.of(1, 2, 3);
    var b = VHashSet.of(3, 1, 2);
    Assertions.assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void hashCode02() {
    var a = VHashSet.of("x", "y");
    var b = VHashSet.of("y", "x");
    Assertions.assertEquals(a.hashCode(), b.hashCode());
  }
}
