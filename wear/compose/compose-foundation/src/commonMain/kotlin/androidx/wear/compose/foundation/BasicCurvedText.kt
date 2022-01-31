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

package androidx.wear.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apply additional space along each edge of the content in [Dp].
 * See the [ArcPaddingValues] factories for convenient ways to
 * build [ArcPaddingValues].
 */
@Stable
public interface ArcPaddingValues {
    /**
     * Padding in the outward direction from the center of the [CurvedLayout]
     */
    fun calculateOuterPadding(): Dp

    /**
     * Padding in the inwards direction towards the center of the [CurvedLayout]
     */
    fun calculateInnerPadding(): Dp

    /**
     * Padding added at the start of the component.
     */
    fun calculateStartPadding(): Dp

    /**
     * Padding added at the end of the component.
     */
    fun calculateEndPadding(): Dp
}

/**
 * Apply additional space along each edge of the content in [Dp]. Note that the start and end
 * edges will be determined by the direction (clockwise or counterclockwise)
 *
 * @param outer Padding in the outward direction from the center of the
 * [CurvedLayout]
 * @param inner Padding in the inwards direction towards the center of the [CurvedLayout]
 * @param start Padding added at the start of the component.
 * @param end Padding added at the end of the component.
 */
public fun ArcPaddingValues(
    outer: Dp = 0.dp,
    inner: Dp = 0.dp,
    start: Dp = 0.dp,
    end: Dp = 0.dp
): ArcPaddingValues =
    ArcPaddingValuesImpl(outer, inner, start, end)

/**
 * Apply [all] dp of additional space along each edge of the content.
 */
public fun ArcPaddingValues(all: Dp): ArcPaddingValues = ArcPaddingValuesImpl(all, all, all, all)

/**
 * Apply [radial] dp of additional space on the edges towards and away from the center, and
 * [angular] dp before and after the component.
 */
public fun ArcPaddingValues(radial: Dp = 0.dp, angular: Dp = 0.dp): ArcPaddingValues =
    ArcPaddingValuesImpl(radial, radial, angular, angular)

@Stable
internal class ArcPaddingValuesImpl(val outer: Dp, val inner: Dp, val start: Dp, val end: Dp) :
    ArcPaddingValues {
    override fun equals(other: Any?): Boolean {
        return other is ArcPaddingValuesImpl &&
            outer == other.outer &&
            inner == other.inner &&
            start == other.start &&
            end == other.end
    }

    override fun hashCode() = ((outer.hashCode() * 31 + inner.hashCode()) * 31 + start.hashCode()) *
        31 + end.hashCode()

    override fun toString(): String {
        return "ArcPaddingValuesImpl(outer=$outer, inner=$inner, start=$start, end=$end)"
    }

    override fun calculateOuterPadding() = outer
    override fun calculateInnerPadding() = inner
    override fun calculateStartPadding() = start
    override fun calculateEndPadding() = end
}

/**
 * [basicCurvedText] is a component allowing developers to easily write curved text following
 * the curvature a circle (usually at the edge of a circular screen).
 * [basicCurvedText] can be only created within the [CurvedLayout] since it's not a not a
 * composable.
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedAndNormalText
 *
 * @param text The text to display
 * @param clockwise The direction the text follows (default is true). Usually text at the top of the
 * screen goes clockwise, and text at the bottom goes counterclockwise.
 * @param contentArcPadding Allows to specify additional space along each "edge" of the content in
 * [Dp] see [ArcPaddingValues]
 * @param style A @Composable factory to provide the style to use. This composable SHOULDN'T
 * generate any compose nodes.
 */
public fun CurvedScope.basicCurvedText(
    text: String,
    clockwise: Boolean = true,
    contentArcPadding: ArcPaddingValues = ArcPaddingValues(0.dp),
    style: @Composable () -> CurvedTextStyle = { CurvedTextStyle() }
) = add(CurvedTextChild(text, clockwise, contentArcPadding, style))

/**
 * [basicCurvedText] is a component allowing developers to easily write curved text following
 * the curvature a circle (usually at the edge of a circular screen).
 * [basicCurvedText] can be only created within the [CurvedLayout] since it's not a not a
 * composable.
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedAndNormalText
 *
 * @param text The text to display
 * @param style A style to use.
 * @param clockwise The direction the text follows (default is true). Usually text at the top of the
 * screen goes clockwise, and text at the bottom goes counterclockwise.
 * @param contentArcPadding Allows to specify additional space along each "edge" of the content in
 * [Dp] see [ArcPaddingValues]
 */
public fun CurvedScope.basicCurvedText(
    text: String,
    style: CurvedTextStyle,
    clockwise: Boolean = true,
    // TODO: reimplement as modifiers
    contentArcPadding: ArcPaddingValues = ArcPaddingValues(0.dp)
) = basicCurvedText(text, clockwise, contentArcPadding) { style }

internal class CurvedTextChild(
    val text: String,
    val clockwise: Boolean = true,
    val contentArcPadding: ArcPaddingValues = ArcPaddingValues(0.dp),
    val style: @Composable () -> CurvedTextStyle = { CurvedTextStyle() }
) : CurvedChild() {
    private val delegate: CurvedTextDelegate = CurvedTextDelegate()
    private lateinit var actualStyle: CurvedTextStyle

    override fun MeasureScope.initializeMeasure(
        measurables: List<Measurable>,
        index: Int
    ): Int {
        // TODO: move padding into a CurvedModifier
        val arcPaddingPx = ArcPaddingPx(
            contentArcPadding.calculateOuterPadding().toPx(),
            contentArcPadding.calculateInnerPadding().toPx(),
            contentArcPadding.calculateStartPadding().toPx(),
            contentArcPadding.calculateEndPadding().toPx()
        )
        delegate.updateIfNeeded(text, clockwise, actualStyle.fontSize.toPx(), arcPaddingPx)
        return index // No measurables where mapped.
    }

    @Composable
    override fun SubComposition() {
        actualStyle = DefaultCurvedTextStyles + style()
    }

    override fun doEstimateThickness(maxRadius: Float): Float = delegate.textHeight

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float
    ): PartialLayoutInfo {
        val measureRadius = parentOuterRadius - delegate.baseLinePosition
        return PartialLayoutInfo(
            delegate.textWidth / measureRadius,
            parentOuterRadius,
            delegate.textHeight,
            measureRadius
        )
    }

    override fun DrawScope.draw() {
        with(delegate) {
            doDraw(layoutInfo!!, actualStyle.color, actualStyle.background)
        }
    }
}

internal data class ArcPaddingPx(
    val outer: Float,
    val inner: Float,
    val before: Float,
    val after: Float
)

internal expect class CurvedTextDelegate() {
    var textWidth: Float
    var textHeight: Float
    var baseLinePosition: Float

    fun updateIfNeeded(
        text: String,
        clockwise: Boolean,
        fontSizePx: Float,
        arcPaddingPx: ArcPaddingPx
    )

    fun DrawScope.doDraw(layoutInfo: CurvedLayoutInfo, color: Color, background: Color)
}
