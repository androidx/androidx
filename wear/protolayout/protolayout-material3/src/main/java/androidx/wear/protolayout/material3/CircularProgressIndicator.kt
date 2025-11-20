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

package androidx.wear.protolayout.material3

import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.annotation.IntRange
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DimensionBuilders.AngularLayoutConstraint
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.DegreesProp
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.ExpandedDimensionProp
import androidx.wear.protolayout.DimensionBuilders.WrappedDimensionProp
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcSpacer
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.DashedArcLine
import androidx.wear.protolayout.LayoutElementBuilders.DashedLinePattern
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.CPI_DEFAULT_DP_SIZE
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.INDICATOR_STROKE_WIDTH_INCREMENT_PX
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.LARGE_STROKE_WIDTH
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.METADATA_TAG
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.TRACK_GAP_SIZE_INCREMENT_PX
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.TRIVIAL_ARC_OFFSET
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.calculateRecommendedGapSize
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.filledProgressIndicatorColors
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.recommendedAnimationSpec
import androidx.wear.protolayout.material3.Versions.hasArcDirectionFixed
import androidx.wear.protolayout.material3.Versions.hasDashedArcLineSupport
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.modifiers.tag
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.dp
import kotlin.math.min

/**
 * ProtoLayout Material3 design circular progress indicator.
 *
 * Note that, the proper implementation of this component requires a ProtoLayout renderer with
 * version equal to or above 1.403. When the renderer is lower than 1.403, this component will
 * automatically fallback to an implementation with reduced features, without support for expandable
 * size, and start/end transition.
 *
 * This component consumes 3 animation quotas when [dynamicProgress] is specified with animation by
 * the caller. It is highly recommend to use the [recommendedAnimationSpec] to animate the progress.
 *
 * The progress indicator's [colors] default to using [ColorScheme] from the [MaterialScope] it's
 * defined in, which defaults to [dynamicColorScheme], meaning that the colors follow system theme
 * if available on device. If not, or switched off by user, uses fallback [ColorScheme] defined in
 * its [MaterialScope].
 *
 * @param staticProgress The static progress of this progress indicator where 0 represent no
 *   progress and 1 represents completion. Progress above 1 is also allowed. If [dynamicProgress] is
 *   also set, this static value will only be used when the dynamic value cannot be evaluated. By
 *   default it equals to 0.
 * @param dynamicProgress The dynamic progress of this progress indicator where 0 represent no
 *   progress and 1 represents completion. Progress above 1 is also allowed. If not provided, the
 *   [staticProgress] is used.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param startAngleDegrees The starting position of the progress arc, measured clockwise in degrees
 *   from the 12 o'clock position.
 * @param endAngleDegrees The ending position of the progress arc, measured clockwise in degrees
 *   from 12 o'clock position. Its value must be bigger than [startAngleDegrees], otherwise an
 *   exception would be thrown. By default it equals to 'startAngleDegrees + 360'.
 * @param strokeWidth The stroke width for the progress indicator. The recommended values are
 *   [CircularProgressIndicatorDefaults.LARGE_STROKE_WIDTH] and
 *   [CircularProgressIndicatorDefaults.SMALL_STROKE_WIDTH].
 * @param gapSize The size in dp of the gap between the ends of the progress indicator and the
 *   track. The stroke end caps are not included in this distance.
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   color for this progress indicator.
 * @param size The bounding box size of this progress indicator, applies to both width and height.
 *   The indicator arc and track arc are located on the largest circle that can be inscribed inside.
 *   It is highly recommended for the progress indicator in a graphics card to have its size as
 *   [ExpandedDimensionProp], which is the default, to fill the available space for the best result
 *   across different screen sizes. Setting [size] with a [WrappedDimensionProp] instance will cause
 *   failure and throws an [IllegalArgumentException].
 * @throws IllegalArgumentException When [size] is set to be [WrappedDimensionProp] instance or the
 *   provided [endAngleDegrees] is smaller than the [startAngleDegrees].
 * @sample androidx.wear.protolayout.material3.samples.singleSegmentCircularProgressIndicator
 */
