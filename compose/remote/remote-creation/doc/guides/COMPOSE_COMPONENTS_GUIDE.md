# Guide to Building RemoteCompose Component-Based Examples (Compose-like)

This guide covers how to build examples using the **Compose-like components and modifiers** — the `@RemoteComposable` approach where you use components like `RemoteColumn`, `RemoteRow`, and `RemoteText` with `RemoteModifier`.

## API Style: Compose-like (`@RemoteComposable`)

`@Composable` functions annotated with `@RemoteComposable`. Used for demos registered in `DemosCompose.kt`.

```kotlin
@RemoteComposable @Composable
fun MyComposableDemo() {
    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize().background(Color.White).padding(16.dp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        RemoteText("Hello World", fontSize = 38.sp, fontWeight = FontWeight.Bold)
    }
}
```

- Uses `RemoteModifier` extension functions
- Units use `Dp`, `sp`, `rdp` (remote dp), `rf` (remote float)
- Components are `RemoteBox`, `RemoteColumn`, `RemoteRow`, `RemoteText`, `RemoteCanvas`, `RemoteImage`
- Register in `DemosCompose.kt` via `getComposeDoc(context, "path") { MyComposableDemo() }`
- Preview via `RemotePreview { MyComposableDemo() }`

---

## Components

| Component | Description |
|-----------|-------------|
| `RemoteBox` | Stacking container (like Compose `Box`) |
| `RemoteColumn` | Vertical layout (like Compose `Column`) |
| `RemoteRow` | Horizontal layout (like Compose `Row`) |
| `RemoteText` | Text display (like Compose `Text`) |
| `RemoteCanvas` | Drawing surface with `drawCircle`, etc. |
| `RemoteCanvas0` | Lower-level canvas with `drawAnchoredText` |
| `RemoteImage` | Image display |
| `FitBox` | Box that fits content with multiple layout variants |
| `RemoteCollapsibleColumn` | Column that can collapse items |
| `RemoteCollapsibleRow` | Row that can collapse items |
| `StateLayout` | Layout that switches between states |

## Layout Parameters

```kotlin
RemoteColumn(
    modifier = RemoteModifier.fillMaxWidth(),
    horizontalAlignment = RemoteAlignment.CenterHorizontally,  // Start, End, CenterHorizontally
    verticalArrangement = RemoteArrangement.SpaceBetween,       // Top, Bottom, Center, SpaceBetween, SpaceEvenly, SpaceAround
) { ... }

RemoteRow(
    modifier = RemoteModifier,
    horizontalArrangement = RemoteArrangement.SpaceBetween,
    verticalAlignment = RemoteAlignment.CenterVertically,
) { ... }

RemoteBox(
    modifier = RemoteModifier,
    horizontalAlignment = RemoteAlignment.CenterHorizontally,
    verticalArrangement = RemoteArrangement.Center,
) { ... }
```

## RemoteModifier Reference

```kotlin
// Sizing
RemoteModifier.fillMaxSize()
RemoteModifier.fillMaxWidth()
RemoteModifier.fillMaxHeight()
RemoteModifier.size(48.rdp)
RemoteModifier.size(60.rdp, 36.rdp)
RemoteModifier.width(100.rdp)
RemoteModifier.height(120.rdp)
RemoteModifier.height(IntrinsicSize.Min)    // intrinsic sizing
RemoteModifier.widthIn(min = 180.dp)
RemoteModifier.heightIn(min = 90.dp)
RemoteModifier.wrapContentSize()

// Spacing
RemoteModifier.padding(8.dp)
RemoteModifier.padding(left = 8.dp, right = 8.dp)
RemoteModifier.padding(bottom = 24.dp)
RemoteModifier.weight(1f)                    // flex weight

// Appearance
RemoteModifier.background(Color(219, 247, 239))
RemoteModifier.clip(RoundedCornerShape(24.dp))
RemoteModifier.clip(RectangleShape)

// Scrolling
RemoteModifier.verticalScroll(scrollState)
RemoteModifier.horizontalScroll(scrollState)

// Interaction
RemoteModifier.clickable(ValueChange(state, newValue))
RemoteModifier.visibility(intState)

// Transforms
RemoteModifier.graphicsLayer(scaleX = scale, scaleY = scale, rotationX = rotation)
RemoteModifier.offset(x = 10.rdp, y = 20.rdp)
RemoteModifier.zIndex(zValue)
```

## Units

```kotlin
8.dp    // density-independent pixels (standard Compose)
48.rdp  // remote dp (RemoteDp - used for size/width/height)
38.sp   // scale-independent pixels (for text)
1f.rf   // remote float (RemoteFloat - for expressions)
```

## State

```kotlin
val checked = rememberRemoteIntValue { 0 }       // mutable int state
val scale = rememberRemoteFloat { 0.8f.rf }       // computed float
val scrollState = rememberRemoteScrollState(evenNotches = 12)
val list = rememberRemoteStringList("OFF", "ON")  // string list for lookup

// Use state in text
RemoteText(list[checked])

// Use state in modifier
RemoteModifier.visibility(checked)
RemoteModifier.clickable(ValueChange(checked, toggleExpression))
```

## Canvas Inside Compose

```kotlin
RemoteCanvas(modifier = RemoteModifier.size(32.rdp)) {
    val paint = RemotePaint().apply { remoteColor = Color(255, 255, 255).rc }
    drawCircle(paint = paint, radius = 34f.rf)
}
```

## Registering Demos

In `DemosCompose.kt`:

```kotlin
getComposeDoc(context, "Compose/MyDemo") { MyComposableDemo() }
```

## Common Patterns

### Card with Rounded Corners

```kotlin
RemoteColumn(
    modifier = RemoteModifier
        .clip(RoundedCornerShape(24.dp))
        .background(Color(219, 247, 239))
        .padding(8.dp)
) { ... }
```

### Weighted Row (Split Layout)

```kotlin
RemoteRow(modifier = RemoteModifier.fillMaxWidth()) {
    RemoteBox(RemoteModifier.weight(1f)) { RemoteText("Left") }
    RemoteBox(RemoteModifier.width(130.rdp)) { RemoteText("Right") }
}
```

### Flexible Spacer

```kotlin
RemoteRow {
    RemoteText("Left")
    RemoteBox(RemoteModifier.weight(1f))
    RemoteText("Right")
}
```

## Example Files Reference

| File | Key Patterns |
|------|-------------|
| `DemoWeather.kt` | Full app: collapsible layouts, images, responsive design |
| `SwitchWidget.kt` | State management, clickable, visibility, StateLayout |
| `ScrollView.kt` | Scroll state, graphicsLayer transforms, RemoteCanvas0 |
