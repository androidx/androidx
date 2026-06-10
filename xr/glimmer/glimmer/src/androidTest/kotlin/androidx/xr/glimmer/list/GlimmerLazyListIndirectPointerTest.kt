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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.performIndirectSwipe
import androidx.xr.glimmer.setGlimmerThemeContent
import androidx.xr.glimmer.testutils.NoFlingBehavior
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class GlimmerLazyListIndirectPointerTest :
    BaseGlimmerLazyListTestWithOrientation(Orientation.Vertical) {

    @Test
    fun performIndirectScrollForward() {
        val density = Density(1f)
        rule.setGlimmerThemeContent(density = density) {
            TestList(
                itemsCount = 100,
                modifier = Modifier.height(100.dp),
                flingBehavior = NoFlingBehavior,
            ) { index ->
                Text(text = "Item-$index", modifier = Modifier.height(100.dp).focusable())
            }
        }

        rule.onNodeWithText("Item-0").assertIsDisplayed()
        rule.onNodeWithText("Item-1").assertIsNotDisplayed()

        // 900 dp for scroll (9 items) and 50 dp for focus (the focus line will be in the center).
        val swipeDistance = with(density) { 950.dp.toPx() }
        rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(rule, swipeDistance)

        rule.onNodeWithText("Item-8").assertIsNotDisplayed()
        rule.onNodeWithText("Item-9").assertIsDisplayed()
        rule.onNodeWithText("Item-10").assertIsNotDisplayed()
    }

    @Test
    fun performIndirectScrollBackward() {
        val density = Density(1f)
        rule.setGlimmerThemeContent(density = density) {
            TestList(
                itemsCount = 100,
                modifier = Modifier.height(100.dp),
                flingBehavior = NoFlingBehavior,
            ) { index ->
                Text(text = "Item-$index", modifier = Modifier.height(100.dp).focusable())
            }
        }

        rule.onNodeWithTag(LIST_TEST_TAG).performScrollToIndex(99)

        rule.onNodeWithText("Item-98").assertIsNotDisplayed()
        rule.onNodeWithText("Item-99").assertIsDisplayed()

        // 900 dp for scroll (9 items) and 50 dp for focus (the focus line will be in the center).
        val swipeDistance = with(density) { 950.dp.toPx() }
        rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(rule, -swipeDistance)

        rule.onNodeWithText("Item-89").assertIsNotDisplayed()
        rule.onNodeWithText("Item-90").assertIsDisplayed()
        rule.onNodeWithText("Item-91").assertIsNotDisplayed()
    }
}
