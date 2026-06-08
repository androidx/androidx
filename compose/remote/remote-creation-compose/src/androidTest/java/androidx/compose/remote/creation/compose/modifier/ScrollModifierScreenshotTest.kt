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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.test.uiautomator.uiAutomator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class ScrollModifierScreenshotTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val colors =
        listOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD), Color(0xFF9E9E9E), Color(0xFF757575))

    @Test
    fun verticalScroll() {
        composeTestRule.setContent {
            val scrollState = rememberRemoteScrollState()
            RemoteColumn(modifier = RemoteModifier.verticalScroll(scrollState).fillMaxSize()) {
                repeat(4) { index ->
                    val color = colors[index % colors.size].rc
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxWidth()
                                .fillParentMaxHeight(0.5f.rf)
                                .background(color),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText("Item #$index", color = Color.White.rc)
                    }
                }
            }
        }
        uiAutomator {
            val cx = device.displayWidth / 2
            val startY = (device.displayHeight * 0.8f).toInt()
            val endY = (device.displayHeight * 0.2f).toInt()
            repeat(3) {
                device.drag(cx, startY, cx, endY, 20)
                device.waitForIdle()
            }
        }
        composeTestRule.composeTestRule.waitForIdle()
        composeTestRule.verifyScreenshot()
    }

    @Test
    fun horizontalScroll() {
        composeTestRule.setContent {
            val scrollState = rememberRemoteScrollState()
            RemoteRow(modifier = RemoteModifier.horizontalScroll(scrollState).fillMaxSize()) {
                repeat(4) { index ->
                    val color = colors[index % colors.size].rc
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxHeight()
                                .fillParentMaxWidth(0.5f.rf)
                                .background(color),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText("Item #$index", color = Color.White.rc)
                    }
                }
            }
        }
        uiAutomator {
            val cx = device.displayWidth / 2
            val cy = device.displayHeight / 2
            repeat(3) {
                device.drag(cx + 200, cy, cx - 200, cy, 20)
                device.waitForIdle()
            }
        }
        composeTestRule.composeTestRule.waitForIdle()
        composeTestRule.verifyScreenshot()
    }

    @Test
    fun horizontalScrollWithPosition() {
        composeTestRule.setContent {
            val scrollState = rememberRemoteScrollState()
            RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                RemoteRow(
                    modifier =
                        RemoteModifier.horizontalScroll(scrollState).fillMaxWidth().weight(1f.rf)
                ) {
                    repeat(4) { index ->
                        val color = colors[index % colors.size].rc
                        RemoteBox(
                            modifier =
                                RemoteModifier.fillMaxHeight()
                                    .fillParentMaxWidth(0.5f.rf)
                                    .background(color),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteText("Item #$index", color = Color.White.rc)
                        }
                    }
                }
                val hasScrolled = scrollState.positionState.isGreaterThan(100f.rf)
                RemoteText(
                    text = "Position: ".rs + hasScrolled.select("Scrolled".rs, "Static".rs),
                    modifier = RemoteModifier.padding(16.rdp),
                    color = Color.Black.rc,
                )
            }
        }
        uiAutomator {
            val cx = device.displayWidth / 2
            val cy = device.displayHeight / 2
            repeat(3) {
                device.drag(cx + 200, cy, cx - 200, cy, 20)
                device.waitForIdle()
            }
        }
        composeTestRule.composeTestRule.waitForIdle()
        composeTestRule.verifyScreenshot()
    }

    @Test
    fun horizontalScrollStateChange() {
        composeTestRule.setContent {
            val scrollState = rememberRemoteScrollState()
            RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                RemoteRow(
                    modifier =
                        RemoteModifier.horizontalScroll(scrollState).fillMaxWidth().weight(1f.rf)
                ) {
                    repeat(4) { index ->
                        val color = colors[index % colors.size].rc
                        RemoteBox(
                            modifier =
                                RemoteModifier.fillMaxHeight()
                                    .fillParentMaxWidth(0.5f.rf)
                                    .background(color),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteText("Item #$index", color = Color.White.rc)
                        }
                    }
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .weight(1f.rf)
                            .background(Color.LightGray.rc)
                            .clickable(valueChange(scrollState.positionState, 200f.rf)),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("Scroll Button", color = Color.Black.rc)
                }
            }
        }
        uiAutomator {
            onElement { text == "Scroll Button" }.click()
            device.waitForIdle()
        }
        composeTestRule.composeTestRule.waitForIdle()
        composeTestRule.verifyScreenshot()
    }
}
