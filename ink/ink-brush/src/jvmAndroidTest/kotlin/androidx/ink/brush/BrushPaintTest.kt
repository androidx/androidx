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

import androidx.ink.geometry.Angle
import androidx.ink.nativeloader.UsedByNative
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class BrushPaintTest {

    // region BrushPaint class tests
    @Test
    fun constructor_withValidArguments_returnsABrushPaint() {
        assertThat(
                BrushPaint(
                    textureLayers =
                        listOf(
                            BrushPaint.TextureLayer(
                                clientTextureId = TEST_TEXTURE_ID,
                                sizeX = 123.45F,
                                sizeY = 678.90F,
                                offsetX = 0.1f,
                                offsetY = 0.2f,
                                rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                                opacity = 0.3f,
                                animationFrames = 2,
                                animationRows = 3,
                                animationColumns = 4,
                                animationDurationMillis = 5000,
                                BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                                BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN,
                                BrushPaint.TextureMapping.TILING,
                                BrushPaint.TextureWrap.MIRROR,
                                BrushPaint.TextureWrap.REPEAT,
                            ),
                            BrushPaint.TextureLayer(
                                clientTextureId = TEST_TEXTURE_ID,
                                sizeX = 256F,
                                sizeY = 256F,
                                offsetX = 0.8f,
                                offsetY = 0.9f,
                                rotationDegrees = Angle.HALF_TURN_DEGREES,
                                opacity = 0.7f,
                                animationFrames = 2,
                                animationRows = 3,
                                animationColumns = 4,
                                animationDurationMillis = 5000,
                                BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                                BrushPaint.TextureOrigin.FIRST_STROKE_INPUT,
                                BrushPaint.TextureMapping.TILING,
                                BrushPaint.TextureWrap.CLAMP,
                                BrushPaint.TextureWrap.MIRROR,
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
        assertThat(customPaint).isNotEqualTo(null)
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
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, -32F, 64F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, 32F, -64F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, -32F, -64F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, 0F, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, 128F, 0F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, Float.NaN, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, 128F, Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, Float.POSITIVE_INFINITY, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, 128F, Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, Float.NEGATIVE_INFINITY, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, 128F, Float.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun textureLayerConstructor_withInvalidOffsetX_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, offsetX = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                offsetX = Float.POSITIVE_INFINITY,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                offsetX = Float.NEGATIVE_INFINITY,
            )
        }
    }

    @Test
    fun textureLayerConstructor_withInvalidOffsetY_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, offsetY = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                offsetY = Float.POSITIVE_INFINITY,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                offsetY = Float.NEGATIVE_INFINITY,
            )
        }
    }

    @Test
    fun textureLayerConstructor_withInvalidRotation_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                rotationDegrees = Float.NaN,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                rotationDegrees = Float.POSITIVE_INFINITY,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                rotationDegrees = Float.NEGATIVE_INFINITY,
            )
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun textureLayerConstructor_withInvalidOpacity_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, opacity = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, opacity = -0.001f)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, opacity = 1.001f)
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun textureLayerConstructor_withInvalidAnimationFrames_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationFrames = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationFrames = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                animationFrames = (1 shl 24) + 1,
            )
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun textureLayerConstructor_withInvalidAnimationAtlasDimensions_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationRows = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationRows = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                animationRows = (1 shl 12) + 1,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationColumns = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(TEST_TEXTURE_ID, sizeX = 1f, sizeY = 1f, animationColumns = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                animationColumns = (1 shl 12) + 1,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
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
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                animationDurationMillis = -1L,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                TEST_TEXTURE_ID,
                sizeX = 1f,
                sizeY = 1f,
                animationDurationMillis = 0L,
            )
        }
    }

    @Test
    fun textureLayerHashCode_withIdenticalValues_matches() {
        assertThat(makeTestTextureLayer().hashCode()).isEqualTo(makeTestTextureLayer().hashCode())
    }

    @Test
    fun textureLayerEquals_checksEqualityOfValues() {
        val layer =
            BrushPaint.TextureLayer(
                clientTextureId = TEST_TEXTURE_ID,
                sizeX = 128F,
                sizeY = 128F,
                offsetX = 0.1f,
                offsetY = 0.2f,
                rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                opacity = 0.3f,
                animationFrames = 2,
                animationRows = 3,
                animationColumns = 4,
                animationDurationMillis = 5000,
                BrushPaint.TextureSizeUnit.BRUSH_SIZE,
                BrushPaint.TextureOrigin.LAST_STROKE_INPUT,
                BrushPaint.TextureMapping.STAMPING,
                BrushPaint.TextureWrap.MIRROR,
                BrushPaint.TextureWrap.CLAMP,
                BrushPaint.BlendMode.SRC_IN,
            )

        // same values.
        assertThat(layer)
            .isEqualTo(
                BrushPaint.TextureLayer(
                    clientTextureId = TEST_TEXTURE_ID,
                    sizeX = 128F,
                    sizeY = 128F,
                    offsetX = 0.1f,
                    offsetY = 0.2f,
                    rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                    opacity = 0.3f,
                    animationFrames = 2,
                    animationRows = 3,
                    animationColumns = 4,
                    animationDurationMillis = 5000,
                    BrushPaint.TextureSizeUnit.BRUSH_SIZE,
                    BrushPaint.TextureOrigin.LAST_STROKE_INPUT,
                    BrushPaint.TextureMapping.STAMPING,
                    BrushPaint.TextureWrap.MIRROR,
                    BrushPaint.TextureWrap.CLAMP,
                    BrushPaint.BlendMode.SRC_IN,
                )
            )

        // different values.
        assertThat(layer).isNotEqualTo(null)
        assertThat(layer).isNotEqualTo(Any())
        assertThat(layer).isNotEqualTo(layer.copy(clientTextureId = OTHER_TEXTURE_ID))
        assertThat(layer).isNotEqualTo(layer.copy(sizeX = 999F))
        assertThat(layer).isNotEqualTo(layer.copy(sizeY = 999F))
        assertThat(layer).isNotEqualTo(layer.copy(offsetX = 0.999F))
        assertThat(layer).isNotEqualTo(layer.copy(offsetY = 0.999F))
        assertThat(layer).isNotEqualTo(layer.copy(rotationDegrees = Angle.HALF_TURN_DEGREES))
        assertThat(layer).isNotEqualTo(layer.copy(opacity = 0.999f))
        assertThat(layer).isNotEqualTo(layer.copy(animationFrames = 5))
        assertThat(layer).isNotEqualTo(layer.copy(animationRows = 6))
        assertThat(layer).isNotEqualTo(layer.copy(animationColumns = 7))
        assertThat(layer).isNotEqualTo(layer.copy(animationDurationMillis = 8000))
        assertThat(layer)
            .isNotEqualTo(layer.copy(sizeUnit = BrushPaint.TextureSizeUnit.STROKE_COORDINATES))
        assertThat(layer)
            .isNotEqualTo(layer.copy(origin = BrushPaint.TextureOrigin.FIRST_STROKE_INPUT))
        assertThat(layer).isNotEqualTo(layer.copy(mapping = BrushPaint.TextureMapping.TILING))
        assertThat(layer).isNotEqualTo(layer.copy(wrapX = BrushPaint.TextureWrap.REPEAT))
        assertThat(layer).isNotEqualTo(layer.copy(wrapY = BrushPaint.TextureWrap.MIRROR))
        assertThat(layer).isNotEqualTo(layer.copy(blendMode = BrushPaint.BlendMode.MODULATE))
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
            BrushPaint.TextureLayer(
                clientTextureId = TEST_TEXTURE_ID,
                sizeX = 128F,
                sizeY = 128F,
                offsetX = 0.1f,
                offsetY = 0.2f,
                rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                opacity = 0.3f,
                animationFrames = 2,
                animationRows = 3,
                animationColumns = 4,
                animationDurationMillis = 5000,
                BrushPaint.TextureSizeUnit.BRUSH_SIZE,
                BrushPaint.TextureOrigin.FIRST_STROKE_INPUT,
                BrushPaint.TextureMapping.STAMPING,
                BrushPaint.TextureWrap.MIRROR,
                BrushPaint.TextureWrap.CLAMP,
                BrushPaint.BlendMode.SRC_IN,
            )
        val changedSizeX = originalLayer.copy(sizeX = 999F)

        // sizeX changed.
        assertThat(changedSizeX).isNotEqualTo(originalLayer)
        assertThat(changedSizeX.sizeX).isNotEqualTo(originalLayer.sizeX)

        assertThat(changedSizeX)
            .isEqualTo(
                BrushPaint.TextureLayer(
                    clientTextureId = TEST_TEXTURE_ID,
                    sizeX = 999F, // Changed
                    sizeY = 128F,
                    offsetX = 0.1f,
                    offsetY = 0.2f,
                    rotationDegrees = Angle.QUARTER_TURN_DEGREES,
                    opacity = 0.3f,
                    animationFrames = 2,
                    animationRows = 3,
                    animationColumns = 4,
                    animationDurationMillis = 5000,
                    BrushPaint.TextureSizeUnit.BRUSH_SIZE,
                    BrushPaint.TextureOrigin.FIRST_STROKE_INPUT,
                    BrushPaint.TextureMapping.STAMPING,
                    BrushPaint.TextureWrap.MIRROR,
                    BrushPaint.TextureWrap.CLAMP,
                    BrushPaint.BlendMode.SRC_IN,
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
        assertThat(string).contains("opacity")
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
        assertThat(BrushPaint.TextureSizeUnit.BRUSH_SIZE.toString())
            .isEqualTo("BrushPaint.TextureSizeUnit.BRUSH_SIZE")
        assertThat(BrushPaint.TextureSizeUnit.STROKE_SIZE.toString())
            .isEqualTo("BrushPaint.TextureSizeUnit.STROKE_SIZE")
        assertThat(BrushPaint.TextureSizeUnit.STROKE_COORDINATES.toString())
            .isEqualTo("BrushPaint.TextureSizeUnit.STROKE_COORDINATES")
    }

    // endregion

    // region Origin class tests
    @Test
    fun originConstants_areDistinct() {
        val set =
            setOf(
                BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN,
                BrushPaint.TextureOrigin.FIRST_STROKE_INPUT,
                BrushPaint.TextureOrigin.LAST_STROKE_INPUT,
            )
        assertThat(set).hasSize(3)
    }

    @Test
    fun originHashCode_withIdenticalValues_match() {
        assertThat(BrushPaint.TextureOrigin.FIRST_STROKE_INPUT.hashCode())
            .isEqualTo(BrushPaint.TextureOrigin.FIRST_STROKE_INPUT.hashCode())
    }

    @Test
    fun originEquals_checksEqualityOfValues() {
        assertThat(BrushPaint.TextureOrigin.FIRST_STROKE_INPUT)
            .isEqualTo(BrushPaint.TextureOrigin.FIRST_STROKE_INPUT)
        assertThat(BrushPaint.TextureOrigin.FIRST_STROKE_INPUT)
            .isNotEqualTo(BrushPaint.TextureOrigin.LAST_STROKE_INPUT)
    }

    @Test
    fun originToString_returnsCorrectString() {
        assertThat(BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN.toString())
            .isEqualTo("BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN")
        assertThat(BrushPaint.TextureOrigin.FIRST_STROKE_INPUT.toString())
            .isEqualTo("BrushPaint.TextureOrigin.FIRST_STROKE_INPUT")
        assertThat(BrushPaint.TextureOrigin.LAST_STROKE_INPUT.toString())
            .isEqualTo("BrushPaint.TextureOrigin.LAST_STROKE_INPUT")
    }

    // endregion

    // region Mapping class tests
    @Test
    fun mappingToString_returnsCorrectString() {
        assertThat(BrushPaint.TextureMapping.TILING.toString())
            .isEqualTo("BrushPaint.TextureMapping.TILING")
        assertThat(BrushPaint.TextureMapping.STAMPING.toString())
            .isEqualTo("BrushPaint.TextureMapping.STAMPING")
    }

    // endregion

    // region Wrap class tests
    @Test
    fun wrapToString_returnsCorrectString() {
        assertThat(BrushPaint.TextureWrap.MIRROR.toString())
            .isEqualTo("BrushPaint.TextureWrap.MIRROR")
        assertThat(BrushPaint.TextureWrap.CLAMP.toString())
            .isEqualTo("BrushPaint.TextureWrap.CLAMP")
        assertThat(BrushPaint.TextureWrap.REPEAT.toString())
            .isEqualTo("BrushPaint.TextureWrap.REPEAT")
    }

    // endregion

    // region BlendMode class tests
    @Test
    fun textureBlendModeToString_returnsCorrectString() {
        assertThat(BrushPaint.BlendMode.MODULATE.toString()).contains("MODULATE")
        assertThat(BrushPaint.BlendMode.DST_IN.toString()).contains("DST_IN")
        assertThat(BrushPaint.BlendMode.DST_OUT.toString()).contains("DST_OUT")
        assertThat(BrushPaint.BlendMode.SRC_ATOP.toString()).contains("SRC_ATOP")
        assertThat(BrushPaint.BlendMode.SRC_IN.toString()).contains("SRC_IN")
        assertThat(BrushPaint.BlendMode.SRC_OVER.toString()).contains("SRC_OVER")
        assertThat(BrushPaint.BlendMode.DST_OVER.toString()).contains("DST_OVER")
        assertThat(BrushPaint.BlendMode.SRC.toString()).contains("SRC")
        assertThat(BrushPaint.BlendMode.DST.toString()).contains("DST")
        assertThat(BrushPaint.BlendMode.SRC_OUT.toString()).contains("SRC_OUT")
        assertThat(BrushPaint.BlendMode.DST_ATOP.toString()).contains("DST_ATOP")
        assertThat(BrushPaint.BlendMode.XOR.toString()).contains("XOR")
    }

    // endregion

    @UsedByNative
    private external fun matchesNativeCustomPaint(brushPaintNativePointer: Long): Boolean

    private fun makeTestTextureLayer() =
        BrushPaint.TextureLayer(
            clientTextureId = TEST_TEXTURE_ID,
            sizeX = 128F,
            sizeY = 128F,
            offsetX = 0.1f,
            offsetY = 0.2f,
            rotationDegrees = Angle.QUARTER_TURN_DEGREES,
            opacity = 0.3f,
            animationFrames = 2,
            animationRows = 3,
            animationColumns = 4,
            animationDurationMillis = 5000,
            BrushPaint.TextureSizeUnit.BRUSH_SIZE,
            BrushPaint.TextureOrigin.FIRST_STROKE_INPUT,
            BrushPaint.TextureMapping.STAMPING,
            BrushPaint.TextureWrap.REPEAT,
            BrushPaint.TextureWrap.REPEAT,
            BrushPaint.BlendMode.SRC_IN,
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
