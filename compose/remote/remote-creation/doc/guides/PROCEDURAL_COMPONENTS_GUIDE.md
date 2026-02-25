# Guide to Building RemoteCompose Procedural Component-Based Examples

This guide covers how to build examples using the **procedural components and modifiers** — the layout-based approach where you compose `box`, `column`, `row`, `text`, `canvas`, and `flow` elements with modifier chains using `RemoteComposeContextAndroid`.

## API Style: Procedural (`RemoteComposeContextAndroid`)

Functions return `RemoteComposeWriter` or `RemoteComposeContext`. Used for demos registered in `DemosCreation.java`.

```kotlin
fun myDemo(): RemoteComposeWriter {
    val rc = RemoteComposeContextAndroid(
        platform = AndroidxRcPlatformServices(),
        apiLevel = 7,
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "My Demo"),
    ) {
        root {
            column(Modifier.fillMaxSize().background(Color.WHITE).padding(16)) {
                text("Hello World", fontSize = 64f)
            }
        }
    }
    return rc.writer
}
```

- Uses `Modifier` property (returns `RecordingModifier()`)
- Uses `RecordingModifier()` directly for explicit construction
- Modifier values are raw `Int`, `Float`, etc.
- Register in `DemosCreation.java` via `getp("path", MyDemoKt::myDemo)`
- Preview via `RemoteDocPreview(myDemo())`

---

## Document Setup

```kotlin
fun myDemo(): RemoteComposeWriter {
    val rc = RemoteComposeContextAndroid(
        platform = AndroidxRcPlatformServices(),
        apiLevel = 7,
        // Header tags (optional, use as needed)
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "description"),
    ) {
        root {
            // component tree goes here
        }
    }
    return rc.writer
}
```

Alternative constructors:
```kotlin
// Width/height/description constructor
RemoteComposeContextAndroid(500, 500, "Demo") { ... }

// Width/height/description/apiLevel/profiles constructor
RemoteComposeContextAndroid(500, 500, "Demo", 7,
    RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
    AndroidxRcPlatformServices()) { ... }
```

If you return `RemoteComposeContext` instead of `RemoteComposeWriter`, return `rc` directly (not `rc.writer`).

## Layout Components

All components take an optional `modifier` and a trailing lambda for children:

### `column`

Vertical layout. Children stack top-to-bottom.

```kotlin
column(
    modifier = Modifier.fillMaxSize().background(Color.YELLOW),
    horizontal = ColumnLayout.CENTER,   // child horizontal alignment
    vertical = ColumnLayout.TOP,        // defaults
) {
    text("Item 1")
    text("Item 2")
}
```

Alignment constants: `ColumnLayout.START`, `CENTER`, `END`, `TOP`, `BOTTOM`, `SPACE_EVENLY`, `SPACE_BETWEEN`.

### `row`

Horizontal layout. Children flow left-to-right.

```kotlin
row(
    modifier = Modifier.fillMaxWidth().padding(8),
    horizontal = RowLayout.SPACE_EVENLY,
    vertical = RowLayout.CENTER,
) {
    text("Left")
    text("Right")
}
```

Alignment constants: `RowLayout.START`, `CENTER`, `END`, `TOP`, `BOTTOM`, `SPACE_EVENLY`, `SPACE_BETWEEN`.

### `box`

Stacking layout. Children are layered on top of each other.

```kotlin
box(
    Modifier.fillMaxSize().background(Color.YELLOW),
    horizontal = BoxLayout.CENTER,
    vertical = BoxLayout.CENTER,
) {
    text("Centered")
}
```

Alignment constants: `BoxLayout.START`, `CENTER`, `END`, `TOP`, `BOTTOM`.

### `flow`

Wrapping row layout. Children wrap to the next line when they exceed width.

```kotlin
flow(
    Modifier.fillMaxWidth().background(Color.DKGRAY),
    RowLayout.SPACE_EVENLY,
    RowLayout.CENTER,
) {
    box(Modifier.background(Color.RED).size(200))
    box(Modifier.background(Color.GREEN).size(200))
    box(Modifier.background(Color.BLUE).size(200))
}
```

### `canvas`

Drawing surface where you can use `painter`, `drawLine`, `drawRect`, etc. Can also contain nested components.

