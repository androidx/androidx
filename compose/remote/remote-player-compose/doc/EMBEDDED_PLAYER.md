# Remote Compose Embedded Player Design Document

**Module:** `androidx.compose.remote:remote-player-compose` ([`embedded/`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayer.kt))

**Status:** Active development. `RcPlayer` and the `RcPlayerFeature` APIs are `@RestrictTo(LIBRARY_GROUP)` and marked or opted in to [`@ExperimentalRemotePlayerApi`](../src/main/java/androidx/compose/remote/player/compose/ExperimentalRemotePlayerApi.kt). The player is gated by [`RemoteComposePlayerFlags.isEmbeddedPlayerEnabled`](../src/main/java/androidx/compose/remote/player/compose/RemoteComposePlayerFlags.kt), which defaults to `false`; while it is off, `RcPlayer` throws an `IllegalStateException`.

---

## 1. Executive Summary & Comparative Analysis: View Player vs. Embedded Player

Remote Compose provides two actively maintained player architectures for rendering serialized [`CoreDocument`](../../remote-core/src/main/java/androidx/compose/remote/core/CoreDocument.java) wire buffers on Android:

1. **View Player (`remote-player-view`)**: An `android.view.View` implementation ([`RemoteComposePlayer`](../../remote-player-view/src/main/java/androidx/compose/remote/player/view/RemoteComposePlayer.java)) that delegates layout, state evaluation, hit-testing, and rendering to [`CoreDocument.paint(RemoteContext, Theme)`](../../remote-core/src/main/java/androidx/compose/remote/core/CoreDocument.java) onto a single `android.graphics.Canvas` (with an optional Compose `AndroidView` wrapper via [`RemoteDocumentPlayer`](../src/main/java/androidx/compose/remote/player/compose/RemoteDocumentPlayer.kt)).
1. **Embedded Player (`remote-player-compose`)**: A native Jetpack Compose player ([`RcPlayer`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayer.kt)) that translates the [`CoreDocument`](../../remote-core/src/main/java/androidx/compose/remote/core/CoreDocument.java) component tree directly into a hierarchy of first-class `@Composable` layout nodes (`Box`, `Row`, `Column`, `BasicText`, `Image`, `AnimatedContent`, `SharedTransitionLayout`) backed by Compose's `Snapshot` state system. Optional operation groups and host capabilities (matrices, particles, meshes, sound, sensors) are pluggable [`RcPlayerFeature`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerFeature.kt)s selected through an [`RcPlayerFeatureSet`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerFeatureSet.kt), so R8 can strip the ones an app does not use.

> [!IMPORTANT]
> Both the **View Player** and the **Embedded Player** are actively maintained first-class targets. Each is purpose-built for distinct host environments and performance profiles.

### Architectural Comparison