public fun MaterialScope.circularProgressIndicator(
    staticProgress: Float = 0F,
    dynamicProgress: DynamicFloat? = null,
    modifier: LayoutModifier = LayoutModifier,
    startAngleDegrees: Float = 0F,
    endAngleDegrees: Float = startAngleDegrees + 360F,
    @Dimension(DP) strokeWidth: Float = LARGE_STROKE_WIDTH,
    @Dimension(DP) gapSize: Float = calculateRecommendedGapSize(strokeWidth),
    colors: ProgressIndicatorColors =
        defaultProgressIndicatorStyle.color ?: filledProgressIndicatorColors(),
    size: ContainerDimension = expand(),
): Box {
    // CircularProgressIndicator could not have size as wrap
    verifySize(size)

    val modifiers = (LayoutModifier.tag(METADATA_TAG) then modifier).toProtoLayoutModifiers()
    val hasDashedArcLineSupport =
        deviceConfiguration.rendererSchemaVersion.hasDashedArcLineSupport()
    // With the fallback implementation, expandable size is not supported, fallback to dp size.
    val containerSize =
        if (hasDashedArcLineSupport || size is DpProp) {
            size
        } else {
            CPI_DEFAULT_DP_SIZE.dp
        }
    val boxBuilder =
        if (hasDashedArcLineSupport) {
            singleSegmentImpl(
                startAngleDegrees = startAngleDegrees,
                endAngleDegrees = checkAndAdjustEndAngle(startAngleDegrees, endAngleDegrees),
                staticProgress = staticProgress,
                dynamicProgress = dynamicProgress,
                strokeWidth = strokeWidth,
                gapSize = gapSize,
                colors = colors,
            )
        } else {
            circularProgressIndicatorFallbackImpl(
                // Without DashedArcLine support, container size fell back to dp size.
                arcContainerSize = (containerSize as DpProp).value,
                startAngleDegrees = startAngleDegrees,
                endAngleDegrees = checkAndAdjustEndAngle(startAngleDegrees, endAngleDegrees),
                staticProgress = staticProgress,
                dynamicProgress = dynamicProgress,
                strokeWidth = strokeWidth,
                gapSize = gapSize,
                colors = colors,
            )
        }

    return boxBuilder
        .setModifiers(modifiers)
        .setWidth(containerSize)
        .setHeight(containerSize)
        .build()
}

/**
 * ProtoLayout Material3 design segmented circular progress indicator.
 *
 * A segmented variant of [circularProgressIndicator] that is divided into equally sized segments.
 *
 * Note that, the proper implementation of this component requires a ProtoLayout renderer with
 * version equal to or above 1.403. When the renderer is lower than 1.403, this component will
 * automatically fallback to an implementation with reduced features, without support for multiple
 * segments, expandable size, and start/end transition.
 *
 * This component consumes 2 animation quotas when [dynamicProgress] is specified with animation by
 * the caller. It is highly recommend to use the [recommendedAnimationSpec] to animate the progress.
 *
 * The progress indicator's [colors] default to using [ColorScheme] from the [MaterialScope] it's
 * defined in, which defaults to [dynamicColorScheme], meaning that the colors follow system theme
 * if available on device. If not, or switched off by user, uses fallback [ColorScheme] defined in
 * its [MaterialScope].
 *
 * @param segmentCount Number of equal segments that the progress indicator should be divided into.
 *   Has to be a number greater than or equal to 1.
 * @param staticProgress The static progress of this progress indicator where 0 represent no
 *   progress and 1 represents completion. Progress above 1 is also allowed. If [dynamicProgress] is
 *   also set, this static value will only be used when the dynamic value cannot be evaluated. By
 *   default it equals to 0.
 * @param dynamicProgress The dynamic progress of this progress indicator where 0 represent no
 *   progress and 1 represents completion. Progress above 1 is also allowed. If not provided, the
 *   [staticProgress] is used.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param startAngleDegrees The starting position of the progress arc, measured clockwise in degrees
 *   from the 12 o'clock position.
 * @param endAngleDegrees The ending position of the progress arc, measured clockwise in degrees
 *   from 12 o'clock position. This value must be bigger than [startAngleDegrees], otherwise an
 *   exception would be thrown. By default it equals to 'startAngleDegrees + 360'.
 * @param strokeWidth The stroke width for the progress indicator. The recommended values are
 *   [CircularProgressIndicatorDefaults.LARGE_STROKE_WIDTH] and
 *   [CircularProgressIndicatorDefaults.SMALL_STROKE_WIDTH].
 * @param gapSize The size in dp of the gap between the ends of the progress indicator and the
 *   track. The stroke end caps are not included in this distance.
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   color for this progress indicator.
 * @param size The bounding box size of this progress indicator, applies to both width and height.
 *   The indicator arc and track arc are located on the largest circle that can be inscribed inside.
 *   It is highly recommended for the progress indicator in [graphicDataCard] to have its size as
 *   [expand], which is the default, to fill the available space for the best result across
 *   different screen sizes. Setting [size] with a [WrappedDimensionProp] instance will cause
 *   failure and throws an [IllegalArgumentException].
 * @throws IllegalArgumentException When [size] is set to be [WrappedDimensionProp] instance or the
 *   provided [endAngleDegrees] is smaller than the [startAngleDegrees].
 * @sample androidx.wear.protolayout.material3.samples.multipleSegmentsCircularProgressIndicator
 */
