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

## Interaction Modifiers
Attach actions to a component. The actions are written as children of the modifier and run on the
player when the interaction happens. These modifiers are only valid inside a component's modifier
block.

| DSL Method | Operation / Modifier Class | Purpose |
| :--- | :--- | :--- |
| `.onClick { ... }` | `ClickModifierOperation` | Run actions on a single tap |
| `.onLongClick { ... }` / `.onDoubleClick { ... }` | `MultiClickModifier` | Run actions on a long press / double tap |
| `.onTouchDown { ... }` | `TouchDownModifierOperation` | Run actions when a touch starts |
| `.onTouchUp { ... }` | `TouchUpModifierOperation` | Run actions when a touch ends |
| `.onTouchCancel { ... }` | `TouchCancelModifierOperation` | Run actions when a touch is canceled |
| `.onEvent(type, filter, flags, dataIds, condition) { ... }` | `EventHandlerModifierOperation` | Run actions when an event of `type` whose metadata equals `filter` is dispatched to the component; optionally copies the event payload into the `dataIds` float variables and gates the actions on an RPN `condition` |

## Shapes
Used with `.clip()` or `.border()`:
- `RectShape`
- `RoundedRectShape`
- `CircleShape`
