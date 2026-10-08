/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress(
    "RestrictedApiAndroidX",
    "PrimitiveInCollection",
    "VisibleForTests",
    "RememberReturnType",
    "ModifierParameter",
    "AutoboxingStateCreation",
)

package androidx.compose.remote.player.compose.embedded

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.collection.IntObjectMap
import androidx.collection.IntSet
import androidx.collection.MutableIntList
import androidx.collection.emptyIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableIntSetOf
import androidx.compose.animation.core.Easing as ComposeEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limiter
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableProvider
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.PathCombine
import androidx.compose.remote.core.operations.PathCreate
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.PathExpression
import androidx.compose.remote.core.operations.PathTween
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.TextLookupInt
import androidx.compose.remote.core.operations.TextMeasure
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.WakeIn
import androidx.compose.remote.core.operations.layout.CanvasContent
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.FlowLayout
import androidx.compose.remote.core.operations.layout.managers.ImageLayout
import androidx.compose.remote.core.operations.layout.managers.LayoutManager
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.matrix.MatrixConstant
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.NanMap
import androidx.compose.remote.core.operations.utilities.easing.Easing as RemoteEasing
import androidx.compose.remote.core.types.BooleanConstant
import androidx.compose.remote.core.types.IntegerConstant
import androidx.compose.remote.core.types.LongConstant
import androidx.compose.remote.creation.compose.action.LambdaAction
import androidx.compose.remote.creation.compose.action.PendingIntentAction
import androidx.compose.remote.creation.compose.capture.CapturedDocument
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerBox
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerColumn
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerFitBoxLayout
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerFlowRow
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerImageLayout
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerRow
import androidx.compose.remote.player.compose.embedded.layout.RcPlayerStateLayout
import androidx.compose.remote.player.compose.embedded.state.hasAnimation
import androidx.compose.remote.player.compose.embedded.state.rememberAnimatedRemoteFloat
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.remote.player.core.state.StateUpdaterImpl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * A player of a [CoreDocument].
 *
 * **One player per document.** First composition installs this player's runtime state *onto the
 * document* (swaps in a [SnapshotRemoteComposeState], re-gathers collections, applies operations),
 * so a given [CoreDocument] instance is bound to a single `RcPlayer`. Don't drive two players from
 * the same `CoreDocument` concurrently — give each its own document (re-`initFromBuffer`).
 * Re-installing onto an already-initialized document is guarded against below so an accidental
 * reuse doesn't clobber existing state, but the two players would then share one state, which is
 * not supported.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApiAndroidX")
