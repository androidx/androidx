# RemoteCompose JSON Parser

The `RemoteComposeJsonParser` provides a standardized way to define RemoteCompose documents using JSON. It allows for a declarative, human-readable representation of layouts, components, and dynamic drawing operations, which is then parsed into a binary document using the `RemoteComposeWriter` API.

---

## Core Principles

- **Polymorphism via Component-Specific Key or `type`**: Every component, modifier, or drawing command is represented as a JSON object. For components inside hierarchy lists (like `root`, `children`), the parser supports two syntaxes:
  - **Short/Key Shorthand Syntax (Recommended)**: The component type name serves directly as the JSON key, containing a nested object of properties (e.g., `{ "column": { ... } }`, `{ "text": { ... } }`).
  - **Explicit Type Syntax**: The object uses a mandatory `"type"` key (e.g., `{ "type": "column", ... }`, `{ "type": "text", ... }`).
  - *Note*: Helper objects representing modifiers, shapes (like `RoundedRect`), or click actions (like `ValueStringChange`) must continue to use the explicit `"type"` key.
- **Flat Attributes**: Component-specific properties (like `id`, `value`, `alignment`, or `source`) are top-level keys within the component object.
- **Ordered Modifiers**: Styling and transformation operations are stored in an optional `"modifiers"` array to ensure they are applied in a deterministic functional order.
- **Ordered Children/Commands**: Nested layout components are stored in a `"children"` array, while drawing commands for canvas components are stored in a `"commands"` array.
- **Deterministic Resource Ordering**: Resources like `colors`, `variables`, `paths`, and `matrices` can be defined as an array of objects (`[ { "name": "n", "value": "v" } ]`) or as a map of key-value pairs. The parser scans the `root` array for `resources` and `variable` commands and parses them *before* root component initialization, enabling bit-for-bit identity with code-generated documents.
- **Infix Expressions**: Dynamic math is represented as infix strings (e.g., `"a + b * sin(time)"`) which are parsed into the underlying expression engine. Expressions can refer to other variables using `@name` (or `$name`), or the legacy `@vars.name` / `$vars.name` format.

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
| `"profiles"` | Integer | Capability profile bitmask (`512` = ANDROIDX, `513` = EXPERIMENTAL). |
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

- `"value"`: The plain text string. Supports variable lookups (e.g. `"$message"` / `@message`, or the legacy `$vars.message` / `@vars.message`).
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
- `"fontWeight"`: Font weight of the text. Supports a float value (e.g., `400.0`, `700.0`). Defaults to `TextStyle.DEFAULT_FONT_WEIGHT`.
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
- `"fillMaxWidth"`, `"fillMaxHeight"`, `"fillMaxSize"`: Sizing modifiers taking a float scale (e.g. `1.0` or `null` for full fill).
- `"width"`, `"height"`: Fixed dimension bounds taking a float value or expression.
- `"size"`: Sizing shortcut setting both width and height to a single float value or `[width, height]` array.
- `"widthIn"`, `"heightIn"`, `"requiredWidthIn"`, `"requiredHeightIn"`: Min/Max bounding constraint arrays containing two floats: `[min, max]`.
- `"weight"` / `"horizontalWeight"`: Horizontal weight allocation inside rows or flows.
- `"verticalWeight"`: Vertical weight allocation inside columns.
- `"background"`: Sets component background color using a hex color string, color ref (`"$colors.name"` / `"@colors.name"`), or variable.
- `"border"`: Border layout modifier: `{ "width": 2.0, "cornerRadius": 8.0, "color": "#FF0000", "shape": 0 }`.
- `"verticalScroll"`, `"horizontalScroll"`: Configures layout scroll position bounds via float value/expression or `{ "position": "@pos", "notches": 0 }`.
- `"collapsiblePriority"`: Priority for collapsible layouts. Accepts number, `[orientation, priority]`, or `{ "orientation": "horizontal"|"vertical", "priority": 1.0 }`.
- `"clip"`: Clips component to a shape (`"circle"`, `"rect"`, `"roundRect"`).
- `"graphicsLayer"`: Layer transforms: `scaleX`, `scaleY`, `rotationZ`, `translationX`, `translationY`, `alpha`.
- `"marquee"`: Animated text marquee: `{ "iterations": 10, "repeatDelayMillis": 1200, "spacing": 20.0, "velocity": 30.0 }`.
- `"ripple"`: Material touch ripple.
- `"semantics"`: Accessibility descriptors (`contentDescription`, `text`, `stateDescription`, `enabled`, `clickable`).
- `"visibility"`: Dynamic visibility state ID/expression.
- `"animationSpec"`: Configures layout motion and visibility enter/exit transitions, including custom `defineVisibilityAnimation` functions and layout sequencing (`"CONCURRENT"` default, `"BEFORE"`, `"AFTER"`):
  ```json
  {
    "animationSpec": {
      "motionDuration": 350,
      "motionEasingType": "standard",
      "visibilityDuration": 600,
      "visibilityEasingType": "standard",
      "enterAnimation": "fadeIn",
      "exitAnimation": "fadeOut",
      "enterFunction": "@customEnter",
      "exitFunction": "@customExit",
      "enterSequence": "AFTER",
      "exitSequence": "BEFORE"
    }
  }
  ```
