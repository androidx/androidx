/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import androidx.compose.animation.core.Easing
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Constraints
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Parameters to control the scaling of the contents of a [ScalingLazyColumn].
 *
 * Items in the ScalingLazyColumn have scaling and alpha effects applied to them depending on
 * their position in the viewport. The closer to the edge (top or bottom) of the viewport that
 * they are the greater the down scaling and transparency that is applied. Note that scaling and
 * transparency effects are applied from the center of the viewport (full size and normal
 * transparency) towards the edge (items can be smaller and more transparent).
 *
 * Deciding how much scaling and alpha to apply is based on the position and size of the item
 * and on a series of properties that are used to determine the transition area for each item.
 *
 * The transition area is defined by the edge of the screen and a transition line which is
 * calculated for each item in the list. The items transition line is based upon its size with
 * the potential for larger list items to start their transition earlier (closer to the center)
 * than smaller items.
 *
 * [minTransitionArea] and [maxTransitionArea] are both in the range [0f..1f] and are
 * the fraction of the distance between the edge of the viewport and the center of
 * the viewport. E.g. a value of 0.2f for minTransitionArea and 0.75f for maxTransitionArea
 * determines that all transition lines will fall between 1/5th (20%) and 3/4s (75%) of the
 * distance between the viewport edge and center.
 *
 * The size of the each item is used to determine where within the transition area range
 * minTransitionArea..maxTransitionArea the actual transition line will be. [minElementHeight]
 * and [maxElementHeight] are used along with the item height (as a fraction of the viewport
 * height in the range [0f..1f]) to find the transition line. So if the items size is 0.25f
 * (25%) of way between minElementSize..maxElementSize then the transition line will be 0.25f
 * (25%) of the way between minTransitionArea..maxTransitionArea.
 *
 * A list item smaller than minElementHeight is rounded up to minElementHeight and larger than
 * maxElementHeight is rounded down to maxElementHeight. Whereabouts the items height sits
 * between minElementHeight..maxElementHeight is then used to determine where the transition
 * line sits between minTransitionArea..maxTransition area.
 *
 * If an item is smaller than or equal to minElementSize its transition line with be at
 * minTransitionArea and if it is larger than or equal to maxElementSize its transition line
 * will be  at maxTransitionArea.
 *
 * For example, if we take the default values for minTransitionArea = 0.2f and
 * maxTransitionArea = 0.6f and minElementSize = 0.2f and maxElementSize= 0.8f then an item
 * with a height of 0.4f (40%) of the viewport height is one third of way between
 * minElementSize and maxElementSize, (0.4f - 0.2f) / (0.8f - 0.2f) = 0.33f. So its transition
 * line would be one third of way between 0.2f and 0.6f, transition line = 0.2f + (0.6f -
 * 0.2f) * 0.33f = 0.33f.
 *
 * Once the position of the transition line is established we now have a transition area
 * for the item, e.g. in the example above the item will start/finish its transitions when it
 * is 0.33f (33%) of the distance from the edge of the viewport and will start/finish its
 * transitions at the viewport edge.
 *
 * The scaleInterpolator is used to determine how much of the scaling and alpha to apply
 * as the item transits through the transition area.
 *
 * The edge of the item furthest from the edge of the screen is used as a scaling trigger
 * point for each item.
 */
public interface ScalingParams {
    /**
     * What fraction of the full size of the item to scale it by when most
     * scaled, e.g. at the edge of the viewport. A value between [0f,1f], so a value of 0.2f
     * means to scale an item to 20% of its normal size.
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val edgeScale: Float

    /**
     * What fraction of the full transparency of the item to draw it with
     * when closest to the edge of the screen. A value between [0f,1f], so a value of
     * 0.2f means to set the alpha of an item to 20% of its normal value.
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val edgeAlpha: Float

    /**
     * The maximum element height as a ratio of the viewport size to use
     * for determining the transition point within ([minTransitionArea], [maxTransitionArea])
     * that a given content item will start to be transitioned. Items larger than [maxElementHeight]
     * will be treated as if [maxElementHeight]. Must be greater than or equal to
     * [minElementHeight].
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val minElementHeight: Float

    /**
     * The maximum element height as a ratio of the viewport size to use
     * for determining the transition point within ([minTransitionArea], [maxTransitionArea])
     * that a given content item will start to be transitioned. Items larger than [maxElementHeight]
     * will be treated as if [maxElementHeight]. Must be greater than or equal to
     * [minElementHeight].
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val maxElementHeight: Float

    /**
     * The lower bound of the transition line area, closest to the
     * edge of the viewport. Defined as a fraction (value between 0f..1f) of the distance between
     * the viewport edge and viewport center line. Must be less than or equal to
     * [maxTransitionArea].
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val minTransitionArea: Float

    /**
     * The upper bound of the transition line area, closest to the
     * center of the viewport. The fraction (value between 0f..1f) of the distance
     * between the viewport edge and viewport center line. Must be greater
     * than or equal to [minTransitionArea].
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val maxTransitionArea: Float

    /**
     * An interpolator to use to determine how to apply scaling as a item
     * transitions across the scaling transition area.
     */
    val scaleInterpolator: Easing

