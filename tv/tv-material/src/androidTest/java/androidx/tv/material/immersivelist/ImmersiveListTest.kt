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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.material.ExperimentalTvMaterialApi
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

    private fun keyPress(keyCode: Int, numberOfPresses: Int = 1) {
        for (index in 0 until numberOfPresses)
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode)
    }
}