@Suppress("PrimitiveInCollection")
@Composable
public fun RcPlayer(
    state: RcPlayerState,
    modifier: Modifier = Modifier,
    imageLoader: RcImageLoader? = null,
    isShaderValid: (shaderSource: String) -> Boolean = { true },
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    customPlugins: CustomPluginRegistry? = null,
    lambdas: IntObjectMap<() -> Unit> = emptyIntObjectMap(),
    pendingIntents: IntObjectMap<PendingIntent> = emptyIntObjectMap(),
    typefaceResolver: TypefaceResolver? = LocalTypefaceResolver.current,
    theme: Int = Theme.SYSTEM,
) {
    check(RemoteComposePlayerFlags.isEmbeddedPlayerEnabled) {
        "Embedded player is disabled. Set RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true to enable."
    }

    val document = state.document
    val clock = remember(document) { document.clock }

    val isDark = isSystemInDarkTheme()
    val resolvedTheme = remember(theme, isDark) { resolveThemeMode(theme, isDark) }
    val androidContext = LocalContext.current

    val preprocessed = state.preprocessed

    val density = LocalDensity.current
    val remoteContext =
        remember(typefaceResolver, preprocessed, state) {
            val ctx = state.remoteContext
            val resolvedResolver = typefaceResolver ?: EmbeddedPlayerTypefaceResolver
            ctx.setTypefaceResolver(resolvedResolver)
            ctx.useChoreographer = true
            ctx.loadFloat(RemoteContext.ID_FONT_SIZE, 14f * density.fontScale * density.density)
            ctx.loadFloat(RemoteContext.ID_DENSITY, density.density)
            ctx.density = density.density
            ctx.paintTheme = resolvedTheme
            ctx.setTheme(resolvedTheme)
            AndroidColorThemeResolver.mapColors(androidContext, document)
            document.themedColors?.fastForEach { themeColor ->
                themeColor.setTheme(ctx, resolvedTheme)
                themeColor.apply(ctx)
            }
            document.checkShaders(
                ctx,
                CoreDocument.ShaderControl { source -> isShaderValid(source) },
            )
            ctx
        }

    LaunchedEffect(density) {
        remoteContext.density = density.density
        remoteContext.loadFloat(
            RemoteContext.ID_FONT_SIZE,
            14f * density.fontScale * density.density,
        )
        remoteContext.loadFloat(RemoteContext.ID_DENSITY, density.density)
    }

    // Time is driven on demand:
    // - Documents with continuous time variables (ID_CONTINUOUS_SEC, ID_ANIMATION_TIME), particles,
    //   or WakeIn run the per-frame loop via withInfiniteAnimationFrameMillis.
    // - Documents with only discrete wall-clock/calendar variables (ID_TIME_IN_SEC, ID_TIME_IN_MIN,
    //   ID_CALENDAR_MONTH, etc.) sleep between whole second boundaries via delay().
    // - Static documents and documents whose animations are driven by Compose's own animation
    // clocks
    //   (FloatAnimation via Animatable, StateLayout via AnimatedContent) initialize t=0 once and
    //   settle immediately to idle without a background loop.
    val currentTimeMillisState = state.currentTimeMillisState
    val needsContinuousLoop =
        preprocessed.hasContinuousTime || preprocessed.hasParticles || preprocessed.hasWakeIn
    val needsDiscreteLoop = !needsContinuousLoop && preprocessed.hasDiscreteTime

    // Pure-Compose evaluation of *derived/computed* operations (color & text expressions,
    // attributes,
    // lookups). Each computed id resolves to a derivedStateOf that runs the op's existing
    // updateVariables+apply against this GraphContext, which routes the op's reads to the reactive
    // store / other computed States and captures its write as the result. No imperative recompute
    // pass, no dirty flags — changing an input invalidates exactly the dependent States, and chains
    // compose naturally.
    val textMeasurer = rememberTextMeasurer()
    val graphContext =
        state.graphContext.also { gc ->
            gc.setTypefaceResolver(remoteContext.typefaceResolver)
            gc.textMeasurer = textMeasurer
        }

    val startClockMillis = remember(document, clock) { clock.millis() }
    val limiter = remember(document) { Limiter() }
    LaunchedEffect(
        document,
        graphContext,
        needsContinuousLoop,
        needsDiscreteLoop,
    ) {
        val startMillis = withInfiniteAnimationFrameMillis { it }
        while (true) {
            val frameMillis = withInfiniteAnimationFrameMillis { it } - startMillis
            limiter.recordDrawStart(frameMillis * 1_000_000L)
            state.updateTime(
                frameMillis = frameMillis.toFloat(),
                updateContinuous = needsContinuousLoop,
            )
            remoteContext.currentTime = startClockMillis + frameMillis

            if (!needsContinuousLoop && !needsDiscreteLoop) break

            if (needsDiscreteLoop) {
                val currentMillis = startClockMillis + frameMillis
                val millisToNextSecond = 1000L - Math.floorMod(currentMillis, 1000L)
                delay(millisToNextSecond)
            } else {
                val delayNs = limiter.computeDelay(0L, frameMillis * 1_000_000L)
                if (delayNs > limiter.minIntervalNs) {
                    delay((delayNs - limiter.minIntervalNs) / 1_000_000L)
                }
            }
        }
    }

    // React to dynamic theme changes (e.g. host night mode changes), updating context paint/theme
    // state and applying the updated theme to document ColorTheme operations.
    LaunchedEffect(resolvedTheme, androidContext) {
        remoteContext.paintTheme = resolvedTheme
        remoteContext.setTheme(resolvedTheme)
        AndroidColorThemeResolver.mapColors(androidContext, document)
        document.themedColors?.fastForEach { themeColor ->
            themeColor.setTheme(remoteContext, resolvedTheme)
            themeColor.apply(remoteContext)
        }
    }
    // The document's root content description (Header DOC_CONTENT_DESCRIPTION /
    // RootContentDescription
    // op, resolved onto the document during initializeContext) labels the whole player for
    // accessibility — the embedded equivalent of the View player's root-view contentDescription.
    val rootContentDescription = remember(document) { document.contentDescription }

    // Desired frame rate (Header DOC_DESIRED_FPS): expressed as a platform hint via Compose's
    // preferredFrameRate modifier (which sets the layer's frame rate) rather than throttling the
    // time
    // ticker ourselves — the system then drives frames at this rate and the withFrameMillis loop
    // above advances time at the same cadence. 0 = no preference (absent / non-positive value).
    val desiredFps =
        remember(document) {
            (document.getProperty(Header.DOC_DESIRED_FPS) as? Int)
                ?.takeIf { it > 0 }
                ?.coerceAtMost(Limits.MAX_FPS)
                ?.toFloat() ?: 0f
        }

    val rootCoordsHolder = remember(document) { RootLayoutCoordinatesHolder() }
    Box(
        modifier =
            modifier
                .then(
                    // preferredFrameRate adds a graphicsLayer, so only apply it when a rate is set.
                    if (desiredFps > 0f) Modifier.preferredFrameRate(desiredFps) else Modifier
                )
                .then(
                    if (rootContentDescription != null)
                        Modifier.semantics { contentDescription = rootContentDescription }
                    else Modifier
                )
                .rcPlayerRootInspector(state)
                .onPlaced {
                    rootCoordsHolder.coordinates = it
                    val position = it.positionOnScreen()
                    document.setOrigin(position.x, position.y)
                    // Publish the player's placed size to core after layout, matching how child
                    // components publish theirs (see RcPlayerComponent), rather than writing core
                    // and snapshot state from composition or draw. Placement precedes draw, so
                    // draws see the new size in the same frame; composition-phase readers of root
                    // ComponentValues recompose on the next frame, as they do for child components.
                    // Only write when the size changed, not on every re-placement (e.g. inside a
                    // scrolling host), matching RcPlayerComponent.
                    val size = it.size
                    if (size != rootCoordsHolder.publishedSize) {
                        rootCoordsHolder.publishedSize = size
                        val root = document.rootLayoutComponent
                        if (root != null) {
                            root.setWidth(size.width.toFloat())
                            root.setHeight(size.height.toFloat())
                            // Re-evaluate the root's ComponentValues (its WIDTH / HEIGHT).
                            root.updateVariables(remoteContext)
                        } else {
                            // Raw draw-list document: mirror CoreDocument.paint, so draws
                            // positioned by the document size resolve.
                            document.setWidth(size.width)
                            document.setHeight(size.height)
                        }
                    }
                }
                .pointerInput(document, remoteContext, preprocessed, graphContext) {
                    // Root-level touch forwarding: update ID_TOUCH_POS_X/Y and drive the
                    // TouchExpressions outside any LayoutComponent (preprocessed
                    // rootTouchExpressions). Component touch handlers and component-scoped
                    // TouchExpressions are dispatched by their own Compose modifiers, so the
                    // CoreDocument touch methods (which also re-traverse the component tree) are
                    // deliberately not used here.
                    val rootTouchExpressions = preprocessed.rootTouchExpressions
                    // Publish the touch position before resolving the expressions' variables, so
                    // their precalculated inputs see this event's position.
                    fun loadRootTouchPosition(pos: Offset) {
                        remoteContext.loadFloat(RemoteContext.ID_TOUCH_POS_X, pos.x)
                        remoteContext.loadFloat(RemoteContext.ID_TOUCH_POS_Y, pos.y)
                        rootTouchExpressions.fastForEach { te -> te.updateVariables(graphContext) }
                    }
                    fun releaseRootTouch(pos: Offset, velocityX: Float, velocityY: Float) {
                        loadRootTouchPosition(pos)
                        rootTouchExpressions.fastForEach { te ->
                            te.touchUp(remoteContext, pos.x, pos.y, velocityX, velocityY)
                        }
                        rootTouchExpressions.fastForEach { te -> te.apply(remoteContext) }
                    }
                    val velocityTracker = VelocityTracker()
                    awaitPointerEventScope {
                        while (true) {
                            val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                            val down =
                                downEvent.changes.fastFirstOrNull {
                                    !it.previousPressed && it.pressed
                                } ?: continue
                            val pointerId = down.id
                            var released = false
                            var lastPos = down.position
                            velocityTracker.resetTracking()
                            velocityTracker.addPointerInputChange(down)
                            loadRootTouchPosition(down.position)
                            rootTouchExpressions.fastForEach { te ->
                                // Core binds these to the RootLayoutComponent; apply refreshes
                                // their hit bounds from its size (TouchExpression.updateBounds)
                                // before the touchDown hit test, as the component handler does.
                                te.apply(remoteContext)
                                te.touchDown(remoteContext, down.position.x, down.position.y)
                            }
                            rootTouchExpressions.fastForEach { te -> te.apply(remoteContext) }
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Final)
                                    val change =
                                        event.changes.fastFirstOrNull { it.id == pointerId }
                                            ?: break
                                    if (change.isConsumed) {
                                        // A child took the gesture: stop forwarding and release
                                        // at the last forwarded position, without a fling.
                                        released = true
                                        releaseRootTouch(lastPos, 0f, 0f)
                                        break
                                    }
                                    val pos = change.position
                                    velocityTracker.addPointerInputChange(change)
                                    if (!change.pressed) {
                                        val velocity = velocityTracker.calculateVelocity()
                                        released = true
                                        releaseRootTouch(pos, velocity.x, velocity.y)
                                        break
                                    }
                                    if (event.type == PointerEventType.Move) {
                                        lastPos = pos
                                        loadRootTouchPosition(pos)
                                        rootTouchExpressions.fastForEach { te ->
                                            te.touchDrag(remoteContext, pos.x, pos.y)
                                        }
                                    }
                                }
                            } finally {
                                // The pointer disappeared or the handler was cancelled or
                                // restarted mid-gesture. TouchExpression has no cancel, so
                                // release at the last position with no velocity and let it settle
                                // according to its stop mode (as the component handler does).
                                if (!released) {
                                    releaseRootTouch(lastPos, 0f, 0f)
                                }
                            }
                        }
                    }
                }
    ) {
        // ColorConstant / IntegerConstant / FloatExpression defaults already live in the
        // snapshot-backed store (applied by applyOperations during setup); the single store is the
        // source of truth for variables now, so there is no separate draw-path map to populate.

        // Identify ComponentValue operations
        val componentValueMap = preprocessed.componentValueMap

        val componentValueStateMap = remember { mutableMapOf<Int, MutableState<Float>>() }
        remember(componentValueMap) {
            componentValueMap.values.forEach { list ->
                list.fastForEach { op ->
                    if (!componentValueStateMap.containsKey(op.valueId)) {
                        componentValueStateMap[op.valueId] = mutableFloatStateOf(0f)
                    }
                }
            }
        }
        graphContext.componentValues = componentValueStateMap

        val stateUpdater = remember(remoteContext) { StateUpdaterImpl(remoteContext) }
        // The image loader: the caller-supplied one, or the default that wraps embedded bitmaps.
        val resolvedImageLoader =
            remember(remoteContext, imageLoader) {
                imageLoader ?: EmbeddedRcImageLoader(remoteContext)
            }
        // Make it reachable from the (non-composable) canvas draw path too — the document image
        // draws
        // resolve through it via the GraphContext.
        graphContext.imageLoader = resolvedImageLoader
        CompositionLocalProvider(
            LocalCoreDocument provides document,
            LocalRemoteContext provides remoteContext,
            LocalComponentValueMap provides componentValueMap,
            LocalComponentValueStateMap provides componentValueStateMap,
            LocalComponentTouchExpressionsMap provides preprocessed.componentTouchExpressionsMap,
            LocalHasTouchExpressions provides preprocessed.touchExpressions.isNotEmpty(),
            LocalRootLayoutCoordinates provides rootCoordsHolder,
            LocalCurrentTimeMillis provides currentTimeMillisState,
            LocalGraphContext provides graphContext,
            LocalRcImageLoader provides resolvedImageLoader,
            LocalRemoteActionHandler provides onAction,
            LocalRemoteNamedActionHandler provides
                { name, value ->
                    val lambdaId = LambdaAction.parseId(name)
                    if (lambdaId != null) {
                        lambdas[lambdaId]?.invoke()
                    } else {
                        val pendingIntentId = PendingIntentAction.parseId(name)
                        if (pendingIntentId != null) {
                            pendingIntents[pendingIntentId]?.send()
                        }
                    }
                    onNamedAction(name, value, stateUpdater)
                },
            LocalRcCustomPlugins provides customPlugins,
            LocalTypefaceResolver provides (typefaceResolver ?: remoteContext.typefaceResolver),
        ) {
            val animatedExpressionIds =
                remember(document) {
                    val ids = MutableIntList()
                    document.getFloatExpressionsReflection().forEach { (id, expr) ->
                        if (expr.hasAnimation) ids.add(id)
                    }
                    ids.sort()
                    ids
                }
            // Keyed to the document like animatedExpressionIds so the map isn't reallocated on
            // every recomposition; the remembered animated States are stable across recompositions.
            val animatedStates =
                remember(document) { HashMap<Int, State<Float>>(animatedExpressionIds.size) }
            for (i in animatedExpressionIds.indices) {
                val animId = animatedExpressionIds[i]
                animatedStates[animId] = rememberAnimatedRemoteFloat(animId)
            }
            graphContext.animatedFloatStates = animatedStates

            val root = document.rootLayoutComponent
            if (root != null) {
                RcPlayerRootLayoutComponent(root)
            } else {
                // Raw draw-list document (no layout component tree): render its operations
                // directly.
                RcPlayerRawDocument()
            }
        }
    }
}