public fun MaterialScope.segmentedCircularProgressIndicator(
    @IntRange(from = 1) segmentCount: Int,
    staticProgress: Float = 0F,
    dynamicProgress: DynamicFloat? = null,
    modifier: LayoutModifier = LayoutModifier,
    startAngleDegrees: Float = 0F,
    endAngleDegrees: Float = startAngleDegrees + 360F,
    @Dimension(DP) strokeWidth: Float = LARGE_STROKE_WIDTH,
    @Dimension(DP) gapSize: Float = calculateRecommendedGapSize(strokeWidth),
    colors: ProgressIndicatorColors =
        defaultProgressIndicatorStyle.color ?: filledProgressIndicatorColors(),
    size: ContainerDimension = expand(),
): Box {
    // CircularProgressIndicator could not have size as wrap
    verifySize(size)

    val modifiers = (LayoutModifier.tag(METADATA_TAG) then modifier).toProtoLayoutModifiers()
    val hasDashedArcLineSupport =
        deviceConfiguration.rendererSchemaVersion.hasDashedArcLineSupport()
    // Without using DashedArcLine, expandable size is not supported, fallback to dp size.
    val containerSize =
        if (hasDashedArcLineSupport || size is DpProp) {
            size
        } else {
            CPI_DEFAULT_DP_SIZE.dp
        }
    val boxBuilder =
        if (hasDashedArcLineSupport) {
            multipleSegmentsImpl(
                segmentCount = segmentCount,
                startAngleDegrees = startAngleDegrees,
                endAngleDegrees = checkAndAdjustEndAngle(startAngleDegrees, endAngleDegrees),
                staticProgress = staticProgress,
                dynamicProgress = dynamicProgress,
                strokeWidth = strokeWidth,
                gapSize = gapSize,
                colors = colors,
            )
        } else {
            circularProgressIndicatorFallbackImpl(
                // Without DashedArcLine support, container size fell back to dp size.
                arcContainerSize = (containerSize as DpProp).value,
                startAngleDegrees = startAngleDegrees,
                endAngleDegrees = checkAndAdjustEndAngle(startAngleDegrees, endAngleDegrees),
                staticProgress = staticProgress,
                dynamicProgress = dynamicProgress,
                strokeWidth = strokeWidth,
                gapSize = gapSize,
                colors = colors,
            )
        }

    return boxBuilder
        .setModifiers(modifiers)
        .setWidth(containerSize)
        .setHeight(containerSize)
        .build()
}

/**
 * Layout the content for single segment progress indicator using [DashedArcLine].
 *
 * Note that we require valid start and end angles for calling this method.
 */
