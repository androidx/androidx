# Scrolling Architecture in RemoteCompose

RemoteCompose provides a physics-based, hierarchical scrolling system that supports orthogonal nesting (e.g., horizontal rows inside vertical columns) and dynamic state synchronization.

## Core Concepts

### 1. Coordinate Spaces
To handle deep nesting and scrolling reliably, the interaction engine uses three distinct coordinate spaces:

| Space | Reference | Usage |
| :--- | :--- | :--- |
| **Root-Relative** | Document origin (0,0) | Used for top-level hit testing (`contains(x, y)`). |
| **Viewport-Relative** (`lx, ly`) | Component top-left on screen | Used by Modifiers (ripples, scroll logic). |
| **Content-Relative** (`cx, cy`) | Component's scrollable origin | Passed to children so they can ignore parent scroll. |

### 2. The Scroll Delegate
Scrolling is not hardcoded into layouts. Instead, `LayoutComponent` uses a `ScrollDelegate` interface. If a component has a `ScrollModifierOperation` in its modifier list, it registers itself as the delegate for horizontal or vertical axes.

- **Measure Phase**: The layout manager queries the delegate for the "content dimension" (total scrollable area).
- **Layout Phase**: The delegate updates the component's internal scroll offsets based on user interaction or bound variables.
- **Paint Phase**: `LayoutComponent` applies a translation transformation (`getScrollX()`, `getScrollY()`) before painting its children.

## The Interaction Pipeline

### 1. Event Entry (`CoreDocument`)
Touch events enter via `CoreDocument.touchDown/Drag/Up`. The document tracks "applied touch operations"—components currently being interacted with—to bypass hit-testing during a drag.

### 2. Recursive Dispatch (`Component`)
Events propagate down the tree in **reverse drawing order** (top-most component first).
- A component first performs a **Root-Relative** hit test.
- If it contains the point, it calculates the **Viewport** and **Content** offsets.
- It dispatches to children using the **Content** space.
- It then dispatches to its own modifiers using the **Viewport** space.

### 3. Event Consumption
Interaction handlers return a `boolean`.
- If a child component consumes an event (e.g., a button click), sibling components are ignored.
- **Crucially**, parent modifiers (like a `ScrollModifier`) are still notified even if a child consumed the event. This allows a nested list to scroll even if the user started the drag on a button.

## Physics and State (`TouchExpression`)

The `ScrollModifierOperation` typically hosts a `TouchExpression`. This engine handles:
- **Velocity Tracking**: Calculates pixels-per-second during a drag.
- **Fling/Easing**: When the user releases, it uses a `VelocityEasing` curve to continue the scroll.
- **Notches**: Supports snapping to specific positions (even spacing, absolute points, or percentages).

## Nested Scrolling Logic

RemoteCompose handles nested scrolls by utilizing the "Orthogonal Capture" rule:
1. A vertical scroll only consumes the **Y** component of a drag.
2. A horizontal scroll only consumes the **X** component.
3. Because propagation continues up the parent chain for modifiers, a vertical swipe inside a horizontal row will be ignored by the row's scroll logic but captured by the parent column's scroll logic.

## Layout Integration

Layout managers like `ColumnLayout` and `RowLayout` are "scroll-aware":
- During `internalLayoutMeasure`, they check `mComponentModifiers.hasVerticalScroll()`.
- If true, they allow the content to exceed the host's viewport height.
- They then call `setVerticalScrollDimension()` to inform the modifier of the total scrollable range, which the `TouchExpression` uses for boundary clamping.
