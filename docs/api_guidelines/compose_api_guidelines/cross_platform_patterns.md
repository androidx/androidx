## Cross-platform patterns {#cross-platform}

### Philosophy

Platform-only types should only be declared and used in platform-specific source
sets or modules.

Do not introduce public `commonMain` APIs that expose platform-only types or
require them in signatures. Interoperability with platform types should be
provided via platform-only extensions in platform-specific source sets or
modules.

For example, avoid using an `expect class` and `actual typealias`. The actual
`typealias` is substituted by the Kotlin compiler with the alias and becomes the
part of the binary API for any use of this type. This prevents a target platform
from being able to supply platform implementations in separate modules such as
deferring the choice between using AWT and Swing, or between using SKIA or
Vulkan or Metal.

### Key Principles

When introducing a platform abstractions (such as `Paragraph` or `Clipboard`),

1.  **Add an `interface` in `commonMain`**

<!-- end list -->

-   Most universal and maintainable approach
-   Does not introduce compatibility issues CMP targets.
-   Use a `sealed` interface when possible.

<!-- end list -->

1.  **Provide Platform Interop via Extensions**

<!-- end list -->

-   Place platform specific extensions in platform-specific source sets only
-   Use `as*()` naming convention (e.g., `asAndroidThing()`, `asComposeThing()`)
    -   A `as*()` must follow `as*()` rules. That is, it is either a helper that
        around a cast or it is a wrapper where all modification made to the
        wrapper are immediately reflect to the wrapped object (that is, there is
        only one source of truth).
-   Or property-style extensions (e.g., `val ComposeThing.androidThing`)

<!-- end list -->

1.  **Keep Platform Types Internal**

<!-- end list -->

-   Implementation classes should be `private` or `internal`.
-   Platform-specific types should not appear in public `commonMain` signatures.

<!-- end list -->

1.  **Factory Functions for Construction**

<!-- end list -->

-   If creation from `commonMain` is needed, use factory functions
-   Delegate to `internal expect` functions for platform-specific creation

### Creating a type that wraps a platform service or feature

When creating a type that wraps behavior expected to be provided or customized
on the target platform use the following pattern,

**Do**

```kotlin {.good}
// commonMain
interface ComposeThing {
  fun doPlatformThing();
}

// Only if creation from commonMain is required
fun ComposeThing(arg1: Int): ComposeThing =
    createPlatformThing(arg1)

internal expect fun createPlatformThing(arg1: Int): ComposeThing
```

```kotlin {.good}
// androidMain (or any platform-specific source set)
private class ComposeThingImpl(
    val _androidThing: AndroidThing,
) : ComposeThing

internal actual fun createPlatformThing(arg1: Int): ComposeThing =
    AndroidThing(arg1).asComposeThing()

// Platform interop via extensions
fun ComposeThing.asAndroidThing(): AndroidThing =
    (this as ComposeThingImpl)._androidThing

fun AndroidThing.asComposeThing(): ComposeThing =
    ComposeThingImpl(this)

// Alternative: property-style (also acceptable)
val ComposeThing.androidThing: AndroidThing
    get() = (this as ComposeThingImpl)._androidThing
```

Some examples in Compose that follow this example include,

-   [`androidx.compose.ui.text.Paragraph`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-text/src/commonMain/kotlin/androidx/compose/ui/text/Paragraph.kt)
-   [`androidx.compose.ui.text.font.Font`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-text/src/commonMain/kotlin/androidx/compose/ui/text/font/Font.kt)
-   [`androidx.compose.ui.graphics.ImageBitmap`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-graphics/src/commonMain/kotlin/androidx/compose/ui/graphics/ImageBitmap.kt)

Avoid patterns like,

**Don't**

```kotlin {.bad}
// commonMain
expect class NativeThing // ❌ Exposes the platform native type in common API.

class ComposeThing1(
    val nativeThing: NativeThing,  // ❌ Exposed in constructor or property
)

interface ComposeThing2 {
    fun asFrameworkThing(): NativeThing  // ❌ Exposed in return type
}

expect val ComposeThing4.nativeThing: NativeThing  // ❌ Exposed as property
```

Platform types,

-   SHOULD use an `interface` or `sealed interface` for platform types.

-   SHOULD create an expect function that creates the platform specific
    instance.

-   SHOULD have an implement the expect for the constructor function in a
    platform-specific source set or module.

-   SHOULD NOT use `actual typealias` for a platform type.

### Exceptions

#### `typealias`

There are times where `actual typealias` is reasonable (see
[CompositeKeyHashCode](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/CompositeKeyHashCode.kt))
and should only be used when a platform cannot efficiently represent a type
(such as JavaScript, `Long` is not efficient and the `CompositeKeyHashCode` then
maps to a type that is more efficient). A `typealias` should not be used for
types that are already reference types (interfaces, classes, etc.). Use the
pattern above for these types.
