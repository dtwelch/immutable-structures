package io.github.dt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class VHashMapTests {

  /** A key whose hashCode is constant so we can force collisions. */
  record BadHash(String value) {
    @Override
    public int hashCode() {
      return 42;
    } // all keys collide (bloch: a bad (but contract-satisfying!) hashcode

    @Override
    public String toString() {
      return value;
    }
  }

  // allows us to vary the low 10 hash bits: low5 for level‑0, next5 for level‑1
  // for example, see collision03 test (asserEquals) -- next5 basically telegraphs
  // which slot of the bucket array we expect to find a given instance of the obj below
  record Key(int low5, int next5) {
    @Override
    public int hashCode() {
      return (next5 << 5) | low5;
    }

    @Override
    public String toString() {
      return "%d:%d".formatted(low5, next5);
    }
  }

  @Test
  void empty01() {
    var m = VHashMap.<String, Integer>empty();
    Assertions.assertTrue(m.isEmpty());
    Assertions.assertEquals(0, m.size());
  }

  @Test
  void insertLookup01() {
    var m = VHashMap.<String, Integer>empty().insert("x", 10);
    Assertions.assertFalse(m.isEmpty());
    Assertions.assertEquals(1, m.size());
    Assertions.assertEquals(Maybe.of(10), m.lookup("x"));
    Assertions.assertEquals(Maybe.none(), m.lookup("y"));
  }

  @Test
  void insertLookup02() {
    var m1 = VHashMap.<String, Integer>empty().insert("x", 10);
    Assertions.assertEquals(Maybe.of(10), m1.lookup("x"));
    Assertions.assertEquals(1, m1.size());

    var m2 = m1.insert("x", 99);
    Assertions.assertEquals(Maybe.of(99), m2.lookup("x"));
    Assertions.assertEquals(1, m2.size());
  }

  @Test
  void insertLookup03() {
    var m0 = VHashMap.<String, Integer>empty();
    var m1 = m0.insert("a", 1);
    var m2 = m1.insert("b", 2);

    Assertions.assertEquals(Maybe.none(), m0.lookup("a"));
    Assertions.assertEquals(Maybe.of(1), m1.lookup("a"));
    Assertions.assertEquals(Maybe.of(2), m2.lookup("b"));
    Assertions.assertEquals(Maybe.of(1), m2.lookup("a"));

    Assertions.assertEquals(0, m0.size());
    Assertions.assertEquals(1, m1.size());
    Assertions.assertEquals(2, m2.size());
  }

  @Test
  void persistence01() {
    var m0 = VHashMap.<String, Integer>empty();
    var m1 = m0.insert("x", 1);
    var m2 = m1.insert("y", 2);
    var m3 = m2.insert("z", 3);

    Assertions.assertTrue(m0.isEmpty());
    Assertions.assertEquals(Maybe.none(), m0.lookup("x"));
    Assertions.assertEquals(1, m1.size());
    Assertions.assertEquals(Maybe.of(1), m1.lookup("x"));
    Assertions.assertEquals(2, m2.size());
    Assertions.assertEquals(3, m3.size());
  }

  @Test
  void collision01() {
    // equals will fail for k1, k2 but both will have same hash (42)..
    // so we'll have a collision
    var k1 = new BadHash("foo");
    var k2 = new BadHash("bar");

    var m = VHashMap.<BadHash, Integer>empty().insert(k1, 1).insert(k2, 2);

    Assertions.assertEquals(Maybe.of(1), m.lookup(k1));
    Assertions.assertEquals(Maybe.of(2), m.lookup(k2));
    Assertions.assertEquals(2, m.size());
  }

  @Test
  void collision02() {
    var m = VHashMap.<Key, Integer>empty();

    // same low‑5 bits = 7, different higher bits (all zeros here)
    m = m.insert(new Key(7, 0), 1); // collision size 1
    m = m.insert(new Key(7, 0), 1); // overwrite (k, v), still size 1
    m = m.insert(new Key(7, 0), 2); // overwrite (k, v) again; still size 1

    m = m.insert(new Key(7, 1), 3); // after this: collision size 2
    m = m.insert(new Key(7, 2), 4); // after this: collision size 3

    /* This insert makes size == maxCollisions (=4) so the node should
    burst into a fresh Entries node at depth‑1. */
    m = m.insert(new Key(7, 3), 5);

    // all lookups must succeed after the burst
    Assertions.assertEquals(Maybe.of(3), m.lookup(new Key(7, 1)));
    Assertions.assertEquals(Maybe.of(5), m.lookup(new Key(7, 3)));
    Assertions.assertEquals(4, m.size()); // 4 distinct keys
  }

  @Test
  void collision03() {
    var m = VHashMap.<Key, Integer>empty();

    // fill first collision bucket and burst as above
    m = m.insert(new Key(31, 0), 100); // low5 = 31
    m = m.insert(new Key(31, 1), 101);
    m = m.insert(new Key(31, 2), 102);
    m = m.insert(new Key(31, 3), 103); // triggers burst (max collisions per a single bucket)

    // now insert another key that shares the same low‑5 bits
    // but different next‑5 bits; it should occupy a different
    // slot in the new (deeper) Entries bucket array, NOT be appended to the collision array
    m = m.insert(new Key(31, 17), 200);

    // so second assertEquals below asserts that key with low31, next5: 17 will occupy slot 17 in
    // the
    // level one (burst) table
    Assertions.assertEquals(Maybe.of(103), m.lookup(new Key(31, 3))); // slot 3
    Assertions.assertEquals(Maybe.of(200), m.lookup(new Key(31, 17))); // slot 17
    Assertions.assertEquals(5, m.size());
  }

  @Test
  void collision04() {
    var m =
        VHashMap.<BadHash, Integer>empty().insert(new BadHash("x"), 1).insert(new BadHash("y"), 2);

    var m2 = m.map(i -> i * 10);

    Assertions.assertEquals(Maybe.of(20), m2.lookup(new BadHash("y")));
    Assertions.assertEquals(2, m2.size());
    Assertions.assertEquals(Maybe.of(2), m.lookup(new BadHash("y")));
  }

  @Test
  void contains01() {
    var m = VHashMap.<String, Integer>empty().insert("p", 7);
    Assertions.assertTrue(m.contains("p"));
    Assertions.assertFalse(m.contains("q"));
    Assertions.assertEquals(Maybe.none(), m.lookup("q"));
  }

  @Test
  void remove01() {
    var m = VHashMap.<String, Integer>empty().insert("a", 1).insert("b", 2).insert("c", 3);

    var removed = m.remove("b");

    Assertions.assertEquals(Maybe.none(), removed.lookup("b"));
    Assertions.assertEquals(Maybe.of(1), removed.lookup("a"));
    Assertions.assertEquals(Maybe.of(3), removed.lookup("c"));
    Assertions.assertEquals(2, removed.size());
    Assertions.assertEquals(3, m.size());
  }

  @Test
  void remove02() {
    var m = VHashMap.<String, Integer>empty().insert("a", 1);

    Assertions.assertSame(m, m.remove("missing"));
  }

  @Test
  void foldl01() {
    var m = VHashMap.<String, Integer>empty().insert("a", 1).insert("b", 2).insert("c", 3);

    var folded =
        m.foldLeft(
            (acc, k, _) -> {
              acc.append(k);
              return acc;
            },
            new StringBuilder());

    Assertions.assertEquals("abc", folded.toString());
    Assertions.assertEquals(3, m.size());
  }

  @Test
  void foldl02() {
    final var N = 5_000;
    var m = VHashMap.<Integer, Integer>empty();
    for (int i = 0; i < N; i++) {
      m = m.insert(i, i + 1);
    }

    var sum = m.foldLeft((acc, _, v) -> acc + v, 0L);

    var expectedSum = ((long) (N + 1) * N) / 2;
    Assertions.assertEquals(expectedSum, sum);
    Assertions.assertEquals(N, m.size());
  }

  @Test
  void map01() {
    var m = VHashMap.<String, Integer>empty().insert("a", 1).insert("b", 2).insert("c", 3);

    var mapped = m.map(v -> "v" + v);
    Assertions.assertEquals(Maybe.of("v2"), mapped.lookup("b"));
    Assertions.assertEquals(Maybe.none(), mapped.lookup("d"));
    Assertions.assertEquals(3, mapped.size());

    // check original map not mutated
    Assertions.assertEquals(Maybe.of(2), m.lookup("b"));
  }

  @Test
  public void filter01() {
    var m = VHashMap.empty();
    var res = m.filterFor((_, _) -> true);

    Assertions.assertEquals(0, res.size());
  }

  @Test
  public void filter02() {
    var m =
        VHashMap.<Integer, Integer>empty() //
            .insert(1, 10)
            .insert(2, 20);

    var res = m.filterFor((_, v) -> v < 0);

    Assertions.assertEquals(0, res.size());
    Assertions.assertNotSame(m, res);
  }

  @Test
  public void filter03() {
    var m =
        VHashMap.empty() //
            .insert("a", "x")
            .insert("b", "y");

    var res = m.filterFor((_, _) -> true);
    Assertions.assertEquals(m, res);
  }

  @Test
  public void filter04() {
    var m =
        VHashMap.<String, Integer>empty().insert("odd1", 1).insert("even2", 2).insert("odd3", 3);

    BiPredicate<String, Integer> isOdd = (_, v) -> v % 2 == 1;

    var res = m.filterFor(isOdd);
    var expected =
        VHashMap.empty() //
            .insert("odd1", 1)
            .insert("odd3", 3);

    Assertions.assertEquals(expected, res);
    Assertions.assertEquals(2, res.size());
  }

  @Test
  public void filter05() {
    var original = VHashMap.<Integer, String>empty().insert(1, "one").insert(2, "two");

    var filtered = original.filterFor((k, _) -> k == 2);

    Assertions.assertEquals(2, original.size());
    Assertions.assertEquals(1, filtered.size());
  }

  @Test
  public void filter06() {
    var m = VHashMap.<String, String>empty().insert("nonnull", "x").insert("isnull", null);

    var res = m.filterFor((_, v) -> v != null);
    var expected = VHashMap.<String, String>empty().insert("nonnull", "x");
    Assertions.assertEquals(expected, res);
  }

  @Test
  public void toList01() {
    var m = VHashMap.<String, Integer>empty().insert("x", 10).insert("y", 20).insert("z", 30);
    var sorted = m.toList().sortBy(Comparator.comparing(Pair::first));

    Assertions.assertEquals(3, sorted.length());
    // Assertions.assertEquals("x", sorted.get(0).first());
    // Assertions.assertEquals(10, sorted.get(0).second());
  }

  @Test
  void toListCollision01() {
    // tolist with some collisions first
    var m =
        VHashMap.<BadHash, Integer>empty()
            .insert(new BadHash("alpha"), 11)
            .insert(new BadHash("beta"), 22)
            .insert(new BadHash("gamma"), 33);

    var asList =
        m.toList().map(Pair::first).map(BadHash::toString).sortBy(Comparator.naturalOrder());

    // deterministic multiset comparison
    // (so if several keys share the same bucket,
    // toList() still contains every key exactly once.)
    Assertions.assertEquals(VList.of("alpha", "beta", "gamma"), asList);
  }

  @Test
  public void iteratorEmpty01() {
    var m = VHashMap.<String, Integer>empty();
    Assertions.assertFalse(m.iterator().hasNext());
  }

  @Test
  public void iterator01() {
    var m = VHashMap.<String, Integer>empty().insert("a", 1).insert("b", 2);

    var iter = m.iterator();
    var collected = new ArrayList<String>();
    while (iter.hasNext()) {
      collected.add(iter.next().first());
    }
    Collections.sort(collected);
    Assertions.assertEquals(List.of("a", "b"), collected);
  }

  @Test
  void iterator02() {
    var m = VHashMap.<BadHash, Integer>empty();
    // force some collisions
    for (int i = 0; i < 10; i++) {
      m = m.insert(new BadHash("k" + i), i);
    }

    List<String> got = new ArrayList<>();
    m.iterator().forEachRemaining(p -> got.add(p.first().toString()));

    // order not guaranteed – compare as sets
    Assertions.assertEquals(10, got.size(), "iterator must yield the correct cardinality");
    Assertions.assertEquals(
        Set.of("k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9"), new HashSet<>(got));
  }

  @Test
  void roundTripRandom01() {
    var rnd = new java.util.Random(0);
    var javaMap = new java.util.HashMap<Integer, Integer>();
    var v = VHashMap.<Integer, Integer>empty();

    for (int i = 0; i < 10_000; i++) {
      int k = rnd.nextInt(3_000);
      int v0 = rnd.nextInt();
      javaMap.put(k, v0);
      v = v.insert(k, v0);
    }
    // size and equality against a std HashMap
    Assertions.assertEquals(javaMap.size(), v.size());
    for (var e : javaMap.entrySet()) {
      Assertions.assertEquals(Maybe.of(e.getValue()), v.lookup(e.getKey()));
    }
  }

  @Test
  void stats01() {
    var a = new BadHash("a");
    var b = new BadHash("b");
    var c = new BadHash("c");

    var m =
        VHashMap.<BadHash, Integer>empty()
            .insert(a, 1)
            .insert(b, 2)
            .insert(c, 3); // all three keys collide,
    // but below burst threshold

    var st = m.stats();

    Assertions.assertEquals(3, m.size());
    Assertions.assertEquals(2, st.numCollisions());
    Assertions.assertTrue(st.maxDepth() >= 1);
  }

  @Test
  void equals01() {
    var a = VHashMap.of(Pair.of("x", 1), Pair.of("y", 2));
    var b = VHashMap.of(Pair.of("y", 2), Pair.of("x", 1));
    Assertions.assertEquals(a, b);
  }

  @Test
  void equals02() {
    var a = VHashMap.singleton("x", 1);
    var b = VHashMap.empty();
    Assertions.assertNotEquals(a, b);
  }

  @Test
  void equals03() {
    var a = VHashMap.singleton("k", 1);
    var b = VHashMap.singleton("k", 2);
    Assertions.assertNotEquals(a, b);
  }

  @Test
  void equals04() {
    var a = VHashMap.singleton(1, "one");
    var b = VHashMap.singleton(1, "one");
    Assertions.assertEquals(a, b);
    Assertions.assertEquals(b, a);
  }

  @Test
  void equals05() {
    var a = VHashMap.singleton(1, "one");
    var b = VHashMap.singleton(1, "one");
    var c = VHashMap.singleton(1, "one");
    Assertions.assertEquals(a, b);
    Assertions.assertEquals(b, c);
    Assertions.assertEquals(a, c);
  }

  @Test
  void equals06() {
    var m1 = VHashMap.<Number, String>empty().insert(1, "one");
    var m2 = VHashMap.<Object, String>empty().insert(1, "one");
    Assertions.assertEquals(m1, m2);
  }

  @Test
  void hashCode01() {
    var a = VHashMap.singleton(Pair.of("p", 5), Pair.of("q", 6));
    var b = VHashMap.singleton(Pair.of("q", 6), Pair.of("p", 5));
    Assertions.assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void hashCode02() {
    var a = VHashMap.singleton("p", 5);
    var b = VHashMap.singleton("p", 6);
    Assertions.assertNotEquals(a.hashCode(), b.hashCode());
  }
}
