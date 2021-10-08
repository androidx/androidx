# AndroidX Collection Benchmarks for Native Platforms

This directory builds a set of benchmarks for AndroidX Collection on native
platforms. Currently only macOS (x86_64) is supported.

To build, run in the collection project root:

    ./gradlew :collection:collection-benchmark-native:assemble

Then execute the resulting binary at:

    ../out/collection-playground/collection-playground/collection/collection-benchmark-native/build/install/main/debug/collection-benchmark-native

The output will look something like this:

```
Running ../out/collection-playground/collection-playground/collection/collection-benchmark-native/build/install/main/debug/lib/collection-benchmark-native
Run on (12 X 2900 MHz CPU s)
CPU Caches:
  L1 Data 32 KiB (x6)
  L1 Instruction 32 KiB (x6)
  L2 Unified 256 KiB (x6)
  L3 Unified 12288 KiB (x1)
Load Average: 2.21, 2.43, 2.23
--------------------------------------------------------------------------------------------------------
Benchmark                                                              Time             CPU   Iterations
--------------------------------------------------------------------------------------------------------
BM_CircularArray_addFromHeadAndPopFromTail_ObjCCallingKMP        4599997 ns      4590038 ns          158
BM_SimpleArrayMap_addAllThenRemoveIndividually_ObjCCallingKMP 1635764920 ns   1628431000 ns            1
```

This project assumes you have
[Google Benchmark](https://github.com/google/benchmark) installed. It also
assumes you have installed Xcode.
