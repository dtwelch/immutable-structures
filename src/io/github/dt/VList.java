package io.github.dt;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/// An immutable linked-list type.
///
/// Most core list operations are present. Call [#of(Object...)] to construct a nonempty list and
/// [#empty()] to obtain a ref. to the empty list.
public sealed interface VList<A> extends Iterable<A> {
  final class Empty<A> implements VList<A> {

    private static final VList<?> Instance = new Empty<>();

    private Empty() {}

    @Override
    public String toString() {
      return "";
    }
  }

  record Cons<A>(A head, VList<A> rest) implements VList<A> {

    @Override
    public String toString() {
      return mkString("[", ", ", "]");
    }

    @Override
    public boolean equals(Object o) {
      return switch (o) {
        case VList<?> other -> {
          var it1 = this.iterator();
          var it2 = other.iterator();
          while (it1.hasNext() && it2.hasNext()) {
            var a = it1.next();
            var b = it2.next();
            if (!Objects.equals(a, b)) {
              yield false;
            }
          }
          yield !it1.hasNext() && !it2.hasNext();
        }
        default -> false;
      };
    }

    @Override
    public int hashCode() {
      var h = 1;
      for (var e : this) {
        h = 31 * h + (e == null ? 0 : e.hashCode());
      }
      return h;
    }
  }

  @SuppressWarnings("unchecked")
  static <T> VList<T> empty() {
    return (VList<T>) Empty.Instance;
  }

  /// O(1) - prepends an `item` onto the front of `this`.
  static <T> VList<T> cons(T item, VList<T> rest) {
    return new Cons<>(item, rest);
  }

  /// O(1) - returns true if this list is empty, false otherwise.
  default boolean isEmpty() {
    return this instanceof Empty<A>;
  }

  /// O(1) - returns the head of this list.
  ///
  /// @throws IllegalArgumentException if called on an empty list.
  default A head() {
    return switch (this) {
      case Cons(var head, _) -> head;
      default -> throw new IllegalArgumentException("called head on empty list");
    };
  }

  /// O(n) - returns the last element in this list.
  ///
  /// @throws IllegalArgumentException if called on an empty list
  default A last() {
    var maybeLast = Maybe.<A>none();
    for (var x : this) {
      maybeLast = Maybe.of(x);
    }
    return switch (maybeLast) {
      case Maybe.Some(var x) -> x;
      default -> throw new IllegalArgumentException("called last on empty list");
    };
  }

  /// O(n) - drops the last `n` elements from this list.
  default VList<A> dropRight(int n) {

    var result = new ArrayList<A>();
    var length = this.length();
    if (n <= 0) {
      return this;
    }
    if (n >= length) {
      return VList.empty();
    }

    var count = 0;
    for (var x : this) {
      if (count == length - n) {
        break;
      }
      result.add(x);
      count++;
    }
    return VList.from(result);
  }

  /// O(min(n, length)) - returns the first `n` elements of this list.
  default VList<A> take(int n) {
    if (n <= 0 || isEmpty()) {
      return VList.empty();
    }

    var result = new ArrayList<A>();
    var count = 0;
    for (var x : this) {
      if (count == n) {
        break;
      }
      result.add(x);
      count++;
    }
    return VList.from(result);
  }

  /// O(1) - returns this list without the head item.
  default VList<A> tail() {
    return switch (this) {
      case Cons(_, var xs) -> xs;
      default -> throw new IllegalArgumentException("called tail on empty list");
    };
  }

  /// O(n) - returns the number of items in {@code this} list.
  default int length() {
    var size = 0;
    for (var x : this) {
      size++;
    }
    return size;
  }

  static <T> VList<T> of(T x1) {
    return VList.cons(x1, VList.empty());
  }

  static <T> VList<T> of(T x1, T x2) {
    return VList.cons(x1, VList.cons(x2, VList.empty()));
  }

  static <T> VList<T> of(T x1, T x2, T x3) {
    return VList.cons(x1, VList.cons(x2, VList.cons(x3, VList.empty())));
  }

