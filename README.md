# immutable-structures

Persistent immutable data structures for recent JDKs (25+).

The current surface area is intentionally small: a list, a hash map, a hash set,
and a few support types built around them.

## currently supported collections:

- `VList`: singly linked persistent list
- `VHashMap`: persistent hash map based on a hash array mapped trie
- `VHashSet`: persistent hash set built on top of `VHashMap`

## runtimes at a glance:

- `VList.cons`: `O(1)`
- `VList.head`: `O(1)`
- `VList.tail`: `O(1)`
- `VList.take`: `O(min(n, length))`
- `VList.append`: `O(n)`
- `VList.reverse`: `O(n)`
- `VHashMap.lookup`: typical `O(1)`
- `VHashMap.insert`: typical `O(1)`
- `VHashMap.remove`: `O(n)` in the current implementation
- `VHashSet.contains`: typical `O(1)`
- `VHashSet.insert`: typical `O(1)`
- `VHashSet.remove`: `O(n)` in the current implementation

## auxiliary (potentially useful) types:

`Maybe`, `Result`, `Validation`, `Pair`, `Unit`, and the small function interfaces
support the core collections.

## Notes

- The published Maven coordinates are currently `io.github.dtwelch:immutable-structures`.
- `VHashMap` expects non-null keys (see doc/ for the original lean4 implementation).
- most of the auxiliary (collection) types allow pattern matching as does `VList`
- the `Validation` type is modeled after the scala3 version used in the frontend checking pipeline in the 
[flix](https://github.com/flix/flix) compiler (which  I believe in turn was inspired by the haskell 
library version)
## License

BSD 3-Clause. See [LICENSE](LICENSE).
