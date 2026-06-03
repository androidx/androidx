# RemoteCompose JSON Parser

The `RemoteComposeJsonParser` provides a standardized way to define RemoteCompose documents using JSON. It allows for a declarative, human-readable representation of layouts, components, and dynamic drawing operations, which is then parsed into a binary document using the `RemoteComposeWriter` API.

---

## Core Principles

- **Polymorphism via `type`**: Every component, modifier, or drawing command is a JSON object with a mandatory `"type"` key (e.g., `"column"`, `"drawCircle"`, `"padding"`).
- **Flat Attributes**: Component-specific properties (like `id`, `value`, `alignment`, or `source`) are top-level keys within the object.
- **Ordered Modifiers**: Styling and transformation operations are stored in an optional `"modifiers"` array to ensure they are applied in a deterministic functional order.
- **Ordered Children/Commands**: Nested layout components are stored in a `"children"` array, while drawing commands for canvas components are stored in a `"commands"` array.
- **Deterministic Resource Ordering**: Resources like `colors`, `variables`, `paths`, and `matrices` can be defined as an array of objects (`[ { "name": "n", "value": "v" } ]`) or as a map of key-value pairs. The parser scans the `root` array for `resources` and `variable` commands and parses them *before* root component initialization, enabling bit-for-bit identity with code-generated documents.
- **Infix Expressions**: Dynamic math is represented as infix strings (e.g., `"a + b * sin(time)"`) which are parsed into the underlying expression engine. Expressions can refer to other variables using `$vars.name` or `@vars.name`.

---

## Document Header

A RemoteCompose JSON document can start with an optional `"header"` object. The header configures global attributes and parameters of the output document buffer:

```json
{
  "header": {
    "width": 400,
    "height": 800,
    "contentDescription": "Main Layout",
    "fps": 60,
    "apiLevel": 7,
    "orderedResources": true,
    "theme": 0,
    "ltResize": true,
    "densityBehavior": 1,
    "featurePaintMeasure": true
  }
}
```

### Supported Header Fields

| Field Name | Type | Description |
| :--- | :--- | :--- |
| `"width"` | Number | The document canvas width. |
| `"height"` | Number | The document canvas height. |
| `"contentDescription"` | String | Description of the document for accessibility. |
| `"fps"` / `"desiredFPS"` | Number | The desired frame-rate for rendering. |
| `"apiLevel"` | Integer | Target API level (defaults to `7` if omitted). |
| `"orderedResources"` | Boolean | If `true`, forces resources to be processed in the exact order they are defined. |
| `"theme"` | Integer | Active color theme of the document. |
| `"ltResize"` | Boolean | Enables Left-Top resizing behavior. |
| `"densityBehavior"` | Integer | Set the document's pixel density behavior. |
| `"featurePaintMeasure"`| Boolean | Enables specialized layout bounds/paint measurements. |

---

## Architecture

The JSON parser is designed with a highly decoupled, registry-driven architecture that isolates the parser core from the built-in component definitions, allowing for clean extensibility and pluggability.

### 1. Registry-Driven Extension System

- **Interfaces**: Component parsing and modifier parsing are decoupled into separate top-level interfaces:
  - `JsonComponentParser`: Interface for parsing a layout component.
  - `JsonModifierParser`: Interface for parsing layout modifiers.
- **Extensible Registry**: Custom components and layout modifiers can be dynamically registered at runtime using:
  - `RemoteComposeJsonParser.registerComponentParser(type, parser)`
  - `RemoteComposeJsonParser.registerModifierParser(key, parser)`
- **Default Registries**: Standard layouts and modifiers are defined and registered in separate registry files:
  - `DefaultComponentParsers.java`: Registers column, row, box, flow, text, spacer, canvas, bitmap, etc.
  - `DefaultModifierParsers.java`: Registers padding, background, size, weight, width/height constraints, clip, id, etc.

---

### 2. Default Layout & Component Registries

Layout components recursively descend using `parseComponent(JSONObject)` and `parseChildren(JSONArray)`.

#### Supported Layout Components

- `"column"`, `"row"`, `"box"`, `"fitBox"`, `"collapsibleColumn"`, `"collapsibleRow"`:
  Support optional layout alignment properties `horizontalAlignment` and `verticalAlignment`.
