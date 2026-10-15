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

@file:Suppress("RestrictedApiAndroidX") // Referring to remote-core

package androidx.compose.remote.player.compose.embedded

import android.graphics.Color as AndroidColor
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins next-frame correctness: the very first rendered frame must already be right — content must
 * not take extra frames to settle or depend on a later update. Not a screenshot test: bitmaps are
 * extracted only to sample individual pixels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerFrameCorrectnessTest {

    @get:Rule val rule = RcPlayerTestRule()

    /**
     * A canvas draw positioned by the canvas's own measured size (WIDTH/HEIGHT ComponentValues)
     * must be correct on the first frame. Guards the layout-time (onSizeChanged) size publishing —
     * when the size was published from the draw pass, a size-driven draw could render a frame late
     * or wrong.
     */
    @Test
    fun sizeDrivenCanvasDrawIsCorrectOnTheFirstFrame() {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.fillMaxSize()) {
                RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                    val paint = RemotePaint().apply { color = Color.Red.rc }
                    drawCircle(paint = paint, radius = width / 2f)
                }
            }
        }

        // Exactly one frame: composition + layout + draw. No settle time allowed.
        val d = rule.density.density
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val center = bitmap.getPixel((50 * d).toInt(), (50 * d).toInt())
        assertThat(AndroidColor.red(center)).isGreaterThan(200)
        assertThat(AndroidColor.green(center)).isLessThan(60)
    }

    /** A plain sized+colored box renders on the first frame (no clock advance at all). */
    @Test
    fun staticContentRendersOnTheFirstFrame() {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent {
            RemoteBox(modifier = RemoteModifier.size(100.rdp).background(Color(0xFF0000FF).rc))
        }

        val d = rule.density.density
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val center = bitmap.getPixel((50 * d).toInt(), (50 * d).toInt())
        assertThat(AndroidColor.blue(center)).isGreaterThan(200)
    }

    /**
     * An animated document must reach idle using withInfiniteAnimationFrameMillis, allowing
     * standard Compose test APIs (like waitForIdle and captureToImage) to complete without timing
     * out.
     */
    @Test
    fun animatedDocumentReachesIdleWithDefaultAutoUpdate() {
        rule.setRemoteContent {
            val animated =
                animateRemoteFloatAsState(
                    targetValue = 1.0f.rf,
                    animationSpec = remoteTween(durationMillis = 1000),
                )
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp)
                        .background(Color.Red.rc)
                        .graphicsLayer(alpha = animated)
            )
        }

        // waitForIdle() and captureToImage() should complete successfully without timing out
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        assertThat(bitmap).isNotNull()
    }
}
