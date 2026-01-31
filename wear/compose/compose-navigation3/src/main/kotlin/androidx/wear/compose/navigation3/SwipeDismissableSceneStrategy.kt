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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.wear.compose.foundation.BasicSwipeToDismissBox

/**
 * Creates and remembers a [SwipeDismissableSceneStrategy].
 *
 * Example of a [androidx.navigation3.ui.NavDisplay] with [SwipeDismissableSceneStrategy]
 * alternating between list and detail entries:
 *
 * @sample androidx.wear.compose.navigation3.samples.ListDetailNavDisplaySample
 *
 * Example of a [androidx.navigation3.ui.NavDisplay] with [SwipeDismissableSceneStrategy] and on
 * back behavior:
 *
 * @sample androidx.wear.compose.navigation3.samples.NavDisplayWithOnBackBehaviorSample
 * @param [T] the type of the backstack key
 * @param isUserSwipeEnabled [Boolean] Whether swipe-to-dismiss gesture is enabled.
 */
@Composable
public fun <T : Any> rememberSwipeDismissableSceneStrategy(
    isUserSwipeEnabled: Boolean = true
): SwipeDismissableSceneStrategy<T> =
    remember(isUserSwipeEnabled) { SwipeDismissableSceneStrategy(isUserSwipeEnabled) }

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
 * @param [T] the type of the backstack keys
 * @param isUserSwipeEnabled [Boolean] Whether swipe-to-dismiss gesture is enabled.
 */
public class SwipeDismissableSceneStrategy<T : Any>(public val isUserSwipeEnabled: Boolean = true) :
    SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.isEmpty()) return null

        val currentEntry = entries.last()
        val previousEntries = entries.dropLast(1)
        val background = previousEntries.lastOrNull()

        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // api 36+, support predictive back
            PredictiveBackScene(
                currentEntry = currentEntry,
                previousEntries = previousEntries,
                backEnabled = isUserSwipeEnabled,
            )
        } else {
            // api < 35, delegates to BasicSwipeToDismissBox
            return SwipeToDismissScene(
                onBack = onBack,
                currentEntry = currentEntry,
                background = background,
                currentBackStack = entries,
                previousEntries = previousEntries,
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
