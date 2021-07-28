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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Constraints
import kotlin.math.min

/**
 * Parameters to control the scaling of the contents of a [ScalingLazyColumn]. The contents
 * of a [ScalingLazyColumn] are scaled down (made smaller) as they move further from the center
 * of the viewport. This scaling gives a "fisheye" effect with the contents in the center being
 * larger and therefore more prominent.
 *
 * Items in the center of the component's viewport will be full sized and with normal transparency.
 * As items move further from the center of the viewport they get smaller and become transparent.
 *
 * The scaling parameters allow for larger items to start being scaled closer to the center than
 * smaller items. This allows for larger items to scale over a bigger transition area giving a more
 * visually pleasing effect.
 *
 * Scaling transitions take place between a transition line and the edge of the screen. The trigger
 * for an item to start being scaled is when its most central edge, the item's edge that is furthest
 * from the screen edge, passing across the item's transition line. The amount of scaling to apply is
 * a function of the how far the item has moved through its transition area. An interpolator is
 * applied to allow for the scaling to follow a bezier curve.
 *
 * There are 4 properties that are used to determine an item's scaling transition point.
 *
 * [maxTransitionArea] and [minTransitionArea] define the range in which all item scaling lines sit.
 * The largest items will start to scale at the [maxTransitionArea] and the smallest items will
 * start to scale at the [minTransitionArea].
 *
 * The [minTransitionArea] and [maxTransitionArea] apply either side of the center line of the
 * viewport creating 2 transition areas one at the top/start the other at the bottom/end.
 * So a [maxTransitionArea] value of 0.6f on a Viewport of size 320 dp will give start transition
 * line for scaling at (320 / 2) * 0.6 = 96 dp from the top/start and bottom/end edges of the
 * viewport. Similarly [minTransitionArea] gives the point at which the scaling transition area
 * ends, e.g. a value of 0.2 with the same 320 dp screen gives an min scaling transition area line
 * of (320 / 2) * 0.2 = 32 dp from top/start and bottom/end. So in this example we have two
 * transition areas in the ranges [32.dp,96.dp] and [224.dp (320.dp-96.d),288.dp (320.dp-32.dp)].
 *
 * Deciding for a specific content item exactly where its transition line will be within the
 * ([minTransitionArea], [maxTransitionArea]) transition area is determined by its height as a
 * fraction of the viewport height and the properties [minElementHeight] and [maxElementHeight],
 * also defined as a fraction of the viewport height.
 *
 * If an item is smaller than [minElementHeight] it is treated as is [minElementHeight] and if
 * larger than [maxElementHeight] then it is treated as if [maxElementHeight].
 *
 * Given the size of an item where it sits between [minElementHeight] and [maxElementHeight] is used
 * to determine what fraction of the transition area to use. For example if [minElementHeight] is
 * 0.2 and [maxElementHeight] is 0.8 then a component item that is 0.4 (40%) of the size of the
 * viewport would start to scale when it was 0.333 (33.3%) of the way through the transition area,
 * (0.4 - 0.2) / (0.8 - 0.2) = 0.2 / 0.6 = 0.333.
 *
 * Taking the example transition area above that means that the scaling line for the item would be a
 * third of the way between 32.dp and 96.dp. 32.dp + ((96.dp-32.dp) * 0.333) = 53.dp. So this item
 * would start to scale when it moved from the center across the 53.dp line and its scaling would be
 * between 53.dp and 0.dp.
 */
public interface ScalingParams {
    /**
     * What fraction of the full size of the item to scale it by when most
     * scaled, e.g. at the viewport edge. A value between [0.0,1.0], so a value of 0.2f means
     * to scale an item to 20% of its normal size.
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val edgeScale: Float

    /**
     * What fraction of the full transparency of the item to scale it by when
     * most scaled, e.g. at the viewport edge. A value between [0.0,1.0], so a value of 0.2f
     * means to set the alpha of an item to 20% of its normal value.
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val edgeAlpha: Float

    /**
     * The minimum element height as a fraction of the viewport size to use
     * for determining the transition point within ([minTransitionArea], [maxTransitionArea]) that a
     * given content item will start to be scaled. Items smaller than [minElementHeight] will be
     * treated as if [minElementHeight]. Must be less than or equal to [maxElementHeight].
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val minElementHeight: Float

    /**
     * The minimum element height as a fraction of the viewport size to use
     * for determining the transition point within ([minTransitionArea], [maxTransitionArea]) that a
     * given content item will start to be scaled. Items smaller than [minElementHeight] will be
     * treated as if [minElementHeight]. Must be less than or equal to [maxElementHeight].
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val maxElementHeight: Float

    /**
     * The lower bound of the scaling transition area, closest to the edge
     * of the component. Defined as a fraction of the distance between the viewport center line and
     * viewport edge of the component. Must be less than or equal to [maxTransitionArea].
     */
//    @FloatRange(
//        fromInclusive = true, from = 0.0, toInclusive = true, to = 1.0
//    )
    val minTransitionArea: Float

    /**
     * The upper bound of the scaling transition area, closest to the center
     * of the component. Defined as a fraction of the distance between the viewport center line and
     * viewport edge of the component. Must be greater
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
     * Determine the offset/extra padding (in pixels) that is used to define a space for additional
     * items that should be considered for drawing on the screen as the scaling of the visible
     * items in viewport is calculated. This additional padding area means that more items will
     * materialized and therefore be in scope for being drawn in the viewport if the scaling of
     * other elements means that there is additional space at the top and bottom of the viewport
     * that can be used. The default value is a fifth of the viewport height allowing an
     * additional 20% of the viewport height above and below the viewport.
     *
     * @param viewportConstraints the viewports constraints
     */
    public fun resolveViewportVerticalOffset(viewportConstraints: Constraints): Int
}

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

    val viewPortHeightPx = (viewPortEndPx - viewPortStartPx).toFloat()
    val itemHeightPx = (itemBottomPx - itemTopPx).toFloat()

    /*
     * Calculate the position of the edge of the item closest to the center line of the viewport as
     * a fraction of the viewport. The [itemEdgePx] and [scrollPositionPx] values are in pixels.
     */
    val itemEdgeAsFractionOfViewport =
        min(itemBottomPx - viewPortStartPx, viewPortEndPx - itemTopPx) / viewPortHeightPx

    val heightAsFractionOfViewPort = itemHeightPx / viewPortHeightPx
    if (itemEdgeAsFractionOfViewport > 0.0f && itemEdgeAsFractionOfViewport < 1.0f) {
        // Work out the scaling line based on size, this is a value between 0.0..1.0
        val sizeRatio: Float =
            (
                (heightAsFractionOfViewPort - scalingParams.minElementHeight) /
                    (scalingParams.maxElementHeight - scalingParams.minElementHeight)
                ).coerceIn(0f, 1f)

        val scalingLineAsFractionOfViewPort =
            scalingParams.minTransitionArea +
                (scalingParams.maxTransitionArea - scalingParams.minTransitionArea) *
                sizeRatio

        if (itemEdgeAsFractionOfViewport < scalingLineAsFractionOfViewPort) {
            // We are scaling
            val fractionOfDiffToApplyRaw =
                (scalingLineAsFractionOfViewPort - itemEdgeAsFractionOfViewport) /
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

@Immutable
internal data class ScaleAndAlpha(
    val scale: Float,
    val alpha: Float
)

@Immutable
internal data class ScaleAlphaAndStartPosition(val scale: Float, val alpha: Float, val top: Int)