- `"flow"`:
  Supports optional alignment properties and a `"maxColumns"` integer property.
- `"spacer"`:
  An empty box component. Automatically defaults to a horizontal weight of `1.0f` if no other modifiers are specified.
- `"bitmap"`:
  Adds a bitmap using the `"id"` property.
- `"global"`:
  A layout wrapper component. When used, all nested child components and resources placed inside its `"children"` array are automatically hoisted/rewritten to the very beginning of the output compiled binary document. It is nesting-safe and merges multiple declarations seamlessly.

#### Supported Alignment Values

| Property | String Values |
| :--- | :--- |
| **`horizontalAlignment`** | `"start"` (Default), `"center"`, `"end"`, `"spaceBetween"`, `"spaceEvenly"`, `"spaceAround"` |
| **`verticalAlignment`** | `"top"` (Default), `"center"`, `"bottom"`, `"spaceBetween"`, `"spaceEvenly"`, `"spaceAround"` |

#### Text Component (`"text"`)

The text component supports advanced dynamic formatting and text overflow capabilities:

- `"value"`: The plain text string. Supports variable lookups (e.g. `"$vars.message"` / `@vars.message`).
- `"textFromFloat"`: Sub-object to dynamically format a floating-point variable as text:
  ```json
  "textFromFloat": {
    "value": "time * 1.5",
    "before": 0,
    "decimal": 2,
    "flags": 0
  }
  ```
- `"fontSize"`: Number or expression specifying the font size (defaults to `TextStyle.DEFAULT_FONT_SIZE` which is `16.0f`).
- `"color"`: The text color, specified as a hex string (`"#RRGGBB"`) or referencing a color resource (`"$colors.primary"`).
- `"maxLines"`: The maximum number of lines to display.
- `"textAlign"`: Alignment of the text. Supports `"left"`, `"right"`, `"center"`, `"justify"`, `"start"`, and `"end"`.
- `"overflow"`: Overflow rendering strategy: `"clip"` (Default), `"visible"`, `"ellipsis"`, `"start_ellipsis"`, `"middle_ellipsis"`.

---

### 3. Modifier System

Modifiers apply visual transformations, padding, and sizing restrictions to layouts and components.

#### Sizing Modifier Shorthands (Plain Strings)
For simple modifiers that don't require custom arguments, you can specify them directly as **plain strings** in the `"modifiers"` array instead of writing a single-key JSON dictionary:
- `"fillMaxWidth"` (equivalent to `{ "fillMaxWidth": "NaN" }`)
- `"fillMaxHeight"` (equivalent to `{ "fillMaxHeight": "NaN" }`)
- `"fillMaxSize"` (equivalent to `{ "fillMaxSize": "NaN" }`)

Example:
```json
"modifiers": [ "fillMaxWidth", { "padding": 16.0 } ]
```

#### Supported Modifier Keys

- `"padding"`: Adds spacing. Can be a single number (uniform padding), an array of four numbers `[start, top, end, bottom]`, or an object:
  `{ "padding": { "start": 10, "top": 5, "end": 10, "bottom": 5 } }`
- `"fillMaxWidth"`, `"fillMaxHeight"`, `"fillMaxSize"`: Sizing modifiers taking a float scale (e.g. `1.0`).
- `"width"`, `"height"`: Fixed dimension bounds taking a float value or expression.
- `"size"`: Sizing shortcut setting both width and height to a single float value.
- `"widthIn"`, `"heightIn"`: Min/Max bounding constraint arrays containing two floats: `[min, max]`.
- `"weight"` / `"horizontalWeight"`: Horizontal weight allocation inside rows or flows (Double).
- `"verticalWeight"`: Vertical weight allocation inside columns (Double).
- `"background"`: Sets the component background color using a hex color string or a color resource ID (`"$colors.name"`).
- `"verticalScroll"`, `"horizontalScroll"`: Configures layout scroll position bounds via a float value or expression.
- `"clip"`: Clips the component to a custom shape:
  `{ "clip": { "type": "roundRect", "radius": 8 } }`
  Supported shape types: `"circle"`, `"rect"`, and `"roundRect"` (supports `"radius"`, or individual `"left"`, `"top"`, `"right"`, `"bottom"` bounds).
