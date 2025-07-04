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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.setGlimmerThemeContent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ListStateTest(orientation: Orientation) : BaseListTestWithOrientation(orientation) {

    @Test
    fun scrollToItemOnState() {
        val state = ListState()
        rule.setGlimmerThemeContent { TestList(state = state) { Text("Item ($it)") } }

        // Make sure the far items are not laid out.
        val targetIndex = Int.MAX_VALUE / 2
        rule.onNodeWithText("Item ($targetIndex)").assertDoesNotExist()

        // Scroll to the far index and check that the item become visible.
        rule.runOnUiThread { runBlocking { state.scrollToItem(targetIndex) } }
        rule.onNodeWithText("Item ($targetIndex)").assertIsDisplayed()
    }

    @Test
    fun dispatchRawDeltaOnState() {
        val state = ListState()
        val sizeInDp = 100.dp
        val sizeInPx = with(rule.density) { sizeInDp.toPx() }

        rule.setGlimmerThemeContent {
            TestList(state = state) {
                Text(text = "Item ($it)", modifier = Modifier.size(sizeInDp))
            }
        }

        rule.runOnUiThread { state.dispatchRawDelta(300 * sizeInPx) }
        rule.onNodeWithText("Item (300)").assertIsDisplayed()
    }

    @Test
    fun scrollByOnState() {
        val state = ListState()
        rule.setGlimmerThemeContent {
            TestList(
                state = state,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag("item-tag-$index"))
                },
            )
        }

        // Scroll the first three elements.
        val scrollInPx = with(rule.density) { 50.dp.toPx() }
        rule.runOnUiThread { runBlocking { state.scrollBy(scrollInPx) } }

        // Check that the 4th element is almost first in the list.
        val childRect = rule.onNodeWithTag("item-tag-3").getBoundsInRoot()
        val offset: Dp = if (vertical) childRect.top else childRect.left
        offset.assertIsEqualTo(expected = 10.dp, tolerance = 1.dp)
    }

    @Test
    fun saveAndRestoreState() {
        val allowingScope = SaverScope { true }
        val original = ListState(firstVisibleItemIndex = 42, firstVisibleItemScrollOffset = 100)

        val saved = with(ListState.Saver) { allowingScope.save(original) }
        val restored = ListState.Saver.restore(requireNotNull(saved))

        assertThat(restored?.firstVisibleItemIndex).isEqualTo(original.firstVisibleItemIndex)
        assertThat(restored?.firstVisibleItemScrollOffset)
            .isEqualTo(original.firstVisibleItemScrollOffset)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
