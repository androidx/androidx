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

package androidx.text.vertical.compose

import android.graphics.Color as AndroidColor
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.text.vertical.AnnotationPosition
import androidx.text.vertical.EmphasisSpan
import androidx.text.vertical.EmphasisStyle
import androidx.text.vertical.FontShearSpan
import androidx.text.vertical.RubySpan
import androidx.text.vertical.TextOrientation
import androidx.text.vertical.TextOrientationSpan
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in [VerticalTextScope] and [buildVerticalText] derived from Kex symbolic and
 * concolic execution paths.
 *
 * Covers [VerticalTextScope.sideways], [VerticalTextScope.upright],
 * [VerticalTextScope.combineUpright], [VerticalTextScope.withRuby], [VerticalTextScope.text],
 * [VerticalTextScope.withStyle], [VerticalTextScope.withEmphasis], and [buildVerticalText].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class VerticalTextUtilsKexVerifiedTest {

    // =========================================================================
    // Target 1: VerticalTextScope.TextStyleSpan
    // =========================================================================

    // Branch: fontSize == TextUnit.Unspecified, textColor == Color.Unspecified,
    // backgroundColor == Color.Unspecified.
    // Leaves textSize, color, and bgColor on TextPaint unchanged.
    @Test
    fun kex_textStyleSpan_updateMeasureState_allUnspecified_leavesPaintUnchanged() {
        val spanned =
            buildVerticalText(Density(2f)) {
                withStyle(
                    fontSize = TextUnit.Unspecified,
                    textColor = Color.Unspecified,
                    backgroundColor = Color.Unspecified,
                ) {
                    text("A")
                }
            }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint =
            TextPaint().apply {
                textSize = 24f
                color = AndroidColor.BLUE
                bgColor = AndroidColor.YELLOW
            }

        span.updateMeasureState(paint)

        assertThat(paint.textSize).isEqualTo(24f)
        assertThat(paint.color).isEqualTo(AndroidColor.BLUE)
        assertThat(paint.bgColor).isEqualTo(AndroidColor.YELLOW)
    }

    // Branch: fontSize.isSpecified == true && fontSize.isSp == true.
    // Resolves fontSize to pixels using the scope Density (including non-default fontScale).
    @Test
    fun kex_textStyleSpan_updateMeasureState_fontSizeSp_resolvesPxWithDensity() {
        val density = Density(density = 2f, fontScale = 1.5f)
        val spanned = buildVerticalText(density) { withStyle(fontSize = 20.sp) { text("A") } }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint =
            TextPaint().apply {
                textSize = 10f
                color = AndroidColor.BLACK
                bgColor = AndroidColor.YELLOW
            }

        span.updateMeasureState(paint)

        assertThat(paint.textSize).isEqualTo(with(density) { 20.sp.toPx() })
        assertThat(paint.color).isEqualTo(AndroidColor.BLACK)
        assertThat(paint.bgColor).isEqualTo(AndroidColor.YELLOW)
    }

    // Branch: fontSize.isSpecified == true && fontSize.isSp == false (Em).
    // Scales existing paint.textSize by fontSize.value.
    @Test
    fun kex_textStyleSpan_updateMeasureState_fontSizeEm_scalesExistingTextSize() {
        val spanned = buildVerticalText(Density(2f)) { withStyle(fontSize = 1.5.em) { text("A") } }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint =
            TextPaint().apply {
                textSize = 20f
                color = AndroidColor.BLACK
                bgColor = AndroidColor.YELLOW
            }

        span.updateMeasureState(paint)

        assertThat(paint.textSize).isEqualTo(30f)
        assertThat(paint.color).isEqualTo(AndroidColor.BLACK)
        assertThat(paint.bgColor).isEqualTo(AndroidColor.YELLOW)
    }

    // Branch: fontSize == TextUnit.Unspecified, textColor.isSpecified == true,
    // backgroundColor == Color.Unspecified.
    // Updates paint.color to textColor.toArgb() and leaves textSize and bgColor unchanged.
    @Test
    fun kex_textStyleSpan_updateMeasureState_textColorSpecified_setsPaintColor() {
        val spanned =
            buildVerticalText(Density(1f)) { withStyle(textColor = Color.Red) { text("A") } }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint =
            TextPaint().apply {
                textSize = 16f
                color = AndroidColor.BLACK
                bgColor = AndroidColor.YELLOW
            }

        span.updateMeasureState(paint)

        assertThat(paint.textSize).isEqualTo(16f)
        assertThat(paint.color).isEqualTo(Color.Red.toArgb())
        assertThat(paint.bgColor).isEqualTo(AndroidColor.YELLOW)
    }

    // Branch: fontSize == TextUnit.Unspecified, textColor == Color.Unspecified,
    // backgroundColor.isSpecified == true.
    // Updates paint.bgColor to backgroundColor.toArgb() and leaves textSize and color unchanged.
    @Test
    fun kex_textStyleSpan_updateMeasureState_backgroundColorSpecified_setsPaintBgColor() {
        val spanned =
            buildVerticalText(Density(1f)) {
                withStyle(backgroundColor = Color.Yellow) { text("A") }
            }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint =
            TextPaint().apply {
                textSize = 16f
                color = AndroidColor.BLACK
                bgColor = AndroidColor.TRANSPARENT
            }

        span.updateMeasureState(paint)

        assertThat(paint.textSize).isEqualTo(16f)
        assertThat(paint.color).isEqualTo(AndroidColor.BLACK)
        assertThat(paint.bgColor).isEqualTo(Color.Yellow.toArgb())
    }

    // Branch: fontSize.isSpecified == true, textColor.isSpecified == true,
    // backgroundColor.isSpecified == true.
    // Updates textSize, color, and bgColor on TextPaint in a single pass.
    @Test
    fun kex_textStyleSpan_updateMeasureState_allSpecified_updatesSizeColorAndBgColor() {
        val spanned =
            buildVerticalText(Density(2f)) {
                withStyle(
                    fontSize = 18.sp,
                    textColor = Color.Green,
                    backgroundColor = Color.Cyan,
                ) {
                    text("A")
                }
            }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint = TextPaint().apply { textSize = 12f }

        span.updateMeasureState(paint)

        assertThat(paint.textSize).isEqualTo(36f)
        assertThat(paint.color).isEqualTo(Color.Green.toArgb())
        assertThat(paint.bgColor).isEqualTo(Color.Cyan.toArgb())
    }

    // Branch: updateDrawState delegates to updateMeasureState when all style fields are
    // Unspecified.
    // Leaves TextPaint unchanged.
    @Test
    fun kex_textStyleSpan_updateDrawState_allUnspecified_leavesPaintUnchanged() {
        val spanned = buildVerticalText(Density(1f)) { withStyle { text("A") } }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint =
            TextPaint().apply {
                textSize = 22f
                color = AndroidColor.DKGRAY
                bgColor = AndroidColor.YELLOW
            }

        span.updateDrawState(paint)

        assertThat(paint.textSize).isEqualTo(22f)
        assertThat(paint.color).isEqualTo(AndroidColor.DKGRAY)
        assertThat(paint.bgColor).isEqualTo(AndroidColor.YELLOW)
    }

    // Branch: updateDrawState with fontSize.isSpecified == true && fontSize.isSp == true.
    // Resolves fontSize to pixels using Density.
    @Test
    fun kex_textStyleSpan_updateDrawState_fontSizeSp_resolvesPxWithDensity() {
        val spanned = buildVerticalText(Density(3f)) { withStyle(fontSize = 16.sp) { text("A") } }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint = TextPaint().apply { textSize = 10f }

        span.updateDrawState(paint)

        assertThat(paint.textSize).isEqualTo(48f)
    }

    // Branch: updateDrawState with fontSize.isSpecified == true && fontSize.isSp == false (Em).
    // Scales existing paint.textSize by fontSize.value.
    @Test
    fun kex_textStyleSpan_updateDrawState_fontSizeEm_scalesExistingTextSize() {
        val spanned = buildVerticalText(Density(1f)) { withStyle(fontSize = 2.em) { text("A") } }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint = TextPaint().apply { textSize = 15f }

        span.updateDrawState(paint)

        assertThat(paint.textSize).isEqualTo(30f)
    }

    // Branch: updateDrawState with textColor.isSpecified == true.
    // Updates paint.color to textColor.toArgb().
    @Test
    fun kex_textStyleSpan_updateDrawState_textColorSpecified_setsPaintColor() {
        val spanned =
            buildVerticalText(Density(1f)) { withStyle(textColor = Color.Magenta) { text("A") } }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint = TextPaint().apply { color = AndroidColor.BLACK }

        span.updateDrawState(paint)

        assertThat(paint.color).isEqualTo(Color.Magenta.toArgb())
    }

    // Branch: updateDrawState with backgroundColor.isSpecified == true.
    // Updates paint.bgColor to backgroundColor.toArgb().
    @Test
    fun kex_textStyleSpan_updateDrawState_backgroundColorSpecified_setsPaintBgColor() {
        val spanned =
            buildVerticalText(Density(1f)) {
                withStyle(backgroundColor = Color.Blue) { text("A") }
            }
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint = TextPaint().apply { bgColor = AndroidColor.TRANSPARENT }

        span.updateDrawState(paint)

        assertThat(paint.bgColor).isEqualTo(Color.Blue.toArgb())
    }

    // =========================================================================
    // Target 2: VerticalTextScope
    // =========================================================================

    // Initializes an empty SpannableStringBuilder and returns it from build().
    @Test
    fun kex_build_whenEmpty_returnsEmptySpanned() {
        val scope = VerticalTextScope(Density(1f))

        val spanned = scope.build()

        assertThat(spanned.toString()).isEmpty()
        assertThat(spanned.getSpans(0, spanned.length, Any::class.java)).isEmpty()
    }

    // Appends text and wraps the range in TextOrientationSpan.Sideways.
    @Test
    fun kex_sideways_appendsTextAndAttachesSidewaysSpan() {
        val spanned = buildVerticalText(Density(1f)) { sideways("ABC") }

        assertThat(spanned.toString()).isEqualTo("ABC")
        val span =
            spanned.getSpans(0, spanned.length, TextOrientationSpan.Sideways::class.java).single()
        assertThat(spanned.getSpanStart(span)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(span)).isEqualTo(3)
        assertThat(spanned.getSpanFlags(span)).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    // Appends text and wraps the range in TextOrientationSpan.Upright.
    @Test
    fun kex_upright_appendsTextAndAttachesUprightSpan() {
        val spanned = buildVerticalText(Density(1f)) { upright("ABC") }

        assertThat(spanned.toString()).isEqualTo("ABC")
        val span =
            spanned.getSpans(0, spanned.length, TextOrientationSpan.Upright::class.java).single()
        assertThat(spanned.getSpanStart(span)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(span)).isEqualTo(3)
        assertThat(spanned.getSpanFlags(span)).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    // Appends text and wraps the range in TextOrientationSpan.CombineUpright.
    @Test
    fun kex_combineUpright_appendsTextAndAttachesCombineUprightSpan() {
        val spanned = buildVerticalText(Density(1f)) { combineUpright("42") }

        assertThat(spanned.toString()).isEqualTo("42")
        val span =
            spanned
                .getSpans(0, spanned.length, TextOrientationSpan.CombineUpright::class.java)
                .single()
        assertThat(spanned.getSpanStart(span)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(span)).isEqualTo(2)
        assertThat(spanned.getSpanFlags(span)).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    // Branch: withRuby with explicit AnnotationPosition.After, TextOrientation.Upright, and
    // textScale.
    // Wraps the block text range in RubySpan and returns the block result.
    @Test
    fun kex_withRuby_withUprightOrientation_attachesRubySpanAndReturnsBlockResult() {
        var blockResult = 0
        val spanned =
            buildVerticalText(Density(1f)) {
                blockResult =
                    withRuby(
                        ruby = "かんじ",
                        position = AnnotationPosition.After,
                        orientation = TextOrientation.Upright,
                        textScale = 0.4f,
                    ) {
                        text("漢字")
                        7
                    }
            }

        assertThat(blockResult).isEqualTo(7)
        assertThat(spanned.toString()).isEqualTo("漢字")
        val span = spanned.getSpans(0, spanned.length, RubySpan::class.java).single()
        assertThat(span.text.toString()).isEqualTo("かんじ")
        assertThat(span.position).isEqualTo(AnnotationPosition.After)
        assertThat(span.orientation).isEqualTo(TextOrientation.Upright)
        assertThat(span.textScale).isEqualTo(0.4f)
        assertThat(spanned.getSpanStart(span)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(span)).isEqualTo(2)
        assertThat(spanned.getSpanFlags(span)).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    // Branch: withRuby with TextOrientation.Sideways and AnnotationPosition.Before.
    // Wraps the block text range in RubySpan with Sideways orientation and returns the block
    // result.
    @Test
    fun kex_withRuby_withSidewaysOrientation_attachesRubySpanAndReturnsBlockResult() {
        var blockResult = ""
        val spanned =
            buildVerticalText(Density(1f)) {
                blockResult =
                    withRuby(
                        ruby = "ruby",
                        position = AnnotationPosition.Before,
                        orientation = TextOrientation.Sideways,
                        textScale = 0.6f,
                    ) {
                        text("base")
                        "done"
                    }
            }

        assertThat(blockResult).isEqualTo("done")
        val span = spanned.getSpans(0, spanned.length, RubySpan::class.java).single()
        assertThat(span.text.toString()).isEqualTo("ruby")
        assertThat(span.position).isEqualTo(AnnotationPosition.Before)
        assertThat(span.orientation).isEqualTo(TextOrientation.Sideways)
        assertThat(span.textScale).isEqualTo(0.6f)
        assertThat(spanned.getSpanStart(span)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(span)).isEqualTo(4)
    }

    // Branch: withRuby default arguments use RubySpan.DEFAULT_POSITION,
    // RubySpan.DEFAULT_ORIENTATION, and RubySpan.DEFAULT_TEXT_SCALE.
    @Test
    fun kex_withRuby_withDefaultArguments_usesRubySpanDefaults() {
        val spanned = buildVerticalText(Density(1f)) { withRuby(ruby = "ふりがな") { text("振仮名") } }

        val span = spanned.getSpans(0, spanned.length, RubySpan::class.java).single()
        assertThat(span.text.toString()).isEqualTo("ふりがな")
        assertThat(span.position).isEqualTo(RubySpan.DEFAULT_POSITION)
        assertThat(span.orientation).isEqualTo(RubySpan.DEFAULT_ORIENTATION)
        assertThat(span.textScale).isEqualTo(RubySpan.DEFAULT_TEXT_SCALE)
    }

    // Branch: withEmphasis with explicit style, filled = false, and custom scale.
    // Wraps the block text range in EmphasisSpan and returns the block result.
    @Test
    fun kex_withEmphasis_withCustomArguments_attachesEmphasisSpanAndReturnsBlockResult() {
        var blockResult = 0
        val spanned =
            buildVerticalText(Density(1f)) {
                blockResult =
                    withEmphasis(
                        style = EmphasisStyle.Sesame,
                        filled = false,
                        scale = 0.6f,
                    ) {
                        text("強調")
                        99
                    }
            }

        assertThat(blockResult).isEqualTo(99)
        assertThat(spanned.toString()).isEqualTo("強調")
        val span = spanned.getSpans(0, spanned.length, EmphasisSpan::class.java).single()
        assertThat(span.style).isEqualTo(EmphasisStyle.Sesame)
        assertThat(span.isFilled).isFalse()
        assertThat(span.scale).isEqualTo(0.6f)
        assertThat(spanned.getSpanStart(span)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(span)).isEqualTo(2)
        assertThat(spanned.getSpanFlags(span)).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    // Branch: withEmphasis default arguments use EmphasisSpan.DEFAULT_EMPHASIS_STYLE,
    // EmphasisSpan.DEFAULT_EMPHASIS_FILL, and EmphasisSpan.DEFAULT_SCALE.
    @Test
    fun kex_withEmphasis_withDefaultArguments_usesEmphasisSpanDefaults() {
        val spanned = buildVerticalText(Density(1f)) { withEmphasis { text("傍点") } }

        val span = spanned.getSpans(0, spanned.length, EmphasisSpan::class.java).single()
        assertThat(span.style).isEqualTo(EmphasisSpan.DEFAULT_EMPHASIS_STYLE)
        assertThat(span.isFilled).isEqualTo(EmphasisSpan.DEFAULT_EMPHASIS_FILL)
        assertThat(span.scale).isEqualTo(EmphasisSpan.DEFAULT_SCALE)
    }

    // Branch: !fontShear.isNaN() == false (fontShear is Float.NaN).
    // Attaches only TextStyleSpan without wrapping in FontShearSpan.
    @Test
    fun kex_withStyle_whenFontShearIsNaN_attachesOnlyTextStyleSpan() {
        var blockResult = 0
        val spanned =
            buildVerticalText(Density(2f)) {
                blockResult =
                    withStyle(
                        fontSize = 20.sp,
                        textColor = Color.Red,
                        fontShear = Float.NaN,
                    ) {
                        text("Hello")
                        123
                    }
            }

        assertThat(blockResult).isEqualTo(123)
        assertThat(spanned.getSpans(0, spanned.length, FontShearSpan::class.java)).isEmpty()
        val metricSpans = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java)
        assertThat(metricSpans).hasLength(1)
        assertThat(spanned.getSpanStart(metricSpans[0])).isEqualTo(0)
        assertThat(spanned.getSpanEnd(metricSpans[0])).isEqualTo(5)
        assertThat(spanned.getSpanFlags(metricSpans[0])).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    // Branch: !fontShear.isNaN() == true (fontShear is specified).
    // Wraps the range in both FontShearSpan and TextStyleSpan and returns the block result.
    @Test
    fun kex_withStyle_whenFontShearIsSpecified_attachesFontShearSpanAndTextStyleSpan() {
        var blockResult = ""
        val spanned =
            buildVerticalText(Density(2f)) {
                blockResult =
                    withStyle(
                        fontSize = 20.sp,
                        textColor = Color.Blue,
                        fontShear = 0.25f,
                    ) {
                        text("Sheared")
                        "ok"
                    }
            }

        assertThat(blockResult).isEqualTo("ok")
        val shearSpan = spanned.getSpans(0, spanned.length, FontShearSpan::class.java).single()
        assertThat(shearSpan.fontShear).isEqualTo(0.25f)
        assertThat(spanned.getSpanStart(shearSpan)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(shearSpan)).isEqualTo(7)
        assertThat(spanned.getSpanFlags(shearSpan)).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

        val metricSpans = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java)
        assertThat(metricSpans).hasLength(2)
        val paint = TextPaint()
        for (span in metricSpans) {
            span.updateMeasureState(paint)
        }
        assertThat(paint.textSize).isEqualTo(40f)
        assertThat(paint.color).isEqualTo(Color.Blue.toArgb())
    }

    // Branch: !fontShear.isNaN() == true when fontShear == 0.0f.
    // Wraps the range in FontShearSpan(0f) alongside TextStyleSpan.
    @Test
    fun kex_withStyle_whenFontShearIsZero_attachesFontShearSpan() {
        val spanned = buildVerticalText(Density(1f)) { withStyle(fontShear = 0f) { text("Zero") } }

        val shearSpan = spanned.getSpans(0, spanned.length, FontShearSpan::class.java).single()
        assertThat(shearSpan.fontShear).isEqualTo(0f)
        assertThat(spanned.getSpanStart(shearSpan)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(shearSpan)).isEqualTo(4)
        assertThat(spanned.getSpanFlags(shearSpan)).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        assertThat(spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java))
            .hasLength(2)
    }

    // Branch: withSpan when block appends no text (start == length).
    // Attaches a zero-length (0..0) span with SPAN_INCLUSIVE_EXCLUSIVE.
    @Test
    fun kex_withStyle_whenBlockAppendsNothing_attachesZeroLengthSpan() {
        val spanned = buildVerticalText(Density(1f)) { withStyle(textColor = Color.Red) {} }

        assertThat(spanned.toString()).isEmpty()
        val span = spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        assertThat(spanned.getSpanStart(span)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(span)).isEqualTo(0)
        assertThat(spanned.getSpanFlags(span)).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    // Branch: rubyMap is empty (fastForEach 0 iterations).
    // Appends text and attaches no RubySpan instances.
    @Test
    fun kex_text_whenRubyMapIsEmpty_appendsTextWithoutRubySpans() {
        val spanned = buildVerticalText(Density(1f)) { text("吾輩は猫である", emptyMap()) }

        assertThat(spanned.toString()).isEqualTo("吾輩は猫である")
        assertThat(spanned.getSpans(0, spanned.length, RubySpan::class.java)).isEmpty()
    }

    // Branch: key.isEmpty() == true inside rubyMap.
    // Skips the empty key via return@fastForEach without entering the indexOf loop.
    @Test
    fun kex_text_whenRubyMapContainsEmptyKey_skipsEmptyKeyWithoutInfiniteLoop() {
        val spanned = buildVerticalText(Density(1f)) { text("漢字", mapOf("" to "empty")) }

        assertThat(spanned.toString()).isEqualTo("漢字")
        assertThat(spanned.getSpans(0, spanned.length, RubySpan::class.java)).isEmpty()
    }

    // Branch: key.isEmpty() == false, result.indexOf(key, textStartOffset) == -1.
    // Skips the while (found != -1) loop when key does not occur in the appended text.
    @Test
    fun kex_text_whenRubyMapKeyIsNotFound_attachesNoRubySpans() {
        val spanned = buildVerticalText(Density(1f)) { text("東京", mapOf("大阪" to "おおさか")) }

        assertThat(spanned.toString()).isEqualTo("東京")
        assertThat(spanned.getSpans(0, spanned.length, RubySpan::class.java)).isEmpty()
    }

    // Branch: key.isEmpty() == false, while (found != -1) iterates across multiple occurrences.
    // Attaches a RubySpan to every non-overlapping occurrence of key in the appended text.
    @Test
    fun kex_text_whenRubyMapKeyOccursMultipleTimes_attachesRubySpanToEachOccurrence() {
        val spanned = buildVerticalText(Density(1f)) { text("日時と後日談と休日", mapOf("日" to "にち")) }

        val spans =
            spanned.getSpans(0, spanned.length, RubySpan::class.java).sortedBy {
                spanned.getSpanStart(it)
            }
        assertThat(spans).hasSize(3)
        assertThat(spanned.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(spanned.getSpanEnd(spans[0])).isEqualTo(1)
        assertThat(spanned.getSpanStart(spans[1])).isEqualTo(4)
        assertThat(spanned.getSpanEnd(spans[1])).isEqualTo(5)
        assertThat(spanned.getSpanStart(spans[2])).isEqualTo(8)
        assertThat(spanned.getSpanEnd(spans[2])).isEqualTo(9)
        for (span in spans) {
            assertThat(span.text.toString()).isEqualTo("にち")
            assertThat(spanned.getSpanFlags(span)).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    // Branch: searchOffset = found + key.length advances past the matched key.
    // Skips self-overlapping occurrences (such as index 1 of "ああ" in "あああ").
    @Test
    fun kex_text_whenRubyKeyCouldOverlapItself_advancesByKeyLength() {
        val spanned = buildVerticalText(Density(1f)) { text("あああ", mapOf("ああ" to "あ")) }

        val span = spanned.getSpans(0, spanned.length, RubySpan::class.java).single()
        assertThat(span.text.toString()).isEqualTo("あ")
        assertThat(spanned.getSpanStart(span)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(span)).isEqualTo(2)
        assertThat(spanned.getSpanFlags(span)).isEqualTo(Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    // Branch: textStartOffset > 0 when prior text already contains key.
    // Searches from textStartOffset so earlier text segments are not annotated by a later call.
    @Test
    fun kex_text_whenAppendingAfterExistingText_annotatesOnlyNewlyAppendedRange() {
        val spanned =
            buildVerticalText(Density(1f)) {
                text("漢字と")
                text("漢字", mapOf("漢字" to "かんじ"))
            }

        assertThat(spanned.toString()).isEqualTo("漢字と漢字")
        val span = spanned.getSpans(0, spanned.length, RubySpan::class.java).single()
        assertThat(span.text.toString()).isEqualTo("かんじ")
        assertThat(spanned.getSpanStart(span)).isEqualTo(3)
        assertThat(spanned.getSpanEnd(span)).isEqualTo(5)
    }

    // Branch: rubyMap contains multiple keys of different lengths.
    // Sorts entries descending by key.length so longer keys are annotated before shorter keys.
    // TODO(b/569212211): VerticalTextScope.text currently searches each key from textStartOffset
    //  without skipping ranges already covered by a longer key, attaching overlapping RubySpans at
    //  [0, 3) and [0, 2) that RubyLayoutRun rejects. Tracked as a separate production follow-up.
    @Test
    fun kex_text_whenRubyMapHasMultipleKeysOfDifferentLengths_sortsDescendingByKeyLength() {
        val spanned =
            buildVerticalText(Density(1f)) {
                text(
                    "東京都と東京",
                    linkedMapOf(
                        "東京" to "とうきょう",
                        "東京都" to "とうきょうと",
                    ),
                )
            }

        val spans = spanned.getSpans(0, spanned.length, RubySpan::class.java)
        assertThat(spans).hasLength(3)
        assertThat(spans[0].text.toString()).isEqualTo("とうきょうと")
        assertThat(spanned.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(spanned.getSpanEnd(spans[0])).isEqualTo(3)
        assertThat(spans[1].text.toString()).isEqualTo("とうきょう")
        assertThat(spanned.getSpanStart(spans[1])).isEqualTo(0)
        assertThat(spanned.getSpanEnd(spans[1])).isEqualTo(2)
        assertThat(spans[2].text.toString()).isEqualTo("とうきょう")
        assertThat(spanned.getSpanStart(spans[2])).isEqualTo(4)
        assertThat(spanned.getSpanEnd(spans[2])).isEqualTo(6)
    }

    // =========================================================================
    // Target 3: VerticalTextUtilsKt (buildVerticalText)
    // =========================================================================

    // Creates a VerticalTextScope with the given Density, runs builder, and returns build().
    @Test
    fun kex_buildVerticalText_withExplicitDensity_executesBuilderAndReturnsSpanned() {
        val spanned =
            buildVerticalText(Density(3f)) { withStyle(fontSize = 10.sp) { upright("AB") } }

        assertThat(spanned.toString()).isEqualTo("AB")
        val uprightSpan =
            spanned.getSpans(0, spanned.length, TextOrientationSpan.Upright::class.java).single()
        assertThat(spanned.getSpanStart(uprightSpan)).isEqualTo(0)
        assertThat(spanned.getSpanEnd(uprightSpan)).isEqualTo(2)

        val styleSpan =
            spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
        val paint = TextPaint()
        styleSpan.updateMeasureState(paint)
        assertThat(paint.textSize).isEqualTo(30f)
    }

    // Reads LocalDensity.current from the composition and delegates to buildVerticalText(Density).
    @MediumTest
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun kex_buildVerticalText_composableOverload_readsLocalDensityFromComposition() =
        runComposeUiTest {
            val expectedDensity = Density(density = 2.5f, fontScale = 2f)
            lateinit var spanned: Spanned
            setContent {
                CompositionLocalProvider(LocalDensity provides expectedDensity) {
                    spanned = buildVerticalText {
                        withStyle(fontSize = 10.sp) { sideways("CD") }
                    }
                }
            }

            waitForIdle()
            assertThat(spanned.toString()).isEqualTo("CD")
            val sidewaysSpan =
                spanned
                    .getSpans(0, spanned.length, TextOrientationSpan.Sideways::class.java)
                    .single()
            assertThat(spanned.getSpanStart(sidewaysSpan)).isEqualTo(0)
            assertThat(spanned.getSpanEnd(sidewaysSpan)).isEqualTo(2)

            val styleSpan =
                spanned.getSpans(0, spanned.length, MetricAffectingSpan::class.java).single()
            val paint = TextPaint()
            styleSpan.updateMeasureState(paint)
            assertThat(paint.textSize).isEqualTo(with(expectedDensity) { 10.sp.toPx() })
            assertThat(paint.textSize).isEqualTo(50f)
        }
}
