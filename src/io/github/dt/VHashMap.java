package io.github.dt;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import io.github.dt.VHashMap.Node.Collision;

/// An immutable hash map based on a hash array mapped trie.
///
/// This implementation is based in part on Lean 4's persistent hash map. Removal currently
/// rebuilds the map, so it is `O(n)`.
public final class VHashMap<K, V> implements Iterable<Pair<K, V>> {

  private static final VHashMap<?, ?> Empty =
      new VHashMap<>(new Node.Entries<>(mkEmptyEntriesArray()), 0);

  private final Node<K, V> root;
  private final int size; // # of kv pairs

  private VHashMap(Node<K, V> root, int size) {
    this.root = root;
    this.size = size;
  }

  sealed interface MapEntry<A, B, C> {
    /// a bucket holding exactly one key-value (k : A, v : B) pair
    record Entry<A, B, C>(A key, B val) implements MapEntry<A, B, C> {}

    /// a bucket that references (points down) into a child node
    record Ref<A, B, C>(C node) implements MapEntry<A, B, C> {}

    /// the empty bucket
    final class Null<A, B, C> implements MapEntry<A, B, C> {

      static final MapEntry<?, ?, ?> Instance = new Null<>();

      private Null() {}
    }
  }

  private static <A, B, C> MapEntry<A, B, C> entry(A key, B val) {
    return new MapEntry.Entry<>(key, val);
  }

  private static <A, B, C> MapEntry<A, B, C> ref(C node) {
    return new MapEntry.Ref<>(node);
  }

  @SuppressWarnings("unchecked")
  private static <A, B, C> MapEntry<A, B, C> null_() {
    return (MapEntry.Null<A, B, C>) MapEntry.Null.Instance;
  }

  sealed interface Node<A, B> {
    record Entries<A, B>(MapEntry<A, B, Node<A, B>>[] es) implements Node<A, B> {
      public Entries {
        Objects.requireNonNull(es);
      }
    }

    record Collision<A, B>(A[] ks, B[] vs) implements Node<A, B> {
      public Collision {
        if (ks.length != vs.length) {
          throw new IllegalArgumentException("arity mismatch");
        }
      }
    }

    default boolean isEmpty() {
      return switch (this) {
        case Collision<A, B> _ -> false;
        case Entries(var es) ->
            Arrays.stream(es)
                .allMatch(
                    entry ->
                        switch (entry) {
                          case MapEntry.Entry(_, _) -> false;
                          case MapEntry.Ref(var n) -> n.isEmpty();
                          case MapEntry.Null<?, ?, ?> _ -> true;
                        });
      };
    }
  }

  private static <A, B> Node<A, B> entries(MapEntry<A, B, Node<A, B>>[] es) {
    return new Node.Entries<>(es);
  }

  private static <A, B> Node<A, B> collision(A[] ks, B[] vs) {
    return new Node.Collision<>(ks, vs);
  }

  private static final int shift = 5; // bits per level
  private static final int branching = 1 << shift; // branching factor
  private static final int maxDepth = 7;
  private static final int maxCollisions = 4;

  @SuppressWarnings("unchecked")
  private static <A, B> MapEntry<A, B, Node<A, B>>[] mkEmptyEntriesArray() {
    var result = (MapEntry<A, B, Node<A, B>>[]) Array.newInstance(MapEntry.class, branching);
    Arrays.fill(result, null_());
    return result;
  }

  /// O(branching-factor) = O(1) - returns true only if this map is empty.
  public boolean isEmpty() {
    return root.isEmpty();
  }

  private static <A, B> Node<A, B> mkEmptyEntries() {
    return entries(mkEmptyEntriesArray());
  }

  private static int div2Shift(int i, int shiftAmt) {
    return i >>> shiftAmt;
  }

  // mask off all low bits except the low 'shift' bits.
  private static int mod2Shift(int i, int shiftAmt) {
    return i & ((1 << shiftAmt) - 1);
  }

  /// O(n) - shallow‑clone src with `src[idx]` replaced by val (shallow to retain structural
  /// sharing)
  private static <T> T[] copyAndSet(T[] src, int idx, T val) {
    var dst = src.clone();
    dst[idx] = val;
    return dst;
  }

  /// O(n) - return a new array one element longer, with val appended.
  private static <T> T[] copyAndAppend(T[] src, T val) {
    var n = src.length;
    var dst = Arrays.copyOf(src, n + 1);
    dst[n] = val;
    return dst;
  }

  // note: IsCollisionNode inductive. captures same idea as
  // the following runtime check: node instanceof Node.Collision<?, ?>

