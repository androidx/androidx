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

package androidx.navigation3.ui

import androidx.collection.mutableObjectFloatMapOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEachReversed
import androidx.navigation3.runtime.DecoratedNavEntryProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay.DEFAULT_TRANSITION_DURATION_MILLISECOND
import androidx.navigation3.ui.NavDisplay.POP_TRANSITION_SPEC
import androidx.navigation3.ui.NavDisplay.PREDICTIVE_POP_TRANSITION_SPEC
import androidx.navigation3.ui.NavDisplay.TRANSITION_SPEC
import androidx.navigationevent.NavigationEvent.Companion.EDGE_NONE
import androidx.navigationevent.NavigationEvent.SwipeEdge
import androidx.navigationevent.NavigationEventState.InProgress
import androidx.navigationevent.compose.NavigationEventHandler
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Object that indicates the features that can be handled by the [NavDisplay] */
public object NavDisplay {
    /**
     * Function to be called on the [NavEntry.metadata] to notify the [NavDisplay] that the content
     * should be animated using the provided [ContentTransform].
     */
    public fun transitionSpec(
        transitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform?
    ): Map<String, Any> = mapOf(TRANSITION_SPEC to transitionSpec)

    /**
     * Function to be called on the [NavEntry.metadata] to notify the [NavDisplay] that, when
     * popping from backstack, the content should be animated using the provided [ContentTransform].
     */
    public fun popTransitionSpec(
        popTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform?
    ): Map<String, Any> = mapOf(POP_TRANSITION_SPEC to popTransitionSpec)

    /**
     * Function to be called on the [NavEntry.metadata] to notify the [NavDisplay] that, when
     * popping from backstack using a Predictive back gesture, the content should be animated using
     * the provided [ContentTransform].
     */
    public fun predictivePopTransitionSpec(
        predictivePopTransitionSpec:
            AnimatedContentTransitionScope<*>.(@SwipeEdge Int) -> ContentTransform?
    ): Map<String, Any> = mapOf(PREDICTIVE_POP_TRANSITION_SPEC to predictivePopTransitionSpec)

    public val defaultPredictivePopTransitionSpec:
        AnimatedContentTransitionScope<*>.(@SwipeEdge Int) -> ContentTransform =
        {
            ContentTransform(
                fadeIn(
                    spring(
                        dampingRatio = 1.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                        stiffness = 1600.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                    )
                ),
                scaleOut(targetScale = 0.7f),
            )
        }

    internal const val TRANSITION_SPEC = "transitionSpec"
    internal const val POP_TRANSITION_SPEC = "popTransitionSpec"
    internal const val PREDICTIVE_POP_TRANSITION_SPEC = "predictivePopTransitionSpec"

