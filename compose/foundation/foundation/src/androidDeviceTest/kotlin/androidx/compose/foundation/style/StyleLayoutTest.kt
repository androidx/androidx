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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class StyleLayoutTest {
    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    @Test
    fun testPadding() {
        rule
            .onParentWith(parent = { contentPadding(10.dp) }, child = { size(10.dp) })
            .assertWidthIsEqualTo(30.dp) // 10.dp (child) + 10.dp (padding) + 10.dp (padding)
            .assertHeightIsEqualTo(30.dp) // 10.dp (child) + 10.dp (padding) + 10.dp (padding)
    }

    @Test
    fun testWidth() {
        rule.onLeafWith { width(100.dp) }.assertWidthIsEqualTo(100.dp)
    }

    @Test
    fun testHeight() {
        rule.onLeafWith { height(100.dp) }.assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun testSize() {
        rule.onLeafWith { size(100.dp) }.assertWidthIsEqualTo(100.dp).assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun testSizeWithWidthAndHeight() {
        rule
            .onLeafWith { size(width = 100.dp, height = 50.dp) }
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun testMinWidth() {
        rule.onLeafWith { minWidth(100.dp) }.assertWidthIsEqualTo(100.dp)
    }

    @Test
    @Ignore("maxWith not implemented yet")
    fun testMaxWidth() {
        rule
            .onChildWith(
                parent = { size(50.dp) },
                child = {
                    width(100.dp)
                    maxWidth(80.dp)
                },
            )
            .assertWidthIsEqualTo(50.dp) // Limited by parent

        rule
            .onLeafWith {
                size(200.dp)
                maxWidth(100.dp)
            }
            .assertWidthIsEqualTo(100.dp) // Limited by maxWidth
    }

    @Test
    fun testMinHeight() {
        rule.onLeafWith { minHeight(100.dp) }.assertHeightIsEqualTo(100.dp)
    }

    @Test
    @Ignore("maxHeight not implemented yet")
    fun testMaxHeight() {
        rule
            .onChildWith(
                parent = { size(50.dp) },
                child = {
                    height(100.dp)
                    maxHeight(80.dp)
                },
            )
            .assertHeightIsEqualTo(50.dp) // Limited by parent

        rule
            .onLeafWith {
                size(200.dp)
                maxHeight(100.dp)
            }
            .assertHeightIsEqualTo(100.dp) // limited by maxHeight
    }

    @Test
    fun testFillWidth() {
        rule
            .onChildWith(parent = { size(100.dp) }, child = { fillWidth() })
            .assertWidthIsEqualTo(100.dp)
    }

    @Test
    fun testFillHeight() {
        rule
            .onChildWith(parent = { size(100.dp) }, child = { fillHeight() })
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun testFillSize() {
        rule
            .onChildWith(parent = { size(100.dp) }, child = { fillSize() })
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun testFractionalWidth() {
        rule
            .onChildWith(parent = { size(100.dp) }, child = { width(0.5f) })
            .assertWidthIsEqualTo(50.dp)
    }

    @Test
    fun testFractionalHeight() {
        rule
            .onChildWith(parent = { size(100.dp) }, child = { height(0.5f) })
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun testPadding_individual_sides() {
        rule
            .onParentWith(
                parent = {
                    contentPaddingStart(5.dp)
                    contentPaddingTop(10.dp)
                    contentPaddingEnd(15.dp)
                    contentPaddingBottom(20.dp)
                },
                child = { size(50.dp) },
            )
            .assertWidthIsEqualTo(50.dp + 5.dp + 15.dp)
            .assertHeightIsEqualTo(50.dp + 10.dp + 20.dp)
    }

    @Test // 483067194
    fun test_border_width_as_additional_padding() {
        rule
            .onParentWith(
                parent = {
                    border(10.dp, Color.Red)
                    contentPadding(5.dp)
                },
                child = { size(50.dp) },
            )
            .assertWidthIsEqualTo(50.dp + 10.dp + 10.dp + 5.dp + 5.dp)
            .assertHeightIsEqualTo(50.dp + 10.dp + 10.dp + 5.dp + 5.dp)
    }

    @Test
    fun testPadding_horizontal_vertical() {
        rule
            .onParentWith(
                parent = {
                    contentPaddingHorizontal(10.dp)
                    contentPaddingVertical(20.dp)
                },
                child = { size(50.dp) },
            )
            .assertWidthIsEqualTo(50.dp + 10.dp * 2)
            .assertHeightIsEqualTo(50.dp + 20.dp * 2)
    }

    @Test
    fun testOffset() {
        val tag = "offsetTag"
        rule.setContent {
            StyledBox(
                style = {
                    left(10.dp)
                    top(20.dp)
                }
            ) {
                StyledBox(modifier = Modifier.testTag(tag))
            }
        }
        rule
            .onNodeWithTag(tag)
            .assertLeftPositionInRootIsEqualTo(10.dp)
            .assertTopPositionInRootIsEqualTo(20.dp)
    }

    @Test
    fun testBorderSize() {
        rule
            .onLeafWith {
                size(100.dp)
                border(10.dp, Color.Red)
            }
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun testPadding_all_sides_overload() {
        rule
            .onParentWith(
                parent = { contentPadding(start = 5.dp, top = 10.dp, end = 15.dp, bottom = 20.dp) },
                child = { size(50.dp) },
            )
            .assertWidthIsEqualTo(50.dp + 5.dp + 15.dp)
            .assertHeightIsEqualTo(50.dp + 10.dp + 20.dp)
    }

    @Test
    fun testOffset_positioning_right_bottom() {
        val childTag = "child"
        rule.setContent {
            StyledBox(
                style = {
                    size(100.dp)
                    background(Color.Blue)
                }
            ) {
                StyledBox(
                    style = {
                        size(20.dp)
                        right(10.dp)
                        bottom(30.dp)
                        background(Color.Red)
                    }
                ) {
                    StyledBox(modifier = Modifier.testTag(childTag), style = { fillSize() })
                }
            }
        }
        rule
            .onNodeWithTag(childTag)
            .assertLeftPositionInRootIsEqualTo(100.dp - 20.dp - 10.dp)
            .assertTopPositionInRootIsEqualTo(100.dp - 20.dp - 30.dp)
    }

    @Test
    fun testText_simple_text() {
        rule.setContent {
            StyledColumn(
                style = {
                    background(Color.Blue)
                    contentPadding(5.dp)
                    contentColor(Color.Yellow)
                    fontWeight(FontWeight.ExtraBold)
                    fontSize(50.sp)
                }
            ) {
                StyledBox(
                    style = {
                        background(Color.Red)
                        contentPadding(5.dp)
                    }
                ) {
                    BasicText("Some text")
                }
                StyledBox(
                    style = {
                        fontSize(25.sp)
                        background(Color.Green)
                    }
                ) {
                    BasicText("Smaller")
                }
            }
        }
    }
}

@Composable
private inline fun StyledColumn(
    modifier: Modifier = Modifier,
    style: Style = Style,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.styleable(null, style),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

@Composable
private inline fun StyledBox(
    modifier: Modifier = Modifier,
    style: Style = Style,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.styleable(null, style),
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints,
        content = content,
    )
}

@Composable
private fun StyledBox(modifier: Modifier = Modifier, style: Style = Style) {
    Box(modifier = modifier.styleable(null, style))
}

private fun ComposeContentTestRule.onLeafWith(style: Style): SemanticsNodeInteraction {
    val tag = "style_layout_test_leaf"
    setContent { StyledBox(Modifier.testTag(tag), style) }

    return onNodeWithTag(tag)
}

private fun ComposeContentTestRule.onParentWith(
    parent: Style,
    child: Style,
): SemanticsNodeInteraction {
    val tag = "style_layout_test_parent"
    setContent {
        Box(Modifier.testTag(tag).styleable(null, parent)) {
            Box(modifier = Modifier.styleable(null, child))
        }
    }

    return onNodeWithTag(tag)
}

private fun ComposeContentTestRule.onChildWith(
    parent: Style,
    child: Style,
): SemanticsNodeInteraction {
    val tag = "style_layout_test_child"
    setContent {
        Box(modifier = Modifier.styleable(null, parent)) {
            Box(Modifier.testTag(tag).styleable(null, child))
        }
    }

    return onNodeWithTag(tag)
}
