# RemoteCompose 3D Protocol

This document specifies the 3D operations of a RemoteCompose document: their wire layout, what
writers and players must do with them, and how the format grows. It extends
[PROTOCOL_SPEC.md](PROTOCOL_SPEC.md), which defines the data types and the operation framing.

Every section is marked:
- **Normative**: every writer and player follows it. "Must", "must not" and "may" mean what they
  say.
- **Informative**: background, advice, and what the reference player does where players may
  differ.

The reference player is the pure-Java software renderer, `JavaPaint3DContext`, driven by
`AndroidPaintContext`. It defines the *semantics* of a 3D document, not its exact pixels. Another
engine may light, interpolate, filter and antialias differently, but it must agree with every
normative rule here: which way is up, which faces are front, which surface is in front, what
`0xFFFF0000` means.

## 1. Overview (Informative)

3D drawing runs inside 2D drawing. A canvas's draw operations mix 3D operations with 2D ones, and
all of them run in document order, every frame:

```
clearDepth3D                     start a scene: every pixel's depth is "far"
camera3D                         projection and view; resets the model matrix
lights3D                         the whole light set
meshPrimitive3D  id=1 sphere     define (or replace) mesh 1
paint color                      the 2D paint color is the mesh's base color
matrix3D rotate                  post-multiply the model matrix
drawMesh3D  mesh=1 smooth        rasterize mesh 1 into the scene
```

The 3D state (camera, model matrix, lights, material, depth bias, texture) lasts until an
operation changes it. Meshes are cached by id and stay defined until they're replaced.

## 2. Conventions (Normative)

| Topic | Rule |
| :--- | :--- |
| Handedness | Right-handed: +X right, +Y up, −Z forward. In eye space the camera looks down −Z. |
| Matrices | 4×4, column-major `float[16]`. A vertex goes to clip space as `projection × view × model × v`. |
| Composition | Matrix operations post-multiply the model matrix (`glTranslate` semantics): the operation written last applies to the vertices first. |
| Front faces | A triangle is front-facing when its vertices run counter-clockwise in NDC (x right, y up), or clockwise when the draw sets `MODE_FRONT_CW`. Back faces are culled unless the draw sets `DOUBLE_SIDED`. Every built-in primitive winds counter-clockwise as seen from outside. |
| Depth | NDC z is in [−1, 1]. Window z = NDC z × 0.5 + 0.5, in [0, 1]; smaller is closer. A fragment passes the depth test when its window z is smaller than the stored depth. Clearing sets the depth to farther than any fragment. |
| Viewport | NDC x = −1 is the left edge of the 3D viewport and NDC y = +1 its top edge (§4.5). |
| Angles | Radians everywhere: field of view, rotations, primitive angles, sweep twists. The 2D rotate takes degrees; the 3D operations never do. |
| Colors | Packed colors are ARGB ints, `0xAARRGGBB`. The infinite plane's line color is 4 floats, A, R, G, B, each in [0, 1]. |
| Texture coordinates | u runs left to right and v runs bottom to top: v = 0 is the bitmap's **bottom** row and v = 1 its top row. |
| Units | Positions are in model units. Line widths are in pixels. Depth bias is in window-z units. |

## 3. Profiles and writer checks (Normative)

### 3.1 Profiles

The 3D opcodes exist only in the experimental operation profiles of API level 7 and up. A
document declares the EXPERIMENTAL bit in its profiles, for example `513` (ANDROIDX |
EXPERIMENTAL) or `257` (WIDGETS | EXPERIMENTAL). A reader working in another profile rejects the
opcodes as unknown operations.

A paint context without 3D support, one that doesn't implement `Paint3DContext`, skips every 3D
operation.

### 3.2 Writer checks

`RemoteComposeWriter3D` (`writer.getRc3DWriter()`) rejects values that no player defines yet with
an `IllegalArgumentException`:
- bit 30 of a mode, mesh id or bitmap id;
- reserved mode bits 4–7, 17–29 and 31;
- `SET_CAMERA_3D` projections other than 0 and 1;
- `MATRIX_3D_OP` sub-ops other than 0–6, and push or pop with arguments;
- `SET_LIGHTS_3D` kinds other than 0–2, and type-word bits 16–31;
- `MESH_PRIMITIVE_3D` types other than 0–23, and flag bits 9–31;
- `MESH_EXPRESSION_3D` surface types other than 0–3, and flag bits 2–31;
- a light set whose colors and parameters don't match its type words.

## 4. Scene model (Normative)

### 4.1 State

| State | Set by | Initial value | Notes |
| :--- | :--- | :--- | :--- |
| Projection and view | `SET_CAMERA_3D` | identity | Never stacked. |
| Model matrix | `MATRIX_3D_OP`; `SET_CAMERA_3D` resets it to identity | identity | Saved and restored by push and pop (§6.3). |
| Light set | `SET_LIGHTS_3D`, which replaces the whole set | the default headlight (§6.1) | |
| Material | `PAINT_3D_STATE` MATERIAL | specular 0, shininess 32 | |
| Depth bias | `PAINT_3D_STATE` DEPTH_BIAS | 0, 0 | |
| Texture | `SET_TEXTURE_3D` | none | |
| Base color | the 2D paint color | | Its alpha is the mesh's alpha. |
| Meshes | `DEFINE_MESH_3D`, `MESH_PRIMITIVE_3D`, `MESH_EXPRESSION_3D` | none | One id space. |

- `SET_CAMERA_3D` sets the model matrix to identity, and leaves the push stack (§6.3) alone. A
  document that sets its camera and then its model transforms in every frame therefore draws the
  same picture every frame.
- *(Informative)* The reference player keeps all of this state across frames. The operations run
  every frame, so a document normally sets it again before it draws.

### 4.2 Scenes

1. A scene starts at `clearDepth3D` (`PAINT_3D_STATE` sub-op 0) and ends at the next
   `clearDepth3D` or at the end of the frame. A document clears depth before the first 3D draw of
   each frame.
2. The draws of a scene share one depth buffer, so they hide each other by depth, whatever their
   order.
3. 2D drawn between two 3D draws lands in document order: over the 3D drawn before it, and under
   the 3D drawn after it.
4. The scene is composited over the canvas with src-over. Where the scene draws nothing, the
   canvas shows through.
