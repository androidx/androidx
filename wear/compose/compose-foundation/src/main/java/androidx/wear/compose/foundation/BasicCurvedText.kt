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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.LINEAR_TEXT_FLAG
import android.graphics.Paint.SUBPIXEL_TEXT_FLAG
import android.graphics.Path
import android.graphics.Typeface
import android.os.Build
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * [basicCurvedText] is a component allowing developers to easily write curved text following the
 * curvature a circle (usually at the edge of a circular screen). [basicCurvedText] can be only
 * created within a [CurvedLayout] since it's not a composable.
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedAndNormalText
 * @param text The text to display
 * @param modifier The [CurvedModifier] to apply to this curved text.
 * @param angularDirection Specify if the text is laid out clockwise or anti-clockwise, and if those
 *   needs to be reversed in a Rtl layout. If not specified, it will be inherited from the enclosing
 *   [curvedRow] or [CurvedLayout] See [CurvedDirection.Angular].
 * @param overflow How visual overflow should be handled. Note that this takes into account only
 *   explicit size curved modifiers in this element, to size this element matching the parent's, add
 *   a CurvedModifier.weight here.
 * @param style A @Composable factory to provide the style to use. This composable SHOULDN'T
 *   generate any compose nodes.
 */
public fun CurvedScope.basicCurvedText(
    text: String,
    modifier: CurvedModifier = CurvedModifier,
    angularDirection: CurvedDirection.Angular? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    style: @Composable () -> CurvedTextStyle = { CurvedTextStyle() },
): Unit =
    add(
        CurvedTextChild(
            text,
            curvedLayoutDirection.copy(overrideAngular = angularDirection).absoluteClockwise(),
            style,
            overflow,
        ),
        modifier,
    )

/**
 * [basicCurvedText] is a component allowing developers to easily write curved text following the
 * curvature a circle (usually at the edge of a circular screen). [basicCurvedText] can be only
 * created within a [CurvedLayout] since it's not a composable.
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedAndNormalText
 * @param text The text to display
 * @param style A style to use.
 * @param modifier The [CurvedModifier] to apply to this curved text.
 * @param angularDirection Specify if the text is laid out clockwise or anti-clockwise, and if those
 *   needs to be reversed in a Rtl layout. If not specified, it will be inherited from the enclosing
 *   [curvedRow] or [CurvedLayout] See [CurvedDirection.Angular].
 * @param overflow How visual overflow should be handled.
 */
public fun CurvedScope.basicCurvedText(
    text: String,
    style: CurvedTextStyle,
    modifier: CurvedModifier = CurvedModifier,
    angularDirection: CurvedDirection.Angular? = null,
    overflow: TextOverflow = TextOverflow.Clip,
): Unit = basicCurvedText(text, modifier, angularDirection, overflow) { style }

