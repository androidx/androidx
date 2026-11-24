/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.rendering.test

import androidx.annotation.ColorInt
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushPaint.AnimationRepeatMode
import androidx.ink.brush.BrushPaint.StampingTexture
import androidx.ink.brush.BrushPaint.TextureLayer
import androidx.ink.brush.BrushPaint.TilingTexture
import androidx.ink.brush.BrushTip
import androidx.ink.brush.ExperimentalInkAnimationApi
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.SelfOverlap
import androidx.ink.brush.behavior.OutOfRange
import androidx.ink.brush.behavior.SourceNode
import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.brush.behavior.TargetNode
import androidx.ink.brush.behavior.TargetNode.Target
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.Angle
import androidx.ink.geometry.ImmutableAffineTransform
import androidx.ink.geometry.MutableAffineTransform
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import kotlin.math.PI
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Abstract parameterized rendering test base class.
 *
 * Concrete subclasses on each platform provide renderer-specific implementations of the following:
 * - Loading files (test inputs, textures, golden images)
 * - Rendering to a platform-specific image format
 * - Image comparison/assertion
 */
@OptIn(ExperimentalInkCustomBrushApi::class, ExperimentalInkAnimationApi::class)
abstract class AbstractStrokeRendererTest {

    class TestCase(
        val name: String,
        val dryStroke: Stroke,
        val wetStroke: InProgressStroke,
        val transform: AffineTransform = AffineTransform.IDENTITY,
        val animatorClockStateMillis: Long = 0L,
        /**
         * Minimum Android API level on which Android `CanvasStrokeRenderer` supports rendering this
         * test case (e.g., 34 for `Canvas.drawMesh`-only features such as `StampingTexture` without
         * a tiling fallback or `SelfOverlap.ACCUMULATE`). Ignored on non-Android platforms.
         */
        val minAndroidSdk: Int = 21,
    ) {
        override fun toString(): String = name
    }

    /**
     * Loads the canonical stroke inputs used for testing - a real recording of someone writing
     * "hello" in cursive.
     *
     * Concrete implementations must provide standard platform-specific loading logic to satisfy the
     * contract. Each concrete implementation must load the same input data so that the resulting
     * rendered images can be compared for consistency.
     */
    protected abstract fun loadCursiveHelloInputs(): ImmutableStrokeInputBatch

    private val cursiveHelloInputs: ImmutableStrokeInputBatch by lazy { loadCursiveHelloInputs() }

    protected val simpleStrokeTexturedTestCase: TestCase by lazy {
        createTestCase(
            name = "simple_stroke_textured",
            brush =
                Brush.createWithColorIntArgb(
                    family =
                        BrushFamily(
                            paint =
                                BrushPaint(
                                    listOf(
                                        TilingTexture(
                                            clientTextureId = TEXTURE_ID_CHECKERBOARD,
                                            sizeX = 10f,
                                            sizeY = 10f,
                                            sizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                                        )
                                    )
                                )
                        ),
                    colorIntArgb = 0xFF0000FF.toInt(),
                    size = 15f,
                    epsilon = 0.1f,
                ),
            inputs = cursiveHelloInputs,
        )
    }

