/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import androidx.compose.Composable
import androidx.compose.Model
import androidx.test.filters.MediumTest
import androidx.ui.core.Layout
import androidx.ui.core.TestTag
import androidx.ui.core.Text
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Row
import androidx.ui.semantics.ScrollTo
import androidx.ui.semantics.Semantics
import androidx.ui.text.TextStyle
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.sp
import androidx.ui.unit.toRect
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class IsDisplayedTests {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun componentInScrollable_isDisplayed() {
        createScrollableContent()

        findByText("2")
            .assertIsDisplayed()
    }

    @Test
    fun componentInScrollable_isNotDisplayed() {
        createScrollableContent()

        findByText("50")
            .assertIsNotDisplayed()
    }

    private fun createScrollableContent() {
        composeTestRule.setContent {
            val style = TextStyle(fontSize = 30.sp)
            VerticalScroller(modifier = LayoutPadding(10.dp)) {
                Column {
                    for (i in 1..100) {
                        Semantics(container = true) {
                            Text(text = i.toString(), style = style)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun toggleParentVisibility() {
        /*
        - topNode
        -- secondNode
        --- thirdNode
        ---- Text
         */

        val model = AssertsUiTestsModel(true)

        composeTestRule.setContent {
            val lastNode = @Composable {
                Center {
                    Semantics(container = true) {
                        Text("Foo")
                    }
                }
            }

            val thirdNode = @Composable {
                Layout({
                    lastNode()
                }) { measurables, constraints ->
                    val placeable = measurables[0].measure(constraints)
                    layout(0.ipx, 0.ipx) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            }

            val secondNode = @Composable {
                Layout({
                    thirdNode()
                }) { measurables, constraints ->
                    if (model.value) {
                        val placeable = measurables[0].measure(constraints)
                        layout(0.ipx, 0.ipx) {
                            placeable.place(0.ipx, 0.ipx)
                        }
                    } else {
                        layout(0.ipx, 0.ipx) {}
                    }
                }
            }

            val topNode = @Composable {
                Layout({
                    secondNode()
                }) { measurables, constraints ->
                    if (model.value) {
                        val placeable = measurables[0].measure(constraints)
                        layout(0.ipx, 0.ipx) {
                            placeable.place(0.ipx, 0.ipx)
                        }
                    } else {
                        layout(0.ipx, 0.ipx) {
                        }
                    }
                }
            }

            topNode()
        }

        findByText("Foo")
            .assertIsDisplayed()

        composeTestRule.runOnUiThread {
            model.value = false
        }

        findByText("Foo")
            .assertIsNotDisplayed()
    }

    @Test
    fun rowTooSmall() {
        composeTestRule.setContent {
            val style = TextStyle(fontSize = 30.sp)
            Center {
                // TODO(popam): remove this when a modifier can be used instead
                Layout({
                    Row {
                        for (i in 1..100) {
                            Semantics(container = true) {
                                Text(text = i.toString(), style = style)
                            }
                        }
                    }
                }) { measurables, constraints ->
                    val placeable =
                        measurables[0].measure(constraints.copy(maxWidth = IntPx.Infinity))
                    layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            }
        }

        findByText("90")
            .assertIsNotDisplayed()
    }

    @Test
    fun checkSemanticsAction_scrollTo_isCalled() {
        var wasScrollToCalled = false
        val tag = "myTag"

        composeTestRule.setContent {
            Semantics(container = true, properties = {
                ScrollTo(action = { _, _ ->
                    wasScrollToCalled = true
                })
            }) {
                Container {
                    TestTag(tag) {
                        Semantics(container = true) {
                            Container { }
                        }
                    }
                }
            }
        }

        composeTestRule.runOnIdleCompose {
            Assert.assertTrue(!wasScrollToCalled)
        }

        findByTag(tag)
            .doScrollTo()

        composeTestRule.runOnIdleCompose {
            Assert.assertTrue(wasScrollToCalled)
        }
    }

    @Test
    fun checkSemanticsAction_scrollTo_coordAreCorrect() {
        var currentScrollPositionY = 0.px
        var currentScrollPositionX = 0.px
        var elementHeight = 0.px
        val tag = "myTag"

        val drawRect = @Composable { color: Color ->
            Semantics(container = true) {
                Canvas(LayoutSize(100.dp)) {
                    val paint = Paint()
                    paint.color = color
                    paint.style = PaintingStyle.fill
                    drawRect(size.toRect(), paint)

                    elementHeight = size.height
                }
            }
        }

        composeTestRule.setContent {
            // Need to make the "scrolling" container the semantics boundary so that it
            // doesn't try to include the padding
            Semantics(container = true, properties = {
                ScrollTo(action = { x, y ->
                    currentScrollPositionY = y
                    currentScrollPositionX = x
                })
            }) {
                val red = Color(alpha = 0xFF, red = 0xFF, green = 0, blue = 0)
                val blue = Color(alpha = 0xFF, red = 0, green = 0, blue = 0xFF)
                val green = Color(alpha = 0xFF, red = 0, green = 0xFF, blue = 0)

                Column {
                    drawRect(red)
                    drawRect(blue)
                    TestTag(tag) {
                        drawRect(green)
                    }
                }
            }
        }

        composeTestRule.runOnIdleCompose {
            assertThat(currentScrollPositionY).isEqualTo(0.px)
            assertThat(currentScrollPositionX).isEqualTo(0.px)
        }

        findByTag(tag)
            .doScrollTo() // scroll to third element

        composeTestRule.runOnIdleCompose {
            val expected = elementHeight * 2
            assertThat(currentScrollPositionY).isEqualTo(expected)
            assertThat(currentScrollPositionX).isEqualTo(0.px)
        }
    }
}

@Model
class AssertsUiTestsModel(var value: Boolean)