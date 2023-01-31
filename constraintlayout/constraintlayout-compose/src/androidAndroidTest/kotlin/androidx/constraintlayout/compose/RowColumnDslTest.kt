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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the Grid Helper (Row / Column)
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RowColumnDslTest {
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
    fun testColumns() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            ColumnComposableTest(
                modifier = Modifier.size(rootSize),
                boxesCount = boxesCount,
                vGap = 0,
                gridRowWeights = intArrayOf(),
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
    fun testRows() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            RowComposableTest(
                modifier = Modifier.size(rootSize),
                boxesCount = boxesCount,
                hGap = 0,
                gridColumnWeights = intArrayOf()
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

    @Composable
    private fun ColumnComposableTest(
        modifier: Modifier = Modifier,
        gridRowWeights: IntArray,
        boxesCount: Int,
        vGap: Int,
    ) {
        ConstraintLayout(
            ConstraintSet {
                val ids = (0 until boxesCount).map { "box$it" }.toTypedArray()
                val elem = arrayListOf<LayoutReference>()
                for (i in ids.indices) {
                    elem.add(createRefFor(ids[i]))
                }

                val g1 = createColumn(
                    elements = elem.toTypedArray(),
                    verticalGap = vGap.dp,
                    rowWeights = gridRowWeights,
                )
                constrain(g1) {
                    width = Dimension.matchParent
                    height = Dimension.matchParent
                }
            },
            modifier = modifier
        ) {
            val ids = (0 until boxesCount).map { "box$it" }.toTypedArray()
            ids.forEach { id ->
                Box(
                    Modifier
                        .layoutId(id)
                        .background(Color.Red)
                        .testTag(id)
                        .size(10.dp)
                )
            }
        }
    }

    @Composable
    private fun RowComposableTest(
        modifier: Modifier = Modifier,
        gridColumnWeights: IntArray,
        boxesCount: Int,
        hGap: Int,
    ) {
        ConstraintLayout(
            ConstraintSet {
                val ids = (0 until boxesCount).map { "box$it" }.toTypedArray()
                val elem = arrayListOf<LayoutReference>()
                for (i in ids.indices) {
                    elem.add(createRefFor(ids[i]))
                }

                val g1 = createRow(
                    elements = elem.toTypedArray(),
                    horizontalGap = hGap.dp,
                    columnWeights = gridColumnWeights,
                )
                constrain(g1) {
                    width = Dimension.matchParent
                    height = Dimension.matchParent
                }
            },
            modifier = modifier
        ) {
            val ids = (0 until boxesCount).map { "box$it" }.toTypedArray()
            ids.forEach { id ->
                Box(
                    Modifier
                        .layoutId(id)
                        .background(Color.Red)
                        .testTag(id)
                        .size(10.dp)
                )
            }
        }
    }
}