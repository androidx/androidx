---
name: jetpack-compose-performance-internal
description: >-
  Diagnose and optimize Jetpack Compose performance in AndroidX and AOSP framework
  code across layout coordinates, synthetic composition side-effects, lazy
  property initialization, unboxed primitives and value classes, and
  allocation-free collections and fast iteration.
---

# Jetpack Compose performance optimization skill

## Optimization audit workflow

1.  **Exhaustive Scan**: Scan the target code against all 5 core categories in
    the following sections. Evaluate layout coordinate transformations,
    side-effect primitives, property initialization, primitive/value class
    boxing, and collection and loop allocations against all checklist rules
    rather than stopping after the first issue found.
2.  **Mandatory Reference Read Before Flagging or Editing**: Whenever a file
    matches a rule in Categories 1–5, you **MUST** open and read the
    corresponding subskill file for the approved fix pattern and full scope
    guards **before** reporting a finding or editing code. Never rely on the
    summary bullets alone to write fixes.

## 1. Fast-path coordinate and transformation caches ([fast-path-coordinate-caches/SKILL.md][1])

**MANDATORY**: Before flagging or fixing any issue in this category, you
**MUST** read [fast-path-coordinate-caches/SKILL.md][1].

-   **Prefer Root-Cached Coordinates (`positionInRoot` / `positionInWindow`)**:
    Prefer `node.positionInRoot()` or `positionInWindow()` over relative
    `node.localPositionOf(source, offset)` queries when calculating global
    screen or root offsets to bypass hierarchy traversal and relative matrix
    chain calculations.
-   **Batch Bounds Queries via `RectList`**: When querying bounds for multiple
    nodes or regions, utilize `RectList` structures to batch rectangular
    coordinate calculations without intermediate bounding box object
    allocations.
-   **Cache Intermediate Offsets**: Cache intermediate offsets when performing
    multi-node layout coordinate calculations in custom layout or measurement
    modifiers.

## 2. Defer or prune synthetic composition overhead ([prune-synthetic-composition-overhead/SKILL.md][2])

**MANDATORY**: Before flagging or fixing any issue in this category, you
**MUST** read [prune-synthetic-composition-overhead/SKILL.md][2].

-   **Guard Optional Callbacks in `LaunchedEffect`**: Check if optional
    callbacks are non-null (`if (callback != null)`) before invoking
    `LaunchedEffect` to avoid spawning internal coroutine state tracking, effect
    nodes, and composition structures when inactive.
-   **`SideEffect` for Synchronous Updates**: Use `SideEffect` for synchronous
    non-suspending updates on recomposition (such as updating external listeners
    or non-Compose state) rather than launching coroutines.
-   **Asynchronous Suspend in `LaunchedEffect`**: Reserve `LaunchedEffect` for
    asynchronous suspend functions tied to composition lifecycle, passing
    meaningful non-null keys.
-   **Event Handlers via `coroutineScope.launch`**: Launch coroutines inside
    user event callbacks (such as `onClick`) via `coroutineScope.launch` so they
    execute asynchronously outside frame composition passes without bloating
    composition overhead.
-   **Prune Unused `CompositionLocalProvider`**: Only instantiate
    `CompositionLocalProvider` or custom provider scopes when child nodes or
    downstream consumers explicitly require them.

## 3. Zero-overhead hand-rolled lazy initialization ([zero-overhead-lazy-init/SKILL.md][3])

**MANDATORY**: Before flagging or fixing any issue in this category, you
**MUST** read [zero-overhead-lazy-init/SKILL.md][3].

-   **Hand-Rolled Nullable Backing Fields**: Replace
    `val property by lazy { ... }` with a private nullable backing field
    (`private var _property: Type? = null`) and custom getter (`get()`) in
    performance-critical internal helper classes, scratch buffer allocations,
    and frame measurement components to eliminate `Lazy` delegate heap
    allocations and locking overhead.
