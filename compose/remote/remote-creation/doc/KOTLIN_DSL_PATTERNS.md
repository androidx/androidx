# Kotlin DSL Patterns

Common usage patterns for creating RemoteCompose documents using the Kotlin DSL.

## Basic Layout
```kotlin
RemoteComposeContextAndroid(500, 500, "Demo", platform) {
    root {
        column(Modifier.fillMaxSize().padding(16f)) {
            text("Title", fontSize = 24f)
            box(Modifier.size(100f).background(Color.BLUE))
        }
    }
}
```

## State and Interactions
```kotlin
val count = addNamedInt("clickCount", 0)
column {
    text("Count: " + count)
    box(Modifier.size(50f).clickable(
        ValueIntegerChangeAction(count, count + 1)
    ))
}
```

## Canvas Drawing
```kotlin
canvas(Modifier.fillMaxWidth().height(200f)) {
    painter.setColor(Color.RED).setStrokeWidth(5f).commit()
    drawLine(0f, 0f, ComponentWidth(), ComponentHeight())
}
```

## Responsive Sizing
```kotlin
row(Modifier.fillMaxWidth()) {
    box(Modifier.horizontalWeight(1f).background(Color.GRAY))
    box(Modifier.horizontalWeight(2f).background(Color.BLUE))
}
```
