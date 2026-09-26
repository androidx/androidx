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
 * Tests symbolic execution branches for `HorizontalSpanHelper.kt`.
 *
 * Kex 0.0.11 generated these 31 branch paths with the KSMT portfolio solver across [NoBgColorSpan],
 * [workingPaintCache], [setFrom], [getCoveringStyles], [resolveBackgroundColor],
 * [drawSpanBackground], `findLastBgColorStyle`, and [cloneWithoutReplacementSpan]. Each `@Test`
 * method maps 1:1 to a symbolic path from the pre-minimized and post-minimized Kex test suites.
 *
 * Coverage summary from Kex symbolic analysis:
 * - `NoBgColorSpan`: 100.00% line (2/2), 100.00% instruction (7/7), 100.00% branch.
 * - `HorizontalSpanHelperKt`: 73.21% line (41/56), 80.57% instruction (228/283), 63.16% branch
 *   (24/38).
 *
 * Raw Kex output accounting (76 pre-minimized files total):
 * - `NoBgColorSpan` (5 pre-minimized files, 3 post-minimized files):
 *     - Core branch paths: 1 (`updateDrawState_21271554561`).
 *     - Infrastructure and non-null NPEs: 4 (`EqualityUtils`, `ReflectionUtils`, `init_7394825870`,
 *       `updateDrawState_2127155456_throw_java_lang_NullPointerException0`).
 * - `HorizontalSpanHelperKt` (71 pre-minimized files, 8 post-minimized files):
 *     - Core branch paths: 25 (`getWorkingPaintCache_14491230630`, `setFrom_13620886862`,
 *       `setFrom_13620886864`, `getCoveringStyles_11166987513`, `getCoveringStyles_11166987515`,
 *       `getCoveringStyles_11166987517`, `getCoveringStyles_11166987518`,
 *       `getCoveringStyles_11166987519`, `getCoveringStyles_111669875110`,
 *       `resolveBackgroundColor_6130108504`, `resolveBackgroundColor_6130108506`,
 *       `resolveBackgroundColor_613010850_throw_java_lang_ClassCastException5`,
 *       `resolveBackgroundColor_613010850_throw_java_lang_ClassCastException7`,
 *       `drawSpanBackground_830925210`, `drawSpanBackground_830925211`,
 *       `drawSpanBackground_830925212`,
 *       `drawSpanBackground_83092521_throw_java_lang_ClassCastException3`,
 *       `findLastBgColorStyle_20534634132`,
 *       `findLastBgColorStyle_2053463413_throw_java_lang_ClassCastException3`,
 *       `cloneWithoutReplacementSpan_13049585768`, `130495857614`, `130495857615`, `130495857621`,
 *       `130495857622`,
 *       `cloneWithoutReplacementSpan_1304958576_throw_java_lang_ClassCastException4`).
 *     - Synthetic SMT edge cases: 5 (`cloneWithoutReplacementSpan_130495857617`, `130495857620`,
 *       `130495857624`, `130495857627`, `130495857630` for `coerceIn` boundary clamping,
 *       `lastBgColorStyle` matching, and `SPAN_PRIORITY` flag preservation).
 *     - Unreachable `Unsafe`-corrupted states: 26 (`getCoveringStyles_11166987512`, `4`, `6`;
 *       `cloneWithoutReplacementSpan_13049585763`, `5`, `6`, `7`, `9`, `11`, `12`, `13`, `16`,
 *       `18`, `19`, `23`, `25`, `26`, `28`, `29`, `31`, `32`, `33`, `34`, `35`;
 *       `cloneWithoutReplacementSpan_1304958576_throw_java_lang_ClassCastException0`, `10` with
 *       inverted `start > end` bounds or mid-loop span type mutation).
 *     - Infrastructure and non-null NPEs: 15 (`EqualityUtils`, `ReflectionUtils`, 3 `setFrom` NPEs,
 *       2 `getCoveringStyles` NPE/CCEs, 4 `resolveBackgroundColor` NPE/CCEs, 2
 *       `findLastBgColorStyle` NPE/CCEs, 2 `cloneWithoutReplacementSpan` NPE/CCEs).
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
    // Target 1: NoBgColorSpan (1 test)
    // =========================================================================================

    // Kex: NoBgColorSpan_updateDrawState_21271554561
    // Post-minimized representative for NoBgColorSpan.updateDrawState.
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
        assertThat(textPaint.bgColor).isEqualTo(0)
    }

    // =========================================================================================
    // Target 2: HorizontalSpanHelperKt (30 tests)
    // =========================================================================================

    // Kex: HorizontalSpanHelperKt_getWorkingPaintCache_14491230630
    // Returns the non-null ThreadLocal cache instance for working TextPaint objects.
    @Test
    fun kex_getWorkingPaintCache_returnsNonNullThreadLocal() {
        val cache = workingPaintCache
        assertThat(cache).isNotNull()

        val paint = TextPaint().apply { textSize = 32f }
        cache.set(paint)
        assertThat(workingPaintCache.get()).isSameInstanceAs(paint)
    }

    // Kex: HorizontalSpanHelperKt_setFrom_13620886862
    // Post-minimized representative for TextPaint.setFrom when src is a TextPaint.
    // Copies all base Paint and TextPaint fields into the receiver TextPaint.
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

    // Kex: HorizontalSpanHelperKt_setFrom_13620886864
    // Post-minimized representative for TextPaint.setFrom when src is a plain Paint.
    // Resets TextPaint-specific fields from defaultTextPaint before copying base Paint fields.
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
        assertThat(target.bgColor).isEqualTo(0)
        assertThat(target.baselineShift).isEqualTo(0)
        assertThat(target.linkColor).isEqualTo(0)
        assertThat(target.density).isEqualTo(1.0f)
        assertThat(target.drawableState).isNull()
    }

    // Kex: HorizontalSpanHelperKt_getCoveringStyles_11166987513
    // Branch: source Spanned contains no CharacterStyle spans in [start, end).
    // Returns an empty list.
    @Test
    fun kex_getCoveringStyles_whenNoSpans_returnsEmptyList() {
        val src = SpannableString("Hello")

        val styles = src.getCoveringStyles(0, 5)

        assertThat(styles).isEmpty()
    }

    // Kex: HorizontalSpanHelperKt_getCoveringStyles_11166987515
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

    // Kex: HorizontalSpanHelperKt_getCoveringStyles_11166987517
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

    // Kex: HorizontalSpanHelperKt_getCoveringStyles_11166987518
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

    // Kex: HorizontalSpanHelperKt_getCoveringStyles_11166987519
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

    // Kex: HorizontalSpanHelperKt_getCoveringStyles_111669875110
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

    // Kex: HorizontalSpanHelperKt_resolveBackgroundColor_6130108504
    // Branch: paint !is TextPaint (plain Paint) and coveringStyles.isEmpty() is true.
    // Returns 0 without entering tempPaint.
    @Test
    fun kex_resolveBackgroundColor_plainPaintAndEmptyStyles_returnsZero() {
        val plainPaint = Paint().apply { color = Color.RED }

        val bgColor = resolveBackgroundColor(plainPaint, emptyList())

        assertThat(bgColor).isEqualTo(0)
    }

    // Kex: HorizontalSpanHelperKt_resolveBackgroundColor_6130108506
    // Post-minimized representative for resolveBackgroundColor.
    // Branch: paint is TextPaint and coveringStyles.isEmpty() is true.
    // Returns paint.bgColor directly without entering tempPaint.
    @Test
    fun kex_resolveBackgroundColor_textPaintAndEmptyStyles_returnsPaintBgColor() {
        val zeroBgPaint = TextPaint().apply { bgColor = 0 }
        val yellowBgPaint = TextPaint().apply { bgColor = Color.YELLOW }

        assertThat(resolveBackgroundColor(zeroBgPaint, emptyList())).isEqualTo(0)
        assertThat(resolveBackgroundColor(yellowBgPaint, emptyList())).isEqualTo(Color.YELLOW)
    }

    // Kex:
    // HorizontalSpanHelperKt_resolveBackgroundColor_613010850_throw_java_lang_ClassCastException5
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

    // Kex:
    // HorizontalSpanHelperKt_resolveBackgroundColor_613010850_throw_java_lang_ClassCastException7
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

    // Kex: HorizontalSpanHelperKt_drawSpanBackground_830925210
    // Branch: bgColor == 0.
    // Returns early without drawing a rectangle on the canvas.
    @Test
    fun kex_drawSpanBackground_whenBgColorIsZero_doesNotDrawRect() {
        val drawnRects = mutableListOf<RectF>()
        val canvas = recordingCanvas(rects = drawnRects)

        canvas.drawSpanBackground(10f, 20f, 110f, 60f, 0)

        assertThat(drawnRects).isEmpty()
    }

    // Kex: HorizontalSpanHelperKt_drawSpanBackground_830925211
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

    // Kex: HorizontalSpanHelperKt_drawSpanBackground_830925212
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

    // Kex: HorizontalSpanHelperKt_drawSpanBackground_83092521_throw_java_lang_ClassCastException3
    // Post-minimized representative for Canvas.drawSpanBackground.
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

    // Kex: HorizontalSpanHelperKt_findLastBgColorStyle_20534634132
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

    // Kex:
    // HorizontalSpanHelperKt_findLastBgColorStyle_2053463413_throw_java_lang_ClassCastException3
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
        assertThat(spans[2]).isNotSameInstanceAs(trailingUnderline)
        assertThat(spans[3]).isSameInstanceAs(trailingUnderline)
    }

    // Kex: HorizontalSpanHelperKt_cloneWithoutReplacementSpan_13049585768
    // Branch: source Spanned has no spans.
    // Returns a new SpannableString with the subSequence text and zero spans.
    @Test
    fun kex_cloneWithoutReplacementSpan_noSpans_returnsClonedTextWithoutSpans() {
        val src = SpannableString("Hello")

        val result = cloneWithoutReplacementSpan(src, 1, 4)

        assertThat(result.toString()).isEqualTo("ell")
        assertThat(result.getSpans(0, result.length, Any::class.java)).isEmpty()
    }

    // Kex: HorizontalSpanHelperKt_cloneWithoutReplacementSpan_130495857621
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

    // Kex: HorizontalSpanHelperKt_cloneWithoutReplacementSpan_130495857614
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

    // Kex: HorizontalSpanHelperKt_cloneWithoutReplacementSpan_130495857622
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

    // Kex:
    // HorizontalSpanHelperKt_cloneWithoutReplacementSpan_1304958576_throw_java_lang_ClassCastException4
    // Post-minimized representative for cloneWithoutReplacementSpan with a covering non-bg span.
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

    // Kex: HorizontalSpanHelperKt_cloneWithoutReplacementSpan_130495857615
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

    // Kex: HorizontalSpanHelperKt_cloneWithoutReplacementSpan_130495857617
    // Post-minimized representative for cloneWithoutReplacementSpan boundary clamping.
    // SMT Edge Case: span starting before start and ending before end (spanStart < start &&
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

    // Kex: HorizontalSpanHelperKt_cloneWithoutReplacementSpan_130495857620
    // SMT Edge Case: span starting after start and extending past end (spanStart > start &&
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

    // Kex: HorizontalSpanHelperKt_cloneWithoutReplacementSpan_130495857624
    // SMT Edge Case: covering BackgroundColorSpan matches span === lastBgColorStyle.
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
        assertThat(paint.bgColor).isEqualTo(0)
    }

    // Kex: HorizontalSpanHelperKt_cloneWithoutReplacementSpan_130495857627
    // SMT Edge Case: covering BackgroundColorSpan with non-zero SPAN_PRIORITY flags.
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

    // Kex: HorizontalSpanHelperKt_cloneWithoutReplacementSpan_130495857630
    // SMT Edge Case: covering BackgroundColorSpan followed by a partial BackgroundColorSpan.
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
