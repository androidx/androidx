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

package androidx.wear.protolayout.material3

import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DimensionBuilders.AngularLayoutConstraint
import androidx.wear.protolayout.DimensionBuilders.DegreesProp
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.DashedArcLine
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.recommendedAnimationSpec
import androidx.wear.protolayout.types.dp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * This method provides the fallback content layout for [circularProgressIndicator] and
 * [segmentedCircularProgressIndicator] using [ArcLine] when the renderer version is lower than
 * 1.403 where [DashedArcLine] is not available.
 *
 * This fallback component consumes 2 animation quotas when [dynamicProgress] is specified with
 * animation by the caller. It is highly recommend to use the [recommendedAnimationSpec] to animate
 * the progress.
 *
 * Note that we require valid start and end angles for calling this method.
 */
internal fun MaterialScope.circularProgressIndicatorFallbackImpl(
    @Dimension(unit = DP) arcContainerSize: Float,
    startAngleDegrees: Float,
    endAngleDegrees: Float,
    staticProgress: Float,
    dynamicProgress: DynamicFloat?,
    @Dimension(unit = DP) strokeWidth: Float,
    @Dimension(unit = DP) gapSize: Float,
    colors: ProgressIndicatorColors
): Box.Builder {
    // Offset the anchor to make space for the cap and half gap.
    val anchorOffsetDegrees =
        (strokeWidth + gapSize).dpToDegree(radius = (arcContainerSize - strokeWidth) / 2.0) / 2

    // Visually, the progress arcline and track arcline never overlaps. But from the layout point
    // of view, their layout size are both from start angle to end angle, and overlaps completely.
    // Thus, we put each arcline in an arc container, and stacks these two arc containers inside a
    // box.
    return Box.Builder()
        .addContent(
            createArc(
                anchorDegree = endAngleDegrees - anchorOffsetDegrees,
                anchorType = LayoutElementBuilders.ARC_ANCHOR_END,
                arcLength =
                    trackInDegrees(
                        sweepAngle = endAngleDegrees - startAngleDegrees,
                        staticProgress = staticProgress,
                        dynamicProgress = dynamicProgress,
                        lengthAdjustment = anchorOffsetDegrees * 2F
                    ),
                color = trackColor(staticProgress, dynamicProgress, colors),
                strokeWidth = strokeWidth
            )
        )
        .addContent(
            createArc(
                anchorDegree = startAngleDegrees + anchorOffsetDegrees,
                anchorType = LayoutElementBuilders.ARC_ANCHOR_START,
                arcLength =
                    progressInDegrees(
                        sweepAngle = endAngleDegrees - startAngleDegrees,
                        staticProgress = staticProgress,
                        dynamicProgress = dynamicProgress,
                        lengthAdjustment = anchorOffsetDegrees * 2F
                    ),
                color = colors.indicatorColor.prop,
                strokeWidth = strokeWidth
            )
        )
}

/**
 * A small offset to make the progress dot remaining when the progress is 0, and the track dot
 * remaining when the progress is 1.
 */
private const val ARC_OFFSET_IN_DEGREES = 0.05f

private fun createArc(
    anchorDegree: Float,
    anchorType: Int,
    arcLength: DegreesProp,
    color: ColorProp,
    @Dimension(unit = DP) strokeWidth: Float
): Arc =
    Arc.Builder()
        .setAnchorAngle(degrees(anchorDegree))
        .setAnchorType(anchorType)
        .setArcDirection(LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE)
        .addContent(
            ArcLine.Builder()
                .setColor(color)
                .setArcDirection(LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE)
                .setLength(arcLength)
                .setLayoutConstraintsForDynamicLength(
                    // We use one Arc container to put one arcline, so it is fine to put 360 here
                    // as layout constraint.
                    AngularLayoutConstraint.Builder(360F).setAngularAlignment(anchorType).build()
                )
                .setThickness(strokeWidth.dp)
                .build()
        )
        .build()