- `"zIndex"`: Stacking order float or expression.
- `"offset"`: Translation offset array `[x, y]` or object `{ "x": "@dx", "y": "@dy" }`.
- `"id"`: Binds a unique integer component ID.
- `"onClick"`, `"multiClick"`, `"onTouchDown"`, `"onTouchUp"`, `"onTouchCancel"`: Touch & click action handlers:
  - `"hostAction"` / `"hostNamedAction"`: Host callback action: `{ "type": "hostAction", "name": "actionName", "value": "@var" }`.
  - `"valueFloatChange"` / `"valueFloatExpressionChange"`: Updates float variable on click/touch.
  - `"valueIntegerChange"` / `"valueIntegerExpressionChange"`: Updates integer variable on click/touch.
  - `"valueStringChange"`: Updates string variable on click/touch.

---

### 4. Drawing Commands & Canvas Subsystem

The `"canvas"` component (and `"defineVisibilityAnimation"` blocks) render custom shapes via their `"commands"` array.

#### Supported Drawing Primitives

- `"drawRect"`: `left`, `top`, `right`, `bottom`.
- `"drawLine"`: `x1`, `y1`, `x2`, `y2`.
- `"drawCircle"`: `cx`, `cy`, `radius`.
- `"drawOval"`: `left`, `top`, `right`, `bottom`.
- `"drawRoundRect"`: `left`, `top`, `right`, `bottom`, `rx`, `ry`.
- `"drawArc"`: `left`, `top`, `right`, `bottom`, `startAngle`, `sweepAngle`.
- `"drawSector"`: `left`, `top`, `right`, `bottom`, `startAngle`, `sweepAngle`.
- `"drawPath"`: Draws a predefined path specified by `"path"` ID, SVG path string, or path name reference (`"$paths.name"` / `"@paths.name"`).
- `"drawTextRun"`: Renders text run with `start`, `end`, `x`, `y`.
- `"drawTextAnchored"`: Renders anchored text with `x`, `y`, `flags`.
- `"drawTextOnPath"`: Renders text along a vector path: `text`, `path`, `hOffset`, `vOffset`.
- `"drawBitmap"`: Renders bitmap image resource by name/ID: `image`, `left`, `top`, `right`, `bottom`.
- `"drawScaledBitmap"`: Renders cropped/scaled bitmap: `image`, `srcLeft`, `srcTop`, `srcRight`, `srcBottom`, `dstLeft`, `dstTop`, `dstRight`, `dstBottom`.
- `"drawComponentContent"` (or `"drawContent"`): Renders the live component subtree inside a custom visibility animation block, automatically composited into an offscreen layer with the active paint alpha.
- `"createOffscreenBitmap"`: Emits a `BitmapData` operation (`ENCODING_COMPONENT_OFFSCREEN_BUFFER`) for a component-sized pooled offscreen bitmap (with scoped pool frames supporting nested component animations) and binds its ID to the specified variable name (e.g. `{ "createOffscreenBitmap": "offscreenBitmap" }`).
- `"drawComponentToBitmap"`: Captures the component identified by `"id"` at `(0, 0)` into the pooled offscreen bitmap specified by `"bitmap"` (`{ "drawComponentToBitmap": { "id": "@id", "bitmap": "@offscreenBitmap" } }`), using stack-based offscreen targets so nested component animations compose cleanly.

#### Paint Operations & Configurations

Paint attributes can be specified via individual commands (`setColor`, `setStyle`, `setStrokeWidth`) or a unified `"paint"` command with an `"ops"` array:

- `"color"`: Hex color, color reference (`"$colors.primary"` / `"@colors.primary"`), or variable.
- `"alpha"`: Opacity float value or expression.
- `"width"` / `"strokeWidth"`: Stroke width float value.
- `"style"`: `"fill"`, `"stroke"`, or `"fillAndStroke"`.
- `"strokeCap"`: `"butt"`, `"round"`, or `"square"`.
- `"strokeJoin"`: `"miter"`, `"round"`, or `"bevel"`.
- `"shader"`: Shader ID (`0` to clear shader) or inline AGSL shader object (`{ "agsl": "...", "uniforms": { ... } }`).
- `"pathEffect"`: Array of floats configuring line dash effects.
- `"linearGradient"`: Configures a linear gradient (`x1`, `y1`, `x2`, `y2`, `colors`, `stops`, `tileMode`).
- `"radialGradient"`: Configures a radial gradient. Two forms are accepted:
  - Single circle: `centerX`, `centerY`, `radius`, `colors`, `stops`, `tileMode`.
  - Two circles (focal): `startX`, `startY`, `startR` and `endX`, `endY`, `endR`, plus the same
    `colors`, `stops`, `tileMode`. `startRadius`/`endRadius` are accepted as aliases of
    `startR`/`endR`, and `focalX`/`focalY`/`focalR` may be layered on top of a
    `centerX`/`centerY`/`radius` gradient, in which case the center circle becomes the end
    circle. The gradient runs from the edge of the start (focal) circle to the edge of the end
    circle. Requires API 31 on the player; below that the end circle alone is drawn.
    ```json
    {
      "radialGradient": {
        "startX": 120.0, "startY": 120.0, "startR": 0.0,
        "endX": 200.0, "endY": 200.0, "endR": 180.0,
        "colors": ["#FFFFFF", "#2266FF", "#001133"],
        "stops": [0.0, 0.45, 1.0],
        "tileMode": 0
      }
    }
    ```
