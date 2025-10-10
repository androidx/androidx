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

package androidx.compose.remote.wear.material3

import androidx.compose.remote.creation.compose.state.rememberRemoteString
import androidx.compose.remote.test.screenshot.TargetPlayer
import androidx.compose.remote.test.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.material3.LocalTextStyle
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteTextTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )

    @Test
    fun simpleText() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString { "simpleText" }
            RemoteText(text, color = Color.White)
        }
    }

    @Test
    fun text_withStyle() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString { "textWithStyle" }
            RemoteText(
                text,
                style = LocalTextStyle.current.copy(fontSize = 36.sp, color = Color.Green),
            )
        }
    }

    @Test
    fun text_withParams() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString { "textWithParams" }
            RemoteText(text, fontSize = 36.sp, color = Color.Green)
        }
    }

    @Test
    fun text_withParamsAndStyle_shouldUseParams() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString { "text_withParamAndStyle_shouldUseParam" }
            RemoteText(
                text,
                color = Color.Green,
                fontSize = 36.sp,
                style = LocalTextStyle.current.copy(color = Color.Red, fontSize = 12.sp),
            )
        }
    }

    @Test
    fun text_withParams_individualParamIsApplied() {
        remoteComposeTestRule.runScreenshotTest(backgroundColor = Color.Black) {
            val text = rememberRemoteString { "text_withParams_individualParamIsApplied" }
            RemoteText(
                text,
                color = Color.Green,
                fontSize = 36.sp,
                fontStyle = FontStyle.Italic,
                style = LocalTextStyle.current.copy(color = Color.Red, fontSize = 12.sp),
            )
        }
    }
}
