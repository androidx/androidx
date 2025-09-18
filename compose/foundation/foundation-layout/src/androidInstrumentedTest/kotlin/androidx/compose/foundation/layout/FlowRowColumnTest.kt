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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.layout

import androidx.collection.mutableFloatListOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import kotlin.math.min
import kotlin.math.roundToInt
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalLayoutApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class FlowRowColumnTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun testFlowRow_wrapsToTheNextLine() {
        var height = 0

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    FlowRow(Modifier.onSizeChanged { height = it.height }) {
                        repeat(6) { Box(Modifier.size(20.toDp())) }
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
                    FlowColumn(Modifier.onSizeChanged { width = it.width }) {
                        repeat(6) { Box(Modifier.size(20.toDp())) }
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
                    FlowRow(Modifier.onSizeChanged { height = it.height }) {
                        repeat(10) { Box(Modifier.size(20.toDp())) }
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
                    FlowColumn(Modifier.onSizeChanged { width = it.width }) {
                        repeat(10) { Box(Modifier.size(20.toDp())) }
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
                    FlowRow(Modifier.onSizeChanged { height = it.height }) {
                        repeat(6) { Box(Modifier.size(20.toDp())) }
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
                    FlowColumn(Modifier.onSizeChanged { width = it.width }) {
                        repeat(6) { Box(Modifier.size(20.toDp())) }
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
                    FlowRow(Modifier.onSizeChanged { height = it.height }, maxItemsInEachRow = 2) {
                        repeat(6) { Box(Modifier.size(20.toDp())) }
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
                        Modifier.onSizeChanged { width = it.width },
                        maxItemsInEachColumn = 2,
                    ) {
                        repeat(6) { Box(Modifier.size(20.toDp())) }
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
                    FlowRow(Modifier.onSizeChanged { height = it.height }, maxItemsInEachRow = 2) {
                        repeat(6) { Box(Modifier.size(20.toDp()).weight(1f, true)) }
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
                        Modifier.onSizeChanged { width = it.width },
                        maxItemsInEachColumn = 2,
                    ) {
                        repeat(6) { Box(Modifier.size(20.toDp()).weight(1f, true)) }
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
                    FlowRow(Modifier.onSizeChanged { height = it.height }) {
                        repeat(2) { Box(Modifier.size(20.toDp())) }
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
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    Modifier.fillMaxWidth(1f)
                        .padding(20.dp)
                        .wrapContentHeight(align = Alignment.Top),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    maxItemsInEachRow = 3,
                ) {
                    repeat(9) {
                        Box(
                            Modifier.onSizeChanged { listOfHeights.add(it.height) }
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
                    Modifier.fillMaxWidth(1f)
                        .wrapContentHeight(align = Alignment.Top)
                        .onSizeChanged { finalHeight = it.height },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3,
                ) {
                    repeat(9) {
                        Box(
                            Modifier.onSizeChanged { listOfHeights.add(it.height) }
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
        repeat(9) { desiredHeights.add(0) }
        Truth.assertThat(listOfHeights).containsExactlyElementsIn(desiredHeights)
        Truth.assertThat(finalHeight).isEqualTo(0)
    }

    @Test
    fun testFlowRow_fillMaxRowHeightWithZero_InSome() {
        val listOfHeights = mutableListOf<Int>()
        var finalHeight = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                with(LocalDensity.current) {
                    FlowRow(
                        Modifier.fillMaxWidth(1f)
                            .padding(20.dp)
                            .wrapContentHeight(align = Alignment.Top)
                            .onSizeChanged { finalHeight = it.height },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 3,
                    ) {
                        repeat(9) {
                            Box(
                                Modifier.onSizeChanged { listOfHeights.add(it.height) }
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
        repeat(9) { desiredHeights.add(if (it % 3 == 0) 0 else 20) }
        Truth.assertThat(listOfHeights).containsExactlyElementsIn(desiredHeights)
        Truth.assertThat(finalHeight).isEqualTo(60)
    }

    @Test
    fun testFlowRow_equalHeight_worksWithWeight() {
        val listOfHeights = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    Modifier.fillMaxWidth(1f)
                        .padding(20.dp)
                        .wrapContentHeight(align = Alignment.Top),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    maxItemsInEachRow = 3,
                ) {
                    repeat(9) {
                        Box(
                            Modifier.onSizeChanged { listOfHeights.add(it.height) }
                                .width(100.dp)
                                .weight(1f, true)
                                .background(Color.Green)
                                .fillMaxRowHeight()
                        ) {
                            val height = it
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
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    Modifier.fillMaxWidth(1f)
                        .padding(20.dp)
                        .wrapContentHeight(align = Alignment.Top, unbounded = true),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    maxItemsInEachRow = 3,
                ) {
                    repeat(9) { index ->
                        Box(
                            Modifier.width(100.dp)
                                .background(Color.Green)
                                .run {
                                    if (index == 0 || index == 3 || index == 6) {
                                        fillMaxRowHeight(0.5f)
                                    } else {
                                        height(200.dp.times(index))
                                    }
                                }
                                .onPlaced { listOfHeights.add(index, it.size.height) }
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
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    Modifier.wrapContentWidth(align = Alignment.Start)
                        .padding(20.dp)
                        .fillMaxHeight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachColumn = 3,
                ) {
                    repeat(9) {
                        Box(
                            Modifier.onSizeChanged { listOfWidths.add(it.width) }
                                .height(100.dp)
                                .background(Color.Green)
                                .fillMaxColumnWidth()
                        ) {
                            val width = 20 * it
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
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    Modifier.wrapContentWidth(align = Alignment.Start)
                        .fillMaxHeight(1f)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachColumn = 3,
                ) {
                    repeat(9) {
                        Box(
                            Modifier.onSizeChanged { listOfWidths.add(it.width) }
                                .height(100.dp)
                                .weight(1f, true)
                                .background(Color.Green)
                                .fillMaxColumnWidth()
                        ) {
                            val width = 20 * it
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
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .padding(20.dp)
                        .fillMaxWidth(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachColumn = 3,
                ) {
                    repeat(9) {
                        Box(
                            Modifier.onPlaced { listOfWidths.add(it.size.width) }
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
                    FlowColumn(Modifier.onSizeChanged { width = it.width }) {
                        repeat(2) { Box(Modifier.size(20.toDp())) }
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
                    FlowRow(Modifier.onSizeChanged { height = it.height }) {
                        repeat(3) { Box(Modifier.size(20.toDp())) }
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
                    FlowColumn(Modifier.onSizeChanged { width = it.width }) {
                        repeat(3) { Box(Modifier.size(20.toDp())) }
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
                    Modifier.onSizeChanged {
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
    fun testFlowColumn_empty() {
        var height = 0
        var width = 0

        rule.setContent {
            Box(Modifier.size(100.dp)) {
                FlowColumn(
                    Modifier.onSizeChanged {
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
                                Modifier.size(
                                        20.toDp(),
                                        if (index == 4) {
                                            shorterHeight.toDp()
                                        } else {
                                            totalRowHeight.toDp()
                                        },
                                    )
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentY = positionInParent.y
                                        }
                                    }
                            )
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
                                Modifier.align(Alignment.CenterVertically)
                                    .size(
                                        20.toDp(),
                                        if (index == 4) {
                                            shorterHeight.toDp()
                                        } else {
                                            totalRowHeight.toDp()
                                        },
                                    )
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentY = positionInParent.y
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentY).isEqualTo(expectedResult)
    }

    @Test
    fun testFlowRow_alignItemsCenterVertically_UsingTopLevelAPI() {

        val totalRowHeight = 20
        val shorterHeight = 10
        val expectedResult = (totalRowHeight - shorterHeight) / 2
        var positionInParentY = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Box(
                                Modifier.size(
                                        20.toDp(),
                                        if (index == 4) {
                                            shorterHeight.toDp()
                                        } else {
                                            totalRowHeight.toDp()
                                        },
                                    )
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentY = positionInParent.y
                                        }
                                    }
                            )
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
                                Modifier.size(
                                        if (index == 4) {
                                            shorterWidth.toDp()
                                        } else {
                                            totalColumnWidth.toDp()
                                        },
                                        20.toDp(),
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
                                Modifier.align(Alignment.CenterHorizontally)
                                    .size(
                                        if (index == 4) {
                                            shorterWidth.toDp()
                                        } else {
                                            totalColumnWidth.toDp()
                                        },
                                        20.toDp(),
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
        }

        rule.waitForIdle()
        Truth.assertThat(positionInParentX).isEqualTo(expectedResult)
    }

    @Test
    fun testRow_withCustomVertical_alignment() {
        val rowHeight = 200
        val boxSize = 20
        var boxOffset = 0f
        var capturedSpace = 0
        var capturedSize = 0
        rule.setContent {
            with(LocalDensity.current) {
                FlowRow(
                    itemVerticalAlignment =
                        object : Alignment.Vertical {
                            override fun align(size: Int, space: Int): Int {
                                capturedSpace = space
                                capturedSize = size
                                val offset = (space - size) / 2
                                return offset.coerceIn(0, space - size)
                            }
                        }
                ) {
                    Box(
                        modifier =
                            Modifier.size(boxSize.toDp()).onPlaced { coordinates ->
                                boxOffset = coordinates.positionInParent().y
                            }
                    )
                    Box(modifier = Modifier.size(rowHeight.toDp()))
                }
            }
        }
        rule.waitForIdle()
        val expectedOffset = ((rowHeight - boxSize) / 2).toFloat()
        assertEquals(expectedOffset, boxOffset)
        assertEquals(rowHeight, capturedSize)
        assertEquals(rowHeight, capturedSpace)
    }

    @Test
    fun testColumn_withCustomHorizontal_alignment() {
        val columnWidth = 300
        val boxSize = 40
        var boxOffset = 0f
        var capturedSpace = 0
        var capturedSize = 0
        rule.setContent {
            with(LocalDensity.current) {
                FlowColumn(
                    itemHorizontalAlignment =
                        object : Alignment.Horizontal {
                            override fun align(
                                size: Int,
                                space: Int,
                                layoutDirection: LayoutDirection,
                            ): Int {
                                capturedSpace = space
                                capturedSize = size
                                val offset = (space - size) / 2
                                return offset.coerceIn(0, space - size)
                            }
                        }
                ) {
                    Box(
                        modifier =
                            Modifier.size(boxSize.toDp()).onPlaced { coordinates ->
                                boxOffset = coordinates.positionInParent().x
                            }
                    )
                    Box(modifier = Modifier.size(columnWidth.toDp()))
                }
            }
        }
        rule.waitForIdle()
        val expectedOffset = ((columnWidth - boxSize) / 2).toFloat()
        assertEquals(expectedOffset, boxOffset)
        assertEquals(columnWidth, capturedSize)
        assertEquals(columnWidth, capturedSpace)
    }

    @Test
    fun testColumn_withCustomHorizontalAlignModifier() {
        val columnWidth = 300
        val boxSize = 20
        var boxOffset = 0f
        var capturedSpace = 0
        var capturedSize = 0
        rule.setContent {
            with(LocalDensity.current) {
                FlowColumn {
                    Box(
                        modifier =
                            Modifier.size(boxSize.toDp())
                                .align(
                                    object : Alignment.Horizontal {
                                        override fun align(
                                            size: Int,
                                            space: Int,
                                            layoutDirection: LayoutDirection,
                                        ): Int {
                                            capturedSpace = space
                                            capturedSize = size
                                            val offset = (space - size) / 2
                                            return offset.coerceIn(0, space - size)
                                        }
                                    }
                                )
                                .onPlaced { coordinates ->
                                    boxOffset = coordinates.positionInParent().x
                                }
                    )
                    Box(modifier = Modifier.size(columnWidth.toDp()))
                }
            }
        }
        rule.waitForIdle()
        val expectedOffset = ((columnWidth - boxSize) / 2).toFloat()
        assertEquals(expectedOffset, boxOffset)
        assertEquals(boxSize, capturedSize)
        assertEquals(columnWidth, capturedSpace)
    }

    @Test
    fun testRow_withCustomVerticalAlignModifier() {
        val rowHeight = 200
        val boxSize = 20
        var boxOffset = 0f
        var capturedSpace = 0
        var capturedSize = 0
        val expectedOffset = ((rowHeight - boxSize) / 2).toFloat()
        rule.setContent {
            with(LocalDensity.current) {
                FlowRow {
                    Box(
                        modifier =
                            Modifier.size(boxSize.toDp())
                                .align(
                                    object : Alignment.Vertical {
                                        override fun align(size: Int, space: Int): Int {
                                            capturedSpace = space
                                            capturedSize = size
                                            val offset = (space - size) / 2
                                            return offset.coerceIn(0, space - size)
                                        }
                                    }
                                )
                                .onPlaced { coordinates ->
                                    boxOffset = coordinates.positionInParent().y
                                }
                    )

                    Box(modifier = Modifier.size(rowHeight.toDp()))
                }
            }
        }
        rule.waitForIdle()
        assertEquals(expectedOffset, boxOffset)
        assertEquals(boxSize, capturedSize)
        assertEquals(rowHeight, capturedSpace)
    }

    @Test
    fun testFlowColumn_alignItemsCenterHorizontally_UsingTopLevelAPI() {

        val totalColumnWidth = 20
        val shorterWidth = 10
        val expectedResult = (totalColumnWidth - shorterWidth) / 2
        var positionInParentX = 0f
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlowColumn(itemHorizontalAlignment = Alignment.CenterHorizontally) {
                        repeat(5) { index ->
                            Box(
                                Modifier.size(
                                        if (index == 4) {
                                            shorterWidth.toDp()
                                        } else {
                                            totalColumnWidth.toDp()
                                        },
                                        20.toDp(),
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
                        Modifier.fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier.size(20.toDp()).onPlaced {
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
            Truth.assertThat(xPosition).isEqualTo(expectedXPosition)
            expectedXPosition += eachSize
            expectedXPosition += gapSize
        }
    }

    @Test
    fun testFlowRow_MaxLinesVisible() {
        val itemSize = 50f
        val maxLines = 2
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = FlowRowOverflow.Visible,
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        // Assert that the number of items shown is as expected
        Truth.assertThat(itemsShownCount).isEqualTo(10)
    }

    @Test
    fun testFlowColumn_MaxLinesVisible() {
        val itemSize = 50f
        val maxLines = 2
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = FlowColumnOverflow.Visible,
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        // Assert that the number of items shown is as expected
        Truth.assertThat(itemsShownCount).isEqualTo(10)
    }

    @Test
    fun testFlowRow_MaxHeightVisible() {
        val itemSize = 50f
        val maxHeight = 120
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    modifier = Modifier.width(200.dp).height(maxHeight.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow = FlowRowOverflow.Visible,
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        // Assert that the number of items shown is as expected
        Truth.assertThat(itemsShownCount).isEqualTo(10)
    }

    @Test
    fun testFlowColumn_MaxWidthVisible() {
        val itemSize = 50f
        val maxWidth = 120
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    modifier = Modifier.height(200.dp).width(maxWidth.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow = FlowColumnOverflow.Visible,
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        // Assert that the number of items shown is as expected
        Truth.assertThat(itemsShownCount).isEqualTo(10)
    }

    @Test
    fun testFlowRow_MaxLinesClipped() {
        val itemSize = 50f
        val maxLines = 2
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = FlowRowOverflow.Clip,
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        // Assert that the number of items shown is as expected
        Truth.assertThat(itemsShownCount).isEqualTo(6)
    }

    @Test
    fun testFlowColumn_MaxLinesClipped() {
        val itemSize = 50f
        val maxLines = 2
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow = FlowColumnOverflow.Clip,
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        // Assert that the number of items shown is as expected
        Truth.assertThat(itemsShownCount).isEqualTo(6)
    }

    @Test
    fun testFlowRow_MaxHeightClipped() {
        val itemSize = 50f
        val maxHeight = 120
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    modifier = Modifier.width(200.dp).height(maxHeight.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow = FlowRowOverflow.Clip,
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        // Assert that the number of items shown is as expected
        Truth.assertThat(itemsShownCount).isEqualTo(6)
    }

    @Test
    fun testFlowColumn_MaxWidthClipped() {
        val itemSize = 50f
        val maxWidth = 120
        val totalItems = 10
        val spacing = 20
        var itemsShownCount = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    modifier = Modifier.height(200.dp).width(maxWidth.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow = FlowColumnOverflow.Clip,
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        // Assert that the number of items shown is as expected
        Truth.assertThat(itemsShownCount).isEqualTo(6)
    }

    @Test
    fun testFlowRow_MaxLinesSeeMore() {
        val itemSize = 50f
        val totalItems = 15
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = false
        val seeMoreTag = "SeeMoreTag"
        var finalMaxLines = 2

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow =
                        FlowRowOverflow.expandIndicator {
                            Box(
                                modifier =
                                    Modifier.clickable {
                                            itemsShownCount = 0
                                            seeMoreShown = false
                                            maxLines += 2
                                            finalMaxLines = maxLines
                                        }
                                        .size(itemSize.dp)
                                        .testTag(seeMoreTag)
                                        .onGloballyPositioned { seeMoreShown = true }
                            )
                        },
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
        }
        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(15)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isFalse()
        }
    }

    @Test
    fun testFlowColumn_MaxLinesSeeMore() {
        val itemSize = 50f
        val totalItems = 15
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = false
        val seeMoreTag = "SeeMoreTag"
        var finalMaxLines = 2

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow =
                        FlowColumnOverflow.expandIndicator {
                            Box(
                                modifier =
                                    Modifier.clickable {
                                            itemsShownCount = 0
                                            seeMoreShown = false
                                            maxLines += 2
                                            finalMaxLines = maxLines
                                        }
                                        .size(itemSize.dp)
                                        .testTag(seeMoreTag)
                                        .onGloballyPositioned { seeMoreShown = true }
                            )
                        },
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
        }
        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(15)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isFalse()
        }
    }

    @Test
    fun testFlowRow_MaxHeightSeeMore() {
        val itemSize = 50f
        val totalItems = 15
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = false
        val seeMoreTag = "SeeMoreTag"
        var finalMaxHeight = 120.dp

        rule.setContent {
            var maxHeight by remember { mutableStateOf(120.dp) }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    modifier = Modifier.width(200.dp).height(maxHeight),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow =
                        FlowRowOverflow.expandIndicator {
                            Box(
                                modifier =
                                    Modifier.clickable {
                                            itemsShownCount = 0
                                            seeMoreShown = false
                                            maxHeight += 100.dp + (spacing.dp * 2)
                                            finalMaxHeight = maxHeight
                                        }
                                        .size(itemSize.dp)
                                        .testTag(seeMoreTag)
                                        .onGloballyPositioned { seeMoreShown = true }
                            )
                        },
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onGloballyPositioned {
                                    itemsShownCount = index + 1
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            itemsShownCount = 0
        }
        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(finalMaxHeight).isEqualTo(260.dp)
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(seeMoreShown).isTrue()
            itemsShownCount = 0
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(15)
            Truth.assertThat(finalMaxHeight).isEqualTo(400.dp)
            Truth.assertThat(seeMoreShown).isFalse()
        }
    }

    @Test
    fun testFlowColumn_MaxWidthSeeMore() {
        val itemSize = 50f
        val totalItems = 15
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = false
        val seeMoreTag = "SeeMoreTag"
        var finalMaxWidth = 120.dp

        rule.setContent {
            var maxWidth by remember { mutableStateOf(120.dp) }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    modifier = Modifier.height(200.dp).width(maxWidth),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    overflow =
                        FlowColumnOverflow.expandIndicator {
                            Box(
                                modifier =
                                    Modifier.clickable {
                                            itemsShownCount = 0
                                            seeMoreShown = false
                                            maxWidth += 100.dp + (spacing.dp * 2)
                                            finalMaxWidth = maxWidth
                                        }
                                        .size(itemSize.dp)
                                        .testTag(seeMoreTag)
                                        .onGloballyPositioned { seeMoreShown = true }
                            )
                        },
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onGloballyPositioned {
                                    itemsShownCount = index + 1
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            itemsShownCount = 0
        }
        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(finalMaxWidth).isEqualTo(260.dp)
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(seeMoreShown).isTrue()
            itemsShownCount = 0
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(15)
            Truth.assertThat(finalMaxWidth).isEqualTo(400.dp)
            Truth.assertThat(seeMoreShown).isFalse()
        }
    }

    @Test
    fun testFlowRow_ThrowsExceptionWhenSeeMoreCalledDuringComposition() {
        val itemSize = 50f
        val totalItems = 18
        val spacing = 20

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow =
                        FlowRowOverflow.expandOrCollapseIndicator(
                            expandIndicator = {
                                Assert.assertThrows(RuntimeException::class.java) {
                                    totalItems - shownItemCount
                                }
                            },
                            collapseIndicator = {
                                Assert.assertThrows(RuntimeException::class.java) {
                                    totalItems - shownItemCount
                                }
                            },
                        ),
                ) {
                    repeat(totalItems) { _ -> Box(modifier = Modifier.size(itemSize.dp)) }
                }
            }
        }
    }

    @Test
    fun testFlowColumn_ThrowsExceptionWhenSeeMoreCalledDuringComposition() {
        val itemSize = 50f
        val totalItems = 18
        val spacing = 20

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow =
                        FlowColumnOverflow.expandOrCollapseIndicator(
                            expandIndicator = {
                                Assert.assertThrows(RuntimeException::class.java) {
                                    totalItems - shownItemCount
                                }
                            },
                            collapseIndicator = {
                                Assert.assertThrows(RuntimeException::class.java) {
                                    totalItems - shownItemCount
                                }
                            },
                        ),
                ) {
                    repeat(totalItems) { _ -> Box(modifier = Modifier.size(itemSize.dp)) }
                }
            }
        }
    }

    @Test
    fun testFlowRow_MaxLinesSeeMoreOrCollapse() {
        val itemSize = 50f
        val totalItems = 18
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = true
        var collapseShown = false
        val seeMoreTag = "SeeMoreTag"
        val collapseTag = "CollapseTag"
        var finalMaxLines = 2
        lateinit var scopeOnExpand: FlowRowOverflowScope
        lateinit var scopeOnCollapse: FlowRowOverflowScope

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow =
                        FlowRowOverflow.expandOrCollapseIndicator(
                            expandIndicator = {
                                scopeOnExpand = this
                                Box(
                                    modifier =
                                        Modifier.clickable {
                                                itemsShownCount = 0
                                                seeMoreShown = false
                                                collapseShown = false
                                                maxLines += 2
                                                finalMaxLines = maxLines
                                            }
                                            .size(itemSize.dp)
                                            .testTag(seeMoreTag)
                                            .onGloballyPositioned { seeMoreShown = true }
                                            .onPlaced { seeMoreShown = true }
                                )
                            },
                            collapseIndicator = {
                                scopeOnCollapse = this
                                Box(
                                    modifier =
                                        Modifier.clickable {
                                                itemsShownCount = 0
                                                seeMoreShown = false
                                                collapseShown = false
                                                maxLines = 2
                                                finalMaxLines = maxLines
                                            }
                                            .size(itemSize.dp)
                                            .testTag(collapseTag)
                                            .onGloballyPositioned { collapseShown = true }
                                            .onPlaced { collapseShown = true }
                                )
                            },
                        ),
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(17)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(18)
            Truth.assertThat(finalMaxLines).isEqualTo(8)
            Truth.assertThat(collapseShown).isTrue()
            Truth.assertThat(seeMoreShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(collapseTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(finalMaxLines).isEqualTo(2)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(5)
        }
    }

    @Test
    fun testFlowColumn_MaxLinesSeeMoreOrCollapse() {
        val itemSize = 50f
        val totalItems = 18
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = true
        var collapseShown = false
        val seeMoreTag = "SeeMoreTag"
        val collapseTag = "CollapseTag"
        var finalMaxLines = 2
        lateinit var scopeOnExpand: FlowColumnOverflowScope
        lateinit var scopeOnCollapse: FlowColumnOverflowScope

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow =
                        FlowColumnOverflow.expandOrCollapseIndicator(
                            expandIndicator = {
                                scopeOnExpand = this
                                Box(
                                    modifier =
                                        Modifier.clickable {
                                                itemsShownCount = 0
                                                seeMoreShown = false
                                                collapseShown = false
                                                maxLines += 2
                                                finalMaxLines = maxLines
                                            }
                                            .size(itemSize.dp)
                                            .testTag(seeMoreTag)
                                            .onGloballyPositioned { seeMoreShown = true }
                                            .onPlaced { seeMoreShown = true }
                                )
                            },
                            collapseIndicator = {
                                scopeOnCollapse = this
                                Box(
                                    modifier =
                                        Modifier.clickable {
                                                itemsShownCount = 0
                                                seeMoreShown = false
                                                collapseShown = false
                                                maxLines = 2
                                                finalMaxLines = maxLines
                                            }
                                            .size(itemSize.dp)
                                            .testTag(collapseTag)
                                            .onGloballyPositioned { collapseShown = true }
                                            .onPlaced { collapseShown = true }
                                )
                            },
                        ),
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(17)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(18)
            Truth.assertThat(finalMaxLines).isEqualTo(8)
            Truth.assertThat(collapseShown).isTrue()
            Truth.assertThat(seeMoreShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(collapseTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(finalMaxLines).isEqualTo(2)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(5)
        }
    }

    @Test
    fun testFlowRow_MaxLines_DifferentCollapseSize() {
        val itemSize = 50f
        val collapseSize = 100f
        val totalItems = 18
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = true
        var collapseShown = false
        val seeMoreTag = "SeeMoreTag"
        val collapseTag = "CollapseTag"
        var finalMaxLines = 2
        lateinit var scopeOnExpand: FlowRowOverflowScope
        lateinit var scopeOnCollapse: FlowRowOverflowScope

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowRow(
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow =
                        FlowRowOverflow.expandOrCollapseIndicator(
                            expandIndicator = {
                                scopeOnExpand = this
                                Box(
                                    modifier =
                                        Modifier.clickable {
                                                itemsShownCount = 0
                                                seeMoreShown = false
                                                collapseShown = false
                                                maxLines += 2
                                                finalMaxLines = maxLines
                                            }
                                            .size(itemSize.dp)
                                            .testTag(seeMoreTag)
                                            .onGloballyPositioned { seeMoreShown = true }
                                )
                            },
                            collapseIndicator = {
                                scopeOnCollapse = this
                                Box(
                                    modifier =
                                        Modifier.clickable {
                                                itemsShownCount = 0
                                                seeMoreShown = false
                                                collapseShown = false
                                                maxLines = 2
                                                finalMaxLines = maxLines
                                            }
                                            .size(collapseSize.dp)
                                            .testTag(collapseTag)
                                            .onGloballyPositioned { collapseShown = true }
                                )
                            },
                        ),
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(17)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(18)
            Truth.assertThat(finalMaxLines).isEqualTo(8)
            Truth.assertThat(collapseShown).isTrue()
            Truth.assertThat(seeMoreShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnCollapse.shownItemCount)
            Truth.assertThat(scopeOnCollapse.shownItemCount).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(collapseTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(finalMaxLines).isEqualTo(2)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnExpand.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(5)
        }
    }

    @Test
    fun testFlowColumn_MaxLines_DifferentCollapseSize() {
        val itemSize = 50f
        val collapseSize = 100f
        val totalItems = 18
        val spacing = 20
        var itemsShownCount = 0
        var seeMoreShown = true
        var collapseShown = false
        val seeMoreTag = "SeeMoreTag"
        val collapseTag = "CollapseTag"
        var finalMaxLines = 2
        lateinit var scopeOnExpand: FlowColumnOverflowScope
        lateinit var scopeOnCollapse: FlowColumnOverflowScope

        rule.setContent {
            var maxLines by remember { mutableStateOf(2) }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlowColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    maxLines = maxLines,
                    overflow =
                        FlowColumnOverflow.expandOrCollapseIndicator(
                            expandIndicator = {
                                scopeOnExpand = this
                                Box(
                                    modifier =
                                        Modifier.clickable {
                                                itemsShownCount = 0
                                                seeMoreShown = false
                                                collapseShown = false
                                                maxLines += 2
                                                finalMaxLines = maxLines
                                            }
                                            .size(itemSize.dp)
                                            .testTag(seeMoreTag)
                                            .onGloballyPositioned { seeMoreShown = true }
                                )
                            },
                            collapseIndicator = {
                                scopeOnCollapse = this
                                Box(
                                    modifier =
                                        Modifier.clickable {
                                                itemsShownCount = 0
                                                seeMoreShown = false
                                                collapseShown = false
                                                maxLines = 2
                                                finalMaxLines = maxLines
                                            }
                                            .size(collapseSize.dp)
                                            .testTag(collapseTag)
                                            .onGloballyPositioned { collapseShown = true }
                                )
                            },
                        ),
                ) {
                    repeat(totalItems) { index ->
                        Box(
                            modifier =
                                Modifier.size(itemSize.dp).onPlaced { itemsShownCount = index + 1 }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(itemsShownCount).isEqualTo(5)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.totalItemCount).isEqualTo(scopeOnExpand.totalItemCount)
            Truth.assertThat(scopeOnExpand.totalItemCount).isEqualTo(totalItems)
            Truth.assertThat(scopeOnCollapse.totalItemCount).isEqualTo(totalItems)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnExpand.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(11)
            Truth.assertThat(finalMaxLines).isEqualTo(4)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnExpand.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(17)
            Truth.assertThat(finalMaxLines).isEqualTo(6)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnExpand.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }

        rule.onNodeWithTag(seeMoreTag).performTouchInput { click() }

        advanceClock()
        rule.runOnIdle {
            // Assert that the number of items shown is as expected
            Truth.assertThat(itemsShownCount).isEqualTo(18)
            Truth.assertThat(finalMaxLines).isEqualTo(8)
            Truth.assertThat(collapseShown).isTrue()
            Truth.assertThat(seeMoreShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnExpand.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(itemsShownCount)
        }
        rule.onNodeWithTag(collapseTag).performTouchInput { click() }

        advanceClock()

        rule.runOnIdle {
            Truth.assertThat(finalMaxLines).isEqualTo(2)
            Truth.assertThat(seeMoreShown).isTrue()
            Truth.assertThat(collapseShown).isFalse()
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(scopeOnExpand.shownItemCount)
            Truth.assertThat(scopeOnExpand.shownItemCount).isEqualTo(5)
        }
    }

    private fun advanceClock() {
        rule.mainClock.advanceTimeBy(100_000L)
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
                        Modifier.fillMaxHeight(1f),
                        verticalArrangement = Arrangement.SpaceAround,
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier.size(20.toDp()).onPlaced {
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
            Truth.assertThat(yPosition).isEqualTo(expectedYPosition)
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
                        Modifier.fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        maxItemsInEachRow = 5,
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier.size(20.toDp()).onPlaced {
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
            Truth.assertThat(xPosition).isEqualTo(expectedXPosition)
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
                        Modifier.fillMaxHeight(1f),
                        verticalArrangement = Arrangement.SpaceAround,
                        maxItemsInEachColumn = 5,
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier.size(20.toDp()).onPlaced {
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
            Truth.assertThat(position).isEqualTo(expectedYPosition)
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
                        Modifier.fillMaxWidth(1f),
                        horizontalArrangement = Arrangement.End,
                        maxItemsInEachRow = 5,
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier.size(20.toDp()).onPlaced {
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
            Truth.assertThat(position).isEqualTo(expectedXPosition)
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
                        Modifier.fillMaxHeight(1f),
                        verticalArrangement = Arrangement.Bottom,
                        maxItemsInEachColumn = 5,
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier.size(20.toDp()).onPlaced {
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
            Truth.assertThat(position).isEqualTo(expectedYPosition)
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
                        maxItemsInEachRow = maxItemsInMainAxis,
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier.size(eachSize.toDp()).onPlaced {
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
            Truth.assertThat(position).isEqualTo(expectedXPosition)
            if (index == (maxItemsInMainAxis - 1)) {
                expectedXPosition = 0
            } else {
                expectedXPosition += eachSize
            }
        }
    }

    @Test
    fun testFlowRow_horizontalArrangementStart_SeeMoreFullWidth() {
        val eachSize = 50

        val totalCount = 20
        val positions: MutableList<Offset> = mutableListOf()
        var seeMorePosition: Offset? = null
        var seeMoreSize: IntSize? = null
        var mainAxisSpacing = 10
        var crossAxisSpacing = 20
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                var maxLines by remember { mutableStateOf(2) }
                Box(modifier = Modifier.width(320.dp).wrapContentHeight()) {
                    FlowRow(
                        Modifier.fillMaxWidth(1f).wrapContentHeight(align = Alignment.Top),
                        horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing.dp),
                        verticalArrangement = Arrangement.spacedBy(crossAxisSpacing.dp),
                        maxLines = maxLines,
                        overflow =
                            FlowRowOverflow.expandIndicator {
                                Box(
                                    modifier =
                                        Modifier.fillMaxWidth(1f)
                                            .height(eachSize.dp)
                                            .background(Color.Green)
                                            .clickable { maxLines += 2 }
                                            .onPlaced {
                                                seeMorePosition = it.positionInParent()
                                                seeMoreSize = it.size
                                            }
                                ) {}
                            },
                    ) {
                        repeat(totalCount) { index ->
                            Box(
                                Modifier.width(eachSize.dp)
                                    .height(50.dp)
                                    .background(Color.Green)
                                    .onPlaced {
                                        val positionInParent = it.positionInParent()
                                        positions.add(index, positionInParent)
                                    }
                            ) {}
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        var expectedYPosition = 0
        Truth.assertThat(positions.size).isEqualTo(5)
        positions.forEach { position ->
            Truth.assertThat(position.x).isEqualTo(expectedXPosition)

            Truth.assertThat(position.y).isEqualTo(expectedYPosition)
            expectedXPosition += eachSize + mainAxisSpacing
        }
        expectedYPosition += eachSize + crossAxisSpacing
        Truth.assertThat(seeMorePosition?.x).isEqualTo(0)
        Truth.assertThat(seeMorePosition?.y).isEqualTo(expectedYPosition)
        Truth.assertThat(seeMoreSize?.width).isEqualTo(320)
        Truth.assertThat(seeMoreSize?.height).isEqualTo(eachSize)
    }

    @Test
    fun testFlowColumn_verticalArrangementTop_SeeMoreFullHeight() {
        val eachSize = 50

        val totalCount = 20
        val positions: MutableList<Offset> = mutableListOf()
        var seeMorePosition: Offset? = null
        var seeMoreSize: IntSize? = null
        var mainAxisSpacing = 10
        var crossAxisSpacing = 20
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                var maxLines by remember { mutableStateOf(2) }
                Box(modifier = Modifier.height(320.dp).wrapContentWidth()) {
                    FlowColumn(
                        Modifier.fillMaxHeight(1f).wrapContentWidth(align = Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(mainAxisSpacing.dp),
                        horizontalArrangement = Arrangement.spacedBy(crossAxisSpacing.dp),
                        maxLines = maxLines,
                        overflow =
                            FlowColumnOverflow.expandIndicator {
                                Box(
                                    modifier =
                                        Modifier.fillMaxHeight(1f)
                                            .width(eachSize.dp)
                                            .clickable { maxLines += 2 }
                                            .onPlaced {
                                                seeMorePosition = it.positionInParent()
                                                seeMoreSize = it.size
                                            }
                                ) {}
                            },
                    ) {
                        repeat(totalCount) { index ->
                            Box(
                                Modifier.width(eachSize.dp).height(eachSize.dp).onPlaced {
                                    val positionInParent = it.positionInParent()
                                    positions.add(index, positionInParent)
                                }
                            ) {}
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        var expectedXPosition = 0
        var expectedYPosition = 0
        Truth.assertThat(positions.size).isEqualTo(5)
        positions.forEach { position ->
            Truth.assertThat(position.x).isEqualTo(expectedXPosition)

            Truth.assertThat(position.y).isEqualTo(expectedYPosition)
            expectedYPosition += eachSize + mainAxisSpacing
        }
        expectedXPosition += eachSize + crossAxisSpacing
        Truth.assertThat(seeMorePosition?.x).isEqualTo(expectedXPosition)
        Truth.assertThat(seeMorePosition?.y).isEqualTo(0)
        Truth.assertThat(seeMoreSize?.height).isEqualTo(320)
        Truth.assertThat(seeMoreSize?.width).isEqualTo(eachSize)
    }

    @Test
    fun testFlowRow_horizontalArrangementStart_MaxLines() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxLinesState = mutableStateOf(2)
        val minLinesToShowCollapseState = mutableStateOf(1)
        val minHeightToShowCollapseState = mutableStateOf(0.dp)
        val total = 20
        //  * Visually: 123####

        val xPositions = mutableListOf<Float>()
        var overflowState = mutableStateOf(FlowRowOverflow.Clip)
        var seeMoreOrCollapse: FlowRowOverflow? = null
        var seeMoreXPosition: Float? = null
        var collapseXPosition: Float? = null
        rule.setContent {
            var overflow by remember { overflowState }
            var maxLines by remember { maxLinesState }
            var minLinesToShowCollapse by remember { minLinesToShowCollapseState }
            var minHeightToShowCollapse by remember { minHeightToShowCollapseState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                seeMoreOrCollapse =
                    FlowRowOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            Box(
                                Modifier.size(20.dp).onGloballyPositioned {
                                    seeMoreXPosition = it.positionInParent().x
                                }
                            )
                        },
                        collapseIndicator = {
                            Box(
                                Modifier.size(20.dp).onGloballyPositioned {
                                    collapseXPosition = it.positionInParent().x
                                }
                            )
                        },
                        minLinesToShowCollapse,
                        minHeightToShowCollapse,
                    )
                Box(Modifier.size(200.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.Start,
                        maxItemsInEachRow = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                    ) {
                        repeat(total) { _ ->
                            Box(
                                Modifier.size(eachSize.dp).onGloballyPositioned {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions.add(xPosition)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsInMainAxis * maxLinesState.value)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            xPositions.clear()
            overflowState.value =
                FlowRowOverflow.expandIndicator {
                    Box(
                        Modifier.size(20.dp).onGloballyPositioned {
                            val positionInParent = it.positionInParent()
                            seeMoreXPosition = positionInParent.x
                        }
                    )
                }
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = min((maxItemsInMainAxis * maxLinesState.value) - 1, total)
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedXPosition)
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
            val maxItemsThatCanFit = min((maxItemsInMainAxis * maxLinesState.value) - 1, total)
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedXPosition)
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
            val maxItemsThatCanFit = min((maxItemsInMainAxis * maxLinesState.value) - 1, total)
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedXPosition)
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
            val maxItemsThatCanFit = min((maxItemsInMainAxis * maxLinesState.value), total)
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedXPosition)
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
    fun testFlowColumn_verticalArrangementStart_MaxLines() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxLinesState = mutableStateOf(2)
        val minLinesToShowCollapseState = mutableStateOf(1)
        val minHeightToShowCollapseState = mutableStateOf(0.dp)
        val total = 20
        //  * Visually: 123####

        val yPositions = mutableListOf<Float>()
        var overflowState = mutableStateOf(FlowColumnOverflow.Clip)
        var seeMoreOrCollapse: FlowColumnOverflow? = null
        var seeMoreYPosition: Float? = null
        var collapseYPosition: Float? = null
        rule.setContent {
            var overflow by remember { overflowState }
            var maxLines by remember { maxLinesState }
            var minLinesToShowCollapse by remember { minLinesToShowCollapseState }
            var minHeightToShowCollapse by remember { minHeightToShowCollapseState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                seeMoreOrCollapse =
                    FlowColumnOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            Box(
                                Modifier.size(20.dp).onGloballyPositioned {
                                    seeMoreYPosition = it.positionInParent().y
                                }
                            )
                        },
                        collapseIndicator = {
                            Box(
                                Modifier.size(20.dp).onGloballyPositioned {
                                    collapseYPosition = it.positionInParent().y
                                }
                            )
                        },
                        minLinesToShowCollapse,
                        minHeightToShowCollapse,
                    )
                Box(Modifier.size(200.dp)) {
                    FlowColumn(
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                    ) {
                        repeat(total) { _ ->
                            Box(
                                Modifier.size(eachSize.dp).onGloballyPositioned {
                                    val positionInParent = it.positionInParent()
                                    val yPosition = positionInParent.y
                                    yPositions.add(yPosition)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Assertions and interaction logic
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(yPositions.size).isEqualTo(maxItemsInMainAxis * maxLinesState.value)
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
            overflowState.value =
                FlowColumnOverflow.expandIndicator {
                    Box(
                        Modifier.size(20.dp).onGloballyPositioned {
                            val positionInParent = it.positionInParent()
                            seeMoreYPosition = positionInParent.y
                        }
                    )
                }
        }
        // Continuing from the previous logic
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = min((maxItemsInMainAxis * maxLinesState.value) - 1, total)
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
            val maxItemsThatCanFit = min((maxItemsInMainAxis * maxLinesState.value) - 1, total)
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
            val maxItemsThatCanFit = min((maxItemsInMainAxis * maxLinesState.value) - 1, total)
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
            val maxItemsThatCanFit = min((maxItemsInMainAxis * maxLinesState.value), total)
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
    fun testFlowRow_horizontalArrangementStart_MaxHeight() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxHeightState = mutableStateOf(40.dp)
        val minLinesToShowCollapseState = mutableStateOf(1)
        val minHeightToShowCollapseState = mutableStateOf(0.dp)
        val total = 20
        //  * Visually: 123####

        val xPositions = mutableListOf<Float>()
        var overflowState = mutableStateOf(FlowRowOverflow.Clip)
        var seeMoreOrCollapse: FlowRowOverflow? = null
        var seeMoreXPosition: Float? = null
        var collapseXPosition: Float? = null
        rule.setContent {
            var overflow by remember { overflowState }
            var maxHeight by remember { maxHeightState }
            var minLinesToShowCollapse by remember { minLinesToShowCollapseState }
            var minHeightToShowCollapse by remember { minHeightToShowCollapseState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                seeMoreOrCollapse =
                    FlowRowOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            Box(
                                Modifier.size(20.dp).onGloballyPositioned {
                                    seeMoreXPosition = it.positionInParent().x
                                }
                            )
                        },
                        collapseIndicator = {
                            Box(
                                Modifier.size(20.dp).onGloballyPositioned {
                                    collapseXPosition = it.positionInParent().x
                                }
                            )
                        },
                        minLinesToShowCollapse,
                        minHeightToShowCollapse,
                    )
                Box(Modifier.width(200.dp).height(maxHeight)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.Start,
                        maxItemsInEachRow = maxItemsInMainAxis,
                        overflow = overflow,
                    ) {
                        repeat(total) { _ ->
                            Box(
                                Modifier.size(eachSize.dp).onGloballyPositioned {
                                    val positionInParent = it.positionInParent()
                                    val xPosition = positionInParent.x
                                    xPositions.add(xPosition)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(xPositions.size).isEqualTo(10)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedXPosition)
                if ((index + 1) % maxItemsInMainAxis == 0) {
                    expectedXPosition = 0
                } else {
                    expectedXPosition += eachSize
                }
            }
            xPositions.clear()
            overflowState.value =
                FlowRowOverflow.expandIndicator {
                    Box(
                        Modifier.size(20.dp).onGloballyPositioned {
                            val positionInParent = it.positionInParent()
                            seeMoreXPosition = positionInParent.x
                        }
                    )
                }
        }
        advanceClock()
        rule.runOnIdle {
            val maxItemsThatCanFit = 9
            Truth.assertThat(xPositions.size).isEqualTo(maxItemsThatCanFit)
            var expectedXPosition = 0
            xPositions.forEachIndexed { index, position ->
                Truth.assertThat(position).isEqualTo(expectedXPosition)
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
                Truth.assertThat(position).isEqualTo(expectedXPosition)
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
                Truth.assertThat(position).isEqualTo(expectedXPosition)
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
                Truth.assertThat(position).isEqualTo(expectedXPosition)
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
                        maxItemsInEachRow = maxItemsInMainAxis,
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier.size(eachSize.toDp()).onPlaced {
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

            Truth.assertThat(position).isEqualTo(expectedXPosition)
        }
    }

    @Test
    fun testFlowColumn_verticalArrangementStart_MaxWidth() {
        val eachSize = 20
        val maxItemsInMainAxis = 5
        val maxWidthState = mutableStateOf(40.dp)
        val minLinesToShowCollapseState = mutableStateOf(1)
        val minHeightToShowCollapseState = mutableStateOf(0.dp)
        val total = 20
        //  * Visually: 123####

        val yPositions = mutableListOf<Float>()
        var overflowState = mutableStateOf(FlowColumnOverflow.Clip)
        var seeMoreOrCollapse: FlowColumnOverflow? = null
        var seeMoreYPosition: Float? = null
        var collapseYPosition: Float? = null
        rule.setContent {
            var overflow by remember { overflowState }
            var maxWidth by remember { maxWidthState }
            var minLinesToShowCollapse by remember { minLinesToShowCollapseState }
            var minHeightToShowCollapse by remember { minHeightToShowCollapseState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                seeMoreOrCollapse =
                    FlowColumnOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            Box(
                                Modifier.size(20.dp).onGloballyPositioned {
                                    seeMoreYPosition = it.positionInParent().y
                                }
                            )
                        },
                        collapseIndicator = {
                            Box(
                                Modifier.size(20.dp).onGloballyPositioned {
                                    collapseYPosition = it.positionInParent().y
                                }
                            )
                        },
                        minLinesToShowCollapse,
                        minHeightToShowCollapse,
                    )
                Box(Modifier.height(200.dp).width(maxWidth)) {
                    FlowColumn(
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        overflow = overflow,
                    ) {
                        repeat(total) { _ ->
                            Box(
                                Modifier.size(eachSize.dp).onGloballyPositioned {
                                    val positionInParent = it.positionInParent()
                                    val yPosition = positionInParent.y
                                    yPositions.add(yPosition)
                                }
                            )
                        }
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
            overflowState.value =
                FlowColumnOverflow.expandIndicator {
                    Box(
                        Modifier.size(20.dp).onGloballyPositioned {
                            val positionInParent = it.positionInParent()
                            seeMoreYPosition = positionInParent.y
                        }
                    )
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
     * Should space something like this: 1 2 3
     *
     * # SpaceAligned
     * 4 5 6 No Space here
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
                        Modifier.onSizeChanged { heightResult = it.height },
                        verticalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachRow = 1,
                    ) {
                        repeat(noOfItems) { index ->
                            Box(
                                Modifier.size(eachSize.toDp()).onPlaced {
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
        Truth.assertThat(heightResult).isEqualTo(expectedHeight)
        var expectedYPosition = 0
        yPositions.forEachIndexed { index, position ->
            Truth.assertThat(position).isEqualTo(expectedYPosition)
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
                        Modifier.onSizeChanged { widthResult = it.width },
                        horizontalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachColumn = 1,
                    ) {
                        repeat(noOfItems) { index ->
                            Box(
                                Modifier.size(eachSize.toDp()).onPlaced {
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
        Truth.assertThat(widthResult).isEqualTo(expectedWidth)
        xPositions.forEachIndexed { index, position ->
            Truth.assertThat(position).isEqualTo(expectedXPosition)
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
                        maxItemsInEachColumn = maxItemsInMainAxis,
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier.size(eachSize.toDp()).onPlaced {
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

            Truth.assertThat(position).isEqualTo(expectedYPosition)
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
                Box(Modifier.widthIn(30.toDp(), 40.toDp())) {
                    FlowRow(
                        Modifier.onSizeChanged { width = it.width },
                        horizontalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachRow = maxItemsInMainAxis,
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier.size(eachSize.toDp()).onPlaced {
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

            Truth.assertThat(position).isEqualTo(expectedXPosition)
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
                Box(Modifier.heightIn(30.toDp(), 40.toDp())) {
                    FlowColumn(
                        Modifier.onSizeChanged { height = it.height },
                        verticalArrangement = Arrangement.spacedBy(spaceAligned.toDp()),
                        maxItemsInEachColumn = maxItemsInMainAxis,
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier.size(eachSize.toDp()).onPlaced {
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

            Truth.assertThat(position).isEqualTo(expectedYPosition)
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
                        Modifier.fillMaxHeight(1f),
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachColumn = maxItemsInMainAxis,
                    ) {
                        repeat(10) { index ->
                            Box(
                                Modifier.size(20.toDp()).onPlaced {
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
            Truth.assertThat(position).isEqualTo(expectedYPosition)
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
                            Modifier.fillMaxWidth(1f),
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = maxItemsInMainAxis,
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier.size(eachSize.toDp()).onPlaced {
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
            Truth.assertThat(position).isEqualTo(expectedXPosition)
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
                            Modifier.fillMaxHeight(1f).fillMaxWidth(1f),
                            verticalArrangement = Arrangement.Top,
                            maxItemsInEachColumn = maxItemsInMainAxis,
                        ) {
                            repeat(10) { index ->
                                Box(
                                    Modifier.size(20.toDp()).onPlaced {
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
            Truth.assertThat(yPosition).isEqualTo(expectedYPosition)
            Truth.assertThat(xPosition).isEqualTo(expectedXPosition)
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
                            Modifier.fillMaxHeight(1f).onSizeChanged {
                                width = it.width
                                itemsThatCanFit = it.height / eachSize
                            },
                            verticalArrangement = Arrangement.Top,
                            maxItemsInEachColumn = maxItemsInMainAxis,
                        ) {
                            repeat(10) { index ->
                                Box(
                                    Modifier.size(20.toDp()).onPlaced {
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
            if (index % maxItemsInMainAxis == 0 || fittedItems == itemsThatCanFit) {
                expectedYPosition = 0
                expectedXPosition -= eachSize
                fittedItems = 0
            }
            Truth.assertThat(yPosition).isEqualTo(expectedYPosition)
            Truth.assertThat(xPosition).isEqualTo(expectedXPosition)
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
                        FlowRow(horizontalArrangement = Arrangement.Start, maxItemsInEachRow = 5) {
                            repeat(6) { index ->
                                Box(
                                    Modifier.size(20.toDp()).onPlaced {
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
            Truth.assertThat(position).isEqualTo(expectedXPosition)
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = 5,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_MaxItemsInRow_MaxLines() {
        var width = 0
        var height = 0
        var itemShown = 0
        val maxItemsInMainAxisState = mutableStateOf(2)
        val maxLinesState = mutableStateOf(1)
        val overflowState = mutableStateOf(FlowRowOverflow.Clip)
        var seeMoreOrCollapse: FlowRowOverflow = FlowRowOverflow.Clip
        var spacingState = mutableStateOf(0)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            var overflow by remember { overflowState }
            var maxItemsInMainAxis by remember { maxItemsInMainAxisState }
            var spacedBy by remember { spacingState }
            seeMoreOrCollapse =
                FlowRowOverflow.expandOrCollapseIndicator(
                    expandIndicator = { Box(Modifier.size(20.dp)) },
                    collapseIndicator = { Box(Modifier.size(20.dp)) },
                )
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.width(200.dp).wrapContentHeight()) {
                    FlowRow(
                        Modifier.width(IntrinsicSize.Min).onSizeChanged {
                            width = it.width
                            height = it.height
                        },
                        horizontalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Top),
                        maxItemsInEachRow = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                    ) {
                        repeat(6) { index ->
                            Box(Modifier.size(20.dp).onPlaced { itemShown = index + 1 })
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(2)
            overflowState.value = FlowRowOverflow.expandIndicator { Box(Modifier.size(20.dp)) {} }
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(1)
            maxLinesState.value = 3
            overflowState.value = seeMoreOrCollapse
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(5)
            maxLinesState.value = 4
            maxItemsInMainAxisState.value = 3
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(80)
            Truth.assertThat(itemShown).isEqualTo(6)
            spacingState.value = 20
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(height).isEqualTo(140)
            Truth.assertThat(itemShown).isEqualTo(6)
        }
    }

    @Test
    fun testFlowColumn_minIntrinsicHeight_MaxItemsInColumn_MaxLines() {
        var height = 0
        var width = 0
        var itemShown = 0
        val maxItemsInMainAxisState = mutableStateOf(2)
        val maxLinesState = mutableStateOf(1)
        val overflowState = mutableStateOf(FlowColumnOverflow.Clip)
        var seeMoreOrCollapse: FlowColumnOverflow = FlowColumnOverflow.Clip
        var spacingState = mutableStateOf(0)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            var overflow by remember { overflowState }
            var maxItemsInMainAxis by remember { maxItemsInMainAxisState }
            var spacedBy by remember { spacingState }
            seeMoreOrCollapse =
                FlowColumnOverflow.expandOrCollapseIndicator(
                    expandIndicator = { Box(Modifier.size(20.dp)) },
                    collapseIndicator = { Box(Modifier.size(20.dp)) {} },
                )
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.height(200.dp).wrapContentWidth()) {
                    FlowColumn(
                        Modifier.height(IntrinsicSize.Min).onSizeChanged {
                            height = it.height
                            width = it.width
                        },
                        verticalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Top),
                        horizontalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Start),
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                    ) {
                        repeat(6) { index ->
                            Box(Modifier.size(20.dp).onPlaced { itemShown = index + 1 })
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(2)
            overflowState.value =
                FlowColumnOverflow.expandIndicator { Box(Modifier.size(20.dp)) {} }
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(1)
            maxLinesState.value = 3
            overflowState.value = seeMoreOrCollapse
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(5)
            maxLinesState.value = 4
            maxItemsInMainAxisState.value = 3
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(80)
            Truth.assertThat(itemShown).isEqualTo(6)
            spacingState.value = 20
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(width).isEqualTo(140)
            Truth.assertThat(itemShown).isEqualTo(6)
        }
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_MaxLines_SeeMoreOrCollapse_MinToShow() {
        var width = 0
        var height = 0
        var itemShown = 0
        val maxItemsInMainAxisState = mutableStateOf(2)
        val maxLinesState = mutableStateOf(4)
        val overflowState = mutableStateOf(FlowRowOverflow.Clip)
        var minLinesToShowCollapseState = mutableStateOf(4)
        var minHeightToShowCollapseState = mutableStateOf(0.dp)
        var spacingState = mutableStateOf(0)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            var maxItemsInMainAxis by remember { maxItemsInMainAxisState }
            var minLinesToShowCollapse by remember { minLinesToShowCollapseState }
            var minHeightToShowCollapse by remember { minHeightToShowCollapseState }
            var spacedBy by remember { spacingState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                overflowState.value =
                    FlowRowOverflow.expandOrCollapseIndicator(
                        expandIndicator = { Box(Modifier.size(20.dp)) },
                        collapseIndicator = { Box(Modifier.size(20.dp)) {} },
                        minLinesToShowCollapse,
                        minHeightToShowCollapse,
                    )
                var overflow by remember { overflowState }
                Box(Modifier.width(200.dp).wrapContentHeight()) {
                    FlowRow(
                        Modifier.width(IntrinsicSize.Min).onSizeChanged {
                            width = it.width
                            height = it.height
                        },
                        horizontalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Top),
                        maxItemsInEachRow = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                    ) {
                        repeat(6) { index ->
                            Box(Modifier.size(20.dp).onPlaced { itemShown = index + 1 })
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(6)
            minLinesToShowCollapseState.value = 3
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(80)
            Truth.assertThat(itemShown).isEqualTo(6)
            minHeightToShowCollapseState.value = 100.dp
            maxLinesState.value = 3
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(6)
            spacingState.value = 20
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(height).isEqualTo(100)
            Truth.assertThat(itemShown).isEqualTo(5)
            minHeightToShowCollapseState.value = 120.dp
        }
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(height).isEqualTo(100)
            Truth.assertThat(itemShown).isEqualTo(6)
            minHeightToShowCollapseState.value = 100.dp
            minLinesToShowCollapseState.value = 4
        }
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(height).isEqualTo(100)
            Truth.assertThat(itemShown).isEqualTo(6)
        }
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_MaxLines() {
        var width = 0
        val maxLinesState = mutableStateOf(1)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                with(LocalDensity.current) {
                    Box(Modifier.width(200.dp).wrapContentHeight()) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = 6,
                            maxLines = maxLines,
                            overflow = FlowRowOverflow.Clip,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(120)
            maxLinesState.value = 2
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(60)
            maxLinesState.value = 4
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            maxLinesState.value = 6
        }
        advanceClock()
        rule.runOnIdle { Truth.assertThat(width).isEqualTo(20) }
    }

    @Test
    fun testFlowColumn_minIntrinsicHeight_MaxLines_SeeMoreOrCollapse_MinToShow() {
        var height = 0
        var width = 0
        var itemShown = 0
        val maxItemsInMainAxisState = mutableStateOf(2)
        val maxLinesState = mutableStateOf(4)
        val overflowState = mutableStateOf(FlowColumnOverflow.Clip)
        var minLinesToShowCollapseState = mutableStateOf(4)
        var minWidthToShowCollapseState = mutableStateOf(0.dp)
        var spacingState = mutableStateOf(0)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            var maxItemsInMainAxis by remember { maxItemsInMainAxisState }
            var minLinesToShowCollapse by remember { minLinesToShowCollapseState }
            var minWidthToShowCollapse by remember { minWidthToShowCollapseState }
            var spacedBy by remember { spacingState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                overflowState.value =
                    FlowColumnOverflow.expandOrCollapseIndicator(
                        expandIndicator = { Box(Modifier.size(20.dp)) },
                        collapseIndicator = { Box(Modifier.size(20.dp)) {} },
                        minLinesToShowCollapse,
                        minWidthToShowCollapse,
                    )
                var overflow by remember { overflowState }
                Box(Modifier.height(200.dp).wrapContentWidth()) {
                    FlowColumn(
                        Modifier.height(IntrinsicSize.Min).onSizeChanged {
                            height = it.height
                            width = it.width
                        },
                        verticalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Top),
                        horizontalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Start),
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                    ) {
                        repeat(6) { index ->
                            Box(Modifier.size(20.dp).onPlaced { itemShown = index + 1 })
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(6)
            minLinesToShowCollapseState.value = 3
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(80)
            Truth.assertThat(itemShown).isEqualTo(6)
            minWidthToShowCollapseState.value = 100.dp
            maxLinesState.value = 3
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(6)
            spacingState.value = 20
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(width).isEqualTo(100)
            Truth.assertThat(itemShown).isEqualTo(5)
            minWidthToShowCollapseState.value = 120.dp
        }
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(width).isEqualTo(100)
            Truth.assertThat(itemShown).isEqualTo(6)
            minWidthToShowCollapseState.value = 100.dp
            minLinesToShowCollapseState.value = 4
        }
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(width).isEqualTo(100)
            Truth.assertThat(itemShown).isEqualTo(6)
        }
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_MaxLinesWithSpacedBy() {
        var width = 0
        val maxLinesState = mutableStateOf(1)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.width(250.dp).wrapContentHeight()) {
                    FlowRow(
                        Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        maxItemsInEachRow = 6,
                        maxLines = maxLines,
                        overflow = FlowRowOverflow.Clip,
                    ) {
                        repeat(6) { Box(Modifier.size(20.dp)) }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(220)
            maxLinesState.value = 2
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(100)
            maxLinesState.value = 4
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(60)
            maxLinesState.value = 6
        }
        advanceClock()
        rule.runOnIdle { Truth.assertThat(width).isEqualTo(20) }
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_MaxLines_SeeMore() {
        var width = 0
        val maxLinesState = mutableStateOf(1)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.width(200.dp).wrapContentHeight()) {
                    FlowRow(
                        Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                        horizontalArrangement = Arrangement.Start,
                        maxItemsInEachRow = 6,
                        maxLines = maxLines,
                        overflow = FlowRowOverflow.expandIndicator { Box(Modifier.size(20.dp)) },
                    ) {
                        repeat(6) { Box(Modifier.size(20.dp)) }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(120)
            maxLinesState.value = 2
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(60)
            maxLinesState.value = 4
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            maxLinesState.value = 6
        }
        advanceClock()
        rule.runOnIdle { Truth.assertThat(width).isEqualTo(20) }
    }

    @Test
    fun testFlowColumn_minIntrinsicHeight_MaxLines_SeeMore() {
        var height = 0
        val maxLinesState = mutableStateOf(1)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.height(200.dp).wrapContentWidth()) {
                    FlowColumn(
                        Modifier.height(IntrinsicSize.Min).onSizeChanged { height = it.height },
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachColumn = 6,
                        maxLines = maxLines,
                        overflow = FlowColumnOverflow.expandIndicator { Box(Modifier.size(20.dp)) },
                    ) {
                        repeat(6) { Box(Modifier.size(20.dp)) }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(120)
            maxLinesState.value = 2
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(60)
            maxLinesState.value = 4
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            maxLinesState.value = 6
        }
        advanceClock()
        rule.runOnIdle { Truth.assertThat(height).isEqualTo(20) }
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_MaxLines_SeeMore_SpacedBy() {
        var width = 0
        val maxLinesState = mutableStateOf(1)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.width(250.dp).wrapContentHeight()) {
                    FlowRow(
                        Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        maxItemsInEachRow = 6,
                        maxLines = maxLines,
                        overflow = FlowRowOverflow.expandIndicator { Box(Modifier.size(20.dp)) },
                    ) {
                        repeat(6) { Box(Modifier.size(20.dp)) }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(220)
            maxLinesState.value = 2
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(100)
            maxLinesState.value = 4
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(60)
            maxLinesState.value = 6
        }
        advanceClock()
        rule.runOnIdle { Truth.assertThat(width).isEqualTo(20) }
    }

    @Test
    fun testFlowColumn_minIntrinsicHeight_MaxLines_SeeMore_SpacedBy() {
        var height = 0
        val maxLinesState = mutableStateOf(1)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.height(250.dp).wrapContentWidth()) {
                    FlowColumn(
                        Modifier.height(IntrinsicSize.Min).onSizeChanged { height = it.height },
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        maxItemsInEachColumn = 6,
                        maxLines = maxLines,
                        overflow = FlowColumnOverflow.expandIndicator { Box(Modifier.size(20.dp)) },
                    ) {
                        repeat(6) { Box(Modifier.size(20.dp)) }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(220)
            maxLinesState.value = 2
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(100)
            maxLinesState.value = 4
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(60)
            maxLinesState.value = 6
        }
        advanceClock()
        rule.runOnIdle { Truth.assertThat(height).isEqualTo(20) }
    }

    @Test
    fun testFlowRow_minIntrinsicWidth_MaxLines_SeeMoreOrCollapse() {
        var width = 0
        var height = 0
        val maxLinesState = mutableStateOf(1)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides NoOpDensity,
            ) {
                with(LocalDensity.current) {
                    Box(Modifier.width(200.dp).wrapContentHeight()) {
                        FlowRow(
                            Modifier.width(IntrinsicSize.Min).onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            horizontalArrangement = Arrangement.Start,
                            maxItemsInEachRow = 6,
                            maxLines = maxLines,
                            overflow =
                                FlowRowOverflow.expandOrCollapseIndicator(
                                    expandIndicator = { Box(Modifier.size(20.dp)) },
                                    collapseIndicator = { Box(Modifier.size(20.dp)) },
                                    minRowsToShowCollapse = 2,
                                ),
                        ) {
                            repeat(6) { Box(Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(120)
            Truth.assertThat(height).isEqualTo(20)
            maxLinesState.value = 2
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(80)
            Truth.assertThat(height).isEqualTo(40)
            maxLinesState.value = 4
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(80)
            maxLinesState.value = 6
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(80)
        }
    }

    @Test
    fun testFlowColumn_minIntrinsicHeight_MaxLines_SeeMoreOrCollapse() {
        var height = 0
        var width = 0
        val maxLinesState = mutableStateOf(1)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides NoOpDensity,
            ) {
                Box(Modifier.height(200.dp).wrapContentWidth()) {
                    FlowColumn(
                        Modifier.height(IntrinsicSize.Min).onSizeChanged {
                            height = it.height
                            width = it.width
                        },
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachColumn = 6,
                        maxLines = maxLines,
                        overflow =
                            FlowColumnOverflow.expandOrCollapseIndicator(
                                expandIndicator = { Box(Modifier.size(20.dp)) },
                                collapseIndicator = { Box(Modifier.size(20.dp)) {} },
                                minColumnsToShowCollapse = 2,
                            ),
                    ) {
                        repeat(6) { Box(Modifier.size(20.dp)) }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(120)
            Truth.assertThat(width).isEqualTo(20)
            maxLinesState.value = 2
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(80)
            Truth.assertThat(width).isEqualTo(40)
            maxLinesState.value = 4
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(80)
            maxLinesState.value = 6
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(80)
        }
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                            maxItemsInEachColumn = 6,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                            maxItemsInEachColumn = 5,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Max).onSizeChanged { width = it.width },
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Max).onSizeChanged { width = it.width }
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                            horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                            maxItemsInEachRow = 5,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                            horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                            maxItemsInEachColumn = 5,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                            horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                            maxItemsInEachColumn = 5,
                        ) {
                            repeat(5) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Max).onSizeChanged { width = it.width },
                            horizontalArrangement = Arrangement.spacedBy(10.toDp()),
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Max).onSizeChanged { width = it.width },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            maxItemsInEachRow = 5,
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier.width(if (index == 5) 100.toDp() else 20.toDp())
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            maxItemsInEachColumn = 5,
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier.height(if (index == 5) 100.toDp() else 20.toDp())
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            horizontalArrangement = Arrangement.spacedBy(10.toDp()),
                            maxItemsInEachRow = 5,
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier.width(if (index == 5) 100.toDp() else 20.toDp())
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            verticalArrangement = Arrangement.spacedBy(10.toDp()),
                            maxItemsInEachColumn = 5,
                        ) {
                            repeat(6) { index ->
                                Box(
                                    Modifier.width(if (index == 5) 100.toDp() else 20.toDp())
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                            maxItemsInEachRow = 2,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged { width = it.width },
                            maxItemsInEachRow = 2,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Max).onSizeChanged { width = it.width },
                            maxItemsInEachRow = 2,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Max).onSizeChanged { width = it.width },
                            maxItemsInEachColumn = 2,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            maxItemsInEachRow = 2,
                        ) {
                            repeat(10) { index ->
                                Box(
                                    Modifier.width(if (index == 5) 100.toDp() else 20.toDp())
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
                            Modifier.width(IntrinsicSize.Min).onSizeChanged {
                                width = it.width
                                height = it.height
                            },
                            maxItemsInEachColumn = 2,
                        ) {
                            repeat(10) { index ->
                                Box(
                                    Modifier.width(if (index == 5) 100.toDp() else 20.toDp())
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
                            Modifier.height(IntrinsicSize.Min).onSizeChanged { height = it.height }
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(20)
    }

    @Test
    fun testFlowRow_minIntrinsicHeight_MaxItemsInRow_MaxLines() {
        var width = 0
        var height = 0
        var itemShown = 0
        val maxItemsInMainAxisState = mutableStateOf(2)
        val maxLinesState = mutableStateOf(1)
        val overflowState = mutableStateOf(FlowRowOverflow.Clip)
        var seeMoreOrCollapse: FlowRowOverflow? = null
        var spacingState = mutableStateOf(0)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            var overflow by remember { overflowState }
            var maxItemsInMainAxis by remember { maxItemsInMainAxisState }
            var spacedBy by remember { spacingState }
            seeMoreOrCollapse =
                FlowRowOverflow.expandOrCollapseIndicator(
                    expandIndicator = { Box(Modifier.size(20.dp)) },
                    collapseIndicator = { Box(Modifier.size(20.dp)) {} },
                )
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlowRow(
                        Modifier.height(IntrinsicSize.Min).onSizeChanged {
                            height = it.height
                            width = it.width
                        },
                        horizontalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Top),
                        maxItemsInEachRow = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                    ) {
                        repeat(6) { index ->
                            Box(Modifier.size(20.dp).onPlaced { itemShown = index + 1 })
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(2)
            overflowState.value = FlowRowOverflow.expandIndicator { Box(Modifier.size(20.dp)) {} }
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(1)
            maxLinesState.value = 3
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(5)
            maxLinesState.value = 4
            maxItemsInMainAxisState.value = 3
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(6)
            spacingState.value = 20
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(100)
            Truth.assertThat(height).isEqualTo(100)
            Truth.assertThat(itemShown).isEqualTo(6)
        }
    }

    @Test
    fun testFlowColumn_minIntrinsicWidth_MaxItemsInColumn_MaxLines() {
        var height = 0
        var width = 0
        var itemShown = 0
        val maxItemsInMainAxisState = mutableStateOf(2)
        val maxLinesState = mutableStateOf(1)
        val overflowState = mutableStateOf(FlowColumnOverflow.Clip)
        var seeMoreOrCollapse: FlowColumnOverflow? = null
        var spacingState = mutableStateOf(0)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            var overflow by remember { overflowState }
            var maxItemsInMainAxis by remember { maxItemsInMainAxisState }
            var spacedBy by remember { spacingState }
            seeMoreOrCollapse =
                FlowColumnOverflow.expandOrCollapseIndicator(
                    expandIndicator = { Box(Modifier.size(20.dp)) },
                    collapseIndicator = { Box(Modifier.size(20.dp)) {} },
                )
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlowColumn(
                        Modifier.width(IntrinsicSize.Min).onSizeChanged {
                            width = it.width
                            height = it.height
                        },
                        verticalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Top),
                        horizontalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Start),
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                    ) {
                        repeat(6) { index ->
                            Box(Modifier.size(20.dp).onGloballyPositioned { itemShown = index + 1 })
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(2)
            overflowState.value =
                FlowColumnOverflow.expandIndicator { Box(Modifier.size(20.dp)) {} }
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(1)
            maxLinesState.value = 3
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(5)
            maxLinesState.value = 4
            maxItemsInMainAxisState.value = 3
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(6)
            spacingState.value = 20
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(100)
            Truth.assertThat(width).isEqualTo(100)
            Truth.assertThat(itemShown).isEqualTo(6)
        }
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
                            Modifier.width(IntrinsicSize.Min)
                                .height(IntrinsicSize.Max)
                                .onSizeChanged { height = it.height }
                        ) {
                            repeat(5) { Box(Modifier.size(20.toDp())) }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(100)
    }

    @Test
    fun testFlowRow_maxIntrinsicHeight_MaxItemsInRow_MaxLines() {
        var width = 0
        var height = 0
        var itemShown = 0
        val maxItemsInMainAxisState = mutableStateOf(2)
        val maxLinesState = mutableStateOf(1)
        val overflowState = mutableStateOf(FlowRowOverflow.Clip)
        var seeMoreOrCollapse: FlowRowOverflow? = null
        var spacingState = mutableStateOf(0)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            var overflow by remember { overflowState }
            var maxItemsInMainAxis by remember { maxItemsInMainAxisState }
            var spacedBy by remember { spacingState }
            seeMoreOrCollapse =
                FlowRowOverflow.expandOrCollapseIndicator(
                    expandIndicator = { Box(Modifier.size(20.dp)) },
                    collapseIndicator = { Box(Modifier.size(20.dp)) {} },
                )
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlowRow(
                        Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Max).onSizeChanged {
                            height = it.height
                            width = it.width
                        },
                        horizontalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Top),
                        maxItemsInEachRow = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                    ) {
                        repeat(6) { index ->
                            Box(Modifier.size(20.dp).onPlaced { itemShown = index + 1 })
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(2)
            overflowState.value = FlowRowOverflow.expandIndicator { Box(Modifier.size(20.dp)) {} }
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(1)
            maxLinesState.value = 3
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(5)
            maxLinesState.value = 4
            maxItemsInMainAxisState.value = 3
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(height).isEqualTo(80)
            Truth.assertThat(itemShown).isEqualTo(6)
            spacingState.value = 20
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(height).isEqualTo(140)
            Truth.assertThat(itemShown).isEqualTo(6)
        }
    }

    @Test
    fun testFlowColumn_maxIntrinsicWidth_MaxItemsInColumn_MaxLines() {
        var height = 0
        var width = 0
        var itemShown = 0
        val maxItemsInMainAxisState = mutableStateOf(2)
        val maxLinesState = mutableStateOf(1)
        val overflowState = mutableStateOf(FlowColumnOverflow.Clip)
        var seeMoreOrCollapse: FlowColumnOverflow? = null
        var spacingState = mutableStateOf(0)
        rule.setContent {
            var maxLines by remember { maxLinesState }
            var overflow by remember { overflowState }
            var maxItemsInMainAxis by remember { maxItemsInMainAxisState }
            var spacedBy by remember { spacingState }
            seeMoreOrCollapse =
                FlowColumnOverflow.expandOrCollapseIndicator(
                    expandIndicator = { Box(Modifier.size(20.dp)) },
                    collapseIndicator = { Box(Modifier.size(20.dp)) {} },
                )
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlowColumn(
                        Modifier.height(IntrinsicSize.Min).width(IntrinsicSize.Max).onSizeChanged {
                            width = it.width
                            height = it.height
                        },
                        verticalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Top),
                        horizontalArrangement = Arrangement.spacedBy(spacedBy.dp, Alignment.Start),
                        maxItemsInEachColumn = maxItemsInMainAxis,
                        maxLines = maxLines,
                        overflow = overflow,
                    ) {
                        repeat(6) { index ->
                            Box(Modifier.size(20.dp).onPlaced { itemShown = index + 1 })
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(2)
            overflowState.value =
                FlowColumnOverflow.expandIndicator { Box(Modifier.size(20.dp)) {} }
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(20)
            Truth.assertThat(itemShown).isEqualTo(1)
            maxLinesState.value = 3
            overflowState.value = seeMoreOrCollapse!!
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(60)
            Truth.assertThat(itemShown).isEqualTo(5)
            maxLinesState.value = 4
            maxItemsInMainAxisState.value = 3
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(width).isEqualTo(80)
            Truth.assertThat(itemShown).isEqualTo(6)
            spacingState.value = 20
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(60)
            Truth.assertThat(width).isEqualTo(140)
            Truth.assertThat(itemShown).isEqualTo(6)
        }
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
                            Modifier.width(IntrinsicSize.Min)
                                .height(IntrinsicSize.Max)
                                .onSizeChanged { height = it.height },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(5) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.height(IntrinsicSize.Min).onSizeChanged { height = it.height },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                            maxItemsInEachRow = 1,
                        ) {
                            repeat(2) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.height(IntrinsicSize.Min).onSizeChanged { height = it.height },
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(2) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.height(IntrinsicSize.Min).onSizeChanged { height = it.height },
                            maxItemsInEachColumn = 5,
                        ) {
                            repeat(6) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.height(IntrinsicSize.Max).onSizeChanged { height = it.height },
                            maxItemsInEachColumn = 5,
                            horizontalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(5) { Box(Modifier.size(20.toDp())) }
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
                            Modifier.height(IntrinsicSize.Max).onSizeChanged { height = it.height },
                            maxItemsInEachColumn = 5,
                            verticalArrangement = Arrangement.spacedBy(20.toDp()),
                        ) {
                            repeat(5) { Box(Modifier.size(20.toDp())) }
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
        var noOfItemsPlaced = 0
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlowRow(
                        Modifier.fillMaxWidth(1f).onSizeChanged { width = it.width },
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        overflow = FlowRowOverflow.Clip,
                    ) {
                        repeat(2) { index ->
                            Layout(
                                modifier =
                                    Modifier.requiredSize(250.dp).onPlaced {
                                        noOfItemsPlaced = index + 1
                                    }
                            ) { _, _ ->
                                layout(250, 250) {}
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(200)
        Truth.assertThat(noOfItemsPlaced).isEqualTo(0)
    }

    @Test
    fun testFlowRow_reuseMeasurePolicy() {
        val maxItemsInMainAxis = 5
        val maxLinesState = mutableStateOf(2)

        var overflow = mutableStateOf(FlowRowOverflow.expandIndicator {})
        var seeMoreOrCollapse: FlowRowOverflow? = null
        var seeMoreTwo: FlowRowOverflow? = null
        var measurePolicy: MultiContentMeasurePolicy? = null
        var previousMeasurePolicy: MultiContentMeasurePolicy? = null
        rule.setContent {
            previousMeasurePolicy = measurePolicy
            val minLinesToShowCollapseState = 1
            val minHeightToShowCollapseState = 0.dp
            seeMoreOrCollapse =
                FlowRowOverflow.expandOrCollapseIndicator(
                    {},
                    {},
                    minLinesToShowCollapseState,
                    minHeightToShowCollapseState,
                )
            seeMoreTwo =
                FlowRowOverflow.expandOrCollapseIndicator(
                    {},
                    {},
                    minLinesToShowCollapseState,
                    minHeightToShowCollapseState,
                )
            var overflowState = remember(overflow.value) { overflow.value.createOverflowState() }
            var maxLines by remember { maxLinesState }
            measurePolicy =
                rowMeasurementMultiContentHelper(
                    verticalArrangement = Arrangement.Top,
                    horizontalArrangement = Arrangement.Start,
                    itemVerticalAlignment = Alignment.Top,
                    maxItemsInMainAxis = maxItemsInMainAxis,
                    maxLines = maxLines,
                    overflowState = overflowState,
                )
        }

        rule.runOnIdle {
            overflow.value = seeMoreOrCollapse!!
            maxLinesState.value = 2
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(previousMeasurePolicy).isNotEqualTo(measurePolicy)
            overflow.value = seeMoreTwo!!
            maxLinesState.value = 2
        }
        rule.waitForIdle()
        Truth.assertThat(previousMeasurePolicy).isSameInstanceAs(measurePolicy)
    }

    @Test
    fun testFlowRow_measurePolicy_Identical() {
        val maxItemsInMainAxis = 5
        val maxLines = 2

        var measurePolicy: MultiContentMeasurePolicy? = null
        var previousMeasurePolicy: MultiContentMeasurePolicy? = null
        rule.setContent {
            previousMeasurePolicy =
                rowMeasurementMultiContentHelper(
                    verticalArrangement = Arrangement.Top,
                    horizontalArrangement = Arrangement.Start,
                    itemVerticalAlignment = Alignment.Top,
                    maxItemsInMainAxis = maxItemsInMainAxis,
                    maxLines = maxLines,
                    overflowState = FlowRowOverflow.expandIndicator {}.createOverflowState(),
                )

            measurePolicy =
                rowMeasurementMultiContentHelper(
                    verticalArrangement = Arrangement.Top,
                    horizontalArrangement = Arrangement.Start,
                    itemVerticalAlignment = Alignment.Top,
                    maxItemsInMainAxis = maxItemsInMainAxis,
                    maxLines = maxLines,
                    overflowState = FlowRowOverflow.expandIndicator {}.createOverflowState(),
                )
        }

        rule.waitForIdle()
        Truth.assertThat(previousMeasurePolicy).isNotSameInstanceAs(measurePolicy)
        Truth.assertThat(previousMeasurePolicy).isEqualTo(measurePolicy)
    }

    @Test
    fun testFlowColumn_reuseMeasurePolicy() {
        val maxItemsInMainAxis = 5
        val maxLinesState = mutableStateOf(2)

        var overflow = mutableStateOf(FlowColumnOverflow.expandIndicator {})
        var seeMoreOrCollapse: FlowColumnOverflow? = null
        var seeMoreTwo: FlowColumnOverflow? = null
        var measurePolicy: MultiContentMeasurePolicy? = null
        var previousMeasurePolicy: MultiContentMeasurePolicy? = null
        rule.setContent {
            previousMeasurePolicy = measurePolicy
            val minLinesToShowCollapseState = 1
            val minWidthToShowCollapseState = 0.dp
            seeMoreOrCollapse =
                FlowColumnOverflow.expandOrCollapseIndicator(
                    {},
                    {},
                    minLinesToShowCollapseState,
                    minWidthToShowCollapseState,
                )
            seeMoreTwo =
                FlowColumnOverflow.expandOrCollapseIndicator(
                    {},
                    {},
                    minLinesToShowCollapseState,
                    minWidthToShowCollapseState,
                )
            var overflowState = remember(overflow.value) { overflow.value.createOverflowState() }
            var maxLines by remember { maxLinesState }
            measurePolicy =
                columnMeasurementMultiContentHelper(
                    verticalArrangement = Arrangement.Top,
                    horizontalArrangement = Arrangement.Start,
                    itemHorizontalAlignment = Alignment.Start,
                    maxItemsInMainAxis = maxItemsInMainAxis,
                    maxLines = maxLines,
                    overflowState = overflowState,
                )
        }

        rule.runOnIdle {
            overflow.value = seeMoreOrCollapse!!
            maxLinesState.value = 2
        }
        advanceClock()
        rule.runOnIdle {
            Truth.assertThat(previousMeasurePolicy).isNotEqualTo(measurePolicy)
            overflow.value = seeMoreTwo!!
            maxLinesState.value = 2
        }
        rule.waitForIdle()
        Truth.assertThat(previousMeasurePolicy).isSameInstanceAs(measurePolicy)
    }

    @Test
    fun testFlowColumn_constrainsOverflow() {
        var height = 0
        var noOfItemsPlaced = 0
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlowColumn(
                        Modifier.fillMaxHeight(1f).onSizeChanged { height = it.height },
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        overflow = FlowColumnOverflow.Clip,
                    ) {
                        repeat(2) { index ->
                            Layout(
                                modifier =
                                    Modifier.requiredSize(250.dp).onPlaced {
                                        noOfItemsPlaced = index + 1
                                    }
                            ) { _, _ ->
                                layout(250, 250) {}
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        Truth.assertThat(height).isEqualTo(200)
        Truth.assertThat(noOfItemsPlaced).isEqualTo(0)
    }

    @Test
    fun testFlowColumn_crossAxisPositioning_withWeightAndAspectRatio() {
        val positionsInParentX = mutableFloatListOf()

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.height(50.toDp())) {
                    FlowColumn(
                        maxItemsInEachColumn = 2,
                        horizontalArrangement = Arrangement.spacedBy(10.toDp()),
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier.weight(1f).aspectRatio(1f, true).onPlaced {
                                    positionsInParentX.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()

        // Cross axis offset for first column
        Truth.assertThat(positionsInParentX[0]).isEqualTo(0)
        Truth.assertThat(positionsInParentX[1]).isEqualTo(0)

        // Cross axis offset for second column
        Truth.assertThat(positionsInParentX[2]).isEqualTo(35)
        Truth.assertThat(positionsInParentX[3]).isEqualTo(35)

        // Cross axis offset for third column
        Truth.assertThat(positionsInParentX[4]).isEqualTo(70)
    }

    @Test
    fun testFlowRow_crossAxisPositioning_withWeightAndAspectRatio() {
        val positionsInParentY = mutableFloatListOf()

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.width(50.toDp())) {
                    FlowRow(
                        maxItemsInEachRow = 2,
                        verticalArrangement = Arrangement.spacedBy(10.toDp()),
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier.weight(1f).aspectRatio(1f, true).onPlaced {
                                    positionsInParentY.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()

        // Cross axis offset for first column
        Truth.assertThat(positionsInParentY[0]).isEqualTo(0)
        Truth.assertThat(positionsInParentY[1]).isEqualTo(0)

        // Cross axis offset for second column
        Truth.assertThat(positionsInParentY[2]).isEqualTo(35)
        Truth.assertThat(positionsInParentY[3]).isEqualTo(35)

        // Cross axis offset for third column
        Truth.assertThat(positionsInParentY[4]).isEqualTo(70)
    }
}

internal val NoOpDensity =
    object : Density {
        override val density = 1f
        override val fontScale = 1f
    }
