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

package androidx.ui.layout.test

import android.view.View
import androidx.benchmark.BenchmarkRule
import androidx.benchmark.measureRepeated
import androidx.compose.Composable
import androidx.compose.CompositionContext
import androidx.compose.FrameManager
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.setContent
import androidx.compose.unaryPlus
import androidx.test.filters.FlakyTest
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.dp
import androidx.ui.core.px
import androidx.ui.core.toRect
import androidx.ui.core.withDensity
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.ScrollerPosition
import androidx.ui.layout.VerticalScroller
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScrollerPerformance : LayoutTest() {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    @FlakyTest
    fun benchmarkScrollLayout() {
        val scrollerPosition = ScrollerPosition()
        val compositionContext =
            composeScroller(scrollerPosition = scrollerPosition)

        val view = findAndroidCraneView()

        activityTestRule.runOnUiThread(object : Runnable {
            override fun run() {
                val widthSpec =
                    View.MeasureSpec.makeMeasureSpec(view.measuredWidth, View.MeasureSpec.EXACTLY)
                val heightSpec =
                    View.MeasureSpec.makeMeasureSpec(view.measuredHeight, View.MeasureSpec.EXACTLY)
                compositionContext.compose()
                view.measure(widthSpec, heightSpec)
                view.layout(view.left, view.top, view.right, view.bottom)

                val exec: BenchmarkRule.Scope.() -> Unit = {
                    runWithTimingDisabled {
                        if (scrollerPosition.position == 0.px) {
                            scrollerPosition.position = 10.px
                        } else {
                            scrollerPosition.position = 0.px
                        }
                        FrameManager.nextFrame()
                        compositionContext.recomposeSync()
                        view.requestLayout()
                    }
                    view.measure(widthSpec, heightSpec)
                    view.layout(view.left, view.top, view.right, view.bottom)
                }
                benchmarkRule.measureRepeated(exec)
            }
        })
    }

    @Test
    @FlakyTest
    fun benchmarkScrollComposition() {
        val scrollerPosition = ScrollerPosition()
        val compositionContext =
            composeScroller(scrollerPosition = scrollerPosition)

        activityTestRule.runOnUiThread(object : Runnable {
            override fun run() {
                compositionContext.compose()
                val exec: BenchmarkRule.Scope.() -> Unit = {
                    runWithTimingDisabled {
                        if (scrollerPosition.position == 0.px) {
                            scrollerPosition.position = 10.px
                        } else {
                            scrollerPosition.position = 0.px
                        }
                        FrameManager.nextFrame()
                    }
                    compositionContext.recomposeSync()
                }
                benchmarkRule.measureRepeated(exec)
            }
        })
    }

    @Test
    @FlakyTest
    fun benchmarkLargeComposition() {
        val scrollerPosition = ScrollerPosition()
        val compositionContext =
            composeScroller(scrollerPosition = scrollerPosition)

        activityTestRule.runOnUiThread(object : Runnable {
            override fun run() {
                compositionContext.compose()
                val exec: BenchmarkRule.Scope.() -> Unit = {
                    runWithTimingDisabled {
                        if (scrollerPosition.position == 0.px) {
                            scrollerPosition.position = 10.px
                        } else {
                            scrollerPosition.position = 0.px
                        }
                        FrameManager.nextFrame()
                    }
                    compositionContext.compose()
                }
                benchmarkRule.measureRepeated(exec)
            }
        })
    }

    fun composeScroller(
        scrollerPosition: ScrollerPosition = ScrollerPosition()
    ): CompositionContext {
        var compositionContext: CompositionContext? = null
        // We assume that the height of the device is more than 45 px
        withDensity(density) {
            val runnable: Runnable = object : Runnable {
                override fun run() {
                    compositionContext = activity.setContent {
                        CraneWrapper {
                            VerticalScroller(
                                scrollerPosition = scrollerPosition
                            ) {
                                Column(crossAxisAlignment = CrossAxisAlignment.Start) {
                                    for (green in 0..0xFF) {
                                        ColorStripe(0xFF, green, 0)
                                    }
                                    for (red in 0xFF downTo 0) {
                                        ColorStripe(red, 0xFF, 0)
                                    }
                                    for (blue in 0..0xFF) {
                                        ColorStripe(0, 0xFF, blue)
                                    }
                                    for (green in 0xFF downTo 0) {
                                        ColorStripe(0, green, 0xFF)
                                    }
                                    for (red in 0..0xFF) {
                                        ColorStripe(red, 0, 0xFF)
                                    }
                                    for (blue in 0xFF downTo 0) {
                                        ColorStripe(0xFF, 0, blue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            activityTestRule.runOnUiThread(runnable)
        }
        return compositionContext!!
    }
}

@Composable
fun ColorStripe(red: Int, green: Int, blue: Int) {
    val paint = +memo { Paint() }
    Container(height = 5.dp, width = 45.dp) {
        Draw { canvas, parentSize ->
            paint.color = Color(red = red, green = green, blue = blue)
            paint.style = PaintingStyle.fill
            canvas.drawRect(parentSize.toRect(), paint)
        }
    }
}
