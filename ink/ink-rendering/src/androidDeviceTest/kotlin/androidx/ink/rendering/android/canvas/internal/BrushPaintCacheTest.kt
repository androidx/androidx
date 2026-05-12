/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.rendering.android.canvas.internal

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Matrix
import android.graphics.Paint
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushPaint.TextureLayer
import androidx.ink.brush.BrushPaint.TilingTexture
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.brush.color.Color as ComposeColor
import androidx.ink.strokes.StrokeInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BrushPaintCacheTest {

    private fun nestedArrayToMatrix(values: Array<Array<Float>>) =
        Matrix().apply { setValues(values.flatten().toFloatArray()) }

    @Test
    fun obtain_withDefaultFlags() {
        val cache = BrushPaintCache(TextureBitmapStore { null })
        val paint =
            cache.obtain(
                BrushPaint(),
                ComposeColor.Red,
                brushSize = 1f,
                StrokeInput(),
                StrokeInput(),
            )
        // Though note that Paint.FILTER_BITMAP_FLAG is set anyways on some configurations before
        // Android Q and all starting at Android Q. Note that in addition to FILTER_BITMAP_FLAG,
        // Paint(), but not Paint(flags), also sets ANTI_ALIAS_FLAG implicitly starting at Android
        // S.
        assertFalse(paint.isAntiAlias)
        assertTrue(paint.isFilterBitmap)
        // Flag bitvs are the same, ignoring the flags that may be set by the boolean settings above
        // instead.
        assertEquals(
            paint.flags or Paint.FILTER_BITMAP_FLAG,
            Paint(0).flags or Paint.FILTER_BITMAP_FLAG,
        )
    }

    @Test
    fun obtain_withSpecifiedFlags() {
        val cache =
            BrushPaintCache(
                TextureBitmapStore { null },
                additionalPaintFlags = Paint.ANTI_ALIAS_FLAG,
            )
        val paint =
            cache.obtain(
                BrushPaint(),
                ComposeColor.Red,
                brushSize = 1f,
                StrokeInput(),
                StrokeInput(),
                Matrix(),
            )
        // Though note that Paint.FILTER_BITMAP_FLAG is set anyways on some configurations before
        // Android Q and all starting at Android Q. Note that in addition to FILTER_BITMAP_FLAG,
        // Paint(), but not Paint(flags), also sets ANTI_ALIAS_FLAG implicitly starting at Android
        // S.
        assertTrue(paint.isAntiAlias)
        assertTrue(paint.isFilterBitmap)
        // Flag bitvs are the same, ignoring the flags that may be set by the boolean settings above
        // instead.
        assertEquals(
            paint.flags or Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG,
            Paint(0).flags or Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG,
        )
    }

    @Test
    fun obtain_positionOnlyWithTexture() {
        var textureIdLoaded: String? = null
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    textureIdLoaded = it
                    createBitmap(10, 20, Bitmap.Config.ARGB_8888)
                }
            )
        val fakeTextureId = "test-texture-one"
        val brushPaint =
            BrushPaint(
                listOf(
                    TilingTexture(
                        fakeTextureId,
                        sizeX = 30F,
                        sizeY = 40F,
                        origin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    )
                )
            )
        val brushSize = 10f

        val paint =
            cache.obtain(
                brushPaint,
                ComposeColor.Red,
                brushSize,
                StrokeInput().apply { update(x = 5f, y = 7f, elapsedTimeMillis = 0L) },
                StrokeInput(),
            )

        assertEquals(textureIdLoaded, fakeTextureId)
        assertEquals(paint.color, Color.RED)
        assertIs<BitmapShader>(paint.shader)
        val expectedLocalMatrix =
            nestedArrayToMatrix(
                arrayOf(arrayOf(3F, 0F, 5F), arrayOf(0F, 2F, 7F), arrayOf(0F, 0F, 1.0F))
            )
        with(Matrix()) {
            assertTrue(paint.shader.getLocalMatrix(this))
            assertEquals(this, expectedLocalMatrix)
        }

        val expectedUpdatedMatrix =
            nestedArrayToMatrix(
                arrayOf(arrayOf(3F, 0F, 0F), arrayOf(0F, 2F, 0F), arrayOf(0F, 0F, 1.0F))
            )
        assertSame(
            cache.obtain(brushPaint, ComposeColor.Red, brushSize, StrokeInput(), StrokeInput()),
            paint,
        )
        with(Matrix()) {
            assertTrue(paint.shader.getLocalMatrix(this))
            assertNotEquals(expectedUpdatedMatrix, expectedLocalMatrix)
            assertEquals(this, expectedUpdatedMatrix)
        }

        assertSame(
            cache.obtain(brushPaint, ComposeColor.Blue, brushSize, StrokeInput(), StrokeInput()),
            paint,
        )
        assertEquals(paint.color, Color.BLUE)
    }

    @Test
    fun obtain_withStrokeToGraphicsObjectTransform_shouldHaveCorrectLocalMatrix() {
        var textureIdLoaded: String? = null
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    textureIdLoaded = it
                    createBitmap(10, 20, Bitmap.Config.ARGB_8888)
                }
            )
        val fakeTextureId = "test-texture-one"
        val brushPaint =
            BrushPaint(
                listOf(
                    TilingTexture(
                        fakeTextureId,
                        sizeX = 30F,
                        sizeY = 40F,
                        origin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    )
                )
            )
        val brushSize = 10f

        val paint =
            cache.obtain(
                brushPaint,
                ComposeColor.Red,
                brushSize,
                StrokeInput(),
                StrokeInput(),
                Matrix().apply { setScale(5F, 5F) },
            )

        assertEquals(textureIdLoaded, fakeTextureId)
        assertEquals(paint.color, Color.RED)
        assertIs<BitmapShader>(paint.shader)
        val expectedLocalMatrix =
            nestedArrayToMatrix(
                arrayOf(arrayOf(15F, 0F, 0F), arrayOf(0F, 10F, 0F), arrayOf(0F, 0F, 1F))
            )
        with(Matrix()) {
            assertTrue(paint.shader.getLocalMatrix(this))
            assertEquals(this, expectedLocalMatrix)
        }

        val expectedUpdatedMatrix =
            nestedArrayToMatrix(
                arrayOf(arrayOf(21F, 0F, 11F), arrayOf(0F, 14F, 13F), arrayOf(0F, 0F, 1F))
            )
        assertSame(
            cache.obtain(
                brushPaint,
                ComposeColor.Red,
                brushSize,
                StrokeInput(),
                StrokeInput(),
                Matrix().apply {
                    postScale(7F, 7F)
                    postTranslate(11F, 13F)
                },
            ),
            paint,
        )
        with(Matrix()) {
            assertTrue(paint.shader.getLocalMatrix(this))
            assertNotEquals(expectedUpdatedMatrix, expectedLocalMatrix)
            assertEquals(this, expectedUpdatedMatrix)
        }

        assertSame(
            cache.obtain(brushPaint, ComposeColor.Blue, brushSize, StrokeInput(), StrokeInput()),
            paint,
        )
        assertEquals(paint.color, Color.BLUE)
    }

    @Test
    fun obtain_forBrushPaintWithSizeUnitBrushSize() {
        val cache =
            BrushPaintCache(TextureBitmapStore { createBitmap(1, 1, Bitmap.Config.ARGB_8888) })
        val textureId = "test-texture-one"
        val brushPaint =
            BrushPaint(
                listOf(
                    TilingTexture(
                        textureId,
                        sizeX = 2f,
                        sizeY = 3f,
                        sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    )
                )
            )

        val paint =
            cache.obtain(
                brushPaint,
                ComposeColor.Red,
                brushSize = 10f,
                StrokeInput(),
                StrokeInput(),
            )

        val expectedLocalMatrix =
            nestedArrayToMatrix(
                arrayOf(arrayOf(20F, 0F, 0F), arrayOf(0F, 30F, 0F), arrayOf(0F, 0F, 1F))
            )
        with(Matrix()) {
            assertTrue(paint.shader.getLocalMatrix(this))
            assertEquals(this, expectedLocalMatrix)
        }

        val expectedUpdatedMatrix =
            nestedArrayToMatrix(
                arrayOf(arrayOf(40F, 0F, 0F), arrayOf(0F, 60F, 0F), arrayOf(0F, 0F, 1F))
            )
        assertSame(
            cache.obtain(
                brushPaint,
                ComposeColor.Red,
                brushSize = 20f,
                StrokeInput(),
                StrokeInput(),
            ),
            paint,
        )
        with(Matrix()) {
            assertTrue(paint.shader.getLocalMatrix(this))
            assertNotEquals(expectedUpdatedMatrix, expectedLocalMatrix)
            assertEquals(this, expectedUpdatedMatrix)
        }
    }

    @Test
    fun obtain_multipleTextureLayers() {
        val textureIdsLoaded: MutableList<String> = mutableListOf()
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    textureIdsLoaded.add(it)
                    createBitmap(/* width= */ 10, /* height= */ 20, Bitmap.Config.ARGB_8888)
                }
            )
        val fakeTextureId1 = "test-texture-one"
        val fakeTextureId2 = "test-texture-two"
        val brushPaint =
            BrushPaint(
                listOf(
                    TilingTexture(fakeTextureId1, sizeX = 30F, sizeY = 40F),
                    TilingTexture(fakeTextureId2, sizeX = 30F, sizeY = 40F),
                )
            )

        val paint =
            cache.obtain(brushPaint, ComposeColor.Red, brushSize = 1f, StrokeInput(), StrokeInput())

        assertEquals(textureIdsLoaded, mutableListOf(fakeTextureId1, fakeTextureId2))
        assertEquals(paint.color, Color.RED)
        assertIs<ComposeShader>(paint.shader)
        // Can't really assert in more detail because ComposeShader's fields are not readable.
    }

    @Test
    fun obtain_textureLayersThatDoNotLoadAreIgnored() {
        val textureIdsLoaded: MutableList<String> = mutableListOf()
        val fakeUnmappedTextureId1 = "unmapped-texture-one"
        val fakeWorkingTextureId = "test-texture-one"
        val fakeUnmappedTextureId2 = "unmapped-texture-two"
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    textureIdsLoaded.add(it)
                    if (it == fakeWorkingTextureId) {
                        createBitmap(/* width= */ 10, /* height= */ 20, Bitmap.Config.ARGB_8888)
                    } else {
                        null
                    }
                }
            )
        val brushPaint =
            BrushPaint(
                listOf(
                    TilingTexture(fakeUnmappedTextureId1, sizeX = 30F, sizeY = 40F),
                    TilingTexture(fakeWorkingTextureId, sizeX = 30F, sizeY = 40F),
                    TilingTexture(fakeUnmappedTextureId2, sizeX = 30F, sizeY = 40F),
                )
            )

        val paint =
            cache.obtain(brushPaint, ComposeColor.Red, brushSize = 1f, StrokeInput(), StrokeInput())

        assertEquals(
            textureIdsLoaded,
            mutableListOf(fakeUnmappedTextureId1, fakeWorkingTextureId, fakeUnmappedTextureId2),
        )
        assertEquals(paint.color, Color.RED)
        assertIs<BitmapShader>(paint.shader)
    }

    @Test
    fun obtain_textureLoadingDisabled() {
        var textureIdLoaded: String? = null
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    textureIdLoaded = it
                    null
                }
            )
        val fakeTextureId = "test-texture-one"
        val brushPaint = BrushPaint(listOf(TilingTexture(fakeTextureId, sizeX = 30F, sizeY = 40F)))
        val brushSize = 5f

        val paint =
            cache.obtain(brushPaint, ComposeColor.Red, brushSize, StrokeInput(), StrokeInput())

        assertEquals(textureIdLoaded, fakeTextureId)
        assertEquals(paint.color, Color.RED)
        assertNull(paint.shader)

        assertSame(
            cache.obtain(brushPaint, ComposeColor.Blue, brushSize, StrokeInput(), StrokeInput()),
            paint,
        )
        assertEquals(paint.color, Color.BLUE)
    }

    @Test
    fun obtain_textureLoadingDisabledMultipleLayers() {
        val textureIdsLoaded: MutableList<String> = mutableListOf()
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    textureIdsLoaded.add(it)
                    null
                }
            )
        val textureLayerWidth = 30F
        val textureLayerHeight = 40F
        val fakeTextureId1 = "test-one"
        val fakeTextureId2 = "test-two"
        val brushPaint =
            BrushPaint(
                listOf(
                    TilingTexture(fakeTextureId1, textureLayerWidth, textureLayerHeight),
                    TilingTexture(fakeTextureId2, textureLayerWidth, textureLayerHeight),
                )
            )

        val paint =
            cache.obtain(brushPaint, ComposeColor.Red, brushSize = 1f, StrokeInput(), StrokeInput())

        assertEquals(textureIdsLoaded, mutableListOf(fakeTextureId1, fakeTextureId2))
        assertEquals(paint.color, Color.RED)
        assertNull(paint.shader)
    }

    @Test
    fun obtain_noTexture() {
        val cache = BrushPaintCache(TextureBitmapStore { null })
        val brushSize = 15f

        val paint =
            cache.obtain(BrushPaint(), ComposeColor.Red, brushSize, StrokeInput(), StrokeInput())

        assertEquals(paint.color, Color.RED)
        assertNull(paint.shader)

        // BrushPaint() is a different instance, but is equal.
        assertSame(
            cache.obtain(BrushPaint(), ComposeColor.Blue, brushSize, StrokeInput(), StrokeInput()),
            paint,
        )
        assertEquals(paint.color, Color.BLUE)
    }

    @Test
    fun obtain_identityLocalMatrix() {
        var textureIdLoaded: String? = null
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    textureIdLoaded = it
                    createBitmap(10, 20, Bitmap.Config.ARGB_8888)
                }
            )
        val fakeTextureId = "test-texture-one"
        val brushPaint =
            BrushPaint(
                // Same size as the Bitmap.
                listOf(TilingTexture(fakeTextureId, sizeX = 10F, sizeY = 20F))
            )

        val paint =
            cache.obtain(brushPaint, ComposeColor.Red, brushSize = 1f, StrokeInput(), StrokeInput())

        assertEquals(textureIdLoaded, fakeTextureId)
        assertEquals(paint.color, Color.RED)
        assertIs<BitmapShader>(paint.shader)
        Matrix().let {
            // Set the matrix to garbage data to make sure it gets overwritten.
            it.preScale(55555F, 7777777F)

            // getLocalMatrix indicates identity either by returning false or overwriting the result
            // to
            // the identity, but it has slightly different behavior on different API versions. The
            // code
            // under test doesn't use getLocalMatrix, we're just confirming that our call to
            // setLocalMatrix matches what we expect.
            val result = paint.shader.getLocalMatrix(it)
            // Don't check it.isIdentity, that seems to be incorrect on earlier API levels.
            assertTrue(!result || it == Matrix())
        }
    }
}