internal class CurvedTextChild(
    val text: String,
    val clockwise: Boolean = true,
    val style: @Composable () -> CurvedTextStyle = { CurvedTextStyle() },
    val overflow: TextOverflow,
) : CurvedChild() {
    private lateinit var delegate: CurvedTextDelegate
    private lateinit var actualStyle: CurvedTextStyle

    // We create a compose-ui node so that we can attach a11y info.
    private lateinit var placeable: Placeable

    @Composable
    override fun SubComposition(semanticProperties: CurvedSemanticProperties) {
        actualStyle = DefaultCurvedTextStyles + style()

        // Avoid recreating the delegate if possible, as it's expensive
        delegate = remember { CurvedTextDelegate() }
        delegate.UpdateFontIfNeeded(
            actualStyle.fontFamily,
            actualStyle.fontWeight,
            actualStyle.fontStyle,
            actualStyle.fontSynthesis,
        )

        val mergedSemantics =
            semanticProperties.merge(CurvedSemanticProperties(contentDescription = text))

        // Empty compose-ui node to attach a11y info.
        Box(Modifier.semantics { with(mergedSemantics) { applySemantics() } })
    }

    override fun CurvedMeasureScope.initializeMeasure(measurables: Iterator<Measurable>) {
        delegate.updateIfNeeded(
            text,
            clockwise,
            actualStyle.fontSize.toPx(),
            if (clockwise || actualStyle.letterSpacingCounterClockwise.isUnspecified)
                actualStyle.letterSpacing
            else actualStyle.letterSpacingCounterClockwise,
            density,
            if (actualStyle.lineHeight.isSpecified) actualStyle.lineHeight.toPx() else -1f,
            actualStyle.warpOffset,
        )

        // Size the compose-ui node reasonably.

        // Heuristic calculations of maxWidth:
        // 1. Find the text's center point. This is offset from the circle's center by a distance
        //    equal to:
        //    radius - (textHeight / 2)
        // 2. Construct a perpendicular line from the text's center point, extending it until it
        //    intersects the circle's circumference.
        // 3. Using the Pythagorean theorem, we can determine half of the desired width. We know:
        //    * The circle's radius
        //    * The distance of the text from the circle's center (calculated in step 1)
        // 4. The Pythagorean equation in this context is:
        //    radius^2 = (radius - textHeight/2)^2 + (maxWidth/2)^2
        // 5. Solving for maxWidth, we get:
        //    maxWidth = 2 * sqrt( textHeight * (radius - textHeight/4) )

        val height = delegate.textHeight.roundToInt()
        val maxWidth = 2 * sqrt(height * (radius - height / 4))
        val width = delegate.textWidth.coerceAtMost(maxWidth).roundToInt()

        // Measure the corresponding measurable.
        placeable =
            measurables
                .next()
                .measure(
                    Constraints(
                        minWidth = width,
                        maxWidth = width,
                        minHeight = height,
                        maxHeight = height,
                    )
                )
    }

    override fun doEstimateThickness(maxRadius: Float): Float = delegate.textHeight

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float,
    ): PartialLayoutInfo {
        val baselineRadius = parentOuterRadius - delegate.baseLinePosition
        // getMeasureOffset is 0 when there is no warping, and the warping offset when there is
        // this defines the horizontal line at which the text will maintain its width (lines closer
        // to the center will be shrunk and lines further away will be stretched).
        val realMeasureRadius =
            baselineRadius + delegate.getMeasureOffset() * (if (clockwise) 1f else -1f)
        return PartialLayoutInfo(
            delegate.textWidth / realMeasureRadius,
            parentOuterRadius,
            delegate.textHeight,
            baselineRadius,
        )
    }

    private var parentSweepRadians: Float = 0f

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset,
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
                actualStyle.background,
            )
        }
    }

    override fun (Placeable.PlacementScope).placeIfNeeded() =
        // clockwise doesn't matter, we have no content in placeable.
        place(placeable, layoutInfo!!, parentSweepRadians, clockwise = false)
}

/** Used to cache computations and objects with expensive construction (Android's Paint & Path) */
internal class CurvedTextDelegate {
    private var text: String = ""
    private var clockwise: Boolean = true
    private var fontSizePx: Float = 0f
    private var letterSpacing: TextUnit = TextUnit.Unspecified
    private var density: Float = 0f
    private var lastLineHeightPx: Float = 0f

    var textWidth by mutableFloatStateOf(0f)
    var textHeight by mutableFloatStateOf(0f)
    var baseLinePosition = 0f

    private var typeFace: State<Typeface?> = mutableStateOf(null)

