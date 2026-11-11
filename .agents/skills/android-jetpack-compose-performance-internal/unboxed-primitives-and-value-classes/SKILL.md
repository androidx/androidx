---
name: unboxed-primitives-and-value-classes
description: >-
  Guides eliminating autoboxing in Jetpack Compose via primitive snapshot states
  (mutableIntStateOf, mutableFloatStateOf, mutableLongStateOf), non-null
  @JvmInline value class sentinels (.Unspecified), avoiding interface dispatch
  on value classes, and bitmask value classes. Use when declaring Compose
  mutable state for primitives, working with Offset, Size, DpSize, IntSize,
  IntOffset, or Color, or designing state holders and cache keys. Don't use for
  non-primitive reference objects that have no unboxed representation.
---

# Eliminate Autoboxing with Primitive States and Value Class Sentinels

Prevent heap allocations and garbage collection pressure in Compose state,
layout, and drawing paths by using specialized primitive snapshot states,
non-nullable `@JvmInline value class` sentinels, and packed `Long`/`Int`
representations.

## Why It Matters

1.  **Generic `mutableStateOf<T>` Autoboxing**: Calling `mutableStateOf(0)`,
    `mutableStateOf(0f)`, or `mutableStateOf(0L)` uses the generic
    `SnapshotMutableStateImpl<T>` class, which boxes every primitive read and
    write into `java.lang.Integer`, `Float`, or `Long` on the heap. Specialized
    factories (`mutableIntStateOf`, `mutableFloatStateOf`, `mutableLongStateOf`,
    `mutableDoubleStateOf`) store and access raw primitives directly.
2.  **Nullable Value Classes Force Heap Boxing**: `@JvmInline value class` types
    (`Offset`, `Size`, `Dp`, `DpSize`, `IntSize`, `IntOffset`, `Color`) compile
    down to raw `Long`, `Int`, or `Float` primitives **only when non-nullable**.
    Declaring a property, parameter, or state as nullable (`DpSize?`,
    `IntSize?`, `Offset?`, `Color?`) forces the Kotlin compiler to allocate a
    wrapper object on the heap whenever a non-null value is assigned or read.
3.  **Value Classes Implementing Interfaces**: When a `@JvmInline value class`
    implements an interface and is stored in a property or passed as a parameter
    typed as that interface, the compiler boxes the value class on every
    assignment and interface method call.
4.  **Heap-Allocated Coordinate Pairs & Flag Objects**: Returning
    `android.graphics.Point` or `Pair<Int, Int>` from position helpers, or
    allocating data classes for boolean configuration flags, creates avoidable
    heap churn compared to packed `IntOffset`, `Offset`, or bitmask
    `@JvmInline value class` types.

## Guidelines

-   **Always Use Primitive Snapshot State Factories**: Replace
    `mutableStateOf(0)` with `mutableIntStateOf(0)`, `mutableStateOf(0f)` with
    `mutableFloatStateOf(0f)`, `mutableStateOf(0L)` with
    `mutableLongStateOf(0L)`, and `mutableStateOf(0.0)` with
    `mutableDoubleStateOf(0.0)`.
-   **Use `.Unspecified` Sentinels Instead of Nullable Value Classes**: Replace
    `DpSize? = null`, `Offset? = null`, `Size? = null`, `Dp? = null`, and
    `Color? = null` with their non-nullable types initialized to
    `DpSize.Unspecified`, `Offset.Unspecified`, `Size.Unspecified`,
    `Dp.Unspecified`, or `Color.Unspecified`. Check presence with `.isSpecified`
    / `.isUnspecified`.
-   **Define Unboxed Sentinels for `IntSize` / `IntOffset` / Primitives**: Where
    a value class lacks a public `.Unspecified` constant (such as `IntSize`),
    use a private sentinel like
    `private val UnspecifiedIntSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)` or
    `-1` / `Float.NaN` instead of `IntSize?` or `Int?` / `Float?`.