- `"sweepGradient"`: Configures a sweep gradient (`centerX`, `centerY`, `colors`, `stops`).

#### Advanced Shaders, Custom Visibility Animations & Particle Systems

- `"defineVisibilityAnimation"`: Defines a reusable, re-entrant custom visibility enter/exit drawing function parameterized by up to 6 runtime arguments (`["progress", "w", "h", "x", "y", "id"]`) that can be referenced by `"@name"` in `animationSpec` (including on both parent and child components simultaneously):
  ```json
  {
    "defineVisibilityAnimation": {
      "name": "meltExit",
      "params": ["progress", "w", "h", "x", "y", "id"],
      "commands": [
        { "createOffscreenBitmap": "offscreenBitmap" },
        { "drawComponentToBitmap": { "id": "@id", "bitmap": "@offscreenBitmap" } },
        {
          "save": [
            { "translate": { "dx": "@x", "dy": "@y" } },
            {
              "paint": {
                "shader": {
                  "agsl": "uniform shader uTexture; uniform float uProgress; uniform float2 uResolution; half4 main(vec2 c) { return uTexture.eval(c) * (1.0 - uProgress); }",
                  "uniforms": {
                    "uTexture": "@offscreenBitmap",
                    "uProgress": "@progress",
                    "uResolution": ["@w", "@h"]
                  }
                }
              }
            },
            { "drawRect": { "left": 0, "top": 0, "right": "@w", "bottom": "@h" } },
            { "paint": { "shader": 0 } }
          ]
        }
      ]
    }
  }
  ```
- `"runtimeShader"`: AGSL (Android Graphics Shading Language) runtime shader effect:
  ```json
  {
    "runtimeShader": {
      "name": "myEffect",
      "agsl": "uniform float time; half4 main(float2 p) { return half4(sin(time), 0.5, 1.0, 1.0); }",
      "uniforms": { "time": "time * 2.0" }
    }
  }
  ```
