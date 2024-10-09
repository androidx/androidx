# Compose Hero Benchmarks

This module contains high-level benchmarks for Compose outlining a broader performance picture. In
comparison to component-level benchmarks, these benchmarks provide a high-level view of Compose
performance.

## Structure
A hero benchmark consists of the following modules:

```
hero
    - example
        - example-implementation # contains the target code
        - example-macrobenchmark # contains macrobenchmarks
        - example-macrobenchmark-target # wrapper for example-implementation to run macrobenchmarks against
        - example-microbenchmark # optional, if microbenchmarks are useful for the given hero project
```

