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

package androidx.tv.material.immersivelist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.material.ExperimentalTvMaterialApi
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class ImmersiveListTest {
    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalTvMaterialApi::class, ExperimentalAnimationApi::class)
    @Test
    fun immersiveList_scroll_backgroundChanges() {
        val firstCard = FocusRequester()
        val secondCard = FocusRequester()

        rule.setContent {
            ImmersiveList(
                background = { index, _ ->
                    AnimatedContent(targetState = index) {
                        Box(
                            Modifier
                                .testTag("background-$it")
                                .size(200.dp)) {
                            BasicText("background-$it")
                        }
                    }
                }) {
                    TvLazyRow {
                        items(3) { index ->
                            var modifier = Modifier
                                .testTag("card-$index")
                                .size(100.dp)
                            when (index) {
                                0 -> modifier = modifier
                                    .focusRequester(firstCard)
                                1 -> modifier = modifier
                                    .focusRequester(secondCard)
                            }

                            Box(modifier.focusableItem(index)) { BasicText("card-$index") }
                        }
                    }
            }
        }

        rule.runOnIdle { firstCard.requestFocus() }

        rule.onNodeWithTag("card-0").assertIsFocused()
        rule.onNodeWithTag("background-0").assertIsDisplayed()
        rule.onNodeWithTag("background-1").assertDoesNotExist()
        rule.onNodeWithTag("background-2").assertDoesNotExist()

        rule.waitForIdle()
        keyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        rule.onNodeWithTag("card-1").assertIsFocused()
        rule.onNodeWithTag("background-1").assertIsDisplayed()
        rule.onNodeWithTag("background-0").assertDoesNotExist()
        rule.onNodeWithTag("background-2").assertDoesNotExist()

        rule.waitForIdle()
        keyPress(NativeKeyEvent.KEYCODE_DPAD_LEFT)

        rule.onNodeWithTag("card-0").assertIsFocused()
        rule.onNodeWithTag("background-0").assertIsDisplayed()
        rule.onNodeWithTag("background-1").assertDoesNotExist()
        rule.onNodeWithTag("background-2").assertDoesNotExist()
    }

    @Test
    fun immersiveList_scrollToRegainFocus_checkBringIntoView() {
        val focusRequesterList = mutableListOf<FocusRequester>()
        for (item in 0..2) { focusRequesterList.add(FocusRequester()) }
        setupContent(focusRequesterList)

        // Initially first focusable element would be focused
        rule.waitForIdle()
        rule.onNodeWithTag("test-card-0").assertIsFocused()

        // Scroll down to the Immersive List's first card
        keyPress(NativeKeyEvent.KEYCODE_DPAD_DOWN, 3)
        rule.waitForIdle()
        rule.onNodeWithTag("list-card-0").assertIsFocused()
        rule.onNodeWithTag("immersive-list").assertIsDisplayed()
        assertThat(checkNodeCompletelyVisible("immersive-list")).isTrue()

        // Scroll down to last element, making sure the immersive list is partially visible
        keyPress(NativeKeyEvent.KEYCODE_DPAD_DOWN, 2)
        rule.waitForIdle()
        rule.onNodeWithTag("test-card-4").assertIsFocused()
        rule.onNodeWithTag("immersive-list").assertIsDisplayed()

        // Scroll back to the immersive list to check if it's brought into view on regaining focus
        keyPress(NativeKeyEvent.KEYCODE_DPAD_UP, 2)
        rule.waitForIdle()
        rule.onNodeWithTag("immersive-list").assertIsDisplayed()
        assertThat(checkNodeCompletelyVisible("immersive-list")).isTrue()
    }

    private fun checkNodeCompletelyVisible(tag: String): Boolean {
        rule.waitForIdle()

        val rootRect = rule.onRoot().getUnclippedBoundsInRoot()
        val itemRect = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

        return itemRect.left >= rootRect.left &&
            itemRect.right <= rootRect.right &&
            itemRect.top >= rootRect.top &&
            itemRect.bottom <= rootRect.bottom
    }

    private fun setupContent(focusRequesterList: List<FocusRequester>) {
        val focusRequester = FocusRequester()
        rule.setContent {
            LazyColumn() {
                items(3) {
                    val modifier =
                        if (it == 0) Modifier.focusRequester(focusRequester)
                        else Modifier
                    BasicText(
                        text = "test-card-$it",
                        modifier = modifier
                            .testTag("test-card-$it")
                            .size(200.dp)
                            .focusable()
                    )
                }
                item { TestImmersiveList(focusRequesterList) }
                items(2) {
                    BasicText(
                        text = "test-card-${it + 3}",
                        modifier = Modifier
                            .testTag("test-card-${it + 3}")
                            .size(200.dp)
                            .focusable()
                    )
                }
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }
    }

    @OptIn(ExperimentalTvMaterialApi::class, ExperimentalAnimationApi::class)
    @Composable
    private fun TestImmersiveList(focusRequesterList: List<FocusRequester>) {
        val frList = remember { focusRequesterList }
        ImmersiveList(
            background = { index, _ ->
                AnimatedContent(targetState = index) {
                    Box(
                        Modifier
                            .testTag("background-$it")
                            .fillMaxWidth()
                            .height(400.dp)
                            .border(2.dp, Color.Black, RectangleShape)
                    ) {
                        BasicText("background-$it")
                    }
                }
            },
            modifier = Modifier.testTag("immersive-list")
        ) {
            TvLazyRow {
                items(frList.count()) { index ->
                    var modifier = Modifier
                        .testTag("list-card-$index")
                        .size(50.dp)
                    for (item in frList) {
                        modifier = modifier.focusRequester(frList[index])
                    }
                    Box(modifier.focusableItem(index)) { BasicText("list-card-$index") }
                }
            }
        }
    }

    private fun keyPress(keyCode: Int, numberOfPresses: Int = 1) {
        for (index in 0 until numberOfPresses)
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode)
    }
}