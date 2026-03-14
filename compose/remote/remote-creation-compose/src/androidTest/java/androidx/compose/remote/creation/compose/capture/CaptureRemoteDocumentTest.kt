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

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based screenshot test of [RecordingCanvas]. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class CaptureRemoteDocumentTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun captureDocument() = runTest {
        val document: ByteArray =
            withContext(Dispatchers.Main) {
                captureSingleRemoteDocument(context) {
                        RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Red))
                    }
                    .bytes
            }

        val remoteComposeDocument =
            CoreDocument().apply {
                ByteArrayInputStream(document).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        assertTrue(remoteComposeDocument.docInfo.mNumberOfOps > 0)
    }

    @Test
    fun captureDocumentWithCustomProfile() = runTest {
        val customProfile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROID_NATIVE,
                AndroidxRcPlatformServices(),
                { setOf(Operations.DRAW_TEXT_ON_CIRCLE) },
            ) { creationDisplayInfo, profile, callback ->
                RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
            }
        val document: ByteArray =
            withContext(Dispatchers.Main) {
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

                            drawTextOnCircle(
                                text = "10:09".rs,
                                centerX = width / 2f,
                                centerY = height / 2f,
                                radius = width / 2f,
                                startAngle = 0f.rf,
                                warpRadiusOffset = 0f.rf,
                                paint = textPaint,
                            )
                        }
                    }
                    .bytes
            }

        assertTrue(document.isNotEmpty())
    }
}
