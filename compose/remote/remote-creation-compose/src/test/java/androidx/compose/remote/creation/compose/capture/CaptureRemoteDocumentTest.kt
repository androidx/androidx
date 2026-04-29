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

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric test of [RecordingCanvas]. */
@OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class CaptureRemoteDocumentTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun captureDocument() =
        runTest(UnconfinedTestDispatcher()) {
            val document: ByteArray =
                captureSingleRemoteDocument(context) {
                        RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Red))
                    }
                    .bytes

            val remoteComposeDocument =
                CoreDocument().apply {
                    ByteArrayInputStream(document).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            assertTrue(remoteComposeDocument.docInfo.mNumberOfOps > 0)
        }

    @Test
    fun captureDocumentWithCustomProfile() =
        runTest(UnconfinedTestDispatcher()) {
            val customProfile =
                Profile(
                    CoreDocument.DOCUMENT_API_LEVEL,
                    RcProfiles.PROFILE_ANDROID_NATIVE,
                    AndroidxRcPlatformServices(),
                    {
                        Operations.getOperations(
                                CoreDocument.DOCUMENT_API_LEVEL,
                                RcProfiles.PROFILE_ANDROIDX,
                            )
                            ?.keySet()
                            .orEmpty() + setOf(Operations.DRAW_TEXT_ON_CIRCLE)
                    },
                ) { creationDisplayInfo, profile, callback ->
                    RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
                }
            val document: ByteArray =
                captureSingleRemoteDocument(context, profile = customProfile) {
                        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                            val redPaint = RemotePaint { color = Color.Red.rc }
                            drawRect(paint = redPaint)
                            val bluePaint = RemotePaint { color = Color.Blue.rc }
                            drawCircle(
                                paint = bluePaint,
                                center = RemoteOffset(width / 2f, height / 2f),
                                radius = width / 4f,
                            )
                            val textPaint = RemotePaint {
                                isAntiAlias = true
                                color = Color.LightGray.rc
                                textSize = 12f.rf
                            }

                            /*
                            drawTextOnCircle(
                                text = "10:09".rs,
                                centerX = width / 2f,
                                centerY = height / 2f,
                                radius = width / 2f,
                                startAngle = 0f.rf,
                                warpRadiusOffset = 0f.rf,
                                paint = textPaint,
                            )
                            */
                        }
                    }
                    .bytes

            assertTrue(document.isNotEmpty())
        }

    @Test
    fun captureDocument_withHostDensity() =
        runTest(UnconfinedTestDispatcher()) {
            var capturedDensity: RemoteDensity? = null
            captureSingleRemoteDocument(context = context, remoteDensity = RemoteDensity.Host) {
                capturedDensity = LocalRemoteComposeCreationState.current.remoteDensity
                RemoteBox(modifier = RemoteModifier.fillMaxSize())
            }

            assertNotNull(capturedDensity)
            // Assert that it is not a constant
            assertNull(capturedDensity?.density?.constantValueOrNull)
        }
}