  static <T> VList<T> of(T x1, T x2, T x3, T x4) {
    return VList.cons(x1, VList.cons(x2, VList.cons(x3, VList.cons(x4, VList.empty()))));
  }

  static <T> VList<T> of(T x1, T x2, T x3, T x4, T x5) {
    return VList.cons(
        x1, VList.cons(x2, VList.cons(x3, VList.cons(x4, VList.cons(x5, VList.empty())))));
  }

  static <T> VList<T> of(T x1, T x2, T x3, T x4, T x5, T x6) {
    return VList.cons(
        x1,
        VList.cons(
            x2, VList.cons(x3, VList.cons(x4, VList.cons(x5, VList.cons(x6, VList.empty()))))));
  }

  @SafeVarargs
  static <T> VList<T> of(T... ts) {
    var res = VList.<T>empty();
    for (int i = ts.length - 1; i >= 0; i--) {
      res = new VList.Cons<>(ts[i], res);
    }
    return res;
  }

  static <T> VList<T> from(Iterator<T> items) {
    var buffer = new ArrayList<T>();
    items.forEachRemaining(buffer::add);

    var lst = VList.<T>empty();
    for (int i = buffer.size() - 1; i >= 0; i--) {
      lst = new Cons<>(buffer.get(i), lst);
    }
    return lst;
  }

  @SafeVarargs
  static <T> VList<T> from(T... items) {
    var result = VList.<T>empty();
    for (int i = items.length - 1; i >= 0; i--) {
      result = VList.cons(items[i], result);
    }
    return result;
  }

  static <T> VList<T> from(Iterable<T> items) {
    var buffer = new ArrayList<T>();
    for (var item : items) {
      buffer.add(item);
    }

    var lst = VList.<T>empty();
    for (int i = buffer.size() - 1; i >= 0; i--) {
      lst = new Cons<>(buffer.get(i), lst);
    }
    return lst;
  }

  /// O(n) - returns a list containing all elements of `prefix` followed by
  /// all elements of `this` (i.e. `prefix ++ this`); preserves the relative
  /// order of both lists and is stack safe.
  default VList<A> prependAll(VList<A> prefix) {

    var buf = new ArrayList<A>();
    for (var x : prefix) {
      buf.add(x);
    }
    var result = this;
    for (int i = buf.size() - 1; i >= 0; i--) {
      result = VList.cons(buf.get(i), result);
    }
    return result;
  }

  /// O(n) - returns a list containing all elements of `this` followed by all
  /// elements of `ys` (i.e. `this ++ ys`); preserves the relative order of
  /// both lists (avoids recursion)
  default VList<A> append(VList<A> ys) {
    var xs = this;
    if (xs.isEmpty()) {
      return ys;
    }
    var buffer = new ArrayList<A>();
    while (!xs.isEmpty()) {
      buffer.add(xs.head());
      xs = xs.tail();
    }
    var result = ys;
    for (int i = buffer.size() - 1; i >= 0; i--) {
      result = new Cons<>(buffer.get(i), result);
    }
    return result;
  }

  default VList<A> filter(Predicate<A> pred) {
    return foldRight(
        empty(),
        (x, acc) -> {
          if (pred.test(x)) {
            return cons(x, acc);
          } else {
            return acc;
          }
        });
  }

  /// O(n) - returns true only if this list contains {@code item}; false otherwise.
  default boolean contains(A item) {
    for (var x : this) {
      if (Objects.equals(x, item)) {
        return true;
      }
    }
    return false;
  }

  /// O(1) - returns the head wrapped in a {@link Maybe.Some} if it exists;
  /// {@link Maybe.None} otherwise.
  default Maybe<A> headMaybe() {
    return switch (this) {
      case Cons(var x, _) -> Maybe.of(x);
      case Empty<A> _ -> Maybe.none();
    };
  }

  /// O(n) - returns a new VList with elements from {@code this} list in
  /// reverse order.
  default VList<A> reverse() {
    var reversed = VList.<A>empty();
    var current = this;
    while (!current.isEmpty()) {
      var head = current.head();
      current = current.tail();
      reversed = new Cons<>(head, reversed);
    }
    return reversed;
  }

