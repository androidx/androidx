package androidx.ui.engine.text.platform

import android.text.TextPaint
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.ParagraphBuilder
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAffinity
import androidx.ui.engine.text.TextPosition
import kotlin.math.floor

internal class ParagraphAndroid constructor(
    val text: StringBuilder,
    val paragraphStyle: ParagraphStyle,
    val textStyles: List<ParagraphBuilder.TextStyleIndex>
) {

    private val textPaint: TextPaint

    private var layout: TextLayout? = null

    // TODO(Migration/siyamed): width having -1 but others having 0 as default value is counter
    // intuitive
    internal var width: Double = -1.0
        get() = layout?.let { field } ?: -1.0

    internal val height: Double
        get() = layout?.let { it.layout.height.toDouble() } ?: 0.0

    // TODO(Migration/siyamed): we do not have this concept. they limit to the max word size.
    // it didn't make sense to me. I believe we might be able to do it. if we can use
    // wordbreaker.
    internal val minIntrinsicWidth: Double
        get() = 0.0

    internal val maxIntrinsicWidth: Double
        get() = layout?.let { it.maxIntrinsicWidth } ?: 0.0

    internal val alphabeticBaseline: Double
        get() = layout?.let { it.layout.getLineBaseline(0).toDouble() } ?: Double.MAX_VALUE

    // TODO(Migration/siyamed):  (metrics.fUnderlinePosition - metrics.fAscent) * style.height;
    internal val ideographicBaseline: Double
        get() = Double.MAX_VALUE

    internal val didExceedMaxLines: Boolean
        get() = false

    init {
        textPaint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    }

    fun layout(width: Double, force: Boolean = false) {
        val floorWidth = floor(width)

        if (paragraphStyle.fontSize != null) {
            textPaint.textSize = paragraphStyle.fontSize.toFloat()
        }
        if (paragraphStyle.fontFamily?.typeface != null) {
            textPaint.typeface = paragraphStyle.fontFamily.typeface
        }
        val charSequence = text.toString() as CharSequence
        layout = TextLayout(charSequence = charSequence, width = floorWidth, textPaint = textPaint)
        this.width = floorWidth
    }

    internal fun getPositionForOffset(offset: Offset): TextPosition {
        val tmpLayout = layout ?: throw IllegalStateException("getPositionForOffset cannot be " +
                "called before layout() is called")

        val line = tmpLayout.layout.getLineForVertical(offset.dy.toInt())
        return TextPosition(
            offset = tmpLayout.layout.getOffsetForHorizontal(line, offset.dx.toFloat()),
            // TODO(Migration/siyamed): we provide a default value
            affinity = TextAffinity.upstream
        )
    }
}