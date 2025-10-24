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

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import android.graphics.Paint
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
                captureRemoteDocument(context) {
                    RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Red)) {}
                }
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
        val document: ByteArray =
            withContext(Dispatchers.Main) {
                captureRemoteDocument(context, profile = customProfile) {
                    RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                        val textPaint =
                            Paint().apply {
                                isAntiAlias = true
                                color = Color.LightGray.toArgb()
                                textSize = 12f
                            }

                        val canvas = drawScope.drawContext.canvas.nativeCanvas
                        if (canvas is RecordingCanvas) {
                            canvas.drawTextOnCircle(
                                text = RemoteString("10:09"),
                                centerX = size.width / 2,
                                centerY = size.height / 2,
                                radius = size.width / 2,
                                startAngle = 0f,
                                warpRadiusOffset = 0f,
                                alignment = DrawTextOnCircle.Alignment.CENTER,
                                placement = DrawTextOnCircle.Placement.INSIDE,
                                paint = textPaint,
                            )
                        }
                    }
                }
            }

        assertTrue(document.isNotEmpty())
    }
}
