/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class StyleTest {
    @Test fun testOneStyle() = styleTest("A") { Style { add("A") } }

    @Test
    fun testTwoStyles() =
        styleTest("A", "B") {
            val styleA = Style { add("A") }
            val styleB = Style { add("B") }
            Style(styleA, styleB)
        }

    @Test
    fun testThreeStyles() =
        styleTest("A", "B", "C") {
            val styleA = Style { add("A") }
            val styleB = Style { add("B") }
            val styleC = Style { add("C") }
            Style(styleA, styleB, styleC)
        }

    @Test
    fun testFourStyles() =
        styleTest("A", "B", "C", "D") {
            val styleA = Style { add("A") }
            val styleB = Style { add("B") }
            val styleC = Style { add("C") }
            val styleD = Style { add("D") }
            Style(styleA, styleB, styleC, styleD)
        }

    @Test fun testEmpty() = styleTest { Style(Style, Style) }

    @Test fun testEmpty_First() = styleTest("A") { Style(Style { add("A") }, Style) }

    @Test fun testEmpty_Second() = styleTest("B") { Style(Style, Style { add("B") }) }

    @Test
    fun test_ThreeParameters() {
        styleTest { Style(Style, Style, Style) }
        styleTest("A") { Style(Style { add("A") }, Style, Style) }
        styleTest("B") { Style(Style, Style { add("B") }, Style) }
        styleTest("C") { Style(Style, Style, Style { add("C") }) }
        styleTest("A", "B") { Style(Style { add("A") }, Style { add("B") }, Style) }
        styleTest("A", "C") { Style(Style { add("A") }, Style, Style { add("C") }) }
        styleTest("B", "C") { Style(Style, Style { add("B") }, Style { add("C") }) }
        styleTest("A", "B", "C") {
            Style((Style { add("A") }), Style { add("B") }, Style { add("C") })
        }
    }

    @Test
    fun test_FourParameters() {
        styleTest { Style(Style, Style, Style, Style) }
        styleTest("A") { Style({ add("A") }, Style, Style, Style) }
        styleTest("B") { Style(Style, { add("B") }, Style, Style) }
        styleTest("C") { Style(Style, Style, { add("C") }, Style) }
        styleTest("D") { Style(Style, Style, Style, { add("D") }) }
        styleTest("A", "B") { Style({ add("A") }, { add("B") }, Style, Style) }
        styleTest("A", "C") { Style({ add("A") }, Style, { add("C") }, Style) }
        styleTest("B", "C") { Style(Style, { add("B") }, { add("C") }, Style) }
        // The rest of the expansion does not produce any other unique branches in Style
    }

    @Test
    fun resolveStyle_height_dp_fraction() {
        resolved(Style({ height(10.dp) }, { height(0.5f) })) {
            assertEquals(Float.NaN, height)
            assertEquals(0.5f, heightFraction)
        }
    }

    @Test
    fun resolveStyle_height_fraction_dp() {
        resolved(Style({ height(0.5f) }, { height(10.dp) })) {
            assertEquals(10.dp.toPx(), height)
            assertEquals(Float.NaN, heightFraction)
        }
    }

    @Test
    fun resolveStyle_width_dp_fraction() {
        resolved(Style({ width(10.dp) }, { width(0.5f) })) {
            assertEquals(Float.NaN, width)
            assertEquals(0.5f, widthFraction)
        }
    }

    @Test
    fun resolveStyle_width_fraction_dp() {
        resolved(Style({ width(0.5f) }, { width(10.dp) })) {
            assertEquals(10.dp.toPx(), width)
            assertEquals(Float.NaN, widthFraction)
        }
    }

    @Test
    fun resolve_size_fraction_height() {
        resolved(Style({ size(10.dp, 20.dp) }, { height(0.5f) })) {
            assertEquals(10.dp.toPx(), width)
            assertEquals(Float.NaN, widthFraction)
            assertEquals(Float.NaN, height)
            assertEquals(0.5f, heightFraction)
        }
    }

    @Test
    fun resolve_size_fraction_width() {
        resolved(Style({ size(10.dp, 20.dp) }, { width(0.5f) })) {
            assertEquals(Float.NaN, width)
            assertEquals(0.5f, widthFraction)
            assertEquals(20.dp.toPx(), height)
            assertEquals(Float.NaN, heightFraction)
        }
    }

    @Test
    fun resolve_width_then_size() {
        resolved(Style({ width(10.dp) }, { size(20.dp) })) {
            assertEquals(20.dp.toPx(), width)
            assertEquals(Float.NaN, widthFraction)
            assertEquals(20.dp.toPx(), height)
            assertEquals(Float.NaN, heightFraction)
        }
    }

    @Test
    fun resolve_size_then_width() {
        resolved(Style({ size(20.dp) }, { width(10.dp) })) {
            assertEquals(10.dp.toPx(), width)
            assertEquals(Float.NaN, widthFraction)
            assertEquals(20.dp.toPx(), height)
            assertEquals(Float.NaN, heightFraction)
        }
    }

    @Test
    fun resolve_height_then_size() {
        resolved(Style({ height(10.dp) }, { size(20.dp) })) {
            assertEquals(20.dp.toPx(), width)
            assertEquals(Float.NaN, widthFraction)
            assertEquals(20.dp.toPx(), height)
            assertEquals(Float.NaN, heightFraction)
        }
    }

    @Test
    fun resolve_size_then_height() {
        resolved(Style({ size(20.dp) }, { height(10.dp) })) {
            assertEquals(20.dp.toPx(), width)
            assertEquals(Float.NaN, widthFraction)
            assertEquals(10.dp.toPx(), height)
            assertEquals(Float.NaN, heightFraction)
        }
    }

    @Test
    fun resolve_background_color_brush() {
        val brush = Brush.linearGradient()
        resolved(Style({ background(Color.Blue) }, { background(brush) })) {
            assertEquals(Color.Unspecified, backgroundColor)
            assertEquals(brush, backgroundBrush)
        }
    }

    @Test
    fun resolve_background_brush_color() {
        val brush = Brush.linearGradient()
        resolved(Style({ background(brush) }, { background(Color.Blue) })) {
            assertEquals(Color.Blue, backgroundColor)
            assertEquals(null, backgroundBrush)
        }
    }

    @Test
    fun resolve_contentColor_contentBrush() {
        val brush = Brush.linearGradient()
        resolved(Style({ contentColor(Color.Blue) }, { contentBrush(brush) })) {
            assertEquals(Color.Unspecified, contentColor)
            assertEquals(brush, contentBrush)
        }
    }

    @Test
    fun resolved_contentBrush_contentColor() {
        val brush = Brush.linearGradient()
        resolved(Style({ contentBrush(brush) }, { contentColor(Color.Blue) })) {
            assertEquals(Color.Blue, contentColor)
            assertEquals(null, contentBrush)
        }
    }

    @Test
    fun resolve_pressed() {
        resolved(
            {
                contentColor(Color.Blue)
                pressed { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isPressed = true },
        ) {
            assertEquals(Color.Red, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                pressed { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isPressed = false },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
    }

    @Test
    fun resolve_checked() {
        resolved(
            {
                contentColor(Color.Blue)
                checked { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isChecked = true },
        ) {
            assertEquals(Color.Red, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                checked { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isChecked = false },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
    }

    @Test
    fun resolve_triStateToggleOn() {
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleOn { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.On },
        ) {
            assertEquals(Color.Red, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleOn { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.Off },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleOn { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.Indeterminate },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
    }

    @Test
    fun resolve_triStateToggleOff() {
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleOff { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.Off },
        ) {
            assertEquals(Color.Red, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleOff { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.On },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleOff { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.Indeterminate },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
    }

    @Test
    fun resolve_triStateToggleIndeterminate() {
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleIndeterminate { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.Indeterminate },
        ) {
            assertEquals(Color.Red, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleIndeterminate { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.On },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleIndeterminate { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.Off },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
    }

    @Test
    fun resolve_disabled() {
        resolved(
            {
                contentColor(Color.Blue)
                disabled { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isEnabled = false },
        ) {
            assertEquals(Color.Red, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                disabled { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isEnabled = true },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
    }

    @Test
    fun resolve_focused() {
        resolved(
            {
                contentColor(Color.Blue)
                focused { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isFocused = true },
        ) {
            assertEquals(Color.Red, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                focused { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isFocused = false },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
    }

    @Test
    fun resolve_hovered() {
        resolved(
            {
                contentColor(Color.Blue)
                hovered { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isHovered = true },
        ) {
            assertEquals(Color.Red, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                hovered { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isHovered = false },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
    }

    @Test
    fun resolve_selected() {
        resolved(
            {
                contentColor(Color.Blue)
                selected { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isSelected = true },
        ) {
            assertEquals(Color.Red, contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                selected { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isSelected = false },
        ) {
            assertEquals(Color.Blue, contentColor)
        }
    }
}

fun styleTest(vararg expected: String, block: MutableList<String>.() -> Style) {
    val result = mutableListOf<String>()
    val style = result.block()
    invoke(style)
    assertEquals(expected.toList(), result)
    assertCombinedStylesCount(style, expected.size)
}

internal inline fun resolved(
    style: Style,
    state: StyleState? = null,
    block: ResolvedStyle.() -> Unit,
) {
    val resolvedStyle = ResolvedStyle()
    resolvedStyle.resolveForTesting(style, Density(100f), true, state)
    resolvedStyle.block()
}

fun invoke(style: Style) {
    with(ResolvedStyle()) { with(style) { applyStyle() } }
}

fun assertCombinedStylesCount(style: Style, count: Int) {
    when (count) {
        0 -> assertEquals(Style, style)
        1 -> assertFalse(style is CombinedStyle)
        else -> {
            val combinedStyle = style as? CombinedStyle
            assertNotNull(combinedStyle)
            assertEquals(count, combinedStyle.styles.size)
        }
    }
}
