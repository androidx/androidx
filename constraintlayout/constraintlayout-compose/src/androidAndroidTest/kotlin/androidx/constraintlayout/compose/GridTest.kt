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
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the Grid Helper
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class GridTest {
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
    fun testTwoByTwo() {
        val rootSize = 200.dp
        val boxesCount = 4
        val rows = 2
        val columns = 2
        rule.setContent {
            gridComposableTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                orientation = 0,
                rows = rows,
                columns = columns,
                hGap = 0,
                vGap = 0,
                spans = "''",
                skips = "''",
                rowWeights = "''",
                columnWeights = "''"
            )
        }
        var leftX = 0.dp
        var topY = 0.dp
        var rightX: Dp
        var bottomY: Dp

        // 10.dp is the size of a singular box
        val gapSize = (rootSize - (10.dp * 2f)) / (columns * 2f)
        rule.waitForIdle()
        leftX += gapSize
        topY += gapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(leftX, topY)
        rightX = leftX + 10.dp + gapSize + gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(rightX, topY)
        bottomY = topY + 10.dp + gapSize + gapSize
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(leftX, bottomY)
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(rightX, bottomY)
    }

    @Test
    fun testOrientation() {
        val rootSize = 200.dp
        val boxesCount = 4
        val rows = 2
        val columns = 2
        rule.setContent {
            gridComposableTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                orientation = 1,
                rows = rows,
                columns = columns,
                hGap = 0,
                vGap = 0,
                spans = "''",
                skips = "''",
                rowWeights = "''",
                columnWeights = "''"
            )
        }
        var leftX = 0.dp
        var topY = 0.dp
        var rightX: Dp
        var bottomY: Dp

        // 10.dp is the size of a singular box
        val gapSize = (rootSize - (10.dp * 2f)) / (columns * 2f)
        rule.waitForIdle()
        leftX += gapSize
        topY += gapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(leftX, topY)
        rightX = leftX + 10.dp + gapSize + gapSize
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(rightX, topY)
        bottomY = topY + 10.dp + gapSize + gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(leftX, bottomY)
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(rightX, bottomY)
    }

    @Test
    fun testRows() {
        val rootSize = 200.dp
        val boxesCount = 4
        val rows = 0
        val columns = 1
        rule.setContent {
            gridComposableTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                orientation = 0,
                rows = rows,
                columns = columns,
                hGap = 0,
                vGap = 0,
                spans = "''",
                skips = "''",
                rowWeights = "''",
                columnWeights = "''"
            )
        }
        var expectedX = 0.dp
        var expectedY = 0.dp

        // 10.dp is the size of a singular box
        val hGapSize = (rootSize - 10.dp) / 2f
        val vGapSize = (rootSize - (10.dp * 4f)) / (boxesCount * 2f)
        rule.waitForIdle()
        expectedX += hGapSize
        expectedY += vGapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, expectedY)
    }

    @Test
    fun testColumns() {
        val rootSize = 200.dp
        val boxesCount = 4
        val rows = 1
        val columns = 0
        rule.setContent {
            gridComposableTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                orientation = 0,
                rows = rows,
                columns = columns,
                hGap = 0,
                vGap = 0,
                spans = "''",
                skips = "''",
                rowWeights = "''",
                columnWeights = "''"
            )
        }
        var expectedX = 0.dp
        var expectedY = 0.dp

        // 10.dp is the size of a singular box
        val hGapSize = (rootSize - (10.dp * 4f)) / (boxesCount * 2f)
        val vGapSize = (rootSize - 10.dp) / 2f
        rule.waitForIdle()
        expectedX += hGapSize
        expectedY += vGapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, expectedY)
    }

    @Test
    fun testSkips() {
        val rootSize = 200.dp
        val boxesCount = 3
        val rows = 2
        val columns = 2
        rule.setContent {
            gridComposableTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                orientation = 0,
                rows = rows,
                columns = columns,
                hGap = 0,
                vGap = 0,
                spans = "''",
                skips = "'0:1x1'",
                rowWeights = "''",
                columnWeights = "''"
            )
        }
        var leftX = 0.dp
        var topY = 0.dp
        var rightX: Dp
        var bottomY: Dp

        // 10.dp is the size of a singular box
        val gapSize = (rootSize - (10.dp * 2f)) / (columns * 2f)
        rule.waitForIdle()
        leftX += gapSize
        topY += gapSize
        rightX = leftX + 10.dp + gapSize + gapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(rightX, topY)
        bottomY = topY + 10.dp + gapSize + gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(leftX, bottomY)
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(rightX, bottomY)
    }

    @Test
    fun testSpans() {
        val rootSize = 200.dp
        val boxesCount = 3
        val rows = 2
        val columns = 2
        rule.setContent {
            gridComposableTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                orientation = 0,
                rows = rows,
                columns = columns,
                hGap = 0,
                vGap = 0,
                spans = "'0:1x2'",
                skips = "''",
                rowWeights = "''",
                columnWeights = "''"
            )
        }
        var leftX = 0.dp
        var topY = 0.dp
        var rightX: Dp
        var bottomY: Dp

        // 10.dp is the size of a singular box
        var spanLeft = (rootSize - 10.dp) / 2f
        val gapSize = (rootSize - (10.dp * 2f)) / (columns * 2f)
        rule.waitForIdle()
        leftX += gapSize
        topY += gapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(spanLeft, topY)
        rightX = leftX + 10.dp + gapSize + gapSize
        bottomY = topY + 10.dp + gapSize + gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(leftX, bottomY)
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(rightX, bottomY)
    }

    @Test
    fun testRowWeights() {
        val rootSize = 200.dp
        val boxesCount = 2
        val rows = 0
        val columns = 1
        rule.setContent {
            gridComposableTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                orientation = 0,
                rows = rows,
                columns = columns,
                hGap = 0,
                vGap = 0,
                spans = "''",
                skips = "''",
                rowWeights = "'1,3'",
                columnWeights = "''"
            )
        }
        var expectedLeft = (rootSize - 10.dp) / 2f
        var expectedTop = 0.dp

        // 10.dp is the size of a singular box
        // first box takes the 1/4 of the height
        val firstGapSize = (rootSize / 4 - 10.dp) / 2
        // second box takes the 3/4 of the height
        val secondGapSize = ((rootSize * 3 / 4) - 10.dp) / 2
        rule.waitForIdle()
        expectedTop += firstGapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedLeft, expectedTop)
        expectedTop += 10.dp + firstGapSize + secondGapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedLeft, expectedTop)
    }

    @Test
    fun testColumnWeights() {
        val rootSize = 200.dp
        val boxesCount = 2
        val rows = 1
        val columns = 0
        rule.setContent {
            gridComposableTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                orientation = 0,
                rows = rows,
                columns = columns,
                hGap = 0,
                vGap = 0,
                spans = "''",
                skips = "''",
                rowWeights = "''",
                columnWeights = "'1,3'"
            )
        }
        var expectedLeft = 0.dp
        var expectedTop = (rootSize - 10.dp) / 2f

        // 10.dp is the size of a singular box
        // first box takes the 1/4 of the width
        val firstGapSize = (rootSize / 4 - 10.dp) / 2
        // second box takes the 3/4 of the width
        val secondGapSize = ((rootSize * 3 / 4) - 10.dp) / 2
        rule.waitForIdle()
        expectedLeft += firstGapSize

        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedLeft, expectedTop)
        expectedLeft += 10.dp + firstGapSize + secondGapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedLeft, expectedTop)
    }

    @Test
    fun testGaps() {
        val rootSize = 200.dp
        val hGap = 10.dp
        val vGap = 20.dp
        rule.setContent {
            gridComposableGapTest(
                modifier = Modifier.size(rootSize),
                width = "'parent'",
                height = "'parent'",
                hGap = Math.round(hGap.value),
                vGap = Math.round(vGap.value),
            )
        }
        var expectedLeft = 0.dp
        var expectedTop = 0.dp

        val boxWidth = (rootSize - hGap) / 2f
        val boxHeight = (rootSize - vGap) / 2f

        rule.waitForIdle()
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(0.dp, 0.dp)
        expectedLeft += boxWidth + hGap
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedLeft, 0.dp)
        expectedTop += boxHeight + vGap
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(0.dp, expectedTop)
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedLeft, expectedTop)
    }

    @Composable
    private fun gridComposableTest(
        modifier: Modifier = Modifier,
        width: String,
        height: String,
        rows: Int,
        columns: Int,
        spans: String,
        skips: String,
        rowWeights: String,
        columnWeights: String,
        boxesCount: Int,
        orientation: Int,
        vGap: Int,
        hGap: Int,
    ) {
        val ids = (0 until boxesCount).map { "box$it" }.toTypedArray()
        val gridContains = ids.joinToString(separator = ", ") { "'$it'" }
        ConstraintLayout(
            modifier = modifier,
            constraintSet = ConstraintSet(
                """
        {
            grid: {
                width: $width,
                height: $height,
                type: 'grid',
                rows: $rows,
                columns $columns,
                vGap: $vGap,
                hGap: $hGap,
                spans: $spans,
                skips: $skips,
                rowWeights: $rowWeights,
                columnWeights: $columnWeights
                orientation: $orientation,
                contains: [$gridContains],
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

    @Composable
    private fun gridComposableGapTest(
        modifier: Modifier = Modifier,
        width: String,
        height: String,
        vGap: Int,
        hGap: Int,
    ) {
        val ids = (0 until 4).map { "box$it" }.toTypedArray()
        val gridContains = ids.joinToString(separator = ", ") { "'$it'" }

        ConstraintLayout(
            modifier = modifier,
            constraintSet = ConstraintSet(
                """
        {
            grid: {
                width: $width,
                height: $height,
                type: 'grid',
                rows: 2,
                columns: 2,
                vGap: $vGap,
                hGap: $hGap,
                contains: [$gridContains],
              },
              box0: {
                width: 'spread',
                height: 'spread',
              },
              box1: {
                width: 'spread',
                height: 'spread',
              },
              box2: {
                width: 'spread',
                height: 'spread',
              },
              box3: {
                width: 'spread',
                height: 'spread',
              }
        }
        """.trimIndent()
            )
        ) {
            ids.forEach { id ->
                Box(
                    Modifier
                        .layoutId(id)
                        .background(Color.Red)
                        .testTag(id)
                )
            }
        }
    }
}