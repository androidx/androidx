# Package androidx.graphics.paths

Androidx Graphics Path is an Android library that provides new functionalities around the
[Path](https://developer.android.com/reference/android/graphics/Path) API. Specifically, it
allows paths to be queried for the segment data they contain,

The library is compatible with API 21+.

## Iterating over a Path

With Pathway you can easily iterate over a `Path` object to inspect its segments
(curves or commands):

```kotlin
val path = Path().apply {
    // Build path content
}

for (segment in path) {
    val type = segment.type // The type of segment (move, cubic, quadratic, line, close, etc.)
    val points = segment.points // The points describing the segment geometry
}
```

This type of iteration is easy to use but may create an allocation per segment iterated over.
If you must avoid allocations, Pathway provides a lower-level API to do so:

```kotlin
val path = Path().apply {
    // Build path content
}

val iterator = path.iterator
val points = FloatArray(8)

while (iterator.hasNext()) {
    val type = iterator.next(points) // The type of segment
    // Read the segment geometry from the points array depending on the type
}

```

### Path segments

Each segment in a `Path` can be of one of the following types:

#### Move

Move command. The path segment contains 1 point indicating the move destination.
The weight is set 0.0f and not meaningful.

#### Line

Line curve. The path segment contains 2 points indicating the two extremities of
the line. The weight is set 0.0f and not meaningful.

#### Quadratic

Quadratic curve. The path segment contains 3 points in the following order:
- Start point
- Control point
- End point

The weight is set 0.0f and not meaningful.

#### Conic

Conic curve. The path segment contains 3 points in the following order:
- Start point
- Control point
- End point

The curve is weighted by the `PathSegment.weight` property.

Conic curves are automatically converted to quadratic curves by default, see
[Handling conic segments](#handling-conic-segments) below for more information.

#### Cubic

Cubic curve. The path segment contains 4 points in the following order:
- Start point
- First control point
- Second control point
- End point

The weight is set to 0.0f and is not meaningful.

#### Close

Close command. Close the current contour by joining the last point added to the
path with the first point of the current contour. The segment does not contain
any point. The weight is set 0.0f and not meaningful.

#### Done

Done command. This optional command indicates that no further segment will be
found in the path. It typically indicates the end of an iteration over a path
and can be ignored.

## Handling conic segments

In some API levels, paths may contain conic curves (weighted quadratics) but the
`Path` API does not offer a way to add conics to a `Path` object. To work around
this, Pathway automatically converts conics into several quadratics by default.

The conic to quadratic conversion is an approximation controlled by a tolerance
threshold, set by default to 0.25f (sub-pixel). If you want to preserve conics
or control the tolerance, you can use the following APIs:

```kotlin
// Preserve conics
val iterator = path.iterator(PathIterator.ConicEvaluation.AsConic)

// Control the tolerance of the conic to quadratic conversion
val iterator = path.iterator(PathIterator.ConicEvaluation.AsQuadratics, 2.0f)

```