5. **One engine per scene.** All the draws of a scene should use the same engine hint (mode bits
   1–3); a wireframe draw counts as software. Draws on different engines don't share a depth
   buffer: a player draws each one over what is already on the canvas, in document order.
6. **Engine substitution.** The engine hint is a hint. A player may draw any draw with any engine
   that honours the draw's flags and the normative rules of this document. The software engine
   honours all of them, so it is always a valid choice, and players fall back to it.

### 4.3 Meshes

- A mesh id is an int. It names an entry in the player's mesh table, a namespace of its own: it's
  neither a document id nor a variable.
- Defining an id replaces the mesh it held. A mesh stays defined across frames until it's
  replaced: `MESH_PRIMITIVE_3D` sends its mesh to the renderer only when it's first painted and
  when one of its variables changes.
- Drawing an id that holds no mesh draws nothing.
- Players skip a triangle with an index outside the vertex range.
- A mesh without normals is shaded flat, even in a smooth mode.

### 4.4 Engines on Android (Informative)

| Engine hint | Android player |
| :--- | :--- |
| 0 software | The software rasterizer, with a depth buffer. A run of 3D draws is composited with one blit. |
| 1 canvas | `Canvas.drawVertices` per draw, clipped to the viewport. No depth buffer: each draw's triangles are sorted back to front, and draws paint in document order. |
| 2 drawmesh | `Canvas.drawMesh` per draw on API 34+, otherwise software. A `TRANSPARENT` draw uses `Canvas.drawVertices`. |
| 3 gl | Software. |
| 4 drawmesh-zbuf | On API 34+, the triangles of a run of draws are sorted back to front together and painted with one `Canvas.drawVertices` call: a painter's sort, not a depth buffer. Otherwise software. |
| 5 canvas-zbuf | Software. |
| 6, 7 | Reserved. Software. |

Wireframe draws always use software. The canvas engines (1, 2 and 4) paint an infinite plane as
one polygon plus its grid lines (§6.4) when it's drawn; with drawmesh-zbuf it lands under the
pending batch. Mode bits 12 and up don't select the engine.

### 4.5 Viewport (Informative)

NDC maps onto the 3D viewport, whose top-left corner is the canvas origin. The reference player
uses the painted component's size, minus its padding, as the viewport. It falls back to the
canvas size when there is no component (procedural documents), when drawing to an offscreen
bitmap, and when the component is larger than the canvas (scrolling content). Canvas engines are
clipped to the viewport too.

## 5. Operations (Normative)

All 3D operations are paint operations: they run in draw order, each time the canvas paints.

| Opcode | Name | JSON command | Purpose |
| :--- | :--- | :--- | :--- |
| 110 | `DEFINE_MESH_3D` | `defineMesh3D` | Define a mesh from explicit triangles |
| 111 | `SET_CAMERA_3D` | `camera3D` | Set the projection and view |
| 112 | `MATRIX_3D_OP` | `matrix3D` | Change the model matrix |
| 113 | `DRAW_MESH_3D` | `drawMesh3D` | Draw a mesh |
| 114 | `PAINT_3D_STATE` | `clearDepth3D`, `material3D`, `depthBias3D` | Clear depth, set the material or the depth bias |
| 115 | `SET_LIGHTS_3D` | `lights3D` | Replace the light set |
| 116 | `VECTOR_EXPRESSION` | - | A vector-valued expression (not 3D specific) |
| 117 | `MESH_EXPRESSION_3D` | `meshExpression3D` | Define a mesh from expressions |
| 118 | `SET_TEXTURE_3D` | `texture3D` | Bind a texture |
| 120 | `MESH_PRIMITIVE_3D` | `meshPrimitive3D` | Define a mesh from a parametric primitive |

Notation for the field tables:
- **int** is a 32-bit signed big-endian integer; **float** a 32-bit IEEE 754 float.
- **float V** may hold a NaN-encoded variable id (see [PROTOCOL_SPEC.md](PROTOCOL_SPEC.md)). The
  operation reads the variable's current value, and updates when it changes.
- **float[n]** is n consecutive floats.
- Read caps are the reference reader's sanity limits, given per field: a count above its cap, or a
  negative count, makes the document corrupt (§8).

### 5.1 `DEFINE_MESH_3D` (110)

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `id` | int | Mesh id. Bit 30 is the advanced bit (§7.1): readers of this version refuse the document. |
| `indexCount` | int | A multiple of 3. Cap 3,000,000. |
| `indices` | int[indexCount] | Vertex indices, 3 per triangle. |
| `vertexFloatCount` | int | 3 × the vertex count. Cap 3,000,000. |
| `vertices` | float[vertexFloatCount] | x, y, z per vertex. |
| `normalFloatCount` | int | 0 (no normals) or `vertexFloatCount`. |
| `normals` | float[normalFloatCount] | nx, ny, nz per vertex. |
| `uvFloatCount` | int | 0 (no UV) or 2 × the vertex count. |
| `uv` | float[uvFloatCount] | u, v per vertex. |

The floats are literal: this operation takes no variables. Without normals, players shade the
mesh flat. Readers treat any other index, vertex, normal or UV count as corrupt.

### 5.2 `SET_CAMERA_3D` (111)

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `projection` | int | 0 perspective, 1 orthographic. Other values are reserved. |
| `projectionCount` | int | 4 for perspective, 6 for orthographic. Cap 256. |
| `projectionParams` | float V[projectionCount] | Perspective: `fovY` (vertical, radians), `aspect` (width / height), `near`, `far`. Orthographic: `left`, `right`, `bottom`, `top`, `near`, `far`. |
| `viewCount` | int | 9. Cap 256. |
| `viewParams` | float V[viewCount] | `eye` x, y, z; `center` x, y, z; `up` x, y, z (`gluLookAt`). |

- The operation sets the projection and the view, and resets the model matrix to identity.
- Players use `aspect` as written. A writer that wants undistorted output passes the viewport's
  aspect.
- Players ignore parameters beyond the ones the projection and the view take. With too few in
  either list, the operation does nothing.
- Writers refuse a reserved projection (`Unknown setCamera3D projection <n> (expected 0 =
  perspective or 1 = ortho)`). What players draw for one is not defined. *(Informative)* The
  reference player draws it as orthographic, given at least 6 parameters.

