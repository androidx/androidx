/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule

abstract class BaseListTestWithOrientation(protected val orientation: Orientation) {

    @get:Rule val rule: ComposeContentTestRule = createComposeRule()

    val vertical: Boolean
        get() = orientation == Orientation.Vertical

    lateinit var scope: CoroutineScope

    @Composable
    internal fun TestList(
        modifier: Modifier = Modifier,
        horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
        verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
        verticalArrangement: Arrangement.Vertical = Arrangement.Center,
        state: ListState = rememberListState(),
        itemsCount: Int = Int.MAX_VALUE,
        keyProvider: ((index: Int) -> Any)? = null,
        contentPadding: PaddingValues = PaddingValues(),
        itemContent: @Composable (index: Int) -> Unit,
    ) {
        List(
            state = state,
            orientation = orientation,
            horizontalAlignment = horizontalAlignment,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            verticalArrangement = verticalArrangement,
            modifier = modifier.testTag(LIST_TEST_TAG),
            contentPadding = contentPadding,
        ) {
            items(itemsCount, key = keyProvider) { index -> itemContent(index) }
        }
    }

    @Stable
    fun Modifier.mainAxisSize(size: Dp) =
        if (vertical) {
            this.height(size)
        } else {
            this.width(size)
        }

    @Stable
    fun Modifier.fillCrossAxisSize() =
        if (vertical) {
            this.fillMaxWidth()
        } else {
            this.fillMaxHeight()
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

    fun PaddingValues(mainAxis: Dp = 0.dp, crossAxis: Dp = 0.dp) =
        PaddingValues(
            beforeContent = mainAxis,
            afterContent = mainAxis,
            beforeContentCrossAxis = crossAxis,
            afterContentCrossAxis = crossAxis,
        )

    private fun PaddingValues(
        beforeContent: Dp = 0.dp,
        afterContent: Dp = 0.dp,
        beforeContentCrossAxis: Dp = 0.dp,
        afterContentCrossAxis: Dp = 0.dp,
    ) =
        if (vertical) {
            androidx.compose.foundation.layout.PaddingValues(
                start = beforeContentCrossAxis,
                top = beforeContent,
                end = afterContentCrossAxis,
                bottom = afterContent,
            )
        } else {
            androidx.compose.foundation.layout.PaddingValues(
                start = beforeContent,
                top = beforeContentCrossAxis,
                end = afterContent,
                bottom = afterContentCrossAxis,
            )
        }

    protected fun ComposeContentTestRule.setContentAndSaveScope(content: @Composable () -> Unit) {
        setContent {
            scope = rememberCoroutineScope()
            content()
        }
    }

    companion object {
        internal const val LIST_TEST_TAG: String = "glimmer-lazy-list"
    }
}
