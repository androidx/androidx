/*
 * Copyright (C) 2022 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FlowTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setup() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testMatchParent_horizontal() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            FlowComposableTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                isHorizontal = true
            )
        }
        var expectedX = 0.dp

        // 10.dp is the size of a singular box
        val gapSize = ((rootSize - (10.dp * boxesCount)) / (boxesCount - 1)) + 10.dp
        rule.waitForIdle()
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, 94.9.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, 94.9.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, 94.9.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, 94.9.dp)
    }

    @Test
    fun testMatchParent_vertical() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            FlowComposableTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                isHorizontal = false
            )
        }
        var expectedY = 0.dp

        // 10.dp is the size of a singular box
        val gapSize = ((rootSize - (10.dp * boxesCount)) / (boxesCount - 1)) + 10.dp
        rule.waitForIdle()
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(94.9.dp, expectedY)
        expectedY += gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(94.9.dp, expectedY)
        expectedY += gapSize
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(94.9.dp, expectedY)
        expectedY += gapSize
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(94.9.dp, expectedY)
    }

    @Test
    fun testFixedSizeMatchesRoot_horizontal() {
        val size = 200.dp
        val boxesCount = 4
        rule.setContent {
            FlowComposableTest(
                modifier = Modifier.size(size),
                width = size.value.toInt().toString(),
                height = size.value.toInt().toString(),
                boxesCount = boxesCount,
                isHorizontal = true
            )
        }
        var expectedX = 0.dp

        // 10.dp is the size of a singular box
        val gapSize = ((size - (10.dp * boxesCount)) / (boxesCount - 1)) + 10.dp
        rule.waitForIdle()
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, 94.9.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, 94.9.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, 94.9.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, 94.9.dp)
    }

    @Test
    fun testFixedSizeMatchesRoot_vertical() {
        val size = 200.dp
        val boxesCount = 4
        rule.setContent {
            FlowComposableTest(
                modifier = Modifier.size(size),
                width = size.value.toInt().toString(),
                height = size.value.toInt().toString(),
                boxesCount = boxesCount,
                isHorizontal = false
            )
        }
        var expectedY = 0.dp

        // 10.dp is the size of a singular box
        val gapSize = ((size - (10.dp * boxesCount)) / (boxesCount - 1)) + 10.dp
        rule.waitForIdle()
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(94.9.dp, expectedY)
        expectedY += gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(94.9.dp, expectedY)
        expectedY += gapSize
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(94.9.dp, expectedY)
        expectedY += gapSize
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(94.9.dp, expectedY)
    }

    @Test
    fun testFixedSizeSmallerThanRoot_horizontal() {
        val rootSize = 200.dp

        // Flow Size Half as big as ConstraintLayout
        val size = 100.dp
        val boxesCount = 4
        rule.setContent {
            FlowComposableTest(
                modifier = Modifier.size(rootSize),
                width = size.value.toInt().toString(),
                height = size.value.toInt().toString(),
                boxesCount = boxesCount,
                isHorizontal = true
            )
        }
        var expectedX = 0.dp

        // 10.dp is the size of a singular box
        val gapSize = ((size - (10.dp * boxesCount)) / (boxesCount - 1)) + 10.dp
        rule.waitForIdle()
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, 45.1.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, 45.1.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, 45.1.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, 45.1.dp)
    }

    @Test
    fun testFixedSizeBiggerThanRoot_horizontal() {
        val rootSize = 200.dp

        // Flow twice as big as ConstraintLayout
        val size = 400.dp
        val boxesCount = 4
        rule.setContent {
            FlowComposableTest(
                modifier = Modifier.size(rootSize),
                width = size.value.toInt().toString(),
                height = size.value.toInt().toString(),
                boxesCount = boxesCount,
                isHorizontal = true
            )
        }
        var expectedX = 0.dp

        // 10.dp is the size of a singular box
        val gapSize = ((size - (10.dp * boxesCount)) / (boxesCount - 1)) + 10.dp
        rule.waitForIdle()
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, 194.9.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, 194.9.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, 194.9.dp)
        expectedX += gapSize
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, 194.9.dp)
    }

    @Test
    fun testChainModeRespectsMaxElements_horizontal() = with(rule.density) {
        val baseBoxSizePx = 40
        val itemCount = 6
        val maxColumns = 3

        // Make one item twice as big so that it pushes the next element into another row
        val indexOfBigItem = 4

        val horizontalPadding = 10

        // Limit the width to fit the desired columns plus some padding
        val flowWidth = baseBoxSizePx * maxColumns + horizontalPadding

        val positions = mutableMapOf<Int, IntOffset>()

        val constraintSet = ConstraintSet {
            val itemRefs = List(itemCount) { createRefFor("item$it") }.toTypedArray()
            val flow = createFlow(
                elements = itemRefs,
                flowVertically = false,
                maxElement = maxColumns,
                wrapMode = Wrap.Chain,
                horizontalStyle = FlowStyle.SpreadInside
            )

            constrain(flow) {
                width = flowWidth.toDp().asDimension()

                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
            itemRefs.forEachIndexed { index, itemRef ->
                constrain(itemRef) {
                    val widthPx =
                        if (index == indexOfBigItem) baseBoxSizePx.times(2) else baseBoxSizePx
                    width = widthPx.toDp().asDimension()
                    height = baseBoxSizePx.toDp().asDimension()
                }
            }
        }

        rule.setContent {
            ConstraintLayout(constraintSet) {
                for (i in 0 until itemCount) {
                    Box(
                        Modifier
                            .layoutId("item$i")
                            .background(Color.Red)
                            .onGloballyPositioned {
                                positions[i] = it
                                    .positionInParent()
                                    .round()
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertEquals(6, positions.size)

            // Row 0
            var expectedX = 0
            var expectedY = baseBoxSizePx * 0
            assertEquals(IntOffset(expectedX, expectedY), positions[0])

            expectedX += baseBoxSizePx + horizontalPadding / 2 // padding split in 2 spaces
            assertEquals(IntOffset(expectedX, expectedY), positions[1])

            expectedX += baseBoxSizePx + horizontalPadding / 2 // padding split in 2 spaces
            assertEquals(IntOffset(expectedX, expectedY), positions[2])

            // Row 1
            expectedX = 0
            expectedY = baseBoxSizePx * 1
            assertEquals(IntOffset(expectedX, expectedY), positions[3])

            expectedX = baseBoxSizePx + horizontalPadding
            assertEquals(IntOffset(expectedX, expectedY), positions[4])

            // Row 2
            expectedX = 0
            expectedY = baseBoxSizePx * 2
            assertEquals(IntOffset(expectedX, expectedY), positions[5])
        }
    }

    @Test
    fun testChainModeRespectsMaxElements_vertical() = with(rule.density) {
        val baseBoxSizePx = 40
        val itemCount = 6
        val maxRows = 3

        // Make one item twice as big so that it pushes the next element into another column
        val indexOfBigItem = 4

        val verticalPadding = 10

        // Limit the height to fit the desired rows plus some padding
        val flowHeight = baseBoxSizePx * maxRows + verticalPadding

        val positions = mutableMapOf<Int, IntOffset>()

        val constraintSet = ConstraintSet {
            val itemRefs = List(itemCount) { createRefFor("item$it") }.toTypedArray()
            val flow = createFlow(
                elements = itemRefs,
                flowVertically = true,
                maxElement = maxRows,
                wrapMode = Wrap.Chain,
                verticalStyle = FlowStyle.SpreadInside
            )

            constrain(flow) {
                height = flowHeight.toDp().asDimension()

                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
            itemRefs.forEachIndexed { index, itemRef ->
                constrain(itemRef) {
                    val widthPx =
                        if (index == indexOfBigItem) baseBoxSizePx.times(2) else baseBoxSizePx
                    height = widthPx.toDp().asDimension()
                    width = baseBoxSizePx.toDp().asDimension()
                }
            }
        }

        rule.setContent {
            ConstraintLayout(constraintSet) {
                for (i in 0 until itemCount) {
                    Box(
                        Modifier
                            .layoutId("item$i")
                            .background(Color.Red)
                            .onGloballyPositioned {
                                positions[i] = it
                                    .positionInParent()
                                    .round()
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertEquals(6, positions.size)

            // Column 0
            var expectedX = baseBoxSizePx * 0
            var expectedY = 0
            assertEquals(IntOffset(expectedX, expectedY), positions[0])

            expectedY += baseBoxSizePx + verticalPadding / 2 // padding split in 2 spaces
            assertEquals(IntOffset(expectedX, expectedY), positions[1])

            expectedY += baseBoxSizePx + verticalPadding / 2 // padding split in 2 spaces
            assertEquals(IntOffset(expectedX, expectedY), positions[2])

            // Column 1
            expectedX = baseBoxSizePx * 1
            expectedY = 0
            assertEquals(IntOffset(expectedX, expectedY), positions[3])

            expectedY = baseBoxSizePx + verticalPadding
            assertEquals(IntOffset(expectedX, expectedY), positions[4])

            // Column 2
            expectedX = baseBoxSizePx * 2
            expectedY = 0
            assertEquals(IntOffset(expectedX, expectedY), positions[5])
        }
    }
}

@Composable
private fun FlowComposableTest(
    modifier: Modifier = Modifier,
    width: String,
    height: String,
    boxesCount: Int,
    isHorizontal: Boolean
) {
    val ids = (0 until boxesCount).map { "box$it" }.toTypedArray()
    val type = if (isHorizontal) "hFlow" else "vFlow"
    val flowContains = ids.joinToString(separator = ", ") { "'$it'" }

    ConstraintLayout(
        modifier = modifier,
        constraintSet = ConstraintSet(
            """
        {
            flow1: {
                width: $width,
                height: $height,
                type: '$type',
                hStyle: 'spread_inside',
                vStyle: 'spread_inside',
                contains: [$flowContains],
              }
        }
        """.trimIndent()
        )
    ) {
        ids.forEach { id ->
            Box(
                Modifier
                    .layoutId(id)
                    .size(10.dp)
                    .background(Color.Red)
                    .testTag(id)
            )
        }
    }
}