### 5.3 `MATRIX_3D_OP` (112)

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `sub` | int | The sub-op, below. |
| `argCount` | int | Cap 256. |
| `args` | float V[argCount] | |

| Sub | Constant | Arguments | Effect on the model matrix M |
| :--- | :--- | :--- | :--- |
| 0 | `M3_IDENTITY` | none | M = I |
| 1 | `M3_TRANSLATE` | x, y, z | M = M × translate(x, y, z) |
| 2 | `M3_SCALE` | x, y, z | M = M × scale(x, y, z) |
| 3 | `M3_ROTATE_AXIS` | angle (radians), ax, ay, az | M = M × rotate(angle, axis), counter-clockwise looking from the axis tip toward the origin |
| 4 | `M3_MULTIPLY` | 16 floats, column-major | M = M × A |
| 5 | `M3_PUSH` | none | Save M (§6.3) |
| 6 | `M3_POP` | none | Restore M (§6.3) |

- These sub-ops change the model matrix only: `M3_IDENTITY` doesn't reset the camera.
- Players ignore arguments beyond the ones a sub-op takes. A sub-op with too few arguments, and an
  unknown sub-op, does nothing.
- Players normalize the rotation axis. A zero axis does nothing.

### 5.4 `DRAW_MESH_3D` (113)

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `meshId` | int | The mesh to draw. |
| `mode` | int | Shading, engine hint and flags, below. |

The draw uses the current camera, model matrix, light set, material, depth bias, texture and 2D
paint color. The integer fields are literal.

| Bits | Name | Meaning |
| :--- | :--- | :--- |
| 0 | `SMOOTH` | 0: flat, one color per triangle. 1: smooth (Gouraud), lit per vertex. |
| 1–3 | Engine hint | `(mode >> 1) & 7`: 0 software, 1 canvas, 2 drawmesh, 3 gl, 4 drawmesh-zbuf, 5 canvas-zbuf, 6–7 reserved. A hint (§4.2). |
| 4–7 | | Reserved; must be 0. |
| 8 | `MODE_WIREFRAME` | Hidden-line wireframe (§6.6). Forces the software engine. |
| 9–11 | `WIRE_EDGE0`–`WIRE_EDGE2` | With wireframe, draw only these edges of each triangle (v0→v1, v1→v2, v2→v0). None set means all three. |
| 12 | `MODE_TRANSPARENT` | Blend src-over; depth test on, depth writes off (§6.7). |
| 13 | `MODE_GPU` | Prefer a GPU engine if the player has one. The result may differ from the software reference within normal GPU tolerances. |
| 14 | `MODE_PBR` | The material is PBR (§6.2). |
| 15 | `MODE_DOUBLE_SIDED` | No back-face culling; back faces are lit with the reversed normal (§6.7). |
| 16 | `MODE_FRONT_CW` | Clockwise triangles are front-facing, like `glFrontFace(GL_CW)`. Culling, the `DOUBLE_SIDED` normal reversal and flat face normals follow. For meshes wound clockwise or drawn through a mirroring model matrix. Infinite planes ignore it. |
| 17–29 | | Reserved for future flags. Players ignore bits they don't know. |
| 30 | `MODE_ADVANCED` | The advanced bit (§7.1): readers of this version refuse the document. |
| 31 | | Reserved. |

*(Informative)* [DRAW_MESH_3D_MODE.md](DRAW_MESH_3D_MODE.md) proposes the contract for bits 17–31
(optional versus required flags, the legacy status of the engine hint, and a header tag) for all
players; once accepted it supersedes this table's rules for those bits.

Named modes:

| Value | Constant | JSON `mode` |
| :--- | :--- | :--- |
| 0 / 1 | `MODE_SOFTWARE_FLAT` / `MODE_SOFTWARE_SMOOTH` | `software-flat` / `software-smooth` |
| 2 / 3 | `MODE_CANVAS_FLAT` / `MODE_CANVAS_SMOOTH` | `canvas-flat` / `canvas-smooth` |
| 4 / 5 | `MODE_DRAWMESH_FLAT` / `MODE_DRAWMESH_SMOOTH` | `drawmesh-flat` / `drawmesh-smooth` |
| 6 / 7 | none | `gl-flat` / `gl-smooth` |
| 8 / 9 | `MODE_DRAWMESH_ZBUF_FLAT` / `MODE_DRAWMESH_ZBUF_SMOOTH` | `drawmesh-zbuf-flat` / `drawmesh-zbuf-smooth` |
| 10 / 11 | none | `canvas-zbuf-flat` / `canvas-zbuf-smooth` |
| 256 | `MODE_SOFTWARE_WIREFRAME` | `"wireframe": true` |

### 5.5 `PAINT_3D_STATE` (114)

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `sub` | int | The sub-op, below. |
| `paramCount` | int | Cap 256. |
| `params` | float V[paramCount] | |

| Sub | Name | Parameters | Effect |
| :--- | :--- | :--- | :--- |
| 0 | `CLEAR_DEPTH` | none | Starts a scene (§4.2) and empties the model-matrix stack (§6.3). |
| 1 | `MATERIAL` | specular, shininess, then extras | Sets the material (§6.2). |
| 2 | `DEPTH_BIAS` | constant, slope | Sets the depth bias (§6.8). |

Players ignore an unknown sub-op, and a sub-op with too few parameters does nothing. Readers keep
MATERIAL extras so the operation writes back unchanged, and otherwise ignore them.

### 5.6 `SET_LIGHTS_3D` (115)

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `lightCount` | int | n. Cap 256. |
| `type`, `color` | (int, int) × n | Per light: its type word, then its color (ARGB; the alpha is ignored). |
| `paramCount` | int | The sum over the lights of (4 + extra). Cap max(1,024, n × 259). |
| `params` | float V[paramCount] | The lights' parameters, in light order. |

Type word: the kind in bits 0–7 (`LIGHT_KIND_MASK`), and the number of extra parameters in
bits 8–15 (`(type >> LIGHT_EXTRA_SHIFT) & LIGHT_EXTRA_MASK`). Light i has 4 + extra(i) parameters.
Bits 16–31 are reserved: writers write 0, and players ignore them.