/**
 * A player of a [CoreDocument].
 *
 * **One player per document.** First composition installs this player's runtime state *onto the
 * document* (swaps in a [SnapshotRemoteComposeState], re-gathers collections, applies operations),
 * so a given [CoreDocument] instance is bound to a single `RcPlayer`. Don't drive two players from
 * the same `CoreDocument` concurrently — give each its own document (re-`initFromBuffer`).
 * Re-installing onto an already-initialized document is guarded against below so an accidental
 * reuse doesn't clobber existing state, but the two players would then share one state, which is
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApiAndroidX")
@Suppress("PrimitiveInCollection")
@Composable
public fun RcPlayer(
    document: CoreDocument,
    modifier: Modifier = Modifier,
    imageLoader: RcImageLoader? = null,
    isShaderValid: (shaderSource: String) -> Boolean = { true },
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    customPlugins: CustomPluginRegistry? = null,
    lambdas: IntObjectMap<() -> Unit> = emptyIntObjectMap(),
    pendingIntents: IntObjectMap<PendingIntent> = emptyIntObjectMap(),
    typefaceResolver: TypefaceResolver? = LocalTypefaceResolver.current,
    theme: Int = Theme.SYSTEM,
) {
    val state = rememberRcPlayerState(document)
    RcPlayer(
        state = state,
        modifier = modifier,
        imageLoader = imageLoader,
        isShaderValid = isShaderValid,
        onAction = onAction,
        onNamedAction = onNamedAction,
        customPlugins = customPlugins,
        lambdas = lambdas,
        pendingIntents = pendingIntents,
        typefaceResolver = typefaceResolver,
        theme = theme,
    )
}

/**
 * A player of a [CapturedDocument].
 *
 * This overload extracts the [CoreDocument] and any associated lambdas from the [CapturedDocument]
 * and forwards them to the underlying [RcPlayer].
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApiAndroidX")
@Suppress("PrimitiveInCollection")
@Composable
public fun RcPlayer(
    capturedDocument: CapturedDocument,
    modifier: Modifier = Modifier,
    imageLoader: RcImageLoader? = null,
    isShaderValid: (shaderSource: String) -> Boolean = { true },
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    customPlugins: CustomPluginRegistry? = null,
    theme: Int = Theme.SYSTEM,
) {
    val state = rememberRcPlayerState(capturedDocument)
    RcPlayer(
        state = state,
        modifier = modifier,
        imageLoader = imageLoader,
        isShaderValid = isShaderValid,
        onAction = onAction,
        onNamedAction = onNamedAction,
        customPlugins = customPlugins,
        lambdas = capturedDocument.lambdas,
        pendingIntents = capturedDocument.pendingIntents,
        theme = theme,
    )
}

/**
 * Renders a document that has no root layout component — a raw draw-list document whose draw
 * operations live at the top level of `mOperations` rather than inside a component tree (e.g.
 * shader / canvas demos built without the layout DSL). The View player renders these via
 * `CoreDocument.paint`; here the document's operations are executed directly in a Canvas. Without
 * this fallback the layout-tree renderer dereferenced a null `rootLayoutComponent` and crashed
 * (NPE) for such documents.
 */
