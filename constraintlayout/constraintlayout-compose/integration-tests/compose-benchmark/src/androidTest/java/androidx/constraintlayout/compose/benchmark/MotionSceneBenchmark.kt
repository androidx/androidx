/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.constraintlayout.compose.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Easing
import androidx.constraintlayout.compose.MotionScene
import androidx.constraintlayout.compose.OnSwipe
import androidx.constraintlayout.compose.SwipeDirection
import androidx.constraintlayout.compose.SwipeMode
import androidx.constraintlayout.compose.SwipeSide
import androidx.constraintlayout.compose.SwipeTouchUp
import androidx.constraintlayout.compose.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MotionSceneBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    /**
     * One of the most basics MotionScenes.
     *
     * Just a box moving from one corner to the other. Fairly minimal example.
     */
    @Test
    fun motionScene_simple() {
        benchmarkRule.measureRepeated {
            MotionScene {
                val boxRef = createRefFor("box")
                defaultTransition(
                    from = constraintSet {
                        constrain(boxRef) {
                            width = 50.dp.asDimension()
                            height = 50.dp.asDimension()

                            top.linkTo(parent.top, 8.dp)
                            start.linkTo(parent.start, 8.dp)
                        }
                    },
                    to = constraintSet {
                        constrain(boxRef) {
                            width = 50.dp.asDimension()
                            height = 50.dp.asDimension()

                            bottom.linkTo(parent.bottom, 8.dp)
                            end.linkTo(parent.end, 8.dp)
                        }
                    }
                )
            }
        }
    }

    /**
     * The MotionScene was mostly a copy of `messageMotionScene()` from NewMessage.kt in the
     * macrobenchmark-target module.
     *
     * It's been modified to represent a more complex scenario. Does not necessarily have to make
     * sense since it's for benchmarking.
     */
    @Test
    fun motionScene_complex() {
        val primary = Color(0xFFF44336)
        val primaryVariant = Color(0xFFE91E63)
        val onPrimary = Color(0xFF673AB7)
        val surface = Color(0xFF3F51B5)
        val onSurface = Color(0xFF2196F3)

        benchmarkRule.measureRepeated {
            MotionScene {
                val (box, minIcon, editClose, title, content) =
                    createRefsFor("box", "minIcon", "editClose", "title", "content")

                val fab = constraintSet(NewMessageLayout.Fab.name) {
                    constrain(box) {
                        width = Dimension.value(50.dp)
                        height = Dimension.value(50.dp)
                        end.linkTo(parent.end, 12.dp)
                        bottom.linkTo(parent.bottom, 12.dp)

                        customColor("background", primary)

                        staggeredWeight = 1f
                    }
                    constrain(minIcon) {
                        width = Dimension.value(40.dp)
                        height = Dimension.value(40.dp)

                        end.linkTo(editClose.start, 8.dp)
                        top.linkTo(editClose.top)
                        customColor("content", onPrimary)
                    }
                    constrain(editClose) {
                        width = Dimension.value(40.dp)
                        height = Dimension.value(40.dp)

                        centerTo(box)

                        customColor("content", onPrimary)
                    }
                    constrain(title) {
                        width = Dimension.fillToConstraints
                        top.linkTo(box.top)
                        bottom.linkTo(editClose.bottom)
                        start.linkTo(box.start, 8.dp)
                        end.linkTo(minIcon.start, 8.dp)
                        customColor("content", onPrimary)

                        visibility = Visibility.Gone
                    }
                    constrain(content) {
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                        start.linkTo(box.start, 8.dp)
                        end.linkTo(box.end, 8.dp)

                        top.linkTo(editClose.bottom, 8.dp)
                        bottom.linkTo(box.bottom, 8.dp)

                        visibility = Visibility.Gone
                    }
                }
                val full = constraintSet(NewMessageLayout.Full.name) {
                    constrain(box) {
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                        start.linkTo(parent.start, 12.dp)
                        end.linkTo(parent.end, 12.dp)
                        bottom.linkTo(parent.bottom, 12.dp)
                        top.linkTo(parent.top, 40.dp)
                        customColor("background", surface)
                    }
                    constrain(minIcon) {
                        width = Dimension.value(40.dp)
                        height = Dimension.value(40.dp)

                        end.linkTo(editClose.start, 8.dp)
                        top.linkTo(editClose.top)
                        customColor("content", onSurface)
                    }
                    constrain(editClose) {
                        width = Dimension.value(40.dp)
                        height = Dimension.value(40.dp)

                        end.linkTo(box.end, 4.dp)
                        top.linkTo(box.top, 4.dp)
                        customColor("content", onSurface)
                    }
                    constrain(title) {
                        width = Dimension.fillToConstraints
                        top.linkTo(box.top)
                        bottom.linkTo(editClose.bottom)
                        start.linkTo(box.start, 8.dp)
                        end.linkTo(minIcon.start, 8.dp)
                        customColor("content", onSurface)
                    }
                    constrain(content) {
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                        start.linkTo(box.start, 8.dp)
                        end.linkTo(box.end, 8.dp)
                        top.linkTo(editClose.bottom, 8.dp)
                        bottom.linkTo(box.bottom, 8.dp)
                    }
                }
                val mini = constraintSet(NewMessageLayout.Mini.name) {
                    constrain(box) {
                        width = Dimension.value(220.dp)
                        height = Dimension.value(50.dp)

                        end.linkTo(parent.end, 12.dp)
                        bottom.linkTo(parent.bottom, 12.dp)

                        customColor("background", primaryVariant)
                    }
                    constrain(minIcon) {
                        width = Dimension.value(40.dp)
                        height = Dimension.value(40.dp)

                        end.linkTo(editClose.start, 8.dp)
                        top.linkTo(editClose.top)

                        rotationZ = 180f

                        customColor("content", onPrimary)
                    }
                    constrain(editClose) {
                        width = Dimension.value(40.dp)
                        height = Dimension.value(40.dp)

                        end.linkTo(box.end, 4.dp)
                        top.linkTo(box.top, 4.dp)
                        customColor("content", onPrimary)
                    }
                    constrain(title) {
                        width = Dimension.fillToConstraints
                        top.linkTo(box.top)
                        bottom.linkTo(editClose.bottom)
                        start.linkTo(box.start, 8.dp)
                        end.linkTo(minIcon.start, 8.dp)
                        customColor("content", onPrimary)
                    }
                    constrain(content) {
                        width = Dimension.fillToConstraints
                        start.linkTo(box.start, 8.dp)
                        end.linkTo(box.end, 8.dp)

                        top.linkTo(editClose.bottom, 8.dp)
                        bottom.linkTo(box.bottom, 8.dp)

                        visibility = Visibility.Gone
                    }
                }

                fun constraintSetFor(layoutState: NewMessageLayout) =
                    when (layoutState) {
                        NewMessageLayout.Full -> full
                        NewMessageLayout.Mini -> mini
                        NewMessageLayout.Fab -> fab
                    }
                defaultTransition(
                    from = constraintSetFor(NewMessageLayout.Fab),
                    to = constraintSetFor(NewMessageLayout.Full)
                ) {
                    maxStaggerDelay = 0.6f

                    keyAttributes(title, content) {
                        frame(30) {
                            alpha = 0.5f
                        }
                        frame(60) {
                            alpha = 0.9f
                        }
                    }
                }

                transition(
                    from = constraintSetFor(NewMessageLayout.Full),
                    to = constraintSetFor(NewMessageLayout.Mini)
                ) {
                    onSwipe = OnSwipe(
                        anchor = editClose,
                        side = SwipeSide.Middle,
                        direction = SwipeDirection.Down,
                        onTouchUp = SwipeTouchUp.AutoComplete,
                        mode = SwipeMode.spring(threshold = 0.001f)
                    )

                    keyCycles(minIcon) {
                        easing = Easing.cubic(x1 = 0.3f, y1 = 0.2f, x2 = 0.8f, y2 = 0.7f)
                        frame(50) {
                            rotationZ = 90f
                            period = 4f
                        }
                    }
                }
            }
        }
    }

    private enum class NewMessageLayout {
        Full,
        Mini,
        Fab
    }
}
