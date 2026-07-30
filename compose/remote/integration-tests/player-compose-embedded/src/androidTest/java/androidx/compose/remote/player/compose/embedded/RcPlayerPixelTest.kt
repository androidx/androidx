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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pixel verification tests for the embedded player. */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerPixelTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    /** Renders [content] in a 100dp player box at top-start and rasterizes the content view. */
    private fun renderPlayerToBitmap(content: @Composable @RemoteComposable () -> Unit): Bitmap {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val document = runBlocking { captureRule.captureDocument(context = ctx, content = content) }
        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag("player")) {
                RcPlayer(document = document)
            }
        }
        rule.waitForIdle()
        return rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
    }

    private fun redPaint() = RemotePaint().apply { color = RemoteColor(Color.Red) }

    /** A solid red rect drawn by the embedded canvas path must read back red. */
    @Test
    fun solidRectRendersRed() {
        val d = rule.density.density
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
        val d = rule.density.density
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
        val d = rule.density.density
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
        val d = rule.density.density
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
        val d = rule.density.density
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val paint =
                    RemotePaint().apply {
                        shader =
                            androidx.compose.remote.creation.compose.shaders.RemoteLinearShader(
                                0f.rf,
                                0f.rf,
                                100f.rf,
                                0f.rf,
                                listOf(Color.Red.rc, Color.Blue.rc),
                                null,
                                androidx.compose.ui.graphics.TileMode.Clamp,
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
        val d = rule.density.density
        val clipSquare =
            androidx.compose.remote.creation.RemotePath("M 0 0 L 50 0 L 50 50 L 0 50 Z")
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
     * Pixel-level proof that [androidx.compose.remote.core.operations.layout.LoopOperation] runs in
     * the embedded draw stream AND loads its index variable each iteration: a 3-iteration loop
     * draws a 20px-wide red stripe at `x = index * 30` (so stripes at 0, 30, 60). All three stripe
     * centers must be red and the gap between them must not — which only holds if the loop ran
     * three times with the index advancing.
     */
    @Test
    fun loopDrawsAStripePerIterationAtTheIndexPosition() {
        val d = rule.density.density
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
     * `drawRect` directly against the raw [androidx.compose.remote.creation.RemoteComposeWriter]
     * (no typed DSL), capture, and render through the player. The center must read back red — which
     * only holds if the applied node emitted the raw ops at the right point in the document tree.
     */
    @Test
    fun writeToDocumentEmitsRawWriterOps() {
        val d = rule.density.density
        val bmp = renderPlayerToBitmap {
            RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                val writer = remoteCanvas.internalCanvas
                val paint =
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.RED
                        style = android.graphics.Paint.Style.FILL
                    }
                writer.drawRect(0f, 0f, 100f, 100f, paint)
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
        val d = rule.density.density
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
            captureRule.captureDocument(context = rule.activity) {
                val radius = rememberNamedRemoteFloat("radius") { 0f.rf }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .clip(RemoteRoundedCornerShape(radius))
                            .background(Color.Red.rc)
                )
            }
        }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag("player")) {
                RcPlayer(document = document)
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
        val radiusId = document.getVariableIdByName("USER:radius")
        document.remoteComposeState.overrideFloat(radiusId, 40f)
        rule.waitForIdle()

        // 3. Now the corner (3, 3) must be empty (clipped).
        assert(!isRed(3, 3)) {
            "After updating radius to 40f, corner (3,3) should be empty (clipped)"
        }
        assert(isRed(50, 3)) { "But the top-center (50,3) should still be red" }
    }

    private fun findVariableId(ops: List<Operation>, name: String): Int? {
        for (op in ops) {
            if (op is NamedVariable && op.mVarName == name) {
                return op.mVarId
            }
            if (op is Container) {
                findVariableId(op.list, name)?.let {
                    return it
                }
            }
        }
        return null
    }

    private fun CoreDocument.getVariableIdByName(name: String): Int {
        return findVariableId(getOperations(), name)
            ?: throw IllegalArgumentException("Named variable not found: $name")
    }
}
