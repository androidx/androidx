/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.compose.animation.scene.demo.transitions

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.Edge
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneTransitionsBuilder
import com.android.compose.animation.scene.TransitionBuilder
import com.android.compose.animation.scene.UserActionDistance
import com.android.compose.animation.scene.UserActionDistanceScope
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.compose.animation.scene.demo.MediaPlayer
import com.android.compose.animation.scene.demo.QuickSettings
import com.android.compose.animation.scene.demo.QuickSettingsGrid
import com.android.compose.animation.scene.demo.Scenes
import com.android.compose.animation.scene.demo.Shade
import com.android.compose.animation.scene.demo.notification.NotificationList
import com.android.compose.animation.scene.inScene
import com.android.compose.animation.scene.transformation.InterpolatedPropertyTransformation
import com.android.compose.animation.scene.transformation.PropertyTransformation
import com.android.compose.animation.scene.transformation.PropertyTransformationScope

fun SceneTransitionsBuilder.shadeTransitions(qsPagerState: PagerState) {
    // The distance when swiping the Shade from/to a scene (except QuickSettings).
    val swipeDistance =
        object : UserActionDistance {
            override fun UserActionDistanceScope.absoluteDistance(
                fromContent: ContentKey,
                toContent: ContentKey,
                orientation: Orientation,
            ): Float {
                val distance =
                    (Shade.Elements.Scrim.targetOffset(Scenes.Shade)?.y ?: return 0f).coerceAtLeast(
                        100.dp.toPx()
                    )

                // Use the bottom of the QS grid for the minimum distance, so that the distance is
                // not too small if the scrim was scrolled at the top because it has a lot of
                // notifications.
                val qsGridOffset =
                    Shade.Elements.CollapsedGrid.targetOffset(Scenes.Shade) ?: return 0f
                val qsGridSize = Shade.Elements.CollapsedGrid.targetSize(Scenes.Shade) ?: return 0f
                val minDistance = qsGridOffset.y + qsGridSize.height
                check(minDistance > 0f) {
                    "Invalid QS offset and size in Shade (qsGridOffset=$qsGridOffset " +
                        "qsGridSize=$qsGridSize)"
                }

                // Note: we might want to have a minimum swipe distance here if the scrim was
                // scrolled at the top of the shade scene because it contains a lot of
                // notifications, or introduce a new element that is always at the initial scrim
                // position and use that to compute the distance.
                return maxOf(distance, minDistance)
            }
        }
    to(Scenes.Shade) {
        spec = tween(durationMillis = 500)
        distance = swipeDistance

        toShadeTransformations()
    }

    from(Scenes.Shade) {
        spec = tween(durationMillis = 500)
        distance = swipeDistance

        // Same transition as when going *to* the shade, except that we never share notifications.
        reversed { toShadeTransformations() }
        sharedElement(NotificationList.Elements.Notifications, enabled = false)
    }

    // The distance when swiping the Shade from/to QuickSettings.
    val qsSwipeDistance =
        object : UserActionDistance {
            override fun UserActionDistanceScope.absoluteDistance(
                fromContent: ContentKey,
                toContent: ContentKey,
                orientation: Orientation,
            ): Float {
                val scrimOffsetInShade =
                    Shade.Elements.Scrim.targetOffset(Scenes.Shade) ?: return 0f
                val shadeSceneSize = Scenes.Shade.targetSize() ?: return 0f
                val distance = shadeSceneSize.height - scrimOffsetInShade.y
                check(distance > 0f) {
                    "Invalid QS swipe distance (scrimOffsetInShade=$scrimOffsetInShade " +
                        "shadeSceneSize=$shadeSceneSize)"
                }
                return distance
            }
        }

    from(Scenes.QuickSettings, to = Scenes.Shade) {
        spec = tween(durationMillis = 500)
        distance = qsSwipeDistance

        sharedElement(
            QuickSettingsGrid.Elements.Tiles,

            // Disable the shared tiles animation if the pager is not on page 0 or is animating.
            enabled = qsPagerState.currentPage == 0 && !qsPagerState.isScrollInProgress,
        )

        anchoredTranslate(QuickSettings.Elements.Date, Shade.Elements.BatteryPercentage)
        anchoredTranslate(QuickSettings.Elements.Operator, Shade.Elements.BatteryPercentage)
        anchoredTranslate(Shade.Elements.Date, Shade.Elements.Time)
        anchoredTranslate(
            QuickSettings.Elements.BrightnessSlider,
            QuickSettingsGrid.Elements.GridAnchor,
        )
        anchoredTranslate(
            QuickSettings.Elements.ExpandedGrid,
            QuickSettingsGrid.Elements.GridAnchor,
        )
        anchoredTranslate(Shade.Elements.CollapsedGrid, QuickSettingsGrid.Elements.GridAnchor)
        anchoredSize(
            QuickSettingsGrid.Elements.Tiles,
            QuickSettingsGrid.Elements.GridAnchor,
            anchorWidth = false,
        )

        translate(Shade.Elements.Scrim, Edge.Bottom)

        // The tiles in the QS appear when going from Shade => QS, and they do so throughout the
        // whole transition.
        @Suppress("DEPRECATION") // This code is copied from SysUi and should not diverge
        fade(QuickSettingsGrid.Elements.Tiles.inScene(Scenes.QuickSettings))

        // The tiles in the Shade are either shared (and therefore the transformations are ignored)
        // or they are not because the QS pager is not on the first page. Those tiles should appear
        // during the second half of the transition from QS => Shade.
        @Suppress("DEPRECATION") // This code is copied from SysUi and should not diverge
        fractionRange(start = 0.5f, end = QuickSettings.TransitionToShadeCommittedProgress) {
            fade(QuickSettingsGrid.Elements.Tiles.inScene(Scenes.Shade))
        }

        timestampRange(endMillis = 83) {
            fade(QuickSettings.Elements.FooterActions)
            fade(QuickSettings.Elements.PagerIndicators)
        }
        timestampRange(endMillis = 150) {
            fade(QuickSettings.Elements.BrightnessSlider)
            fade(QuickSettings.Elements.Operator)
            fade(QuickSettings.Elements.Date)
        }
        timestampRange(startMillis = 350) { fade(Shade.Elements.Date) }
    }
}