```kotlin
canvas(Modifier.fillMaxWidth().height(200).background(Color.BLUE)) {
    val w = ComponentWidth()
    val h = ComponentHeight()
    painter.setColor(Color.RED).setStrokeWidth(4f).commit()
    drawLine(0f, 0f, w, h)

    // Components can be embedded inside canvas
    box(Modifier.size(100, 50).background(Color.CYAN).computePosition {
        x = w / 2f - RFloat(width.toFloat()) / 2f
        y = h / 2f - RFloat(height.toFloat()) / 2f
    }) {
        text("Inside Canvas", autosize = true, textAlign = CoreText.TEXT_ALIGN_CENTER)
    }
}
```

### `text`

Text display component with extensive formatting options.

```kotlin
// Simple string
text("Hello World")

// With full options
text(
    "Arsenal vs Bayern Munich",
    modifier = Modifier.fillMaxWidth().background(Color.LTGRAY),
    color = Color.WHITE,             // static ARGB color
    colorId = dynamicColorId,        // OR dynamic color ID (mutually exclusive)
    fontSize = 64f,                  // font size in pixels
    fontWeight = 700f,               // 100-900
    fontStyle = 1,                   // 0=normal, 1=italic
    fontFamily = "sans-serif",       // "default", "sans-serif", "serif", "monospace", or font file name
    textAlign = CoreText.TEXT_ALIGN_CENTER,    // LEFT, CENTER, RIGHT, JUSTIFY
    overflow = CoreText.OVERFLOW_ELLIPSIS,     // CLIP, ELLIPSIS, MIDDLE_ELLIPSIS, START_ELLIPSIS
    maxLines = 2,
    autosize = true,                 // auto-fit text to container
    minFontSize = 48f,               // min size when autosize=true
    maxFontSize = 200f,              // max size when autosize=true
    underline = true,
    strikethrough = false,
    letterSpacing = 0.1f,
    lineHeightMultiplier = 1.2f,
    lineBreakStrategy = CoreText.BREAK_STRATEGY_HIGH_QUALITY,
    hyphenationFrequency = CoreText.HYPHENATION_FREQUENCY_FULL,
    justificationMode = CoreText.JUSTIFICATION_MODE_INTER_CHARACTER,
    fontAxis = listOf("wght" to 600f),  // variable font axes
)

// With dynamic text ID (from createTextFromFloat, textMerge, etc.)
val textId = createTextFromFloat(someValue, 3, 1, Rc.TextFromFloat.PAD_PRE_NONE)
text(textId, fontSize = 48f, colorId = myColorId)
```

### `image`

Display a bitmap image.

```kotlin
val imageId = addBitmap(myBitmap)
image(Modifier.size(100), imageId, ImageScaling.SCALE_INSIDE, 1f)
```

### `space` (helper pattern)

There's no built-in `space()` component. Create one as an extension function:

```kotlin
fun RemoteComposeContextAndroid.space() {
    box(Modifier.horizontalWeight(1f))
}
```

## Modifier Reference (RecordingModifier)

Access via `Modifier` property inside `RemoteComposeContextAndroid` blocks, or construct with `RecordingModifier()`.

### Sizing

```kotlin
Modifier.fillMaxSize()           // fill both dimensions
Modifier.fillMaxWidth()          // fill width
Modifier.fillMaxHeight()         // fill height
Modifier.size(200)               // square 200x200
Modifier.size(300, 200)          // width=300, height=200
Modifier.width(200)              // fixed width (float or int)
Modifier.height(100)             // fixed height (float or int)
Modifier.widthIn(100f, 400f)     // min/max width constraints
Modifier.heightIn(50f, 300f)     // min/max height constraints
Modifier.wrapContentSize()       // wrap content
Modifier.wrapContentWidth()
Modifier.wrapContentHeight()
```

### Spacing & Weight

```kotlin
Modifier.padding(16)                 // all sides
Modifier.padding(8, 16, 8, 16)      // start, top, end, bottom
Modifier.padding(8f, 0f, 8f, 0f)    // float variant
Modifier.horizontalWeight(1f)       // flex weight in row
Modifier.verticalWeight(1f)         // flex weight in column
Modifier.spacedBy(8f)               // gap between children
```

### Appearance