  /// O(n) - (left) folds the list into a single value of type {@code B};
  /// is stack safe.
  default <B> B fold(B initial, BiFunction<B, A, B> f) {
    return foldLeft(initial, f);
  }

  /// O(n) - folds the list into a single value of type {@code B} from left
  /// to right according to function {@code f} (a -> b -> c) -- which carries
  /// an accumulator (stack safe)
  default <B> B foldLeft(B initial, BiFunction<B, A, B> f) {
    B acc = initial;
    var curr = this;
    while (!curr.isEmpty()) {
      acc = f.apply(acc, curr.head());
      curr = curr.tail();
    }
    return acc;
  }

  /// O(n) - returns true only if there exists some item in this list
  /// that satisfies the provided predicate {@code p}.
  default boolean exists(Predicate<A> p) {
    for (A element : this) {
      if (p.test(element)) {
        return true;
      }
    }
    return false;
  }

  /// O(n) - returns the first item in this list that satisfies `p`, if any.
  default Maybe<A> find(Predicate<A> p) {
    for (var item : this) {
      if (p.test(item)) {
        return Maybe.of(item);
      }
    }
    return Maybe.none();
  }

  /// O(n) - returns the index of the first entry that satisfies `p`.
  default Maybe<Integer> findFirstIndex(Predicate<A> p) {
    var i = 0;
    for (var item : this) {
      if (p.test(item)) {
        return Maybe.of(i);
      }
      i++;
    }
    return Maybe.none();
  }

  /// O(n) - applies `f` from left to right and returns the first nonempty result, if any.
  default <U> Maybe<U> findMap(Function<A, Maybe<U>> f) {
    for (var x : this) {
      var r = f.apply(x);
      if (r.nonEmpty()) {
        return r;
      }
    }
    return Maybe.none();
  }

  default <B> B foldRight(B initial, BiFunction<A, B, B> f) {
    var stack = new ArrayDeque<A>();
    var current = this;
    while (!current.isEmpty()) {
      stack.push(current.head());
      current = current.tail();
    }
    B acc = initial;
    while (!stack.isEmpty()) {
      acc = f.apply(stack.pop(), acc);
    }
    return acc;
  }

  default List<A> asMutableList() {
    var result = new ArrayList<A>();
    for (var x : this) {
      result.add(x);
    }
    return result;
    // more functional version of above :
    //        return foldLeft(
    //                new ArrayList<>(),
    //                (acc, x) -> {
    //                    acc.add(x);
    //                    return acc;
    //                });
  }

  default <B> VList<B> map(Function<A, B> f) {
    return foldRight(VList.empty(), (x, acc) -> cons(f.apply(x), acc));
  }

  default <B> VList<B> flatMap(Function<A, VList<B>> f) {
    return foldRight(VList.<B>empty(), (x, acc) -> f.apply(x).append(acc));
  }

  /// O(n) - flattens `as` into a list
  static <A> VList<A> flatten(VList<VList<A>> as) {
    var buffer = new ArrayList<A>();
    for (var lst : as) {
      for (var x : lst) {
        buffer.add(x);
      }
    }
    return VList.from(buffer);
  }

  static <K, V> VHashMap<K, V> toMap(VList<Pair<K, V>> pairs) {
    var result = VHashMap.<K, V>empty();
    for (var p : pairs) {
      result = result.insert(p.first(), p.second());
    }
    return result;
  }

  /// O(n) — forms a pair of lists using the conversion function `f`
  /// that maps an element `A` to a `Pair<L,R>`; order of resulting list of pairs follows
  /// ordering of this list.
  default <L, R> Pair<VList<L>, VList<R>> unzip(Function<A, Pair<L, R>> f) {
    var leftList = VList.<L>empty();
    var rightList = VList.<R>empty();
    for (A x : this) {
      var p = f.apply(x);
      leftList = VList.cons(p.first(), leftList);
      rightList = VList.cons(p.second(), rightList);
    }
    return Pair.of(leftList.reverse(), rightList.reverse());
  }

