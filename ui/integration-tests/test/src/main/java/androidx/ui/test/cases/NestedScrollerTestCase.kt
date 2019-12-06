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

package androidx.ui.test.cases

import androidx.compose.Composable
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.core.WithDensity
import androidx.ui.core.px
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.FlexColumn
import androidx.ui.foundation.HorizontalScroller
import androidx.ui.layout.Row
import androidx.ui.foundation.ScrollerPosition
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.LayoutExpandedWidth
import androidx.ui.layout.LayoutGravity
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.ToggleableTestCase
import androidx.ui.text.TextStyle
import kotlin.random.Random

/**
 * Test case that puts many horizontal scrollers in a vertical scroller
 */
class NestedScrollerTestCase : ComposeTestCase, ToggleableTestCase {
    private val scrollerPosition = ScrollerPosition()

    @Composable
    override fun emitContent() {
        MaterialTheme {
            Surface {
                VerticalScroller {
                    Column {
                        repeat(5) { index ->
                            SquareRow(index == 0)
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
    fun SquareRow(useScrollerPosition: Boolean) {
        val playStoreColor = Color(red = 0x00, green = 0x00, blue = 0x80)
        val content = @Composable {
            Row(LayoutExpandedWidth) {
                repeat(6) {
                    WithDensity {
                        FlexColumn {
                            val color = +memo {
                                val red = Random.nextInt(256)
                                val green = Random.nextInt(256)
                                val blue = Random.nextInt(256)
                                Color(red = red, green = green, blue = blue)
                            }
                            inflexible {
                                ColoredRect(
                                    width = 350.px.toDp(),
                                    height = 350.px.toDp(),
                                    color = color
                                )
                                Text(
                                    text = "Some title",
                                    style = TextStyle(Color.Black, 60.px.toSp())
                                )
                                Row(LayoutExpandedWidth) {
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
        }
        if (useScrollerPosition) {
            HorizontalScroller(scrollerPosition = scrollerPosition, child = content)
        } else {
            HorizontalScroller(child = content)
        }
    }
}
