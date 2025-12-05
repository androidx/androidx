/*
 * Copyright 2025 The Android Open Source Project
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

@file:JvmName("NavDisplayKt")
@file:JvmMultifileClass

package androidx.navigation3.ui

import androidx.collection.mutableObjectFloatMapOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.LocalEntriesToExcludeFromCurrentScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SceneState
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay.POP_TRANSITION_SPEC
import androidx.navigation3.ui.NavDisplay.PREDICTIVE_POP_TRANSITION_SPEC
import androidx.navigation3.ui.NavDisplay.TRANSITION_SPEC
import androidx.navigation3.ui.NavDisplay.popTransitionSpec
import androidx.navigation3.ui.NavDisplay.predictivePopTransitionSpec
import androidx.navigation3.ui.NavDisplay.transitionSpec
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState.Idle
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Object that indicates the features that can be handled by the [NavDisplay] */
public object NavDisplay {
    /**
     * Function to be called on the [NavEntry.metadata] or [Scene.metadata] to notify the
     * [NavDisplay] of how the content should be animated using the provided [ContentTransform].
     *
     * **IMPORTANT** [NavDisplay] only looks at the [Scene.metadata] to determine the
     * [transitionSpec], it is the responsibility of the [Scene.metadata] to decide which
     * [transitionSpec] to return, whether that be from the [NavEntry.metadata] or something custom.
     *
     * @param transitionSpec the [ContentTransform] to be used when adding to the backstack. If this
     *   is null, the transition will fallback to the transition set on the [NavDisplay]
     */
    public fun transitionSpec(
        transitionSpec: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform?
    ): Map<String, Any> = mapOf(TRANSITION_SPEC to transitionSpec)

    /**
     * Function to be called on the [NavEntry.metadata] or [Scene.metadata] to notify the
     * [NavDisplay] that, when popping from backstack, the content should be animated using the
     * provided [ContentTransform].
     *
     * **IMPORTANT** [NavDisplay] only looks at the [Scene.metadata] to determine the
     * [popTransitionSpec], it is the responsibility of the [Scene.metadata] to decide which
     * [popTransitionSpec] to return, whether that be from the [NavEntry.metadata] or something
     * custom.
     *
     * @param popTransitionSpec the [ContentTransform] to be used when popping from backstack. If
     *   this is null, the transition will fallback to the transition set on the [NavDisplay]
     */
    public fun popTransitionSpec(
        popTransitionSpec: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform?
    ): Map<String, Any> = mapOf(POP_TRANSITION_SPEC to popTransitionSpec)

    /**
     * Function to be called on the [NavEntry.metadata] or [Scene.metadata] to notify the
     * [NavDisplay] that, when popping from backstack using a Predictive back gesture, the content
     * should be animated using the provided [ContentTransform].
     *
     * **IMPORTANT** [NavDisplay] only looks at the [Scene.metadata] to determine the
     * [predictivePopTransitionSpec], it is the responsibility of the [Scene.metadata] to decide
     * which [predictivePopTransitionSpec] to return, whether that be from the [NavEntry.metadata]
     * or something custom.
     *
     * @param predictivePopTransitionSpec the [ContentTransform] to be used when popping from
     *   backStack with predictive back gesture. If this is null, the transition will fallback to
     *   the transition set on the [NavDisplay]
     */
    public fun predictivePopTransitionSpec(
        predictivePopTransitionSpec:
            AnimatedContentTransitionScope<Scene<*>>.(
                @NavigationEvent.SwipeEdge Int
            ) -> ContentTransform?
    ): Map<String, Any> = mapOf(PREDICTIVE_POP_TRANSITION_SPEC to predictivePopTransitionSpec)

    internal const val TRANSITION_SPEC = "transitionSpec"
    internal const val POP_TRANSITION_SPEC = "popTransitionSpec"
    internal const val PREDICTIVE_POP_TRANSITION_SPEC = "predictivePopTransitionSpec"
}

