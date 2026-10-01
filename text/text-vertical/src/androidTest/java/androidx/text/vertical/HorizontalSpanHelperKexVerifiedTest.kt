/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.text.vertical

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.NoCopySpan
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.UnderlineSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in `HorizontalSpanHelper.kt` derived from Kex symbolic execution paths.
 *
 * Covers `NoBgColorSpan`, [setFrom], [getCoveringStyles], [resolveBackgroundColor],
 * [drawSpanBackground], `findLastBgColorStyle`, and [cloneWithoutReplacementSpan].
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class HorizontalSpanHelperKexVerifiedTest {

    private var canvasBitmap: Bitmap? = null

    @Before
    fun clearThreadLocalPaintCache() {
        workingPaintCache.remove()
    }

    @After
    fun recycleCanvasBitmap() {
        canvasBitmap?.recycle()
        canvasBitmap = null
    }

    // =========================================================================================
    // Target 1: NoBgColorSpan
    // =========================================================================================

    // Clears TextPaint.bgColor to 0 on a non-null TextPaint instance.
    @Test
    fun kex_noBgColorSpan_updateDrawState_clearsBgColorToZero() {
        val bgSpan = BackgroundColorSpan(Color.YELLOW)
        val src =
            SpannableString("A").apply {
                setSpan(bgSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val cloned = cloneWithoutReplacementSpan(src, 0, 1)
        val styles = cloned.getSpans(0, 1, CharacterStyle::class.java)
        assertThat(styles).hasLength(2)
        val noBgSpan = styles[1]
        val textPaint = TextPaint().apply { bgColor = Color.YELLOW }

        noBgSpan.updateDrawState(textPaint)

        assertThat(noBgSpan).isNotSameInstanceAs(bgSpan)
        assertThat(noBgSpan).isInstanceOf(CharacterStyle::class.java)
        assertThat(textPaint.bgColor).isEqualTo(Color.TRANSPARENT)
    }

    // =========================================================================================
    // Target 2: HorizontalSpanHelperKt
    // =========================================================================================

    // Branch: src is a TextPaint. Copies all base Paint and TextPaint fields into the receiver
    // TextPaint.
    @Test
    fun kex_setFrom_whenSourceIsTextPaint_copiesAllTextPaintFields() {
        val source =
            TextPaint().apply {
                color = Color.RED
                textSize = 42f
                bgColor = Color.GREEN
                baselineShift = 12
                linkColor = Color.BLUE
                density = 2.5f
                drawableState = intArrayOf(android.R.attr.state_selected)
            }
        val target = TextPaint()

        target.setFrom(source)

        assertThat(target.color).isEqualTo(Color.RED)
        assertThat(target.textSize).isEqualTo(42f)
        assertThat(target.bgColor).isEqualTo(Color.GREEN)
        assertThat(target.baselineShift).isEqualTo(12)
        assertThat(target.linkColor).isEqualTo(Color.BLUE)
        assertThat(target.density).isEqualTo(2.5f)
        assertThat(target.drawableState).isEqualTo(intArrayOf(android.R.attr.state_selected))
    }

    // Branch: src is a plain Paint. Resets TextPaint-specific fields from defaultTextPaint before
    // copying base Paint fields.
    @Test
    fun kex_setFrom_whenSourceIsPlainPaint_resetsTextPaintFieldsAndCopiesBasePaint() {
        val target =
            TextPaint().apply {
                bgColor = Color.YELLOW
                baselineShift = -15
                linkColor = Color.CYAN
                density = 3.0f
                drawableState = intArrayOf(android.R.attr.state_pressed)
            }
        val plainSource =
            Paint().apply {
                color = Color.MAGENTA
                textSize = 28f
                isAntiAlias = true
            }

        target.setFrom(plainSource)

        assertThat(target.color).isEqualTo(Color.MAGENTA)
        assertThat(target.textSize).isEqualTo(28f)
        assertThat(target.isAntiAlias).isTrue()
        assertThat(target.bgColor).isEqualTo(Color.TRANSPARENT)
        assertThat(target.baselineShift).isEqualTo(0)
        assertThat(target.linkColor).isEqualTo(0)
        assertThat(target.density).isEqualTo(1.0f)
        assertThat(target.drawableState).isNull()
    }

    // Branch: source Spanned contains no CharacterStyle spans in [start, end).
    // Returns an empty list.
    @Test
    fun kex_getCoveringStyles_whenNoSpans_returnsEmptyList() {
        val src = SpannableString("Hello")

        val styles = src.getCoveringStyles(0, 5)

        assertThat(styles).isEmpty()
    }

    // Branch: source Spanned contains a CharacterStyle that implements NoCopySpan.
    // Excludes the NoCopySpan from the returned covering styles.
    @Test
    fun kex_getCoveringStyles_whenSpanIsNoCopySpan_excludesSpan() {
        val noCopyStyle = TestNoCopyCharacterStyle()
        val src =
            SpannableString("Hello").apply {
                setSpan(noCopyStyle, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val styles = src.getCoveringStyles(0, 5)

        assertThat(styles).isEmpty()
    }

    // Branch: source Spanned contains a ReplacementSpan (RubySpan).
    // Excludes the ReplacementSpan from the returned covering styles.
    @Test
    fun kex_getCoveringStyles_whenSpanIsReplacementSpan_excludesSpan() {
        val rubySpan = RubySpan("ruby")
        val src =
            SpannableString("Hello").apply {
                setSpan(rubySpan, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val styles = src.getCoveringStyles(0, 5)

        assertThat(styles).isEmpty()
    }

    // Branch: CharacterStyle covers all of [start, end) (spanStart <= start && spanEnd >= end)
    // and is neither NoCopySpan nor ReplacementSpan. Returns the span in the covering list.
    @Test
    fun kex_getCoveringStyles_whenCharacterStyleCoversRange_returnsSpan() {
        val underline = UnderlineSpan()
        val bgSpan = BackgroundColorSpan(Color.GREEN)
        val src =
            SpannableString("HelloWorld").apply {
                setSpan(underline, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(bgSpan, 2, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val styles = src.getCoveringStyles(2, 7)

        assertThat(styles).containsExactly(underline, bgSpan).inOrder()
    }

    // Branch: CharacterStyle starts at or before start, but ends before end (getSpanEnd < end).
    // Excludes the partial span from the returned covering styles.
    @Test
    fun kex_getCoveringStyles_whenSpanEndsBeforeRangeEnd_excludesPartialSpan() {
        val partialSpan = UnderlineSpan()
        val src =
            SpannableString("HelloWorld").apply {
                setSpan(partialSpan, 1, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val styles = src.getCoveringStyles(1, 7)

        assertThat(styles).isEmpty()
    }

    // Branch: CharacterStyle starts after start (getSpanStart > start).
    // Excludes the partial span from the returned covering styles.
    @Test
    fun kex_getCoveringStyles_whenSpanStartsAfterRangeStart_excludesPartialSpan() {
        val partialSpan = UnderlineSpan()
        val src =
            SpannableString("HelloWorld").apply {
                setSpan(partialSpan, 3, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val styles = src.getCoveringStyles(1, 7)

        assertThat(styles).isEmpty()
    }

    // Branch: paint !is TextPaint (plain Paint) and coveringStyles.isEmpty() is true.
    // Returns 0 without entering tempPaint.
    @Test
    fun kex_resolveBackgroundColor_plainPaintAndEmptyStyles_returnsZero() {
        val plainPaint = Paint().apply { color = Color.RED }

        val bgColor = resolveBackgroundColor(plainPaint, emptyList())

        assertThat(bgColor).isEqualTo(Color.TRANSPARENT)
    }

    // Branch: paint is TextPaint and coveringStyles.isEmpty() is true.
    // Returns paint.bgColor directly without entering tempPaint.
    @Test
    fun kex_resolveBackgroundColor_textPaintAndEmptyStyles_returnsPaintBgColor() {
        val zeroBgPaint = TextPaint().apply { bgColor = Color.TRANSPARENT }
        val yellowBgPaint = TextPaint().apply { bgColor = Color.YELLOW }

        assertThat(resolveBackgroundColor(zeroBgPaint, emptyList())).isEqualTo(Color.TRANSPARENT)
        assertThat(resolveBackgroundColor(yellowBgPaint, emptyList())).isEqualTo(Color.YELLOW)
    }

    // Branch: paint !is TextPaint (plain Paint) and coveringStyles is non-empty.
    // Applies all coveringStyles in order on workPaint and returns the final bgColor.
    @Test
    fun kex_resolveBackgroundColor_plainPaintWithCoveringStyles_appliesStylesInOrder() {
        val plainPaint = Paint()
        val styles =
            listOf<CharacterStyle>(
                BackgroundColorSpan(Color.RED),
                UnderlineSpan(),
                BackgroundColorSpan(Color.CYAN),
            )

        val bgColor = resolveBackgroundColor(plainPaint, styles)

        assertThat(bgColor).isEqualTo(Color.CYAN)
    }

    // Branch: paint is TextPaint and coveringStyles is non-empty.
    // Seeds workPaint.bgColor from paint.bgColor, applies coveringStyles in order, and leaves
    // the caller TextPaint unchanged.
    @Test
    fun kex_resolveBackgroundColor_textPaintWithCoveringStyles_preservesOrOverridesBgColor() {
        val textPaint = TextPaint().apply { bgColor = Color.GREEN }

        val preservedColor = resolveBackgroundColor(textPaint, listOf(UnderlineSpan()))
        val overriddenColor =
            resolveBackgroundColor(
                textPaint,
                listOf(UnderlineSpan(), BackgroundColorSpan(Color.MAGENTA)),
            )

        assertThat(preservedColor).isEqualTo(Color.GREEN)
        assertThat(overriddenColor).isEqualTo(Color.MAGENTA)
        assertThat(textPaint.bgColor).isEqualTo(Color.GREEN)
    }

    // Branch: bgColor == 0.
    // Returns early without drawing a rectangle on the canvas.
    @Test
    fun kex_drawSpanBackground_whenBgColorIsZero_doesNotDrawRect() {
        val drawnRects = mutableListOf<RectF>()
        val canvas = recordingCanvas(rects = drawnRects)

        canvas.drawSpanBackground(10f, 20f, 110f, 60f, Color.TRANSPARENT)

        assertThat(drawnRects).isEmpty()
    }

    // Branch: bgColor != 0 && left >= right.
    // Returns early without drawing an empty or horizontally inverted box.
    @Test
    fun kex_drawSpanBackground_whenLeftEqualsOrExceedsRight_doesNotDrawRect() {
        val drawnRects = mutableListOf<RectF>()
        val canvas = recordingCanvas(rects = drawnRects)

        canvas.drawSpanBackground(50f, 20f, 50f, 60f, Color.YELLOW)
        canvas.drawSpanBackground(80f, 20f, 50f, 60f, Color.YELLOW)

        assertThat(drawnRects).isEmpty()
    }

    // Branch: bgColor != 0 && left < right && top >= bottom.
    // Returns early without drawing an empty or vertically inverted box.
    @Test
    fun kex_drawSpanBackground_whenTopEqualsOrExceedsBottom_doesNotDrawRect() {
        val drawnRects = mutableListOf<RectF>()
        val canvas = recordingCanvas(rects = drawnRects)

        canvas.drawSpanBackground(10f, 60f, 110f, 60f, Color.YELLOW)
        canvas.drawSpanBackground(10f, 80f, 110f, 60f, Color.YELLOW)

        assertThat(drawnRects).isEmpty()
    }

    // Branch: bgColor != 0 && left < right && top < bottom.
    // Resets the pooled paint (clearing any prior Paint.Style.STROKE) and fills the box with
    // bgColor.
    @Test
    fun kex_drawSpanBackground_whenBoxAndColorAreValid_resetsPaintAndDrawsFilledRect() {
        // Seed the paint pool with a dirty STROKE style to verify bgPaint.reset() clears it.
        tempPaint { dirtyPaint ->
            dirtyPaint.style = Paint.Style.STROKE
            dirtyPaint.strokeWidth = 10f
        }
        val drawnRects = mutableListOf<RectF>()
        val drawnColors = mutableListOf<Int>()
        val drawnStyles = mutableListOf<Paint.Style>()
        val canvas =
            recordingCanvas(
                rects = drawnRects,
                rectColors = drawnColors,
                rectStyles = drawnStyles,
            )

        canvas.drawSpanBackground(12f, 24f, 84f, 64f, Color.YELLOW)

        assertThat(drawnRects).containsExactly(RectF(12f, 24f, 84f, 64f))
        assertThat(drawnColors).containsExactly(Color.YELLOW)
        assertThat(drawnStyles).containsExactly(Paint.Style.FILL)
    }

    // Branch: coveringStyles list is empty (isEmpty() == true).
    // Returns null so cloneWithoutReplacementSpan does not attach NoBgColorSpan.
    @Test
    fun kex_findLastBgColorStyle_whenCoveringStylesEmpty_returnsNullWithoutNoBgSpan() {
        val partialBg = BackgroundColorSpan(Color.YELLOW)
        val src =
            SpannableString("Hello").apply {
                setSpan(partialBg, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val cloned = cloneWithoutReplacementSpan(src, 0, 5)

        val spans = cloned.getSpans(0, cloned.length, Any::class.java)
        assertThat(spans).asList().containsExactly(partialBg)
    }

    // Branch: coveringStyles is non-empty, probing each style and updating lastStyle when
    // probe.bgColor != 0. Places NoBgColorSpan right after the last covering BackgroundColorSpan.
    @Test
    fun kex_findLastBgColorStyle_whenMultipleCoveringStyles_selectsLastBgColorSpan() {
        val firstBg = BackgroundColorSpan(Color.RED)
        val secondBg = BackgroundColorSpan(Color.GREEN)
        val trailingUnderline = UnderlineSpan()
        val src =
            SpannableString("Hello").apply {
                setSpan(firstBg, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(secondBg, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(trailingUnderline, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val cloned = cloneWithoutReplacementSpan(src, 0, 5)

        val spans = cloned.getSpans(0, cloned.length, CharacterStyle::class.java)
        assertThat(spans).hasLength(4)
        assertThat(spans[0]).isSameInstanceAs(firstBg)
        assertThat(spans[1]).isSameInstanceAs(secondBg)
        assertThat(spans[3]).isSameInstanceAs(trailingUnderline)

        val paint = TextPaint().apply { bgColor = Color.GREEN }
        spans[2].updateDrawState(paint)
        assertThat(paint.bgColor).isEqualTo(Color.TRANSPARENT)
    }

    // Branch: source Spanned has no spans.
    // Returns a new SpannableString with the subSequence text and zero spans.
    @Test
    fun kex_cloneWithoutReplacementSpan_noSpans_returnsClonedTextWithoutSpans() {
        val src = SpannableString("Hello")

        val result = cloneWithoutReplacementSpan(src, 1, 4)

        assertThat(result.toString()).isEqualTo("ell")
        assertThat(result.getSpans(0, result.length, Any::class.java)).isEmpty()
    }

    // Branch: source Spanned contains a NoCopySpan.
    // Skips the NoCopySpan during span copying.
    @Test
    fun kex_cloneWithoutReplacementSpan_withNoCopySpan_excludesNoCopySpan() {
        val callerNoCopySpan = TestNoCopySpan()
        val src =
            SpannableString("Hello").apply {
                setSpan(callerNoCopySpan, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val result = cloneWithoutReplacementSpan(src, 0, 5)

        assertThat(result.getSpans(0, result.length, Any::class.java)).isEmpty()
    }

    // Branch: source Spanned contains an EmphasisSpan (ReplacementSpan).
    // Skips the ReplacementSpan to avoid infinite recursion during measurement.
    @Test
    fun kex_cloneWithoutReplacementSpan_withEmphasisSpan_excludesReplacementSpan() {
        val emphasisSpan = EmphasisSpan()
        val src =
            SpannableString("Hello").apply {
                setSpan(emphasisSpan, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val result = cloneWithoutReplacementSpan(src, 0, 5)

        assertThat(result.getSpans(0, result.length, Any::class.java)).isEmpty()
    }

    // Branch: source Spanned contains a RubySpan (ReplacementSpan).
    // Skips the RubySpan during span copying.
    @Test
    fun kex_cloneWithoutReplacementSpan_withRubySpan_excludesReplacementSpan() {
        val rubySpan = RubySpan("ruby")
        val src =
            SpannableString("Hello").apply {
                setSpan(rubySpan, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val result = cloneWithoutReplacementSpan(src, 0, 5)

        assertThat(result.getSpans(0, result.length, Any::class.java)).isEmpty()
    }

    // Branch: covering UnderlineSpan sets probe.bgColor == 0, so lastBgColorStyle is null and
    // NoBgColorSpan is not attached.
    @Test
    fun kex_cloneWithoutReplacementSpan_coveringNonBgSpan_copiesSpanWithoutNoBgSpan() {
        val underline = UnderlineSpan()
        val src =
            SpannableString("Hello").apply {
                setSpan(underline, 1, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val result = cloneWithoutReplacementSpan(src, 1, 4)

        assertThat(result.toString()).isEqualTo("ell")
        val spans = result.getSpans(0, result.length, Any::class.java)
        assertThat(spans).asList().containsExactly(underline)
        assertThat(result.getSpanStart(underline)).isEqualTo(0)
        assertThat(result.getSpanEnd(underline)).isEqualTo(3)
    }

    // Branch: partial span starting at start and ending before end (spanStart == start &&
    // spanEnd < end). Copies the span with relative offsets and does not attach NoBgColorSpan.
    @Test
    fun kex_cloneWithoutReplacementSpan_partialSpanAtStartEndingEarly_copiesWithRelativeOffset() {
        val underline = UnderlineSpan()
        val src =
            SpannableString("HelloWorld").apply {
                setSpan(underline, 2, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val result = cloneWithoutReplacementSpan(src, 2, 8)

        assertThat(result.toString()).isEqualTo("lloWor")
        val spans = result.getSpans(0, result.length, Any::class.java)
        assertThat(spans).asList().containsExactly(underline)
        assertThat(result.getSpanStart(underline)).isEqualTo(0)
        assertThat(result.getSpanEnd(underline)).isEqualTo(3)
    }

    // Branch: span starting before start and ending before end (spanStart < start &&
    // spanEnd < end). Clamps spanStart to start via coerceIn(start, end).
    @Test
    fun kex_cloneWithoutReplacementSpan_spanOverlappingLeftBoundary_clampsStartToZero() {
        val underline = UnderlineSpan()
        val src =
            SpannableString("HelloWorld").apply {
                setSpan(underline, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val result = cloneWithoutReplacementSpan(src, 2, 8)

        assertThat(result.toString()).isEqualTo("lloWor")
        assertThat(result.getSpanStart(underline)).isEqualTo(0)
        assertThat(result.getSpanEnd(underline)).isEqualTo(3)
    }

    // Branch: span starting after start and extending past end (spanStart > start &&
    // spanEnd > end). Clamps spanEnd to end via coerceIn(start, end).
    @Test
    fun kex_cloneWithoutReplacementSpan_spanOverlappingRightBoundary_clampsEndToLength() {
        val underline = UnderlineSpan()
        val src =
            SpannableString("HelloWorld").apply {
                setSpan(underline, 4, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val result = cloneWithoutReplacementSpan(src, 2, 7)

        assertThat(result.toString()).isEqualTo("lloWo")
        assertThat(result.getSpanStart(underline)).isEqualTo(2)
        assertThat(result.getSpanEnd(underline)).isEqualTo(5)
    }

    // Branch: covering BackgroundColorSpan matches span === lastBgColorStyle.
    // Attaches NoBgColorSpan across [0, spannable.length) right after the covering
    // BackgroundColorSpan.
    @Test
    fun kex_cloneWithoutReplacementSpan_coveringBgColorSpan_attachesNoBgColorSpanAfterIt() {
        val bgSpan = BackgroundColorSpan(Color.YELLOW)
        val src =
            SpannableString("HelloWorld").apply {
                setSpan(bgSpan, 0, length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

        val result = cloneWithoutReplacementSpan(src, 2, 7)

        assertThat(result.toString()).isEqualTo("lloWo")
        val spans = result.getSpans(0, result.length, CharacterStyle::class.java)
        assertThat(spans).hasLength(2)
        assertThat(spans[0]).isSameInstanceAs(bgSpan)
        assertThat(result.getSpanStart(bgSpan)).isEqualTo(0)
        assertThat(result.getSpanEnd(bgSpan)).isEqualTo(5)
        assertThat(result.getSpanStart(spans[1])).isEqualTo(0)
        assertThat(result.getSpanEnd(spans[1])).isEqualTo(5)

        val paint = TextPaint()
        for (style in spans) {
            style.updateDrawState(paint)
        }
        assertThat(paint.bgColor).isEqualTo(Color.TRANSPARENT)
    }

    // Branch: covering BackgroundColorSpan with non-zero SPAN_PRIORITY flags.
    // Preserves (spanFlags and Spanned.SPAN_PRIORITY) on the attached NoBgColorSpan.
    @Test
    fun kex_cloneWithoutReplacementSpan_coveringBgSpanWithPriority_preservesPriorityOnNoBgSpan() {
        val priorityFlags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or (5 shl Spanned.SPAN_PRIORITY_SHIFT)
        val bgSpan = BackgroundColorSpan(Color.GREEN)
        val src =
            SpannableString("Hello").apply {
                setSpan(bgSpan, 0, length, priorityFlags)
            }

        val result = cloneWithoutReplacementSpan(src, 0, 5)

        val spans = result.getSpans(0, result.length, CharacterStyle::class.java)
        assertThat(spans).hasLength(2)
        val noBgSpan = spans[1]
        assertThat(result.getSpanFlags(noBgSpan))
            .isEqualTo(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or (5 shl Spanned.SPAN_PRIORITY_SHIFT))
    }

    // Branch: covering BackgroundColorSpan followed by a partial BackgroundColorSpan.
    // Places NoBgColorSpan after the covering BackgroundColorSpan so the partial
    // BackgroundColorSpan still applies to its sub-range.
    @Test
    fun kex_cloneWithoutReplacementSpan_coveringAndPartialBgSpans_keepsPartialBgAfterNoBgSpan() {
        val coveringBg = BackgroundColorSpan(Color.YELLOW)
        val partialBg = BackgroundColorSpan(Color.CYAN)
        val src =
            SpannableString("HelloWorld").apply {
                setSpan(coveringBg, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(partialBg, 4, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

        val result = cloneWithoutReplacementSpan(src, 2, 7)

        val spans = result.getSpans(0, result.length, CharacterStyle::class.java)
        assertThat(spans).hasLength(3)
        assertThat(spans[0]).isSameInstanceAs(coveringBg)
        assertThat(spans[2]).isSameInstanceAs(partialBg)
        assertThat(result.getSpanStart(partialBg)).isEqualTo(2)
        assertThat(result.getSpanEnd(partialBg)).isEqualTo(5)

        val paint = TextPaint()
        for (style in spans) {
            style.updateDrawState(paint)
        }
        assertThat(paint.bgColor).isEqualTo(Color.CYAN)
    }

    private class TestNoCopySpan : NoCopySpan

    private class TestNoCopyCharacterStyle : CharacterStyle(), NoCopySpan {
        override fun updateDrawState(tp: TextPaint) {
            tp.bgColor = Color.RED
        }
    }

    private fun recordingCanvas(
        rects: MutableList<RectF>? = null,
        rectColors: MutableList<Int>? = null,
        rectStyles: MutableList<Paint.Style>? = null,
    ): Canvas {
        val bitmap =
            canvasBitmap
                ?: Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).also {
                    canvasBitmap = it
                }
        return object : Canvas(bitmap) {
            override fun drawRect(
                left: Float,
                top: Float,
                right: Float,
                bottom: Float,
                paint: Paint,
            ) {
                rects?.add(RectF(left, top, right, bottom))
                rectColors?.add(paint.color)
                rectStyles?.add(paint.style)
            }
        }
    }
}
