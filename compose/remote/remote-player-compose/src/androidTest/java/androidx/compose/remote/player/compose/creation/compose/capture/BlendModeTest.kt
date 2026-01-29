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

package androidx.compose.remote.player.compose.creation.compose.capture

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Paint
import android.util.Log
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * A test for BlendMode in RemoteCanvas, see
 * [BlendMode](https://developer.android.com/reference/android/graphics/BlendMode)
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class BlendModeTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val saveDocument = false

    @Test
    fun all_blend_modes() {
        runBlocking {
            remoteComposeTestRule.runScreenshotTest(
                creationDisplayInfo =
                    CreationDisplayInfo(2000, 2500, context.resources.displayMetrics.densityDpi)
            ) {
                AllBlendModes()
            }

            if (!saveDocument) return@runBlocking
            val document = remoteComposeTestRule.captureDocument(context) { AllBlendModes() }
            val wireBuffer: WireBuffer = document.buffer.buffer
            val file =
                File(
                    "/sdcard/Android/data/androidx.compose.remote.player.compose.test/cache/documents",
                    "test_blend_mode.rc",
                )
            file.parentFile?.mkdirs()
            try {
                file.writeBytes(wireBuffer.buffer.copyOf(wireBuffer.size))
            } catch (e: Exception) {
                Log.e("BlendModeTest", "Failed to save document: $file", e)
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun AllBlendModes() {
        val blendModes = BlendMode.entries.toTypedArray()
        val chunkedBlendModes = blendModes.toList().chunked(4)
        RemoteColumn {
            for (rowItems in chunkedBlendModes) {
                RemoteRow {
                    for (blendMode in rowItems) {
                        RemoteBlendModeVisual(blendMode = blendMode, name = blendMode.name)
                    }
                }
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun RemoteBlendModeVisual(blendMode: BlendMode, name: String) {
        RemoteBox(
            RemoteModifier.size(100.rdp).border(1.rdp, Color.Black.rc).padding(8.dp),
            horizontalAlignment = RemoteAlignment.Start,
            verticalArrangement = RemoteArrangement.Top,
        ) {
            RemoteCanvas(RemoteModifier.size(100.rdp)) {
                val w = remoteWidth
                val h = remoteHeight

                val paint =
                    RemotePaint().apply {
                        style = Paint.Style.FILL
                        this.color = Color.Magenta.toArgb()
                    }

                // Draw dst
                drawCircle(
                    paint = paint,
                    center = RemoteOffset(w * 2f / 3f, h * 1f / 3f),
                    radius = w / 3f,
                )

                // Draw src
                paint.color = Color.Blue.toArgb()
                paint.blendMode = blendMode
                drawRect(
                    paint = paint,
                    topLeft = RemoteOffset(0f.rf, h * 1f / 3f),
                    size = RemoteSize(w * 2f / 3f, h * 2f / 3f),
                )
            }
            RemoteText(name, fontSize = 12f.sp)
        }
    }
}
