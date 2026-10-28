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
    "RememberReturnType",
    "ModifierParameter",
    "AutoboxingStateCreation",
)
@file:OptIn(ExperimentalRemotePlayerApi::class)

package androidx.compose.remote.player.compose.embedded

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.util.Log
import android.view.ViewTreeObserver
import androidx.annotation.RestrictTo
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntList
import androidx.collection.emptyIntObjectMap
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limiter
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.RootContentBehavior
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.creation.compose.action.LambdaAction
import androidx.compose.remote.creation.compose.action.PendingIntentActionHandler
import androidx.compose.remote.creation.compose.capture.CapturedDocument
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.embedded.components.RcPlayerClickAreas
import androidx.compose.remote.player.compose.embedded.components.RcPlayerHapticEffect
import androidx.compose.remote.player.compose.embedded.components.RcPlayerRawDocument
import androidx.compose.remote.player.compose.embedded.components.RcPlayerRootLayoutComponent
import androidx.compose.remote.player.compose.embedded.components.publishComponentValues
import androidx.compose.remote.player.compose.embedded.components.rootTouchExpressions
import androidx.compose.remote.player.compose.embedded.doc.ClockScheduler
import androidx.compose.remote.player.compose.embedded.doc.SnapshotRemoteComposeState
import androidx.compose.remote.player.compose.embedded.inspectable.rcPlayerRootInspector
import androidx.compose.remote.player.compose.embedded.resources.AndroidColorThemeResolver
import androidx.compose.remote.player.compose.embedded.resources.EmbeddedRcImageLoader
import androidx.compose.remote.player.compose.embedded.resources.resolveThemeMode
import androidx.compose.remote.player.compose.embedded.state.hasAnimation
import androidx.compose.remote.player.compose.embedded.state.rememberAnimatedRemoteFloat
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.remote.player.core.state.StateUpdaterImpl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.util.fastForEach
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
 *
 * This overload runs every library feature that the document's profile can use (see
 * [RcPlayerFeatures]), which keeps all of them in the app. To keep only the features an app needs,
 * pass an [RcPlayerFeatureSet] instead.
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
    trustedContent: Boolean = false,
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    customPlugins: CustomPluginRegistry? = null,
    lambdas: IntObjectMap<() -> Unit> = emptyIntObjectMap(),
    pendingIntents: IntObjectMap<PendingIntent> = emptyIntObjectMap(),
    typefaceResolver: RcTypefaceResolver? = LocalRcTypefaceResolver.current,
    theme: Int = Theme.SYSTEM,
) {
    val features =
        remember(state.document) { RcPlayerFeatures.forProfiles(state.document.profileMask) }
    RcPlayerImpl(
        state = state,
        features = features,
        modifier = modifier,
        imageLoader = imageLoader,
        trustedContent = trustedContent,
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
 * A player of a [CoreDocument] that runs with a fixed set of [features], on top of the built-in
 * support for [RcPlayerFeatureSet.BuiltInProfiles].
 *
 * Unlike the overload without [features], this one doesn't reference every feature the library
 * provides, so R8 can remove the features an app doesn't pass. Operations that need a missing
 * feature are skipped, and a warning lists the opcodes of the document's profile that the player
 * can't execute.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApiAndroidX")
@Suppress("PrimitiveInCollection")
@Composable
public fun RcPlayer(
    state: RcPlayerState,
    features: RcPlayerFeatureSet,
    modifier: Modifier = Modifier,
    imageLoader: RcImageLoader? = null,
    trustedContent: Boolean = false,
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    customPlugins: CustomPluginRegistry? = null,
    lambdas: IntObjectMap<() -> Unit> = emptyIntObjectMap(),
    pendingIntents: IntObjectMap<PendingIntent> = emptyIntObjectMap(),
    typefaceResolver: RcTypefaceResolver? = LocalRcTypefaceResolver.current,
    theme: Int = Theme.SYSTEM,
) {
    RcPlayerImpl(
        state = state,
        features = features,
        modifier = modifier,
        imageLoader = imageLoader,
        trustedContent = trustedContent,
        onAction = onAction,
        onNamedAction = onNamedAction,
        customPlugins = customPlugins,
        lambdas = lambdas,
        pendingIntents = pendingIntents,
        typefaceResolver = typefaceResolver,
        theme = theme,
    )
}

@OptIn(ExperimentalRemotePlayerApi::class)
@SuppressLint("RestrictedApiAndroidX")
@Suppress("PrimitiveInCollection")
@Composable
internal fun RcPlayerImpl(
    state: RcPlayerState,
    features: RcPlayerFeatureSet,
    modifier: Modifier = Modifier,
    imageLoader: RcImageLoader? = null,
    trustedContent: Boolean = false,
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    customPlugins: CustomPluginRegistry? = null,
    lambdas: IntObjectMap<() -> Unit> = emptyIntObjectMap(),
    pendingIntents: IntObjectMap<PendingIntent> = emptyIntObjectMap(),
    typefaceResolver: RcTypefaceResolver? = LocalRcTypefaceResolver.current,
    theme: Int = Theme.SYSTEM,
) {
    check(RemoteComposePlayerFlags.isEmbeddedPlayerEnabled) {
        "Embedded player is disabled. Set RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true to enable."
    }

    (state.remoteContext.mRemoteComposeState as? SnapshotRemoteComposeState)?.trustedContent =
        trustedContent

    val document = state.document
    val clock = remember(document) { document.clock }

    val isDark = isSystemInDarkTheme()
    val resolvedTheme = remember(theme, isDark) { resolveThemeMode(theme, isDark) }
    val androidContext = LocalContext.current

    val preprocessed = state.preprocessed

    val density = LocalDensity.current
    val remoteContext =
        remember(preprocessed, state, trustedContent) {
            val ctx = state.remoteContext
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
                CoreDocument.ShaderControl { trustedContent },
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
    //   WakeIn or impulses run the per-frame loop via withInfiniteAnimationFrameMillis. Impulses
    //   count as continuous time: their draw reads the animation time only until their window
    //   closes, so the loop can idle afterwards.
    // - Documents with only discrete wall-clock/calendar variables (ID_TIME_IN_SEC, ID_TIME_IN_MIN,
    //   ID_CALENDAR_MONTH, etc.) sleep between whole second boundaries via delay().
    // - Static documents and documents whose animations are driven by Compose's own animation
    //   clocks (FloatAnimation via Animatable, StateLayout via AnimatedContent) keep the t=0 seeded
    //   by GraphTimeState and never request a frame.
    // The loop awaits infinite-animation frames, so a host can pause clock-driven content (e.g. in
    // previews or offscreen) with an InfiniteAnimationPolicy in the composition's coroutine
    // context; finite tween, spring and visibility animations keep running.
    // The document scan is an upper bound. ClockScheduler narrows it from the time reads that the
    // active composition, layout and draw passes make, so an unreferenced expression or an
    // inactive state or fit-box branch that reads time doesn't keep the loop ticking.
    val currentTimeMillisState = state.currentTimeMillisState
    val needsContinuousLoop =
        state.hasContinuousTime || preprocessed.hasParticles || preprocessed.hasWakeIn
    val needsDiscreteLoop = !needsContinuousLoop && state.hasDiscreteTime

    // Pure-Compose evaluation of *derived/computed* operations (color & text expressions,
    // attributes, lookups). Each computed id resolves to a derivedStateOf that runs the op's
    // existing updateVariables+apply against this GraphContext, which routes the op's reads to the
    // reactive store / other computed States and captures its write as the result. No imperative
    // recompute pass, no dirty flags — changing an input invalidates exactly the dependent States,
    // and chains compose naturally.
    val textMeasurer = rememberTextMeasurer()
    val graphContext =
        state.graphContext.also { gc ->
            gc.typefaceResolver = typefaceResolver
            gc.textMeasurer = textMeasurer
        }
    DisposableEffect(graphContext, typefaceResolver) {
        graphContext.typefaceCache.onAttach(typefaceResolver)
        onDispose { graphContext.typefaceCache.onDetach() }
    }

    // Frame draws of the host window mean draw passes really run, so from then on time fields that
    // nobody reads after a change let the clock loop idle. Environments that never run frame draws
    // (such as Robolectric without real draw traversals) never see this and keep ticking.
    val view = LocalView.current
    DisposableEffect(view, state) {
        val timeReads = state.timeReads
        val listener = ViewTreeObserver.OnDrawListener { timeReads.drawObserved = true }
        // A detached view's observer is merged into the window's when the view is attached.
        view.viewTreeObserver.addOnDrawListener(listener)
        onDispose {
            val observer = view.viewTreeObserver
            if (observer.isAlive) observer.removeOnDrawListener(listener)
        }
    }

    val startClockMillis = remember(document, clock) { clock.millis() }
    val limiter = remember(document) { Limiter() }
    LaunchedEffect(
        document,
        graphContext,
        needsContinuousLoop,
        needsDiscreteLoop,
    ) {
        remoteContext.currentTime = startClockMillis
        // Static document: time stays at the t = 0 seeded at setup, so don't wait for any frame.
        if (!needsContinuousLoop && !needsDiscreteLoop) return@LaunchedEffect

        val scheduler =
            ClockScheduler(
                reads = state.timeReads,
                needsContinuous = needsContinuousLoop,
                discreteTimeMask = state.discreteTimeMask,
                // Particles and wake-in operations need frames whether or not time is read.
                canIdle = !preprocessed.hasParticles && !preprocessed.hasWakeIn,
            )
        val startMillis = withInfiniteAnimationFrameMillis { it }
        while (true) {
            val frameMillis = withInfiniteAnimationFrameMillis { it } - startMillis
            if (scheduler.evaluate() == ClockScheduler.Mode.PARKED) {
                // Nothing composed, laid out or drawn reads time: wait for a read, then take a
                // fresh frame so the reader sees the current time.
                scheduler.awaitRead()
                continue
            }
            val continuous = scheduler.mode == ClockScheduler.Mode.CONTINUOUS
            limiter.recordDrawStart(frameMillis * 1_000_000L)
            state.updateTime(frameMillis = frameMillis.toFloat(), updateContinuous = continuous)
            remoteContext.currentTime = startClockMillis + frameMillis
            val timeState = graphContext.timeState
            // Slow fields (hour, day, year, ...) rarely change, so probe whether anything reads
            // them.
            timeState.probe(scheduler.probeMask(timeState.lastChangedDiscreteMask))
            scheduler.afterWrite(timeState.lastChangedDiscreteMask)

            if (!continuous) {
                val currentMillis = startClockMillis + frameMillis
                val millisToNextSecond = 1000L - Math.floorMod(currentMillis, 1000L)
                scheduler.awaitDiscreteTick(millisToNextSecond)
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
    // RootContentDescription op, resolved onto the document during initializeContext) labels the
    // whole player for accessibility — the embedded equivalent of the View player's root-view
    // contentDescription.
    val rootContentDescription = remember(document) { document.contentDescription }

    RcPlayerHapticEffect(document)

    val featureDocumentScope =
        remember(document, remoteContext) { RcFeatureDocumentScope(document, remoteContext) }
    features.features.fastForEach { feature ->
        key(feature.key) { feature.Install(featureDocumentScope) }
    }
    LaunchedEffect(document, features) {
        val unsupported = features.unsupportedOpcodes(document.profileMask)
        if (unsupported.isNotEmpty()) {
            Log.w(
                "RcPlayer",
                "Document profile 0x${document.profileMask.toString(16)} needs opcodes " +
                    "$unsupported that $features does not execute",
            )
        }
    }

    // Desired frame rate (Header DOC_DESIRED_FPS): expressed as a platform hint via Compose's
    // preferredFrameRate modifier (which sets the layer's frame rate) rather than throttling the
    // time ticker ourselves — the system then drives frames at this rate and the withFrameMillis
    // loop above advances time at the same cadence. 0 = no preference (absent / non-positive
    // value).
    val desiredFps =
        remember(document) {
            (document.getProperty(Header.DOC_DESIRED_FPS) as? Int)
                ?.takeIf { it > 0 }
                ?.coerceAtMost(Limits.MAX_FPS)
                ?.toFloat() ?: 0f
        }

    val rootCoordsHolder = remember(document) { RootLayoutCoordinatesHolder() }

    // Identify ComponentValue operations, each with reactive state that graph reads resolve first.
    // Created here so the root's own values (published from its onPlaced below) reach it too.
    val componentValueMap = preprocessed.componentValueMap
    val componentValueStateMap =
        remember(componentValueMap) {
            val states = mutableMapOf<Int, MutableState<Float>>()
            componentValueMap.values.forEach { list ->
                list.fastForEach { op ->
                    if (!states.containsKey(op.valueId)) {
                        states[op.valueId] = mutableFloatStateOf(0f)
                    }
                }
            }
            states
        }
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
                    val previousSize = rootCoordsHolder.publishedSize
                    if (size != previousSize) {
                        rootCoordsHolder.publishedSize = size
                        val root = document.rootLayoutComponent
                        if (root != null) {
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            root.setWidth(w)
                            root.setHeight(h)
                            // Re-evaluate the root's ComponentValues (its WIDTH / HEIGHT) in the
                            // store.
                            root.updateVariables(remoteContext)
                            // Also publish them to their reactive state, which graph reads resolve
                            // before the store. The root has no padding, so its content is its
                            // whole size.
                            val rootOps = componentValueMap[root.componentId]
                            if (!rootOps.isNullOrEmpty()) {
                                publishComponentValues(
                                    ops = rootOps,
                                    width = w,
                                    height = h,
                                    contentWidth = w,
                                    contentHeight = h,
                                    posInParent = Offset.Zero,
                                    rootOffset = Offset.Zero,
                                    store = remoteContext,
                                    states = componentValueStateMap,
                                    initial = previousSize == null,
                                )
                            }
                        } else if (document.contentSizing != RootContentBehavior.SIZING_SCALE) {
                            // Raw draw-list document: mirror CoreDocument.paint, so draws
                            // positioned by the document size resolve. A document that scales
                            // its content keeps its own size (see RcPlayerRawDocument).
                            document.setWidth(size.width)
                            document.setHeight(size.height)
                        }
                    }
                }
                .then(
                    // Only listen when root touch expressions or touch position readers need the
                    // pointer, so a press or drag on a document without them writes no state.
                    if (preprocessed.rootTouchExpressions.isNotEmpty() || state.readsTouchPosition)
                        Modifier.rootTouchExpressions(
                            document,
                            remoteContext,
                            preprocessed,
                            graphContext,
                        )
                    else Modifier
                )
    ) {
        // ColorConstant / IntegerConstant / FloatExpression defaults already live in the
        // snapshot-backed store (applied by applyOperations during setup); the single store is the
        // source of truth for variables now, so there is no separate draw-path map to populate.
        graphContext.componentValues = componentValueStateMap

        val stateUpdater = remember(remoteContext) { StateUpdaterImpl(remoteContext) }
        val pendingIntentHandler =
            remember(androidContext, pendingIntents) {
                PendingIntentActionHandler(androidContext, pendingIntents)
            }
        // The image loader: the caller-supplied one, or the default that wraps embedded bitmaps.
        val resolvedImageLoader =
            remember(remoteContext, imageLoader) {
                imageLoader ?: EmbeddedRcImageLoader(remoteContext)
            }
        // Make it reachable from the (non-composable) canvas draw path too — the document image
        // draws resolve through it via the GraphContext.
        graphContext.imageLoader = resolvedImageLoader
        graphContext.features = features
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
            // A host action either sends a PendingIntentAction's PendingIntent, whose id is
            // allocated by the library, or is delivered to the host. Never both.
            LocalRemoteActionHandler provides
                { id, metadata ->
                    if (!pendingIntentHandler.onAction(id, metadata)) {
                        onAction(id, metadata)
                    }
                },
            LocalRemoteNamedActionHandler provides
                { name, value ->
                    val lambdaId = LambdaAction.parseId(name)
                    if (lambdaId != null) {
                        lambdas[lambdaId]?.invoke()
                        onNamedAction(name, value, stateUpdater)
                    } else if (!pendingIntentHandler.onNamedAction(name, value)) {
                        onNamedAction(name, value, stateUpdater)
                    }
                },
            LocalRcCustomPlugins provides customPlugins,
            LocalRcTypefaceResolver provides typefaceResolver,
        ) {
            val animatedExpressionIds =
                remember(document) {
                    val ids = MutableIntList()
                    document.getFloatExpressions().forEach { (id, expr) ->
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
            // Click area boxes obscure areas below them, matching the View player's ClickAreaView
            // overlay behavior.
            RcPlayerClickAreas(document)
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
 * not supported.
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
    trustedContent: Boolean = false,
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    customPlugins: CustomPluginRegistry? = null,
    lambdas: IntObjectMap<() -> Unit> = emptyIntObjectMap(),
    pendingIntents: IntObjectMap<PendingIntent> = emptyIntObjectMap(),
    typefaceResolver: RcTypefaceResolver? = LocalRcTypefaceResolver.current,
    theme: Int = Theme.SYSTEM,
) {
    val state = rememberRcPlayerState(document)
    RcPlayer(
        state = state,
        modifier = modifier,
        imageLoader = imageLoader,
        trustedContent = trustedContent,
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
 * A player of a [CoreDocument] that runs with a fixed set of [features]. See the [RcPlayerState]
 * overload taking an [RcPlayerFeatureSet].
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApiAndroidX")
@Suppress("PrimitiveInCollection")
@Composable
public fun RcPlayer(
    document: CoreDocument,
    features: RcPlayerFeatureSet,
    modifier: Modifier = Modifier,
    imageLoader: RcImageLoader? = null,
    trustedContent: Boolean = false,
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    customPlugins: CustomPluginRegistry? = null,
    lambdas: IntObjectMap<() -> Unit> = emptyIntObjectMap(),
    pendingIntents: IntObjectMap<PendingIntent> = emptyIntObjectMap(),
    typefaceResolver: RcTypefaceResolver? = LocalRcTypefaceResolver.current,
    theme: Int = Theme.SYSTEM,
) {
    val state = rememberRcPlayerState(document)
    RcPlayerImpl(
        state = state,
        features = features,
        modifier = modifier,
        imageLoader = imageLoader,
        trustedContent = trustedContent,
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
    trustedContent: Boolean = false,
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    customPlugins: CustomPluginRegistry? = null,
    typefaceResolver: RcTypefaceResolver? = LocalRcTypefaceResolver.current,
    theme: Int = Theme.SYSTEM,
) {
    val state = rememberRcPlayerState(capturedDocument)
    RcPlayer(
        state = state,
        modifier = modifier,
        imageLoader = imageLoader,
        trustedContent = trustedContent,
        onAction = onAction,
        onNamedAction = onNamedAction,
        customPlugins = customPlugins,
        lambdas = capturedDocument.lambdas,
        pendingIntents = capturedDocument.pendingIntents,
        typefaceResolver = typefaceResolver,
        theme = theme,
    )
}