- `"id"`: Binds a unique integer component ID.

> [!NOTE]
> Advanced modifiers like `collapsiblePriority` and interactive click handlers like `onTouchDown` are not supported in the default modifier parser and will be ignored unless custom modifiers are registered.

---

### 4. Drawing Commands & Canvas Subsystem

The `"canvas"` component renders custom shapes via its `"commands"` array.

#### Supported Drawing Primitives

- `"drawRect"`: `left`, `top`, `right`, `bottom`.
- `"drawLine"`: `x1`, `y1`, `x2`, `y2`.
- `"drawCircle"`: `cx`, `cy`, `radius`.
- `"drawOval"`: `left`, `top`, `right`, `bottom`.
- `"drawRoundRect"`: `left`, `top`, `right`, `bottom`, `rx`, `ry`.
- `"drawPath"`: Draws a predefined path specified by `"path"` ID or path name reference (`"$paths.name"`).

#### Paint Operations & Configurations

Paint attributes can be specified via individual commands or a unified `"paint"` command:

- `"setColor"`: Sets the primary paint color (`"color"`).
- `"setStyle"`: Sets drawing mode to `"fill"`, `"stroke"`, or `"fillAndStroke"`.
- `"setStrokeWidth"`: Sets the stroke line thickness.
- `"paint"`: Applies multiple paint properties simultaneously via attributes or a nested `"ops"` array:
  - `"color"`: Hex color or color reference (`"$colors.primary"`).
  - `"alpha"`: Opacity float value or expression.
  - `"width"`: Stroke width float value.
  - `"style"`: `"fill"`, `"stroke"`, or `"fillAndStroke"`.
  - `"strokeCap"`: `"butt"`, `"round"`, or `"square"`.
  - `"shader"`: Integer shader ID.
  - `"pathEffect"`: An array of floats configuring line dash effects.
  - `"linearGradient"`: Configures a linear gradient:
    ```json
    "linearGradient": {
      "x1": 0, "y1": 0,
      "x2": "width", "y2": "height",
      "colors": [ "$colors.brand", "#FF0000" ],
      "stops": [ 0.0, 1.0 ],
      "tileMode": 0
    }
    ```

#### Control Flow & Logic

- `"conditionalOperations"`: Executes commands if a comparison evaluates to true:
  - `"condition"`: `"gt"`, `"ge"`, `"lt"`, `"le"`, `"eq"`.
  - `"v1"`, `"v2"`: Floating point values or expressions to compare.
  - `"commands"`: A nested array of drawing commands to execute when true.
- `"loop"`: Repeats drawing commands in a range:
  - `"from"`, `"until"`: Loop boundaries.
  - `"step"`: Increment step value (defaults to `1.0`).
  - `"index"`: Loop iterator variable name (defaults to `"i"`). Accessible inside loop expressions using `$vars.i`.
  - `"commands"`: A nested array of drawing commands.
- `"global"`:
  A canvas drawing command wrapper. When used, all nested drawing commands placed in its `"commands"` array are wrapped in a global context in the writer and automatically hoisted/rewritten to the very beginning of the output compiled binary document. It is nesting-safe and merges with other global components.
- `"save"` / `"restore"`: Saves the canvas matrix/paint state, and restores it. A `"save"` block can optionally contain nested `"commands"`.
- `"clipRect"`: Clips subsequent operations to bounds: `left`, `top`, `right`, `bottom`.
- `"scale"`: Scales subsequent drawing coordinate space: `sx`, `sy`.

#### Path Creation

- `"pathCreate"`: Creates a new canvas path. Takes initial coordinate `x`, `y` and an optional `"id"` string to reference it.
- `"pathAppendLineTo"`: Appends a line segment: `"path"` reference, `x`, `y`.
- `"pathAppendClose"`: Closes the path: `"path"` reference.

#### Matrix Operations (`"matrixMultiply"`)

