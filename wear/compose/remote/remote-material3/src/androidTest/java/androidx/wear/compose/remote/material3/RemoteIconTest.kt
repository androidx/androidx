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
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.toRemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
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
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun volumeUpRemoteIcon() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    size,
                    size,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            RemoteIcon(imageVector = TestImageVectors.VolumeUp, contentDescription = null)
        }
    }

    @Test
    fun volumeUpRemoteIcon_tintedRed() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    size,
                    size,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            RemoteIcon(
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                tint = RemoteColor.Red,
            )
        }
    }

    @Test
    fun volumeUpRemoteIcon_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    size,
                    size,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            creationComposableWrapper = ComposableWrappers.rtl,
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            RemoteIcon(imageVector = TestImageVectors.VolumeUp, contentDescription = null)
        }
    }

    @Test
    fun volumeUpRemoteIcon_scaledUp() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    48,
                    48,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
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
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    48,
                    48,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            RemoteIcon(
                imageVector = Icons.Filled.Favorite.toRemoteImageVector(),
                contentDescription = null,
                modifier = RemoteModifier.size(48.rdp),
            )
        }
    }

    @Test
    fun remoteIcon_setBorderSizeUnchanged() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    200,
                    100,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            RemoteRow {
                RemoteIcon(imageVector = TestImageVectors.VolumeUp, contentDescription = null)
                RemoteIcon(
                    imageVector = TestImageVectors.VolumeUp,
                    contentDescription = null,
                    modifier = RemoteModifier.border(1.rdp, RemoteColor.Yellow),
                )
            }
        }
    }

    companion object {
        const val size = 24
    }
}
