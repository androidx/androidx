/*
 * Copyright 2024 The Android Open Source Project
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

import android.view.Surface
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.internal.SurfaceEntity as RtSurfaceEntity
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose

/**
 * SurfaceEntity is a concrete implementation of Entity that hosts a StereoSurface Canvas. The
 * entity creates and owns an Android Surface into which the application can render stereo image
 * content. This Surface is then texture mapped to the canvas, and if a stereoscopic StereoMode is
 * specified, then the User will see left and right eye content mapped to the appropriate display.
 *
 * Note that it is not currently possible to synchronize CanvasShape and StereoMode changes with
 * application rendering or video decoding. Applications are advised to carefully hide this entity
 * around transitions to manage glitchiness.
 *
 * @property canvasShape The [CanvasShape] which describes the mesh to which the Surface is mapped.
 * @property stereoMode The [StereoMode] which describes how parts of the surface are displayed to
 *   the user's eyes.
 * @property dimensions The dimensions of the canvas in the local spatial coordinate system of the
 *   entity.
 * @property primaryAlphaMaskTexture The texture to be composited into the alpha channel of the
 *   surface. If null, the alpha mask will be disabled.
 * @property auxiliaryAlphaMaskTexture The texture to be composited into the alpha channel of the
 *   secondary view of the surface. This is only used for interleaved stereo content. If null, the
 *   alpha mask will be disabled.
 * @property edgeFeather The [EdgeFeather] which describes the edge fading effects for the surface.
 * @property contentColorMetadata The [ContentColorMetadata] of the content (nullable).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SurfaceEntity
private constructor(
    private val lifecycleManager: LifecycleManager,
    rtEntity: RtSurfaceEntity,
    entityManager: EntityManager,
    canvasShape: CanvasShape,
    private var disposed: Boolean = false, // TODO b/427314036: remove this
) : BaseEntity<RtSurfaceEntity>(rtEntity, entityManager) {

    /** Represents the shape of the Canvas that backs a SurfaceEntity. */
    public abstract class CanvasShape private constructor() {
        public open val dimensions: FloatSize3d = FloatSize3d(0.0f, 0.0f, 0.0f)

        // A Quad-shaped canvas. Width and height are represented in the local spatial coordinate
        // system of the entity. (0,0,0) is the center of the canvas.
        public class Quad(public val width: Float, public val height: Float) : CanvasShape() {
            override val dimensions: FloatSize3d
                get() = FloatSize3d(width, height, 0.0f)
        }

        // An inwards-facing sphere-shaped canvas, centered at (0,0,0) in the local coordinate
        // space.
        // This is intended to be used by setting the entity's pose to the user's head pose.
        // Radius is represented in the local spatial coordinate system of the entity.
        // The center of the Surface will be mapped to (0, 0, -radius) in the local coordinate
        // space,
        // and UV's are applied from positive X to negative X in an equirectangular projection.
        public class Vr360Sphere(public val radius: Float) : CanvasShape() {
            override val dimensions: FloatSize3d
                get() = FloatSize3d(radius * 2, radius * 2, radius * 2)
        }

        // An inwards-facing hemisphere-shaped canvas, where (0,0,0) is the center of the base of
        // the
        // hemisphere. Radius is represented in the local spatial coordinate system of the entity.
        // This is intended to be used by setting the entity's pose to the user's head pose.
        // The center of the Surface will be mapped to (0, 0, -radius) in the local coordinate
        // space,
        // and UV's are applied from positive X to negative X in an equirectangular projection.
        public class Vr180Hemisphere(public val radius: Float) : CanvasShape() {
            override val dimensions: FloatSize3d
                get() = FloatSize3d(radius * 2, radius * 2, radius)
        }
    }

    /** Represents edge fading effects for a SurfaceEntity. */
    public abstract class EdgeFeatheringParams private constructor() {
        /**
         * @property leftRight a [Float] which controls the canvas-relative radius of the edge
         *   fadeout on the left and right edges of the SurfaceEntity canvas.
         * @property topBottom a [Float] which controls the canvas-relative radius of the edge
         *   fadeout on the top and bottom edges of the SurfaceEntity canvas.
         *
         * A radius of 0.05 represents 5% of the width of the visible canvas surface. Please note
         * that this is scaled by the aspect ratio of Quad-shaped canvases.
         *
         * Applications are encouraged to use ZeroFeather or set this to 0.0 on Spherical canvases.
         * The behavior is only defined for values between [0.0f - 0.5f]. Default values are 0.0f.
         */
        public class SmoothFeather(
            @FloatRange(from = 0.0, to = 0.5) public val leftRight: Float = 0.0f,
            @FloatRange(from = 0.0, to = 0.5) public val topBottom: Float = 0.0f,
        ) : EdgeFeatheringParams() {}

        /** Applies no edge fading to any canvas. */
        public class SolidEdge : EdgeFeatheringParams() {}
    }

    @IntDef(ContentSecurityLevel.NONE, ContentSecurityLevel.PROTECTED)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class ContentSecurityLevelValue

    /**
     * Specifies whether the Surface which backs this entity should support DRM content. This is
     * useful when decoding video content which requires DRM.
     *
     * See https://developer.android.com/reference/android/media/MediaDrm for more details.
     */
    public object ContentSecurityLevel {
        // The Surface content is not secured. DRM content can not be decoded into this Surface.
        // Screen captures of the SurfaceEntity will show the Surface content.
        public const val NONE: Int = 0

        // The surface content is secured. DRM content can be decoded into this Surface.
        // Screen captures of the SurfaceEntity will redact the Surface content.
        public const val PROTECTED: Int = 1
    }

    @IntDef(SuperSampling.NONE, SuperSampling.DEFAULT)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class SuperSamplingValue

    /**
     * Specifies whether super sampling should be enabled for this surface. Super sampling can
     * improve text clarity at a performance cost.
     */
    public object SuperSampling {
        // Super sampling is disabled.
        public const val NONE: Int = 0
        // Super sampling is enabled. This is the default.
        public const val DEFAULT: Int = 1
    }

    /**
     * Specifies how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     *
     * Values here match values from androidx.media3.common.C.StereoMode in
     * //third_party/java/android_libs/media:common
     */
    @IntDef(
        StereoMode.MONO,
        StereoMode.TOP_BOTTOM,
        StereoMode.SIDE_BY_SIDE,
        StereoMode.MULTIVIEW_LEFT_PRIMARY,
        StereoMode.MULTIVIEW_RIGHT_PRIMARY,
    )
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class StereoModeValue

    public object StereoMode {
        // Each eye will see the entire surface (no separation)
        public const val MONO: Int = 0
        // The [top, bottom] halves of the surface will map to [left, right] eyes
        public const val TOP_BOTTOM: Int = 1
        // The [left, right] halves of the surface will map to [left, right] eyes
        public const val SIDE_BY_SIDE: Int = 2
        // Multiview video, [primary, auxiliary] views will map to [left, right] eyes
        public const val MULTIVIEW_LEFT_PRIMARY: Int = 4
        // Multiview video, [primary, auxiliary] views will map to [right, left] eyes
        public const val MULTIVIEW_RIGHT_PRIMARY: Int = 5
    }

    /**
     * Color information for the content drawn on a surface.
     *
     * @param colorSpace The color space of the content.
     * @param colorTransfer The transfer function of the content.
     * @param colorRange The color range of the content.
     * @param maxCLL The maximum content light level of the content (in nits).
     */
    public class ContentColorMetadata(
        @ColorSpaceValue public val colorSpace: Int = ColorSpace.BT709,
        @ColorTransferValue public val colorTransfer: Int = ColorTransfer.SRGB,
        @ColorRangeValue public val colorRange: Int = ColorRange.FULL,
        public val maxCLL: Int = Companion.maxCLLUnknown,
    ) {

        /**
         * Specifies the color space of the media asset drawn on the surface.
         *
         * These values are a superset of androidx.media3.common.C.ColorSpace.
         */
        @IntDef(
            ColorSpace.BT709,
            ColorSpace.BT601_PAL,
            ColorSpace.BT2020,
            ColorSpace.BT601_525,
            ColorSpace.DISPLAY_P3,
            ColorSpace.DCI_P3,
            ColorSpace.ADOBE_RGB,
        )
        @Retention(AnnotationRetention.SOURCE)
        internal annotation class ColorSpaceValue

        public object ColorSpace {
            public const val BT709: Int = 1
            public const val BT601_PAL: Int = 2
            public const val BT2020: Int = 6
            public const val BT601_525: Int = 0xf0
            public const val DISPLAY_P3: Int = 0xf1
            public const val DCI_P3: Int = 0xf2
            public const val ADOBE_RGB: Int = 0xf3
        }

        /**
         * Specifies the color transfer function of the media asset drawn on the surface.
         *
         * Enum members cover the transfer functions available in android::ADataSpace Enum values
         * match values from androidx.media3.common.C.ColorTransfer.
         */
        @IntDef(
            ColorTransfer.LINEAR,
            ColorTransfer.SRGB,
            ColorTransfer.SDR, // SMPTE170M
            ColorTransfer.GAMMA_2_2,
            ColorTransfer.ST2084,
            ColorTransfer.HLG,
        )
        @Retention(AnnotationRetention.SOURCE)
        internal annotation class ColorTransferValue

        public object ColorTransfer {
            public const val LINEAR: Int = 1
            public const val SRGB: Int = 2
            public const val SDR: Int = 3
            public const val GAMMA_2_2: Int = 10
            public const val ST2084: Int = 6
            public const val HLG: Int = 7
        }

        /**
         * Specifies the color range of the media asset drawn on the surface.
         *
         * Enum values match values from androidx.media3.common.C.ColorRange.
         */
        @IntDef(ColorRange.FULL, ColorRange.LIMITED)
        @Retention(AnnotationRetention.SOURCE)
        internal annotation class ColorRangeValue

        public object ColorRange {
            public const val FULL: Int = 1
            public const val LIMITED: Int = 2
        }

        public companion object {
            // Represents an unknown maximum content light level.
            public const val maxCLLUnknown: Int = 0

            internal fun getRtColorSpace(colorSpace: Int): Int {
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

            internal fun getRtColorTransfer(colorTransfer: Int): Int {
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

            internal fun getRtColorRange(colorRange: Int): Int {
                return when (colorRange) {
                    ColorRange.FULL -> RtSurfaceEntity.ColorRange.FULL
                    ColorRange.LIMITED -> RtSurfaceEntity.ColorRange.LIMITED
                    else -> RtSurfaceEntity.ColorRange.FULL
                }
            }
        }
    }

    // TODO b/427314036: remove this once this is enforced within BaseEntity.
    override fun dispose() {
        super.dispose()
        disposed = true
    }

    // TODO b/427314036: remove this once this is enforced within BaseEntity.
    private fun checkDisposed() {
        if (disposed) {
            throw IllegalStateException("Entity is disposed.")
        }
    }

    public companion object {
        private fun getRtStereoMode(stereoMode: Int): Int {
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

        private fun getRtContentSecurityLevel(contentSecurityLevel: Int): Int {
            return when (contentSecurityLevel) {
                ContentSecurityLevel.NONE -> RtSurfaceEntity.ContentSecurityLevel.NONE
                ContentSecurityLevel.PROTECTED -> RtSurfaceEntity.ContentSecurityLevel.PROTECTED
                else -> RtSurfaceEntity.ContentSecurityLevel.NONE
            }
        }

        private fun getRtSuperSampling(superSampling: Int): Int {
            return when (superSampling) {
                SuperSampling.NONE -> RtSurfaceEntity.SuperSampling.NONE
                SuperSampling.DEFAULT -> RtSurfaceEntity.SuperSampling.DEFAULT
                else -> RtSurfaceEntity.SuperSampling.DEFAULT
            }
        }

        /**
         * Factory method for SurfaceEntity.
         *
         * @param lifecycleManager A SceneCore LifecycleManager
         * @param adapter JxrPlatformAdapter to use.
         * @param entityManager A SceneCore EntityManager
         * @param stereoMode An [Int] which defines how surface subregions map to eyes
         * @param pose Pose for this StereoSurface entity, relative to its parent.
         * @param canvasShape The [CanvasShape] which describes the spatialized shape of the canvas.
         * @param contentSecurityLevel The [ContentSecurityLevel] which describes whether DRM is
         *   enabled for the surface.
         * @param contentColorMetadata The [ContentColorMetadata] of the content (nullable).
         * @param superSampling The [SuperSampling] which describes whether super sampling is
         *   enabled for the surface.
         * @return a SurfaceEntity instance
         */
        internal fun create(
            lifecycleManager: LifecycleManager,
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            stereoMode: Int = StereoMode.SIDE_BY_SIDE,
            pose: Pose = Pose.Identity,
            canvasShape: CanvasShape = CanvasShape.Quad(1.0f, 1.0f),
            contentSecurityLevel: Int = ContentSecurityLevel.NONE,
            contentColorMetadata: ContentColorMetadata? = null,
            superSampling: Int = SuperSampling.DEFAULT,
        ): SurfaceEntity {
            val rtCanvasShape =
                when (canvasShape) {
                    is CanvasShape.Quad ->
                        RtSurfaceEntity.CanvasShape.Quad(canvasShape.width, canvasShape.height)
                    is CanvasShape.Vr360Sphere ->
                        RtSurfaceEntity.CanvasShape.Vr360Sphere(canvasShape.radius)
                    is CanvasShape.Vr180Hemisphere ->
                        RtSurfaceEntity.CanvasShape.Vr180Hemisphere(canvasShape.radius)
                    else -> throw IllegalArgumentException("Unsupported canvas shape: $canvasShape")
                }
            val surfaceEntity =
                SurfaceEntity(
                    lifecycleManager,
                    adapter.createSurfaceEntity(
                        getRtStereoMode(stereoMode),
                        pose,
                        rtCanvasShape,
                        getRtContentSecurityLevel(contentSecurityLevel),
                        getRtSuperSampling(superSampling),
                        adapter.activitySpaceRootImpl,
                    ),
                    entityManager,
                    canvasShape,
                )
            surfaceEntity.contentColorMetadata = contentColorMetadata
            return surfaceEntity
        }

        /**
         * Public factory function for a SurfaceEntity.
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session Session to create the SurfaceEntity in.
         * @param stereoMode Stereo mode for the surface.
         * @param pose Pose of this entity relative to its parent, default value is Identity.
         * @param canvasShape The [CanvasShape] which describes the spatialized shape of the canvas.
         * @param contentSecurityLevel The [ContentSecurityLevel] which describes whether DRM is
         *   enabled for the surface.
         * @param contentColorMetadata The [ContentColorMetadata] of the content (nullable).
         * @param superSampling The [SuperSampling] which describes whether super sampling is
         *   enabled for the surface.
         * @return a SurfaceEntity instance
         */
        @MainThread
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            stereoMode: Int = SurfaceEntity.StereoMode.SIDE_BY_SIDE,
            pose: Pose = Pose.Identity,
            canvasShape: CanvasShape = CanvasShape.Quad(1.0f, 1.0f),
            contentSecurityLevel: Int = ContentSecurityLevel.NONE,
            contentColorMetadata: ContentColorMetadata? = null,
            superSampling: Int = SuperSampling.DEFAULT,
        ): SurfaceEntity =
            SurfaceEntity.create(
                session.runtime.lifecycleManager,
                session.platformAdapter,
                session.scene.entityManager,
                stereoMode,
                pose,
                canvasShape,
                contentSecurityLevel,
                contentColorMetadata,
                superSampling,
            )
    }

    /**
     * Controls how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     *
     * Values must be one of the values from [StereoMode].
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var stereoMode: Int
        get() {
            checkDisposed()
            return rtEntity.stereoMode
        }
        @MainThread
        set(value) {
            checkDisposed()
            rtEntity.stereoMode = getRtStereoMode(value)
        }

    /**
     * Returns the dimensions of the Entity.
     *
     * This is the size of the canvas in the local spatial coordinate system of the entity. This
     * field cannot be directly set - to update the dimensions of the canvas, update the value of
     * [canvasShape].
     */
    public val dimensions: FloatSize3d
        get() {
            checkDisposed()
            return rtEntity.dimensions.toFloatSize3d()
        }

    /**
     * The shape of the canvas that backs the Entity. Updating this value will alter the dimensions
     * of the Entity.
     *
     * @throws IllegalArgumentException if an invalid canvas shape is provided.
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var canvasShape: CanvasShape = canvasShape
        @MainThread
        set(value) {
            checkDisposed()
            val rtCanvasShape =
                when (value) {
                    is CanvasShape.Quad ->
                        RtSurfaceEntity.CanvasShape.Quad(value.width, value.height)
                    is CanvasShape.Vr360Sphere ->
                        RtSurfaceEntity.CanvasShape.Vr360Sphere(value.radius)
                    is CanvasShape.Vr180Hemisphere ->
                        RtSurfaceEntity.CanvasShape.Vr180Hemisphere(value.radius)
                    else -> throw IllegalArgumentException("Unsupported canvas shape: $value")
                }
            rtEntity.canvasShape = rtCanvasShape
            field = value
        }

    /**
     * The texture to be composited into the alpha channel of the surface. If null, the alpha mask
     * will be disabled.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var primaryAlphaMaskTexture: Texture? = null
        @MainThread
        set(value) {
            checkDisposed()
            rtEntity.setPrimaryAlphaMaskTexture(value?.texture)
            field = value
        }

    /**
     * The texture to be composited into the alpha channel of the secondary view of the surface.
     * This is only used for interleaved stereo content. If null, the alpha mask will be disabled.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var auxiliaryAlphaMaskTexture: Texture? = null
        @MainThread
        set(value) {
            checkDisposed()
            rtEntity.setAuxiliaryAlphaMaskTexture(value?.texture)
            field = value
        }

    /**
     * The [EdgeFeather] feathering pattern to be used along the edges of the CanvasShape. This
     * value must only be set from the main thread.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var edgeFeather: EdgeFeatheringParams = EdgeFeatheringParams.SolidEdge()
        get() {
            checkDisposed()
            return field
        }
        @MainThread
        set(value) {
            checkDisposed()
            val rtEdgeFeather =
                when (value) {
                    is EdgeFeatheringParams.SolidEdge -> RtSurfaceEntity.EdgeFeather.SolidEdge()
                    is EdgeFeatheringParams.SmoothFeather ->
                        RtSurfaceEntity.EdgeFeather.SmoothFeather(value.leftRight, value.topBottom)
                    else -> throw IllegalArgumentException("Unsupported edge feather: $value")
                }
            rtEntity.edgeFeather = rtEdgeFeather
            field = value
        }

    /**
     * Manages the explicit [ContentColorMetadata] for the surface's content.
     *
     * Describes how the application wants the system renderer to color convert Surface content to
     * the Display. When this is null, the system will make a best guess at the appropriate
     * conversion.
     *
     * The setter must be called from the main thread.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var contentColorMetadata: ContentColorMetadata?
        get() {
            checkDisposed()
            return if (!rtEntity.contentColorMetadataSet) {
                null
            } else {
                ContentColorMetadata(
                    colorSpace = rtEntity.colorSpace,
                    colorTransfer = rtEntity.colorTransfer,
                    colorRange = rtEntity.colorRange,
                    maxCLL = rtEntity.maxCLL,
                )
            }
        }
        @MainThread
        set(value) {
            checkDisposed()
            if (value == null) {
                rtEntity.resetContentColorMetadata()
            } else {
                rtEntity.setContentColorMetadata(
                    ContentColorMetadata.getRtColorSpace(value.colorSpace),
                    ContentColorMetadata.getRtColorTransfer(value.colorTransfer),
                    ContentColorMetadata.getRtColorRange(value.colorRange),
                    value.maxCLL,
                )
            }
        }

    /**
     * Returns a surface into which the application can render stereo image content.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @throws IllegalStateException if the Entity has been disposed.
     */
    @MainThread
    public fun getSurface(): Surface {
        checkDisposed()
        return rtEntity.surface
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
     * @throws [IllegalStateException] if [Session.config.headTracking] is set to
     *   [Config.HeadTrackingMode.DISABLED].
     * @see PerceivedResolutionResult
     */
    public fun getPerceivedResolution(): PerceivedResolutionResult {
        checkDisposed()
        check(lifecycleManager.config.headTracking != Config.HeadTrackingMode.DISABLED) {
            "Config.HeadTrackingMode is set to Disabled."
        }
        return rtEntity.getPerceivedResolution().toPerceivedResolutionResult()
    }
}