private fun MaterialScope.singleSegmentImpl(
    startAngleDegrees: Float,
    endAngleDegrees: Float,
    staticProgress: Float,
    dynamicProgress: DynamicFloat?,
    @Dimension(DP) strokeWidth: Float,
    @Dimension(DP) gapSize: Float,
    colors: ProgressIndicatorColors,
): Box.Builder {
    val sweepAngle = endAngleDegrees - startAngleDegrees
    val progressInDegrees =
        progressInDegrees(
            sweepAngle = sweepAngle,
            staticProgress = staticProgress,
            dynamicProgress =
                if (
                    deviceConfiguration.rendererSchemaVersion.hasArcDirectionFixed() ||
                        dynamicProgress != null
                ) {
                    dynamicProgress
                } else {
                    // We got issue with the arcDirection handling before renderer version 1.520,
                    // which is fixed in newer version of renderer.  When the progress is static,
                    // the counterclockwise is not rendered with correct direction, see b/432663972.
                    // This issue does not happen when the progress is dynamic, so the hack here
                    // is to set a constant dynamic value, which delays the set of arc length after
                    // the view is attached.
                    DynamicFloat.constant(staticProgress)
                },
        )
    val trackInDegrees = trackInDegrees(sweepAngle, progressInDegrees)

    // Indicator: anchor end to startAngle, counter clockwise
    // | half gap spacer | progress arc | full gap |
    //                                  Track: anchor end to endAngle, clockwise
    //                                  | full gap |  indicator arc | half gap spacer|
    val linePattern =
        DashedLinePattern.Builder()
            // We set doubled gap size, as zero is the center of the gap, and we only draw
            // from zero, this gives us one gap size as commented above. This allows
            //    1. have a gap between indicator and track arclines
            //    2. have a gap between head and tail when the sweep angle is 360 and only one
            //       arcline is displayed (progress is zero or one).
            .setGapSize(gapSize * 2)
            .setGapLocations(0f)
            .build()
    val spacer =
        ArcSpacer.Builder().setAngularLength((gapSize / 2F).dp).setThickness(strokeWidth.dp).build()
    return Box.Builder()
        .addContent( // the track
            createArc(
                    anchorAngle = degrees(endAngleDegrees),
                    anchorType = LayoutElementBuilders.ARC_ANCHOR_END,
                    arcLength = trackInDegrees,
                    arcColor = trackColor(staticProgress, dynamicProgress, colors),
                    strokeWidth = strokeWidth,
                    linePattern = linePattern,
                    arcDirection = LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE,
                )
                .addContent(spacer)
                .build()
        )
        .addContent( // the indicator
            createArc(
                    anchorAngle = degrees(-startAngleDegrees),
                    anchorType = LayoutElementBuilders.ARC_ANCHOR_END,
                    arcLength = progressInDegrees,
                    arcColor = colors.indicatorColor.prop,
                    strokeWidth = strokeWidth,
                    linePattern = linePattern,
                    arcDirection = LayoutElementBuilders.ARC_DIRECTION_COUNTER_CLOCKWISE,
                )
                .addContent(spacer)
                .build()
        )
}

/**
 * Layout the content for segmented variant of progress indicator using [DashedArcLine].
 *
 * Note that we require valid start and end angles for calling this method.
 */
private fun MaterialScope.multipleSegmentsImpl(
    segmentCount: Int,
    startAngleDegrees: Float,
    endAngleDegrees: Float,
    staticProgress: Float,
    dynamicProgress: DynamicFloat?,
    @Dimension(DP) strokeWidth: Float,
    @Dimension(DP) gapSize: Float,
    colors: ProgressIndicatorColors,
): Box.Builder {
    val sweepAngle = endAngleDegrees - startAngleDegrees
    val progressInDegrees =
        progressInDegrees(
            sweepAngle = sweepAngle,
            staticProgress = staticProgress,
            dynamicProgress = dynamicProgress,
        )
    val gapInterval = sweepAngle / segmentCount

    // To prevent aliasing issue, we need to make sure the top arc covers the bottom one completely
    // in the overlapped area:
    // 1. make the indicator arc a bit wider than the track arc
    val insetPadding = INDICATOR_STROKE_WIDTH_INCREMENT_PX / deviceConfiguration.screenDensity / 2F
    // 2. make the track arc slightly shorter than the indicator arc .
    val trackGapIncrement = TRACK_GAP_SIZE_INCREMENT_PX / deviceConfiguration.screenDensity
    return Box.Builder()
        .addContent(
            // the track
            createArc(
                    anchorAngle = degrees(startAngleDegrees),
                    anchorType = LayoutElementBuilders.ARC_ANCHOR_START,
                    arcLength = degrees(sweepAngle),
                    arcColor = trackColor(staticProgress, dynamicProgress, colors),
                    strokeWidth = strokeWidth,
                    linePattern =
                        DashedLinePattern.Builder()
                            .setGapSize(gapSize + trackGapIncrement)
                            .setGapInterval(gapInterval)
                            .build(),
                    arcDirection = LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE,
                )
                .setModifiers(
                    Modifiers.Builder()
                        // Note that View#setPadding only accept integer, so this will be
                        // rounded up to one pixel during inflation.
                        .setPadding(padding(insetPadding))
                        .build()
                )
                .build()
        )
        .addContent(
            // the indicator
            createArc(
                    anchorAngle = degrees(startAngleDegrees),
                    anchorType = LayoutElementBuilders.ARC_ANCHOR_START,
                    arcLength = progressInDegrees,
                    arcColor = colors.indicatorColor.prop,
                    strokeWidth = strokeWidth + insetPadding * 2F,
                    linePattern =
                        DashedLinePattern.Builder()
                            .setGapSize(gapSize)
                            .setGapInterval(gapInterval)
                            .build(),
                    arcDirection = LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE,
                )
                .build()
        )
}

