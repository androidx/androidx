package androidx.ui.engine.text.platform

import android.os.Build
import android.text.BoringLayout
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import kotlin.math.ceil
import kotlin.math.min

private const val ALIGN_NORMAL = 0
private const val ALIGN_OPPOSITE = 1
private const val ALIGN_CENTER = 2
private const val ALIGN_LEFT = 3
private const val ALIGN_RIGHT = 4

private const val JUSTIFICATION_MODE_NONE = Layout.JUSTIFICATION_MODE_NONE
private const val JUSTIFICATION_MODE_INTER_WORD = Layout.JUSTIFICATION_MODE_INTER_WORD

private const val HYPHENATION_FREQUENCY_NORMAL = Layout.HYPHENATION_FREQUENCY_NORMAL
private const val HYPHENATION_FREQUENCY_FULL = Layout.HYPHENATION_FREQUENCY_FULL
private const val HYPHENATION_FREQUENCY_NONE = Layout.HYPHENATION_FREQUENCY_NONE

private const val BREAK_STRATEGY_SIMPLE = Layout.BREAK_STRATEGY_SIMPLE
private const val BREAK_STRATEGY_HIGH_QUALITY = Layout.BREAK_STRATEGY_HIGH_QUALITY
private const val BREAK_STRATEGY_BALANCED = Layout.BREAK_STRATEGY_BALANCED

private const val TEXT_DIRECTION_LTR = 0
private const val TEXT_DIRECTION_RTL = 1
private const val TEXT_DIRECTION_FIRST_STRONG_LTR = 2
private const val TEXT_DIRECTION_FIRST_STRONG_RTL = 3
private const val TEXT_DIRECTION_ANY_RTL_LTR = 4
private const val TEXT_DIRECTION_LOCALE = 5

private val DEFAULT_LINESPACING_MULTIPLIER = 1.0f
private val DEFAULT_LINESPACING_EXTRA = 0.0f

@IntDef(ALIGN_NORMAL,
    ALIGN_CENTER,
    ALIGN_OPPOSITE,
    ALIGN_LEFT,
    ALIGN_RIGHT)
internal annotation class TextLayoutAlignment

@IntDef(JUSTIFICATION_MODE_NONE,
    JUSTIFICATION_MODE_INTER_WORD)
internal annotation class JustificationMode

@IntDef(BREAK_STRATEGY_SIMPLE,
    BREAK_STRATEGY_HIGH_QUALITY,
    BREAK_STRATEGY_BALANCED)
internal annotation class BreakStrategy

@IntDef(HYPHENATION_FREQUENCY_NORMAL,
    HYPHENATION_FREQUENCY_FULL,
    HYPHENATION_FREQUENCY_NONE)
internal annotation class HyphenationFrequency

@IntDef(TEXT_DIRECTION_LTR,
    TEXT_DIRECTION_RTL,
    TEXT_DIRECTION_FIRST_STRONG_LTR,
    TEXT_DIRECTION_FIRST_STRONG_RTL,
    TEXT_DIRECTION_ANY_RTL_LTR,
    TEXT_DIRECTION_LOCALE)
internal annotation class TextDirection

