# Embedded Remote Compose Player

This module provides an embedded player for [CoreDocument](https://developer.android.com/reference/androidx/compose/remote/core/CoreDocument) that can be used in other Compose applications.

All draw operations is implemented using Compose APIs and no Android Views are used. All State is tracked using Compose State and the player is fully interoperable with the Compose UI system including A11y.

## Usage

```kotlin
@Composable
fun MyScreen() {
    val document: CoreDocument = rememberRemoteDocument {
      RemoteColumn {
        RemoteText("Hello World".rs)
        RemoteButton("Click Me".rs)
      }
    }
    RcPlayer(
        modifier = Modifier.size(100.dp),
        document = document.value,
        onNamedAction = { name, value, stateUpdater ->
            // Handle named actions
        }
    )
}
```

## Design & Architecture

The Embedded Remote Compose Player is designed to render `CoreDocument` using pure Compose APIs, bypassing the traditional Android View-based player. This approach brings several benefits but also introduces specific challenges and tradeoffs.

### Overall Approach
Instead of using a custom layout and drawing system (like the View-based player does in `remote-core`), this player maps remote components directly to standard Compose layout primitives:
- `BoxLayout` -> `Box`
- `ColumnLayout` -> `Column`
- `RowLayout` -> `Row`
Drawing operations (like `DrawRect`, `DrawCircle`) are executed inside `Modifier.drawWithContent` using Compose `DrawScope`.

### Benefits
- **Accessibility (a11y) for free**: By mapping to standard Compose components and using standard modifiers (like `Modifier.semantics`), we automatically get Compose's rich accessibility support without having to manually expose accessibility nodes.
- **Tooling**: Standard Compose tooling (like Layout Inspector and Previews) works out of the box for the rendered tree.
- **Interoperability**: The player can be placed anywhere in a Compose hierarchy and respects parent constraints and modifiers.

### Tradeoffs & Challenges
- **Layout Pass Conflict**: `remote-core` was designed with its own layout pass (`RootLayoutComponent.layout`) that requires a `PaintContext` (abstract class for drawing). Since the Compose player lets Compose do the layout, running the `remote-core` layout pass is redundant and often crashes due to missing `PaintContext`.
- **State Reactivity**: `remote-core` updates variables in a continuous loop or via explicit triggers. To make this reactive in Compose without infinite recompositions, document variables are backed by Compose snapshot state (`SnapshotRemoteComposeState`), and *computed* operations (color/text/float/int expressions, attributes) resolve through a `derivedStateOf` graph (`GraphContext`). Drawing operations read the same store, so the core's writes invalidate readers with no separate draw-path variable map.

### Workarounds
- **Layout via Compose, size fed back after layout**: To avoid calling `root.layout()`, Compose measures normally and component sizes are published back into the snapshot store via `Modifier.onSizeChanged` (which fires *after* layout, so it's safe to write snapshot state there). Operations relying on component dimensions read those reactive values.
- **DrawWithContent Support**: Operations inside `drawWithContent` are recorded into `CanvasOperations` and stored in `mDrawContentOperations` of `LayoutComponent`. The player executes these on the Compose `DrawScope` during `drawWithContent` (`executeOperations`).
- **Named Colors Resolution**: Default values for named colors are stored as `ColorConstant` operations in the document. The player reads these operations to populate defaults in the store when they are not provided externally.
- **Layout Scope Modifiers**: Some modifiers (like `weight` in `Row`) require scope-specific extensions. Since the player model flattens the hierarchy, a `modifierProvider` lambda in `RcPlayerChildren` applies these within the specific layout scope (e.g., `RowScope`).

## Implementation Progress

See [`STATUS.md`](STATUS.md) for the current branch state, [`operation_coverage.md`](operation_coverage.md)
for the exhaustive per-operation matrix, and [`HISTORY.md`](HISTORY.md) for design decisions and
root-cause history. In brief: containers (Box/Column/Row), Text, State, Image, the common modifiers,
reactive state, all scalar expression operators, and most canvas draw ops (shapes, bitmaps, paths,
matrices, gradients, canvas text, clipping, offscreen `DRAW_TO_BITMAP`) render; the long tail of
partial/degraded features is enumerated in `operation_coverage.md`.