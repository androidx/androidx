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

@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
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
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun timeOnly() = runTest {
        runDocumentTest {
            RemoteTimeText(modifier = RemoteModifier.fillMaxSize(), time = "10:09".rs)
        }
    }

    @Test
    fun timeWithText() = runTest {
        runDocumentTest {
            RemoteTimeText(
                modifier = RemoteModifier.fillMaxSize(),
                time = "10:09".rs,
                leadingText = "paused".rs,
                trailingText = "eta 13 min".rs,
            )
        }
    }

    @Test
    fun withFontConfigured() = runTest {
        runDocumentTest {
            RemoteTimeText(
                modifier = RemoteModifier.fillMaxSize(),
                time = "10:09".rs,
                fontSize = 15.rsp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }

    suspend fun runDocumentTest(content: @Composable @RemoteComposable () -> Unit) {
        val bytes =
            withContext(Dispatchers.Main) {
                captureSingleRemoteDocument(context, profile = TestProfiles.androidNativeProfile) {
                        content()
                    }
                    .bytes
            }
        assertTrue(bytes.isNotEmpty())
    }
}
