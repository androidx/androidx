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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.SwipeToDismissKeys
import androidx.wear.compose.material3.SwipeToDismissBox

/** A scene to display content for API <= 35 */
internal class SwipeToDismissScene<T : Any>(
    onBack: () -> Unit,
    val currentEntry: NavEntry<T>,
    background: NavEntry<T>?,
    // a list of entries that users can back into
    override val previousEntries: List<NavEntry<T>>,
    currentBackStack: List<NavEntry<T>>,
    backEnabled: Boolean,
) : Scene<T> {
    // A Unit scene key disables animations in NavDisplay, so that this scene
    // can internally handle animations for navigation forward and back
    override val key: Any = Unit

    // a list of entries to be displayed in this scene
    override val entries: List<NavEntry<T>> = listOf(currentEntry)

    // There could be a delay from when onDismissed is called to Nav3 actually updating the
    // current entry (because onDismissed only updates the backStack - the new scene with
    // updated entry happens later asynchronously). So we manually track whether
    // swipe is committed, and if so, make sure SwipeToDismissBox renders the background
    // (now foreground) entry instead of the entry that has already been swiped away.
    var onDismissedCalled by mutableStateOf(false)

    override val content: @Composable (() -> Unit) = {
        // Stores entire previous backstack
        val previousBackStack = rememberSaveable { mutableListOf<Any>() }

        // Determine whether the nav event was a navigate or pop, and then update the internal
        // previousStack to current stack
        val currentBackStackMapped = currentBackStack.map { it.contentKey }
        val isPop =
            if (previousBackStack.isNotEmpty()) {
                isPop(previousBackStack, currentBackStackMapped)
            } else false
        previousBackStack.clear()
        previousBackStack.addAll(currentBackStackMapped)

        val reduceMotionEnabled = LocalReduceMotion.current

        val animationProgress =
            remember(currentEntry) { if (!isPop) Animatable(0f) else Animatable(1f) }

        val isRoundDevice = isRoundDevice()

        SwipeToDismissBox(
            onDismissed = {
                onDismissedCalled = true
                onBack()
            },
            modifier = Modifier,
            backgroundKey = background?.contentKey ?: SwipeToDismissKeys.Background,
            userSwipeEnabled = backEnabled,
            contentKey = currentEntry.contentKey,
        ) { isBackground ->
            BoxedStackEntryContent(
                if (isBackground || onDismissedCalled) background else currentEntry,
                modifier =
                    if (isBackground) {
                        Modifier
                    } else {
                        // define transition for both popEnter and enter
                        Modifier.graphicsLayer {
                            val scaleProgression =
                                NAV_HOST_ENTER_TRANSITION_EASING_STANDARD.transform(
                                    (animationProgress.value / 0.75f)
                                )
                            val alphaProgression =
                                NAV_HOST_ENTER_TRANSITION_EASING_STANDARD.transform(
                                    (animationProgress.value / 0.25f)
                                )
                            val scale = lerp(0.75f, 1f, scaleProgression).coerceAtMost(1f)
                            val alpha = lerp(0.1f, 1f, alphaProgression).coerceIn(0f, 1f)
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            clip = true
                            shape = if (isRoundDevice) CircleShape else RectangleShape
                        }
                    },
                layerColor =
                    if (isBackground || isPop) {
                        Color.Unspecified
                    } else FLASH_COLOR,
                animatable = animationProgress,
            )
        }

        // runs transitions but only for navigates, not pops
        LaunchedEffect(currentEntry) {
            if (!isPop) {
                if (reduceMotionEnabled) {
                    animationProgress.snapTo(1f)
                } else {
                    animationProgress.animateTo(
                        targetValue = 1f,
                        animationSpec =
                            tween(
                                durationMillis =
                                    NAV_HOST_ENTER_TRANSITION_DURATION_MEDIUM +
                                        NAV_HOST_ENTER_TRANSITION_DURATION_SHORT,
                                easing = LinearEasing,
                            ),
                    )
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SwipeToDismissScene<*>

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
        return "SwipeToDismissScene(key=$key, entry=$currentEntry, previousEntries=$previousEntries, entries=$entries)"
    }
}

@Composable
private fun <T : Any> BoxedStackEntryContent(
    entry: NavEntry<T>?,
    modifier: Modifier = Modifier,
    layerColor: Color,
    animatable: Animatable<Float, AnimationVector1D>,
) {
    if (entry != null) {
        val isRoundDevice = isRoundDevice()
        Box(modifier, propagateMinConstraints = true) {
            entry.Content()
            // Adding a flash effect when a new screen opens
            if (layerColor != Color.Unspecified) {
                Canvas(Modifier.fillMaxSize()) {
                    val absoluteProgression = ((animatable.value - 0.25f) / 0.75f).coerceIn(0f, 1f)
                    val easedProgression =
                        NAV_HOST_ENTER_TRANSITION_EASING_STANDARD.transform(absoluteProgression)
                    val alpha = lerp(0.07f, 0f, easedProgression).coerceIn(0f, 1f)
                    if (isRoundDevice) {
                        drawCircle(color = layerColor.copy(alpha))
                    } else {
                        drawRect(color = layerColor.copy(alpha))
                    }
                }
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

private const val NAV_HOST_ENTER_TRANSITION_DURATION_SHORT = 100
private const val NAV_HOST_ENTER_TRANSITION_DURATION_MEDIUM = 300
private val NAV_HOST_ENTER_TRANSITION_EASING_STANDARD = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val FLASH_COLOR = Color.White
