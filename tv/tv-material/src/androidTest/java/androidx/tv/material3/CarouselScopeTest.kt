/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

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
import org.junit.Rule
import org.junit.Test

const val sampleButtonTag = "sample-button"

class CarouselScopeTest {
    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Test
    fun carouselItem_parentContainerGainsFocused_onBackPress() {
        val containerBoxTag = "container-box"
        val carouselItemTag = "carousel-item"

        rule.setContent {
            val carouselState = remember { CarouselState() }
            var isContainerBoxFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .testTag(containerBoxTag)
                    .fillMaxSize()
                    .onFocusChanged { isContainerBoxFocused = it.isFocused }
                    .border(10.dp, if (isContainerBoxFocused) Color.Green else Color.Transparent)
                    .focusable()
            ) {
                CarouselScope(carouselState = carouselState)
                    .CarouselItem(
                        modifier = Modifier
                            .testTag(carouselItemTag),
                        background = {
                            Box(
                                modifier = Modifier
                                    .size(300.dp)
                                    .background(Color.Cyan))
                        },
                        content = { SampleButton() },
                    )
            }
        }

        // Request focus for Carousel Item on start
        rule.onNodeWithTag(carouselItemTag)
            .performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()

        // Check if overlay button in carousel item is focused
        rule.onNodeWithTag(sampleButtonTag, useUnmergedTree = true)
            .assertIsFocused()

        // Trigger back press
        performKeyPress(NativeKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()

        // Check if carousel item loses focus and parent container gains focus
        rule.onNodeWithTag(containerBoxTag).assertIsFocused()
    }

    private fun performKeyPress(keyCode: Int, count: Int = 1) {
        repeat(count) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode)
        }
    }
}

@Composable
private fun SampleButton(text: String = sampleButtonTag) {
    var isFocused by remember { mutableStateOf(false) }
    BasicText(
        text = text,
        modifier = Modifier
            .testTag(text)
            .size(100.dp, 20.dp)
            .background(Color.Yellow)
            .onFocusChanged { isFocused = it.isFocused }
            .border(2.dp, if (isFocused) Color.Green else Color.Transparent)
            .focusable()
    )
}
