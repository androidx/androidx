---
name: unboxed-collections-and-fast-iteration
description: >-
  Guides using allocation-free androidx.collection data structures (ScatterSet,
  ScatterMap, MutableObjectList, IntObjectMap, MutableIntList) and
  androidx.compose.ui.util fast* iteration extensions (fastForEach, fastMap,
  fastAny, fastFirstOrNull, fastSumBy) instead of standard JDK collections,
  Iterator loops, or ClosedRange coerceIn(a..b) in Compose hot paths. Use when
  storing, diffing, or iterating collections in Compose state, layout, or draw
  passes. Don't use when an external public API strictly requires java.util.Map
  or Set interfaces.
---

# Use Unboxed Collections and Allocation-Free Fast Iteration

Replace standard JDK collections (`HashSet`, `HashMap`, `ArrayList`), iterator
loops (`.forEach`, `for (x in list)`, `indices.reversed()`), and
range-allocating helpers (`coerceIn(min..max)`) with `androidx.collection`
flat-array containers and `androidx.compose.ui.util.fast*` indexed loops in
Composition, Layout, and Draw paths.

## Why It Matters

1.  **Hash Node & Boxing Overhead in JDK Collections**: `HashSet`,
    `LinkedHashSet`, and `HashMap<Int, V>` allocate entry wrapper nodes per
    element and box primitive keys (`Int`, `Long`). `androidx.collection`
    structures (`ScatterSet`, `ScatterMap`, `MutableIntObjectMap`,
    `MutableLongSet`, `MutableIntList`, `MutableObjectList`) use open-addressing
    flat arrays with zero entry nodes and unboxed primitive storage.
2.  **Iterator Allocations in Hot Loops**: Calling standard `.forEach`, `.map`,
    `.any`, `.firstOrNull`, or `for (item in list)` on a `List` allocates a
    `ListIterator` on the heap. Calling `list.indices.reversed()` allocates an
    `IntProgression` and iterator. In layout measurement, placement, pointer
    input, and draw passes that run at 60–120Hz, indexed `fastForEach`,
    `fastForEachReversed`, `fastMap`, `fastAny`, and `fastFirstOrNull` eliminate
    iterator allocations completely.
3.  **Redundant `.toList()` Copies on `SnapshotStateList`**: Calling
    `stateList.toList().fastForEach { ... }` inside `derivedStateOf` or layout
    blocks allocates an unnecessary shallow copy on every read;
    `SnapshotStateList` implements `RandomAccess` and can be traversed directly
    with `fast*` extensions.
4.  **Range Object Allocation in `coerceIn(min..max)`**: Calling
    `value.coerceIn(min..max)` on custom `Comparable` or `Dp` types constructs a
    `ClosedRange` object on every invocation, whereas the two-argument overload
    `value.coerceIn(min, max)` performs direct comparisons with zero allocation.

## Guidelines

-   **Prefer `androidx.collection` Containers**:
    -   Replace `HashSet<T>` / `mutableSetOf<T>()` with `MutableScatterSet<T>`
        (`mutableScatterSetOf()`).
    -   Replace `HashMap<K, V>` / `mutableMapOf<K, V>()` with
        `MutableScatterMap<K, V>` (`mutableScatterMapOf()`).
    -   Replace `Map<Int, V>` / `Map<Long, V>` with `IntObjectMap<V>` /
        `LongObjectMap<V>` (`mutableIntObjectMapOf()`,
        `mutableLongObjectMapOf()`).
    -   Replace `List<Int>` / `List<Float>` with `IntList` / `FloatList`
        (`mutableIntListOf()`, `mutableFloatListOf()`).
    -   Replace `var items: List<T> = emptyList()` reassigned via `items = items
        +   item`with a single`val items = mutableObjectListOf<T>()`.
-   **Reuse Destination Buffers When Diffing Sets**: In frame or focus/node
    diffing paths, pass a pre-allocated `MutableScatterSet<T>` destination
    buffer (`clear()` + populate) rather than allocating a fresh set on every
    diff call.
