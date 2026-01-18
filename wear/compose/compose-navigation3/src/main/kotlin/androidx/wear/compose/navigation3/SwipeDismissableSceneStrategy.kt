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

package androidx.wear.compose.navigation3

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.wear.compose.foundation.BasicSwipeToDismissBox
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState

/**
 * Remembers and returns a [SwipeDismissableSceneStrategyState]
 *
 * @param swipeToDismissBoxState State for [BasicSwipeToDismissBox], which is used to support the
 *   swipe-to-dismiss gesture in [SwipeDismissableSceneStrategy] for devices running API 35 or
 *   under. Defaults to null for devices running API 36+.
 */
@Composable
public fun rememberSwipeDismissableSceneStrategyState(
    swipeToDismissBoxState: SwipeToDismissBoxState? =
        if (Build.VERSION.SDK_INT <= 35) rememberSwipeToDismissBoxState() else null
): SwipeDismissableSceneStrategyState =
    remember(swipeToDismissBoxState) { SwipeDismissableSceneStrategyState(swipeToDismissBoxState) }

/**
 * Creates and remembers a [SwipeDismissableSceneStrategy].
 *
 * Example of a [androidx.navigation3.ui.NavDisplay] with [SwipeDismissableSceneStrategy]
 * alternating between list and detail entries:
 *
 * @sample androidx.wear.compose.navigation3.samples.ListDetailNavDisplaySample
 * @param [T] the type of the backstack key
 * @param swipeDismissableSceneStrategyState State containing information about ongoing swipe and
 *   animation. This parameter is unused API level 36 onwards, because the platform supports
 *   predictive back and [SwipeDismissableSceneStrategy] uses platform gestures to detect the back
 *   gestures.
 * @param modifier The modifier to be applied to the layout
 * @param isUserSwipeEnabled [Boolean] Whether swipe-to-dismiss gesture is enabled.
 */
@Composable
public fun <T : Any> rememberSwipeDismissableSceneStrategy(
    swipeDismissableSceneStrategyState: SwipeDismissableSceneStrategyState =
        rememberSwipeDismissableSceneStrategyState(),
    modifier: Modifier = Modifier,
    isUserSwipeEnabled: Boolean = true,
): SwipeDismissableSceneStrategy<T> =
    remember(swipeDismissableSceneStrategyState) {
        SwipeDismissableSceneStrategy(
            swipeDismissableSceneStrategyState,
            modifier,
            isUserSwipeEnabled,
        )
    }

/**
 * State for [SwipeDismissableSceneStrategy]
 *
 * @param swipeToDismissBoxState State for [BasicSwipeToDismissBox], which is used to support the
 *   swipe-to-dismiss gesture in [SwipeDismissableSceneStrategy] for devices running API 35 or
 *   under.
 */
public class SwipeDismissableSceneStrategyState(
    internal val swipeToDismissBoxState: SwipeToDismissBoxState?
)

/**
 * A [SceneStrategy] that displays entries within a Wear Material component.
 *
 * Below API level 36, content of the current entry (the last entry on the backstack) is displayed
 * within a [BasicSwipeToDismissBox] to detect swipe back gestures.
 *
 * API level 36 onwards, [SwipeDismissableSceneStrategy] listens to platform predictive back events
 * for navigation, and [BasicSwipeToDismissBox] is not used for swipe gesture detection. Also,
 * transition specifications can be overridden at [NavEntry] level.
 *
 * Example of a [androidx.navigation3.ui.NavDisplay] with [SwipeDismissableSceneStrategy]
 * alternating between list and detail entries:
 *
 * @sample androidx.wear.compose.navigation3.samples.ListDetailNavDisplaySample
 * @param [T] the type of the backstack key
 * @param state State containing information about ongoing swipe and animation. This parameter is
 *   unused API level 36 onwards, because the platform supports predictive back and
 *   [SwipeDismissableSceneStrategy] uses platform gestures to detect the back gestures.
 * @param modifier The modifier to be applied to the layout
 * @param isUserSwipeEnabled [Boolean] Whether swipe-to-dismiss gesture is enabled.
 */
public class SwipeDismissableSceneStrategy<T : Any>(
    public val state: SwipeDismissableSceneStrategyState,
    public val modifier: Modifier = Modifier,
    public val isUserSwipeEnabled: Boolean = true,
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.isEmpty()) return null

        val currentEntry = entries.last()
        val previousEntries = entries.dropLast(1)
        val background = previousEntries.lastOrNull()

        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // api 36+, support predictive back
            PredictiveBackScene(
                modifier = modifier,
                currentEntry = currentEntry,
                previousEntries = previousEntries,
                backEnabled = isUserSwipeEnabled,
            )
        } else {
            val swipeToDismissBoxState = state.swipeToDismissBoxState
            requireNotNull(swipeToDismissBoxState) { "SwipeToDismissBoxState cannot be null." }

            // api < 35, delegates to BasicSwipeToDismissBox
            return SwipeToDismissScene(
                onBack = onBack,
                modifier = modifier,
                currentEntry = currentEntry,
                background = background,
                currentBackStack = entries,
                previousEntries = previousEntries,
                swipeToDismissBoxState = swipeToDismissBoxState,
                backEnabled = isUserSwipeEnabled && background != null,
            )
        }
    }
}

@Composable
internal fun isRoundDevice(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.isScreenRound }
}
