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

## Running Hero Benchmarks

Hero Benchmarks are micro- and macrobenchmarks just like other AndroidX
benchmarks. From an AndroidX checkout, they can be run through different ways:

### Through Android Studio's UI
Open a hero benchmark, e.g. PokedexScrollBenchmark, and run using the run button.

### From the command line

`$ ./gradlew :compose:integration-tests:hero:pokedex:pokedex-macrobenchmark:connectedCheck`

You can specify a filter like:

`$ ./gradlew :compose:integration-tests:hero:pokedex:pokedex-macrobenchmark:connectedCheck \ -P android.testInstrumentationRunnerArguments.class=androidx.compose.integration.hero.pokedex.macrobenchmark.PokedexScrollBenchmark`

### CI (Googlers)
Googlers can use go/androidx-bench-experiments to run benchmarks from CI.