-   **Use `fast*` Extensions for `RandomAccess` Lists**: Use `fastForEach`,
    `fastForEachIndexed`, `fastForEachReversed`, `fastMap`, `fastAny`,
    `fastFirstOrNull`, `fastSumBy`, and `fastMaxOfOrDefault` from
    `androidx.compose.ui.util`.
-   **Avoid `indices.reversed()`**: Replace `for (i in list.indices.reversed())`
    with `for (i in list.lastIndex downTo 0)` or `list.fastForEachReversed`.
-   **Do Not Call `.toList()` Before `fast*` on `SnapshotStateList`**: Call
    `stateList.fastForEach` or `stateList.fastMap` directly.
-   **Use Two-Argument `coerceIn(min, max)`**: Always call
    `x.coerceIn(min, max)` instead of `x.coerceIn(min..max)`.
-   **Lazily Allocate Filtered Lists**: When filtering a list where most calls
    exclude zero items, return the original list reference until the first
    excluded element is encountered, then allocate a `MutableObjectList` or
    `ArrayList` seeded with the prefix in a single pass.

## Examples

### 1. `androidx.collection` Over Boxed Maps, Sets, and Reassigned Lists

#### Anti-Pattern (Before)

```kotlin
class SubtreeNodeTracker {
    // Allocates HashMap Entry nodes and boxes Int keys into java.lang.Integer
    private val nodesById = mutableMapOf<Int, LayoutNode>()
    // Allocates a new List copy on every addition
    private var activeListeners: List<Listener> = emptyList()

    fun findCommonAncestor(a: LayoutNode, b: LayoutNode): LayoutNode? {
        // Allocates LinkedHashSet and entry wrappers on every call
        val ancestors = mutableSetOf<LayoutNode>()
        var current: LayoutNode? = a
        while (current != null) {
            ancestors.add(current)
            current = current.parent
        }
        return b.firstAncestorIn(ancestors)
    }

    fun addListener(listener: Listener) {
        activeListeners = activeListeners + listener
    }
}
```

#### Optimization (After)

```kotlin
class SubtreeNodeTracker {
    // Flat open-addressing array with unboxed Int keys
    private val nodesById = mutableIntObjectMapOf<LayoutNode>()
    // Single reusable flat object array
    private val activeListeners = mutableObjectListOf<Listener>()

    fun findCommonAncestor(a: LayoutNode, b: LayoutNode): LayoutNode? {
        val ancestors = mutableScatterSetOf<LayoutNode>()
        var current: LayoutNode? = a
        while (current != null) {
            ancestors.add(current)
            current = current.parent
        }
        return b.firstAncestorIn(ancestors)
    }

    fun addListener(listener: Listener) {
        activeListeners.add(listener)
    }
}
```

### 2. `fast*` Iteration, Reverse Loops, and Two-Argument `coerceIn`

#### Anti-Pattern (Before)

```kotlin
fun MeasureScope.measureRow(
    measurables: List<Measurable>,
    constraints: Constraints,
    scrollOffset: Int,
    maxScroll: Int,
): MeasureResult {
    // Allocates Iterator and intermediate List via standard .map and .sumOf
    val placeables = measurables.map { it.measure(constraints) }
    val totalWidth = placeables.sumOf { it.width }
    // Allocates IntRange object
    val clampedScroll = scrollOffset.coerceIn(0..maxScroll)

    return layout(totalWidth, constraints.maxHeight) {
        // Allocates IntProgression iterator
        for (i in placeables.indices.reversed()) {
            placeables[i].placeRelative(-clampedScroll, 0)
        }
    }
}
```

#### Optimization (After)

```kotlin
fun MeasureScope.measureRow(
    measurables: List<Measurable>,
    constraints: Constraints,
    scrollOffset: Int,
    maxScroll: Int,
): MeasureResult {
    // Zero-iterator indexed loop
    val placeables = measurables.fastMap { it.measure(constraints) }
    val totalWidth = placeables.fastSumBy { it.width }
    // Zero-allocation two-argument coerceIn
    val clampedScroll = scrollOffset.coerceIn(0, maxScroll)

    return layout(totalWidth, constraints.maxHeight) {
        for (i in placeables.lastIndex downTo 0) {
            placeables[i].placeRelative(-clampedScroll, 0)
        }
    }
}
```
