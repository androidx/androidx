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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class RcPlayerFirstFrameTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun testFirstFrameBlankWithoutClockAdvance() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val document = rememberRemoteDocument {
                // A box that draws a circle based on its size
                RemoteBox(modifier = RemoteModifier.fillMaxSize()) {
                    androidx.compose.remote.creation.compose.layout.RemoteCanvas(
                        modifier = RemoteModifier.fillMaxSize()
                    ) {
                        val paint =
                            androidx.compose.remote.creation.compose.state.RemotePaint().apply {
                                color = Color.Red.rc
                            }
                        // Draw a circle with radius as half of width
                        drawCircle(paint = paint, radius = width / 2f)
                    }
                }
            }

            Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        // We do NOT advance the clock here!
        // Expectation: It will be blank because onGloballyPositioned hasn't updated sizes yet!
        rule.onRoot().captureToImage().assertAgainstGolden(screenshotRule, "firstFrameBlank")
    }
}
