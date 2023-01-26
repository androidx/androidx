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
class RowColumnTest {
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
    fun testRows() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            RowColumnComposableTest(
                modifier = Modifier.size(rootSize),
                type = "'row'",
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                orientation = 0,
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
        rule.setContent {
            RowColumnComposableTest(
                modifier = Modifier.size(rootSize),
                type = "'column'",
                width = "'parent'",
                height = "'parent'",
                boxesCount = boxesCount,
                orientation = 0,
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

    @Composable
    private fun RowColumnComposableTest(
        modifier: Modifier = Modifier,
        type: String,
        width: String,
        height: String,
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
                type: $type,
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
}