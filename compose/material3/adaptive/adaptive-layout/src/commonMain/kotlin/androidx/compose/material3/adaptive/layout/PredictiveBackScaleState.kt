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

package androidx.compose.material3.adaptive.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.internal.getValue
import androidx.compose.material3.adaptive.layout.internal.rememberRef
import androidx.compose.material3.adaptive.layout.internal.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset

internal fun Modifier.predictiveBackScale(state: PredictiveBackScaleState): Modifier =
    graphicsLayer {
        val scaleValue = state.scale
        scaleX = scaleValue
        scaleY = scaleValue
        transformOrigin = state.transformOrigin
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class PredictiveBackScaleState(
    private val scaffoldSize: () -> IntSize,
    private val isPredictiveBackInProgress: () -> Boolean,
) {
    val scaleAnimatable = Animatable(initialValue = 1f)
    val scale: Float
        get() = scaleAnimatable.value

    private val transformMatrix = Matrix()

    val transformOrigin = TransformOrigin.Center

    fun convert(offset: IntOffset): IntOffset =
        if (isPredictiveBackInProgress()) {
            transformMatrix
                .apply {
                    val scaffoldSize = scaffoldSize()
                    resetToPivotedTransform(
                        pivotX = scaffoldSize.width.toFloat() * transformOrigin.pivotFractionX,
                        pivotY = scaffoldSize.height.toFloat() * transformOrigin.pivotFractionY,
                        scaleX = scaleAnimatable.value,
                        scaleY = scaleAnimatable.value,
                    )
                }
                .map(offset.toOffset())
                .round()
        } else {
            offset
        }

    companion object {
        const val PredictiveBackMinScale: Float = 0.95f
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ThreePaneScaffoldState.CollectPredictiveBackScale(
    predictiveBackScaleState: PredictiveBackScaleState
) {
    var previousValue by rememberRef(0f)
    LaunchedEffect(this) {
        snapshotFlow { progressFraction }
            .collect { value ->
                if (value == previousValue) return@collect
                previousValue = value
                if (isPredictiveBackInProgress) {
                    val scale = convertStateProgressToPredictiveBackScale(value)
                    predictiveBackScaleState.scaleAnimatable.snapTo(scale)
                } else {
                    predictiveBackScaleState.scaleAnimatable.animateTo(1f)
                }
            }
    }
}

private fun convertStateProgressToPredictiveBackScale(fraction: Float): Float {
    // A decay curve such that: When fraction = 0, function returns 1.
    // When fraction -> 1, function asymptotically approaches PredictiveBackMinScale
    val delta = 1f - PredictiveBackScaleState.PredictiveBackMinScale
    val shift = delta / 2
    val curveScale = delta * delta / 2
    return curveScale / (fraction + shift) + PredictiveBackScaleState.PredictiveBackMinScale
}
