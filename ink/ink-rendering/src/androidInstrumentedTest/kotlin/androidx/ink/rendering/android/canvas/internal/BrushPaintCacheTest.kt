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
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.rendering.android.TextureBitmapStore
import androidx.ink.strokes.StrokeInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class BrushPaintCacheTest {

    private fun nestedArrayToMatrix(values: Array<Array<Float>>) =
        Matrix().apply { setValues(values.flatten().toFloatArray()) }

    @Test
    fun obtain_positionOnlyWithTexture() {
        var uriLoaded: String? = null
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    uriLoaded = it
                    createBitmap(10, 20, Bitmap.Config.ARGB_8888)
                }
            )
        val fakeTextureUri = "ink://ink/texture:test-texture-one"
        val brushPaint =
            BrushPaint(listOf(BrushPaint.TextureLayer(fakeTextureUri, sizeX = 30F, sizeY = 40F)))
        val brushSize = 10f
        val internalToStrokeTransform = Matrix().apply { preTranslate(50F, 60F) }

        val paint =
            cache.obtain(
                brushPaint,
                Color.RED,
                brushSize,
                StrokeInput(),
                StrokeInput(),
                internalToStrokeTransform,
            )

        assertThat(uriLoaded).isEqualTo(fakeTextureUri)
        assertThat(paint.color).isEqualTo(Color.RED)
        assertThat(paint.shader).isInstanceOf(BitmapShader::class.java)
        val expectedLocalMatrix =
            nestedArrayToMatrix(
                arrayOf(arrayOf(3F, 0F, -50F), arrayOf(0F, 2F, -60F), arrayOf(0F, 0F, 1.0F))
            )
        with(Matrix()) {
            assertThat(paint.shader.getLocalMatrix(this)).isTrue()
            assertThat(this).isEqualTo(expectedLocalMatrix)
        }

        val newInternalToStrokeTransform = Matrix().apply { preTranslate(-50F, -60F) }
        val expectedUpdatedMatrix =
            nestedArrayToMatrix(
                arrayOf(arrayOf(3F, 0F, 50F), arrayOf(0F, 2F, 60F), arrayOf(0F, 0F, 1.0F))
            )
        assertThat(
                cache.obtain(
                    brushPaint,
                    Color.RED,
                    brushSize,
                    StrokeInput(),
                    StrokeInput(),
                    newInternalToStrokeTransform,
                )
            )
            .isSameInstanceAs(paint)
        with(Matrix()) {
            assertThat(paint.shader.getLocalMatrix(this)).isTrue()
            assertThat(expectedUpdatedMatrix).isNotEqualTo(expectedLocalMatrix)
            assertThat(this).isEqualTo(expectedUpdatedMatrix)
        }

        assertThat(
                cache.obtain(
                    brushPaint,
                    Color.BLUE,
                    brushSize,
                    StrokeInput(),
                    StrokeInput(),
                    newInternalToStrokeTransform,
                )
            )
            .isSameInstanceAs(paint)
        assertThat(paint.color).isEqualTo(Color.BLUE)
    }

    @Test
    fun obtain_forBrushPaintWithSizeUnitBrushSize() {
        val cache =
            BrushPaintCache(TextureBitmapStore { createBitmap(1, 1, Bitmap.Config.ARGB_8888) })
        val textureUri = "ink://ink/texture:test-texture-one"
        val brushPaint =
            BrushPaint(
                listOf(
                    BrushPaint.TextureLayer(
                        textureUri,
                        sizeX = 2f,
                        sizeY = 3f,
                        sizeUnit = BrushPaint.TextureSizeUnit.BRUSH_SIZE,
                    )
                )
            )
        val internalToStrokeTransform = Matrix().apply { preTranslate(7f, 5f) }

        val paint =
            cache.obtain(
                brushPaint,
                Color.RED,
                brushSize = 10f,
                StrokeInput(),
                StrokeInput(),
                internalToStrokeTransform,
            )

        val expectedLocalMatrix =
            nestedArrayToMatrix(
                arrayOf(arrayOf(20F, 0F, -7F), arrayOf(0F, 30F, -5F), arrayOf(0F, 0F, 1F))
            )
        with(Matrix()) {
            assertThat(paint.shader.getLocalMatrix(this)).isTrue()
            assertThat(this).isEqualTo(expectedLocalMatrix)
        }

        val expectedUpdatedMatrix =
            nestedArrayToMatrix(
                arrayOf(arrayOf(40F, 0F, -7F), arrayOf(0F, 60F, -5F), arrayOf(0F, 0F, 1F))
            )
        assertThat(
                cache.obtain(
                    brushPaint,
                    Color.RED,
                    brushSize = 20f,
                    StrokeInput(),
                    StrokeInput(),
                    internalToStrokeTransform,
                )
            )
            .isSameInstanceAs(paint)
        with(Matrix()) {
            assertThat(paint.shader.getLocalMatrix(this)).isTrue()
            assertThat(expectedUpdatedMatrix).isNotEqualTo(expectedLocalMatrix)
            assertThat(this).isEqualTo(expectedUpdatedMatrix)
        }
    }

    @Test
    fun obtain_multipleTextureLayers() {
        val urisLoaded: MutableList<String> = mutableListOf()
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    urisLoaded.add(it)
                    createBitmap(/* width= */ 10, /* height= */ 20, Bitmap.Config.ARGB_8888)
                }
            )
        val fakeTextureUri1 = "ink://ink/texture:test-texture-one"
        val fakeTextureUri2 = "ink://ink/texture:test-texture-two"
        val brushPaint =
            BrushPaint(
                listOf(
                    BrushPaint.TextureLayer(fakeTextureUri1, sizeX = 30F, sizeY = 40F),
                    BrushPaint.TextureLayer(fakeTextureUri2, sizeX = 30F, sizeY = 40F),
                )
            )

        val paint =
            cache.obtain(
                brushPaint,
                Color.RED,
                brushSize = 1f,
                StrokeInput(),
                StrokeInput(),
                Matrix()
            )

        assertThat(urisLoaded).containsExactly(fakeTextureUri1, fakeTextureUri2).inOrder()
        assertThat(paint.color).isEqualTo(Color.RED)
        assertThat(paint.shader).isInstanceOf(ComposeShader::class.java)
        // Can't really assert in more detail because ComposeShader's fields are not readable.
    }

    @Test
    fun obtain_textureLayersThatDoNotLoadAreIgnored() {
        val urisLoaded: MutableList<String> = mutableListOf()
        val fakeBrokenTextureUri1 = "//fake/texture:broken:1"
        val fakeWorkingTextureUri = "ink://ink/texture:test-texture-one"
        val fakeBrokenTextureUri2 = "//fake/texture:broken:2"
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    urisLoaded.add(it)
                    if (it == fakeWorkingTextureUri) {
                        createBitmap(/* width= */ 10, /* height= */ 20, Bitmap.Config.ARGB_8888)
                    } else {
                        null
                    }
                }
            )
        val brushPaint =
            BrushPaint(
                listOf(
                    BrushPaint.TextureLayer(fakeBrokenTextureUri1, sizeX = 30F, sizeY = 40F),
                    BrushPaint.TextureLayer(fakeWorkingTextureUri, sizeX = 30F, sizeY = 40F),
                    BrushPaint.TextureLayer(fakeBrokenTextureUri2, sizeX = 30F, sizeY = 40F),
                )
            )

        val paint =
            cache.obtain(brushPaint, Color.RED, brushSize = 1f, StrokeInput(), StrokeInput())

        assertThat(urisLoaded)
            .containsExactly(fakeBrokenTextureUri1, fakeWorkingTextureUri, fakeBrokenTextureUri2)
            .inOrder()
        assertThat(paint.color).isEqualTo(Color.RED)
        assertThat(paint.shader).isInstanceOf(BitmapShader::class.java)
    }

    @Test
    fun obtain_textureLoadingDisabled() {
        var uriLoaded: String? = null
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    uriLoaded = it
                    null
                }
            )
        val fakeTextureUri = "ink://ink/texture:test-texture-one"
        val brushPaint =
            BrushPaint(listOf(BrushPaint.TextureLayer(fakeTextureUri, sizeX = 30F, sizeY = 40F)))
        val brushSize = 5f
        val internalToStrokeTransform = Matrix().apply { preTranslate(50F, 60F) }

        val paint =
            cache.obtain(
                brushPaint,
                Color.RED,
                brushSize,
                StrokeInput(),
                StrokeInput(),
                internalToStrokeTransform,
            )

        assertThat(uriLoaded).isEqualTo(fakeTextureUri)
        assertThat(paint.color).isEqualTo(Color.RED)
        assertThat(paint.shader).isNull()

        assertThat(
                cache.obtain(
                    brushPaint,
                    Color.BLUE,
                    brushSize,
                    StrokeInput(),
                    StrokeInput(),
                    internalToStrokeTransform,
                )
            )
            .isSameInstanceAs(paint)
        assertThat(paint.color).isEqualTo(Color.BLUE)
    }

    @Test
    fun obtain_textureLoadingDisabledMultipleLayers() {
        val urisLoaded: MutableList<String> = mutableListOf()
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    urisLoaded.add(it)
                    null
                }
            )
        val textureLayerWidth = 30F
        val textureLayerHeight = 40F
        val fakeTextureUri1 = "ink://ink/texture:test-one"
        val fakeTextureUri2 = "ink://ink/texture:test-two"
        val brushPaint =
            BrushPaint(
                listOf(
                    BrushPaint.TextureLayer(fakeTextureUri1, textureLayerWidth, textureLayerHeight),
                    BrushPaint.TextureLayer(fakeTextureUri2, textureLayerWidth, textureLayerHeight),
                )
            )
        val internalToStrokeTransform =
            Matrix().apply { preTranslate(/* dx= */ 50F, /* dy= */ 60F) }

        val paint =
            cache.obtain(
                brushPaint,
                Color.RED,
                brushSize = 1f,
                StrokeInput(),
                StrokeInput(),
                internalToStrokeTransform,
            )

        assertThat(urisLoaded).containsExactly(fakeTextureUri1, fakeTextureUri2).inOrder()
        assertThat(paint.color).isEqualTo(Color.RED)
        assertThat(paint.shader).isNull()
    }

    @Test
    fun obtain_noTexture() {
        val cache = BrushPaintCache(TextureBitmapStore { null })
        val brushSize = 15f
        val internalToStrokeTransform = Matrix().apply { preTranslate(50F, 60F) }

        val paint =
            cache.obtain(
                BrushPaint(),
                Color.RED,
                brushSize,
                StrokeInput(),
                StrokeInput(),
                internalToStrokeTransform,
            )

        assertThat(paint.color).isEqualTo(Color.RED)
        assertThat(paint.shader).isNull()

        // BrushPaint() is a different instance, but is equal.
        assertThat(
                cache.obtain(
                    BrushPaint(),
                    Color.BLUE,
                    brushSize,
                    StrokeInput(),
                    StrokeInput(),
                    internalToStrokeTransform,
                )
            )
            .isSameInstanceAs(paint)
        assertThat(paint.color).isEqualTo(Color.BLUE)
    }

    @Test
    fun obtain_defaultInternalToStrokeTransform() {
        var uriLoaded: String? = null
        val cache =
            BrushPaintCache(
                TextureBitmapStore {
                    uriLoaded = it
                    createBitmap(10, 20, Bitmap.Config.ARGB_8888)
                }
            )
        val fakeTextureUri = "ink://ink/texture:test-texture-one"
        val brushPaint =
            BrushPaint(
                // Same size as the Bitmap.
                listOf(BrushPaint.TextureLayer(fakeTextureUri, sizeX = 10F, sizeY = 20F))
            )

        val paint =
            cache.obtain(brushPaint, Color.RED, brushSize = 1f, StrokeInput(), StrokeInput())

        assertThat(uriLoaded).isEqualTo(fakeTextureUri)
        assertThat(paint.color).isEqualTo(Color.RED)
        assertThat(paint.shader).isInstanceOf(BitmapShader::class.java)
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
            assertThat(!result || it == Matrix()).isTrue()
        }
    }
}