private fun Float.dpToDegree(radius: Double): Float =
    // radianAngle = arcLength / radius
    Math.toDegrees(this / radius).toFloat()

// Progress overflow is handled with modulo.
private fun progressInDegrees(
    sweepAngle: Float,
    staticProgress: Float,
    dynamicProgress: DynamicFloat?,
    lengthAdjustment: Float
): DegreesProp =
    arcInDegrees(
        sweepAngle = sweepAngle,
        staticRatio = staticProgress - floor(staticProgress),
        dynamicRatio = dynamicProgress?.rem(1F),
        lengthAdjustment = lengthAdjustment
    )

// Progress overflow is handled with modulo.
private fun trackInDegrees(
    sweepAngle: Float,
    staticProgress: Float,
    dynamicProgress: DynamicFloat?,
    lengthAdjustment: Float
): DegreesProp =
    arcInDegrees(
        sweepAngle = sweepAngle,
        staticRatio = 1F - (staticProgress - floor(staticProgress)),
        dynamicRatio =
            if (dynamicProgress == null) {
                null
            } else {
                DynamicFloat.constant(1F).minus(dynamicProgress.rem(1F))
            },
        lengthAdjustment = lengthAdjustment
    )

private fun arcInDegrees(
    sweepAngle: Float,
    staticRatio: Float,
    dynamicRatio: DynamicFloat?,
    lengthAdjustment: Float
): DegreesProp {
    val staticValue =
        getCorrectStaticArcLength(
            sweepAngle = sweepAngle,
            ratio = staticRatio,
            lengthAdjustment = lengthAdjustment
        )

    if (dynamicRatio == null) { // static value
        return degrees(staticValue)
    }

    return DegreesProp.Builder(staticValue)
        .setDynamicValue(
            getApproximateDynamicArcLength(
                sweepAngle = sweepAngle,
                ratio = dynamicRatio,
                lengthAdjustment = lengthAdjustment
            )
        )
        .build()
}

/**
 * When drawing the progress arc line, we need to adjust its Length to make space for two caps and
 * gap. Note that, event the progress arc is 0, we still leave the above space, for a good
 * transition to a non-zero progress. Similar for track arc line. The arc length calculation is as
 * follows:
 * ```
 * sweepAngle = endAngle-startAngle
 * perArcLengthAdjustment = cap * 2 - gap
 * maxTotalLength = endAngle - startAngle- 2 * perArcLengthAdjustment
 * ProgressArcLength = clamp(sweepAngle * progress - perArcLengthAdjustment, 0, maxTotalLength)
 * trackArcLength = clamp(sweepAngle*(1-progress) - perArcLengthAdjustment, 0, maxTotalLength)
 * ```
 */
private fun getCorrectStaticArcLength(
    sweepAngle: Float,
    ratio: Float,
    lengthAdjustment: Float
): Float {
    val length = (sweepAngle) * ratio - lengthAdjustment
    val maxLength = sweepAngle - 2F * lengthAdjustment
    return (max(min(length, maxLength), 0F) + ARC_OFFSET_IN_DEGREES)
}

/**
 * When the progress is static, we calculate the arc lengths as {@link #getCorrectStaticArcLength}.
 * However, the clamp operation adds two extra animation quota per arc with animated dynamic
 * progress which is not acceptable. We thus use an approximation calculation for dynamic values,
 * Which is not very precise, but a good approximation. As follows:
 * ```
 * sweepAngle = endAngle-startAngle
 * perArcLengthAdjustment = cap * 2 - gap
 * maxTotalLength = endAngle-startAngle - 2 * perArcLengthAdjustment
 * progressArcLength = maxTotalLength * progress
 * trackArcLength = maxTotalLength * (1-progress)
 * ```
 */
private fun getApproximateDynamicArcLength(
    sweepAngle: Float,
    ratio: DynamicFloat,
    lengthAdjustment: Float
) = ratio.times(sweepAngle - lengthAdjustment * 2).plus(ARC_OFFSET_IN_DEGREES)