- `"createParticles"` / `"particlesLoop"` / `"impulseProcess"`: Defines high-performance GPU particle emitter creation, evolution equations, and particle loop rendering:
  ```json
  {
    "createParticles": {
      "count": 500,
      "equations": [ "rand() * 6.28", "time * 100" ]
    }
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
  - `"index"`: Loop iterator variable name (defaults to `"i"`). Accessible inside loop expressions using `@i` (or `$i`).
  - `"commands"`: A nested array of drawing commands.
- `"global"`:
  A canvas drawing command wrapper. When used, all nested drawing commands placed in its `"commands"` array are wrapped in a global context in the writer and automatically hoisted/rewritten to the very beginning of the output compiled binary document. It is nesting-safe and merges with other global components.
- `"save"` / `"restore"`: Saves the canvas matrix/paint state, and restores it. A `"save"` block can contain an array of commands (`{ "save": [ ... ] }`) or an object with nested `"commands"`.
- `"clipRect"`: Clips subsequent operations to bounds: `left`, `top`, `right`, `bottom`.
- `"scale"`: Scales subsequent drawing coordinate space: `sx`, `sy`, `pivotX`, `pivotY`.

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

#### 3D Commands (`Command3DParser.java`)

A canvas draws 3D with eleven commands. They sit in its `"commands"` array, mixed with the 2D commands, and run in order. Camera, lights, matrix and material last until a command changes them. Each command maps to a 3D operation specified in [PROTOCOL_3D.md](../../remote-core/doc/PROTOCOL_3D.md), which also defines what a player does with it.

```json
{
  "header": { "apiLevel": 7, "profiles": 513 },
  "root": [
    { "canvas": { "modifiers": ["fillMaxSize"], "commands": [
      { "clearDepth3D": {} },
      { "camera3D": { "projection": "perspective", "fovY": 0.85, "aspect": 1.0,
                      "near": 0.1, "far": 100.0,
                      "eye": [0, 0.35, 4.4], "center": [0, 0, 0], "up": [0, 1, 0] } },
      { "lights3D": { "lights": [
          { "type": "directional", "color": "#FFFFF4E4", "dir": [-0.4, -0.6, -0.7], "intensity": 1.0 } ] } },
      { "meshPrimitive3D": { "id": 1, "primitive": "sphere", "segments": 24,
                             "radius": 0.42, "center": [0, 0, 0] } },
      { "paint": { "ops": [ { "color": "#FFD98A4A" } ] } },
      { "matrix3D": { "op": "rotate", "angle": 0.52, "axis": [0, 1, 0] } },
      { "drawMesh3D": { "mesh": 1, "mode": "software-smooth" } }
    ] } }
  ]
}
```

- The 3D operations exist only in the experimental profiles, so a 3D document sets `"profiles": 513` (ANDROIDX | EXPERIMENTAL) and keeps the default `"apiLevel": 7` or higher.
- Command names are case-insensitive: the parser lower-cases them before dispatch.
- Every float key takes a number, a variable reference (`"@name"`) or an infix expression, so geometry, camera, lights and material can animate. The exception is `defineMesh3D`, whose arrays take numbers only.
- A wrong name is an error, not a default: a mistyped primitive, mode or projection would otherwise draw something plausible and wrong. Errors name the key and list the valid values.

| Command | Operation | Keys (defaults) |
| :--- | :--- | :--- |
| `"clearDepth3D"` | `PAINT_3D_STATE` 0 | `{}`. Starts a scene: needed before the first 3D draw of each frame. |
| `"material3D"` | `PAINT_3D_STATE` 1 | `specular` (`0`), `shininess` (`32`) |
| `"depthBias3D"` | `PAINT_3D_STATE` 2 | `constant` (`0`), `slope` (`0`) |
| `"camera3D"` | `SET_CAMERA_3D` | `projection` (`"perspective"`), `fovY` (`0.9`), `aspect` (`1`), `near` (`0.1`), `far` (`100`), `left`, `right`, `bottom`, `top`, `eye` (`[0, 0, 4]`), `center` (`[0, 0, 0]`), `up` (`[0, 1, 0]`) |
| `"lights3D"` | `SET_LIGHTS_3D` | `lights`: an array of lights. Replaces the whole light set. |
| `"matrix3D"` | `MATRIX_3D_OP` | `op` (`"identity"`) and its arguments |
| `"defineMesh3D"` | `DEFINE_MESH_3D` | `id`, `verts`, `indices`, `normals`, `uv` |
| `"meshPrimitive3D"` | `MESH_PRIMITIVE_3D` | `id`, `primitive`, `segments` (`0`), `flags` (`0`), `uv` (`"none"`), `axis` (`"native"`), `curvatureDensity` (`false`), the primitive's keys |
| `"meshExpression3D"` | `MESH_EXPRESSION_3D` | `id`, `surface` (`"general"`), the surface's keys, `normal`, `uv`, `flipWinding` (`false`), `flipNormals` (`false`), `flags` (`0`) |
| `"texture3D"` | `SET_TEXTURE_3D` | `bitmap` (absent clears the texture) |
| `"drawMesh3D"` | `DRAW_MESH_3D` | `mesh`, `mode` (`"software-flat"`), `wireframe`, `edges`, `transparent`, `doubleSided`, `gpu`, `pbr` (all `false`), `frontFace` (`"ccw"`) |

**`"camera3D"`**
- `projection`: `"perspective"`, `"ortho"` or `"orthographic"`. Perspective takes `fovY` (vertical, in radians), `aspect` (width / height), `near` and `far`. Orthographic requires `left`, `right`, `bottom` and `top`, and takes the same `near` and `far`.
- `eye`, `center` and `up` are 3 values each (a look-at view).
- The camera resets the 3D model matrix to identity.

**`"lights3D"`**
- Each light has a `type`, a `color` (`"#FFFFFFFF"`; a color string or reference) and an `intensity` (`1`).
- `"directional"` (the default; also `"dir"`) requires `dir`, the 3-component direction the light travels, in world space.
- `"point"` requires `pos`, its 3-component position. Point lights have no attenuation.
- `"ambient"` takes only `color` and `intensity`, and lights every surface evenly. A `dir` or `pos` on it is an error.
- A missing or wrong-sized vector is an error rather than a default: a light pointing nowhere is invisible, and would be debugged as a material or normals problem.
- An absent or empty `lights` array means no lights: surfaces get the default ambient light only. A document that never uses `lights3D` gets a headlight from the camera.

**`"matrix3D"`**
```json
{ "matrix3D": { "op": "identity" } }
{ "matrix3D": { "op": "translate", "x": 0.0, "y": 1.2, "z": 0.0 } }
{ "matrix3D": { "op": "scale", "x": 2.0, "y": 2.0, "z": 2.0 } }
{ "matrix3D": { "op": "rotate", "angle": 0.52, "axis": [0, 1, 0] } }
{ "matrix3D": { "op": "multiply", "m": [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1] } }
{ "matrix3D": { "op": "push" } }
{ "matrix3D": { "op": "pop" } }
```
- Each op post-multiplies the model matrix. `translate` and `scale` default each of `x`, `y`, `z` to `0`. `rotate` requires `angle`, and `axis` defaults to `[0, 1, 0]`. `multiply` takes 16 column-major values in `m`.
- **`rotate` takes radians.** The 2D `rotate` command takes degrees.
- **The op name goes in an `"op"` key.** `{"matrix3D": {"translate": [x, y, z]}}` is an error that lists the keys found: it used to be read as `identity`, which stacked every instance at the origin.
- `push` and `pop` save and restore the model matrix (only), up to 32 deep, so a hierarchy no longer has to restate its whole chain from identity.

**`"defineMesh3D"`**
```json
{ "defineMesh3D": { "id": 7, "verts": [0, 0, 0, 1, 0, 0, 0, 1, 0], "indices": [0, 1, 2],
                    "normals": [0, 0, 1, 0, 0, 1, 0, 0, 1], "uv": [0, 0, 1, 0, 0, 1] } }
