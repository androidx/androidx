# API Guidelines for `@Composable` components in Jetpack Compose

## Last updated: July 19, 2023

Set of guidelines and recommendations for building scalable and user-friendly @Composable components.

The requirement level of each of these guidelines is specified using the terms set forth in [RFC2119](https://www.ietf.org/rfc/rfc2119.txt) for each of the following developer audiences. If an audience is not specifically named with a requirement level for a guideline, it should be assumed that the guideline is OPTIONAL for that audience.

### Jetpack Compose framework development

Contributions to the androidx.compose libraries and tools generally follow these guidelines to a strict degree to promote consistency, setting expectations and examples for consumer code at all layers.

### Library development based on Jetpack Compose

It is expected and desired that an ecosystem of external libraries will come to exist that target Jetpack Compose, exposing a public API of `@Composable` functions and supporting types for consumption by apps and other libraries. While it is desirable for these libraries to follow these guidelines to the same degree as Jetpack Compose framework development would, organizational priorities and local consistency may make it appropriate for some purely stylistic guidelines to be relaxed.

### App development based on Jetpack Compose

App development is often subject to strong organizational priorities and norms and requirements to integrate with existing app architecture. This may call for not only stylistic deviation from these guidelines but structural deviation as well. Where possible, alternative approaches for app development will be listed in this document that may be more appropriate in these situations.

## Table of content
- [Note on vocabulary in this doc](#note-on-vocabulary-in-this-doc)
- [Before you create a component](#before-you-create-a-component)
    - [Component’s purpose](#components-purpose)
    - [Component layering](#component-layering)
    - [Do you need a component?](#do-you-need-a-component)
    - [Component or Modifier](#component-or-modifier)
- [Name of a Component](#name-of-a-component)
    - [BasicComponent vs Component](#basiccomponent-vs-component)
    - [Design, Usecase or Company/Project specific prefixes](#design-usecase-or-companyproject-specific-prefixes)
- [Component dependencies](#component-dependencies)
    - [Prefer multiple components over style classes](#prefer-multiple-components-over-style-classes)
    - [Explicit vs implicit dependencies](#explicit-vs-implicit-dependencies)
- [Component parameters](#component-parameters)
    - [Parameters vs. Modifier on the component](#parameters-vs-modifier-on-the-component)
    - [`modifier` parameter](#modifier-parameter)
    - [Parameters order](#parameters-order)
    - [Nullable parameter](#nullable-parameter)
    - [Default expressions](#default-expressions)
    - [MutableState\<T\> as a parameter](#mutablestatet-as-a-parameter)
    - [State\<T\> as a parameter](#statet-as-a-parameter)
    - [Slot parameters](#slot-parameters)
        - [What are slots](#what-are-slots)
        - [Why slots](#why-slots)
        - [Single “content” slot overloads](#single-content-slot-overloads)
        - [Layout strategy scope for slot APIs](#layout-strategy-scope-for-slot-apis)
        - [Lifecycle expectations for slot parameters](#lifecycle-expectations-for-slot-parameters)
        - [DSL based slots](#dsl-based-slots)
- [Component-related classes and functions](#component-related-classes-and-functions)
    - [State](#state)
    - [ComponentDefault object](#componentdefault-object)
    - [ComponentColor/ComponentElevation objects](#componentcolorcomponentelevation-objects)
- [Documentation for the component](#documentation-for-the-component)
    - [Documentation structure and ordering](#documentation-structure-and-ordering)
    - [Documentation example](#documentation-example)
- [Accessibility of the component](#accessibility-of-the-component)
    - [Semantics merging](#semantics-merging)
    - [Accessibility related parameters](#accessibility-related-parameters)
    - [Accessibility tuning](#accessibility-tuning)
- [Tooling support](#tooling-support)
- [Evolution of the Component APIs](#evolution-of-the-component-apis)

## Note on vocabulary in this doc

**@Composable component** - A @Composable function that returns `Unit` and emits the UI when it is composed in a hierarchy (later: component).

**Developer** - a person who creates a component that is to be used by a user in an application or in another component library.

**User** - the user of the component - a person who uses the component in a composable hierarchy to show some ui to the end-user.

**End-user** - the person who will use the application created by the user of your component.

These guidelines outline the best practices for developing UI components using Jetpack Compose. Best practices ensure that the API of the components is:

*   **Scalable long term**: the author is able to evolve the API to cause the least amount of friction to users.
*   **Consistent across other components**: developers can use existing knowledge and patterns to work with new components that are created by different authors.
*   **Guide developers towards the happy path**: components will encourage the right practices and usages and disallow the incorrect usage where possible.

## Before you create a component

When creating a new component:

*   Make sure there’s a single problem the component solves. Split components into subcomponents and building blocks until each solves a single problem users have.
*   Make sure you need a component and it brings value that justifies the long term support and evolution of its APIs. Often developers might find that it is easier for users to write the component code themselves so they can adjust later.

### Component’s purpose

Consider the value a new component adds and the problem it solves. Each component should solve only **one** problem, and each problem should be solved in **one** place. If your component solves more than one problem look for opportunities to split it into layers or subcomponents. With the benefit of smaller, concise and use case targeted API comes the easy of use and clear understanding of the component contract.

Lower level building blocks and components usually add certain new single functionality and are easy to combine together. Higher level components serve a purpose of combining building blocks to provide an opinionated, ready to use behavior.

**DON’T**
```
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
```
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

### Component layering

When creating components, provide various layers of single purpose building blocks first that are needed for the component to work. Increase level of opinion ingrained and reduce the amount of customisations as you go from low level APIs to higher level. Higher level components should provide more opinionated defaults and fewer customisation options.

`@Composable` component creation was designed to be a low-effort operation in Compose so that users can create their own single purpose components and adjust them as needed.

**Do:**
```
// single purpose building blocks component
@Composable
fun Checkbox(...) { ... }

@Composable
fun Text(...) { ... }

@Composable
fun Row(...) { ... }

// high level component that is more opinionated combination of lower level blocks
@Composable
fun CheckboxRow(...) {
    Row {
        Checkbox(...)
        Spacer(...)
        Text(...)
    }
}
```

### Do you need a component?

Question the need for creating the component in the first place. With high-level components that can be combined from building blocks, there has to be a strong reason for it to exist. Lower level components should solve a real problem that users have.

Try to create a Component from the publicly available building blocks. This provides the sense of what it feels like to be a developer who needs your component. If it looks simple, readable and doesn’t require hidden knowledge to make - this means users can do it themselves.

Consider the value your component brings to users if they choose it over doing it themselves. Consider the burden a component puts on a user who would need to learn new APIs to use them.

For example, a developer wants to create a `RadioGroup` component. In order to accommodate various requirements such as vertical and horizontal layouts, different types of data and decorations, the API might look like this:
```
@Composable
fun <T> RadioGroup(
    // `options` are a generic type
    options: List<T>,
    // horizontal or vertical
    orientation: Orientation,
    // some adjustments around content layout
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    optionContent: @Composable (T) -> Unit
) { ... }
```

While doing this, look first at how users would write it themselves using the available building blocks:
```
// Modifier.selectableGroup adds semantics of a radio-group like behavior
// accessibility services will treat it as a parent of various options
Column(Modifier.selectableGroup()) {
   options.forEach { item ->
       Row(
           modifier = Modifier.selectable(
               selected = (select.value == item),
               onClick = { select.value = item }
           ),
           verticalAlignment = Alignment.CenterVertically
       ) {
           Text(item.toString())
           RadioButton(
               selected = (select.value == item),
               onClick = { select.value = item }
           )
       }
   }
}
```

Now, developers should make a conscious decision on whether the `RadioGroup` API is worth it. In this particular example, users utilize familiar building blocks such as `Row`, `Text` and other basic tools. They also gain the flexibility to define the layouts needed or add any decorations and customisations. The case might be made to not to introduce any `RadioGroup` APIs at all.

Shipping a component is costly, involving at least the development, testing, long term support and subsequent evolution of the API.

### Component or Modifier

Make a component if it has a distinct UI that cannot be applied to other components or if the component wants to make structural changes in the UI (add/remove other components).

Make the feature to be a Modifier instead if the bit of functionality can be applied to any arbitrary **single** component to add extra behavior. This is especially important when the functionality has undefined behavior when applied to a few UI components at the same time.

**DON’T**
```
@Composable
fun Padding(allSides: Dp) {
    // impl
}

// usage
Padding(12.dp) {
    // 1. Is it a padding around both card and picture or for each one?
    // 2. What are the layout expectations for card and picture?
    // 3. What if there is no content (no card and picture at all)?
    UserCard()
    UserPicture()
}
```

**Do:**
```
fun Modifier.padding(allSides: Dp): Modifier = // implementation

// usage
UserCard(modifier = Modifier.padding(12.dp))
```

If the bit of functionality can be applied to any composable, but it has to alter the hierarchy of composables, it has to be a Component, since Modifiers cannot change the hierarchy:

**Do**
```
@Composable
fun AnimatedVisibility(
    visibile: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // impl
}

// usage: AnimatedVisibility has to have power to remove/add UserCard
// to hierarchy depending on the visibility flag
AnimatedVisibility(visible = false) {
    UserCard()
}
```

## Name of a Component

Please, refer to the corresponding [Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md#naming-unit-composable-functions-as-entities) section for naming conventions. However, there are more detailed considerations to keep in mind.

**Jetpack Compose framework development** MUST follow the rules in this section.

**Library development** MUST follow the section below.

**App development** MAY follow the rules below.

### BasicComponent vs Component

Consider `Basic*` prefix for components that provide barebones functionality with no decoration and/or with no design-system based visual opinions. This is a signal that users are expected to wrap it in their own decoration, as the component is not expected to be used as-is. As a counterpart to that, `Component` name without a prefix can represent components that are ready to use and are decorated according to some design specification.

**Do:**
```
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

### Design, Usecase or Company/Project specific prefixes

Avoid `CompanyName` (`GoogleButton`) or Module (`WearButton`) prefixes where possible and consider use-case or domain specific names if needed to. If the component you are building is a part of component library built using ``compose-foundation`` or ``compose-ui`` building blocks as a basis, the majority of the non-prefixed names should be available to developers without clashes: `com.companyname.ui.Button` or `com.companyname.ui.Icon`. Simple names make sure these components feel first-class when used.

If wrapping existing components or building on top of another design system, consider names that are derived from the use case first: `ScalingLazyColumn`, `CurvedText`. If impossible or the use case clashes with the existing component, module/library prefix can be used e.g. `GlideImage.`

If your design system specification introduces a number of similar components with different appearances, consider using specification prefixes: `ContainedButton`, `OutlinedButton`, `SuggestionChip`, etc. Using prefixes helps you avoid “style” patterns and keep the API simple. See "[ComponentColor/ComponentElevation](#componentcolorcomponentelevation-objects)" section for more details.

If you have a set of components with prefixes, consider choosing the default component, which is the one most likely to be used, and keep it without the prefix.

**Do**
```
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
```
// package com.company.project
// depends on foundation, DOES NOT depend on material or material3

@Composable
fun Button(...) {} // simple name that feel like a first-class button

@Composable
fun TextField(...) {} // simple name that feel like a first-class TF

```

## Component dependencies

**Jetpack Compose framework development** MUST follow the rules in this section.

**Library development** SHOULD follow the section below.

**App development** MAY follow the rules below.

### Prefer multiple components over style classes

Express dependencies in a granular, semantically meaningful way. Avoid grab-bag style parameters and classes, akin to `ComponentStyle` or `ComponentConfiguration`.

When a certain subset of components of the same type need to have the same configurations or stylistical visual appearance, users should be encouraged to create their own semantically meaningful version of a component. This can be done either by wrapping the component or forking it and using lower-level building blocks. This is the component developer’s responsibility to make sure that both of those ways are low cost operations.

Instead of relying on the `ComponentStyle` to specify different component variations in the component library, consider providing separate `@Composable` functions named differently to signify the difference in styling and use cases for those components.

**DON’T**

```
// library code
class ButtonStyles(
    /* grab bag of different parameters like colors, paddings, borders */
    background: Color,
    border: BorderStroke,
    textColor: Color,
    shape: Shape,
    contentPadding: PaddingValues
)

val PrimaryButtonStyle = ButtonStyle(...)
val SecondaryButtonStyle = ButtonStyle(...)
val AdditionalButtonStyle = ButtonStyle(...)

@Composable
fun Button(
    onClick: () -> Unit,
    style: ButtonStyle = SecondaryButtonStyle
) {
    // impl
}

// usage
val myLoginStyle = ButtonStyle(...)
Button(style = myLoginStyle)
```

**Do:**
```
// library code
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    background: Color,
    border: BorderStroke,
    // other relevant parameters
) {
    // impl
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    background: Color,
    border: BorderStroke,
    // other relevant parameters
) {
    // impl
}

// usage 1:
PrimaryButton(onClick = { loginViewModel.login() }, border = NoBorder)
// usage 2:
@Composable
fun MyLoginButton(
    onClick: () -> Unit
) {
    // delegate to and wrap other components or its building blocks
    SecondaryButton(
        onClick,
        background = MyLoginGreen,
        border = LoginStroke
    )
}
```

### Explicit vs implicit dependencies

Prefer explicit inputs and configuration options in your components, such as function parameters. Explicit inputs for the component make it easy to predict the component's behavior, adjust it, test and use.

Avoid implicit inputs provided via `CompositionLocal` or other similar mechanisms. Those inputs add complexity to the components and every usage of it and make it hard to track where customisation comes from for users. To avoid implicit dependencies, make it easy for users to create their own opinionated components with a subset of explicit inputs they wish to customize.

**DON’T**
```
// avoid composition locals for component specific customisations
// they are implicit. Components become difficult to change, test, use.
val LocalButtonBorder = compositionLocalOf<BorderStroke>(...)

@Composable
fun Button(
    onClick: () -> Unit,
) {
    val border = LocalButtonBorder.current
}

```

**Do:**
```
@Composable
fun Button(
    onClick: () -> Unit,
    // explicitly asking for explicit parameter that might have
    // reasonable default value
    border: BorderStroke = ButtonDefaults.borderStroke,
) {
    // impl
}
```

Consider using `CompositionLocal` to provide a global app or screen styling if needed. For example, design theming or typography in the material library can be implicitly specified for the whole app or screen. When doing so, make sure that those CompositionLocals are being read in the default expressions on the component parameters, so users can override them.

Since those objects rarely change and cover big subtrees of components of different kinds, the flexibility of app-wide customisation is usually worth the aforementioned downsides of the implicit inputs. In cases like this, components should be discouraged to read this `CompositionLocal` in implementation and instead read it in the default expressions, so it is easy to override when customizing or wrapping the component.

**DON’T**
```
// this is ok: theme is app global, but...
class Theme(val mainAppColor: Color)
val LocalAppTheme = compositionLocalOf { Theme(Color.Green) }

@Composable
fun Button(
    onClick: () -> Unit,
) {
    // reading theme in implementation makes it impossible to opt out
    val buttonColor = LocalAppTheme.current.mainAppColor
    Box(modifier = Modifier.background(buttonColor)) { ... }
}

```

**Do:**
```
// this is ok: theme is app global
class Theme(val mainAppColor: Color)
val LocalAppTheme = compositionLocalOf { Theme(Color.Green) }

@Composable
fun Button(
    onClick: () -> Unit,
    // easy to see where the values comes from and change it
    backgroundColor: Color = LocalAppTheme.current.mainAppColor
) {
    Box(modifier = Modifier.background(backgroundColor)) { ... }
}
```

_There’s a [blogpost](https://medium.com/androiddevelopers/pushing-the-right-buttons-in-jetpack-compose-124cb4b17197) published that describes the reasoning in depth in the chapter “Maintaining API consistency”._

## Component parameters

Set of considerations regarding parameters of `@Composable` component.

**Jetpack Compose framework development** MUST follow the rules in this section below.

**Compose library development** SHOULD follow the rules in the sections below.

**App development** SHOULD follow.

### Parameters vs. Modifier on the component

Do not introduce optional parameters that add optional behavior that could otherwise be added via Modifier. Parameters should allow to set or customize the behavior that exists internally in the component.

**DON’T:**
```
@Composable
fun Image(
    bitmap: ImageBitmap,
    // not core functionality, click can be added via Modifier.clickable
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    // can be specified via `Modifier.clip(CircleShape)`
    clipToCircle: Boolean = false
)
```

**Do:**
```
@Composable
fun Button(
    onClick: () -> Unit,
    // modifier param specified so that width, padding etc can be added
    modifier: Modifier = Modifier,
    // button is a colored rect that clicks, so background
    // considered as a core functionality, OK as a param
    backgroundColor: Color = MaterialTheme.colors.primary
)
```

### `modifier` parameter

Every component that emits UI should have a modifier parameter. Make sure that modifier parameter:

* Has the type `Modifier`.
    *   Type Modifier ensures that any Modifier can be passed to the component.
*   Is the first optional parameter.
    *   If a component has non-zero default size - modifier should be optional, since the component is self sufficient. For components with zero default size modifier parameter can be a required param.
    *   Since the modifier is recommended for any component and is used often, placing it first ensures that it can be set without a named parameter and provides a consistent place for this parameter in any component.
*   Has a no-op default value `Modifier`.
    *   No-op default value ensures that no functionality will be lost when users provide their own modifiers for the component.
*   Is the only parameter of type Modifier in the parameter list.
    *   Since modifiers intend to modify the external behaviors and appearance of the component, one modifier parameter should be sufficient. Consider asking for specific parameters or reconsidering the layering of the component (e.g. braking component into two) instead.
*   Is applied once as a first modifier in the chain to the root-most layout in the component implementation.
    *   Since modifiers intend to modify the external behaviors and appearance of the component, they must be applied to the outer-most layout and be the first modifiers in the chain. It is ok to chain other modifiers to the modifier that is passed as a parameter.

**Why?** Modifiers are the essential part of compose, users have expectations about their behavior and API. Essentially, modifiers provide a way to modify the external component behavior and appearance, while component implementation will be responsible for the internal behavior and appearance.

**DON’T:**
```
@Composable
fun Icon(
    bitmap: ImageBitmap,
    // no modifier parameter
    tint: Color = Color.Black
)
```

**DON’T:**
```
@Composable
fun Icon(
    bitmap: ImageBitmap,
    tint: Color = Color.Black,
    // 1: modifier is not the first optional parameter
    // 2: padding will be lost as soon as the user sets its own modifier
    modifier: Modifier = Modifier.padding(8.dp)
)
```

**DON’T:**
```
@Composable
fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    // DON'T - modifier is intended to specify the external behavior of
    // the CheckboxRow itself, not its subparts. Make them slots instead
    rowModifier: Modifier = Modifier,
    checkboxModifier: Modifier = Modifier
)
```

**DON’T:**
```
@Composable
fun IconButton(
    buttonBitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    tint: Color = Color.Black
) {
    Box(Modifier.padding(16.dp)) {
        Icon(
            buttonBitmap,
            // modifier should be applied to the outer-most layout
            // and be the first one in the chain
            modifier = Modifier.aspectRatio(1f).then(modifier),
            tint = tint
        )
    }
}
```

**Do:**
```
@Composable
fun IconButton(
    buttonBitmap: ImageBitmap,
    // good: first optional parameter, single of its kind
    modifier: Modifier = Modifier,
    tint: Color = Color.Black
) {
    // good: applied before other modifiers to the outer layout
    Box(modifier.padding(16.dp)) {
        Icon(buttonBitmap, modifier = Modifier.aspectRatio(1f), tint = tint)
    }
}
```

**Also Do:**
```
@Composable
fun ColoredCanvas(
    // ok: canvas has no intrinsic size, asking for size modifiers
    modifier: Modifier,
    color: Color = Color.White,
    ...
) {
    // good: applied before other modifiers to the outer layout
    Box(modifier.background(color)) {
        ...
    }
}
```

### Parameters order

The order of parameters in a component must be as follows:

1. Required parameters.
2. Single `modifier: Modifier = Modifier`.
3. Optional parameters.
4. (optional) trailing `@Composable` lambda.

**Why?** Required parameters indicate the contract of the component, since they have to be passed and are necessary for the component to work properly. By placing required parameters first, API clearly indicates the requirements and contract of the said component. Optional parameters represent some customisation and additional capabilities of the component, and don’t require immediate attention of the user.

Explanation for the order of the parameters:

1. Required parameters. Parameters that don’t have default values and the user is required to pass the values for those parameters in order to use the components. Coming first, they allow users to set them without using named parameters.
2. `modifier: Modifier = Modifier`. Modifiers should come as a first optional parameter in a @composable function. It must be named `modifier` and have a default value of `Modifier`. There should be only one modifier parameter and it should be applied to the root-most layout in the implementation. See "[modifier parameter](#modifier-parameter)" section for more information.
3. Optional parameters. Parameters that have default values that will be used if not overridden by the user of the component. Coming after required parameters and a `modifier` parameter, they do not require the user to make an immediate choice and allow one-by-one override using named parameters.
4. (optional) trailing `@Composable` lambda representing the main content of the component, usually named `content`. It can have a default value. Having non-@composable trailing lambda (e.g. `onClick`) might be misleading as it is a user expectation to have a trailing lambda in a component to be `@Composable`. For `LazyColumn` and other DSL-like exceptions, it is ok to have non-@composable lambda since it still represents the main content.

Think about the order of parameters inside the “required” and “optional” subgroups as well. Similar to the split between required and optional parameters, it is beneficial for the reader and user of the API to see the data, or “what” part of the component first, while metadata, customisation, the “how” of the component should come after.

It makes sense to group parameters semantically within the required or optional groups. If you have a number of color parameters (`backgroundColor` and `contentColor`), consider placing them next to each other to make it easy for the user to see customisation options.

**Do**
```
@Composable
fun Icon(
    // image bitmap and contentDescription are required
    // bitmap goes first since it is the required data for the icon
    bitmap: ImageBitmap,
    // contentDescription follows as required, but it is a "metadata", so
    // it goes after the "data" above.
    contentDescription: String?,
    // modifier is the first optional parameter
    modifier: Modifier = Modifier,
    // tint is optional, default value uses theme-like composition locals
    // so it's clear where it's coming from and to change it
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
)
```

**Do**
```
@Composable
fun LazyColumn(
    // no required parameters beyond content, modifier is the first optional
    modifier: Modifier = Modifier,
    // state is important and is a "data": second optional parameter
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    // arrangement and alignment go one-by-one since they are related
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    // trailing lambda with content
    content: LazyListScope.() -> Unit
)
```

### Nullable parameter

Make conscious choices between the semantical meaning of the parameter or its absence. There’s a difference between default value, empty value and absent value. A conscious choice has to be made when choosing the right semantic for the API.

*   Nullability of the parameters should be introduced as a signal to allow users to express “absence” of the parameter and corresponding component’s capabilities.
*   Avoid making parameters nullable to utilize `null` as a “use default in the implementation” signal.
*   Avoid making parameter nullable to signal that the value exists, but is empty, prefer a meaningful empty default value.

**DON’T**
```
@Composable
fun IconCard(
    bitmap: ImageBitmap,
    // avoid having null as a signal to gather default
    elevation: Dp? = null
) {
    // instead of implementation based default resolution, provide meaningful default
    val resolvedElevation = elevation ?: DefaultElevation
}
```

**Do:**
```
@Composable
fun IconCard(
    bitmap: ImageBitmap,
    elevation: Dp = 8.dp
) { ... }
```


**Or Do (null is meaningful here):**
```
@Composable
fun IconCard(
    bitmap: ImageBitmap,
    // null description is NOT the same as "" description
    // since when it is null - we don't add any accessibility info.
    contentDescription: String?
) { ... }
```

### Default expressions

Developers should make sure that default expressions on optional parameters are publicly available and meaningful. Best practices:

*   Default expression does not contain private/internal calls. This allows users that wrap/extend components to provide the same default. Alternatively, this default can be used by the users in the if statement: `if (condition) default else myUserValue`.
*   Default should have meaningful value, it should be clear what the default value is. Avoid using `null` as a marker to use the default value internally. Refer to null as the “absence” of the value (per "[nullable parameter](#nullable-parameter)" section). Absence of the value (null) is a valid default in this case.
*   Use `ComponentDefaults` objects to name-space defaults values if you have a number of them.

**DON’T**
```
@Composable
fun IconCard(
    bitmap: ImageBitmap,
    //backgroundColor has meaningful default, but it is inaccessible to users
    backgroundColor: Color = DefaultBackgroundColor,
    // avoid having null as a signal to gather default
    elevation: Dp? = null
) {
    // instead of implementation based default resolution, provide meaningful default
    val resolvedElevation = elevation ?: DefaultElevation
}

// this default expression is private.
// Users unable to access it when wrapping your component.
private val DefaultBackgroundColor = Color.Red
private val DefaultElevation = 8.dp
```

**Do:**
```
@Composable
fun IconCard(
    bitmap: ImageBitmap,
    //all params have meaningful defaults that are accessible
    backgroundColor: Color = IconCardDefaults.BackgroundColor,
    elevation: Dp = IconCardDefaults.Elevation
) { ... }

// defaults namespaced in the ComponentNameDefaults object and public
object IconCardDefaults {
    val BackgroundColor = Color.Red
    val Elevation = 8.dp
}
```

**Note:** If your component has a limited number of parameters that have short and predictable defaults (``elevation = 0.dp``), `ComponentDefaults` object might be omitted in favor of simple inline constants.

### MutableState\<T\> as a parameter

Parameters of type `MutableState<T>` are discouraged since it promotes joint ownership over a state between a component and its user. If possible, consider making the component stateless and concede the state change to the caller. If mutation of the parent’s owned property is required in the component, consider creating a `ComponentState` class with the domain specific meaningful field that is backed by `mutableStateOf()`.

When a component accepts `MutableState` as a parameter, it gains the ability to change it. This results in the split ownership of the state, and the usage side that owns the state now has no control over how and when it will be changed from within the component’s implementation.

**DON’T**
```
@Composable
fun Scroller(
    offset: MutableState<Float>
) {}
```

**Do (stateless version, if possible):**
```
@Composable
fun Scroller(
    offset: Float,
    onOffsetChange: (Float) -> Unit,
) {}
```

**Or do (state-based component version, if stateless not possible):**
```
class ScrollerState {
    val offset: Float by mutableStateOf(0f)
}

@Composable
fun Scroller(
    state: ScrollerState
) {}
```

### State\<T\> as a parameter

Parameters of type `State<T> `are discouraged since it unnecessarily narrows the type of objects that can be passed in the function. Given `param: State<Float>`, there are two better alternatives available, depending on the use case:

1. `param: Float`. If the parameter doesn’t change often, or is being read immediately in the component (composition), developers can provide just a plain parameter and recompose the component when it changes.
2. `param: () -> Float`. To delay reading the value until a later time via `param.invoke()`, lambda might be provided as a parameter. This allows the developers of the component to read the value only when/if it is needed and avoid unnecessary work. For example, if the value is only read during drawing operation, [only redraw will occur](https://developer.android.com/jetpack/compose/phases#state-reads). This leaves the flexibility to the user to provide any expression, including the `State<T>`’s read:
    1. `param = { myState.value }` - read the `State<T>`’s value
    2. `param = { justValueWithoutState }` - plain value not backed by the `State<T>`
    3. `param = { myObject.offset }` - user can have a custom state object where the field (e.g. ``offset``) is backed by the `mutableStateOf()`

**DON’T**
```
fun Badge(position: State<Dp>) {}

// not possible since only State<T> is allowed
Badge(position = scrollState.offset) // DOES NOT COMPILE
```

**Do:**
```
fun Badge(position: () -> Dp) {}

// works ok
Badge(position = { scrollState.offset })
```

### Slot parameters

#### What are slots

Slot is a `@Composable` lambda parameter that specifies a certain sub hierarchy of the component. Content slot in a Button might look like this:
```
@Composable
fun Button(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {}

// usage
Button(onClick = { /* handle the click */}) {
    Icon(...)
}
```

This pattern allows the button to have no opinion on the content, while playing the role of drawing the necessary decoration around, handling clicks and showing ripples.

#### Why slots

It might be tempting to write the button as follows:

**DON’T**
```
@Composable
fun Button(
    onClick: () -> Unit,
    text: String? = null,
    icon: ImageBitmap? = null
) {}
```

Where either text or icon or both are present, leaving the button to arrange the show. While it handles basic use cases or sample usages well, it has some fundamental flexibility flaws:

*   **Restricts styling choice:** by using only `String`, Button disallows users to use   `AnnotatedString` or other sources of text information, if required. To provide some styling, Button will have to accept `TextStyle` parameters as well, plus some other ones. This will bloat the API of the button quickly.
*   **Restricts component choice:** While Button might want to show a text, `String` might not be enough. If a user has their own `MyTextWithLogging()` component, they might want to use it in a button to do some additional logic like logging events and such. This is impossible with the String API unless the user forks the Button.
*   **Overloads explosion:** If we want some flexibility, for example accepting both ImageBitmap and VectorPainter as icons, we have to provide an overload for that. We can multiply it for every such parameter (`text` being `String` or `AnnotatedString` or `CharSequence`), resulting in the big number of overloads we have to provide in order to cater the users’ use cases.
*   **Restricts component layout capabilities:** In the example above, the Button is opinionated about the arrangement between text and icon. If a user has a special icon that they want to put with a custom arrangement (e.g. on a button’s text baseline or with 4dp additional padding) - they won’t be able to do it.

Slot APIs in components are free from these problems, as a user can pass any component with any styling in a slot. Slots come with the price of simple usages being a bit more verbose, but this downside disappears quickly as soon as a real-application usage begins.

**Do**
```
@Composable
fun Button(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit
) {}
```

#### Single “content” slot overloads

For components that are responsible for layouting of multiple slot APIs it accepts, consider providing an overload with a single slot, usually named `content`. This allows for more flexibility on the usage side when needed as it is possible to change the slot layout logic.

**Do**
```
@Composable
fun Button(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {}

// usage
Button(onClick = { /* handle the click */}) {
    Row {
        Icon(...)
        Text(...)
   }
}
```

#### Layout strategy scope for slot APIs

If applicable, consider choosing an appropriate layout strategy for the slot lambda. This is especially important for single `content` overloads. In the example above, developers of the Button might notice that most common usage patterns include: single text, single icon, icon and text in a row, text then icon in a row. It might make sense to provide `RowScope` in a content slot, making it easier for the user to use the button

**Do**
```
@Composable
fun Button(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {}

// usage
Button(onClick = { /* handle the click */ }) { // this: RowScope
    Icon(...)
    Text(...)
}
```

`ColumnScope` or `BoxScope` are good candidates for other types of layout strategies for components. The author of the component SHOULD always think about what will happen if multiple components are passed in a slot and consider communicating this behaviour to a user via scopes (`RowScope` in a Button example above).

#### Lifecycle expectations for slot parameters

Developers should ensure that the lifecycle of the visible and composed slot parameter composables is either the same as the composable that accepts that slot, or is tied to visibility of the slot in the viewport.

`@Composable` components that are passed in the slot should not be disposed of and composed again on the structural or visual changes in the parent component.

If in need to make structural changes internally that affect slot composables lifecycle, use `remember{}` and `movableContentOf()`

**DON’T**
```
@Composable
fun PreferenceItem(
    checked: Boolean,
    content: @Composable () -> Unit
) {
    // don't: this logic will dispose and compose again from scratch the content() composable on the `checked` boolean change
    if (checked) {
        Row {
            Text("Checked")
            content()
        }
    } else {
        Column {
            Text("Unchecked")
            content()
        }
    }
}
```

**Do**
```
@Composable
fun PreferenceItem(
    checked: Boolean,
    content: @Composable () -> Unit
) {
    Layout({
        Text("Preference item")
        content()
    }) {
        // custom layout that relayouts the same instance of `content`
        // when `checked` changes
    }
}
```

**Or Do**
```
@Composable
fun PreferenceItem(
    checked: Boolean,
    content: @Composable () -> Unit
) {
    // this call preserves the lifecycle of `content` between row and column
    val movableContent = remember(content) { movableContentOf(content)}
    if (checked) {
        Row {
            Text("Checked")
            movableContent()
        }
    } else {
        Column {
            Text("Unchecked")
            movableContent()
        }
    }
}
```

It is expected that slots that become absent from the UI or leave the view port will be  disposed of and composed again when they become visible:

**Do:**
```
@Composable
fun PreferenceRow(
    checkedContent: @Composable () -> Unit,
    checked: Boolean
) {
    // since checkedContent() is only visible in the checked state
    // it is ok for this slot to be disposed when not present
    // and be composed again when present again
    if (checked) {
        Row {
            Text("Checked")
            checkedContent()
        }
    } else {
        Column {
            Text("Unchecked")
        }
    }
}
```

#### DSL based slots

Avoid DSL based slots and APIs where possible and prefer simple slot `@Composable` lambdas. While giving the developers control over what the user might place in the particular slot, DSL API still restricts the choice of component and layout capabilities. Moreover, the DSL introduces the new API overhead for users to learn and for developers to support.

**DON’T**
```
@Composable
fun TabRow(
    tabs: TabRowScope.() -> Unit
) {}

interface TabRowScope {
    // can be a string
    fun tab(string: String)
    // Can be a @composable as well
    fun tab(tabContent: @Composable () -> Unit)
}
```

Instead of DSL, consider relying on plain slots with parameters. This allows the users to operate with tools they already know while not sacrificing any flexibility.

**Do instead:**
```
@Composable
fun TabRow(
    tabs: @Composable () -> Unit
) {}

@Composable
fun Tab(...) {}

// usage
TabRow {
    tabsData.forEach { data ->
        Tab(...)
    }
}
```

DSL for defining content of the component or its children should be perceived as an exception. There are some cases that benefit from the DSL approach, notably when the component wants to lazily show and compose only the subset of children (e.g. `LazyRow`, `LazyColumn`).

**Allowed, since laziness and flexibility with different data types is needed:**
```
@Composable
fun LazyColumn(
    content: LazyListScope.() -> Unit
) {}

// usage: DSL is fine since it allows Lazycolumn to lazily compose the subset of children
LazyColumn {
    // allow to define different types of children and treat them differently
    // since sticky header can act both like an item and a sticky header
    stickyHeader {
        Text("Header")
    }
    items(...) {
        Text($index)
    }
}
```

Even in such cases like with `LazyColumn` it is possible to define the API structure without DSL, so simple version should be considered first

**Do. Simpler, easier to learn and use API that still provides laziness of children composition:**
```
@Composable
fun HorizontalPager(
    // pager still lazily composes pages when needed
    // but the api is simpler and easier to use; no need for DSL
    pageContent: @Composable (pageIndex: Int) -> Unit
) {}
```

## Component-related classes and functions

**Jetpack Compose framework development** MUST follow the rules in this section.

**Library development** SHOULD follow the section below.

**App development** MAY follow the rules below.

### State

For core design practices with state, visit [corresponding section in compose api guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md#compose-api-design-patterns).

### ComponentDefault object

All component default expressions should either be inline or live in the top level object called `ComponentDefaults`, where `Component` is a real component name. Refer to the “[Default expressions](#default-expressions)” section for details.

### ComponentColor/ComponentElevation objects

Consider a simple if-else expression in the default statements for a simple branching logic, or a dedicated `ComponentColor`/`ComponentElevation` class that clearly defines the inputs that a particular Color/Elevation can be reflected against.

There’s a number of ways to provide and/or allow customisation of a certain single type of parameters (e.g. colors, dp) depending on the state of the component (e.g. enabled/disabled, focused/hovered/pressed).

**Do (if color choosing logic is simple)**
```
@Composable
fun Button(
    onClick: () -> Unit,
    enabled: Boolean = true,
    backgroundColor =
        if (enabled) ButtonDefaults.enabledBackgroundColor
        else ButtonDefaults.disabledBackgroundColor,
    elevation =
        if (enabled) ButtonDefaults.enabledElevation
        else ButtonDefaults.disabledElevation,
    content: @Composable RowScope.() -> Unit
) {}
```

While this works well, those expressions can grow pretty quickly and pollute the API space. That’s why it might be sensible to isolate this to a domain and parameter specific class.

**Do (if color conditional logic is more complicated)**
```
class ButtonColors(
    backgroundColor: Color,
    disabledBackgroundColor: Color,
    contentColor: Color,
    disabledContentColor: Color
) {
    fun backgroundColor(enabled: Boolean): Color { ... }

    fun contentColor(enabled: Boolean): Color { ... }
}

object ButtonDefaults {
    // default factory for the class
    // can be @Composable to access the theme composition locals
    fun colors(
        backgroundColor: Color = ...,
        disabledBackgroundColor: Color = ...,
        contentColor: Color = ...,
        disabledContentColor: Color = ...
    ): ButtonColors { ... }
}

@Composable
fun Button(
    onClick: () -> Unit,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.colors(),
    content: @Composable RowScope.() -> Unit
) {
    val resolvedBackgroundColor = colors.backgroundColor(enabled)
}
```

This way, while not introducing the overhead and complexities of the “styles” pattern, we isolate the configuration of a specific part of the component. Additionally, unlike plain default expression, `ComponentColors` or `ComponentElevation` classes allow for more granular control, where the user can specify the enabled and disabled colors/elevation separately.

**Note:** This approach is different from styles that are discouraged in compose "[no styles](#prefer-multiple-components-over-style-classes)" chapter for rationale. `ComponentColor` and other such classes target a certain type of functionality of the component, allowing for definition of the color against explicit inputs. The instances of this class must be passed as an explicit parameter for the component.

**Note:** While `ComponentColors` and `ComponentElevation` are the most common patterns, there are other component parameters that can be isolated in the similar fashion.

## Documentation for the component

**Jetpack Compose framework development** SHOULD follow the rules in this section below.

**Compose library development** MAY follow the rules in the sections below.

**App development** MAY follow.

Documentation for `@Composable` components should follow JetBrains’s [ktdoc guidelines and syntax](https://kotlinlang.org/docs/kotlin-doc.html#kdoc-syntax). Additionally, documentation must communicate a component's capabilities to developers via multiple channels: description of the component purpose, parameters and expectations about those parameters, usage examples.

### Documentation structure and ordering

Every component should have following documentation structure:

1. One-liner paragraph summarizing the component and what it does.
2. Paragraphs going more into the detail of components, outlining the capabilities, behavior and might include one or more of:
    * `@sample` tag providing an example of the usage for this components and its states, default, etc. If you don't have access to `@sample` functionality, consider inline examples in the ktdoc.
    * `@see` tags pointing to other related apis.
    * Links to design or other materials to help to use the components to its full potential.
3. Description for each parameter of the component, starting with `@param paramname`.
    * Developers might decide to optionally omit the documentation for the trailing `@Composable` `content` lambda as it is always implied to be the main content slot for the component.

### Documentation example

**Do**
```
/**
* Material Design badge box.
*
* A badge represents dynamic information such as a number of pending requests in a navigation bar. Badges can be icon only or contain short text.
*
* ![Badge image](https://developer.android.com/images/reference/androidx/compose/material3/badge.png)
*
* A common use case is to display a badge with navigation bar items.
* For more information, see [Navigation Bar](https://m3.material.io/components/navigation-bar/overview)
*
* A simple icon with badge example looks like:
* @sample androidx.compose.material3.samples.NavigationBarItemWithBadge
*
* @param badge the badge to be displayed - typically a [Badge]
* @param modifier the [Modifier] to be applied to this BadgedBox
* @param content the anchor to which this badge will be positioned
*/
@ExperimentalMaterial3Api
@Composable
fun BadgedBox(
    badge: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
)
```

## Accessibility of the component

Consider using foundation building blocks like `Modifier.clickable` or `Image` for better accessibility. Those building blocks will provide good defaults when possible, or will explicitly ask for needed information. Accessibility needs to be manually handled when using ui-level blocks, such as `Layout` or `Modifier.pointerInput`. This section contains best practices regarding accessible API design and accessibility implementation tuning.

### Semantics merging

Jetpack Compose uses semantics merging for accessibility purposes. This way, `Button` with the content slot doesn’t have to set the text for accessibility service to announce. Instead, the content’s semantics (`Icon`’s contentDescription or `Text`’s text) will be merged into the button. Refer to the [official documentation](https://developer.android.com/jetpack/compose/semantics#merged-vs-unmerged) for more info.

To manually create a node that will merge all of its children, you can set a `Modifier.semantics(mergeDescendants = true)` modifier to your component. This will force all non-merging children to collect and pass the data to your component, so it will be treated as a single entity. Some foundation-layer modifiers merge descendants by default (example: `Modifier.clickable` or `Modifier.toggleable`).

### Accessibility related parameters

For especially common accessibility needs, developers might want to accept some accessibility-related parameters to let users help to provide better accessibility. This is especially true for leaf components like `Image` or `Icon`. `Image` has a required parameter `contentDescription` to signal to the user the need to pass the necessary description for an image. When developing components, developers need to make a conscious decision on what to build in in the implementation vs what to ask from the user via parameters.

Note that if you follow the normal best practice of providing an ordinary Modifier parameter and put it on your root layout element, this on its own provides a large amount of implicit accessibility customizability.  Because the user of your component can provide their own `Modifier.semantics` which will apply to your component.  In addition, this also provides a way for developers to override a portion of your component’s default semantics: if there are two `SemanticsProperties` with identical keys on one modifier chain, Compose resolves the conflict by having the first one win and the later ones ignored.

Therefore, you don’t need to add a parameter for every possible semantics your component might need.  You should reserve them for especially common cases where it would be inconvenient to write out the `semantics` block every time, or use cases where for some reason the Modifier mechanism doesn’t work (for example, you need to add semantics to an inner child of your component).

### Accessibility tuning

While basic accessibility capabilities will be granted by using foundation layer building blocks, there’s a potential for developers to make the component more accessible.

There are specific semantics expected for individual categories of components: simple components typically require 1-3 semantics, whereas more complex components like text fields, scroll containers or time/date pickers require a very rich set of semantics to function correctly with screenreaders.  When developing a new custom component, first consider which of the existing standard Compose components it’s most similar to, and imitating the semantics provided by that component’s implementation, and the exact foundation building blocks it uses. Go from there to fine-tune and add more semantical actions and/or properties when needed.

## Tooling support

**Jetpack Compose framework development** SHOULD follow the rules in this
section below.

**Compose library development** MAY follow the rules in the sections below.

**App development** MAY follow.

Consider component behaviour in app developer tooling including Android Studio
Previews and test infrastructure. Components are expected to behave correctly in
those environments to make the developer experience productive.

### Compose Preview tooling

Components are expected to display initial state when used in non-interactive
preview mode.

Components should avoid patterns that delay the initial render to a subsequent
frame. Avoid using LaunchedEffects or asynchronous logic for initial component
state set up.

If required use `LocalInspectionMode.current` to detect when running as a
preview, and do the minimal change to ensure Previews are functional. Avoid
replacing a complex component with some placeholder image in Previews. Ensure
your component works correctly with various parameters provided via the preview
tooling.

In interactive mode, Previews should allow direct use of the component with the
same interactive experience as when running in an application.

### Screenshot testing

Components should support screenshot testing.

Prefer stateless components where state is passed as a parameter to make sure
the component is screenshot-testable in various states. Alternatively, support
use of
[Compose testing APIs](https://developer.android.com/develop/ui/compose/testing/apis)
such as SemanticsMatcher to affect the internal state.

Android specific components should ideally support both
[Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing)
and Robolectric
([RNG](https://github.com/robolectric/robolectric/releases/tag/robolectric-4.10))
to enable effective screenshot testing.

## Evolution of the Component APIs

**Jetpack Compose framework development** MUST follow the rules in this section below.

**Compose library development** MUST follow the rules in the sections below.

**App development** MAY follow.

Refer to the [kotlin backwards compatibility](https://kotlinlang.org/docs/jvm-api-guidelines-backward-compatibility.html) guidelines for additional information.

Since every compose is a function, the following rules apply to the component API changes:

*   Parameters of the functions MUST NOT be removed.
*   Newly added parameter to existing functions MUST have default expressions.
*   New parameters MAY be added as a last parameter, or second to last in cases of trailing lambdas.
    *   The developer might decide to put the new parameter closer to other parameters that are semantically close to a new one. Keep in mind that this might break source compatibility if the user uses the component without named parameters.

The workflow to add a new parameter to a component:

1. Create a new overload with the new parameter containing the default.
2. Deprecate the existing function with `DeprecationLevel.Hidden` for binary compatibility.
3. Make the deprecated version to call your new one.

**Do:**
```
// existing API we want to extend
@Deprecated(
    "Maintained for compatibility purposes. Use another overload",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun Badge(color: Color) {}

// new overload has to be created
@Composable
fun Badge(
    color: Color,
    // default should be provided
    secondaryColor: Color = Color.Blue
) {}
```
