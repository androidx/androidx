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

package androidx.constraintlayout.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MultiMeasureLayout
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.channels.Channel

/**
 * A version of [MotionLayout] that obtains its [ConstraintSet]s at measure time.
 *
 * Note that this version has limited functionality and DOES NOT support on swipe features.
 *
 * It's only meant to be used by ConstraintLayout to animate changes.
 */
@PublishedApi
@Composable
internal fun LateMotionLayout(
    start: MutableState<ConstraintSet?>,
    end: MutableState<ConstraintSet?>,
    animationSpec: AnimationSpec<Float>,
    channel: Channel<ConstraintSet>,
    contentTracker: State<Unit>,
    compositionSource: Ref<CompositionSource>,
    optimizationLevel: Int,
    finishedAnimationListener: (() -> Unit)?,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val measurer = remember { MotionMeasurer(density) }

    val animatableProgress = remember { Animatable(0.0f) }
    val motionProgress = remember { animatableProgress.asState() }
    val direction = remember { mutableIntStateOf(1) }

    // Start and end are guaranteed to be non-null when the lambda is invoked at the measure
    // step.
    val measurePolicy = lateMotionLayoutMeasurePolicy(
        startProvider = remember { { start.value!! } },
        endProvider = remember { { end.value!! } },
        contentTracker = contentTracker,
        compositionSource = compositionSource,
        motionProgress = motionProgress,
        measurer = measurer,
        optimizationLevel = optimizationLevel
    )

    @Suppress("DEPRECATION")
    MultiMeasureLayout(
        modifier = modifier
            .semantics { designInfoProvider = measurer },
        measurePolicy = measurePolicy,
        content = content
    )

    LaunchedEffect(channel) {
        for (constraints in channel) {
            val newConstraints = channel.tryReceive().getOrNull() ?: constraints
            val currentConstraints =
                if (direction.intValue == 1) start.value else end.value
            if (newConstraints != currentConstraints) {
                if (direction.intValue == 1) {
                    end.value = newConstraints
                } else {
                    start.value = newConstraints
                }
                // Force invalidate, since we don't support all MotionLayout features here, we
                // can do this instead of calling MotionMeasurer.initWith
                compositionSource.value = CompositionSource.Content

                animatableProgress.animateTo(direction.intValue.toFloat(), animationSpec)
                direction.intValue = if (direction.intValue == 1) 0 else 1
                finishedAnimationListener?.invoke()
            }
        }
    }
}

/**
 * Same as [motionLayoutMeasurePolicy] but the [ConstraintSet] objects are not available at call
 * time.
 */
private fun lateMotionLayoutMeasurePolicy(
    startProvider: () -> ConstraintSet,
    endProvider: () -> ConstraintSet,
    contentTracker: State<Unit>,
    compositionSource: Ref<CompositionSource>,
    motionProgress: State<Float>,
    measurer: MotionMeasurer,
    optimizationLevel: Int,
): MeasurePolicy =
    MeasurePolicy { measurables, constraints ->
        // Do a state read, to guarantee that we control measure when the content recomposes without
        // notifying our Composable caller
        contentTracker.value

        val layoutSize = measurer.performInterpolationMeasure(
            constraints = constraints,
            layoutDirection = this.layoutDirection,
            constraintSetStart = startProvider(),
            constraintSetEnd = endProvider(),
            transition = TransitionImpl.EMPTY,
            measurables = measurables,
            optimizationLevel = optimizationLevel,
            progress = motionProgress.value,
            compositionSource = compositionSource.value ?: CompositionSource.Unknown,
            invalidateOnConstraintsCallback = null
        )
        compositionSource.value = CompositionSource.Unknown // Reset after measuring

        layout(layoutSize.width, layoutSize.height) {
            with(measurer) {
                performLayout(measurables)
            }
        }
    }