/**
 * A nav display that renders and animates between different [Scene]s, each of which can render one
 * or more [NavEntry]s.
 *
 * The [Scene]s are calculated with the given [SceneStrategy], which may be an assembled delegated
 * chain of [SceneStrategy]s. If no [Scene] is calculated, the fallback will be to a
 * [SinglePaneSceneStrategy].
 *
 * It is allowable for different [Scene]s to render the same [NavEntry]s, perhaps on some conditions
 * as determined by the [sceneStrategy] based on window size, form factor, other arbitrary logic.
 *
 * If this happens, and these [Scene]s are rendered at the same time due to animation or predictive
 * back, then the content for the [NavEntry] will only be rendered in the most recent [Scene] that
 * is the target for being the current scene as determined by [sceneStrategy]. This enforces a
 * unique invocation of each [NavEntry], even if it is displayable by two different [Scene]s.
 *
 * By default, AnimatedContent transitions are prioritized in this order:
 * ```
 * transitioning [NavEntry.metadata] > current [Scene.metadata] > NavDisplay defaults
 * ```
 *
 * However, a [Scene.metadata] does have the ability to override [NavEntry.metadata]. Nevertheless,
 * the final fallback will always be the NavDisplay's default transitions.
 *
 * @param backStack the collection of keys that represents the state that needs to be handled
 * @param modifier the modifier to be applied to the layout.
 * @param contentAlignment The [Alignment] of the [AnimatedContent]
 * @param onBack a callback for handling system back press. By default, this pops a single item off
 *   of the given back stack if it is a [MutableList], otherwise you should provide this parameter.
 * @param entryDecorators list of [NavEntryDecorator] to add information to the entry content
 * @param sceneStrategy the [SceneStrategy] to determine which scene to render a list of entries.
 * @param sizeTransform the [SizeTransform] for the [AnimatedContent].
 * @param transitionSpec Default [ContentTransform] when navigating to [NavEntry]s.
 * @param popTransitionSpec Default [ContentTransform] when popping [NavEntry]s.
 * @param predictivePopTransitionSpec Default [ContentTransform] when popping with predictive back
 *   [NavEntry]s.
 * @param entryProvider lambda used to construct each possible [NavEntry]
 * @sample androidx.navigation3.ui.samples.SceneNav
 * @sample androidx.navigation3.ui.samples.SceneNavSharedElementSample
 */
@Deprecated(
    message = "Deprecated in favor of NavDisplay that supports sharedTransitionScope",
    level = DeprecationLevel.HIDDEN,
)
@Composable
public fun <T : Any> NavDisplay(
    backStack: List<T>,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    onBack: () -> Unit = {
        if (backStack is MutableList<T>) {
            backStack.removeLastOrNull()
        }
    },
    entryDecorators: List<NavEntryDecorator<T>> =
        listOf(rememberSaveableStateHolderNavEntryDecorator()),
    sceneStrategy: SceneStrategy<T> = SinglePaneSceneStrategy(),
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        defaultTransitionSpec(),
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        defaultPopTransitionSpec(),
    predictivePopTransitionSpec:
        AnimatedContentTransitionScope<Scene<T>>.(
            @NavigationEvent.SwipeEdge Int
        ) -> ContentTransform =
        defaultPredictivePopTransitionSpec(),
    entryProvider: (key: T) -> NavEntry<T>,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        contentAlignment = contentAlignment,
        onBack = onBack,
        entryDecorators = entryDecorators,
        sceneStrategy = sceneStrategy,
        sharedTransitionScope = null,
        sizeTransform = sizeTransform,
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec,
        entryProvider = entryProvider,
    )
}

