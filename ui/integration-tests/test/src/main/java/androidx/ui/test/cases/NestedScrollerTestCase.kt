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

import android.app.Activity
import androidx.compose.composer
import androidx.compose.Composable
import androidx.compose.FrameManager
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.core.WithDensity
import androidx.ui.core.px
import androidx.ui.core.setContent
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.LayoutSize
import androidx.ui.foundation.HorizontalScroller
import androidx.ui.layout.Row
import androidx.ui.foundation.ScrollerPosition
import androidx.ui.foundation.VerticalScroller
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.ToggleableTestCase
import androidx.ui.text.TextStyle
import kotlin.random.Random

/**
 * Test case that puts many horizontal scrollers in a vertical scroller
 */
class NestedScrollerTestCase(
    activity: Activity
) : ComposeTestCase(activity), ToggleableTestCase {
    private val scrollerPosition = ScrollerPosition()

    override fun setComposeContent(activity: Activity) = activity.setContent {
        MaterialTheme {
            Surface {
                VerticalScroller {
                    Column(mainAxisSize = LayoutSize.Expand) {
                        repeat(5) { index ->
                            SquareRow(index == 0)
                        }
                    }
                }
            }
        }
    }!!

    override fun toggleState() {
        scrollerPosition.value = if (scrollerPosition.value == 0.px) 10.px else 0.px
        FrameManager.nextFrame()
    }

    @Composable
    fun SquareRow(useScrollerPosition: Boolean) {
        val playStoreColor = Color(red = 0x00, green = 0x00, blue = 0x80)
        val content = @Composable {
            Row(mainAxisSize = LayoutSize.Expand) {
                repeat(6) {
                    WithDensity {
                        FlexColumn(crossAxisAlignment = CrossAxisAlignment.Start) {
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
                                    color = color)
                                Text(
                                    text = "Some title",
                                    style = TextStyle(Color.Black, 60.px.toSp())
                                )
                                Row(
                                    mainAxisSize = LayoutSize.Expand,
                                    crossAxisAlignment = CrossAxisAlignment.Center
                                ) {
                                    Text("3.5 ★", TextStyle(fontSize = 40.px.toSp()))
                                    ColoredRect(
                                        width = 40.px.toDp(),
                                        height = 40.px.toDp(),
                                        color = playStoreColor
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
