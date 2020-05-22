/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.tag
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.layout.ConstraintLayout
import androidx.ui.layout.ConstraintSet
import androidx.ui.layout.ExperimentalLayout
import androidx.ui.layout.aspectRatio
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.layout.rtl
import androidx.ui.layout.wrapContentSize
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalLayout::class)
class ConstraintLayoutTest : LayoutTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    // region sizing tests

    @Test
    fun dividerMatchTextHeight_spread() = with(density) {
        val aspectRatioBoxSize = Ref<IntPxSize>()
        val dividerSize = Ref<IntPxSize>()
        composeTestRule.setContent {
            ConstraintLayout(
                // Make CL fixed width and wrap content height.
                modifier = Modifier.wrapContentSize(Alignment.TopStart).fillMaxWidth(),
                constraintSet = ConstraintSet {
                    val aspectRatioBox = tag("aspectRatioBox")
                    val divider = tag("divider")
                    val guideline = createGuidelineFromLeft(0.5f)

                    aspectRatioBox constrainTo parent
                    aspectRatioBox.left constrainTo guideline
                    divider constrainTo parent

                    aspectRatioBox.width = wrap
                    aspectRatioBox.height = wrapFixed
                    divider.width = valueFixed(1.dp)
                    divider.height = spread
                }
            ) {
                Box(Modifier.tag("aspectRatioBox")
                    // Try to be large to make wrap content impossible.
                    .preferredWidth((composeTestRule.displayMetrics.widthPixels).ipx.toDp())
                    // This could be any (width in height out child) e.g. text
                    .aspectRatio(2f)
                    .onPositioned { coordinates ->
                        aspectRatioBoxSize.value = coordinates.size
                    }
                )
                Box(Modifier.tag("divider")
                    .onPositioned { coordinates ->
                        dividerSize.value = coordinates.size
                    }
                )
            }
        }

        runOnIdleCompose {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (composeTestRule.displayMetrics.widthPixels / 2).ipx,
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (composeTestRule.displayMetrics.widthPixels / 2 / 2).ipx,
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.toIntPx(), dividerSize.value!!.width)
            // Divider has spread height so it should spread to fill the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            assertEquals(aspectRatioBoxSize.value!!.height, dividerSize.value!!.height)
        }
    }

    @Test
    fun dividerMatchTextHeight_percent() = with(density) {
        val aspectRatioBoxSize = Ref<IntPxSize>()
        val dividerSize = Ref<IntPxSize>()
        composeTestRule.setContent {
            ConstraintLayout(
                // Make CL fixed width and wrap content height.
                modifier = Modifier.wrapContentSize(Alignment.TopStart).fillMaxWidth(),
                constraintSet = ConstraintSet {
                    val aspectRatioBox = tag("aspectRatioBox")
                    val divider = tag("divider")
                    val guideline = createGuidelineFromLeft(0.5f)

                    aspectRatioBox constrainTo parent
                    aspectRatioBox.left constrainTo guideline
                    divider constrainTo parent

                    aspectRatioBox.width = wrap
                    aspectRatioBox.height = wrapFixed
                    divider.width = valueFixed(1.dp)
                    divider.height = percent(0.8f)
                }
            ) {
                Box(Modifier.tag("aspectRatioBox")
                    // Try to be large to make wrap content impossible.
                    .preferredWidth((composeTestRule.displayMetrics.widthPixels).ipx.toDp())
                    // This could be any (width in height out child) e.g. text
                    .aspectRatio(2f)
                    .onPositioned { coordinates ->
                        aspectRatioBoxSize.value = coordinates.size
                    }
                )
                Box(Modifier.tag("divider")
                    .onPositioned { coordinates ->
                        dividerSize.value = coordinates.size
                    }
                )
            }
        }

        runOnIdleCompose {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (composeTestRule.displayMetrics.widthPixels / 2).ipx,
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (composeTestRule.displayMetrics.widthPixels / 2 / 2).ipx,
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.toIntPx(), dividerSize.value!!.width)
            // Divider has percent height so it should spread to fill 0.8 of the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            // TODO(popam; b/150277566): uncomment
            assertEquals(aspectRatioBoxSize.value!!.height * 0.8f, dividerSize.value!!.height)
        }
    }

    @Test
    fun dividerMatchTextHeight_inWrapConstraintLayout_longText() = with(density) {
        val aspectRatioBoxSize = Ref<IntPxSize>()
        val dividerSize = Ref<IntPxSize>()
        composeTestRule.setContent {
            ConstraintLayout(
                // Make CL wrap width and height.
                modifier = Modifier.wrapContentSize(Alignment.TopStart),
                constraintSet = ConstraintSet {
                    val aspectRatioBox = tag("aspectRatioBox")
                    val divider = tag("divider")
                    val guideline = createGuidelineFromLeft(0.5f)

                    aspectRatioBox constrainTo parent
                    aspectRatioBox.left constrainTo guideline
                    divider constrainTo parent

                    aspectRatioBox.width = wrap
                    aspectRatioBox.height = wrapFixed
                    divider.width = valueFixed(1.dp)
                    divider.height = percent(0.8f)
                }
            ) {
                Box(Modifier.tag("aspectRatioBox")
                    // Try to be large to make wrap content impossible.
                    .preferredWidth((composeTestRule.displayMetrics.widthPixels).ipx.toDp())
                    // This could be any (width in height out child) e.g. text
                    .aspectRatio(2f)
                    .onPositioned { coordinates ->
                        aspectRatioBoxSize.value = coordinates.size
                    }
                )
                Box(Modifier.tag("divider")
                    .onPositioned { coordinates ->
                        dividerSize.value = coordinates.size
                    }
                )
            }
        }

        runOnIdleCompose {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (composeTestRule.displayMetrics.widthPixels / 2).ipx,
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (composeTestRule.displayMetrics.widthPixels / 2 / 2).ipx,
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.toIntPx(), dividerSize.value!!.width)
            // Divider has percent height so it should spread to fill 0.8 of the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            // TODO(popam; b/150277566): uncomment
            assertEquals(aspectRatioBoxSize.value!!.height * 0.8f, dividerSize.value!!.height)
        }
    }

    @Test
    fun dividerMatchTextHeight_inWrapConstraintLayout_shortText() = with(density) {
        val constraintLayoutSize = Ref<IntPxSize>()
        val aspectRatioBoxSize = Ref<IntPxSize>()
        val dividerSize = Ref<IntPxSize>()
        val size = 40.ipx.toDp()
        composeTestRule.setContent {
            ConstraintLayout(
                // Make CL wrap width and height.
                modifier = Modifier.wrapContentSize(Alignment.TopStart).onPositioned {
                    constraintLayoutSize.value = it.size
                },
                constraintSet = ConstraintSet {
                    val aspectRatioBox = tag("aspectRatioBox")
                    val divider = tag("divider")
                    val guideline = createGuidelineFromLeft(0.5f)

                    aspectRatioBox constrainTo parent
                    aspectRatioBox.left constrainTo guideline
                    divider constrainTo parent

                    aspectRatioBox.width = wrap
                    aspectRatioBox.height = wrapFixed
                    divider.width = valueFixed(1.dp)
                    divider.height = spread
                }
            ) {
                Box(Modifier.tag("aspectRatioBox")
                        // Small width for the CL to wrap it.
                    .preferredWidth(size)
                        // This could be any (width in height out child) e.g. text
                    .aspectRatio(2f)
                    .onPositioned { coordinates ->
                        aspectRatioBoxSize.value = coordinates.size
                    }
                )
                Box(Modifier.tag("divider")
                    .onPositioned { coordinates ->
                        dividerSize.value = coordinates.size
                    }
                )
            }
        }

        runOnIdleCompose {
            // The width of the ConstraintLayout should be twice the width of the aspect ratio box.
            assertEquals(size.toIntPx() * 2, constraintLayoutSize.value!!.width)
            // The height of the ConstraintLayout should be the height of the aspect ratio box.
            assertEquals(size.toIntPx() / 2, constraintLayoutSize.value!!.height)
            // The aspect ratio gets the requested size.
            assertEquals(size.toIntPx(), aspectRatioBoxSize.value!!.width)
            // Aspect ratio is preserved.
            assertEquals(size.toIntPx() / 2, aspectRatioBoxSize.value!!.height)
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.toIntPx(), dividerSize.value!!.width)
            // Divider should have the height of the aspect ratio box.
            assertEquals(aspectRatioBoxSize.value!!.height, dividerSize.value!!.height)
        }
    }

    // endregion

    // region positioning tests

    @Test
    fun testConstraintLayout() = with(density) {
        val boxSize = 100.ipx
        val offset = 150.ipx

        val position = Array(3) { Ref<PxPosition>() }

        composeTestRule.setContent {
            ConstraintLayout(ConstraintSet {
                    val box0 = tag("box0")
                    val box1 = tag("box1")
                    val box2 = tag("box2")

                    box0.center()

                    val half = createGuidelineFromLeft(percent = 0.5f)
                    box1.apply {
                        left constrainTo half
                        left.margin = offset.toDp()
                        bottom constrainTo box0.top
                    }

                    box2.apply {
                        left constrainTo parent.left
                        left.margin = offset.toDp()
                        bottom constrainTo parent.bottom
                        bottom.margin = offset.toDp()
                    }
                },
                Modifier.fillMaxSize()
            ) {
                for (i in 0..2) {
                    Box(Modifier.tag("box$i").preferredSize(boxSize.toDp(), boxSize.toDp())
                        .onPositioned {
                            position[i].value = it.globalPosition
                        }
                    )
                }
            }
        }

        val displayWidth = composeTestRule.displayMetrics.widthPixels.ipx
        val displayHeight = composeTestRule.displayMetrics.heightPixels.ipx

        runOnIdleCompose {
            assertEquals(
                PxPosition((displayWidth - boxSize) / 2, (displayHeight - boxSize) / 2),
                position[0].value
            )
            assertEquals(
                PxPosition(displayWidth / 2 + offset, (displayHeight - boxSize) / 2 - boxSize),
                position[1].value
            )
            assertEquals(
                PxPosition(offset, displayHeight - boxSize - offset),
                position[2].value
            )
        }
    }

    @Test
    fun testConstraintLayout_rtl() = with(density) {
        val boxSize = 100.ipx
        val offset = 150.ipx

        val position = Array(3) { Ref<PxPosition>() }

        composeTestRule.setContent {
            ConstraintLayout(
                modifier = Modifier.rtl.fillMaxSize(),
                constraintSet = ConstraintSet {
                    val box0 = tag("box0")
                    val box1 = tag("box1")
                    val box2 = tag("box2")

                    box0.center()

                    val half = createGuidelineFromLeft(percent = 0.5f)
                    box1.apply {
                        left constrainTo half
                        left.margin = offset.toDp()
                        bottom constrainTo box0.top
                    }

                    box2.apply {
                        left constrainTo parent.left
                        left.margin = offset.toDp()
                        bottom constrainTo parent.bottom
                        bottom.margin = offset.toDp()
                    }
                }
            ) {
                for (i in 0..2) {
                    Box(Modifier.tag("box$i").preferredSize(boxSize.toDp(), boxSize.toDp())
                        .onPositioned {
                            position[i].value = it.globalPosition
                        }
                    )
                }
            }
        }

        val displayWidth = composeTestRule.displayMetrics.widthPixels.ipx
        val displayHeight = composeTestRule.displayMetrics.heightPixels.ipx

        runOnIdleCompose {
            assertEquals(
                PxPosition((displayWidth - boxSize) / 2, (displayHeight - boxSize) / 2),
                position[0].value
            )
            assertEquals(
                PxPosition(
                    displayWidth / 2 - offset - boxSize,
                    (displayHeight - boxSize) / 2 - boxSize
                ),
                position[1].value
            )
            assertEquals(
                PxPosition(displayWidth - offset - boxSize, displayHeight - boxSize - offset),
                position[2].value
            )
        }
    }
}
