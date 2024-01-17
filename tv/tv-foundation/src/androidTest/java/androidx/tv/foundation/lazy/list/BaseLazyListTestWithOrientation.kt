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

package androidx.tv.foundation.lazy.list

import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.AutoTestFrameClock
import androidx.tv.foundation.lazy.grid.keyPress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule

open class BaseLazyListTestWithOrientation(private val orientation: Orientation) {

    @get:Rule
    val rule = createComposeRule()

    val vertical: Boolean
        get() = orientation == Orientation.Vertical

    fun Modifier.mainAxisSize(size: Dp) =
        if (vertical) {
            this.height(size)
        } else {
            this.width(size)
        }

    fun Modifier.crossAxisSize(size: Dp) =
        if (vertical) {
            this.width(size)
        } else {
            this.height(size)
        }

    fun Modifier.fillMaxCrossAxis() =
        if (vertical) {
            this.fillMaxWidth()
        } else {
            this.fillMaxHeight()
        }

    fun TvLazyListItemScope.fillParentMaxMainAxis() =
        if (vertical) {
            Modifier.fillParentMaxHeight()
        } else {
            Modifier.fillParentMaxWidth()
        }

    fun TvLazyListItemScope.fillParentMaxCrossAxis() =
        if (vertical) {
            Modifier.fillParentMaxWidth()
        } else {
            Modifier.fillParentMaxHeight()
        }

    fun SemanticsNodeInteraction.assertMainAxisSizeIsEqualTo(expectedSize: Dp) =
        if (vertical) {
            assertHeightIsEqualTo(expectedSize)
        } else {
            assertWidthIsEqualTo(expectedSize)
        }

    fun SemanticsNodeInteraction.assertCrossAxisSizeIsEqualTo(expectedSize: Dp) =
        if (vertical) {
            assertWidthIsEqualTo(expectedSize)
        } else {
            assertHeightIsEqualTo(expectedSize)
        }

    fun SemanticsNodeInteraction.assertStartPositionIsAlmost(expected: Dp) {
        val position = if (vertical) {
            getUnclippedBoundsInRoot().top
        } else {
            getUnclippedBoundsInRoot().left
        }
        position.assertIsEqualTo(expected, tolerance = 2.dp)
    }

    fun SemanticsNodeInteraction.assertStartPositionInRootIsEqualTo(expectedStart: Dp) =
        if (vertical) {
            assertTopPositionInRootIsEqualTo(expectedStart)
        } else {
            assertLeftPositionInRootIsEqualTo(expectedStart)
        }

    fun SemanticsNodeInteraction.assertCrossAxisStartPositionInRootIsEqualTo(expectedStart: Dp) =
        if (vertical) {
            assertLeftPositionInRootIsEqualTo(expectedStart)
        } else {
            assertTopPositionInRootIsEqualTo(expectedStart)
        }

    fun PaddingValues(
        mainAxis: Dp = 0.dp,
        crossAxis: Dp = 0.dp
    ) = PaddingValues(
        beforeContent = mainAxis,
        afterContent = mainAxis,
        beforeContentCrossAxis = crossAxis,
        afterContentCrossAxis = crossAxis
    )

    fun PaddingValues(
        beforeContent: Dp = 0.dp,
        afterContent: Dp = 0.dp,
        beforeContentCrossAxis: Dp = 0.dp,
        afterContentCrossAxis: Dp = 0.dp,
    ) = if (vertical) {
        PaddingValues(
            start = beforeContentCrossAxis,
            top = beforeContent,
            end = afterContentCrossAxis,
            bottom = afterContent
        )
    } else {
        PaddingValues(
            start = beforeContent,
            top = beforeContentCrossAxis,
            end = afterContent,
            bottom = afterContentCrossAxis
        )
    }

    fun TvLazyListState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
        }
    }

    fun TvLazyListState.scrollTo(index: Int) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            scrollToItem(index)
        }
    }

    fun ComposeContentTestRule.keyPress(numberOfKeyPresses: Int, reverseScroll: Boolean = false) {
        val keyCode: Int =
            when {
                vertical && reverseScroll -> NativeKeyEvent.KEYCODE_DPAD_UP
                vertical && !reverseScroll -> NativeKeyEvent.KEYCODE_DPAD_DOWN
                !vertical && reverseScroll -> NativeKeyEvent.KEYCODE_DPAD_LEFT
                !vertical && !reverseScroll -> NativeKeyEvent.KEYCODE_DPAD_RIGHT
                else -> NativeKeyEvent.KEYCODE_DPAD_RIGHT
            }

        keyPress(keyCode, numberOfKeyPresses)
    }

    @Composable
    fun LazyColumnOrRow(
        modifier: Modifier = Modifier,
        state: TvLazyListState = rememberTvLazyListState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        userScrollEnabled: Boolean = true,
        spacedBy: Dp = 0.dp,
        pivotOffsets: PivotOffsets =
            PivotOffsets(parentFraction = 0f),
        content: TvLazyListScope.() -> Unit
    ) {
        if (vertical) {
            val verticalArrangement = when {
                spacedBy != 0.dp -> Arrangement.spacedBy(spacedBy)
                !reverseLayout -> Arrangement.Top
                else -> Arrangement.Bottom
            }
            TvLazyColumn(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                userScrollEnabled = userScrollEnabled,
                verticalArrangement = verticalArrangement,
                pivotOffsets = pivotOffsets,
                content = content
            )
        } else {
            val horizontalArrangement = when {
                spacedBy != 0.dp -> Arrangement.spacedBy(spacedBy)
                !reverseLayout -> Arrangement.Start
                else -> Arrangement.End
            }
            TvLazyRow(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                userScrollEnabled = userScrollEnabled,
                horizontalArrangement = horizontalArrangement,
                pivotOffsets = pivotOffsets,
                content = content
            )
        }
    }
}