| Kind | Constant | Parameters | Meaning |
| :--- | :--- | :--- | :--- |
| 0 | `LIGHT_DIRECTIONAL` | dx, dy, dz, intensity | The direction the light travels, in world space. |
| 1 | `LIGHT_POINT` | x, y, z, intensity | The light's position in world space. No attenuation. |
| 2 | `LIGHT_AMBIENT` | 0, 0, 0, intensity | Light added to every surface. |

- The operation replaces the whole light set. An empty set means no directional or point light:
  surfaces get the default ambient light only (§6.1).
- World space is the space the view matrix maps from: the model matrix doesn't move lights.
- Players drop lights of an unknown kind and ignore extra parameters, so the renderer only sees
  kinds 0–2 with 4 parameters each. A future spot light is a point light with extra cone
  parameters, so a player without spot lights shows a point light.
- If the parameters run out before light i has its 4 base parameters, players drop light i and
  every light after it.
- *(Informative)* The reference player keeps the first 32 directional and point lights. It adds up
  all the ambient lights.

### 5.7 `VECTOR_EXPRESSION` (116)

Not a 3D operation, but 3D documents use it to compute vectors such as positions and directions.

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `id` | int | The first output variable id. |
| `dimension` | byte | Number of components. |
| `flags` | byte | Reserved. |
| `length` | short | Number of floats in the program. |
| `program` | float[length] | The vector RPN program (`VectorRpn`). |

It writes its components to the consecutive variables `id` to `id + dimension − 1`, which any
operation can read as ordinary floats. A non-finite component is stored as 0.

### 5.8 `MESH_EXPRESSION_3D` (117)

Defines a grid mesh whose vertices come from expressions, evaluated each time the operation
paints.

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `id` | int | Mesh id. |
| `type` | int | Surface family, below. |
| `flags` | int | Bit 0: `FLAG_FLIP_WINDING`. Bit 1: `FLAG_FLIP_NORMALS`. Bits 2–31 are reserved. |
| `paramCount` | int | Cap 4,096. |
| `params` | float V[paramCount] | The family's parameters, below. |
| `positionCount` | int | Number of position expressions. Cap 8. |
| per expression | int, float[] | Its length (cap 4,096), then its RPN program. |
| `normalCount` | int | 0, or 3 expressions: nx, ny, nz. Cap 8. |
| per expression | int, float[] | As above. |
| `uvCount` | int | 0, or 2 expressions: u, v. Cap 8. |
| per expression | int, float[] | As above. |

The programs are float expressions in which `VAR1` and `VAR2` (the RPN variables also used by
path expressions) hold the grid parameters a and b:

| Type | Family | Parameters | a, b | Positions |
| :--- | :--- | :--- | :--- | :--- |
| 0 | `SURFACE_GENERAL` | uMin, uMax, uCount, vMin, vMax, vCount | a ∈ [uMin, uMax], b ∈ [vMin, vMax] | 3 expressions: x, y, z |
| 1 | `SURFACE_HEIGHT_FIELD` | xMin, xMax, uCount, zMin, zMax, vCount, yMin, yMax | a = x ∈ [xMin, xMax], b = z ∈ [zMin, zMax] | 1 expression: y, clamped to [yMin, yMax] |
| 2 | `SURFACE_SPHERE` | radius, uCount, vCount | a = longitude ∈ [0, 2π], b = latitude ∈ [0, π] from +Y | 1 expression: displacement d; position = (radius + d) × (sin b cos a, cos b, sin b sin a) |
| 3 | `SURFACE_CYLINDER` | radius, height, uCount, vCount | a = angle ∈ [0, 2π], b ∈ [−height/2, height/2] | 1 expression: displacement d; position = ((radius + d) cos a, b, (radius + d) sin a) |

- The grid has uCount × vCount vertices, spread evenly over the ranges, and 2 triangles per cell.
  The counts are truncated to integers. With a count below 2, or more than 1,000,000 vertices,
  the operation defines nothing.
- Players use the normal expressions when there are at least 3, as written (they aren't
  normalized); otherwise normals come from finite differences over the grid. They use the UV
  expressions when there are at least 2; otherwise u and v are the grid fractions, from 0 to 1.
- Triangles wind counter-clockwise as seen from the front side, and finite-difference normals
  point to it. Without flags, the front side is:

  | Type | Front side |
  | :--- | :--- |
  | `SURFACE_GENERAL` | Where ∂P/∂b × ∂P/∂a points, when both ranges increase: −Z for (a, b, 0), +Y for (a, 0, b). |
  | `SURFACE_HEIGHT_FIELD` | +Y, when xMin < xMax and zMin < zMax. |
  | `SURFACE_SPHERE` | The inside, for a positive radius. |
  | `SURFACE_CYLINDER` | The outside, for a positive height. |

- `FLAG_FLIP_WINDING` reverses the winding and the finite-difference normals, which swaps the
  sides. Normal expressions are used as written. `FLAG_FLIP_NORMALS` reverses every normal,
  normal expressions included, and keeps the winding. The two flags combine. Players ignore the
  other bits.
- An unknown type, too few parameters (6, 8, 3 and 4 for types 0 to 3) or too few position
  expressions defines nothing. So does a program that fails to evaluate.
- Each program holds at most 128 floats, the reference evaluator's limit. Readers accept up to
  4,096, but a longer program fails to evaluate.
- *(Informative)* Every paint re-evaluates every vertex: keep grids small.

### 5.9 `SET_TEXTURE_3D` (118)

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `bitmapId` | int | A bitmap defined in the document, or 0 to clear the texture. Bit 30 is the advanced bit (§7.1): readers of this version refuse the document. |

The texture applies to the draws that follow, until changed (§6.5).

### 5.10 `MESH_PRIMITIVE_3D` (120)

Defines a mesh that the player builds from a parametric primitive.

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `id` | int | Mesh id. |
| `type` | int | Primitive type, below. |
| `segments` | float V | Tessellation, per type (§5.10.2). |
| `flags` | int | §5.10.3. |
| `channelCount` | int | Number of parameter channels; most types use 1. Cap 64. |
| per channel | int, float V[] | Its length (cap 1,048,576), then its floats. |

Players rebuild the mesh when a variable it uses changes. *(Informative)* The reference player
builds it once, on the first paint, and again only after such a change.

#### 5.10.1 Types

Channel 0 holds the scalar parameters. A trailing `x, y, z` (or `cx, cy, cz`) translates the shape.