    private val paint =
        TextPaint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            flags = flags or (SUBPIXEL_TEXT_FLAG + LINEAR_TEXT_FLAG)
        }
    private val backgroundPath = Path()

    var lastLayoutInfo: CurvedLayoutInfo? = null
    var lastParentSweepRadians: Float = 0f

    // Start with the android rendered, will switch to the warped rendered if possible/needed.
    private var textRender: CurvedTextRenderer = AndroidCurvedTextRenderer()
    private var prevWarping = CurvedTextStyle.WarpOffset.None
    private var warpRadiusOffset = 0f

    fun updateIfNeeded(
        text: String,
        clockwise: Boolean,
        fontSizePx: Float,
        letterSpacing: TextUnit,
        density: Float,
        lineHeightPx: Float,
        warpOffset: CurvedTextStyle.WarpOffset,
    ) {
        var needsUpdate = false

        if (Build.VERSION.SDK_INT >= 29) {
            // Defaults to not warping.
            val actualWarping = warpOffset.takeOrElse { CurvedTextStyle.WarpOffset.None }
            if (actualWarping != prevWarping) {
                prevWarping = actualWarping
                // Note that the Rendered may be stateful (computing things in the `preRender` to
                // (re)use during `render`), so we need our own instance.
                textRender =
                    if (actualWarping != CurvedTextStyle.WarpOffset.None) WarpedCurvedTextRenderer()
                    else AndroidCurvedTextRenderer()

                needsUpdate = true
                warpRadiusOffset =
                    if (actualWarping == CurvedTextStyle.WarpOffset.None) 0f
                    else actualWarping.determineWarpRadiusOffset(paint.ascent(), paint.descent())
            }
        }

        if (
            text != this.text ||
                clockwise != this.clockwise ||
                fontSizePx != this.fontSizePx ||
                letterSpacing != this.letterSpacing ||
                density != this.density ||
                lineHeightPx != lastLineHeightPx
        ) {
            this.text = text
            this.clockwise = clockwise
            this.fontSizePx = fontSizePx
            this.letterSpacing = letterSpacing
            this.density = density
            this.lastLineHeightPx = lineHeightPx

            paint.textSize = fontSizePx
            paint.letterSpacing =
                letterSpacing.let {
                    when (it.type) {
                        TextUnitType.Em -> it.value
                        TextUnitType.Sp -> {
                            val emWidth = paint.textSize * paint.textScaleX
                            if (emWidth == 0.0f) 0f else it.value * density / emWidth
                        }
                        // This includes the TextUnit.Unspecified case
                        else -> 0f
                    }
                }

            needsUpdate = true
        }

        if (needsUpdate) {
            updateMeasures()
            lastLayoutInfo = null // Ensure paths are recomputed
        }
    }

    /**
     * This is 0 when there is no warping, and the warping offset when there is, defining the
     * horizontal (before warping) line at which the text will maintain its width (lines closer to
     * the center will be shrink and lines further away will be stretched).
     */
    fun getMeasureOffset() = warpRadiusOffset

    @Composable
    fun UpdateFontIfNeeded(
        fontFamily: FontFamily?,
        fontWeight: FontWeight?,
        fontStyle: FontStyle?,
        fontSynthesis: FontSynthesis?,
    ) {
        val fontFamilyResolver = LocalFontFamilyResolver.current
        typeFace =
            remember(fontFamily, fontWeight, fontStyle, fontSynthesis, fontFamilyResolver) {
                derivedStateOf {
                    fontFamilyResolver
                        .resolveAsTypeface(
                            fontFamily,
                            fontWeight ?: FontWeight.Normal,
                            fontStyle ?: FontStyle.Normal,
                            fontSynthesis ?: FontSynthesis.All,
                        )
                        .value
                }
            }
        updateTypeFace()
    }

    private fun updateMeasures() {
        // We use measureText and not getTextBounds because the former correctly accounts for the
        // spacing added when rendering.
        textWidth = paint.measureText(text)

        // Note that ascent is negative, since it's above the baseline (which is at 0).
        val height = paint.fontMetrics.descent - paint.fontMetrics.ascent
        val diff = if (lastLineHeightPx >= 0f) (lastLineHeightPx - height) else 0f
        val actualAscent = -paint.fontMetrics.ascent + diff / 2
        val actualDescent = paint.fontMetrics.descent + diff / 2
        textHeight = actualAscent + actualDescent
        baseLinePosition = if (clockwise) actualAscent else actualDescent
    }

    private fun updateTypeFace() {
        val currentTypeface = typeFace.value
        if (currentTypeface != paint.typeface) {
            paint.typeface = currentTypeface
            updateMeasures()
            lastLayoutInfo = null // Ensure paths are recomputed
        }
    }

    private fun updatePathsIfNeeded(layoutInfo: CurvedLayoutInfo, parentSweepRadians: Float) {
        if (
            layoutInfo != lastLayoutInfo || abs(lastParentSweepRadians - parentSweepRadians) > 1e-4
        ) {
            lastLayoutInfo = layoutInfo
            lastParentSweepRadians = parentSweepRadians

            with(layoutInfo) {
                val clockwiseFactor = if (clockwise) 1f else -1f

                val sweepDegree =
                    min(sweepRadians, parentSweepRadians).toDegrees().coerceAtMost(360f)

                val centerX = centerOffset.x
                val centerY = centerOffset.y

                // TODO: move background drawing to a CurvedModifier
                backgroundPath.reset()
                backgroundPath.arcTo(
                    centerX - outerRadius,
                    centerY - outerRadius,
                    centerX + outerRadius,
                    centerY + outerRadius,
                    startAngleRadians.toDegrees(),
                    sweepDegree,
                    false,
                )
                backgroundPath.arcTo(
                    centerX - innerRadius,
                    centerY - innerRadius,
                    centerX + innerRadius,
                    centerY + innerRadius,
                    startAngleRadians.toDegrees() + sweepDegree,
                    -sweepDegree,
                    false,
                )
                backgroundPath.close()

                textRender.preRender(
                    centerOffset,
                    measureRadius,
                    clockwise,
                    sweepDegree,
                    startAngleRadians,
                    text,
                    paint,
                    warpRadiusOffset,
                )
            }
        }
    }

    fun DrawScope.doDraw(
        layoutInfo: CurvedLayoutInfo,
        parentSweepRadians: Float,
        overflow: TextOverflow,
        color: Color,
        background: Color,
    ) {
        updateTypeFace()
        updatePathsIfNeeded(layoutInfo, parentSweepRadians)

        drawIntoCanvas { canvas ->
            if (background.isSpecified && background != Color.Transparent) {
                paint.color = background.toArgb()
                canvas.nativeCanvas.drawPath(backgroundPath, paint)
            }

            paint.color = color.toArgb()
            val actualText =
                if (
                    // Float arithmetic can make the parentSweepRadians slightly smaller
                    layoutInfo.sweepRadians <= parentSweepRadians + 0.001f ||
                        overflow == TextOverflow.Visible
                ) {
                    text
                } else {
                    ellipsize(
                        text,
                        paint,
                        overflow == TextOverflow.Ellipsis,
                        (parentSweepRadians * layoutInfo.measureRadius).roundToInt(),
                    )
                }

            textRender.render(canvas.nativeCanvas, actualText, paint)
        }
    }

    private fun ellipsize(
        text: String,
        paint: TextPaint,
        addEllipsis: Boolean,
        ellipsizedWidth: Int,
    ): String {
        if (addEllipsis) {
            return TextUtils.ellipsize(
                    text,
                    paint,
                    ellipsizedWidth.toFloat(),
                    TextUtils.TruncateAt.END,
                )
                .toString()
        }

        val layout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, ellipsizedWidth)
                .setEllipsize(null)
                .setMaxLines(1)
                .build()

        // Cut text that it's too big when in TextOverFlow.Clip mode.
        return text.substring(0, layout.getLineEnd(0))
    }
}