internal class TextLayout constructor(
    charSequence: CharSequence,
    width: Double = 0.0,
    textPaint: TextPaint,
    @TextLayoutAlignment alignment: Int = ALIGN_NORMAL,
    ellipsize: TextUtils.TruncateAt? = null,
    @TextDirection textDirectionHeuristic: Int = TEXT_DIRECTION_FIRST_STRONG_LTR,
    lineSpacingMultiplier: Double = 1.0,
    lineSpacingExtra: Double = 0.0,
    includePadding: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    @BreakStrategy breakStrategy: Int = BREAK_STRATEGY_SIMPLE,
    @HyphenationFrequency hyphenationFrequency: Int = HYPHENATION_FREQUENCY_NONE,
    @JustificationMode justificationMode: Int = JUSTIFICATION_MODE_NONE,
    leftIndents: IntArray? = null,
    rightIndents: IntArray? = null
) {
    val maxIntrinsicWidth: Double
    val layout: Layout

    init {
        val boringMetrics = BoringLayout.isBoring(charSequence, textPaint, null /* metrics */)
        val start = 0
        val end = charSequence.length
        maxIntrinsicWidth = if (boringMetrics == null) {
            // we may need to getWidthWithLimits(maxWidth: Int, maxLines: Int)
            Layout.getDesiredWidth(charSequence, start, end, textPaint).toDouble()
        } else {
            boringMetrics.width.toDouble()
        }

        val finalWidth = ceil(min(maxIntrinsicWidth, width)).toInt()
        val ellipsizeWidth = finalWidth

        layout = if (boringMetrics != null && maxIntrinsicWidth <= width) {
            createBoringLayout(
                textPaint = textPaint,
                charSequence = charSequence,
                width = finalWidth,
                alignment = alignment,
                metrics = boringMetrics,
                includePadding = includePadding,
                ellipsize = ellipsize,
                ellipsizeWidth = ellipsizeWidth
            )
        } else {
            createStaticLayout(
                textPaint = textPaint,
                charSequence = charSequence,
                width = finalWidth,
                alignment = alignment,
                ellipsize = ellipsize,
                ellipsizeWidth = ellipsizeWidth,
                start = start,
                end = end,
                textDirectionHeuristic = textDirectionHeuristic,
                lineSpacingMultiplier = lineSpacingMultiplier,
                lineSpacingExtra = lineSpacingExtra,
                includePadding = includePadding,
                maxLines = maxLines,
                breakStrategy = breakStrategy,
                hyphenationFrequency = hyphenationFrequency,
                justificationMode = justificationMode,
                leftIndents = leftIndents,
                rightIndents = rightIndents
            )
        }
    }

    private fun createBoringLayout(
        textPaint: TextPaint,
        charSequence: CharSequence,
        width: Int = 0,
        @TextLayoutAlignment alignment: Int,
        metrics: BoringLayout.Metrics,
        includePadding: Boolean = false,
        ellipsize: TextUtils.TruncateAt?,
        ellipsizeWidth: Int = 0
    ): Layout {
        val frameworkAlignment = TextAlignmentAdapter.get(alignment)
        return if (ellipsize == null) {
            BoringLayout(
                charSequence,
                textPaint,
                width,
                frameworkAlignment,
                DEFAULT_LINESPACING_MULTIPLIER,
                DEFAULT_LINESPACING_EXTRA,
                metrics,
                includePadding
            )
        } else {
            BoringLayout(
                charSequence,
                textPaint,
                width,
                frameworkAlignment,
                DEFAULT_LINESPACING_MULTIPLIER,
                DEFAULT_LINESPACING_EXTRA,
                metrics,
                includePadding,
                ellipsize,
                ellipsizeWidth
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun createStaticLayout(
        textPaint: TextPaint,
        charSequence: CharSequence,
        width: Int = 0,
        @TextLayoutAlignment alignment: Int,
        ellipsize: TextUtils.TruncateAt?,
        ellipsizeWidth: Int,
        start: Int = 0,
        end: Int = charSequence.length,
        @TextDirection textDirectionHeuristic: Int,
        lineSpacingMultiplier: Double,
        lineSpacingExtra: Double,
        includePadding: Boolean,
        maxLines: Int,
        @BreakStrategy breakStrategy: Int,
        @HyphenationFrequency hyphenationFrequency: Int,
        @JustificationMode justificationMode: Int,
        leftIndents: IntArray?,
        rightIndents: IntArray?
    ): Layout {

        val frameworkAlignment = TextAlignmentAdapter.get(alignment)

        return if (Build.VERSION.SDK_INT >= 23) {
            // TODO(Migration/siyamed): textDirectionHeuristic was added in 18 but no constructor
            // for that before 23, this is a little trouble
            val frameworkTextDirectionHeuristic = getTextDirectionHeuristic(textDirectionHeuristic)
            val builder = StaticLayout.Builder.obtain(
                charSequence,
                start,
                end,
                textPaint,
                width)
                .setAlignment(frameworkAlignment)
                .setTextDirection(frameworkTextDirectionHeuristic)
                .setLineSpacing(lineSpacingExtra.toFloat(), lineSpacingMultiplier.toFloat())
                .setBreakStrategy(breakStrategy)
                .setHyphenationFrequency(hyphenationFrequency)
                .setIncludePad(includePadding)
                .setMaxLines(maxLines)
                .setEllipsize(ellipsize)
                .setIndents(leftIndents, rightIndents)

            if (Build.VERSION.SDK_INT >= 26) {
                builder.setJustificationMode(justificationMode)
            }
            // if (Build.VERSION.SDK_INT >= 28) {
            // TODO(Migration/siyamed): last line spacing is required for editable text, otherwise
            // we will need tricks
            // builder.setAddLastLineLineSpacing(builder.mAddLastLineLineSpacing);
            // builder.setUseLineSpacingFromFallbacks(true);
            // }

            builder.build()
        } else {
            if (ellipsize != null) {
                StaticLayout(
                    charSequence,
                    start,
                    end,
                    textPaint,
                    width,
                    frameworkAlignment,
                    lineSpacingMultiplier.toFloat(),
                    lineSpacingExtra.toFloat(),
                    includePadding,
                    ellipsize,
                    ellipsizeWidth
                )
            } else {
                StaticLayout(
                    charSequence,
                    start,
                    end,
                    textPaint,
                    width,
                    frameworkAlignment,
                    lineSpacingMultiplier.toFloat(),
                    lineSpacingExtra.toFloat(),
                    includePadding
                )
            }
        }
    }

    @RequiresApi(api = 18)
    fun getTextDirectionHeuristic(@TextDirection textDirectionHeuristic: Int):
        TextDirectionHeuristic {
        return when (textDirectionHeuristic) {
            TEXT_DIRECTION_LTR -> TextDirectionHeuristics.LTR
            TEXT_DIRECTION_LOCALE -> TextDirectionHeuristics.LOCALE
            TEXT_DIRECTION_RTL -> TextDirectionHeuristics.RTL
            TEXT_DIRECTION_FIRST_STRONG_RTL -> TextDirectionHeuristics.FIRSTSTRONG_RTL
            TEXT_DIRECTION_ANY_RTL_LTR -> TextDirectionHeuristics.ANYRTL_LTR
            TEXT_DIRECTION_FIRST_STRONG_LTR -> TextDirectionHeuristics.FIRSTSTRONG_LTR
            else -> TextDirectionHeuristics.FIRSTSTRONG_LTR
        }
    }
}

object TextAlignmentAdapter {
    private val ALIGN_LEFT_FRAMEWORK: Layout.Alignment
    private val ALIGN_RIGHT_FRAMEWORK: Layout.Alignment

    init {
        val values = Layout.Alignment.values()
        var alignLeft = Layout.Alignment.ALIGN_NORMAL
        var alignRight = Layout.Alignment.ALIGN_NORMAL
        for (value in values) {
            if (value.name.equals("ALIGN_LEFT")) {
                alignLeft = value
                continue
            }

            if (value.name.equals("ALIGN_RIGHT")) {
                alignRight = value
                continue
            }
        }

        ALIGN_LEFT_FRAMEWORK = alignLeft
        ALIGN_RIGHT_FRAMEWORK = alignRight
    }

    fun get(@TextLayoutAlignment value: Int): Layout.Alignment {
        return when (value) {
            ALIGN_LEFT -> ALIGN_LEFT_FRAMEWORK
            ALIGN_RIGHT -> ALIGN_RIGHT_FRAMEWORK
            ALIGN_CENTER -> Layout.Alignment.ALIGN_CENTER
            ALIGN_OPPOSITE -> Layout.Alignment.ALIGN_OPPOSITE
            ALIGN_NORMAL -> Layout.Alignment.ALIGN_NORMAL
            else -> Layout.Alignment.ALIGN_NORMAL
        }
    }
}

//    class Metrics : BoringLayout.Metrics() {
//        var interlineSpacing = -1
//
//        fun reset() {
//            top = 0
//            bottom = 0
//            ascent = 0
//            descent = 0
//            width = -1
//            //TODO: isn't leading same as interline spacing
//            leading = 0
//            interlineSpacing = 0
//        }
//    }

// private fun getWidthWithLimits(maxWidth: Int, maxLines: Int): Int {
//    if (isBasicText()) {
//        basicMetrics = BoringLayoutFactory.Metrics()
//        val width: Float
//        if (!isSpanned) {
//            //TODO: this did not change any values
//            width = textPaint.measureText(charSequence, 0, charSequence.length)
//        } else {
//            width = Layout.getDesiredWidth(charSequence, 0, charSequence.length, textPaint)
//        }
//
//        // TODO: height given in this metrics will not be valid for Spanned
//        val interlineSpacing = textPaint.getFontMetricsInt(basicMetrics)
//        basicMetrics!!.interlineSpacing = interlineSpacing
//        basicMetrics!!.width = Math.ceil(width.toDouble()).toInt()
//        return basicMetrics!!.width
//    } else {
//        val length = charSequence.length
//        var currentLine = 0
//        var lineStartIndex: Int
//        var index = 0
//        var currentMax = 0
//        while (index < length && currentLine < maxLines) {
//            lineStartIndex = TextUtils.indexOf(charSequence, '\n', index, length)
//
//            if (lineStartIndex < 0) lineStartIndex = length
//            //TODO need improvement
//            val width = ceil(
//                Layout.getDesiredWidth(
//                    charSequence,
//                    index,
//                    lineStartIndex,
//                    textPaint
//                )
//            ).toInt()
//            if (width > currentMax) currentMax = width
//            if (width > maxWidth) break
//            lineStartIndex++
//            currentLine++
//            index = lineStartIndex
//        }
//
//        return min(currentMax, maxWidth)
//    }
// }

//  Get the line bottom discarding the line spacing added.
//
// fun Layout.getLineBottomWithoutSpacing(line: Int): Int {
//    val lineBottom = getLineBottom(line)
//    val lastLineSpacingNotAdded = Build.VERSION.SDK_INT >= 19
//    val isLastLine = line == lineCount - 1
//
//    val lineBottomWithoutSpacing: Int
//    val lineSpacingExtra = spacingAdd
//    val lineSpacingMultiplier = spacingMultiplier
//    val hasLineSpacing = lineSpacingExtra != DEFAULT_LINESPACING_EXTRA
//            || lineSpacingMultiplier != DEFAULT_LINESPACING_MULTIPLIER
//
//    if (!hasLineSpacing || isLastLine && lastLineSpacingNotAdded) {
//        lineBottomWithoutSpacing = lineBottom
//    } else {
//        val extra: Float
//        if (lineSpacingMultiplier.compareTo(DEFAULT_LINESPACING_MULTIPLIER) != 0) {
//            val lineHeight = getLineHeight(line)
//            extra = lineHeight - (lineHeight - lineSpacingExtra) / lineSpacingMultiplier
//        } else {
//            extra = lineSpacingExtra
//        }
//
//        lineBottomWithoutSpacing = (lineBottom - extra).toInt()
//    }
//
//    return lineBottomWithoutSpacing
// }