-   **Strict Single-Thread Confinement Scope Guard**: Only apply hand-rolled
    lazy initialization when property access is strictly guaranteed to be
    single-threaded (such as UI/main thread or thread-confined Compose runtime
    internals).
-   **Preserve Thread Safety in Concurrent Contexts**: Never apply hand-rolled
    lazy initialization to properties accessed concurrently across multiple
    threads; retain standard `by lazy` (or atomic concurrency primitives /
    `AtomicReference`) to prevent race conditions and double-initialization.

## 4. Eliminate autoboxing with primitive states and value class sentinels ([unboxed-primitives-and-value-classes/SKILL.md][4])

**MANDATORY**: Before flagging or fixing any issue in this category, you
**MUST** read [unboxed-primitives-and-value-classes/SKILL.md][4].

-   **Primitive Snapshot States**: Replace `mutableStateOf(0)`,
    `mutableStateOf(0f)`, `mutableStateOf(0L)`, and `mutableStateOf(0.0)` with
    `mutableIntStateOf`, `mutableFloatStateOf`, `mutableLongStateOf`, and
    `mutableDoubleStateOf`.
-   **Non-Null Value Class Sentinels (`.Unspecified`)**: Replace nullable
    `@JvmInline value class` types (`DpSize?`, `Offset?`, `Size?`, `Dp?`,
    `Color?`, `IntSize?`) in state, properties, and parameters with non-nullable
    types using `.Unspecified` (or private
    `IntSize(Int.MIN_VALUE, Int.MIN_VALUE)` / `-1` sentinels) to prevent heap
    boxing.
-   **No Interface Dispatch on Value Classes**: Do not store or pass
    `@JvmInline value class` instances through an implemented interface in hot
    paths; use packed `Long` primitives (`packFloats` / `unpackFloat1` /
    `unpackFloat2`).
-   **Unboxed `IntOffset`/`Offset` & Bitmask Value Classes**: Return `IntOffset`
    or `Offset` instead of `android.graphics.Point` or `Pair`, and pack
    multi-boolean state into a bitmask `@JvmInline value class` backed by `Int`.

## 5. Unboxed collections and allocation-free fast iteration ([unboxed-collections-and-fast-iteration/SKILL.md][5])

**MANDATORY**: Before flagging or fixing any issue in this category, you
**MUST** read [unboxed-collections-and-fast-iteration/SKILL.md][5].

-   **`androidx.collection` Flat Containers**: Prefer `MutableScatterSet`
    (`mutableScatterSetOf()`), `MutableScatterMap`, `MutableObjectList`
    (`mutableObjectListOf()`), `IntObjectMap` (`mutableIntObjectMapOf()`), and
    `MutableIntList` over `HashSet`, `HashMap<Int, V>`, `ArrayList`, or
    reassigned `var list: List<T> = emptyList()`. Reuse destination
    `MutableScatterSet` buffers when diffing sets per frame.
-   **`fast*` Indexed Iteration Over `Iterator` Loops**: Use `fastForEach`,
    `fastForEachReversed`, `fastMap`, `fastAny`, `fastFirstOrNull`, and
    `fastSumBy` from `androidx.compose.ui.util` on `RandomAccess` lists instead
    of `.forEach`, `.map`, `for (x in list)`, or `indices.reversed()`. Iterate
    `SnapshotStateList` directly with `fast*` without `.toList()` copies.
-   **Two-Argument `coerceIn(min, max)` & Lazy Filtering**: Use
    `value.coerceIn(min, max)` instead of `value.coerceIn(min..max)` to avoid
    `ClosedRange`/`IntRange` allocations, and lazily allocate filtered lists
    only when the first excluded element is encountered.

[1]: fast-path-coordinate-caches/SKILL.md
[2]: prune-synthetic-composition-overhead/SKILL.md
[3]: zero-overhead-lazy-init/SKILL.md
[4]: unboxed-primitives-and-value-classes/SKILL.md
[5]: unboxed-collections-and-fast-iteration/SKILL.md