  @SuppressWarnings("unchecked")
  public static <A, B> VHashMap<A, B> empty() {
    return (VHashMap<A, B>) Empty;
  }

  public static <K, V> VHashMap<K, V> singleton(K k1, V v1) {
    return VHashMap.<K, V>empty().insert(k1, v1);
  }

  /// A map with two entries.
  @SafeVarargs
  public static <K, V> VHashMap<K, V> of(Pair<K, V>... kvs) {
    var result = VHashMap.<K, V>empty();
    for (var kv : kvs) {
      result = result.insert(kv.first(), kv.second());
    }
    return result;
  }

  /// Build a map from any [Iterable<Pair>]. Useful for streams or collections of tuples.
  public static <K, V> VHashMap<K, V> from(Iterable<Pair<K, V>> entries) {
    var m = VHashMap.<K, V>empty();
    for (var e : entries) {
      m = m.insert(e.first(), e.second());
    }
    return m;
  }

  /// O(n) - converts `mutMap` into an immutable hashmap.
  public static <K, V> VHashMap<K, V> from(Map<K, V> mutMap) {
    var m = VHashMap.<K, V>empty();
    for (var kv : mutMap.entrySet()) {
      m = m.insert(kv.getKey(), kv.getValue());
    }
    return m;
  }

  /// O(1*) - insert a `(k, v)` pair into a collision node; if `k` exists at position `i`, replace
  /// `v` else append `(k, v)`.
  private static <A, B> Node<A, B> insertAtCollisionNodeAux(Node<A, B> n, int i, A k, B v) {
    return switch (n) {
      // collision node is correct by construction -- throws exception if the
      // size of keys and vals are different
      case Node.Collision(var keys, var vals) -> {
        if (i < keys.length) {
          // check existing slot to see if we have a collision
          var k_p = keys[i];
          if (Objects.equals(k, k_p)) {
            // found the colliding node
            // so return a new collision node with the key and val arrays
            // updated to reflect the new (k, v) that replaced the old (k, v)
            yield collision(copyAndSet(keys, i, k), copyAndSet(vals, i, v));
          } else {
            yield insertAtCollisionNodeAux(n, i + 1, k, v);
          }
        } else {
          yield collision(copyAndAppend(keys, k), copyAndAppend(vals, v));
        }
      }
      case Node.Entries(_) -> throw new IllegalStateException();
    };
  }

  // kickoff
  private static <A, B> Node<A, B> insertAtCollisionNode(Node<A, B> n, A k, B v) {
    return insertAtCollisionNodeAux(n, 0, k, v);
  }

  private static <A, B> int collisionNodeSize(Node<A, B> n) {
    return switch (n) {
      case Node.Collision(var keys, _) -> keys.length;
      case Node.Entries(_) -> throw new IllegalStateException();
    };
  }

  private static <A, B> Node<A, B> mkCollisionNode(A k1, B v1, A k2, B v2) {
    var keys = Utils.arrayOf(k1, k2);
    var vals = Utils.arrayOf(v1, v2);
    return collision(keys, vals);
  }

  // returns the table after inserting and a boolean indicating whether it was
  // a new addition (for global size tracking)
  private static <A, B> Pair<Node<A, B>, Boolean> insertAux(
      Node<A, B> n, int h, int depth, A k, B v) {
    return switch (n) {
      case Node.Collision(var keys, var vals) -> {
        var newNode = insertAtCollisionNode(collision(keys, vals), k, v);
        // was the key actually added to the table?
        var wasAdded = newNode instanceof Collision<?, ?> c && c.ks().length > keys.length;

        // keep collision while it's small -- or we're at maxDepth
        // (the trie's depth is bounded)
        if (depth >= maxDepth || collisionNodeSize(newNode) < maxCollisions) {
          yield Pair.of(newNode, wasAdded);
        }
        // okay, we have 4 collisions stacked, time to create a new 32slot bucket array
        // (so burst: redistribute into a new Entries node one level down)
        Node<A, B> entries = mkEmptyEntries();
        var grew = wasAdded;

        switch (newNode) {
          case Node.Collision(var keys1, var vals1) -> {
            for (int i = 0; i < keys1.length; i++) {
              var k0 = keys1[i];
              var v0 = vals1[i];
              // lean: let h := hash k |>.toUSize
              //       let h := div2Shift h (shift * (depth - 1)) */
              int h0 = k0.hashCode();
              int sh = div2Shift(h0, shift * (depth - 1));
              var pr = insertAux(entries, sh, depth, k0, v0);
              entries = pr.first();
              grew |= pr.second();
            }
            yield Pair.of(entries, grew);
          }
          default -> throw new IllegalStateException("should be collision node...");
        }
      }
      case Node.Entries(var entries) -> {
        var j = mod2Shift(h, shift);

        Pair<VHashMap.MapEntry<A, B, Node<A, B>>, Boolean> inner =
            switch (entries[j]) {
              case MapEntry.Null<?, ?, ?> _ -> Pair.of(entry(k, v), true);
              case MapEntry.Ref(var node) ->
                  insertAux(node, div2Shift(h, shift), depth + 1, k, v).mapFirst(n2 -> ref(n2));
              case MapEntry.Entry(var k1, var v1) -> {
                if (Objects.equals(k, k1)) {
                  yield Pair.of(entry(k, v), false); // overwrite
                } else {
                  yield Pair.of(ref(mkCollisionNode(k1, v1, k, v)), true);
                }
              }
            };
        var es2 = copyAndSet(entries, j, inner.first());
        yield Pair.of(entries(es2), inner.second());
      }
    };
  }

