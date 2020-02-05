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

package androidx.ui.integration.test.foundation

import androidx.compose.Composable
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Text
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.HorizontalScroller
import androidx.ui.foundation.ScrollerPosition
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.integration.test.ToggleableTestCase
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.test.ComposeTestCase
import androidx.ui.text.TextStyle
import androidx.ui.unit.px
import kotlin.random.Random

/**
 * Test case that puts many horizontal scrollers in a vertical scroller
 */
class NestedScrollerTestCase : ComposeTestCase, ToggleableTestCase {
    // ScrollerPosition must now be constructed during composition to obtain the Density
    private lateinit var scrollerPosition: ScrollerPosition

    @Composable
    override fun emitContent() {
        val scrollerPosition = ScrollerPosition()
        onCommit(true) {
            this@NestedScrollerTestCase.scrollerPosition = scrollerPosition
        }
        MaterialTheme {
            Surface {
                VerticalScroller {
                    Column {
                        repeat(5) { index ->
                            if (index == 0) SquareRow(scrollerPosition)
                            else SquareRow()
                        }
                    }
                }
            }
        }
    }

    override fun toggleState() {
        scrollerPosition.scrollTo(if (scrollerPosition.value == 0.px) 10.px else 0.px)
    }

    @Composable
    fun SquareRow(scrollerPosition: ScrollerPosition = ScrollerPosition()) {
        val playStoreColor = Color(red = 0x00, green = 0x00, blue = 0x80)
        val content = @Composable {
            Row(LayoutWidth.Fill) {
                repeat(6) {
                    with(DensityAmbient.current) {
                        Column(LayoutHeight.Fill) {
                            val color = remember {
                                val red = Random.nextInt(256)
                                val green = Random.nextInt(256)
                                val blue = Random.nextInt(256)
                                Color(red = red, green = green, blue = blue)
                            }
                            ColoredRect(
                                width = 350.px.toDp(),
                                height = 350.px.toDp(),
                                color = color
                            )
                            Text(
                                text = "Some title",
                                style = TextStyle(Color.Black, 60.px.toSp())
                            )
                            Row(LayoutWidth.Fill) {
                                Text(
                                    "3.5 ★",
                                    style = TextStyle(fontSize = 40.px.toSp()),
                                    modifier = LayoutGravity.Center
                                )
                                ColoredRect(
                                    width = 40.px.toDp(),
                                    height = 40.px.toDp(),
                                    color = playStoreColor,
                                    modifier = LayoutGravity.Center
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalScroller(scrollerPosition = scrollerPosition, child = content)
    }
}
