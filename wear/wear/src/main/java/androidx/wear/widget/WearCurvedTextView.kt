/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.widget

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.wear.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.properties.Delegates

/**
 * A WearCurvedTextView is a component allowing developers to easily write curved text following
 * the curvature of the largest circle that can be inscribed in the view. WearArcLayout could be
 * used to concatenate multiple curved texts, also layout together with other widgets such as icons.
 */
public class WearCurvedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), WearArcLayout.ArcLayoutWidget {

    private val path = Path()
    private val bgPath = Path()
    private val paint = TextPaint().apply { setAntiAlias(true) }
    private val bounds = Rect()
    private val bgBounds = Rect()
    private var dirty = true
    private var textToDraw: String = ""
    private var pathRadius: Float = 0f
    private var textSweepDegrees: Float = 0f
    private var backgroundSweepDegrees: Float = 359.9f
    private var parentClockwise: Boolean? = null
    private var lastUsedTextAlignment = -1
    private var localRotateAngle = 0f
    private var parentRotateAngle = 0f

    private companion object {
        // make 0 degree at 12 o'clock, since canvas assumes 0 degree is 3 o'clock
        private const val ANCHOR_DEGREE_OFFSET = -90f
        private const val UNSET_ANCHOR_DEGREE = -1f
        private const val UNSET_ANCHOR_TYPE = -1
        private const val UNSET_SWEEP_DEGREE = -1f
        private const val DEFAULT_TEXT_SIZE = 24f
        private const val DEFAULT_TEXT_COLOR = Color.WHITE
        private const val DEFAULT_TEXT_STYLE = Typeface.NORMAL
        private const val DEFAULT_CLOCKWISE = true
        private const val FONT_WEIGHT_MAX = 1000
        private const val ITALIC_SKEW_X = -0.25f
    }

    private fun doUpdate() {
        dirty = true
        updatePaint()
        requestLayout()
        postInvalidate()
    }

    private fun doRedraw() {
        dirty = true
        postInvalidate()
    }

    /**
     * Change of the value triggers paint update, re-layout and re-draw
     */
    private fun <T> makeUpdateDelegate(v: T) =
        Delegates.observable(v) { _, o, n -> if (n != o) doUpdate() }

    /**
     * Change of the value triggers re-draw
     */
    private fun <T> makeRedrawDelegate(v: T) =
        Delegates.observable(v) { _, o, n -> if (n != o) doRedraw() }

    public var anchorType: Int by makeUpdateDelegate(UNSET_ANCHOR_TYPE)
    public var anchorAngleDegrees: Float by makeRedrawDelegate(UNSET_ANCHOR_DEGREE)
    public var sweepDegrees: Float by makeUpdateDelegate(UNSET_SWEEP_DEGREE)
    public var text: String by makeUpdateDelegate("")
    public var textSize: Float by makeUpdateDelegate(DEFAULT_TEXT_SIZE)
    public var typeface: Typeface? by makeUpdateDelegate(null)
    public var clockwise: Boolean by makeUpdateDelegate(DEFAULT_CLOCKWISE)
    public var textColor: Int by makeRedrawDelegate(DEFAULT_TEXT_COLOR)
    public var ellipsize: TextUtils.TruncateAt? by makeRedrawDelegate(null)
    public var letterSpacing: Float by makeUpdateDelegate(0f)
    public var fontFeatureSettings: String? by makeUpdateDelegate(null)
    public var fontVariationSettings: String? by makeUpdateDelegate(null)

    override fun getSweepAngleDegrees(): Float = backgroundSweepDegrees
    override fun getThicknessPx(): Int =
        (paint.fontMetrics.descent - paint.fontMetrics.ascent).toInt()

    /**
     * @throws IllegalArgumentException if the anchorType and/or anchorAngleDegrees attributes
     * were set for a widget in WearArcLayout
     */
    override fun checkInvalidAttributeAsChild(clockwise: Boolean) {
        parentClockwise = clockwise

        if (anchorType != UNSET_ANCHOR_TYPE) {
            throw IllegalArgumentException(
                "WearCurvedTextView shall not set anchorType value when added into WearArcLayout"
            )
        }

        if (anchorAngleDegrees != UNSET_ANCHOR_DEGREE) {
            throw IllegalArgumentException(
                "WearCurvedTextView shall not set anchorAngleDegrees value when added into " +
                    "WearArcLayout"
            )
        }
    }

