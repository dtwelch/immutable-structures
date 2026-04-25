# immutable-structures

Persistent immutable data structures for the JVM.

## Collections

- `VList`: singly linked persistent list
- `VHashMap`: persistent hash map
- `VHashSet`: persistent hash set (ported from lean4)

## Runtime Highlights

- `VList.cons`: `O(1)`
- `VList.head`: `O(1)`
- `VList.tail`: `O(1)`
- `VList.append`: `O(n)`
- `VList.reverse`: `O(n)`
- `VHashMap.lookup`: typical `O(1)`
- `VHashMap.insert`: typical `O(1)`
- `VHashSet.contains`: typical `O(1)`
- `VHashSet.insert`: typical `O(1)`

## Other Types

`Maybe`, `Result`, `Validation`, `Pair`, `Graph`, and the small function interfaces support the
core collections.

## License

BSD 3-Clause. See [LICENSE](LICENSE).
