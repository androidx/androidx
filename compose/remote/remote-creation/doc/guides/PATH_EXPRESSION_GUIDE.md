# RemoteCompose Path Expression Guide

Path Expressions allow you to algorithmically generate vector paths on the player side using mathematical equations. This is extremely efficient for dynamic shapes like graphs, waves, or complex ornaments that change over time.

## Core Operations

### 1. `addPathExpression` (Cartesian)
Generates a path by evaluating X and Y equations over a range.

```kotlin
val pathId = addPathExpression(
    expressionX = VAR1, // X = t
    expressionY = sin(VAR1 * 2f * PI.toFloat()) * 100f, // Y = sin(t) * 100
    min = 0f,
    max = 100f,
    count = 64, // Number of points to sample
    flags = Rc.PathExpression.SPLINE_PATH
)
```

- **`VAR1`**: The independent variable that ranges from `min` to `max`.
- **`count`**: The resolution of the path. Higher values mean smoother curves but more computation.
- **`flags`**: Controls interpolation (see below).

### 2. `addPolarPathExpression` (Polar)
Generates a path using radius and angle, which is often easier for circular or symmetrical shapes.

```kotlin
val pathId = addPolarPathExpression(
    expressionRad = 100f + 20f * sin(VAR1 * 10f), // Radius varies by angle
    startAngle = 0f,
    endAngle = 2f * PI.toFloat(),
    count = 100,
    centerX = 250f,
    centerY = 250f,
    flags = Rc.PathExpression.LOOP_PATH or Rc.PathExpression.SPLINE_PATH
)
```

- **`VAR1`**: Represents the current angle (in radians) between `startAngle` and `endAngle`.

## Interpolation Modes

The `flags` parameter controls how the sampled points are connected:

- **`SPLINE_PATH` (0)**: Uses a standard cubic spline. Very smooth but can overshoot.
- **`MONOTONIC_PATH` (2)**: Uses a monotonic cubic spline. Smoother than linear but guaranteed not to overshoot local extrema. Good for data graphs.
- **`LINEAR_PATH` (4)**: Connects points with straight lines.

### Other Flags
- **`LOOP_PATH` (1)**: Automatically connects the last point back to the first point to close the shape.

## Using the Generated Path

Once a path is created and you have its `pathId`, you can use it with several drawing operations:

### 1. Drawing the Path
```kotlin
drawPath(pathId)
```

### 2. Text on Path
```kotlin
drawTextOnPath(textId, pathId, hOffset, vOffset)
```

### 3. Path Morphing (`drawTweenPath`)
You can smoothly animate between two different path expressions if they have the same `count`.

```kotlin
drawTweenPath(pathId1, pathId2, progress, start, stop)
```

## Practical Example: Animated Wave

```kotlin
val time = ContinuousSec()
val pathId = addPathExpression(
    expressionX = VAR1,
    expressionY = cy + 50f * sin(VAR1 * 0.1f + time),
    min = 0f,
    max = width,
    count = 40,
    flags = Rc.PathExpression.SPLINE_PATH
)

painter.setColor(Color.Blue).setStyle(Style.STROKE).setStrokeWidth(5f).commit()
drawPath(pathId)
```

## Performance Considerations
- **Resolution**: Don't use a `count` higher than necessary for the visual size of the path.
- **Complexity**: Keep the expressions relatively simple. They are re-evaluated on the player whenever a dependency (like `time`) changes.
- **Pre-calculation**: If a path doesn't change, use a static `RemotePath` instead of an expression.
