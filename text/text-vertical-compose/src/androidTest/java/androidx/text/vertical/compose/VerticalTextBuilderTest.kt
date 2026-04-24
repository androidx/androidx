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

import android.text.Spanned
import android.text.style.MetricAffectingSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.text.vertical.EmphasisSpan
import androidx.text.vertical.EmphasisStyle
import androidx.text.vertical.FontShearSpan
import androidx.text.vertical.RubySpan
import androidx.text.vertical.TextOrientationSpan
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VerticalTextBuilderTest {

    private val density = Density(density = 2f, fontScale = 1f)

    @Test
    fun emptyBuilder_producesEmptySpanned() {
        val result = buildVerticalText(density) {}

        assertThat(result.toString()).isEmpty()
        assertThat(result.getSpans(0, result.length, Any::class.java)).isEmpty()
    }

    @Test
    fun text_appendsInOrder() {
        val result =
            buildVerticalText(density) {
                text("Hello")
                text(" ")
                text("World")
            }

        assertThat(result.toString()).isEqualTo("Hello World")
    }

    @Test
    fun text_withRubyMap_attachesWithRubyAtEachMatch() {
        val result = buildVerticalText(density) { text("吾輩は猫である。吾輩はねこだ。", mapOf("吾輩" to "わがはい")) }

        val spans = result.getSpans(0, result.length, RubySpan::class.java)
        assertThat(spans).hasLength(2)
        assertThat(spans.map { it.text.toString() }).containsExactly("わがはい", "わがはい")
        assertThat(result.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(2)
        assertThat(result.getSpanStart(spans[1])).isEqualTo(8)
        assertThat(result.getSpanEnd(spans[1])).isEqualTo(10)
    }

    @Test
    fun sideways_wrapsTextInSidewaysSpan() {
        val result = buildVerticalText(density) { sideways("ABC") }

        val spans = result.getSpans(0, result.length, TextOrientationSpan.Sideways::class.java)
        assertThat(spans).hasLength(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(3)
        assertThat(result.toString()).isEqualTo("ABC")
    }

    @Test
    fun upright_wrapsTextInUprightSpan() {
        val result = buildVerticalText(density) { upright("123") }

        val spans = result.getSpans(0, result.length, TextOrientationSpan.Upright::class.java)
        assertThat(spans).hasLength(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(3)
    }

    @Test
    fun combineUpright_wrapsTextInTextCombineUprightSpan() {
        val result = buildVerticalText(density) { combineUpright("25") }

        val spans =
            result.getSpans(0, result.length, TextOrientationSpan.CombineUpright::class.java)
        assertThat(spans).hasLength(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(2)
    }

    @Test
    fun withRuby_wrapsBlockRangeWithAnnotation() {
        val result =
            buildVerticalText(density) {
                text("前")
                withRuby("くりすます") { text("クリスマス") }
                text("後")
            }

        assertThat(result.toString()).isEqualTo("前クリスマス後")
        val spans = result.getSpans(0, result.length, RubySpan::class.java)
        assertThat(spans).hasLength(1)
        assertThat(spans[0].text.toString()).isEqualTo("くりすます")
        assertThat(result.getSpanStart(spans[0])).isEqualTo(1)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(6)
    }

    @Test
    fun withStyle_wrapsBlockRangeInMetricAffectingSpan() {
        val result =
            buildVerticalText(density) {
                text("a")
                withStyle(fontSize = 2.em, textColor = Color.Red) { text("BIG") }
                text("b")
            }

        assertThat(result.toString()).isEqualTo("aBIGb")
        val spans = result.getSpans(0, result.length, MetricAffectingSpan::class.java)
        assertThat(spans).hasLength(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(1)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(4)
    }

    @Test
    fun withStyle_wrapsBlockRangeInFontShearSpan() {
        val result = buildVerticalText(density) { withStyle(fontShear = 0.5f) { text("斜体") } }

        val spans = result.getSpans(0, result.length, FontShearSpan::class.java)
        assertThat(spans).hasLength(1)
        assertThat(spans[0].fontShear).isEqualTo(0.5f)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(2)
    }

    @Test
    fun withEmphasis_wrapsBlockRangeInEmphasisSpan() {
        val result =
            buildVerticalText(density) { withEmphasis(style = EmphasisStyle.Sesame) { text("強調") } }

        val spans = result.getSpans(0, result.length, EmphasisSpan::class.java)
        assertThat(spans).hasLength(1)
        assertThat(result.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(result.getSpanEnd(spans[0])).isEqualTo(2)
    }

    @Test
    fun nestedBuildVerticalText_appendedAsCharSequence_preservesInnerSpans() {
        // Using the Density-taking overload from inside a non-composable builder lambda — the
        // typical pattern for ruby text / TateChuYoko content.
        val result =
            buildVerticalText(density) {
                withRuby(buildVerticalText(density) { sideways("inline") }) { text("外側") }
            }

        assertThat(result.toString()).isEqualTo("外側")
        val ruby = result.getSpans(0, result.length, RubySpan::class.java).single()
        val innerSpans =
            (ruby.text as android.text.Spanned).let {
                it.getSpans(0, it.length, TextOrientationSpan.Sideways::class.java)
            }
        assertThat(innerSpans).hasLength(1)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun composableOverload_returnsExpectedSpanned() = runComposeUiTest {
        var result: Spanned? = null
        setContent { result = buildVerticalText { text("abc", mapOf("b" to "x")) } }
        waitForIdle()

        val snapshot =
            checkNotNull(result) { "composable buildVerticalText did not produce a value" }
        assertThat(snapshot.toString()).isEqualTo("abc")
        assertThat(snapshot.getSpans(0, snapshot.length, RubySpan::class.java)).hasLength(1)
    }
}
