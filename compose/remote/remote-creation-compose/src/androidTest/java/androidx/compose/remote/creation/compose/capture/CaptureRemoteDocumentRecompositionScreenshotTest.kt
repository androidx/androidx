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

@file:OptIn(androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class CaptureRemoteDocumentRecompositionScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotTestRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val TEST_TAG = "player_root"

    @Composable
    fun RecomposingPlayer(
        flow: Flow<ByteArray>,
        size: Size,
        modifier: Modifier = Modifier,
        onDocumentUpdated: (CoreDocument) -> Unit = {},
    ) {
        val documentState = remember { mutableStateOf<CoreDocument?>(null) }
        LaunchedEffect(flow) {
            flow.collect { bytes ->
                val doc =
                    CoreDocument().apply {
                        initFromBuffer(
                            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes))
                        )
                    }
                documentState.value = doc
                onDocumentUpdated(doc)
            }
        }
        Box(modifier = modifier.testTag(TEST_TAG).background(Color.White)) {
            documentState.value?.let { doc ->
                RemoteDocumentPlayer(
                    document = doc,
                    documentWidth = size.width.toInt(),
                    documentHeight = size.height.toInt(),
                )
            }
        }
    }

    @Test
    fun testRecompositionScreenshot() {
        val state = mutableStateOf("Initial")
        val width = 300
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())

        val flow =
            captureRemoteDocument(
                creationDisplayInfo = RemoteCreationDisplayInfo(width, height, 160, 1.0f),
                writerEvents = WriterEvents(),
                context = context,
                content = {
                    val color = if (state.value == "Initial") Color.Red else Color.Green
                    RemoteBox(modifier = RemoteModifier.fillMaxSize().background(color)) {
                        RemoteText(state.value, color = RemoteColor(Color.White))
                    }
                },
            )

        val receivedTexts = mutableListOf<String>()

        composeTestRule.setContent {
            RecomposingPlayer(
                flow = flow,
                size = size,
                modifier = Modifier.size(width.dp, height.dp),
                onDocumentUpdated = { doc -> receivedTexts.addAll(doc.mTextData.values) },
            )
        }

        // Wait for initial render containing "Initial"
        composeTestRule.waitUntil(timeoutMillis = 5000) { receivedTexts.contains("Initial") }

        // Capture initial screenshot
        val initialScreenshot = composeTestRule.onNodeWithTag(TEST_TAG).captureToImage()
        initialScreenshot
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotTestRule, "recomposition_initial")

        // Update state to trigger recomposition
        composeTestRule.runOnUiThread { state.value = "Updated" }

        // Wait for recomposition and player update containing "Updated"
        composeTestRule.waitUntil(timeoutMillis = 5000) { receivedTexts.contains("Updated") }

        // Capture updated screenshot
        val updatedScreenshot = composeTestRule.onNodeWithTag(TEST_TAG).captureToImage()
        updatedScreenshot
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotTestRule, "recomposition_updated")
    }
}
