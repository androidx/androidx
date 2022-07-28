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

package androidx.constraintlayout.compose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ConstraintLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    var displaySize: IntSize = IntSize.Zero

    // region sizing tests

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
        displaySize = ApplicationProvider
            .getApplicationContext<Context>().resources.displayMetrics.let {
                IntSize(it.widthPixels, it.heightPixels)
            }
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun dividerMatchTextHeight_spread() = with(rule.density) {
        val aspectRatioBoxSize = Ref<IntSize>()
        val dividerSize = Ref<IntSize>()

        rule.setContent {
            ConstraintLayout(
                // Make CL fixed width and wrap content height.
                modifier = Modifier.fillMaxWidth()
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(
                    Modifier
                        .constrainAs(aspectRatioBox) {
                            centerTo(parent)
                            start.linkTo(guideline)
                            width = Dimension.preferredWrapContent
                            height = Dimension.wrapContent
                        }
                        // Try to be large to make wrap content impossible.
                        .width((displaySize.width).toDp())
                        // This could be any (width in height out child) e.g. text
                        .aspectRatio(2f)
                        .onGloballyPositioned { coordinates ->
                            aspectRatioBoxSize.value = coordinates.size
                        }
                )
                Box(
                    Modifier
                        .constrainAs(divider) {
                            centerTo(parent)
                            width = Dimension.value(1.dp)
                            height = Dimension.fillToConstraints
                        }
                        .onGloballyPositioned { coordinates ->
                            dividerSize.value = coordinates.size
                        }
                )
            }
        }

        rule.runOnIdle {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (displaySize.width / 2),
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (displaySize.width / 2 / 2),
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.roundToPx(), dividerSize.value!!.width)
            // Divider has spread height so it should spread to fill the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            assertEquals(aspectRatioBoxSize.value!!.height, dividerSize.value!!.height)
        }
    }

    @Test
    fun dividerMatchTextHeight_spread_withPreferredWrapHeightText() = with(rule.density) {
        val aspectRatioBoxSize = Ref<IntSize>()
        val dividerSize = Ref<IntSize>()
        rule.setContent {
            ConstraintLayout(
                // Make CL fixed width and wrap content height.
                modifier = Modifier.fillMaxWidth()
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(
                    Modifier
                        .constrainAs(aspectRatioBox) {
                            centerTo(parent)
                            start.linkTo(guideline)
                            width = Dimension.preferredWrapContent
                            height = Dimension.preferredWrapContent
                        }
                        // Try to be large to make wrap content impossible.
                        .width((displaySize.width).toDp())
                        // This could be any (width in height out child) e.g. text
                        .aspectRatio(2f)
                        .onGloballyPositioned { coordinates ->
                            aspectRatioBoxSize.value = coordinates.size
                        }
                )
                Box(
                    Modifier
                        .constrainAs(divider) {
                            centerTo(parent)
                            width = Dimension.value(1.dp)
                            height = Dimension.fillToConstraints
                        }
                        .onGloballyPositioned { coordinates ->
                            dividerSize.value = coordinates.size
                        }
                )
            }
        }

        rule.runOnIdle {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (displaySize.width / 2),
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (displaySize.width / 2 / 2),
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.roundToPx(), dividerSize.value!!.width)
            // Divider has spread height so it should spread to fill the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            assertEquals(aspectRatioBoxSize.value!!.height, dividerSize.value!!.height)
        }
    }

    @Test
    fun dividerMatchTextHeight_percent() = with(rule.density) {
        val aspectRatioBoxSize = Ref<IntSize>()
        val dividerSize = Ref<IntSize>()
        rule.setContent {
            ConstraintLayout(
                // Make CL fixed width and wrap content height.
                modifier = Modifier.fillMaxWidth()
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(
                    Modifier
                        .constrainAs(aspectRatioBox) {
                            centerTo(parent)
                            start.linkTo(guideline)
                            width = Dimension.preferredWrapContent
                            height = Dimension.wrapContent
                        }
                        // Try to be large to make wrap content impossible.
                        .width((displaySize.width).toDp())
                        // This could be any (width in height out child) e.g. text
                        .aspectRatio(2f)
                        .onGloballyPositioned { coordinates ->
                            aspectRatioBoxSize.value = coordinates.size
                        }
                )
                Box(
                    Modifier
                        .constrainAs(divider) {
                            centerTo(parent)
                            width = Dimension.value(1.dp)
                            height = Dimension.percent(0.8f)
                        }
                        .onGloballyPositioned { coordinates ->
                            dividerSize.value = coordinates.size
                        }
                )
            }
        }

        rule.runOnIdle {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (displaySize.width / 2),
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (displaySize.width / 2 / 2),
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.roundToPx(), dividerSize.value!!.width)
            // Divider has percent height so it should spread to fill 0.8 of the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            assertEquals(
                (aspectRatioBoxSize.value!!.height * 0.8f).roundToInt(),
                dividerSize.value!!.height
            )
        }
    }

    @Test
    @Ignore
    fun dividerMatchTextHeight_inWrapConstraintLayout_longText() = with(rule.density) {
        val aspectRatioBoxSize = Ref<IntSize>()
        val dividerSize = Ref<IntSize>()
        rule.setContent {
            // CL is wrap content.
            ConstraintLayout {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(
                    Modifier
                        .constrainAs(aspectRatioBox) {
                            centerTo(parent)
                            start.linkTo(guideline)
                            width = Dimension.preferredWrapContent
                            height = Dimension.wrapContent
                        }
                        // Try to be large to make wrap content impossible.
                        .width((displaySize.width).toDp())
                        // This could be any (width in height out child) e.g. text
                        .aspectRatio(2f)
                        .onGloballyPositioned { coordinates ->
                            aspectRatioBoxSize.value = coordinates.size
                        }
                )
                Box(
                    Modifier
                        .constrainAs(divider) {
                            centerTo(parent)
                            width = Dimension.value(1.dp)
                            height = Dimension.percent(0.8f)
                        }
                        .onGloballyPositioned { coordinates ->
                            dividerSize.value = coordinates.size
                        }
                )
            }
        }

        rule.runOnIdle {
            // The aspect ratio could not wrap and it is wrap suggested, so it respects constraints.
            assertEquals(
                (displaySize.width / 2),
                aspectRatioBoxSize.value!!.width
            )
            // Aspect ratio is preserved.
            assertEquals(
                (displaySize.width / 2 / 2),
                aspectRatioBoxSize.value!!.height
            )
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.roundToPx(), dividerSize.value!!.width)
            // Divider has percent height so it should spread to fill 0.8 of the height of the CL,
            // which in turns is given by the size of the aspect ratio box.
            // TODO(popam; b/150277566): uncomment
            assertEquals(
                "broken, display size ${displaySize.width}x${displaySize.height} aspect" +
                    " height ${aspectRatioBoxSize.value!!.width}x" +
                    "${aspectRatioBoxSize.value!!.height}, " +
                    "divider: ${dividerSize.value!!.height}",
                (aspectRatioBoxSize.value!!.height * 0.8f).roundToInt(),
                dividerSize.value!!.height
            )
            assertEquals(
                "broken, aspect height ${aspectRatioBoxSize.value!!.width}x" +
                    "${aspectRatioBoxSize.value!!.height}," +
                    " divider: ${dividerSize.value!!.height}",
                aspectRatioBoxSize.value!!.width,
                540
            )
        }
    }

    @Test
    fun dividerMatchTextHeight_inWrapConstraintLayout_shortText() = with(rule.density) {
        val constraintLayoutSize = Ref<IntSize>()
        val aspectRatioBoxSize = Ref<IntSize>()
        val dividerSize = Ref<IntSize>()
        val size = 40.toDp()
        rule.setContent {
            ConstraintLayout(
                // CL is wrapping width and height.
                modifier = Modifier.onGloballyPositioned {
                    constraintLayoutSize.value = it.size
                }
            ) {
                val (aspectRatioBox, divider) = createRefs()
                val guideline = createGuidelineFromAbsoluteLeft(0.5f)

                Box(
                    Modifier
                        .constrainAs(aspectRatioBox) {
                            centerTo(parent)
                            start.linkTo(guideline)
                            width = Dimension.preferredWrapContent
                            height = Dimension.wrapContent
                        }
                        // Small width for the CL to wrap it.
                        .width(size)
                        // This could be any (width in height out child) e.g. text
                        .aspectRatio(2f)
                        .onGloballyPositioned { coordinates ->
                            aspectRatioBoxSize.value = coordinates.size
                        }
                )
                Box(
                    Modifier
                        .constrainAs(divider) {
                            centerTo(parent)
                            width = Dimension.value(1.dp)
                            height = Dimension.fillToConstraints
                        }
                        .onGloballyPositioned { coordinates ->
                            dividerSize.value = coordinates.size
                        }
                )
            }
        }

        rule.runOnIdle {
            // The width of the ConstraintLayout should be twice the width of the aspect ratio box.
            assertEquals(size.roundToPx() * 2, constraintLayoutSize.value!!.width)
            // The height of the ConstraintLayout should be the height of the aspect ratio box.
            assertEquals(size.roundToPx() / 2, constraintLayoutSize.value!!.height)
            // The aspect ratio gets the requested size.
            assertEquals(size.roundToPx(), aspectRatioBoxSize.value!!.width)
            // Aspect ratio is preserved.
            assertEquals(size.roundToPx() / 2, aspectRatioBoxSize.value!!.height)
            // Divider has fixed width 1.dp in constraint set.
            assertEquals(1.dp.roundToPx(), dividerSize.value!!.width)
            // Divider should have the height of the aspect ratio box.
            assertEquals(aspectRatioBoxSize.value!!.height, dividerSize.value!!.height)
        }
    }

    // endregion

    // region positioning tests

    @Test
    fun testConstraintLayout_withInlineDSL() = with(rule.density) {
        var rootSize: IntSize = IntSize.Zero
        val boxSize = 100
        val offset = 150

        val position = Array(3) { Ref<Offset>() }

        rule.setContent {
            ConstraintLayout(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        rootSize = it.size
                    }) {
                val (box0, box1, box2) = createRefs()
                Box(
                    Modifier
                        .constrainAs(box0) {
                            centerTo(parent)
                        }
                        .size(boxSize.toDp(), boxSize.toDp())
                        .onGloballyPositioned {
                            position[0].value = it.positionInRoot()
                        }
                )
                val half = createGuidelineFromAbsoluteLeft(fraction = 0.5f)
                Box(
                    Modifier
                        .constrainAs(box1) {
                            start.linkTo(half, margin = offset.toDp())
                            bottom.linkTo(box0.top)
                        }
                        .size(boxSize.toDp(), boxSize.toDp())
                        .onGloballyPositioned {
                            position[1].value = it.positionInRoot()
                        }
                )
                Box(
                    Modifier
                        .constrainAs(box2) {
                            start.linkTo(parent.start, margin = offset.toDp())
                            bottom.linkTo(parent.bottom, margin = offset.toDp())
                        }
                        .size(boxSize.toDp(), boxSize.toDp())
                        .onGloballyPositioned {
                            position[2].value = it.positionInRoot()
                        }
                )
            }
        }

        val displayWidth = rootSize.width
        val displayHeight = rootSize.height

        rule.runOnIdle {
            assertEquals(
                Offset(
                    ((displayWidth - boxSize) / 2).toFloat(),
                    ((displayHeight - boxSize) / 2).toFloat()
                ),
                position[0].value
            )
            assertEquals(
                Offset(
                    (displayWidth / 2 + offset).toFloat(),
                    ((displayHeight - boxSize) / 2 - boxSize).toFloat()
                ),
                position[1].value
            )
            assertEquals(
                Offset(
                    offset.toFloat(),
                    (displayHeight - boxSize - offset).toFloat()
                ),
                position[2].value
            )
        }
    }

    @Test
    fun testConstraintLayout_withConstraintSet() = with(rule.density) {
        var rootSize: IntSize = IntSize.Zero
        val boxSize = 100
        val offset = 150

        val position = Array(3) { Ref<Offset>() }

        rule.setContent {
            ConstraintLayout(
                constraintSet = ConstraintSet {
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
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        rootSize = it.size
                    }
            ) {
                for (i in 0..2) {
                    Box(
                        Modifier
                            .layoutId("box$i")
                            .size(boxSize.toDp(), boxSize.toDp())
                            .onGloballyPositioned {
                                position[i].value = it.positionInRoot()
                            }
                    )
                }
            }
        }

        val displayWidth = rootSize.width
        val displayHeight = rootSize.height

        rule.runOnIdle {
            assertEquals(
                Offset(
                    (displayWidth - boxSize) / 2f,
                    (displayHeight - boxSize) / 2f
                ),
                position[0].value
            )
            assertEquals(
                Offset(
                    (displayWidth / 2f + offset),
                    ((displayHeight - boxSize) / 2 - boxSize).toFloat()
                ),
                position[1].value
            )
            assertEquals(
                Offset(
                    offset.toFloat(),
                    (displayHeight - boxSize - offset).toFloat()
                ),
                position[2].value
            )
        }
    }

    @Test
    @Ignore
    fun testConstraintLayout_rtl() = with(rule.density) {
        val boxSize = 100
        val offset = 150

        val position = Array(3) { Ref<Offset>() }

        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ConstraintLayout(Modifier.fillMaxSize()) {
                    val (box0, box1, box2) = createRefs()
                    Box(
                        Modifier
                            .constrainAs(box0) {
                                centerTo(parent)
                            }
                            .size(boxSize.toDp(), boxSize.toDp())
                            .onGloballyPositioned {
                                position[0].value = it.positionInRoot()
                            }
                    )
                    val half = createGuidelineFromAbsoluteLeft(fraction = 0.5f)
                    Box(
                        Modifier
                            .constrainAs(box1) {
                                start.linkTo(half, margin = offset.toDp())
                                bottom.linkTo(box0.top)
                            }
                            .size(boxSize.toDp(), boxSize.toDp())
                            .onGloballyPositioned {
                                position[1].value = it.positionInRoot()
                            }
                    )
                    Box(
                        Modifier
                            .constrainAs(box2) {
                                start.linkTo(parent.start, margin = offset.toDp())
                                bottom.linkTo(parent.bottom, margin = offset.toDp())
                            }
                            .size(boxSize.toDp(), boxSize.toDp())
                            .onGloballyPositioned {
                                position[2].value = it.positionInRoot()
                            }
                    )
                }
            }
        }

        val displayWidth = displaySize.width
        val displayHeight = displaySize.height

        rule.runOnIdle {
            assertEquals(
                Offset(
                    (displayWidth - boxSize) / 2f,
                    (displayHeight - boxSize) / 2f
                ),
                position[0].value
            )
            assertEquals(
                Offset(
                    (displayWidth / 2 - offset - boxSize).toFloat(),
                    ((displayHeight - boxSize) / 2 - boxSize).toFloat()
                ),
                position[1].value
            )
            assertEquals(
                Offset(
                    (displayWidth - offset - boxSize).toFloat(),
                    (displayHeight - boxSize - offset).toFloat()
                ),
                position[2].value
            )
        }
    }

    @Test
    fun testConstraintLayout_helpers_ltr() = with(rule.density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(8) { 0f }
        rule.setContent {
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
                    Box(
                        Modifier
                            .size(1.dp)
                            .constrainAs(ref) {
                                absoluteLeft.linkTo(guideline)
                            }
                            .onGloballyPositioned {
                                position[index] = it.positionInParent().x
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertEquals(50f, position[0])
            assertEquals(50f, position[1])
            assertEquals(150f, position[2])
            assertEquals(150f, position[3])
            assertEquals(50f, position[4])
            assertEquals(50f, position[5])
            assertEquals(150f, position[6])
            assertEquals(150f, position[7])
        }
    }

    @Test
    fun testConstraintLayout_helpers_rtl() = with(rule.density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(8) { 0f }
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                        Box(
                            Modifier
                                .size(1.dp)
                                .constrainAs(ref) {
                                    absoluteLeft.linkTo(guideline)
                                }
                                .onGloballyPositioned {
                                    position[index] = it.positionInParent().x
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(150f, position[0])
            assertEquals(50f, position[1])
            assertEquals(50f, position[2])
            assertEquals(150f, position[3])
            assertEquals(150f, position[4])
            assertEquals(50f, position[5])
            assertEquals(50f, position[6])
            assertEquals(150f, position[7])
        }
    }

    @Test
    fun testConstraintLayout_barriers_ltr() = with(rule.density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(4) { 0f }
        rule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val (box1, box2) = createRefs()
                val guideline1 = createGuidelineFromAbsoluteLeft(offset)
                val guideline2 = createGuidelineFromAbsoluteRight(offset)
                Box(
                    Modifier
                        .size(1.toDp())
                        .constrainAs(box1) {
                            absoluteLeft.linkTo(guideline1)
                        }
                )
                Box(
                    Modifier
                        .size(1.toDp())
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
                    Box(
                        Modifier
                            .size(1.dp)
                            .constrainAs(ref) {
                                absoluteLeft.linkTo(barrier)
                            }
                            .onGloballyPositioned {
                                position[index] = it.positionInParent().x
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertEquals(50f, position[0])
            assertEquals(50f, position[1])
            assertEquals(151f, position[2])
            assertEquals(151f, position[3])
        }
    }

    @Test
    fun testConstraintLayout_barriers_rtl() = with(rule.density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(4) { 0f }
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ConstraintLayout(Modifier.size(size)) {
                    val (box1, box2) = createRefs()
                    val guideline1 = createGuidelineFromAbsoluteLeft(offset)
                    val guideline2 = createGuidelineFromAbsoluteRight(offset)
                    Box(
                        Modifier
                            .size(1.toDp())
                            .constrainAs(box1) {
                                absoluteLeft.linkTo(guideline1)
                            }
                    )
                    Box(
                        Modifier
                            .size(1.toDp())
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
                        Box(
                            Modifier
                                .size(1.dp)
                                .constrainAs(ref) {
                                    absoluteLeft.linkTo(barrier)
                                }
                                .onGloballyPositioned {
                                    position[index] = it.positionInParent().x
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(151f, position[0])
            assertEquals(50f, position[1])
            assertEquals(50f, position[2])
            assertEquals(151f, position[3])
        }
    }

    fun listAnchors(box: ConstrainedLayoutReference): List<ConstrainScope.() -> Unit> {
        // TODO(172055763) directly construct an immutable list when Lint supports it
        val anchors = mutableListOf<ConstrainScope.() -> Unit>()
        anchors.add({ start.linkTo(box.start) })
        anchors.add({ absoluteLeft.linkTo(box.start) })
        anchors.add({ start.linkTo(box.absoluteLeft) })
        anchors.add({ absoluteLeft.linkTo(box.absoluteLeft) })
        anchors.add({ end.linkTo(box.start) })
        anchors.add({ absoluteRight.linkTo(box.start) })
        anchors.add({ end.linkTo(box.absoluteLeft) })
        anchors.add({ absoluteRight.linkTo(box.absoluteLeft) })
        anchors.add({ start.linkTo(box.end) })
        anchors.add({ absoluteLeft.linkTo(box.end) })
        anchors.add({ start.linkTo(box.absoluteRight) })
        anchors.add({ absoluteLeft.linkTo(box.absoluteRight) })
        anchors.add({ end.linkTo(box.end) })
        anchors.add({ absoluteRight.linkTo(box.end) })
        anchors.add({ end.linkTo(box.absoluteRight) })
        anchors.add({ absoluteRight.linkTo(box.absoluteRight) })
        return anchors
    }

    @Test
    fun testConstraintLayout_anchors_ltr() = with(rule.density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(16) { 0f }
        rule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val box = createRef()
                val guideline = createGuidelineFromAbsoluteLeft(offset)
                Box(
                    Modifier
                        .size(1.toDp())
                        .constrainAs(box) {
                            absoluteLeft.linkTo(guideline)
                        }
                )

                val anchors = listAnchors(box)

                anchors.forEachIndexed { index, anchor ->
                    val ref = createRef()
                    Box(
                        Modifier
                            .size(1.toDp())
                            .constrainAs(ref) {
                                anchor()
                            }
                            .onGloballyPositioned {
                                position[index] = it.positionInParent().x
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertEquals(50f, position[0])
            assertEquals(50f, position[1])
            assertEquals(50f, position[2])
            assertEquals(50f, position[3])
            assertEquals(49f, position[4])
            assertEquals(49f, position[5])
            assertEquals(49f, position[6])
            assertEquals(49f, position[7])
            assertEquals(51f, position[8])
            assertEquals(51f, position[9])
            assertEquals(51f, position[10])
            assertEquals(51f, position[11])
            assertEquals(50f, position[12])
            assertEquals(50f, position[13])
            assertEquals(50f, position[14])
            assertEquals(50f, position[15])
        }
    }

    @Test
    fun testConstraintLayout_anchors_rtl() = with(rule.density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(16) { 0f }
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ConstraintLayout(Modifier.size(size)) {
                    val box = createRef()
                    val guideline = createGuidelineFromAbsoluteLeft(offset)
                    Box(
                        Modifier
                            .size(1.toDp())
                            .constrainAs(box) {
                                absoluteLeft.linkTo(guideline)
                            }
                    )

                    val anchors = listAnchors(box)

                    anchors.forEachIndexed { index, anchor ->
                        val ref = createRef()
                        Box(
                            Modifier
                                .size(1.toDp())
                                .constrainAs(ref) {
                                    anchor()
                                }
                                .onGloballyPositioned {
                                    position[index] = it.positionInParent().x
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(50f, position[0])
            assertEquals(51f, position[1])
            assertEquals(49f, position[2])
            assertEquals(50f, position[3])
            assertEquals(51f, position[4])
            assertEquals(50f, position[5])
            assertEquals(50f, position[6])
            assertEquals(49f, position[7])
            assertEquals(49f, position[8])
            assertEquals(50f, position[9])
            assertEquals(50f, position[10])
            assertEquals(51f, position[11])
            assertEquals(50f, position[12])
            assertEquals(49f, position[13])
            assertEquals(51f, position[14])
            assertEquals(50f, position[15])
        }
    }

    @Test
    fun testConstraintLayout_barriers_margins() = with(rule.density) {
        val size = 200.toDp()
        val offset = 50.toDp()

        val position = Array(2) { Offset(0f, 0f) }
        rule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val box = createRef()
                val guideline1 = createGuidelineFromAbsoluteLeft(offset)
                val guideline2 = createGuidelineFromTop(offset)
                Box(
                    Modifier
                        .size(1.toDp())
                        .constrainAs(box) {
                            absoluteLeft.linkTo(guideline1)
                            top.linkTo(guideline2)
                        }
                )

                val leftBarrier = createAbsoluteLeftBarrier(box, margin = 10.toDp())
                val topBarrier = createTopBarrier(box, margin = 10.toDp())
                val rightBarrier = createAbsoluteRightBarrier(box, margin = 10.toDp())
                val bottomBarrier = createBottomBarrier(box, margin = 10.toDp())

                Box(
                    Modifier
                        .size(1.dp)
                        .constrainAs(createRef()) {
                            absoluteLeft.linkTo(leftBarrier)
                            top.linkTo(topBarrier)
                        }
                        .onGloballyPositioned {
                            position[0] = it.positionInParent()
                        }
                )

                Box(
                    Modifier
                        .size(1.dp)
                        .constrainAs(createRef()) {
                            absoluteLeft.linkTo(rightBarrier)
                            top.linkTo(bottomBarrier)
                        }
                        .onGloballyPositioned {
                            position[1] = it.positionInParent()
                        }
                )
            }
        }

        rule.runOnIdle {
            assertEquals(Offset(60f, 60f), position[0])
            assertEquals(Offset(61f, 61f), position[1])
        }
    }

    @Test
    fun links_canBeOverridden() = with(rule.density) {
        rule.setContent {
            ConstraintLayout(Modifier.width(10.dp)) {
                val box = createRef()
                Box(
                    Modifier
                        .constrainAs(box) {
                            start.linkTo(parent.end)
                            start.linkTo(parent.start)
                        }
                        .onGloballyPositioned {
                            assertEquals(0f, it.positionInParent().x)
                        }
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    @Ignore
    fun chains_defaultOutsideConstraintsCanBeOverridden() = with(rule.density) {
        val size = 100.toDp()
        val boxSize = 10.toDp()
        val guidelinesOffset = 20.toDp()
        rule.setContent {
            ConstraintLayout(Modifier.size(size)) {
                val (box1, box2) = createRefs()
                val startGuideline = createGuidelineFromStart(guidelinesOffset)
                val topGuideline = createGuidelineFromTop(guidelinesOffset)
                val endGuideline = createGuidelineFromEnd(guidelinesOffset)
                val bottomGuideline = createGuidelineFromBottom(guidelinesOffset)
                createHorizontalChain(box1, box2, chainStyle = ChainStyle.SpreadInside)
                createVerticalChain(box1, box2, chainStyle = ChainStyle.SpreadInside)
                Box(
                    Modifier
                        .size(boxSize)
                        .constrainAs(box1) {
                            start.linkTo(startGuideline)
                            top.linkTo(topGuideline)
                        }
                        .onGloballyPositioned {
                            assertEquals(20f, it.boundsInParent().left)
                            assertEquals(20f, it.boundsInParent().top)
                        }
                )
                Box(
                    Modifier
                        .size(boxSize)
                        .constrainAs(box2) {
                            end.linkTo(endGuideline)
                            bottom.linkTo(bottomGuideline)
                        }
                        .onGloballyPositioned {
                            assertEquals(80f, it.boundsInParent().right)
                            assertEquals(80f, it.boundsInParent().bottom)
                        }
                )
            }
        }
        rule.waitForIdle()
    }

    @Test(expected = Test.None::class)
    fun testConstraintLayout_inlineDSL_recompositionDoesNotCrash() = with(rule.density) {
        val first = mutableStateOf(true)
        rule.setContent {
            ConstraintLayout {
                val box = createRef()
                if (first.value) {
                    Box(Modifier.constrainAs(box) { })
                } else {
                    Box(Modifier.constrainAs(box) { })
                }
            }
        }
        rule.runOnIdle {
            first.value = false
        }
        rule.waitForIdle()
    }

    @Test(expected = Test.None::class)
    fun testConstraintLayout_ConstraintSetDSL_recompositionDoesNotCrash() = with(rule.density) {
        val first = mutableStateOf(true)
        rule.setContent {
            ConstraintLayout(
                ConstraintSet {
                    val box = createRefFor("box")
                    constrain(box) { }
                }
            ) {
                if (first.value) {
                    Box(Modifier.layoutId("box"))
                } else {
                    Box(Modifier.layoutId("box"))
                }
            }
        }
        rule.runOnIdle {
            first.value = false
        }
        rule.waitForIdle()
    }

    @Test(expected = Test.None::class)
    fun testConstraintLayout_inlineDSL_remeasureDoesNotCrash() = with(rule.density) {
        val first = mutableStateOf(true)
        rule.setContent {
            ConstraintLayout(if (first.value) Modifier else Modifier.padding(10.dp)) {
                Box(if (first.value) Modifier else Modifier.size(20.dp))
            }
        }
        rule.runOnIdle {
            first.value = false
        }
        rule.waitForIdle()
    }

    @Test(expected = Test.None::class)
    fun testConstraintLayout_ConstraintSetDSL_remeasureDoesNotCrash() = with(rule.density) {
        val first = mutableStateOf(true)
        rule.setContent {
            ConstraintLayout(
                modifier = if (first.value) Modifier else Modifier.padding(10.dp),
                constraintSet = ConstraintSet { }
            ) {
                Box(if (first.value) Modifier else Modifier.size(20.dp))
            }
        }
        rule.runOnIdle {
            first.value = false
        }
        rule.waitForIdle()
    }

    @Test
    fun testConstraintLayout_doesNotCrashWhenOnlyContentIsRecomposed() {
        var smallSize by mutableStateOf(true)
        rule.setContent {
            Box {
                ConstraintLayout {
                    val (box1, box2) = createRefs()
                    val barrier = createBottomBarrier(box1)
                    Box(
                        Modifier
                            .height(if (smallSize) 30.dp else 40.dp)
                            .constrainAs(box1) {})
                    Box(Modifier)
                }
            }
        }
        rule.runOnIdle {
            smallSize = false
        }
        rule.waitForIdle()
    }

    @Test
    fun testInspectorValue() {
        rule.setContent {
            ConstraintLayout(Modifier.width(10.dp)) {
                val ref = createRef()
                val block: ConstrainScope.() -> Unit = {}
                val modifier = Modifier.constrainAs(ref, block) as InspectableValue

                assertEquals("constrainAs", modifier.nameFallback)
                assertNull(modifier.valueOverride)
                val inspectableElements = modifier.inspectableElements.toList()
                assertEquals(2, inspectableElements.size)
                assertEquals(ValueElement("ref", ref), inspectableElements[0])
                assertEquals(ValueElement("constrainBlock", block), inspectableElements[1])
            }
        }
    }

    @Test
    fun testConstraintLayout_doesNotRemeasureUnnecessarily() {
        var first by mutableStateOf(true)
        var dslExecutions = 0
        rule.setContent {
            val dslExecuted = remember { { ++dslExecutions } }
            ConstraintLayout {
                val (box1) = createRefs()
                val box2 = createRef()
                val guideline = createGuidelineFromStart(0.5f)
                val barrier = createAbsoluteLeftBarrier(box1)

                // Make sure the content is reexecuted when first changes.
                first

                // If the reference changed, we would remeasure and reexecute the DSL.
                Box(Modifier.constrainAs(box1) {})
                // If the guideline, barrier or anchor changed or were inferred as un@Stable, we
                // would remeasure and reexecute the DSL.
                Box(
                    Modifier.constrainAs(box2) {
                        start.linkTo(box1.end)
                        end.linkTo(guideline)
                        start.linkTo(barrier)
                        dslExecuted()
                    }
                )
            }
        }
        rule.runOnIdle {
            assertEquals(1, dslExecutions)
            first = false
        }
        rule.runOnIdle { assertEquals(1, dslExecutions) }
    }

    @Test
    fun testConstraintLayout_doesRemeasure_whenHelpersChange_butConstraintsDont() {
        val size = 100
        val sizeDp = with(rule.density) { size.toDp() }
        var first by mutableStateOf(true)
        var box1Position = Offset(-1f, -1f)
        var box2Position = Offset(-1f, -1f)
        val box1PositionUpdater =
            Modifier.onGloballyPositioned { box1Position = it.positionInRoot() }
        val box2PositionUpdater =
            Modifier.onGloballyPositioned { box2Position = it.positionInRoot() }
        rule.setContent {
            ConstraintLayout {
                val (box1, box2) = createRefs()

                if (!first) {
                    createVerticalChain(box1, box2)
                }

                Box(
                    Modifier
                        .size(sizeDp)
                        .then(box1PositionUpdater)
                        .constrainAs(box1) {})
                Box(
                    Modifier
                        .size(sizeDp)
                        .then(box2PositionUpdater)
                        .constrainAs(box2) {})
            }
        }
        rule.runOnIdle {
            assertEquals(Offset.Zero, box1Position)
            assertEquals(Offset.Zero, box2Position)
            first = false
        }
        rule.runOnIdle {
            assertEquals(Offset.Zero, box1Position)
            assertEquals(Offset(0f, size.toFloat()), box2Position)
        }
    }

    @Test
    fun testConstraintLayout_doesRemeasure_whenHelpersDontChange_butConstraintsDo() {
        val size = 100
        val sizeDp = with(rule.density) { size.toDp() }
        var first by mutableStateOf(true)
        var box1Position = Offset(-1f, -1f)
        var box2Position = Offset(-1f, -1f)
        val box1PositionUpdater =
            Modifier.onGloballyPositioned { box1Position = it.positionInRoot() }
        val box2PositionUpdater =
            Modifier.onGloballyPositioned { box2Position = it.positionInRoot() }
        rule.setContent {
            ConstraintLayout {
                val (box1, box2) = createRefs()

                val topBarrier = createTopBarrier(box1)
                val bottomBarrier = createBottomBarrier(box1)

                Box(
                    Modifier
                        .size(sizeDp)
                        .then(box1PositionUpdater)
                        .constrainAs(box1) {})
                Box(
                    Modifier
                        .size(sizeDp)
                        .then(box2PositionUpdater)
                        .constrainAs(box2) {
                            if (first) {
                                top.linkTo(topBarrier)
                            } else {
                                top.linkTo(bottomBarrier)
                            }
                        }
                )
            }
        }
        rule.runOnIdle {
            assertEquals(Offset.Zero, box1Position)
            assertEquals(Offset.Zero, box2Position)
            first = false
        }
        rule.runOnIdle {
            assertEquals(Offset.Zero, box1Position)
            assertEquals(Offset(0f, size.toFloat()), box2Position)
        }
    }

    @Test
    fun testConstraintLayout_updates_whenConstraintSetChanges() = with(rule.density) {
        val box1Size = 20
        var first by mutableStateOf(true)
        val constraintSet1 = ConstraintSet {
            val box1 = createRefFor("box1")
            val box2 = createRefFor("box2")
            constrain(box2) {
                start.linkTo(box1.end)
            }
        }
        val constraintSet2 = ConstraintSet {
            val box1 = createRefFor("box1")
            val box2 = createRefFor("box2")
            constrain(box2) {
                top.linkTo(box1.bottom)
            }
        }

        var box2Position = IntOffset.Zero
        rule.setContent {
            ConstraintLayout(if (first) constraintSet1 else constraintSet2) {
                Box(
                    Modifier
                        .size(box1Size.toDp())
                        .layoutId("box1")
                )
                Box(
                    Modifier
                        .layoutId("box2")
                        .onGloballyPositioned {
                            box2Position = it
                                .positionInRoot()
                                .round()
                        }
                )
            }
        }

        rule.runOnIdle {
            assertEquals(IntOffset(box1Size, 0), box2Position)
            first = false
        }

        rule.runOnIdle {
            assertEquals(IntOffset(0, box1Size), box2Position)
        }
    }

    @Test
    fun testConstraintLayout_doesNotRebuildFromDsl_whenResizedOnly() = with(rule.density) {
        var size by mutableStateOf(100.dp)
        var builds = 0
        rule.setContent {
            val onBuild = remember { { ++builds } }
            ConstraintLayout(Modifier.size(size)) {
                val box = createRef()
                Box(Modifier.constrainAs(box) { onBuild() })
            }
        }

        rule.runOnIdle {
            assertEquals(1, builds)
            size = 200.dp
        }

        rule.runOnIdle {
            assertEquals(1, builds)
        }
    }

    @Test
    fun testConstraintLayout_rebuildsConstraintSet_whenHelpersChange() = with(rule.density) {
        var offset by mutableStateOf(10.dp)
        var builds = 0
        var obtainedX = 0f
        rule.setContent {
            ConstraintLayout {
                val box = createRef()
                val g = createGuidelineFromStart(offset)
                Box(
                    Modifier
                        .constrainAs(box) {
                            start.linkTo(g)
                            ++builds
                        }
                        .onGloballyPositioned { obtainedX = it.positionInRoot().x })
            }
        }

        rule.runOnIdle {
            assertEquals(offset.roundToPx().toFloat(), obtainedX)
            offset = 20.dp
            assertEquals(1, builds)
        }

        rule.runOnIdle {
            assertEquals(offset.roundToPx().toFloat(), obtainedX)
            assertEquals(2, builds)
        }
    }

    @Test
    fun testConstraintLayout_rebuilds_whenLambdaChanges() = with(rule.density) {
        var first by mutableStateOf(true)
        var obtainedX = 0f
        rule.setContent {
            ConstraintLayout {
                val l1 = remember<ConstrainScope.() -> Unit> {
                    { start.linkTo(parent.start, 10.dp) }
                }
                val l2 = remember<ConstrainScope.() -> Unit> {
                    { start.linkTo(parent.start, 20.dp) }
                }
                val box = createRef()
                Box(
                    Modifier
                        .constrainAs(box, if (first) l1 else l2)
                        .onGloballyPositioned {
                            obtainedX = it.positionInRoot().x
                        })
            }
        }

        rule.runOnIdle {
            assertEquals(10.dp.roundToPx().toFloat(), obtainedX)
            first = false
        }

        rule.runOnIdle {
            assertEquals(20.dp.roundToPx().toFloat(), obtainedX)
        }
    }

    @Test
    fun testConstraintLayout_updates_whenConstraintSetChangesConstraints() = with(rule.density) {
        val box1Size = 20
        var first by mutableStateOf(true)
        val constraintSet = ConstraintSet {
            val box1 = createRefFor("box1")
            val box2 = createRefFor("box2")
            constrain(box2) {
                if (first) start.linkTo(box1.end) else top.linkTo(box1.bottom)
            }
        }

        var box2Position = IntOffset.Zero
        rule.setContent {
            ConstraintLayout(constraintSet) {
                Box(
                    Modifier
                        .size(box1Size.toDp())
                        .layoutId("box1")
                )
                Box(
                    Modifier
                        .layoutId("box2")
                        .onGloballyPositioned {
                            box2Position = it
                                .positionInRoot()
                                .round()
                        }
                )
            }
        }

        rule.runOnIdle {
            assertEquals(IntOffset(box1Size, 0), box2Position)
            first = false
        }

        rule.runOnIdle {
            assertEquals(IntOffset(0, box1Size), box2Position)
        }
    }

    @Test
    fun testClearDerivedConstraints_withConstraintSet() {
        var startOrEnd by mutableStateOf(true)
        val boxTag = "box1"
        rule.setContent {
            val start = remember {
                ConstraintSet {
                    constrain(createRefFor(boxTag)) {
                        width = Dimension.value(20.dp)
                        height = Dimension.value(20.dp)
                        start.linkTo(parent.start, 10.dp)
                        bottom.linkTo(parent.bottom, 10.dp)
                    }
                }
            }
            val end = remember {
                ConstraintSet(start) {
                    constrain(createRefFor(boxTag)) {
                        clearConstraints()
                        top.linkTo(parent.top, 5.dp)
                        end.linkTo(parent.end, 5.dp)
                    }
                }
            }
            ConstraintLayout(
                modifier = Modifier.size(200.dp),
                constraintSet = if (startOrEnd) start else end
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Red)
                        .testTag(boxTag)
                        .layoutId(boxTag)
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag(boxTag).assertPositionInRootIsEqualTo(10.dp, 170.dp)
        rule.runOnIdle {
            startOrEnd = !startOrEnd
        }
        rule.waitForIdle()
        rule.onNodeWithTag(boxTag).assertPositionInRootIsEqualTo(175.dp, 5.dp)
    }

    @Test
    fun testLayoutReference_withConstraintSet() {
        val boxTag1 = "box1"
        val boxTag2 = "box2"
        val constraintSet = ConstraintSet {
            val box1 = createRefFor(boxTag1)
            val g1 = createGuidelineFromEnd(50.dp)
            val b1 = createEndBarrier(g1.reference, box1)

            constrain(box1) {
                width = Dimension.value(10.dp)
                height = Dimension.value(10.dp)
                top.linkTo(parent.top)
                end.linkTo(g1, 10.dp)
            }
            constrain(createRefFor(boxTag2)) {
                width = Dimension.value(10.dp)
                height = Dimension.value(10.dp)
                top.linkTo(parent.top)
                start.linkTo(b1, 10.dp)
            }
        }
        rule.setContent {
            ConstraintLayout(
                modifier = Modifier.size(100.dp),
                constraintSet = constraintSet
            ) {
                Box(
                    modifier = Modifier
                        .layoutTestId(boxTag1)
                        .background(Color.Red)
                )
                Box(
                    modifier = Modifier
                        .layoutTestId(boxTag2)
                        .background(Color.Blue)
                )
            }
        }
        rule.waitForIdle()
        // TODO: Investigate, Left position should be 30.dp
        rule.onNodeWithTag(boxTag1).assertPositionInRootIsEqualTo(29.5.dp, 0.dp)
        rule.onNodeWithTag(boxTag2).assertPositionInRootIsEqualTo(60.dp, 0.dp)
    }

    @Test
    fun testLayoutReference_withInlineDsl() {
        val boxTag1 = "box1"
        val boxTag2 = "box2"
        rule.setContent {
            ConstraintLayout(modifier = Modifier.size(100.dp)) {
                val (box1, box2) = createRefs()
                val g1 = createGuidelineFromEnd(50.dp)
                val b1 = createEndBarrier(g1.reference, box1)
                Box(modifier = Modifier
                    .constrainAs(box1) {
                        width = Dimension.value(10.dp)
                        height = Dimension.value(10.dp)
                        top.linkTo(parent.top)
                        end.linkTo(g1, 10.dp)
                    }
                    .layoutTestId(boxTag1)
                    .background(Color.Red))
                Box(modifier = Modifier
                    .constrainAs(box2) {
                        width = Dimension.value(10.dp)
                        height = Dimension.value(10.dp)
                        top.linkTo(parent.top)
                        start.linkTo(b1, 10.dp)
                    }
                    .layoutTestId(boxTag2)
                    .background(Color.Blue))
            }
        }
        rule.waitForIdle()
        // TODO: Investigate, Left position should be 30.dp
        rule.onNodeWithTag(boxTag1).assertPositionInRootIsEqualTo(29.5.dp, 0.dp)
        rule.onNodeWithTag(boxTag2).assertPositionInRootIsEqualTo(60.dp, 0.dp)
    }
}