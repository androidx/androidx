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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.vector.painterRemoteAnimatedVector
import androidx.compose.remote.player.compose.test.R
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * End-to-end integration test verifying that a native Animated Vector Drawable renders and animates
 * faithfully in the embedded player ([RcPlayer]) without faking dynamic evaluation.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerAnimatedVectorTest {

    @get:Rule val playerRule = RcPlayerTestRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private fun Bitmap.getPixelAt(xDp: Int, yDp: Int): Int {
        val d = playerRule.density.density
        val px = (xDp * d).toInt().coerceIn(0, width - 1)
        val py = (yDp * d).toInt().coerceIn(0, height - 1)
        return getPixel(px, py)
    }

    private fun isBlue(pixel: Int): Boolean =
        AndroidColor.blue(pixel) > 200 &&
            AndroidColor.red(pixel) < 80 &&
            AndroidColor.green(pixel) < 60

    private fun isRed(pixel: Int): Boolean =
        AndroidColor.red(pixel) > 200 &&
            AndroidColor.blue(pixel) < 60 &&
            AndroidColor.green(pixel) < 60

    private fun isPurple(pixel: Int): Boolean =
        AndroidColor.red(pixel) > 100 &&
            AndroidColor.blue(pixel) > 100 &&
            AndroidColor.green(pixel) < 60

    private fun isWhite(pixel: Int): Boolean =
        AndroidColor.red(pixel) > 220 &&
            AndroidColor.green(pixel) > 220 &&
            AndroidColor.blue(pixel) > 220

    @Test
    fun clockHandAvd_animatesRotationAndColor_withEmbeddedPlayer() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Capture the document using painterRemoteAnimatedVector with default continuous time
        // progress
        val document = runBlocking {
            captureRule.captureDocument(context = context) {
                val painter = painterRemoteAnimatedVector(context, R.drawable.clock_hand_animated)
                RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                    with(painter) { onDraw() }
                }
            }
        }

        playerRule.mainClock.autoAdvance = false
        playerRule.setContent {
            Box(modifier = Modifier.size(100.dp).background(Color.White).testTag("clockBox")) {
                RcPlayer(document = document)
            }
        }

        // 1. Initial state (t = 0ms):
        // Hand should be at 12 o'clock (blue), background white elsewhere.
        playerRule.mainClock.advanceTimeByFrame()
        playerRule.waitForIdle()

        val bmp0 = playerRule.onNodeWithTag("clockBox").captureToImage().asAndroidBitmap()
        val p12At0 = bmp0.getPixelAt(50, 25)
        val p3At0 = bmp0.getPixelAt(75, 50)
        val p6At0 = bmp0.getPixelAt(50, 75)
        val p9At0 = bmp0.getPixelAt(25, 50)

        assertWithMessage("t=0ms: Expected blue at 12 o'clock, was ${Integer.toHexString(p12At0)}")
            .that(isBlue(p12At0))
            .isTrue()
        assertWithMessage("t=0ms: Expected white at 3 o'clock").that(isWhite(p3At0)).isTrue()
        assertWithMessage("t=0ms: Expected white at 6 o'clock").that(isWhite(p6At0)).isTrue()
        assertWithMessage("t=0ms: Expected white at 9 o'clock").that(isWhite(p9At0)).isTrue()

        // 2. Quarter cycle (t = 250ms):
        // Hand should be at 3 o'clock (purple: halfway between blue and red).
        playerRule.mainClock.advanceTimeBy(250)
        playerRule.waitForIdle()

        val bmp250 = playerRule.onNodeWithTag("clockBox").captureToImage().asAndroidBitmap()
        val p12At250 = bmp250.getPixelAt(50, 25)
        val p3At250 = bmp250.getPixelAt(75, 50)
        val p6At250 = bmp250.getPixelAt(50, 75)
        val p9At250 = bmp250.getPixelAt(25, 50)

        assertWithMessage("t=250ms: Expected white at 12 o'clock").that(isWhite(p12At250)).isTrue()
        assertWithMessage("t=250ms: Expected purple at 3 o'clock").that(isPurple(p3At250)).isTrue()
        assertWithMessage("t=250ms: Expected white at 6 o'clock").that(isWhite(p6At250)).isTrue()
        assertWithMessage("t=250ms: Expected white at 9 o'clock").that(isWhite(p9At250)).isTrue()

        // 3. Midpoint (t = 500ms):
        // Hand should be at 6 o'clock (red).
        playerRule.mainClock.advanceTimeBy(250)
        playerRule.waitForIdle()

        val bmp500 = playerRule.onNodeWithTag("clockBox").captureToImage().asAndroidBitmap()
        val p12At500 = bmp500.getPixelAt(50, 25)
        val p3At500 = bmp500.getPixelAt(75, 50)
        val p6At500 = bmp500.getPixelAt(50, 75)
        val p9At500 = bmp500.getPixelAt(25, 50)

        assertWithMessage("t=500ms: Expected white at 12 o'clock").that(isWhite(p12At500)).isTrue()
        assertWithMessage("t=500ms: Expected white at 3 o'clock").that(isWhite(p3At500)).isTrue()
        assertWithMessage("t=500ms: Expected red at 6 o'clock").that(isRed(p6At500)).isTrue()
        assertWithMessage("t=500ms: Expected white at 9 o'clock").that(isWhite(p9At500)).isTrue()

        // 4. Three-quarter cycle (t = 750ms):
        // Hand should be at 9 o'clock (purple: halfway between red and blue).
        playerRule.mainClock.advanceTimeBy(250)
        playerRule.waitForIdle()

        val bmp750 = playerRule.onNodeWithTag("clockBox").captureToImage().asAndroidBitmap()
        val p12At750 = bmp750.getPixelAt(50, 25)
        val p3At750 = bmp750.getPixelAt(75, 50)
        val p6At750 = bmp750.getPixelAt(50, 75)
        val p9At750 = bmp750.getPixelAt(25, 50)

        assertWithMessage("t=750ms: Expected white at 12 o'clock").that(isWhite(p12At750)).isTrue()
        assertWithMessage("t=750ms: Expected white at 3 o'clock").that(isWhite(p3At750)).isTrue()
        assertWithMessage("t=750ms: Expected white at 6 o'clock").that(isWhite(p6At750)).isTrue()
        assertWithMessage("t=750ms: Expected purple at 9 o'clock").that(isPurple(p9At750)).isTrue()

        // 5. Full cycle (t = 1000ms):
        // Hand should have completed 360-degree rotation back to 12 o'clock (blue).
        playerRule.mainClock.advanceTimeBy(250)
        playerRule.waitForIdle()

        val bmp1000 = playerRule.onNodeWithTag("clockBox").captureToImage().asAndroidBitmap()
        val p12At1000 = bmp1000.getPixelAt(50, 25)
        val p3At1000 = bmp1000.getPixelAt(75, 50)
        val p6At1000 = bmp1000.getPixelAt(50, 75)
        val p9At1000 = bmp1000.getPixelAt(25, 50)

        assertWithMessage(
                "t=1000ms: Expected blue at 12 o'clock, was ${Integer.toHexString(p12At1000)}"
            )
            .that(isBlue(p12At1000))
            .isTrue()
        assertWithMessage("t=1000ms: Expected white at 3 o'clock").that(isWhite(p3At1000)).isTrue()
        assertWithMessage("t=1000ms: Expected white at 6 o'clock").that(isWhite(p6At1000)).isTrue()
        assertWithMessage("t=1000ms: Expected white at 9 o'clock").that(isWhite(p9At1000)).isTrue()
    }
}