Renders drawing transformations using a matrix:
- `"matrix"`: Floating point matrix ID or name lookup (`"$matrices.name"`).
- `"mType"`: Short code identifying the multiplication operation (defaults to `0`).
- `"from"`: Input matrix array of floats or expressions.
- `"out"`: Array of output variable names to bind the computed values to.

Supported matrix operator keywords: `IDENTITY`, `TRANSLATE_X`, `TRANSLATE_Y`, `TRANSLATE_Z`, `TRANSLATE2`, `TRANSLATE3`, `SCALE_X`, `SCALE_Y`, `SCALE_Z`, `SCALE2`, `SCALE3`, `ROT_X`, `ROT_Y`, `ROT_Z`, `ROT_PZ`, `ROT_AXIS`, `MUL`, `PROJECTION`. These keywords can be referenced in resources as `"matrix:IDENTITY"`.

---

### 5. Decoupled Resource Subsystem (`ResourceParser.java`)

Resources are parsed in a first pass before layout initialization. They can be defined in three formats:
1. **JSONObject Map format**:
   `"colors": { "primary": "#123456" }`
2. **JSONArray Verbose format**:
   `"colors": [ { "name": "primary", "value": "#123456" } ]`
3. **JSONArray Tag-Key Shorthand format (Recommended for order-dependent reference structures)**:
   `"colors": [ { "primary": "#123456" } ]`

The Tag-Key shorthand format combines the benefits of strict, deterministic array ordering with the compact, clean notation of map keys. If the value is a themed color block, nested resource-level properties (like `"export": false`) can be defined inline:
```json
"colors": [
  { "blue": "#0047AB" },
  { "brand": { "light": "@colors.blue", "dark": "#001122", "export": false } }
]
```


#### Supported Resource Sections

- `"v_dims"`: Component dimension named float variables. Allows binding component properties like `"width"`, `"height"`, and `"fontSize"` directly to variables before the hierarchy is built.
- `"colors"`: Defines hexadecimal hex colors or themed colors. Themed colors dynamically switch based on system dark mode:
  ```json
  "colors": [
    {
      "name": "brand",
      "value": {
        "light": "$colors.blue",
        "dark": "#001122"
      }
    }
  ]
  ```
- `"paths"`: SVG-style path string definition or an operations array containing: `"moveTo"`, `"lineTo"`, `"quadTo"`, `"cubicTo"`, and `"close"`.
- `"floatArrays"`: Numeric arrays of floats (useful for spline charts, color lookup grids, etc.).
- `"variables"`: Float numbers, text-from-float sub-objects, dimension lookups (`width`/`height`/`fontSize`), or complex mathematical expressions. **By default, variables are compiled as unnamed float constants (`"export": false`) to save buffer space. If you need a public, dynamic variable that can be inspected or updated at runtime by the host application, specify `"export": true` explicitly.**
- `"matrices"`: Evaluates arrays of floats and matrix operator tags (`"matrix:TRANSLATE2"`) to compile a transformation matrix.

#### Global Resource Scoping & Hoisting

- **`global` Block (Recommended)**: The preferred, structured declarative way to hoist operations is by using the `"global"` layout component wrapper or the `"global"` canvas drawing command wrapper. The parser automatically and safely manages the nesting levels of these blocks, making them extremely robust and preventing double-start buffer exceptions.
- **`order`**: An optional array of strings inside resources to custom-sequence parsing blocks (e.g., `["v_dims", "colors", "variables"]`). If omitted, the default order is: `v_dims` $\rightarrow$ `colors` $\rightarrow$ `paths` $\rightarrow$ `floatArrays` $\rightarrow$ `variables` $\rightarrow$ `matrices`.

---

### 6. Expression Subsystem & Compiler (`ExpressionParser.java`)

The expression subsystem parses infix math string equations and compiles them into stack-based reverse polish notation (RPN) instructions utilizing the **Shunting-yard algorithm**.

#### Supported Operators & Precedence

| Operator | Operation | Precedence |
| :---: | :--- | :---: |
| **`+`** | Addition | 1 |
| **`-`** | Subtraction | 1 |
| **`*`** | Multiplication | 2 |
| **`/`** | Division | 2 |
| **`%`** | Modulo | 2 |
| **`-`** (Unary) | Change Sign | 3 |