/**
 * A nav display that renders and animates between different [Scene]s, each of which can render one
 * or more [NavEntry]s.
 *
 * The [Scene]s are calculated with the given [SceneStrategy], which may be an assembled delegated
 * chain of [SceneStrategy]s. If no [Scene] is calculated, the fallback will be to a
 * [SinglePaneSceneStrategy].
 *
 * It is allowable for different [Scene]s to render the same [NavEntry]s, perhaps on some conditions
 * as determined by the [sceneStrategy] based on window size, form factor, other arbitrary logic.
 *
 * If this happens, and these [Scene]s are rendered at the same time due to animation or predictive
 * back, then the content for the [NavEntry] will only be rendered in the most recent [Scene] that
 * is the target for being the current scene as determined by [sceneStrategy]. This enforces a
 * unique invocation of each [NavEntry], even if it is displayable by two different [Scene]s.
 *
 * By default, AnimatedContent transitions are prioritized in this order:
 * ```
 * transitioning [NavEntry.metadata] > current [Scene.metadata] > NavDisplay defaults
 * ```
 *
 * However, a [Scene.metadata] does have the ability to override [NavEntry.metadata]. Nevertheless,
 * the final fallback will always be the NavDisplay's default transitions.
 *
 * @param backStack the collection of keys that represents the state that needs to be handled
 * @param modifier the modifier to be applied to the layout.
 * @param contentAlignment The [Alignment] of the [AnimatedContent]
 * @param onBack a callback for handling system back press. By default, this pops a single item off
 *   of the given back stack if it is a [MutableList], otherwise you should provide this parameter.
 * @param entryDecorators list of [NavEntryDecorator] to add information to the entry content
 * @param sceneStrategy the [SceneStrategy] to determine which scene to render a list of entries.
 * @param sharedTransitionScope the [SharedTransitionScope] to allow transitions between scenes.
 * @param sizeTransform the [SizeTransform] for the [AnimatedContent].
 * @param transitionSpec Default [ContentTransform] when navigating to [NavEntry]s.
 * @param popTransitionSpec Default [ContentTransform] when popping [NavEntry]s.
 * @param predictivePopTransitionSpec Default [ContentTransform] when popping with predictive back
 *   [NavEntry]s.
 * @param entryProvider lambda used to construct each possible [NavEntry]
 * @sample androidx.navigation3.ui.samples.SceneNav
 * @sample androidx.navigation3.ui.samples.SceneNavSharedEntrySample
 * @sample androidx.navigation3.ui.samples.SceneNavSharedElementSample
 */
@Composable
public fun <T : Any> NavDisplay(
    backStack: List<T>,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    onBack: () -> Unit = {
        if (backStack is MutableList<T>) {
            backStack.removeLastOrNull()
        }
    },
    entryDecorators: List<NavEntryDecorator<T>> =
        listOf(rememberSaveableStateHolderNavEntryDecorator()),
    sceneStrategy: SceneStrategy<T> = SinglePaneSceneStrategy(),
    sharedTransitionScope: SharedTransitionScope? = null,
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        defaultTransitionSpec(),
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        defaultPopTransitionSpec(),
    predictivePopTransitionSpec:
        AnimatedContentTransitionScope<Scene<T>>.(
            @NavigationEvent.SwipeEdge Int
        ) -> ContentTransform =
        defaultPredictivePopTransitionSpec(),
    entryProvider: (key: T) -> NavEntry<T>,
) {
    require(backStack.isNotEmpty()) { "NavDisplay backstack cannot be empty" }

    val entries =
        rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = entryDecorators,
            entryProvider = entryProvider,
        )

    NavDisplay(
        entries = entries,
        sceneStrategy = sceneStrategy,
        sharedTransitionScope = sharedTransitionScope,
        modifier = modifier,
        contentAlignment = contentAlignment,
        sizeTransform = sizeTransform,
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec,
        onBack = onBack,
    )
}

