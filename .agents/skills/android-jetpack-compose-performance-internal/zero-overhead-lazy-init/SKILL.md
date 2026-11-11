---
name: zero-overhead-lazy-init
description: >-
  Guides using hand-rolled nullable backing fields for lazy property
  initialization in AndroidX internal components and hot paths instead of
  Kotlin standard by lazy delegate allocations. Emphasizes single-threaded
  confinement caveats and thread-safety requirements.
---

# Zero-Overhead Hand-Rolled Lazy Initialization

Avoid Kotlin's standard `by lazy` delegate for class properties in
high-frequency internal components, system diagnostics, or Compose internals,
provided the component is strictly single-threaded.

## Why It Matters

Standard `by lazy` delegates allocate a separate `Lazy` delegate instance on the
heap and incur concurrency synchronization locking overhead on property lookups
(via `LazyThreadSafetyMode.SYNCHRONIZED`).

In low-latency custom-render routines, frame measurement paths, or system
diagnostics, replacing `by lazy` with a nullable backing field and a custom
getter eliminates both the heap delegate allocation and locking overhead.

## Critical Caveat: Thread Safety & Multi-Threading

> [!CAUTION] Hand-rolled lazy initialization using a nullable backing field
> without synchronization is **ONLY safe when property access is guaranteed to
> be single-threaded** (e.g., main/UI thread or thread-confined Compose runtime
> internals).

-   If a property is accessed across multiple threads concurrently, hand-rolled
    null-checks without locks introduce race conditions and
    double-initialization.

-   For multi-threaded scenarios, keep standard `by lazy` (or explicit
    concurrency primitives / `AtomicReference`) to ensure thread safety.

## Guidelines

-   Verify single-thread access constraints before refactoring `by lazy`.

-   Replace `val property by lazy { ... }` with a private nullable backing field
    `private var _property: Type? = null`.

-   Provide a custom getter `get()` that checks for `null`, initializes the
    field, and returns the cached instance.

## Anti-Pattern (Before)

```kotlin
class LayoutInfo(var laneCount: Int)

class ScratchBufferUser(private val layoutInfo: LayoutInfo) {
    // Performance Hit: Delegate object allocation and concurrency locking overhead
    private val reusableScratchBuffer by lazy {
        IntArray(layoutInfo.laneCount)
    }

    fun getBuffer(): IntArray = reusableScratchBuffer
}
```

## Optimization (After)

```kotlin
class LayoutInfo(var laneCount: Int)

class ScratchBufferUser(private val layoutInfo: LayoutInfo) {
    private var _reusableScratchBuffer: IntArray? = null

    // Optimized: Nullable backing field avoids delegate object allocation (Single-Threaded context)
    private val reusableScratchBuffer: IntArray
        get() {
            var buffer = _reusableScratchBuffer
            if (buffer == null) {
                buffer = IntArray(layoutInfo.laneCount)
                _reusableScratchBuffer = buffer
            }
            return buffer
        }

    fun getBuffer(): IntArray = reusableScratchBuffer
}
```