```kotlin
Modifier.background(Color.YELLOW)            // ARGB int color
Modifier.background(0xFF007700L.toInt())     // hex color
Modifier.backgroundId(dynamicColorId)        // dynamic color expression ID (Int or Short)
Modifier.border(2f, 0f, Color.BLUE, 0)      // width, roundedCorner, color, shape
Modifier.dynamicBorder(10f, 30f, colorId, 0) // dynamic color border
Modifier.clip(RoundedRectShape(s, s, s, s))  // clip to rounded rect
Modifier.clip(RectShape(8f, 8f, 8f, 8f))    // clip to rect with corner radii
```

### Scrolling

```kotlin
Modifier.verticalScroll()                    // enable vertical scrolling
Modifier.verticalScroll(position.toFloat())  // with position tracking
Modifier.horizontalScroll()                  // enable horizontal scrolling
```

### Interaction

```kotlin
Modifier.onClick(action)           // click handler
Modifier.onTouchDown(action)       // touch down handler
Modifier.onTouchUp(action)         // touch up handler
```

### Advanced

```kotlin
Modifier.componentId(4343)          // assign component ID for reference
Modifier.visibility(intId)          // dynamic visibility
Modifier.alignByBaseline()          // baseline alignment in rows
Modifier.computeMeasure { height = width }  // dynamic measurement
Modifier.computePosition {                  // dynamic positioning
    x = (parentWidth - width) / 2f
    y = (parentHeight - height) / 2f
}
```

### Chaining

Modifiers chain left-to-right. Order matters for visual effects:

```kotlin
// Red background with yellow padding inside
Modifier.background(Color.RED).padding(32).background(Color.YELLOW)

// Double-border effect with clip
Modifier.padding(8)
    .clip(RoundedRectShape(s, s, s, s))
    .backgroundId(outerColor)
    .padding(4)
    .clip(RoundedRectShape(s, s, s, s))
    .backgroundId(innerColor)
    .padding(16)
```

## Building Reusable Components

Extract repeated UI patterns as extension functions on `RemoteComposeContextAndroid`:

```kotlin
fun RemoteComposeContextAndroid.stockCard(name: String, price: Float) {
    val s = 48f
    row(
        Modifier.padding(32, 0, 32, 28)
            .clip(RoundedRectShape(s, s, s, s))
            .backgroundId(panelColor)
            .horizontalWeight(1f)
            .widthIn(160f, Float.MAX_VALUE)
            .padding(24)
    ) {
        column {
            text(name, colorId = nameColorId)
            val priceText = createTextFromFloat(price, 8, 0, flags)
            text(priceText, colorId = priceColorId, fontSize = fontSize)
        }
    }
}
```

Then use it in your layout:

```kotlin
root {
    column(Modifier.fillMaxWidth()) {
        stockCard("S&P 500", 6846.51f)
        stockCard("Nasdaq", 23545.9f)
    }
}
```

## Themed / Dynamic Colors

Use `addThemedColor` for light/dark mode support, `addColorExpression` for animated/data-driven colors:

```kotlin
// Themed colors (global section)
beginGlobal()
val bgColor = writer.addThemedColor(
    Rc.AndroidColors.GROUP,
    Rc.AndroidColors.SYSTEM_ACCENT2_50,
    Rc.AndroidColors.SYSTEM_ACCENT2_800,
    0xFFFFFFFF.toInt(),  // light fallback
    0xFF000000.toInt(),  // dark fallback
)
endGlobal()

// Use with backgroundId
box(RecordingModifier().backgroundId(bgColor).fillMaxSize()) { ... }

// Named colors (resolved from system)
val namedColor = addNamedColor("color.system_accent2_800", 0xFFFF0000.toInt())

// Animated tween between two colors
val bounce = pingPong(1, ContinuousSec()).toFloat()
val animColor = addColorExpression(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), bounce)
box(RecordingModifier().backgroundId(animColor).height(120)) {}
```

## Mixing Canvas Drawing Inside Components

Components can contain `canvas` blocks, and canvas blocks can contain components:

```kotlin
column(Modifier.fillMaxSize().padding(16)) {
    text("Header", fontSize = 48f)

    // Canvas inside column
    canvas(RecordingModifier().fillMaxWidth().height(260)) {
        val w = ComponentWidth()
        val h = ComponentHeight()
        painter.setColor(Color.RED).setStrokeWidth(6f).commit()
        drawLine(0f, 0f, w, h)
    }

    // More components after canvas
    row(Modifier.fillMaxWidth()) {
        text("Footer Left")
        box(Modifier.horizontalWeight(1f))  // spacer
        text("Footer Right")
    }
}
```

