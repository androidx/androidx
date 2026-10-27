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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.DrawPath
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.matrix.MatrixConstant
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
class RcPlayerPaintTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun defaultColorIsOpaqueBlackAndIsColorSetIsFalse() {
        val document = buildDocument { drawRect(0f, 0f, 100f, 100f) }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(100.dp).background(Color.White)) {
                    RcPlayer(document = document)
                }
            }
        }
        val bitmap = rule.captureRootBitmap()
        val pixel = bitmap.getPixel(50, 50)
        assertThat(AndroidColor.red(pixel)).isEqualTo(0)
        assertThat(AndroidColor.green(pixel)).isEqualTo(0)
        assertThat(AndroidColor.blue(pixel)).isEqualTo(0)
        assertThat(AndroidColor.alpha(pixel)).isEqualTo(255)
    }

    @Test
    fun effectiveColorAppliesAlphaToDefaultBlack() {
        val document = buildDocument {
            writer.buffer.addPaint(PaintBundle().apply { setAlpha(0.5f) })
            drawRect(0f, 0f, 100f, 100f)
        }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(100.dp).background(Color.White)) {
                    RcPlayer(document = document)
                }
            }
        }
        val bitmap = rule.captureRootBitmap()
        val pixel = bitmap.getPixel(50, 50)
        // 50% black composited over white yields mid-gray.
        assertThat(AndroidColor.red(pixel)).isIn(110..145)
        assertThat(AndroidColor.green(pixel)).isIn(110..145)
        assertThat(AndroidColor.blue(pixel)).isIn(110..145)
    }

    @Test
    fun updatePaintWithExplicitColorSetsIsColorSetTrue() {
        val bitmap = rule.renderDocument {
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFF0000.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
        }
        assertRed(bitmap, 50, 50)
    }

    @Test
    fun updatePaintResolvesNanBoxedStrokeWidth() {
        val document = buildDocument {
            val strokeWidthVar = addNamedFloat("strokeWidth", 20f)
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setColor(0xFFFF0000.toInt())
                    setStyle(PaintBundle.STYLE_STROKE)
                    setStrokeWidth(strokeWidthVar)
                }
            )
            drawLine(10f, 50f, 90f, 50f)
        }
        val state = RcPlayerState(document)
        val bitmap1 = renderWithState(state)
        assertRed(bitmap1, 50, 42)
        assertNotRed(bitmap1, 50, 30)

        requireNotNull(state.floatState("strokeWidth")).value = 4f
        rule.waitForIdle()
        val bitmap2 = rule.captureRootBitmap()
        assertRed(bitmap2, 50, 50)
        assertNotRed(bitmap2, 50, 42)
    }

    @Test
    fun updatePaintResolvesNanBoxedTextSize() {
        val sample = "MMMM"
        val document = buildDocument {
            val textSizeVar = addNamedFloat("textSize", 10f)
            val textId = addText(sample)
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setColor(0xFFFF0000.toInt())
                    setTextSize(textSizeVar)
                }
            )
            drawTextRun(textId, 0, sample.length, 0, sample.length, 5f, 80f, false)
        }
        val state = RcPlayerState(document)
        val smallCount = countRedPixels(renderWithState(state))

        requireNotNull(state.floatState("textSize")).value = 44f
        rule.waitForIdle()
        val largeCount = countRedPixels(rule.captureRootBitmap())

        assertThat(smallCount).isGreaterThan(0)
        assertThat(largeCount).isGreaterThan(smallCount * 4)
    }

    @Test
    fun updatePaintResolvesNanBoxedAlpha() {
        val document = buildDocument {
            val alphaVar = addNamedFloat("alpha", 0.5f)
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setColor(0xFFFF0000.toInt())
                    setAlpha(alphaVar)
                }
            )
            drawRect(0f, 0f, 100f, 100f)
        }
        val state = RcPlayerState(document)
        val halfPixel = renderWithState(state).getPixel(50, 50)
        assertThat(AndroidColor.red(halfPixel)).isGreaterThan(240)
        assertThat(AndroidColor.green(halfPixel)).isIn(100..155)
        assertThat(AndroidColor.blue(halfPixel)).isIn(100..155)

        requireNotNull(state.floatState("alpha")).value = 1f
        rule.waitForIdle()
        assertRed(rule.captureRootBitmap(), 50, 50)
    }

    @Test
    fun updatePaintResolvesNanBoxedGradientStops() {
        val document = buildDocument {
            val stop1 = addNamedFloat("stop1", 0.45f)
            val stop2 = addNamedFloat("stop2", 0.55f)
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setLinearGradient(
                        intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
                        0,
                        floatArrayOf(stop1, stop2),
                        0f,
                        0f,
                        100f,
                        0f,
                        0,
                    )
                }
            )
            drawRect(0f, 0f, 100f, 100f)
        }
        val state = RcPlayerState(document)
        val bitmap1 = renderWithState(state)
        assertThat(AndroidColor.red(bitmap1.getPixel(20, 50))).isLessThan(20)
        assertThat(AndroidColor.red(bitmap1.getPixel(65, 50))).isGreaterThan(235)

        requireNotNull(state.floatState("stop1")).value = 0.8f
        requireNotNull(state.floatState("stop2")).value = 0.9f
        rule.waitForIdle()
        val bitmap2 = rule.captureRootBitmap()
        assertThat(AndroidColor.red(bitmap2.getPixel(65, 50))).isLessThan(20)
    }

    @Test
    fun drawCircleResolvesNanBoxedCoordinates() {
        val document = buildDocument {
            val cx = addNamedFloat("cx", 50f)
            val cy = addNamedFloat("cy", 60f)
            val r = addNamedFloat("r", 20f)
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFF0000.toInt()) })
            drawCircle(cx, cy, r)
        }
        val state = RcPlayerState(document)
        val bitmap1 = renderWithState(state)
        assertRed(bitmap1, 50, 60)
        assertNotRed(bitmap1, 20, 20)

        requireNotNull(state.floatState("cx")).value = 20f
        requireNotNull(state.floatState("cy")).value = 20f
        requireNotNull(state.floatState("r")).value = 15f
        rule.waitForIdle()
        val bitmap2 = rule.captureRootBitmap()
        assertRed(bitmap2, 20, 20)
        assertNotRed(bitmap2, 50, 60)
    }

    @Test
    fun drawOvalResolvesNanBoxedCoordinates() {
        val bitmap = rule.renderDocument {
            val left = addNamedFloat("left", 10f)
            val top = addNamedFloat("top", 20f)
            val right = addNamedFloat("right", 80f)
            val bottom = addNamedFloat("bottom", 90f)
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFF0000.toInt()) })
            drawOval(left, top, right, bottom)
        }

        assertRed(bitmap, 45, 55)
        assertNotRed(bitmap, 90, 95)
    }

    @Test
    fun drawPathDereferencesPointerId() {
        val bitmap = rule.renderDocument {
            addPathString("M 0 0 L 40 0 L 40 40 L 0 40 Z")
            val targetPathId = addPathString("M 60 60 L 100 60 L 100 100 L 60 100 Z")
            val ptrLong = addInteger(targetPathId)
            val ptrVarId = Utils.idFromLong(ptrLong).toInt()
            val dereferencedId =
                (ptrVarId and PaintOperation.VALUE_MASK) or PaintOperation.PTR_DEREFERENCE

            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFF0000.toInt()) })
            DrawPath.apply(writer.buffer.buffer, dereferencedId)
        }

        assertRed(bitmap, 80, 80)
        assertNotRed(bitmap, 20, 20)
    }

    @Test
    fun clipPathDereferencesPointerId() {
        var dereferencedId = 0
        val document = buildDocument {
            addPathString("M 0 0 L 40 0 L 40 40 L 0 40 Z")
            val targetPathId = addPathString("M 60 60 L 100 60 L 100 100 L 60 100 Z")
            val ptrLong = addInteger(targetPathId)
            val ptrVarId = Utils.idFromLong(ptrLong).toInt()
            dereferencedId =
                (ptrVarId and PaintOperation.VALUE_MASK) or PaintOperation.PTR_DEREFERENCE

            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
            save()
            ClipPath.apply(writer.buffer.buffer, 0)
            writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFF0000.toInt()) })
            drawRect(0f, 0f, 100f, 100f)
            restore()
        }
        // ClipPath.read masks wire ids to 20 bits, so set the in-memory PTR_DEREFERENCE id directly
        // on the decoded ClipPath operation to exercise pointer dereferencing in applyClipPath.
        val clipIndex = document.operations.indexOfFirst { it is ClipPath }
        document.operations[clipIndex] = ClipPath(dereferencedId, ClipPath.PATH_CLIP_INTERSECT)
        val bitmap = rule.renderDocument(document)

        assertRed(bitmap, 80, 80)
        assertNotRed(bitmap, 20, 20)
    }

    @Test
    fun updatePaintFromBundle_resolvesGradientBoundColorIdStop() {
        val document = buildDocument {
            val midColorId = addNamedColor("midColor", 0xFF00FF00.toInt())
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setLinearGradient(
                        intArrayOf(0xFF000000.toInt(), midColorId, 0xFF000000.toInt()),
                        1 shl 1, // bit 1 indicates stop 1 is a color-id reference
                        floatArrayOf(0f, 0.5f, 1f),
                        0f,
                        0f,
                        100f,
                        0f,
                        0,
                    )
                }
            )
            drawRect(0f, 0f, 100f, 100f)
        }
        val state = RcPlayerState(document)
        val bitmap1 = renderWithState(state)
        val mid1 = bitmap1.getPixel(50, 50)
        assertThat(AndroidColor.green(mid1)).isGreaterThan(200)
        assertThat(AndroidColor.red(mid1)).isLessThan(50)

        // Live recolor: update midColor from Green to Red
        requireNotNull(state.colorState("midColor")).value = Color.Red
        rule.waitForIdle()
        val bitmap2 = rule.captureRootBitmap()
        assertRed(bitmap2, 50, 50)
    }

    @Test
    fun updatePaintFromBundle_allLiteralGradientStops() {
        val bitmap = rule.renderDocument {
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setLinearGradient(
                        intArrayOf(
                            0xFFFF0000.toInt(),
                            0xFF00FF00.toInt(),
                            0xFF0000FF.toInt(),
                        ),
                        0, // all literal stops
                        floatArrayOf(0f, 0.5f, 1f),
                        0f,
                        0f,
                        100f,
                        0f,
                        0,
                    )
                }
            )
            drawRect(0f, 0f, 100f, 100f)
        }

        assertRed(bitmap, 2, 50)
        val center = bitmap.getPixel(50, 50)
        assertThat(AndroidColor.green(center)).isGreaterThan(200)
        val right = bitmap.getPixel(98, 50)
        assertThat(AndroidColor.blue(right)).isGreaterThan(200)
    }

    @Test
    fun updatePaintFromBundle_radialGradientWithColorIdStop() {
        val document = buildDocument {
            val edgeColorId = addNamedColor("edgeColor", 0xFF00FF00.toInt())
            writer.buffer.addPaint(
                PaintBundle().apply {
                    setRadialGradient(
                        intArrayOf(0xFF0000FF.toInt(), edgeColorId),
                        1 shl 1, // bit 1 is colorId
                        floatArrayOf(0f, 1f),
                        50f,
                        50f,
                        45f,
                        0,
                    )
                }
            )
            drawRect(0f, 0f, 100f, 100f)
        }
        val state = RcPlayerState(document)
        val bitmap1 = renderWithState(state)
        assertThat(AndroidColor.blue(bitmap1.getPixel(50, 50))).isGreaterThan(200)
        assertThat(AndroidColor.green(bitmap1.getPixel(95, 50))).isGreaterThan(200)

        requireNotNull(state.colorState("edgeColor")).value = Color.Red
        rule.waitForIdle()
        assertRed(rule.captureRootBitmap(), 95, 50)
    }

    @Test
    fun updatePaintFromBundle_tracksFilterBitmapAndImageFilterQuality() {
        val srcBitmap =
            Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
                setPixel(0, 0, 0xFF000000.toInt())
                setPixel(0, 1, 0xFF000000.toInt())
                setPixel(1, 0, 0xFFFFFFFF.toInt())
                setPixel(1, 1, 0xFFFFFFFF.toInt())
            }
        val docUnfiltered = buildDocument {
            val imgId = addBitmap(srcBitmap)
            writer.buffer.addPaint(PaintBundle().apply { setFilterBitmap(false) })
            drawScaledBitmap(
                imgId,
                0f,
                0f,
                2f,
                2f,
                0f,
                0f,
                100f,
                100f,
                Rc.ImageScale.FILL_BOUNDS,
                1f,
                null,
            )
        }
        val docFiltered = buildDocument {
            val imgId = addBitmap(srcBitmap)
            writer.buffer.addPaint(PaintBundle().apply { setFilterBitmap(true) })
            drawScaledBitmap(
                imgId,
                0f,
                0f,
                2f,
                2f,
                0f,
                0f,
                100f,
                100f,
                Rc.ImageScale.FILL_BOUNDS,
                1f,
                null,
            )
        }

        var currentDoc by mutableStateOf(docUnfiltered)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = currentDoc) }
            }
        }
        val unfilteredBmp = rule.captureRootBitmap()
        // Nearest-neighbor (FilterQuality.None): sharp black/white boundary at x=50, no gray blend.
        assertThat(AndroidColor.red(unfilteredBmp.getPixel(45, 50))).isEqualTo(0)
        assertThat(AndroidColor.red(unfilteredBmp.getPixel(55, 50))).isEqualTo(255)

        currentDoc = docFiltered
        rule.waitForIdle()
        val filteredBmp = rule.captureRootBitmap()
        // Bilinear filtering (FilterQuality.Low): interpolated gray near the center seam.
        assertThat(AndroidColor.red(filteredBmp.getPixel(45, 50))).isIn(30..225)
    }

    private fun renderWithState(state: RcPlayerState): Bitmap {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = state) }
            }
        }
        return rule.captureRootBitmap()
    }

    private fun countRedPixels(bitmap: Bitmap): Int {
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val p = bitmap.getPixel(x, y)
                if (
                    AndroidColor.red(p) > 200 &&
                        AndroidColor.green(p) < 60 &&
                        AndroidColor.blue(p) < 60
                ) {
                    count++
                }
            }
        }
        return count
    }

    /** A red to blue gradient from x = 0 to 50, painted across the whole player. */
    private fun RemoteComposeContextAndroid.drawRedToBlue(
        tileMode: Int,
        shaderMatrixId: Int? = null,
    ) {
        writer.buffer.addPaint(
            PaintBundle().apply {
                setLinearGradient(
                    intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt()),
                    0,
                    null,
                    0f,
                    0f,
                    50f,
                    0f,
                    tileMode,
                )
                shaderMatrixId?.let { setShaderMatrix(Utils.asNan(it)) }
            }
        )
        drawRect(0f, 0f, 100f, 100f)
    }

    private fun isBlue(pixel: Int) = AndroidColor.blue(pixel) > 200 && AndroidColor.red(pixel) < 60

    @Test
    fun linearGradient_clampTileMode_extendsEndColor() {
        val bitmap = rule.renderDocument { drawRedToBlue(tileMode = TILE_MODE_CLAMP) }
        assertThat(isBlue(bitmap.getPixel(75, 50))).isTrue()
    }

    @Test
    fun linearGradient_constantShaderMatrix_transformsGradient() {
        val bitmap = rule.renderDocument {
            // Translate the gradient 50px right, so it runs from x = 50 to 100.
            val matrixId = 1000
            MatrixConstant.apply(
                writer.buffer.buffer,
                matrixId,
                0,
                floatArrayOf(1f, 0f, 50f, 0f, 1f, 0f, 0f, 0f, 1f),
            )
            drawRedToBlue(tileMode = TILE_MODE_CLAMP, shaderMatrixId = matrixId)
        }
        // Before the translated start, clamp extends the start color.
        assertRed(bitmap, 25, 50)
        assertThat(isBlue(bitmap.getPixel(99, 50))).isTrue()
    }

    private companion object {
        const val TILE_MODE_CLAMP = 0
    }
}