/**
 * A nav display that renders and animates between different [Scene]s, each of which can render one
 * or more [NavEntry]s.
 *
 * The [Scene]s are calculated with the given [SceneStrategy], which may be an assembled delegated
 * chain of [SceneStrategy]s. If no [Scene] is calculated, the fallback will be to a
 * [SinglePaneSceneStrategy].
 *
 * It is allowable for different [Scene]s to render the same [NavEntry]s, perhaps on some conditions
 * as determined by the [sceneStrategy] based on window size, form factor, other arbitrary logic.
 *
 * If this happens, and these [Scene]s are rendered at the same time due to animation or predictive
 * back, then the content for the [NavEntry] will only be rendered in the most recent [Scene] that
 * is the target for being the current scene as determined by [sceneStrategy]. This enforces a
 * unique invocation of each [NavEntry], even if it is displayable by two different [Scene]s.
 *
 * By default, AnimatedContent transitions are prioritized in this order:
 * ```
 * transitioning [NavEntry.metadata] > current [Scene.metadata] > NavDisplay defaults
 * ```
 *
 * However, a [Scene.metadata] does have the ability to override [NavEntry.metadata]. Nevertheless,
 * the final fallback will always be the NavDisplay's default transitions.
 *
 * **WHEN TO USE** This overload can be used when you need to switch between different backStacks
 * and each with their own separate decorator states, or when you want to concatenate backStacks and
 * their states to form a larger backstack.
 *
 * **HOW TO USE** The [entries] can first be created via [rememberDecoratedNavEntries] in order to
 * associate a backStack with a particular set of states.
 *
 * @param entries the list of [NavEntry] built from a backStack. The entries can be created from a
 *   backStack decorated with [NavEntryDecorator] via [rememberDecoratedNavEntries].
 * @param modifier the modifier to be applied to the layout.
 * @param contentAlignment The [Alignment] of the [AnimatedContent]
 * @param sceneStrategy the [SceneStrategy] to determine which scene to render a list of entries.
 * @param sizeTransform the [SizeTransform] for the [AnimatedContent].
 * @param transitionSpec Default [ContentTransform] when navigating to [NavEntry]s.
 * @param popTransitionSpec Default [ContentTransform] when popping [NavEntry]s.
 * @param predictivePopTransitionSpec Default [ContentTransform] when popping with predictive back
 *   [NavEntry]s.
 * @param onBack a callback for handling system back press.
 * @sample androidx.navigation3.ui.samples.MultipleBackStackSample
 * @sample androidx.navigation3.ui.samples.ConcatenatedBackStackSample
 * @see [rememberDecoratedNavEntries]
 */
@Deprecated(
    message = "Deprecated in favor of NavDisplay that supports sharedTransitionScope",
    level = DeprecationLevel.HIDDEN,
)
@Composable
public fun <T : Any> NavDisplay(
    entries: List<NavEntry<T>>,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    sceneStrategy: SceneStrategy<T> = SinglePaneSceneStrategy(),
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        defaultTransitionSpec(),
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        defaultPopTransitionSpec(),
    predictivePopTransitionSpec:
        AnimatedContentTransitionScope<Scene<T>>.(
            @NavigationEvent.SwipeEdge Int
        ) -> ContentTransform =
        defaultPredictivePopTransitionSpec(),
    onBack: () -> Unit,
) {
    NavDisplay(
        entries = entries,
        sceneStrategy = sceneStrategy,
        sharedTransitionScope = null,
        modifier = modifier,
        contentAlignment = contentAlignment,
        sizeTransform = sizeTransform,
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec,
        onBack = onBack,
    )
}

