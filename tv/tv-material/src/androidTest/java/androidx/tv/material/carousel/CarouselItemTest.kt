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

package androidx.tv.material.carousel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material.ExperimentalTvMaterialApi
import org.junit.Rule
import org.junit.Test

class CarouselItemTest {
    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalTvMaterialApi::class)
    @Test
    fun carouselItem_overlayVisibleAfterRenderTime() {
        val overlayEnterTransitionStartDelay: Long = 2000
        val overlayTag = "overlay"
        val backgroundTag = "background"
        rule.setContent {
            CarouselItem(
                overlayEnterTransitionStartDelayMillis = overlayEnterTransitionStartDelay,
                background = {
                    Box(
                        Modifier
                            .testTag(backgroundTag)
                            .size(200.dp)
                            .background(Color.Blue)) }) {
                Box(
                    Modifier
                        .testTag(overlayTag)
                        .size(50.dp)
                        .background(Color.Red))
            }
        }

        // only background is visible
        rule.onNodeWithTag(backgroundTag).assertExists()
        rule.onNodeWithTag(overlayTag).assertDoesNotExist()

        // advance clock by `overlayEnterTransitionStartDelay`
        rule.mainClock.advanceTimeBy(overlayEnterTransitionStartDelay)

        rule.onNodeWithTag(backgroundTag).assertExists()
        rule.onNodeWithTag(overlayTag).assertExists()
    }

    @OptIn(ExperimentalTvMaterialApi::class)
    @Test
    fun carouselItem_parentContainerGainsFocused_onBackPress() {
        rule.setContent {
            Box(modifier = Modifier
                .testTag("box-container")
                .fillMaxSize()
                .focusable()) {
                CarouselItem(
                    overlayEnterTransitionStartDelayMillis = 0,
                    modifier = Modifier.testTag("carousel-item"),
                    background = { Box(Modifier.size(300.dp).background(Color.Cyan)) }
                ) {
                    SampleButton()
                }
            }
        }

        // Request focus for Carousel Item on start
        rule.onNodeWithTag("carousel-item")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()

        // Check if overlay button in carousel item is focused
        rule.onNodeWithTag("sample-button").assertIsFocused()

        // Trigger back press
        performKeyPress(NativeKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()

        // Check if carousel item loses focus and parent container gains focus
        rule.onNodeWithTag("box-container").assertIsFocused()
    }

    @Composable
    private fun SampleButton(text: String = "sample-button") {
        var isFocused by remember { mutableStateOf(false) }
        BasicText(
            text = text,
            modifier = Modifier.testTag(text)
                .size(100.dp, 20.dp)
                .background(Color.Yellow)
                .onFocusChanged { isFocused = it.isFocused }
                .border(2.dp, if (isFocused) Color.Green else Color.Transparent)
                .focusable()
        )
    }

    private fun performKeyPress(keyCode: Int, count: Int = 1) {
        repeat(count) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode)
        }
    }
}
