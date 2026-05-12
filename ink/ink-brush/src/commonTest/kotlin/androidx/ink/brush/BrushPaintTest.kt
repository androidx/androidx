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

package androidx.ink.brush

import androidx.ink.brush.BrushPaint.ColorFunction
import androidx.ink.brush.BrushPaint.StampingTexture
import androidx.ink.brush.BrushPaint.TextureLayer
import androidx.ink.brush.BrushPaint.TilingTexture
import androidx.ink.geometry.Angle
import androidx.ink.nativeloader.testing.awaitNativePointerCleanupAfter
import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BrushPaintTest {

    @Test
    fun brushPaintNativePointers_cleanedUpWhenOutOfScope() {
        awaitNativePointerCleanupAfter {
            val unused =
                BrushPaint(
                    textureLayers = listOf(makeTestTextureLayer()),
                    colorFunctions = listOf(ColorFunction.OpacityMultiplier(0.75f)),
                )
        }
    }

    @Test
    fun brushTip_usesPassedInTextureLayers() {
        val textureLayer = makeTestTextureLayer()
        val brushPaint = BrushPaint(textureLayers = listOf(textureLayer))
        assertThat(brushPaint.textureLayers).hasSize(1)
        assertThat(brushPaint.textureLayers[0]).isSameInstanceAs(textureLayer)
    }

    @Test
    fun brushTip_usesPassedInColorFunctions() {
        val colorFunction = ColorFunction.OpacityMultiplier(0.75f)
        val brushPaint = BrushPaint(colorFunctions = listOf(colorFunction))
        assertThat(brushPaint.colorFunctions).hasSize(1)
        assertThat(brushPaint.colorFunctions[0]).isSameInstanceAs(colorFunction)
    }

    // region BrushPaint class tests
    @Test
    fun constructor_withValidArguments_returnsABrushPaint() {
        assertThat(
                BrushPaint(
                    textureLayers =
                        listOf(
                            TilingTexture(
                                clientTextureId = TEST_TEXTURE_ID,
                                sizeX = 123.45F,
                                sizeY = 678.90F,
                                offsetX = 0.1f,
                                offsetY = 0.2f,
                                rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                                sizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                                origin = TilingTexture.Origin.STROKE_SPACE_ORIGIN,
                                wrapX = TextureLayer.Wrap.MIRROR,
                                wrapY = TextureLayer.Wrap.REPEAT,
                            ),
                            TilingTexture(
                                clientTextureId = TEST_TEXTURE_ID,
                                sizeX = 256F,
                                sizeY = 256F,
                                offsetX = 0.8f,
                                offsetY = 0.9f,
                                rotationDegrees = Angle.HALF_TURN_DEGREES,
                                sizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                                origin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                                wrapX = TextureLayer.Wrap.CLAMP,
                                wrapY = TextureLayer.Wrap.MIRROR,
                            ),
                        ),
                    colorFunctions = listOf(ColorFunction.OpacityMultiplier(0.75f)),
                    selfOverlap = SelfOverlap.DISCARD,
                )
            )
            .isNotNull()
    }

    @Test
    fun constructor_withDefaultArguments_returnsABrushPaint() {
        assertThat(BrushPaint()).isNotNull()
    }

    @Test
    fun hashCode_withIdenticalValues_matches() {
        assertThat(BrushPaint(listOf(makeTestTextureLayer())).hashCode())
            .isEqualTo(BrushPaint(listOf(makeTestTextureLayer())).hashCode())
    }

    @Test
    fun equals_comparesValues() {
        val customPaint = makeTestPaint()
        val defaultPaint = BrushPaint()
        // same values are equal.
        assertThat(customPaint).isEqualTo(makeTestPaint())

        // different values are not equal.
        assertThat(customPaint).isNotNull()
        assertThat(customPaint).isNotEqualTo(Any())
        assertThat(customPaint).isNotEqualTo(defaultPaint)
    }

    @Test
    fun toString_returnsExpectedValues() {
        val string = makeTestPaint().toString()
        assertThat(string).contains("BrushPaint")
        assertThat(string).contains("textureLayers")
        assertThat(string).contains("selfOverlap")
    }

    // endregion

    // region TextureLayer class tests
    @Test
    @Suppress("Range") // Testing error cases.
    fun tilingTextureConstructor_withInvalidSizes_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { TilingTexture(TEST_TEXTURE_ID, -32F, 64F) }
        assertFailsWith<IllegalArgumentException> { TilingTexture(TEST_TEXTURE_ID, 32F, -64F) }
        assertFailsWith<IllegalArgumentException> { TilingTexture(TEST_TEXTURE_ID, -32F, -64F) }
        assertFailsWith<IllegalArgumentException> { TilingTexture(TEST_TEXTURE_ID, 0F, 128F) }
        assertFailsWith<IllegalArgumentException> { TilingTexture(TEST_TEXTURE_ID, 128F, 0F) }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(TEST_TEXTURE_ID, Float.NaN, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(TEST_TEXTURE_ID, 128F, Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(TEST_TEXTURE_ID, Float.POSITIVE_INFINITY, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(TEST_TEXTURE_ID, 128F, Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(TEST_TEXTURE_ID, Float.NEGATIVE_INFINITY, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(TEST_TEXTURE_ID, 128F, Float.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun tilingTextureConstructor_withInvalidOffsetX_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, offsetX = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                offsetX = Float.POSITIVE_INFINITY,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                offsetX = Float.NEGATIVE_INFINITY,
            )
        }
    }

    @Test
    fun tilingTextureConstructor_withInvalidOffsetY_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, offsetY = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                offsetY = Float.POSITIVE_INFINITY,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                offsetY = Float.NEGATIVE_INFINITY,
            )
        }
    }

    @Test
    fun tilingTextureConstructor_withInvalidRotation_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, rotationDegrees = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                rotationDegrees = Float.POSITIVE_INFINITY,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TilingTexture(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                rotationDegrees = Float.NEGATIVE_INFINITY,
            )
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun stampingTextureConstructor_withInvalidAnimationFrames_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationFrames = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationFrames = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationFrames = (1 shl 24) + 1)
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun stampingTextureConstructor_withInvalidAnimationAtlasDimensions_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationRows = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationRows = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationRows = (1 shl 12) + 1)
        }
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationColumns = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationColumns = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationColumns = (1 shl 12) + 1)
        }
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(
                clientTextureId = TEST_TEXTURE_ID,
                animationFrames = 7,
                animationRows = 2,
                animationColumns = 3,
            )
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun stampingTextureConstructor_withInvalidAnimationDuration_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationDurationMillis = -1L)
        }
        assertFailsWith<IllegalArgumentException> {
            StampingTexture(TEST_TEXTURE_ID, animationDurationMillis = 1L shl 25)
        }
    }

    @Test
    fun textureLayerHashCode_withIdenticalValues_matches() {
        assertThat(makeTestTextureLayer().hashCode()).isEqualTo(makeTestTextureLayer().hashCode())
    }

    @Test
    fun tilingTextureEquals_checksEqualityOfValues() {
        val layer =
            TilingTexture(
                clientTextureId = TEST_TEXTURE_ID,
                sizeX = 128F,
                sizeY = 128F,
                offsetX = 0.1f,
                offsetY = 0.2f,
                rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                origin = TilingTexture.Origin.LAST_STROKE_INPUT,
                wrapX = TextureLayer.Wrap.MIRROR,
                wrapY = TextureLayer.Wrap.CLAMP,
                blendMode = TextureLayer.BlendMode.SRC_IN,
            )

        // same values.
        assertThat(layer)
            .isEqualTo(
                TilingTexture(
                    clientTextureId = TEST_TEXTURE_ID,
                    sizeX = 128F,
                    sizeY = 128F,
                    offsetX = 0.1f,
                    offsetY = 0.2f,
                    rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                    sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    origin = TilingTexture.Origin.LAST_STROKE_INPUT,
                    wrapX = TextureLayer.Wrap.MIRROR,
                    wrapY = TextureLayer.Wrap.CLAMP,
                    blendMode = TextureLayer.BlendMode.SRC_IN,
                )
            )

        // different values.
        assertThat(layer).isNotNull()
        assertThat(layer).isNotEqualTo(Any())
        assertThat(layer).isNotEqualTo(layer.copy(clientTextureId = OTHER_TEXTURE_ID))
        assertThat(layer).isNotEqualTo(layer.copy(sizeX = 999F))
        assertThat(layer).isNotEqualTo(layer.copy(sizeY = 999F))
        assertThat(layer).isNotEqualTo(layer.copy(offsetX = 0.999F))
        assertThat(layer).isNotEqualTo(layer.copy(offsetY = 0.999F))
        assertThat(layer).isNotEqualTo(layer.copy(rotationDegrees = Angle.HALF_TURN_DEGREES))
        assertThat(layer)
            .isNotEqualTo(layer.copy(sizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES))
        assertThat(layer).isNotEqualTo(layer.copy(origin = TilingTexture.Origin.FIRST_STROKE_INPUT))
        assertThat(layer).isNotEqualTo(layer.copy(wrapX = TextureLayer.Wrap.REPEAT))
        assertThat(layer).isNotEqualTo(layer.copy(wrapY = TextureLayer.Wrap.MIRROR))
        assertThat(layer).isNotEqualTo(layer.copy(blendMode = TextureLayer.BlendMode.MODULATE))
    }

    @Test
    fun stampingTextureEquals_checksEqualityOfValues() {
        val layer =
            StampingTexture(
                clientTextureId = TEST_TEXTURE_ID,
                animationFrames = 2,
                animationRows = 3,
                animationColumns = 4,
                animationDurationMillis = 5000,
                blendMode = TextureLayer.BlendMode.SRC_IN,
            )

        // same values.
        assertThat(layer)
            .isEqualTo(
                StampingTexture(
                    clientTextureId = TEST_TEXTURE_ID,
                    animationFrames = 2,
                    animationRows = 3,
                    animationColumns = 4,
                    animationDurationMillis = 5000,
                    blendMode = TextureLayer.BlendMode.SRC_IN,
                )
            )

        // different values.
        assertThat(layer).isNotNull()
        assertThat(layer).isNotEqualTo(Any())
        assertThat(layer).isNotEqualTo(layer.copy(clientTextureId = OTHER_TEXTURE_ID))
        assertThat(layer).isNotEqualTo(layer.copy(animationFrames = 5))
        assertThat(layer).isNotEqualTo(layer.copy(animationRows = 6))
        assertThat(layer).isNotEqualTo(layer.copy(animationColumns = 7))
        assertThat(layer).isNotEqualTo(layer.copy(animationDurationMillis = 8000))
        assertThat(layer).isNotEqualTo(layer.copy(blendMode = TextureLayer.BlendMode.MODULATE))
    }

    @Test
    fun textureLayerCopy_createsCopy() {
        val layer = makeTestTextureLayer()
        val copy = layer.copy()

        // Pure copy returns `this`.
        assertThat(copy).isSameInstanceAs(layer)
    }

    @Test
    fun tilingTextureCopy_withArguments_createsCopyWithChanges() {
        val originalLayer =
            TilingTexture(
                clientTextureId = TEST_TEXTURE_ID,
                sizeX = 128F,
                sizeY = 128F,
                offsetX = 0.1f,
                offsetY = 0.2f,
                rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                origin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                wrapX = TextureLayer.Wrap.MIRROR,
                wrapY = TextureLayer.Wrap.CLAMP,
                blendMode = TextureLayer.BlendMode.SRC_IN,
            )
        val changedSizeX = originalLayer.copy(sizeX = 999F)

        // sizeX changed.
        assertThat(changedSizeX).isNotEqualTo(originalLayer)
        assertThat(changedSizeX.sizeX).isNotEqualTo(originalLayer.sizeX)

        assertThat(changedSizeX)
            .isEqualTo(
                TilingTexture(
                    clientTextureId = TEST_TEXTURE_ID,
                    sizeX = 999F, // Changed
                    sizeY = 128F,
                    offsetX = 0.1f,
                    offsetY = 0.2f,
                    rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                    sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    origin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    wrapX = TextureLayer.Wrap.MIRROR,
                    wrapY = TextureLayer.Wrap.CLAMP,
                    blendMode = TextureLayer.BlendMode.SRC_IN,
                )
            )
    }

    @Test
    fun stampingTextureCopy_withArguments_createsCopyWithChanges() {
        val originalLayer =
            StampingTexture(
                clientTextureId = TEST_TEXTURE_ID,
                animationFrames = 2,
                animationRows = 3,
                animationColumns = 4,
                animationDurationMillis = 5000,
                blendMode = TextureLayer.BlendMode.SRC_IN,
            )
        val changedAnimationRows = originalLayer.copy(animationRows = 9)

        // animationRows changed.
        assertThat(changedAnimationRows).isNotEqualTo(originalLayer)
        assertThat(changedAnimationRows.animationRows).isNotEqualTo(originalLayer.animationRows)

        assertThat(changedAnimationRows)
            .isEqualTo(
                StampingTexture(
                    clientTextureId = TEST_TEXTURE_ID,
                    animationFrames = 2,
                    animationRows = 9, // Changed
                    animationColumns = 4,
                    animationDurationMillis = 5000,
                    blendMode = TextureLayer.BlendMode.SRC_IN,
                )
            )
    }

    @Test
    fun textureLayerToString_returnsExpectedValues() {
        val string = makeTestTextureLayer().toString()
        assertThat(string).contains("Texture")
        assertThat(string).contains("clientTextureId")
        assertThat(string).contains("blendMode")
    }

    // endregion

    // region SizeUnit class tests

    @Test
    fun sizeUnitToString_returnsCorrectString() {
        assertThat(TextureLayer.SizeUnit.BRUSH_SIZE.toString())
            .isEqualTo("TextureLayer.SizeUnit.BRUSH_SIZE")
        assertThat(TextureLayer.SizeUnit.STROKE_COORDINATES.toString())
            .isEqualTo("TextureLayer.SizeUnit.STROKE_COORDINATES")
    }

    // endregion

    // region Origin class tests
    @Test
    fun originConstants_areDistinct() {
        val set =
            setOf(
                TilingTexture.Origin.STROKE_SPACE_ORIGIN,
                TilingTexture.Origin.FIRST_STROKE_INPUT,
                TilingTexture.Origin.LAST_STROKE_INPUT,
            )
        assertThat(set).hasSize(3)
    }

    @Test
    fun originHashCode_withIdenticalValues_match() {
        assertThat(TilingTexture.Origin.FIRST_STROKE_INPUT.hashCode())
            .isEqualTo(TilingTexture.Origin.FIRST_STROKE_INPUT.hashCode())
    }

    @Test
    fun originEquals_checksEqualityOfValues() {
        assertThat(TilingTexture.Origin.FIRST_STROKE_INPUT)
            .isEqualTo(TilingTexture.Origin.FIRST_STROKE_INPUT)
        assertThat(TilingTexture.Origin.FIRST_STROKE_INPUT)
            .isNotEqualTo(TilingTexture.Origin.LAST_STROKE_INPUT)
    }

    @Test
    fun originToString_returnsCorrectString() {
        assertThat(TilingTexture.Origin.STROKE_SPACE_ORIGIN.toString())
            .isEqualTo("TilingTexture.Origin.STROKE_SPACE_ORIGIN")
        assertThat(TilingTexture.Origin.FIRST_STROKE_INPUT.toString())
            .isEqualTo("TilingTexture.Origin.FIRST_STROKE_INPUT")
        assertThat(TilingTexture.Origin.LAST_STROKE_INPUT.toString())
            .isEqualTo("TilingTexture.Origin.LAST_STROKE_INPUT")
    }

    // endregion

    // region Wrap class tests
    @Test
    fun wrapToString_returnsCorrectString() {
        assertThat(TextureLayer.Wrap.MIRROR.toString()).isEqualTo("TextureLayer.Wrap.MIRROR")
        assertThat(TextureLayer.Wrap.CLAMP.toString()).isEqualTo("TextureLayer.Wrap.CLAMP")
        assertThat(TextureLayer.Wrap.REPEAT.toString()).isEqualTo("TextureLayer.Wrap.REPEAT")
    }

    // endregion

    // region BlendMode class tests
    @Test
    fun textureBlendModeToString_returnsCorrectString() {
        assertThat(TextureLayer.BlendMode.MODULATE.toString()).contains("MODULATE")
        assertThat(TextureLayer.BlendMode.DST_IN.toString()).contains("DST_IN")
        assertThat(TextureLayer.BlendMode.DST_OUT.toString()).contains("DST_OUT")
        assertThat(TextureLayer.BlendMode.SRC_ATOP.toString()).contains("SRC_ATOP")
        assertThat(TextureLayer.BlendMode.SRC_IN.toString()).contains("SRC_IN")
        assertThat(TextureLayer.BlendMode.SRC_OVER.toString()).contains("SRC_OVER")
        assertThat(TextureLayer.BlendMode.DST_OVER.toString()).contains("DST_OVER")
        assertThat(TextureLayer.BlendMode.SRC.toString()).contains("SRC")
        assertThat(TextureLayer.BlendMode.DST.toString()).contains("DST")
        assertThat(TextureLayer.BlendMode.SRC_OUT.toString()).contains("SRC_OUT")
        assertThat(TextureLayer.BlendMode.DST_ATOP.toString()).contains("DST_ATOP")
        assertThat(TextureLayer.BlendMode.XOR.toString()).contains("XOR")
    }

    // endregion

    private fun makeTestTextureLayer() =
        StampingTexture(
            clientTextureId = TEST_TEXTURE_ID,
            animationFrames = 2,
            animationRows = 3,
            animationColumns = 4,
            animationDurationMillis = 5000,
            blendMode = TextureLayer.BlendMode.SRC_IN,
        )

    private fun makeTestPaint() =
        BrushPaint(
            textureLayers = listOf(makeTestTextureLayer()),
            selfOverlap = SelfOverlap.ACCUMULATE,
        )

    private companion object {
        const val TEST_TEXTURE_ID = "test-texture"
        const val OTHER_TEXTURE_ID = "other-texture"
    }
}