/**
 * A nav display that renders and animates between different [Scene]s, each of which can render one
 * or more [NavEntry]s.
 *
 * The [Scene]s are calculated with the given [SceneStrategy], which may be an assembled delegated
 * chain of [SceneStrategy]s. If no [Scene] is calculated, the fallback will be to a
 * [SinglePaneSceneStrategy].
 *
 * It is allowable for different [Scene]s to render the same [NavEntry]s, perhaps on some conditions
 * as determined by the [sceneStrategy] based on window size, form factor, other arbitrary logic.
 *
 * If this happens, and these [Scene]s are rendered at the same time due to animation or predictive
 * back, then the content for the [NavEntry] will only be rendered in the most recent [Scene] that
 * is the target for being the current scene as determined by [sceneStrategy]. This enforces a
 * unique invocation of each [NavEntry], even if it is displayable by two different [Scene]s.
 *
 * By default, AnimatedContent transitions are prioritized in this order:
 * ```
 * transitioning [NavEntry.metadata] > current [Scene.metadata] > NavDisplay defaults
 * ```
 *
 * However, a [Scene.metadata] does have the ability to override [NavEntry.metadata]. Nevertheless,
 * the final fallback will always be the NavDisplay's default transitions.
 *
 * **WHEN TO USE** This overload can be used when you need to switch between different backStacks
 * and each with their own separate decorator states, or when you want to concatenate backStacks and
 * their states to form a larger backstack.
 *
 * **HOW TO USE** The [entries] can first be created via [rememberDecoratedNavEntries] in order to
 * associate a backStack with a particular set of states.
 *
 * @param entries the list of [NavEntry] built from a backStack. The entries can be created from a
 *   backStack decorated with [NavEntryDecorator] via [rememberDecoratedNavEntries].
 * @param modifier the modifier to be applied to the layout.
 * @param contentAlignment The [Alignment] of the [AnimatedContent]
 * @param sceneStrategy the [SceneStrategy] to determine which scene to render a list of entries.
 * @param sharedTransitionScope the [SharedTransitionScope] to allow transitions between scenes.
 * @param sizeTransform the [SizeTransform] for the [AnimatedContent].
 * @param transitionSpec Default [ContentTransform] when navigating to [NavEntry]s.
 * @param popTransitionSpec Default [ContentTransform] when popping [NavEntry]s.
 * @param predictivePopTransitionSpec Default [ContentTransform] when popping with predictive back
 *   [NavEntry]s.
 * @param onBack a callback for handling system back press.
 * @sample androidx.navigation3.ui.samples.MultipleBackStackSample
 * @sample androidx.navigation3.ui.samples.ConcatenatedBackStackSample
 * @sample androidx.navigation3.ui.samples.SceneNavSharedEntrySample
 * @see [rememberDecoratedNavEntries]
 */
@Composable
public fun <T : Any> NavDisplay(
    entries: List<NavEntry<T>>,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    sceneStrategy: SceneStrategy<T> = SinglePaneSceneStrategy(),
    sharedTransitionScope: SharedTransitionScope? = null,
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        defaultTransitionSpec(),
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        defaultPopTransitionSpec(),
    predictivePopTransitionSpec:
        AnimatedContentTransitionScope<Scene<T>>.(
            @NavigationEvent.SwipeEdge Int
        ) -> ContentTransform =
        defaultPredictivePopTransitionSpec(),
    onBack: () -> Unit,
) {
    require(entries.isNotEmpty()) { "NavDisplay entries cannot be empty" }

    val sceneState = rememberSceneState(entries, sceneStrategy, sharedTransitionScope, onBack)
    val scene = sceneState.currentScene

    // Predictive Back Handling
    val currentInfo = SceneInfo(scene)
    val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
    val gestureState =
        rememberNavigationEventState(currentInfo = currentInfo, backInfo = previousSceneInfos)

    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCompleted = {
            // If `enabled` becomes stale (e.g., it was set to false but a gesture was
            // dispatched in the same frame), this may result in no entries being popped
            // due to entries.size being smaller than scene.previousEntries.size
            // but that's preferable to crashing with an IndexOutOfBoundsException
            repeat(entries.size - scene.previousEntries.size) { onBack() }
        },
    )

    NavDisplay(
        sceneState,
        gestureState,
        modifier,
        contentAlignment,
        sizeTransform,
        transitionSpec,
        popTransitionSpec,
        predictivePopTransitionSpec,
    )
}

