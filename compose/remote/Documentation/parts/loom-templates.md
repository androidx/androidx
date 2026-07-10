# Loom: The RemoteCompose Component Template System

The Component Template system in RemoteCompose (technically implemented via binary **Patterns**) is a sophisticated templating and composition engine. It is designed to bridge the gap between high-level developer abstractions (like reusable components) and a compact, efficient binary wire format.

By allowing developers to define logic and layout patterns once and reuse them with dynamic parameters, the system significantly reduces document size while maintaining maximum runtime flexibility.

## Why a Template System?

Standard static inclusion systems (like simple "copy-paste" of operation blocks) are insufficient for modern UI needs. The RemoteCompose Template System was built to solve several critical challenges:

1.  **Binary Efficiency**: Sending a complex component (like a button with 10+ operations) multiple times is wasteful. Templates allow sending the definition once and only sending the unique arguments for each instance.
2.  **Dynamic Parameterization**: Static blocks cannot change their behavior based on where they are used. Templates can accept colors, text, dimensions, and even click-action targets as parameters.
3.  **UI Consistency**: Centralizing the definition of UI elements (buttons, cards, headers) ensures that a change to the template definition automatically updates every instance across the entire document.
4.  **Decoupling**: It separates the *design* of a component (the template) from its *implementation* at the call site (the arguments).

---

## Internal Mechanics: How It Works

The system operates on a **Late Expansion** model. Expansion happens on the **Player side** during the **Materialization** (Inflation) phase, after reading the raw binary buffer but before the layout tree is fully initialized.

### 1. The Expansion Pipeline
When `CoreDocument` processes its operations, it executes the following pipeline:
-   **Registration**: All `PatternDefine` (Template Definition) blocks are stored in a `LoomManager`. The binary body of the macro is captured and stored.
-   **Expansion/Inflation**: The document traverses its flat list of operations. When it encounters a **`PatternInflation`** (Template Inclusion) or **`IncludeReferencedOperations`** (Static Style Inclusion), it triggers the expansion engine (`ExpansionContext`).
-   **Re-inflation**: Instead of cloning objects via `deepCopy()`, the system **re-reads the macro body from its binary source**.
    -   The standard `read(buffer, operations, RemapContext)` method is called.
    -   Crucially, the input `WireBuffer` is **wrapped** by the `RemapContext` (see `LoomWireBuffer`) to handle IDs transparently.
-   **ID Management (WireBuffer)**: The engine processes every ID using the buffer's enhanced API.
    -   **Global IDs (Tier 1: 0-41)**: System variables (e.g., `WINDOW_WIDTH`) are **never** remapped by the buffer.
    -   **Pattern-Local IDs (Tier 2: 0x4000 - 0x4FFF)**: Reserved for template-local state. The buffer's `declareId()` method generates a new, unique ID for these for every call site.
    -   **Parameter Substitution**: The call site seeds the `RemapContext` with mappings from **Parameter IDs** to **Argument IDs**, which the buffer uses during resolution.
-   **Block Injection**: `PatternArgument` placeholders are swapped for the specific list of operations provided in the `PatternBlock` at the call site.
-   **Collection Iteration**: `PatternForEach` forks the context and creates a new wrapped buffer for every item in a collection, remapping the `localItemId` to the current item's ID.

### 2. ID Management API
To participate in the system, operations use four key methods on the **`WireBuffer`** during their `read` implementation:

-   **`buffer.declareId()`**: Used when reading an operation that **defines** a new ID (e.g., `BooleanConstant`, `PathData`).
    -   It reads an `int` and allocates a new unique ID if we are inside a macro or if the ID is macro-local.
    -   It records the mapping so future references can find it.
-   **`buffer.resolveId()`**: Used when reading an operation that **references** an existing ID (e.g., `DrawPath(id)`).
    -   It reads an `int` and looks it up in the current mapping table.
    -   If no mapping exists, it returns the original ID (allowing global references).
-   **`buffer.resolveNanId()`**: Similar to `resolveId`, but reads a `float` and resolves it if it contains a NaN-encoded ID.
-   **`buffer.resolveLongNanId()`**: Similar to `resolveId`, but reads a `long` and resolves it if it contains a NaN-encoded ID.