    override fun handleLayoutRotate(angle: Float): Boolean {
        parentRotateAngle = angle
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        doUpdate()
    }

    private fun updatePaint() {
        paint.textSize = textSize
        paint.getTextBounds(text, 0, text.length, bounds)

        // Note that ascent is negative.
        pathRadius = min(width, height) / 2f +
            if (clockwise) paint.fontMetrics.ascent - paddingTop
            else -paint.fontMetrics.descent - paddingBottom.toFloat()
        textSweepDegrees =
            (getWidthSelf() / pathRadius / Math.PI.toFloat() * 180f).coerceAtMost(360f)
        backgroundSweepDegrees =
            if (sweepDegrees == UNSET_SWEEP_DEGREE) textSweepDegrees
            else sweepDegrees
    }

    private fun getWidthSelf() = bounds.width().toFloat() + paddingLeft + paddingRight

    private fun ellipsize(ellipsizedWidth: Int): String {
        val layoutBuilder =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, ellipsizedWidth)
        layoutBuilder.setEllipsize(ellipsize)
        layoutBuilder.setMaxLines(1)
        val layout = layoutBuilder.build()

        // Cut text that it's too big even if no ellipsize mode is provided.
        if (ellipsize == null) {
            return text.substring(0, layout.getLineEnd(0))
        }

        val ellipsisCount = layout.getEllipsisCount(0)
        if (ellipsisCount == 0) {
            return text
        }

        val ellipsisStart = layout.getEllipsisStart(0)