internal interface CurvedTextRenderer {
    /*
    Prepare this render to draw the text, this is guaranteed to be called before render, any
    time some of the parameters change.
     */
    fun preRender(
        center: Offset,
        radius: Float,
        clockwise: Boolean,
        sweepDegree: Float,
        startAngleRadians: Float,
        text: String,
        paint: TextPaint,
        warpRadiusOffset: Float,
    )

    /*
    Actually render the text. This will be called during the draw phase.
     */
    fun render(canvas: Canvas, text: String, paint: TextPaint)
}

private class AndroidCurvedTextRenderer : CurvedTextRenderer {
    private val textPath = Path()

    override fun preRender(
        center: Offset,
        radius: Float,
        clockwise: Boolean,
        sweepDegree: Float,
        startAngleRadians: Float,
        text: String,
        paint: TextPaint,
        warpRadiusOffset: Float,
    ) {
        textPath.reset()
        textPath.addArc(
            center.x - radius,
            center.y - radius,
            center.x + radius,
            center.y + radius,
            startAngleRadians.toDegrees() + (if (clockwise) 0f else sweepDegree),
            if (clockwise) sweepDegree else -sweepDegree,
        )
    }

    override fun render(canvas: Canvas, text: String, paint: TextPaint) {
        canvas.drawTextOnPath(text, textPath, 0f, 0f, paint)
    }
}
