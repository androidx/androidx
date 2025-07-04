
## Naming {#naming}

<!-- TODO(seanmcq) Update this link to new location after drop -->

Please, refer to the corresponding
[Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md#naming-unit-composable-functions-as-entities)
section for naming conventions. However, there are more detailed considerations
to keep in mind.

Jetpack Compose framework development **MUST** follow the rules in this section.

Library development **MUST** follow the section below.

App development MAY follow the rules below.

### Naming Components {#general-naming-rules}

SHOULD avoid `CompanyName` (`GoogleButton`) or Module (`WearButton`) prefixes
where possible.

SHOULD use design/app package names (e.g. `com.companyname.ui.Button` or
`com.companyname.ui.Icon`). Simple names make sure these components feel
first-class when used.

SHOULD expose use-case derived names such as Button.

SHOULD use prefixes based on use-case or design-language to differentiate
related components (e.g.`ContainedButton`, `OutlinedButton`)

SHOULD expose a prefix-free name for the "default" or most common invocation.

SHOULD use use-case prefixes when adding a new layer (e.g. `ScalingLazyColumn`,
`CurvedText`). If impossible or the use case clashes with the existing
component, module/library prefix can be used e.g. `GlideImage.`

**Do**

```kotlin {.good}
// This button is called ContainedButton in the spec
// It has no prefix because it is the most common one
@Composable
fun Button(...) {}

// Other variations of buttons below:
@Composable
fun OutlinedButton(...) {}

@Composable
fun TextButton(...) {}

@Composable
fun GlideImage(...) {}
```

**Also do (if your library is based on compose-foundation)**

```kotlin {.good}
// package com.company.project
// depends on foundation, DOES NOT depend on material or material3

@Composable
fun Button(...) {} // simple name that feel like a first-class button

@Composable
fun TextField(...) {} // simple name that feel like a first-class TF

```

### BasicComponent {#basic-component}

SHOULD use `BasicComponent` prefix for components that provide barebones
functionality with no design system.

SHOULD use `Component` for the opiniated design system API that is ready for
direct use on a screen.

Foundation and UI SHOULD prefer to expose `BasicComponent` for aggregate
components (`BasicText`), and `Component` for simple components (e.g.
`Layout`/`Box`). An aggregate component combines several pieces of functionality
and solves a complete use case, while a simple component exposes a single
capability.

```kotlin {.good}
// component that has no decoration, but basic functionality
@Composable
fun BasicTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    ...
)

// ready to use component with decorations
@Composable
fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    ...
)
```
