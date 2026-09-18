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
import androidx.collection.emptyIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.animation.core.Easing as ComposeEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.WakeIn
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
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.NanMap
import androidx.compose.remote.core.operations.utilities.easing.Easing as RemoteEasing
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
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.remote.player.core.state.StateUpdaterImpl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
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
    val graphContext =
        state.graphContext.also { gc -> gc.setTypefaceResolver(remoteContext.typefaceResolver) }

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

    var size by remember { mutableStateOf(IntSize.Zero) }
    BoxWithConstraints(
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
                .onPlaced {
                    val position = it.positionOnScreen()
                    document.setOrigin(position.x, position.y)
                    size = it.size
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
            val rootSize = IntSize(constraints.maxWidth, constraints.maxHeight)
            if (document.rootLayoutComponent != null) {
                RcPlayerRootLayoutComponent(rootSize)
            } else {
                // Raw draw-list document (no layout component tree): render its operations
                // directly.
                RcPlayerRawDocument(rootSize)
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
internal fun RcPlayerRawDocument(size: IntSize) {
    val document = LocalCoreDocument.current
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Publish the on-screen size as the document dimensions before painting, mirroring
        // CoreDocument.paint, so draws positioned by the document size resolve.
        document.setWidth(size.width)
        document.setHeight(size.height)
        executeOperations(
            document.getOperationsReflection(),
            remoteContext,
            graph = graph,
            textMeasurer = textMeasurer,
        )
    }
}

@Composable
internal fun RcPlayerRootLayoutComponent(size: IntSize) {
    val document = LocalCoreDocument.current
    val root: RootLayoutComponent = document.rootLayoutComponent!!
    val remoteContext = LocalRemoteContext.current
    val graph = LocalGraphContext.current

    root.setWidth(size.width.toFloat())
    root.setHeight(size.height.toFloat())
    root.updateVariables(remoteContext)

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

@Composable
internal fun RcPlayerComponent(component: Component, modifier: Modifier = Modifier) {
    if (component is LayoutComponent) {
        val componentValueMap = LocalComponentValueMap.current
        val componentValueStateMap = LocalComponentValueStateMap.current

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
                .then(
                    component.componentModifiers.toModifier(
                        component.getDrawContentOperationsListReflection(),
                        ignoreVisibility = visibilityDelegatedToParent,
                    )
                )
                .then(modifier)

        // Publish the component's measured WIDTH/HEIGHT (read by ComponentValue expressions) from
        // an onSizeChanged callback rather than a custom Modifier.layout that wrote snapshot state
        // during the measure pass (a relayout hazard) and sat ahead of the real modifiers (which
        // disturbed constraint propagation, e.g. FILL children collapsing to wrap size). As the
        // outermost modifier, onSizeChanged reports the full component size and fires after layout.
        val sizeFeedbackOps = componentValueMap[component.getId()]
        if (!sizeFeedbackOps.isNullOrEmpty()) {
            modifier =
                Modifier.onSizeChanged { sz ->
                        sizeFeedbackOps.fastForEach { op ->
                            val state = componentValueStateMap[op.valueId] ?: return@fastForEach
                            val w = sz.width.toFloat()
                            val h = sz.height.toFloat()
                            if (op.type == ComponentValue.WIDTH && abs(w - state.value) > 2.0f) {
                                state.value = w
                            } else if (
                                op.type == ComponentValue.HEIGHT && abs(h - state.value) > 2.0f
                            ) {
                                state.value = h
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
            is Custom -> RcPlayerCustom(component, modifier)
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
    val computedOpIndex = mutableIntObjectMapOf<Operation>()
    val rawComponentValues = ArrayList<ComponentValue>()
    val componentsById = mutableIntObjectMapOf<Component>()
    var hasParticles = false
    var hasWakeIn = false
    var hasContinuousTime = false
    var hasDiscreteTime = false

    fun visitOp(op: Operation) {
        val definedId =
            when (op) {
                is NamedVariable -> op.mVarId
                is VariableProvider -> op.id
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

        if (op is TextFromFloat && Utils.isVariable(op.mValue)) {
            val id = Utils.idFromNan(op.mValue)
            if (isContinuousTimeVariable(id)) {
                hasContinuousTime = true
            } else if (isDiscreteTimeVariable(id)) {
                hasDiscreteTime = true
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
                op.javaClass.simpleName.endsWith("Constant")
        ) {
            constantOps.add(op)
        }

        if (op is ParticlesLoop || op is ParticlesCompare) {
            hasParticles = true
        }

        if (op is WakeIn) {
            hasWakeIn = true
        }

        if (op is VariableSupport && op is VariableProvider) {
            val animated = op is FloatExpression && op.mFloatAnimation != null
            val id = op.id
            if (!animated && id > 0 && !computedOpIndex.containsKey(id)) {
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
                visitOp(canvasOps)
            }
        }

        if (op is Container) {
            val list = op.getList()
            for (i in 0 until list.size) {
                visitOp(list[i])
            }
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
        if (targetComponent is LayoutComponentContent) {
            val parent = targetComponent.parent
            parent?.let { targetId = it.id }
        }
        componentValueMap.getOrPut(targetId) { ArrayList() }.add(op)
    }

    val floatExpressions = document.getFloatExpressionsReflection().values
    for (expr in floatExpressions) {
        if (isExpressionContinuousTimeDependent(expr)) {
            hasContinuousTime = true
        }
        if (isExpressionDiscreteTimeDependent(expr)) {
            hasDiscreteTime = true
        }
    }

    return DocumentPreprocessResult(
        globalOps = globalOps,
        constantOps = constantOps,
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
    if (document.remoteComposeState !is SnapshotRemoteComposeState) {
        document.setRemoteComposeState(SnapshotRemoteComposeState())
        document.recollectCollectionsReflection()
    }
    document.initializeContext(ctx, null)
    document.applyDataOperationsWithoutBitmaps(ctx)
    document.setLayoutCallback {}
    document.applyOperationsReflection(ctx, preprocessed.globalOps)
    document.applyOperationsReflection(ctx, preprocessed.constantOps)
    val dataOps = ArrayList<Operation>()
    document.rootLayoutComponent?.getData(dataOps, true)
    document.applyOperationsReflection(ctx, dataOps)
    return ctx
}
