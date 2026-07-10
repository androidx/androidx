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
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.wear.compose.foundation.LocalScreenIsActive
import androidx.wear.compose.foundation.LocalSwipeToDismissBackgroundScrimColor
import androidx.wear.compose.foundation.hierarchicalFocusGroup

/**
 * A [Scene] that displays content for Wear devices running API 36+
 *
 * Supports Predictive Back by delegating to the [NavigationBackHandler] in [NavDisplay]. However,
 * it overrides that [NavigationBackHandler] with an internal no-op [NavigationBackHandler] if
 * [backEnabled] is false.
 *
 * During a swipe-to-dismiss gesture, the previous screen (if any) is shown in the background behind
 * a scrim given by [LocalSwipeToDismissBackgroundScrimColor].
 *
 * @param [currentEntry] The current [NavEntry] displayed on the screen
 * @param [previousEntries] The [NavEntry]s on the backStack before the [currentEntry]
 * @param [backEnabled] Whether predictive back is enabled
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal class PredictiveBackScene<T : Any>(
    val currentEntry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    backEnabled: Boolean,
) : Scene<T> {
    override val key: Any = currentEntry.contentKey

    override val entries: List<NavEntry<T>> = listOf(currentEntry)

    // Provide Wear-specific transitions to NavDisplay. Also includes current entry's
    // transitions (if any) to allow users to override Wear's defaults.
    override val metadata: Map<String, Any> =
        NavDisplay.transitionSpec { ENTER_TRANSITION togetherWith EXIT_TRANSITION } +
            NavDisplay.popTransitionSpec { POP_ENTER_TRANSITION togetherWith POP_EXIT_TRANSITION } +
            NavDisplay.predictivePopTransitionSpec {
                PREDICTIVE_BACK_ENTER_TRANSITION togetherWith PREDICTIVE_BACK_EXIT_TRANSITION
            } +
            currentEntry.metadata

    override val content: @Composable (() -> Unit) = {
        NavigationBackHandler(
            state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
            // This no-op back handler is active only when back is disabled, so that it will
            // override NavDisplay's back handler with a no-op onBackCompleted. Otherwise,
            // back behavior will be delegated back to NavDisplay's default back handler
            isBackEnabled = !backEnabled,
            // override by doing nothing on back
            onBackCompleted = {},
        )

        val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateAsState().value
        val shouldFocus = remember(lifecycleState) { lifecycleState == Lifecycle.State.RESUMED }

        val scrimColor = LocalSwipeToDismissBackgroundScrimColor.current
        val isRoundDevice = isRoundDevice()

        val parentScreenActive = LocalScreenIsActive.current

        CompositionLocalProvider(LocalScreenIsActive provides (shouldFocus && parentScreenActive)) {
            Box(
                modifier =
                    Modifier.clip(if (isRoundDevice) CircleShape else RectangleShape)
                        .background(scrimColor)
                        .fillMaxSize()
                        .hierarchicalFocusGroup(shouldFocus)
            ) {
                currentEntry.Content()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PredictiveBackScene<*>

        return key == other.key &&
            currentEntry == other.currentEntry &&
            previousEntries == other.previousEntries &&
            entries == other.entries
    }

    override fun hashCode(): Int {
        return key.hashCode() * 31 +
            currentEntry.hashCode() * 31 +
            previousEntries.hashCode() * 31 +
            entries.hashCode() * 31
    }

    override fun toString(): String {
        return "PredictiveBackScene(key=$key, entry=$currentEntry, previousEntries=$previousEntries, entries=$entries)"
    }
}

private val ENTER_TRANSITION =
    slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = spring(0.8f, 300f)) +
        scaleIn(initialScale = 0.8f, animationSpec = spring(1f, 500f)) +
        fadeIn(animationSpec = spring(1f, 1500f))
private val EXIT_TRANSITION =
    scaleOut(targetScale = 0.85f, animationSpec = spring(1f, 150f)) +
        slideOutHorizontally(targetOffsetX = { -it / 2 }, animationSpec = spring(0.8f, 200f)) +
        fadeOut(targetAlpha = 0.6f, animationSpec = spring(1f, 1400f))
private val POP_ENTER_TRANSITION =
    scaleIn(initialScale = 0.8f, animationSpec = tween(easing = LinearEasing)) +
        slideInHorizontally(
            initialOffsetX = { -it / 2 },
            animationSpec = tween(easing = LinearEasing),
        ) +
        fadeIn(initialAlpha = 0.5f, animationSpec = tween(easing = LinearEasing))
private val POP_EXIT_TRANSITION =
    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(easing = LinearEasing)) +
        scaleOut(targetScale = 0.8f, animationSpec = tween(easing = LinearEasing))

// same as popEnter / popExit transitions
private val PREDICTIVE_BACK_ENTER_TRANSITION = POP_ENTER_TRANSITION
private val PREDICTIVE_BACK_EXIT_TRANSITION = POP_EXIT_TRANSITION
