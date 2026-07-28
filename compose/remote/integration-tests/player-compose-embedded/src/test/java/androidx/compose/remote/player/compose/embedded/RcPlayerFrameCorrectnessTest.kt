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

@file:Suppress("RestrictedApiAndroidX") // Referring to background, drawCircle, remote-core

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins next-frame correctness: the very first rendered frame must already be right — content must
 * not take extra frames to settle or depend on a later update. Not a screenshot test: bitmaps are
 * extracted only to sample individual pixels.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerFrameCorrectnessTest {

    @get:Rule val rule = createComposeRule()

    private fun loadDocument(bytes: ByteArray): CoreDocument =
        CoreDocument().apply {
            ByteArrayInputStream(bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }

    /**
     * A canvas draw positioned by the canvas's own measured size (WIDTH/HEIGHT ComponentValues)
     * must be correct on the first frame. Guards the layout-time (onSizeChanged) size publishing —
     * when the size was published from the draw pass, a size-driven draw could render a frame late
     * or wrong.
     */
    @Test
    fun sizeDrivenCanvasDrawIsCorrectOnTheFirstFrame() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteBox(modifier = RemoteModifier.fillMaxSize()) {
                                RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                                    val paint = RemotePaint().apply { color = Color.Red.rc }
                                    drawCircle(paint = paint, radius = width / 2f)
                                }
                            }
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(document = document, autoUpdate = false)
                }
            }

            // Exactly one frame: composition + layout + draw. No settle time allowed.
            val d = rule.density.density
            val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
            val center = bitmap.getPixel((50 * d).toInt(), (50 * d).toInt())
            assertThat(AndroidColor.red(center)).isGreaterThan(200)
            assertThat(AndroidColor.green(center)).isLessThan(60)
        }
    }

    /** A plain sized+colored box renders on the first frame (no clock advance at all). */
    @Test
    fun staticContentRendersOnTheFirstFrame() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp).background(Color(0xFF0000FF).rc)
                            )
                        },
                    )
                    .bytes

            val document = loadDocument(bytes)
            rule.mainClock.autoAdvance = false
            rule.setContent {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(document = document, autoUpdate = false)
                }
            }

            val d = rule.density.density
            val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
            val center = bitmap.getPixel((50 * d).toInt(), (50 * d).toInt())
            assertThat(AndroidColor.blue(center)).isGreaterThan(200)
        }
    }
}