-   **Never Store or Pass Value Classes Through an Implemented Interface**: In
    hot layout or gesture math, represent 1D/2D vectors or packed coordinates as
    primitive `Long` values (using `packFloats(x, y)` / `unpackFloat1(v)` /
    `unpackFloat2(v)`) rather than polymorphic `@JvmInline value class` wrappers
    implementing a shared interface.
-   **Return `IntOffset` or `Offset` Instead of `Point` or `Pair`**: Replace
    `android.graphics.Point`, `PointF`, or `Pair<Int, Int>` return types in
    bounds and coordinate calculators with `IntOffset` or `Offset`.
-   **Pack Multi-Boolean Configurations into a Bitmask Value Class**: When
    passing or storing multiple boolean flags across modifiers or state holders,
    pack them into a `@JvmInline value class Flags(val mask: Int)` using bit
    shifts.
-   **Provide Primitive `*OrDefault` Accessors**: Alongside nullable `Long?` or
    `Int?` map/bundle getters, provide primitive
    `getLongOrDefault(key, defaultValue: Long): Long` overloads to avoid boxing
    on lookup.

## Examples

### 1. Primitive Snapshot States and `.Unspecified` Value Class Sentinels

#### Anti-Pattern (Before)

```kotlin
class TooltipLayoutState {
    // Boxes Float into java.lang.Float on every frame read/write
    var alpha by mutableStateOf(1f)
    // Boxes Int into java.lang.Integer on every read/write
    var selectedIndex by mutableStateOf(-1)
    // Nullable value class properties force heap wrapper allocation on every assignment
    var containerSize: DpSize? = null
    var anchorOffset: Offset? = null
    var lastMeasuredSize: IntSize? = null
}
```

#### Optimization (After)

```kotlin
private val UnspecifiedIntSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)

class TooltipLayoutState {
    // Unboxed primitive snapshot states
    var alpha by mutableFloatStateOf(1f)
    var selectedIndex by mutableIntStateOf(-1)
    // Non-nullable value class properties compile to primitive Long fields
    // (Note: if snapshot state is required, MutableState<T> is generic and still boxes value
    // classes unless backed by mutableLongStateOf with pack/unpack helpers)
    var containerSize: DpSize = DpSize.Unspecified
    var anchorOffset: Offset = Offset.Unspecified
    var lastMeasuredSize: IntSize = UnspecifiedIntSize
}
```

### 2. Avoiding Value Class Interface Boxing and `Point` Allocations

#### Anti-Pattern (Before)

```kotlin
// Polymorphic interface causes value class boxing whenever referenced as Vector
sealed interface Vector
@JvmInline value class Vector2D(val packed: Long) : Vector

fun calculateCenter(bounds: IntRect): android.graphics.Point {
    // Allocates a heap Point object on every call
    return android.graphics.Point(bounds.center.x, bounds.center.y)
}
```

#### Optimization (After)

```kotlin
// Packed Long helper functions operate on raw 64-bit primitives without boxing
fun Vector2D(x: Float, y: Float): Long = packFloats(x, y)

// Returns unboxed 64-bit primitive Long via @JvmInline value class IntOffset
fun calculateCenter(bounds: IntRect): IntOffset = bounds.center
```

### 3. Bitmask `@JvmInline value class` for Multi-Boolean State

#### Anti-Pattern (Before)

```kotlin
// Allocates a heap object and stores 4 separate fields
data class GestureAxisFlags(
    val top: Boolean = false,
    val bottom: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
)
```

#### Optimization (After)

```kotlin
// Zero heap allocation: represented as a single primitive Int at runtime
@JvmInline
value class GestureAxisFlags private constructor(private val mask: Int) {
    constructor(
        top: Boolean = false,
        bottom: Boolean = false,
        left: Boolean = false,
        right: Boolean = false,
    ) : this(
        (if (top) 1 else 0) or
            (if (bottom) 2 else 0) or
            (if (left) 4 else 0) or
            (if (right) 8 else 0)
    )

    val top: Boolean get() = (mask and 1) != 0
    val bottom: Boolean get() = (mask and 2) != 0
    val left: Boolean get() = (mask and 4) != 0
    val right: Boolean get() = (mask and 8) != 0
}
```
