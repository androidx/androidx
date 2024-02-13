/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.foundation.clickable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalLayoutApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class ContextualFlowRowColumnTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testContextualFlowRow_wrapsToTheNextLine() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .onSizeChanged {
                                height = it.height
                            },
                        itemCount = 6
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testContextualFlowRow_wrapsToTheNextLine_MultipleContentPerIndex() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .onSizeChanged {
                                height = it.height
                            },
                        itemCount = 3
                    ) {
                        repeat(2) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testContextualFlowRow_wrapsToTheNextLine_NoContentPlusMultiplePerIndex() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .onSizeChanged {
                                height = it.height
                            },
                        itemCount = 3
                    ) { index ->
                        if (index == 0) {
                            repeat(5) {
                                Box(Modifier.size(20.toDp()))
                            }
                        } else if (index == 1) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testContextualFlowColumn_wrapsToTheNextLine() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    ContextualFlowColumn(
                        modifier = Modifier
                            .onSizeChanged {
                                width = it.width
                            },
                        itemCount = 6
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testContextualFlowRow_wrapsToTheNextLine_withExactSpaceNeeded() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .onSizeChanged {
                                height = it.height
                            },
                        itemCount = 10
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testContextualFlowColumn_wrapsToTheNextLine_withExactSpaceNeeded() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    ContextualFlowColumn(
                        modifier = Modifier
                            .onSizeChanged {
                                width = it.width
                            },
                        itemCount = 10
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testContextualFlowRow_wrapsToTheNextLineMultipleTimes() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .onSizeChanged {
                                height = it.height
                            },
                        itemCount = 6
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testContextualFlowRow_doesNotCrashOnEmpty() {
        var itemShown = 0
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                        itemCount = 10
                    ) {
                        if (it in 2..5 || it == 9) {
                        } else {
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        itemShown++
                                    }
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemShown).isEqualTo(5)
        }
    }

    @Test
    fun testContextualFlowColumn_wrapsToTheNextLineMultipleTimes() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    ContextualFlowColumn(
                        itemCount = 6,
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testContextualFlowRow_wrapsWithMaxItems() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    ContextualFlowRow(
                        itemCount = 6,
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }, maxItemsInEachRow = 2
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(60)
    }

    @Test
    fun testContextualFlowColumn_wrapsWithMaxItems() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    ContextualFlowColumn(
                        itemCount = 6,
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }, maxItemsInEachColumn = 2
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testContextualFlowRow_wrapsWithWeights() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    ContextualFlowRow(
                        itemCount = 6,
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }, maxItemsInEachRow = 2
                    ) {
                        Box(
                            Modifier
                                .size(20.toDp())
                                .weight(1f, true)
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(60)
    }

    @Test
    fun testContextualFlowColumn_wrapsWithWeights() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    ContextualFlowColumn(
                        itemCount = 6,
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }, maxItemsInEachColumn = 2
                    ) {
                        Box(
                            Modifier
                                .size(20.toDp())
                                .weight(1f, true)
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testContextualFlowRow_staysInOneRow() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    ContextualFlowRow(
                        itemCount = 2,
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(20)
    }

    @Test
    fun testContextualFlowRow_equalHeight() {
        val listOfHeights = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    itemCount = 9,
                    Modifier
                        .fillMaxWidth(1f)
                        .padding(20.dp)
                        .wrapContentHeight(align = Alignment.Top),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    maxItemsInEachRow = 3,
                ) {
                    Box(
                        Modifier
                            .onSizeChanged {
                                listOfHeights.add(it.height)
                            }
                            .width(100.dp)
                            .background(Color.Green)
                            .fillMaxRowHeight()
                    ) {
                        val height = it * 20
                        Box(modifier = Modifier.height(height.dp))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(listOfHeights[0]).isEqualTo(listOfHeights[1])
        Truth.assertThat(listOfHeights[1]).isEqualTo(listOfHeights[2])
        Truth.assertThat(listOfHeights[2]).isNotEqualTo(listOfHeights[3])
        Truth.assertThat(listOfHeights[3]).isEqualTo(listOfHeights[4])
        Truth.assertThat(listOfHeights[4]).isEqualTo(listOfHeights[5])
        Truth.assertThat(listOfHeights[5]).isNotEqualTo(listOfHeights[6])
        Truth.assertThat(listOfHeights[6]).isEqualTo(listOfHeights[7])
        Truth.assertThat(listOfHeights[7]).isEqualTo(listOfHeights[8])
    }

    @Test
    fun testContextualFlowRow_equalHeight_worksWithWeight() {
        val listOfHeights = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    itemCount = 9,
                    Modifier
                        .fillMaxWidth(1f)
                        .padding(20.dp)
                        .wrapContentHeight(align = Alignment.Top),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    maxItemsInEachRow = 3,
                ) {
                    Box(
                        Modifier
                            .onSizeChanged {
                                listOfHeights.add(it.height)
                            }
                            .width(100.dp)
                            .weight(1f, true)
                            .background(Color.Green)
                            .fillMaxRowHeight()
                    ) {
                        val height = Random.Default.nextInt(1, 200) - it
                        Box(modifier = Modifier.height(height.dp))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(listOfHeights[0]).isEqualTo(listOfHeights[1])
        Truth.assertThat(listOfHeights[1]).isEqualTo(listOfHeights[2])
        Truth.assertThat(listOfHeights[2]).isNotEqualTo(listOfHeights[3])
        Truth.assertThat(listOfHeights[3]).isEqualTo(listOfHeights[4])
        Truth.assertThat(listOfHeights[4]).isEqualTo(listOfHeights[5])
        Truth.assertThat(listOfHeights[5]).isNotEqualTo(listOfHeights[6])
        Truth.assertThat(listOfHeights[6]).isEqualTo(listOfHeights[7])
        Truth.assertThat(listOfHeights[7]).isEqualTo(listOfHeights[8])
    }

    @Test
    fun testContextualFlowRow_equalHeight_WithFraction() {
        val listOfHeights = mutableMapOf<Int, Int>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                with(LocalDensity.current) {
                    ContextualFlowRow(
                        itemCount = 9,
                        Modifier
                            .fillMaxWidth(1f)
                            .padding(20.dp)
                            .wrapContentHeight(align = Alignment.Top, unbounded = true),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        maxItemsInEachRow = 3,
                    ) {
                        Box(
                            Modifier
                                .onSizeChanged { item ->
                                    listOfHeights[it] = item.height
                                }
                                .width(100.dp)
                                .background(Color.Green)
                                .run {
                                    if (it == 0 || it == 3 || it == 6) {
                                        fillMaxRowHeight(0.5f)
                                    } else {
                                        this
                                    }
                                }
                        ) {
                            val height = it * 400
                            Box(modifier = Modifier.height(height.dp))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(listOfHeights.keys).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8)
        Truth.assertThat(listOfHeights[0]).isEqualTo((.5 * listOfHeights[2]!!).roundToInt())
        Truth.assertThat(listOfHeights[1]).isNotEqualTo(listOfHeights[2])
        Truth.assertThat(listOfHeights[2]).isEqualTo(800)
        Truth.assertThat(listOfHeights[2]).isNotEqualTo(listOfHeights[3])
        Truth.assertThat(listOfHeights[3]).isEqualTo((.5 * listOfHeights[5]!!).roundToInt())
        Truth.assertThat(listOfHeights[4]).isNotEqualTo(listOfHeights[5])
        Truth.assertThat(listOfHeights[5]).isEqualTo(2000)
        Truth.assertThat(listOfHeights[5]).isNotEqualTo(listOfHeights[6])
        Truth.assertThat(listOfHeights[6]).isEqualTo((.5 * listOfHeights[8]!!).roundToInt())
        Truth.assertThat(listOfHeights[7]).isNotEqualTo(listOfHeights[8])
        Truth.assertThat(listOfHeights[8]).isEqualTo(3200)
    }

    @Test
    fun testContextualFlowColumn_equalWidth() {
        val listOfWidths = mutableMapOf<Int, Int>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = 9,
                    Modifier
                        .wrapContentWidth(align = Alignment.Start)
                        .padding(20.dp)
                        .fillMaxHeight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachColumn = 3,
                ) {
                    Box(
                        Modifier
                            .onSizeChanged { item ->
                                listOfWidths[it] = item.width
                            }
                            .height(100.dp)
                            .background(Color.Green)
                            .fillMaxColumnWidth()
                    ) {
                        val width = it * 20
                        Box(modifier = Modifier.width(width.dp))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(listOfWidths.keys).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8)
        Truth.assertThat(listOfWidths[0]).isEqualTo(listOfWidths[1])
        Truth.assertThat(listOfWidths[1]).isEqualTo(listOfWidths[2])
        Truth.assertThat(listOfWidths[2]).isNotEqualTo(listOfWidths[3])
        Truth.assertThat(listOfWidths[3]).isEqualTo(listOfWidths[4])
        Truth.assertThat(listOfWidths[4]).isEqualTo(listOfWidths[5])
        Truth.assertThat(listOfWidths[5]).isNotEqualTo(listOfWidths[6])
        Truth.assertThat(listOfWidths[6]).isEqualTo(listOfWidths[7])
        Truth.assertThat(listOfWidths[7]).isEqualTo(listOfWidths[8])
    }

    @Test
    fun testContextualFlowColumn_equalWidth_worksWithWeight() {
        val listOfWidths = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = 9,
                    Modifier
                        .wrapContentWidth(align = Alignment.Start)
                        .fillMaxHeight(1f)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachColumn = 3,
                ) {
                    Box(
                        Modifier
                            .onSizeChanged {
                                listOfWidths.add(it.width)
                            }
                            .height(100.dp)
                            .weight(1f, true)
                            .background(Color.Green)
                            .fillMaxColumnWidth()
                    ) {
                        val width = it * 20
                        Box(modifier = Modifier.width(width.dp))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(listOfWidths[0]).isEqualTo(listOfWidths[1])
        Truth.assertThat(listOfWidths[1]).isEqualTo(listOfWidths[2])
        Truth.assertThat(listOfWidths[2]).isNotEqualTo(listOfWidths[3])
        Truth.assertThat(listOfWidths[3]).isEqualTo(listOfWidths[4])
        Truth.assertThat(listOfWidths[4]).isEqualTo(listOfWidths[5])
        Truth.assertThat(listOfWidths[5]).isNotEqualTo(listOfWidths[6])
        Truth.assertThat(listOfWidths[6]).isEqualTo(listOfWidths[7])
        Truth.assertThat(listOfWidths[7]).isEqualTo(listOfWidths[8])
    }

    @Test
    fun testContextualFlowColumn_equalWidth_fraction() {
        val listOfWidths = mutableMapOf<Int, Int>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = 9,
                    Modifier
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .padding(20.dp)
                        .fillMaxWidth(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachColumn = 3,
                ) { index ->
                    Box(
                        Modifier
                            .onSizeChanged {
                                listOfWidths[index] = it.width
                            }
                            .height(100.dp)
                            .background(Color.Green)
                            .run {
                                if (index == 0 || index == 3 || index == 6) {
                                    fillMaxColumnWidth(0.5f)
                                } else {
                                    this
                                }
                            }
                    ) {
                        val width = index * 400
                        Box(modifier = Modifier.width(width.dp))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(listOfWidths.keys).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8)
        Truth.assertThat(listOfWidths[0]).isEqualTo((.5 * listOfWidths[2]!!).roundToInt())
        Truth.assertThat(listOfWidths[1]).isNotEqualTo(listOfWidths[2])
        Truth.assertThat(listOfWidths[2]).isEqualTo(800)
        Truth.assertThat(listOfWidths[2]).isNotEqualTo(listOfWidths[3])
        Truth.assertThat(listOfWidths[3]).isEqualTo((.5 * listOfWidths[5]!!).roundToInt())
        Truth.assertThat(listOfWidths[4]).isNotEqualTo(listOfWidths[5])
        Truth.assertThat(listOfWidths[5]).isEqualTo(2000)
        Truth.assertThat(listOfWidths[5]).isNotEqualTo(listOfWidths[6])
        Truth.assertThat(listOfWidths[6]).isEqualTo((.5 * listOfWidths[8]!!).roundToInt())
        Truth.assertThat(listOfWidths[7]).isNotEqualTo(listOfWidths[8])
        Truth.assertThat(listOfWidths[8]).isEqualTo(3200)
    }

    @Test
    fun testContextualFlowColumn_staysInOneRow() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    ContextualFlowColumn(
                        itemCount = 2,
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testContextualFlowRow_wrapsToTheNextLine_Rounding() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    ContextualFlowRow(
                        itemCount = 3,
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testContextualFlowColumn_wrapsToTheNextLine_Rounding() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    ContextualFlowColumn(
                        itemCount = 3,
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }
                    ) {
                        Box(Modifier.size(20.toDp()))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testContextualFlowRow_empty() {
        var height = 0
        var width = 0

        rule.setContent {
            Box(Modifier.size(100.dp)) {
                ContextualFlowRow(
                    itemCount = 0,
                    Modifier
                        .onSizeChanged {
                            height = it.height
                            width = it.width
                        }
                ) {}
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(0)
        Truth.assertThat(width).isEqualTo(0)
    }

    @Test
    fun testContextualFlowColumn_empty() {
        var height = 0
        var width = 0

        rule.setContent {
            Box(Modifier.size(100.dp)) {
                ContextualFlowColumn(
                    itemCount = 0,
                    Modifier
                        .onSizeChanged {
                            height = it.height
                            width = it.width
                        }
                ) {}
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(0)
        Truth.assertThat(width).isEqualTo(0)
    }

    @Test
    fun testContextualFlowRow_alignItemsDefaultsToLeft() {
        val shorterHeight = 10
        val expectedResult = 0f
        var positionInParentY = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowRow(itemCount = 5) {
                        Box(
                            Modifier
                                .size(
                                    20.toDp(),
                                    shorterHeight.toDp()
                                )
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    positionInParentY = positionInParent.y
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentY).isEqualTo(expectedResult)
    }

    @Test
    fun testContextualFlowRow_alignItemsCenterVertically() {
        val totalRowHeight = 20
        val shorterHeight = 10
        val expectedResult = (totalRowHeight - shorterHeight) / 2
        var positionInParentY = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowRow(itemCount = 5) { index ->
                        Box(
                            Modifier
                                .align(Alignment.CenterVertically)
                                .size(
                                    20.toDp(),
                                    if (index == 4) {
                                        shorterHeight.toDp()
                                    } else {
                                        totalRowHeight.toDp()
                                    }
                                )
                                .onPlaced {
                                    if (index == 4) {
                                        val positionInParent = it.positionInParent()
                                        positionInParentY = positionInParent.y
                                    }
                                })
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentY).isEqualTo(expectedResult)
    }

    @Test
    fun testContextualFlowColumn_alignItemsDefaultsToTop() {
        val shorterWidth = 10
        val expectedResult = 0f
        var positionInParentX = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowColumn(itemCount = 5) {
                        Box(
                            Modifier
                                .size(
                                    shorterWidth.toDp(),
                                    20.toDp()
                                )
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    positionInParentX = positionInParent.x
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentX).isEqualTo(expectedResult)
    }

    @Test
    fun testContextualFlowColumn_alignItemsCenterHorizontally() {
        val totalColumnWidth = 20
        val shorterWidth = 10
        val expectedResult = (totalColumnWidth - shorterWidth) / 2
        var positionInParentX = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowColumn(itemCount = 5) { index ->
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(
                                    if (index == 4) {
                                        shorterWidth.toDp()
                                    } else {
                                        totalColumnWidth.toDp()
                                    },
                                    20.toDp()
                                )
                                .onPlaced {
                                    if (index == 4) {
                                        val positionInParent = it.positionInParent()
                                        positionInParentX = positionInParent.x
                                    }
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentX).isEqualTo(expectedResult)
    }

    @Test
    fun testContextualFlowRow_horizontalArrangementSpaceAround() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val eachItemSpaceGiven = spaceAvailable / noOfItemsPerRow
        val gapSize = (eachItemSpaceGiven / 2).roundToInt()
        val xPositions = FloatArray(noOfItemsPerRow)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowRow(
                        itemCount = 5,
                        Modifier.fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) { index ->
                        Box(
                            Modifier
                                .size(20.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions[index] = xPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        xPositions.forEach {
            val xPosition = it
            expectedXPosition += gapSize
            Truth.assertThat(xPosition).isEqualTo(expectedXPosition)
            expectedXPosition += eachSize
            expectedXPosition += gapSize
        }
    }

    @Test
    fun testContextualFlowRow_MaxLinesVisible() {
        val itemSize = 50f
        val maxLines = 2
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    itemCount = totalItems,
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowRowOverflow.Visible
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(itemsShownCount).isEqualTo(10)
    }

    @Test
    fun testContextualFlowColumn_MaxLinesVisible() {
        val itemSize = 50f
        val maxLines = 2
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = totalItems,
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowColumnOverflow.Visible
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(itemsShownCount).isEqualTo(10)
    }

    @Test
    fun testContextualFlowRow_MaxHeightVisible() {
        val itemSize = 50f
        val maxHeight = 120
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    itemCount = totalItems,
                    modifier = Modifier
                        .width(200.dp)
                        .height(maxHeight.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow = ContextualFlowRowOverflow.Visible
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(itemsShownCount).isEqualTo(10)
    }

    @Test
    fun testContextualFlowColumn_MaxWidthVisible() {
        val itemSize = 50f
        val maxWidth = 120
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = totalItems,
                    modifier = Modifier
                        .height(200.dp)
                        .width(maxWidth.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow = ContextualFlowColumnOverflow.Visible
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(itemsShownCount).isEqualTo(10)
    }

    @Test
    fun testContextualFlowRow_MaxLinesClipped() {
        val itemSize = 50f
        val maxLines = 2
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    itemCount = totalItems,
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowRowOverflow.Clip
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(itemsShownCount).isEqualTo(6)
    }

    @Test
    fun testContextualFlowColumn_MaxLinesClipped() {
        val itemSize = 50f
        val maxLines = 2
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = totalItems,
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowColumnOverflow.Clip
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(itemsShownCount).isEqualTo(6)
    }

    @Test
    fun testContextualFlowRow_MaxHeightClipped() {
        val itemSize = 50f
        val maxHeight = 120
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    itemCount = totalItems,
                    modifier = Modifier
                        .width(200.dp)
                        .height(maxHeight.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow = ContextualFlowRowOverflow.Clip
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(itemsShownCount).isEqualTo(6)
    }

    @Test
    fun testContextualFlowColumn_MaxWidthClipped() {
        val itemSize = 50f
        val maxWidth = 120
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = totalItems,
                    modifier = Modifier
                        .height(200.dp)
                        .width(maxWidth.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow = ContextualFlowColumnOverflow.Clip
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(itemsShownCount).isEqualTo(6)
    }

    @Test
    fun testContextualFlowRow_MaxLinesSeeMore() {
        val itemSize = 50f
        val totalItems = 15
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = false
        var seeMoreTag = "SeeMoreTag"
        var finalMaxLines = 2

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    itemCount = totalItems,
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowRowOverflow.expandIndicator {
                        seeMoreTag = "seeMoreTag$shownItemCount"
                        Box(
                            modifier = Modifier
                                .clickable {
                                    itemsShownCount = 0
                                    seeMoreShown = false
                                    maxLines += 2
                                    finalMaxLines = maxLines
                                }
                                .size(itemSize.dp)
                                .testTag(seeMoreTag)
                                .onPlaced {
                                    seeMoreShown = true
                                }
                        )
                    }
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
        }
        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(15)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isFalse()
        }
    }

    @Test
    fun testContextualFlowColumn_MaxLinesSeeMore() {
        val itemSize = 50f
        val totalItems = 15
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = false
        var seeMoreTag = "SeeMoreTag"
        var finalMaxLines = 2

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = totalItems,
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowColumnOverflow.expandIndicator {
                        seeMoreTag = "SeeMoreTag$shownItemCount"
                        Box(
                            modifier = Modifier
                                .clickable {
                                    itemsShownCount = 0
                                    seeMoreShown = false
                                    maxLines += 2
                                    finalMaxLines = maxLines
                                }
                                .size(itemSize.dp)
                                .testTag(seeMoreTag)
                                .onPlaced {
                                    seeMoreShown = true
                                }
                        )
                    }
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
        }
        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(15)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isFalse()
        }
    }

    @Test
    fun testContextualFlowRow_MaxHeightSeeMore() {
        val itemSize = 50f
        val totalItems = 15
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = false
        var seeMoreTag = "SeeMoreTag"
        var finalMaxHeight = 120.dp

        rule.setContent {
            var maxHeight by remember { mutableStateOf(120.dp) }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    itemCount = totalItems,
                    modifier = Modifier
                        .width(200.dp)
                        .height(maxHeight),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow = ContextualFlowRowOverflow.expandIndicator {
                        seeMoreTag = "seeMoreTag$shownItemCount"
                        Box(
                            modifier = Modifier
                                .clickable {
                                    itemsShownCount = 0
                                    seeMoreShown = false
                                    maxHeight += 100.dp + (spacing.dp * 2)
                                    finalMaxHeight = maxHeight
                                }
                                .size(itemSize.dp)
                                .testTag(seeMoreTag)
                                .onGloballyPositioned {
                                    seeMoreShown = true
                                }
                        )
                    }
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onGloballyPositioned {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            itemsShownCount = 0
        }
        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(finalMaxHeight).isEqualTo(260.dp)
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(seeMoreShown).isTrue()
            itemsShownCount = 0
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(15)
            Truth.assertThat(finalMaxHeight).isEqualTo(400.dp)
            Truth.assertThat(seeMoreShown).isFalse()
        }
    }

    @Test
    fun testContextualFlowColumn_MaxWidthSeeMore() {
        val itemSize = 50f
        val totalItems = 15
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = false
        var seeMoreTag = "SeeMoreTag"
        var finalMaxWidth = 120.dp

        rule.setContent {
            var maxWidth by remember { mutableStateOf(120.dp) }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = totalItems,
                    modifier = Modifier
                        .height(200.dp)
                        .width(maxWidth),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow = ContextualFlowColumnOverflow.expandIndicator {
                        seeMoreTag = "seeMoreTag$shownItemCount"
                        Box(
                            modifier = Modifier
                                .clickable {
                                    itemsShownCount = 0
                                    seeMoreShown = false
                                    maxWidth += 100.dp + (spacing.dp * 2)
                                    finalMaxWidth = maxWidth
                                }
                                .size(itemSize.dp)
                                .testTag(seeMoreTag)
                                .onGloballyPositioned {
                                    seeMoreShown = true
                                }
                        )
                    }
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onGloballyPositioned {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            itemsShownCount = 0
        }
        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(finalMaxWidth).isEqualTo(260.dp)
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(seeMoreShown).isTrue()
            itemsShownCount = 0
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(15)
            Truth.assertThat(finalMaxWidth).isEqualTo(400.dp)
            Truth.assertThat(seeMoreShown).isFalse()
        }
    }

    @Test
    fun testContextualFlowRow_DoesNotThrowExceptionWhenSeeMoreCalledDuringComposition() {
        val itemSize = 50f
        val totalItems = 18
        val spacing = 20

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    itemCount = totalItems,
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            val remainingItems = totalItems - shownItemCount
                            if (remainingItems > 0) {
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            maxLines += 2
                                        }
                                        .size(itemSize.dp)
                                )
                            }
                        },
                        collapseIndicator = {
                            val remainingItems = totalItems - shownItemCount
                            if (remainingItems > 0) {
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            maxLines += 2
                                        }
                                        .size(itemSize.dp)
                                )
                            }
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                    )
                }
            }
        }
    }

    @Test
    fun testContextualFlowColumn_DoesNotThrowExceptionWhenSeeMoreCalledDuringComposition() {
        val itemSize = 50f
        val totalItems = 18
        val spacing = 20

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = totalItems,
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowColumnOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            val remainingItems = totalItems - shownItemCount
                            if (remainingItems > 0) {
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            maxLines += 2
                                        }
                                        .size(itemSize.dp)
                                )
                            }
                        },
                        collapseIndicator = {
                            val remainingItems = totalItems - shownItemCount
                            if (remainingItems > 0) {
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            maxLines += 2
                                        }
                                        .size(itemSize.dp)
                                )
                            }
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                    )
                }
            }
        }
    }

    @Test
    fun testContextualFlowRow_MaxLinesexpandOrCollapseIndicator() {
        val itemSize = 50f
        val totalItems = 18
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = true
        var collapseShown = false
        var seeMoreTag = "SeeMoreTag"
        var collapseTag = "CollapseTag"
        var finalMaxLines = 2
        lateinit var expandOnScope: ContextualFlowRowOverflowScope
        lateinit var collapseOnScope: ContextualFlowRowOverflowScope

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    itemCount = totalItems,
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            expandOnScope = this
                            seeMoreTag = "seeMoreTag$shownItemCount"
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        itemsShownCount = 0
                                        seeMoreShown = false
                                        collapseShown = false
                                        maxLines += 2
                                        finalMaxLines = maxLines
                                    }
                                    .size(itemSize.dp)
                                    .testTag(seeMoreTag)
                                    .onGloballyPositioned {
                                        seeMoreShown = true
                                    }
                                    .onPlaced {
                                        seeMoreShown = true
                                    }
                            )
                        },
                        collapseIndicator = {
                            collapseOnScope = this
                            collapseTag = "collapseTag$shownItemCount"
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        itemsShownCount = 0
                                        seeMoreShown = false
                                        collapseShown = false
                                        maxLines = 2
                                        finalMaxLines = maxLines
                                    }
                                    .size(itemSize.dp)
                                    .testTag(collapseTag)
                                    .onGloballyPositioned {
                                        collapseShown = true
                                    }
                                    .onPlaced {
                                        collapseShown = true
                                    }
                            )
                        }
                    )
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(expandOnScope.shownItemCount).isEqualTo(collapseOnScope.shownItemCount)
            Truth.assertThat(expandOnScope.shownItemCount).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(expandOnScope.shownItemCount).isEqualTo(collapseOnScope.shownItemCount)
            Truth.assertThat(expandOnScope.shownItemCount).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(17)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(expandOnScope.shownItemCount).isEqualTo(collapseOnScope.shownItemCount)
            Truth.assertThat(expandOnScope.shownItemCount).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(18)
            Truth.assertThat(finalMaxLines).isEqualTo(8)
            Truth.assertThat(collapseShown).isTrue()
            Truth.assertThat(seeMoreShown).isFalse()
            Truth.assertThat(expandOnScope.shownItemCount).isEqualTo(collapseOnScope.shownItemCount)
            Truth.assertThat(expandOnScope.shownItemCount).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(collapseTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(finalMaxLines).isEqualTo(2)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(expandOnScope.shownItemCount).isEqualTo(collapseOnScope.shownItemCount)
            Truth.assertThat(expandOnScope.shownItemCount).isEqualTo(5)
        }
    }

    @Test
    fun testContextualFlowColumn_MaxLinesexpandOrCollapseIndicator() {
        val itemSize = 50f
        val totalItems = 18
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = true
        var collapseShown = false
        var seeMoreTag = "SeeMoreTag"
        var collapseTag = "CollapseTag"
        var finalMaxLines = 2
        var itemsShownOnExpand = 0
        var itemsShownOnCollapse = 0

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    itemCount = totalItems,
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowColumnOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            itemsShownOnExpand = shownItemCount
                            seeMoreTag = "seeMoreTag$shownItemCount"
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        itemsShownCount = 0
                                        seeMoreShown = false
                                        collapseShown = false
                                        maxLines += 2
                                        finalMaxLines = maxLines
                                    }
                                    .size(itemSize.dp)
                                    .testTag(seeMoreTag)
                                    .onGloballyPositioned {
                                        seeMoreShown = true
                                    }
                                    .onPlaced {
                                        seeMoreShown = true
                                    }
                            )
                        },
                        collapseIndicator = {
                            itemsShownOnCollapse = shownItemCount
                            collapseTag = "collapseTag$shownItemCount"
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        itemsShownCount = 0
                                        seeMoreShown = false
                                        collapseShown = false
                                        maxLines = 2
                                        finalMaxLines = maxLines
                                    }
                                    .size(itemSize.dp)
                                    .testTag(collapseTag)
                                    .onGloballyPositioned {
                                        collapseShown = true
                                    }
                                    .onPlaced {
                                        collapseShown = true
                                    }
                            )
                        }
                    )
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnCollapse).isEqualTo(0)
            Truth.assertThat(itemsShownOnExpand).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(17)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(18)
            Truth.assertThat(finalMaxLines).isEqualTo(8)
            Truth.assertThat(collapseShown).isTrue()
            Truth.assertThat(seeMoreShown).isFalse()
            Truth.assertThat(itemsShownOnCollapse).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(collapseTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(finalMaxLines).isEqualTo(2)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(5)
        }
    }

    @Test
    fun testContextualFlowRow_MaxLines_DifferentCollapseSize() {
        val itemSize = 50f
        val collapseSize = 100f
        val totalItems = 18
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = true
        var collapseShown = false
        var seeMoreTag = "SeeMoreTag"
        var collapseTag = "CollapseTag"

        var finalMaxLines = 2
        var itemsShownOnExpand = 0
        var itemsShownOnCollapse = 0

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowRow(
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            seeMoreTag = "seeMoreTag$shownItemCount"
                            itemsShownOnExpand = shownItemCount
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        itemsShownCount = 0
                                        seeMoreShown = false
                                        collapseShown = false
                                        maxLines += 2
                                        finalMaxLines = maxLines
                                    }
                                    .size(itemSize.dp)
                                    .testTag(seeMoreTag)
                                    .onPlaced {
                                        seeMoreShown = true
                                    }
                            )
                        },
                        collapseIndicator = {
                            collapseTag = "collapseTag$shownItemCount"
                            itemsShownOnCollapse = shownItemCount
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        itemsShownCount = 0
                                        seeMoreShown = false
                                        collapseShown = false
                                        maxLines = 2
                                        finalMaxLines = maxLines
                                    }
                                    .size(collapseSize.dp)
                                    .testTag(collapseTag)
                                    .onPlaced {
                                        collapseShown = true
                                    }
                            )
                        }),
                    itemCount = totalItems
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(17)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(18)
            Truth.assertThat(finalMaxLines).isEqualTo(8)
            Truth.assertThat(collapseShown).isTrue()
            Truth.assertThat(seeMoreShown).isFalse()
            Truth.assertThat(itemsShownOnCollapse).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(collapseTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(finalMaxLines).isEqualTo(2)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(5)
        }
    }

    @Test
    fun testContextualFlowColumn_MaxLines_DifferentCollapseSize() {
        val itemSize = 50f
        val collapseSize = 100f
        val totalItems = 18
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = true
        var collapseShown = false
        var seeMoreTag = "SeeMoreTag"
        var collapseTag = "CollapseTag"
        var finalMaxLines = 2
        var itemsShownOnExpand = 0
        var itemsShownOnCollapse = 0

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                ContextualFlowColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = ContextualFlowColumnOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            itemsShownOnExpand = shownItemCount
                            seeMoreTag = "seeMoreTag$shownItemCount"
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        itemsShownCount = 0
                                        seeMoreShown = false
                                        collapseShown = false
                                        maxLines += 2
                                        finalMaxLines = maxLines
                                    }
                                    .size(itemSize.dp)
                                    .testTag(seeMoreTag)
                                    .onPlaced {
                                        seeMoreShown = true
                                    }
                            )
                        },
                        collapseIndicator = {
                            itemsShownOnCollapse = shownItemCount
                            collapseTag = "collapseTag$shownItemCount"
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        itemsShownCount = 0
                                        seeMoreShown = false
                                        collapseShown = false
                                        maxLines = 2
                                        finalMaxLines = maxLines
                                    }
                                    .size(collapseSize.dp)
                                    .testTag(collapseTag)
                                    .onPlaced {
                                        collapseShown = true
                                    }
                            )
                        }
                    ),
                    itemCount = totalItems
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(itemSize.dp)
                            .onPlaced {
                                itemsShownCount = index + 1
                            }
                    )
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(17)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag)
            .performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(18)
            Truth.assertThat(finalMaxLines).isEqualTo(8)
            Truth.assertThat(collapseShown).isTrue()
            Truth.assertThat(seeMoreShown).isFalse()
            Truth.assertThat(itemsShownOnCollapse).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(collapseTag)
            .performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(finalMaxLines).isEqualTo(2)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(itemsShownOnExpand).isEqualTo(5)
        }
    }

    private fun advanceClock() {
        rule.mainClock.advanceTimeBy(100_000L)
    }

    @Test
    fun testContextualFlowColumn_verticalArrangementSpaceAround() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val eachItemSpaceGiven = spaceAvailable / noOfItemsPerRow
        val gapSize = (eachItemSpaceGiven / 2).roundToInt()

        val yPositions = FloatArray(noOfItemsPerRow)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowColumn(
                        modifier = Modifier
                            .fillMaxHeight(1f),
                        verticalArrangement = Arrangement.SpaceAround,
                        itemCount = 5
                    ) { index ->
                        Box(
                            Modifier
                                .size(20.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val yPosition = positionInParent.y
                                    yPositions[index] = yPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = 0
        yPositions.forEach {
            val yPosition = it
            expectedYPosition += gapSize
            Truth
                .assertThat(yPosition)
                .isEqualTo(expectedYPosition)
            expectedYPosition += eachSize
            expectedYPosition += gapSize
        }
    }

    @Test
    fun testContextualFlowRow_horizontalArrangementSpaceAround_withTwoRows() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val eachItemSpaceGiven = spaceAvailable / noOfItemsPerRow
        val gapSize = (eachItemSpaceGiven / 2).roundToInt()
        //  ----
        //      * Visually: #1##2##3# for LTR and #3##2##1# for RTL
        // --(front) - (back) --

        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        maxItemsInEachRow = 5,
                        itemCount = 10
                    ) { index ->
                        Box(
                            Modifier
                                .size(20.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions[index] = xPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        xPositions.forEachIndexed { index, xPosition ->
            if (index % 5 == 0) {
                expectedXPosition = 0
            }
            expectedXPosition += gapSize
            Truth
                .assertThat(xPosition)
                .isEqualTo(expectedXPosition)
            expectedXPosition += eachSize
            expectedXPosition += gapSize
        }
    }

    @Test
    fun testContextualFlowColumn_verticalArrangementSpaceAround_withTwoColumns() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val eachItemSpaceGiven = spaceAvailable / noOfItemsPerRow
        val gapSize = (eachItemSpaceGiven / 2).roundToInt()

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowColumn(
                        modifier = Modifier
                            .fillMaxHeight(1f),
                        verticalArrangement = Arrangement.SpaceAround,
                        maxItemsInEachColumn = 5,
                        itemCount = 10
                    ) { index ->
                        Box(
                            Modifier
                                .size(20.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val yPosition = positionInParent.y
                                    yPositions[index] = yPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = 0
        yPositions.forEachIndexed { index, position ->
            if (index % 5 == 0) {
                expectedYPosition = 0
            }
            expectedYPosition += gapSize
            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
            expectedYPosition += eachSize
            expectedYPosition += gapSize
        }
    }

    @Test
    fun testContextualFlowRow_horizontalArrangementEnd() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val gapSize = spaceAvailable.roundToInt()
        //  * Visually: ####123

        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.End,
                        maxItemsInEachRow = 5,
                        itemCount = 10
                    ) { index ->
                        Box(
                            Modifier
                                .size(20.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions[index] = xPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = gapSize
        xPositions.forEachIndexed { index, position ->
            if (index % 5 == 0) {
                expectedXPosition = gapSize
            }
            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
            expectedXPosition += eachSize
        }
    }

    @Test
    fun testContextualFlowColumn_verticalArrangementBottom() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val gapSize = spaceAvailable.roundToInt()

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowColumn(
                        modifier = Modifier
                            .fillMaxHeight(1f),
                        verticalArrangement = Arrangement.Bottom,
                        maxItemsInEachColumn = 5,
                        itemCount = 10
                    ) { index ->
                        Box(
                            Modifier
                                .size(20.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val yPosition = positionInParent.y
                                    yPositions[index] = yPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = gapSize
        yPositions.forEachIndexed { index, position ->
            if (index % 5 == 0) {
                expectedYPosition = gapSize
            }
            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
            expectedYPosition += eachSize
        }
    }

    @Test
    fun testContextualFlowRow_horizontalArrangementStart() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        //  * Visually: 123####

        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowRow(
                        horizontalArrangement = Arrangement.Start,
                        maxItemsInEachRow = maxItemsInMainAxis,
                        itemCount = 10
                    ) { index ->
                        Box(
                            Modifier
                                .size(eachSize.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions[index] = xPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        xPositions.forEachIndexed { index, position ->
            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
            if (index == (maxItemsInMainAxis - 1)) {
                expectedXPosition = 0
            } else {
                expectedXPosition += eachSize
            }
        }
    }

    @Test
    fun testContextualFlowRow_horizontalArrangementStart_MaxLines() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxLinesState = mutableStateOf(2)
        val minLinesToShowCollapseState = mutableStateOf(1)
        val minHeightToShowCollapseState = mutableStateOf(0.dp)
        val total = 20
        //  * Visually: 123####

        val xPositions = mutableListOf<Float>()
        var overflowState = mutableStateOf(ContextualFlowRowOverflow.Clip)
        var seeMoreOrCollapse: ContextualFlowRowOverflow? = null
        var seeMoreXPosition: Float? = null
        var collapseXPosition: Float? = null
        rule.setContent {
            var overflow by remember { overflowState }
            var maxLines by remember { maxLinesState }
            var minLinesToShowCollapse by remember { minLinesToShowCollapseState }
            var minHeightToShowCollapse by remember { minHeightToShowCollapseState }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                seeMoreOrCollapse = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                    expandIndicator = {
                        Box(
                            Modifier
                                .size(20.dp)
                                .onGloballyPositioned {
                                    seeMoreXPosition = it.positionInParent().x
                                })
                    },
                    collapseIndicator = {
                        Box(
                            Modifier
                                .size(20.dp)
                                .onGloballyPositioned {
                                    collapseXPosition = it.positionInParent().x
                                })
                    },
                    minLinesToShowCollapse,
                    minHeightToShowCollapse
                )
                Box(Modifier.size(200.dp)) {
                    ContextualFlowRow(
                        horizontalArrangement = Arrangement.Start,
                        maxItemsInEachRow = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                        itemCount = total
                    ) {
                        Box(
                            Modifier
                                .size(eachSize.dp)
                                .onGloballyPositioned {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions.add(xPosition)
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(xPositions.size).isEqualTo(
                maxItemsInMainAxis * maxLinesState.value
            )
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth
                    .assertThat(position)
                    .isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            xPositions.clear()
            overflowState.value = ContextualFlowRowOverflow.expandIndicator {
                Box(
                    Modifier
                        .size(20.dp)
                        .onGloballyPositioned {
                            val positionInParent = it.positionInParent()
                            seeMoreXPosition = positionInParent.x
                        })
            }
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = min(
                (maxItemsInMainAxis * maxLinesState.value) - 1, total
            )
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth
                    .assertThat(position)
                    .isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            Truth.assertThat(seeMoreXPosition).isEqualTo(expectedXPosition)
            Truth.assertThat(collapseXPosition).isEqualTo(null)
            xPositions.clear()
            seeMoreXPosition = null
            maxLinesState.value = 4
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = min(
                (maxItemsInMainAxis * maxLinesState.value) - 1, total
            )
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth
                    .assertThat(position)
                    .isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            Truth.assertThat(seeMoreXPosition).isEqualTo(expectedXPosition)
            Truth.assertThat(collapseXPosition).isEqualTo(null)
            xPositions.clear()
            seeMoreXPosition = null
            maxLinesState.value = 5
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = min(
                (maxItemsInMainAxis * maxLinesState.value) - 1, total
            )
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth
                    .assertThat(position)
                    .isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            Truth.assertThat(collapseXPosition).isEqualTo(expectedXPosition)
            Truth.assertThat(seeMoreXPosition).isEqualTo(null)
            xPositions.clear()
            collapseXPosition = null
            minLinesToShowCollapseState.value = maxLinesState.value + 1
        }
        advanceClock()
        rule.runOnIdle {
            xPositions.clear()
            collapseXPosition = null
            seeMoreXPosition = null
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = min(
                (maxItemsInMainAxis * maxLinesState.value), total
            )
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth
                    .assertThat(position)
                    .isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            Truth.assertThat(collapseXPosition).isEqualTo(null)
            Truth.assertThat(seeMoreXPosition).isEqualTo(null)
        }
    }

    @Test
    fun testContextualFlowColumn_verticalArrangementStart_MaxLines() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxLinesState = mutableStateOf(2)
        val minLinesToShowCollapseState = mutableStateOf(1)
        val minHeightToShowCollapseState = mutableStateOf(0.dp)
        val total = 20
        //  * Visually: 123####

        val yPositions = mutableListOf<Float>()
        var overflowState = mutableStateOf(ContextualFlowColumnOverflow.Clip)
        var seeMoreOrCollapse: ContextualFlowColumnOverflow? = null
        var seeMoreYPosition: Float? = null
        var collapseYPosition: Float? = null
        rule.setContent {
            var overflow by remember { overflowState }
            var maxLines by remember { maxLinesState }
            var minLinesToShowCollapse by remember { minLinesToShowCollapseState }
            var minHeightToShowCollapse by remember { minHeightToShowCollapseState }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                seeMoreOrCollapse = ContextualFlowColumnOverflow.expandOrCollapseIndicator(
                    expandIndicator = {
                        Box(
                            Modifier
                                .size(20.dp)
                                .onGloballyPositioned {
                                    seeMoreYPosition = it.positionInParent().y
                                })
                    },
                    collapseIndicator = {
                        Box(
                            Modifier
                                .size(20.dp)
                                .onGloballyPositioned {
                                    collapseYPosition = it.positionInParent().y
                                })
                    },
                    minLinesToShowCollapse,
                    minHeightToShowCollapse
                )
                Box(Modifier.size(200.dp)) {
                    ContextualFlowColumn(
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                        itemCount = total
                    ) {
                        Box(
                            Modifier
                                .size(eachSize.dp)
                                .onGloballyPositioned {
                                    val positionInParent = it.positionInParent()
                                    val yPosition = positionInParent.y
                                    yPositions.add(yPosition)
                                }
                        )
                    }
                }
            }
        }

        // Assertions and interaction logic
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(yPositions.size).isEqualTo(
                maxItemsInMainAxis * maxLinesState.value
            )
            var expectedYPosition = 0
            yPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedYPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedYPosition = 0
                } else {
                    expectedYPosition += eachSize
                }
            }
            yPositions.clear()
            overflowState.value = ContextualFlowColumnOverflow.expandIndicator {
                Box(
                    Modifier
                        .size(20.dp)
                        .onGloballyPositioned {
                            val positionInParent = it.positionInParent()
                            seeMoreYPosition = positionInParent.y
                        }
                )
            }
        }
        // Continuing from the previous logic
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = min(
                (maxItemsInMainAxis * maxLinesState.value) - 1, total
            )
            Truth.assertThat(yPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedYPosition = 0
            yPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedYPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedYPosition = 0
                } else {
                    expectedYPosition += eachSize
                }
            }
            Truth.assertThat(seeMoreYPosition).isEqualTo(expectedYPosition)
            Truth.assertThat(collapseYPosition).isEqualTo(null)
            yPositions.clear()
            seeMoreYPosition = null
            maxLinesState.value = 4
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = min(
                (maxItemsInMainAxis * maxLinesState.value) - 1, total
            )
            Truth.assertThat(yPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedYPosition = 0
            yPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedYPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedYPosition = 0
                } else {
                    expectedYPosition += eachSize
                }
            }
            Truth.assertThat(seeMoreYPosition).isEqualTo(expectedYPosition)
            Truth.assertThat(collapseYPosition).isEqualTo(null)
            yPositions.clear()
            seeMoreYPosition = null
            maxLinesState.value = 5
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = min(
                (maxItemsInMainAxis * maxLinesState.value) - 1, total
            )
            Truth.assertThat(yPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedYPosition = 0
            yPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedYPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedYPosition = 0
                } else {
                    expectedYPosition += eachSize
                }
            }
            Truth.assertThat(collapseYPosition).isEqualTo(expectedYPosition)
            Truth.assertThat(seeMoreYPosition).isEqualTo(null)
            yPositions.clear()
            collapseYPosition = null
            minLinesToShowCollapseState.value = maxLinesState.value + 1
        }
        advanceClock()
        rule.runOnIdle {
            yPositions.clear()
            collapseYPosition = null
            seeMoreYPosition = null
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = min(
                (maxItemsInMainAxis * maxLinesState.value), total
            )
            Truth.assertThat(yPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedYPosition = 0
            yPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedYPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedYPosition = 0
                } else {
                    expectedYPosition += eachSize
                }
            }
            Truth.assertThat(collapseYPosition).isEqualTo(null)
            Truth.assertThat(seeMoreYPosition).isEqualTo(null)
        }
    }

    @Test
    fun testContextualFlowRow_horizontalArrangementStart_MaxHeight() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxHeightState = mutableStateOf(40.dp)
        val minLinesToShowCollapseState = mutableStateOf(1)
        val minHeightToShowCollapseState = mutableStateOf(0.dp)
        val total = 20
        //  * Visually: 123####

        val xPositions = mutableListOf<Float>()
        var overflowState = mutableStateOf(ContextualFlowRowOverflow.Clip)
        var seeMoreOrCollapse: ContextualFlowRowOverflow? = null
        var seeMoreXPosition: Float? = null
        var collapseXPosition: Float? = null
        rule.setContent {
            var overflow by remember { overflowState }
            var maxHeight by remember { maxHeightState }
            var minLinesToShowCollapse by remember { minLinesToShowCollapseState }
            var minHeightToShowCollapse by remember { minHeightToShowCollapseState }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                seeMoreOrCollapse = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                    expandIndicator = {
                        Box(
                            Modifier
                                .size(20.dp)
                                .onGloballyPositioned {
                                    seeMoreXPosition = it.positionInParent().x
                                })
                    },
                    collapseIndicator = {
                        Box(
                            Modifier
                                .size(20.dp)
                                .onGloballyPositioned {
                                    collapseXPosition = it.positionInParent().x
                                })
                    },
                    minLinesToShowCollapse,
                    minHeightToShowCollapse
                )
                Box(
                    Modifier
                        .width(200.dp)
                        .height(maxHeight)
                ) {
                    ContextualFlowRow(
                        horizontalArrangement = Arrangement.Start,
                        maxItemsInEachRow = maxItemsInMainAxis,
                        overflow = overflow,
                        itemCount = total
                    ) {
                        Box(
                            Modifier
                                .size(eachSize.dp)
                                .onGloballyPositioned {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions.add(xPosition)
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(xPositions.size).isEqualTo(10)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth
                    .assertThat(position)
                    .isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            xPositions.clear()
            overflowState.value = ContextualFlowRowOverflow.expandIndicator {
                Box(
                    Modifier
                        .size(20.dp)
                        .onGloballyPositioned {
                            val positionInParent = it.positionInParent()
                            seeMoreXPosition = positionInParent.x
                        })
            }
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = 9
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth
                    .assertThat(position)
                    .isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            Truth.assertThat(seeMoreXPosition).isEqualTo(expectedXPosition)
            Truth.assertThat(collapseXPosition).isEqualTo(null)
            xPositions.clear()
            seeMoreXPosition = null
            maxHeightState.value = 80.dp
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = 19
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth
                    .assertThat(position)
                    .isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            Truth.assertThat(seeMoreXPosition).isEqualTo(expectedXPosition)
            Truth.assertThat(collapseXPosition).isEqualTo(null)
            xPositions.clear()
            seeMoreXPosition = null
            maxHeightState.value = 220.dp
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = 20
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth
                    .assertThat(position)
                    .isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            Truth.assertThat(collapseXPosition).isEqualTo(expectedXPosition)
            Truth.assertThat(seeMoreXPosition).isEqualTo(null)
            xPositions.clear()
            collapseXPosition = null
            minLinesToShowCollapseState.value = 5
        }
        advanceClock()
        rule.runOnIdle {
            xPositions.clear()
            collapseXPosition = null
            seeMoreXPosition = null
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = 20
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth
                    .assertThat(position)
                    .isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            Truth.assertThat(collapseXPosition).isEqualTo(null)
            Truth.assertThat(seeMoreXPosition).isEqualTo(null)
        }
    }

    @Test
    fun testContextualFlowRow_SpaceAligned() {
        val eachSize = 10
        val maxItemsInMainAxis = 5
        val spaceAligned = 10

        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachRow = maxItemsInMainAxis,
                        itemCount = 10
                    ) { index ->
                        Box(
                            Modifier
                                .size(eachSize.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions[index] = xPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        xPositions.forEachIndexed { index, position ->
            if (index % maxItemsInMainAxis == 0) {
                expectedXPosition = 0
            } else {
                expectedXPosition += eachSize
                expectedXPosition += spaceAligned
            }

            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
        }
    }

    @Test
    fun testContextualFlowColumn_verticalArrangementStart_MaxWidth() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxWidthState = mutableStateOf(40.dp)
        val minLinesToShowCollapseState = mutableStateOf(1)
        val minHeightToShowCollapseState = mutableStateOf(0.dp)
        val total = 20
        //  * Visually: 123####

        val yPositions = mutableListOf<Float>()
        var overflowState = mutableStateOf(ContextualFlowColumnOverflow.Clip)
        var seeMoreOrCollapse: ContextualFlowColumnOverflow? = null
        var seeMoreYPosition: Float? = null
        var collapseYPosition: Float? = null
        rule.setContent {
            var overflow by remember { overflowState }
            var maxWidth by remember { maxWidthState }
            var minLinesToShowCollapse by remember { minLinesToShowCollapseState }
            var minHeightToShowCollapse by remember { minHeightToShowCollapseState }
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                seeMoreOrCollapse = ContextualFlowColumnOverflow.expandOrCollapseIndicator(
                    expandIndicator = {
                        Box(
                            Modifier
                                .size(20.dp)
                                .onGloballyPositioned {
                                    seeMoreYPosition = it.positionInParent().y
                                })
                    },
                    collapseIndicator = {
                        Box(
                            Modifier
                                .size(20.dp)
                                .onGloballyPositioned {
                                    collapseYPosition = it.positionInParent().y
                                })
                    },
                    minLinesToShowCollapse,
                    minHeightToShowCollapse
                )
                Box(
                    Modifier
                        .height(200.dp)
                        .width(maxWidth)
                ) {
                    ContextualFlowColumn(
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        overflow = overflow,
                        itemCount = total
                    ) {
                        Box(
                            Modifier
                                .size(eachSize.dp)
                                .onGloballyPositioned {
                                    val positionInParent = it.positionInParent()
                                    val yPosition = positionInParent.y
                                    yPositions.add(yPosition)
                                }
                        )
                    }
                }
            }
        }

        // Continuing from the previous logic
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(yPositions.size).isEqualTo(10)
            var expectedYPosition = 0
            yPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedYPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedYPosition = 0
                } else {
                    expectedYPosition += eachSize
                }
            }
            yPositions.clear()
            overflowState.value = ContextualFlowColumnOverflow.expandIndicator {
                Box(
                    Modifier
                        .size(20.dp)
                        .onGloballyPositioned {
                            val positionInParent = it.positionInParent()
                            seeMoreYPosition = positionInParent.y
                        })
            }
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = 9
            Truth.assertThat(yPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedYPosition = 0
            yPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedYPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedYPosition = 0
                } else {
                    expectedYPosition += eachSize
                }
            }
            Truth.assertThat(seeMoreYPosition).isEqualTo(expectedYPosition)
            Truth.assertThat(collapseYPosition).isEqualTo(null)
            yPositions.clear()
            seeMoreYPosition = null
            maxWidthState.value = 80.dp
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = 19
            Truth.assertThat(yPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedYPosition = 0
            yPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedYPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedYPosition = 0
                } else {
                    expectedYPosition += eachSize
                }
            }
            Truth.assertThat(seeMoreYPosition).isEqualTo(expectedYPosition)
            Truth.assertThat(collapseYPosition).isEqualTo(null)
            yPositions.clear()
            seeMoreYPosition = null
            maxWidthState.value = 220.dp
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = 20
            Truth.assertThat(yPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedYPosition = 0
            yPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedYPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedYPosition = 0
                } else {
                    expectedYPosition += eachSize
                }
            }
            Truth.assertThat(collapseYPosition).isEqualTo(expectedYPosition)
            Truth.assertThat(seeMoreYPosition).isEqualTo(null)
            yPositions.clear()
            collapseYPosition = null
            minLinesToShowCollapseState.value = 5
        }
        advanceClock()
        rule.runOnIdle {
            yPositions.clear()
            collapseYPosition = null
            seeMoreYPosition = null
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = 20
            Truth.assertThat(yPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedYPosition = 0
            yPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedYPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedYPosition = 0
                } else {
                    expectedYPosition += eachSize
                }
            }
            Truth.assertThat(collapseYPosition).isEqualTo(null)
            Truth.assertThat(seeMoreYPosition).isEqualTo(null)
        }
    }

    /**
     * Should space something like this:
     * 1 2 3
     * # SpaceAligned
     * 4 5 6
     * No Space here
     */
    @Test
    fun testContextualFlowRow_crossAxisSpacedBy() {
        val eachSize = 20
        val spaceAligned = 20
        val noOfItems = 3
        val expectedHeight = 100
        var heightResult = 0

        val yPositions = FloatArray(noOfItems)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .onSizeChanged {
                                heightResult = it.height
                            },
                        verticalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachRow = 1,
                        itemCount = noOfItems
                    ) { index ->
                        Box(
                            Modifier
                                .size(eachSize.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val yPosition = positionInParent.y
                                    yPositions[index] = yPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth
            .assertThat(heightResult)
            .isEqualTo(expectedHeight)
        var expectedYPosition = 0
        yPositions.forEachIndexed { index, position ->
            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
            expectedYPosition += eachSize
            if (index < (noOfItems - 1)) {
                expectedYPosition += spaceAligned
            }
        }
    }

    @Test
    fun testContextualFlowColumn_crossAxisSpacedBy() {
        val eachSize = 20
        val spaceAligned = 20
        val noOfItems = 3
        val expectedWidth = 100
        var widthResult = 0

        val xPositions = FloatArray(noOfItems)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowColumn(
                        modifier = Modifier
                            .onSizeChanged {
                                widthResult = it.width
                            },
                        horizontalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachColumn = 1,
                        itemCount = noOfItems
                    ) { index ->
                        Box(
                            Modifier
                                .size(eachSize.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions[index] = xPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        Truth
            .assertThat(widthResult)
            .isEqualTo(expectedWidth)
        xPositions.forEachIndexed { index, position ->
            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
            expectedXPosition += eachSize
            if (index < (noOfItems - 1)) {
                expectedXPosition += spaceAligned
            }
        }
    }

    @Test
    fun testContextualFlowColumn_SpaceAligned() {
        val eachSize = 10
        val maxItemsInMainAxis = 5
        val spaceAligned = 10

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    ContextualFlowColumn(
                        verticalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        itemCount = 10
                    ) { index ->
                        Box(
                            Modifier
                                .size(eachSize.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val position = positionInParent.y
                                    yPositions[index] = position
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = 0
        yPositions.forEachIndexed { index, position ->
            if (index % maxItemsInMainAxis == 0) {
                expectedYPosition = 0
            } else {
                expectedYPosition += eachSize
                expectedYPosition += spaceAligned
            }

            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
        }
    }

    @Test
    fun testContextualFlowRow_SpaceAligned_notExact() {
        val eachSize = 10
        val maxItemsInMainAxis = 5
        val spaceAligned = 10
        val noOfItemsThatCanFit = 2

        var width = 0
        val expectedWidth = 30
        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .widthIn(30.toDp(), 40.toDp())
                ) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .onSizeChanged {
                                width = it.width
                            },
                        horizontalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachRow = maxItemsInMainAxis,
                        itemCount = 10
                    ) { index ->
                        Box(
                            Modifier
                                .size(eachSize.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions[index] = xPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(expectedWidth)
        var expectedXPosition = 0
        xPositions.forEachIndexed { index, position ->
            if (index % noOfItemsThatCanFit == 0) {
                expectedXPosition = 0
            } else {
                expectedXPosition += eachSize
                expectedXPosition += spaceAligned
            }

            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
        }
    }

    @Test
    fun testContextualFlowColumn_SpaceAligned_notExact() {
        val eachSize = 10
        val maxItemsInMainAxis = 5
        val spaceAligned = 10
        val noOfItemsThatCanFit = 2

        var height = 0
        val expectedHeight = 30
        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .heightIn(30.toDp(), 40.toDp())

                ) {
                    ContextualFlowColumn(
                        modifier = Modifier
                            .onSizeChanged {
                                height = it.height
                            },
                        verticalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        itemCount = 10
                    ) { index ->
                        Box(
                            Modifier
                                .size(eachSize.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val yPosition = positionInParent.y
                                    yPositions[index] = yPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(expectedHeight)
        var expectedYPosition = 0
        yPositions.forEachIndexed { index, position ->
            if (index % noOfItemsThatCanFit == 0) {
                expectedYPosition = 0
            } else {
                expectedYPosition += eachSize
                expectedYPosition += spaceAligned
            }

            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
        }
    }

    @Test
    fun testContextualFlowColumn_verticalArrangementTop() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(size.toDp())) {
                    ContextualFlowColumn(
                        modifier = Modifier
                            .fillMaxHeight(1f),
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        itemCount = 10
                    ) { index ->
                        Box(
                            Modifier
                                .size(20.toDp())
                                .onPlaced {
                                    val positionInParent = it.positionInParent()
                                    val yPosition = positionInParent.y
                                    yPositions[index] = yPosition
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = 0
        yPositions.forEachIndexed { index, position ->
            if (index % 5 == 0) {
                expectedYPosition = 0
            }
            Truth
                .assertThat(position)
                .isEqualTo(expectedYPosition)
            expectedYPosition += eachSize
        }
    }

    @Test
    fun testContextualFlowRow_horizontalArrangementStart_rtl_fillMaxWidth() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5
        //  * Visually:
        //  #54321
        //  ####6

        val xPositions = FloatArray(6)
        rule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(size.toDp())) {
                        ContextualFlowRow(
                            modifier = Modifier
                                .fillMaxWidth(1f),
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = maxItemsInMainAxis,
                            itemCount = 6
                        ) { index ->
                            Box(
                                Modifier
                                    .size(eachSize.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val xPosition = positionInParent.x
                                        xPositions[index] = xPosition
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = size.toInt() - eachSize
        xPositions.forEachIndexed { index, position ->
            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
            if (index == (maxItemsInMainAxis - 1)) {
                expectedXPosition = size.toInt() - eachSize
            } else {
                expectedXPosition -= eachSize
            }
        }
    }

    @Test
    fun testContextualFlowColumn_verticalArrangementTop_rtl_fillMaxWidth() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5

        val xYPositions = Array(10) { Pair(0f, 0f) }
        rule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(size.toDp())) {
                        ContextualFlowColumn(
                            modifier = Modifier
                                .fillMaxHeight(1f)
                                .fillMaxWidth(1f),
                            verticalArrangement = Arrangement.Top,
                            maxItemsInEachColumn = maxItemsInMainAxis,
                            itemCount = 10
                        ) { index ->
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val yPosition = positionInParent.y
                                        val xPosition = positionInParent.x
                                        xYPositions[index] = Pair(xPosition, yPosition)
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()

        var expectedYPosition = 0
        var expectedXPosition = size.toInt() - eachSize
        for (index in xYPositions.indices) {
            val xPosition = xYPositions[index].first
            val yPosition = xYPositions[index].second
            if (index % 5 == 0) {
                expectedYPosition = 0
            }
            Truth
                .assertThat(yPosition)
                .isEqualTo(expectedYPosition)
            Truth
                .assertThat(xPosition)
                .isEqualTo(expectedXPosition)
            if (index == (maxItemsInMainAxis - 1)) {
                expectedXPosition -= eachSize
            }
            expectedYPosition += eachSize
        }
    }

    @Test
    fun testContextualFlowColumn_verticalArrangementTop_rtl_wrapContentWidth() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5

        var itemsThatCanFit = 0
        var width = 0
        val xYPositions = Array(10) { Pair(0f, 0f) }
        rule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(size.toDp())) {
                        ContextualFlowColumn(
                            modifier = Modifier
                                .fillMaxHeight(1f)
                                .onSizeChanged {
                                    width = it.width
                                    itemsThatCanFit = it.height / eachSize
                                },
                            verticalArrangement = Arrangement.Top,
                            maxItemsInEachColumn = maxItemsInMainAxis,
                            itemCount = 10
                        ) { index ->
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val xPosition = positionInParent.x
                                        val yPosition = positionInParent.y
                                        xYPositions[index] = Pair(xPosition, yPosition)
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedYPosition = 0
        var expectedXPosition = width
        var fittedItems = 0
        for (index in xYPositions.indices) {
            val pair = xYPositions[index]
            val xPosition = pair.first
            val yPosition = pair.second
            if (index % maxItemsInMainAxis == 0 ||
                fittedItems == itemsThatCanFit
            ) {
                expectedYPosition = 0
                expectedXPosition -= eachSize
                fittedItems = 0
            }
            Truth
                .assertThat(yPosition)
                .isEqualTo(expectedYPosition)
            Truth
                .assertThat(xPosition)
                .isEqualTo(expectedXPosition)
            expectedYPosition += eachSize
            fittedItems++
        }
    }

    @Test
    fun testContextualFlowRow_horizontalArrangementStart_rtl_wrap() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxMainAxisSize = 100
        //  * Visually:
        //  #54321
        //  ####6

        val xPositions = FloatArray(6)
        rule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        ContextualFlowRow(
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = 5,
                            itemCount = 6
                        ) { index ->
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        val xPosition = positionInParent.x
                                        xPositions[index] = xPosition
                                    }
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        var expectedXPosition = maxMainAxisSize - eachSize
        xPositions.forEachIndexed { index, position ->
            Truth
                .assertThat(position)
                .isEqualTo(expectedXPosition)
            if (index == (maxItemsInMainAxis - 1)) {
                expectedXPosition = maxMainAxisSize - eachSize
            } else {
                expectedXPosition -= eachSize
            }
        }
    }

    @Test
    fun testContextualFlowRow_constrainsOverflow() {
        var width = 0
        var noOfItemsPlaced = 0
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                Box(Modifier.size(200.dp)) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .onSizeChanged {
                                width = it.width
                            },
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        itemCount = 2,
                        overflow = ContextualFlowRowOverflow.Clip
                    ) { index ->
                        Layout(
                            modifier = Modifier
                                .requiredSize(250.dp)
                                .onPlaced {
                                    noOfItemsPlaced = index + 1
                                }
                        ) { _, _ ->
                            layout(250, 250) {} }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(200)
        Truth.assertThat(noOfItemsPlaced).isEqualTo(0)
    }

    @Test
    fun testContextualFlowColumn_constrainsOverflow() {
        var height = 0
        var noOfItemsPlaced = 0
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                Box(Modifier.size(200.dp)) {
                    ContextualFlowColumn(
                        modifier = Modifier
                            .fillMaxHeight(1f)
                            .onSizeChanged {
                                height = it.height
                            },
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        itemCount = 2,
                        overflow = ContextualFlowColumnOverflow.Clip
                    ) { index ->
                        Layout(
                            modifier = Modifier
                                .requiredSize(250.dp)
                                .onPlaced {
                                    noOfItemsPlaced = index + 1
                                }
                        ) { _, _ ->
                            layout(250, 250) {} }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(200)
        Truth.assertThat(noOfItemsPlaced).isEqualTo(0)
    }
}