  /// O(1) average case - inserts a new `(k, v)` pair into this hashmap.
  ///
  /// Keys are expected to be non-null.
  public VHashMap<K, V> insert(K k, V v) {
    var updatedRoot = insertAux(root, k.hashCode(), 1, k, v);
    var isNodeAdded = updatedRoot.second();
    var updatedSize = isNodeAdded ? size + 1 : size;
    return new VHashMap<>(updatedRoot.first(), updatedSize);
  }

  /// O(n) - returns the union of this map and `other`, preferring values from `other` on key
  /// collisions.
  public VHashMap<K, V> union(VHashMap<K, V> other) {
    var result = this;
    for (var kv : other) {
      result = result.insert(kv.first(), kv.second());
    }
    return result;
  }

  private static <A, B> Maybe<B> findAtAux(A[] keys, B[] vals, int i, A k) {
    if (i < keys.length) {
      var k1 = keys[i];
      if (Objects.equals(k, k1)) {
        return Maybe.of(vals[i]);
      } else {
        return findAtAux(keys, vals, i + 1, k);
      }
    }
    return Maybe.none();
  }

  private static <A, B> Maybe<B> findAux(Node<A, B> n, int h, A k) {
    return switch (n) {
      case Node.Entries(var entries) -> {
        var j = mod2Shift(h, shift);
        yield switch (entries[j]) {
          case MapEntry.Null<?, ?, ?> _ -> Maybe.none();
          case MapEntry.Ref(var node) -> findAux(node, div2Shift(h, shift), k);
          case MapEntry.Entry(var k2, var v) when (Objects.equals(k, k2)) -> Maybe.of(v);
          case MapEntry.Entry(_, _) -> Maybe.none();
        };
      }
      case Node.Collision(var keys, var vals) -> findAtAux(keys, vals, 0, k);
    };
  }

  /// O(1) average case - returns the value associated with key `k`.
  public Maybe<V> lookup(K k) {
    return findAux(root, k.hashCode(), k);
  }

  /// O(1) average case - returns the value associated with key `k`;
  ///
  ///  Requires `k` to exist in this map's key set.
  ///
  /// @throws IllegalArgumentException if `k` is not present.
  public V lookupUnsafe(K k) {
    return switch (this.lookup(k)) {
      case Maybe.Some(var b) -> b;
      default -> throw new IllegalArgumentException();
    };
  }

  /// O(1) average case - returns the value associated with key `k` or `defaultVal` if the key `k`
  /// is not present.
  public V lookupOrElse(K k, V defaultVal) {
    return switch (findAux(root, k.hashCode(), k)) {
      case Maybe.Some(var x) -> x;
      case Maybe.None<?> _ -> defaultVal;
    };
  }

  private static <A, B> Maybe<Pair<A, B>> findMapEntryAtAux(A[] keys, B[] vals, int i, A k) {
    if (i < keys.length) {
      var k1 = keys[i];
      if (Objects.equals(k, k1)) {
        return Maybe.of(Pair.of(k1, vals[i]));
      } else {
        return findMapEntryAtAux(keys, vals, i + 1, k);
      }
    }
    return Maybe.none();
  }

