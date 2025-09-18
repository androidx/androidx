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
package androidx.compose.remote.foundation

import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.foundation.icons.RemoteImageVector
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.size
import androidx.compose.remote.frontend.state.rememberRemoteColor
import androidx.compose.remote.test.screenshot.TargetPlayer
import androidx.compose.remote.test.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteIconTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )

    @Test
    fun volumeUpRemoteIcon() {
        remoteComposeTestRule.runScreenshotTest(size = size, backgroundColor = Color.Black) {
            RemoteIcon(imageVector = VolumeUp, contentDescription = null)
        }
    }

    @Test
    fun volumeUpRemoteIcon_tintedRed() {
        remoteComposeTestRule.runScreenshotTest(size = size, backgroundColor = Color.Black) {
            val color = rememberRemoteColor("testColor") { Color.Red }
            RemoteIcon(imageVector = VolumeUp, contentDescription = null, tint = color)
        }
    }

    @Test
    fun volumeUpRemoteIcon_rtl() {
        remoteComposeTestRule.runScreenshotTest(size = size, backgroundColor = Color.Black) {
            val layoutDirection = LayoutDirection.Rtl
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                RemoteIcon(imageVector = VolumeUp, contentDescription = null)
            }
        }
    }

    @Test
    fun volumeUpRemoteIcon_scaledUp() {
        remoteComposeTestRule.runScreenshotTest(
            size = Size(48.dp.value, 48.dp.value),
            backgroundColor = Color.Black,
        ) {
            RemoteIcon(
                imageVector = VolumeUp,
                contentDescription = null,
                modifier = RemoteModifier.size(48.dp),
            )
        }
    }

    companion object {
        val size = Size(24.dp.value, 24.dp.value)
        val VolumeUp =
            object : RemoteImageVector(autoMirror = true) {
                override fun RemotePath.buildPath() {
                    moveTo(3.0f, 9.0f)
                    verticalLineToRelative(6.0f)
                    horizontalLineToRelative(4.0f)
                    lineToRelative(5.0f, 5.0f)
                    lineTo(12.0f, 4.0f)
                    lineTo(7.0f, 9.0f)
                    lineTo(3.0f, 9.0f)
                    close()
                    moveTo(16.5f, 12.0f)
                    curveToRelative(0.0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
                    verticalLineToRelative(8.05f)
                    curveToRelative(1.48f, -0.73f, 2.5f, -2.25f, 2.5f, -4.02f)
                    close()
                    moveTo(14.0f, 3.23f)
                    verticalLineToRelative(2.06f)
                    curveToRelative(2.89f, 0.86f, 5.0f, 3.54f, 5.0f, 6.71f)
                    reflectiveCurveToRelative(-2.11f, 5.85f, -5.0f, 6.71f)
                    verticalLineToRelative(2.06f)
                    curveToRelative(4.01f, -0.91f, 7.0f, -4.49f, 7.0f, -8.77f)
                    reflectiveCurveToRelative(-2.99f, -7.86f, -7.0f, -8.77f)
                    close()
                }
            }
    }
}
