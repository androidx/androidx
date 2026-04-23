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
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RotateModifierScreenshotTest {
    @get:Rule
    val composeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            matcher = MSSIMMatcher(threshold = 0.999),
        )
    }

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun grid() =
        composeTestRule.runScreenshotTest {
            val tests =
                listOf<Pair<String, @RemoteComposable @Composable () -> Unit>>(
                    "rotate(0f)" to
                        @Composable @RemoteComposable { Content(RemoteModifier.rotate(0f.rf)) },
                    "rotate(45f)" to
                        @Composable @RemoteComposable { Content(RemoteModifier.rotate(45f.rf)) },
                    "rotate(60f)" to
                        @Composable @RemoteComposable { Content(RemoteModifier.rotate(60f.rf)) },
                    "rotate(90f)" to
                        @Composable @RemoteComposable { Content(RemoteModifier.rotate(90f.rf)) },
                    "rotate(-45f)" to
                        @Composable @RemoteComposable { Content(RemoteModifier.rotate((-45f).rf)) },
                )

            gridScreenshotUI.GridContent(tests)
        }

    @Composable
    @RemoteComposable
    private fun Content(testModifier: RemoteModifier) {
        RemoteBox(
            modifier = RemoteModifier.size(50.rdp).background(Color.Red),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteBox(modifier = testModifier.size(20.rdp).background(Color.Blue))
        }
    }
}
