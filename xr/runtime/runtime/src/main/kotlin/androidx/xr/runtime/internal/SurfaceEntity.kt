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

package androidx.xr.runtime.internal

import android.view.Surface
import androidx.annotation.RestrictTo

/**
 * Interface for a spatialized Entity which manages an Android Surface. Applications can render to
 * this Surface in various ways, such as via MediaPlayer, ExoPlayer, or custom rendering. The
 * Surface content is texture mapped to the geometric shape defined by the [CanvasShape]. The
 * application can render stereoscopic content into the Surface and specify how it is routed to the
 * User's eyes for stereo viewing using the [stereoMode] property.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SurfaceEntity : Entity {
    /**
     * Specifies how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var stereoMode: Int

    /**
     * Specifies the shape of the spatial canvas which the surface is texture mapped to.
     *
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var canvasShape: CanvasShape

    /**
     * Retrieves the dimensions of the "spatial canvas" which the surface is mapped to. These values
     * are not impacted by scale.
     *
     * @return The canvas [Dimensions].
     */
    public val dimensions: Dimensions

    /**
     * Retrieves the surface that the Entity will display. The app can write into this surface
     * however it wants, i.e. MediaPlayer, ExoPlayer, or custom rendering.
     *
     * @return an Android [Surface]
     */
    public val surface: Surface

    /**
     * The texture to be composited into the alpha channel of the surface. If null, the alpha mask
     * will be disabled.
     *
     * @param alphaMask The primary alpha mask texture.
     * @throws IllegalStateException if the Entity has been disposed.
     */
    public fun setPrimaryAlphaMaskTexture(alphaMask: TextureResource?)

    /**
     * The texture to be composited into the alpha channel of the auxiliary view of the surface.
     * This is only used for interleaved stereo content. If null, the alpha mask will be disabled.
     *
     * @param alphaMask The auxiliary alpha mask texture.
     * @throws IllegalStateException if the Entity has been disposed.
     */
    public fun setAuxiliaryAlphaMaskTexture(alphaMask: TextureResource?)

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
     * @see PerceivedResolutionResult
     */
    public fun getPerceivedResolution(): PerceivedResolutionResult

    /**
     * Indicates whether explicit color information has been set for the surface content. If
     * `false`, the runtime should signal the backend to use its best effort color correction and
     * tonemapping. If `true`, the runtime should inform the backend to use the values specified in
     * [colorSpace], [colorTransfer], [colorRange], and [maxCLL] for color correction and
     * tonemapping of the surface content.
     *
     * This property is typically managed by the `setContentColorMetadata` and
     * `resetContentColorMetadata` methods.
     */
    public val contentColorMetadataSet: Boolean

    /**
     * The active color space of the media asset drawn on the surface. Use constants from
     * [SurfaceEntity.ColorSpace]. This value is used if [contentColorMetadataSet] is `true`.
     */
    public val colorSpace: Int

    /**
     * The active color transfer function of the media asset drawn on the surface. Use constants
     * from [SurfaceEntity.ColorTransfer]. This value is used if [contentColorMetadataSet] is
     * `true`.
     */
    public val colorTransfer: Int

    /**
     * The active color range of the media asset drawn on the surface. Use constants from
     * [SurfaceEntity.ColorRange]. This value is used if [contentColorMetadataSet] is `true`.
     */
    public val colorRange: Int

    /**
     * The active maximum content light level (MaxCLL) in nits. A value of 0 indicates that MaxCLL
     * is not set or is unknown. This value is used if [contentColorMetadataSet] is `true`.
     */
    public val maxCLL: Int

    /**
     * Sets the explicit color information for the surface content. This will also set
     * [contentColorMetadataSet] to `true`.
     *
     * @param colorSpace The runtime color space value (e.g., [SurfaceEntity.ColorSpace.BT709]).
     * @param colorTransfer The runtime color transfer value (e.g.,
     *   [SurfaceEntity.ColorTransfer.SRGB]).
     * @param colorRange The runtime color range value (e.g., [SurfaceEntity.ColorRange.FULL]).
     * @param maxCLL The maximum content light level in nits.
     * @throws IllegalStateException if the Entity has been disposed.
     */
    public fun setContentColorMetadata(
        colorSpace: Int,
        colorTransfer: Int,
        colorRange: Int,
        maxCLL: Int,
    )

    /**
     * Resets the color information to the runtime's default handling. This will set
     * [contentColorMetadataSet] to `false` and typically involves reverting [colorSpace],
     * [colorTransfer], [colorRange], and [maxCLL] to their default runtime values.
     *
     * @throws IllegalStateException if the Entity has been disposed.
     */
    public fun resetContentColorMetadata()

    /**
     * Selects the view configuration for the surface. MONO creates a surface contains a single
     * view. SIDE_BY_SIDE means the surface is split in half with two views. The first half of the
     * surface maps to the left eye and the second half mapping to the right eye.
     */
    public annotation class StereoMode {
        public companion object {
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
    }

    /**
     * Specifies whether the Surface which backs this Entity should support DRM content. This is
     * useful when decoding video content which requires DRM.
     *
     * See https://developer.android.com/reference/android/media/MediaDrm for more details.
     */
    public annotation class ContentSecurityLevel {
        public companion object {
            // The Surface content is not secured. DRM content can not be decoded into this Surface.
            // Screen captures of the SurfaceEntity will show the Surface content.
            public const val NONE: Int = 0
            // The surface content is secured. DRM content can be decoded into this Surface.
            // Screen captures of the SurfaceEntity will redact the Surface content.
            // TODO: b/411767049 - Redact only the Surface content, not the entire feed while the
            // Surface is visible.
            public const val PROTECTED: Int = 1
        }
    }

    /**
     * Specifies whether super sampling should be enabled for this surface. Super sampling can
     * improve text clarity at a performance cost.
     */
    public annotation class SuperSampling {
        public companion object {
            // Super sampling is disabled.
            public const val NONE: Int = 0
            // Super sampling is enabled. This is the default.
            public const val DEFAULT: Int = 1
        }
    }

    /**
     * Specifies the color space of the media asset drawn on the surface.
     *
     * Enum members cover the color spaces available in android::ADataSpace.
     */
    public annotation class ColorSpace {
        public companion object {
            public const val BT709: Int = 1
            public const val BT601_PAL: Int = 2
            public const val BT2020: Int = 6
            public const val BT601_525: Int = 0xf0
            public const val DISPLAY_P3: Int = 0xf1
            public const val DCI_P3: Int = 0xf2
            public const val ADOBE_RGB: Int = 0xf3
        }
    }

    /**
     * Specifies the color transfer function of the media asset drawn on the surface.
     *
     * Enum members cover the transfer functions available in android::ADataSpace. Enum values match
     * values from androidx.media3.common.C.ColorTransfer in
     * //third_party/java/android_libs/media:common
     */
    public annotation class ColorTransfer {
        public companion object {
            public const val LINEAR: Int = 1
            public const val SRGB: Int = 2
            public const val SDR: Int = 3 // SMPTE170M
            public const val GAMMA_2_2: Int = 10
            public const val ST2084: Int = 6
            public const val HLG: Int = 7
        }
    }

    /**
     * Specifies the color range of the media asset drawn on the surface.
     *
     * Enum values match values from androidx.media3.common.C.ColorRange in
     * //third_party/java/android_libs/media:common
     */
    public annotation class ColorRange {
        public companion object {
            public const val FULL: Int = 1
            public const val LIMITED: Int = 2
        }
    }

    /** Represents the shape of the spatial canvas which the surface is texture mapped to. */
    public interface CanvasShape {
        public val dimensions: Dimensions

        /**
         * A 2D rectangle-shaped canvas. Width and height are represented in the local spatial
         * coordinate system of the entity. (0,0,0) is the center of the canvas.
         */
        public class Quad(public val width: Float, public val height: Float) : CanvasShape {
            override val dimensions: Dimensions = Dimensions(width, height, 0f)
        }

        /**
         * A sphere-shaped canvas. Radius is represented in the local spatial coordinate system of
         * the entity. (0,0,0) is the center of the sphere.
         */
        public class Vr360Sphere(public val radius: Float) : CanvasShape {
            override val dimensions: Dimensions = Dimensions(radius * 2, radius * 2, radius * 2)
        }

        /**
         * A hemisphere-shaped canvas. Radius is represented in the local spatial coordinate system
         * of the entity. (0,0,0) is the center of the base of the hemisphere.
         */
        public class Vr180Hemisphere(public val radius: Float) : CanvasShape {
            override val dimensions: Dimensions = Dimensions(radius * 2, radius * 2, radius)
        }
    }

    /** Specifies edge transparency effects for the canvas. */
    public interface EdgeFeather {
        /** A smooth feathering effect which moves from edges of UV space to the center. */
        public class SmoothFeather(public val leftRight: Float, public val topBottom: Float) :
            EdgeFeather

        /** A Default implementation of EdgeFeather that does nothing. */
        public class SolidEdge() : EdgeFeather
    }

    /**
     * The edge feathering effect for the spatialized geometry.
     *
     * @throws IllegalStateException if the Entity has been disposed.
     */
    public var edgeFeather: EdgeFeather
}
