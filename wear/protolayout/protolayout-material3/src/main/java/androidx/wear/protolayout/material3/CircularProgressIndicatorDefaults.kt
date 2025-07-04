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
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationParameters
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationSpec
import androidx.wear.protolayout.expression.AnimationParameterBuilders.Easing
import androidx.wear.protolayout.types.LayoutColor

/**
 * Represents the indicator and track colors used in progress indicator.
 *
 * @param indicatorColor Color used to draw the indicator of progress indicator.
 * @param trackColor Color used to draw the track of progress indicator.
 * @param trackOverflowColor Color used to draw the track for progress overflow (>1).
 */
public class ProgressIndicatorColors(
    public val indicatorColor: LayoutColor,
    public val trackColor: LayoutColor,
    public val trackOverflowColor: LayoutColor = trackColor,
) {
    /**
     * Returns a copy of this [ProgressIndicatorColors], optionally overriding some of the values.
     *
     * @param indicatorColor Color used to draw the indicator of progress indicator.
     * @param trackColor Color used to draw the track of progress indicator.
     * @param trackOverflowColor Color used to draw the track for progress overflow (>1).
     */
    public fun copy(
        indicatorColor: LayoutColor = this.indicatorColor,
        trackColor: LayoutColor = this.trackColor,
        trackOverflowColor: LayoutColor = this.trackOverflowColor,
    ): ProgressIndicatorColors =
        ProgressIndicatorColors(
            indicatorColor = indicatorColor,
            trackColor = trackColor,
            trackOverflowColor = trackOverflowColor,
        )
}

public object CircularProgressIndicatorDefaults {
    /**
     * Returns the recommended [ProgressIndicatorColors] object to be used when placing the progress
     * indicator inside a graphic card with [CardDefaults.filledCardColors] from the given
     * [MaterialScope]'s [ColorScheme].
     */
    public fun MaterialScope.filledProgressIndicatorColors(): ProgressIndicatorColors =
        ProgressIndicatorColors(
            theme.colorScheme.onPrimary,
            theme.colorScheme.onPrimary.withOpacity(0.2F),
            theme.colorScheme.onPrimary.withOpacity(0.6F),
        )

    /**
     * Returns the recommended [ProgressIndicatorColors] object to be used when placing the progress
     * indicator inside a graphic card with [CardDefaults.filledTonalCardColors] from the given
     * [MaterialScope]'s [ColorScheme].
     */
    public fun MaterialScope.filledTonalProgressIndicatorColors(): ProgressIndicatorColors =
        ProgressIndicatorColors(
            theme.colorScheme.primary,
            theme.colorScheme.primary.withOpacity(0.2F),
            theme.colorScheme.primary.withOpacity(0.6F),
        )

    /**
     * Returns the recommended [ProgressIndicatorColors] object to be used when placing the progress
     * indicator inside a graphic card with [CardDefaults.filledVariantCardColors] from the given
     * [MaterialScope]'s [ColorScheme].
     */
    public fun MaterialScope.filledVariantProgressIndicatorColors(): ProgressIndicatorColors =
        ProgressIndicatorColors(
            theme.colorScheme.onPrimaryContainer,
            theme.colorScheme.onPrimaryContainer.withOpacity(0.2F),
            theme.colorScheme.onPrimaryContainer.withOpacity(0.6F),
        )

    /**
     * The recommended animation spec for animations from current progress to a new progress value.
     */
    public val recommendedAnimationSpec: AnimationSpec =
        AnimationSpec.Builder()
            .setAnimationParameters(
                AnimationParameters.Builder()
                    .setDurationMillis(450)
                    .setEasing(Easing.cubicBezier(0.2f, 0f, 0f, 1f))
                    .build()
            )
            .build()

    /** Large stroke width for circular progress indicator. */
    @Dimension(DP) public const val LARGE_STROKE_WIDTH: Float = 8F

    /** Small stroke width for circular progress indicator. */
    @Dimension(DP) public const val SMALL_STROKE_WIDTH: Float = 4F

    /**
     * Returns recommended size of the gap based on [strokeWidth].
     *
     * The absolute value can be customized with `gapSize` parameter on [circularProgressIndicator].
     */
    @Dimension(DP)
    public fun calculateRecommendedGapSize(@Dimension(DP) strokeWidth: Float): Float =
        strokeWidth / 3F

    internal const val METADATA_TAG: String = "M3CPI"
    /**
     * A small offset to make the progress arc remaining when the progress is 1, with the module
     * operation applied for handling overflow.
     */
    internal const val TRIVIAL_ARC_OFFSET: Float = 0.05f

    /**
     * Extra stroke width for progress arc to prevent aliasing issue where the progress arc draws on
     * top of the track arc where there are multiple segments.
     */
    internal const val INDICATOR_STROKE_WIDTH_INCREMENT_PX: Float = 1.5f

    /**
     * Extra gap size in pixels between track arc segments to make the each track arc segment
     * slightly shorter than the indicator arc segment on its top, so that it can be fully covered
     * by the indicator on segment ends to prevent aliasing issue.
     */
    internal const val TRACK_GAP_SIZE_INCREMENT_PX: Float = 1f

    /** Default size for the fallback implementation. */
    @Dimension(DP) internal const val CPI_DEFAULT_DP_SIZE: Float = 52F
}
