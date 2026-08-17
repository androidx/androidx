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

import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Contains default values and components used by the Material 3 A2UI catalog. */
internal object MaterialA2uiDefaults {

    /** Test tag for [LoadingIndicator]. */
    internal const val LOADING_INDICATOR_TEST_TAG = "LoadingIndicator"

    /**
     * Displays an indicator while content is loading.
     *
     * @param modifier [Modifier] to apply to the indicator
     */
    @Composable
    fun LoadingIndicator(modifier: Modifier = Modifier) {
        // TODO(b/546038727): Replace the static loading indicator with an animated shimmer effect
        Box(
            modifier =
                modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .testTag(LOADING_INDICATOR_TEST_TAG)
        )
    }

    /**
     * Displays an error message inside a [Surface].
     *
     * @param exception [A2uiException] that caused the render failure
     */
    @Composable
    fun ErrorFallback(exception: A2uiException) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(text = stringResource(R.string.error), modifier = Modifier.padding(12.dp))
        }
    }

    /** Transition animation between loading, success, and error states for A2UI components. */
    val transitionSpec: AnimatedContentTransitionScope<A2uiComponentState>.() -> ContentTransform
        @Composable
        get() {
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