/** The progress until which the shade scrim should be completely opaque when going to the shade. */
@Suppress("MayBeConstant") // This code is copied from SysUi and should not diverge
val ToShadeScrimFadeEndFraction = 0.5f

private fun TransitionBuilder.toShadeTransformations() {
    translate(Shade.Elements.Scrim, Edge.Top, startsOutsideLayoutBounds = false)

    // Extend the scrim to content height during transition to prevent a visual gap at the bottom.
    // Its target height is shorter, because it is its size at rest, where the underScrim is fully
    // visible.
    transformation(Shade.Elements.Scrim) {
        object : InterpolatedPropertyTransformation<IntSize> {
            override fun PropertyTransformationScope.transform(
                content: ContentKey,
                element: ElementKey,
                transition: TransitionState.Transition,
                idleValue: IntSize,
            ): IntSize {
                return content.targetSize()
                    ?: error("Content ${content.debugName} does not have a target size")
            }

            override val property: PropertyTransformation.Property<IntSize>
                get() = PropertyTransformation.Property.Size
        }
    }

    scaleSize(QuickSettingsGrid.Elements.Tiles, height = 0.5f)

    // Never share the media player when going to the shade. We will compose it in the lockscreen
    // or in the shade depending on the transition progress.
    sharedElement(MediaPlayer.Elements.MediaPlayer, enabled = false)

    sharedElement(Shade.Elements.BatteryPercentage, enabled = false)

    fractionRange(end = ToShadeScrimFadeEndFraction) {
        fade(Shade.Elements.ScrimBackground)
        translate(Shade.Elements.CollapsedGrid, Edge.Top, startsOutsideLayoutBounds = false)
    }

    fractionRange(start = ToShadeScrimFadeEndFraction) {
        fade(Shade.Elements.Date)
        fade(Shade.Elements.Time)
        fade(Shade.Elements.BatteryPercentage)
        @Suppress("DEPRECATION") // This code is copied from SysUi and should not diverge
        fade(NotificationList.Elements.Notifications.inScene(Scenes.Shade))
    }

    @Suppress("DEPRECATION") // This code is copied from SysUi and should not diverge
    fractionRange(start = 0.8f) { fade(MediaPlayer.Elements.MediaPlayer.inScene(Scenes.Shade)) }
}
