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
import androidx.ink.brush.BrushPaint.TextureLayer
import androidx.ink.geometry.Angle
import androidx.ink.nativeloader.UsedByNative
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BrushPaintTest {

    // region BrushPaint class tests
    @Test
    fun constructor_withValidArguments_returnsABrushPaint() {
        assertThat(
                BrushPaint(
                    textureLayers =
                        listOf(
                            TextureLayer(
                                clientTextureId = TEST_TEXTURE_ID,
                                sizeX = 123.45F,
                                sizeY = 678.90F,
                                offsetX = 0.1f,
                                offsetY = 0.2f,
                                rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                                animationFrames = 2,
                                animationRows = 3,
                                animationColumns = 4,
                                animationDurationMillis = 5000,
                                sizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                                origin = TextureLayer.Origin.STROKE_SPACE_ORIGIN,
                                mapping = TextureLayer.Mapping.TILING,
                                wrapX = TextureLayer.Wrap.MIRROR,
                                wrapY = TextureLayer.Wrap.REPEAT,
                            ),
                            TextureLayer(
                                clientTextureId = TEST_TEXTURE_ID,
                                sizeX = 256F,
                                sizeY = 256F,
                                offsetX = 0.8f,
                                offsetY = 0.9f,
                                rotationDegrees = Angle.HALF_TURN_DEGREES,
                                animationFrames = 2,
                                animationRows = 3,
                                animationColumns = 4,
                                animationDurationMillis = 5000,
                                sizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                                origin = TextureLayer.Origin.FIRST_STROKE_INPUT,
                                mapping = TextureLayer.Mapping.TILING,
                                wrapX = TextureLayer.Wrap.CLAMP,
                                wrapY = TextureLayer.Wrap.MIRROR,
                            ),
                        ),
                    colorFunctions = listOf<ColorFunction>(ColorFunction.OpacityMultiplier(0.75f)),
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
    fun textureLayerConstructor_withInvalidSizes_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { TextureLayer(TEST_TEXTURE_ID, -32F, 64F) }
        assertFailsWith<IllegalArgumentException> { TextureLayer(TEST_TEXTURE_ID, 32F, -64F) }
        assertFailsWith<IllegalArgumentException> { TextureLayer(TEST_TEXTURE_ID, -32F, -64F) }
        assertFailsWith<IllegalArgumentException> { TextureLayer(TEST_TEXTURE_ID, 0F, 128F) }
        assertFailsWith<IllegalArgumentException> { TextureLayer(TEST_TEXTURE_ID, 128F, 0F) }
        assertFailsWith<IllegalArgumentException> { TextureLayer(TEST_TEXTURE_ID, Float.NaN, 128F) }
        assertFailsWith<IllegalArgumentException> { TextureLayer(TEST_TEXTURE_ID, 128F, Float.NaN) }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, Float.POSITIVE_INFINITY, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, 128F, Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, Float.NEGATIVE_INFINITY, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, 128F, Float.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun textureLayerConstructor_withInvalidOffsetX_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, offsetX = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, offsetX = Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, offsetX = Float.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun textureLayerConstructor_withInvalidOffsetY_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, offsetY = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, offsetY = Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, offsetY = Float.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun textureLayerConstructor_withInvalidRotation_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, rotationDegrees = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                rotationDegrees = Float.POSITIVE_INFINITY,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                rotationDegrees = Float.NEGATIVE_INFINITY,
            )
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun textureLayerConstructor_withInvalidAnimationFrames_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationFrames = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationFrames = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationFrames = (1 shl 24) + 1)
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun textureLayerConstructor_withInvalidAnimationAtlasDimensions_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationRows = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationRows = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationRows = (1 shl 12) + 1)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationColumns = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationColumns = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationColumns = (1 shl 12) + 1)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                animationFrames = 7,
                animationRows = 2,
                animationColumns = 3,
            )
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun textureLayerConstructor_withInvalidAnimationDuration_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationDurationMillis = -1L)
        }
        assertFailsWith<IllegalArgumentException> {
            TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationDurationMillis = 0L)
        }
    }

    @Test
    fun textureLayerHashCode_withIdenticalValues_matches() {
        assertThat(makeTestTextureLayer().hashCode()).isEqualTo(makeTestTextureLayer().hashCode())
    }

    @Test
    fun textureLayerEquals_checksEqualityOfValues() {
        val layer =
            TextureLayer(
                clientTextureId = TEST_TEXTURE_ID,
                sizeX = 128F,
                sizeY = 128F,
                offsetX = 0.1f,
                offsetY = 0.2f,
                rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                animationFrames = 2,
                animationRows = 3,
                animationColumns = 4,
                animationDurationMillis = 5000,
                sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                origin = TextureLayer.Origin.LAST_STROKE_INPUT,
                mapping = TextureLayer.Mapping.STAMPING,
                wrapX = TextureLayer.Wrap.MIRROR,
                wrapY = TextureLayer.Wrap.CLAMP,
                blendMode = TextureLayer.BlendMode.SRC_IN,
            )

        // same values.
        assertThat(layer)
            .isEqualTo(
                TextureLayer(
                    clientTextureId = TEST_TEXTURE_ID,
                    sizeX = 128F,
                    sizeY = 128F,
                    offsetX = 0.1f,
                    offsetY = 0.2f,
                    rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                    animationFrames = 2,
                    animationRows = 3,
                    animationColumns = 4,
                    animationDurationMillis = 5000,
                    sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    origin = TextureLayer.Origin.LAST_STROKE_INPUT,
                    mapping = TextureLayer.Mapping.STAMPING,
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
        assertThat(layer).isNotEqualTo(layer.copy(animationFrames = 5))
        assertThat(layer).isNotEqualTo(layer.copy(animationRows = 6))
        assertThat(layer).isNotEqualTo(layer.copy(animationColumns = 7))
        assertThat(layer).isNotEqualTo(layer.copy(animationDurationMillis = 8000))
        assertThat(layer)
            .isNotEqualTo(layer.copy(sizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES))
        assertThat(layer).isNotEqualTo(layer.copy(origin = TextureLayer.Origin.FIRST_STROKE_INPUT))
        assertThat(layer).isNotEqualTo(layer.copy(mapping = TextureLayer.Mapping.TILING))
        assertThat(layer).isNotEqualTo(layer.copy(wrapX = TextureLayer.Wrap.REPEAT))
        assertThat(layer).isNotEqualTo(layer.copy(wrapY = TextureLayer.Wrap.MIRROR))
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
    fun textureLayerCopy_withArguments_createsCopyWithChanges() {
        val originalLayer =
            TextureLayer(
                clientTextureId = TEST_TEXTURE_ID,
                sizeX = 128F,
                sizeY = 128F,
                offsetX = 0.1f,
                offsetY = 0.2f,
                rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                animationFrames = 2,
                animationRows = 3,
                animationColumns = 4,
                animationDurationMillis = 5000,
                sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                origin = TextureLayer.Origin.FIRST_STROKE_INPUT,
                mapping = TextureLayer.Mapping.STAMPING,
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
                TextureLayer(
                    clientTextureId = TEST_TEXTURE_ID,
                    sizeX = 999F, // Changed
                    sizeY = 128F,
                    offsetX = 0.1f,
                    offsetY = 0.2f,
                    rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                    animationFrames = 2,
                    animationRows = 3,
                    animationColumns = 4,
                    animationDurationMillis = 5000,
                    sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    origin = TextureLayer.Origin.FIRST_STROKE_INPUT,
                    mapping = TextureLayer.Mapping.STAMPING,
                    wrapX = TextureLayer.Wrap.MIRROR,
                    wrapY = TextureLayer.Wrap.CLAMP,
                    blendMode = TextureLayer.BlendMode.SRC_IN,
                )
            )
    }

    @Test
    fun textureLayerToString_returnsExpectedValues() {
        val string = makeTestTextureLayer().toString()
        assertThat(string).contains("TextureLayer")
        assertThat(string).contains("clientTextureId")
        assertThat(string).contains("size")
        assertThat(string).contains("offset")
        assertThat(string).contains("rotation")
        assertThat(string).contains("animationFrames")
        assertThat(string).contains("animationRows")
        assertThat(string).contains("animationColumns")
        assertThat(string).contains("animationDurationMillis")
        assertThat(string).contains("sizeUnit")
        assertThat(string).contains("origin")
        assertThat(string).contains("mapping")
        assertThat(string).contains("wrapX")
        assertThat(string).contains("wrapY")
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
                TextureLayer.Origin.STROKE_SPACE_ORIGIN,
                TextureLayer.Origin.FIRST_STROKE_INPUT,
                TextureLayer.Origin.LAST_STROKE_INPUT,
            )
        assertThat(set).hasSize(3)
    }

    @Test
    fun originHashCode_withIdenticalValues_match() {
        assertThat(TextureLayer.Origin.FIRST_STROKE_INPUT.hashCode())
            .isEqualTo(TextureLayer.Origin.FIRST_STROKE_INPUT.hashCode())
    }

    @Test
    fun originEquals_checksEqualityOfValues() {
        assertThat(TextureLayer.Origin.FIRST_STROKE_INPUT)
            .isEqualTo(TextureLayer.Origin.FIRST_STROKE_INPUT)
        assertThat(TextureLayer.Origin.FIRST_STROKE_INPUT)
            .isNotEqualTo(TextureLayer.Origin.LAST_STROKE_INPUT)
    }

    @Test
    fun originToString_returnsCorrectString() {
        assertThat(TextureLayer.Origin.STROKE_SPACE_ORIGIN.toString())
            .isEqualTo("TextureLayer.Origin.STROKE_SPACE_ORIGIN")
        assertThat(TextureLayer.Origin.FIRST_STROKE_INPUT.toString())
            .isEqualTo("TextureLayer.Origin.FIRST_STROKE_INPUT")
        assertThat(TextureLayer.Origin.LAST_STROKE_INPUT.toString())
            .isEqualTo("TextureLayer.Origin.LAST_STROKE_INPUT")
    }

    // endregion

    // region Mapping class tests
    @Test
    fun mappingToString_returnsCorrectString() {
        assertThat(TextureLayer.Mapping.TILING.toString()).isEqualTo("TextureLayer.Mapping.TILING")
        assertThat(TextureLayer.Mapping.STAMPING.toString())
            .isEqualTo("TextureLayer.Mapping.STAMPING")
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

    @UsedByNative
    private external fun matchesNativeCustomPaint(brushPaintNativePointer: Long): Boolean

    private fun makeTestTextureLayer() =
        TextureLayer(
            clientTextureId = TEST_TEXTURE_ID,
            sizeX = 128F,
            sizeY = 128F,
            offsetX = 0.1f,
            offsetY = 0.2f,
            rotationDegrees = Angle.QUARTER_TURN_DEGREES,
            animationFrames = 2,
            animationRows = 3,
            animationColumns = 4,
            animationDurationMillis = 5000,
            sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
            origin = TextureLayer.Origin.FIRST_STROKE_INPUT,
            mapping = TextureLayer.Mapping.STAMPING,
            wrapX = TextureLayer.Wrap.REPEAT,
            wrapY = TextureLayer.Wrap.REPEAT,
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
