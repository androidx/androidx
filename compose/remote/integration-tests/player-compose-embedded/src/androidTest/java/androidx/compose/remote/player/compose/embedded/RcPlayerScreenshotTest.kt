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
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteFlowRow
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
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
class RcPlayerScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun basicLayout() {
        rule.setContent {
            val document =
                rememberRemoteDocument(profile = TEST_PROFILE) {
                    RemoteColumn(
                        modifier = RemoteModifier.size(200.rdp).border(1.rdp, Color.Magenta.rc)
                    ) {
                        RemoteColumn(
                            modifier = RemoteModifier.fillMaxSize().border(1.rdp, Color.Blue.rc)
                        ) {
                            RemoteText("Column 1".rs, color = Color.Green.rc)
                            RemoteText("Column 2".rs)
                        }
                        RemoteColumn(
                            modifier = RemoteModifier.fillMaxSize().border(1.rdp, Color.Blue.rc)
                        ) {
                            RemoteText("Row 1".rs)
                            RemoteText("Row 2".rs)
                        }
                    }
                }

            Box(modifier = Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.onRoot().captureToImage().assertAgainstGolden(screenshotRule, "basicLayout")
    }

    @Test
    fun complexLayout() {
        rule.setContent {
            val document =
                rememberRemoteDocument(profile = TEST_PROFILE) {
                    RemoteColumn(
                        modifier = RemoteModifier.fillMaxSize().background(Color.LightGray.rc)
                    ) {
                        RemoteFlowRow(
                            modifier = RemoteModifier.padding(8.rdp).border(2.rdp, Color.Black.rc)
                        ) {
                            RemoteText(
                                "Flow 1".rs,
                                modifier = RemoteModifier.background(Color.Red.rc).padding(4.rdp),
                            )
                            RemoteText(
                                "Flow 2".rs,
                                modifier = RemoteModifier.background(Color.Green.rc).padding(4.rdp),
                            )
                            RemoteText(
                                "Flow 3".rs,
                                modifier = RemoteModifier.background(Color.Blue.rc).padding(4.rdp),
                            )
                        }
                        RemoteRow(modifier = RemoteModifier.padding(16.rdp).clip()) {
                            RemoteBox(
                                modifier = RemoteModifier.size(50.rdp).background(Color.Yellow.rc)
                            )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(50.rdp)
                                        .offset(10.rdp, 10.rdp)
                                        .background(Color.Cyan.rc)
                            )
                        }
                    }
                }

            Box(modifier = Modifier.size(200.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.onRoot().captureToImage().assertAgainstGolden(screenshotRule, "complexLayout")
    }
}
