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

package androidx.compose.remote.creation.compose.state

import android.content.Context
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.remote.player.core.state.RemoteDomains
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemoteColorTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun copy_rememberRemoteColor_resultsSingleNamedColor() = runBlocking {
        val colorName = "TEST"
        val coreDoc =
            remoteComposeTestRule.captureDocument(context = context) {
                val color = rememberRemoteColor(colorName) { Color.Red }

                val copy = color.copy(alpha = 0f.rf)

                val creationState = LocalRemoteComposeCreationState.current
                copy.getIdForCreationState(creationState)
            }
        assertThat(coreDoc.namedColors).hasLength(1)
        assertThat(coreDoc.namedColors[0]).isEqualTo("${RemoteDomains.USER}:$colorName")
    }
}