> [!WARNING]
> The power exponent operator `^` is **NOT** supported. Use the `pow(base, exponent)` function instead.

#### System Variables

Equations can reference real-time system environment variables:

- `time`: Continuous floating-point seconds elapsed since layout load.
- `width` / `componentWidth` / `componentWidth()`: Current component width.
- `height` / `componentHeight` / `componentHeight()`: Current component height.
- `touchX` / `touchY`: The canvas coordinates of the last touch event.
- `fontSize`: System font size.
- `animationTime`: Continuous time elapsed since the current animation was started.
- `touchTime`: Timestamp of the last touch event.

#### Custom Variable Lookups

Custom variables declared in the `"resources"` block can be referenced using prefix lookups:
- `$vars.variableName` or `@vars.variableName`
- `$matrices.matrixName` or `@matrices.matrixName`

#### Mathematical Functions (33 Functions Supported)

| Category | Supported Functions |
| :--- | :--- |
| **Trigonometric** | `sin(x)`, `cos(x)`, `tan(x)`, `asin(x)`, `acos(x)`, `atan(x)`, `atan2(y, x)` |
| **Exponential** | `pow(base, exp)`, `sqrt(x)`, `exp(x)`, `log(x)`, `ln(x)` |
| **Arithmetic** | `abs(x)`, `min(a, b)`, `max(a, b)`, `floor(x)`, `ceil(x)`, `round(x)`, `sign(x)`, `fract(x)`, `square(x)`, `hypot(a, b)` |
| **Math Utilities** | `lerp(a, b, t)`, `step(edge, x)`, `smooth_step(edge0, edge1, x)`, `clamp(x, min, max)`, `mad(a, b, c)` *(computes $a \times b + c$)*, `ping_pong(x)` |
| **Array Utilities** | `arrayMin(arrayId)`, `arrayMax(arrayId)` |
| **Splines** | `spline(x, arrayId)` / `arraySpline(x, arrayId)`, `splineLoop(x, arrayId)` |
| **Animation** | `anim(duration)` |

---

### 7. Error Diagnostics & Path Recovery

The parser recursively pushes breadcrumbs to a diagnostic context stack as it traverses components, children, and nested modifiers. If a syntax error or unsupported parameter is encountered during either traversal pass, the parser intercepts the exception and prepends the complete diagnostic location hierarchy:

```
Parsing error at ContextPath: root -> children[1] -> children[0] -> commands[3]
Reason: Unknown component type: unknown_layout
```

---

## Example: Optimized Document Specification

The following example showcases deterministic themed resource definitions, global scoping, dynamic math expression tokens, and drawing commands:

```json
{
  "header": {
    "width": 400,
    "height": 800,
    "fps": 60,
    "orderedResources": true
  },
  "root": {
    "global": [
      {
        "resources": {
          "colors": [
            { "blue": "#0047AB" },
            { "brand": { "light": "@colors.blue", "dark": "#001122" } }
          ],
          "variables": [
            { "pulse": { "value": "50 + sin(time * 2.0) * 20", "export": true } }
          ]
        }
      },
      {
        "canvas": {
          "commands": [
            {
              "paint": {
                "color": "$colors.brand",
                "style": "fill"
              }
            },
            {
              "drawCircle": {
                "cx": "width / 2",
                "cy": "height / 2",
                "radius": "$vars.pulse"
              }
            }
          ]
        }
      }
    ]
  }
}
```

---

## Implementation Technical Reference

- **Modular Target Package**: `androidx.compose.remote.creation.json`
- **Core Files**:
  - `RemoteComposeJsonParser.java`: Two-pass descent traversal parser kernel.
  - `ResourceParser.java`: Decoder engine for colors, vectors, matrices, variables, and arrays.
  - `ExpressionParser.java`: Infix RPN tokenizer and Shunting-yard compiler.
  - `JsonComponentParser.java` / `JsonModifierParser.java`: Extensible plugin registries interfaces.
- **Dependencies**: `org.json`
- **Compatibility**: Pure Java implementation, no Android dependencies in the core parser logic.
- **Validation**: Verified via `RemoteComposeJsonParserTest.java` for schema compliance and error propagation.