/**
 * A nav display that renders and animates between different [Scene]s, each of which can render one
 * or more [NavEntry]s.
 *
 * By default, AnimatedContent transitions are prioritized in this order:
 * ```
 * transitioning [NavEntry.metadata] > current [Scene.metadata] > NavDisplay defaults
 * ```
 *
 * However, a [Scene.metadata] does have the ability to override [NavEntry.metadata]. Nevertheless,
 * the final fallback will always be the NavDisplay's default transitions.
 *
 * @param sceneState the state that determines what current scene of the NavDisplay.
 * @param modifier the modifier to be applied to the layout.
 * @param contentAlignment The [Alignment] of the [AnimatedContent]
 * @param navigationEventState the [NavigationEventState] responsible for handling back navigation
 * @param sizeTransform the [SizeTransform] for the [AnimatedContent].
 * @param transitionSpec Default [ContentTransform] when navigating to [NavEntry]s.
 * @param popTransitionSpec Default [ContentTransform] when popping [NavEntry]s.
 * @param predictivePopTransitionSpec Default [ContentTransform] when popping with predictive back
 *   [NavEntry]s.
 * @sample androidx.navigation3.scene.samples.SceneStateSample
 * @see [rememberSceneState]
 */
@Composable
public fun <T : Any> NavDisplay(
    sceneState: SceneState<T>,
    navigationEventState: NavigationEventState<SceneInfo<T>>,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        defaultTransitionSpec(),
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
        defaultPopTransitionSpec(),
    predictivePopTransitionSpec:
        AnimatedContentTransitionScope<Scene<T>>.(
            @NavigationEvent.SwipeEdge Int
        ) -> ContentTransform =
        defaultPredictivePopTransitionSpec(),
) {
    // Calculate current Scene and set up transitions
    val scene = sceneState.currentScene
    val transitionState = remember {
        // The state returned here cannot be nullable cause it produces the input of the
        // transitionSpec passed into the AnimatedContent and that must match the non-nullable
        // scope exposed by the transitions on the NavHost and composable APIs.
        SeekableTransitionState(scene)
    }

    val transition = rememberTransition(transitionState, label = "scene")

    // Transition Handling
    /** Keep track of the previous entries for the transition's current scene. */
    val transitionCurrentStateEntries =
        remember(transition.currentState) { sceneState.entries.toList() }

    // Set up Gesture Back tracking
    val previousScene = sceneState.previousScenes.lastOrNull()
    val gestureTransition = navigationEventState.transitionState

    val inPredictiveBack = gestureTransition is InProgress && previousScene != null
    val progress =
        when (gestureTransition) {
            is Idle -> 0f
            is InProgress -> gestureTransition.latestEvent.progress
        }
    val swipeEdge =
        when (gestureTransition) {
            is Idle -> NavigationEvent.EDGE_NONE
            is InProgress -> gestureTransition.latestEvent.swipeEdge
        }

    val isPop =
        isPop(
            // Consider this a pop if the current entries match the previous entries we have
            // recorded
            // from the current state of the transition
            transitionCurrentStateEntries.map { it.contentKey },
            sceneState.entries.map { it.contentKey },
        )

    // Track currently rendered Scenes and their ZIndices
    val sceneMap = remember { mutableStateMapOf<Pair<KClass<*>, Any>, Scene<T>>() }
    val zIndices = remember { mutableObjectFloatMapOf<Pair<KClass<*>, Any>>() }
    val initialKey = transition.currentState::class to transition.currentState.key
    val targetKey = transition.targetState::class to transition.targetState.key
    val initialZIndex = zIndices.getOrPut(initialKey) { 0f }
    val targetZIndex =
        when {
            initialKey == targetKey -> initialZIndex
            isPop || inPredictiveBack -> initialZIndex - 1f
            else -> initialZIndex + 1f
        }
    sceneMap[targetKey] = transition.targetState
    zIndices[targetKey] = targetZIndex

    val overlayScenes = sceneState.overlayScenes

    // Determine which entries should be rendered within each currently rendered scene,
    // using the z-index of each screen to always show the entry on the topmost screen
    // The map is Pair<KCLass<Scene<T>, Scene.key> to a Set of NavEntry.key values
    val sceneToExcludedEntryMap =
        remember(sceneMap.entries.toList(), overlayScenes.toList(), zIndices.toString()) {
            buildMap {
                val scenes = mutableListOf<Scene<T>>()
                // First sort the non-overlay scenes by z-order in descending order.
                sceneMap.entries
                    .sortedByDescending { zIndices[it.key] }
                    .map { it.value }
                    .forEach { if (!scenes.contains(it)) scenes.add(it) }

                // At this point we have a list in this order
                // [zIndex larger --> zIndex smaller]

                // Then combine them with overlay scenes to get the complete order of scenes in
                // z-order
                // overlayScenes is already in order of [top most overlay ---> lowest overlay],
                // so we put overlayScenes in front, and then add the scenes after.
                val scenesInZOrder = overlayScenes + scenes
                // At this point we have a list of all scenes in this order
                // [top most overlay ---> lowest overlay, other scenes zIndex larger --> zIndex
                // smaller]

                // Then we track which entries are already covered
                val coveredEntryKeys = mutableSetOf<Any>()

                // In scenesInZOrder's natural order, go through each scene, marking
                // all of the entries not already covered as associated
                // with that scene. This ensures that each unique contentKey will only be
                // rendered by one scene.
                scenesInZOrder.fastForEach { scene ->
                    val newlyCoveredEntryKeys =
                        scene.entries
                            .map { it.contentKey }
                            .filterNot(coveredEntryKeys::contains)
                            .toSet()
                    put(scene::class to scene.key, coveredEntryKeys.toMutableSet())
                    coveredEntryKeys.addAll(newlyCoveredEntryKeys)
                }
            }
        }

    // Determine which NavEntry's transition to use(if any), prioritizing the one with highest
    // zIndex
    val transitionScene =
        if (initialZIndex >= targetZIndex) {
            transition.currentState
        } else {
            transition.targetState
        }

    // check if in gesture back
    if (inPredictiveBack) {
        if (transition.currentState != previousScene) {
            LaunchedEffect(previousScene, progress) {
                // Retarget on key change; seek on progress updates.
                transitionState.seekTo(progress, previousScene)
            }
        }
    } else {
        LaunchedEffect(scene) {
            if (transitionState.currentState != scene) {
                // We are animating to the final state for regular navigate forward and regular pop
                transitionState.animateTo(scene)
            } else {
                // Predictive Back has either been completed or cancelled
                // so now we need to seekTo+snapTo the final state

                // convert from nanoseconds to milliseconds
                val totalDuration = transition.totalDurationNanos / 1000000
                // Which way we have to seek depends on whether the
                // Predictive Back was completed or cancelled
                val predictiveBackCompleted = transition.targetState == scene
                val (finalFraction, remainingDuration) =
                    if (predictiveBackCompleted) {
                        // If it completed, animate to the state we were
                        // already seeking to with the remaining duration
                        1f to ((1f - transitionState.fraction) * totalDuration).toInt()
                    } else {
                        // It it got cancelled, animate back to the
                        // initial state, reversing what we seeked to
                        0f to (transitionState.fraction * totalDuration).toInt()
                    }
                animate(
                    transitionState.fraction,
                    finalFraction,
                    animationSpec = tween(remainingDuration),
                ) { value, _ ->
                    this@LaunchedEffect.launch {
                        if (value != finalFraction) {
                            // Seek the transition towards the finalFraction
                            transitionState.seekTo(value)
                        }
                        if (value == finalFraction) {
                            // Once the animation finishes, we need to snap to the right state.
                            transitionState.snapTo(scene)
                        }
                    }
                }
            }
        }
    }

    val contentTransform: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        when {
            inPredictiveBack -> {
                transitionScene.predictivePopSpec()?.invoke(this, swipeEdge)
                    ?: predictivePopTransitionSpec(swipeEdge)
            }
            isPop -> {
                transitionScene.contentTransform(POP_TRANSITION_SPEC)?.invoke(this)
                    ?: popTransitionSpec(this)
            }
            else -> {
                transitionScene.contentTransform(TRANSITION_SPEC)?.invoke(this)
                    ?: transitionSpec(this)
            }
        }
    }

    transition.AnimatedContent(
        contentKey = { scene -> scene::class to scene.key },
        contentAlignment = contentAlignment,
        modifier = modifier,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = contentTransform(this).targetContentEnter,
                initialContentExit = contentTransform(this).initialContentExit,
                // z-index increases during navigate and decreases during pop.
                targetContentZIndex = targetZIndex,
                sizeTransform = sizeTransform,
            )
        },
    ) { targetScene ->
        // If there is a transition in progress, set the maximum state of the scene (and every
        // entry within the scene) to STARTED - only allow the RESUMED state when the
        // AnimatedContent has settled into its final state
        val isSettled = transition.currentState == transition.targetState
        val sceneLifecycleOwner =
            rememberLifecycleOwner(
                maxLifecycle = if (isSettled) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
            )
        CompositionLocalProvider(
            LocalLifecycleOwner provides sceneLifecycleOwner,
            LocalNavAnimatedContentScope provides this,
            LocalEntriesToExcludeFromCurrentScene provides
                sceneToExcludedEntryMap.getValue(targetScene::class to targetScene.key),
        ) {
            targetScene.content()
        }
    }

    // Clean-up scene book-keeping once the transition is finished
    LaunchedEffect(transition) {
        snapshotFlow { transition.isRunning }
            .filter { !it }
            .collect {
                val targetKey = transition.targetState::class to transition.targetState.key
                // Creating a copy to avoid ConcurrentModificationException
                @Suppress("ListIterator")
                sceneMap.keys.toList().forEach { key ->
                    if (key != targetKey) {
                        sceneMap.remove(key)
                    }
                }
                // Creating a copy to avoid ConcurrentModificationException
                zIndices.removeIf { key, _ -> key != targetKey }
            }
    }

    // Show all OverlayScene instances above the AnimatedContent
    overlayScenes.fastForEachReversed { overlayScene ->
        CompositionLocalProvider(
            LocalEntriesToExcludeFromCurrentScene provides
                sceneToExcludedEntryMap.getValue(overlayScene::class to overlayScene.key)
        ) {
            overlayScene.content.invoke()
        }
    }
}

