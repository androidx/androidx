---
name: fast-path-coordinate-caches
description: >-
  Guides utilizing fast-path coordinate transformation APIs like positionInRoot
  instead of relative localPositionOf calls to minimize matrix chain calculations
  in Compose layout hierarchies. Mentions RectList APIs for bounding batching.
---

# Utilize Fast-Path Coordinate and Transformation Caches

Leverage built-in parent or root cached coordinate transformations (such as
`positionInRoot()`) rather than computing local hierarchy differences via
relative transformation APIs like `localPositionOf()`.

## Why It Matters

Traversing layout hierarchies to calculate relative positions using
`localPositionOf(source, offset)` requires climbing parent layout nodes and
performing matrix multiplications per frame, leading to CPU stalls during
scrolling or animation.

Root-cached coordinate transformation APIs (`positionInRoot()`,
`positionInWindow()`) reuse pre-calculated transformation matrices, bypassing
relative matrix chains and parent tree traversal. For batching rectangular
region bounds across nodes, weave in `RectList` APIs to avoid individual
bounding box allocations.

## Guidelines

-   Prefer `node.positionInRoot()` over relative
    `node.localPositionOf(source, offset)` queries when calculating global
    screen or root offsets.

-   When querying bounds for multiple nodes or regions, utilize `RectList`
    structures to batch rectangular coordinate calculations without intermediate
    object allocations.

-   Cache intermediate offsets when performing multi-node layout coordinate
    calculations in custom layout or measurement modifiers.

## Anti-Pattern (Before)

```kotlin
data class Offset(val x: Float, val y: Float) {
    companion object {
        val Zero = Offset(0f, 0f)
    }
}

data class IntSize(val width: Int, val height: Int)

interface LayoutCoordinates {
    val size: IntSize
    fun findRootCoordinates(): LayoutCoordinates
    fun localPositionOf(sourceCoordinates: LayoutCoordinates, relativeToSource: Offset): Offset
    fun positionInWindow(): Offset
    fun positionInRoot(): Offset
}

class VisualDebugComponent {
    var rootOffset: Offset = Offset.Zero
    var rootSize: IntSize = IntSize(0, 0)

    fun updateRoot(coordinates: LayoutCoordinates) {
        // Performance issue: localPositionOf requires traversing hierarchy and computing transform matrices each time
        rootOffset = coordinates.findRootCoordinates().localPositionOf(coordinates, Offset.Zero)
        rootSize = coordinates.findRootCoordinates().size
    }
}

```

## Optimization (After)

```kotlin
data class Offset(val x: Float, val y: Float) {
    companion object {
        val Zero = Offset(0f, 0f)
    }
}

data class IntSize(val width: Int, val height: Int)

interface LayoutCoordinates {
    val size: IntSize
    fun findRootCoordinates(): LayoutCoordinates
    fun localPositionOf(sourceCoordinates: LayoutCoordinates, relativeToSource: Offset): Offset
    fun positionInWindow(): Offset
    fun positionInRoot(): Offset
}

class VisualDebugComponent {
    var rootOffset: Offset = Offset.Zero
    var rootSize: IntSize = IntSize(0, 0)

    fun updateRoot(coordinates: LayoutCoordinates) {
        // Optimized: positionInRoot leverages internally cached layout hierarchy position calculations
        rootOffset = coordinates.positionInRoot()
        rootSize = coordinates.findRootCoordinates().size
    }
}

```