    /**
     * The additional padding to consider above and below the
     * viewport of a [ScalingLazyColumn] when considering which items to draw in the viewport. If
     * set to 0 then no additional padding will be provided and only the items which would appear
     * in the viewport before any scaling is applied will be considered for drawing, this may
     * leave blank space at the top and bottom of the viewport where the next available item
     * could have been drawn once other items have been scaled down in size. The larger this
     * value is set to will allow for more content items to be considered for drawing in the
     * viewport, however there is a performance cost associated with materializing items that are
     * subsequently not drawn. The higher/more extreme the scaling parameters that are applied to
     * the [ScalingLazyColumn] the more padding may be needed to ensure there are always enough
     * content items available to be rendered. By default will be 20% of the maxHeight of the
     * viewport above and below the content.
     *
     * @param viewportConstraints the viewports constraints
     */
    public fun resolveViewportVerticalOffset(viewportConstraints: Constraints): Int
}

@Stable
internal class DefaultScalingParams(
    override val edgeScale: Float,
    override val edgeAlpha: Float,
    override val minElementHeight: Float,
    override val maxElementHeight: Float,
    override val minTransitionArea: Float,
    override val maxTransitionArea: Float,
    override val scaleInterpolator: Easing,
    val viewportVerticalOffsetResolver: (Constraints) -> Int,
) : ScalingParams {

    init {
        check(
            minElementHeight <= maxElementHeight,
            { "minElementHeight must be less than or equal to maxElementHeight" }
        )
        check(
            minTransitionArea <= maxTransitionArea,
            { "minTransitionArea must be less than or equal to maxTransitionArea" }
        )
    }

    override fun resolveViewportVerticalOffset(viewportConstraints: Constraints): Int {
        return viewportVerticalOffsetResolver(viewportConstraints)
    }

    override fun toString(): String {
        return "ScalingParams(edgeScale=$edgeScale, edgeAlpha=$edgeAlpha, " +
            "minElementHeight=$minElementHeight, maxElementHeight=$maxElementHeight, " +
            "minTransitionArea=$minTransitionArea, maxTransitionArea=$maxTransitionArea)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultScalingParams

        if (edgeScale != other.edgeScale) return false
        if (edgeAlpha != other.edgeAlpha) return false
        if (minElementHeight != other.minElementHeight) return false
        if (maxElementHeight != other.maxElementHeight) return false
        if (minTransitionArea != other.minTransitionArea) return false
        if (maxTransitionArea != other.maxTransitionArea) return false
        if (scaleInterpolator != other.scaleInterpolator) return false
        if (viewportVerticalOffsetResolver != other.viewportVerticalOffsetResolver) return false

        return true
    }

    override fun hashCode(): Int {
        var result = edgeScale.hashCode()
        result = 31 * result + edgeAlpha.hashCode()
        result = 31 * result + minElementHeight.hashCode()
        result = 31 * result + maxElementHeight.hashCode()
        result = 31 * result + minTransitionArea.hashCode()
        result = 31 * result + maxTransitionArea.hashCode()
        result = 31 * result + scaleInterpolator.hashCode()
        result = 31 * result + viewportVerticalOffsetResolver.hashCode()
        return result
    }
}

/**
 * Calculate the scale and alpha to apply for an item based on the start and end position of the
 * component's viewport in pixels and top and bottom position of the item, also in pixels.
 *
 * Firstly worked out if the component is above or below the viewport's center-line which determines
 * whether the item's top or bottom will be used as the trigger for scaling/alpha.
 *
 * Uses the scalingParams to determine where the scaling transition line is for the component.
 *
 * Then determines if the component is inside the scaling area, and if so what scaling/alpha effects
 * to apply.
 *
 * @param viewPortStartPx The start position of the component's viewport in pixels
 * @param viewPortEndPx The end position of the component's viewport in pixels
 * @param itemTopPx The top of the content item in pixels.
 * @param itemBottomPx The bottom of the content item in pixels.
 * @param scalingParams The parameters that determine where the item's scaling transition line
 * is, how scaling and transparency to apply.
 */