@Composable
internal fun RcPlayerRawDocument() {
    val document = LocalCoreDocument.current
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxSize()) {
        // The document size is published from the player's onPlaced, which runs before draw.
        executeOperations(
            document.getOperationsReflection(),
            remoteContext,
            graph = graph,
            textMeasurer = textMeasurer,
        )
    }
}

@Composable
internal fun RcPlayerRootLayoutComponent(root: RootLayoutComponent) {
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current

    val drawOps = remember(root) { root.list.fastFilter { it !is Component } }
    if (drawOps.isNotEmpty()) {
        val textMeasurer = rememberTextMeasurer()
        Canvas(modifier = Modifier.fillMaxSize()) {
            executeOperations(
                drawOps,
                remoteContext,
                graph = graph,
                textMeasurer = textMeasurer,
            )
        }
    }

    RcPlayerChildren(root)
}

private class ComponentPlacementHolder {
    var rootOffset: Offset = Offset.Zero
    var posInParent: Offset = Offset.Zero
    var width: Float = Float.NaN
    var height: Float = Float.NaN
    var boundsInitialized: Boolean = false
    /** The component's outermost coordinates (its full, padding-inclusive box). */
    var outerCoords: LayoutCoordinates? = null
    /** The coordinates of the TouchExpression pointerInput, inside the component's modifiers. */
    var touchCoords: LayoutCoordinates? = null

    /**
     * The offset of the TouchExpression pointerInput within the component's outer box, e.g. its
     * padding. Core reports touch positions relative to the outer box (and hit-tests against the
     * full component size), so pointer positions are shifted by this before reaching core.
     */
    fun touchOrigin(): Offset {
        val outer = outerCoords ?: return Offset.Zero
        val touch = touchCoords ?: return Offset.Zero
        if (!outer.isAttached || !touch.isAttached) return Offset.Zero
        return outer.localPositionOf(touch, Offset.Zero)
    }
}

private fun touchExpressionAxes(touchExpressions: List<TouchExpression>): Pair<Boolean, Boolean> {
    var usesX = false
    var usesY = false
    touchExpressions.fastForEach { te ->
        val exp = te.mSrcExp ?: return@fastForEach
        for (v in exp) {
            if (v.isNaN()) {
                when (Utils.idFromNan(v)) {
                    RemoteContext.ID_TOUCH_POS_X -> usesX = true
                    RemoteContext.ID_TOUCH_POS_Y -> usesY = true
                }
            }
        }
    }
    if (!usesX && !usesY) {
        return true to true
    }
    return usesX to usesY
}

