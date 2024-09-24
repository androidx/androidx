/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.util.lerp
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ThreePaneScaffoldPredictiveBackHandler(
    navigator: ThreePaneScaffoldNavigator<Any>,
    backBehavior: BackNavigationBehavior,
    scale: Animatable<Float, AnimationVector1D>,
) {
    fun backProgressToAnimationProgress(value: Float): Float =
        PredictiveBackDefaults.Easing.transform(value) *
            when (navigator.scaffoldValue.expandedCount) {
                1 -> PredictiveBackDefaults.SinglePaneProgressRatio
                2 -> PredictiveBackDefaults.DualPaneProgressRatio
                else -> PredictiveBackDefaults.TriplePaneProgressRatio
            }
    fun backProgressToScale(value: Float): Float =
        lerp(1f, PredictiveBackDefaults.MinScale, PredictiveBackDefaults.Easing.transform(value))

    key(navigator, backBehavior) {
        PredictiveBackHandler(enabled = navigator.canNavigateBack(backBehavior)) { progress ->
            // code for gesture back started
            try {
                progress.collect { backEvent ->
                    scale.snapTo(backProgressToScale(backEvent.progress))
                    navigator.seekBack(
                        backBehavior,
                        fraction = backProgressToAnimationProgress(backEvent.progress),
                    )
                }
                // code for completion
                scale.animateTo(1f)
                navigator.navigateBack(backBehavior)
            } catch (e: CancellationException) {
                // code for cancellation
                scale.animateTo(1f)
                navigator.seekBack(backBehavior, fraction = 0f)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val ThreePaneScaffoldValue.expandedCount: Int
    get() {
        var count = 0
        if (primary == PaneAdaptedValue.Expanded) {
            count++
        }
        if (secondary == PaneAdaptedValue.Expanded) {
            count++
        }
        if (tertiary == PaneAdaptedValue.Expanded) {
            count++
        }
        return count
    }

private object PredictiveBackDefaults {
    val Easing: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
    const val MinScale: Float = 0.95f
    const val SinglePaneProgressRatio: Float = 0.1f
    const val DualPaneProgressRatio: Float = 0.15f
    const val TriplePaneProgressRatio: Float = 0.2f
}