/**
 * Verify that it is not a size of wrap, otherwise throw an exception.
 *
 * @throws IllegalArgumentException When [size] is set to be [WrappedDimensionProp] instance.
 */
private fun verifySize(size: ContainerDimension) {
    if (size is WrappedDimensionProp) {
        throw IllegalArgumentException("CircularProgressIndicator could not have size as wrap")
    }
}

/*
 * Check the endAngle is valid with the provided startAngle, and adjust the sweep angle to be 360
 * maximum.
 * @throws IllegalArgumentException When the  engAngle is smaller than the startAngle.
 */
private fun checkAndAdjustEndAngle(startAngle: Float, endAngle: Float): Float {
    if (endAngle < startAngle) {
        throw IllegalArgumentException("End angle must be bigger than start angle")
    }

    // the maximum sweep angle is 360
    return min(endAngle, startAngle + 360F)
}

private fun progressInDegrees(
    sweepAngle: Float,
    staticProgress: Float,
    dynamicProgress: DynamicFloat?,
): DegreesProp =
    // Note that (-0.05) % 360 = -0.05;  while (-0.05).mod(360) = 359.95, we need to use % here.
    DegreesProp.Builder((staticProgress * sweepAngle - TRIVIAL_ARC_OFFSET) % sweepAngle)
        .apply {
            dynamicProgress?.let {
                setDynamicValue(it.times(sweepAngle).minus(TRIVIAL_ARC_OFFSET).rem(sweepAngle))
            }
        }
        .build()

private fun trackInDegrees(sweepAngle: Float, progressInDegrees: DegreesProp): DegreesProp =
    DegreesProp.Builder(sweepAngle - progressInDegrees.value)
        .apply {
            progressInDegrees.dynamicValue?.let {
                setDynamicValue(DynamicFloat.constant(sweepAngle).minus(it))
            }
        }
        .build()

internal fun trackColor(
    staticProgress: Float,
    dynamicProgress: DynamicFloat?,
    colors: ProgressIndicatorColors,
): ColorProp =
    ColorProp.Builder(
            if (staticProgress > 1) {
                colors.trackOverflowColor.prop.argb
            } else {
                colors.trackColor.prop.argb
            }
        )
        .apply {
            dynamicProgress?.let {
                setDynamicValue(
                    DynamicColor.onCondition(it.gt(1F))
                        .use(colors.trackOverflowColor.prop.argb)
                        .elseUse(colors.trackColor.prop.argb)
                )
            }
        }
        .build()

private fun createArc(
    anchorAngle: DegreesProp,
    anchorType: Int,
    arcLength: DegreesProp,
    arcColor: ColorProp,
    @Dimension(DP) strokeWidth: Float,
    linePattern: DashedLinePattern,
    arcDirection: Int,
): Arc.Builder =
    Arc.Builder()
        .setAnchorAngle(anchorAngle)
        .setAnchorType(anchorType)
        .setArcDirection(arcDirection)
        .addContent(
            DashedArcLine.Builder()
                .setColor(arcColor)
                .setThickness(strokeWidth)
                .setLinePattern(linePattern)
                .setLength(arcLength)
                .setArcDirection(arcDirection)
                .setLayoutConstraintsForDynamicLength(
                    // We use one Arc container to put one arcline, so it is fine to put 360 here
                    // as layout constraint.
                    AngularLayoutConstraint.Builder(360f).setAngularAlignment(anchorType).build()
                )
                .build()
        )
