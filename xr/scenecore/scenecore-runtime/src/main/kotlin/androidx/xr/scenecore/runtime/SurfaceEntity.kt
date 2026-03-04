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

package androidx.xr.scenecore.runtime

import android.view.Surface
import androidx.annotation.RestrictTo
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.math.FloatSize2d
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.jvm.JvmOverloads

/**
 * Interface for a spatialized Entity which manages an Android Surface. Applications can render to
 * this Surface in various ways, such as via MediaPlayer, ExoPlayer, or custom rendering. The
 * Surface content is texture mapped to the geometric shape defined by the [Shape]. The application
 * can render stereoscopic content into the Surface and specify how it is routed to the User's eyes
 * for stereo viewing using the [stereoMode] property.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SurfaceEntity : Entity {
    /**
     * Specifies how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     *
     * @throws kotlin.IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var stereoMode: Int

    /**
     * Specifies the blending mode of the content.
     *
     * @throws kotlin.IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var mediaBlendingMode: Int

    /**
     * Specifies the geometry of the spatial canvas which the surface is texture mapped to.
     *
     * @throws kotlin.IllegalStateException when setting this value if the Entity has been disposed.
     */
    public var shape: Shape

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
     * Sets the dimensions of the Surface in pixels. This is needed if the application wishes to use
     * android.graphics.Canvas apis to render still images into the Surface. It is usually not
     * needed if the application is using a MediaPlayer or ExoPlayer to render the Surface.
     *
     * @param width The width of the Surface in pixels.
     * @param height The height of the Surface in pixels.
     * @throws IllegalArgumentException if the dimensions are invalid.
     * @throws IllegalStateException when setting this value if the Entity has been disposed.
     */
    public fun setSurfacePixelDimensions(width: Int, height: Int)

    /**
     * The texture to be composited into the alpha channel of the surface. If null, the alpha mask
     * will be disabled.
     *
     * @param alphaMask The primary alpha mask texture.
     * @throws kotlin.IllegalStateException if the Entity has been disposed.
     */
    public fun setPrimaryAlphaMaskTexture(alphaMask: TextureResource?)

    /**
     * The texture to be composited into the alpha channel of the auxiliary view of the surface.
     * This is only used for interleaved stereo content. If null, the alpha mask will be disabled.
     *
     * @param alphaMask The auxiliary alpha mask texture.
     * @throws kotlin.IllegalStateException if the Entity has been disposed.
     */
    public fun setAuxiliaryAlphaMaskTexture(alphaMask: TextureResource?)

    /**
     * Gets the perceived resolution of the entity in the camera view.
     *
     * This API is only intended for use in Full Space Mode and will return
     * [PerceivedResolutionResult.InvalidRenderViewpoint] in Home Space Mode.
     *
     * The entity's own rotation and the camera's viewing direction are disregarded; this value
     * represents the dimensions of the entity on the camera view if its largest surface was facing
     * the camera without changing the distance of the entity to the camera.
     *
     * @param renderViewScenePose The [ScenePose] that represents the camera pose.
     * @param renderViewFov The [FieldOfView] of the camera.
     * @return A [PerceivedResolutionResult] which encapsulates the outcome:
     *     - [PerceivedResolutionResult.Success] containing the [PixelDimensions] if the calculation
     *       is successful.
     *     - [PerceivedResolutionResult.EntityTooClose] if the entity is too close to the camera.
     *     - [PerceivedResolutionResult.InvalidRenderViewpoint] if the camera information required
     *       for the calculation is invalid or unavailable.
     *
     * @see PerceivedResolutionResult
     */
    public fun getPerceivedResolution(
        renderViewScenePose: ScenePose,
        renderViewFov: FieldOfView,
    ): PerceivedResolutionResult

    /**
     * Indicates whether explicit color information has been set for the surface content. If
     * `false`, the runtime should signal the backend to use its best effort color correction and
     * tonemapping. If `true`, the runtime should inform the backend to use the values specified in
     * [colorSpace], [colorTransfer], [colorRange], and [maxContentLightLevel] for color correction
     * and tonemapping of the surface content.
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
     * The active maximum content light level (MaxContentLightLevel) in nits. A value of 0 indicates
     * that MaxContentLightLevel is not set or is unknown. This value is used if
     * [contentColorMetadataSet] is `true`.
     */
    public val maxContentLightLevel: Int

    /**
     * Sets the explicit color information for the surface content. This will also set
     * [contentColorMetadataSet] to `true`.
     *
     * @param colorSpace The runtime color space value (e.g., [SurfaceEntity.ColorSpace.BT709]).
     * @param colorTransfer The runtime color transfer value (e.g.,
     *   [SurfaceEntity.ColorTransfer.SRGB]).
     * @param colorRange The runtime color range value (e.g., [SurfaceEntity.ColorRange.FULL]).
     * @param maxContentLightLevel The maximum content light level in nits.
     * @throws kotlin.IllegalStateException if the Entity has been disposed.
     */
    public fun setContentColorMetadata(
        colorSpace: Int,
        colorTransfer: Int,
        colorRange: Int,
        maxContentLightLevel: Int,
    )

    /**
     * Resets the color information to the runtime's default handling. This will set
     * [contentColorMetadataSet] to `false` and typically involves reverting [colorSpace],
     * [colorTransfer], [colorRange], and [maxContentLightLevel] to their default runtime values.
     *
     * @throws kotlin.IllegalStateException if the Entity has been disposed.
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
    public annotation class SurfaceProtection {
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

    /** Specifies the draw mode of the mesh. */
    public annotation class DrawMode {
        public companion object {
            /** Draw the mesh as a list of triangles. */
            public const val TRIANGLES: Int = 0
            /** Draw the mesh as a triangle strip. */
            public const val TRIANGLE_STRIP: Int = 1
            /** Draw the mesh as a triangle fan. */
            public const val TRIANGLE_FAN: Int = 2
        }
    }

    /** Specifies the blending mode of the content. */
    public annotation class MediaBlendingMode {
        public companion object {
            // Content is alpha-blended with the background.
            public const val TRANSPARENT: Int = 0
            // Content is opaque and does not blend with the background.
            public const val OPAQUE: Int = 1
        }
    }

    /** Represents the shape of the spatial canvas which the surface is texture mapped to. */
    public interface Shape {
        public val dimensions: Dimensions

        /**
         * A 2D rectangle-shaped canvas. Width, height and corner radius are represented in the
         * local spatial coordinate system of the entity. (0,0,0) is the center of the canvas.
         */
        public class Quad
        @JvmOverloads
        constructor(public val extents: FloatSize2d, public val cornerRadius: Float = 0.0f) :
            Shape {
            override val dimensions: Dimensions = Dimensions(extents.width, extents.height, 0f)
        }

        /**
         * A sphere-shaped canvas. Radius is represented in the local spatial coordinate system of
         * the entity. (0,0,0) is the center of the sphere.
         */
        public class Sphere(public val radius: Float) : Shape {
            override val dimensions: Dimensions = Dimensions(radius * 2, radius * 2, radius * 2)
        }

        /**
         * A hemisphere-shaped canvas. Radius is represented in the local spatial coordinate system
         * of the entity. (0,0,0) is the center of the base of the hemisphere.
         */
        public class Hemisphere(public val radius: Float) : Shape {
            override val dimensions: Dimensions = Dimensions(radius * 2, radius * 2, radius)
        }

        /**
         * A triangle mesh. The mesh is specified by a list of positions and texture coordinates and
         * an optional list of indices.
         */
        public class TriangleMesh(
            /** The positions of the vertices in the mesh. */
            public var positions: FloatBuffer,
            public var texCoords: FloatBuffer,
            public var indices: IntBuffer?,
        )

        /**
         * A custom mesh canvas. The mesh is specified by a left eye mesh and an optional right eye
         * mesh.
         */
        public class CustomMesh(
            public val leftEye: TriangleMesh,
            public val rightEye: TriangleMesh?,
            public val drawMode: Int = DrawMode.TRIANGLES,
        ) : Shape {
            override val dimensions: Dimensions by lazy {
                val min = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
                val max = floatArrayOf(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)

                fun updateBounds(positions: FloatBuffer) {
                    // Rewind buffer to make sure we start from the beginning
                    positions.rewind()
                    while (positions.remaining() >= 3) {
                        val x = positions.get()
                        val y = positions.get()
                        val z = positions.get()
                        min[0] = kotlin.math.min(min[0], x)
                        max[0] = kotlin.math.max(max[0], x)
                        min[1] = kotlin.math.min(min[1], y)
                        max[1] = kotlin.math.max(max[1], y)
                        min[2] = kotlin.math.min(min[2], z)
                        max[2] = kotlin.math.max(max[2], z)
                    }
                }

                updateBounds(leftEye.positions)
                rightEye?.let { updateBounds(it.positions) }

                val width = max[0] - min[0]
                val height = max[1] - min[1]
                val depth = max[2] - min[2]

                Dimensions(width, height, depth)
            }
        }
    }

    /** Specifies edge transparency effects for the canvas. */
    public interface EdgeFeather {
        /** A smooth feathering effect which moves from edges of UV space to the center. */
        public class RectangleFeather(public val leftRight: Float, public val topBottom: Float) :
            EdgeFeather

        /** A Default implementation of EdgeFeather that does nothing. */
        public class NoFeathering() : EdgeFeather
    }

    /**
     * The edge feathering effect for the spatialized shape.
     *
     * @throws kotlin.IllegalStateException if the Entity has been disposed.
     */
    public var edgeFeather: EdgeFeather
}
