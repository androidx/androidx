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

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.text.vertical.RubySpan
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class HorizontalTextTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun widthInMax_wrapsToWidestLine() {
        val style = TextStyle(fontSize = 20.sp)
        var measuredSize = IntSize.Zero
        var maxWidthPx = 0

        rule.setContent {
            val density = LocalDensity.current
            maxWidthPx = with(density) { 300.dp.roundToPx() }
            HorizontalText(
                text = "Hello",
                modifier = Modifier.maxWidth(maxWidthPx).onSizeChanged { measuredSize = it },
                style = style,
            )
        }

        rule.runOnIdle {
            assertThat(measuredSize.width).isGreaterThan(0)
            assertThat(measuredSize.width).isLessThan(maxWidthPx)
            assertThat(measuredSize.height).isGreaterThan(0)
        }
    }

    @Test
    fun fixedWidth_keepsMaxWidth() {
        val style = TextStyle(fontSize = 20.sp)
        var measuredSize = IntSize.Zero
        var fixedWidthPx = 0

        rule.setContent {
            val density = LocalDensity.current
            fixedWidthPx = with(density) { 240.dp.roundToPx() }
            HorizontalText(
                text = "Hi",
                modifier = Modifier.fixedWidth(fixedWidthPx).onSizeChanged { measuredSize = it },
                style = style,
            )
        }

        rule.runOnIdle {
            assertThat(measuredSize.width).isEqualTo(fixedWidthPx)
            assertThat(measuredSize.height).isGreaterThan(0)
        }
    }

    @Test
    fun unboundedConstraints_doesNotCrashAndWrapsWidth() {
        val style = TextStyle(fontSize = 20.sp)
        var childSize = IntSize.Zero
        var expectedWidth = 0

        rule.setContent {
            val density = LocalDensity.current
            expectedWidth =
                HorizontalTextLayoutCache()
                    .getLayout(
                        text = "Hello world",
                        minWidth = 0,
                        maxWidth = Constraints.Infinity,
                        layoutDirection = LayoutDirection.Ltr,
                        style = style,
                        typeface = Typeface.DEFAULT,
                        density = density,
                    )
                    .width
            Layout(content = { HorizontalText(text = "Hello world", style = style) }) {
                measurables,
                constraints ->
                val placeable =
                    measurables
                        .single()
                        .measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                childSize = IntSize(placeable.width, placeable.height)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.runOnIdle {
            assertThat(childSize.width).isGreaterThan(0)
            assertThat(childSize.width).isEqualTo(expectedWidth)
            assertThat(childSize.height).isGreaterThan(0)
        }
    }

    @Test
    fun firstLineBottom_equalsSingleLineHeight() {
        val style = TextStyle(fontSize = 24.sp)
        var firstLineBottom = AlignmentLine.Unspecified
        var height = 0

        rule.setContent {
            Layout(content = { HorizontalText(text = "Hello", style = style) }) {
                measurables,
                constraints ->
                val placeable = measurables.single().measure(constraints)
                firstLineBottom = placeable[FirstLineBottom]
                height = placeable.height
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.runOnIdle {
            assertThat(firstLineBottom).isNotEqualTo(AlignmentLine.Unspecified)
            assertThat(firstLineBottom).isGreaterThan(0)
            assertThat(firstLineBottom).isEqualTo(height)
        }
    }

    @Test
    fun firstLineBottom_isUnchangedByExtraLines() {
        val style = TextStyle(fontSize = 24.sp)
        var twoLineFirstLineBottom = AlignmentLine.Unspecified
        var twoLineHeight = 0
        var threeLineFirstLineBottom = AlignmentLine.Unspecified
        var threeLineHeight = 0

        rule.setContent {
            Layout(
                content = {
                    HorizontalText(text = "Line 1\nLine 2", style = style)
                    HorizontalText(text = "Line 1\nLine 2\nLine 3", style = style)
                }
            ) { measurables, constraints ->
                val twoLinePlaceable = measurables[0].measure(constraints)
                val threeLinePlaceable = measurables[1].measure(constraints)
                twoLineFirstLineBottom = twoLinePlaceable[FirstLineBottom]
                twoLineHeight = twoLinePlaceable.height
                threeLineFirstLineBottom = threeLinePlaceable[FirstLineBottom]
                threeLineHeight = threeLinePlaceable.height
                layout(threeLinePlaceable.width, threeLinePlaceable.height) {
                    twoLinePlaceable.place(0, 0)
                    threeLinePlaceable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(twoLineFirstLineBottom).isNotEqualTo(AlignmentLine.Unspecified)
            assertThat(threeLineFirstLineBottom).isEqualTo(twoLineFirstLineBottom)
            assertThat(threeLineHeight).isGreaterThan(twoLineHeight)
        }
    }

    @Test
    fun firstLineBottom_growsWithRubySpanOnFirstLine() {
        val style = TextStyle(fontSize = 24.sp)
        val plainText = "漢字\n二行目"
        val rubyText =
            SpannableString(plainText).apply {
                setSpan(RubySpan("かんじ"), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        var plainFirstLineBottom = AlignmentLine.Unspecified
        var rubyFirstLineBottom = AlignmentLine.Unspecified

        rule.setContent {
            Layout(
                content = {
                    HorizontalText(text = plainText, style = style)
                    HorizontalText(text = rubyText, style = style)
                }
            ) { measurables, constraints ->
                val plainPlaceable = measurables[0].measure(constraints)
                val rubyPlaceable = measurables[1].measure(constraints)
                plainFirstLineBottom = plainPlaceable[FirstLineBottom]
                rubyFirstLineBottom = rubyPlaceable[FirstLineBottom]
                layout(rubyPlaceable.width, rubyPlaceable.height) {
                    plainPlaceable.place(0, 0)
                    rubyPlaceable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(plainFirstLineBottom).isNotEqualTo(AlignmentLine.Unspecified)
            assertThat(rubyFirstLineBottom).isGreaterThan(plainFirstLineBottom)
        }
    }

    @Test
    fun firstBaseline_and_lastBaseline_provided() {
        val style = TextStyle(fontSize = 24.sp)
        var firstBaseline = AlignmentLine.Unspecified
        var lastBaseline = AlignmentLine.Unspecified

        rule.setContent {
            Layout(content = { HorizontalText(text = "Line 1\nLine 2", style = style) }) {
                measurables,
                constraints ->
                val placeable = measurables.single().measure(constraints)
                firstBaseline = placeable[FirstBaseline]
                lastBaseline = placeable[LastBaseline]
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.runOnIdle {
            assertThat(firstBaseline).isNotEqualTo(AlignmentLine.Unspecified)
            assertThat(lastBaseline).isNotEqualTo(AlignmentLine.Unspecified)
            assertThat(firstBaseline).isGreaterThan(0)
            assertThat(lastBaseline).isGreaterThan(firstBaseline)
        }
    }

    @Test
    fun semanticsText_matchesInput() {
        val text = SpannableString("Hello Horizontal")

        rule.setContent { HorizontalText(text = text, style = TextStyle(fontSize = 24.sp)) }

        val node =
            rule
                .onNodeWithText("Hello Horizontal")
                .assertExists()
                .assertIsDisplayed()
                .fetchSemanticsNode()
        assertThat(node.config[SemanticsProperties.Text].single().text)
            .isEqualTo("Hello Horizontal")
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun clip_doesNotDrawPastBounds() {
        var childWidthPx = 0

        rule.setContent {
            val density = LocalDensity.current
            childWidthPx = with(density) { 40.dp.roundToPx() }
            val outerSizePx = with(density) { 120.dp.roundToPx() }
            // Center the test region in the window so system bars and ActionBar shadows do not
            // overlap the captured node on edge-to-edge API levels.
            Layout(
                content = {
                    Layout(
                        modifier = Modifier.drawBehind { drawRect(Color.White) }.testTag("outer"),
                        content = {
                            HorizontalText(
                                text = "WWWWWWWWWWWWWWWW",
                                modifier = Modifier.fixedSize(childWidthPx, childWidthPx),
                                style = TextStyle(color = Color.Red, fontSize = 36.sp),
                                overflow = TextOverflow.Clip,
                            )
                        },
                    ) { measurables, _ ->
                        val placeable =
                            measurables
                                .single()
                                .measure(
                                    Constraints.fixed(width = childWidthPx, height = childWidthPx)
                                )
                        layout(outerSizePx, outerSizePx) { placeable.place(0, 0) }
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables.single().measure(Constraints())
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(
                        (constraints.maxWidth - placeable.width) / 2,
                        (constraints.maxHeight - placeable.height) / 2,
                    )
                }
            }
        }

        val bitmap = rule.onNodeWithTag("outer").captureToImage().asAndroidBitmap()
        val white = Color.White.toArgb()
        var nonWhiteInside = 0
        var nonWhiteOutside = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (x < childWidthPx && y < childWidthPx) {
                    if (pixel != white) nonWhiteInside++
                } else if (x >= childWidthPx + 2 || y >= childWidthPx + 2) {
                    if (pixel != white) nonWhiteOutside++
                }
            }
        }
        assertThat(nonWhiteInside).isGreaterThan(0)
        assertThat(nonWhiteOutside).isEqualTo(0)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun visible_drawsPastBounds() {
        var childWidthPx = 0

        rule.setContent {
            val density = LocalDensity.current
            childWidthPx = with(density) { 40.dp.roundToPx() }
            val outerSizePx = with(density) { 120.dp.roundToPx() }
            Layout(
                content = {
                    Layout(
                        modifier = Modifier.drawBehind { drawRect(Color.White) }.testTag("outer"),
                        content = {
                            HorizontalText(
                                text = "WWWWWWWWWWWWWWWW",
                                modifier = Modifier.fixedSize(childWidthPx, childWidthPx),
                                style = TextStyle(color = Color.Red, fontSize = 36.sp),
                                overflow = TextOverflow.Visible,
                            )
                        },
                    ) { measurables, _ ->
                        val placeable =
                            measurables
                                .single()
                                .measure(
                                    Constraints.fixed(width = childWidthPx, height = childWidthPx)
                                )
                        layout(outerSizePx, outerSizePx) { placeable.place(0, 0) }
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables.single().measure(Constraints())
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(
                        (constraints.maxWidth - placeable.width) / 2,
                        (constraints.maxHeight - placeable.height) / 2,
                    )
                }
            }
        }

        val bitmap = rule.onNodeWithTag("outer").captureToImage().asAndroidBitmap()
        val white = Color.White.toArgb()
        var nonWhiteInside = 0
        var nonWhiteOutside = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (x < childWidthPx && y < childWidthPx) {
                    if (pixel != white) nonWhiteInside++
                } else if (x >= childWidthPx + 2 || y >= childWidthPx + 2) {
                    if (pixel != white) nonWhiteOutside++
                }
            }
        }
        assertThat(nonWhiteInside).isGreaterThan(0)
        assertThat(nonWhiteOutside).isGreaterThan(0)
    }
}

private fun Modifier.maxWidth(maxWidthPx: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = maxWidthPx))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private fun Modifier.fixedWidth(widthPx: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = widthPx, maxWidth = widthPx))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private fun Modifier.fixedSize(widthPx: Int, heightPx: Int): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints.fixed(widthPx, heightPx))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}
