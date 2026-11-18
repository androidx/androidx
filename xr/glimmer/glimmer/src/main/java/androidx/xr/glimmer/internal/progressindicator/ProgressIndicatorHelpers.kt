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

package androidx.xr.glimmer.internal.progressindicator

import androidx.compose.animation.core.AnimationScope
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.calculateLowEmphasisContentColor
import androidx.xr.glimmer.currentContentColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

// Default values shared by the linear and circular progress indicators.
internal val ProgressIndicatorStrokeWidth: Dp = 6.dp
internal val ProgressIndicatorGapSize: Dp = 6.dp

internal object MotionTokens {
    const val DurationLong2 = 500.0

    inline val EasingEmphasizedAccelerateCubicBezier: CubicBezierEasing
        get() = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    inline val EasingLinearCubicBezier: CubicBezierEasing
        get() = CubicBezierEasing(0.0f, 0.0f, 1.0f, 1.0f)

    inline val EasingStandardCubicBezier: CubicBezierEasing
        get() = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}

/**
 * Returns [trackColor], or a low emphasis version of the content color if [trackColor] is
 * [Color.Unspecified].
 */
internal fun DelegatableNode.resolveTrackColor(trackColor: Color): Color = trackColor.takeOrElse {
    calculateLowEmphasisContentColor(currentContentColor())
}

/**
 * A `Canvas` whose [onDraw] receives the track color resolved from [trackColor] with
 * [resolveTrackColor].
 */
@Composable
internal fun ProgressIndicatorCanvas(
    modifier: Modifier,
    trackColor: Color,
    onDraw: DrawScope.(trackColor: Color) -> Unit,
) {
    Spacer(modifier.then(ProgressIndicatorDrawElement(trackColor, onDraw)))
}

/** A function that returns a determinate indicator's amplitude for a given progress. */
internal val IndicatorAmplitude: (progress: Float) -> Float = { progress ->
    // Sets the amplitude to the max on 10%, and back to zero on 95% of the progress.
    if (progress <= 0.1f || progress >= 0.95f) {
        0f
    } else {
        1f
    }
}

// Animation spec for increasing the amplitude drawing when its changing.
internal val IncreasingAmplitudeAnimationSpec: AnimationSpec<Float> =
    tween(
        durationMillis = MotionTokens.DurationLong2.toInt(),
        easing = MotionTokens.EasingStandardCubicBezier,
    )

// Animation spec for decreasing the amplitude drawing when its changing.
internal val DecreasingAmplitudeAnimationSpec: AnimationSpec<Float> =
    tween(
        durationMillis = MotionTokens.DurationLong2.toInt(),
        easing = MotionTokens.EasingEmphasizedAccelerateCubicBezier,
    )

/**
 * Advances this clock by one unit per millisecond, calling [onFrame] on every frame, until
 * [isIndeterminate] returns false. The clock follows [MotionDurationScale]. Its value modulo
 * [cycleDurationMillis] is the play time in the indeterminate cycle.
 */
internal suspend fun AnimationState<Float, AnimationVector1D>.animateIndeterminateCycle(
    cycleDurationMillis: Int,
    isIndeterminate: () -> Boolean,
    onFrame: AnimationScope<Float, AnimationVector1D>.() -> Unit,
) {
    var sequentialAnimation = true
    while (isIndeterminate()) {
        var stoppedByPolicy = false
        try {
            animateTo(
                targetValue = value + cycleDurationMillis,
                animationSpec =
                    infiniteRepeatable(tween(cycleDurationMillis, easing = LinearEasing)),
                sequentialAnimation = sequentialAnimation,
            ) {
                onFrame()
                if (!isIndeterminate()) cancelAnimation()
            }
        } catch (e: CancellationException) {
            // An InfiniteAnimationPolicy, such as the one in tests, stops infinite animations
            // without cancelling the coroutine.
            currentCoroutineContext().ensureActive()
            stoppedByPolicy = true
        }
        // An infinite animation also ends when animations are disabled.
        val motionDurationScale = currentCoroutineContext()[MotionDurationScale]
        snapshotFlow {
            !isIndeterminate() ||
                (!stoppedByPolicy && (motionDurationScale?.scaleFactor ?: 1f) > 0f)
        }
            .first { it }
        sequentialAnimation = false
    }
}

private class ProgressIndicatorDrawElement(
    private val trackColor: Color,
    private val onDraw: DrawScope.(trackColor: Color) -> Unit,
) : ModifierNodeElement<ProgressIndicatorDrawNode>() {
    override fun create(): ProgressIndicatorDrawNode = ProgressIndicatorDrawNode(trackColor, onDraw)

    override fun update(node: ProgressIndicatorDrawNode) {
        node.trackColor = trackColor
        node.onDraw = onDraw
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "progressIndicatorDraw"
        properties["trackColor"] = trackColor
        properties["onDraw"] = onDraw
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProgressIndicatorDrawElement) return false
        return trackColor == other.trackColor && onDraw === other.onDraw
    }

    override fun hashCode(): Int = 31 * trackColor.hashCode() + onDraw.hashCode()
}

private class ProgressIndicatorDrawNode(
    var trackColor: Color,
    var onDraw: DrawScope.(trackColor: Color) -> Unit,
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        onDraw(resolveTrackColor(trackColor))
        drawContent()
    }
}