@Composable
internal fun RcPlayerComponent(component: Component, modifier: Modifier = Modifier) {
    if (component is LayoutComponent) {
        val document = LocalCoreDocument.current
        val componentValueMap = LocalComponentValueMap.current
        val componentValueStateMap = LocalComponentValueStateMap.current
        val componentTouchExpressionsMap = LocalComponentTouchExpressionsMap.current
        val hasTouchExpressions = LocalHasTouchExpressions.current
        val rootCoordsHolder = LocalRootLayoutCoordinates.current
        val remoteContext = LocalRemoteContext.current
        val graphContext = LocalGraphContext.current

        val visibilityOp =
            component.componentModifiers.list.fastFirstOrNull { it is ComponentVisibilityOperation }
                as? ComponentVisibilityOperation
        // FitBoxLayout and StateLayout unconditionally override the visibility of their direct
        // candidate/page children (matching FitBoxLayout.addVisibilityOverride and
        // StateLayout.measure in remote-core).
        val visibilityDelegatedToParent =
            component.parent is FitBoxLayout || component.parent is StateLayout
        if (visibilityOp != null && !visibilityDelegatedToParent) {
            val visible = rememberComponentVisibility(visibilityOp)
            if (visible == Component.Visibility.GONE) {
                return
            }
        }

        // Custom components whose plugin handles clicks itself (e.g. a native button) receive the
        // click actions via RcCustomComponent instead of a wrapping clickable.
        val customClickHandlers =
            if (component is Custom) rememberCustomClickHandlers(component) else null

        val touchExpressions = componentTouchExpressionsMap[component.componentId].orEmpty()
        val placement = remember(component) { ComponentPlacementHolder() }
        var modifier =
            Modifier.sharedElementTransition(component)
                .then(
                    // Allow StateLayout to adopt its active child's measured dimensions rather than
                    // expanding to incoming minimum parent constraints.
                    if (component is StateLayout) {
                        Modifier.wrapContentSize(Alignment.TopStart)
                    } else {
                        Modifier
                    }
                )
                .rcComponentOuterInspector(component)
                .then(
                    component.componentModifiers.toModifier(
                        component.getDrawContentOperationsListReflection(),
                        ignoreVisibility = visibilityDelegatedToParent,
                        ignoreClicks = customClickHandlers != null,
                    )
                )
                .rcComponentContentInspector(component)
                .then(
                    if (touchExpressions.isNotEmpty()) {
                        val (usesX, usesY) =
                            remember(touchExpressions) { touchExpressionAxes(touchExpressions) }
                        Modifier.onPlaced { placement.touchCoords = it }
                            .pointerInput(
                                component,
                                touchExpressions,
                                remoteContext,
                                graphContext,
                            ) {
                                val velocityTracker = VelocityTracker()
                                val touchSlop = viewConfiguration.touchSlop
                                awaitPointerEventScope {
                                    while (true) {
                                        val downEvent = awaitPointerEvent(PointerEventPass.Main)
                                        val down =
                                            downEvent.changes.fastFirstOrNull {
                                                !it.previousPressed && it.pressed
                                            } ?: continue
                                        val pointerId = down.id
                                        val downPos = down.position
                                        var dragLocked = false
                                        var dragRejected = false
                                        val isLegacyTouch =
                                            remoteContext.touchVersion !=
                                                LayoutManager.FIX_TOUCH_EVENT
                                        // Pointer positions are relative to this pointerInput,
                                        // which
                                        // sits inside the component's modifiers (e.g. padding).
                                        // Shift
                                        // them into the component's outer box, the space core uses
                                        // for touch positions and the hit bounds (the component's
                                        // full size, published by onPlaced).
                                        val touchOrigin = placement.touchOrigin()
                                        velocityTracker.resetTracking()
                                        velocityTracker.addPointerInputChange(down)
                                        val downLocal = downPos + touchOrigin
                                        val downRootX = placement.rootOffset.x + downLocal.x
                                        val downRootY = placement.rootOffset.y + downLocal.y
                                        val downTouchX =
                                            if (isLegacyTouch) downRootX else downLocal.x
                                        val downTouchY =
                                            if (isLegacyTouch) downRootY else downLocal.y
                                        remoteContext.loadFloat(
                                            RemoteContext.ID_TOUCH_POS_X,
                                            downRootX,
                                        )
                                        remoteContext.loadFloat(
                                            RemoteContext.ID_TOUCH_POS_Y,
                                            downRootY,
                                        )
                                        touchExpressions.fastForEach { te ->
                                            te.updateVariables(graphContext ?: remoteContext)
                                            // Bounds are only read by the touchDown hit test, so
                                            // refresh them here (apply runs updateBounds) rather
                                            // than on every placement. Legacy bounds depend on the
                                            // ancestors' positions, which change while scrolling.
                                            te.apply(remoteContext)
                                            te.touchDown(remoteContext, downTouchX, downTouchY)
                                            te.apply(remoteContext)
                                        }
                                        var ended = false
                                        var lastTouchX = downTouchX
                                        var lastTouchY = downTouchY
                                        try {
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Main)
                                                val change =
                                                    event.changes.fastFirstOrNull {
                                                        it.id == pointerId
                                                    } ?: break
                                                val pos = change.position
                                                val local = pos + touchOrigin
                                                val rootX = placement.rootOffset.x + local.x
                                                val rootY = placement.rootOffset.y + local.y
                                                val touchX = if (isLegacyTouch) rootX else local.x
                                                val touchY = if (isLegacyTouch) rootY else local.y
                                                lastTouchX = touchX
                                                lastTouchY = touchY
                                                if (!change.pressed) {
                                                    ended = true
                                                    val velocity =
                                                        if (!dragRejected && !change.isConsumed) {
                                                            velocityTracker.addPointerInputChange(
                                                                change
                                                            )
                                                            velocityTracker.calculateVelocity()
                                                        } else {
                                                            null
                                                        }
                                                    if (!dragRejected && !change.isConsumed) {
                                                        remoteContext.loadFloat(
                                                            RemoteContext.ID_TOUCH_POS_X,
                                                            rootX,
                                                        )
                                                        remoteContext.loadFloat(
                                                            RemoteContext.ID_TOUCH_POS_Y,
                                                            rootY,
                                                        )
                                                    }
                                                    touchExpressions.fastForEach { te ->
                                                        te.updateVariables(
                                                            graphContext ?: remoteContext
                                                        )
                                                        te.touchUp(
                                                            remoteContext,
                                                            touchX,
                                                            touchY,
                                                            velocity?.x ?: 0f,
                                                            velocity?.y ?: 0f,
                                                        )
                                                        te.apply(remoteContext)
                                                    }
                                                    break
                                                }
                                                if (event.type == PointerEventType.Move) {
                                                    if (!dragLocked && !dragRejected) {
                                                        val dx = abs(pos.x - downPos.x)
                                                        val dy = abs(pos.y - downPos.y)
                                                        if (usesX && !usesY) {
                                                            if (dy > touchSlop && dy > dx) {
                                                                dragRejected = true
                                                            } else if (dx > touchSlop) {
                                                                dragLocked = true
                                                            }
                                                        } else if (usesY && !usesX) {
                                                            if (dx > touchSlop && dx > dy) {
                                                                dragRejected = true
                                                            } else if (dy > touchSlop) {
                                                                dragLocked = true
                                                            }
                                                        } else if (
                                                            dx * dx + dy * dy >
                                                                touchSlop * touchSlop
                                                        ) {
                                                            dragLocked = true
                                                        }
                                                    }
                                                    if (!dragRejected) {
                                                        velocityTracker.addPointerInputChange(
                                                            change
                                                        )
                                                        remoteContext.loadFloat(
                                                            RemoteContext.ID_TOUCH_POS_X,
                                                            rootX,
                                                        )
                                                        remoteContext.loadFloat(
                                                            RemoteContext.ID_TOUCH_POS_Y,
                                                            rootY,
                                                        )
                                                        touchExpressions.fastForEach { te ->
                                                            te.updateVariables(
                                                                graphContext ?: remoteContext
                                                            )
                                                            te.touchDrag(
                                                                remoteContext,
                                                                touchX,
                                                                touchY,
                                                            )
                                                        }
                                                        if (dragLocked) {
                                                            change.consume()
                                                        }
                                                    }
                                                }
                                            }
                                        } finally {
                                            // The pointer disappeared or the handler was cancelled
                                            // or
                                            // restarted mid-gesture. TouchExpression has no cancel,
                                            // so
                                            // release at the last position with no velocity and let
                                            // it settle according to its stop mode.
                                            if (!ended) {
                                                touchExpressions.fastForEach { te ->
                                                    te.updateVariables(
                                                        graphContext ?: remoteContext
                                                    )
                                                    te.touchUp(
                                                        remoteContext,
                                                        lastTouchX,
                                                        lastTouchY,
                                                        0f,
                                                        0f,
                                                    )
                                                    te.apply(remoteContext)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    } else {
                        Modifier
                    }
                )
                .then(modifier)

        // Publish the component's measured dimensions and positions (read by ComponentValue
        // expressions and TouchExpression) from an onPlaced callback rather than a custom
        // Modifier.layout that wrote snapshot state during the measure pass (a relayout hazard) and
        // sat ahead of the real modifiers (which disturbed constraint propagation, e.g. FILL
        // children collapsing to wrap size). As the outermost modifier, onPlaced reports the full
        // component size and placement and fires after layout.
        val sizeFeedbackOps = componentValueMap[component.getId()]
        if (
            !sizeFeedbackOps.isNullOrEmpty() || touchExpressions.isNotEmpty() || hasTouchExpressions
        ) {
            modifier =
                Modifier.onPlaced { coords ->
                        placement.outerCoords = coords
                        val posInParent = coords.positionInParent()
                        val w = coords.size.width.toFloat()
                        val h = coords.size.height.toFloat()
                        val rootCoords = rootCoordsHolder.coordinates
                        val rootOffset =
                            if (rootCoords != null && rootCoords.isAttached) {
                                rootCoords.localPositionOf(coords, Offset.Zero)
                            } else {
                                val posOnScreen = coords.positionOnScreen()
                                Offset(
                                    posOnScreen.x - document.originX,
                                    posOnScreen.y - document.originY,
                                )
                            }
                        val sizeChanged =
                            !placement.boundsInitialized ||
                                placement.width != w ||
                                placement.height != h
                        val parentPosChanged =
                            !placement.boundsInitialized || placement.posInParent != posInParent
                        val rootPosChanged =
                            !placement.boundsInitialized || placement.rootOffset != rootOffset
                        placement.boundsInitialized = true
                        placement.width = w
                        placement.height = h
                        placement.posInParent = posInParent
                        placement.rootOffset = rootOffset

                        if (parentPosChanged) {
                            component.setLayoutPosition(posInParent.x, posInParent.y)
                        }
                        if (sizeChanged) {
                            component.setWidth(w)
                            component.setHeight(h)
                        }
                        if (
                            !sizeFeedbackOps.isNullOrEmpty() &&
                                (sizeChanged || parentPosChanged || rootPosChanged)
                        ) {
                            val contentW =
                                (w - component.paddingLeft - component.paddingRight).coerceAtLeast(
                                    0f
                                )
                            val contentH =
                                (h - component.paddingTop - component.paddingBottom).coerceAtLeast(
                                    0f
                                )
                            sizeFeedbackOps.fastForEach { op ->
                                val newVal =
                                    when (op.type) {
                                        ComponentValue.WIDTH -> w
                                        ComponentValue.CONTENT_WIDTH -> contentW
                                        ComponentValue.HEIGHT -> h
                                        ComponentValue.CONTENT_HEIGHT -> contentH
                                        ComponentValue.POS_X -> posInParent.x
                                        ComponentValue.POS_Y -> posInParent.y
                                        ComponentValue.POS_ROOT_X -> rootOffset.x
                                        ComponentValue.POS_ROOT_Y -> rootOffset.y
                                        else -> return@fastForEach
                                    }
                                remoteContext.loadFloat(op.valueId, newVal)
                                val state = componentValueStateMap[op.valueId] ?: return@fastForEach
                                if (abs(newVal - state.value) > 2.0f) {
                                    state.value = newVal
                                }
                            }
                        }
                        // Position changes don't re-apply touch expressions: their bounds are
                        // refreshed at touch down.
                        if (touchExpressions.isNotEmpty() && sizeChanged) {
                            touchExpressions.fastForEach { te ->
                                te.updateVariables(graphContext ?: remoteContext)
                                te.apply(remoteContext)
                            }
                        }
                    }
                    .then(modifier)
        }

        when (component) {
            is CanvasLayout -> RcPlayerCanvas(component, modifier)
            is ColumnLayout -> RcPlayerColumn(component, modifier)
            is FlowLayout -> RcPlayerFlowRow(component, modifier)
            is RowLayout -> RcPlayerRow(component, modifier)
            is CoreText -> RcPlayerText(component, modifier)
            is TextLayout -> RcPlayerText(component, modifier)
            is FitBoxLayout -> RcPlayerFitBoxLayout(component, modifier)
            is StateLayout -> RcPlayerStateLayout(component, modifier)
            is ImageLayout -> RcPlayerImageLayout(component, modifier)
            is Custom -> RcPlayerCustom(component, modifier, customClickHandlers)
            // Last as others are often BoxLayout subclasses
            is BoxLayout -> RcPlayerBox(component, modifier)
            else -> {
                // Unsupported layout type. Render nothing rather than crash; see
                // operation_coverage.md. The modifier (incl. any drawContent) was still applied
                // above.
                println(
                    "Warning: unsupported layout component ${component::class.java.simpleName}; rendering nothing"
                )
            }
        }
    } else {
        // Non-LayoutComponent component reached dispatch — skip gracefully instead of crashing.
        println(
            "Warning: unsupported component ${component?.let { it::class.java.simpleName }}; rendering nothing"
        )
    }
}

@Composable
internal fun RcPlayerChildren(
    layout: Component,
    modifierProvider: @Composable (Component) -> Modifier = { Modifier },
) {
    if (layout is LayoutComponent) {
        layout.childrenComponents.fastForEach { child ->
            val scopeModifier = modifierProvider(child)
            RcPlayerComponent(child, scopeModifier)
        }
    } else {
        val children =
            remember(layout) { ArrayList<Component>().apply { layout.getComponents(this) } }
        children.fastForEach { op -> RcPlayerComponent(op) }
    }
}

internal class DocumentPreprocessResult(
    val globalOps: ArrayList<Operation>,
    val constantOps: ArrayList<Operation>,
    val touchExpressions: ArrayList<TouchExpression>,
    val rootTouchExpressions: ArrayList<TouchExpression>,
    val componentTouchExpressionsMap: Map<Int, List<TouchExpression>>,
    val touchExpressionIds: IntSet,
    val computedOpIndex: IntObjectMap<Operation>,
    val componentValueMap: Map<Int, List<ComponentValue>>,
    val hasParticles: Boolean,
    val hasWakeIn: Boolean,
    val hasContinuousTime: Boolean,
    val hasDiscreteTime: Boolean,
)

internal fun preprocessDocument(document: CoreDocument): DocumentPreprocessResult {
    val rootComponent = document.rootLayoutComponent
    val ops = document.getOperationsReflection()
    val globalOps = ArrayList<Operation>()
    if (rootComponent != null) {
        for (i in 0 until ops.size) {
            val op = ops[i]
            if (op === rootComponent) break
            globalOps.add(op)
        }
    } else {
        globalOps.addAll(ops)
    }

    val constantOps = ArrayList<Operation>()
    val touchExpressions = ArrayList<TouchExpression>()
    val rootTouchExpressions = ArrayList<TouchExpression>()
    val componentTouchExpressionsMap = HashMap<Int, MutableList<TouchExpression>>()
    val computedOpIndex = mutableIntObjectMapOf<Operation>()
    val touchExpressionIds = mutableIntSetOf()
    val rawComponentValues = ArrayList<ComponentValue>()
    val componentsById = mutableIntObjectMapOf<Component>()
    var hasParticles = false
    var hasWakeIn = false
    var hasContinuousTime = false
    var hasDiscreteTime = false
    var currentLayoutComponent: LayoutComponent? = null
    var inScrollModifier = false

    fun visitOp(op: Operation) {
        val prevLayoutComponent = currentLayoutComponent
        val prevInScroll = inScrollModifier
        if (op is LayoutComponent) {
            currentLayoutComponent = op
        }
        if (op is ScrollModifierOperation) {
            inScrollModifier = true
        }
        try {
            val definedId =
                when (op) {
                    is NamedVariable -> op.mVarId
                    is VariableProvider -> op.id
                    is TextMeasure -> op.mId
                    is ImageAttribute -> op.mId
                    else -> -1
                }
            // Warn when a document operation defines an ID that collides with a reserved system
            // variable.
            if (definedId > 0 && isTimeVariable(definedId)) {
                Log.w(
                    "RcPlayer",
                    "Operation ${op.javaClass.simpleName} defines reserved system variable ID $definedId",
                )
            }

            // Collect direct references to continuous or discrete time variables from expressions
            // and variable-reading operations so we know which clock loop (if any) needs to run.
            if (op is StateLayout) {
                // StateLayout does not implement VariableSupport, so inspect its indexId directly.
                val id = op.indexIdReflection
                if (isContinuousTimeVariable(id)) {
                    hasContinuousTime = true
                } else if (isDiscreteTimeVariable(id)) {
                    hasDiscreteTime = true
                }
            } else {
                forEachListenedVariableId(op) { id ->
                    if (isContinuousTimeVariable(id)) {
                        hasContinuousTime = true
                    } else if (isDiscreteTimeVariable(id)) {
                        hasDiscreteTime = true
                    }
                }
            }

            if (op is TimeAttribute) {
                val type = op.mType.toInt() and 255
                if (
                    type == TimeAttribute.TIME_FROM_NOW_SEC.toInt() ||
                        type == TimeAttribute.TIME_FROM_NOW_MIN.toInt() ||
                        type == TimeAttribute.TIME_FROM_NOW_HR.toInt() ||
                        type == TimeAttribute.TIME_FROM_LOAD_SEC.toInt()
                ) {
                    hasContinuousTime = true
                } else {
                    hasDiscreteTime = true
                }
            }

            if (
                op is ColorConstant ||
                    op is FloatConstant ||
                    op is ColorTheme ||
                    op is NamedVariable ||
                    op is IntegerConstant ||
                    op is LongConstant ||
                    op is BooleanConstant ||
                    op is MatrixConstant
            ) {
                constantOps.add(op)
            }

            if (op is ParticlesLoop || op is ParticlesCompare) {
                hasParticles = true
            }

            if (op is WakeIn) {
                hasWakeIn = true
            }

            if (op is TouchExpression) {
                val id = op.id
                if (id > 0) {
                    touchExpressionIds.add(id)
                    computedOpIndex.remove(id)
                }
                if (!inScrollModifier) {
                    touchExpressions.add(op)
                    val enclosingComponent = currentLayoutComponent
                    if (enclosingComponent != null) {
                        // Bind the enclosing LayoutComponent: core only binds TouchExpressions
                        // that are direct ComponentData children of a component, not those
                        // nested in CanvasOperations / DrawContent. The component supplies the
                        // hit-test bounds (TouchExpression.updateBounds) and scopes dispatch to
                        // this component's pointerInput. TouchExpressions outside any
                        // LayoutComponent (bound by core to the RootLayoutComponent, if any) are
                        // dispatched by the root pointer handler via rootTouchExpressions; the
                        // player does not read CoreDocument.mTouchListeners.
                        op.setComponent(enclosingComponent)
                        val list =
                            componentTouchExpressionsMap.getOrPut(enclosingComponent.componentId) {
                                ArrayList()
                            }
                        if (!list.contains(op)) {
                            list.add(op)
                        }
                    } else {
                        if (!rootTouchExpressions.contains(op)) {
                            rootTouchExpressions.add(op)
                        }
                    }
                }
            } else if (
                op is VariableSupport &&
                    op is VariableProvider &&
                    op !is PathData &&
                    op !is PathCreate &&
                    op !is PathCombine &&
                    op !is PathTween &&
                    op !is PathExpression
            ) {
                val id = op.id
                if (
                    id > 0 && !touchExpressionIds.contains(id) && !computedOpIndex.containsKey(id)
                ) {
                    computedOpIndex[id] = op
                }
            } else if (op is TextMeasure) {
                val id = op.mId
                if (id > 0 && !computedOpIndex.containsKey(id)) {
                    computedOpIndex[id] = op
                }
            } else if (op is ImageAttribute) {
                val id = op.mId
                if (id > 0 && !computedOpIndex.containsKey(id)) {
                    computedOpIndex[id] = op
                }
            }

            if (op is ComponentValue) {
                rawComponentValues.add(op)
            }

            if (op is Component) {
                if (op.componentId !in componentsById) {
                    componentsById[op.componentId] = op
                }
            }

            if (op is LayoutComponent) {
                val content = op.getContentReflection()
                if (content != null && content.componentId !in componentsById) {
                    componentsById[content.componentId] = content
                }
                op.componentModifiers?.list?.fastForEach { visitOp(it) }
                val canvasOps = op.getCanvasOperations()
                if (canvasOps != null) {
                    canvasOps.list.fastForEach { childOp ->
                        if (childOp is ComponentValue && childOp.componentId == -1) {
                            childOp.componentId = op.componentId
                        }
                    }
                    visitOp(canvasOps)
                }
            }

            if (op is Container) {
                val list = op.getList()
                for (i in 0 until list.size) {
                    val child = list[i]
                    if (op is LayoutComponent && child is ComponentModifiers) continue
                    visitOp(child)
                }
            }
        } finally {
            currentLayoutComponent = prevLayoutComponent
            inScrollModifier = prevInScroll
        }
    }

    for (i in 0 until ops.size) {
        visitOp(ops[i])
    }

    val componentValueMap = HashMap<Int, MutableList<ComponentValue>>()
    for (i in 0 until rawComponentValues.size) {
        val op = rawComponentValues[i]
        var targetId = op.componentId
        val targetComponent = componentsById[targetId]
        if (targetComponent is LayoutComponentContent || targetComponent is CanvasContent) {
            val parent = targetComponent.parent
            parent?.let { targetId = it.id }
        }
        componentValueMap.getOrPut(targetId) { ArrayList() }.add(op)
    }

    return DocumentPreprocessResult(
        globalOps = globalOps,
        constantOps = constantOps,
        touchExpressions = touchExpressions,
        rootTouchExpressions = rootTouchExpressions,
        componentTouchExpressionsMap = componentTouchExpressionsMap,
        touchExpressionIds = touchExpressionIds,
        computedOpIndex = computedOpIndex,
        componentValueMap = componentValueMap,
        hasParticles = hasParticles,
        hasWakeIn = hasWakeIn,
        hasContinuousTime = hasContinuousTime,
        hasDiscreteTime = hasDiscreteTime,
    )
}

internal fun isExpressionContinuousTimeDependent(expr: FloatExpression): Boolean {
    val srcValues = expr.mSrcValue
    for (j in 0 until srcValues.size) {
        val v = srcValues[j]
        if (v.isNaN() && !AnimatedFloatExpression.isMathOperator(v) && !NanMap.isDataVariable(v)) {
            if (isContinuousTimeVariable(Utils.idFromNan(v))) return true
        }
    }
    return false
}

internal fun isExpressionDiscreteTimeDependent(expr: FloatExpression): Boolean {
    val srcValues = expr.mSrcValue
    for (j in 0 until srcValues.size) {
        val v = srcValues[j]
        if (v.isNaN() && !AnimatedFloatExpression.isMathOperator(v) && !NanMap.isDataVariable(v)) {
            if (isDiscreteTimeVariable(Utils.idFromNan(v))) return true
        }
    }
    return false
}

internal fun isExpressionTimeDependent(expr: FloatExpression): Boolean =
    isExpressionContinuousTimeDependent(expr) || isExpressionDiscreteTimeDependent(expr)

/**
 * Invokes [action] for each variable ID that [op] would pass to [RemoteContext.listensTo] from its
 * `registerListening` implementation.
 *
 * Only [FloatExpression], [IntegerExpression], [TextFromFloat], [TextLookupInt] and
 * [ComponentVisibilityOperation] are decoded; other operations are ignored. This mirrors core's
 * `registerListening` without allocating a throwaway [RemoteContext] (and its [CoreDocument] and
 * state stores) during document preprocessing. Keep in sync with core; parity is verified by
 * `RcPlayerExpressionTest`.
 */
internal inline fun forEachListenedVariableId(op: Operation, action: (Int) -> Unit) {
    when (op) {
        is FloatExpression -> {
            val srcValues = op.mSrcValue
            for (i in srcValues.indices) {
                val v = srcValues[i]
                if (
                    v.isNaN() &&
                        !AnimatedFloatExpression.isMathOperator(v) &&
                        !NanMap.isDataVariable(v)
                ) {
                    action(Utils.idFromNan(v))
                }
            }
        }
        is IntegerExpression -> {
            val mask = op.maskReflection
            val srcValues = op.mSrcValue
            for (i in srcValues.indices) {
                if (IntegerExpression.isId(mask, i, srcValues[i])) {
                    action(srcValues[i])
                }
            }
        }
        is TextFromFloat -> {
            if (op.mValue.isNaN()) {
                action(Utils.idFromNan(op.mValue))
            }
        }
        is TextLookupInt -> {
            action(op.mIndex)
            action(op.mDataSetId)
        }
        is ComponentVisibilityOperation -> action(op.getVisibilityIdReflection())
    }
}

internal fun mapEasing(type: Int): ComposeEasing {
    return when (type) {
        RemoteEasing.CUBIC_LINEAR -> LinearEasing
        RemoteEasing.CUBIC_STANDARD -> FastOutSlowInEasing
        RemoteEasing.CUBIC_ACCELERATE -> FastOutLinearInEasing
        RemoteEasing.CUBIC_DECELERATE -> LinearOutSlowInEasing
        else -> LinearEasing
    }
}

internal fun initializePlayerRemoteContext(
    document: CoreDocument,
    clock: RemoteClock,
    preprocessed: DocumentPreprocessResult,
): AndroidRemoteContext {
    val ctx = AndroidRemoteContext(clock)
    ctx.useChoreographer = true
    val touchVersion = document.featureIntValue(Header.FEATURE_TOUCH_VERSION)
    if (touchVersion != -1) {
        ctx.touchVersion = touchVersion
    }
    if (document.remoteComposeState !is SnapshotRemoteComposeState) {
        document.setRemoteComposeState(SnapshotRemoteComposeState())
        document.recollectCollectionsReflection()
    }
    ctx.withOpCountReset {
        document.initializeContext(ctx, null)
        document.applyDataOperationsWithoutBitmaps(ctx)
        document.setLayoutCallback {}
        document.applyOperationsReflection(ctx, preprocessed.globalOps)
        document.applyOperationsReflection(ctx, preprocessed.constantOps)
        val dataOps = ArrayList<Operation>()
        document.rootLayoutComponent?.getData(dataOps, true)
        document.applyOperationsReflection(ctx, dataOps)
        preprocessed.touchExpressions.fastForEach { te ->
            te.updateVariables(ctx)
            te.apply(ctx)
        }
    }
    return ctx
}