```
- `verts` and `normals` hold x, y, z per vertex; `uv` holds u, v per vertex; `indices` holds 3 per triangle, counter-clockwise for the front face. `normals` and `uv` are optional; empty arrays count as absent.
- The lengths are checked: indices and verts must be multiples of 3, `normals` as long as `verts`, and `uv` 2 per vertex.

**`"meshPrimitive3D"`**
```json
{ "meshPrimitive3D": { "id": 1, "primitive": "torus", "segments": 32,
                       "majorRadius": 0.6, "minorRadius": 0.18, "center": [0, 0, 0] } }
```
- `primitive` is one of the names below. Case, `_` and `-` are ignored, so `"rounded_cube"` works.
- Primitives take named keys. `center`, `from` and `to` are 3 values that default to the origin; every other key is required, and a missing one is an error that lists the keys the primitive needs.
- `params` (a positional list of channel 0) replaces the named keys, and `channels` appends extra channels, as the wire format lays them out.
- `segments` (`0` = the primitive's default) is a slice or side count for most primitives, but the subdivision level for `icosphere`, and unused by some. See [PROTOCOL_3D.md](../../remote-core/doc/PROTOCOL_3D.md) for each primitive's meaning, default and minimum.
- `uv`: `"none"`, `"uv"`, `"uv2"` or `"uv3"` generates texture coordinates. `flags` is OR-ed into the wire flags as a raw number.
- `axis`: `"native"` (the default), `"x"`, `"y"` or `"z"` in any case, or `0` to `3`. It turns the primitive's built-in axis onto the given one.
- `curvatureDensity: true` places tube rings by curvature instead of arc length. Only `tube`, `capTube` and `profileTube` take it.

| Primitive | Keys |
| :--- | :--- |
| `sphere`, `icosphere` | `radius`, `center` |
| `cylinder` | `radius`, `from`, `to` |
| `cone` | `radius`, `height`, `center` (the base center) |
| `cube` | `dimX`, `dimY`, `dimZ`, `center` |
| `roundedCube` | `dimX`, `dimY`, `dimZ`, `center`, `cornerRadius` |
| `sphericalSector`, `sphericalDome` | `radius`, `angle`, `center` |
| `torus` | `majorRadius`, `minorRadius`, `center` |
| `plane` | `width`, `height`, `center` |
| `extrudeCircle` | `radius`, `depth`, `center` |
| `extrudeSector`, `extrudeSegment` | `radius`, `startAngle`, `sweepAngle`, `depth`, `center` |
| `extrudeArc` | `innerRadius`, `outerRadius`, `startAngle`, `sweepAngle`, `depth`, `center` |
| `extrudeRoundedRect` | `width`, `height`, `cornerRadius`, `depth`, `center` |
| `extrudeSquircle` | `radius`, `exponent`, `depth`, `center` |
| `helix` | `coilRadius`, `tubeRadius`, `pitch`, `turns`, `center` |
| `lathe` | `center`, `profile`: `[[radius, y], ...]` |
| `tube`, `capTube` | `radius`, `points`: `[[x, y, z], ...]` (at least 2); optional `ringsPerSpan`, `legacy`, `spline` |
| `profileTube` | `points`, `radii`: `[r0, r1, ...]`; optional `ringsPerSpan`, `capped` (`true`) |
| `sweep` | `section`: `[[x, y], ...]`, `path`: `[[x, y, z], ...]`, `center`; `closed`, `capStart`, `capEnd` (all `true`); optional `scales`, and `twists` (radians; requires `scales`) |
| `extrudePath` | `contours`: `[[[x, y], ...], ...]`, `depth` (`1`), `bevel` (`0`), `center` |
| `infinitePlane` | `center` (`[0, 0, 0]`), `spacing` (`0`: no grid), `lineWidth` (`1`, in pixels), `lineColor` (`"#FF808080"`): `"#RRGGBB"`, `"#AARRGGBB"`, an ARGB integer, or `[a, r, g, b]` with each entry a number in 0..1 or an expression. Named and theme colors don't work here. |

All angles are in radians.

**`"meshExpression3D"`**
```json
{ "meshExpression3D": { "id": 3, "surface": "heightField",
                        "xRange": [-1, 1], "uCount": 32, "zRange": [-1, 1], "vCount": 32,
                        "height": "0.2 * sin(u * 6) * cos(v * 6)" } }
