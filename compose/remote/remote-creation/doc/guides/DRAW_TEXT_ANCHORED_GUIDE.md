# RemoteCompose DrawTextAnchored Guide

The `drawTextAnchored` operation provides a flexible way to position and align text relative to a specific coordinate (the "anchor"). Unlike standard `drawText` which often uses the baseline and left edge, `drawTextAnchored` uses "panning" factors to justify the text within its own bounding box relative to the anchor point.

## Core Principles

- **Anchor-Based Positioning**: You specify a fixed (x, y) point on the canvas.
- **Bounding Box Panning**: Instead of simple alignment (LEFT, CENTER, RIGHT), you use `panX` and `panY` floats to position the text's bounding box relative to the anchor.
- **Dynamic Measurement**: The player calculates the text's bounding box at runtime and applies the panning, ensuring correct alignment even if the text content or font size changes dynamically.

## Usage in Kotlin (DSL)

```kotlin
canvas {
    val cx = ComponentWidth() / 2f
    val cy = ComponentHeight() / 2f

    // Center text perfectly at (cx, cy)
    drawTextAnchored(
        str = "Hello World",
        x = cx,
        y = cy,
        panX = 0f, // 0 = centered horizontally
        panY = 0f, // 0 = centered vertically
        flags = 0
    )
}
```

## Parameter Details

### `panX` (Horizontal Panning)
- **`-1.0f`**: Right edge of text is at the anchor (text is to the left).
- **`0.0f`**: Text is horizontally centered on the anchor.
- **`1.0f`**: Left edge of text is at the anchor (text is to the right).

### `panY` (Vertical Panning)
- **`-1.0f`**: Bottom edge of text is at the anchor (text is above).
- **`0.0f`**: Text is vertically centered on the anchor.
- **`1.0f`**: Top edge of text is at the anchor (text is below).
- **`NaN`**: Special value; the anchor `y` coordinate is used as the text **baseline**.

### `flags`
- **`ANCHOR_TEXT_RTL` (1)**: Renders text with Right-to-Left direction.
- **`ANCHOR_MONOSPACE_MEASURE` (2)**: Forces measurement using monospace character widths.
- **`MEASURE_EVERY_TIME` (4)**: Forces a fresh measurement on every frame (useful if the font or style is animating).
- **`BASELINE_RELATIVE` (8)**: Adjusts vertical centering logic to be relative to the baseline rather than the geometric center.

## Internal Architecture

### 1. Creation Phase (`RemoteComposeWriter.java`)
The writer serializes the `DRAW_TEXT_ANCHOR` opcode along with the text ID, coordinates, panning factors, and flags. If a raw `String` is passed, it is first added to the document's text data pool.

### 2. Player Phase (`DrawTextAnchored.java`)
On the player:
1.  **Measurement**: The player retrieves the string and gets its geometric bounds using the current `Paint` state.
2.  **Offset Calculation**:
    - `horizontalOffset`: Calculated based on the text width and `panX`.
    - `verticalOffset`: Calculated based on the text height and `panY` (or baseline if `panY` is NaN).
3.  **Rendering**: The final coordinates are `(x + horizontalOffset, y + verticalOffset)`, and the text is drawn using `drawTextRun`.

## Practical Examples

### Corner Alignment
```kotlin
// Position text at the top-right corner of the component
drawTextAnchored("Top Right", width, 0f, panX = -1f, panY = 1f, flags = 0)
```

### Labeling a Data Point
In a graph, you might want a label to appear just above a point:
```kotlin
drawTextAnchored(
    str = valueText,
    x = pointX,
    y = pointY - 5f, // 5px gap
    panX = 0f,  // Centered over the point
    panY = -1f, // Text is above the point
    flags = 0
)
```

### Baseline Alignment
```kotlin
// Align multiple text elements to the same baseline regardless of their height
drawTextAnchored("Big Text", 100f, 200f, 0f, Float.NaN, 0)
drawTextAnchored("small", 300f, 200f, 0f, Float.NaN, 0)
```

## Comparison with Standard DrawText
While `drawText` is simpler, `drawTextAnchored` is the preferred way to handle dynamic text in RemoteCompose because it abstracts away the need for the developer to manually calculate text width and height on the server side (which might not match the player's font rendering exactly).