        return text.replaceRange(ellipsisStart, ellipsisStart + ellipsisCount, "\u2026")
    }

    private fun updatePathsIfNeeded(withBackground: Boolean) {
        // The dirty flag is not set when properties we inherit from View are modified
        if (!dirty && textAlignment == lastUsedTextAlignment) {
            return
        }
        dirty = false
        lastUsedTextAlignment = textAlignment
        paint.textSize = textSize

        val maxSweepDegrees = if (sweepDegrees == UNSET_SWEEP_DEGREE) 360f else sweepDegrees
        if (textSweepDegrees <= maxSweepDegrees) {
            textToDraw = text
        } else {
            textToDraw = ellipsize(
                (maxSweepDegrees / 180f * Math.PI * pathRadius - paddingLeft - paddingRight).toInt()
            )
            textSweepDegrees = maxSweepDegrees
        }

        val clockwiseFactor = if (clockwise) 1f else -1f
        val parentClockwiseFactor = if (parentClockwise ?: clockwise) 1f else -1f

        val alignmentFactor = when (textAlignment) {
            TEXT_ALIGNMENT_TEXT_START -> 0f
            TEXT_ALIGNMENT_VIEW_START -> 0f
            TEXT_ALIGNMENT_TEXT_END -> 1f
            TEXT_ALIGNMENT_VIEW_END -> 1f
            else -> 0.5f // TEXT_ALIGNMENT_CENTER
        }

        val anchorTypeFactor = when (anchorType) {
            WearArcLayout.ANCHOR_START -> 0f
            WearArcLayout.ANCHOR_CENTER -> 0.5f
            WearArcLayout.ANCHOR_END -> 1f
            else -> if (parentClockwiseFactor == clockwiseFactor) 0f else -1f
        }

        val actualAnchorDegree =
            (if (anchorAngleDegrees == UNSET_ANCHOR_DEGREE) 0f else anchorAngleDegrees) +
                ANCHOR_DEGREE_OFFSET

        // Always draw the curved text on top center, then rotate the canvas to the right position
        val backgroundStartAngle = - 0.5f * backgroundSweepDegrees + ANCHOR_DEGREE_OFFSET
        localRotateAngle = (
            actualAnchorDegree -
                parentClockwiseFactor * anchorTypeFactor * backgroundSweepDegrees -
                backgroundStartAngle
            )

        val textStartAngle = backgroundStartAngle + clockwiseFactor * (
            alignmentFactor * (backgroundSweepDegrees - textSweepDegrees) +
                paddingLeft / pathRadius / Math.PI * 180
            ).toFloat()

        val centerX = width / 2f
        val centerY = height / 2f
        path.reset()
        path.addArc(
            centerX - pathRadius,
            centerY - pathRadius,
            centerX + pathRadius,
            centerY + pathRadius,
            textStartAngle,
            clockwiseFactor * textSweepDegrees
        )

        if (withBackground) {
            bgPath.reset()
            val radius1 = pathRadius - clockwiseFactor * paint.fontMetrics.descent
            val radius2 = pathRadius - clockwiseFactor * paint.fontMetrics.ascent
            bgPath.arcTo(
                centerX - radius2,
                centerY - radius2,
                centerX + radius2,
                centerY + radius2,
                backgroundStartAngle,
                clockwiseFactor * backgroundSweepDegrees, false
            )
            bgPath.arcTo(
                centerX - radius1,
                centerY - radius1,
                centerX + radius1,
                centerY + radius1,
                backgroundStartAngle + clockwiseFactor * backgroundSweepDegrees,
                -clockwiseFactor * backgroundSweepDegrees, false
            )
            bgPath.close()

            val angle1 = backgroundStartAngle
            val angle2 = backgroundStartAngle + clockwiseFactor * backgroundSweepDegrees
            val pointsRadial =
                listOf(radius1, radius2).flatMap { listOf(it to angle1, it to angle2) }
            val x = pointsRadial.map { (r, a) -> (centerX + r * cos(a * PI / 180)).toInt() }
            val y = pointsRadial.map { (r, a) -> (centerY + r * sin(a * PI / 180)).toInt() }
            bgBounds.left = x.minOrNull()!!.toInt()
            bgBounds.top = (centerY - radius2).toInt() // 0 degree angle value to cover the arc top
            bgBounds.right = x.maxOrNull()!!.toInt()
            bgBounds.bottom = y.maxOrNull()!!.toInt()
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.save()

        var withBackground = getBackground() != null
        updatePathsIfNeeded(withBackground)
        canvas.rotate(localRotateAngle + parentRotateAngle, width / 2f, height / 2f)

        if (withBackground) {
            canvas.clipPath(bgPath)
            getBackground().setBounds(bgBounds)
        }
        super.draw(canvas)

        canvas.restore()
    }

    private fun getTintFilter(): PorterDuffColorFilter? {
        val tintColor = getBackgroundTintList()
        val tintMode = getBackgroundTintMode() ?: PorterDuff.Mode.SRC_IN

        if (tintColor == null) {
            return null
        }
        val color = tintColor.getDefaultColor()
        return PorterDuffColorFilter(color, tintMode)
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = textColor
        paint.style = Paint.Style.FILL
        canvas.drawTextOnPath(textToDraw, path, 0f, 0f, paint)
    }

    /**
     * Sets the Typeface taking into account the given attributes.
     *
     * @param familyName family name string, e.g. "serif"
     * @param typefaceIndex an index of the typeface enum, e.g. SANS, SERIF.
     * @param style a typeface style
     * @param weight a weight value for the Typeface or -1 if not specified.
     */
    private fun setTypefaceFromAttrs(
        familyName: String?,
        typefaceIndex: Int,
        style: Int,
        weight: Int
    ) {
        // typeface is ignored when font family is set
        val computedTypeface = familyName?.let { Typeface.create(familyName, Typeface.NORMAL) }
            ?: when (typefaceIndex) {
                1 -> Typeface.SANS_SERIF
                2 -> Typeface.SERIF
                3 -> Typeface.MONOSPACE
                else -> null
            }

        resolveStyleAndSetTypeface(computedTypeface, style, weight)
    }

    private fun resolveStyleAndSetTypeface(tf: Typeface?, style: Int, weight: Int) {
        if (weight >= 0 && Build.VERSION.SDK_INT >= 28) {
            val _weight = min(FONT_WEIGHT_MAX, weight)
            val italic = (style and Typeface.ITALIC) != 0
            typeface = Api28Impl.createTypeface(tf, _weight, italic)
            paint.setTypeface(typeface)
        } else {
            setTypeface(tf, style)
        }
    }

    /**
     * Sets the typeface and style in which the text should be displayed, and turns on the fake
     * bold and italic bits in the Paint if the Typeface that you provided does not have all the
     * bits in the style that you specified.
     */
    private fun setTypeface(tf: Typeface?, style: Int) {
        if (style > 0) {
            var _tf = tf?.let { Typeface.create(it, style) } ?: Typeface.defaultFromStyle(style)

            if (paint.typeface != _tf) {
                paint.typeface = _tf
                typeface = _tf
            }
            // now compute what (if any) algorithmic styling is needed
            val typefaceStyle: Int = _tf?.style ?: 0
            val need: Int = style and typefaceStyle.inv() // style & ~typefaceStyle
            paint.isFakeBoldText = (need and Typeface.BOLD) != 0
            paint.textSkewX = if ((need and Typeface.ITALIC) != 0) ITALIC_SKEW_X else 0f
        } else {
            paint.isFakeBoldText = false
            paint.textSkewX = 0f
            if (paint.typeface != tf) {
                paint.typeface = tf
            }
        }
    }

    /**
     * Set of attribute that can be defined in a Text Appearance.
     */
    private class TextAppearanceAttributes {
        var textColor: ColorStateList? = null
        var textSize: Float = DEFAULT_TEXT_SIZE
        var fontFamily: String? = null
        var fontFamilyExplicit: Boolean = false
        var fontTypeface: Typeface? = null
        var typefaceIndex: Int = -1
        var textStyle: Int = DEFAULT_TEXT_STYLE
        var fontWeight: Int = -1
        var letterSpacing: Float = 0f
        var fontFeatureSettings: String? = null
        var fontVariationSettings: String? = null
    }

    /**
     * Sets the textColor, size, style, font etc from the specified TextAppearanceAttributes
     */
    private fun applyTextAppearance(attributes: TextAppearanceAttributes) {
        attributes.textColor ?. let { textColor = it.getDefaultColor() }
        if (attributes.textSize != -1f) {
            textSize = attributes.textSize
        }

        setTypefaceFromAttrs(
            attributes.fontFamily,
            attributes.typefaceIndex,
            attributes.textStyle,
            attributes.fontWeight
        )

        paint.setLetterSpacing(attributes.letterSpacing)
        letterSpacing = attributes.letterSpacing
        paint.setFontFeatureSettings(attributes.fontFeatureSettings)
        fontFeatureSettings = attributes.fontFeatureSettings
        if (Build.VERSION.SDK_INT >= 26 ) {
            Api26Impl.paintSetFontVariationSettings(paint, attributes.fontVariationSettings)
        }
        fontVariationSettings = attributes.fontVariationSettings
    }

    /**
     * Read the Text Appearance attributes from a given TypedArray and set its values to the
     * given set. If the TypedArray contains a value that already set in the given attributes,
     * that will be overridden.
     */
    private fun readTextAppearance(
        appearance: TypedArray,
        attributes: TextAppearanceAttributes,
        isTextAppearance: Boolean
    ) {
        appearance.apply {
            getColorStateList(
                if (isTextAppearance) R.styleable.TextAppearance_android_textColor
                else R.styleable.WearCurvedTextView_android_textColor
            ) ?. let { color ->
                attributes.textColor = color
            }

            attributes.textSize = getDimension(
                if (isTextAppearance) R.styleable.TextAppearance_android_textSize
                else R.styleable.WearCurvedTextView_android_textSize,
                attributes.textSize
            )

            attributes.textStyle = getInt(
                if (isTextAppearance) R.styleable.TextAppearance_android_textStyle
                else R.styleable.WearCurvedTextView_android_textStyle,
                attributes.textStyle
            )

            // make sure that the typeface attribute is read before fontFamily attribute
            attributes.typefaceIndex = getInt(
                if (isTextAppearance) R.styleable.TextAppearance_android_typeface
                else R.styleable.WearCurvedTextView_android_typeface,
                attributes.typefaceIndex
            )
            if (attributes.typefaceIndex != -1 && !attributes.fontFamilyExplicit) {
                attributes.fontFamily = null
            }

            var attr = if (isTextAppearance) R.styleable.TextAppearance_android_fontFamily
            else R.styleable.WearCurvedTextView_android_fontFamily
            if (hasValue(attr)) {
                attributes.fontFamily = getString(attr)
                attributes.fontFamilyExplicit = !isTextAppearance
            }

            attributes.fontWeight = getInt(
                if (isTextAppearance) R.styleable.TextAppearance_android_textFontWeight
                else R.styleable.WearCurvedTextView_android_textFontWeight,
                attributes.fontWeight
            )

            attributes.letterSpacing = getFloat(
                if (isTextAppearance) R.styleable.TextAppearance_android_letterSpacing
                else R.styleable.WearCurvedTextView_android_letterSpacing,
                attributes.letterSpacing
            )

            getString(
                if (isTextAppearance) R.styleable.TextAppearance_android_fontFeatureSettings
                else R.styleable.WearCurvedTextView_android_fontFeatureSettings
            ) ?. let { value ->
                attributes.fontFeatureSettings = value
            }

            getString(
                if (isTextAppearance) R.styleable.TextAppearance_android_fontVariationSettings
                else R.styleable.WearCurvedTextView_android_fontVariationSettings
            ) ?. let { value ->
                attributes.fontVariationSettings = value
            }
        }
    }

    init {
        val attributes = TextAppearanceAttributes()
        attributes.textColor = ColorStateList.valueOf(DEFAULT_TEXT_COLOR)

        val theme: Resources.Theme = context.theme
        var a: TypedArray = theme.obtainStyledAttributes(
            attrs, R.styleable.TextViewAppearance, defStyleAttr, defStyleRes
        )

        var appearance: TypedArray? = null
        val ap: Int = a.getResourceId(R.styleable.TextViewAppearance_android_textAppearance, -1)
        a.recycle()

        if (ap != -1) {
            appearance = theme.obtainStyledAttributes(ap, R.styleable.TextAppearance)
        }
        if (appearance != null) {
            readTextAppearance(appearance, attributes, true)
            appearance.recycle()
        }

        context.obtainStyledAttributes(
            attrs, R.styleable.WearCurvedTextView, defStyleAttr, defStyleRes
        ).apply {
            // overrride the value in the appearance with explicitly specified attribute values
            readTextAppearance(this, attributes, false)

            // read the other supported TextView attributes
            text = getString(R.styleable.WearCurvedTextView_android_text) ?: ""
            val textEllipsize = getInt(R.styleable.WearCurvedTextView_android_ellipsize, 0)
            ellipsize = when (textEllipsize) {
                1 -> TextUtils.TruncateAt.START
                2 -> TextUtils.TruncateAt.MIDDLE
                3 -> TextUtils.TruncateAt.END
                else -> null
            }

            // read the custom WearCurvedTextView attributes
            sweepDegrees = getFloat(R.styleable.WearCurvedTextView_sweepDegrees, UNSET_SWEEP_DEGREE)

            anchorType = getInt(R.styleable.WearCurvedTextView_anchorPosition, UNSET_ANCHOR_TYPE)

            anchorAngleDegrees =
                getFloat(R.styleable.WearCurvedTextView_anchorAngleDegrees, UNSET_ANCHOR_DEGREE)

            clockwise = getBoolean(R.styleable.WearCurvedTextView_clockwise, DEFAULT_CLOCKWISE)

            recycle()
        }

        applyTextAppearance(attributes)
    }

    /**
     * Nested class to avoid verification errors for methods induces in API level 26
     */
    @RequiresApi(26)
    private object Api26Impl {
        fun paintSetFontVariationSettings(paint: Paint, fontVariationSettings: String?) {
            paint.setFontVariationSettings(fontVariationSettings)
        }
    }

    /**
     * Nested class to avoid verification errors for methods induces in API level 28
     */
    @RequiresApi(28)
    private object Api28Impl {
        fun createTypeface(family: Typeface?, weight: Int, italic: Boolean): Typeface {
            return Typeface.create(family, weight, italic)
        }
    }
}