    protected val testCases: List<TestCase> by lazy {
        listOf(
            // =========================================================================
            // 1. SIMPLE STROKES
            // =========================================================================
            createTestCase(
                name = "simple_stroke_solid",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(paint = BrushPaint()),
                        colorIntArgb = COLOR_LIGHT_BLUE,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
            ),
            simpleStrokeTexturedTestCase,
            createTestCase(
                name = "simple_stroke_translucent",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(paint = BrushPaint()),
                        colorIntArgb = COLOR_COBALT_BLUE_TRANSLUCENT,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
            ),
            createTestCase(
                name = "simple_stroke_multicoat",
                brush =
                    Brush.createWithColorIntArgb(
                        family =
                            BrushFamily(
                                listOf(
                                    BrushCoat(
                                        paint =
                                            tilingTextureBrushPaint(
                                                textureSizeUnit =
                                                    TextureLayer.SizeUnit.STROKE_COORDINATES,
                                                textureSize = 10f,
                                            )
                                    ),
                                    BrushCoat(tip = BrushTip(scaleX = 0.5f, scaleY = 0.5f)),
                                )
                            ),
                        colorIntArgb = COLOR_RED,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
            ),
            createTestCase(
                name = "simple_stroke_opacity_and_hcl_shift",
                brush =
                    Brush.createWithColorIntArgb(
                        family =
                            BrushFamily(
                                tip =
                                    BrushTip(
                                        behaviors =
                                            listOf(
                                                BrushBehavior(
                                                    TargetNode(
                                                        target = Target.OPACITY_MULTIPLIER,
                                                        targetModifierRangeStart = 1f,
                                                        targetModifierRangeEnd = 0.25f,
                                                        input =
                                                            SourceNode(
                                                                source =
                                                                    Source
                                                                        .DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                                                                sourceValueRangeStart = 0f,
                                                                sourceValueRangeEnd = 2f,
                                                                sourceOutOfRangeBehavior =
                                                                    OutOfRange.MIRROR,
                                                            ),
                                                    )
                                                ),
                                                BrushBehavior(
                                                    TargetNode(
                                                        target = Target.HUE_OFFSET_IN_RADIANS,
                                                        targetModifierRangeStart = 0f,
                                                        targetModifierRangeEnd =
                                                            Angle.FULL_TURN_RADIANS,
                                                        input =
                                                            SourceNode(
                                                                source =
                                                                    Source
                                                                        .DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                                                                sourceValueRangeStart = 0f,
                                                                sourceValueRangeEnd = 3f,
                                                                sourceOutOfRangeBehavior =
                                                                    OutOfRange.REPEAT,
                                                            ),
                                                    )
                                                ),
                                            )
                                    )
                            ),
                        colorIntArgb = COLOR_AVOCADO_GREEN,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
                // Per-vertex opacity and HCL shift brush behaviors require CanvasMeshRenderer
                // (Canvas.drawMesh, Android API 34+) on Android.
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),

            // =========================================================================
            // 2. PARTICLE STROKES
            // =========================================================================
            createTestCase(
                name = "particle_stroke_solid",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(tip = BrushTip(particleGapDistanceScale = 2f)),
                        colorIntArgb = COLOR_RED,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
            ),
            createTestCase(
                name = "particle_stroke_translucent",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(tip = BrushTip(particleGapDistanceScale = 0.75f)),
                        colorIntArgb = COLOR_COBALT_BLUE_TRANSLUCENT,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
            ),
            createTestCase(
                name = "particle_stroke_tiling",
                brush =
                    tilingTextureBrush(
                        particleGapDistanceScale = 2f,
                        textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                        textureSize = 10f,
                    ),
            ),
            createTestCase(
                name = "particle_stroke_stamping",
                brush =
                    Brush.createWithColorIntArgb(
                        family =
                            BrushFamily(
                                tip = BrushTip(particleGapDistanceScale = 2f),
                                paint =
                                    BrushPaint(
                                        listOf(
                                            StampingTexture(
                                                clientTextureId = TEXTURE_ID_CHECKERBOARD
                                            )
                                        )
                                    ),
                            ),
                        colorIntArgb = COLOR_BLACK,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
                // StampingTexture without a tiling fallback requires Android API 34+
                // (CanvasMeshRenderer / Canvas.drawMesh).
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),

            // =========================================================================
            // 3. TEXTURE ORIGINS
            // =========================================================================
            createTestCase(
                name = "texture_origin_stroke_space_origin",
                brush =
                    tilingTextureBrush(
                        textureId = TEXTURE_ID_CIRCLE,
                        textureSize = 1f,
                        textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        textureOrigin = TilingTexture.Origin.STROKE_SPACE_ORIGIN,
                        textureOffsetX = 0.5f,
                        textureOffsetY = 0.5f,
                        brushSize = 25f,
                    ),
            ),
            createTestCase(
                name = "texture_origin_first_stroke_input",
                brush =
                    tilingTextureBrush(
                        textureId = TEXTURE_ID_CIRCLE,
                        textureSize = 1f,
                        textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                        textureOffsetX = 0.5f,
                        textureOffsetY = 0.5f,
                        brushSize = 25f,
                    ),
            ),
            createTestCase(
                name = "texture_origin_last_stroke_input",
                brush =
                    tilingTextureBrush(
                        textureId = TEXTURE_ID_CIRCLE,
                        textureSize = 1f,
                        textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        textureOrigin = TilingTexture.Origin.LAST_STROKE_INPUT,
                        textureOffsetX = 0.5f,
                        textureOffsetY = 0.5f,
                        brushSize = 25f,
                    ),
            ),

            // =========================================================================
            // 4. TEXTURE SIZE UNITS
            // =========================================================================
            createTestCase(
                name = "texture_size_unit_brush_size",
                brush =
                    tilingTextureBrush(
                        textureSize = 1f,
                        textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        brushSize = 15f,
                    ),
            ),
            createTestCase(
                name = "texture_size_unit_stroke_coordinates",
                brush =
                    tilingTextureBrush(
                        textureSize = 10f,
                        textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                        brushSize = 15f,
                    ),
            ),

            // =========================================================================
            // 5. TILING TEXTURE WRAP MODES
            // =========================================================================
            createTestCase(
                name = "tiling_texture_wrap_repeat_repeat",
                brush =
                    tilingTextureBrush(
                        textureSize = 1f,
                        textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                        textureWrapX = TextureLayer.Wrap.REPEAT,
                        textureWrapY = TextureLayer.Wrap.REPEAT,
                        brushSize = 25f,
                    ),
            ),
            createTestCase(
                name = "tiling_texture_wrap_mirror_mirror",
                brush =
                    tilingTextureBrush(
                        textureSize = 1f,
                        textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                        textureWrapX = TextureLayer.Wrap.MIRROR,
                        textureWrapY = TextureLayer.Wrap.MIRROR,
                        brushSize = 25f,
                    ),
            ),
            createTestCase(
                name = "tiling_texture_wrap_clamp_clamp",
                brush =
                    tilingTextureBrush(
                        textureSize = 1f,
                        textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                        textureWrapX = TextureLayer.Wrap.CLAMP,
                        textureWrapY = TextureLayer.Wrap.CLAMP,
                        brushSize = 25f,
                    ),
            ),
            createTestCase(
                name = "tiling_texture_wrap_repeat_mirror",
                brush =
                    tilingTextureBrush(
                        textureSize = 1f,
                        textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                        textureWrapX = TextureLayer.Wrap.REPEAT,
                        textureWrapY = TextureLayer.Wrap.MIRROR,
                        brushSize = 25f,
                    ),
            ),
            createTestCase(
                name = "tiling_texture_wrap_mirror_clamp",
                brush =
                    tilingTextureBrush(
                        textureSize = 1f,
                        textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                        textureWrapX = TextureLayer.Wrap.MIRROR,
                        textureWrapY = TextureLayer.Wrap.CLAMP,
                        brushSize = 25f,
                    ),
            ),
            createTestCase(
                name = "tiling_texture_wrap_clamp_repeat",
                brush =
                    tilingTextureBrush(
                        textureSize = 1f,
                        textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                        textureWrapX = TextureLayer.Wrap.CLAMP,
                        textureWrapY = TextureLayer.Wrap.REPEAT,
                        brushSize = 25f,
                    ),
            ),

            // =========================================================================
            // 6. BLEND MODES WITH BRUSH COLOR
            // Source = opaque poop-emoji texture on a transparent background;
            // Destination = 50%-alpha red brush color (COLOR_RED_HALF_ALPHA).
            // =========================================================================
            // Multiplies texture color and alpha by the 50%-alpha red brush color:
            // 50%-alpha red-tinted poop emojis inside the stroke, transparent between emojis.
            createTestCase(
                name = "blend_mode_color_modulate",
                brush = colorBlendedBrush(TextureLayer.BlendMode.MODULATE, COLOR_RED_HALF_ALPHA),
            ),
            // Replaces brush color and alpha with the texture directly:
            // full-opacity untinted poop emojis inside the stroke, transparent between emojis.
            createTestCase(
                name = "blend_mode_color_src",
                brush = colorBlendedBrush(TextureLayer.BlendMode.SRC, COLOR_RED_HALF_ALPHA),
            ),
            // Draws the texture atop the 50%-alpha red stroke while keeping destination alpha:
            // 50%-alpha untinted poop emojis over a 50%-alpha red stroke background.
            createTestCase(
                name = "blend_mode_color_src_atop",
                brush = colorBlendedBrush(TextureLayer.BlendMode.SRC_ATOP, COLOR_RED_HALF_ALPHA),
            ),
            // Scales texture color and alpha by destination alpha (50%):
            // 50%-alpha untinted poop emojis inside the stroke, transparent between emojis.
            createTestCase(
                name = "blend_mode_color_src_in",
                brush = colorBlendedBrush(TextureLayer.BlendMode.SRC_IN, COLOR_RED_HALF_ALPHA),
            ),
            // Uses texture alpha as a stencil/mask for the 50%-alpha red brush color:
            // flat 50%-alpha red poop-emoji silhouettes (no brown/white colors), transparent
            // elsewhere.
            createTestCase(
                name = "blend_mode_color_dst_in",
                brush = colorBlendedBrush(TextureLayer.BlendMode.DST_IN, COLOR_RED_HALF_ALPHA),
            ),
            // Punches out opaque texture pixels from the 50%-alpha red stroke:
            // 50%-alpha red stroke with transparent poop-emoji-shaped cutouts.
            createTestCase(
                name = "blend_mode_color_dst_out",
                brush = colorBlendedBrush(TextureLayer.BlendMode.DST_OUT, COLOR_RED_HALF_ALPHA),
            ),

            // =========================================================================
            // 7. BLEND MODES WITH TWO TEXTURES
            // Source (layer 1) = airplane-emoji texture on a transparent background;
            // Destination (layer 2) = poop-emoji texture on a transparent background,
            // modulated with opaque white brush color (COLOR_WHITE_FOR_TEXTURE).
            // =========================================================================
            // Replaces the destination texture with the source texture:
            // only airplane emojis inside the stroke, transparent between emojis.
            createTestCase(
                name = "blend_mode_two_textures_src",
                brush = twoTexturesBlendedBrush(TextureLayer.BlendMode.SRC),
            ),
            // Keeps only the destination texture and discards the source texture:
            // only poop emojis inside the stroke, transparent between emojis.
            createTestCase(
                name = "blend_mode_two_textures_dst",
                brush = twoTexturesBlendedBrush(TextureLayer.BlendMode.DST),
            ),
            // Draws the source texture in front of the destination texture:
            // airplane emojis drawn on top of poop emojis inside the stroke.
            createTestCase(
                name = "blend_mode_two_textures_src_over",
                brush = twoTexturesBlendedBrush(TextureLayer.BlendMode.SRC_OVER),
            ),
            // Draws the destination texture in front of the source texture:
            // poop emojis drawn on top of airplane emojis, with airplane tips peeking out behind.
            createTestCase(
                name = "blend_mode_two_textures_dst_over",
                brush = twoTexturesBlendedBrush(TextureLayer.BlendMode.DST_OVER),
            ),
            // Keeps only source pixels that do not overlap destination pixels:
            // airplane nose and wingtips visible outside the poop emoji, with poop-shaped cutouts.
            createTestCase(
                name = "blend_mode_two_textures_src_out",
                brush = twoTexturesBlendedBrush(TextureLayer.BlendMode.SRC_OUT),
            ),
            // Clips destination pixels to the source alpha and draws them over non-overlapping
            // source:
            // poop-emoji pixels inside the airplane silhouette, with airplane tips visible outside
            // poop.
            createTestCase(
                name = "blend_mode_two_textures_dst_atop",
                brush = twoTexturesBlendedBrush(TextureLayer.BlendMode.DST_ATOP),
            ),
            // Keeps non-overlapping pixels from both textures and punches out their intersection:
            // airplane tips and outer poop regions visible, with a transparent airplane cutout on
            // poop.
            createTestCase(
                name = "blend_mode_two_textures_xor",
                brush = twoTexturesBlendedBrush(TextureLayer.BlendMode.XOR),
            ),

            // =========================================================================
            // 8. TEXTURE OFFSET & ROTATION
            // =========================================================================
            createTestCase(
                name = "texture_offset",
                brush =
                    tilingTextureBrush(
                        textureSize = 30f,
                        textureOffsetX = 0.25f,
                        textureOffsetY = 0.25f,
                        textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                        brushSize = 30f,
                    ),
            ),
            createTestCase(
                name = "texture_rotation",
                brush =
                    tilingTextureBrush(
                        textureId = TEXTURE_ID_AIRPLANE_EMOJI,
                        textureSize = 30f,
                        textureRotationDegrees = 45f,
                        textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                        brushSize = 30f,
                    ),
            ),
            createTestCase(
                name = "texture_offset_and_rotation",
                brush =
                    tilingTextureBrush(
                        textureId = TEXTURE_ID_AIRPLANE_EMOJI,
                        textureSize = 30f,
                        textureOffsetX = 0.5f,
                        textureOffsetY = 0f,
                        textureRotationDegrees = 45f,
                        textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                        brushSize = 30f,
                    ),
            ),

            // =========================================================================
            // 9. SELF-OVERLAP MODES & PAINT PREFERENCES
            // =========================================================================
            createTestCase(
                name = "self_overlap_notexture_any",
                brush = textureSelfOverlapBrush(textureLayer = null, selfOverlap = SelfOverlap.ANY),
            ),
            createTestCase(
                name = "self_overlap_notexture_accumulate",
                brush =
                    textureSelfOverlapBrush(
                        textureLayer = null,
                        selfOverlap = SelfOverlap.ACCUMULATE,
                    ),
                // SelfOverlap.ACCUMULATE requires Android API 34+ (CanvasMeshRenderer /
                // Canvas.drawMesh).
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),
            createTestCase(
                name = "self_overlap_notexture_discard",
                brush =
                    textureSelfOverlapBrush(textureLayer = null, selfOverlap = SelfOverlap.DISCARD),
            ),
            createTestCase(
                name = "self_overlap_tiling_any",
                brush = tilingTextureSelfOverlapBrush(SelfOverlap.ANY),
            ),
            createTestCase(
                name = "self_overlap_tiling_accumulate",
                brush = tilingTextureSelfOverlapBrush(SelfOverlap.ACCUMULATE),
                // SelfOverlap.ACCUMULATE requires Android API 34+ (CanvasMeshRenderer /
                // Canvas.drawMesh).
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),
            createTestCase(
                name = "self_overlap_tiling_discard",
                brush = tilingTextureSelfOverlapBrush(SelfOverlap.DISCARD),
            ),
            createTestCase(
                name = "self_overlap_stamping_any",
                brush = stampingTextureSelfOverlapBrush(SelfOverlap.ANY),
                // StampingTexture without a tiling fallback requires Android API 34+
                // (CanvasMeshRenderer / Canvas.drawMesh).
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),
            createTestCase(
                name = "self_overlap_stamping_accumulate",
                brush = stampingTextureSelfOverlapBrush(SelfOverlap.ACCUMULATE),
                // StampingTexture and SelfOverlap.ACCUMULATE require Android API 34+
                // (CanvasMeshRenderer / Canvas.drawMesh).
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),
            createTestCase(
                name = "self_overlap_stamping_discard_with_tiling_fallback",
                brush = stampingDiscardWithTilingFallbackBrush(),
            ),

            // =========================================================================
            // 10. TRANSFORMATIONS
            // =========================================================================
            createTestCase(
                name = "transform_identity",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(paint = BrushPaint()),
                        colorIntArgb = COLOR_AVOCADO_GREEN,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
                transform = AffineTransform.IDENTITY,
            ),
            createTestCase(
                name = "transform_uniform_scale",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(paint = BrushPaint()),
                        colorIntArgb = COLOR_AVOCADO_GREEN,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
                transform = ImmutableAffineTransform.scale(2f),
            ),
            createTestCase(
                name = "transform_non_uniform_scale",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(paint = BrushPaint()),
                        colorIntArgb = COLOR_AVOCADO_GREEN,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
                transform = ImmutableAffineTransform.scale(0.5f, 2f),
            ),
            createTestCase(
                name = "transform_rotation",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(paint = BrushPaint()),
                        colorIntArgb = COLOR_AVOCADO_GREEN,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
                transform = ImmutableAffineTransform.rotateDegrees(45f),
            ),
            createTestCase(
                name = "transform_skew",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(paint = BrushPaint()),
                        colorIntArgb = COLOR_AVOCADO_GREEN,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
                transform = ImmutableAffineTransform.skewX(0.5f),
            ),

            // =========================================================================
            // 11. BRUSH PAINT ANIMATION
            // Animated StampingTexture rendering on Android requires CanvasMeshRenderer
            // (Canvas.drawMesh, Android API 34+) and is skipped on older Android SDKs.
            // =========================================================================
            createTestCase(
                name = "paint_animation_restart_progress_0",
                brush =
                    animatedStampingBrush(
                        animationRepeatMode = AnimationRepeatMode.RESTART,
                        withParticleOffset = false,
                    ),
                animatorClockStateMillis = 0L,
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),
            createTestCase(
                name = "paint_animation_restart_progress_mid",
                brush =
                    animatedStampingBrush(
                        animationRepeatMode = AnimationRepeatMode.RESTART,
                        withParticleOffset = false,
                    ),
                animatorClockStateMillis = 600L,
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),
            createTestCase(
                name = "paint_animation_restart_with_particle_offset",
                brush =
                    animatedStampingBrush(
                        animationRepeatMode = AnimationRepeatMode.RESTART,
                        withParticleOffset = true,
                    ),
                animatorClockStateMillis = 250L,
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),
            createTestCase(
                name = "paint_animation_reverse_with_particle_offset",
                brush =
                    animatedStampingBrush(
                        animationRepeatMode = AnimationRepeatMode.REVERSE,
                        withParticleOffset = true,
                    ),
                animatorClockStateMillis = 250L,
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),
        )
    }

    private fun createTestCase(
        name: String,
        brush: Brush,
        inputs: StrokeInputBatch = cursiveHelloInputs,
        transform: AffineTransform = AffineTransform.IDENTITY,
        animatorClockStateMillis: Long = 0L,
        minAndroidSdk: Int = 21,
    ): TestCase {
        val wetStroke =
            InProgressStroke().apply {
                start(brush)
                enqueueInputs(inputs, ImmutableStrokeInputBatch.EMPTY)
                finishInput()
                updateShape(inputs.getDurationMillis())
            }
        val dryStroke = wetStroke.toImmutable()
        return TestCase(
            name = name,
            dryStroke = dryStroke,
            wetStroke = wetStroke,
            transform = transform,
            animatorClockStateMillis = animatorClockStateMillis,
            minAndroidSdk = minAndroidSdk,
        )
    }

    private fun tilingTextureBrush(
        particleGapDistanceScale: Float = 0f,
        textureId: String = TEXTURE_ID_CHECKERBOARD,
        textureSizeUnit: TextureLayer.SizeUnit,
        textureSize: Float,
        textureOrigin: TilingTexture.Origin = TilingTexture.Origin.STROKE_SPACE_ORIGIN,
        textureOffsetX: Float = 0f,
        textureOffsetY: Float = 0f,
        textureRotationDegrees: Float = 0f,
        textureWrapX: TextureLayer.Wrap = TextureLayer.Wrap.REPEAT,
        textureWrapY: TextureLayer.Wrap = TextureLayer.Wrap.REPEAT,
        @ColorInt brushColor: Int = COLOR_BLACK,
        brushSize: Float = 15f,
    ): Brush {
        val tip = BrushTip(particleGapDistanceScale = particleGapDistanceScale)
        val paint =
            tilingTextureBrushPaint(
                textureId = textureId,
                textureWrapX = textureWrapX,
                textureWrapY = textureWrapY,
                textureSizeUnit = textureSizeUnit,
                textureSize = textureSize,
                textureOrigin = textureOrigin,
                textureOffsetX = textureOffsetX,
                textureOffsetY = textureOffsetY,
                textureRotationDegrees = textureRotationDegrees,
            )
        return Brush.createWithColorIntArgb(
            family = BrushFamily(tip = tip, paint = paint),
            colorIntArgb = brushColor,
            size = brushSize,
            epsilon = 0.1f,
        )
    }

    private fun tilingTextureBrushPaint(
        textureId: String = TEXTURE_ID_CHECKERBOARD,
        textureSizeUnit: TextureLayer.SizeUnit,
        textureSize: Float,
        textureOrigin: TilingTexture.Origin = TilingTexture.Origin.STROKE_SPACE_ORIGIN,
        textureOffsetX: Float = 0f,
        textureOffsetY: Float = 0f,
        textureRotationDegrees: Float = 0f,
        textureWrapX: TextureLayer.Wrap = TextureLayer.Wrap.REPEAT,
        textureWrapY: TextureLayer.Wrap = TextureLayer.Wrap.REPEAT,
    ): BrushPaint {
        val textureLayer =
            TilingTexture(
                clientTextureId = textureId,
                sizeX = textureSize,
                sizeY = textureSize,
                offsetX = textureOffsetX,
                offsetY = textureOffsetY,
                rotationDegrees = textureRotationDegrees,
                sizeUnit = textureSizeUnit,
                origin = textureOrigin,
                wrapX = textureWrapX,
                wrapY = textureWrapY,
            )
        return BrushPaint(listOf(textureLayer))
    }

    private fun colorBlendedBrush(blendMode: TextureLayer.BlendMode, @ColorInt color: Int): Brush {
        val textureLayer =
            TilingTexture(
                clientTextureId = TEXTURE_ID_POOP_EMOJI,
                sizeX = 1f,
                sizeY = 1f,
                sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                blendMode = blendMode,
            )
        val paint = BrushPaint(listOf(textureLayer))
        return Brush.createWithColorIntArgb(
            family = BrushFamily(paint = paint),
            colorIntArgb = color,
            size = 20f,
            epsilon = 0.1f,
        )
    }

    private fun twoTexturesBlendedBrush(blendMode: TextureLayer.BlendMode): Brush {
        val textureLayer1 =
            TilingTexture(
                clientTextureId = TEXTURE_ID_AIRPLANE_EMOJI,
                sizeX = 1f,
                sizeY = 1f,
                sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                blendMode = blendMode,
            )
        val textureLayer2 =
            TilingTexture(
                clientTextureId = TEXTURE_ID_POOP_EMOJI,
                sizeX = 1f,
                sizeY = 1f,
                sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
            )
        val paint = BrushPaint(listOf(textureLayer1, textureLayer2))
        return Brush.createWithColorIntArgb(
            family = BrushFamily(paint = paint),
            colorIntArgb = COLOR_WHITE_FOR_TEXTURE,
            size = 25f,
            epsilon = 0.1f,
        )
    }

    private fun tilingTextureSelfOverlapBrush(selfOverlap: SelfOverlap): Brush =
        textureSelfOverlapBrush(
            textureLayer =
                TilingTexture(
                    clientTextureId = TEXTURE_ID_POOP_EMOJI,
                    sizeX = 1f,
                    sizeY = 1f,
                    sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                ),
            selfOverlap = selfOverlap,
        )

    private fun stampingTextureSelfOverlapBrush(selfOverlap: SelfOverlap): Brush =
        textureSelfOverlapBrush(
            textureLayer = StampingTexture(clientTextureId = TEXTURE_ID_POOP_EMOJI),
            selfOverlap = selfOverlap,
        )

    private fun stampingDiscardWithTilingFallbackBrush(): Brush {
        val stampingDiscardPaint =
            BrushPaint(
                textureLayers = listOf(StampingTexture(clientTextureId = TEXTURE_ID_POOP_EMOJI)),
                selfOverlap = SelfOverlap.DISCARD,
            )
        val tilingDiscardPaint =
            BrushPaint(
                textureLayers =
                    listOf(
                        TilingTexture(
                            clientTextureId = TEXTURE_ID_POOP_EMOJI,
                            sizeX = 1f,
                            sizeY = 1f,
                            sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                        )
                    ),
                selfOverlap = SelfOverlap.DISCARD,
            )
        val coat =
            BrushCoat(
                tip = selfOverlapBrushTip(),
                paintPreferences = listOf(stampingDiscardPaint, tilingDiscardPaint),
            )
        return Brush.createWithColorIntArgb(
            family = BrushFamily(coats = listOf(coat)),
            colorIntArgb = COLOR_TRANSLUCENT_GREEN,
            size = 15f,
            epsilon = 0.1f,
        )
    }

    private fun textureSelfOverlapBrush(
        textureLayer: TextureLayer?,
        selfOverlap: SelfOverlap,
    ): Brush {
        val textureLayers = listOfNotNull(textureLayer)
        val paint = BrushPaint(textureLayers = textureLayers, selfOverlap = selfOverlap)
        return Brush.createWithColorIntArgb(
            family = BrushFamily(tip = selfOverlapBrushTip(), paint = paint),
            colorIntArgb = COLOR_TRANSLUCENT_GREEN,
            size = 15f,
            epsilon = 0.1f,
        )
    }

    private fun selfOverlapBrushTip(): BrushTip =
        BrushTip(
            cornerRounding = 0.2f,
            particleGapDistanceScale = 1.5f,
            behaviors =
                listOf(
                    BrushBehavior(
                        terminalNodes =
                            listOf(
                                TargetNode(
                                    target = Target.ROTATION_OFFSET_IN_RADIANS,
                                    targetModifierRangeStart = -PI.toFloat(),
                                    targetModifierRangeEnd = PI.toFloat(),
                                    input =
                                        SourceNode(
                                            source = Source.DIRECTION_ABOUT_ZERO_IN_RADIANS,
                                            sourceValueRangeStart = -PI.toFloat(),
                                            sourceValueRangeEnd = PI.toFloat(),
                                            sourceOutOfRangeBehavior = OutOfRange.REPEAT,
                                        ),
                                )
                            )
                    )
                ),
        )

    private fun animatedStampingBrush(
        animationRepeatMode: AnimationRepeatMode,
        withParticleOffset: Boolean,
    ): Brush {
        val behaviors =
            if (withParticleOffset) {
                listOf(
                    BrushBehavior(
                        TargetNode(
                            target = Target.PAINT_ANIMATION_PROGRESS_OFFSET,
                            targetModifierRangeStart = 0f,
                            targetModifierRangeEnd = 1f,
                            input =
                                SourceNode(
                                    source = Source.DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                                    sourceValueRangeStart = 0f,
                                    sourceValueRangeEnd = 4f,
                                    sourceOutOfRangeBehavior = OutOfRange.REPEAT,
                                ),
                        )
                    )
                )
            } else {
                emptyList()
            }
        val tip = BrushTip(particleGapDistanceScale = 1.5f, behaviors = behaviors)
        val paint =
            BrushPaint(
                listOf(
                    StampingTexture(
                        clientTextureId = TEXTURE_ID_NUMBERS_32X32_60FRAMES,
                        animationFrames = 60,
                        animationRows = 60,
                        animationColumns = 1,
                        animationDurationMillis = 1000L,
                        animationRepeatMode = animationRepeatMode,
                    )
                )
            )
        return Brush.createWithColorIntArgb(
            family = BrushFamily(tip = tip, paint = paint),
            colorIntArgb = COLOR_WHITE_FOR_TEXTURE,
            size = 20f,
            epsilon = 0.1f,
        )
    }

    /** Returns whether the current platform/SDK environment supports rendering [testCase]. */
    protected open fun isSupported(testCase: TestCase): Boolean = true

    /**
     * Renders a dry [stroke] and compares the resulting image against [goldenName].
     *
     * The resulting image must have the given [imageWidth] and [imageHeight] dimensions, with a
     * completely transparent background (0x00000000).
     *
     * Lazy image comparison is encouraged for runtime efficiency and the human efficiency of
     * reviewing and approving golden image updates. Make sure to assert that the lazy asserts pass
     * in [assertLazyAssertsPass].
     */
    protected abstract fun renderAndCompareToGolden(
        stroke: Stroke,
        transform: AffineTransform,
        imageWidth: Int,
        imageHeight: Int,
        goldenName: String,
        animatorClockStateMillis: Long = 0L,
    )

    /**
     * Renders a wet [inProgressStroke] and compares the resulting image against [goldenName].
     *
     * The resulting image must have the given [imageWidth] and [imageHeight] dimensions, with a
     * completely transparent background (0x00000000).
     *
     * Lazy image comparison is encouraged for runtime efficiency and the human efficiency of
     * reviewing and approving golden image updates. Make sure to assert that the lazy asserts pass
     * in [assertLazyAssertsPass].
     */
    protected abstract fun renderAndCompareToGolden(
        inProgressStroke: InProgressStroke,
        transform: AffineTransform,
        imageWidth: Int,
        imageHeight: Int,
        goldenName: String,
        animatorClockStateMillis: Long = 0L,
    )

    /** Validates any lazy assertions made during [renderAndCompareToGolden]. */
    protected abstract fun assertLazyAssertsPass()

    @Test
    fun dryRendering_matchesExpectedGolden() {
        for (testCase in testCases) {
            if (!isSupported(testCase)) continue
            val params = computeRenderParams(testCase)
            renderAndCompareToGolden(
                stroke = testCase.dryStroke,
                transform = params.transform,
                imageWidth = params.width,
                imageHeight = params.height,
                goldenName = "${testCase.name}_dry",
                animatorClockStateMillis = testCase.animatorClockStateMillis,
            )
        }
    }

    @Test
    fun wetRendering_matchesExpectedGolden() {
        for (testCase in testCases) {
            if (!isSupported(testCase)) continue
            val params = computeRenderParams(testCase)
            renderAndCompareToGolden(
                inProgressStroke = testCase.wetStroke,
                transform = params.transform,
                imageWidth = params.width,
                imageHeight = params.height,
                goldenName = "${testCase.name}_wet",
                animatorClockStateMillis = testCase.animatorClockStateMillis,
            )
        }
    }

    @AfterTest
    fun tearDown() {
        assertLazyAssertsPass()
    }

    protected class RenderParams(val transform: AffineTransform, val width: Int, val height: Int)

    protected fun computeRenderParams(testCase: TestCase): RenderParams {
        val rawBox =
            checkNotNull(testCase.dryStroke.shape.computeBoundingBox()) {
                "Stroke bounding box must not be null"
            }
        val box = testCase.transform.applyTransform(rawBox).computeBoundingBox()
        val padding = 10f
        val width = kotlin.math.ceil(box.xMax - box.xMin + 2f * padding).toInt()
        val height = kotlin.math.ceil(box.yMax - box.yMin + 2f * padding).toInt()
        val translation =
            ImmutableAffineTransform(1f, 0f, padding - box.xMin, 0f, 1f, padding - box.yMin)
        val transform =
            MutableAffineTransform()
                .also { AffineTransform.multiply(translation, testCase.transform, it) }
                .toImmutable()
        return RenderParams(transform, width, height)
    }

    protected companion object {
        const val TEXTURE_ID_CHECKERBOARD = "checkerboard"
        const val TEXTURE_ID_CIRCLE = "circle"
        const val TEXTURE_ID_AIRPLANE_EMOJI = "airplane_emoji"
        const val TEXTURE_ID_POOP_EMOJI = "poop_emoji"
        const val TEXTURE_ID_NUMBERS_32X32_60FRAMES = "numbers_32x32_60frames"

        private const val ANDROID_MESH_RENDERER_MIN_SDK = 34
        @ColorInt private const val COLOR_BLACK = 0xFF290E1C.toInt()
        @ColorInt private const val COLOR_RED = 0xFFF7251E.toInt()
        @ColorInt private const val COLOR_RED_HALF_ALPHA = 0x80F7251E.toInt()
        @ColorInt private const val COLOR_AVOCADO_GREEN = 0xFF558B2F.toInt()
        @ColorInt private const val COLOR_LIGHT_BLUE = 0xFF4FB5E8.toInt()
        @ColorInt private const val COLOR_COBALT_BLUE_TRANSLUCENT = 0x6601579B
        @ColorInt private const val COLOR_TRANSLUCENT_GREEN = 0x7733FC66
        @ColorInt private const val COLOR_WHITE_FOR_TEXTURE = 0xFFFFFFFF.toInt()
    }
}
