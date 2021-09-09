# AndroidX Collection Benchmarks for Kotlin/JS

This directory builds a set of AndroidX Collection benchmarks for Kotlin/JS.

This project depends on node.js. Run this one-time setup task first:

    ./gradlew :collection:collection-benchmark-js:setupJsBenchmark

To run the benchmarks:

    ./gradlew :collection:collection-benchmark-js:runJsBenchmark

The output will look something like this:

```
circularArray_addFromHeadAndPopFromTail x 2,256 ops/sec ±1.96% (88 runs sampled)
circularArray_addFromTailAndPopFromHead x 2,355 ops/sec ±1.04% (93 runs sampled)
lruCache_allHits x 65.37 ops/sec ±0.75% (68 runs sampled)
lruCache_allMisses x 402 ops/sec ±0.79% (88 runs sampled)
lruCache_customCreate x 52,805 ops/sec ±0.74% (89 runs sampled)
benchmark suite completed
```
