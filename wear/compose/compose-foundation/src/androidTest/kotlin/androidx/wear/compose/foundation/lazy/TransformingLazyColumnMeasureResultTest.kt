/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnMeasureResultTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun measureResult_viewportSize_delegatesToMeasureResultDimensions() {
        val measureResult =
            createMeasureResult(
                width = 240,
                height = 360,
            )

        val viewportSize = measureResult.viewportSize

        assertThat(viewportSize).isEqualTo(IntSize(width = 240, height = 360))
    }

    // Verifies delegation of placeChildren and alignmentLines from MeasureResult.
    // getAlignmentLines is not queried by Compose NodeCoordinator unless items define alignment
    // lines.
    @Test
    fun measureResult_measureResultDelegation_delegatesPlaceChildrenAndAlignmentLines() {
        var placeChildrenCalled = false
        val testLine = HorizontalAlignmentLine { _, _ -> 0 }
        val alignmentLines = mapOf<AlignmentLine, Int>(testLine to 42)
        val measureResult =
            createMeasureResult(
                alignmentLines = alignmentLines,
                onPlaceChildren = { placeChildrenCalled = true },
            )

        measureResult.placeChildren()
        val retrievedLines = measureResult.alignmentLines

        assertThat(placeChildrenCalled).isTrue()
        assertThat(retrievedLines).isEqualTo(alignmentLines)
    }

    @Test
    fun checkLayoutIsCorrect_emptyVisibleItems_succeedsWithoutException() {
        val measureResult = createMeasureResult(visibleItems = emptyList())

        measureResult.checkLayoutIsCorrect()

        assertThat(measureResult.visibleItems).isEmpty()
    }

    @Test
    fun checkLayoutIsCorrect_singleItem_succeedsWithoutException() {
        val item =
            createTestItem(
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.1f,
                bottomOffsetFraction = 0.6f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item))

        measureResult.checkLayoutIsCorrect()

        assertThat(measureResult.visibleItems).hasSize(1)
    }

    @Test
    fun checkLayoutIsCorrect_flatHeightRates_succeedsWithoutException() {
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.1f,
                bottomOffsetFraction = 0.4f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.4f,
                bottomOffsetFraction = 0.7f,
            )
        val item2 =
            createTestItem(
                index = 2,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.7f,
                bottomOffsetFraction = 1.0f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1, item2))

        measureResult.checkLayoutIsCorrect()

        assertThat(measureResult.visibleItems).hasSize(3)
    }

    @Test
    fun checkLayoutIsCorrect_strictlyIncreasingHeightRates_succeedsWithoutException() {
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 60,
                topOffsetFraction = 0.1f,
                bottomOffsetFraction = 0.4f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 80,
                topOffsetFraction = 0.4f,
                bottomOffsetFraction = 0.7f,
            )
        val item2 =
            createTestItem(
                index = 2,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.7f,
                bottomOffsetFraction = 1.0f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1, item2))

        measureResult.checkLayoutIsCorrect()

        assertThat(measureResult.visibleItems).hasSize(3)
    }

    @Test
    fun checkLayoutIsCorrect_strictlyDecreasingHeightRates_succeedsWithoutException() {
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.1f,
                bottomOffsetFraction = 0.4f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 80,
                topOffsetFraction = 0.4f,
                bottomOffsetFraction = 0.7f,
            )
        val item2 =
            createTestItem(
                index = 2,
                measuredHeight = 100,
                transformedHeight = 60,
                topOffsetFraction = 0.7f,
                bottomOffsetFraction = 1.0f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1, item2))

        measureResult.checkLayoutIsCorrect()

        assertThat(measureResult.visibleItems).hasSize(3)
    }

    @Test
    fun checkLayoutIsCorrect_increasingThenDecreasingHeightRates_succeedsWithoutException() {
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 70,
                topOffsetFraction = 0.1f,
                bottomOffsetFraction = 0.4f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.4f,
                bottomOffsetFraction = 0.7f,
            )
        val item2 =
            createTestItem(
                index = 2,
                measuredHeight = 100,
                transformedHeight = 70,
                topOffsetFraction = 0.7f,
                bottomOffsetFraction = 1.0f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1, item2))

        measureResult.checkLayoutIsCorrect()

        assertThat(measureResult.visibleItems).hasSize(3)
    }

    @Test
    fun checkLayoutIsCorrect_decreasingThenIncreasingHeightRates_throwsException() {
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.1f,
                bottomOffsetFraction = 0.4f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 60,
                topOffsetFraction = 0.4f,
                bottomOffsetFraction = 0.7f,
            )
        val item2 =
            createTestItem(
                index = 2,
                measuredHeight = 100,
                transformedHeight = 90,
                topOffsetFraction = 0.7f,
                bottomOffsetFraction = 1.0f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1, item2))

        val exception =
            assertFailsWith<IllegalStateException> { measureResult.checkLayoutIsCorrect() }

        assertThat(exception.message)
            .contains("Incorrect layout: Measured items height rates are not correct")
    }

    @Test
    fun checkLayoutIsCorrect_decreasingThenFlatHeightRates_throwsException() {
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.1f,
                bottomOffsetFraction = 0.4f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 60,
                topOffsetFraction = 0.4f,
                bottomOffsetFraction = 0.7f,
            )
        val item2 =
            createTestItem(
                index = 2,
                measuredHeight = 100,
                transformedHeight = 60,
                topOffsetFraction = 0.7f,
                bottomOffsetFraction = 1.0f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1, item2))

        val exception =
            assertFailsWith<IllegalStateException> { measureResult.checkLayoutIsCorrect() }

        assertThat(exception.message)
            .contains("Incorrect layout: Measured items height rates are not correct")
    }

    @Test
    fun checkLayoutIsCorrect_itemWithZeroHeightFraction_throwsException() {
        val item =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.3f,
                bottomOffsetFraction = 0.3f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item))

        val exception =
            assertFailsWith<IllegalStateException> { measureResult.checkLayoutIsCorrect() }

        assertThat(exception.message)
            .contains("Incorrect layout: Items could not have zero height or negative height")
    }

    @Test
    fun checkLayoutIsCorrect_itemWithNegativeHeightFraction_throwsException() {
        val item =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.5f,
                bottomOffsetFraction = 0.2f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item))

        val exception =
            assertFailsWith<IllegalStateException> { measureResult.checkLayoutIsCorrect() }

        assertThat(exception.message)
            .contains("Incorrect layout: Items could not have zero height or negative height")
    }

    @Test
    fun checkLayoutIsCorrect_duplicateTopOffsetFractions_throwsException() {
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.2f,
                bottomOffsetFraction = 0.4f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.2f,
                bottomOffsetFraction = 0.5f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1))

        val exception =
            assertFailsWith<IllegalStateException> { measureResult.checkLayoutIsCorrect() }

        assertThat(exception.message)
            .contains("Incorrect layout: scrollProgress top offset fraction")
    }

    @Test
    fun checkLayoutIsCorrect_decreasingTopOffsetFractions_throwsException() {
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.4f,
                bottomOffsetFraction = 0.6f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.2f,
                bottomOffsetFraction = 0.7f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1))

        val exception =
            assertFailsWith<IllegalStateException> { measureResult.checkLayoutIsCorrect() }

        assertThat(exception.message)
            .contains("Incorrect layout: scrollProgress top offset fraction")
    }

    @Test
    fun checkLayoutIsCorrect_duplicateBottomOffsetFractions_throwsException() {
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.1f,
                bottomOffsetFraction = 0.5f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.3f,
                bottomOffsetFraction = 0.5f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1))

        val exception =
            assertFailsWith<IllegalStateException> { measureResult.checkLayoutIsCorrect() }

        assertThat(exception.message)
            .contains("Incorrect layout: scrollProgress bottom offset fraction")
    }

    @Test
    fun checkLayoutIsCorrect_decreasingBottomOffsetFractions_throwsException() {
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.1f,
                bottomOffsetFraction = 0.6f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.3f,
                bottomOffsetFraction = 0.5f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1))

        val exception =
            assertFailsWith<IllegalStateException> { measureResult.checkLayoutIsCorrect() }

        assertThat(exception.message)
            .contains("Incorrect layout: scrollProgress bottom offset fraction")
    }

    @Test
    fun checkLayoutIsCorrect_overlappingItemsWithMonotonicOffsets_doesNotThrow() {
        // Simulates items that legitimately overlap in vertical space (e.g. from
        // negative arrangement spacing, like cards dealt onto a table), where
        // Item 0 spans 0.1..0.6 and Item 1 spans 0.3..0.8.
        // Validates that checkLayoutIsCorrect() allows the overlap because both
        // top edges (0.1 < 0.3) and bottom edges (0.6 < 0.8) advance monotonically.
        val item0 =
            createTestItem(
                index = 0,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.1f,
                bottomOffsetFraction = 0.6f,
            )
        val item1 =
            createTestItem(
                index = 1,
                measuredHeight = 100,
                transformedHeight = 100,
                topOffsetFraction = 0.3f,
                bottomOffsetFraction = 0.8f,
            )
        val measureResult = createMeasureResult(visibleItems = listOf(item0, item1))

        measureResult.checkLayoutIsCorrect()
    }

    @Test
    fun transformingLazyColumn_renderedInViewport_passesLayoutCorrectnessCheck() {
        lateinit var state: TransformingLazyColumnState
        val containerSizeDp = 200.dp
        val expectedSizePx = with(rule.density) { containerSizeDp.roundToPx() }

        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it },
                modifier = Modifier.requiredSize(containerSizeDp),
            ) {
                items(count = 4) { Box(modifier = Modifier.requiredSize(50.dp)) }
            }
        }

        rule.runOnIdle {
            val measureResult = state.layoutInfo as TransformingLazyColumnMeasureResult
            measureResult.checkLayoutIsCorrect()

            assertThat(measureResult.viewportSize)
                .isEqualTo(IntSize(expectedSizePx, expectedSizePx))
            assertThat(measureResult.visibleItems).hasSize(4)
        }
    }

    private fun createTestItem(
        index: Int = 0,
        measuredHeight: Int = 100,
        transformedHeight: Int = 100,
        topOffsetFraction: Float = 0f,
        bottomOffsetFraction: Float = 0.5f,
    ): TransformingLazyColumnVisibleItemInfo =
        TransformingLazyColumnMeasuredItem(
            index = index,
            placeable =
                EmptyPlaceable(
                    width = 100,
                    height = measuredHeight,
                    transformedHeight = { _, _ -> transformedHeight },
                ),
            containerConstraints = Constraints(),
            offset = 0,
            spacing = 0,
            leftPadding = 0,
            rightPadding = 0,
            measureScrollProgress =
                TransformingLazyColumnItemScrollProgress(
                    topOffsetFraction = topOffsetFraction,
                    bottomOffsetFraction = bottomOffsetFraction,
                ),
            measurementDirection = MeasurementDirection.DOWNWARD,
            horizontalAlignment = Alignment.CenterHorizontally,
            layoutDirection = LayoutDirection.Ltr,
            key = index,
            contentType = null,
            reverseLayout = false,
        )

    private fun createMeasureResult(
        visibleItems: List<TransformingLazyColumnVisibleItemInfo> = emptyList(),
        width: Int = 100,
        height: Int = 100,
        alignmentLines: Map<AlignmentLine, Int> = emptyMap(),
        onPlaceChildren: () -> Unit = {},
    ): TransformingLazyColumnMeasureResult =
        TransformingLazyColumnMeasureResult(
            measureResult =
                object : MeasureResult {
                    override val width: Int = width
                    override val height: Int = height
                    override val alignmentLines: Map<AlignmentLine, Int> = alignmentLines

                    override fun placeChildren() {
                        onPlaceChildren()
                    }
                },
            anchorItemKey = 0,
            anchorItemIndex = 0,
            anchorItemScrollOffset = 0,
            lastMeasuredItemHeight = 0,
            coroutineScope = CoroutineScope(context = EmptyCoroutineContext),
            visibleItems = visibleItems,
            positionedItems = visibleItems,
            totalItemsCount = visibleItems.size,
            itemSpacing = 0,
            childConstraints = Constraints(),
            density = Density(density = 1f),
            beforeContentPadding = 0,
            afterContentPadding = 0,
            canScrollForward = false,
            canScrollBackward = false,
            reverseLayout = false,
            consumedScroll = 0f,
        )

    private class EmptyPlaceable(
        width: Int,
        height: Int,
        val transformedHeight: (Int, TransformingLazyColumnItemScrollProgress) -> Int,
    ) : Placeable() {
        init {
            measuredSize = IntSize(width = width, height = height)
        }

        override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified

        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?,
        ) {}

        override val parentData: Any
            get() = TransformingLazyColumnParentData(heightProvider = transformedHeight)
    }
}
