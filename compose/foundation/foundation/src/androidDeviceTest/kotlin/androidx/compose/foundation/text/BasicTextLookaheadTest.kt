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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class BasicTextLookaheadTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun lookaheadText_centerAligned_translatesCanvasInApproachPass() {
        val containerTag = "container"
        val textWidthLookahead = 100
        val textWidthApproach = 300
        val textHeight = 50

        val text = buildAnnotatedString {
            withStyle(SpanStyle(background = Color.Red)) { append("XXXXXXXXXX") }
        }

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Box(
                        modifier =
                            Modifier.testTag(containerTag).background(Color.Gray).layout {
                                measurable,
                                _ ->
                                val width =
                                    if (isLookingAhead) textWidthLookahead else textWidthApproach
                                val childConstraints = Constraints.fixed(width, textHeight)
                                val placeable = measurable.measure(childConstraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        BasicText(
                            text = text,
                            style =
                                TextStyle(
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color.Transparent,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        val bitmap = rule.onNodeWithTag(containerTag).captureToImage()

        // Since it is Center-aligned, the 100px lookahead text should be translated
        // by (300 - 100) / 2 = 100px.
        // Red background is drawn in the center region (X = 100..199).
        // Side regions (X = 25..75 and X = 225..275) must be Gray.
        bitmap.assertPixels(expectedSize = IntSize(textWidthApproach, textHeight)) { position ->
            val x = position.x
            val y = position.y
            if (x in 25..75 && y in 20..30) {
                Color.Gray
            } else if (x in 225..275 && y in 20..30) {
                Color.Gray
            } else if (x in 140..160 && y in 20..30) {
                Color.Red
            } else {
                null
            }
        }
    }

    @Test
    fun lookaheadText_placeholder_stableTargetSlotsInApproachPass() {
        val containerTag = "container"
        val textWidthLookahead = 100
        val textWidthApproach = 300
        val textHeight = 50
        val placeholderWidth = 20
        val placeholderHeight = 20

        val inlineContentId = "inlineContent"
        var reportedPlaceholderRects: List<Rect?>? = null

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val density = LocalDensity.current
                val placeholderWidthSp = with(density) { placeholderWidth.toSp() }
                val placeholderHeightSp = with(density) { placeholderHeight.toSp() }

                val inlineContent =
                    remember(placeholderWidthSp, placeholderHeightSp) {
                        mapOf(
                            inlineContentId to
                                InlineTextContent(
                                    Placeholder(
                                        width = placeholderWidthSp,
                                        height = placeholderHeightSp,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Blue))
                                }
                        )
                    }

                val text = remember {
                    buildAnnotatedString { appendInlineContent(inlineContentId, "[alternate]") }
                }

                LookaheadScope {
                    Box(
                        modifier =
                            Modifier.testTag(containerTag).background(Color.Gray).layout {
                                measurable,
                                _ ->
                                val width =
                                    if (isLookingAhead) textWidthLookahead else textWidthApproach
                                val childConstraints = Constraints.fixed(width, textHeight)
                                val placeable = measurable.measure(childConstraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        BasicText(
                            text = text,
                            style = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center),
                            inlineContent = inlineContent,
                            onTextLayout = { layoutResult ->
                                reportedPlaceholderRects = layoutResult.placeholderRects
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        assertThat(reportedPlaceholderRects).isNotNull()
        assertThat(reportedPlaceholderRects).hasSize(1)
        val rect = reportedPlaceholderRects!![0]
        assertThat(rect).isNotNull()
        assertThat(rect!!.width).isEqualTo(placeholderWidth.toFloat())
        assertThat(rect.height).isEqualTo(placeholderHeight.toFloat())
    }

    @Test
    fun lookaheadText_selectionHighlight_clearsSelectionInApproachPass() {
        val containerTag = "container"
        val textTag = "text"
        val textWidthLookahead = 100
        val textWidthApproach = 300
        val textHeight = 50

        val customSelectionColors =
            TextSelectionColors(
                handleColor = Color.Transparent, // hide handles
                backgroundColor = Color.Blue, // Blue selection background highlight
            )

        var inAnimation by mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(
                LocalTextSelectionColors provides customSelectionColors,
                LocalDensity provides Density(1f),
            ) {
                LookaheadScope {
                    Box(
                        modifier =
                            Modifier.testTag(containerTag).background(Color.Gray).layout {
                                measurable,
                                _ ->
                                val width =
                                    if (isLookingAhead || !inAnimation) {
                                        textWidthLookahead
                                    } else {
                                        textWidthApproach
                                    }
                                val childConstraints = Constraints.fixed(width, textHeight)
                                val placeable = measurable.measure(childConstraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    ) {
                        SelectionContainer {
                            BasicText(
                                text = "SELECTABLE",
                                style =
                                    TextStyle(
                                        fontSize = 20.sp,
                                        textAlign = TextAlign.Center,
                                        color = Color.Transparent,
                                    ),
                                modifier = Modifier.fillMaxWidth().testTag(textTag),
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()

        // 1. Select the text before animation while settled at lookahead size (100px)
        rule.onNodeWithTag(textTag).performTouchInput { longClick() }
        rule.waitForIdle()

        // 2. Trigger the approach animation / size change to 300px
        inAnimation = true
        rule.waitForIdle()

        val bitmap = rule.onNodeWithTag(containerTag).captureToImage()

        // Selection is cleared on transition start to prevent detached handles/highlight during
        // morphing.
        // Entire region (including center X = 140..160) must now be Gray (no Blue highlight).
        bitmap.assertPixels(expectedSize = IntSize(textWidthApproach, textHeight)) { position ->
            val x = position.x
            val y = position.y
            if (x in 50..70 && y in 11..15) {
                Color.Gray
            } else if (x in 230..250 && y in 11..15) {
                Color.Gray
            } else if (x in 140..160 && y in 11..15) {
                Color.Gray
            } else {
                null
            }
        }
    }
}
