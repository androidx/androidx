# RemoteCompose Creation DSL

The RemoteCompose Creation DSL is a modern, type-safe Kotlin API designed to build RemoteCompose documents. It is inspired by Jetpack Compose and aims to provide a familiar and expressive developer experience while maintaining the efficiency and portability of the underlying wire format.

## 1. Aims

*   **Type Safety**: Replace raw `Int` IDs with type-safe references like `RcFloat`, `RcPath`, and `RcText` to prevent common ID-reuse or type-mismatch errors.
*   **Familiarity**: Mimic the Jetpack Compose API (e.g., `Box`, `Column`, `Row`, `Modifier`, `dp`/`sp` units) to leverage existing developer knowledge.
*   **Expressiveness**: Allow for natural mathematical expressions and procedural logic that are automatically serialized for player-side evaluation.
*   **Encapsulation**: Hide the complexity of the `RemoteComposeWriter` and the binary protocol behind a clean, declarative interface.
*   **Contextual Awareness**: Use specialized scopes (e.g., `RcCanvasScope`) to provide only the operations relevant to the current component.

## 2. Implementation Architecture

The DSL is built as a layered abstraction over the low-level `RemoteComposeWriter`.

### 2.1. Scopes (`RcScope`)
The core of the DSL is the `RcScope` interface and its implementations.
*   **`RcScope`**: The root interface providing general layout components (`Box`, `Column`, `Row`), resource registration (`addText`, `addColor`), and global state management.
*   **Specialized Scopes**:
    *   `RcCanvasScope`: Provides drawing primitives (`drawCircle`, `drawRect`), matrix transformations (`scale`, `rotate`), and path operations.
    *   `RcColumnScope` / `RcRowScope`: Provide layout-specific modifiers like `Modifier.weight()`.
*   **`RcScopeImpl`**: The internal implementation that delegates calls to a `RemoteComposeWriter`. It maintains the relationship between the DSL and the underlying document buffer.

### 2.2. Type-Safe References
Instead of returning raw integers, the DSL uses specialized classes to represent remote resources and expressions:
*   **`RcFloat`**: Encapsulates a float expression. It supports operator overloading (`+`, `-`, `*`, `/`, `%`) and functions (`sin`, `cos`, `abs`, `sqrt`) that build a Reverse Polish Notation (RPN) array for player-side execution.
*   **`RcPath`**: Represents remote path data. It provides methods like `lineTo()`, `moveTo()`, and `close()` that append commands to an existing remote path resource.
*   **`RcText`, `RcColor`, `RcImage`**: Inline value classes that wrap remote resource IDs, ensuring they are used correctly in components and drawing commands.

### 2.3. Modifiers
The `Modifier` system mirrors Jetpack Compose's immutable chain pattern.
*   **`Modifier.Element`**: Individual modifications (e.g., `PaddingModifier`, `BackgroundModifier`).
*   **`toRecordingModifier()`**: An internal helper that folds the DSL modifier chain into the legacy `RecordingModifier` expected by the writer.
*   **Scopes**: Some modifiers (like `weight`) are only available within specific scopes, preventing layout errors at compile-time.

## 3. Pros and Cons

### Pros
*   **Readability**: Declarative layout blocks and natural math expressions make the document structure and logic easy to follow.
*   **Maintainability**: Type safety and encapsulated state reduce the risk of regressions when refactoring complex procedural documents.
*   **Interactivity**: Integrated support for time-based variables (`Hour()`, `animationTime()`) and touch feedback (`onClick`) makes building interactive widgets straightforward.
*   **Ergonomics**: Features like member extensions (`5.rf`, `16.rsp`) and trailing lambdas reduce boilerplate.

### Cons
*   **Learning Curve for Expressions**: While math operators are natural, understanding when an expression is "flushed" to a remote variable vs. remaining an inline RPN array requires some familiarity with the underlying engine.
*   **Performance Overhead**: As a high-level abstraction, it introduces some object allocation (though `value classes` and efficient pooling minimize this).
*   **Dual API**: During the transition period, developers may need to navigate both the new DSL and the legacy procedural APIs in existing codebases.

## 4. Design Patterns & Examples

### 4.1. Remote Expressions
The DSL allows for complex logic that runs on the player (e.g., a clock hand).
```kotlin
// In RcCanvasScope
val hrHand = (Hour() + (Minutes() % 60f) / 60f) * 30f
save {
    rotate(hrHand, cx, cy)
    drawLine(cx, cy, cx, cy - rad / 3f)
}
```

### 4.2. Path Builders
Paths are created and modified using a fluent, stateful API or a scoped builder lambda.
```kotlin
val path = addPath(0f, 0f) {
    lineTo(100f, 100f)
    quadTo(150f, 50f, 200f, 100f)
}
drawPath(path)
```

### 4.3. Resource Deduping & Theming
The DSL simplifies themed resource creation.
```kotlin
val background = addThemedColor(
    light = "color.system_accent2_50",
    lightDefault = 0xFF113311.toInt(),
    dark = "color.system_accent2_800",
    darkDefault = 0xFFFF9966.toInt()
)
```

### 4.4. Component Hierarchy
Layouts are nested naturally, with modifiers applied at each level.
```kotlin
Box(Modifier.fillMaxSize()) {
    Column(Modifier.padding(16.rdp)) {
        Text("Hello DSL", fontSize = 24.rsp)
        Spacer(Modifier.height(8.rdp))
        Image(myIcon, Modifier.size(48.rdp))
    }
}
```