```

| `surface` | Keys | `u` and `v` are |
| :--- | :--- | :--- |
| `"general"` | `uRange`, `uCount`, `vRange`, `vCount`, and the expressions `x`, `y`, `z` | the grid parameters, over the ranges |
| `"heightField"` | `xRange`, `uCount`, `zRange`, `vCount`, `height`; optional `yRange` (clamps the height; default ±1e6) | x and z |
| `"sphere"` | `radius`, `uCount`, `vCount`; optional `displacement` (`0`) | longitude (0 to 2π) and latitude (0 to π, from +Y) |
| `"cylinder"` | `radius`, `height`, `uCount`, `vCount`; optional `displacement` (`0`) | the angle (0 to 2π) and the height along the axis |

- Ranges are `[min, max]`. The `surface` name ignores case and `_`.
- Optional `normal` (3 expressions, used as written, not normalized) and `uv` (2 expressions) replace the generated normals and texture coordinates.
- The generated normals point to the front side: +Y for `heightField`, the outside for `cylinder`, but the **inside** for `sphere`, and for `general` the side ∂P/∂v × ∂P/∂u points to (−Z for `(u, v, 0)`, +Y for `(u, 0, v)`). `flipWinding: true` reverses the triangles and the generated normals, which swaps the sides: a `sphere` seen from outside needs it. `flipNormals: true` reverses every normal, `normal` expressions included, and keeps the winding. `flags` sets raw flag bits.
- `u` and `v` refer to the grid only inside these expressions, so they don't shadow a document's own variables elsewhere.
- The player evaluates every vertex on every frame: keep grids small.

**`"texture3D"`**
- `bitmap` is a `resources.bitmaps` name (`"@earth"`, `"$earth"` or `"earth"`) or a numeric bitmap id. An absent, `null` or `0` bitmap clears the texture.
- A draw shows the texture only if its mesh has texture coordinates: give primitives a `uv` mode.
- v = 0 samples the bitmap's **bottom** row. Mapping v = 0 to a sphere's north pole renders an equirectangular map upside down.

**`"drawMesh3D"`**
```json
{ "drawMesh3D": { "mesh": 1, "mode": "software-smooth" } }
{ "drawMesh3D": { "mesh": 1, "mode": "software-flat", "wireframe": true, "edges": [0, 1] } }
```
- `mode` is a name or a number. The names combine an engine and a shading: `software-`, `canvas-`, `drawmesh-`, `gl-`, `drawmesh-zbuf-` and `canvas-zbuf-`, each followed by `flat` or `smooth`. `"flat"` and `"smooth"` mean software. An engine a player doesn't implement falls back to software, so any mode is safe to ask for; keep one engine per scene.
- `wireframe: true` draws hidden-line edges in the paint color, and forces the software engine. `edges` (with `wireframe`) lists the triangle edges to draw: `0` (v0→v1), `1` (v1→v2) and `2` (v2→v0).
- Optional boolean flags: `transparent` (blend over the scene without writing depth; draw opaque meshes first, then transparent ones back to front), `doubleSided` (no back-face culling: shows wrongly wound meshes), `gpu` and `pbr` (hints; no effect on Android yet).
- `frontFace`: `"ccw"` (the default) or `"cw"`, in any case. `"cw"` makes clockwise triangles front-facing, like `glFrontFace(GL_CW)`: one flag fixes a mesh wound clockwise or drawn through a mirroring matrix.

##### Errors

| Problem | Message |
| :--- | :--- |
| Unknown projection | `camera3D projection '<name>'` |
| Eye, center or up not 3 values | `camera3D needs eye/center/up of 3 components each` |
| Unknown light type | `lights3D type '<name>' (expected one of [...])` |
| Directional or point light without its vector | `lights3D <type> needs a 3-component dir` (or `pos`) |
| Ambient light with a vector | `lights3D ambient takes only color and intensity, not dir` (or `pos`) |
| `matrix3D` without `op` | `matrix3D needs an "op" (identity/translate/scale/rotate/multiply/push/pop); got keys [...]` |
| Unknown matrix op | `matrix3D op '<name>' (expected one of [...])` |
| `rotate` axis not 3 values | `matrix3D rotate axis needs 3 components` |
| `multiply` without 16 values | `matrix3D multiply needs 16 floats, got <n>` |
| Bad mesh lengths | `defineMesh3D indices length <n> is not a multiple of 3`, `defineMesh3D verts length <n> is not a multiple of 3`, `defineMesh3D normals <a> != verts <b>`, `defineMesh3D uv <a> != 2 per vertex (<b>)` |
| Unknown primitive | `meshPrimitive3D primitive '<name>' (expected one of [...])` |
| Missing primitive key | `meshPrimitive3D <primitive>: missing '<key>' (needs [...])` |
| Unknown UV mode | `meshPrimitive3D uv '<name>'` |
| Unknown axis | `meshPrimitive3D axis '<name>' (expected one of [native, x, y, z] or 0-3)` |
| `curvatureDensity` on another primitive | `meshPrimitive3D <primitive>: 'curvatureDensity' applies only to tube, capTube and profileTube` |
| Plane center not 3 values | `meshPrimitive3D infinitePlane: center needs 3 components` |
| Bad line color | `meshPrimitive3D infinitePlane: lineColor <value> (expected "#RRGGBB", "#AARRGGBB", an ARGB integer or [a, r, g, b] floats in 0..1)`; a number outside 0..1 in the list: `meshPrimitive3D infinitePlane: lineColor components are 0..1, got <x>` |
| Unknown surface | `meshExpression3D surface '<name>' (expected one of [...])` |
| Missing surface key | `meshExpression3D <surface>: missing '<key>'` |
| Range not 2 values | `meshExpression3D <surface>: '<key>' must be [min, max]` |
| Wrong expression counts | `meshExpression3D: 'normal' needs three expressions (nx, ny, nz); omit it for finite-difference normals`, `meshExpression3D: 'uv' needs two expressions (u, v)` |
| Unknown bitmap | `bitmap '<name>' not found (declared: ...)` |
| Unknown mode | `drawMesh3D mode '<name>' (expected one of [...])` |
| Bad edge | `drawMesh3D edge <n> (expected 0, 1 or 2)` |
| Unknown front face | `drawMesh3D frontFace '<name>' (expected "ccw" or "cw")` |

##### Things to know

- **Angles are radians** in every 3D command: `fovY`, `rotate`, primitive angles, sweep twists.
- **Clear depth once per frame**, before the first 3D draw.
- **A wireframe mesh hides what's behind it.** Its faces write depth over their whole area, so draw a wireframe cage last, after what it contains. A wireframe never shows a texture.
- **`depthBias3D` is in window-depth units**, so its size in the world grows with distance squared and with `far / near`. A value tuned for one camera can hide whole objects from another.
- **Expressions are capped at 32 tokens.** A float key given as an expression becomes a float expression, which players refuse beyond 32 RPN tokens; compute constants in the generator instead. `meshExpression3D` expressions are separate programs, so this cap doesn't apply to them, but each holds at most 128 floats.
- **Rotate a scene by touch with a `touchExpression`**, not a bare `touchX()`: `touchX()` tracks the finger's absolute position and snaps back on release.

#### Touch-driven values (`"touchExpression"`)

`{"touchExpression": {...}}` defines a float that accumulates touch movement, typically to rotate a 3D scene. It's a canvas command:

```json
{ "touchExpression": { "name": "spin", "expression": "touchX() * 0.012",
                       "min": 0, "max": 6.2832, "stopMode": "gently" } }