internal fun calculateScaleAndAlpha(
    viewPortStartPx: Int,
    viewPortEndPx: Int,
    itemTopPx: Int,
    itemBottomPx: Int,
    scalingParams: ScalingParams,
): ScaleAndAlpha {
    var scaleToApply = 1.0f
    var alphaToApply = 1.0f

    val viewPortEdgeToCenterPx = (viewPortEndPx - viewPortStartPx).toFloat() / 2f
    val itemHeightPx = (itemBottomPx - itemTopPx).toFloat()

    val viewPortCenterPx = viewPortStartPx + viewPortEdgeToCenterPx

    if ((itemTopPx..itemBottomPx).contains(viewPortCenterPx.roundToInt())) {
        // No scaling of the centerItem
        return ScaleAndAlpha.noScaling
    }

    /*
     * Calculate the position of the edge of the item closest to the center line of the viewport as
     * a fraction. The [itemEdgePx] and [scrollPositionPx] values are in pixels.
     */
    val itemEdgeAsFractionOfHalfViewport =
        min(itemBottomPx - viewPortStartPx, viewPortEndPx - itemTopPx) / viewPortEdgeToCenterPx

    // TODO(b/202164558) - double check the height calculations with UX
    val heightAsFractionOfHalfViewPort = itemHeightPx / viewPortEdgeToCenterPx
    if (itemEdgeAsFractionOfHalfViewport > 0.0f && itemEdgeAsFractionOfHalfViewport < 1.0f) {
        // Work out the scaling line based on size, this is a value between 0.0..1.0
        val sizeRatio: Float =
            (
                (heightAsFractionOfHalfViewPort - scalingParams.minElementHeight) /
                    (scalingParams.maxElementHeight - scalingParams.minElementHeight)
                ).coerceIn(0f, 1f)

        val scalingLineAsFractionOfViewPort =
            scalingParams.minTransitionArea +
                (scalingParams.maxTransitionArea - scalingParams.minTransitionArea) *
                sizeRatio

        if (itemEdgeAsFractionOfHalfViewport < scalingLineAsFractionOfViewPort) {
            // We are scaling
            val fractionOfDiffToApplyRaw =
                (scalingLineAsFractionOfViewPort - itemEdgeAsFractionOfHalfViewport) /
                    scalingLineAsFractionOfViewPort
            val fractionOfDiffToApplyInterpolated =
                scalingParams.scaleInterpolator.transform(fractionOfDiffToApplyRaw)

            scaleToApply =
                scalingParams.edgeScale +
                (1.0f - scalingParams.edgeScale) *
                (1.0f - fractionOfDiffToApplyInterpolated)
            alphaToApply =
                scalingParams.edgeAlpha +
                (1.0f - scalingParams.edgeAlpha) *
                (1.0f - fractionOfDiffToApplyInterpolated)
        }
    } else {
        scaleToApply = scalingParams.edgeScale
        alphaToApply = scalingParams.edgeAlpha
    }

    return ScaleAndAlpha(scaleToApply, alphaToApply)
}

/**
 * Create a [ScalingLazyListItemInfo] given an unscaled start and end position for an item.
 *
 * @param itemStart the x-axis position of a list item. The x-axis position takes into account
 * any adjustment to the original position based on the scaling of other list items.
 * @param item the original item info used to provide the pre-scaling position and size
 * information for the item.
 * @param verticalAdjustment the amount of vertical adjustment to apply to item positions to
 * allow for content padding in order to determine the adjusted position of the item within the
 * viewport in order to correctly calculate the scaling to apply.
 * @param viewportHeightPx the height of the viewport in pixels
 * @param scalingParams the scaling params to use for determining the scaled size of the item
 */
internal fun createItemInfo(
    itemStart: Int,
    item: LazyListItemInfo,
    verticalAdjustment: Int,
    viewportHeightPx: Int,
    scalingParams: ScalingParams,
): ScalingLazyListItemInfo {
    val adjustedItemStart = itemStart - verticalAdjustment
    val adjustedItemEnd = itemStart + item.size - verticalAdjustment

    val scaleAndAlpha = calculateScaleAndAlpha(
        viewPortStartPx = 0, viewPortEndPx = viewportHeightPx, itemTopPx = adjustedItemStart,
        itemBottomPx = adjustedItemEnd, scalingParams = scalingParams
    )

    val isAboveLine = (adjustedItemEnd + adjustedItemStart) < viewportHeightPx
    val scaledHeight = (item.size * scaleAndAlpha.scale).roundToInt()
    val scaledItemTop = if (!isAboveLine) {
        itemStart
    } else {
        itemStart + item.size - scaledHeight
    }

    return DefaultScalingLazyListItemInfo(
        index = item.index,
        unadjustedOffset = item.offset,
        offset = scaledItemTop,
        size = scaledHeight,
        scale = scaleAndAlpha.scale,
        alpha = scaleAndAlpha.alpha
    )
}

internal class DefaultScalingLazyListLayoutInfo(
    override val visibleItemsInfo: List<ScalingLazyListItemInfo>,
    override val viewportStartOffset: Int,
    override val viewportEndOffset: Int,
    override val totalItemsCount: Int
) : ScalingLazyListLayoutInfo

internal class DefaultScalingLazyListItemInfo(
    override val index: Int,
    override val unadjustedOffset: Int,
    override val offset: Int,
    override val size: Int,
    override val scale: Float,
    override val alpha: Float
) : ScalingLazyListItemInfo

@Immutable
internal data class ScaleAndAlpha(
    val scale: Float,
    val alpha: Float

) {
    companion object {
        internal val noScaling = ScaleAndAlpha(1.0f, 1.0f)
    }
}
