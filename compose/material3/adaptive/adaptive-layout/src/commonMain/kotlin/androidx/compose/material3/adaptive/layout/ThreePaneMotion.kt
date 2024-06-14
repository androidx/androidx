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

package androidx.compose.material3.adaptive.layout

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEachIndexed

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("PrimitiveInCollection") // No way to get underlying Long of IntSize or IntOffset
internal class ThreePaneScaffoldMotionScopeImpl(
    override val positionAnimationSpec: FiniteAnimationSpec<IntOffset> =
        ThreePaneMotionDefaults.PanePositionAnimationSpec,
    override val sizeAnimationSpec: FiniteAnimationSpec<IntSize> =
        ThreePaneMotionDefaults.PaneSizeAnimationSpec,
    override val delayedPositionAnimationSpec: FiniteAnimationSpec<IntOffset> =
        ThreePaneMotionDefaults.PanePositionAnimationSpecDelayed,
) : PaneScaffoldMotionScope {
    override var scaffoldSize: IntSize = IntSize.Zero
    override val paneMotionDataList: List<PaneMotionData> =
        listOf(PaneMotionData(), PaneMotionData(), PaneMotionData())

    internal fun updatePaneMotions(paneMotions: List<PaneMotion>) {
        paneMotionDataList.fastForEachIndexed { index, it -> it.motion = paneMotions[index] }
    }
}

internal class DelayedSpringSpec<T>(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    private val delayedRatio: Float,
    visibilityThreshold: T? = null
) : FiniteAnimationSpec<T> {
    private val originalSpringSpec = spring(dampingRatio, stiffness, visibilityThreshold)

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V> =
        DelayedVectorizedSpringSpec(originalSpringSpec.vectorize(converter), delayedRatio)
}

private class DelayedVectorizedSpringSpec<V : AnimationVector>(
    val originalVectorizedSpringSpec: VectorizedFiniteAnimationSpec<V>,
    val delayedRatio: Float,
) : VectorizedFiniteAnimationSpec<V> {
    var delayedTimeNanos: Long = 0
    var cachedInitialValue: V? = null
    var cachedTargetValue: V? = null
    var cachedInitialVelocity: V? = null
    var cachedOriginalDurationNanos: Long = 0

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        updateDelayedTimeNanosIfNeeded(initialValue, targetValue, initialVelocity)
        return if (playTimeNanos <= delayedTimeNanos) {
            initialValue
        } else {
            originalVectorizedSpringSpec.getValueFromNanos(
                playTimeNanos - delayedTimeNanos,
                initialValue,
                targetValue,
                initialVelocity
            )
        }
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        updateDelayedTimeNanosIfNeeded(initialValue, targetValue, initialVelocity)
        return if (playTimeNanos <= delayedTimeNanos) {
            initialVelocity
        } else {
            originalVectorizedSpringSpec.getVelocityFromNanos(
                playTimeNanos - delayedTimeNanos,
                initialValue,
                targetValue,
                initialVelocity
            )
        }
    }

    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long {
        updateDelayedTimeNanosIfNeeded(initialValue, targetValue, initialVelocity)
        return cachedOriginalDurationNanos + delayedTimeNanos
    }

    private fun updateDelayedTimeNanosIfNeeded(
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ) {
        if (
            initialValue != cachedInitialValue ||
                targetValue != cachedTargetValue ||
                initialVelocity != cachedInitialVelocity
        ) {
            cachedOriginalDurationNanos =
                originalVectorizedSpringSpec.getDurationNanos(
                    initialValue,
                    targetValue,
                    initialVelocity
                )
            delayedTimeNanos = (cachedOriginalDurationNanos * delayedRatio).toLong()
        }
    }
}

@ExperimentalMaterial3AdaptiveApi
internal object ThreePaneMotionDefaults {
    // TODO(conradchen): open this to public when we support motion customization
    val PanePositionAnimationSpec: SpringSpec<IntOffset> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 600f,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )

    // TODO(conradchen): open this to public when we support motion customization
    val PanePositionAnimationSpecDelayed: DelayedSpringSpec<IntOffset> =
        DelayedSpringSpec(
            dampingRatio = 0.8f,
            stiffness = 600f,
            delayedRatio = 0.1f,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )

    // TODO(conradchen): open this to public when we support motion customization
    val PaneSizeAnimationSpec: SpringSpec<IntSize> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 600f,
            visibilityThreshold = IntSize.VisibilityThreshold
        )
}