  private static <A, B> Maybe<Pair<A, B>> findMapEntryAux(Node<A, B> n, int h, A k) {
    return switch (n) {
      case Node.Entries(var entries) -> {
        var j = mod2Shift(h, shift);
        yield switch (entries[j]) {
          case MapEntry.Null<?, ?, ?> _ -> Maybe.none();
          case MapEntry.Ref(var node) -> findMapEntryAux(node, div2Shift(h, shift), k);
          case MapEntry.Entry(var k1, var v) when (Objects.equals(k, k1)) ->
              Maybe.of(Pair.of(k1, v));
          case MapEntry.Entry(_, _) -> Maybe.none();
        };
      }
      case Node.Collision(var keys, var vals) -> findMapEntryAtAux(keys, vals, 0, k);
    };
  }

  /// O(1) average case - returns the key-value pair for `k` in this map; nothing otherwise.
  public Maybe<Pair<K, V>> lookupMapEntry(K k) {
    return findMapEntryAux(root, k.hashCode(), k);
  }

  private static <A, B> boolean containsAtAux(A[] keys, B[] vals, int i, A k) {
    if (i < keys.length) {
      var k1 = keys[i];
      if (Objects.equals(k, k1)) {
        return true;
      } else {
        return containsAtAux(keys, vals, i + 1, k);
      }
    }
    return false;
  }

  private static <A, B> boolean containsAux(Node<A, B> n, int h, A k) {
    return switch (n) {
      case Node.Entries(var entries) -> {
        var j = mod2Shift(h, shift);
        yield switch (entries[j]) {
          case MapEntry.Null<?, ?, ?> _ -> false;
          case MapEntry.Ref(var node) -> containsAux(node, div2Shift(h, shift), k);
          case MapEntry.Entry(var k1, _) -> Objects.equals(k, k1);
        };
      }
      case Node.Collision(var keys, var vals) -> containsAtAux(keys, vals, 0, k);
    };
  }

  /// O(1) average case - returns true only if `k` exists the keyset of this map.
  public boolean contains(K k) {
    return containsAux(root, k.hashCode(), k);
  }

  /// O(n) - returns a copy of this map without the binding for `k`.
  public VHashMap<K, V> remove(K k) {
    if (!contains(k)) {
      return this;
    }

    var result = VHashMap.<K, V>empty();
    for (var kv : this) {
      if (!Objects.equals(k, kv.first())) {
        result = result.insert(kv.first(), kv.second());
      }
    }
    return result;
  }

  private static <A, B, U> U foldLeftAux(Function3<U, A, B, U> f, U acc, Node<A, B> n) {
    return switch (n) {
      case Node.Collision(var keys, var vals) -> {
        for (int i = 0; i < keys.length; i++) {
          var k = keys[i];
          var v = vals[i];
          acc = f.apply(acc, k, v);
        }
        yield acc;
      }
      case Node.Entries(var entries) -> {
        var res = acc;
        for (var entry : entries) {
          res =
              switch (entry) {
                case MapEntry.Null<?, ?, ?> _ -> res;
                case MapEntry.Entry(var k, var v) -> f.apply(res, k, v);
                case MapEntry.Ref(var node) -> foldLeftAux(f, res, node);
              };
        }
        yield res;
      }
    };
  }

  /// O(1) - returns the number of key value pairs in this hashmap.
  public int size() {
    return this.size;
  }

  /// O(n) - left folds the entries of this map via the provided function `f`.
  public <U> U foldLeft(Function3<U, K, V, U> f, U neutral) {
    return foldLeftAux(f, neutral, this.root);
  }

  private static <A, B, U> Node<A, U> mapAux(Function<B, U> f, Node<A, B> n) {
    return switch (n) {
      case Node.Collision(var keys, var vals) -> {
        U[] valsMapped =
            Arrays.stream(vals)
                .map(f)
                .toArray(len -> Utils.newArrayLike(vals, len));
        yield collision(keys, valsMapped);
      }
      case Node.Entries(var vals) -> {
        MapEntry<A, U, Node<A, U>>[] valsMapped =
            Arrays.stream(vals)
                .map(
                    e ->
                        switch (e) {
                          case MapEntry.Null<?, ?, ?> _ -> null_();
                          case MapEntry.Entry(var k, var v) -> entry(k, f.apply(v));
                          case MapEntry.Ref(var child) -> ref(mapAux(f, child));
                        })
                .toArray(len -> Utils.newArrayLike(vals, len));
        yield entries(valsMapped);
      }
    };
  }

  /// O(n) - maps the function `f` over the values stored in this map.
  public <U> VHashMap<K, U> map(Function<V, U> f) {
    var mapped = mapAux(f, this.root);
    return new VHashMap<>(mapped, this.size);
  }

  /// O(n) - returns a hashmap containing only key-value pairs that satisfy the predicate `p`.
  public VHashMap<K, V> filterFor(BiPredicate<K, V> p) {
    var res = VHashMap.<K, V>empty();
    for (var kv : this) {
      res =
          switch (kv) {
            case Pair(var k, var v) when p.test(k, v) -> res.insert(k, v);
            default -> res;
          };
    }
    return res;
  }

