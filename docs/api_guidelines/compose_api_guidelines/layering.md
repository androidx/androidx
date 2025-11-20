
## Layering {#layering}

UI Libraries in `androidx.compose` **MUST** adhere to a strict layering.

Each layer may only depend on any layers layer below it, and SHOULD pin to a
stable version.

Design System (e.g. Material, Material3)
:   A collection of components that define a single design system. Design system
    components are pre-designed to give a unified look and feel.

Foundation
:   Components and APIs used by design systems, apps, or other libraries. Basic
    UI building blocks for building UI components at any higher layer.

UI
:   Primitive components for UI (e.g. Layout, Modifier), I/O systems (e.g.
    keyboard and mouse), and user agents (e.g. Semantics and Autofill).

Runtime
:   Tools for interacting with `@Composable`s, the composer, Snapshots, and the
    slot table.

Platform (Android)
:   Provides the runtime environment for windowing, input, drawing, and
    executing programs.

### Single responsibility {#single-responsibility}

Components SHOULD be single responsibility, and expose a necessary capability
for the next layer up.

Components at the top layers SHOULD solve exactly one foundation or design
language problem. Components at lower layers should provide one capability (e.g.
`Layout` or `clickable`) for use by higher layers.

Components at the bottom layers SHOULD provide one capability instead of large
aggregate component. Prefer to make `Layout` instead of `Button`.

If a component is solving multiple problems, split each problem into building
blocks and expose them too, then aggregate the building blocks together in the
high level API.

#### Case study: Button

**Layer:** Button is a design system concept and belongs in a design system
library.

**Dependency:** Button should be easily expressible in terms of ui and
foundation capabilities. Prefer to depend on capabilities in foundation and ui
like `Layout`, `Box`, `clickable`, `semantics`,.

**Single presentation:** Button should only be one kind of button and not try to
be everything clickable.

**DON’T**

```kotlin {.bad}
// avoid multipurpose components: for example, this button solves more than 1 problem
@Composable
fun Button(
    // problem 1: button is a clickable rectangle
    onClick: () -> Unit = {},
    // problem 2: button is a check/uncheck checkbox-like component
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) { ... }
```

**Do:**

```kotlin {.good}
@Composable
fun Button(
    // problem 1: button is a clickable rectangle
    onClick: () -> Unit,
) { ... }

@Composable
fun ToggleButton(
    // problem 1: button is a check/uncheck checkbox-like component
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) { ... }
```

### Expose your building blocks {#legos-all-the-way-down}

When creating components, `androidx.compose` library authors SHOULD separate
aggregate components such as Button into layers of single purpose building
blocks that solve exactly one problem each.

### Foundation and UI components are general {#layering-foundation-and-ui}

*   Foundation and UI components SHOULD solve a single problem.
*   Foundation and UI components SHOULD NOT contain opinions about design
    systems.
*   Foundation and UI components SHOULD NOT participate directly in theming.

The problem should be specific, have a real use case, and be able to be used by
multiple design languages and preferably multiple high-level components in the
same design language.

**Don't:**

```kotlin {.bad}
@Composable fun BaseButtonWithRipple
```

**Do:**

```kotlin {.good}
- @Composable fun Box
- Modifier.clickable
- Semantics
- Interaction (ripple)
- ...
```
