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

package androidx.wear.compose.remote.material3

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteContentScale
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.createNamedRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteContainerPainterUnitTest {
    @get:Rule val painterRule = DocumentPainterRule(canvasSize = 200f)

    @Test
    fun button_containerPainter_withRemoteContentScale_crop() {
        val image = createPatternedBitmap(width = 20, height = 60)
        val painter = paintButton(image, RemoteContentScale.Crop, "btn_crop")

        val side = painter.colorAt((painter.width * 0.35f).toInt(), painter.height / 2)
        val top = painter.colorAt(painter.width / 2, (painter.height * 0.38f).toInt())
        val center = painter.colorAt(painter.width / 2, painter.height / 2)

        assertIsWhite(side)
        assertIsWhite(top)
        assertIsWhite(center)
    }

    @Test
    fun button_containerPainter_withRemoteContentScale_fit() {
        val image = createPatternedBitmap(width = 20, height = 60)
        val painter = paintButton(image, RemoteContentScale.Fit, "btn_fit")

        val side = painter.colorAt((painter.width * 0.35f).toInt(), painter.height / 2)
        val top = painter.colorAt(painter.width / 2, (painter.height * 0.38f).toInt())
        val center = painter.colorAt(painter.width / 2, painter.height / 2)

        assertIsBlue(side)
        assertIsRed(top)
        assertIsWhite(center)
    }

    @Test
    fun button_containerPainter_withRemoteContentScale_fillBounds() {
        val image = createPatternedBitmap(width = 20, height = 60)
        val painter = paintButton(image, RemoteContentScale.FillBounds, "btn_fill_bounds")

        val side = painter.colorAt((painter.width * 0.35f).toInt(), painter.height / 2)
        val top = painter.colorAt(painter.width / 2, (painter.height * 0.38f).toInt())
        val center = painter.colorAt(painter.width / 2, painter.height / 2)

        assertIsWhite(side)
        assertIsRed(top)
        assertIsWhite(center)
    }

    @Test
    fun card_containerPainter_withRemoteContentScale_crop() {
        val image = createPatternedBitmap(width = 20, height = 80)
        val painter = paintCard(image, RemoteContentScale.Crop, "card_crop")

        val side = painter.colorAt((painter.width * 0.35f).toInt(), painter.height / 2)
        val top = painter.colorAt(painter.width / 2, (painter.height * 0.33f).toInt())
        val center = painter.colorAt(painter.width / 2, painter.height / 2)

        assertIsWhite(side)
        assertIsWhite(top)
        assertIsWhite(center)
    }

    @Test
    fun card_containerPainter_withRemoteContentScale_fit() {
        val image = createPatternedBitmap(width = 20, height = 80)
        val painter = paintCard(image, RemoteContentScale.Fit, "card_fit")

        val side = painter.colorAt((painter.width * 0.35f).toInt(), painter.height / 2)
        val top = painter.colorAt(painter.width / 2, (painter.height * 0.33f).toInt())
        val center = painter.colorAt(painter.width / 2, painter.height / 2)

        assertIsBlue(side)
        assertIsRed(top)
        assertIsWhite(center)
    }

    @Test
    fun card_containerPainter_withRemoteContentScale_fillBounds() {
        val image = createPatternedBitmap(width = 20, height = 80)
        val painter = paintCard(image, RemoteContentScale.FillBounds, "card_fill_bounds")

        val side = painter.colorAt((painter.width * 0.35f).toInt(), painter.height / 2)
        val top = painter.colorAt(painter.width / 2, (painter.height * 0.33f).toInt())
        val center = painter.colorAt(painter.width / 2, painter.height / 2)

        assertIsWhite(side)
        assertIsRed(top)
        assertIsWhite(center)
    }

    private fun paintButton(
        image: ImageBitmap,
        contentScale: RemoteContentScale,
        imageName: String,
    ): DocumentPainterRule.DocumentPainter = painterRule.paintDocument {
        val bg = remember { createNamedRemoteImageBitmap(imageName) { image } }
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize().background(RemoteColor.Black),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteButton(
                onClick = RemoteAction.None,
                modifier = RemoteModifier.size(100.rdp, 60.rdp),
                shape = RemoteRectangleShape,
                containerPainter =
                    RemoteButtonDefaults.containerPainter(
                        image = bg,
                        scrim = null,
                        contentScale = contentScale,
                    ),
            ) {}
        }
    }

    private fun paintCard(
        image: ImageBitmap,
        contentScale: RemoteContentScale,
        imageName: String,
    ): DocumentPainterRule.DocumentPainter = painterRule.paintDocument {
        val bg = remember { createNamedRemoteImageBitmap(imageName) { image } }
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize().background(RemoteColor.Black),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteCard(
                onClick = RemoteAction.None,
                modifier = RemoteModifier.size(100.rdp, 80.rdp),
                shape = RemoteRectangleShape,
                containerPainter =
                    RemoteCardDefaults.containerPainter(
                        image = bg,
                        scrim = null,
                        contentScale = contentScale,
                    ),
            ) {}
        }
    }

    private fun assertIsBlue(color: Color) {
        assertThat(color.blue).isGreaterThan(0.9f)
        assertThat(color.red).isLessThan(0.1f)
        assertThat(color.green).isLessThan(0.1f)
    }

    private fun assertIsRed(color: Color) {
        assertThat(color.red).isGreaterThan(0.9f)
        assertThat(color.blue).isLessThan(0.1f)
        assertThat(color.green).isLessThan(0.1f)
    }

    private fun assertIsWhite(color: Color) {
        assertThat(color.red).isGreaterThan(0.9f)
        assertThat(color.green).isGreaterThan(0.9f)
        assertThat(color.blue).isGreaterThan(0.9f)
    }

    private fun createPatternedBitmap(width: Int, height: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color =
                    when {
                        x < 2 || x >= width - 2 -> AndroidColor.BLUE
                        y < 20 || y >= height - 20 -> AndroidColor.RED
                        else -> AndroidColor.WHITE
                    }
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap.asImageBitmap()
    }
}
