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
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.runtime.Composable
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.TestProfiles
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class RemoteTimeTextTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun timeOnly() =
        runTest(UnconfinedTestDispatcher()) {
            runDocumentTest {
                RemoteTimeText(modifier = RemoteModifier.fillMaxSize(), time = "10:09".rs)
            }
        }

    @Test
    fun timeWithText() =
        runTest(UnconfinedTestDispatcher()) {
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
    fun withFontConfigured() =
        runTest(UnconfinedTestDispatcher()) {
            runDocumentTest {
                RemoteTimeText(
                    modifier = RemoteModifier.fillMaxSize(),
                    time = "10:09".rs,
                    fontSize = 15.rsp,
                    fontFamily = RemoteFontFamily.SansSerif,
                )
            }
        }

    private suspend fun runDocumentTest(content: @Composable @RemoteComposable () -> Unit) {
        val bytes =
            captureSingleRemoteDocument(context, profile = TestProfiles.androidNativeProfile) {
                    content()
                }
                .bytes
        assertTrue(bytes.isNotEmpty())
    }
}