private fun <T : Any> isPop(oldBackStack: List<T>, newBackStack: List<T>): Boolean {
    // entire stack replaced
    if (oldBackStack.first() != newBackStack.first()) return false
    // navigated
    if (newBackStack.size > oldBackStack.size) return false

    val divergingIndex =
        newBackStack.indices.firstOrNull { index -> newBackStack[index] != oldBackStack[index] }
    // if newBackStack never diverged from oldBackStack, then it is a clean subset of the oldStack
    // and is a pop
    return divergingIndex == null && newBackStack.size != oldBackStack.size
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> Scene<T>.contentTransform(
    key: String
): (AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform)? {
    return metadata[key] as? AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> Scene<T>.predictivePopSpec():
    (AnimatedContentTransitionScope<Scene<T>>.(
        @NavigationEvent.SwipeEdge Int
    ) -> ContentTransform)? {
    return metadata[PREDICTIVE_POP_TRANSITION_SPEC]
        as?
        AnimatedContentTransitionScope<Scene<T>>.(
            @NavigationEvent.SwipeEdge Int
        ) -> ContentTransform
}

/** Default [transitionSpec] for forward navigation to be used by [NavDisplay]. */
public expect fun <T : Any> defaultTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform

/** Default [transitionSpec] for pop navigation to be used by [NavDisplay]. */
public expect fun <T : Any> defaultPopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform

/** Default [transitionSpec] for predictive pop navigation to be used by [NavDisplay]. */
public expect fun <T : Any> defaultPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform
