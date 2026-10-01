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

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that [RemoteEmbeddedScreenshotTestRule] enables and plays content with the embedded
 * player ([RcPlayer]).
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemoteEmbeddedScreenshotTestRuleTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(100f, 100f))

    @Test
    fun embeddedPlayerIsEnabled() {
        assertThat(RemoteComposePlayerFlags.isEmbeddedPlayerEnabled).isTrue()
    }

    @Test
    fun setContent_rendersContent() {
        remoteComposeTestRule.setContent(remoteCreationDisplayInfo = creationDisplayInfo) {
            RemoteBox(RemoteModifier.fillMaxSize().background(Color.Red.rc))
        }
        remoteComposeTestRule.composeTestRule.waitForIdle()

        val bitmap =
            remoteComposeTestRule.composeTestRule
                .onNodeWithTag(RemoteEmbeddedScreenshotTestRule.ROOT_TEST_TAG)
                .captureToImage()
                .asAndroidBitmap()

        assertThat(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)).isEqualTo(AndroidColor.RED)
    }
}
