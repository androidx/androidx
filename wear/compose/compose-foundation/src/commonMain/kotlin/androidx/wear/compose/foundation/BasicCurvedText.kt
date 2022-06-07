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
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * [basicCurvedText] is a component allowing developers to easily write curved text following
 * the curvature a circle (usually at the edge of a circular screen).
 * [basicCurvedText] can be only created within the [CurvedLayout] since it's not a not a
 * composable.
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedAndNormalText
 *
 * @param text The text to display
 * @param modifier The [CurvedModifier] to apply to this curved text.
 * @param angularDirection Specify if the text is laid out clockwise or anti-clockwise, and if
 * those needs to be reversed in a Rtl layout.
 * If not specified, it will be inherited from the enclosing [curvedRow] or [CurvedLayout]
 * See [CurvedDirection.Angular].
 * @param overflow How visual overflow should be handled.
 * @param style A @Composable factory to provide the style to use. This composable SHOULDN'T
 * generate any compose nodes.
 */
public fun CurvedScope.basicCurvedText(
    text: String,
    modifier: CurvedModifier = CurvedModifier,
    angularDirection: CurvedDirection.Angular? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    style: @Composable () -> CurvedTextStyle = { CurvedTextStyle() }
) = add(CurvedTextChild(
    text,
    curvedLayoutDirection.copy(overrideAngular = angularDirection).absoluteClockwise(),
    style,
    overflow
), modifier)

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
 * @param modifier The [CurvedModifier] to apply to this curved text.
 * @param angularDirection Specify if the text is laid out clockwise or anti-clockwise, and if
 * those needs to be reversed in a Rtl layout.
 * If not specified, it will be inherited from the enclosing [curvedRow] or [CurvedLayout]
 * See [CurvedDirection.Angular].
 * @param overflow How visual overflow should be handled.
 */
public fun CurvedScope.basicCurvedText(
    text: String,
    style: CurvedTextStyle,
    modifier: CurvedModifier = CurvedModifier,
    angularDirection: CurvedDirection.Angular? = null,
    overflow: TextOverflow = TextOverflow.Clip,
) = basicCurvedText(text, modifier, angularDirection, overflow) { style }

internal class CurvedTextChild(
    val text: String,
    val clockwise: Boolean = true,
    val style: @Composable () -> CurvedTextStyle = { CurvedTextStyle() },
    val overflow: TextOverflow
) : CurvedChild() {
    private lateinit var delegate: CurvedTextDelegate
    private lateinit var actualStyle: CurvedTextStyle

    @Composable
    override fun SubComposition() {
        actualStyle = DefaultCurvedTextStyles + style()
        // Avoid recreating the delegate if possible, as it's expensive
        delegate = remember { CurvedTextDelegate() }
    }

    override fun CurvedMeasureScope.initializeMeasure(
        measurables: List<Measurable>,
        index: Int
    ): Int {
        delegate.updateIfNeeded(
            text,
            clockwise,
            actualStyle.fontSize.toPx(),
            actualStyle.fontWeight
        )
        return index // No measurables where mapped.
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

    private var parentSweepRadians: Float = 0f

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ): Float {
        this.parentSweepRadians = parentSweepRadians
        return super.doAngularPosition(parentStartAngleRadians, parentSweepRadians, centerOffset)
    }

    override fun DrawScope.draw() {
        with(delegate) {
            doDraw(
                layoutInfo!!,
                parentSweepRadians,
                overflow,
                actualStyle.color,
                actualStyle.background
            )
        }
    }
}

internal expect class CurvedTextDelegate() {
    var textWidth: Float
    var textHeight: Float
    var baseLinePosition: Float

    fun updateIfNeeded(
        text: String,
        clockwise: Boolean,
        fontSizePx: Float,
        fontWeight: FontWeight?
    )

    fun DrawScope.doDraw(
        layoutInfo: CurvedLayoutInfo,
        parentSweepRadians: Float,
        overflow: TextOverflow,
        color: Color,
        background: Color
    )
}
