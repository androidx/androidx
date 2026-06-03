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

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            assertEquals(Float.NaN, it.height)
            assertEquals(0.5f, it.heightFraction)
        }
    }

    @Test
    fun resolveStyle_height_fraction_dp() {
        resolved(Style({ height(0.5f) }, { height(10.dp) })) {
            assertEquals(10.dp.toPx(), it.height)
            assertEquals(Float.NaN, it.heightFraction)
        }
    }

    @Test
    fun resolveStyle_width_dp_fraction() {
        resolved(Style({ width(10.dp) }, { width(0.5f) })) {
            assertEquals(Float.NaN, it.width)
            assertEquals(0.5f, it.widthFraction)
        }
    }

    @Test
    fun resolveStyle_width_fraction_dp() {
        resolved(Style({ width(0.5f) }, { width(10.dp) })) {
            assertEquals(10.dp.toPx(), it.width)
            assertEquals(Float.NaN, it.widthFraction)
        }
    }

    @Test
    fun resolve_size_fraction_height() {
        resolved(Style({ size(10.dp, 20.dp) }, { height(0.5f) })) {
            assertEquals(10.dp.toPx(), it.width)
            assertEquals(Float.NaN, it.widthFraction)
            assertEquals(Float.NaN, it.height)
            assertEquals(0.5f, it.heightFraction)
        }
    }

    @Test
    fun resolve_size_fraction_width() {
        resolved(Style({ size(10.dp, 20.dp) }, { width(0.5f) })) {
            assertEquals(Float.NaN, it.width)
            assertEquals(0.5f, it.widthFraction)
            assertEquals(20.dp.toPx(), it.height)
            assertEquals(Float.NaN, it.heightFraction)
        }
    }

    @Test
    fun resolve_width_then_size() {
        resolved(Style({ width(10.dp) }, { size(20.dp) })) {
            assertEquals(20.dp.toPx(), it.width)
            assertEquals(Float.NaN, it.widthFraction)
            assertEquals(20.dp.toPx(), it.height)
            assertEquals(Float.NaN, it.heightFraction)
        }
    }

    @Test
    fun resolve_size_then_width() {
        resolved(Style({ size(20.dp) }, { width(10.dp) })) {
            assertEquals(10.dp.toPx(), it.width)
            assertEquals(Float.NaN, it.widthFraction)
            assertEquals(20.dp.toPx(), it.height)
            assertEquals(Float.NaN, it.heightFraction)
        }
    }

    @Test
    fun resolve_height_then_size() {
        resolved(Style({ height(10.dp) }, { size(20.dp) })) {
            assertEquals(20.dp.toPx(), it.width)
            assertEquals(Float.NaN, it.widthFraction)
            assertEquals(20.dp.toPx(), it.height)
            assertEquals(Float.NaN, it.heightFraction)
        }
    }

    @Test
    fun resolve_size_then_height() {
        resolved(Style({ size(20.dp) }, { height(10.dp) })) {
            assertEquals(20.dp.toPx(), it.width)
            assertEquals(Float.NaN, it.widthFraction)
            assertEquals(10.dp.toPx(), it.height)
            assertEquals(Float.NaN, it.heightFraction)
        }
    }

    @Test
    fun resolve_background_color_brush() {
        val brush = Brush.linearGradient()
        resolved(Style({ background(Color.Blue) }, { background(brush) })) {
            assertEquals(Color.Unspecified, it.backgroundColor)
            assertEquals(brush, it.backgroundBrush)
        }
    }

    @Test
    fun resolve_background_brush_color() {
        val brush = Brush.linearGradient()
        resolved(Style({ background(brush) }, { background(Color.Blue) })) {
            assertEquals(Color.Blue, it.backgroundColor)
            assertEquals(null, it.backgroundBrush)
        }
    }

    @Test
    fun resolve_contentColor_contentBrush() {
        val brush = Brush.linearGradient()
        resolved(Style({ contentColor(Color.Blue) }, { contentBrush(brush) })) {
            assertEquals(Color.Unspecified, it.contentColor)
            assertEquals(brush, it.contentBrush)
        }
    }

    @Test
    fun resolved_contentBrush_contentColor() {
        val brush = Brush.linearGradient()
        resolved(Style({ contentBrush(brush) }, { contentColor(Color.Blue) })) {
            assertEquals(Color.Blue, it.contentColor)
            assertEquals(null, it.contentBrush)
        }
    }

    @Test
    fun resolve_foreground_color_brush() {
        val brush = Brush.linearGradient()
        resolved(Style({ foreground(Color.Blue) }, { foreground(brush) })) {
            assertEquals(Color.Unspecified, it.foregroundColor)
            assertEquals(brush, it.foregroundBrush)
        }
    }

    @Test
    fun resolve_foreground_brush_color() {
        val brush = Brush.linearGradient()
        resolved(Style({ foreground(brush) }, { foreground(Color.Blue) })) {
            assertEquals(Color.Blue, it.foregroundColor)
            assertEquals(null, it.foregroundBrush)
        }
    }

    @Test
    fun resolve_colorFilter() {
        val filter = ColorFilter.tint(Color.Red)
        resolved({ colorFilter(filter) }) { assertEquals(filter, it.colorFilter) }
    }

    @Test
    fun diff_contentPaddingTop() {
        diff({ contentPaddingTop(10.dp) }, { contentPaddingTop(20.dp) }, InnerLayoutFlag)
    }

    @Test
    fun diff_contentPaddingBottom() {
        diff({ contentPaddingBottom(10.dp) }, { contentPaddingBottom(20.dp) }, InnerLayoutFlag)
    }

    @Test
    fun diff_contentPaddingStart() {
        diff({ contentPaddingStart(10.dp) }, { contentPaddingStart(20.dp) }, InnerLayoutFlag)
    }

    @Test
    fun diff_contentPaddingEnd() {
        diff({ contentPaddingEnd(10.dp) }, { contentPaddingEnd(20.dp) }, InnerLayoutFlag)
    }

    @Test
    fun diff_externalPaddingTop() {
        diff({ externalPaddingTop(10.dp) }, { externalPaddingTop(20.dp) }, OuterLayoutFlag)
    }

    @Test
    fun diff_externalPaddingBottom() {
        diff({ externalPaddingBottom(10.dp) }, { externalPaddingBottom(20.dp) }, OuterLayoutFlag)
    }

    @Test
    fun diff_externalPaddingStart() {
        diff({ externalPaddingStart(10.dp) }, { externalPaddingStart(20.dp) }, OuterLayoutFlag)
    }

    @Test
    fun diff_externalPaddingEnd() {
        diff({ externalPaddingEnd(10.dp) }, { externalPaddingEnd(20.dp) }, OuterLayoutFlag)
    }

    @Test
    fun diff_borderWidth() {
        diff({ borderWidth(10.dp) }, { borderWidth(20.dp) }, InnerLayoutFlag or DrawFlag)
    }

    @Test
    fun diff_borderColor() {
        diff({ borderColor(Color.Blue) }, { borderColor(Color.Green) }, DrawFlag)
    }

    @Test
    fun diff_borderBrush() {
        diff(
            { borderBrush(SolidColor(Color.Blue)) },
            { borderBrush(SolidColor(Color.Green)) },
            DrawFlag,
        )
    }

    @Test
    fun diff_width() {
        diff({ width(10.dp) }, { width(20.dp) }, OuterLayoutFlag)
    }

    @Test
    fun diff_height() {
        diff({ height(10.dp) }, { height(20.dp) }, OuterLayoutFlag)
    }

    @Test
    fun diff_widthFraction() {
        diff({ width(1f) }, { width(0.5f) }, OuterLayoutFlag)
    }

    @Test
    fun diff_heightFraction() {
        diff({ height(1f) }, { height(0.5f) }, OuterLayoutFlag)
    }

    @Test
    fun diff_left() {
        diff({ left(10.dp) }, { left(20.dp) }, OuterLayoutFlag)
    }

    @Test
    fun diff_top() {
        diff({ top(10.dp) }, { top(20.dp) }, OuterLayoutFlag)
    }

    @Test
    fun diff_right() {
        diff({ right(10.dp) }, { right(20.dp) }, OuterLayoutFlag)
    }

    @Test
    fun diff_bottom() {
        diff({ bottom(10.dp) }, { bottom(20.dp) }, OuterLayoutFlag)
    }

    @Test
    fun diff_alpha() {
        diff({ alpha(1f) }, { alpha(0.5f) }, LayerFlag)
    }

    @Test
    fun diff_scaleX() {
        diff({ scaleX(1f) }, { scaleX(0.5f) }, LayerFlag)
    }

    @Test
    fun diff_scaleY() {
        diff({ scaleY(1f) }, { scaleY(0.5f) }, LayerFlag)
    }

    @Test
    fun diff_translationX() {
        diff({ translationX(1f) }, { translationX(0.5f) }, LayerFlag)
    }

    @Test
    fun diff_translationY() {
        diff({ translationY(1f) }, { translationY(0.5f) }, LayerFlag)
    }

    @Test
    fun diff_transformOriginX() {
        diff(
            { transformOrigin(TransformOrigin(1f, 0f)) },
            { transformOrigin(TransformOrigin(0.5f, 0f)) },
            LayerFlag,
        )
    }

    @Test
    fun diff_transformOriginY() {
        diff(
            { transformOrigin(TransformOrigin(0f, 1f)) },
            { transformOrigin(TransformOrigin(0f, 0.5f)) },
            LayerFlag,
        )
    }

    @Test
    fun diff_rotationX() {
        diff({ rotationX(1f) }, { rotationX(0.5f) }, LayerFlag)
    }

    @Test
    fun diff_rotationY() {
        diff({ rotationY(1f) }, { rotationY(0.5f) }, LayerFlag)
    }

    @Test
    fun diff_rotationZ() {
        diff({ rotationZ(1f) }, { rotationZ(0.5f) }, LayerFlag)
    }

    @Test
    fun diff_zIndex() {
        diff({ zIndex(1f) }, { zIndex(0.5f) }, LayerFlag)
    }

    @Test
    fun diff_backgroundColor() {
        diff({ background(Color.Red) }, { background(Color.Blue) }, DrawFlag)
    }

    @Test
    fun diff_backgroundBrush() {
        diff(
            { background(SolidColor(Color.Blue)) },
            { background(SolidColor(Color.Green)) },
            DrawFlag,
        )
    }

    @Test
    fun diff_foregroundColor() {
        diff({ foreground(Color.Red) }, { foreground(Color.Blue) }, DrawFlag)
    }

    @Test
    fun diff_foregroundBrush() {
        diff(
            { foreground(SolidColor(Color.Blue)) },
            { foreground(SolidColor(Color.Green)) },
            DrawFlag,
        )
    }

    @Test
    fun diff_clip() {
        diff({ clip(true) }, { clip(false) }, LayerFlag)
    }

    @Test
    fun diff_shape() {
        diff({ shape(RectangleShape) }, { shape(CircleShape) }, DrawFlag)
    }

    @Test
    fun diff_colorFilter() {
        diff({ colorFilter(null) }, { colorFilter(ColorFilter.tint(Color.Blue)) }, LayerFlag)
    }

    @Test
    fun diff_dropShadow() {
        diff(
            { dropShadow(Shadow(5.dp, Color.Black)) },
            { dropShadow(Shadow(10.dp, Color.White)) },
            DrawFlag,
        )
    }

    @Test
    fun diff_innerShadow() {
        diff(
            { innerShadow(Shadow(5.dp, Color.Black)) },
            { innerShadow(Shadow(10.dp, Color.White)) },
            DrawFlag,
        )
    }

    @Test
    fun diff_contentColor() {
        diff({ contentColor(Color.Red) }, { contentColor(Color.Blue) }, TextDrawFlag)
    }

    @Test
    fun diff_contentBrush() {
        diff(
            { contentBrush(SolidColor(Color.Blue)) },
            { contentBrush(SolidColor(Color.Green)) },
            TextDrawFlag,
        )
    }

    @Test
    fun diff_fontFamily() {
        diff(
            { fontFamily(FontFamily.Serif) },
            { fontFamily(FontFamily.Cursive) },
            TextLayoutFlag or TextDrawFlag,
        )
    }

    @Test
    fun diff_textMotion() {
        diff(
            { textMotion(TextMotion.Static) },
            { textMotion(TextMotion.Animated) },
            TextLayoutFlag or TextDrawFlag,
        )
    }

    @Test
    fun diff_textIndent() {
        diff(
            { textIndent(TextIndent()) },
            { textIndent(TextIndent(5.sp, 10.sp)) },
            TextLayoutFlag or TextDrawFlag,
        )
    }

    @Test
    fun diff_fontSize() {
        diff({ fontSize(12.sp) }, { fontSize(24.sp) }, TextLayoutFlag or TextDrawFlag)
    }

    @Test
    fun diff_letterSpacing() {
        diff({ letterSpacing(12.sp) }, { letterSpacing(24.sp) }, TextLayoutFlag or TextDrawFlag)
    }

    @Test
    fun diff_lineHeight() {
        diff({ lineHeight(12.sp) }, { lineHeight(24.sp) }, TextLayoutFlag or TextDrawFlag)
    }

    @Test
    fun diff_baselineShift() {
        diff(
            { baselineShift(BaselineShift.None) },
            { baselineShift(BaselineShift.Subscript) },
            TextLayoutFlag or TextDrawFlag,
        )
    }

    @Test
    fun diff_lineBreak() {
        diff(
            { lineBreak(LineBreak.Simple) },
            { lineBreak(LineBreak.Paragraph) },
            TextLayoutFlag or TextDrawFlag,
        )
    }

    @Test
    fun diff_fontWeight() {
        diff(
            { fontWeight(FontWeight.Normal) },
            { fontWeight(FontWeight.Bold) },
            TextLayoutFlag or TextDrawFlag,
        )
    }

    @Test
    fun diff_fontStyle() {
        diff(
            { fontStyle(FontStyle.Normal) },
            { fontStyle(FontStyle.Italic) },
            TextLayoutFlag or TextDrawFlag,
        )
    }

    @Test
    fun diff_textAlign() {
        diff(
            { textAlign(TextAlign.Center) },
            { textAlign(TextAlign.End) },
            TextLayoutFlag or TextDrawFlag,
        )
    }

    @Test
    fun diff_textDirection() {
        diff(
            { textDirection(TextDirection.Rtl) },
            { textDirection(TextDirection.Ltr) },
            TextLayoutFlag or TextDrawFlag,
        )
    }

    @Test
    fun diff_hyphens() {
        diff({ hyphens(Hyphens.None) }, { hyphens(Hyphens.Auto) }, TextLayoutFlag or TextDrawFlag)
    }

    @Test
    fun diff_fontSynthesis() {
        diff(
            { fontSynthesis(FontSynthesis.Weight) },
            { fontSynthesis(FontSynthesis.None) },
            TextLayoutFlag or TextDrawFlag,
        )
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
            assertEquals(Color.Red, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                pressed { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isPressed = false },
        ) {
            assertEquals(Color.Blue, it.contentColor)
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
            assertEquals(Color.Red, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                checked { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isChecked = false },
        ) {
            assertEquals(Color.Blue, it.contentColor)
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
            assertEquals(Color.Red, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleOn { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.Off },
        ) {
            assertEquals(Color.Blue, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleOn { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.Indeterminate },
        ) {
            assertEquals(Color.Blue, it.contentColor)
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
            assertEquals(Color.Red, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleOff { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.On },
        ) {
            assertEquals(Color.Blue, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleOff { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.Indeterminate },
        ) {
            assertEquals(Color.Blue, it.contentColor)
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
            assertEquals(Color.Red, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleIndeterminate { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.On },
        ) {
            assertEquals(Color.Blue, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                triStateToggleIndeterminate { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.triStateToggle = ToggleableState.Off },
        ) {
            assertEquals(Color.Blue, it.contentColor)
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
            assertEquals(Color.Red, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                disabled { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isEnabled = true },
        ) {
            assertEquals(Color.Blue, it.contentColor)
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
            assertEquals(Color.Red, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                focused { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isFocused = false },
        ) {
            assertEquals(Color.Blue, it.contentColor)
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
            assertEquals(Color.Red, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                hovered { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isHovered = false },
        ) {
            assertEquals(Color.Blue, it.contentColor)
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
            assertEquals(Color.Red, it.contentColor)
        }
        resolved(
            {
                contentColor(Color.Blue)
                selected { contentColor(Color.Red) }
            },
            MutableStyleState(null).also { it.isSelected = false },
        ) {
            assertEquals(Color.Blue, it.contentColor)
        }
    }

    @Test
    fun resolve_textStyle_textMotion() {
        resolved({ textStyle(TextStyle(textMotion = TextMotion.Animated)) }) {
            assertEquals(TextMotion.Animated, it.textMotion)
        }
    }

    @Test
    fun resolve_textStyle_fontFamily() {
        resolved({ textStyle(TextStyle(fontFamily = FontFamily.Serif)) }) {
            assertEquals(FontFamily.Serif, it.fontFamily)
        }
    }

    @Test
    fun resolve_textMotion() {
        resolved({ textMotion(TextMotion.Animated) }) {
            assertEquals(TextMotion.Animated, it.textMotion)
        }
    }

    @Test
    fun resolve_no_text_style() {
        resolved(NoTextStyle { background(Color.Blue) }.toStyle()) {
            assertEquals(Color.Blue, it.backgroundColor)
        }
    }

    @Test
    fun resolve_extended_style() {
        val state = MutableStyleState(null)
        val style =
            ExtendedStyle {
                    background(Color.Red)
                    extended { background(Color.Green) }
                }
                .toStyle()
        resolved(style, state) { assertEquals(Color.Green, it.backgroundColor) }
        state[ExtendedStyleStateKey] = false
        resolved(style, state) { assertEquals(Color.Red, it.backgroundColor) }
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
    block: Density.(properties: StyleProperties) -> Unit,
) {
    val resolvedStyle = ResolvedStyle()
    resolvedStyle.buildForTesting(style, Density(100f), state)
    val properties = StyleProperties()
    resolvedStyle.resolveInto(PhaseFlagMask, properties)
    resolvedStyle.block(properties)
}

internal fun diff(a: Style, b: Style, phases: Int) {
    val resolvedStyles = ResolvedStyle()

    val emptyProperties = StyleProperties()
    resolvedStyles.buildForTesting({}, Density(1f))
    resolvedStyles.resolveInto(PhaseFlagMask, emptyProperties)

    val propertiesA1 = StyleProperties()
    resolvedStyles.buildForTesting(a, Density(1f))
    resolvedStyles.resolveInto(PhaseFlagMask, propertiesA1)

    val propertiesA2 = StyleProperties()
    resolvedStyles.buildForTesting(a, Density(1f))
    resolvedStyles.resolveInto(PhaseFlagMask, propertiesA2)

    val propertiesB = StyleProperties()
    resolvedStyles.buildForTesting(b, Density(1f))
    resolvedStyles.resolveInto(PhaseFlagMask, propertiesB)

    val changesEA = emptyProperties.diff(propertiesA1)
    assertEquals(phases, changesEA)

    val changes12 = propertiesA1.diff(propertiesA2)
    assertEquals(0, changes12)

    val changesAB = propertiesA1.diff(propertiesB)
    assertEquals(phases, changesAB)
}

internal fun invoke(style: Style) {
    with(ResolvedStyle()) { with(style) { applyStyle() } }
}

internal fun assertCombinedStylesCount(style: Style, count: Int) {
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

// Subsetting the scope
@ExperimentalFoundationStyleApi
internal interface NoTextStyleScope :
    CustomStyleScope,
    StyleStateScope,
    AnimateStyleScope,
    LayoutStyleScope,
    LayerStyleScope,
    DrawStyleScope

internal fun interface NoTextStyle : CustomStyle<NoTextStyleScope>

@ExperimentalFoundationStyleApi
internal fun NoTextStyle.toStyle(): Style = Style {
    val scope = object : StyleScope by this, NoTextStyleScope {}
    with(scope) { applyStyle() }
}

// Extending the scope
@ExperimentalFoundationStyleApi internal interface ExtendedStyleScope : StyleScope

internal fun interface ExtendedStyle : CustomStyle<ExtendedStyleScope>

@ExperimentalFoundationStyleApi internal val ExtendedStyleStateKey = StyleStateKey(true)

internal fun ExtendedStyleScope.extended(block: () -> Unit) = state(ExtendedStyleStateKey, block)

@ExperimentalFoundationStyleApi
internal fun ExtendedStyle.toStyle() = Style {
    val scope = object : StyleScope by this, ExtendedStyleScope {}
    with(scope) { applyStyle() }
}