| Type | Name | Channel 0 (and other channels) | Native axis |
| :--- | :--- | :--- | :--- |
| 0 | `SPHERE` | radius, x, y, z | Y |
| 1 | `CYLINDER` | radius, x1, y1, z1, x2, y2, z2 (the axis end points) | none |
| 2 | `CONE` | radius, height, x, y, z (the base center; apex toward +Y) | Y |
| 3 | `CUBE` | dimX, dimY, dimZ, x, y, z | Y |
| 4 | `ROUNDED_CUBE` | dimX, dimY, dimZ, x, y, z, cornerRadius | Y |
| 5 | `SPHERICAL_SECTOR` | radius, angle, x, y, z | Y |
| 6 | `SPHERICAL_DOME` | radius, angle, x, y, z | Y |
| 7 | `TUBE` | radius, [ringsPerSpan], then x, y, z per point (at least 2) | none |
| 8 | `CAP_TUBE` | as `TUBE`, with both ends closed | none |
| 9 | `PROFILE_TUBE` | pointCount, [ringsPerSpan], x, y, z per point, then radii r0, r1, … (at least 1) | none |
| 10 | `TORUS` | majorRadius, minorRadius, x, y, z (the ring in XZ) | Y |
| 11 | `PLANE` | width, height, x, y, z (in XY, facing +Z) | Z |
| 12 | `EXTRUDE_CIRCLE` | radius, depth, cx, cy, cz | Z |
| 13 | `EXTRUDE_SECTOR` | radius, startAngle, sweepAngle, depth, cx, cy, cz | Z |
| 14 | `EXTRUDE_SEGMENT` | radius, startAngle, sweepAngle, depth, cx, cy, cz | Z |
| 15 | `EXTRUDE_ARC` | innerRadius, outerRadius, startAngle, sweepAngle, depth, cx, cy, cz | Z |
| 16 | `EXTRUDE_ROUNDED_RECT` | width, height, cornerRadius, depth, cx, cy, cz | Z |
| 17 | `EXTRUDE_SQUIRCLE` | radius, exponent, depth, cx, cy, cz | Z |
| 18 | `EXTRUDE_PATH` | depth, cx, cy, cz, bevel, contourCount, then per contour: pointCount, x, y per point | Z |
| 19 | `LATHE` | cx, cy, cz; channel 1: radius, y per profile point, revolved around Y | Y |
| 20 | `SWEEP` | closed, capStart, capEnd, cx, cy, cz; channel 1: section x, y per point; channel 2: path x, y, z per point; channel 3 (optional): a scale per path point; channel 4 (optional): a twist per path point, in radians | none |
| 21 | `HELIX` | coilRadius, tubeRadius, pitch, turns, cx, cy, cz (`pitch` is the rise per turn) | Y |
| 22 | `ICOSPHERE` | radius, cx, cy, cz | Y |
| 23 | `INFINITE_PLANE` | cx, cy, cz, spacing, lineWidth, lineA, lineR, lineG, lineB (§6.4) | Y |

- `[ringsPerSpan]` is present only with `FLAG_TUBE_PATH_DENSITY`.
- `EXTRUDE_*` shapes are a 2D profile in the XY plane, extruded `depth` along Z. In
  `EXTRUDE_PATH`, nested contours become holes; the bevel is at most 90% of half the depth.
- `SWEEP` keeps its section from spinning along the path (parallel-transport frames). Its
  booleans are 1 or 0. Channel 4 needs channel 3, because channels are positional.
- Players ignore parameters beyond the ones a type takes. A type with too few parameters, an
  unknown type, and parameters a type can't be built from (such as a tube with fewer than 2
  points) define nothing, without throwing.
- The center of `LATHE`, `HELIX` and `ICOSPHERE`, and every channel 0 value of `SWEEP`, is
  optional: a missing value is 0.

#### 5.10.2 `segments`

`segments` means different things for different types. For most types it's a count, computed as
`count = max(min, segments > 0 ? round(segments) : default)`, so 0 (or less) selects the default.

| Types | `segments` counts | Default | Minimum |
| :--- | :--- | :--- | :--- |
| `SPHERE` | slices around Y; stacks = max(2, round(slices / 2)) | 24 | 3 |
| `SPHERICAL_SECTOR`, `SPHERICAL_DOME` | slices; stacks = max(2, round(slices / 3)) | 24 | 3 |
| `CYLINDER`, `CONE`, `EXTRUDE_CIRCLE`, `LATHE` | sides around the axis | 24 | 3 |
| `TORUS` | segments around the ring; around the tube: max(3, round(segments × \|minorRadius / majorRadius\|)) | 24 | 3 |
| `ROUNDED_CUBE` | divisions per rounded corner | 4 | 1 |
| `EXTRUDE_SECTOR`, `EXTRUDE_SEGMENT`, `EXTRUDE_ARC` | arc divisions | 24 | 1 |
| `EXTRUDE_ROUNDED_RECT` | divisions per corner | 6 | 1 |
| `EXTRUDE_SQUIRCLE` | perimeter samples | 48 | 8 |
| `TUBE`, `CAP_TUBE`, `PROFILE_TUBE` | sides of the cross-section | 14 | 3 |
| `HELIX` | sides of the tube | 12 | 3 |
| `ICOSPHERE` | the **subdivision level**, not a count (below) | 2 | 0 |
| `CUBE`, `PLANE`, `EXTRUDE_PATH`, `SWEEP`, `INFINITE_PLANE` | unused | | |

- `ICOSPHERE`: the level is `segments > 0 ? round(segments) : 2`, clamped to 0–4, and the mesh has
  20 × 4^level triangles, so only factor-of-four steps exist. Because 0 selects the default, level
  0 needs a value in (0, 0.5), such as 0.25.
- Tube rings along the path: by default each span gets about (arc length / cross-section edge
  length) rings, so the quads stay square. `FLAG_TUBE_CURVATURE_DENSITY` places them by curvature
  instead, and `ringsPerSpan` (with `FLAG_TUBE_PATH_DENSITY`) overrides both. With
  `FLAG_TUBE_LEGACY | FLAG_SPLINE`, `segments` is also the number of Catmull-Rom samples per span,
  unless `ringsPerSpan` is given.