| **Dimension** | **View Player (`remote-player-view`)** | **Embedded Player (`remote-player-compose`)** |
| --- | --- | --- |
| **Primary Host Environment** | `ViewGroup` hierarchies, app widgets, or standalone full-document surfaces. | Jetpack Compose hierarchies (`LazyColumn`, Wear OS `TransformingLazyColumn`, inline cards). |
| **Layout Engine** | `remote-core` internal imperative measure/layout pass (`Component.layout`). | Native Compose UI `LayoutNode` intrinsic & constraint-based measure/placement (`Box`, `Row`, `Column`, custom `Layout`). |
| **State & Invalidations** | Imperative [`RemoteComposeState`](../../remote-core/src/main/java/androidx/compose/remote/core/RemoteComposeState.java) + `context.needsRepaint()` triggering full-view `invalidate()` and re-running `CoreDocument.paint()`. | [`SnapshotRemoteComposeState`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/SnapshotRemoteComposeState.kt) (`SnapshotStateMap`) + `derivedStateOf` ASTs triggering surgical, scoped recomposition or draw/layer-only invalidation. |
| **Scrolling & Nesting** | Monolithic single `View` canvas; internal `ScrollModifier` managed by `remote-core` touch offsets. | Native `Modifier.verticalScroll` / `horizontalScroll` with Compose `NestedScrollConnection` handoff to parent `LazyColumn` / `Pager` ([`ScrollModifier.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/ScrollModifier.kt)). |
| **Accessibility (A11y)** | Virtual view hierarchy bridged via `ExploreByTouchHelper` ([`RemoteComposeDocumentAccessibility`](../../remote-player-view/src/main/java/androidx/compose/remote/player/view/accessibility/RemoteComposeDocumentAccessibility.java)). | Full Compose UI semantics via `Modifier.semantics` (`SemanticsNode` tree)—supports TalkBack, Switch Access, and `ComposeTestRule` semantics matchers out of the box. |
| **Shared Element Transitions** | Interpolated intermediate `Component` bounds during `CoreDocument.paint()`. | Native Compose [`SharedTransitionLayout`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerStateLayout.kt) + `AnimatedContent` + `Modifier.sharedBounds`. |
| **Custom Host Components** | Imperative `android.view.View` instances managed via [`AndroidCustomSupport`](../../integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/customviews/AndroidCustomSupport.java) (`MeasureSpec`, `layout`, `draw`). | Native `@Composable` plugins via [`CustomComposablePlugin<T>`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerCustom.kt) participating directly in the Compose layout & focus tree. |
| **Operation Coverage** | Reference implementation for the [`Operations.java`](../../remote-core/src/main/java/androidx/compose/remote/core/Operations.java) opcodes, all executed by `remote-core` (including `LAYOUT_COMPUTE`). | Built-in draw loop covers the AndroidX profile (`RcPlayerFeatureSet.BuiltInProfiles`): standard layouts, modifiers, reactive expressions, paths, shaders, canvas draw ops, impulses, click areas and document fonts. Matrices (`MatrixFeature`) and particles (`ParticlesFeature`) are features that the overloads without an explicit `RcPlayerFeatureSet` always include; experimental-profile meshes (`MeshFeature`), sound (`SoundFeature`) and device sensors (`SensorFeature`) are also features. `LAYOUT_COMPUTE` is consumed as a no-op (see [Section 4.4](#44-operation-support-comparison-view-player-vs-embedded-player)). |
| **Unit Testing** | Screenshot / golden bitmap tests or Espresso `onView` tests. | Fast JVM Robolectric and device tests via [`RcPlayerTestRule`](../src/sharedTest/java/androidx/compose/remote/player/compose/embedded/RcPlayerTestRule.kt) (a `ComposeContentTestRule`) with standard `onNodeWithText()` / `performClick()` semantics assertions, and the internal [`RcPlayerInspector`](../src/main/java/androidx/compose/remote/player/compose/embedded/inspectable/RcPlayerInspector.kt). |

### When to Use Which

- **Use the View Player (`remote-player-view`) when:**
    - The host is a traditional Android `ViewGroup` hierarchy without a Compose runtime.
    - Rendering procedural, canvas-heavy, or standalone full-screen documents where a single `Canvas` draw pass with zero `LayoutNode` tree overhead is desired.
    - Target environments require strict binary/dependency footprints without `androidx.compose.ui` or `androidx.compose.animation`.
    - The document relies on operations only the View Player executes (see [Section 4.4](#44-operation-support-comparison-view-player-vs-embedded-player) for the full matrix):
        - **Procedural Layout Compute (`LAYOUT_COMPUTE` /** [**`LayoutComputeOperation`**](../../remote-core/src/main/java/androidx/compose/remote/core/operations/layout/modifiers/LayoutComputeOperation.java)**)**: Custom RPN measure/placement equations that imperatively compute child bounds during `remote-core`'s layout pass. The Embedded Player consumes this modifier as a no-op in [`RcPlayerModifiers.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/RcPlayerModifiers.kt), since Compose `LayoutNode`s own layout.

- **Use the Embedded Player (`remote-player-compose`) when:**
    - Embedding one or many Remote Compose documents inside a Jetpack Compose host—especially inside virtualized scrolling containers like `LazyColumn` or Wear OS `TransformingLazyColumn` (avoiding `AndroidView` bridge overhead and oversized offscreen hardware layers).
    - Seamless interoperability with Compose features is required: nested scrolling, host image loading via [`RcImageLoader`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcImageLoader.kt) (e.g. Coil), Compose `TextMeasurer` / downloadable fonts ([`GmsFontTypefaceResolver`](../src/main/java/androidx/compose/remote/player/compose/embedded/GmsFontTypefaceResolver.kt)), full Compose UI semantics, or host-provided `@Composable` plugins (such as `BasicTextField`).
    - Writing fast, deterministic JVM unit tests using Compose's semantic test assertions.
    - Shrinking the player with R8: pass an `RcPlayerFeatureSet` with only the features your documents need.

---

## 2. Implementation Walkthrough: From `CoreDocument` to `@Composable` Nodes

### Step 1: Document Preprocessing & Context Initialization

When [`rememberRcPlayerState(document)`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerState.kt) creates an `RcPlayerState` for a [`CoreDocument`](../../remote-core/src/main/java/androidx/compose/remote/core/CoreDocument.java), [`preprocessDocument`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/RcPlayerPreprocess.kt) walks the operation tree once to build an index of the document's static and dynamic characteristics ([`DocumentPreprocessResult`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/RcPlayerPreprocess.kt)):

- **`globalOps`**: Operations before the `RootLayoutComponent` (for example `Header`), applied once during setup.
- **`constantOps`**: `NamedVariable`s plus constants (`ColorConstant`, `FloatConstant`, `ColorTheme`, `IntegerConstant`, `LongConstant`, `BooleanConstant`, `MatrixConstant`) found anywhere in the tree outside loops, also applied once during setup.
- **`computedOpIndex`**: Maps every computed variable ID to its defining `Operation` for lazy `derivedStateOf` evaluation through the `GraphContext`. This covers `VariableSupport` + `VariableProvider` operations such as `FloatExpression`, `IntegerExpression`, `ColorExpression`, `TextFromFloat` and `TextMerge` (path and matrix operations are excluded), plus `TextMeasure`, `ImageAttribute`, `TextAttribute`, `TimeAttribute`, `BitmapTextMeasure`, `TextLookup`, `IdLookup`, `TextSubtext` and `TextTransform`. IDs written by a `TouchExpression`, and float variables that a draw stream writes more than once per pass (`streamFloatIds`: reassigned, or assigned inside loops, conditionals or float functions), are left out.
- **`componentValueMap`**: Maps target `componentId`s to the [`ComponentValue`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/ComponentValue.java) operations that read them (`WIDTH`, `HEIGHT`, `POS_X`, `POS_Y`, `POS_ROOT_X`, `POS_ROOT_Y`, `CONTENT_WIDTH`, `CONTENT_HEIGHT`), so only components whose measured bounds are actually read by expressions attach an `onPlaced` callback that publishes them.
- **Touch indexes (`touchExpressions`, `rootTouchExpressions`, `componentTouchExpressionsMap`)**: Each `TouchExpression` is bound to its enclosing `LayoutComponent`, or to the root pointer handler if it has none.
- **Activity flags (`hasContinuousTime`, `hasDiscreteTime`, `discreteTimeMask`, `hasParticles`, `hasWakeIn`)**: Determines whether [`RcPlayer`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayer.kt) needs a per-frame clock loop, a loop that wakes on second boundaries, or **no background coroutine loop at all**. At runtime, [`ClockScheduler`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/ClockScheduler.kt) narrows this upper bound to the time fields that composition, layout and draw actually read.

Next, [`initializePlayerRemoteContext`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/RcPlayerPreprocess.kt) creates an `AndroidRemoteContext` and installs a reactive [`SnapshotRemoteComposeState`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/SnapshotRemoteComposeState.kt) on the document. It then applies the data operations, `globalOps`, `constantOps` and the root's data operations without eagerly decoding embedded bitmaps ([`applyDataOperationsWithoutBitmaps`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/CoreDataAccessors.kt) / `applyOperationsWithoutBitmaps`), and seeds the touch expressions.

### Step 2: Recursive Component Dispatch (`RcPlayerComponent`)

At composition time, [`RcPlayer`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayer.kt) uses `CompositionLocalProvider` to provide `LocalCoreDocument`, `LocalRemoteContext`, `LocalGraphContext`, `LocalRcImageLoader`, the component value and touch-expression maps, the action handlers, custom component plugins and the typeface resolver. It then invokes [`RcPlayerRootLayoutComponent`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerComponents.kt) -> [`RcPlayerChildren`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerComponents.kt) -> [`RcPlayerComponent`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerComponents.kt):

```kotlin
// components/RcPlayerComponents.kt
@Composable
internal fun RcPlayerComponent(component: Component, modifier: Modifier = Modifier) {
    if (component is LayoutComponent) {
        // ...
        // 1. Find the visibility operation, unless FitBoxLayout / StateLayout own it
        val visibilityDelegatedToParent =
            component.parent is FitBoxLayout || component.parent is StateLayout
        val visibilityOp =
            if (visibilityDelegatedToParent) null
            else
                component.componentModifiers.list.fastFirstOrNull {
                    it is ComponentVisibilityOperation
                } as? ComponentVisibilityOperation
        // ...
        // 2. Translate remote ModifierOperations + DrawContentOperations into a Compose Modifier
        var outerModifier =
            Modifier.sharedElementTransition(component)
                .stateBranchEnterExit(component)
                // ...
                .then(modifier)
        val contentModifier =
            Modifier.rcComponentOuterInspector(component)
                .then(
                    component.componentModifiers.toModifier(
                        component.drawContentOperations?.list,
                        ignoreVisibility = true,
                        ignoreClicks = customClickHandlers != null,
                    )
                )
                // ... touch expressions; onPlaced publishes ComponentValue bounds
        // 3. Skip GONE components (or animate visibility), then dispatch to a Compose node
        RcPlayerAnimatedVisibility(component, visibilityOp, animSpec, outerModifier) {
            visibilityModifier ->
            RcPlayerLayoutComponent(
                component,
                visibilityModifier.then(contentModifier),
                customClickHandlers,
            )
        }
    }
    // ...
}

@Composable
private fun RcPlayerLayoutComponent(
    component: LayoutComponent,
    modifier: Modifier,
    customClickHandlers: ClickActionHandlers?,
) {
    when (component) {
        is CanvasLayout -> RcPlayerCanvas(component, modifier)
        is ColumnLayout -> RcPlayerColumn(component, modifier)
        is FlowLayout -> RcPlayerFlowRow(component, modifier)
        is RowLayout -> RcPlayerRow(component, modifier)
        is CoreText -> RcPlayerText(component, modifier)
        is TextLayout -> RcPlayerText(component, modifier)
        is FitBoxLayout -> // ...
            RcPlayerFitBoxLayout(component, modifier)
        is StateLayout -> RcPlayerStateLayout(component, modifier)
        is ImageLayout -> RcPlayerImageLayout(component, modifier)
        is Custom -> RcPlayerCustom(component, modifier, customClickHandlers)
        // Last as others are often BoxLayout subclasses
        is BoxLayout -> RcPlayerBox(component, modifier)
        else -> { /* unsupported layout: render nothing */ }
    }
}
```

When a component has a visibility operation, [`RcPlayerAnimatedVisibility`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerVisibility.kt) returns before composing the component's content if it is `GONE` and has no enabled `AnimationSpec`. Otherwise it animates the enter/exit transition.

### Example A: Simple Container — `RemoteBox` > `BoxLayout` > `RcPlayerBox`

When a document author writes `RemoteBox(modifier = ..., contentAlignment = ...)` in the creation DSL, it serializes a [`BoxLayout`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/layout/managers/BoxLayout.java) operation. In the Embedded Player, [`RcPlayerBox`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerBoxLayout.kt) maps it directly to `androidx.compose.foundation.layout.Box`:

```kotlin
// layout/RcPlayerBoxLayout.kt
@Composable
internal fun RcPlayerBox(layout: BoxLayout, modifier: Modifier) {
    // ...
    Box(
        modifier = modifier,
        contentAlignment =
            boxContentAlignment(
                layout.horizontalPositioning,
                layout.verticalPositioning,
            ),
    ) {
        RcPlayerChildren(layout)
    }
}
```

All sizing, padding, background, border, clipping and click operations attached to the `BoxLayout` are already translated into the `modifier` chain by [`ComponentModifiers.toModifier()`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/RcPlayerModifiers.kt). [`RcPlayerChildren(layout)`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerComponents.kt) then recursively emits each child `Component` inside the `BoxScope`.

### Example B: Leaf Node — `CoreText` > `RcPlayerText` on Compose `BasicText`

For text nodes ([`CoreText`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/layout/managers/CoreText.java) and [`TextLayout`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/layout/managers/TextLayout.java)), [`RcPlayerText`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerTextLayout.kt) does the following:

- subscribes reactively to the text ID and to dynamic color and paint properties
- resolves the `FontFamily` through `LocalRcTypefaceResolver`, including variable font axes and, with a resolver such as `GmsFontTypefaceResolver`, downloadable fonts
- resolves variable letter spacing and line height
- maps `autosize` to `TextAutoSize.StepBased`

It then hands layout and glyph rendering to Compose Foundation's `BasicText`:

```kotlin
// components/RcPlayerTextLayout.kt
@Composable
internal fun RcPlayerText(layout: CoreText, modifier: Modifier) {
    val textId = layout.textId ?: return
    val text by rememberRemoteStringAsState(textId)
    val paintState = remember { ComposeLocalPaint() }.apply { reset() }
    updatePaintFromBundle(
        layout.mPaint,
        paintState,
        LocalGraphContext.current ?: LocalRemoteContext.current,
    )
    val color =
        resolveTextColor(
            paintState = paintState,
            isDynamicColorEnabled = layout.isDynamicColorEnabled,
            colorId = layout.colorId,
            colorValue = layout.colorValue,
        )
    val density = LocalDensity.current
    val fontSize = resolveTextFontSizePx(paintState, layout.fontSize)
    val fontSizeSp = with(density) { fontSize.toSp() }
    // ... resolves fontWeight, fontStyle, fontFamily and fontFeatureSettings (font axes),
    // textDecoration, variable letterSpacing / lineHeight, StepBased TextAutoSize,
    // textAlign, lineBreak, hyphens and overflow ...
    BasicText(
        text = text,
        modifier = modifier,
        autoSize = autoSize,
        style =
            TextStyle(
                color = color,
                fontSize = fontSizeSp,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                fontStyle = fontStyle,
                fontFeatureSettings = fontFeatureSettings,
                textAlign = textAlign,
                lineBreak = lineBreak,
                hyphens = hyphens,
                letterSpacing = letterSpacing.em,
                lineHeight =
                    resolveCoreTextLineHeight(
                        fontSize = fontSize,
                        lineHeightMultiplier = lineHeightMultiplier,
                        lineHeightAdd = lineHeightAdd,
                        autosize = layout.autosize,
                        density = density,
                    ),
                textDecoration = textDecoration,
            ),
        overflow = overflow,
        maxLines = resolveEffectiveMaxLines(layout.maxLines, layout.overflow),
    )
}
```

Because `text` is backed by [`rememberRemoteStringAsState(textId)`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerState.kt), updating that string ID (for example through a host state override, a `TextFromFloat` expression or a custom component return handler) recomposes **only** this `RcPlayerText` scope. Sibling and parent nodes do not recompose. If the new text changes the node's size, Compose re-measures the parent layout without recomposing it.

---

## 3. State, Expressions, Animation, and Time in a Compose `State` Way

A core innovation of the Embedded Player is replacing `remote-core`'s imperative per-frame `updateVariables()` + full-tree repaint loop with **reactive Compose `Snapshot` state and lazy, on-demand evaluation**.

### 3.1 `SnapshotRemoteComposeState` — Snapshot-Backed Reactive Store

[`SnapshotRemoteComposeState`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/SnapshotRemoteComposeState.kt) subclasses `RemoteComposeState` and backs its reactive caches (`floats`, `integers`, `colors`, `data`, `objects`, plus per-type `overridden*` flags) with Compose `mutableStateMapOf` instances by overriding the public getter/setter APIs:

```kotlin
// doc/SnapshotRemoteComposeState.kt
internal class SnapshotRemoteComposeState : RemoteComposeState() {
    private val floats: SnapshotStateMap<Int, Float> = mutableStateMapOf()
    // ... integers, colors, data, objects, overriddenFloats, ...

    /** Float ids a draw stream writes repeatedly per pass; kept out of the snapshot map. */
    internal var streamFloatIds: IntSet = emptyIntSet()

    // --- Float ---
    override fun getFloat(id: Int): Float {
        if (streamFloatIds.contains(id)) return super.getFloat(id)
        return floats[id] ?: super.getFloat(id)
    }
    // ...
    override fun updateFloat(id: Int, value: Float) {
        if (streamFloatIds.contains(id)) {
            super.updateFloat(id, value)
            return
        }
        val old = floats[id]
        super.updateFloat(id, value)
        val new = super.getFloat(id)
        if (new != old) {
            floats[id] = new
            colors[id] = super.getColor(id)
        }
    }
    // ...
}
```

- Every read (`getFloat(id)`, `getInteger(id)`, `getColor(id)`, `getFromId(id)`, `getObject(id)`) performed inside composition, a `derivedStateOf`, `Modifier.graphicsLayer { ... }` or a `DrawScope` records a Compose **snapshot read** of the backing `SnapshotStateMap`.
- Writes (`cacheFloat`/`updateFloat`/`overrideFloat`, `updateInteger`/`overrideInteger`, `updateColor`/`overrideColor`, `cacheData`/`updateData`/`overrideData`, and the `clear*Override` calls) only write the map when the stored value actually changes, so unchanged writes invalidate nothing. Because the map is the unit of invalidation, the player narrows recomposition further by reading each id through a per-id `derivedStateOf` (see `rememberRemoteFloatAsState` in [`state/RcPlayerState.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerState.kt)), which only notifies its readers when that id's value changes.
- Float ids that a draw stream writes several times per pass (`streamFloatIds`, computed by `preprocessDocument`) bypass the snapshot map entirely, so per-frame draw-stream writes do not invalidate the draw that just made them.

### 3.2 Compiling RPN Expressions into `RemoteOp` ASTs with Imperative Fallback (`RcPlayerRpnOps`)

In `remote-core`, [`FloatExpression`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/FloatExpression.java) encodes Reverse Polish Notation (RPN) bytecode inside a `FloatArray` (`mSrcValue`), where variable IDs, array references, and math opcodes (`ADD`, `MUL`, `SIN`, `CLAMP`, `IFELSE`, `CUBIC`, `A_DEREF`, `STORE_RO`, etc.) are encoded as distinct IEEE-754 `NaN` bit patterns ([`Utils.idFromNan`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/Utils.java)). The player classifies each token with the [`MaybeVariableFloat`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/MaybeVariableFloat.kt) value class (`isLiteral`, `isOperator`, `isArray`, `isScalarReference`).

For animated expressions (or expressions that transitively depend on an animated value, per `expressionDependsOnAnimation`), [`rememberRemoteExpression`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerExpression.kt) parses the RPN `FloatArray` via [`parseRpn`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerRpnOps.kt) into an inspectable [`RemoteOp`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerRpnOps.kt) AST (`ConstantOp`, `VarOp`, `AddOp`, `SubOp`, `MulOp`, `DivOp`, `MinOp`, `MadOp`, `LerpOp`, `CubicOp`, `UnaryOp`, `BinaryOp`, `TernaryOp`), evaluated lazily inside `remember(tree) { derivedStateOf { tree.eval() } }`:

```kotlin
// state/RcPlayerRpnOps.kt
public sealed class RemoteOp : InspectableValue {
    public abstract fun eval(): Float
    // ...
}

public class VarOp(public val id: Int, public val state: State<Float>) : RemoteOp() {
    override fun eval(): Float = state.value // Tracked by Compose snapshot!
    // ...
}

public class AddOp(public val left: RemoteOp, public val right: RemoteOp) : RemoteOp() {
    override fun eval(): Float = left.eval() + right.eval()
    // ...
}

public fun parseRpn(
    exp: FloatArray,
    varStates: Map<Int, State<Float>>,
    collections: CollectionsAccess? = null,
): RemoteOp {
    if (needsImperativeEval(exp)) {
        return ImperativeRpnOp(exp, varStates, collections)
    }
    val stack = mutableListOf<RemoteOp>()
    // ... pop(), unary(), binary(), ternary() helpers
    for (v in exp) {
        val token = MaybeVariableFloat(v)
        if (!token.isLiteral) {
            if (token.isOperator) {
                val opCode = AnimatedFloatExpression.fromNaN(v)
                when (opCode) {
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.ADD) -> {
                        val right = stack.removeAt(stack.size - 1)
                        val left = stack.removeAt(stack.size - 1)
                        stack.add(AddOp(left, right))
                    }
                    // Ternary (pop 3); `a` is the deepest operand.
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.IFELSE) ->
                        ternary("ifElse") { a, b, c -> if (c > 0f) b else a }
                    // ... remaining unary, binary, ternary, DUP/SWAP and CUBIC opcodes
                    else -> {
                        // Unsupported operator: log a warning and substitute 0.
                        stack.add(ConstantOp(0f))
                    }
                }
            } else {
                val id = token.id
                val state =
                    varStates[id] ?: throw IllegalArgumentException("State not found for id $id")
                stack.add(VarOp(id, state))
            }
        } else {
            stack.add(ConstantOp(v))
        }
    }
    return when {
        stack.size == 1 -> stack[0]
        stack.isEmpty() -> ConstantOp(0f)
        else -> stack.last() // unbalanced stack: logged, last result used
    }
}
```

#### The Imperative Fallback (`needsImperativeEval` & `ImperativeRpnOp`)

While pure arithmetic, trigonometry, and `CUBIC` easing map cleanly to a stateless `RemoteOp` tree, four families of RPN opcodes (listed in `IMPERATIVE_OPCODES`) have evaluation-order side effects or depend on `CollectionsAccess` / core caches that a pure tree cannot model:

1. **Register load/store (`STORE_RO..STORE_R3`, `LOAD_R0..LOAD_R3`)**: Imperative scratch registers written and read across the RPN stream.
1. **Random / Noise (`RAND`, `RAND_SEED`, `NOISE_FROM`, `RAND_IN_RANGE`)**: Non-pure stateful PRNG operations.
1. **External `var` parameters (`VAR1..VAR3`)**.
1. **Array / collection opcodes & data-variable IDs (`A_DEREF`, `A_MAX`, `A_MIN`, `A_SUM`, `A_AVG`, `A_LEN`, `A_SPLINE`, `A_SPLINE_LOOP`, `A_SUM_TILL`, `A_SUM_XY`, `A_SUM_SQR`, `A_LERP`, or any array token, i.e. `NanMap.isDataVariable(v)`)**: Require a `CollectionsAccess` handle and `AnimatedFloatExpression`'s per-instance spline caches.

[`needsImperativeEval`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerRpnOps.kt) detects these opcodes upfront:

```kotlin
// state/RcPlayerRpnOps.kt
private fun needsImperativeEval(exp: FloatArray): Boolean = exp.any { v ->
    val token = MaybeVariableFloat(v)
    (token.isOperator && AnimatedFloatExpression.fromNaN(v) in IMPERATIVE_OPCODES) || token.isArray
}
```

When `needsImperativeEval(exp)` returns `true`, `parseRpn` falls back to [`ImperativeRpnOp`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerRpnOps.kt). Inside `eval()`, `ImperativeRpnOp` resolves each scalar-reference token from `varStates[id].value` into a reusable `FloatArray` buffer—**preserving Compose snapshot read tracking on every scalar dependency**—while passing operators, constants, and array/data-variable NaNs verbatim to a dedicated `AnimatedFloatExpression` instance (`core.eval(collections, buffer, buffer.size)`, or `core.eval(buffer, buffer.size)` when no `CollectionsAccess` is available).

### 3.3 `GraphContext` — Pure-Compose DAG Evaluation of Derived Operations

For non-animated `FloatExpression`s and other derived operations, [`GraphContext`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/GraphContext.kt) evaluates values on demand. `preprocessDocument` (in [`doc/RcPlayerPreprocess.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/RcPlayerPreprocess.kt)) indexes the *computed* ops: every `VariableSupport` + `VariableProvider` op except path and matrix ops and `TouchExpression` (e.g. `FloatExpression`, `IntegerExpression`, `TextFromFloat`, `TextMerge`, `TextLookupInt`, `ColorExpression`, …), plus `TextMeasure`, `ImageAttribute`, `TextAttribute`, `TimeAttribute`, `BitmapTextMeasure`, `TextLookup`, `IdLookup`, `TextSubtext` and `TextTransform`. Rather than holding its own `derivedStateOf` per id, `GraphContext` memoizes within each evaluation pass; the per-id `derivedStateOf` wrappers live in the callers (`rememberRemoteFloatAsState`, `rememberRemoteIntAsState`, `rememberRemoteStringAsState`, … in [`state/RcPlayerState.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerState.kt)), e.g. `remember(graph, id) { derivedStateOf { graph.getFloat(id) } }`.

```kotlin
// doc/GraphContext.kt
/**
 * A pure-Compose evaluator for *computed* operations (color/text/float/int expressions, attributes,
 * lookups) using per-pass DAG memoization over reactive leaf states — no imperative recompute pass,
 * no dirty flags, and no intermediate `derivedStateOf` nodes.
 *
 * Each computed id evaluates the op's existing `updateVariables` + `apply` against **this**
 * context, memoized within the active top-level evaluation pass (`withEvalPass` / top-level
 * `computedValue`). We intercept the two things an op does:
 * - **reads** (`getFloat`/`getInteger`/`getColor`/`getText`) — for a computed input we recurse into
 *   `computedValue(depId)` (memoized in Θ(1) if already visited in the current pass, so
 *   reconvergent diamond DAGs evaluate in Θ(V + E) rather than Θ(2^D)); for a leaf we
 *   read the real snapshot-backed store via `super`, which records the snapshot dependency directly
 *   on the caller's active Compose observation scope.
 * - **writes** (`loadFloat`/`loadInteger`/`loadColor`/`loadText`) — captured as the op's result
 *   rather than mutated into the store, keeping evaluation pure.
 * ...
 */
internal class GraphContext(
    private val realState: SnapshotRemoteComposeState,
    private val computedOps: IntObjectMap<Operation>,
    /** Reactive time fields and clock that time reads in this context resolve against. */
    internal val timeState: GraphTimeState,
    internal var componentValues: Map<Int, State<Float>> = emptyMap(),
    private val touchExpressionIds: IntSet = emptyIntSet(),
    internal var animatedFloatStates: Map<Int, State<Float>> = emptyMap(),
) : StoreBackedRemoteContext(timeState.baseClock) {
```

### 3.4 Animations & Phase-Skipped `GraphicsLayerModifier`

The Embedded Player handles animations at three levels:

1. **Value-Targeted `FloatAnimation` (`Animatable` / `spring` / `tween`)**: When a `FloatExpression` carries an animation (`mFloatAnimation` or `mSrcAnimation`), [`rememberRemoteFloatAsState`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerState.kt) routes it to [`rememberAnimatedRemoteFloat`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerExpression.kt) (the player also creates these states up front at its root in `RcPlayer`). The expression's source RPN, evaluated by `rememberRemoteExpression`, is the animation target; a Compose `Animatable` seeded at the authored initial value animates toward each new target with Compose's `tween()` (duration + mapped easing) or `spring()` physics. Target changes are posted to a conflated channel and consumed by `LaunchedEffect(animated) { animated.animateToTargets() }` (`collectLatest`), so animations are interruptible and settle to idle when complete. Targets that read a time field are evaluated only by their readers, so an animation in an inactive branch doesn't keep the clock loop ticking.
1. **Implicit graphics-layer animation**: Mirroring core's `AnimatableValue`, [`GraphicsLayerModifier.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/GraphicsLayerModifier.kt) wraps variable-backed layer properties in `animateFloatAsState` with a 300 ms `CUBIC_STANDARD` tween (snap on first composition, animate on change). Properties driven by time, component values, or an already-animated expression skip this so they don't lag their source.
1. **Phase-Skipped `GraphicsLayerModifier`**: `Modifier.graphicsLayer(op: GraphicsLayerModifierOperation)` resolves each transform property (`scaleX`, `scaleY`, `alpha`, `translationX/Y`, `shadowElevation`, `rotationX/Y/Z`, `cameraDistance`, `transformOriginX/Y`) as a `State<Float>` *outside* the lambda and reads `.value` **exclusively inside** `Modifier.graphicsLayer { ... }` (`compositingStrategy` is a static wire value):

```kotlin
// modifier/GraphicsLayerModifier.kt
@Composable
internal fun Modifier.graphicsLayer(op: GraphicsLayerModifierOperation): Modifier {
    val values = op.values
    val scaleX =
        rememberGraphicsLayerFloatAsState(values[GraphicsLayerModifierOperation.SCALE_X].source)
    val alpha =
        rememberGraphicsLayerFloatAsState(values[GraphicsLayerModifierOperation.ALPHA].source)
    val rotationZ =
        rememberGraphicsLayerFloatAsState(values[GraphicsLayerModifierOperation.ROTATION_Z].source)
    // ...
    return this.graphicsLayer {
        this.scaleX = scaleX.value
        this.alpha = alpha.value
        this.rotationZ = rotationZ.value
        // ...
        this.transformOrigin = TransformOrigin(transformOriginX.value, transformOriginY.value)
        this.compositingStrategy = compositingStrategy
    }
}
```

Because the `.value` read happens inside the `GraphicsLayerScope` block, **time-driven (60/120fps) layer animations bypass Compose recomposition and layout**, executing only in the layer-update phase. (The implicit `animateFloatAsState` path reads its *target* in composition, so a discrete variable change recomposes once to start the tween; the per-frame animated value is still only read inside the layer block.) The 300 ms implicit animation is covered by [`RcPlayerGraphicsLayerTest`](../src/test/java/androidx/compose/remote/player/compose/embedded/RcPlayerGraphicsLayerTest.kt).

### 3.5 Demand-Driven Time Architecture (`GraphTimeState` & `ClockScheduler`)

[`GraphTimeState`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/GraphTimeState.kt) holds the time fields as Compose snapshot states (continuous `ID_ANIMATION_TIME` / `ID_CONTINUOUS_SEC`, plus discrete wall-clock and calendar fields that only change when the epoch second advances), and records every read through a `TimeReadTracker`. The clock loop in [`RcPlayer.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayer.kt) picks one of **three execution tiers** from the upper bound found by [`preprocessDocument`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/RcPlayerPreprocess.kt) (plus core listener registration):

1. **Static / Event-Driven Documents (`!needsContinuousLoop && !needsDiscreteLoop`)**: Documents with no time variables (including documents that only use `FloatAnimation` or `StateLayout` transitions, which run on Compose's own animation clocks) keep the `t = 0` seeded by `GraphTimeState` and **return from the `LaunchedEffect` immediately** without awaiting a frame. Zero CPU or choreographer wakeups occur while idle.
1. **Discrete Wall-Clock Documents (`needsDiscreteLoop`)**: Documents referencing `ID_TIME_IN_SEC`, `ID_TIME_IN_MIN`, `ID_TIME_IN_HR`, `ID_CALENDAR_MONTH`, `ID_WEEK_DAY`, `ID_EPOCH_SECOND`, etc., compute `millisToNextSecond = 1000L - Math.floorMod(currentMillis, 1000L)` and wait via `scheduler.awaitDiscreteTick(millisToNextSecond)`, waking up at most once per second on wall-clock boundaries.
1. **Continuous Animation / Particle Documents (`needsContinuousLoop`)**: Documents referencing `ID_CONTINUOUS_SEC` or `ID_ANIMATION_TIME`, or containing `ParticlesLoop` / `ParticlesCompare`, `WakeIn`, or `ImpulseOperation`, advance time every frame via `withInfiniteAnimationFrameMillis` (so a host `InfiniteAnimationPolicy` can pause them), paced by core's `Limiter`. `Header.DOC_DESIRED_FPS` is applied as a platform hint via `Modifier.preferredFrameRate(desiredFps)`.

Because the document scan is only an upper bound, [`ClockScheduler`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/ClockScheduler.kt) narrows it at runtime from the time reads the active composition, layout and draw passes actually make: it drops from `CONTINUOUS` to `DISCRETE` after a run of frames with no continuous read, and to `PARKED` (no frames requested until a time field is read again) once the discrete fields go unread. Rarely-changing fields (hour, day, year, UTC offset) are *probed* to detect readers. Idling only starts once a host window draw has been observed, and never for particle or `WakeIn` documents.

### 3.6 Host State Synchronization (`RcPlayerState`)

Hosts can read and write named variables in the document using Compose `MutableState` property delegates on [`RcPlayerState`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerState.kt): `floatState`, `intState`, `booleanState`, `stringState`, `colorState` (`MutableState<Color>`), `bitmapState` and `floatArrayState`, plus `clearOverride(name)` to restore the authored default. Writes are applied as named overrides on the document context; reads go through the `GraphContext`, so computed variables resolve reactively. Names are resolved against `defaultPrefix` (`"USER"` by default): a name containing `:` is used verbatim, otherwise `"USER:name"` is preferred, falling back to the unprefixed name.

```kotlin
val playerState = rememberRcPlayerState(document)
var temperature by playerState.floatState("weather.temp")
var city by playerState.stringState("weather.city")
var accentColor by playerState.colorState("theme.accent")
RcPlayer(state = playerState)
```

---

## 4. Feature Categorization: Compose UI vs. `remote-core` vs. Hybrid

### 4.1 Implemented via Compose UI

These features translate Remote Compose operations directly into native Jetpack Compose primitives:

| **Remote Compose Feature** | **Embedded Player Implementation** | **Compose UI APIs Used** |
| --- | --- | --- |
| **Box Layout** | [`RcPlayerBox`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerBoxLayout.kt) | `androidx.compose.foundation.layout.Box`, `Alignment` |
| **Row & Column Layouts** | [`RcPlayerRow`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerRowLayout.kt), [`RcPlayerColumn`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerColumnLayout.kt), [`RcPlayerLayoutAlignment.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerLayoutAlignment.kt) | `Row`, `Column`, `Arrangement.Horizontal/Vertical`, `Arrangement.spacedBy`, `RowScope.weight`, `ColumnScope.weight`, `RowScope.alignByBaseline()` ([`AlignByModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/AlignByModifier.kt)) |
| **Flow Layout** | [`RcPlayerFlowRow`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerRowLayout.kt) | `FlowRow`, `Arrangement` |
| **Text & AutoSize** | [`RcPlayerText`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerTextLayout.kt) | `BasicText`, `TextStyle`, `TextAutoSize.StepBased`, `FontFamily`, `FontVariation.Settings`, `LineBreak`, `Hyphens`, `TextOverflow`; downloadable fonts through `FontsContractCompat` ([`GmsFontTypefaceResolver`](../src/main/java/androidx/compose/remote/player/compose/embedded/GmsFontTypefaceResolver.kt)) |
| **Images** | [`RcPlayerImageLayout`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerImageLayout.kt) | `androidx.compose.foundation.Image`, `BitmapPainter`, [`DrawablePainter`](../src/main/java/androidx/compose/remote/player/compose/embedded/resources/DrawablePainter.kt), `ContentScale` |
| **State Layout & Shared Transitions** | [`RcPlayerStateLayout`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerStateLayout.kt), [`RcPlayerVisibility.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerVisibility.kt) | `SharedTransitionLayout`, `AnimatedContent`, `Modifier.sharedBounds`, `rememberSharedContentState`, `SizeTransform`, `fadeIn`, `fadeOut`, `tween` |
| **Dimensions** | [`WidthModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/WidthModifier.kt), [`HeightModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/HeightModifier.kt) | `Modifier.width`, `Modifier.height`, `Modifier.fillMaxWidth/Height`, `Modifier.widthIn/heightIn`, `Modifier.requiredWidthIn/requiredHeightIn` |
| **Padding, Offset, Z-Index** | [`PaddingModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/PaddingModifier.kt), [`OffsetModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/OffsetModifier.kt), [`ZIndexModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/ZIndexModifier.kt) | `Modifier.padding`, `Modifier.offset { IntOffset(...) }`, `Modifier.zIndex` |
| **Background, Border, Clip** | [`BackgroundModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/BackgroundModifier.kt), [`BorderModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/BorderModifier.kt), [`ClipModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/ClipModifier.kt) | `Modifier.background(color, shape)`, `Modifier.border`, `Modifier.clip`, `RoundedCornerShape`, `CircleShape`, `RectangleShape` |
| **Graphics Layer** | [`GraphicsLayerModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/GraphicsLayerModifier.kt) | `Modifier.graphicsLayer { ... }`, `TransformOrigin`, `CompositingStrategy` |
| **Marquee** | [`MarqueeModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/MarqueeModifier.kt) | `Modifier.basicMarquee`, `MarqueeSpacing` |
| **Click, Ripple & Haptics** | [`ClickModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/ClickModifier.kt), [`RippleModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/RippleModifier.kt), [`RcPlayerHaptics.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerHaptics.kt) | `Modifier.clickable`, `Modifier.combinedClickable`, `Modifier.indication`, `ripple()`, `MutableInteractionSource`, `View.performHapticFeedback` |
| **Accessibility Semantics** | [`SemanticsModifier`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/SemanticsModifier.kt) | `Modifier.semantics` (optionally `mergeDescendants = true`), `Modifier.clearAndSetSemantics`, `contentDescription`, `text`, `role` (`Role`) |
| **Frame Rate Hints** | [`RcPlayer.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayer.kt) | `Modifier.preferredFrameRate(desiredFps)` |

### 4.2 Implemented via `remote-core`

These subsystems execute their domain logic using `remote-core`'s battle-tested engines:

- **Particle System Engine (**[**`ParticlesFeature`**](../src/main/java/androidx/compose/remote/player/compose/embedded/ParticlesFeature.kt) **→** [**`RcPlayerParticles.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerParticles.kt)**)**: `ParticlesCreate`, `ParticlesLoop`, and `ParticlesCompare` manage particle state arrays, initialization equations, spawn/recycle conditions, and physics evolution steps in `remote-core`, emitting draw calls during the canvas execution pass. These ops are provided by the optional `ParticlesFeature` (see the note in §4.4).
- **Procedural Path Geometry (`PathCreate`, `PathData`, `PathCombine`, `PathTween`, `PathExpression`)**: Polar/Cartesian algorithmic path generation, path morphing (`PathTween`), and boolean path operations (`PathCombine` via Compose's `Path.combine(PathOperation, ...)` in [`RcPlayerPrimitiveDrawing.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerPrimitiveDrawing.kt)).
- **Color & String Expression Evaluation (`ColorExpression`, `TextFromFloat`, `TextMerge`, `TimeAttribute`, `IntegerExpression`)**: Invoked on demand inside [`GraphContext`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/GraphContext.kt)'s `derivedStateOf` wrappers using `remote-core`'s formatting, spline, and color interpolation math.

### 4.3 Hybrid (Compose UI + `remote-core` Working Together)

Several sophisticated features combine Compose UI's layout/gesture/draw infrastructure with `remote-core`'s procedural operations:

1. **`DrawContentOperation` & Modifier-Interleaved Custom Drawing (**[**`RcPlayerModifiers.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/RcPlayerModifiers.kt) **&** [**`RcPlayerDrawing.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerDrawing.kt)**)**: A component in Remote Compose can interleave modifiers with `DrawContentOperation` and `CanvasOperations` (e.g., draw a custom procedural shader behind content, apply padding, render child `@Composable`s, then draw a particle overlay on top). Remote Compose merges a component's draw blocks into a single `CanvasOperations` stream, so [`toModifier()`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/RcPlayerModifiers.kt) attaches one `Modifier.drawWithContent` at the position of the first `DrawContentOperation` in the modifier chain. That block runs the stream through `executeOperations`, which calls `drawContent()` (rendering the Compose children) when it reaches the stream's `DrawContent` op, so draws before and after it land behind and on top of the children.
1. **Touch Expressions & Gestures (**[**`RcPlayerTouch.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerTouch.kt)**,** [**`ClickModifier.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/ClickModifier.kt) **&** [**`ScrollModifier.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/ScrollModifier.kt)**)**: Compose's `Modifier.pointerInput` captures pointer down/drag/up events. `rootTouchExpressions` / `componentTouchExpressions` track velocity with a `VelocityTracker` and feed positions into `remote-core`'s `TouchExpression` (which computes spring/fling stop-points, notches, and settle animations); click and touch-action modifiers use `awaitEachGesture`. `MODIFIER_SCROLL` is driven by Compose's own `rememberScrollState` + fling (with a snapping fling for even-notch stops) rather than the core touch engine, and the live offset is published back to the scroll's bound position variable.
1. **`CollapsibleLayout` & `FitBoxLayout` (**[**`RcPlayerCollapsibleLayout.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerCollapsibleLayout.kt) **&** [**`RcPlayerFitBoxLayout.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerFitBoxLayout.kt)**)**: Collapsible rows/columns use a single-pass custom Compose `Layout` (`Measurable` / `Placeable`) that sorts children by `CollapsiblePriority` (when any child declares a priority) and measures them against remaining Compose `Constraints`; `FitBoxLayout` iterates its candidates using their minimum intrinsic sizes. Both update `child.mVisibility` so only fitting children are shown.
1. **Canvas Text Measurement (**[**`GraphPaintContext.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/GraphPaintContext.kt) **&** [**`RcPlayerTextDrawing.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerTextDrawing.kt)**)**: When procedural canvas operations call `getTextBounds`, `GraphPaintContext` measures with an `android.graphics.Paint` built from the current paint state (`getTextBounds`, `getFontMetrics`, `measureText`). Canvas text draw ops (`DrawText`, `DrawTextAnchored`, `DrawTextOnCircle`) lay out and draw text with Compose's `TextMeasurer`.

### 4.4 Operation Support Comparison: View Player vs. Embedded Player

[`Operations.java`](../../remote-core/src/main/java/androidx/compose/remote/core/Operations.java) defines the full wire protocol across `remote-core`. Because the View Player executes `CoreDocument.paint()` directly against `AndroidPaintContext`, it serves as the reference implementation. The Embedded Player covers the baseline, AndroidX and experimental operation families, some of them through optional features:

> [!NOTE]
> **Feature plugins.** Some operations are executed by optional [`RcPlayerFeature`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerFeature.kt)s rather than the built-in draw loop. The loop in [`RcPlayerDrawing.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerDrawing.kt) has no branch for them; any op it doesn't handle is offered to each feature's `paint()` in turn, and a paint op that no feature claims goes to [`UnknownOperations.onUnknown`](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/UnknownOperations.kt), which logs a warning once per operation class (or throws, under the `Fail` policy used by tests). Features can also run document-lifetime side effects in `Install()` (audio engines, sensor listeners).
>
> [`RcPlayerFeatureSet`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerFeatureSet.kt) fixes the features at build time so R8 can strip unused ones; `RcPlayer` overloads without a feature set use `RcPlayerFeatures.forProfiles(document.profileMask)` (all of `MatrixFeature`, `ParticlesFeature`, `MeshFeature`, `SoundFeature.Default`, `SensorFeature` that fit the profile). `BuiltInProfiles` is `PROFILE_ANDROIDX`; `FeatureOpcodes` lists the AndroidX-profile ops the loop leaves to features (matrix expression/vector math/from-path and the three particle ops), and `BuiltInExtraOpcodes` lists experimental ops still executed built in (`LAYOUT_FLOW`, `LAYOUT_CUSTOM`, `MODIFIER_MULTI_CLICK`, `MODIFIER_DIMENSION_CONSTRAINTS`). `RcPlayerFeatureSet.unsupportedOpcodes(profileMask)` returns the opcodes a profile allows that neither the loop nor the features execute, and `RcPlayer` logs a warning when it is non-empty.

| **Operation Family** | **Operations** | **View Player (`remote-player-view`)** | **Embedded Player (`remote-player-compose`)** | **Notes / Differences** |
| --- | --- | :---: | :---: | --- |
| **Core Layouts** | `LAYOUT_ROOT`, `LAYOUT_CONTENT`, `LAYOUT_BOX`, `LAYOUT_FIT_BOX`, `LAYOUT_ROW`, `LAYOUT_COLUMN`, `LAYOUT_FLOW`, `LAYOUT_CANVAS`, `LAYOUT_CANVAS_CONTENT`, `LAYOUT_STATE`, `LAYOUT_CUSTOM` | Full | Full | Embedded Player maps to native Compose `Box`, `Row`, `Column`, `FlowRow`, `Canvas`, `AnimatedContent` + `SharedTransitionLayout`, and [`CustomComposablePlugin`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerCustom.kt). Raw draw-list documents without a `RootLayoutComponent` render via [`RcPlayerRawDocument`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerComponents.kt). |
| **Collapsible Layouts** | `LAYOUT_COLLAPSIBLE_ROW`, `LAYOUT_COLLAPSIBLE_COLUMN`, `MODIFIER_COLLAPSIBLE_PRIORITY` | Full | Full | Embedded Player uses a custom single-pass Compose `Layout` ([`RcPlayerCollapsible`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerCollapsibleLayout.kt)) that hides lowest-priority children until the remaining children fit. |
| **Text & Image Layouts** | `LAYOUT_TEXT`, `CORE_TEXT`, `TEXT_STYLE`, `LAYOUT_IMAGE` | Full | Full (Minor diffs) | `RcPlayerImageLayout` maps `ImageScaling.SCALE_FIXED_SCALE` to `ContentScale.None` (1:1 fallback, [`RcPlayerImageLayout.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerImageLayout.kt)). `TEXT_STYLE` (`242`) is not read by `RcPlayerText` directly: core's `CoreText.updateVariables` merges the referenced `TextStyle` into the `CoreText` fields, which `RcPlayerText` reads (alongside the text's `PaintBundle`). |
| **Standard Modifiers** | `MODIFIER_WIDTH`, `MODIFIER_HEIGHT`, `MODIFIER_WIDTH_IN`, `MODIFIER_HEIGHT_IN`, `MODIFIER_DIMENSION_CONSTRAINTS`, `MODIFIER_PADDING`, `MODIFIER_BACKGROUND`, `MODIFIER_BORDER`, `MODIFIER_CLIP_RECT`, `MODIFIER_ROUNDED_CLIP_RECT`, `MODIFIER_OFFSET`, `MODIFIER_ZINDEX`, `MODIFIER_GRAPHICS_LAYER`, `MODIFIER_SCROLL`, `MODIFIER_MARQUEE`, `MODIFIER_RIPPLE`, `MODIFIER_ALIGN_BY`, `MODIFIER_VISIBILITY`, `MODIFIER_DRAW_CONTENT`, `ACCESSIBILITY_SEMANTICS`, `ANIMATION_SPEC` | Full | Full | [`RcPlayerModifiers.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/RcPlayerModifiers.kt) translates these modifier operations to native Compose `Modifier`s, coalescing click operations into a single `combinedClickable`. `MODIFIER_ALIGN_BY` is applied per child inside `Row` scope; `ANIMATION_SPEC` is consumed in the modifier chain. |
| **Procedural Layout Compute** | `LAYOUT_COMPUTE` ([`LayoutComputeOperation`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/layout/modifiers/LayoutComputeOperation.java)) | Full | No-op | Consumed without warning in [`RcPlayerModifiers.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/RcPlayerModifiers.kt); layout measurement/placement is owned by Compose UI. |
| **Touch, Click & Actions** | `MODIFIER_CLICK`, `MODIFIER_MULTI_CLICK`, `MODIFIER_TOUCH_DOWN/UP/CANCEL`, `TOUCH_EXPRESSION`, `HOST_ACTION`, `HOST_NAMED_ACTION`, `HOST_METADATA_ACTION`, `RUN_ACTION`, `EVENT_ACTION`, `VALUE_*_CHANGE_ACTION`, `HAPTIC_FEEDBACK`, `CLICK_AREA` | Full | Full | Canvas-level `CLICK_AREA` (`64`, non-component hit region) is rendered by [`RcPlayerClickAreas`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerClickAreas.kt) as clickable, labelled boxes that obscure targets below them, matching the View player's `ClickAreaView`. |
| **Root Content Behavior** | `ROOT_CONTENT_BEHAVIOR` (`65`) | Full | Full (raw documents) | For raw draw-list documents with `SIZING_SCALE`, [`RcPlayerRawDocument`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerComponents.kt) scales and aligns the document's own size into the player (mirroring `CoreDocument.computeScale` / `computeTranslate`); otherwise the document takes the player's size. Layout documents are sized by Compose. |
| **Expressions, Data & Collections** | `DATA_FLOAT`, `DATA_INT`, `DATA_BOOLEAN`, `DATA_LONG`, `DATA_TEXT`, `COLOR_CONSTANT`, `COLOR_THEME`, `NAMED_VARIABLE`, `ANIMATED_FLOAT` (RPN float expressions), `INTEGER_EXPRESSION`, `COLOR_EXPRESSIONS`, `TEXT_FROM_FLOAT`, `TEXT_MERGE`, `TEXT_LOOKUP`, `TEXT_LOOKUP_INT`, `TEXT_LENGTH`, `TEXT_SUBTEXT`, `TEXT_TRANSFORM`, `ID_MAP`, `ID_LIST`, `FLOAT_LIST`, `DYNAMIC_FLOAT_LIST`, `UPDATE_DYNAMIC_FLOAT_LIST`, `DATA_MAP_LOOKUP`, `ID_LOOKUP`, `ATTRIBUTE_TIME`, `ATTRIBUTE_TEXT`, `ATTRIBUTE_IMAGE`, `ATTRIBUTE_COLOR`, `TEXT_MEASURE`, `BITMAP_TEXT_MEASURE`, `COMPONENT_VALUE`, `WAKE_IN` | Full | Full | Evaluated reactively via [`RcPlayerExpression`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerExpression.kt) and [`GraphContext`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/GraphContext.kt) with per-pass O(V + E) DAG memoization. |
| **Canvas Primitives, Paths, Bitmaps & Text** | `DRAW_RECT`, `DRAW_ROUND_RECT`, `DRAW_CIRCLE`, `DRAW_OVAL`, `DRAW_LINE`, `DRAW_ARC`, `DRAW_SECTOR`, `DRAW_PATH`, `DRAW_TWEEN_PATH`, `DRAW_BITMAP`, `DRAW_BITMAP_INT`, `DRAW_BITMAP_SCALED`, `DRAW_TO_BITMAP`, `DRAW_TEXT_RUN`, `DRAW_TEXT_ANCHOR`, `DRAW_TEXT_ON_PATH`, `DRAW_TEXT_ON_CIRCLE`, `DRAW_BITMAP_FONT_TEXT_RUN`, `DRAW_BITMAP_TEXT_ANCHORED`, `DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH`, `DRAW_CONTENT`, `CLIP_RECT`, `CLIP_PATH`, `MATRIX_SAVE/RESTORE/TRANSLATE/SCALE/ROTATE/SKEW`, `DATA_PATH`, `PATH_CREATE`, `PATH_ADD`, `PATH_TWEEN`, `PATH_COMBINE`, `PATH_EXPRESSION` | Full | Full | Executed in [`RcPlayerDrawing.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerDrawing.kt) on Compose's `DrawScope` (including offscreen `DRAW_TO_BITMAP` canvas target redirection in [`RcPlayerOffscreenDrawing.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerOffscreenDrawing.kt)). |
| **Paint, Shaders, Gradients & PathEffects** | `PAINT_VALUES` (`COLOR`, `COLOR_ID`, `ALPHA`, `STROKE_*`, `STYLE`, `TEXT_SIZE`, `TYPEFACE`, `FONT_AXIS`, `BLEND_MODE`, `COLOR_FILTER`, `COLOR_FILTER_ID`, `CLEAR_COLOR_FILTER`, `FILTER_BITMAP`, `IMAGE_FILTER_QUALITY`, `GRADIENT` Linear/Radial/Sweep, `SHADER` AGSL `RuntimeShader`, `SHADER_MATRIX`, `TEXTURE`, `PATH_EFFECT` Dash/Discrete/PathDash/Sum/Compose) | Full | Full (`FALLBACK_TYPEFACE` no-op) | Decoded by `updatePaintFromBundle` in [`RcPlayerPaintBundle.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerPaintBundle.kt) into `ComposeLocalPaint` ([`RcPlayerPaint.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerPaint.kt)). `PaintBundle.FALLBACK_TYPEFACE` is consumed to stay word-aligned but not yet applied. AGSL shaders require `trustedContent` and API 33+. |
| **Loops, Conditionals & Functions** | `LOOP_START`, `CONDITIONAL_OPERATIONS`, `FUNCTION_DEFINE`, `FUNCTION_CALL` | Full | Full | `LOOP_START` includes a `MAX_LOOP_ITERATIONS = 100_000` safety guard in [`RcPlayerDrawing.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerDrawing.kt). |
| **Particles** | `PARTICLE_DEFINE`, `PARTICLE_LOOP`, `PARTICLE_COMPARE` | Full | Full (via `ParticlesFeature`) | Executed by [`ParticlesFeature`](../src/main/java/androidx/compose/remote/player/compose/embedded/ParticlesFeature.kt); part of `FeatureOpcodes`, so a player built with an `RcPlayerFeatureSet` that omits it skips these ops. |
| **Impulse Containers** | `IMPULSE_START`, `IMPULSE_PROCESS` | Full (Event-gated) | Full (Event-gated) | Mirrors `ImpulseOperation.paint`: the body only runs inside the `[startAt, startAt + duration]` window; the first pass in a window runs the setup list once and later passes run the trailing `ImpulseProcess`. Fire state is kept per loop iteration ([`RcPlayerDrawing.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerDrawing.kt), covered by `RcPlayerImpulseRetriggerTest`). |
| **Canvas Matrices** | `MATRIX_CONSTANT` (`186`), `MATRIX_EXPRESSION` (`187`), `MATRIX_VECTOR_MATH` (`188`), `MATRIX_FROM_PATH` (`181`) | Full | Full (via `MatrixFeature`) | `MATRIX_CONSTANT` is handled by the built-in loop (and used by `PaintBundle.SHADER_MATRIX`). `MATRIX_EXPRESSION`, `MATRIX_VECTOR_MATH` and `MATRIX_FROM_PATH` are executed by [`MatrixFeature`](../src/main/java/androidx/compose/remote/player/compose/embedded/MatrixFeature.kt); `MATRIX_FROM_PATH` concatenates the path's position/tangent matrix onto the canvas. `MATRIX_SET` (`132`) is only a reserved constant in `Operations.java` with no operation registered, so neither player executes it. |
| **2D Vertex Meshes** | `ADD_MESH_2D` (`104`), `DRAW_MESH_2D` (`105`), `MATRIX_FROM_MESH_2D` (`106`) | Full | Full (via `MeshFeature`) | Experimental-profile ops executed by [`MeshFeature`](../src/main/java/androidx/compose/remote/player/compose/embedded/MeshFeature.kt) through the core paint context shared with the View player (which caches mesh geometry across frames); mesh textures resolve through the host `RcImageLoader`. |
| **Loom / Pattern Macros & Includes** | `MACRO_DEFINE`, `MACRO_CALL`, `MACRO_ARGUMENT`, `MACRO_BLOCK`, `MACRO_FOR_EACH`, `REFERENCED_OPERATIONS`, `INCLUDE_REFERENCED_OPERATIONS` | Full | Full | Expanded during `CoreDocument.initFromBuffer()` (`LoomManager.expandAll` → `ExpansionContext.expandRecursive`) before either player inspects the document. |
| **Embedded Fonts** | `DATA_FONT` (`189`) | Full | Full (`trustedContent`) | Embedded `FontData` typefaces are built and cached by [`DocumentFonts.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/resources/DocumentFonts.kt) when the player is created with `trustedContent = true`; otherwise fonts resolve via `RcTypefaceResolver`, `device:`, and `google:` ([`GmsFontTypefaceResolver`](../src/main/java/androidx/compose/remote/player/compose/embedded/GmsFontTypefaceResolver.kt)). |
| **Audio** | `DATA_SOUND` (`169`), `SOUND_EXPRESSION` (`206`), `PLAY_SOUND` (`141`) | Full | Full (via `SoundFeature`) | Experimental-profile ops executed by [`SoundFeature`](../src/main/java/androidx/compose/remote/player/compose/embedded/SoundFeature.kt) through the View player's `SoundSupport`. An engine is only created for documents that contain sound ops; `PLAY_SOUND` fires each time the stream runs it, so documents gate it with an impulse or conditional. |
| **Device Sensors** | — (system variables `ID_ACCELERATION_*`, `ID_GYRO_ROT_*`, … `ID_LIGHT`) | Full | Full (via `SensorFeature`) | A host capability rather than opcodes: [`SensorFeature`](../src/main/java/androidx/compose/remote/player/compose/embedded/SensorFeature.kt) registers only the sensors whose variables the document listens to and writes readings into the document state. |

---

## 5. Deep Dive: `StateLayout` and Shared Element Transitions

[`StateLayout`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/layout/managers/StateLayout.java) represents a state-machine container whose children correspond to distinct UI states (the active child is selected by the integer variable `indexId`), optionally sharing elements identified by [`Component.mAnimationId`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/layout/Component.java).

In [`RcPlayerStateLayout.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerStateLayout.kt), the Embedded Player translates `StateLayout` into Compose's native `SharedTransitionLayout` and `AnimatedContent`:

### How It Works

1. **Subtree Inspection (`rememberLayoutTransitionConfig`)**: Before rendering, [`rememberLayoutTransitionConfig`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerStateLayout.kt) inspects `layout` and recursively walks its child subtrees (stopping at nested `StateLayout` or `FitBoxLayout` boundaries) to build a `LayoutTransitionConfig`:
    - The governing `AnimationSpec` (the layout's own spec, otherwise the child spec with the longest effective `motionDuration`; default 300 ms), with `motionEasingType` mapped to a Compose `Easing` via `mapEasing`.
    - Whether any descendant has a valid shared-element `animationId` (`comp.animationId != -1 && comp.animationId != 0`), and which IDs are *paired* (appear in more than one branch).
    - The overall `transitionDuration` (the longest of the motion duration and every component's enter/exit), and which branches fade their components individually instead of cross-fading as a whole.
1. **Instant Switch**: If `transitionDuration <= 0`, the active child is rendered directly in a `Box` with no `AnimatedContent`.
1. **Conditional `SharedTransitionLayout` Wrapping**: If `hasSharedElements` is `true`, `RcPlayerStateLayout` wraps `AnimatedContent` in a `SharedTransitionLayout`; the local `AnimatedStateContent` function provides `LocalSharedTransitionScope`, `LocalAnimatedVisibilityScope` and `LocalStateBranchScope` via `CompositionLocalProvider` inside the `AnimatedContent` body:

```kotlin
// RcPlayerStateLayout.kt (local function inside RcPlayerStateLayout)
@Composable
fun AnimatedStateContent(
    sharedTransitionScope: SharedTransitionScope?,
    contentModifier: Modifier,
) {
    AnimatedContent(
        targetState = targetIndex,
        modifier =
            contentModifier.rcComponentContentInspector(layout, forStateLayoutContent = true),
        contentAlignment = Alignment.Center,
        label = "RcPlayerStateLayout",
        transitionSpec = {
            // Branches whose components fade individually are not cross-faded as a whole.
            val enter =
                if (transitionConfig.componentFades(targetState)) EnterTransition.None
                else fadeIn(animationSpec = tween(durationMillis = fadeDuration, easing = easing))
            val exit = // ... mirror of enter using fadeOut / ExitTransition.None
            (enter togetherWith exit).using(
                SizeTransform(clip = false) { _, _ ->
                    if (motionDuration <= 0) snap()
                    else tween(durationMillis = motionDuration, easing = easing)
                }
            )
        },
    ) { currentIndex ->
        val branchScope = remember(this, transitionConfig, currentIndex) { StateBranchScope(/* ... */) }
        CompositionLocalProvider(
            LocalSharedTransitionScope provides sharedTransitionScope,
            LocalAnimatedVisibilityScope provides this@AnimatedContent,
            LocalStateBranchScope provides branchScope,
        ) {
            Box(contentAlignment = Alignment.Center) {
                RcPlayerComponent(children[currentIndex])
            }
        }
    }
}
```

1. **Automatic `Modifier.sharedBounds` Attachment**: On every descendant [`RcPlayerComponent`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerComponents.kt), [`Modifier.sharedElementTransition(component)`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerVisibility.kt) checks that both scopes are present and `component.animationId` is set:

```kotlin
// components/RcPlayerVisibility.kt
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.sharedElementTransition(component: Component): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current ?: return this

    val animationId = component.animationId
    if (animationId == -1 || animationId == 0) return this

    val spec =
        (component as? LayoutComponent)?.componentModifiers?.list
            ?.fastFirstOrNull { it is AnimationSpec } as? AnimationSpec
            ?: component.animationSpec
            ?: AnimationSpec.DEFAULT
    val motionDuration = spec.motionDuration.toInt()
    val motionEasing = mapEasing(spec.motionEasingType)
    val boundsTransform = remember(motionDuration, motionEasing) {
        BoundsTransform { _, _ ->
            if (motionDuration <= 0) snap() else tween(durationMillis = motionDuration, easing = motionEasing)
        }
    }
    val fadeSpec = // ... snap() or tween<Float>(motionDuration, easing = motionEasing)

    with(sharedTransitionScope) {
        return this@sharedElementTransition.sharedBounds(
            sharedContentState = rememberSharedContentState(key = animationId),
            animatedVisibilityScope = animatedVisibilityScope,
            enter = fadeIn(animationSpec = fadeSpec),
            exit = fadeOut(animationSpec = fadeSpec),
            boundsTransform = boundsTransform,
            resizeMode = ResizeMode.RemeasureToBounds,
        )
    }
}
```

When `targetIndex` changes, Compose's `SharedTransitionLayout` automatically interpolates the position and size bounds of matching `animationId` components across the outgoing and incoming state trees while cross-fading non-shared elements and smoothly morphing the container's outer dimensions via `SizeTransform(clip = false)`. `RcPlayerFitBoxLayout` provides the same composition locals for its own `AnimatedContent`.

---

## 6. Deep Dive: Font and Image Loading (Including Coil Integration)

### 6.1 Font Resolution Architecture

Remote Compose documents can specify fonts in five ways:

1. **Standard System Families (`0..3`)**: `default`, `sans-serif`, `serif`, `monospace`.
1. **Device Font Families (`device:<name>`)**: System-installed OEM fonts (e.g., `device:google-sans`, `device:roboto-flex`) with optional OpenType variation axes (`wght`, `wdth`, `ital`, `GRAD`, `ROND`).
1. **Downloadable Google Fonts (`google:<name>`)**: Fonts fetched asynchronously from the GMS font provider (`com.google.android.gms.fonts`) when a [`GmsFontTypefaceResolver`](../src/main/java/androidx/compose/remote/player/compose/embedded/GmsFontTypefaceResolver.kt) is supplied; otherwise the prefix is stripped and the name is resolved as a system font.
1. **Document-Embedded Fonts (`FontData`)**: Font bytes carried in the document (an object id `> 3` with no family name), built into a `Typeface` by [`DocumentFonts.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/resources/DocumentFonts.kt) only when the player runs with `trustedContent = true`.
1. **Custom Host-Provided Typefaces**: Resolved via a host-supplied suspending [`RcTypefaceResolver`](../src/main/java/androidx/compose/remote/player/compose/embedded/EmbeddedPlayerTypefaceResolver.kt), passed as `RcPlayer(typefaceResolver = ...)` (default: `LocalRcTypefaceResolver.current`).

In [`RcPlayerFontResolution.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/resources/RcPlayerFontResolution.kt), `resolveFontFamily` serves the Compose `BasicText` pipeline (called from [`components/RcPlayerTextLayout.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerTextLayout.kt) and [`draw/RcPlayerPaint.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/draw/RcPlayerPaint.kt)), and `resolveTypeface` serves the Canvas draw pipeline (native `Paint`). Both build an `RcTypefaceRequest` (`buildTypefaceRequest`) and go through the per-player [`RcTypefaceCache`](../src/main/java/androidx/compose/remote/player/compose/embedded/resources/RcTypefaceCache.kt):

- **OpenType Variable Font Axes (`buildFontVariationSettings`)**: Extracts `fontAxis` tag IDs and `fontAxisValues` from `CoreText`, resolves their string names from `RemoteContext` (keeping only variation-axis tags), automatically injects `wght` (and `ital` for italic styles) if omitted, and constructs a Compose `FontVariation.Settings`. `RcTypefaceCache.deviceFontFamily` turns this into `FontFamily(Font(DeviceFontFamilyName(familyName), weight, style, variationSettings))`, cached per input. `buildFontVariationSettingsString` produces the equivalent string form used in `RcTypefaceRequest.variationSettings` and for document fonts.
- **Downloadable GMS Fonts (**[**`GmsFontTypefaceResolver`**](../src/main/java/androidx/compose/remote/player/compose/embedded/GmsFontTypefaceResolver.kt)**)**:
    - `GmsFontTypefaceResolver(context, fontCertsResId, delegate)` is an `RcTypefaceResolver`. For `google:` families it issues `FontsContractCompat.requestFont(...)` for the *base* (axis-free) typeface, caches and de-duplicates loads, and otherwise delegates (default `EmbeddedPlayerTypefaceResolver`). The player applies `variationSettings` to the returned typeface itself, so animated axes don't trigger new downloads.
    - `RcTypefaceCache.resolve` starts the suspending resolution and, until it completes, returns the value of a snapshot `MutableState<Typeface?>` (initially `null`, so the caller falls back to the default typeface). When the font arrives the state is updated, so composables and canvas draws that read it are invalidated and re-run with the downloaded font. For `BasicText`, the resolved `Typeface` is wrapped as `FontFamily(Typeface(typeface))`.
- **Custom `RcTypefaceResolver` Injection**: Any non-default `RcTypefaceResolver` (a host's, `GmsFontTypefaceResolver`, or a Robolectric test's) is routed through `RcTypefaceCache`; the built-in [`EmbeddedPlayerTypefaceResolver`](../src/main/java/androidx/compose/remote/player/compose/embedded/EmbeddedPlayerTypefaceResolver.kt) instead falls through to Compose's native `FontFamily.SansSerif` / `DeviceFontFamilyName` resolution. `RcTypefaceCache.coreTypefaceResolver` adapts the same cache to `remote-player-core`'s synchronous [`TypefaceResolver`](../../remote-player-core/src/main/java/androidx/compose/remote/player/core/platform/TypefaceResolver.java) for core code paths that use `AndroidRemoteContext`.

### 6.2 Pluggable Image Loading (`RcImageLoader` & Coil Integration)

To keep `remote-player-compose` free of transitive third-party networking or image-caching dependencies while supporting rich host image pipelines, image resolution is abstracted behind [`RcImageLoader`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcImageLoader.kt), passed as `RcPlayer(imageLoader = ...)` and exposed to the tree as `LocalRcImageLoader`:

```kotlin
// RcImageLoader.kt
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public fun interface RcImageLoader {
    /** A reactive holder for the [Drawable] of `bitmapId`; `null` until/unless one is available. */
    public fun loadImage(bitmapId: Int): State<Drawable?>
}
```

Implementations must return a **stable** `State` per `bitmapId` (cache per id) so the composable and canvas paths observe the same value.

#### Why `State<Drawable?>`?

Returning `State<Drawable?>` allows a single interface to drive **both** rendering paths in the Embedded Player:

1. **`@Composable RcPlayerImageLayout`** ([`layout/RcPlayerImageLayout.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/layout/RcPlayerImageLayout.kt)): Observes `val drawable by LocalRcImageLoader.current.loadImage(layout.bitmapId)`. When an async load completes, only that `RcPlayerImageLayout` recomposes. If the `Drawable` is a `BitmapDrawable`, it renders via `BitmapPainter(resolved.bitmap.asImageBitmap())`; for arbitrary drawables (animated vector drawables, custom Coil/Glide drawables), it wraps them in [`DrawablePainter`](../src/main/java/androidx/compose/remote/player/compose/embedded/resources/DrawablePainter.kt).
1. **Canvas Draw Operations (`DrawBitmap`, `DrawBitmapInt`, `DrawBitmapScaled`)**: Inside a non-composable Canvas `DrawScope`, `resolveCanvasBitmap` in [`resources/RcImageResources.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/resources/RcImageResources.kt) reads `graph.imageLoader.loadImage(id).value` (see [`GraphContext.imageLoader`](../src/main/java/androidx/compose/remote/player/compose/embedded/doc/GraphContext.kt)) imperatively during drawing. Because that read happens inside Compose's `DrawScope`, Compose records a snapshot read dependency for the draw phase—when the async image finishes loading and updates the `State<Drawable?>`, **the `Canvas` automatically invalidates and redraws without recomposing**. The blit ops need a `Bitmap`, so only a `BitmapDrawable` result is used directly; any other drawable (or `null`) falls back to the document's embedded bitmap.

#### Default Loader (`EmbeddedRcImageLoader`)

By default, the internal [`EmbeddedRcImageLoader`](../src/main/java/androidx/compose/remote/player/compose/embedded/resources/RcImageResources.kt) lazily decodes embedded `BitmapData` bytes on the first `loadImage(bitmapId)` request (avoiding upfront decode spikes during document initialization) and caches a `derivedStateOf` per `bitmapId` that reads the decoded bitmap from the snapshot-backed `SnapshotRemoteComposeState`, so a host swap of the underlying bitmap updates the returned `State`.

Documents that reference images by URL or file (`BitmapData.ENCODING_URL` / `ENCODING_FILE`) can only be parsed once [`RemoteImageSupport.enableEncodedImageReferences()`](../src/main/java/androidx/compose/remote/player/compose/embedded/resources/RemoteImageSupport.kt) has set `Limits.ENABLE_IMAGE_URLS` / `Limits.ENABLE_IMAGE_FILES`; actual fetching is left to the image loader.

#### Integrating Coil (`CoilRcImageLoader`)

> [!NOTE]
> The following is an illustrative example; it is not part of the repository, and `remote-player-compose` has no Coil dependency. `RcImageLoader` and `RcPlayer` are currently `@RestrictTo(LIBRARY_GROUP)`.

A host application using **Coil 3** (`io.coil-kt.coil3`) can plug in a custom `RcImageLoader` that maps `BitmapData` ids to URLs (or host-mapped asset IDs) and delegates fetching, disk/memory caching, and decoding to Coil's `ImageLoader`:

```kotlin
// Illustrative only — not in the repository.
class CoilRcImageLoader(
    private val appContext: Context,
    private val coilImageLoader: ImageLoader, // coil3.ImageLoader
    private val urlByBitmapId: Map<Int, String>,
) : RcImageLoader {
    private val cache = HashMap<Int, State<Drawable?>>()

    override fun loadImage(bitmapId: Int): State<Drawable?> =
        cache.getOrPut(bitmapId) {
            val state = mutableStateOf<Drawable?>(null)
            val url = urlByBitmapId[bitmapId] ?: return@getOrPut state
            val request =
                ImageRequest.Builder(appContext)
                    .data(url)
                    .target(
                        onStart = { placeholder -> state.value = placeholder?.asDrawable(appContext.resources) },
                        onSuccess = { result -> state.value = result.asDrawable(appContext.resources) },
                        onError = { error -> state.value = error?.asDrawable(appContext.resources) },
                    )
                    .build()
            coilImageLoader.enqueue(request)
            state
        }
}

// Usage at the host call-site:
RcPlayer(
    state = playerState,
    imageLoader = remember(urls) {
        CoilRcImageLoader(context, SingletonImageLoader.get(context), urls)
    },
)
```

> [!NOTE]
> A custom loader replaces `EmbeddedRcImageLoader` (which is internal and cannot be delegated to). For ids the custom loader returns `null` for, canvas `DrawBitmap*` ops still fall back to the embedded decode, but `RcPlayerImageLayout` draws nothing, so a host loader should cover every id used by image layouts.

---

## 7. Deep Dive: Custom Components (`@Composable` Plugins vs. `View` Implementations)

Remote Compose's [`Custom`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/layout/managers/Custom.java) layout component (`LAYOUT_CUSTOM` opcode) allows a document to embed a host-implemented widget identified by a `config` name (a constant string or a text ID, e.g., `"support:edit-text"`, `"SupportSpannableString"`), parameterized by typed input properties and two-way return channels (`Custom.CustomProperty` data types):

- **Input Properties**: `INT_PROP`, `INT_ID_PROP`, `FLOAT_PROP`, `STRING_PROP`, `COLOR_PROP`, `COLOR_ID_PROP` (each carrying a `Short` property ID; the `*_ID_PROP` variants reference a reactive document variable instead of a constant, `FLOAT_PROP` may hold a NaN-encoded float variable, and `STRING_PROP` references a text ID).
- **Return Channels**: `INT_RETURN`, `FLOAT_RETURN`, `TEXT_RETURN`, `COLOR_RETURN` (binding a `Short` property ID to a target variable/text ID in the document so the custom component can write user input or state back into the document). The embedded player currently exposes write-back handlers for `FLOAT_RETURN` and `TEXT_RETURN` only.

### 7.1 Comparison: Embedded Player vs. View Player vs. Canvas Overlay

| **Aspect** | **Embedded Player (**[**`RcPlayerCustom.kt`**](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerCustom.kt)**)** | **View Player (**[**`AndroidCustomSupport.java`**](../../integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/customviews/AndroidCustomSupport.java)**)** | **Single-Canvas Compose Player (**[**`ComposeCustomSupport.kt`**](../src/main/java/androidx/compose/remote/player/compose/custom/ComposeCustomSupport.kt)**)** |
| --- | --- | --- | --- |
| **Host Implementation Type** | Native `@Composable` via [`CustomComposablePlugin<T>`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerCustom.kt). | Imperative `android.view.View` created by per-config [`AndroidComponentSupport`](../../remote-player-core/src/main/java/androidx/compose/remote/player/core/platform/AndroidComponentSupport.java) delegates, dispatched through an `AndroidCustomContext`. | `@Composable` rendered in a separate overlay `Box` above the single root `Canvas`. |
| **Layout & Measurement** | Placed **in-line** in the Compose layout tree; participates natively in parent `Row`/`Column`/`Box` intrinsic & constraint measurement. | Imperative `measureCustom(id, bounds)` builds `View.MeasureSpec`s and calls `view.measure()` + `view.layout()` during `remote-core`'s pass. | Measured by `remote-core`, then positioned via `Modifier.offset(x, y).size(w, h)` in a top-level overlay layer. |
| **Z-Ordering, Clipping & Scroll** | Naturally inherits parent clipping, `graphicsLayer` transforms, `StateLayout` transitions, and `Modifier.verticalScroll`. | Drawn onto `mCanvas` (`view.draw(mCanvas)`) or attached to parent `FrameLayout` with manual margin offsets for IME focus (`EditText`). | Always sits on top of the root `Canvas`; cannot interleave behind sibling canvas elements or parent clips. |
| **Property Updates** | Reactive Compose `State<T>` (`component.intState()`, `floatState()`, `textState()`, `colorState()`). | Imperative `configureCustom(id, type, value)` callback mutating the `View` instance on each frame. | Imperative `configureCustom` writing into per-component `SnapshotStateMap`s (`stringProps`, `intProps`, `floatProps`, keyed by property type) on a `ComponentState`. |
| **Touch & IME Input** | Native Compose pointer input, focus management, and `SoftwareKeyboardController` (`BasicTextField`). | Manual `MotionEvent.obtain(...)` synthesis in `touchCustom(id, type, x, y)` or `FrameLayout` child attachment. | Native Compose pointer input on the overlay `Box` (`touchCustom` is a no-op returning `false`). |

### 7.2 Embedded Player Two-Phase Plugin Architecture (`CustomComposablePlugin<T>`)

In [`RcPlayerCustom.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerCustom.kt), host plugins implement a type-safe two-phase contract registered in a [`CustomPluginRegistry`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerCustom.kt) (keyed by each plugin's `name`) and passed to `RcPlayer` via its `customPlugins` parameter, which provides it as `LocalRcCustomPlugins`. [`components/RcPlayerCustomComponent.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerCustomComponent.kt) resolves the component's config, wraps it in an `RcCustomComponent`, and calls `CustomPluginRegistry.Render`, which looks up the plugin by `component.config` and runs `extract` then (if non-null) `Content`:

```kotlin
// RcPlayerCustom.kt
public interface CustomComposablePlugin<T : Any> {
    public val name: String
    @Composable public fun extract(component: RcCustomComponent): T?
    @Composable public fun Content(data: T, component: RcCustomComponent, modifier: Modifier)
    public val handlesClick: Boolean
        get() = false
}
```

1. **`extract(component: RcCustomComponent): T?`**: Binds typed property keys (`IntProperty`, `FloatProperty`, `StringProperty`, `ColorProperty`) to reactive Compose `State<T>` instances (`component.intState(prop)`, `component.textState(prop)`, etc.) and binds return channels (`TextReturnProperty`, `FloatReturnProperty`) to write-back lambdas (`component.returnTextHandler(prop)`, `component.returnFloatHandler(prop)`). Returning `null` skips rendering.
1. **`Content(data: T, component: RcCustomComponent, modifier: Modifier)`**: Renders the `@Composable` UI using the extracted `data` model. The player applies the Remote Compose sizing, padding, clip, and semantics modifiers to a wrapping `Box` and renders the plugin inside it, so `Content` receives a plain `Modifier`.

`RcCustomComponent` can also host remote child components (`childCount`, `Child(index)`, `Children()`, `customChild(index)`), and a plugin that sets `handlesClick = true` receives the component's click actions as `onClick` / `onDoubleClick` / `onLongClick` instead of the player applying click modifiers to the wrapper.

#### Concrete Example 1: Interactive Two-Way `SupportEditTextPlugin`

In [`SupportEditText.kt`](../../integration-tests/player-compose-embedded/src/main/java/androidx/compose/remote/player/compose/embedded/demos/SupportEditText.kt), a custom `EditText` component binds reactive text, hint, color, and font-size states, renders a Compose `BasicTextField`, and streams user keystrokes back into the document's shared text variable via `returnTextHandler(RET_TEXT)`:

```kotlin
// SupportEditText.kt
public object SupportEditTextPlugin : CustomComposablePlugin<SupportEditTextData> {
    override val name: String = "support:edit-text"
    @Composable
    override fun extract(component: RcCustomComponent): SupportEditTextData? {
        val rawTextColorState = component.intState(SupportEditTextData.TEXT_COLOR)
        val textColorState = remember {
            derivedStateOf {
                val argb = rawTextColorState.value
                if (argb != 0) Color(argb) else Color.Unspecified
            }
        }
        // ... extracts textSizeState and backgroundColorState ...
        return SupportEditTextData(
            textState = component.textState(SupportEditTextData.TEXT),
            hintState = component.textState(SupportEditTextData.HINT),
            textColorState = textColorState,
            textSizeState = textSizeState,
            backgroundColorState = backgroundColorState,
            config = component.config,
            onTextChanged = component.returnTextHandler(SupportEditTextData.RET_TEXT),
        )
    }
    @Composable
    override fun Content(
        data: SupportEditTextData,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        var textValue by remember(data.text) { mutableStateOf(data.text) }
        val textStyle = TextStyle(color = data.textColor, fontSize = data.textSize)
        Box(
            modifier = modifier.background(data.backgroundColor).fillMaxSize(),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = { newText ->
                    textValue = newText
                    data.onTextChanged(newText) // writes back into the document's text
                },
                textStyle = textStyle,
                // ...
            )
            // ... renders data.hint with BasicText when textValue is empty ...
        }
    }
}
```

When `data.onTextChanged(newText)` is called, `returnTextHandler` looks up the `TEXT_RETURN` property and invokes `remoteContext.loadText(targetId, newText)`, which updates the text in the player's `SnapshotRemoteComposeState`. Any `RemoteText` or text expression in the document referencing `targetId` then recomposes with the new value (verified in [`RcPlayerCustomComponentTest.supportEditTextUpdatesSharedTextState`](../src/test/java/androidx/compose/remote/player/compose/embedded/RcPlayerCustomComponentTest.kt), which asserts both the edit field and a `RemoteText` bound to the same `MutableRemoteString` show the updated value; the test uses a copy of the demo plugin under `src/test/java/.../embedded/demos/`).

#### Concrete Example 2: Rich Hyperlinks via `SupportSpannableStringPlugin`

In [`SupportSpannableString.kt`](../../integration-tests/player-compose-embedded/src/main/java/androidx/compose/remote/player/compose/embedded/demos/embedded/SupportSpannableString.kt), a custom component (config `"SupportSpannableString"`) reads `PROP_TEXT`, `PROP_TEXT_COLOR`, `PROP_TEXT_SIZE`, `PROP_LINK_COUNT` and indexed link ranges (`PROP_LINK_URL_BASE + i`, `PROP_LINK_START_BASE + i`, `PROP_LINK_END_BASE + i`) and constructs a Compose `AnnotatedString` with `LinkAnnotation.Url` rendered by `BasicText`—giving Remote Compose documents native clickable hyperlinks with TalkBack link actions.

---

## 8. Accessibility (A11y) and Testing

### 8.1 Native Compose Semantics Mapping

Unlike the View Player—which must maintain a separate `ExploreByTouchHelper` virtual view tree synchronized with `Component` bounds—the Embedded Player maps Remote Compose's [`CoreSemantics`](../../remote-core/src/main/java/androidx/compose/remote/core/semantics/CoreSemantics.java) ([`AccessibleComponent`](../../remote-core/src/main/java/androidx/compose/remote/core/semantics/AccessibleComponent.java)) modifier directly onto each component's `LayoutNode`. [`RcPlayerModifiers.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/RcPlayerModifiers.kt) dispatches each `CoreSemantics` op to the `Modifier.semantics(op: CoreSemantics)` extension in [`SemanticsModifier.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/SemanticsModifier.kt), which emits `Modifier.semantics` / `Modifier.clearAndSetSemantics`:

- **Merge Modes** (`AccessibleComponent.Mode`, serialized as its ordinal):
    - `Mode.SET` (0) > `Modifier.semantics(mergeDescendants = false)`
    - `Mode.CLEAR_AND_SET` (1) > `Modifier.clearAndSetSemantics`
    - `Mode.MERGE` (2) > `Modifier.semantics(mergeDescendants = true)`
- **Reactive Semantic Properties**: `mContentDescriptionId` and `mTextId` are observed via `rememberRemoteStringAsState(id)` (from [`RcPlayerState.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/state/RcPlayerState.kt)) and set as `contentDescription` and `text`. When the backing string variable changes, the `SemanticsNode` updates reactively without rebuilding the tree. If the op has no content description, text or role, no semantics modifier is added.
- **Roles**: Maps `AccessibleComponent.Role` (`BUTTON`, `CHECKBOX`, `SWITCH`, `RADIO_BUTTON`, `TAB`, `IMAGE`, `DROPDOWN_LIST`) to the matching `androidx.compose.ui.semantics.Role`. `PICKER`, `CAROUSEL` and any other value have no Compose equivalent and fall back to `Role.Button`.

> [!NOTE]
> `CoreSemantics` also carries `mStateDescriptionId` and `mEnabled`, but `SemanticsModifier.kt` does not map them yet: there is no `stateDescription` or `disabled()` semantics on the embedded player. There is no heading mapping either.

- **Root Document Description**: The document's root content description ([`Header.DOC_CONTENT_DESCRIPTION`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/Header.java) / [`RootContentDescription`](../../remote-core/src/main/java/androidx/compose/remote/core/operations/RootContentDescription.java), resolved to `CoreDocument.contentDescription`) is attached to the root `Box` in [`RcPlayer.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayer.kt) via `Modifier.semantics { contentDescription = rootContentDescription }` when it is non-null.

### 8.2 Shared Unit & Instrumentation Testing

Because the Embedded Player emits real Compose `LayoutNode`s and `SemanticsNode`s, tests can run both on the JVM via Robolectric (`src/test`) and on physical/emulated Android devices (`src/androidTest`). Helpers in `src/sharedTest` are compiled into both source sets.

[`RcPlayerTestRule`](../src/sharedTest/java/androidx/compose/remote/player/compose/embedded/RcPlayerTestRule.kt) wraps [`RemoteBaseContentTestRule`](../../remote-testing/src/main/java/androidx/compose/remote/testing/RemoteBaseContentTestRule.kt) (and is itself a `ComposeContentTestRule`). It turns on `RemoteComposePlayerFlags.isEmbeddedPlayerEnabled` for the test, and its `setRemoteContent { ... }` captures the Remote Compose DSL into a document and plays it back through `RcPlayer` (using `RcPlayerFeatures.forProfiles(...)` unless the test overrides `features`). That gives end-to-end DSL capture > `RcPlayer` playback > semantics and layout assertions on the JVM. An abbreviated example from [`RcPlayerSemanticsModifierTest.kt`](../src/test/java/androidx/compose/remote/player/compose/embedded/RcPlayerSemanticsModifierTest.kt):

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerSemanticsModifierTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun semanticsRoleSurfacesToTheAccessibilityTree() {
        rule.setRemoteContent {
            RemoteBox(
                modifier =
                    RemoteModifier.semantics {
                            contentDescription = "switchy".rs
                            role = RemoteRole.Switch
                        }
                        .size(40.rdp)
                        .background(Color(0xFF3F51B5).rc)
            )
        }

        rule
            .onNodeWithContentDescription("switchy")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
    }

    @Test
    fun mergedSemanticsCombineDescendantsIntoOneNode() {
        rule.setRemoteContent {
            RemoteColumn(
                modifier =
                    RemoteModifier.semantics(mergeDescendants = true) {
                            contentDescription = "outer".rs
                        }
                        .size(100.rdp)
            ) {
                RemoteText(text = "inner label".rs)
            }
        }

        // MERGE folds the descendants' semantics into the parent node.
        rule.onNodeWithContentDescription("outer").assertTextEquals("inner label")
    }

    // ...
}
```

The same rule drives on-device a11y tests such as [`RcPlayerBasicA11yTest.kt`](../src/androidTest/java/androidx/compose/remote/a11y/RcPlayerBasicA11yTest.kt) and [`RcPlayerLayoutA11yTest.kt`](../src/androidTest/java/androidx/compose/remote/player/compose/embedded/RcPlayerLayoutA11yTest.kt).

Two Robolectric suites cover update behaviour:

1. [`RcPlayerRecompositionEventsTest.kt`](../src/test/java/androidx/compose/remote/player/compose/embedded/RcPlayerRecompositionEventsTest.kt) covers the creation side. It captures a document with `captureRemoteDocument`, feeds each emitted document into `RcPlayer`, and checks that Compose state changes in the creation composition (`mutableStateOf`, `derivedStateOf`, `LaunchedEffect`, `collectAsStateWithLifecycle`, boolean display branches) re-emit a document that the player shows (e.g. `"Initial"` becomes `"Updated"`). It also checks that state changes that leave the layout unchanged emit no new document.
1. [`RcPlayerRecompositionAndRepaintTest.kt`](../src/test/java/androidx/compose/remote/player/compose/embedded/RcPlayerRecompositionAndRepaintTest.kt) advances virtual time frame by frame. For each document category it counts Snapshot apply batches (`snapshotWrites`) and frames where a tracked node's bounds changed (`boundsChanges`). For example, a static document gets **0** of each after the initial render. A canvas-only continuous-time expression writes state on nearly every frame (≥ 9 of 10 frames) with **0** bounds changes. Seconds- and month-based expressions only update when that time unit changes.

---

## 9. Inspectability

### 9.1 Zero-Overhead `RcPlayerInspector` & Android Studio Layout Inspector Integration

Debugging a serialized wire format inside a Compose hierarchy can be opaque without dedicated tooling. The [`inspectable/`](../src/main/java/androidx/compose/remote/player/compose/embedded/inspectable/) package ([`RcPlayerInspector.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/inspectable/RcPlayerInspector.kt), [`RcComponentInspectable.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/inspectable/RcComponentInspectable.kt), [`RcModifierInspectable.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/inspectable/RcModifierInspectable.kt), [`RcOperationInspectable.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/inspectable/RcOperationInspectable.kt)) provides an inspection system built on Compose's `InspectableValue`, `ModifierNodeElement` and `LayoutInfo`. It is gated by Compose UI's global `isDebugInspectorInfoEnabled` flag (`androidx.compose.ui.platform`), which Layout Inspector turns on:

1. **Zero Production Overhead when Disabled**: When `isDebugInspectorInfoEnabled` is `false` (the default in production), `rcPlayerRootInspector` (applied in [`RcPlayer.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayer.kt)), `rcComponentOuterInspector` and `rcComponentContentInspector` (applied in [`RcPlayerComponents.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/components/RcPlayerComponents.kt)), and `rcModifierInspector` (applied in [`RcPlayerModifiers.kt`](../src/main/java/androidx/compose/remote/player/compose/embedded/modifier/RcPlayerModifiers.kt)) all go through the inline `Modifier.remoteInspectable { ... }` helper. That helper returns `this` (`Modifier`) without allocating an element or creating a modifier node.

    ```kotlin
    internal inline fun Modifier.remoteInspectable(element: () -> Modifier.Element): Modifier =
        if (isDebugInspectorInfoEnabled) this.then(element()) else this
    ```

1. **Android Studio Layout Inspector Metadata**: When enabled, the player attaches lightweight `ModifierNodeElement`s that hold only object references and compute their properties lazily in `InspectorInfo.inspectableProperties()`:
    - `rcPlayerRoot`: `state`, `document`, `componentId`.
    - One element per component, named by type (`rcRootLayout`, `rcBoxLayout`, `rcColumnLayout`, `rcCoreText`, `rcStateLayout`, …): `componentId`, `kind`, `visibility`, `x`/`y`/`width`/`height`, `modifiers`, `children`, plus type-specific fields (e.g. resolved text, colour and font size for `rcCoreText`, `currentIndex` for `rcStateLayout`).
    - `rcComponentContent`: marks the post-padding content node of a component.
    - One element per modifier op (`rcPadding`, `rcWidth`, `rcBackground`, `rcGraphicsLayer`, `rcVisibility`, `rcSemantics`, `rcClick`, …).

    In addition, [`RcPlayerState`](../src/main/java/androidx/compose/remote/player/compose/embedded/RcPlayerState.kt) is itself an `InspectableValue` that exposes `document`, `remoteContext`, `currentTimeMillis`, `rootComponent`, `variables`, `functions` and `operations` with live evaluated values. The root and content nodes also publish `RcPlayerInspector.PlayerStateKey`, `ComponentIdKey` and `ContentComponentIdKey` semantics properties.
1. **Programmatic Tree Snapshots (`captureTreeSnapshot()`)**: Internal tooling and tests can call `RcPlayerInspector.captureTreeSnapshot(rootSemanticsNode, state)`. It walks the semantics tree's `LayoutInfo.getModifierInfo()` to find the `rcPlayerRoot` / component / `rcComponentContent` elements and returns a `List<RcPlayerInspector.TreeNodeSnapshot>`. Each entry carries the component `id`, `kind`, laid-out `x`/`y`/`width`/`height` (relative to the parent's content node), absolute `absX`/`absY`, tree `depth`, `isGone` and `visibility`. For a `StateLayout`, the size comes from the active child. This requires `isDebugInspectorInfoEnabled` to have been `true` when `RcPlayer` was composed. [`RcPlayerInspectorTest.kt`](../src/test/java/androidx/compose/remote/player/compose/embedded/RcPlayerInspectorTest.kt) checks both modes: no `rc*` inspector modifiers when the flag is off, and the expected inspectables, coordinates and variable values when it is on.

> [!NOTE]
> `RcPlayerInspector` is an `internal object`, not a public API. The inspectors are switched on by the global `isDebugInspectorInfoEnabled` flag, not by a `CompositionLocal`.

---
