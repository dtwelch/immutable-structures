package io.github.dt;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

/// An immutable hashset.
///
/// Offers standard operations for querying, adding, filtering, etc. As usual, obtain an instance
/// via the factory methods (i.e.: [VHashSet#of(Object)] or [VHashSet#empty()]).
public final class VHashSet<T> implements Iterable<T> {

  private static final VHashSet<?> Empty = new VHashSet<>(VHashMap.empty());

  private final VHashMap<T, Unit> hashMap;

  private VHashSet(VHashMap<T, Unit> m) {
    this.hashMap = m;
  }

  @SuppressWarnings("unchecked")
  public static <A> VHashSet<A> empty() {
    return (VHashSet<A>) Empty;
  }

  public static <A> VHashSet<A> of(A x) {
    return VHashSet.<A>empty().insert(x);
  }

  public static <A> VHashSet<A> of(A e1, A e2) {
    return VHashSet.<A>empty().insert(e1).insert(e2);
  }

  public static <A> VHashSet<A> of(A e1, A e2, A e3) {
    return VHashSet.<A>empty().insert(e1).insert(e2).insert(e3);
  }

  public static <A> VHashSet<A> of(A e1, A e2, A e3, A e4) {
    return VHashSet.<A>empty().insert(e1).insert(e2).insert(e3).insert(e4);
  }

  public static <A> VHashSet<A> of(A e1, A e2, A e3, A e4, A e5) {
    return VHashSet.<A>empty().insert(e1).insert(e2).insert(e3).insert(e4).insert(e5);
  }

  @SafeVarargs
  public static <A> VHashSet<A> of(A... es) {
    var result = VHashSet.<A>empty();
    for (var e : es) {
      result = result.insert(e);
    }
    return result;
  }

  public static <A> VHashSet<A> from(Iterable<A> xs) {
    var result = VHashSet.<A>empty();
    for (var e : xs) {
      result = result.insert(e);
    }
    return result;
  }

  /// O(1) - adds `item` to this hashset.
  public VHashSet<T> insert(T item) {
    var updatedMap = hashMap.insert(item, Unit.Instance);
    return new VHashSet<>(updatedMap);
  }

  /// O(n) - returns this set without `item`.
  public VHashSet<T> remove(T item) {
    if (!contains(item)) {
      return this;
    }
    return new VHashSet<>(hashMap.remove(item));
  }

  /// O(n) - returns true only if there exists some item in this set that satisfies predicate `p`;
  // false otherwise.
  public boolean exists(Predicate<T> p) {
    for (var kv : this.hashMap) {
      switch (kv) {
        case Pair(T k, _) when p.test(k) -> {
          return true;
        }
        default -> {}
      }
    }
    return false;
  }

  /// O(n) - returns true only all elements of this set satisfy predicate `p`; false otherwise
  public boolean forall(Predicate<T> p) {
    return !exists(t -> !p.test(t));
  }

  /// O(n) - combines the contents of this set with `other`.
  public VHashSet<T> union(VHashSet<T> other) {
    var result = this;
    for (var x : other) {
      result = result.insert(x);
    }
    return result;
  }

  /// O(n) - returns this set with all elements from `other` excluded.
  public VHashSet<T> difference(VHashSet<T> other) {
    var result = VHashSet.<T>empty();
    for (var x : this) {
      if (!other.contains(x)) {
        result = result.insert(x);
      }
    }
    return result;
  }

  /// O(1) - returns true only if this hashset contains `item`; false otherwise.
  public boolean contains(T item) {
    return switch (hashMap.lookup(item)) {
      case Maybe.Some(_) -> true;
      default -> false;
    };
  }

  /// O(n) - returns the first element of this hashset that satisfies predicate `p` wrapped in a
  /// [Maybe.Some]; if nothing satisfies `p`, then returns [Maybe.None].
  public Maybe<T> find(Predicate<T> p) {
    var res = Maybe.<T>none();
    for (var x : this) {
      if (p.test(x)) {
        res = Maybe.of(x);
        break;
      }
    }
    return res;
  }

  /// O(n) - flatmaps the function `f` over this set.
  public <R> VHashSet<R> flatMap(Function<T, VHashSet<R>> f) {
    var result = VHashSet.<R>empty();
    for (var x : this) {
      result = result.union(f.apply(x));
    }
    return result;
  }

  /// O(n) -- returns true only if all the entries of this are in `other`; false otherwise.
  public boolean subsetOf(VHashSet<T> other) {
    for (var entry : this) {
      if (!other.contains(entry)) {
        return false;
      }
    }
    return true;
  }

  public VList<T> toList() {
    return hashMap.foldLeft((acc, k, v) -> VList.cons(k, acc), VList.empty());
  }

  /// O(1) - returns the number of entries in this set.
  public int size() {
    return hashMap.size();
  }

  @Override
  public Iterator<T> iterator() {
    var mapIter = hashMap.iterator();
    return new SetIter<>(mapIter);
  }

  @Override
  public boolean equals(Object o) {
    return switch (o) {
      case VHashSet<?> other -> this.hashMap.equals(other.hashMap);
      default -> false;
    };
  }

  @Override
  public int hashCode() {
    var h = 0;
    for (var e : this) {
      h += (e == null ? 0 : e.hashCode());
    }
    return h;
  }

  public String mkString(String delimiter) {
    return Utils.mkString(this, "", delimiter, "");
  }

  public String mkString(String left, String delimiter, String right) {
    return Utils.mkString(this, left, delimiter, right);
  }

  @Override
  public String toString() {
    return mkString("Set(", ", ", ")");
  }

  private record SetIter<A>(Iterator<Pair<A, Unit>> mapIter) implements Iterator<A> {

    @Override
    public A next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      var kv = mapIter.next();
      return kv.first();
    }

    @Override
    public boolean hasNext() {
      return mapIter.hasNext();
    }
  }
}