#### 5.10.3 Flags

| Bits | Name | Meaning |
| :--- | :--- | :--- |
| 0 | `FLAG_SPLINE` | With `FLAG_TUBE_LEGACY`: smooth the points with a Catmull-Rom spline. |
| 1 | `FLAG_TUBE_LEGACY` | Tubes: the older parallel-transport sweep over the raw points, instead of the default monotonic spline. |
| 2 | `FLAG_TUBE_PATH_DENSITY` | Tubes: channel 0 carries `ringsPerSpan` after its first value. |
| 3 | `FLAG_TUBE_CAP` | `PROFILE_TUBE`: close both ends. |
| 4–5 | UV mode | 0 `UV_NONE`, 1 `UV`, 2 `UV2`, 3 `UV3`. Any non-zero value generates the type's UV mapping; `UV2` and `UV3` are reserved for other mappings and currently equal `UV`. |
| 6–7 | Axis | 0 `AXIS_NATIVE`, 1 `AXIS_X`, 2 `AXIS_Y`, 3 `AXIS_Z`, below. |
| 8 | `FLAG_TUBE_CURVATURE_DENSITY` | Tubes: place rings by curvature (§5.10.2). Ignored with `FLAG_TUBE_LEGACY`. |
| 9–31 | | Reserved. |

**Axis.** Each type is built around its native axis (§5.10.1). A non-native axis rigidly turns the
finished mesh a quarter turn about its pivot, the shape's center (the base center for `CONE`), so
the native axis points along the chosen one. Winding and UV don't change. Types without a native
axis ignore the field.

| From | To | Quarter turn |
| :--- | :--- | :--- |
| +Y | +X | about Z: +Y → +X, +X → −Y |
| +Y | +Z | about X: +Y → +Z, +Z → −Y |
| +Z | +X | about Y: +Z → +X, +X → −Z |
| +Z | +Y | about X: +Z → +Y, +Y → −Z |

**UV.** With a UV mode, every type generates coordinates in [0, 1] that read unmirrored from
outside, with v = 0 at the bottom:

| Types | Mapping |
| :--- | :--- |
| `SPHERE`, `ICOSPHERE`, sector and dome caps | Longitude and latitude, like a globe. |
| `PLANE`, `CUBE`, `ROUNDED_CUBE` | The unit square on each face. |
| `CYLINDER`, `CONE`, `LATHE` | u once around, v along the axis or the profile. Flat caps are disks inscribed in the unit square. |
| `TORUS` | u around the ring, v around the tube. Mirrored, for compatibility. |
| `EXTRUDE_*` | Caps planar over the profile's bounding box (the back cap mirrored so it reads correctly from behind). Walls: u is the perimeter fraction, v goes from 0 at the back to 1 at the front. |
| Tubes, `SWEEP`, `HELIX` | u is the arc-length fraction along the path, v goes around the section. |
| `INFINITE_PLANE` | None: the plane is never textured (§6.4). The fallback square maps the unit square, u along +X and v along −Z. |

## 6. Rendering (Normative)

### 6.1 Lighting

One lighting model applies in every engine. For a surface point P with normal N:

```
N'       = N for a front-facing triangle, −N for a back-facing one (drawn with DOUBLE_SIDED)
L        = −normalize(d) for a directional light; normalize(position − P) for a point light
diffuse  = Σ  max(0, N'·L) × color × intensity              over directional and point lights
specular = Σ  s × max(0, N'·H)^shininess × color × intensity, where N'·L > 0,
              H = normalize(L + V), V = the direction from P to the eye; only when s > 0
ambient  = Σ  color × intensity over ambient lights, or 0.2 white when the set has none
rgb      = base.rgb × (ambient + diffuse) + specular,      each channel clamped to [0, 1]
alpha    = base.alpha
```

- `base` is the 2D paint color, times the texel color when a texture applies (§6.5). `s` and
  `shininess` come from the material (§6.2).
- In smooth mode, N is the vertex normal as given: players don't turn normals toward the viewer or
  re-orient them to the winding. A whole back-facing triangle uses −N; per-vertex normals aren't
  flipped one by one.
- In flat mode, N' is the triangle's face normal on the side that faces the viewer.
- Point lights have no attenuation; falloff is reserved for light extras (§7.2).
- **Default light.** Until `SET_LIGHTS_3D` runs, the light set is one directional headlight from
  the eye (eye-space direction toward the light (0, 0, 1)), of white intensity 0.8, plus the
  default ambient of 0.2. An empty light set gives the default ambient only.
- *(Informative)* Where the light lands is normative; how precisely is not. The reference player
  lights per vertex in smooth mode. In flat mode it lights each triangle once, at its centroid,
  or, with a highlight (s > 0), at each vertex with the face normal. It interpolates colors
  affinely in screen space. Other engines may light per pixel.
- *(Informative)* A face turned away from every light gets ambient light only.

### 6.2 Material and the PBR contract

`MATERIAL` = `[specular, shininess, …extras]`:
- `specular` (s) scales a Blinn-Phong highlight in each light's color. 0, the default, means matte:
  no highlight.
- `shininess` is the highlight exponent: higher is tighter and glossier. A value of 0 or less
  counts as 1. The default is 32.
- The extras are reserved for PBR fields. Readers keep them, and players that don't know
  them ignore them.

The PBR contract:
1. Writers always write the two base fields, even for a PBR material.
2. A renderer without PBR renders the base material, so a PBR object shows as shiny gray (or in
   its paint color). `MODE_PBR` changes nothing for it.
3. A PBR renderer given a material without PBR fields uses the paint color as the base color,
   metallic 0, and the roughness derived from `shininess`: α = sqrt(2 / (shininess + 2)), where α
   is the GGX width (perceptual roughness = sqrt(α)).
4. No PBR field is defined yet. They will be defined as MATERIAL extras, never as a new opcode.

### 6.3 Push and pop

- `M3_PUSH` saves a copy of the model matrix; `M3_POP` restores the most recent copy.
  Neither takes arguments.
- They save and restore the **model matrix only**. The camera (projection and view) is never
  stacked, and nor are the lights, material, depth bias or texture.
- The stack holds 32 matrices (`M3_STACK_DEPTH`). A push onto a full stack does nothing, and so
  does the pop that matches it. A pop on an empty stack does nothing.
