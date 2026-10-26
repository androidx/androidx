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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.modifiers.TextAnnotatedStringElement
import androidx.compose.foundation.text.modifiers.TextStringSimpleElement
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ReusableTextStyleScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextStyleProviderTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun stringOverload_withStyleProvider_properties() {
        var textLayoutResult: TextLayoutResult? = null
        rule.setContent {
            BasicText(
                text = "Hello World",
                styleProvider = {
                    setFontSize(24.sp)
                    setFontWeight(FontWeight.Bold)
                    setFontStyle(FontStyle.Italic)
                    setColor(Color.Red)
                },
                onTextLayout = { textLayoutResult = it },
            )
        }
        rule.waitForIdle()
        val resolvedStyle = textLayoutResult?.layoutInput?.style
        assertThat(resolvedStyle?.fontSize).isEqualTo(24.sp)
        assertThat(resolvedStyle?.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(resolvedStyle?.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun annotatedStringOverload_withStyleProvider() {
        var textLayoutResult: TextLayoutResult? = null
        rule.setContent {
            BasicText(
                text = AnnotatedString("Hello World"),
                styleProvider = {
                    setFontSize(24.sp)
                    setColor(Color.Green)
                },
                onTextLayout = { textLayoutResult = it },
            )
        }
        rule.waitForIdle()
        val resolvedStyle = textLayoutResult?.layoutInput?.style
        assertThat(resolvedStyle?.fontSize).isEqualTo(24.sp)
    }

    @Test
    fun string_drawOnlyChange_invalidatesDraw() {
        var colorState by mutableStateOf(Color.Red)
        var measureCount = 0
        var drawCount = 0

        rule.setContent {
            BasicText(
                text = "Hello World",
                styleProvider = {
                    setColor(colorState)
                },
                modifier =
                    Modifier.layout { measurable, constraints ->
                            measureCount++
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                        .drawWithContent {
                            drawCount++
                            drawContent()
                        },
            )
        }
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(1)
        assertThat(drawCount).isEqualTo(1)

        colorState = Color.Blue
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(2)
        assertThat(drawCount).isEqualTo(2)

        colorState = Color.Green
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(3)
        assertThat(drawCount).isEqualTo(3)
    }

    @Test
    fun string_layoutAffectingChange_invalidatesMeasureAndDraw() {
        var fontSizeState by mutableStateOf(20.sp)
        var measureCount = 0
        var drawCount = 0
        var measuredWidth = 0
        var measuredHeight = 0

        rule.setContent {
            BasicText(
                text = "Hello World",
                styleProvider = {
                    setFontSize(fontSizeState)
                    setColor(Color.Red)
                },
                modifier =
                    Modifier.layout { measurable, constraints ->
                            measureCount++
                            val placeable = measurable.measure(constraints)
                            measuredWidth = placeable.width
                            measuredHeight = placeable.height
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                        .drawWithContent {
                            drawCount++
                            drawContent()
                        },
            )
        }
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(1)
        assertThat(drawCount).isEqualTo(1)
        val initialWidth = measuredWidth
        val initialHeight = measuredHeight
        assertThat(initialWidth).isGreaterThan(0)
        assertThat(initialHeight).isGreaterThan(0)

        fontSizeState = 30.sp
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(2)
        assertThat(drawCount).isEqualTo(2)
        assertThat(measuredWidth).isGreaterThan(initialWidth)
        assertThat(measuredHeight).isGreaterThan(initialHeight)
    }

    @Test
    fun string_dynamicFontSize_matchesStaticTextStyleSize() {
        var fontSizeState by mutableStateOf(16.sp)
        var dynamicWidth = 0
        var dynamicHeight = 0
        var staticWidth = 0
        var staticHeight = 0

        rule.setContent {
            BasicText(
                text = "Compose Text Layout Test",
                styleProvider = { setFontSize(fontSizeState) },
                modifier =
                    Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        dynamicWidth = placeable.width
                        dynamicHeight = placeable.height
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    },
            )
            BasicText(
                text = "Compose Text Layout Test",
                style = TextStyle(fontSize = fontSizeState),
                modifier =
                    Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        staticWidth = placeable.width
                        staticHeight = placeable.height
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    },
            )
        }
        rule.waitForIdle()
        assertThat(dynamicWidth).isEqualTo(staticWidth)
        assertThat(dynamicHeight).isEqualTo(staticHeight)
        assertThat(dynamicWidth).isGreaterThan(0)

        fontSizeState = 28.sp
        rule.waitForIdle()
        assertThat(dynamicWidth).isEqualTo(staticWidth)
        assertThat(dynamicHeight).isEqualTo(staticHeight)
        assertThat(dynamicWidth).isGreaterThan(0)
    }

    @Test
    fun annotatedString_drawOnlyChange_invalidatesDraw() {
        var colorState by mutableStateOf(Color.Red)
        var measureCount = 0
        var drawCount = 0
        var onTextLayoutCount = 0

        rule.setContent {
            BasicText(
                text = AnnotatedString("Hello World"),
                styleProvider = {
                    setColor(colorState)
                },
                onTextLayout = { onTextLayoutCount++ },
                modifier =
                    Modifier.layout { measurable, constraints ->
                            measureCount++
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                        .drawWithContent {
                            drawCount++
                            drawContent()
                        },
            )
        }
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(1)
        assertThat(drawCount).isEqualTo(1)
        assertThat(onTextLayoutCount).isEqualTo(1)

        colorState = Color.Blue
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(2)
        assertThat(drawCount).isEqualTo(2)
        assertThat(onTextLayoutCount).isEqualTo(1)

        colorState = Color.Green
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(3)
        assertThat(drawCount).isEqualTo(3)
        assertThat(onTextLayoutCount).isEqualTo(1)
    }

    @Test
    fun annotatedString_layoutAffectingChange_invalidatesMeasureAndDraw() {
        var fontSizeState by mutableStateOf(20.sp)
        var measureCount = 0
        var drawCount = 0
        var measuredWidth = 0
        var measuredHeight = 0

        rule.setContent {
            BasicText(
                text = AnnotatedString("Hello World"),
                styleProvider = {
                    setFontSize(fontSizeState)
                    setColor(Color.Red)
                },
                modifier =
                    Modifier.layout { measurable, constraints ->
                            measureCount++
                            val placeable = measurable.measure(constraints)
                            measuredWidth = placeable.width
                            measuredHeight = placeable.height
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                        .drawWithContent {
                            drawCount++
                            drawContent()
                        },
            )
        }
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(1)
        assertThat(drawCount).isEqualTo(1)
        val initialWidth = measuredWidth
        val initialHeight = measuredHeight
        assertThat(initialWidth).isGreaterThan(0)
        assertThat(initialHeight).isGreaterThan(0)

        fontSizeState = 30.sp
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(2)
        assertThat(drawCount).isEqualTo(2)
        assertThat(measuredWidth).isGreaterThan(initialWidth)
        assertThat(measuredHeight).isGreaterThan(initialHeight)
    }

    @Test
    fun annotatedString_dynamicFontSize_matchesStaticTextStyleSize() {
        var fontSizeState by mutableStateOf(16.sp)
        var dynamicResult: TextLayoutResult? = null
        var staticResult: TextLayoutResult? = null

        rule.setContent {
            BasicText(
                text = AnnotatedString("Compose Text Layout Test"),
                styleProvider = { setFontSize(fontSizeState) },
                onTextLayout = { dynamicResult = it },
            )
            BasicText(
                text = AnnotatedString("Compose Text Layout Test"),
                style = TextStyle(fontSize = fontSizeState),
                onTextLayout = { staticResult = it },
            )
        }
        rule.waitForIdle()
        assertThat(dynamicResult?.size).isEqualTo(staticResult?.size)
        assertThat(dynamicResult?.layoutInput?.style?.fontSize).isEqualTo(16.sp)

        fontSizeState = 28.sp
        rule.waitForIdle()
        assertThat(dynamicResult?.size).isEqualTo(staticResult?.size)
        assertThat(dynamicResult?.layoutInput?.style?.fontSize).isEqualTo(28.sp)
    }

    @Test
    fun stringOverload_withStyleProvider_baselineShift() {
        var textLayoutResult: TextLayoutResult? = null
        rule.setContent {
            BasicText(
                text = "Hello World",
                styleProvider = {
                    setBaselineShift(BaselineShift.Superscript)
                },
                onTextLayout = { textLayoutResult = it },
            )
        }
        rule.waitForIdle()
        val resolvedStyle = textLayoutResult?.layoutInput?.style
        assertThat(resolvedStyle?.baselineShift).isEqualTo(BaselineShift.Superscript)
    }

    @Test
    fun reusableTextStyleScope_baselineShift_defaultsAndConversions() {
        // Value class equality for BaselineShift.Unspecified (Float.NaN) uses Float.compare
        assertThat(BaselineShift.Unspecified == BaselineShift.Unspecified).isTrue()
        assertThat(BaselineShift.Unspecified != BaselineShift.Unspecified).isFalse()

        val scope = ReusableTextStyleScope()
        // Default value is BaselineShift.Unspecified
        assertThat(scope.baselineShift).isEqualTo(BaselineShift.Unspecified)

        // Setting a specific BaselineShift
        scope.setBaselineShift(BaselineShift.Superscript)
        assertThat(scope.baselineShift).isEqualTo(BaselineShift.Superscript)

        // Comparison against TextStyle
        val nullBaselineStyle = TextStyle(baselineShift = null)
        val unspecifiedBaselineStyle = TextStyle(baselineShift = BaselineShift.Unspecified)
        val superscriptStyle = TextStyle(baselineShift = BaselineShift.Superscript)
        val subscriptStyle = TextStyle(baselineShift = BaselineShift.Subscript)

        assertThat(scope.hasSameLayoutAffectingAttributes(superscriptStyle)).isTrue()
        assertThat(scope.hasSameLayoutAffectingAttributes(nullBaselineStyle)).isFalse()
        assertThat(scope.hasSameLayoutAffectingAttributes(subscriptStyle)).isFalse()

        // Setting back to BaselineShift.Unspecified
        scope.setBaselineShift(BaselineShift.Unspecified)
        assertThat(scope.baselineShift).isEqualTo(BaselineShift.Unspecified)
        assertThat(scope.toTextStyle().baselineShift).isNull()
        assertThat(scope.hasSameLayoutAffectingAttributes(nullBaselineStyle)).isTrue()
        assertThat(scope.hasSameLayoutAffectingAttributes(unspecifiedBaselineStyle)).isTrue()
        assertThat(scope.hasSameLayoutAffectingAttributes(superscriptStyle)).isFalse()
    }

    @Test
    fun reusableTextStyleScope_layoutAttributesComparison() {
        val scope1 = ReusableTextStyleScope()
        val scope2 = ReusableTextStyleScope()

        scope1.setFontSize(20.sp)
        scope2.setFontSize(20.sp)

        assertThat(scope1.hasSameLayoutAffectingAttributes(scope2)).isTrue()

        scope2.setFontSize(30.sp)
        assertThat(scope1.hasSameLayoutAffectingAttributes(scope2)).isFalse()

        // Setting same value resets equality
        scope2.setFontSize(20.sp)
        assertThat(scope1.hasSameLayoutAffectingAttributes(scope2)).isTrue()
    }

    @Test
    fun reusableTextStyleScope_settersAndReset() {
        val scope = ReusableTextStyleScope()
        assertThat(scope.color).isEqualTo(Color.Unspecified)
        assertThat(scope.alpha.isNaN()).isTrue()

        scope.setColor(Color.Red)
        scope.setAlpha(0.5f)
        assertThat(scope.color).isEqualTo(Color.Red)
        assertThat(scope.alpha).isEqualTo(0.5f)

        scope.reset()
        assertThat(scope.color).isEqualTo(Color.Unspecified)
        assertThat(scope.alpha.isNaN()).isTrue()
    }

    @Test
    fun string_updateLayoutArgs_whenStyleChanges() {
        var minLines by mutableStateOf(1)
        var fontSize by mutableStateOf(10.sp)
        var measuredHeight = 0

        rule.setContent {
            BasicText(
                text = "Single line",
                minLines = minLines,
                styleProvider = { setFontSize(fontSize) },
                modifier =
                    Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        measuredHeight = placeable.height
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    },
            )
        }
        rule.waitForIdle()
        val initialHeight = measuredHeight

        // Simultaneously change minLines and fontSize (so style invalidates layout)
        minLines = 5
        fontSize = 20.sp
        rule.waitForIdle()

        // With minLines = 5 and larger font size, height should be significantly larger
        assertThat(measuredHeight).isGreaterThan(initialHeight * 2)
    }

    @Test
    fun annotatedString_updateLayoutArgs_whenStyleChanges() {
        var minLines by mutableStateOf(1)
        var fontSize by mutableStateOf(10.sp)
        var measuredHeight = 0

        rule.setContent {
            BasicText(
                text = AnnotatedString("Single line"),
                minLines = minLines,
                styleProvider = { setFontSize(fontSize) },
                modifier =
                    Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        measuredHeight = placeable.height
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    },
            )
        }
        rule.waitForIdle()
        val initialHeight = measuredHeight

        // Simultaneously change minLines and fontSize (so style invalidates layout)
        minLines = 5
        fontSize = 20.sp
        rule.waitForIdle()

        assertThat(measuredHeight).isGreaterThan(initialHeight * 2)
    }

    @Test
    fun string_transitionFromStaticStyleToStyleProvider_withSameSize_invalidatesDraw() {
        var useStyleProvider by mutableStateOf(false)
        var measureCount = 0
        var drawCount = 0

        rule.setContent {
            val fontFamilyResolver = LocalFontFamilyResolver.current
            val defaultLocaleList = LocalLocaleList.current
            val trackingModifier = remember {
                Modifier.layout { measurable, constraints ->
                        measureCount++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .drawWithContent {
                        drawCount++
                        drawContent()
                    }
            }
            val textElement =
                if (useStyleProvider) {
                    TextStringSimpleElement(
                        text = "Hello World",
                        styleProvider = {
                            setFontSize(20.sp)
                            setColor(Color.Blue)
                        },
                        fontFamilyResolver = fontFamilyResolver,
                        defaultLocaleList = defaultLocaleList,
                    )
                } else {
                    TextStringSimpleElement(
                        text = "Hello World",
                        style = TextStyle(fontSize = 20.sp, color = Color.Red),
                        fontFamilyResolver = fontFamilyResolver,
                        defaultLocaleList = defaultLocaleList,
                    )
                }
            Layout(
                modifier = trackingModifier.then(textElement),
                measurePolicy = { _, constraints ->
                    layout(constraints.minWidth, constraints.minHeight) {}
                },
            )
        }
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(1)
        assertThat(drawCount).isEqualTo(1)

        // Transition from static style to styleProvider with identical layout size but new draw
        // color
        useStyleProvider = true
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(2)
        assertThat(drawCount).isEqualTo(2)
    }

    @Test
    fun annotatedString_transitionFromStaticStyleToStyleProvider_withSameSize_invalidatesDraw() {
        var useStyleProvider by mutableStateOf(false)
        var measureCount = 0
        var drawCount = 0

        rule.setContent {
            val fontFamilyResolver = LocalFontFamilyResolver.current
            val defaultLocaleList = LocalLocaleList.current
            val trackingModifier = remember {
                Modifier.layout { measurable, constraints ->
                        measureCount++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .drawWithContent {
                        drawCount++
                        drawContent()
                    }
            }
            val textElement =
                if (useStyleProvider) {
                    TextAnnotatedStringElement(
                        text = AnnotatedString("Hello World"),
                        styleProvider = {
                            setFontSize(20.sp)
                            setColor(Color.Blue)
                        },
                        fontFamilyResolver = fontFamilyResolver,
                        defaultLocaleList = defaultLocaleList,
                    )
                } else {
                    TextAnnotatedStringElement(
                        text = AnnotatedString("Hello World"),
                        style = TextStyle(fontSize = 20.sp, color = Color.Red),
                        fontFamilyResolver = fontFamilyResolver,
                        defaultLocaleList = defaultLocaleList,
                    )
                }
            Layout(
                modifier = trackingModifier.then(textElement),
                measurePolicy = { _, constraints ->
                    layout(constraints.minWidth, constraints.minHeight) {}
                },
            )
        }
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(1)
        assertThat(drawCount).isEqualTo(1)

        // Transition from static style to styleProvider with identical layout size but new draw
        // color
        useStyleProvider = true
        rule.waitForIdle()
        assertThat(measureCount).isEqualTo(2)
        assertThat(drawCount).isEqualTo(2)
    }
}