Operations also implement the **`VariableProvider`** interface if they define a primary variable ID (e.g., `FloatConstant`). This allows the runtime to track which operations are sources of data for the expression engine.

---

## Advanced Capabilities

### 1. Style Factories (Style Templates)
`PatternInflation` implements both the `ModifierOperation` and `Container` interfaces. 
-   **Expansion in Modifiers**: Templates can be expanded *inside* component modifier blocks. The expansion engine traverses into `ComponentModifiers` to resolve any nested templates.
-   **Advantage**: You can define a button's "look" once and apply it to any component. Changing the template updates the design system-wide.

### 2. Templates with Local State
By using **Tier 2 IDs**, you can create templates that maintain their own independent interactive state.

```kotlin
// Example: A button that counts its own clicks
val templateId = defineMacro("Counter", labelParam) {
    val countId = localId() // Allocates in 0x4000 range
    addFloat(countId, 0f)
    
    val incrementExpr = floatExpression(floatId(countId), 1f, ADD)
    val displayText = createTextFromFloat(floatId(countId), 3, 0, 0)
    
    column(Modifier.onClick(ValueFloatExpressionChange(countId, incrementExpr))) {
        text(labelParam)
        text(displayText)
    }
}
```
Because `countId` is in the macro-local range, every instance of the `Counter` button will have its own independent variable in the document state.

### 3. Slot-Based Composition
Using `PatternArgument` and `MacroBlock`, you can implement the "Slot Pattern" common in frameworks like Compose.
- **Capability**: Create a `Card` template that defines the frame but provides a "slot" for the body content.
- **Advantage**: The parent template manages layout while the caller provides specific child content.

### 4. Pattern-Level Iteration (PatternForEach)
The `PatternForEach` operation enables templates to handle variable-sized collections (like breadcrumbs or list items) during expansion. It iterates over a collection ID and clones its child operations for every item found.

- **Dynamic Mapping**: Inside the `PatternForEach` block, you define a `localItemId`. For each iteration, the expansion engine automatically remaps this local ID to the actual ID of the current item in the collection.
- **Efficiency**: Allows complex repeated UI structures to be defined once in a template and expanded based on data provided at the call site.

```kotlin
// Example: A template for a simple list
val listItemsParam = defineMacroParameter("items")
defineMacro("ListTemplate", listItemsParam) {
    column {
        val localItem = localId() // Unique ID for remapping
        macroForEach(listItemsParam, localItem) {
            row {
                image(localItem) // localItem will be the actual bitmap ID
                text("Item found")
            }
        }
    }
}
```

### 5. Conditional Expansion (PatternConditional - Planned)
Templates are designed to include logic that is evaluated during expansion. The proposed `PatternConditional` opcode will allow you to include or exclude blocks of operations based on the values of parameters *at inflation time*.

---

## Practical DSL Examples

### Example 1: Creating a "Normal" Component Helper
To make templates feel like native components in the Kotlin DSL, wrap the `callMacro` in a standard function.

```kotlin
// 1. Define the Template (once)
fun RemoteComposeContext.DefineMyButton() {
    val labelParam = defineMacroParameter("label")
    val targetParam = defineMacroParameter("targetId")

    defineMacro("MyButton", labelParam, targetParam) {
        box(Modifier.padding(16).background(Color.BLUE).onClick(ValueStringChange(targetParam, "Clicked!"))) {
            text(labelParam, color = Color.WHITE)
        }
    }
}

// 2. Wrap the inclusion in a "normal-looking" function
fun RemoteComposeContext.MyButton(label: String, targetId: Int) {
    val macroId = textId("MyButton")
    // Hide the template complexity from the caller
    callMacro(macroId, textId(label), targetId)
}

// 3. Usage looks like a standard component
root {
    column {
        MyButton(label = "Save", targetId = saveId)
        MyButton(label = "Cancel", targetId = cancelId)
    }
}
```

### Example 2: List with ForEach
You can pass a collection ID to a macro and let it handle the repetition.

