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

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScalingLazyColumnMeasureTest {

    private val testInterpolator: Easing = CubicBezierEasing(a = 0.0f, b = 0.0f, c = 0.2f, d = 1.0f)
    private val defaultOffsetResolver: (Constraints) -> Int = { 20 }

    // --- Group A: Preconditions & Validation ---

    @Test
    fun defaultScalingParams_minElementHeightExceedsMax_throwsIllegalStateException() {
        val invalidMinHeight = 0.8f
        val invalidMaxHeight = 0.2f

        val exception =
            assertThrows(IllegalStateException::class.java) {
                DefaultScalingParams(
                    edgeScale = 0.5f,
                    edgeAlpha = 0.5f,
                    minElementHeight = invalidMinHeight,
                    maxElementHeight = invalidMaxHeight,
                    minTransitionArea = 0.2f,
                    maxTransitionArea = 0.6f,
                    scaleInterpolator = testInterpolator,
                    viewportVerticalOffsetResolver = defaultOffsetResolver,
                )
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("minElementHeight must be less than or equal to maxElementHeight")
    }

    @Test
    fun defaultScalingParams_minTransitionAreaExceedsMax_throwsIllegalStateException() {
        val invalidMinArea = 0.8f
        val invalidMaxArea = 0.3f

        val exception =
            assertThrows(IllegalStateException::class.java) {
                DefaultScalingParams(
                    edgeScale = 0.5f,
                    edgeAlpha = 0.5f,
                    minElementHeight = 0.2f,
                    maxElementHeight = 0.8f,
                    minTransitionArea = invalidMinArea,
                    maxTransitionArea = invalidMaxArea,
                    scaleInterpolator = testInterpolator,
                    viewportVerticalOffsetResolver = defaultOffsetResolver,
                )
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("minTransitionArea must be less than or equal to maxTransitionArea")
    }

    @Test
    fun defaultScalingParams_resolveViewportVerticalOffset_delegatesToResolver() {
        val resolver: (Constraints) -> Int = { constraints -> constraints.maxHeight / 4 }
        val params =
            DefaultScalingParams(
                edgeScale = 0.7f,
                edgeAlpha = 0.7f,
                minElementHeight = 0.2f,
                maxElementHeight = 0.8f,
                minTransitionArea = 0.2f,
                maxTransitionArea = 0.6f,
                scaleInterpolator = testInterpolator,
                viewportVerticalOffsetResolver = resolver,
            )

        val resolvedOffset =
            params.resolveViewportVerticalOffset(viewportConstraints = Constraints(maxHeight = 200))

        assertThat(resolvedOffset).isEqualTo(50)
    }

    // --- Group B: DefaultScalingParams Value Equality & Hashing ---

    @Test
    fun defaultScalingParams_sameInstance_areEqual() {
        val params = createDefaultParams()

        val isSame = params == params

        assertThat(isSame).isTrue()
    }

    @Test
    fun defaultScalingParams_identicalValues_areEqual() {
        val params1 = createDefaultParams()
        val params2 = createDefaultParams()

        val areEqual = params1 == params2

        assertThat(areEqual).isTrue()
    }

    @Test
    fun defaultScalingParams_identicalValues_sameHashCode() {
        val params1 = createDefaultParams()
        val params2 = createDefaultParams()

        val hash1 = params1.hashCode()
        val hash2 = params2.hashCode()

        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    fun defaultScalingParams_comparedWithNull_notEqual() {
        val params = createDefaultParams()

        val isEqual = params.equals(null)

        assertThat(isEqual).isFalse()
    }

    @Test
    fun defaultScalingParams_comparedWithDifferentClass_notEqual() {
        val params = createDefaultParams()

        val isEqual = params.equals("not-a-scaling-params")

        assertThat(isEqual).isFalse()
    }

    @Test
    fun defaultScalingParams_differentEdgeScale_notEqual() {
        val params1 = createDefaultParams(edgeScale = 0.7f)
        val params2 = createDefaultParams(edgeScale = 0.5f)

        val areEqual = params1 == params2

        assertThat(areEqual).isFalse()
    }

    @Test
    fun defaultScalingParams_differentEdgeAlpha_notEqual() {
        val params1 = createDefaultParams(edgeAlpha = 0.7f)
        val params2 = createDefaultParams(edgeAlpha = 0.3f)

        val areEqual = params1 == params2

        assertThat(areEqual).isFalse()
    }

    @Test
    fun defaultScalingParams_differentMinElementHeight_notEqual() {
        val params1 = createDefaultParams(minElementHeight = 0.2f)
        val params2 = createDefaultParams(minElementHeight = 0.1f)

        val areEqual = params1 == params2

        assertThat(areEqual).isFalse()
    }

    @Test
    fun defaultScalingParams_differentMaxElementHeight_notEqual() {
        val params1 = createDefaultParams(maxElementHeight = 0.8f)
        val params2 = createDefaultParams(maxElementHeight = 0.9f)

        val areEqual = params1 == params2

        assertThat(areEqual).isFalse()
    }

    @Test
    fun defaultScalingParams_differentMinTransitionArea_notEqual() {
        val params1 = createDefaultParams(minTransitionArea = 0.2f)
        val params2 = createDefaultParams(minTransitionArea = 0.1f)

        val areEqual = params1 == params2

        assertThat(areEqual).isFalse()
    }

    @Test
    fun defaultScalingParams_differentMaxTransitionArea_notEqual() {
        val params1 = createDefaultParams(maxTransitionArea = 0.6f)
        val params2 = createDefaultParams(maxTransitionArea = 0.7f)

        val areEqual = params1 == params2

        assertThat(areEqual).isFalse()
    }

    @Test
    fun defaultScalingParams_differentScaleInterpolator_notEqual() {
        val otherInterpolator: Easing = CubicBezierEasing(a = 0.1f, b = 0.1f, c = 0.3f, d = 0.9f)
        val params1 = createDefaultParams(scaleInterpolator = testInterpolator)
        val params2 = createDefaultParams(scaleInterpolator = otherInterpolator)

        val areEqual = params1 == params2

        assertThat(areEqual).isFalse()
    }

    @Test
    fun defaultScalingParams_differentOffsetResolver_notEqual() {
        val resolver1: (Constraints) -> Int = { 10 }
        val resolver2: (Constraints) -> Int = { 20 }
        val params1 = createDefaultParams(viewportVerticalOffsetResolver = resolver1)
        val params2 = createDefaultParams(viewportVerticalOffsetResolver = resolver2)

        val areEqual = params1 == params2

        assertThat(areEqual).isFalse()
    }

    // --- Group C: ReduceMotionScalingParams ---

    @Test
    fun reduceMotionScalingParams_onInstantiation_forcesEdgeScaleAndAlphaToOne() {
        val initial =
            DefaultScalingParams(
                edgeScale = 0.3f,
                edgeAlpha = 0.4f,
                minElementHeight = 0.2f,
                maxElementHeight = 0.8f,
                minTransitionArea = 0.2f,
                maxTransitionArea = 0.6f,
                scaleInterpolator = testInterpolator,
                viewportVerticalOffsetResolver = defaultOffsetResolver,
            )

        val reduceMotion = ReduceMotionScalingParams(initial = initial)

        assertThat(reduceMotion.edgeScale).isWithin(0.0001f).of(1.0f)
        assertThat(reduceMotion.edgeAlpha).isWithin(0.0001f).of(1.0f)
        assertThat(reduceMotion.minElementHeight).isWithin(0.0001f).of(0.2f)
        assertThat(reduceMotion.maxElementHeight).isWithin(0.0001f).of(0.8f)
        assertThat(reduceMotion.minTransitionArea).isWithin(0.0001f).of(0.2f)
        assertThat(reduceMotion.maxTransitionArea).isWithin(0.0001f).of(0.6f)
        assertThat(reduceMotion.scaleInterpolator).isSameInstanceAs(testInterpolator)
    }

    @Test
    fun reduceMotionScalingParams_resolveViewportVerticalOffset_delegatesToInitial() {
        val initial =
            DefaultScalingParams(
                edgeScale = 0.5f,
                edgeAlpha = 0.5f,
                minElementHeight = 0.2f,
                maxElementHeight = 0.8f,
                minTransitionArea = 0.2f,
                maxTransitionArea = 0.6f,
                scaleInterpolator = testInterpolator,
                viewportVerticalOffsetResolver = { constraints -> constraints.maxHeight / 2 },
            )
        val reduceMotion = ReduceMotionScalingParams(initial = initial)

        val resolvedOffset =
            reduceMotion.resolveViewportVerticalOffset(
                viewportConstraints = Constraints(maxHeight = 300)
            )

        assertThat(resolvedOffset).isEqualTo(150)
    }

    @Test
    fun reduceMotionScalingParams_identicalValues_areEqual() {
        val initial1 = createDefaultParams()
        val initial2 = createDefaultParams()
        val reduceMotion1 = ReduceMotionScalingParams(initial = initial1)
        val reduceMotion2 = ReduceMotionScalingParams(initial = initial2)

        val areEqual = reduceMotion1 == reduceMotion2

        assertThat(areEqual).isTrue()
    }

    @Test
    fun reduceMotionScalingParams_identicalValues_sameHashCode() {
        val initial1 = createDefaultParams()
        val initial2 = createDefaultParams()
        val reduceMotion1 = ReduceMotionScalingParams(initial = initial1)
        val reduceMotion2 = ReduceMotionScalingParams(initial = initial2)

        val hash1 = reduceMotion1.hashCode()
        val hash2 = reduceMotion2.hashCode()

        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    fun reduceMotionScalingParams_comparedWithNull_notEqual() {
        val reduceMotion = ReduceMotionScalingParams(initial = createDefaultParams())

        val isEqual = reduceMotion.equals(null)

        assertThat(isEqual).isFalse()
    }

    @Test
    fun reduceMotionScalingParams_comparedWithDifferentClass_notEqual() {
        val reduceMotion = ReduceMotionScalingParams(initial = createDefaultParams())

        val isEqual = reduceMotion.equals("not-reduce-motion")

        assertThat(isEqual).isFalse()
    }

    @Test
    fun reduceMotionScalingParams_differentInitialParams_notEqual() {
        val initial1 = createDefaultParams(minElementHeight = 0.2f)
        val initial2 = createDefaultParams(minElementHeight = 0.1f)
        val reduceMotion1 = ReduceMotionScalingParams(initial = initial1)
        val reduceMotion2 = ReduceMotionScalingParams(initial = initial2)

        val areEqual = reduceMotion1 == reduceMotion2

        assertThat(areEqual).isFalse()
    }

    // --- Group D: Viewport Scaling Geometry (calculateScaleAndAlpha) ---

    @Test
    fun calculateScaleAndAlpha_zeroViewportHeight_returnsUnscaled() {
        val params = createDefaultParams(edgeScale = 0.5f, edgeAlpha = 0.5f)

        val result =
            calculateScaleAndAlpha(
                viewPortStartPx = 0,
                viewPortEndPx = 0,
                itemTopPx = 0,
                itemBottomPx = 50,
                scalingParams = params,
            )

        assertThat(result.scale).isWithin(0.0001f).of(1.0f)
        assertThat(result.alpha).isWithin(0.0001f).of(1.0f)
    }

    @Test
    fun calculateScaleAndAlpha_negativeViewportHeight_returnsUnscaled() {
        val params = createDefaultParams(edgeScale = 0.5f, edgeAlpha = 0.5f)

        val result =
            calculateScaleAndAlpha(
                viewPortStartPx = 100,
                viewPortEndPx = 50,
                itemTopPx = 0,
                itemBottomPx = 50,
                scalingParams = params,
            )

        assertThat(result.scale).isWithin(0.0001f).of(1.0f)
        assertThat(result.alpha).isWithin(0.0001f).of(1.0f)
    }

    @Test
    fun calculateScaleAndAlpha_itemInCenter_returnsUnscaled() {
        val params =
            createDefaultParams(
                edgeScale = 0.5f,
                edgeAlpha = 0.5f,
                minElementHeight = 0.2f,
                maxElementHeight = 0.8f,
                minTransitionArea = 0.2f,
                maxTransitionArea = 0.6f,
                scaleInterpolator = LinearEasing,
            )

        val result =
            calculateScaleAndAlpha(
                viewPortStartPx = 0,
                viewPortEndPx = 200,
                itemTopPx = 80,
                itemBottomPx = 120,
                scalingParams = params,
            )

        assertThat(result.scale).isWithin(0.0001f).of(1.0f)
        assertThat(result.alpha).isWithin(0.0001f).of(1.0f)
    }

    @Test
    fun calculateScaleAndAlpha_itemBisectingTopEdge_interpolatesScaleAndAlpha() {
        val params =
            createDefaultParams(
                edgeScale = 0.5f,
                edgeAlpha = 0.4f,
                minElementHeight = 0.2f,
                maxElementHeight = 0.8f,
                minTransitionArea = 0.2f,
                maxTransitionArea = 0.6f,
                scaleInterpolator = LinearEasing,
            )

        val result =
            calculateScaleAndAlpha(
                viewPortStartPx = 0,
                viewPortEndPx = 200,
                itemTopPx = -20,
                itemBottomPx = 20,
                scalingParams = params,
            )

        assertThat(result.scale).isWithin(0.0001f).of(0.75f)
        assertThat(result.alpha).isWithin(0.0001f).of(0.7f)
    }

    @Test
    fun calculateScaleAndAlpha_itemBisectingBottomEdge_interpolatesScaleAndAlpha() {
        val params =
            createDefaultParams(
                edgeScale = 0.5f,
                edgeAlpha = 0.4f,
                minElementHeight = 0.2f,
                maxElementHeight = 0.8f,
                minTransitionArea = 0.2f,
                maxTransitionArea = 0.6f,
                scaleInterpolator = LinearEasing,
            )

        // Viewport 0..200. Item is 180..220 (height 40, centered at bottom boundary 200).
        // Distance from bottom edge = 200 - 180 = 20px (itemEdgeFraction = 0.1).
        // Scaling line for height 40/200 (0.2) is at 0.2 (40px).
        // scalingProgressRaw = 1 - 0.1 / 0.2 = 0.5.
        // scale = lerp(1.0, 0.5, 0.5) = 0.75f; alpha = lerp(1.0, 0.4, 0.5) = 0.7f.
        val result =
            calculateScaleAndAlpha(
                viewPortStartPx = 0,
                viewPortEndPx = 200,
                itemTopPx = 180,
                itemBottomPx = 220,
                scalingParams = params,
            )

        assertThat(result.scale).isWithin(0.0001f).of(0.75f)
        assertThat(result.alpha).isWithin(0.0001f).of(0.7f)
    }

    @Test
    fun calculateScaleAndAlpha_itemLargerThanMaxElementHeight_clampsToMaxTransitionArea() {
        val params =
            createDefaultParams(
                edgeScale = 0.5f,
                edgeAlpha = 0.5f,
                minElementHeight = 0.2f,
                maxElementHeight = 0.5f,
                minTransitionArea = 0.2f,
                maxTransitionArea = 0.6f,
                scaleInterpolator = LinearEasing,
            )

        // Viewport 0..200. Item height = 160 (fraction 0.8 > maxElementHeight 0.5).
        // sizeRatio clamps to 1.0, transition line is clamped to maxTransitionArea = 0.6 (120px).
        // Item placed at -100..60 (distance of trailing edge from top viewport boundary = 60px).
        // itemEdgeAsFractionOfViewPort = 60 / 200 = 0.3 < 0.6.
        // scalingProgress = 1 - (0.3 / 0.6) = 0.5.
        // scale = lerp(1.0, 0.5, 0.5) = 0.75f.
        val result =
            calculateScaleAndAlpha(
                viewPortStartPx = 0,
                viewPortEndPx = 200,
                itemTopPx = -100,
                itemBottomPx = 60,
                scalingParams = params,
            )

        assertThat(result.scale).isWithin(0.0001f).of(0.75f)
    }

    @Test
    fun calculateScaleAndAlpha_itemSmallerThanMinElementHeight_clampsToMinTransitionArea() {
        val params =
            createDefaultParams(
                edgeScale = 0.5f,
                edgeAlpha = 0.5f,
                minElementHeight = 0.2f,
                maxElementHeight = 0.5f,
                minTransitionArea = 0.2f,
                maxTransitionArea = 0.6f,
                scaleInterpolator = LinearEasing,
            )

        // Viewport 0..200. Item height = 20 (fraction 0.1 < minElementHeight 0.2).
        // sizeRatio clamps to 0.0, transition line is at minTransitionArea = 0.2 (40px).
        // Item placed at -10..10. Distance from edge = 10px (fraction = 0.05).
        // scalingProgress = 1 - 0.05 / 0.2 = 0.75.
        // scale = lerp(1.0, 0.5, 0.75) = 0.625f.
        val result =
            calculateScaleAndAlpha(
                viewPortStartPx = 0,
                viewPortEndPx = 200,
                itemTopPx = -10,
                itemBottomPx = 10,
                scalingParams = params,
            )

        assertThat(result.scale).isWithin(0.0001f).of(0.625f)
    }

    // --- Group E: Item Info & Layout Placement (calculateItemInfo) ---

    @Test
    fun calculateItemInfo_itemAboveCenterLine_adjustsScaledItemTop() {
        val params = createDefaultParams(edgeScale = 0.7f, scaleInterpolator = LinearEasing)
        val item = TestLazyListItemInfo(index = 1, key = "key-1", offset = -10, size = 40)

        // Viewport 200. Item start = -10, end = 30. (30 + -10) = 20 < 200 -> isAboveLine = true.
        // itemEdgeDistanceFromViewPortEdge = min(200 - (-10), 30 - 0) = 30px.
        // itemEdgeFraction = 30 / 200 = 0.15. Scaling line for height 40/200 (0.2) is at 0.2
        // (40px).
        // scalingProgress = 1 - 0.15 / 0.2 = 0.25.
        // scale = 1.0 - (1.0 - 0.7) * 0.25 = 0.925f.
        // scaledHeight = kotlin.math.round(40 * 0.925f).toInt() = 37.
        // scaledItemTop = itemStart + item.size - scaledHeight = -10 + 40 - 37 = -7.
        // offset = scaledItemTop - viewportCenterLinePx = -7 - 100 = -107.
        val info =
            calculateItemInfo(
                itemStart = -10,
                item = item,
                verticalAdjustment = 0,
                viewportHeightPx = 200,
                viewportCenterLinePx = 100,
                scalingParams = params,
                beforeContentPaddingPx = 0,
                anchorType = ScalingLazyListAnchorType.ItemStart,
                autoCentering = null,
                visible = true,
            )

        assertThat(info.offset).isEqualTo(-107)
        assertThat(info.size).isEqualTo(37)
        assertThat(info.unadjustedSize).isEqualTo(40)
    }

    @Test
    fun calculateItemInfo_itemBelowCenterLine_preservesItemStartAsScaledItemTop() {
        val params = createDefaultParams(scaleInterpolator = LinearEasing)
        val item = TestLazyListItemInfo(index = 2, key = "key-2", offset = 140, size = 40)

        // Viewport 200. Item start = 140, end = 180. (140 + 180) = 320 >= 200 -> isAboveLine =
        // false.
        // scaledItemTop = itemStart = 140.
        val info =
            calculateItemInfo(
                itemStart = 140,
                item = item,
                verticalAdjustment = 0,
                viewportHeightPx = 200,
                viewportCenterLinePx = 100,
                scalingParams = params,
                beforeContentPaddingPx = 0,
                anchorType = ScalingLazyListAnchorType.ItemStart,
                autoCentering = null,
                visible = true,
            )

        // offset for ItemStart = scaledItemTop - centerLine = 140 - 100 = 40.
        assertThat(info.offset).isEqualTo(40)
    }

    @Test
    fun calculateItemInfo_notVisible_forcesAlphaToZero() {
        val params = createDefaultParams(edgeAlpha = 0.5f)
        val item = TestLazyListItemInfo(index = 0, key = "key-0", offset = 80, size = 40)

        val info =
            calculateItemInfo(
                itemStart = 80,
                item = item,
                verticalAdjustment = 0,
                viewportHeightPx = 200,
                viewportCenterLinePx = 100,
                scalingParams = params,
                beforeContentPaddingPx = 0,
                anchorType = ScalingLazyListAnchorType.ItemCenter,
                autoCentering = null,
                visible = false,
            )

        assertThat(info.alpha).isWithin(0.0001f).of(0f)
    }

    @Test
    fun calculateItemInfo_withAutoCentering_decrementsIndexForSpacer() {
        val params = createDefaultParams()
        val item = TestLazyListItemInfo(index = 3, key = "key-3", offset = 80, size = 40)

        val info =
            calculateItemInfo(
                itemStart = 80,
                item = item,
                verticalAdjustment = 0,
                viewportHeightPx = 200,
                viewportCenterLinePx = 100,
                scalingParams = params,
                beforeContentPaddingPx = 0,
                anchorType = ScalingLazyListAnchorType.ItemCenter,
                autoCentering = AutoCenteringParams(),
                visible = true,
            )

        assertThat(info.index).isEqualTo(2)
    }

    @Test
    fun calculateItemInfo_withoutAutoCentering_preservesItemIndex() {
        val params = createDefaultParams()
        val item = TestLazyListItemInfo(index = 3, key = "key-3", offset = 80, size = 40)

        val info =
            calculateItemInfo(
                itemStart = 80,
                item = item,
                verticalAdjustment = 0,
                viewportHeightPx = 200,
                viewportCenterLinePx = 100,
                scalingParams = params,
                beforeContentPaddingPx = 0,
                anchorType = ScalingLazyListAnchorType.ItemCenter,
                autoCentering = null,
                visible = true,
            )

        assertThat(info.index).isEqualTo(3)
    }

    // --- Group F: Center Offset & Dimension Extensions ---

    @Test
    fun convertToCenterOffset_itemStartAnchor_doesNotAddHalfItemSize() {
        val offset =
            convertToCenterOffset(
                anchorType = ScalingLazyListAnchorType.ItemStart,
                itemScrollOffset = 80,
                viewportCenterLinePx = 100,
                beforeContentPaddingInPx = 10,
                itemSizeInPx = 40,
            )

        assertThat(offset).isEqualTo(-10)
    }

    @Test
    fun convertToCenterOffset_itemCenterAnchor_roundsDownOddItemSize() {
        val offsetOdd =
            convertToCenterOffset(
                anchorType = ScalingLazyListAnchorType.ItemCenter,
                itemScrollOffset = 80,
                viewportCenterLinePx = 100,
                beforeContentPaddingInPx = 10,
                itemSizeInPx = 19,
            )

        assertThat(offsetOdd).isEqualTo(-1)
    }

    @Test
    fun startOffset_itemCenterAnchor_subtractsHalfSize() {
        val itemInfo =
            DefaultScalingLazyListItemInfo(
                index = 0,
                key = "item",
                unadjustedOffset = 10,
                offset = 50,
                size = 40,
                scale = 1.0f,
                alpha = 1.0f,
                unadjustedSize = 40,
            )

        val start = itemInfo.startOffset(anchorType = ScalingLazyListAnchorType.ItemCenter)

        assertThat(start).isWithin(0.0001f).of(30.0f)
    }

    @Test
    fun startOffset_itemStartAnchor_doesNotSubtractHalfSize() {
        val itemInfo =
            DefaultScalingLazyListItemInfo(
                index = 0,
                key = "item",
                unadjustedOffset = 10,
                offset = 50,
                size = 40,
                scale = 1.0f,
                alpha = 1.0f,
                unadjustedSize = 40,
            )

        val start = itemInfo.startOffset(anchorType = ScalingLazyListAnchorType.ItemStart)

        assertThat(start).isWithin(0.0001f).of(50.0f)
    }

    @Test
    fun unadjustedStartOffset_itemCenterAnchor_subtractsHalfUnadjustedSize() {
        val itemInfo =
            DefaultScalingLazyListItemInfo(
                index = 0,
                key = "item",
                unadjustedOffset = 100,
                offset = 50,
                size = 30,
                scale = 0.5f,
                alpha = 1.0f,
                unadjustedSize = 60,
            )

        val unadjustedStart =
            itemInfo.unadjustedStartOffset(anchorType = ScalingLazyListAnchorType.ItemCenter)

        assertThat(unadjustedStart).isWithin(0.0001f).of(70.0f)
    }

    @Test
    fun unadjustedStartOffset_itemStartAnchor_doesNotSubtractHalfUnadjustedSize() {
        val itemInfo =
            DefaultScalingLazyListItemInfo(
                index = 0,
                key = "item",
                unadjustedOffset = 100,
                offset = 50,
                size = 30,
                scale = 0.5f,
                alpha = 1.0f,
                unadjustedSize = 60,
            )

        val unadjustedStart =
            itemInfo.unadjustedStartOffset(anchorType = ScalingLazyListAnchorType.ItemStart)

        assertThat(unadjustedStart).isWithin(0.0001f).of(100.0f)
    }

    // --- Group G: Layout Info State Contracts ---

    @Test
    fun defaultScalingLazyListLayoutInfo_uninitialized_returnsEmptyVisibleItems() {
        val itemInfo =
            DefaultScalingLazyListItemInfo(
                index = 0,
                key = "item-0",
                unadjustedOffset = 10,
                offset = 10,
                size = 40,
                scale = 1.0f,
                alpha = 1.0f,
                unadjustedSize = 40,
            )
        val layoutInfo =
            DefaultScalingLazyListLayoutInfo(
                internalVisibleItemsInfo = listOf(itemInfo),
                viewportStartOffset = 0,
                viewportEndOffset = 200,
                totalItemsCount = 1,
                centerItemIndex = 0,
                centerItemScrollOffset = 0,
                reverseLayout = false,
                orientation = Orientation.Vertical,
                viewportSize = IntSize(width = 200, height = 200),
                beforeContentPadding = 0,
                afterContentPadding = 0,
                beforeAutoCenteringPadding = 0,
                afterAutoCenteringPadding = 0,
                readyForInitialScroll = false,
                initialized = false,
                anchorType = ScalingLazyListAnchorType.ItemCenter,
            )

        assertThat(layoutInfo.visibleItemsInfo).isEmpty()
    }

    @Test
    fun defaultScalingLazyListLayoutInfo_initialized_returnsInternalVisibleItems() {
        val itemInfo =
            DefaultScalingLazyListItemInfo(
                index = 0,
                key = "item-0",
                unadjustedOffset = 10,
                offset = 10,
                size = 40,
                scale = 1.0f,
                alpha = 1.0f,
                unadjustedSize = 40,
            )
        val layoutInfo =
            DefaultScalingLazyListLayoutInfo(
                internalVisibleItemsInfo = listOf(itemInfo),
                viewportStartOffset = 0,
                viewportEndOffset = 200,
                totalItemsCount = 1,
                centerItemIndex = 0,
                centerItemScrollOffset = 0,
                reverseLayout = false,
                orientation = Orientation.Vertical,
                viewportSize = IntSize(width = 200, height = 200),
                beforeContentPadding = 0,
                afterContentPadding = 0,
                beforeAutoCenteringPadding = 0,
                afterAutoCenteringPadding = 0,
                readyForInitialScroll = true,
                initialized = true,
                anchorType = ScalingLazyListAnchorType.ItemStart,
            )

        assertThat(layoutInfo.visibleItemsInfo).containsExactly(itemInfo)
    }

    // --- Helper Functions & Test Doubles ---

    private fun createDefaultParams(
        edgeScale: Float = 0.7f,
        edgeAlpha: Float = 0.7f,
        minElementHeight: Float = 0.2f,
        maxElementHeight: Float = 0.8f,
        minTransitionArea: Float = 0.2f,
        maxTransitionArea: Float = 0.6f,
        scaleInterpolator: Easing = testInterpolator,
        viewportVerticalOffsetResolver: (Constraints) -> Int = defaultOffsetResolver,
    ): DefaultScalingParams {
        return DefaultScalingParams(
            edgeScale = edgeScale,
            edgeAlpha = edgeAlpha,
            minElementHeight = minElementHeight,
            maxElementHeight = maxElementHeight,
            minTransitionArea = minTransitionArea,
            maxTransitionArea = maxTransitionArea,
            scaleInterpolator = scaleInterpolator,
            viewportVerticalOffsetResolver = viewportVerticalOffsetResolver,
        )
    }

    private class TestLazyListItemInfo(
        override val index: Int,
        override val key: Any,
        override val offset: Int,
        override val size: Int,
        override val contentType: Any? = null,
    ) : LazyListItemInfo
}
