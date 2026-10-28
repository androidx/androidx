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
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.verticalGradient
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap.Companion.createNamedRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
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
class RemoteContainerPainterTest {
    @get:Rule val painterRule = DocumentPainterRule(canvasSize = 200f)

    @Test
    fun button_disabledContainerPainter_dimsImage_embeddedPlayer() {
        val solidWhiteImage = createSolidBitmap(64, 64, AndroidColor.WHITE)
        val enabledPainter = painterRule.paintDocument {
            val bg = remember {
                createNamedRemoteImageBitmap("white_enabled") { solidWhiteImage }
            }
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteButton(
                    onClick = RemoteAction.Empty,
                    enabled = true.rb,
                    modifier = RemoteModifier.size(100.rdp, 60.rdp),
                    shape = RemoteRectangleShape,
                    containerPainter =
                        RemoteButtonDefaults.containerPainter(image = bg, scrim = null),
                ) {}
            }
        }
        val enabledCenter =
            enabledPainter.colorAt(enabledPainter.width / 2, enabledPainter.height / 2)
        assertThat(enabledCenter.red).isGreaterThan(0.9f)

        val disabledPainter = painterRule.paintDocument {
            val bg = remember {
                createNamedRemoteImageBitmap("white_disabled") { solidWhiteImage }
            }
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteButton(
                    onClick = RemoteAction.Empty,
                    enabled = false.rb,
                    modifier = RemoteModifier.size(100.rdp, 60.rdp),
                    shape = RemoteRectangleShape,
                    containerPainter =
                        RemoteButtonDefaults.containerPainter(image = bg, scrim = null),
                ) {}
            }
        }
        val disabledCenter =
            disabledPainter.colorAt(disabledPainter.width / 2, disabledPainter.height / 2)
        assertThat(disabledCenter.red).isWithin(0.05f).of(0.12f)
    }

    @Test
    fun button_disabledContainerPainter_dimsImageAndGradientScrim_embeddedPlayer() {
        val solidBlueImage = createSolidBitmap(64, 64, AndroidColor.BLUE)
        val disabledPainter = painterRule.paintDocument {
            val bg = remember {
                createNamedRemoteImageBitmap("blue_disabled") { solidBlueImage }
            }
            val redGradient = RemoteBrush.verticalGradient(listOf(Color.Red.rc, Color.Red.rc))
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteButton(
                    onClick = RemoteAction.Empty,
                    enabled = false.rb,
                    modifier = RemoteModifier.size(100.rdp, 60.rdp),
                    shape = RemoteRectangleShape,
                    containerPainter =
                        RemoteButtonDefaults.containerPainter(image = bg, scrim = redGradient),
                ) {}
            }
        }
        val disabledCenter =
            disabledPainter.colorAt(disabledPainter.width / 2, disabledPainter.height / 2)
        // Both blue image and red gradient scrim must be dimmed (neither full 1.0f red nor 1.0f
        // blue)
        assertThat(disabledCenter.red).isWithin(0.06f).of(0.12f)
        assertThat(disabledCenter.blue).isLessThan(0.20f)
    }

    @Test
    fun card_containerPainterAlpha_dimsImageAndScrim_embeddedPlayer() {
        val solidWhiteImage = createSolidBitmap(64, 64, AndroidColor.WHITE)
        val painter = painterRule.paintDocument {
            val bg = remember {
                createNamedRemoteImageBitmap("card_white_alpha") { solidWhiteImage }
            }
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteCard(
                    onClick = RemoteAction.Empty,
                    modifier = RemoteModifier.size(100.rdp, 80.rdp),
                    shape = RemoteRectangleShape,
                    containerPainter =
                        RemoteCardDefaults.containerPainter(
                            image = bg,
                            scrim = null,
                            alpha = 0.25f.rf,
                        ),
                ) {}
            }
        }
        val center = painter.colorAt(painter.width / 2, painter.height / 2)
        assertThat(center.red).isWithin(0.05f).of(0.25f)
    }

    private fun createSolidBitmap(width: Int, height: Int, color: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(color)
        return bitmap.asImageBitmap()
    }
}
