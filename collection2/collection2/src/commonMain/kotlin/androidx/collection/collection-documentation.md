# Module root

Collections

# Package androidx.collection

A set of collection libraries suited for small data sets which are also optimized for Android,
usually by sacrificing speed for memory efficiency.

* [ArraySet] / [ArrayMap]: Implementations of [Set] and [Map], respectively, which
  are backed by an array with lookups done by a binary search.
* [SparseArray] / [LongSparseArray]: Map-like structures whose keys are [Int] and [Long],
  respectively, which prevents boxing compared to a traditional [Map].
* [LruCache]: A map-like cache which keeps frequently-used entries and automatically evicts others.
* [CircularArray] / [CircularIntArray]: List-like structures which can efficiently prepend and
  append elements.