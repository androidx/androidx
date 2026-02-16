# RemoteCompose Modifier Registry

This document lists the available layout modifiers in the `remote-creation` API and how they map to underlying operations.

| DSL Method | Operation / Modifier Class | Purpose |
| :--- | :--- | :--- |
| `.fillMaxSize()` | `DimensionModifierOperation.Type.FILL` | Occupies entire parent space |
| `.width(w)` / `.height(h)` | `DimensionModifierOperation.Type.EXACT` | Fixed dimensions |
| `.widthIn(min, max)` | `WidthInModifierOperation` | Constraints on width |
| `.padding(p)` | `PaddingModifierOperation` | External spacing |
| `.background(color)` | `BackgroundModifierOperation` | Solid color layer |
| `.backgroundId(id)` | `DynamicBackgroundModifier` | Theme-aware color layer |
| `.border(w, r, c)` | `BorderModifierOperation` | Stroke around boundary |
| `.clip(shape)` | `ClipRectModifierOperation` | Restrict content to shape |
| `.offset(x, y)` | `OffsetModifierOperation` | Positional translation |
| `.zIndex(z)` | `ZIndexModifierOperation` | Layer order |
| `.graphicsLayer(...)` | `GraphicsLayerModifierOperation`| Opacity, Rotation, Scaling |
| `.clickable(actions)` | `ClickModifierOperation` | Handle tap events |
| `.verticalScroll()` | `ScrollModifierOperation` | Enable vertical panning |
| `.alignByBaseline()` | `AlignByModifierOperation` | Align siblings by text baseline |
| `.visibility(id)` | `ComponentVisibilityOperation` | Dynamic show/hide |

## Shapes
Used with `.clip()` or `.border()`:
- `RectShape`
- `RoundedRectShape`
- `CircleShape`
