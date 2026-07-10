# RemoteCompose Loop Guide

The `loop` mechanism in RemoteCompose allows you to repeat a sequence of operations on the player side. This is highly efficient for drawing repetitive elements (like clock ticks, grid lines, or multiple particles) without sending each individual command over the wire.

## Core Principles

- **Player-Side Execution**: The loop logic runs on the player. You define the loop once, and the player iterates through the operations.
- **Dynamic Index**: Every loop has an index variable that updates with each iteration. This variable can be used in expressions to vary the output (e.g., rotation, position, color).
- **RPN Compatibility**: The `from`, `step`, and `until` parameters can be dynamic expressions (Remote IDs).

## Usage in Kotlin (DSL)

The `RemoteComposeContextAndroid` provides a convenient `loop` extension.

```kotlin
loop(from = 0f, step = 1f, until = 12f) { index ->
    // 'index' is an RFloat representing the current iteration value
    save {
        rotate(index * 30f, centerX, centerY)
        drawLine(centerX, centerY - 100f, centerX, centerY - 120f)
    }
}
```

- **`from`**: The starting value of the index.
- **`step`**: The increment added to the index after each iteration.
- **`until`**: The loop continues as long as `index < until`.
- **`index`**: The closure provides the index as an `RFloat`, which you can use in math expressions.

## Usage in Java (Procedural)

In Java, you use the `RemoteComposeWriter` directly.

```java
float indexVar = rc.startLoopVar(0f, 1f, 12f);
// Drawing operations using indexVar
rc.save();
rc.rotate(rc.floatExpression(indexVar, 30f, MUL), cx, cy);
rc.drawLine(cx, cy - 100, cx, cy - 120);
rc.restore();
rc.endLoop();
```

Alternatively, using a functional interface:
```java
rc.loop(indexId, 0f, 1f, 12f, () -> {
    // operations
});
```

## Internal Architecture

### 1. Creation Phase (`RemoteComposeWriter.java`)
When you call `startLoop`, the writer emits a `LOOP_START` opcode into the `WireBuffer` along with:
- `indexId`: The ID of the variable that will store the current index.
- `from`, `step`, `until`: The loop range parameters.

Operations following `LOOP_START` are buffered as children of the loop until an `endLoop` (which closes the container) is encountered.

### 2. Player Phase (`LoopOperation.java`)
On the player side, the `LoopOperation` acts as a container for its child operations.
During the `paint` pass:
1.  It resolves the current values for `from`, `step`, and `until`.
2.  It enters a standard `for` loop.
3.  In each iteration:
    - It updates the index variable in the `RemoteContext` using `loadFloat(mIndexVariableId, i)`.
    - It iterates through all child operations and calls `apply()` on them.
    - If a child operation depends on the index variable, it is marked dirty and its variables are re-evaluated for that specific iteration.

## Practical Examples

### Drawing a Clock Face
```kotlin
// Draw 60 minute ticks
loop(0f, 1f, 60f) { i ->
    val angle = i * 6f // 360 / 60
    save {
        rotate(angle, cx, cy)
        val length = ifElse(i % 5f, 10f, 20f) // Longer ticks every 5 mins
        drawLine(cx, cy - radius, cx, cy - radius + length)
    }
}
```

### Dynamic Grid
```kotlin
val spacing = 50f
loop(0f, spacing, width) { x ->
    drawLine(x, 0f, x, height)
}
loop(0f, spacing, height) { y ->
    drawLine(0f, y, width, y)
}
```

## Performance Considerations
- **Iteration Count**: Be mindful of the number of iterations. Thousands of iterations with complex drawing operations can impact frame rates on the player.
- **Variable Dependencies**: Operations inside a loop that depend on the loop index must be re-evaluated every iteration. Keep expressions inside loops as simple as possible.
- **Nested Loops**: Loops can be nested, but the complexity grows exponentially. Use with caution.
