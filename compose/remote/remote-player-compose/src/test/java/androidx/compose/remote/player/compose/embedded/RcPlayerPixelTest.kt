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

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Matrix as AndroidMatrix
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.RemoteLinearShader
import androidx.compose.remote.creation.compose.shaders.image
import androidx.compose.remote.creation.compose.shaders.verticalGradient
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteFloatArray.Companion.createNamedRemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.state.createNamedRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
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

/** Pixel verification tests for the embedded player. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerPixelTest {

    @get:Rule val rule = RcPlayerTestRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    /** Renders [content] in a 100dp player box at top-start and rasterizes the content view. */
    private fun renderPlayerToBitmap(content: @Composable @RemoteComposable () -> Unit): Bitmap {
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(modifier = Modifier.size(100.dp).testTag("player")) { it() }
            },
            content = content,
        )
        rule.waitForIdle()
        return rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
    }

    private fun redPaint() = RemotePaint().apply { color = RemoteColor(Color.Red) }

    /** A solid red rect drawn by the embedded canvas path must read back red. */
    @Test
    fun solidRectRendersRed() {
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawRect(
                    paint = redPaint(),
                    topLeft = RemoteOffset(0f.rf, 0f.rf),
                    size = RemoteSize(100f.rf, 100f.rf),
                )
            }
        }
        val px = bmp.getPixel(50, 50)
        assert(AndroidColor.red(px) > 200 && AndroidColor.green(px) < 60) {
            "Expected pure red, got #${Integer.toHexString(px)}"
        }
    }

    /**
     * Pixel-level proof that a bitmap decoded lazily (on first draw, not eagerly at composition)
     * still renders: draw a solid-red inline bitmap and read back red. Exercises the
     * `resolveBitmap` decode-on-draw path.
     */
    @Test
    fun lazilyDecodedBitmapRendersWhenDrawn() {
        val red =
            Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.RED)
            }
        val remoteBitmap = red.asImageBitmap().rb
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawImage(remoteBitmap, RemoteOffset(0f.rf, 0f.rf), null)
            }
        }
        val px = bmp.getPixel(10, 10)
        assert(AndroidColor.red(px) > 200 && AndroidColor.green(px) < 60) {
            "Lazily-decoded bitmap should render red, got #${Integer.toHexString(px)}"
        }
    }

    /**
     * Pixel-level proof that DRAW_TO_BITMAP round-trips: draw a red rect into an offscreen bitmap,
     * restore the on-screen canvas, then blit that bitmap to screen. The center pixel must be red —
     * which only holds if the offscreen redirect actually captured the rect into the bitmap and the
     * blit drew it back.
     */
    @Test
    fun drawToOffscreenBitmapRoundTripsToScreen() {
        val offscreen = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).asImageBitmap().rb
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawToOffscreenBitmap(offscreen) {
                    drawRect(
                        paint = redPaint(),
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = RemoteSize(100f.rf, 100f.rf),
                    )
                }
                drawImage(offscreen, RemoteOffset(0f.rf, 0f.rf), null)
            }
        }
        val px = bmp.getPixel(50, 50)
        assert(AndroidColor.red(px) > 200 && AndroidColor.green(px) < 60) {
            "Offscreen-rendered bitmap should blit red to screen, got #${Integer.toHexString(px)}"
        }
    }

    /**
     * Pixel-level proof that CLIP_RECT actually clips: clip to the top-left 50x50 quadrant then
     * fill the whole 100x100 canvas red. Inside the clip must be pure red; outside must NOT be red
     * (background shows through).
     */
    @Test
    fun clipRectActuallyClips() {
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                clipRect(0f.rf, 0f.rf, 50f.rf, 50f.rf) {
                    drawRect(
                        paint = redPaint(),
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = RemoteSize(100f.rf, 100f.rf),
                    )
                }
            }
        }
        val inside = bmp.getPixel(25, 25)
        val outside = bmp.getPixel(75, 75)
        val insideIsRed = AndroidColor.red(inside) > 200 && AndroidColor.green(inside) < 60
        val outsideIsRed = AndroidColor.red(outside) > 200 && AndroidColor.green(outside) < 60
        assert(insideIsRed) { "Inside clip should be red, got #${Integer.toHexString(inside)}" }
        assert(!outsideIsRed) {
            "Outside clip should NOT be red (clip leaked), got #${Integer.toHexString(outside)}"
        }
    }

    /**
     * Pixel check for a horizontal red->blue linear gradient drawn via a paint shader. Documents
     * whether the embedded player rasterizes the gradient (left red-dominant, right blue-dominant).
     */
    @Test
    fun linearGradientPixels() {
        val bmp = renderPlayerToBitmap {
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
        val y = 50
        val left = bmp.getPixel(10, y)
        val right = bmp.getPixel(90, y)
        assert(AndroidColor.red(left) > AndroidColor.blue(left)) {
            "gradient left should be red-dominant, got #${Integer.toHexString(left)}"
        }
        assert(AndroidColor.blue(right) > AndroidColor.red(right)) {
            "gradient right should be blue-dominant, got #${Integer.toHexString(right)}"
        }
    }

    /**
     * Pixel-level proof that CLIP_PATH clips: clip to a top-left 50x50 square path, then fill the
     * whole canvas red. Inside the path must be red; outside must not be.
     */
    @Test
    fun clipPathActuallyClips() {
        val clipSquare = RemotePath("M 0 0 L 50 0 L 50 50 L 0 50 Z")
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                clipPath(clipSquare) {
                    drawRect(
                        paint = redPaint(),
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = RemoteSize(100f.rf, 100f.rf),
                    )
                }
            }
        }
        val inside = bmp.getPixel(25, 25)
        val outside = bmp.getPixel(75, 75)
        val insideIsRed = AndroidColor.red(inside) > 200 && AndroidColor.green(inside) < 60
        val outsideIsRed = AndroidColor.red(outside) > 200 && AndroidColor.green(outside) < 60
        assert(insideIsRed) {
            "Inside clip path should be red, got #${Integer.toHexString(inside)}"
        }
        assert(!outsideIsRed) {
            "Outside clip path should NOT be red, got #${Integer.toHexString(outside)}"
        }
    }

    /**
     * Pixel-level proof that [LoopOperation] runs in the embedded draw stream AND loads its index
     * variable each iteration: a 3-iteration loop draws a 20px-wide red stripe at `x = index * 30`
     * (so stripes at 0, 30, 60). All three stripe centers must be red and the gap between them must
     * not — which only holds if the loop ran three times with the index advancing.
     */
    @Test
    fun loopDrawsAStripePerIterationAtTheIndexPosition() {
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                loop(from = 0f.rf, until = 3f.rf, step = 1f.rf) { index ->
                    drawRect(
                        paint = redPaint(),
                        topLeft = RemoteOffset(index * 30f, 0f.rf),
                        size = RemoteSize(20f.rf, 100f.rf),
                    )
                }
            }
        }
        fun isRed(x: Int): Boolean {
            val px = bmp.getPixel(x, 50)
            return AndroidColor.red(px) > 200 && AndroidColor.green(px) < 60
        }
        assert(isRed(10)) { "stripe 0 (index 0) should be red" }
        assert(isRed(40)) { "stripe 1 (index 1) should be red" }
        assert(isRed(70)) { "stripe 2 (index 2) should be red" }
        assert(!isRed(25)) { "gap between stripe 0 and 1 should NOT be red" }
    }

    /**
     * Proves the `WriteToDocument` escape hatch round-trips: author a red paint + full-canvas
     * `drawRect` directly against the raw [RemoteComposeWriter] (no typed DSL), capture, and render
     * through the player. The center must read back red — which only holds if the applied node
     * emitted the raw ops at the right point in the document tree.
     */
    @Test
    fun writeToDocumentEmitsRawWriterOps() {
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                document.rcPaint.setColor(AndroidColor.RED).commit()
                document.drawRect(0f, 0f, 100f, 100f)
            }
        }
        val px = bmp.getPixel(50, 50)
        assert(AndroidColor.red(px) > 200 && AndroidColor.green(px) < 60) {
            "WriteToDocument raw drawRect should render red, got #${Integer.toHexString(px)}"
        }
    }

    /**
     * Proves the border modifier honors a rounded-corner shape. A 100dp box with a 20dp-wide red
     * border rounded at radius 40dp: the middle of the top edge sits on the stroke (red), but the
     * extreme corner is rounded away (empty).
     */
    @Test
    fun roundedBorderLeavesCornersEmpty() {
        val d = rule.density.density
        val bmp = renderPlayerToBitmap {
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp)
                        .border(20.rdp, Color.Red.rc, RemoteRoundedCornerShape(40.rdp))
            )
        }
        fun isRed(x: Int, y: Int): Boolean {
            val px = bmp.getPixel((x * d).toInt(), (y * d).toInt())
            return AndroidColor.red(px) > 180 && AndroidColor.green(px) < 80
        }
        assert(isRed(50, 3)) { "top edge of the rounded border should be red" }
        assert(!isRed(3, 3)) { "rounded border corner should be empty, not red" }
    }

    /** Verifies that drawing a rect with color validation works and renders red. */
    @Test
    fun testDrawRectWithColorValidation() {
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                drawRect(
                    paint = redPaint(),
                    topLeft = RemoteOffset(0f.rf, 0f.rf),
                    size = RemoteSize(100f.rf, 100f.rf),
                )
            }
        }
        val px = bmp.getPixel(50, 50)
        assert(px == AndroidColor.RED) {
            "Expected RED color at (50, 50), but found ${Integer.toHexString(px)}"
        }
    }

    @Test
    fun roundedClipRectActuallyClipsAndUpdatesReactively() {
        val d = rule.density.density
        val document = runBlocking {
            captureRule.captureDocument(context = ApplicationProvider.getApplicationContext()) {
                val radius = remember { createNamedRemoteFloatExpression("radius") { 0f.rf } }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .clip(RemoteRoundedCornerShape(radius))
                            .background(Color.Red.rc)
                )
            }
        }
        val playerState = RcPlayerState(document)
        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag("player")) {
                RcPlayer(state = playerState)
            }
        }
        rule.waitForIdle()

        // 1. Initially radius is 0f, so the corner (3, 3) must be RED (not clipped).
        fun isRed(x: Int, y: Int): Boolean {
            val bmp = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
            val px = bmp.getPixel((x * d).toInt(), (y * d).toInt())
            return AndroidColor.red(px) > 180 && AndroidColor.green(px) < 80
        }
        assert(isRed(3, 3)) { "Initially, corner (3,3) should be red" }

        // 2. Update the variable to 40f.
        playerState.floatState("radius").value = 40f
        rule.waitForIdle()

        // 3. Now the corner (3, 3) must be empty (clipped).
        assert(!isRed(3, 3)) {
            "After updating radius to 40f, corner (3,3) should be empty (clipped)"
        }
        assert(isRed(50, 3)) { "But the top-center (50,3) should still be red" }
    }

    @Test
    fun chartWithNamedFloatArrayUpdatesReactively() {
        val d = rule.density.density
        val initialData = floatArrayOf(20f, 60f, 90f)
        val document = runBlocking {
            captureRule.captureDocument(context = ApplicationProvider.getApplicationContext()) {
                val chartData = remember {
                    createNamedRemoteFloatArray(
                        name = "chartData",
                        defaultValue = initialData,
                    )
                }
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val paintRed = RemotePaint().apply { color = RemoteColor(Color.Red) }
                    val paintGreen = RemotePaint().apply { color = RemoteColor(Color.Green) }
                    val paintBlue = RemotePaint().apply { color = RemoteColor(Color.Blue) }
                    drawRect(
                        paint = paintRed,
                        topLeft = RemoteOffset(10f.rf, 100f.rf - chartData[0]),
                        size = RemoteSize(20f.rf, chartData[0]),
                    )
                    drawRect(
                        paint = paintGreen,
                        topLeft = RemoteOffset(40f.rf, 100f.rf - chartData[1]),
                        size = RemoteSize(20f.rf, chartData[1]),
                    )
                    drawRect(
                        paint = paintBlue,
                        topLeft = RemoteOffset(70f.rf, 100f.rf - chartData[2]),
                        size = RemoteSize(20f.rf, chartData[2]),
                    )
                }
            }
        }

        val playerState = RcPlayerState(document)
        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag("player")) {
                RcPlayer(state = playerState)
            }
        }
        rule.waitForIdle()

        fun isColorAt(x: Int, y: Int, check: (Int) -> Boolean): Boolean {
            val bmp = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
            val px = bmp.getPixel((x * d).toInt(), (y * d).toInt())
            return check(px)
        }

        fun isRed(px: Int) =
            AndroidColor.red(px) > 200 && AndroidColor.green(px) < 60 && AndroidColor.blue(px) < 60
        fun isGreen(px: Int) =
            AndroidColor.green(px) > 200 && AndroidColor.red(px) < 60 && AndroidColor.blue(px) < 60
        fun isBlue(px: Int) =
            AndroidColor.blue(px) > 200 && AndroidColor.red(px) < 60 && AndroidColor.green(px) < 60

        // Bar 1 (x: 10..30): initial height = 20, so y in 80..100 is red, y=70 is not red.
        assert(!isColorAt(20, 70, ::isRed)) { "Bar 1 at y=70 should not be red initially" }
        assert(isColorAt(20, 90, ::isRed)) { "Bar 1 at y=90 should be red initially" }

        // Bar 2 (x: 40..60): initial height = 60, so y in 40..100 is green, y=30 is not green.
        assert(!isColorAt(50, 30, ::isGreen)) { "Bar 2 at y=30 should not be green initially" }
        assert(isColorAt(50, 50, ::isGreen)) { "Bar 2 at y=50 should be green initially" }

        // Bar 3 (x: 70..90): initial height = 90, so y in 10..100 is blue.
        assert(isColorAt(80, 20, ::isBlue)) { "Bar 3 at y=20 should be blue initially" }

        // Update the named float array state via State
        val updatedData = floatArrayOf(80f, 20f, 40f)
        playerState.floatArrayState("chartData").value = updatedData
        rule.waitForIdle()

        // After update:
        // Bar 1 now height = 80: y in 20..100 is red! So y=70 is now red.
        assert(isColorAt(20, 70, ::isRed)) {
            "Bar 1 at y=70 should be red after update to height 80"
        }

        // Bar 2 now height = 20: y in 80..100 is green. y=50 is NO LONGER green!
        assert(!isColorAt(50, 50, ::isGreen)) {
            "Bar 2 at y=50 should no longer be green after update to height 20"
        }
        assert(isColorAt(50, 90, ::isGreen)) { "Bar 2 at y=90 should be green after update" }

        // Bar 3 now height = 40: y in 60..100 is blue. y=20 is NO LONGER blue!
        assert(!isColorAt(80, 20, ::isBlue)) {
            "Bar 3 at y=20 should no longer be blue after update to height 40"
        }
        assert(isColorAt(80, 70, ::isBlue)) { "Bar 3 at y=70 should be blue after update" }
    }

    /**
     * Direct non-Component paint operations placed inside RootLayoutComponent (without a child
     * CanvasLayout) must still execute when rendered through the embedded player.
     */
    @Test
    fun rootLayoutComponent_executesDirectDrawOperations() {
        val writer = RemoteComposeWriterAndroid(100, 100, "test", AndroidxRcPlatformServices())
        writer.root {
            writer.getRcPaint().setColor(0xFFFF0000.toInt()).commit()
            writer.drawRect(0f, 0f, 100f, 100f)
        }
        val bytes = writer.buffer()
        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(bytes, 0, writer.bufferSize()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag("player")) {
                RcPlayer(document = document)
            }
        }
        rule.waitForIdle()

        val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        val centerPixel = bitmap.getPixel(50, 50)
        assertThat(AndroidColor.red(centerPixel)).isGreaterThan(200)
        assertThat(AndroidColor.green(centerPixel)).isLessThan(50)
        assertThat(AndroidColor.blue(centerPixel)).isLessThan(50)
    }

    @Test
    fun drawWithContent_chainedModifiersWithPadding_drawsOuterAndInnerInOrder() {
        val bmp = renderPlayerToBitmap {
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp)
                        .drawWithContent {
                            drawRect(
                                paint = RemotePaint { color = Color.Red.rc },
                                topLeft = RemoteOffset.Zero,
                                size = size,
                            )
                            drawContent()
                            drawRect(
                                paint = RemotePaint { color = Color.Green.rc },
                                topLeft = RemoteOffset.Zero,
                                size = RemoteSize(width, 10f.rf),
                            )
                        }
                        .padding(20.rdp)
                        .drawWithContent {
                            drawRect(
                                paint = RemotePaint { color = Color.Blue.rc },
                                topLeft = RemoteOffset.Zero,
                                size = size,
                            )
                            drawContent()
                        }
            ) {}
        }

        // Top 10px strip was drawn after drawContent() by the outer modifier -> Green
        val topStrip = bmp.getPixel(50, 5)
        assertThat(AndroidColor.green(topStrip)).isGreaterThan(200)
        assertThat(AndroidColor.red(topStrip)).isLessThan(50)

        // Outer padding band (outside 20..80) -> Red
        val outerBand = bmp.getPixel(10, 50)
        assertThat(AndroidColor.red(outerBand)).isGreaterThan(200)
        assertThat(AndroidColor.blue(outerBand)).isLessThan(50)

        // Inner padded content area (inside 20..80) -> Blue
        val innerCenter = bmp.getPixel(50, 50)
        assertThat(AndroidColor.blue(innerCenter)).isGreaterThan(200)
        assertThat(AndroidColor.red(innerCenter)).isLessThan(50)
    }

    @Test
    fun remoteCanvas_chainedDrawWithContent_drawsInOuterToInnerOrder() {
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(
                modifier =
                    RemoteModifier.size(100.rdp)
                        .drawWithContent {
                            drawRect(
                                paint = RemotePaint { color = Color.Red.rc },
                                topLeft = RemoteOffset.Zero,
                                size = RemoteSize(100f.rf, 100f.rf),
                            )
                            drawContent()
                            drawRect(
                                paint = RemotePaint { color = Color.Green.rc },
                                topLeft = RemoteOffset.Zero,
                                size = RemoteSize(100f.rf, 10f.rf),
                            )
                        }
                        .drawWithContent {
                            drawRect(
                                paint = RemotePaint { color = Color.Blue.rc },
                                topLeft = RemoteOffset(20f.rf, 20f.rf),
                                size = RemoteSize(60f.rf, 60f.rf),
                            )
                            drawContent()
                        }
            ) {
                drawRect(
                    paint = RemotePaint { color = Color.Yellow.rc },
                    topLeft = RemoteOffset(40f.rf, 40f.rf),
                    size = RemoteSize(20f.rf, 20f.rf),
                )
            }
        }

        val topStrip = bmp.getPixel(50, 5)
        assertThat(AndroidColor.green(topStrip)).isGreaterThan(200)
        val outerPixel = bmp.getPixel(10, 50)
        assertThat(AndroidColor.red(outerPixel)).isGreaterThan(200)
        val midPixel = bmp.getPixel(25, 50)
        assertThat(AndroidColor.blue(midPixel)).isGreaterThan(200)
        val centerPixel = bmp.getPixel(50, 50)
        assertThat(AndroidColor.red(centerPixel)).isGreaterThan(200)
        assertThat(AndroidColor.green(centerPixel)).isGreaterThan(200)
        assertThat(AndroidColor.blue(centerPixel)).isLessThan(50)
    }

    @Test
    fun duplicateDrawContentOperationsOnWire_executesCanvasOperationsOnce() {
        val writer = RemoteComposeWriterAndroid(100, 100, "test", AndroidxRcPlatformServices())
        writer.root {
            writer.startBox(
                RecordingModifier()
                    .size(100, 100)
                    .background(AndroidColor.BLACK)
                    .drawContent()
                    .drawContent()
            )
            writer.startCanvasOperations()
            writer.getRcPaint().setColor(0x40FFFFFF).commit()
            writer.drawRect(0f, 0f, 100f, 100f)
            writer.drawComponentContent()
            writer.endCanvasOperations()
            writer.endBox()
        }
        val bytes = writer.buffer()
        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(bytes, 0, writer.bufferSize()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag("player")) {
                RcPlayer(document = document)
            }
        }
        rule.waitForIdle()

        val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        val center = Color(bitmap.getPixel(50, 50))
        // Drawn once -> ~0.25f; if executed twice due to duplicate DrawContentOperation -> ~0.44f
        assertThat(center.red).isWithin(0.06f).of(0.25f)
    }

    @Test
    fun paintColorAlpha_dimsGradientBrushAndBitmap() {
        val whiteBitmap =
            Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)
                .apply { eraseColor(AndroidColor.WHITE) }
                .asImageBitmap()

        val bmp = renderPlayerToBitmap {
            val remoteImage = remember {
                createNamedRemoteImageBitmap("white_bitmap_alpha") { whiteBitmap }
            }
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp).background(Color.Black.rc).drawWithContent {
                        // Left half: drawImage with paint.color alpha = 0.25f
                        drawImage(
                            image = remoteImage,
                            topLeft = RemoteOffset.Zero,
                            paint = RemotePaint { color = Color.White.rc.copy(alpha = 0.25f.rf) },
                        )

                        // Right half: Gradient brush with paint.color alpha = 0.25f
                        val gradPaint = RemotePaint {
                            with(RemoteBrush.verticalGradient(listOf(Color.Red.rc, Color.Red.rc))) {
                                applyTo(this@RemotePaint, size)
                            }
                            color = color.copy(alpha = 0.25f.rf)
                        }
                        drawRect(
                            paint = gradPaint,
                            topLeft = RemoteOffset(50f.rf, 0f.rf),
                            size = RemoteSize(50f.rf, 100f.rf),
                        )
                        drawContent()
                    }
            ) {}
        }

        val leftCenter = Color(bmp.getPixel(25, 50))
        assertThat(leftCenter.red).isWithin(0.05f).of(0.25f)
        assertThat(leftCenter.green).isWithin(0.05f).of(0.25f)
        assertThat(leftCenter.blue).isWithin(0.05f).of(0.25f)

        val rightCenter = Color(bmp.getPixel(75, 50))
        assertThat(rightCenter.red).isWithin(0.05f).of(0.25f)
        assertThat(rightCenter.green).isLessThan(0.05f)
        assertThat(rightCenter.blue).isLessThan(0.05f)
    }

    @Test
    fun imageBrushWithContentScale_appliesMatrixExpressionAndPaintColorAlpha() {
        // 200x100 bitmap: left/right 50px bands are BLUE, middle 100px band is WHITE.
        // Cropped into a 100x100 square with ContentScale.Crop, the outer BLUE bands are cropped
        // away and the center WHITE band fills the square at alpha = 0.25f.
        val splitBitmap =
            Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
                .apply {
                    for (y in 0 until 100) {
                        for (x in 0 until 200) {
                            setPixel(
                                x,
                                y,
                                if (x in 50 until 150) AndroidColor.WHITE else AndroidColor.BLUE,
                            )
                        }
                    }
                }
                .asImageBitmap()

        val bmp = renderPlayerToBitmap {
            val remoteImage = remember {
                createNamedRemoteImageBitmap("split_shader_img") { splitBitmap }
            }
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp).background(Color.Black.rc).drawWithContent {
                        val imgPaint = RemotePaint {
                            with(RemoteBrush.image(remoteImage, contentScale = ContentScale.Crop)) {
                                applyTo(this@RemotePaint, size)
                            }
                            color = color.copy(alpha = 0.25f.rf)
                        }
                        drawRect(paint = imgPaint, topLeft = RemoteOffset.Zero, size = size)
                        drawContent()
                    }
            ) {}
        }

        // Near the left edge (x=10), ContentScale.Crop shifts x=50..150 into 0..100 -> dimmed WHITE
        // (without MatrixExpression, x=10 would sample unshifted x=10 -> BLUE!)
        val leftEdge = Color(bmp.getPixel(10, 50))
        assertThat(leftEdge.red).isWithin(0.05f).of(0.25f)
        assertThat(leftEdge.green).isWithin(0.05f).of(0.25f)
        assertThat(leftEdge.blue).isWithin(0.05f).of(0.25f)
    }

    @Test
    fun matrixConstant_appliesShaderMatrix() {
        val splitAndroidBitmap =
            Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888).apply {
                for (y in 0 until 100) {
                    for (x in 0 until 200) {
                        setPixel(x, y, if (x >= 100) AndroidColor.GREEN else AndroidColor.BLUE)
                    }
                }
            }
        val writer = RemoteComposeWriterAndroid(100, 100, "test", AndroidxRcPlatformServices())
        val bitmapId = writer.addBitmap(splitAndroidBitmap)
        val matrixId = writer.addMatrixConst(AndroidMatrix().apply { setTranslate(-100f, 0f) })
        writer.root {
            writer
                .getRcPaint()
                .setTextureShader(bitmapId, 0, 0, 0, 0)
                .setShaderMatrix(matrixId)
                .commit()
            writer.drawRect(0f, 0f, 100f, 100f)
        }
        val bytes = writer.buffer()
        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(bytes, 0, writer.bufferSize()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag("player")) {
                RcPlayer(document = document)
            }
        }
        rule.waitForIdle()

        val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        val center = Color(bitmap.getPixel(50, 50))
        assertThat(center.green).isGreaterThan(0.8f)
        assertThat(center.blue).isLessThan(0.2f)
    }
}
