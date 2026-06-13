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

package androidx.compose.ui.graphics.blur

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class ProgressiveBlurEffectTest {

    private val density = Density(2f)
    private val size = Size(200f, 200f)

    private fun effect(radius: BlurRadiusSpec) = radius.createRenderEffect(size, density)

    @Test
    fun uniformZeroRadiusProducesIdentityEffect() {
        val result = effect(BlurRadiusSpec.uniform(0.dp))
        assertTrue(result.isSupported())
        assertTrue(renderSplit(BlurRadiusSpec.uniform(0.dp)).sameAs(renderSplit(null)))
    }

    @Test
    fun uniformNonZeroRadiusIsSupported() {
        val result = effect(BlurRadiusSpec.uniform(8.dp))
        assertTrue(result.isSupported())
        assertNotNull(result.asAndroidRenderEffect())
    }

    @Test
    fun zeroGradientProducesIdentityEffect() {
        // All-zero stops produce an effectively-zero configuration. The rendered output must be
        // pixel-identical to the unblurred content.
        val zero = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 0.dp)
        assertNotNull(effect(zero).asAndroidRenderEffect())
        assertTrue(renderSplit(zero).sameAs(renderSplit(null)))
    }

    @Test
    fun verticalGradientIsSupported() {
        val result = effect(BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp))
        assertTrue(result.isSupported())
        assertNotNull(result.asAndroidRenderEffect())
    }

    @Test
    fun horizontalGradientProducesEffect() {
        val result =
            effect(BlurRadiusSpec.horizontalGradient(startRadius = 4.dp, endRadius = 20.dp))
        assertNotNull(result.asAndroidRenderEffect())
    }

    @Test
    fun platformEffectIsCreatedLazilyOncePerInstance() {
        val result = effect(BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp))
        assertSame(result.asAndroidRenderEffect(), result.asAndroidRenderEffect())
    }

    @Test
    fun axisAlignedLinearGradientProducesEffect() {
        val result =
            effect(
                // The 200px layer is 100dp at the 2x test density, so this line spans its full
                // height.
                BlurRadiusSpec.linearGradient(
                    start = DpOffset(0.dp, 0.dp),
                    end = DpOffset(0.dp, 100.dp),
                    startRadius = 0.dp,
                    endRadius = 20.dp,
                )
            )
        assertNotNull(result.asAndroidRenderEffect())
    }

    @Test
    fun diagonalLinearGradientProducesEffect() {
        // Diagonal lines must route through the custom mask shader rather than the axis-aligned
        // fast path.
        val result =
            effect(
                BlurRadiusSpec.linearGradient(
                    start = DpOffset(0.dp, 0.dp),
                    end = DpOffset(100.dp, 100.dp),
                    startRadius = 0.dp,
                    endRadius = 20.dp,
                )
            )
        assertTrue(result.isSupported())
        assertNotNull(result.asAndroidRenderEffect())
    }

    @Test
    fun multiStopGradientProducesEffect() {
        val result =
            effect(
                BlurRadiusSpec.verticalGradient(
                    listOf(
                        BlurStop(fraction = 0f, radius = 0.dp),
                        BlurStop(fraction = 0.5f, radius = 8.dp),
                        BlurStop(fraction = 1f, radius = 20.dp),
                    )
                )
            )
        assertNotNull(result.asAndroidRenderEffect())
    }

    @Test
    fun radialGradientProducesEffect() {
        val result = effect(BlurRadiusSpec.radialGradient(startRadius = 0.dp, endRadius = 20.dp))
        assertNotNull(result.asAndroidRenderEffect())
    }

    @Test
    fun radialGradientWithExplicitCenterAndExtentProducesEffect() {
        val result =
            effect(
                BlurRadiusSpec.radialGradient(
                    startRadius = 0.dp,
                    endRadius = 20.dp,
                    center = DpOffset(50.dp, 50.dp),
                    fallOffRadius = 40.dp,
                )
            )
        assertNotNull(result.asAndroidRenderEffect())
    }

    @Test
    fun customMaskIsSupported() {
        val radiusShader =
            RuntimeShader(
                """
                half4 main(float2 coord) { return half4(0.0, 0.0, 0.0, 1.0); }
                """
                    .trimIndent()
            )
        val result = effect(BlurRadiusSpec.shader(8.dp) { radiusShader })
        assertTrue(result.isSupported())
        assertNotNull(result.asAndroidRenderEffect())
    }

    @Test
    fun gradientShaderMaskBlursWhereAlphaIsHigh() {
        // Any platform Shader can serve as the mask, not just RuntimeShader. A gradient whose
        // alpha ramps 0 -> 1 top to bottom keeps the top sharp and blurs the bottom.
        val gradient =
            BlurRadiusSpec.shader(20.dp) { sizePx ->
                android.graphics.LinearGradient(
                    0f,
                    0f,
                    0f,
                    sizePx.height,
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.BLACK,
                    android.graphics.Shader.TileMode.CLAMP,
                )
            }
        val bitmap = renderSplit(gradient)
        // Top rows: mask alpha ~0 -> radius < 1px -> exact source colors.
        for (y in 0..2) bitmap.assertSharpBoundaryAt(y)
        // Bottom rows: mask alpha ~1 -> ~20px radius -> boundary pixels are a black/white mix.
        for (y in PIXEL_TEST_SIZE - 3 until PIXEL_TEST_SIZE) bitmap.assertBlurredBoundaryAt(y)
    }

    @Test
    fun verticalGradientBlursOnlyWhereRadiusIsNonZero() {
        val bitmap =
            renderSplit(BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp))
        // Top rows: gradient intensity ~0 -> radius < 1px -> exact source colors.
        for (y in 0..2) bitmap.assertSharpBoundaryAt(y)
        // Bottom rows: radius ~20px -> boundary pixels are a black/white mix.
        for (y in PIXEL_TEST_SIZE - 3 until PIXEL_TEST_SIZE) bitmap.assertBlurredBoundaryAt(y)
    }

    @Test
    fun radialGradientIsSharpAtCenterAndBlurredAtEdge() {
        val bitmap =
            renderSplit(BlurRadiusSpec.radialGradient(startRadius = 0.dp, endRadius = 20.dp))
        // On the boundary at the gradient center: radius ~0 -> sharp.
        bitmap.assertSharpBoundaryAt(PIXEL_TEST_SIZE / 2)
        // On the boundary near the bottom edge (~90% of the default falloff): blurred.
        bitmap.assertBlurredBoundaryAt(PIXEL_TEST_SIZE - 5)
    }

    @Test
    fun maskAlphaScalesMaxRadiusAndClampsAboveOne() {
        // The mask's sampled alpha is a 0..1 intensity over maxRadius. Full intensity at a larger
        // maxRadius must spread the boundary wider, and alpha above 1.0 must clamp to 1.0 rather
        // than exceed maxRadius. renderSplit uses Density(1f), so dp values equal pixels here.
        fun spread(alpha: Float, maxRadius: Dp): Int {
            val shader =
                RuntimeShader("half4 main(float2 coord) { return half4(0.0, 0.0, 0.0, $alpha); }")
            val bitmap = renderSplit(BlurRadiusSpec.shader(maxRadius) { shader })
            val y = PIXEL_TEST_SIZE / 2
            var count = 0
            for (x in 0 until PIXEL_TEST_SIZE) {
                val red = android.graphics.Color.red(bitmap.getPixel(x, y))
                if (red in 6..249) count++
            }
            return count
        }

        val narrow = spread(1f, 2.dp)
        val wide = spread(1f, 20.dp)
        assertTrue(
            "full-intensity 2dp mask must blur boundary pixels, spread was $narrow",
            narrow >= 1,
        )
        assertTrue("20dp spread ($wide) must be > 2x the 2dp spread ($narrow)", wide > narrow * 2)
        // Out-of-range alpha clamps to full intensity: identical output to alpha exactly 1.0.
        assertEquals(wide, spread(10f, 20.dp))
    }

    @Test
    fun decalEdgeTreatmentFadesAlphaAtBoundsClampDoesNot() {
        fun renderFullBleedWhite(tileMode: TileMode): Bitmap {
            val radius = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
            val effect = radius.createRenderEffect(pixelTestSize, Density(1f), tileMode)
            return renderWithEffect(effect) { canvas ->
                canvas.drawRect(
                    0f,
                    0f,
                    PIXEL_TEST_SIZE.toFloat(),
                    PIXEL_TEST_SIZE.toFloat(),
                    Paint().apply { color = android.graphics.Color.WHITE },
                )
            }
        }

        val decal = renderFullBleedWhite(TileMode.Decal)
        val decalEdgeAlpha =
            android.graphics.Color.alpha(decal.getPixel(PIXEL_TEST_SIZE / 2, PIXEL_TEST_SIZE - 1))
        val decalInteriorAlpha =
            android.graphics.Color.alpha(decal.getPixel(PIXEL_TEST_SIZE / 2, PIXEL_TEST_SIZE / 2))
        assertTrue(
            "decal must keep the interior opaque, was $decalInteriorAlpha",
            decalInteriorAlpha >= 254,
        )
        assertTrue(
            "decal must fade the bottom edge toward transparent, alpha was $decalEdgeAlpha",
            decalEdgeAlpha < 250,
        )
        assertTrue("faded edge must remain visible, alpha was $decalEdgeAlpha", decalEdgeAlpha > 0)

        val clamp = renderFullBleedWhite(TileMode.Clamp)
        val clampEdgeAlpha =
            android.graphics.Color.alpha(clamp.getPixel(PIXEL_TEST_SIZE / 2, PIXEL_TEST_SIZE - 1))
        assertTrue(
            "clamp must renormalize and keep the bottom edge opaque, alpha was $clampEdgeAlpha",
            clampEdgeAlpha >= 254,
        )
    }

    private val pixelTestSize = Size(PIXEL_TEST_SIZE.toFloat(), PIXEL_TEST_SIZE.toFloat())

    /** Renders split content (left black, right white) with [radius] applied; null = no effect. */
    private fun renderSplit(radius: BlurRadiusSpec?): Bitmap {
        val effect = radius?.createRenderEffect(pixelTestSize, Density(1f))
        return renderWithEffect(effect) { canvas ->
            val paint = Paint()
            paint.color = android.graphics.Color.BLACK
            canvas.drawRect(0f, 0f, PIXEL_TEST_SIZE / 2f, PIXEL_TEST_SIZE.toFloat(), paint)
            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(
                PIXEL_TEST_SIZE / 2f,
                0f,
                PIXEL_TEST_SIZE.toFloat(),
                PIXEL_TEST_SIZE.toFloat(),
                paint,
            )
        }
    }

    /** Hardware-renders [drawContent] into a [PIXEL_TEST_SIZE] square with [effect] applied. */
    private fun renderWithEffect(effect: RenderEffect?, drawContent: (Canvas) -> Unit): Bitmap {
        val renderNode =
            RenderNode("progressiveBlurContent").apply {
                setRenderEffect(effect?.asAndroidRenderEffect())
                setPosition(0, 0, PIXEL_TEST_SIZE, PIXEL_TEST_SIZE)
            }
        drawContent(renderNode.beginRecording())
        renderNode.endRecording()
        val picture = RenderNodePicture(renderNode)
        val hardwareBitmap = Bitmap.createBitmap(picture)
        val bitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
        hardwareBitmap.recycle()
        return bitmap
    }

    private class RenderNodePicture(val renderNode: RenderNode) : android.graphics.Picture() {
        override fun beginRecording(width: Int, height: Int): Canvas = Canvas()

        override fun endRecording() {}

        override fun getWidth(): Int = renderNode.width

        override fun getHeight(): Int = renderNode.height

        override fun requiresHardwareAcceleration(): Boolean = true

        override fun draw(canvas: Canvas) {
            canvas.drawRenderNode(renderNode)
        }
    }

    /** Pixels adjacent to the split boundary are exactly the source colors. */
    private fun Bitmap.assertSharpBoundaryAt(y: Int) {
        val boundary = PIXEL_TEST_SIZE / 2
        assertEquals(
            "expected exact black just left of the boundary at y=$y",
            android.graphics.Color.BLACK,
            getPixel(boundary - 1, y),
        )
        assertEquals(
            "expected exact white just right of the boundary at y=$y",
            android.graphics.Color.WHITE,
            getPixel(boundary, y),
        )
    }

    /** Boundary pixels are an intermediate black/white mix (tolerance based). */
    private fun Bitmap.assertBlurredBoundaryAt(y: Int) {
        val boundary = PIXEL_TEST_SIZE / 2
        for (x in intArrayOf(boundary - 1, boundary)) {
            val red = android.graphics.Color.red(getPixel(x, y))
            assertTrue("expected a blur mix at ($x, $y) but channel was $red", red in 6..249)
        }
    }

    companion object {
        /** Side length of the pixel-test surface; rendered at Density(1f) so dp == px. */
        private const val PIXEL_TEST_SIZE = 100
    }
}
