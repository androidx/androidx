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
                    listOf(
                        BrushPaint.TextureLayer(
                            colorTextureUri = makeTestTextureUri(1),
                            sizeX = 123.45F,
                            sizeY = 678.90F,
                            offsetX = 0.1f,
                            offsetY = 0.2f,
                            rotation = Angle.QUARTER_TURN_RADIANS,
                            opacity = 0.3f,
                            BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                            BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN,
                            BrushPaint.TextureMapping.TILING,
                        ),
                        BrushPaint.TextureLayer(
                            colorTextureUri = makeTestTextureUri(2),
                            sizeX = 256F,
                            sizeY = 256F,
                            offsetX = 0.8f,
                            offsetY = 0.9f,
                            rotation = Angle.HALF_TURN_RADIANS,
                            opacity = 0.7f,
                            BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                            BrushPaint.TextureOrigin.FIRST_STROKE_INPUT,
                            BrushPaint.TextureMapping.TILING,
                        ),
                    )
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
    }

    // endregion

    // region TextureLayer class tests
    @Test
    @Suppress("Range") // Testing error cases.
    fun textureLayerConstructor_withInvalidSizes_throwsIllegalArgumentException() {
        val fakeValidUri = makeTestTextureUri()
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, -32F, 64F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, 32F, -64F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, -32F, -64F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, 0F, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, 128F, 0F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, Float.NaN, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, 128F, Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, Float.POSITIVE_INFINITY, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, 128F, Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, Float.NEGATIVE_INFINITY, 128F)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, 128F, Float.NEGATIVE_INFINITY)
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun textureLayerConstructor_withInvalidOffsetX_throwsIllegalArgumentException() {
        val fakeValidUri = makeTestTextureUri()
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, sizeX = 1f, sizeY = 1f, offsetX = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, sizeX = 1f, sizeY = 1f, offsetX = -0.001f)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, sizeX = 1f, sizeY = 1f, offsetX = 1.001f)
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun textureLayerConstructor_withInvalidOffsetY_throwsIllegalArgumentException() {
        val fakeValidUri = makeTestTextureUri()
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, sizeX = 1f, sizeY = 1f, offsetY = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, sizeX = 1f, sizeY = 1f, offsetY = -0.001f)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, sizeX = 1f, sizeY = 1f, offsetY = 1.001f)
        }
    }

    @Test
    fun textureLayerConstructor_withInvalidRotation_throwsIllegalArgumentException() {
        val fakeValidUri = makeTestTextureUri()
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, sizeX = 1f, sizeY = 1f, rotation = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                fakeValidUri,
                sizeX = 1f,
                sizeY = 1f,
                rotation = Float.POSITIVE_INFINITY,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(
                fakeValidUri,
                sizeX = 1f,
                sizeY = 1f,
                rotation = Float.NEGATIVE_INFINITY,
            )
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun textureLayerConstructor_withInvalidOpacity_throwsIllegalArgumentException() {
        val fakeValidUri = makeTestTextureUri()
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, sizeX = 1f, sizeY = 1f, opacity = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, sizeX = 1f, sizeY = 1f, opacity = -0.001f)
        }
        assertFailsWith<IllegalArgumentException> {
            BrushPaint.TextureLayer(fakeValidUri, sizeX = 1f, sizeY = 1f, opacity = 1.001f)
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
                colorTextureUri = makeTestTextureUri(),
                sizeX = 128F,
                sizeY = 128F,
                offsetX = 0.1f,
                offsetY = 0.2f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                opacity = 0.3f,
                BrushPaint.TextureSizeUnit.BRUSH_SIZE,
                BrushPaint.TextureOrigin.LAST_STROKE_INPUT,
                BrushPaint.TextureMapping.WINDING,
                BrushPaint.BlendMode.SRC_IN,
            )

        // same values.
        assertThat(layer)
            .isEqualTo(
                BrushPaint.TextureLayer(
                    colorTextureUri = makeTestTextureUri(),
                    sizeX = 128F,
                    sizeY = 128F,
                    offsetX = 0.1f,
                    offsetY = 0.2f,
                    rotation = Angle.QUARTER_TURN_RADIANS,
                    opacity = 0.3f,
                    BrushPaint.TextureSizeUnit.BRUSH_SIZE,
                    BrushPaint.TextureOrigin.LAST_STROKE_INPUT,
                    BrushPaint.TextureMapping.WINDING,
                    BrushPaint.BlendMode.SRC_IN,
                )
            )

        // different values.
        assertThat(layer).isNotEqualTo(null)
        assertThat(layer).isNotEqualTo(Any())
        assertThat(layer).isNotEqualTo(layer.copy(colorTextureUri = makeTestTextureUri(2)))
        assertThat(layer).isNotEqualTo(layer.copy(sizeX = 999F))
        assertThat(layer).isNotEqualTo(layer.copy(sizeY = 999F))
        assertThat(layer).isNotEqualTo(layer.copy(offsetX = 0.999F))
        assertThat(layer).isNotEqualTo(layer.copy(offsetY = 0.999F))
        assertThat(layer).isNotEqualTo(layer.copy(rotation = Angle.HALF_TURN_RADIANS))
        assertThat(layer).isNotEqualTo(layer.copy(opacity = 0.999f))
        assertThat(layer)
            .isNotEqualTo(layer.copy(sizeUnit = BrushPaint.TextureSizeUnit.STROKE_COORDINATES))
        assertThat(layer)
            .isNotEqualTo(layer.copy(origin = BrushPaint.TextureOrigin.FIRST_STROKE_INPUT))
        assertThat(layer).isNotEqualTo(layer.copy(mapping = BrushPaint.TextureMapping.TILING))
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
                colorTextureUri = makeTestTextureUri(),
                sizeX = 128F,
                sizeY = 128F,
                offsetX = 0.1f,
                offsetY = 0.2f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                opacity = 0.3f,
                BrushPaint.TextureSizeUnit.BRUSH_SIZE,
                BrushPaint.TextureOrigin.FIRST_STROKE_INPUT,
                BrushPaint.TextureMapping.WINDING,
                BrushPaint.BlendMode.SRC_IN,
            )
        val changedSizeX = originalLayer.copy(sizeX = 999F)

        // sizeX changed.
        assertThat(changedSizeX).isNotEqualTo(originalLayer)
        assertThat(changedSizeX.sizeX).isNotEqualTo(originalLayer.sizeX)

        assertThat(changedSizeX)
            .isEqualTo(
                BrushPaint.TextureLayer(
                    colorTextureUri = makeTestTextureUri(),
                    sizeX = 999F, // Changed
                    sizeY = 128F,
                    offsetX = 0.1f,
                    offsetY = 0.2f,
                    rotation = Angle.QUARTER_TURN_RADIANS,
                    opacity = 0.3f,
                    BrushPaint.TextureSizeUnit.BRUSH_SIZE,
                    BrushPaint.TextureOrigin.FIRST_STROKE_INPUT,
                    BrushPaint.TextureMapping.WINDING,
                    BrushPaint.BlendMode.SRC_IN,
                )
            )
    }

    @Test
    fun textureLayerToString_returnsExpectedValues() {
        val string = makeTestTextureLayer().toString()
        assertThat(string).contains("TextureLayer")
        assertThat(string).contains("colorTextureUri")
        assertThat(string).contains("size")
        assertThat(string).contains("offset")
        assertThat(string).contains("rotation")
        assertThat(string).contains("opacity")
        assertThat(string).contains("sizeUnit")
        assertThat(string).contains("origin")
        assertThat(string).contains("mapping")
        assertThat(string).contains("blendMode")
    }

    // endregion

    // region SizeUnit class tests
    @Test
    fun sizeUnitConstants_areDistinct() {
        val set =
            setOf(
                BrushPaint.TextureSizeUnit.BRUSH_SIZE,
                BrushPaint.TextureSizeUnit.STROKE_SIZE,
                BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
            )
        assertThat(set).hasSize(3)
    }

    @Test
    fun sizeUnitHashCode_withIdenticalValues_match() {
        assertThat(BrushPaint.TextureSizeUnit.STROKE_COORDINATES.hashCode())
            .isEqualTo(BrushPaint.TextureSizeUnit.STROKE_COORDINATES.hashCode())
    }

    @Test
    fun sizeUnitEquals_checksEqualityOfValues() {
        assertThat(BrushPaint.TextureSizeUnit.STROKE_COORDINATES)
            .isEqualTo(BrushPaint.TextureSizeUnit.STROKE_COORDINATES)
        assertThat(BrushPaint.TextureSizeUnit.STROKE_COORDINATES)
            .isNotEqualTo(BrushPaint.TextureSizeUnit.BRUSH_SIZE)
    }

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
    fun originConstants_areDistint() {
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
    fun mappingConstants_areDistint() {
        val set = setOf(BrushPaint.TextureMapping.TILING, BrushPaint.TextureMapping.WINDING)
        assertThat(set).hasSize(2)
    }

    @Test
    fun mappingHashCode_withIdenticalValues_match() {
        assertThat(BrushPaint.TextureMapping.TILING.hashCode())
            .isEqualTo(BrushPaint.TextureMapping.TILING.hashCode())
    }

    @Test
    fun mappingEquals_checksEqualityOfValues() {
        assertThat(BrushPaint.TextureMapping.TILING).isEqualTo(BrushPaint.TextureMapping.TILING)
        assertThat(BrushPaint.TextureMapping.TILING).isNotEqualTo(BrushPaint.TextureMapping.WINDING)
    }

    @Test
    fun mappingToString_returnsCorrectString() {
        assertThat(BrushPaint.TextureMapping.TILING.toString())
            .isEqualTo("BrushPaint.TextureMapping.TILING")
        assertThat(BrushPaint.TextureMapping.WINDING.toString())
            .isEqualTo("BrushPaint.TextureMapping.WINDING")
    }

    // endregion

    // region BlendMode class tests
    @Test
    fun textureBlendModeConstants_areDistinct() {
        val set =
            setOf(
                BrushPaint.BlendMode.MODULATE,
                BrushPaint.BlendMode.DST_IN,
                BrushPaint.BlendMode.DST_OUT,
                BrushPaint.BlendMode.SRC_ATOP,
                BrushPaint.BlendMode.SRC_IN,
                BrushPaint.BlendMode.SRC_OVER,
            )
        assertThat(set).hasSize(6)
    }

    @Test
    fun textureBlendModeHashCode_withIdenticalValues_match() {
        assertThat(BrushPaint.BlendMode.MODULATE.hashCode())
            .isEqualTo(BrushPaint.BlendMode.MODULATE.hashCode())
    }

    @Test
    fun textureBlendModeEquals_checksEqualityOfValues() {
        assertThat(BrushPaint.BlendMode.MODULATE).isEqualTo(BrushPaint.BlendMode.MODULATE)
        assertThat(BrushPaint.BlendMode.MODULATE).isNotEqualTo(BrushPaint.BlendMode.SRC_OVER)
    }

    @Test
    fun textureBlendModeToString_returnsCorrectString() {
        assertThat(BrushPaint.BlendMode.MODULATE.toString()).contains("MODULATE")
        assertThat(BrushPaint.BlendMode.DST_IN.toString()).contains("DST_IN")
        assertThat(BrushPaint.BlendMode.DST_OUT.toString()).contains("DST_OUT")
        assertThat(BrushPaint.BlendMode.SRC_ATOP.toString()).contains("SRC_ATOP")
        assertThat(BrushPaint.BlendMode.SRC_IN.toString()).contains("SRC_IN")
        assertThat(BrushPaint.BlendMode.SRC_OVER.toString()).contains("SRC_OVER")
    }

    // endregion

    @UsedByNative
    private external fun matchesNativeCustomPaint(brushPaintNativePointer: Long): Boolean

    private fun makeTestTextureUri(version: Int = 0) =
        "ink://ink/texture:test-texture" + if (version == 0) "" else ":" + version

    private fun makeTestTextureLayer() =
        BrushPaint.TextureLayer(
            colorTextureUri = makeTestTextureUri(),
            sizeX = 128F,
            sizeY = 128F,
            offsetX = 0.1f,
            offsetY = 0.2f,
            rotation = Angle.QUARTER_TURN_RADIANS,
            opacity = 0.3f,
            BrushPaint.TextureSizeUnit.BRUSH_SIZE,
            BrushPaint.TextureOrigin.FIRST_STROKE_INPUT,
            BrushPaint.TextureMapping.WINDING,
            BrushPaint.BlendMode.SRC_IN,
        )

    private fun makeTestPaint() = BrushPaint(listOf(makeTestTextureLayer()))
}