    internal const val DEFAULT_TRANSITION_DURATION_MILLISECOND = 700
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
 * @param backStack the collection of keys that represents the state that needs to be handled
 * @param modifier the modifier to be applied to the layout.
 * @param contentAlignment The [Alignment] of the [AnimatedContent]
 * @param onBack a callback for handling system back press. The passed [Int] refers to the number of
 *   entries to pop from the end of the backstack, as calculated by the [sceneStrategy].
 * @param entryDecorators list of [NavEntryDecorator] to add information to the entry content
 * @param sceneStrategy the [SceneStrategy] to determine which scene to render a list of entries.
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
    onBack: (Int) -> Unit = {
        if (backStack is MutableList<T>) {
            repeat(it) { backStack.removeAt(backStack.lastIndex) }
        }
    },
    entryDecorators: List<NavEntryDecorator<*>> =
        listOf(rememberSceneSetupNavEntryDecorator(), rememberSavedStateNavEntryDecorator()),
    sceneStrategy: SceneStrategy<T> = SinglePaneSceneStrategy(),
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        ContentTransform(
            fadeIn(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
            fadeOut(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
        )
    },
    popTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        ContentTransform(
            fadeIn(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
            fadeOut(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
        )
    },
    predictivePopTransitionSpec:
        AnimatedContentTransitionScope<*>.(@SwipeEdge Int) -> ContentTransform =
        NavDisplay.defaultPredictivePopTransitionSpec,
    entryProvider: (key: T) -> NavEntry<T>,
) {
    require(backStack.isNotEmpty()) { "NavDisplay backstack cannot be empty" }

    var isSettled by remember { mutableStateOf(true) }
    val transitionAwareLifecycleNavEntryDecorator =
        transitionAwareLifecycleNavEntryDecorator(backStack, isSettled)

    DecoratedNavEntryProvider(
        backStack = backStack,
        entryDecorators = entryDecorators + transitionAwareLifecycleNavEntryDecorator,
        entryProvider = entryProvider,
    ) { entries ->
        val allScenes =
            mutableListOf(sceneStrategy.calculateSceneWithSinglePaneFallback(entries, onBack))
        do {
            val overlayScene = allScenes.last() as? OverlayScene
            val overlaidEntries = overlayScene?.overlaidEntries
            if (overlaidEntries != null) {
                // TODO Consider allowing a NavDisplay of only OverlayScene instances
                require(overlaidEntries.isNotEmpty()) {
                    "Overlaid entries from $overlayScene must not be empty"
                }
                allScenes +=
                    sceneStrategy.calculateSceneWithSinglePaneFallback(overlaidEntries, onBack)
            }
        } while (overlaidEntries != null)
        val overlayScenes = allScenes.dropLast(1)
        val scene =
            remember(backStack.map { it }, entryDecorators.map { it }, sceneStrategy, onBack) {
                allScenes.last()
            }

        // Predictive Back Handling
        val gestureState by
            checkNotNull(LocalNavigationEventDispatcherOwner.current) {
                    "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
                }
                .navigationEventDispatcher
                .state
                .collectAsState()

        val progress = gestureState.progress
        val inPredictiveBack = gestureState is InProgress
        val swipeEdge =
            when (val currentGestureState = gestureState) {
                is InProgress -> currentGestureState.latestEvent.swipeEdge
                else -> EDGE_NONE
            }

        NavigationEventHandler(enabled = scene.previousEntries.isNotEmpty()) { progress ->
            progress.collect()

            // If `enabled` becomes stale (e.g., it was set to false but a gesture was
            // dispatched in the same frame), this ensures that the calculated index is valid
            // before calling onBack, avoiding IndexOutOfBoundsException in edge cases.
            if (entries.size > scene.previousEntries.size) {
                onBack(entries.size - scene.previousEntries.size)
            }
        }

        // Scene Handling
        val sceneKey = scene::class to scene.key

        val scenes = remember { mutableStateMapOf<Pair<KClass<*>, Any>, Scene<T>>() }
        // TODO: This should really be a mutableOrderedStateSetOf
        val mostRecentSceneKeys = remember { mutableStateListOf<Pair<KClass<*>, Any>>() }
        scenes[sceneKey] = scene

        val transitionState = remember {
            // The state returned here cannot be nullable cause it produces the input of the
            // transitionSpec passed into the AnimatedContent and that must match the non-nullable
            // scope exposed by the transitions on the NavHost and composable APIs.
            SeekableTransitionState(sceneKey)
        }

        val transition = rememberTransition(transitionState, label = sceneKey.toString())

        LaunchedEffect(transition.targetState) {
            if (mostRecentSceneKeys.lastOrNull() != transition.targetState) {
                mostRecentSceneKeys.remove(transition.targetState)
                mostRecentSceneKeys.add(transition.targetState)
            }
        }
        // Determine which NavEntrys should be rendered within each scene.
        // Each renderable Scene, in order from the scene that is most recently the target scene to
        // the scene that is least recently the target scene will be assigned each visible
        // entry that hasn't already been assigned to a Scene that is more recent.
        val sceneToRenderableEntryMap =
            remember(
                mostRecentSceneKeys.toList(),
                scenes.values.map { scene -> scene.entries.map(NavEntry<T>::contentKey) },
                transition.targetState,
            ) {
                buildMap {
                    val coveredEntryKeys = mutableSetOf<Any>()
                    (mostRecentSceneKeys.filter { it != transition.targetState } +
                            listOf(transition.targetState))
                        .fastForEachReversed { sceneKey ->
                            val scene = scenes.getValue(sceneKey)
                            put(
                                sceneKey,
                                scene.entries
                                    .map { it.contentKey }
                                    .filterNot(coveredEntryKeys::contains)
                                    .toSet(),
                            )
                            scene.entries.forEach { coveredEntryKeys.add(it.contentKey) }
                        }
                }
            }

        // Transition Handling
        /** Keep track of the previous entries for the transition's current scene. */
        val transitionCurrentStateEntries = remember(transition.currentState) { entries.toList() }

        // Consider this a pop if the current entries match the previous entries we have recorded
        // from the current state of the transition
        val isPop =
            isPop(
                transitionCurrentStateEntries.map { it.contentKey },
                entries.map { it.contentKey },
            )

        val zIndices = remember { mutableObjectFloatMapOf<Pair<KClass<*>, Any>>() }
        val initialKey = transition.currentState
        val targetKey = transition.targetState
        val initialZIndex = zIndices.getOrPut(initialKey) { 0f }
        val targetZIndex =
            when {
                initialKey == targetKey -> initialZIndex
                isPop || inPredictiveBack -> initialZIndex - 1f
                else -> initialZIndex + 1f
            }
        zIndices[targetKey] = targetZIndex
        val transitionEntry =
            if (initialZIndex >= targetZIndex) {
                scenes[initialKey]!!.entries.last()
            } else {
                scenes[targetKey]!!.entries.last()
            }

        if (inPredictiveBack) {
            val peekScene =
                sceneStrategy.calculateSceneWithSinglePaneFallback(scene.previousEntries, onBack)
            val peekSceneKey = peekScene::class to peekScene.key
            scenes[peekSceneKey] = peekScene
            if (transitionState.currentState != peekSceneKey) {
                LaunchedEffect(progress) { transitionState.seekTo(progress, peekSceneKey) }
            }
        } else {
            LaunchedEffect(sceneKey) {
                if (transitionState.currentState != sceneKey) {
                    transitionState.animateTo(sceneKey)
                }
                // This ensures we don't animate after the back gesture is cancelled and we
                // are already on the current state
                if (transitionState.currentState != sceneKey) {
                    transitionState.animateTo(sceneKey)
                } else {
                    // convert from nanoseconds to milliseconds
                    val totalDuration = transition.totalDurationNanos / 1000000
                    // When the predictive back gesture is cancelled, we need to manually animate
                    // the SeekableTransitionState from where it left off, to zero and then
                    // snapTo the final position.
                    animate(
                        transitionState.fraction,
                        0f,
                        animationSpec = tween((transitionState.fraction * totalDuration).toInt()),
                    ) { value, _ ->
                        this@LaunchedEffect.launch {
                            if (value > 0) {
                                // Seek the original transition back to the currentState
                                transitionState.seekTo(value)
                            }
                            if (value == 0f) {
                                // Once we animate to the start, we need to snap to the right state.
                                transitionState.snapTo(sceneKey)
                            }
                        }
                    }
                }
            }
        }

        val contentTransform: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
            when {
                inPredictiveBack -> {
                    transitionEntry.predictivePopSpec()?.invoke(this, swipeEdge)
                        ?: predictivePopTransitionSpec(swipeEdge)
                }
                isPop -> {
                    transitionEntry.contentTransform(POP_TRANSITION_SPEC)?.invoke(this)
                        ?: popTransitionSpec(this)
                }
                else -> {
                    transitionEntry.contentTransform(TRANSITION_SPEC)?.invoke(this)
                        ?: transitionSpec(this)
                }
            }
        }

        transition.AnimatedContent(
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
        ) { targetSceneKey ->
            val targetScene = scenes.getValue(targetSceneKey)
            CompositionLocalProvider(
                LocalNavAnimatedContentScope provides this,
                LocalEntriesToRenderInCurrentScene provides
                    sceneToRenderableEntryMap.getValue(targetSceneKey),
            ) {
                targetScene.content()
            }
        }

        // Clean-up scene book-keeping once the transition is finished.
        LaunchedEffect(transition) {
            snapshotFlow { transition.isRunning }
                .filter { !it }
                .collect {
                    scenes.keys.toList().forEach { key ->
                        if (key != transition.targetState) {
                            scenes.remove(key)
                        }
                    }
                    mostRecentSceneKeys.toList().forEach { key ->
                        if (key != transition.targetState) {
                            mostRecentSceneKeys.remove(key)
                        }
                    }
                }
        }

        LaunchedEffect(transition.currentState, transition.targetState) {
            // If we've reached the targetState, our animation has settled
            val settled = transition.currentState == transition.targetState
            isSettled = settled
        }

        // Show all OverlayScene instances above the AnimatedContent
        overlayScenes.fastForEachReversed { overlayScene ->
            // TODO Calculate what entries should be displayed from sceneToRenderableEntryMap
            @Suppress("ListIterator")
            val allEntries = overlayScene.entries.map { it.contentKey }.toSet()
            CompositionLocalProvider(LocalEntriesToRenderInCurrentScene provides allEntries) {
                overlayScene.content.invoke()
            }
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
private fun <T : Any> NavEntry<T>.contentTransform(
    key: String
): (AnimatedContentTransitionScope<*>.() -> ContentTransform)? {
    return metadata[key] as? AnimatedContentTransitionScope<*>.() -> ContentTransform
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> NavEntry<T>.predictivePopSpec():
    (AnimatedContentTransitionScope<*>.(@SwipeEdge Int) -> ContentTransform)? {
    return metadata[PREDICTIVE_POP_TRANSITION_SPEC]
        as? AnimatedContentTransitionScope<*>.(@SwipeEdge Int) -> ContentTransform
}