```

| Key | Default | Meaning |
| :--- | :--- | :--- |
| `name` | | The variable name, for `"@spin"` references. |
| `expression` | `"touchX()"` | Maps the raw touch to the value: an infix string, a number, or an RPN array. |
| `defaultValue` | `0` | The initial value. |
| `min` | NaN | The minimum. When absent, the value wraps around `max` instead of clamping. |
| `max` | `1` | The maximum. |
| `stopMode` (or `touchMode`) | `0` | A number, a numeric string such as `"2"`, or a name: `gently`, `instantly`, `ends`, `notchesEven`, `notchesPercents`, `notchesAbsolute`, `absolutePos`, `notchesSingleEven` (case and `_` ignored). Any other string is an error. |
| `velocityId` | NaN | A variable that receives the velocity. |
| `touchEffects` | `0` | Haptic effects. |
| `touchSpec`, `easingSpec` | | Arrays of numbers passed to the operation as is. |

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
- `"bitmaps"`: PNG images embedded in the document, for `texture3D`. Either a map `{ "earth": source }` or an array of `{ "earth": source }`, `{ "name": "earth", "value": source }` or `{ "name": "earth", "file": ... }` entries. A source is a file path string (relative paths resolve against the parser's base directory) or an object with `file`, `base64` or `data`; `data` is an alias of `base64` and may be a base64 `data:` URI. Only PNG is accepted (a JPEG is an error), with each dimension at most 65535. The image is written inline as `TYPE_PNG_8888`.

#### Global Resource Scoping & Hoisting

- **`global` Block (Recommended)**: The preferred, structured declarative way to hoist operations is by using the `"global"` layout component wrapper or the `"global"` canvas drawing command wrapper. The parser automatically and safely manages the nesting levels of these blocks, making them extremely robust and preventing double-start buffer exceptions.
- **`order`**: An optional array of strings inside resources to custom-sequence parsing blocks (e.g., `["v_dims", "colors", "variables"]`). If omitted, the default order is: `v_dims` $\rightarrow$ `colors` $\rightarrow$ `paths` $\rightarrow$ `floatArrays` $\rightarrow$ `variables` $\rightarrow$ `integers` $\rightarrow$ `matrices` $\rightarrow$ `bitmaps`.

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

- `time` / `continuousSec` / `continuousSec()`: Continuous floating-point seconds elapsed since layout load.
- `seconds` / `timeInSec` / `timeInSec()`: Current second of the minute (`0.0` - `59.0`).
- `timeInMin` / `timeInMin()`: Current minute of the hour (`0.0` - `59.0`).
- `timeInHr` / `timeInHr()`: Current hour of the day (`0.0` - `23.0`).
- `windowWidth` / `windowWidth()`: Screen window width.
- `windowHeight` / `windowHeight()`: Screen window height.
- `width` / `componentWidth` / `componentWidth()`: Current component width.
- `height` / `componentHeight` / `componentHeight()`: Current component height.
- `touchX` / `touchY`: The canvas coordinates of the last touch event.
- `fontSize`: System font size.
- `animationTime`: Continuous time elapsed since the current animation was started.
- `touchTime`: Timestamp of the last touch event.

#### Custom Variable Lookups

Custom variables declared in the `"resources"` block can be referenced using prefix lookups:
- `@variableName` or `$variableName` (or legacy `$vars.variableName` / `@vars.variableName`)
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
                "radius": "@pulse"
              }
            }
          ]
        }
      }
    ]
  }
}
```

### 8. Loom Templates & Referenced Operations

The JSON parser supports defining and reusing binary Component Templates (Loom) and shared styles (Referenced Operations).

#### Defining and Inflating Templates

A component template is defined once using `definePattern`, and can be called either using `patternInflation` or using the pattern's name directly as a custom component type:

> [!IMPORTANT]
> The template `name` defined in `definePattern` must be unique and cannot override pre-existing registered component names (like `box`, `column`, `row`, `text`, etc.). Attempting to override a pre-existing component parser will fail and throw a `JSONException`.

```json
{
  "type": "definePattern",
  "name": "MyCard",
  "parameters": ["title", "color"],
  "locals": ["internalState"],
  "children": [
    {
      "type": "variable",
      "name": "internalState",
      "value": 0
    },
    {
      "type": "column",
      "modifiers": [
        { "background": "@color" }
      ],
      "children": [
        { "type": "text", "value": "@title" },
        { "type": "patternArgument", "parameterIndex": 0 }
      ]
    }
  ]
}
```

To inflate the template, call it by name and pass parameter values as standard key-value fields. Children slots (using `patternArgument`) can be populated at the call site by wrapping children inside `patternBlock`:

```json
{
  "type": "MyCard",
  "title": "Welcome Card",
  "color": "#FF0000",
  "children": [
    {
      "type": "patternBlock",
      "parameterIndex": 0,
      "children": [
        { "type": "text", "value": "Card Body Slot Content" }
      ]
    }
  ]
}
```

#### Shared Styles (Referenced Operations)

Static groups of layout modifiers can be compiled once using `referencedOperations` and applied to components via the `include` modifier:

```json
{
  "type": "referencedOperations",
  "name": "CardStyle",
  "modifiers": [
    { "padding": 16 },
    { "background": "#E0E0E0" }
  ]
}
```

At any component call site, include the style reference using the `include` modifier:

```json
{
  "type": "column",
  "modifiers": [
    { "include": "@CardStyle" }
  ],
  "children": [ ... ]
}
```

---

## Implementation Technical Reference

- **Modular Target Package**: `androidx.compose.remote.creation.json`
- **Core Files**:
  - `RemoteComposeJsonParser.java`: Two-pass descent traversal parser kernel.
  - `ResourceParser.java`: Decoder engine for colors, vectors, matrices, variables, arrays and bitmaps.
  - `Command3DParser.java`: The 3D canvas commands.
  - `ExpressionParser.java`: Infix RPN tokenizer and Shunting-yard compiler.
  - `JsonComponentParser.java` / `JsonModifierParser.java`: Extensible plugin registries interfaces.
- **Dependencies**: `org.json`
- **Compatibility**: Pure Java implementation, no Android dependencies in the core parser logic.
- **Validation**: Verified via `RemoteComposeJsonParserTest.java` for schema compliance and error propagation.

---

## Automated JSON Document Validation & Schema

For automated IDE IntelliSense, linting, and server-side document validation, a formal JSON Schema (Draft 2020-12) is available.

- **Schema File**: [`remote_compose_schema.json`](remote_compose_schema.json)
- **Validation Example (Python / Node / Java)**:
  ```json
  {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "RemoteCompose JSON Document",
    "required": ["root"]
  }
  ```
- **Validation Tooling**: Can be integrated into CI/CD pipelines, design export tools (Figma plugins), and REST / gRPC API servers before compiling JSON payloads into binary `.rcd` wirebuffers.
