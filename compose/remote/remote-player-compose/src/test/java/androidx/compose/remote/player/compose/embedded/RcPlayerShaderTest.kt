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

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteLinearShader
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Shader verification under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RcPlayerShaderTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun runtimeShaderDisabledByDefaultWhenTrustedContentIsFalse() {
        var shaderId = 0
        val document = buildDocument {
            shaderId = writer.createShader(AGSL_SOLID_RED).commit()
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setColor(0xFF0000FF.toInt())
                    setShader(shaderId)
                }
            )
            writer.drawRect(0f, 0f, 100f, 100f)
        }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag("player")) {
                RcPlayer(document = document)
            }
        }
        rule.waitForIdle()
        val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()

        assertThat(document.remoteComposeState.getFromId(shaderId)).isNull()
        val pixel = bitmap.getPixel(50, 50)
        assertThat(AndroidColor.blue(pixel)).isGreaterThan(200)
        assertThat(AndroidColor.red(pixel)).isLessThan(60)
    }

    @Test
    fun runtimeShaderEnabledWhenTrustedContentIsTrue() {
        var shaderId = 0
        val document = buildDocument {
            shaderId = writer.createShader(AGSL_SOLID_RED).commit()
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setColor(0xFF0000FF.toInt())
                    setShader(shaderId)
                }
            )
            writer.drawRect(0f, 0f, 100f, 100f)
        }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag("player")) {
                RcPlayer(document = document, trustedContent = true)
            }
        }
        rule.waitForIdle()
        val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()

        assertThat(document.remoteComposeState.getFromId(shaderId)).isNotNull()
        assertRed(bitmap, 50, 50)
    }

    /**
     * The embedded player should ingest a shader/gradient document and run its draw path without
     * throwing (it constructs a Compose gradient `Brush` in RcPlayerDrawing).
     */
    @Test
    fun embeddedPlayerComposesGradientDocument() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(100.dp).testTag("gradient")) { content() }
            }
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        shader =
                            RemoteLinearShader(
                                0f.rf,
                                0f.rf,
                                100f.rf,
                                0f.rf,
                                listOf(Color.Red.rc, Color.Blue.rc),
                                null,
                                RemoteTileMode.Clamp,
                            )
                    }
                drawRect(
                    paint = paint,
                    topLeft = RemoteOffset(0f.rf, 0f.rf),
                    size = RemoteSize(100f.rf, 100f.rf),
                )
            }
        }

        // Composed + laid out without throwing through the gradient draw path.
        rule.onNodeWithTag("gradient").assertExists()
    }

    /**
     * Exercises the CLIP_RECT draw op (dispatched via drawContext.canvas.clipRect): a canvas that
     * clips to a sub-rectangle and then draws a full-size rect must compose/lay out without
     * throwing.
     */
    @Test
    fun embeddedPlayerComposesClippedCanvas() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(100.dp).testTag("clipped")) { content() }
            }
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                // Scoped clip (emits CLIP_RECT + save/restore around the block).
                clipRect(0f.rf, 0f.rf, 50f.rf, 50f.rf) {
                    drawRect(
                        paint = RemotePaint().apply { color = Color.Red.rc },
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = RemoteSize(100f.rf, 100f.rf),
                    )
                }
            }
        }

        rule.onNodeWithTag("clipped").assertExists()
    }

    /**
     * Exercises the DRAW_TEXT (canvas text run) op, dispatched via the native canvas. A canvas that
     * draws a text run must compose/lay out without throwing.
     */
    @Test
    fun embeddedPlayerComposesCanvasText() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(100.dp).testTag("canvastext")) { content() }
            }
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawText("Hi".rs, 10f.rf, 50f.rf, RemotePaint().apply { color = Color.Black.rc })
            }
        }

        rule.onNodeWithTag("canvastext").assertExists()
    }

    /**
     * Exercises the DRAW_TEXT_ON_PATH op (native `Canvas.drawTextOnPath`): a canvas that lays text
     * along a path must compose/lay out without throwing.
     */
    @Test
    fun embeddedPlayerComposesTextOnPath() {
        val path = RemotePath("M 10 50 L 90 50")
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(100.dp).testTag("textonpath")) { content() }
            }
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawTextOnPath(
                    "Hi".rs,
                    path,
                    0f.rf,
                    0f.rf,
                    RemotePaint().apply { color = Color.Black.rc },
                )
            }
        }

        rule.onNodeWithTag("textonpath").assertExists()
    }

    /**
     * Exercises the DRAW_TEXT_ANCHOR op (anchored text via measured bounds): a canvas that draws
     * text centered about an anchor point must compose/lay out without throwing.
     */
    @Test
    fun embeddedPlayerComposesAnchoredText() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(100.dp).testTag("anchoredtext")) { content() }
            }
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawAnchoredText(
                    "Hi".rs,
                    50f.rf,
                    50f.rf,
                    RemotePaint().apply { color = Color.Black.rc },
                    0f.rf,
                    0f.rf,
                    0,
                )
            }
        }

        rule.onNodeWithTag("anchoredtext").assertExists()
    }

    /**
     * Exercises the DRAW_TWEEN_PATH op: a canvas that draws an interpolated path between two paths
     * must compose/lay out without throwing.
     */
    @Test
    fun embeddedPlayerComposesTweenPath() {
        val pathA = RemotePath("M 10 10 L 90 10 L 90 90 Z")
        val pathB = RemotePath("M 10 10 L 90 50 L 50 90 Z")
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(100.dp).testTag("tweenpath")) { content() }
            }
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawTweenPath(
                    pathA,
                    pathB,
                    0.5f.rf,
                    0f.rf,
                    1f.rf,
                    RemotePaint().apply { color = Color.Red.rc },
                )
            }
        }

        rule.onNodeWithTag("tweenpath").assertExists()
    }

    private companion object {
        const val AGSL_SOLID_RED = "half4 main(float2 coord) { return half4(1.0, 0.0, 0.0, 1.0); }"
    }
}
