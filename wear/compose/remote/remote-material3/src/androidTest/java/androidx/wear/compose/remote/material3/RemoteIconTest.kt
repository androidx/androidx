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
package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestImageVectors
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
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun volumeUpRemoteIcon() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = createCreationDisplayInfo(context, size.toSize()),
            backgroundColor = Color.Black,
        ) {
            RemoteIcon(imageVector = TestImageVectors.VolumeUp, contentDescription = null)
        }
    }

    @Test
    fun volumeUpRemoteIcon_tintedRed() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = createCreationDisplayInfo(context, size.toSize()),
            backgroundColor = Color.Black,
        ) {
            RemoteIcon(
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                tint = RemoteColor(Color.Red),
            )
        }
    }

    @Test
    fun volumeUpRemoteIcon_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = createCreationDisplayInfo(context, size.toSize()),
            backgroundColor = Color.Black,
            layoutDirection = LayoutDirection.Rtl,
        ) {
            RemoteIcon(imageVector = TestImageVectors.VolumeUp, contentDescription = null)
        }
    }

    @Test
    fun volumeUpRemoteIcon_scaledUp() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = createCreationDisplayInfo(context, Size(48f, 48f)),
            backgroundColor = Color.Black,
        ) {
            RemoteIcon(
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                modifier = RemoteModifier.size(48.rdp),
            )
        }
    }

    @Test
    fun remoteIcon_fromImageVector() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = createCreationDisplayInfo(context, Size(48f, 48f)),
            backgroundColor = Color.Black,
        ) {
            RemoteIcon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                modifier = RemoteModifier.size(48.rdp),
            )
        }
    }

    @Test
    fun remoteIcon_setBorderSizeUnchanged() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = createCreationDisplayInfo(context, Size(200f, 100f)),
            backgroundColor = Color.Black,
        ) {
            RemoteRow {
                RemoteIcon(imageVector = TestImageVectors.VolumeUp, contentDescription = null)
                RemoteIcon(
                    imageVector = TestImageVectors.VolumeUp,
                    contentDescription = null,
                    modifier = RemoteModifier.border(1.rdp, Color.Yellow.rc),
                )
            }
        }
    }

    companion object {
        val size = IntSize(24, 24)
    }
}
