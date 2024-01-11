/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import kotlin.math.roundToInt
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalLayoutApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class FlowRowColumnTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testFlowRow_wrapsToTheNextLine() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    FlowRow(
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }) {
                        repeat(6) {
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
    fun testFlowColumn_wrapsToTheNextLine() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    FlowColumn(
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }) {
                        repeat(6) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_wrapsToTheNextLine_withExactSpaceNeeded() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    FlowRow(
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }) {
                        repeat(10) {
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
    fun testFlowColumn_wrapsToTheNextLine_withExactSpaceNeeded() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    FlowColumn(
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }) {
                        repeat(10) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_wrapsToTheNextLineMultipleTimes() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowRow(
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }) {
                        repeat(6) {
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
    fun testFlowColumn_wrapsToTheNextLineMultipleTimes() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowColumn(
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }) {
                        repeat(6) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_wrapsWithMaxItems() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowRow(
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }, maxItemsInEachRow = 2
                    ) {
                        repeat(6) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(60)
    }

    @Test
    fun testFlowColumn_wrapsWithMaxItems() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowColumn(
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }, maxItemsInEachColumn = 2
                    ) {
                        repeat(6) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testFlowRow_wrapsWithWeights() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowRow(
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }, maxItemsInEachRow = 2
                    ) {
                        repeat(6) {
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .weight(1f, true)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(60)
    }

    @Test
    fun testFlowColumn_wrapsWithWeights() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(60.toDp())) {
                    FlowColumn(
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }, maxItemsInEachColumn = 2
                    ) {
                        repeat(6) {
                            Box(
                                Modifier
                                    .size(20.toDp())
                                    .weight(1f, true)
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testFlowRow_staysInOneRow() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    FlowRow(
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }) {
                        repeat(2) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(20)
    }

    @Test
    fun testFlowRow_equalHeight() {
        val listOfHeights = mutableListOf<Int>()

        rule.setContent {
            with(LocalDensity.current) {
                FlowRow(
                    Modifier
                        .fillMaxWidth(1f)
                        .padding(20.dp)
                        .wrapContentHeight(align = Alignment.Top),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    maxItemsInEachRow = 3,
                ) {
                    repeat(9) {
                        Box(
                            Modifier
                                .onSizeChanged {
                                    listOfHeights.add(it.height)
                                }
                                .width(100.dp)
                                .background(Color.Green)
                                .fillMaxRowHeight()
                        ) {
                            val height = it * Random.Default.nextInt(0, 200)
                            Box(modifier = Modifier.height(height.dp))
                        }
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
    fun testFlowRow_fillMaxRowHeightWithZero() {
        val listOfHeights = mutableListOf<Int>()
        var finalHeight = 0

        rule.setContent {
            with(LocalDensity.current) {
                FlowRow(
                    Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight(align = Alignment.Top)
                        .onSizeChanged {
                                  finalHeight = it.height
                        },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3,
                ) {
                    repeat(9) {
                        Box(
                            Modifier
                                .onSizeChanged {
                                    listOfHeights.add(it.height)
                                }
                                .width(100.dp)
                                .background(Color.Green)
                                .fillMaxRowHeight(0f)
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        var desiredHeights = mutableListOf<Int>()
        repeat(9) {
            desiredHeights.add(0)
        }
        Truth.assertThat(listOfHeights).containsExactlyElementsIn(desiredHeights)
        Truth.assertThat(finalHeight).isEqualTo(0)
    }

    @Test
    fun testFlowRow_fillMaxRowHeightWithZero_InSome() {
        val listOfHeights = mutableListOf<Int>()
        var finalHeight = 0

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                with(LocalDensity.current) {
                    FlowRow(
                        Modifier
                            .fillMaxWidth(1f)
                            .padding(20.dp)
                            .wrapContentHeight(align = Alignment.Top)
                            .onSizeChanged {
                                finalHeight = it.height
                            },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 3,
                    ) {
                        repeat(9) {
                            Box(
                                Modifier
                                    .onSizeChanged {
                                        listOfHeights.add(it.height)
                                    }
                                    .width(100.dp)
                                    .background(Color.Green)
                                    .run {
                                        if (it % 3 == 0) {
                                            fillMaxRowHeight(0f)
                                        } else {
                                            this
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var desiredHeights = mutableListOf<Int>()
        repeat(9) {
            desiredHeights.add(if (it % 3 == 0) 0 else 20)
        }
        Truth.assertThat(listOfHeights).containsExactlyElementsIn(desiredHeights)
        Truth.assertThat(finalHeight).isEqualTo(60)
    }

    @Test
    fun testFlowRow_equalHeight_worksWithWeight() {
        val listOfHeights = mutableListOf<Int>()

        rule.setContent {
            with(LocalDensity.current) {
                FlowRow(
                    Modifier
                        .fillMaxWidth(1f)
                        .padding(20.dp)
                        .wrapContentHeight(align = Alignment.Top),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    maxItemsInEachRow = 3,
                ) {
                    repeat(9) {
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
                            val height = (it * Random.Default.nextInt(0, 200)) + it
                            Box(modifier = Modifier.height(height.dp))
                        }
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
    fun testFlowRow_equalHeight_WithFraction() {
        val listOfHeights = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                FlowRow(
                    Modifier
                        .fillMaxWidth(1f)
                        .padding(20.dp)
                        .wrapContentHeight(align = Alignment.Top, unbounded = true),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    maxItemsInEachRow = 3,
                ) {
                    repeat(9) { index ->
                        Box(
                            Modifier
                                .width(100.dp)
                                .background(Color.Green)
                                .run {
                                    if (index == 0 || index == 3 || index == 6) {
                                        fillMaxRowHeight(0.5f)
                                    } else {
                                        height(200.dp.times(index))
                                    }
                                }
                                .onPlaced {
                                    listOfHeights.add(index, it.size.height)
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(listOfHeights[0]).isEqualTo((.5 * listOfHeights[2]).roundToInt())
        Truth.assertThat(listOfHeights[1]).isNotEqualTo(listOfHeights[2])
        Truth.assertThat(listOfHeights[2]).isEqualTo(400)
        Truth.assertThat(listOfHeights[2]).isNotEqualTo(listOfHeights[3])
        Truth.assertThat(listOfHeights[3]).isEqualTo((.5 * listOfHeights[5]).roundToInt())
        Truth.assertThat(listOfHeights[4]).isNotEqualTo(listOfHeights[5])
        Truth.assertThat(listOfHeights[5]).isEqualTo(1000)
        Truth.assertThat(listOfHeights[5]).isNotEqualTo(listOfHeights[6])
        Truth.assertThat(listOfHeights[6]).isEqualTo((.5 * listOfHeights[8]).roundToInt())
        Truth.assertThat(listOfHeights[7]).isNotEqualTo(listOfHeights[8])
        Truth.assertThat(listOfHeights[8]).isEqualTo(1600)
    }

    @Test
    fun testFlowColumn_equalWidth() {
        val listOfWidths = mutableListOf<Int>()

        rule.setContent {
            with(LocalDensity.current) {
                FlowColumn(
                    Modifier
                        .fillMaxWidth(1f)
                        .padding(20.dp)
                        .wrapContentHeight(align = Alignment.Top),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachColumn = 3,
                ) {
                    repeat(9) {
                        Box(
                            Modifier
                                .onSizeChanged {
                                    listOfWidths.add(it.width)
                                }
                                .height(100.dp)
                                .background(Color.Green)
                                .fillMaxColumnWidth()
                        ) {
                            val width = it * Random.Default.nextInt(0, 500)
                            Box(modifier = Modifier.width(width.dp))
                        }
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
    fun testFlowColumn_equalWidth_worksWithWeight() {
        val listOfWidths = mutableListOf<Int>()

        rule.setContent {
            with(LocalDensity.current) {
                FlowColumn(
                    Modifier
                        .fillMaxHeight(1f)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachColumn = 3,
                ) {
                    repeat(9) {
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
                            val width = it * Random.Default.nextInt(0, 500)
                            Box(modifier = Modifier.width(width.dp))
                        }
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
    fun testFlowColumn_equalWidth_fraction() {
        val listOfWidths = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity
            ) {
                FlowColumn(
                    Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .padding(20.dp)
                        .fillMaxHeight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachColumn = 3,
                ) {
                    repeat(9) {
                        Box(
                            Modifier
                                .onPlaced {
                                    listOfWidths.add(it.size.width)
                                }
                                .height(100.dp)
                                .background(Color.Green)
                                .run {
                                    if (it == 0 || it == 3 || it == 6) {
                                        fillMaxColumnWidth(0.5f)
                                    } else {
                                        val width = it * 200
                                        width(width.dp)
                                    }
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(listOfWidths[0]).isEqualTo((.5 * listOfWidths[2]).roundToInt())
        Truth.assertThat(listOfWidths[1]).isNotEqualTo(listOfWidths[2])
        Truth.assertThat(listOfWidths[2]).isEqualTo(400)
        Truth.assertThat(listOfWidths[2]).isNotEqualTo(listOfWidths[3])
        Truth.assertThat(listOfWidths[3]).isEqualTo((.5 * listOfWidths[5]).roundToInt())
        Truth.assertThat(listOfWidths[4]).isNotEqualTo(listOfWidths[5])
        Truth.assertThat(listOfWidths[5]).isEqualTo(1000)
        Truth.assertThat(listOfWidths[5]).isNotEqualTo(listOfWidths[6])
        Truth.assertThat(listOfWidths[6]).isEqualTo((.5 * listOfWidths[8]).roundToInt())
        Truth.assertThat(listOfWidths[7]).isNotEqualTo(listOfWidths[8])
        Truth.assertThat(listOfWidths[8]).isEqualTo(1600)
    }

    @Test
    fun testFlowColumn_staysInOneRow() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    FlowColumn(
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }) {
                        repeat(2) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowRow_wrapsToTheNextLine_Rounding() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    FlowRow(
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            }) {
                        repeat(3) {
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
    fun testFlowColumn_wrapsToTheNextLine_Rounding() {
        var width = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(50.toDp())) {
                    FlowColumn(
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            }) {
                        repeat(3) {
                            Box(Modifier.size(20.toDp()))
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_empty() {
        var height = 0
        var width = 0

        rule.setContent {
            Box(Modifier.size(100.dp)) {
                FlowRow(
                    Modifier
                        .onSizeChanged {
                            height = it.height
                            width = it.width
                        }) {
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(0)
        Truth.assertThat(width).isEqualTo(0)
    }

    @Test
    fun testFlowColumn_empty() {
        var height = 0
        var width = 0

        rule.setContent {
            Box(Modifier.size(100.dp)) {
                FlowColumn(
                    Modifier
                        .onSizeChanged {
                            height = it.height
                            width = it.width
                        }) {
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(0)
        Truth.assertThat(width).isEqualTo(0)
    }

    @Test
    fun testFlowRow_alignItemsDefaultsToLeft() {

        val totalRowHeight = 20
        val shorterHeight = 10
        val expectedResult = 0f
        var positionInParentY = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow() {
                        repeat(5) { index ->
                            Box(
                                Modifier
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
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentY).isEqualTo(expectedResult)
    }

    @Test
    fun testFlowRow_alignItemsCenterVertically() {

        val totalRowHeight = 20
        val shorterHeight = 10
        val expectedResult = (totalRowHeight - shorterHeight) / 2
        var positionInParentY = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow() {
                        repeat(5) { index ->
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
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentY).isEqualTo(expectedResult)
    }

    @Test
    fun testFlowColumn_alignItemsDefaultsToTop() {
        val totalColumnWidth = 20
        val shorterWidth = 10
        val expectedResult = 0f
        var positionInParentX = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn() {
                        repeat(5) { index ->
                            Box(
                                Modifier
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
                                    })
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentX).isEqualTo(expectedResult)
    }

    @Test
    fun testFlowColumn_alignItemsCenterHorizontally() {

        val totalColumnWidth = 20
        val shorterWidth = 10
        val expectedResult = (totalColumnWidth - shorterWidth) / 2
        var positionInParentX = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn() {
                        repeat(5) { index ->
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
                                    })
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentX).isEqualTo(expectedResult)
    }

    @Test
    fun testFlowRow_horizontalArrangementSpaceAround() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val eachItemSpaceGiven = spaceAvailable / noOfItemsPerRow
        val gapSize = (eachItemSpaceGiven / 2).roundToInt()
        //  ----
        //      * Visually: #1##2##3# for LTR and #3##2##1# for RTL
        // --(front) - (back) --

        val xPositions = FloatArray(noOfItemsPerRow)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(
                        Modifier
                            .fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        repeat(5) { index ->
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
        var expectedXPosition = 0
        xPositions.forEach {
            val xPosition = it
            expectedXPosition += gapSize
            Truth
                .assertThat(xPosition)
                .isEqualTo(expectedXPosition)
            expectedXPosition += eachSize
            expectedXPosition += gapSize
        }
    }

    @Test
    fun testFlowColumn_verticalArrangementSpaceAround() {
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
                    FlowColumn(
                        Modifier
                            .fillMaxHeight(1f),
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        repeat(5) { index ->
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
    fun testFlowRow_horizontalArrangementSpaceAround_withTwoRows() {
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
                    FlowRow(
                        Modifier
                            .fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        maxItemsInEachRow = 5
                    ) {
                        repeat(10) { index ->
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
    fun testFlowColumn_verticalArrangementSpaceAround_withTwoColumns() {
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
                    FlowColumn(
                        Modifier
                            .fillMaxHeight(1f),
                        verticalArrangement = Arrangement.SpaceAround,
                        maxItemsInEachColumn = 5
                    ) {
                        repeat(10) { index ->
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
    fun testFlowRow_horizontalArrangementEnd() {
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
                    FlowRow(
                        Modifier
                            .fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.End,
                        maxItemsInEachRow = 5
                    ) {
                        repeat(10) { index ->
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
    fun testFlowColumn_verticalArrangementBottom() {
        val size = 200f
        val noOfItemsPerRow = 5
        val eachSize = 20
        val spaceAvailable = size - (noOfItemsPerRow * eachSize) // 100
        val gapSize = spaceAvailable.roundToInt()

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn(
                        Modifier
                            .fillMaxHeight(1f),
                        verticalArrangement = Arrangement.Bottom,
                        maxItemsInEachColumn = 5
                    ) {
                        repeat(10) { index ->
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
    fun testFlowRow_horizontalArrangementStart() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        //  * Visually: 123####

        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(
                        horizontalArrangement = Arrangement.Start,
                        maxItemsInEachRow = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
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
    fun testFlowRow_SpaceAligned() {
        val eachSize = 10
        val maxItemsInMainAxis = 5
        val spaceAligned = 10

        val xPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachRow = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
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

    /**
     * Should space something like this:
     * 1 2 3
     * # SpaceAligned
     * 4 5 6
     * No Space here
     */
    @Test
    fun testFlowRow_crossAxisSpacedBy() {
        val eachSize = 20
        val spaceAligned = 20
        val noOfItems = 3
        val expectedHeight = 100
        var heightResult = 0

        val yPositions = FloatArray(noOfItems)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(
                        Modifier
                            .onSizeChanged {
                                heightResult = it.height
                            },
                        verticalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachRow = 1
                    ) {
                        repeat(noOfItems) { index ->
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
    fun testFlowColumn_crossAxisSpacedBy() {
        val eachSize = 20
        val spaceAligned = 20
        val noOfItems = 3
        val expectedWidth = 100
        var widthResult = 0

        val xPositions = FloatArray(noOfItems)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn(
                        Modifier
                            .onSizeChanged {
                                widthResult = it.width
                            },
                        horizontalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachColumn = 1
                    ) {
                        repeat(noOfItems) { index ->
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
    fun testFlowColumn_SpaceAligned() {
        val eachSize = 10
        val maxItemsInMainAxis = 5
        val spaceAligned = 10

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn(
                        verticalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachColumn = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
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
    fun testFlowRow_SpaceAligned_notExact() {
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
                    FlowRow(
                        Modifier
                            .onSizeChanged {
                                width = it.width
                            },
                        horizontalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachRow = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
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
    fun testFlowColumn_SpaceAligned_notExact() {
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
                    FlowColumn(
                        Modifier
                            .onSizeChanged {
                                height = it.height
                            },
                        verticalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachColumn = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
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
    fun testFlowColumn_verticalArrangementTop() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5

        val yPositions = FloatArray(10)
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(size.toDp())) {
                    FlowColumn(
                        Modifier
                            .fillMaxHeight(1f),
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachColumn = maxItemsInMainAxis
                    ) {
                        repeat(10) { index ->
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
    fun testFlowRow_horizontalArrangementStart_rtl_fillMaxWidth() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5
        //  * Visually:
        //  #54321
        //  ####6

        val xPositions = FloatArray(6)
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(size.toDp())) {
                        FlowRow(
                            Modifier
                                .fillMaxWidth(1f),
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = maxItemsInMainAxis
                        ) {
                            repeat(6) { index ->
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
    fun testFlowColumn_verticalArrangementTop_rtl_fillMaxWidth() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5

        val xYPositions = Array(10) { Pair(0f, 0f) }
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(size.toDp())) {
                        FlowColumn(
                            Modifier
                                .fillMaxHeight(1f)
                                .fillMaxWidth(1f),
                            verticalArrangement = Arrangement.Top,
                            maxItemsInEachColumn = maxItemsInMainAxis
                        ) {
                            repeat(10) { index ->
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
    fun testFlowColumn_verticalArrangementTop_rtl_wrapContentWidth() {
        val size = 200f
        val eachSize = 20
        val maxItemsInMainAxis = 5

        var itemsThatCanFit = 0
        var width = 0
        val xYPositions = Array(10) { Pair(0f, 0f) }
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(size.toDp())) {
                        FlowColumn(
                            Modifier
                                .fillMaxHeight(1f)
                                .onSizeChanged {
                                    width = it.width
                                    itemsThatCanFit = it.height / eachSize
                                },
                            verticalArrangement = Arrangement.Top,
                            maxItemsInEachColumn = maxItemsInMainAxis
                        ) {
                            repeat(10) { index ->
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
    fun testFlowRow_horizontalArrangementStart_rtl_wrap() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxMainAxisSize = 100
        //  * Visually:
        //  #54321
        //  ####6

        val xPositions = FloatArray(6)
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = 5
                        ) {
                            repeat(6) { index ->
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
    fun testFlowRow_minIntrinsicWidth() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                },
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = 5
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                },
                            maxItemsInEachColumn = 6
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_wrap() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                },
                            maxItemsInEachColumn = 5
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_maxIntrinsicWidth() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Max)
                                .onSizeChanged {
                                    width = it.width
                                },
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(120)
    }

    @Test
    fun testFlowColumn_maxIntrinsicWidth() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Max)
                                .onSizeChanged {
                                    width = it.width
                                },
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_withSpaceBy() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                },
                            horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                            maxItemsInEachRow = 5
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_withSpaceBy() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(80.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_horizontalArrangement_withSpaceBy_MultipleRows() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                },
                            horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                            maxItemsInEachColumn = 5
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_horizontalArrangement_withSpaceBy() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                },
                            horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                            maxItemsInEachColumn = 5
                        ) {
                            repeat(5) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowRow_maxIntrinsicWidth_withSpaceBy() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Max)
                                .onSizeChanged {
                                    width = it.width
                                },
                            horizontalArrangement = Arrangement.spacedBy(10.toDp()),
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(170)
    }

    @Test
    fun testFlowColumn_maxIntrinsicWidth_withSpaceBy() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Max)
                                .onSizeChanged {
                                    width = it.width
                                },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_withAConstraint() {
        var width = 0
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                    height = it.height
                                },
                            maxItemsInEachRow = 5
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier
                                        .width(if (index == 5) 100.toDp() else 20.toDp())
                                        .height(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(100)
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_withAConstraint() {
        var width = 0
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                    height = it.height
                                },
                            maxItemsInEachColumn = 5
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier
                                        .height(if (index == 5) 100.toDp() else 20.toDp())
                                        .width(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(100)
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_withAConstraint_withSpacedBy() {
        var width = 0
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                    height = it.height
                                },
                            horizontalArrangement = Arrangement.spacedBy(10.toDp()),
                            maxItemsInEachRow = 5
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier
                                        .width(if (index == 5) 100.toDp() else 20.toDp())
                                        .height(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(100)
        Truth.assertThat(height).isEqualTo(60)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_withAConstraint_withSpacedBy() {
        var width = 0
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                    height = it.height
                                },
                            verticalArrangement = Arrangement.spacedBy(10.toDp()),
                            maxItemsInEachColumn = 5
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier
                                        .width(if (index == 5) 100.toDp() else 20.toDp())
                                        .height(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(140)
        Truth.assertThat(width).isEqualTo(120)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_withMaxItems() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                },
                            maxItemsInEachRow = 2
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_withMaxItems() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                },
                            maxItemsInEachRow = 2
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowRow_maxIntrinsicWidth_withMaxItems() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Max)
                                .onSizeChanged {
                                    width = it.width
                                },
                            maxItemsInEachRow = 2
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(40)
    }

    @Test
    fun testFlowColumn_maxIntrinsicWidth_withMaxItems() {
        var width = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Max)
                                .onSizeChanged {
                                    width = it.width
                                },
                            maxItemsInEachColumn = 2
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(60)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_withAConstraint_withMaxItems() {
        var width = 0
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                    height = it.height
                                },
                            maxItemsInEachRow = 2
                        ) {
                            repeat(10) { index ->
                                Box(
                                    Modifier
                                        .width(if (index == 5) 100.toDp() else 20.toDp())
                                        .height(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(101)
        Truth.assertThat(height).isEqualTo(120)
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_withAConstraint_withMaxItems() {
        var width = 0
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .onSizeChanged {
                                    width = it.width
                                    height = it.height
                                },
                            maxItemsInEachColumn = 2
                        ) {
                            repeat(10) { index ->
                                Box(
                                    Modifier
                                        .width(if (index == 5) 100.toDp() else 20.toDp())
                                        .height(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(180)
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testFlowRow_minIntrinsicHeight() {
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .height(IntrinsicSize.Min)
                                .onSizeChanged {
                                    height = it.height
                                },
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(20)
    }

    @Test
    fun testFlowRow_maxIntrinsicHeight() {
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .height(IntrinsicSize.Max)
                                .onSizeChanged {
                                    height = it.height
                                },
                        ) {
                            repeat(5) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(100)
    }

    @Test
    fun testFlowRow_maxIntrinsicHeight_withSpacedBy() {
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .height(IntrinsicSize.Max)
                                .onSizeChanged {
                                    height = it.height
                                },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(5) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(180)
    }

    @Test
    fun testFlowRow_minIntrinsicHeight_withSpaceBy_MultipleRows() {
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .height(IntrinsicSize.Min)
                                .onSizeChanged {
                                    height = it.height
                                },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                            maxItemsInEachRow = 1
                        ) {
                            repeat(2) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(60)
    }

    @Test
    fun testFlowRow_minIntrinsicHeight_withSpaceBy() {
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowRow(
                            Modifier
                                .height(IntrinsicSize.Min)
                                .onSizeChanged {
                                    height = it.height
                                },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(2) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_minIntrinsicHeight() {
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .height(IntrinsicSize.Min)
                                .onSizeChanged {
                                    height = it.height
                                },
                            maxItemsInEachColumn = 5
                        ) {
                            repeat(6) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(20)
    }

    @Test
    fun testFlowColumn_maxIntrinsicHeight() {
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .height(IntrinsicSize.Max)
                                .onSizeChanged {
                                    height = it.height
                                },
                            maxItemsInEachColumn = 5,
                            horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(5) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(100)
    }

    @Test
    fun testFlowColumn_maxIntrinsicHeight_withSpacedByOnMainAxis() {
        var height = 0
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.size(200.toDp())) {
                        FlowColumn(
                            Modifier
                                .height(IntrinsicSize.Max)
                                .onSizeChanged {
                                    height = it.height
                                },
                            maxItemsInEachColumn = 5,
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(5) {
                                Box(
                                    Modifier
                                        .size(20.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(180)
    }

    @Test
    fun testFlowRow_constrainsOverflow() {
        var width = 0
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(
                        Modifier
                            .fillMaxWidth(1f)
                            .onSizeChanged {
                                width = it.width
                            },
                        verticalArrangement = Arrangement.spacedBy(20.toDp()),
                    ) {
                        repeat(2) {
                            Box(
                                Modifier
                                    .size(250.toDp())
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(200)
    }

    @Test
    fun testFlowColumn_constrainsOverflow() {
        var height = 0
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn(
                        Modifier
                            .fillMaxWidth(1f)
                            .onSizeChanged {
                                height = it.height
                            },
                        horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                    ) {
                        repeat(2) {
                            Box(
                                Modifier
                                    .size(250.toDp())
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(200)
    }
}

private val NoOpDensity = object : Density {
    override val density = 1f
    override val fontScale = 1f
}
