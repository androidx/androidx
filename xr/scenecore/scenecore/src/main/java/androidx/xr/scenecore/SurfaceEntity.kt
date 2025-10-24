/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore

import android.annotation.SuppressLint
import android.view.Surface
import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SurfaceEntity as RtSurfaceEntity

/**
 * SurfaceEntity is an [Entity] that hosts a [Surface], which will be texture mapped onto the
 * [Shape]. If a stereoscopic [StereoMode] is specified, then the User will see left and right eye
 * content mapped to the appropriate display.
 *
 * Note that it is not currently possible to synchronize [Shape] and [StereoMode] changes with
 * application rendering or video decoding. Applications are advised to carefully hide this entity
 * around state transitions (for example in response to video events) to manage glitchiness.
 *
 * @property shape The [Shape] which describes the mesh to which the Surface is mapped.
 * @property stereoMode The [StereoMode] which describes how parts of the surface are displayed to
 *   the user's eyes.
 * @property dimensions The dimensions of the canvas in the local spatial coordinate system of the
 *   entity.
 * @property edgeFeatheringParams The [EdgeFeatheringParams] which describes the edge fading effects
 *   for the surface.
 */
public class SurfaceEntity
private constructor(
    private val lifecycleManager: LifecycleManager,
    rtEntity: RtSurfaceEntity,
    entityManager: EntityManager,
    shape: Shape,
) : BaseEntity<RtSurfaceEntity>(rtEntity, entityManager) {

    /** Represents the shape of the Canvas that backs a SurfaceEntity. */
    public interface Shape {

        /**
         * A Quadrilateral-shaped canvas. Width and height are expressed in the X and Y axis in the
         * local spatial coordinate system of the entity. (0,0) is the center of the Quad mesh; the
         * lower-left corner of the Quad is at (-width/2, -height/2).
         *
         * @property extents The size of the Quad in the local spatial coordinate system of the
         *   entity.
         */
        public class Quad(public val extents: FloatSize2d) : Shape {}

        /**
         * cal An inwards-facing sphere-shaped mesh, centered at (0,0,0) in the local coordinate
         * space. This is intended to be used by setting the entity's pose to the user's head pose.
         * Radius is represented in the local spatial coordinate system of the entity. The center of
         * the Surface will be mapped to (0, 0, -radius) in the local coordinate space, and texture
         * coordinate UVs are applied from positive X to negative X in an equirectangular
         * projection. This means that if this Entity is set to the user's head pose, then they will
         * be looking at the center of the video if it were viewed in a 2D player.
         *
         * @property radius The radius of the sphere in the local spatial coordinate system of the
         *   entity.
         */
        public class Sphere(public val radius: Float) : Shape {}

        /**
         * An inwards-facing hemisphere-shaped canvas, where (0,0,0) is the center of the base of
         * the hemisphere. Radius is represented in the local spatial coordinate system of the
         * entity. This is intended to be used by setting the entity's pose to the user's head pose.
         * The center of the Surface will be mapped to (0, 0, -radius) in the local coordinate
         * space, and texture coordinate UV's are applied from positive X to negative X in an
         * equirectangular projection.
         *
         * @property radius The radius of the hemisphere in the local spatial coordinate system of
         *   the entity.
         */
        public class Hemisphere(public val radius: Float) : Shape {}
    }

    /** Represents edge fading effects for a SurfaceEntity. */
    public abstract class EdgeFeatheringParams private constructor() {
        /**
         * @property leftRight a [Float] which controls the shape-relative radius of the edge
         *   fadeout on the left and right edges of the SurfaceEntity canvas.
         * @property topBottom a [Float] which controls the shape-relative radius of the edge
         *   fadeout on the top and bottom edges of the SurfaceEntity canvas.
         *
         * A radius of 0.05 represents 5% of the width of the visible canvas surface. Please note
         * that this is scaled by the aspect ratio of Quad-shaped canvases.
         *
         * Applications are encouraged to use [NoFeathering] on Spherical canvases. The behavior is
         * only defined for values between [0.0f - 0.5f]. Default values are 0.0f.
         */
        public class RectangleFeather(
            @FloatRange(from = 0.0, to = 0.5) public val leftRight: Float = 0.0f,
            @FloatRange(from = 0.0, to = 0.5) public val topBottom: Float = 0.0f,
        ) : EdgeFeatheringParams() {}

        /** Applies no edge fading to any canvas. */
        public class NoFeathering : EdgeFeatheringParams() {}
    }

    /**
     * Specifies whether the [Surface] which backs this [Entity] should be backed by
     * [android.hardware.HardwareBuffer]s with the USAGE_PROTECTED_CONTENT flag set. These buffers
     * support hardware paths for decoding protected content.
     *
     * @see https://developer.android.com/reference/android/media/MediaDrm for more details.
     */
    public class SurfaceProtection private constructor(private val name: String) {
        public companion object {
            /**
             * The Surface content is not protected. Non-protected content can be decoded into this
             * surface. Protected content can not be decoded into this Surface. Screen captures of
             * the SurfaceEntity will show the Surface content.
             */
            @JvmField public val NONE: SurfaceProtection = SurfaceProtection("NONE")

            /**
             * The Surface content is protected. Non-protected content can be decoded into this
             * surface. Protected content can be decoded into this Surface. Screen captures of the
             * SurfaceEntity will redact the Surface content.
             */
            @JvmField public val PROTECTED: SurfaceProtection = SurfaceProtection("PROTECTED")
        }

        override fun toString(): String = name
    }

    /**
     * Specifies whether super sampling should be enabled for this surface. Super sampling can
     * improve text clarity at a performance cost.
     */
    public class SuperSampling private constructor(private val name: String) {
        public companion object {

            /** Super sampling is disabled. */
            @JvmField public val NONE: SuperSampling = SuperSampling("NONE")

            /**
             * Super sampling is enabled with a default sampling pattern. This is the value that is
             * set if SuperSampling is not specified when the Entity is created.
             */
            @JvmField public val PENTAGON: SuperSampling = SuperSampling("PENTAGON")
        }

        override fun toString(): String = name
    }

    /**
     * Specifies how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what they provided here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     *
     * Values here match values from [androidx.media3.common.C.StereoMode].
     *
     * @see https://developer.android.com/reference/androidx/media3/common/C.StereoMode
     */
    public class StereoMode private constructor(private val name: String) {
        public companion object {

            /** Each eye will see the entire surface (no separation) */
            @JvmField public val MONO: StereoMode = StereoMode("MONO")

            /** The [top, bottom] halves of the surface will map to [left, right] eyes */
            @JvmField public val TOP_BOTTOM: StereoMode = StereoMode("TOP_BOTTOM")

            /** The [left, right] halves of the surface will map to [left, right] eyes */
            @JvmField public val SIDE_BY_SIDE: StereoMode = StereoMode("SIDE_BY_SIDE")

            /** Multiview video, [primary, auxiliary] views will map to [left, right] eyes */
            @JvmField
            public val MULTIVIEW_LEFT_PRIMARY: StereoMode = StereoMode("MULTIVIEW_LEFT_PRIMARY")

            /** Multiview video, [primary, auxiliary] views will map to [right, left] eyes */
            @JvmField
            public val MULTIVIEW_RIGHT_PRIMARY: StereoMode = StereoMode("MULTIVIEW_RIGHT_PRIMARY")
        }

        override fun toString(): String = name
    }

    /**
     * Color information for the content drawn on a surface. This is used to hint to the system how
     * the content should be rendered depending on display settings.
     *
     * @property colorSpace The color space of the content.
     * @property colorTransfer The transfer function to apply to the content.
     * @property colorRange The color range of the content.
     * @property maxContentLightLevel The maximum brightness of the content (in nits).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public class ContentColorMetadata(
        public val colorSpace: ColorSpace = ColorSpace.BT709,
        public val colorTransfer: ColorTransfer = ColorTransfer.SRGB,
        public val colorRange: ColorRange = ColorRange.FULL,
        public val maxContentLightLevel: Int = Companion.MAX_CONTENT_LIGHT_LEVEL_UNKNOWN,
    ) {

        /**
         * Specifies the color space of the media asset drawn on the surface.
         *
         * These values are a superset of androidx.media3.common.C.ColorSpace.
         */
        public class ColorSpace private constructor(private val name: String) {
            public companion object {
                /** Please see androidx.media3.common.C.COLOR_SPACE_BT709 (1) */
                @JvmField public val BT709: ColorSpace = ColorSpace("BT709")

                /** Please see androidx.media3.common.C.COLOR_SPACE_BT601 (2) */
                @JvmField public val BT601_PAL: ColorSpace = ColorSpace("BT601_PAL")

                /** Please see androidx.media3.common.C.COLOR_SPACE_BT2020 (6) */
                @JvmField public val BT2020: ColorSpace = ColorSpace("BT2020")

                /** Please see ADataSpace::ADATASPACE_BT601_525 (0xf0) */
                @JvmField public val BT601_525: ColorSpace = ColorSpace("BT601_525")

                /** Please see ADataSpace::ADATASPACE_DISPLAY_P3 (0xf1) */
                @JvmField public val DISPLAY_P3: ColorSpace = ColorSpace("DISPLAY_P3")

                /** Please see ADataSpace::ADATASPACE_DCI_P3 (0xf2) */
                @JvmField public val DCI_P3: ColorSpace = ColorSpace("DCI_P3")

                /** Please see ADataSpace::ADATASPACE_ADOBE_RGB (0xf3) */
                @JvmField public val ADOBE_RGB: ColorSpace = ColorSpace("ADOBE_RGB")
            }

            override fun toString(): String = name
        }

        /**
         * Specifies the color transfer function of the media asset drawn on the surface.
         *
         * Enum members cover the transfer functions available in android::ADataSpace Enum values
         * match values from androidx.media3.common.C.ColorTransfer.
         */
        public class ColorTransfer private constructor(private val name: String) {
            public companion object {

                /** Linear transfer characteristic curve. */
                @JvmField public val LINEAR: ColorTransfer = ColorTransfer("LINEAR")

                /**
                 * The standard RGB transfer function, used for some SDR use-cases like image input.
                 */
                @JvmField public val SRGB: ColorTransfer = ColorTransfer("SRGB")

                /**
                 * SMPTE 170M transfer characteristic curve used by BT.601/BT.709/BT.2020. This is
                 * the curve used by most non-HDR video content.
                 */
                @JvmField public val SDR: ColorTransfer = ColorTransfer("SDR")

                /**
                 * The Gamma 2.2 transfer function, used for some SDR use-cases like tone-mapping.
                 */
                @JvmField public val GAMMA_2_2: ColorTransfer = ColorTransfer("GAMMA_2_2")

                /** SMPTE ST 2084 transfer function. This is used by some HDR video content. */
                @JvmField public val ST2084: ColorTransfer = ColorTransfer("ST2084")

                /**
                 * ARIB STD-B67 hybrid-log-gamma transfer function. This is used by some HDR video
                 * content.
                 */
                @JvmField public val HLG: ColorTransfer = ColorTransfer("HLG")
            }

            override fun toString(): String = name
        }

        /**
         * Specifies the color range of the media asset drawn on the surface.
         *
         * Enum values match values from androidx.media3.common.C.ColorRange.
         */
        public class ColorRange private constructor(private val name: String) {
            public companion object {
                /** Please see android.media.MediaFormat.COLOR_RANGE_FULL */
                @JvmField public val FULL: ColorRange = ColorRange("FULL")

                /** Please see android.media.MedaiFormat.COLOR_RANGE_LIMITED */
                @JvmField public val LIMITED: ColorRange = ColorRange("LIMITED")
            }

            override fun toString(): String = name
        }

        public companion object {
            /**
             * Represents an unknown maximum content light level.
             *
             * Note that the smallest value for this is 1 nit, so this value should only be used if
             * the actual value is unknown or if the content is constant luminance.
             */
            public const val MAX_CONTENT_LIGHT_LEVEL_UNKNOWN: Int = 0

            /**
             * A Default (unset) value for ContentColorMetadata. Setting this will cause the system
             * to render the content according to values set on the underlying [HardwareBuffer]s;
             * these are usually set correctly by the MediaCodec.
             */
            public val DEFAULT_UNSET_CONTENT_COLOR_METADATA: ContentColorMetadata =
                ContentColorMetadata(
                    colorSpace = ColorSpace.BT709,
                    colorTransfer = ColorTransfer.SRGB,
                    colorRange = ColorRange.FULL,
                    maxContentLightLevel = MAX_CONTENT_LIGHT_LEVEL_UNKNOWN,
                )

            internal fun getRtColorSpace(colorSpace: ColorSpace): Int {
                return when (colorSpace) {
                    ColorSpace.BT709 -> RtSurfaceEntity.ColorSpace.BT709
                    ColorSpace.BT601_PAL -> RtSurfaceEntity.ColorSpace.BT601_PAL
                    ColorSpace.BT2020 -> RtSurfaceEntity.ColorSpace.BT2020
                    ColorSpace.BT601_525 -> RtSurfaceEntity.ColorSpace.BT601_525
                    ColorSpace.DISPLAY_P3 -> RtSurfaceEntity.ColorSpace.DISPLAY_P3
                    ColorSpace.DCI_P3 -> RtSurfaceEntity.ColorSpace.DCI_P3
                    ColorSpace.ADOBE_RGB -> RtSurfaceEntity.ColorSpace.ADOBE_RGB
                    else -> RtSurfaceEntity.ColorSpace.BT709
                }
            }

            internal fun getColorSpaceFromRt(colorSpace: Int): ColorSpace {
                return when (colorSpace) {
                    RtSurfaceEntity.ColorSpace.BT709 -> ColorSpace.BT709
                    RtSurfaceEntity.ColorSpace.BT601_PAL -> ColorSpace.BT601_PAL
                    RtSurfaceEntity.ColorSpace.BT2020 -> ColorSpace.BT2020
                    RtSurfaceEntity.ColorSpace.BT601_525 -> ColorSpace.BT601_525
                    RtSurfaceEntity.ColorSpace.DISPLAY_P3 -> ColorSpace.DISPLAY_P3
                    RtSurfaceEntity.ColorSpace.DCI_P3 -> ColorSpace.DCI_P3
                    RtSurfaceEntity.ColorSpace.ADOBE_RGB -> ColorSpace.ADOBE_RGB
                    else -> ColorSpace.BT709
                }
            }

            internal fun getRtColorTransfer(colorTransfer: ColorTransfer): Int {
                return when (colorTransfer) {
                    ColorTransfer.LINEAR -> RtSurfaceEntity.ColorTransfer.LINEAR
                    ColorTransfer.SRGB -> RtSurfaceEntity.ColorTransfer.SRGB
                    ColorTransfer.SDR -> RtSurfaceEntity.ColorTransfer.SDR
                    ColorTransfer.GAMMA_2_2 -> RtSurfaceEntity.ColorTransfer.GAMMA_2_2
                    ColorTransfer.ST2084 -> RtSurfaceEntity.ColorTransfer.ST2084
                    ColorTransfer.HLG -> RtSurfaceEntity.ColorTransfer.HLG
                    else -> RtSurfaceEntity.ColorTransfer.SRGB
                }
            }

            internal fun getColorTransferFromRt(colorTransfer: Int): ColorTransfer {
                return when (colorTransfer) {
                    RtSurfaceEntity.ColorTransfer.LINEAR -> ColorTransfer.LINEAR
                    RtSurfaceEntity.ColorTransfer.SRGB -> ColorTransfer.SRGB
                    RtSurfaceEntity.ColorTransfer.SDR -> ColorTransfer.SDR
                    RtSurfaceEntity.ColorTransfer.GAMMA_2_2 -> ColorTransfer.GAMMA_2_2
                    RtSurfaceEntity.ColorTransfer.ST2084 -> ColorTransfer.ST2084
                    RtSurfaceEntity.ColorTransfer.HLG -> ColorTransfer.HLG
                    else -> ColorTransfer.SRGB
                }
            }

            internal fun getRtColorRange(colorRange: ColorRange): Int {
                return when (colorRange) {
                    ColorRange.FULL -> RtSurfaceEntity.ColorRange.FULL
                    ColorRange.LIMITED -> RtSurfaceEntity.ColorRange.LIMITED
                    else -> RtSurfaceEntity.ColorRange.FULL
                }
            }

            internal fun getColorRangeFromRt(colorRange: Int): ColorRange {
                return when (colorRange) {
                    RtSurfaceEntity.ColorRange.FULL -> ColorRange.FULL
                    RtSurfaceEntity.ColorRange.LIMITED -> ColorRange.LIMITED
                    else -> ColorRange.FULL
                }
            }
        }
    }

    public companion object {
        private fun getRtStereoMode(stereoMode: StereoMode): Int {
            return when (stereoMode) {
                StereoMode.MONO -> RtSurfaceEntity.StereoMode.MONO
                StereoMode.TOP_BOTTOM -> RtSurfaceEntity.StereoMode.TOP_BOTTOM
                StereoMode.MULTIVIEW_LEFT_PRIMARY ->
                    RtSurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY
                StereoMode.MULTIVIEW_RIGHT_PRIMARY ->
                    RtSurfaceEntity.StereoMode.MULTIVIEW_RIGHT_PRIMARY
                else -> RtSurfaceEntity.StereoMode.SIDE_BY_SIDE
            }
        }

        private fun getStereoModeFromRt(stereoMode: Int): StereoMode {
            return when (stereoMode) {
                RtSurfaceEntity.StereoMode.MONO -> StereoMode.MONO
                RtSurfaceEntity.StereoMode.TOP_BOTTOM -> StereoMode.TOP_BOTTOM
                RtSurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY ->
                    StereoMode.MULTIVIEW_LEFT_PRIMARY
                RtSurfaceEntity.StereoMode.MULTIVIEW_RIGHT_PRIMARY ->
                    StereoMode.MULTIVIEW_RIGHT_PRIMARY
                else -> StereoMode.SIDE_BY_SIDE
            }
        }

        private fun getRtSurfaceProtection(surfaceProtection: SurfaceProtection): Int {
            return when (surfaceProtection) {
                SurfaceProtection.NONE -> RtSurfaceEntity.SurfaceProtection.NONE
                SurfaceProtection.PROTECTED -> RtSurfaceEntity.SurfaceProtection.PROTECTED
                else -> RtSurfaceEntity.SurfaceProtection.NONE
            }
        }

        private fun getRtSuperSampling(superSampling: SuperSampling): Int {
            return when (superSampling) {
                SuperSampling.NONE -> RtSurfaceEntity.SuperSampling.NONE
                SuperSampling.PENTAGON -> RtSurfaceEntity.SuperSampling.DEFAULT
                else -> RtSurfaceEntity.SuperSampling.DEFAULT
            }
        }

        /**
         * Factory method for SurfaceEntity.
         *
         * @param lifecycleManager A SceneCore LifecycleManager
         * @param sceneRuntime SceneRuntime to use.
         * @param renderingRuntime RenderingRuntime to use.
         * @param entityManager A SceneCore EntityManager
         * @param stereoMode An [Int] which defines how surface subregions map to eyes
         * @param pose Pose for this StereoSurface entity, relative to its parent.
         * @param shape The [Shape] which describes the spatialized shape of the canvas.
         * @param surfaceProtection The Int member of [SurfaceProtection] which describes whether
         *   DRM is enabled for the surface - which will create protected hardware buffers for
         *   presentation.
         * @param contentColorMetadata The [ContentColorMetadata] of the content (nullable).
         * @param superSampling The [SuperSampling] which describes whether super sampling is
         *   enabled for the surface.
         * @return a SurfaceEntity instance
         */
        internal fun create(
            lifecycleManager: LifecycleManager,
            sceneRuntime: SceneRuntime,
            renderingRuntime: RenderingRuntime,
            entityManager: EntityManager,
            stereoMode: StereoMode = StereoMode.MONO,
            pose: Pose = Pose.Identity,
            shape: Shape = Shape.Quad(FloatSize2d(1.0f, 1.0f)),
            surfaceProtection: SurfaceProtection = SurfaceProtection.NONE,
            contentColorMetadata: ContentColorMetadata? = null,
            superSampling: SuperSampling = SuperSampling.PENTAGON,
        ): SurfaceEntity {
            val rtShape =
                when (shape) {
                    is Shape.Quad -> RtSurfaceEntity.Shape.Quad(shape.extents)
                    is Shape.Sphere -> RtSurfaceEntity.Shape.Sphere(shape.radius)
                    is Shape.Hemisphere -> RtSurfaceEntity.Shape.Hemisphere(shape.radius)
                    else -> throw IllegalArgumentException("Unsupported shape: $shape")
                }
            val surfaceEntity =
                SurfaceEntity(
                    lifecycleManager,
                    renderingRuntime.createSurfaceEntity(
                        getRtStereoMode(stereoMode),
                        pose,
                        rtShape,
                        getRtSurfaceProtection(surfaceProtection),
                        getRtSuperSampling(superSampling),
                        sceneRuntime.activitySpace,
                    ),
                    entityManager,
                    shape,
                )
            surfaceEntity.contentColorMetadata = contentColorMetadata
            return surfaceEntity
        }

        /**
         * Public factory function for a SurfaceEntity.
         *
         * @param session Session to create the SurfaceEntity in.
         * @param stereoMode Stereo mode for the surface.
         * @param pose Pose of this entity relative to its parent, default value is Identity.
         * @param shape The [Shape] which describes the spatialized shape of the canvas.
         * @param surfaceProtection The [SurfaceProtection] which describes whether the hosted
         *   surface should support Widevine DRM.
         * @param superSampling The [SuperSampling] which describes whether super sampling is
         *   enabled for the surface.
         * @return a SurfaceEntity instance
         */
        @MainThread
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            pose: Pose = Pose.Identity,
            shape: Shape = Shape.Quad(FloatSize2d(1.0f, 1.0f)),
            stereoMode: StereoMode = StereoMode.MONO,
            superSampling: SuperSampling = SuperSampling.PENTAGON,
            surfaceProtection: SurfaceProtection = SurfaceProtection.NONE,
        ): SurfaceEntity =
            SurfaceEntity.create(
                session.perceptionRuntime.lifecycleManager,
                session.sceneRuntime,
                session.renderingRuntime,
                session.scene.entityManager,
                stereoMode,
                pose,
                shape,
                surfaceProtection,
                null,
                superSampling,
            )
    }

    /**
     * Controls how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var stereoMode: StereoMode
        get() {
            checkNotDisposed()
            return getStereoModeFromRt(rtEntity!!.stereoMode)
        }
        @MainThread
        set(value) {
            checkNotDisposed()
            rtEntity!!.stereoMode = getRtStereoMode(value)
        }

    /**
     * Returns the size of the canvas in the local spatial coordinate system of the entity.
     *
     * This value is entirely determined by the value of [shape].
     */
    public val dimensions: FloatSize3d
        get() {
            checkNotDisposed()
            return rtEntity!!.dimensions.toFloatSize3d()
        }

    /**
     * The shape of the canvas that backs the Entity. Updating this value will alter the
     * [dimensions] of the Entity.
     *
     * @throws IllegalArgumentException if an invalid canvas shape is provided.
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var shape: Shape = shape
        @MainThread
        set(value) {
            checkNotDisposed()
            val rtShape =
                when (value) {
                    is Shape.Quad -> RtSurfaceEntity.Shape.Quad(value.extents)
                    is Shape.Sphere -> RtSurfaceEntity.Shape.Sphere(value.radius)
                    is Shape.Hemisphere -> RtSurfaceEntity.Shape.Hemisphere(value.radius)
                    else -> throw IllegalArgumentException("Unsupported canvas shape: $value")
                }
            rtEntity!!.shape = rtShape
            field = value
        }

    /**
     * The texture to be composited into the alpha channel of the surface. If null, the alpha mask
     * will be disabled.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public var primaryAlphaMaskTexture: Texture? = null
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        get() {
            checkNotDisposed()
            return field
        }
        @MainThread
        @SuppressLint("HiddenTypeParameter")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        set(value) {
            checkNotDisposed()
            rtEntity!!.setPrimaryAlphaMaskTexture(value?.texture)
            field = value
        }

    /**
     * The texture to be composited into the alpha channel of the secondary view of the surface.
     * This is only used for interleaved stereo content. If null, the alpha mask will be disabled.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public var auxiliaryAlphaMaskTexture: Texture? = null
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        get() {
            checkNotDisposed()
            return field
        }
        @MainThread
        @SuppressLint("HiddenTypeParameter")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        set(value) {
            checkNotDisposed()
            rtEntity!!.setAuxiliaryAlphaMaskTexture(value?.texture)
            field = value
        }

    /**
     * The [EdgeFeatheringParams] feathering pattern to be used along the edges of the Shape. This
     * value must only be set from the main thread.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var edgeFeatheringParams: EdgeFeatheringParams = EdgeFeatheringParams.NoFeathering()
        get() {
            checkNotDisposed()
            return field
        }
        @MainThread
        set(value) {
            checkNotDisposed()
            val rtEdgeFeather =
                when (value) {
                    is EdgeFeatheringParams.NoFeathering ->
                        RtSurfaceEntity.EdgeFeather.NoFeathering()
                    is EdgeFeatheringParams.RectangleFeather ->
                        RtSurfaceEntity.EdgeFeather.RectangleFeather(
                            value.leftRight,
                            value.topBottom,
                        )
                    else -> throw IllegalArgumentException("Unsupported edge feather: $value")
                }
            rtEntity!!.edgeFeather = rtEdgeFeather
            field = value
        }

    /**
     * Manages the explicit [ContentColorMetadata] for the surface's content.
     *
     * Describes how the application wants the system renderer to color convert [Surface] content to
     * the Display. When this is null, the system will make a best guess at the appropriate
     * conversion. Most applications will not need to set this - video playback libraries such as
     * ExoPlayer will automatically apply the correct conversion for media playback. Applications
     * rendering to the surface using APIs such as Vulkan are encouraged to use Vulkan extensions to
     * specify the color space and transfer function of their content and leave this value as null.
     *
     * The setter must be called from the main thread.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public var contentColorMetadata: ContentColorMetadata? = null
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        get() {
            checkNotDisposed()
            return if (!rtEntity!!.contentColorMetadataSet) {
                null
            } else {
                ContentColorMetadata(
                    colorSpace = ContentColorMetadata.getColorSpaceFromRt(rtEntity!!.colorSpace),
                    colorTransfer =
                        ContentColorMetadata.getColorTransferFromRt(rtEntity!!.colorTransfer),
                    colorRange = ContentColorMetadata.getColorRangeFromRt(rtEntity!!.colorRange),
                    maxContentLightLevel = rtEntity!!.maxContentLightLevel,
                )
            }
        }
        @MainThread
        @SuppressLint("HiddenTypeParameter")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        set(value) {
            checkNotDisposed()
            if (value == null) {
                rtEntity!!.resetContentColorMetadata()
            } else {
                rtEntity!!.setContentColorMetadata(
                    ContentColorMetadata.getRtColorSpace(value.colorSpace),
                    ContentColorMetadata.getRtColorTransfer(value.colorTransfer),
                    ContentColorMetadata.getRtColorRange(value.colorRange),
                    value.maxContentLightLevel,
                )
            }
            field = value
        }

    /**
     * Returns a surface into which the application can render stereo image content. Note that
     * android.graphics.Canvas Apis are not currently supported on this Canvas.
     *
     * @throws IllegalStateException if the Entity has been disposed.
     */
    @MainThread
    public fun getSurface(): Surface {
        checkNotDisposed()
        return rtEntity!!.surface
    }

    /**
     * Sets the dimensions of the Surface in pixels. This is needed if the application wishes to use
     * android.graphics.Canvas apis to render still images into the Surface. It is usually not
     * needed if the application is using a MediaPlayer or ExoPlayer to render the Surface.
     *
     * @throws IllegalArgumentException if the dimensions are not greater than 0.
     * @throws IllegalStateException if the Entity has been disposed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @MainThread
    public fun setSurfacePixelDimensions(dimensions: IntSize2d) {
        checkNotDisposed()
        rtEntity!!.setSurfacePixelDimensions(dimensions.width, dimensions.height)
    }

    /**
     * Gets the perceived resolution of the entity in the camera view.
     *
     * This API is only intended for use in Full Space Mode and will return
     * [PerceivedResolutionResult.InvalidCameraView] in Home Space Mode.
     *
     * The entity's own rotation and the camera's viewing direction are disregarded; this value
     * represents the dimensions of the entity on the camera view if its largest surface was facing
     * the camera without changing the distance of the entity to the camera.
     *
     * @return A [PerceivedResolutionResult] which encapsulates the outcome:
     *     - [PerceivedResolutionResult.Success] containing the [PixelDimensions] if the calculation
     *       is successful.
     *     - [PerceivedResolutionResult.EntityTooClose] if the entity is too close to the camera.
     *     - [PerceivedResolutionResult.InvalidCameraView] if the camera information required for
     *       the calculation is invalid or unavailable.
     *
     * @throws [IllegalStateException] if [Session.config.deviceTracking] is not set to
     *   [Config.DeviceTrackingMode.LAST_KNOWN].
     * @see PerceivedResolutionResult
     */
    public fun getPerceivedResolution(): PerceivedResolutionResult {
        checkNotDisposed()
        check(lifecycleManager.config.deviceTracking == Config.DeviceTrackingMode.LAST_KNOWN) {
            "Config.DeviceTrackingMode is not set to LastKnown."
        }
        return rtEntity!!.getPerceivedResolution().toPerceivedResolutionResult()
    }
}
