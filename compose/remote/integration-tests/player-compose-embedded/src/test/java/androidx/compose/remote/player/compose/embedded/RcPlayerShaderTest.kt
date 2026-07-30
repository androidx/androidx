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
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteLinearShader
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Shader verification under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerShaderTest {

    @get:Rule val rule = createComposeRule()

    /**
     * Baseline: does the Robolectric NATIVE graphics pipeline actually rasterize a `Shader`? Draws
     * a horizontal red->blue `LinearGradient` straight to an `android.graphics.Bitmap` (no Compose,
     * no RcPlayer) and checks the ends differ in the expected direction. This is the "do shaders
     * work in Robolectric" question in its purest form.
     */

    /**
     * The embedded player should ingest a shader/gradient document and run its draw path without
     * throwing (it constructs a Compose gradient `Brush` in RcPlayerDrawing). Pixel output isn't
     * checkable here (see class doc), so this asserts the player composes and lays out the node.
     */
    @Test
    fun embeddedPlayerComposesGradientDocument() {
        kotlinx.coroutines.runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val content: @Composable @RemoteComposable () -> Unit = {
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
                                    TileMode.Clamp,
                                )
                        }
                    drawRect(
                        paint = paint,
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = RemoteSize(100f.rf, 100f.rf),
                    )
                }
            }
            val bytes = captureSingleRemoteDocument(context = context, content = content).bytes
            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("gradient")) {
                    RcPlayer(document = document)
                }
            }
            rule.mainClock.advanceTimeBy(100)
            rule.waitForIdle()

            // Composed + laid out without throwing through the gradient draw path.
            rule.onNodeWithTag("gradient").assertExists()
        }
    }

    /**
     * Exercises the CLIP_RECT draw op (now dispatched via drawContext.canvas.clipRect): a canvas
     * that clips to a sub-rectangle and then draws a full-size rect must compose/lay out without
     * throwing. Pixel correctness of the clip isn't checkable here (see class doc), so this asserts
     * the path runs end to end.
     */
    @Test
    fun embeddedPlayerComposesClippedCanvas() {
        kotlinx.coroutines.runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val content: @Composable @RemoteComposable () -> Unit = {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    // Scoped clip (emits CLIP_RECT + save/restore around the block).
                    clipRect(0f.rf, 0f.rf, 50f.rf, 50f.rf) {
                        drawRect(
                            paint =
                                RemotePaint().apply {
                                    color =
                                        androidx.compose.remote.creation.compose.state.RemoteColor(
                                            Color.Red
                                        )
                                },
                            topLeft = RemoteOffset(0f.rf, 0f.rf),
                            size = RemoteSize(100f.rf, 100f.rf),
                        )
                    }
                }
            }
            val bytes = captureSingleRemoteDocument(context = context, content = content).bytes
            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("clipped")) {
                    RcPlayer(document = document)
                }
            }
            rule.mainClock.advanceTimeBy(100)
            rule.waitForIdle()

            rule.onNodeWithTag("clipped").assertExists()
        }
    }

    /**
     * Exercises the DRAW_TEXT (canvas text run) op, newly dispatched via the native canvas. A
     * canvas that draws a text run must compose/lay out without throwing. (Pixel correctness isn't
     * checkable here — see class doc. Layout `Text` is the separately-verified path.)
     */
    @Test
    fun embeddedPlayerComposesCanvasText() {
        kotlinx.coroutines.runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val content: @Composable @RemoteComposable () -> Unit = {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    drawText(
                        "Hi".rs,
                        10f.rf,
                        50f.rf,
                        RemotePaint().apply {
                            color =
                                androidx.compose.remote.creation.compose.state.RemoteColor(
                                    Color.Black
                                )
                        },
                    )
                }
            }
            val bytes = captureSingleRemoteDocument(context = context, content = content).bytes
            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("canvastext")) {
                    RcPlayer(document = document)
                }
            }
            rule.mainClock.advanceTimeBy(100)
            rule.waitForIdle()

            rule.onNodeWithTag("canvastext").assertExists()
        }
    }

    /**
     * Exercises the DRAW_TEXT_ON_PATH op (native `Canvas.drawTextOnPath`): a canvas that lays text
     * along a path must compose/lay out without throwing. (Curved text isn't pixel-assertable here
     * — see class doc.)
     */
    @Test
    fun embeddedPlayerComposesTextOnPath() {
        kotlinx.coroutines.runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val path = androidx.compose.remote.creation.RemotePath("M 10 50 L 90 50")
            val content: @Composable @RemoteComposable () -> Unit = {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    drawTextOnPath(
                        "Hi".rs,
                        path,
                        0f.rf,
                        0f.rf,
                        RemotePaint().apply {
                            color =
                                androidx.compose.remote.creation.compose.state.RemoteColor(
                                    Color.Black
                                )
                        },
                    )
                }
            }
            val bytes = captureSingleRemoteDocument(context = context, content = content).bytes
            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("textonpath")) {
                    RcPlayer(document = document)
                }
            }
            rule.mainClock.advanceTimeBy(100)
            rule.waitForIdle()

            rule.onNodeWithTag("textonpath").assertExists()
        }
    }

    /**
     * Exercises the DRAW_TEXT_ANCHOR op (anchored text via measured bounds): a canvas that draws
     * text centered about an anchor point must compose/lay out without throwing. (Anchor placement
     * isn't pixel-assertable here — see class doc.)
     */
    @Test
    fun embeddedPlayerComposesAnchoredText() {
        kotlinx.coroutines.runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val content: @Composable @RemoteComposable () -> Unit = {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    drawAnchoredText(
                        "Hi".rs,
                        50f.rf,
                        50f.rf,
                        RemotePaint().apply {
                            color =
                                androidx.compose.remote.creation.compose.state.RemoteColor(
                                    Color.Black
                                )
                        },
                        0f.rf,
                        0f.rf,
                        0,
                    )
                }
            }
            val bytes = captureSingleRemoteDocument(context = context, content = content).bytes
            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("anchoredtext")) {
                    RcPlayer(document = document)
                }
            }
            rule.mainClock.advanceTimeBy(100)
            rule.waitForIdle()

            rule.onNodeWithTag("anchoredtext").assertExists()
        }
    }

    /**
     * Exercises the DRAW_TWEEN_PATH op: a canvas that draws an interpolated path between two paths
     * must compose/lay out without throwing. (Path shape isn't pixel-assertable here — see class
     * doc.)
     */
    @Test
    fun embeddedPlayerComposesTweenPath() {
        kotlinx.coroutines.runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val pathA = androidx.compose.remote.creation.RemotePath("M 10 10 L 90 10 L 90 90 Z")
            val pathB = androidx.compose.remote.creation.RemotePath("M 10 10 L 90 50 L 50 90 Z")
            val content: @Composable @RemoteComposable () -> Unit = {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    drawTweenPath(
                        pathA,
                        pathB,
                        0.5f.rf,
                        0f.rf,
                        1f.rf,
                        RemotePaint().apply {
                            color =
                                androidx.compose.remote.creation.compose.state.RemoteColor(
                                    Color.Red
                                )
                        },
                    )
                }
            }
            val bytes = captureSingleRemoteDocument(context = context, content = content).bytes
            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                Box(modifier = androidx.compose.ui.Modifier.size(100.dp).testTag("tweenpath")) {
                    RcPlayer(document = document)
                }
            }
            rule.mainClock.advanceTimeBy(100)
            rule.waitForIdle()

            rule.onNodeWithTag("tweenpath").assertExists()
        }
    }
}
