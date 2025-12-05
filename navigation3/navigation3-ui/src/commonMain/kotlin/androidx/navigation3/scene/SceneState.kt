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

package androidx.navigation3.scene

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.util.fastMap
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries

/**
 * Returns a [SceneState] that is remembered across compositions based on the parameters.
 *
 * This calculates all of the scenes and provides them in a [SceneState].
 *
 * @param entries all of the entries that are associated with this state
 * @param sceneStrategy the [SceneStrategy] to determine which scene to render a list of entries.
 * @param onBack a callback for handling system back press.
 * @sample androidx.navigation3.scene.samples.SceneStateSample
 */
@Deprecated(
    message = "Deprecated in favor of rememberSceneState that supports sharedTransitionScope",
    level = DeprecationLevel.HIDDEN,
)
@Composable
public fun <T : Any> rememberSceneState(
    entries: List<NavEntry<T>>,
    sceneStrategy: SceneStrategy<T>,
    onBack: () -> Unit,
): SceneState<T> {
    return rememberSceneState(
        entries = entries,
        sceneStrategy = sceneStrategy,
        sharedTransitionScope = null,
        onBack = onBack,
    )
}

/**
 * Returns a [SceneState] that is remembered across compositions based on the parameters.
 *
 * This calculates all of the scenes and provides them in a [SceneState].
 *
 * @param entries all of the entries that are associated with this state
 * @param sceneStrategy the [SceneStrategy] to determine which scene to render a list of entries.
 * @param sharedTransitionScope the [SharedTransitionScope] needed to wrap the scene decorator. If
 *   this parameter is added, this function will require the [LocalNavAnimatedContentScope].
 * @param onBack a callback for handling system back press.
 * @sample androidx.navigation3.scene.samples.SceneStateSample
 */
@Composable
public fun <T : Any> rememberSceneState(
    entries: List<NavEntry<T>>,
    sceneStrategy: SceneStrategy<T>,
    sharedTransitionScope: SharedTransitionScope? = null,
    onBack: () -> Unit,
): SceneState<T> {
    val currentOnBack by rememberUpdatedState(onBack)

    val sharedElementDecorator: SharedEntryInSceneNavEntryDecorator<T>? =
        sharedTransitionScope?.let { rememberSharedEntryInSceneNavEntryDecorator(it) }

    // Re-wrap the entries with:
    // - SharedEntryInSceneNavEntryDecorator to allow entries between scenes to be animated
    // - SceneSetupNavEntryDecorator to ensure all the ensures are inside of a moveable content
    // - BackStackAwareLifecycleNavEntryDecorator to ensure that the Lifecycle of entries that
    // are no longer on the back stack is capped at CREATED
    val decoratedEntries =
        rememberDecoratedNavEntries(
            entries,
            listOfNotNull(
                sharedElementDecorator,
                rememberSceneSetupNavEntryDecorator(),
                rememberBackStackAwareLifecycleNavEntryDecorator(entries),
            ),
        )

    return remember(sceneStrategy, decoratedEntries) {
        val scope =
            SceneStrategyScope<T>(
                // `currentOnBack` invokes the *latest* `onBack` lambda. The outer
                // `remember` block intentionally skips `onBack` as a key to avoid
                // recalculating all scenes when just the `onBack` instance changes.
                onBack = @Suppress("UnnecessaryLambdaCreation") { currentOnBack() }
            )

        // Calculate the single scene based on the sceneStrategy and start the list there.
        val allScenes =
            mutableListOf(
                sceneStrategy.calculateSceneWithSinglePaneFallback(scope, decoratedEntries)
            )

        // find all of the OverlayScenes
        do {
            // Starts from previously calculated scene and check if it is an OverlayScene
            val overlayScene = allScenes.last() as? OverlayScene
            val overlaidEntries = overlayScene?.overlaidEntries
            if (overlaidEntries != null) {
                // TODO Consider allowing a NavDisplay of only OverlayScene instances
                require(overlaidEntries.isNotEmpty()) {
                    "Overlaid entries from $overlayScene must not be empty"
                }
                // Keep added scenes to the end of our list until we find a non-overlay scene
                allScenes +=
                    sceneStrategy.calculateSceneWithSinglePaneFallback(scope, overlaidEntries)
            }
        } while (overlaidEntries != null)

        // Find all the overlay scenes
        val overlayScenes = allScenes.dropLast(1).fastMap { it as OverlayScene<T> }
        // The currentScene is just just whatever is last on the list.
        val currentScene = allScenes.last()
        // Get the previous scenes, starting from the current scene.
        val previousScenes = mutableListOf(allScenes.first())

        do {
            // get the first scene off the list
            val previousScene = previousScenes.firstOrNull()
            val previousEntries = previousScene?.previousEntries
            if (!previousEntries.isNullOrEmpty()) {
                // If there are previous entries, add the scene from those entries to the front of
                // the list
                previousScenes.add(
                    index = 0,
                    sceneStrategy.calculateSceneWithSinglePaneFallback(scope, previousEntries),
                )
            }
        } while (!previousEntries.isNullOrEmpty())

        // remove the currentScene from the list
        previousScenes.remove(currentScene)

        SceneState(decoratedEntries, overlayScenes, currentScene, previousScenes)
    }
}

/**
 * Class for holding the state associated with a scene
 *
 * This provides information to the androidx.navigation3.ui.NavDisplay and
 * androidx.navigationevent.compose.NavigationBackHandler to ensure they can behave correctly.
 *
 * @param entries all of the entries that are associated with this state
 * @param overlayScenes any overlay scenes available to the state
 * @param currentScene the current scene that could be displayed
 * @param previousScenes the list of all of the previous scenes before the currentScene
 */
@Immutable
public class SceneState<T : Any>
internal constructor(
    public val entries: List<NavEntry<T>>,
    public val overlayScenes: List<OverlayScene<T>>,
    public val currentScene: Scene<T>,
    public val previousScenes: List<Scene<T>>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SceneState<*>

        return entries == other.entries &&
            overlayScenes == other.overlayScenes &&
            currentScene == other.currentScene &&
            previousScenes == other.previousScenes
    }

    override fun hashCode(): Int {
        return entries.hashCode() * 31 +
            overlayScenes.hashCode() * 31 +
            currentScene.hashCode() * 31 +
            previousScenes.hashCode() * 31
    }

    override fun toString(): String {
        return "SceneState(entries=$entries, overlayScenes=$overlayScenes, currentScene=$currentScene, previousScenes=$previousScenes)"
    }
}
