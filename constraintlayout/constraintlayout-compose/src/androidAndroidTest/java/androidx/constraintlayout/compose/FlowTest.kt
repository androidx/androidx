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