```kotlin
// 1. Define list template
val collectionParam = defineMacroParameter("items")
defineMacro("ImageGallery", collectionParam) {
    flow {
        val itemId = localId()
        macroForEach(collectionParam, itemId) {
            box(Modifier.size(100.rdp).padding(4.rdp)) {
                image(itemId)
            }
        }
    }
}

// 2. Pass an actual collection at call site
val myImages = addDataListIds(intArrayOf(bitmap1, bitmap2, bitmap3))
callMacro("ImageGallery", myImages)
```

### Example 3: Dynamic Style Templates (Style Factories)
You can define a shared visual style that takes parameters and apply it as a modifier to many components.

```kotlin
// 1. Define a "Style Template" that takes a background color parameter
val colorParam = defineMacroParameter("backgroundColor")

val buttonStyleId = defineMacro("ButtonStyle", colorParam) {
    // Emit modifiers directly into the macro
    modifier(Modifier
        .clip(RoundedRectShape(8f, 8f, 8f, 8f))
        .backgroundId(colorParam)
        .padding(24f, 12f, 24f, 12f)
    )
}

// 2. Apply the template as a modifier with different arguments
val primaryColor = addColor(0xFF6200EE.toInt())
val secondaryColor = addColor(0xFF03DAC6.toInt())

column {
    // Use includeMacro with the template ID and the color ID
    box(Modifier.includeMacro(buttonStyleId, intArrayOf(primaryColor))) {
        text("Primary", color = Color.WHITE)
    }

    box(Modifier.size(20f))

    box(Modifier.includeMacro(buttonStyleId, intArrayOf(secondaryColor))) {
        text("Secondary", color = Color.BLACK)
    }
}
```

### Example 3: Static Style Inclusion (Referenced Operations)
For simple, static reuse of modifier sets without parameters, use `ReferencedOperations`.

```kotlin
// 1. Define the shared style
val sharedButtonStyleId = referencedOperations {
    Modifier.padding(16)
        .clip(RoundedRectShape(16f, 16f, 16f, 16f))
        .background(0xFFE0E0E0.toInt())
        .padding(24)
}

// 2. Use it via Modifier.include(id)
box(Modifier.include(sharedButtonStyleId)) {
    text("Ok", fontSize = 30f)
}
```

### Example 4: Complex Composition (Slots)
A container that adds a header and footer around any provided content.

```kotlin
// Definition
defineMacro("StandardPage") {
    column(Modifier.fillMaxSize()) {
        text("App Header", fontSize = 40f)
        macroArgument(0) // The Content Slot
        text("Copyright 2026", fontSize = 12f)
    }
}

// Inclusion (Call)
callMacro("StandardPage") {
    macroBlock(0) {
        text("Main Body Content")
        image(myBitmapId)
    }
}
```

---

## Using Templates in the Frontend (`creation-compose`)

The high-level `creation-compose` layer (which uses `@Composable` functions) can also leverage templates, particularly for **Style Templates**.

### Reusing Style Templates via Modifiers
If you have a template that defines a set of modifiers, you can apply it using the `includeMacro` extension on `RemoteModifier`.

```kotlin
@Composable
fun RemoteTemplateButton(
    label: RemoteString,
    backgroundColorId: RemoteInt,
    modifier: RemoteModifier = RemoteModifier
) {
    val styleTemplateId = 100 // Assume this is the ID of your "ButtonStyle" template
    
    RemoteBox(
        modifier = modifier
            // Apply the shared style template with a dynamic color argument
            .includeMacro(styleTemplateId, backgroundColorId)
            .onClick { /* ... */ }
    ) {
        RemoteText(label)
    }
}
```

This approach allows you to combine the ease of use of the `@Composable` frontend with the binary efficiency and consistency of the Component Template system.

## Best Practices

-   **Avoid Excessive Nesting**: Deeply nested templates (e.g., 5+ levels) can increase the CPU time required for document inflation.
-   **Name Your Parameters**: Always use `defineMacroParameter("name")`. While the system uses numeric IDs internally, having names in the string pool makes debugging the binary format much easier.
-   **Use Style Templates for Theming**: Encapsulate your design system's primitive atoms (buttons, inputs, cards) into templates early to ensure consistency across both the DSL and the Compose frontend.