- `clearDepth3D` and the end of the frame empty the stack; the model matrix itself keeps its
  value.
- The 2D matrix save and restore never touch any 3D state.
- *(Informative)* The reference player logs overflows and underflows in debug builds.

### 6.4 Infinite plane

`MESH_PRIMITIVE_3D` type 23 defines a mesh id as an infinite plane, with an optional grid.
It's drawn with `DRAW_MESH_3D` like any mesh: the model matrix places it, and the paint color,
material, lights and depth bias apply to it.

| Parameter | Meaning |
| :--- | :--- |
| cx, cy, cz | A point on the plane, in model space. |
| spacing | The grid cell size, in model units. 0 means no grid. |
| lineWidth | The grid line width, in pixels. |
| lineA, lineR, lineG, lineB | The grid line color, each in [0, 1]. |

Rules:
1. The plane's normal is +Y, or the axis that flag bits 6–7 select. Its front is the side the
   normal points to. The back is culled unless the draw sets `DOUBLE_SIDED`, which lights it with
   the reversed normal. The plane ignores `FRONT_CW`.
2. Players pad missing trailing parameters with the defaults `0, 0, 0, 0, 1, 1, 0.5, 0.5, 0.5`:
   no grid, 1-pixel lines, opaque mid gray. They ignore parameters after the ninth.
3. The plane writes depth, unless the draw sets `TRANSPARENT`, so meshes intersect it correctly and
   draw order handles occlusion. The depth test and bias are the same as for a mesh.
4. Grid lines are the lines u = k × spacing and v = k × spacing through the center point C, for
   every integer k, where u and v run along the in-plane axes U and V:

   | Normal | U | V |
   | :--- | :--- | :--- |
   | +Y | +X | −Z |
   | +X | −Y | −Z |
   | +Z | +X | +Y |

   The lines blend over the plane src-over with the line alpha. They are antialiased, and fade
   out as their spacing on screen shrinks toward the line width, so there is no moiré. There is no
   grid when `spacing` or `lineWidth` is 0 or less, or the line alpha is 0.
5. The plane reaches the horizon: players don't cut it at the far plane. An orthographic camera
   has no horizon. A camera that sees the plane exactly edge-on draws nothing.
6. A wireframe draw draws only the grid, in the line color, and writes the depth of the whole
   plane (no depth with `TRANSPARENT`). Without a grid, it only writes depth.
7. The plane has no UV, so it's never textured.
8. A player without an infinite-plane path draws a finite square instead: 2,000 units across
   (`Primitive3D.INFINITE_PLANE_FALLBACK_HALF_SIZE` is 1,000), centered on C, facing the normal,
   without a grid. That is what the default `Paint3DContext.defineInfinitePlane3D` does.

*(Informative)* The software engine fills the plane with its own span loop over the rows it
covers. Its window depth is affine in screen space and tends to f / (f − n) at the horizon of a
perspective camera; pixels within 1e-4 pixels of the horizon are skipped. A non-finite parameter
takes its default. Directional and ambient light is exact. Point lights and specular are shaded at
the ends of span segments at most 32 pixels long, and interpolated linearly in between; flat and
smooth modes look the same. Each family of grid lines fades linearly from full strength at an
on-screen spacing of 2m pixels to nothing at m, where m = max(2, lineWidth + 1). Canvas engines
draw the part of the plane inside the view frustum as one polygon of at most 8 vertices, plus the
projected grid lines in 5 fade levels.

### 6.5 Texturing

- `SET_TEXTURE_3D` binds a bitmap to the draws that follow; 0 clears it.
- A draw uses the texture only if its mesh has UV. Otherwise the texture is ignored.
- The texel color multiplies the lit color (§6.1), alpha included.
- v = 0 samples the bitmap's **bottom** row and v = 1 its top row; u = 0 is the left column.
  Coordinates outside [0, 1] are clamped to the edge.
- A texel with alpha 0 is a cutout: nothing is drawn there, and depth isn't written. In a
  `TRANSPARENT` draw, the texel alpha also scales the blend (§6.7).
- Textures never show on wireframe draws (§6.6).
- *(Informative)* The reference player samples the nearest texel, and interpolates UV
  perspective-correctly but lit colors affinely. A canvas engine flips v so that it samples the
  same rows.

### 6.6 Wireframe

`MODE_WIREFRAME` draws hidden-line wireframes:
1. The mesh's front-facing triangles (all of them with `DOUBLE_SIDED`) write depth over their
   **whole area**, without color. A `TRANSPARENT` wireframe skips this step.
2. Then their edges are drawn as depth-tested lines in the paint color, unlit, so edges behind
   nearer surfaces are hidden. The `WIRE_EDGE*` bits select edges.

Consequences:
- A wireframe mesh hides whatever lies behind its faces. To show a wireframe cage around another
  object, draw the cage last.
- A wireframe draw never shows a texture. A textured shell around other objects must be a filled
  mesh, which hides what's inside it unless it's `TRANSPARENT`.
- For a shaded mesh with wireframe edges, draw the mesh shaded, then again in wireframe: both
  draws share the scene's depth buffer.
- Wireframe forces the software engine (§4.4).
- *(Informative)* The reference player draws 1-pixel lines that don't write depth, biased 6e-4
  toward the camera in window z so that edges on visible surfaces pass the depth test. A
  `TRANSPARENT` wireframe blends its lines, so an edge shared by two triangles blends twice.

### 6.7 Transparency and double-sided drawing

- `MODE_TRANSPARENT`: the draw keeps the depth test but doesn't write depth, and blends its
  pixels src-over into the scene. Its alpha is the paint alpha times the texel alpha. The
  document orders the draws: opaque meshes first, then transparent ones from back to front.
- Without `MODE_TRANSPARENT`, a mesh replaces the 3D behind it. *(Informative)* The reference
  player keeps the paint alpha in the scene's pixels, so a translucent mesh still shows the 2D
  content behind the scene.
- `MODE_DOUBLE_SIDED`: back faces aren't culled, and they're lit with the reversed normal.
  It makes a mesh with inconsistent winding visible.
- Canvas engines, which have no depth buffer, give both flags the same meaning, with triangles
  painted back to front (§4.4).

### 6.8 Depth bias

`DEPTH_BIAS` = `[constant, slope]` offsets the window z of every triangle drawn after it:

