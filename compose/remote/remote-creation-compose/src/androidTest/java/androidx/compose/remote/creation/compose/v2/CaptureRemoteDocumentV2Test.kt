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

package androidx.compose.remote.creation.compose.v2

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class CaptureRemoteDocumentV2Test {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("compose/remote/remote-creation-compose")

    @Test
    fun captureDocumentV2_withDefaultDensity() = runTest {
        val creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f)
        val bytes =
            captureSingleRemoteDocument(
                    creationDisplayInfo = creationDisplayInfo,
                    context = context,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .border(2.rdp, Color.Blue.rc)
                                .background(Color.DarkGray.rc)
                    ) {
                        RemoteText(text = "Hello world!".rs)
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            Box(Modifier.size(100.dp)) { RemoteDocumentPlayer(doc, 100, 100) }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, "captureDocumentV2_defaultDensity")
    }

    @Test
    fun captureDocumentV2_withCustomDensity() = runTest {
        val creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f)
        val bytes =
            captureSingleRemoteDocument(
                    creationDisplayInfo = creationDisplayInfo,
                    remoteDensity = RemoteDensity(3f.rf, 1.5f.rf),
                    context = context,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .border(2.rdp, Color.Blue.rc)
                                .background(Color.DarkGray.rc)
                    ) {
                        RemoteText(text = "Hello world!".rs)
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            Box(Modifier.size(100.dp)) { RemoteDocumentPlayer(doc, 100, 100) }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, "captureDocumentV2_customDensity")
    }

    private fun parseToCoreDocument(bytes: ByteArray): CoreDocument {
        return CoreDocument().apply {
            ByteArrayInputStream(bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    }
}
