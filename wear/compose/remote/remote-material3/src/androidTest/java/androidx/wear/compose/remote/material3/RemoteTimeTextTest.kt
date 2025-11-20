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
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.compose.capture.captureRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteTimeTextTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun timeOnly() = runTest {
        runDocumentTest {
            RemoteTimeText(modifier = RemoteModifier.fillMaxSize(), time = RemoteString("10:09"))
        }
    }

    @Test
    fun timeWithText() = runTest {
        runDocumentTest {
            RemoteTimeText(
                modifier = RemoteModifier.fillMaxSize(),
                time = RemoteString("10:09"),
                leadingText = RemoteString("paused"),
                trailingText = RemoteString("eta 13 min"),
            )
        }
    }

    suspend fun runDocumentTest(content: @Composable @RemoteComposable () -> Unit) {
        val customProfile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROID_NATIVE,
                AndroidxRcPlatformServices(),
            ) { width, height, contentDescription, profile ->
                RcPlatformProfiles.ANDROIDX.profileFactory
                    .create(width, height, contentDescription, profile)
                    .apply {
                        buffer.setVersion(
                            CoreDocument.DOCUMENT_API_LEVEL,
                            setOf(Operations.DRAW_TEXT_ON_CIRCLE),
                        )
                    }
            }
        val bytes =
            withContext(Dispatchers.Main) {
                captureRemoteDocument(context, profile = customProfile) { content() }
            }
        assertTrue(bytes.isNotEmpty())
    }
}
