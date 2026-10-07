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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteSegmentedCircularProgressBinary
import androidx.wear.compose.remote.material3.previews.RemoteSegmentedCircularProgressCustomAngle
import androidx.wear.compose.remote.material3.previews.RemoteSegmentedCircularProgressCustomColor
import androidx.wear.compose.remote.material3.previews.RemoteSegmentedCircularProgressDisabled
import androidx.wear.compose.remote.material3.previews.RemoteSegmentedCircularProgressDot
import androidx.wear.compose.remote.material3.previews.RemoteSegmentedCircularProgressFull
import androidx.wear.compose.remote.material3.previews.RemoteSegmentedCircularProgressHalf
import androidx.wear.compose.remote.material3.previews.RemoteSegmentedCircularProgressPartialSegment
import androidx.wear.compose.remote.material3.previews.RemoteSegmentedCircularProgressSmallStroke
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteSegmentedCircularProgressIndicatorTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test fun segmented_progress_half() = screenshotTest { RemoteSegmentedCircularProgressHalf() }

    @Test
    fun segmented_progress_partial_segment() = screenshotTest {
        RemoteSegmentedCircularProgressPartialSegment()
    }

    @Test fun segmented_progress_dot() = screenshotTest { RemoteSegmentedCircularProgressDot() }

    @Test fun segmented_progress_full() = screenshotTest { RemoteSegmentedCircularProgressFull() }

    @Test
    fun segmented_progress_binary() = screenshotTest { RemoteSegmentedCircularProgressBinary() }

    @Test
    fun segmented_progress_custom_angle() = screenshotTest {
        RemoteSegmentedCircularProgressCustomAngle()
    }

    @Test
    fun segmented_progress_custom_color() = screenshotTest {
        RemoteSegmentedCircularProgressCustomColor()
    }

    @Test
    fun segmented_progress_disabled() = screenshotTest {
        RemoteSegmentedCircularProgressDisabled()
    }

    @Test
    fun segmented_progress_small_stroke() = screenshotTest {
        RemoteSegmentedCircularProgressSmallStroke()
    }

    private fun screenshotTest(content: @Composable () -> Unit) {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { content() }
        }
    }
}
