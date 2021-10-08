/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange
import androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyNotDefined
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.SwipeableState
import androidx.wear.compose.material.swipeable
import org.junit.Rule
import org.junit.Test
import kotlin.math.absoluteValue

// TODO(b/201009199) Some of these tests may need specific values adjusted when swipeable
//  supports property nested scrolling, but the tests should all still be valid.
@OptIn(ExperimentalWearMaterialApi::class)
class SwipeableTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun hasHorizontalScrollSemantics_atMaxValue_whenUnswiped() {
        rule.setContent {
            SimpleSwipeableBox { size ->
                Modifier.swipeable(
                    state = SwipeableState(false),
                    anchors = mapOf(0f to false, size.width to true),
                    orientation = Horizontal,
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(hasScrollRangeCloseTo(Horizontal, value = 1f, maxValue = 1f))
            .assert(keyNotDefined(VerticalScrollAxisRange))
    }

    @Test
    fun hasVerticalScrollSemantics_atMaxValue_whenUnswiped() {
        rule.setContent {
            SimpleSwipeableBox { size ->
                Modifier.swipeable(
                    state = SwipeableState(false),
                    anchors = mapOf(0f to false, size.height to true),
                    orientation = Vertical,
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(hasScrollRangeCloseTo(Vertical, value = 1f, maxValue = 1f))
            .assert(keyNotDefined(HorizontalScrollAxisRange))
    }

    @Test
    fun hasScrollSemantics_whenSwiped() {
        rule.setContent {
            SimpleSwipeableBox { size ->
                Modifier.swipeable(
                    state = SwipeableState(false),
                    anchors = mapOf(0f to false, size.width to true),
                    orientation = Horizontal,
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .performTouchInput {
                down(centerLeft)
                moveTo(centerLeft + percentOffset(.25f, 0f))
            }
        rule.onNodeWithTag(TEST_TAG)
            .assert(hasScrollRangeCloseTo(Horizontal, value = 0.75f, maxValue = 1f))

        rule.onNodeWithTag(TEST_TAG)
            .performTouchInput {
                moveTo(center)
            }
        rule.onNodeWithTag(TEST_TAG)
            .assert(hasScrollRangeCloseTo(Horizontal, value = 0.5f, maxValue = 1f))
    }

    @Test
    fun hasScrollSemantics_whenSwipedWithReverseDirection() {
        rule.setContent {
            SimpleSwipeableBox { size ->
                Modifier.swipeable(
                    state = SwipeableState(false),
                    anchors = mapOf(0f to false, size.width to true),
                    orientation = Horizontal,
                    reverseDirection = true
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .performTouchInput {
                down(centerRight)
                moveTo(centerRight - percentOffset(.25f, 0f))
            }
        rule.onNodeWithTag(TEST_TAG)
            .assert(
                hasScrollRangeCloseTo(
                    orientation = Horizontal,
                    value = 0.75f,
                    maxValue = 1f,
                    reverseScrolling = true
                )
            )

        rule.onNodeWithTag(TEST_TAG)
            .performTouchInput {
                moveTo(center)
            }
        rule.onNodeWithTag(TEST_TAG)
            .assert(
                hasScrollRangeCloseTo(
                    orientation = Horizontal,
                    value = 0.5f,
                    maxValue = 1f,
                    reverseScrolling = true
                )
            )
    }

    @Test
    fun hasNoScrollSemantics_whenDisabled() {
        rule.setContent {
            SimpleSwipeableBox { size ->
                Modifier.swipeable(
                    state = SwipeableState(false),
                    anchors = mapOf(0f to false, size.width to true),
                    orientation = Horizontal,
                    enabled = false
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(keyNotDefined(HorizontalScrollAxisRange))
            .assert(keyNotDefined(VerticalScrollAxisRange))
    }

    /**
     * A square [Box] has the [TEST_TAG] test tag. Touch slop is disabled to make swipe calculations
     * more exact.
     */
    @Composable
    private fun SimpleSwipeableBox(swipeableModifier: (Size) -> Modifier) {
        val originalViewConfiguration = LocalViewConfiguration.current
        val viewConfiguration = remember(originalViewConfiguration) {
            object : ViewConfiguration by originalViewConfiguration {
                override val touchSlop: Float = 0f
            }
        }

        with(LocalDensity.current) {
            val size = 100.dp
            val sizePx = size.toPx()

            CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
                Box(
                    Modifier
                        .testTag(TEST_TAG)
                        .requiredSize(size)
                        .then(remember { swipeableModifier(Size(sizePx, sizePx)) })
                )
            }
        }
    }

    /**
     * Matches either a [HorizontalScrollAxisRange] or [VerticalScrollAxisRange] that has the given
     * [maxValue] and [reverseScrolling], and a `value` that is within a small threshold of [value].
     */
    private fun hasScrollRangeCloseTo(
        orientation: Orientation,
        value: Float,
        maxValue: Float,
        reverseScrolling: Boolean = false
    ): SemanticsMatcher = SemanticsMatcher(
        "has $orientation scroll range [0,$maxValue] with " +
            "value=$value" + if (reverseScrolling) " (reversed)" else ""
    ) { node ->
        val threshold = .1f
        val property = when (orientation) {
            Horizontal -> HorizontalScrollAxisRange
            Vertical -> VerticalScrollAxisRange
        }
        node.config.getOrNull(property)?.let { range ->
            (range.value() - value).absoluteValue <= threshold &&
                range.maxValue() == maxValue &&
                range.reverseScrolling == reverseScrolling
        } ?: false
    }
}

private const val TEST_TAG = "swipeable"
