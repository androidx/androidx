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

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RcPlayerDrawTextOnCircleTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val circularTextProfile =
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

    @Test
    fun literalGeometry_drawsTextAlongTopOfCircle() {
        // The canvas is 100dp square, so literal pixel values depend on density: derive them
        // from the captured size instead, and only use literals for the angle and offset.
        val bitmap =
            renderTextOnCircle(
                centerX = { (it / 2f).rf },
                centerY = { (it / 2f).rf },
                radius = { (it / 4f).rf },
            )

        assertThat(redPixelsAboveTopOfCircle(bitmap)).isGreaterThan(20)
    }

    @Test
    fun expressionGeometry_drawsTextAlongTopOfCircle() {
        // As RemoteTimeText does: the geometry is NaN-encoded float expressions over the canvas
        // size, which the player must resolve.
        val bitmap =
            renderTextOnCircle(
                centerX = { width / 2f.rf },
                centerY = { height / 2f.rf },
                radius = { width / 4f.rf },
            )

        assertThat(redPixelsAboveTopOfCircle(bitmap)).isGreaterThan(20)
    }

    @Test
    fun zeroRadius_drawsNothing() {
        // As the View player, an empty circle draws nothing rather than dividing by zero.
        val bitmap =
            renderTextOnCircle(
                centerX = { width / 2f.rf },
                centerY = { height / 2f.rf },
                radius = { width * 0f.rf },
            )

        assertThat(redPixelsAboveTopOfCircle(bitmap)).isEqualTo(0)
    }

    /**
     * Draws a wide red string outside a circle centred in a square canvas, centred on the top of
     * the circle. The geometry lambdas receive the canvas width in pixels.
     */
    private fun renderTextOnCircle(
        centerX: RemoteDrawScope.(Float) -> RemoteFloat,
        centerY: RemoteDrawScope.(Float) -> RemoteFloat,
        radius: RemoteDrawScope.(Float) -> RemoteFloat,
    ): Bitmap {
        val sizePx = 100f * rule.density.density
        rule.setRemoteContent(profile = circularTextProfile) {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                drawTextOnCircle(
                    text = RemoteString("WWWWW"),
                    centerX = centerX(sizePx),
                    centerY = centerY(sizePx),
                    radius = radius(sizePx),
                    startAngle = 270f.rf,
                    warpRadiusOffset = 0f.rf,
                    paint =
                        RemotePaint {
                            color = RemoteColor(Color.Red)
                            textSize = (sizePx / 6f).rf
                        },
                )
            }
        }
        rule.mainClock.advanceTimeBy(100)
        return rule.captureRootBitmap()
    }

    /** Red pixels in the band between the top of the circle and the top of the canvas. */
    private fun redPixelsAboveTopOfCircle(bitmap: Bitmap): Int {
        val size = bitmap.width
        var count = 0
        for (y in 0 until size / 4) {
            for (x in size / 5 until size * 4 / 5) {
                val pixel = bitmap.getPixel(x, y)
                if (
                    AndroidColor.red(pixel) > 150 &&
                        AndroidColor.green(pixel) < 100 &&
                        AndroidColor.blue(pixel) < 100
                ) {
                    count++
                }
            }
        }
        return count
    }
}