## Scrolling Content

```kotlin
// Vertical scroll with position tracking
val position = rf(0f)
column(
    Modifier.fillMaxSize().padding(32).verticalScroll(position.toFloat()),
    horizontal = ColumnLayout.CENTER,
) {
    for (i in 0..100) {
        text(" $i ", fontSize = 64f, textAlign = CoreText.TEXT_ALIGN_CENTER)
    }
}

// Two side-by-side scrollable columns
row {
    column(Modifier.horizontalWeight(1f).fillMaxHeight().verticalScroll()) {
        for (i in 0..100) { text("$i") }
    }
    column(Modifier.horizontalWeight(1f).fillMaxHeight().verticalScroll()) {
        for (i in 0..100) {
            box(Modifier.size(200, 60).background(Color.RED)
                .border(2f, 0f, Color.BLUE, 0)
                .clip(RectShape(8f, 8f, 8f, 8f)),
                horizontal = BoxLayout.CENTER,
                vertical = BoxLayout.CENTER,
            ) { text("$i") }
        }
    }
}
```

## Previewing

Add a preview function for Android Studio:

```kotlin
@Preview @Composable private fun MyDemoPreview() = RemoteDocPreview(myDemo())
```

For functions returning `RemoteComposeContext`:
```kotlin
@Preview @Composable private fun MyDemoPreview() = RemoteDocPreview(MyDemo())
```

## Registering Demos

In `DemosCreation.java`:

```java
import androidx.compose.remote.integration.view.demos.examples.MyDemoKt;

// In getDemos():
getp("Category/DemoName", MyDemoKt::myDemoFunction),    // returns RemoteComposeWriter
getpc("Category/DemoName", MyDemoKt::myDemoFunction),   // returns RemoteComposeContext
```

## Common Patterns

### Card with Rounded Corners

```kotlin
val s = 48f
row(
    Modifier.padding(32, 0, 32, 28)
        .clip(RoundedRectShape(s, s, s, s))
        .backgroundId(panelColorId)
        .padding(24)
) { ... }
```

### Weighted Row (Split Layout)

```kotlin
row(Modifier.fillMaxWidth()) {
    column(Modifier.horizontalWeight(1f).background(Color.YELLOW)) {
        text("Left side")
    }
    column(Modifier.size(130).background(Color.CYAN)) {
        text("Fixed right")
    }
}
```

### Flexible Spacer

```kotlin
// Create an extension function
fun RemoteComposeContextAndroid.space() {
    box(Modifier.horizontalWeight(1f))
}

// Then use:
row {
    text("Left")
    space()
    text("Right")
}
```

### Badge / Pill Button

```kotlin
val s = 60f
box(
    Modifier.size(120).padding(16)
        .clip(RoundedRectShape(s, s, s, s))
        .backgroundId(colorId),
    horizontal = BoxLayout.CENTER,
    vertical = BoxLayout.CENTER,
) {
    text("Label", fontSize = 48f, colorId = textColorId,
         textAlign = TextLayout.TEXT_ALIGN_CENTER)
}
```

### Dynamic Font Sizing

```kotlin
// Scale fonts by system font size
val fontScale = rf(Rc.System.FONT_SIZE)
val headlineSize = (42f / 37f * fontScale).toFloat()
text("Title", fontSize = headlineSize)
```

### Animated Text Properties

```kotlin
// Pulsing font weight
val tween = (sin(ContinuousSec() % 3600f) + 1f) * 500f
text("Animated", fontWeight = tween.toFloat())

// Animated font size
val fontSize = (sin(ContinuousSec() % 3600f) + 1f) * 100f + 16f
text("Growing", fontSize = fontSize.toFloat())
```

## Example Files Reference

| File | Key Patterns |
|------|-------------|
| `Text.kt` | Text formatting, baseline alignment, autosize, overflow, font axes |
| `LayoutModifiersDemo.kt` | computeMeasure, computePosition, dynamic positioning |
| `RcTicker.kt` | Full app: themed colors, scrolling, extension functions, canvas in layout |
| `RcFlow.kt` | Flow layout with weights and size constraints |
| `CanvasComponents.kt` | Canvas with embedded components, scrolling, horizontal scroll |
| `ColorThemeCheck.kt` | Themed colors, verticalScroll, RecordingModifier |
| `DemoColor.kt` | Animated colors, dynamic borders, canvas in layout |