  /// O(n) - returns the set of keys present in this map.
  public VHashSet<K> keySet() {
    var result = VHashSet.<K>empty();
    for (var kv : this) {
      result = result.insert(kv.first());
    }
    return result;
  }

  /// O(n) - returns the entries of this map as a list.
  public VList<Pair<K, V>> toList() {
    return foldLeftAux((acc, k, v) -> VList.cons(Pair.of(k, v), acc), VList.empty(), this.root);
  }

  @Override
  public Iterator<Pair<K, V>> iterator() {
    return new LazyIter<>(root);
  }

  public record Stats(int numNodes, int numNull, int numCollisions, int maxDepth) {}

  private static <A, B> Stats collectStats(Node<A, B> n, Stats stats, int depth) {
    return switch (n) {
      case Node.Collision(var keys, _) ->
          new Stats(
              stats.numNodes + 1,
              stats.numNull,
              stats.numCollisions + keys.length - 1,
              Math.max(stats.maxDepth, depth));
      case Node.Entries(var entries) -> {
        var ustats =
            new Stats(
                stats.numNodes + 1,
                stats.numNull,
                stats.numCollisions,
                Math.max(stats.maxDepth, depth));
        for (var entry : entries) {
          ustats =
              switch (entry) {
                case MapEntry.Null<?, ?, ?> _ ->
                    new Stats(
                        ustats.numNodes, ustats.numNull + 1, ustats.numCollisions, ustats.maxDepth);
                case MapEntry.Ref(var node) -> collectStats(node, ustats, depth + 1);
                case MapEntry.Entry(_, _) -> ustats;
              };
        }
        yield ustats;
      }
    };
  }

  /// O(n) - returns statistics including trie depth, number of collisions, etc.
  public Stats stats() {
    return collectStats(this.root, new Stats(0, 0, 0, 0), 1);
  }

  public String mkString(String delimiter) {
    return Utils.mkString(this, delimiter);
  }

  public String mkString(String left, String delimiter, String right) {
    return Utils.mkString(this, left, delimiter, right);
  }

  @Override
  public String toString() {
    return Utils.mkString(this, "VHashMap(", ", ", ")");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VHashMap<?, ?> other)) {
      return false;
    }
    if (this.size != other.size) {
      return false;
    }
    @SuppressWarnings("unchecked")
    VHashMap<K, V> otherAsMap = (VHashMap<K, V>) other;
    for (var kv : this) {
      var key = kv.first();
      var value = kv.second();

      var found = otherAsMap.lookup(key);
      if (found.isEmpty() || !Objects.equals(value, found.get())) {
        // either the key is missing, or the value doesn’t match
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    var h = 0;
    for (var e : this) {
      h += Objects.hashCode(e.first()) ^ Objects.hashCode(e.second());
    }
    return h;
  }

  private static final class LazyIter<K, V> implements Iterator<Pair<K, V>> {

    private static final class Frame<K, V> {

      final Node<K, V> node;
      int pos = 0;

      Frame(Node<K, V> n) {
        node = n;
      }
    }

    private final Deque<Frame<K, V>> stack = new ArrayDeque<>();
    private Pair<K, V> next;

    LazyIter(Node<K, V> root) {
      stack.push(new Frame<>(root));
      advance();
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public Pair<K, V> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      var result = next;
      advance();
      return result;
    }

    private void advance() {
      next = null;

      while (!stack.isEmpty()) {
        var frame = stack.peek(); // look at, but don't pop yet

        switch (frame.node) {
          case Node.Collision(var keys, var vals) -> {
            if (frame.pos < keys.length) {
              next = Pair.of(keys[frame.pos], vals[frame.pos]);
              frame.pos++;
              return;
            }
            stack.pop(); // collision exhausted
          }
          case Node.Entries(var es) -> {
            // finished scanning Entries? -> pop & loop again
            if (frame.pos >= es.length) {
              stack.pop();
              break;
            }

            var entry = es[frame.pos++];
            switch (entry) {
              case MapEntry.Null<?, ?, ?> _ -> {
                // noop
              }
              case MapEntry.Entry(var k, var v) -> {
                next = Pair.of(k, v);
                return;
              }
              case MapEntry.Ref(var node) -> {
                // Descend: push child frame, BUT keep parent on stack
                stack.push(new Frame<>(node));
              }
            }
          }
        }
      }
    }
  }
}
