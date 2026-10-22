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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.RemoteContext
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
import androidx.compose.remote.player.compose.embedded.draw.ComposeLocalPaint
import androidx.compose.remote.player.compose.embedded.draw.updatePaintFromBundle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
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

/** Shader verification under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerShaderTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun runtimeShaderDisabledByDefaultWhenTrustedContentIsFalse() {
        var shaderId = 0
        lateinit var remoteContext: RemoteContext
        val document = buildDocument {
            shaderId = writer.createShader(AGSL_SOLID_RED).commit()
            writer.drawCircle(1f, 1f, 1f)
        }

        rule.setContent {
            val state = rememberRcPlayerState(document)
            remoteContext = state.remoteContext
            Box(modifier = Modifier.size(100.dp).testTag("player")) { RcPlayer(state = state) }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("player").captureToImage()

        val bundle = PaintBundle().apply { setShader(shaderId) }
        val paintState = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paintState, remoteContext)

        assertThat(remoteContext.mRemoteComposeState.getFromId(shaderId)).isNull()
        assertThat(paintState.brush).isNull()
        assertThat(paintState.nativeShader).isNull()
    }

    @Test
    fun runtimeShaderEnabledWhenTrustedContentIsTrue() {
        var shaderId = 0
        lateinit var remoteContext: RemoteContext
        val document = buildDocument {
            shaderId = writer.createShader(AGSL_SOLID_RED).commit()
            writer.drawCircle(1f, 1f, 1f)
        }

        rule.setContent {
            val state = rememberRcPlayerState(document)
            remoteContext = state.remoteContext
            Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = state, trustedContent = true) }
        }
        rule.waitForIdle()

        val bundle = PaintBundle().apply { setShader(shaderId) }
        val paintState = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paintState, remoteContext)

        assertThat(remoteContext.mRemoteComposeState.getFromId(shaderId)).isNotNull()
        assertThat(paintState.brush).isInstanceOf(ShaderBrush::class.java)
        assertThat(paintState.nativeShader).isNotNull()
    }

    /**
     * The embedded player should ingest a shader/gradient document and run its draw path without
     * throwing (it constructs a Compose gradient `Brush` in RcPlayerDrawing). Pixel output isn't
     * checkable here (see class doc), so this asserts the player composes and lays out the node.
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
     * Exercises the CLIP_RECT draw op (now dispatched via drawContext.canvas.clipRect): a canvas
     * that clips to a sub-rectangle and then draws a full-size rect must compose/lay out without
     * throwing. Pixel correctness of the clip isn't checkable here (see class doc), so this asserts
     * the path runs end to end.
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
     * Exercises the DRAW_TEXT (canvas text run) op, newly dispatched via the native canvas. A
     * canvas that draws a text run must compose/lay out without throwing. (Pixel correctness isn't
     * checkable here — see class doc. Layout `Text` is the separately-verified path.)
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
     * along a path must compose/lay out without throwing. (Curved text isn't pixel-assertable here
     * — see class doc.)
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
     * text centered about an anchor point must compose/lay out without throwing. (Anchor placement
     * isn't pixel-assertable here — see class doc.)
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
     * must compose/lay out without throwing. (Path shape isn't pixel-assertable here — see class
     * doc.)
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