  static <A> Pair<VList<A>, VList<A>> splitHalf(VList<A> xs) {
    int len = xs.length();
    if (len <= 1) {
      return Pair.of(xs, VList.empty());
    }
    int splitPoint = len / 2;

    var left = VList.<A>empty();
    var cur = xs;

    for (int i = 0; i < splitPoint; i++) {
      left = VList.cons(cur.head(), left);
      cur = cur.tail();
    }
    left = left.reverse();
    var right = cur;

    return Pair.of(left, right);
  }

  /// O(n log n) - returns this list sorted via the passed {@code order}
  /// comparator.
  default VList<A> sortBy(Comparator<A> order) {
    return sort(this, order);
  }

  default <B> VList<A> sortBy(Function<A, B> key, Comparator<B> keyOrder) {
    return VList.sort(this, Comparator.comparing(key, keyOrder));
  }

  /// O(n log n) - sorts the provided list {@code xs} of orderable items
  /// {@code T}.
  static <T extends Comparable<T>> VList<T> sort(VList<T> xs) {
    return sort(xs, T::compareTo);
  }

  /// O(n log n) - sorts the provided list {@code xs} of via the passed
  /// {@code order} comparator. This is stable (merge) sort
  /// (use of `<=` in first while in {@link #merge(VList, VList, Comparator)}).
  ///
  /// Argument for stability (courtesy of gemini ... so maybe take with grain of salt :-)
  ///  "Because every recursive call to `merge` now resolves ties in favour of
  ///   the left-hand element due to use of `<=` in first while loop, no equal-key pair is
  ///   ever reversed at any level, so the whole algorithm is stable."
  ///
  /// So ours is "stable", but not "in place" since we can't sort within the same list
  /// without creating a new one (since this is an immutable list)
  static <T> VList<T> sort(VList<T> xs, Comparator<T> order) {
    if (xs.isEmpty() || xs.tail().isEmpty()) {
      return xs;
    }
    var lr = splitHalf(xs);
    var left = lr.first();
    var right = lr.second();

    var leftSorted = sort(left, order);
    var rightSorted = sort(right, order);
    return merge(leftSorted, rightSorted, order);
  }

  private static <T> VList<T> merge(VList<T> a, VList<T> b, Comparator<T> order) {
    var result = new ArrayList<T>();
    while (!a.isEmpty() && !b.isEmpty()) {
      var x = a.head();
      var y = b.head();
      if (order.compare(x, y) <= 0) {
        result.add(x);
        a = a.tail();
      } else {
        // x > y
        result.add(y);
        b = b.tail();
      }
    }
    // take any leftovers:
    while (!a.isEmpty()) {
      result.add(a.head());
      a = a.tail();
    }
    while (!b.isEmpty()) {
      result.add(b.head());
      b = b.tail();
    }
    return VList.from(result);
  }

  // ported from scala: Iterable#groupBy(..)
  default <K> VHashMap<K, VList<A>> groupBy(Function<A, K> f) {
    var m = new HashMap<K, VList<A>>();
    for (var elem : this) {
      var key = f.apply(elem);
      m.put(key, VList.cons(elem, m.getOrDefault(key, VList.empty())));
    }

    // reverse each list so the order matches the original traversal
    var result = VHashMap.<K, VList<A>>empty();
    for (var e : m.entrySet()) {
      result = result.insert(e.getKey(), e.getValue().reverse());
    }
    return result;
  }

  @Override
  default Iterator<A> iterator() {
    return new VListIter<>(this);
  }

  class VListIter<A> implements Iterator<A> {

    private VList<A> cur;

    public VListIter(VList<A> start) {
      this.cur = start;
    }

    @Override
    public boolean hasNext() {
      return !cur.isEmpty();
    }

    @Override
    public A next() {
      if (cur.isEmpty()) {
        throw new NoSuchElementException();
      }
      A elem = cur.head();
      cur = cur.tail();
      return elem;
    }
  }

  default String mkString(String sep) {
    return Utils.mkString(this, sep);
  }

  default String mkString(String left, String delimiter, String right) {
    return Utils.mkString(this, left, delimiter, right);
  }
}