```
bias = constant + slope × max(|∂z/∂x|, |∂z/∂y|)       window-z units per pixel
```

The bias is added to all 3 vertex depths. Positive values push a surface away from the camera;
negative values pull it closer. The default (0, 0) means no bias. These are `glPolygonOffset`
semantics. *(Informative)* Canvas engines have no depth buffer: there the bias only shifts the
depths they sort triangles by.

> [!WARNING]
> A constant bias is in window z, so its size in world units grows with the square of the
> distance and with the far/near ratio. With near/far 0.4/40, a constant of 0.0018 is about 0.036
> world units at z = 4. With near/far 0.25/80, the same constant is about 0.43 world units at
> z = 11. A value tuned for one camera can make whole objects disappear for another.
>
> - Prefer geometry that doesn't need a bias: separate the surfaces a little, use
>   `DOUBLE_SIDED` for wrongly wound meshes, and use the infinite plane's grid instead of a
>   wireframe floor.
> - Keep the far/near ratio as small as the scene allows.
> - Tune the bias at every camera distance the document uses, and prefer `slope` for coplanar
>   decals.

### 6.9 Clipping and culling

- Players clip triangles that cross the near plane (z ≥ −w in clip space). They never drop a
  triangle whole because one vertex is behind the camera.
- Players drop zero-area triangles.
- *(Informative)* The reference player also clips to a guard band 65,536 pixels beyond each edge
  of the viewport, and keeps clip-space w at least 1e-4 for degenerate projections. It doesn't clip
  against the far plane.

## 7. Extension points (Normative)

The format grows only inside the existing operations: new sub-ops, types, flag bits, fields and
JSON keys. There will be no new 3D opcodes.

### 7.1 The advanced bit

Bit 30 of one field of each of these operations marks an *advanced form* of the operation, whose
layout a later version of this format will define:

| Operation | Field | Reserved for |
| :--- | :--- | :--- |
| `DRAW_MESH_3D` | `mode` bit 30 (`MODE_ADVANCED`) | Instancing, node ids |
| `DEFINE_MESH_3D` | `id` bit 30 | Vertex colors, tangents |
| `SET_TEXTURE_3D` | `bitmapId` bit 30 | Texture slots |

- This version defines no advanced form. Writers refuse the bit (§3.2).
- A reader of this version that meets the bit **refuses the document** with an error, as it does
  for an unknown opcode. It can't know the operation's layout, and it must not render a guess:
  a wrong picture is worse than no picture.
- A later version that defines an advanced form reads the extra fields only when the bit is set,
  so its documents without the bit stay readable by this version.

### 7.2 Other extension points

| Where | How it grows | Reserved for |
| :--- | :--- | :--- |
| `SET_LIGHTS_3D` type word | Extra parameters per light (bits 8–15); new kinds | Spot cones, falloff, shadows |
| `PAINT_3D_STATE` MATERIAL | Extra trailing parameters | PBR fields (§6.2) |
| `DRAW_MESH_3D` mode | Bits 17–29 | Further capability flags |
| `MESH_PRIMITIVE_3D` | Types above 23, flag bits 9 and up | More primitives and options |
| `MESH_EXPRESSION_3D` | Surface types above 3, flag bits 2 and up | More surfaces and options |
| `MATRIX_3D_OP`, `PAINT_3D_STATE` | Sub-ops above 6 and 2 | More state |

## 8. Tolerant reading (Normative)

Players must read every document a later writer can produce, as long as it uses no new opcode:
1. Readers accept parameter counts up to generous sanity limits, and treat negative or larger
   counts as a corrupt document. §5 gives the reference reader's cap for each count.
2. Players ignore parameters beyond the ones a sub-op, type or light needs.
3. A known sub-op or type with too few parameters, an unknown `MATRIX_3D_OP` or `PAINT_3D_STATE`
   sub-op, an unknown primitive or surface type, and an unknown light kind do nothing. They never
   throw, either when read or when painted.
4. Players ignore mode and flag bits they don't know, and refuse the advanced bit.
5. *(Informative)* The reference player logs each of these cases in debug builds.

## 9. Compatibility (Normative)

1. New players read and render every existing document.
2. Old players need not read new documents.
3. The format grows additively inside the existing operations (§7). No new opcode, now or later,
   including for PBR and GPU rendering.

## 10. Expressions (Normative)

- The **V** fields of §5 may hold variables, so geometry, camera, lights and material can animate.
  `DEFINE_MESH_3D` and the integer fields of `DRAW_MESH_3D` and `SET_TEXTURE_3D` take literal
  values only.
- A float expression (opcode `ANIMATED_FLOAT`, class `FloatExpression`) holds at most 32 RPN
  tokens (`Limits.MAX_EXPRESSION_SIZE`), per expression. The reader enforces this cap, and the
  Java writer refuses longer expressions. Writers outside this library must check it too: a
  player rejects a document with a longer expression. Compute constants in the generator rather
  than in the document.
- `MESH_EXPRESSION_3D` programs aren't float expressions, so the 32-token cap doesn't apply. Each
  program holds at most 128 floats, the reference evaluator's limit (§5.8).

## 11. Where it lives (Informative)

| Area | Code |
| :--- | :--- |
| Opcodes | `Operations` (`DEFINE_MESH_3D` … `MESH_PRIMITIVE_3D`) |
| Operations | `operations/d3/`: `DefineMesh3D`, `SetCamera3D`, `Matrix3DOp`, `DrawMesh3D`, `Paint3DState`, `SetLights3D`, `MeshExpression3D`, `SetTexture3D`, `MeshPrimitive`; `operations/VectorExpression` |
| Renderer interface and constants | `Paint3DContext` |
| Primitive tessellation | `operations/utilities/d3/Primitive3D` |
| Creation | `RemoteComposeWriter3D` (`RemoteComposeWriter.getRc3DWriter()`), `RcScene3DScope` (Kotlin DSL) |
| JSON | `Command3DParser`, `ResourceParser` (`resources.bitmaps`); see `Documentation/parts/json-parser.md` |
| Reference player | `JavaPaint3DContext`, `Rasterizer`, `Matrix4` (`remote-player-core`, `platform/d3`) |
| Android integration | `AndroidPaintContext` (engine choice, blits, textures) |
