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

package androidx.xr.scenecore.testing

import android.graphics.ImageFormat
import android.media.ImageReader
import android.view.Surface
import androidx.annotation.RestrictTo
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.SurfaceEntity.Shape
import androidx.xr.scenecore.runtime.SurfaceFeature
import androidx.xr.scenecore.runtime.TextureResource

/**
 * Test-only implementation of [androidx.xr.scenecore.runtime.SurfaceEntity].
 *
 * Interface for a spatialized Entity which manages an Android Surface. Applications can render to
 * this Surface in various ways, such as via MediaPlayer, ExoPlayer, or custom rendering. The
 * Surface content is texture mapped to the geometric shape defined by the [Shape]. The application
 * can render stereoscopic content into the Surface and specify how it is routed to the User's eyes
 * for stereo viewing using the [stereoMode] property.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSurfaceEntity(private val feature: SurfaceFeature? = null) :
    FakeEntity(), SurfaceEntity {
    private var _stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE

    /**
     * Specifies how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     */
    override var stereoMode: Int
        get() {
            return feature?.stereoMode ?: _stereoMode
        }
        set(value) {
            if (feature == null) {
                _stereoMode = value
            } else {
                feature.stereoMode = value
            }
        }

    @SurfaceEntity.MediaBlendingMode
    private var _mediaBlendingMode = SurfaceEntity.MediaBlendingMode.TRANSPARENT

    /** Specifies the blending mode of the content. */
    @SurfaceEntity.MediaBlendingMode
    override var mediaBlendingMode: Int
        get() {
            return _mediaBlendingMode
        }
        set(value) {
            _mediaBlendingMode = value
        }

    private var _shape: Shape = Shape.Quad(FloatSize2d(0f, 0f))

    /** Specifies the shape of the spatial canvas which the surface is texture mapped to. */
    override var shape: Shape
        get() {
            return feature?.shape ?: _shape
        }
        set(value) {
            if (feature == null) {
                _shape = value
            } else {
                feature.shape = value
            }
        }

    /**
     * Retrieves the dimensions of the "spatial canvas" which the surface is mapped to. These values
     * are not impacted by scale.
     *
     * @return The canvas [androidx.xr.scenecore.runtime.Dimensions].
     */
    override val dimensions: Dimensions
        get() {
            return feature?.shape?.dimensions ?: shape.dimensions
        }

    private var _surface: Surface? = null

    /**
     * Retrieves the surface that the Entity will display. The app can write into this surface
     * however it wants, i.e. MediaPlayer, ExoPlayer, or custom rendering.
     *
     * @return an Android [Surface]
     */
    override val surface: Surface
        get() {
            if (feature == null) {
                if (_surface == null)
                    _surface = ImageReader.newInstance(1, 1, ImageFormat.YUV_420_888, 1).surface
                return _surface!!
            } else {
                return feature.surface
            }
        }

    /** For test purposes only. Caches the input of [setSurfacePixelDimensions]. */
    private var _surfacePixelDimensions: IntSize2d = IntSize2d(0, 0)

    /**
     * Sets the dimensions of the Surface in pixels.
     *
     * @param width The width of the Surface in pixels.
     * @param height The height of the Surface in pixels.
     * @throws IllegalArgumentException if the dimensions are invalid.
     */
    override fun setSurfacePixelDimensions(width: Int, height: Int) {
        if (feature == null) {
            _surfacePixelDimensions = IntSize2d(width, height)
        } else {
            feature.setSurfacePixelDimensions(width, height)
        }
    }

    /**
     * For test purposes only. Sets or replaces the underlying [Surface] for this fake entity.
     *
     * <p>This allows tests to provide a specific [Surface] instance, such as one connected to a
     * test-controlled producer, to verify rendering behavior.
     *
     * @param surface The new [Surface] to associate with this entity.
     */
    public fun setSurface(surface: Surface) {
        _surface = surface
    }

    /** For test purposes only. Represents the result of [setPrimaryAlphaMaskTexture]. */
    public var primaryAlphaMask: TextureResource? = null
        private set

    /**
     * The texture to be composited into the alpha channel of the surface. If null, the alpha mask
     * will be disabled.
     *
     * @param alphaMask The primary alpha mask texture.
     */
    override fun setPrimaryAlphaMaskTexture(alphaMask: TextureResource?) {
        if (feature == null) primaryAlphaMask = alphaMask
        else feature.setPrimaryAlphaMaskTexture(alphaMask)
    }

    /** For test purposes only. Represents the result of [setAuxiliaryAlphaMaskTexture] */
    public var auxiliaryAlphaMask: TextureResource? = null
        private set

    /**
     * The texture to be composited into the alpha channel of the auxiliary view of the surface.
     * This is only used for interleaved stereo content. If null, the alpha mask will be disabled.
     *
     * @param alphaMask The auxiliary alpha mask texture.
     */
    override fun setAuxiliaryAlphaMaskTexture(alphaMask: TextureResource?) {
        if (feature == null) auxiliaryAlphaMask = alphaMask
        else feature.setAuxiliaryAlphaMaskTexture(alphaMask)
    }

    /**
     * For test purposes only.
     *
     * The [androidx.xr.scenecore.runtime.PerceivedResolutionResult] that will be returned by
     * [getPerceivedResolution]. This can be modified in tests to simulate different perceived
     * resolution.
     */
    private var perceivedResolutionResult: PerceivedResolutionResult =
        PerceivedResolutionResult.InvalidRenderViewpoint()

    /**
     * For test purposes only.
     *
     * Sets the [androidx.xr.scenecore.runtime.PerceivedResolutionResult] that will be returned by
     * [getPerceivedResolution].
     */
    public fun setPerceivedResolution(perceivedResolution: PerceivedResolutionResult) {
        this.perceivedResolutionResult = perceivedResolution
    }

    /**
     * Gets the perceived resolution of the entity in the camera view.
     *
     * This API is only intended for use in Full Space Mode and will return
     * [androidx.xr.scenecore.runtime.PerceivedResolutionResult.InvalidRenderViewpoint] in Home
     * Space Mode.
     *
     * The entity's own rotation and the camera's viewing direction are disregarded; this value
     * represents the dimensions of the entity on the camera view if its largest surface was facing
     * the camera without changing the distance of the entity to the camera.
     *
     * @param renderViewScenePose The [ScenePose] that represents the camera pose.
     * @param renderViewFov The [FieldOfView] of the camera.
     * @return A [androidx.xr.scenecore.runtime.PerceivedResolutionResult] which encapsulates the
     *   outcome:
     *     - [PerceivedResolutionResult.Success] containing the [PixelDimensions] if the calculation
     *       is successful.
     *     - [PerceivedResolutionResult.EntityTooClose] if the entity is too close to the camera.
     *     - [PerceivedResolutionResult.InvalidCameraView] if the camera information required for
     *       the calculation is invalid or unavailable.
     *
     * @see androidx.xr.scenecore.runtime.PerceivedResolutionResult
     */
    override fun getPerceivedResolution(
        renderViewScenePose: ScenePose,
        renderViewFov: FieldOfView,
    ): PerceivedResolutionResult {
        return perceivedResolutionResult
    }

    /** For test purposes only. Specifies the value returned by [contentColorMetadataSet] */
    public var mContentColorMetadataSet: Boolean = false

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
    override val contentColorMetadataSet: Boolean
        get() = mContentColorMetadataSet

    private var _colorSpace: Int = SurfaceEntity.ColorSpace.BT709

    /**
     * The active color space of the media asset drawn on the surface. Use constants from
     * [androidx.xr.scenecore.runtime.SurfaceEntity.ColorSpace]. This value is used if
     * [contentColorMetadataSet] is `true`.
     */
    override val colorSpace: Int
        get() = _colorSpace

    private var _colorTransfer: Int = SurfaceEntity.ColorTransfer.LINEAR

    /**
     * The active color transfer function of the media asset drawn on the surface. Use constants
     * from [androidx.xr.scenecore.runtime.SurfaceEntity.ColorTransfer]. This value is used if
     * [contentColorMetadataSet] is `true`.
     */
    override val colorTransfer: Int
        get() = _colorTransfer

    private var _colorRange: Int = SurfaceEntity.ColorRange.FULL

    /**
     * The active color range of the media asset drawn on the surface. Use constants from
     * [androidx.xr.scenecore.runtime.SurfaceEntity.ColorRange]. This value is used if
     * [contentColorMetadataSet] is `true`.
     */
    override val colorRange: Int
        get() = _colorRange

    private var _maxContentLightLevel: Int = 0

    /**
     * The active maximum content light level (MaxCLL) in nits. A value of 0 indicates that MaxCLL
     * is not set or is unknown. This value is used if [contentColorMetadataSet] is `true`.
     */
    override val maxContentLightLevel: Int
        get() = _maxContentLightLevel

    /**
     * Sets the explicit color information for the surface content. This will also set
     * [contentColorMetadataSet] to `true`.
     *
     * @param colorSpace The runtime color space value (e.g.,
     *   [androidx.xr.scenecore.runtime.SurfaceEntity.ColorSpace.Companion.BT709]).
     * @param colorTransfer The runtime color transfer value (e.g.,
     *   [androidx.xr.scenecore.runtime.SurfaceEntity.ColorTransfer.Companion.SRGB]).
     * @param colorRange The runtime color range value (e.g.,
     *   [androidx.xr.scenecore.runtime.SurfaceEntity.ColorRange.Companion.FULL]).
     * @param maxContentLightLevel The maximum content light level in nits.
     */
    override fun setContentColorMetadata(
        colorSpace: Int,
        colorTransfer: Int,
        colorRange: Int,
        maxContentLightLevel: Int,
    ) {
        _colorSpace = colorSpace
        _colorTransfer = colorTransfer
        _colorRange = colorRange
        _maxContentLightLevel = maxContentLightLevel
    }

    /**
     * Resets the color information to the runtime's default handling. This will set
     * [contentColorMetadataSet] to `false` and typically involves reverting [colorSpace],
     * [colorTransfer], [colorRange], and [maxContentLightLevel] to their default runtime values.
     */
    override fun resetContentColorMetadata() {
        _colorSpace = SurfaceEntity.ColorSpace.BT709
        _colorTransfer = SurfaceEntity.ColorTransfer.LINEAR
        _colorRange = SurfaceEntity.ColorRange.FULL
        _maxContentLightLevel = 0
    }

    /**
     * The edge feathering effect for the spatialized geometry.
     *
     * @throws IllegalStateException if the Entity has been disposed.
     */
    override var edgeFeather: SurfaceEntity.EdgeFeather = SurfaceEntity.EdgeFeather.NoFeathering()
}
