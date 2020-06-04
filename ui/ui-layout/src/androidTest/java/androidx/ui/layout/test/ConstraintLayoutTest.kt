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

import androidx.compose.mutableStateOf
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.tag
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.core.positionInParent
import androidx.ui.foundation.Box
import androidx.ui.layout.ConstrainScope
import androidx.ui.layout.ConstraintLayout
import androidx.ui.layout.ConstraintSet2
import androidx.ui.layout.Dimension
import androidx.ui.layout.ExperimentalLayout
import androidx.ui.layout.aspectRatio
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.layout.rtl
import androidx.ui.layout.size
import androidx.ui.layout.wrapContentSize
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.waitForIdle
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import org.junit.Assert
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
                modifier = Modifier.wrapContentSize(Alignment.TopStart).fillMaxWidth()
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(Modifier
                    .constrainAs(aspectRatioBox) {
                        centerTo(parent)
                        start.linkTo(guideline)
                        width = Dimension.preferredWrapContent
                        height = Dimension.wrapContent
                    }
                    // Try to be large to make wrap content impossible.
                    .preferredWidth((composeTestRule.displayMetrics.widthPixels).ipx.toDp())
                    // This could be any (width in height out child) e.g. text
                    .aspectRatio(2f)
                    .onPositioned { coordinates ->
                        aspectRatioBoxSize.value = coordinates.size
                    }
                )
                Box(Modifier
                    .constrainAs(divider) {
                        centerTo(parent)
                        width = Dimension.value(1.dp)
                        height = Dimension.fillToConstraints
                    }.onPositioned { coordinates ->
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
                modifier = Modifier.wrapContentSize(Alignment.TopStart).fillMaxWidth()
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(Modifier
                    .constrainAs(aspectRatioBox) {
                        centerTo(parent)
                        start.linkTo(guideline)
                        width = Dimension.preferredWrapContent
                        height = Dimension.wrapContent
                    }
                    // Try to be large to make wrap content impossible.
                    .preferredWidth((composeTestRule.displayMetrics.widthPixels).ipx.toDp())
                    // This could be any (width in height out child) e.g. text
                    .aspectRatio(2f)
                    .onPositioned { coordinates ->
                        aspectRatioBoxSize.value = coordinates.size
                    }
                )
                Box(Modifier
                    .constrainAs(divider) {
                        centerTo(parent)
                        width = Dimension.value(1.dp)
                        height = Dimension.percent(0.8f)
                    }
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
                modifier = Modifier.wrapContentSize(Alignment.TopStart)
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(Modifier
                    .constrainAs(aspectRatioBox) {
                        centerTo(parent)
                        start.linkTo(guideline)
                        width = Dimension.preferredWrapContent
                        height = Dimension.wrapContent
                    }
                    // Try to be large to make wrap content impossible.
                    .preferredWidth((composeTestRule.displayMetrics.widthPixels).ipx.toDp())
                    // This could be any (width in height out child) e.g. text
                    .aspectRatio(2f)
                    .onPositioned { coordinates ->
                        aspectRatioBoxSize.value = coordinates.size
                    }
                )
                Box(Modifier
                    .constrainAs(divider) {
                        centerTo(parent)
                        width = Dimension.value(1.dp)
                        height = Dimension.percent(0.8f)
                    }
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
                }
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(Modifier
                    .constrainAs(aspectRatioBox) {
                        centerTo(parent)
                        start.linkTo(guideline)
                        width = Dimension.preferredWrapContent
                        height = Dimension.wrapContent
                    }
                    // Small width for the CL to wrap it.
                    .preferredWidth(size)
                    // This could be any (width in height out child) e.g. text
                    .aspectRatio(2f)
                    .onPositioned { coordinates ->
                        aspectRatioBoxSize.value = coordinates.size
                    }
                )
                Box(Modifier
                    .constrainAs(divider) {
                        centerTo(parent)
                        width = Dimension.value(1.dp)
                        height = Dimension.fillToConstraints
                    }
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
    fun testConstraintLayout_withInlineDSL() = with(density) {
        val boxSize = 100.ipx
        val offset = 150.ipx

        val position = Array(3) { Ref<PxPosition>() }

        composeTestRule.setContent {
            ConstraintLayout(Modifier.fillMaxSize()) {
                val (box0, box1, box2) = createRefs()
                Box(Modifier
                    .constrainAs(box0) {
                        centerTo(parent)
                    }
                    .preferredSize(boxSize.toDp(), boxSize.toDp())
                    .onPositioned {
                        position[0].value = it.globalPosition
                    }
                )
                val half = createGuidelineFromAbsoluteLeft(fraction = 0.5f)
                Box(Modifier
                    .constrainAs(box1) {
                        start.linkTo(half, margin = offset.toDp())
                        bottom.linkTo(box0.top)
                    }
                    .preferredSize(boxSize.toDp(), boxSize.toDp())
                    .onPositioned {
                        position[1].value = it.globalPosition
                    }
                )
                Box(Modifier
                    .constrainAs(box2) {
                        start.linkTo(parent.start, margin = offset.toDp())
                        bottom.linkTo(parent.bottom, margin = offset.toDp())
                    }
                    .preferredSize(boxSize.toDp(), boxSize.toDp())
                    .onPositioned {
                        position[2].value = it.globalPosition
                    }
                )
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
    fun testConstraintLayout_withConstraintSet() = with(density) {
        val boxSize = 100.ipx
        val offset = 150.ipx

        val position = Array(3) { Ref<PxPosition>() }

        composeTestRule.setContent {
            ConstraintLayout(
                ConstraintSet2 {
                    val box0 = createRefFor("box0")
                    val box1 = createRefFor("box1")
                    val box2 = createRefFor("box2")

                    constrain(box0) {
                        centerTo(parent)
                    }

                    val half = createGuidelineFromAbsoluteLeft(fraction = 0.5f)
                    constrain(box1) {
                        start.linkTo(half, margin = offset.toDp())
                        bottom.linkTo(box0.top)
                    }

                    constrain(box2) {
                        start.linkTo(parent.start, margin = offset.toDp())
                        bottom.linkTo(parent.bottom, margin = offset.toDp())
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
            ConstraintLayout(Modifier.rtl.fillMaxSize()) {
                val (box0, box1, box2) = createRefs()
                Box(Modifier
                    .constrainAs(box0) {
                        centerTo(parent)
                    }
                    .preferredSize(boxSize.toDp(), boxSize.toDp())
                    .onPositioned {
                        position[0].value = it.globalPosition
                    }
                )
                val half = createGuidelineFromAbsoluteLeft(fraction = 0.5f)
                Box(Modifier
                    .constrainAs(box1) {
                        start.linkTo(half, margin = offset.toDp())
                        bottom.linkTo(box0.top)
                    }
                    .preferredSize(boxSize.toDp(), boxSize.toDp())
                    .onPositioned {
                        position[1].value = it.globalPosition
                    }
                )
                Box(Modifier
                    .constrainAs(box2) {
                        start.linkTo(parent.start, margin = offset.toDp())
                        bottom.linkTo(parent.bottom, margin = offset.toDp())
                    }
                    .preferredSize(boxSize.toDp(), boxSize.toDp())
                    .onPositioned {
                        position[2].value = it.globalPosition
                    }
                )
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

    @Test
    fun testConstraintLayout_helpers_ltr() = with(density) {
        val size = 200.ipx.toDp()
        val offset = 50.ipx.toDp()

        val position = Array(8) { 0f }
        composeTestRule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val guidelines = arrayOf(
                    createGuidelineFromStart(offset),
                    createGuidelineFromAbsoluteLeft(offset),
                    createGuidelineFromEnd(offset),
                    createGuidelineFromAbsoluteRight(offset),
                    createGuidelineFromStart(0.25f),
                    createGuidelineFromAbsoluteLeft(0.25f),
                    createGuidelineFromEnd(0.25f),
                    createGuidelineFromAbsoluteRight(0.25f)
                )

                guidelines.forEachIndexed { index, guideline ->
                    val ref = createRef()
                    Box(Modifier.size(1.dp)
                        .constrainAs(ref) {
                            absoluteLeft.linkTo(guideline)
                        }.onPositioned {
                            position[index] = it.positionInParent.x
                        }
                    )
                }
            }
        }

        runOnIdleCompose {
            Assert.assertEquals(50f, position[0])
            Assert.assertEquals(50f, position[1])
            Assert.assertEquals(150f, position[2])
            Assert.assertEquals(150f, position[3])
            Assert.assertEquals(50f, position[4])
            Assert.assertEquals(50f, position[5])
            Assert.assertEquals(150f, position[6])
            Assert.assertEquals(150f, position[7])
        }
    }

    @Test
    fun testConstraintLayout_helpers_rtl() = with(density) {
        val size = 200.ipx.toDp()
        val offset = 50.ipx.toDp()

        val position = Array(8) { 0f }
        composeTestRule.setContent {
            ConstraintLayout(Modifier.size(size).rtl) {
                val guidelines = arrayOf(
                    createGuidelineFromStart(offset),
                    createGuidelineFromAbsoluteLeft(offset),
                    createGuidelineFromEnd(offset),
                    createGuidelineFromAbsoluteRight(offset),
                    createGuidelineFromStart(0.25f),
                    createGuidelineFromAbsoluteLeft(0.25f),
                    createGuidelineFromEnd(0.25f),
                    createGuidelineFromAbsoluteRight(0.25f)
                )

                guidelines.forEachIndexed { index, guideline ->
                    val ref = createRef()
                    Box(Modifier.size(1.dp)
                        .constrainAs(ref) {
                            absoluteLeft.linkTo(guideline)
                        }.onPositioned {
                            position[index] = it.positionInParent.x
                        }
                    )
                }
            }
        }

        runOnIdleCompose {
            Assert.assertEquals(150f, position[0])
            Assert.assertEquals(50f, position[1])
            Assert.assertEquals(50f, position[2])
            Assert.assertEquals(150f, position[3])
            Assert.assertEquals(150f, position[4])
            Assert.assertEquals(50f, position[5])
            Assert.assertEquals(50f, position[6])
            Assert.assertEquals(150f, position[7])
        }
    }

    @Test
    fun testConstraintLayout_barriers_ltr() = with(density) {
        val size = 200.ipx.toDp()
        val offset = 50.ipx.toDp()

        val position = Array(4) { 0f }
        composeTestRule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val (box1, box2) = createRefs()
                val guideline1 = createGuidelineFromAbsoluteLeft(offset)
                val guideline2 = createGuidelineFromAbsoluteRight(offset)
                Box(Modifier.size(1.ipx.toDp())
                    .constrainAs(box1) {
                        absoluteLeft.linkTo(guideline1)
                    }
                )
                Box(Modifier.size(1.ipx.toDp())
                    .constrainAs(box2) {
                        absoluteLeft.linkTo(guideline2)
                    }
                )

                val barriers = arrayOf(
                    createStartBarrier(box1, box2),
                    createAbsoluteLeftBarrier(box1, box2),
                    createEndBarrier(box1, box2),
                    createAbsoluteRightBarrier(box1, box2)
                )

                barriers.forEachIndexed { index, barrier ->
                    val ref = createRef()
                    Box(Modifier.size(1.dp)
                        .constrainAs(ref) {
                            absoluteLeft.linkTo(barrier)
                        }.onPositioned {
                            position[index] = it.positionInParent.x
                        }
                    )
                }
            }
        }

        runOnIdleCompose {
            Assert.assertEquals(50f, position[0])
            Assert.assertEquals(50f, position[1])
            Assert.assertEquals(151f, position[2])
            Assert.assertEquals(151f, position[3])
        }
    }

    @Test
    fun testConstraintLayout_barriers_rtl() = with(density) {
        val size = 200.ipx.toDp()
        val offset = 50.ipx.toDp()

        val position = Array(4) { 0f }
        composeTestRule.setContent {
            ConstraintLayout(Modifier.size(size).rtl) {
                val (box1, box2) = createRefs()
                val guideline1 = createGuidelineFromAbsoluteLeft(offset)
                val guideline2 = createGuidelineFromAbsoluteRight(offset)
                Box(Modifier.size(1.ipx.toDp())
                    .constrainAs(box1) {
                        absoluteLeft.linkTo(guideline1)
                    }
                )
                Box(Modifier.size(1.ipx.toDp())
                    .constrainAs(box2) {
                        absoluteLeft.linkTo(guideline2)
                    }
                )

                val barriers = arrayOf(
                    createStartBarrier(box1, box2),
                    createAbsoluteLeftBarrier(box1, box2),
                    createEndBarrier(box1, box2),
                    createAbsoluteRightBarrier(box1, box2)
                )

                barriers.forEachIndexed { index, barrier ->
                    val ref = createRef()
                    Box(Modifier.size(1.dp)
                        .constrainAs(ref) {
                            absoluteLeft.linkTo(barrier)
                        }.onPositioned {
                            position[index] = it.positionInParent.x
                        }
                    )
                }
            }
        }

        runOnIdleCompose {
            Assert.assertEquals(151f, position[0])
            Assert.assertEquals(50f, position[1])
            Assert.assertEquals(50f, position[2])
            Assert.assertEquals(151f, position[3])
        }
    }

    @Test
    fun testConstraintLayout_anchors_ltr() = with(density) {
        val size = 200.ipx.toDp()
        val offset = 50.ipx.toDp()

        val position = Array(16) { 0f }
        composeTestRule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val box = createRef()
                val guideline = createGuidelineFromAbsoluteLeft(offset)
                Box(Modifier.size(1.ipx.toDp())
                    .constrainAs(box) {
                        absoluteLeft.linkTo(guideline)
                    }
                )

                val anchors = listOf<ConstrainScope.() -> Unit>(
                    { start.linkTo(box.start) },
                    { absoluteLeft.linkTo(box.start) },
                    { start.linkTo(box.absoluteLeft) },
                    { absoluteLeft.linkTo(box.absoluteLeft) },
                    { end.linkTo(box.start) },
                    { absoluteRight.linkTo(box.start) },
                    { end.linkTo(box.absoluteLeft) },
                    { absoluteRight.linkTo(box.absoluteLeft) },
                    { start.linkTo(box.end) },
                    { absoluteLeft.linkTo(box.end) },
                    { start.linkTo(box.absoluteRight) },
                    { absoluteLeft.linkTo(box.absoluteRight) },
                    { end.linkTo(box.end) },
                    { absoluteRight.linkTo(box.end) },
                    { end.linkTo(box.absoluteRight) },
                    { absoluteRight.linkTo(box.absoluteRight) }
                )

                anchors.forEachIndexed { index, anchor ->
                    val ref = createRef()
                    Box(Modifier.size(1.ipx.toDp())
                        .constrainAs(ref) {
                            anchor()
                        }.onPositioned {
                            position[index] = it.positionInParent.x
                        }
                    )
                }
            }
        }

        runOnIdleCompose {
            Assert.assertEquals(50f, position[0])
            Assert.assertEquals(50f, position[1])
            Assert.assertEquals(50f, position[2])
            Assert.assertEquals(50f, position[3])
            Assert.assertEquals(49f, position[4])
            Assert.assertEquals(49f, position[5])
            Assert.assertEquals(49f, position[6])
            Assert.assertEquals(49f, position[7])
            Assert.assertEquals(51f, position[8])
            Assert.assertEquals(51f, position[9])
            Assert.assertEquals(51f, position[10])
            Assert.assertEquals(51f, position[11])
            Assert.assertEquals(50f, position[12])
            Assert.assertEquals(50f, position[13])
            Assert.assertEquals(50f, position[14])
            Assert.assertEquals(50f, position[15])
        }
    }

    @Test
    fun testConstraintLayout_anchors_rtl() = with(density) {
        val size = 200.ipx.toDp()
        val offset = 50.ipx.toDp()

        val position = Array(16) { 0f }
        composeTestRule.setContent {
            ConstraintLayout(Modifier.size(size).rtl) {
                val box = createRef()
                val guideline = createGuidelineFromAbsoluteLeft(offset)
                Box(Modifier.size(1.ipx.toDp())
                    .constrainAs(box) {
                        absoluteLeft.linkTo(guideline)
                    }
                )

                val anchors = listOf<ConstrainScope.() -> Unit>(
                    { start.linkTo(box.start) },
                    { absoluteLeft.linkTo(box.start) },
                    { start.linkTo(box.absoluteLeft) },
                    { absoluteLeft.linkTo(box.absoluteLeft) },
                    { end.linkTo(box.start) },
                    { absoluteRight.linkTo(box.start) },
                    { end.linkTo(box.absoluteLeft) },
                    { absoluteRight.linkTo(box.absoluteLeft) },
                    { start.linkTo(box.end) },
                    { absoluteLeft.linkTo(box.end) },
                    { start.linkTo(box.absoluteRight) },
                    { absoluteLeft.linkTo(box.absoluteRight) },
                    { end.linkTo(box.end) },
                    { absoluteRight.linkTo(box.end) },
                    { end.linkTo(box.absoluteRight) },
                    { absoluteRight.linkTo(box.absoluteRight) }
                )

                anchors.forEachIndexed { index, anchor ->
                    val ref = createRef()
                    Box(Modifier.size(1.ipx.toDp())
                        .constrainAs(ref) {
                            anchor()
                        }.onPositioned {
                            position[index] = it.positionInParent.x
                        }
                    )
                }
            }
        }

        runOnIdleCompose {
            Assert.assertEquals(50f, position[0])
            Assert.assertEquals(51f, position[1])
            Assert.assertEquals(49f, position[2])
            Assert.assertEquals(50f, position[3])
            Assert.assertEquals(51f, position[4])
            Assert.assertEquals(50f, position[5])
            Assert.assertEquals(50f, position[6])
            Assert.assertEquals(49f, position[7])
            Assert.assertEquals(49f, position[8])
            Assert.assertEquals(50f, position[9])
            Assert.assertEquals(50f, position[10])
            Assert.assertEquals(51f, position[11])
            Assert.assertEquals(50f, position[12])
            Assert.assertEquals(49f, position[13])
            Assert.assertEquals(51f, position[14])
            Assert.assertEquals(50f, position[15])
        }
    }

    @Test
    fun testConstraintLayout_barriers_margins() = with(density) {
        val size = 200.ipx.toDp()
        val offset = 50.ipx.toDp()

        val position = Array(2) { PxPosition(0f, 0f) }
        composeTestRule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val box = createRef()
                val guideline1 = createGuidelineFromAbsoluteLeft(offset)
                val guideline2 = createGuidelineFromTop(offset)
                Box(Modifier.size(1.ipx.toDp())
                    .constrainAs(box) {
                        absoluteLeft.linkTo(guideline1)
                        top.linkTo(guideline2)
                    }
                )

                val leftBarrier = createAbsoluteLeftBarrier(box, margin = 10.ipx.toDp())
                val topBarrier = createTopBarrier(box, margin = 10.ipx.toDp())
                val rightBarrier = createAbsoluteRightBarrier(box, margin = 10.ipx.toDp())
                val bottomBarrier = createBottomBarrier(box, margin = 10.ipx.toDp())

                Box(Modifier.size(1.dp)
                    .constrainAs(createRef()) {
                        absoluteLeft.linkTo(leftBarrier)
                        top.linkTo(topBarrier)
                    }.onPositioned {
                        position[0] = it.positionInParent
                    }
                )

                Box(Modifier.size(1.dp)
                    .constrainAs(createRef()) {
                        absoluteLeft.linkTo(rightBarrier)
                        top.linkTo(bottomBarrier)
                    }.onPositioned {
                        position[1] = it.positionInParent
                    }
                )
            }
        }

        runOnIdleCompose {
            Assert.assertEquals(PxPosition(60f, 60f), position[0])
            Assert.assertEquals(PxPosition(61f, 61f), position[1])
        }
    }

    @Test
    fun testConstraintLayout_inlineDSL_recompositionDoesNotCrash() = with(density) {
        val first = mutableStateOf(true)
        composeTestRule.setContent {
            ConstraintLayout {
                val box = createRef()
                if (first.value) {
                    Box(Modifier.constrainAs(box) { })
                } else {
                    Box(Modifier.constrainAs(box) { })
                }
            }
        }
        runOnIdleCompose {
            first.value = false
        }
        waitForIdle()
    }

    @Test
    fun testConstraintLayout_ConstraintSetDSL_recompositionDoesNotCrash() = with(density) {
        val first = mutableStateOf(true)
        composeTestRule.setContent {
            ConstraintLayout(ConstraintSet2 {
                val box = createRefFor("box")
                constrain(box) { }
            }) {
                if (first.value) {
                    Box(Modifier.tag("box"))
                } else {
                    Box(Modifier.tag("box"))
                }
            }
        }
        runOnIdleCompose {
            first.value = false
        }
        waitForIdle()
    }
}
