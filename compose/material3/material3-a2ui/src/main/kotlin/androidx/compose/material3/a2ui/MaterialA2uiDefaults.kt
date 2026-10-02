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

package androidx.compose.material3.a2ui

import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Contains default values and components used by the Material 3 A2UI catalog. */
internal object MaterialA2uiDefaults {

    /** Test tag for [LoadingIndicator]. */
    internal const val LOADING_INDICATOR_TEST_TAG = "LoadingIndicator"

    /**
     * Coordinates a shared radial shimmer sweep and breathing pulse across multiple child
     * [LoadingIndicator] instances within a single component container (such as a Row, Column, or
     * List).
     *
     * The highlight ring originates from the top-left corner of the container measured via the
     * [Modifier] passed to [content], so each child [LoadingIndicator] renders a slice of the same
     * expanding ring based on its relative position inside the container.
     */
    @Composable
    fun LoadingIndicatorGroup(content: @Composable (Modifier) -> Unit) {
        val coroutineScope = rememberCoroutineScope()
        val groupState = remember(coroutineScope) { ShimmerIndicatorGroupState(coroutineScope) }
        val rootModifier =
            remember(groupState) {
                Modifier.onPlaced { groupState.rootCoordinates = it }
            }
        CompositionLocalProvider(LocalShimmerIndicatorGroupState provides groupState) {
            content(rootModifier)
        }
    }

    /**
     * Displays an indicator while content is loading.
     *
     * When placed inside a [LoadingIndicatorGroup], this indicator shares the group's expanding
     * radial highlight ring (offset by its own position within the group container) and staggers
     * its breathing pulse by [index].
     *
     * @param modifier [Modifier] to apply to the indicator
     * @param index 0-based index of this indicator within its group, used to stagger the breathing
     *   opacity pulse
     */
    @Composable
    fun LoadingIndicator(modifier: Modifier = Modifier, index: Int = 0) {
        val groupState = LocalShimmerIndicatorGroupState.current
        val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
        val shape = MaterialTheme.shapes.medium

        Box(
            modifier =
                modifier
                    .shimmerIndicator(
                        isLight = isLight,
                        shape = shape,
                        index = index,
                        groupState = groupState,
                    )
                    .testTag(LOADING_INDICATOR_TEST_TAG)
        )
    }

    /**
     * Displays an error message inside a [Surface].
     *
     * @param exception [A2uiException] that caused the render failure
     * @param modifier [Modifier] to apply to the error fallback
     */
    @Composable
    fun ErrorFallback(exception: A2uiException, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(text = stringResource(R.string.error), modifier = Modifier.padding(12.dp))
        }
    }

    /** Transition animation between loading, success, and error states for A2UI components. */
    @Composable
    fun <S> transitionSpec(): AnimatedContentTransitionScope<S>.() -> ContentTransform {
        // Effects specs are used for non-spatial opacity/color changes
        val defaultEffectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val fastEffectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

        // Spatial specs are used for bounds/size/position changes
        val defaultSpatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()

        return remember(defaultEffectsSpec, fastEffectsSpec, defaultSpatialSpec) {
            {
                (fadeIn(animationSpec = defaultEffectsSpec) togetherWith
                        fadeOut(animationSpec = fastEffectsSpec))
                    .using(
                        SizeTransform(
                            clip = true,
                            sizeAnimationSpec = { _, _ -> defaultSpatialSpec },
                        )
                    )
            }
        }
    }
}
