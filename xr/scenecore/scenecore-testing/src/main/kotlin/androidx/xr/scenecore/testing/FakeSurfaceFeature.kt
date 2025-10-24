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
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.SurfaceFeature
import androidx.xr.scenecore.runtime.TextureResource

/** Test-only implementation of [androidx.xr.scenecore.runtime.SurfaceFeature] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSurfaceFeature(nodeHolder: NodeHolder<*>) :
    FakeBaseRenderingFeature(nodeHolder), SurfaceFeature {
    private var mockSurfaceFeature: SurfaceFeature? = null

    private var _stereoMode: Int = SurfaceEntity.StereoMode.SIDE_BY_SIDE

    /**
     * Specifies how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     */
    override var stereoMode: Int
        get() = mockSurfaceFeature?.stereoMode ?: _stereoMode
        set(value) {
            if (mockSurfaceFeature == null) {
                _stereoMode = value
            } else {
                mockSurfaceFeature!!.stereoMode = value
            }
        }

    private var _canvasShape: SurfaceEntity.Shape = SurfaceEntity.Shape.Quad(FloatSize2d(0f, 0f))

    /** Specifies the shape of the spatial canvas which the surface is texture mapped to. */
    override var shape: SurfaceEntity.Shape
        get() = mockSurfaceFeature?.shape ?: _canvasShape
        set(value) {
            if (mockSurfaceFeature == null) {
                _canvasShape = value
            } else {
                mockSurfaceFeature!!.shape = value
            }
        }

    /**
     * Retrieves the dimensions of the "spatial canvas" which the surface is mapped to. These values
     * are not impacted by scale.
     *
     * @return The canvas [androidx.xr.scenecore.runtime.Dimensions].
     */
    override val dimensions: Dimensions
        get() = shape.dimensions

    private var _surface: Surface =
        ImageReader.newInstance(1, 1, ImageFormat.YUV_420_888, 1).surface

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
        _surfacePixelDimensions = IntSize2d(width, height)
    }

    /**
     * Retrieves the surface that the Entity will display. The app can write into this surface
     * however it wants, i.e. MediaPlayer, ExoPlayer, or custom rendering.
     *
     * @return an Android [Surface]
     */
    override val surface: Surface
        get() = _surface

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

    private var _colliderEnabled: Boolean = false

    /**
     * Sets whether the collider is enabled.
     *
     * @param enableCollider Whether the collider is enabled.
     */
    override fun setColliderEnabled(enableCollider: Boolean) {
        mockSurfaceFeature!!.setColliderEnabled(enableCollider)
        _colliderEnabled = enableCollider
    }

    /** For test purposes only. Represents the result of [setColliderEnabled]. */
    public fun getColliderEnabled(): Boolean {
        return _colliderEnabled
    }

    /**
     * The texture to be composited into the alpha channel of the surface. If null, the alpha mask
     * will be disabled.
     *
     * @param alphaMask The primary alpha mask texture.
     */
    override fun setPrimaryAlphaMaskTexture(alphaMask: TextureResource?) {
        primaryAlphaMask = alphaMask
    }

    private var auxiliaryAlphaMask: TextureResource? = null

    /**
     * The texture to be composited into the alpha channel of the auxiliary view of the surface.
     * This is only used for interleaved stereo content. If null, the alpha mask will be disabled.
     *
     * @param alphaMask The auxiliary alpha mask texture.
     */
    override fun setAuxiliaryAlphaMaskTexture(alphaMask: TextureResource?) {
        if (mockSurfaceFeature == null) {
            auxiliaryAlphaMask = alphaMask
        } else {
            mockSurfaceFeature!!.setAuxiliaryAlphaMaskTexture(alphaMask)
        }
    }

    private var _contentColorMetadataSet: Boolean = false

    /**
     * Indicates whether explicit color information has been set for the surface content. If
     * `false`, the runtime should signal the backend to use its best effort color correction and
     * tone-mapping. If `true`, the runtime should inform the backend to use the values specified in
     * [colorSpace], [colorTransfer], [colorRange], and [maxContentLightLevel] for color correction
     * and tone-mapping of the surface content.
     *
     * This property is typically managed by the `setContentColorMetadata` and
     * `resetContentColorMetadata` methods.
     */
    override val contentColorMetadataSet: Boolean
        get() = mockSurfaceFeature?.contentColorMetadataSet ?: _contentColorMetadataSet

    private var _colorSpace: Int = SurfaceEntity.ColorSpace.BT709

    /**
     * The active color space of the media asset drawn on the surface. Use constants from
     * [androidx.xr.scenecore.runtime.SurfaceEntity.ColorSpace]. This value is used if
     * [contentColorMetadataSet] is `true`.
     */
    override val colorSpace: Int
        get() = mockSurfaceFeature?.colorSpace ?: _colorSpace

    private var _colorTransfer: Int = SurfaceEntity.ColorTransfer.LINEAR

    /**
     * The active color transfer function of the media asset drawn on the surface. Use constants
     * from [androidx.xr.scenecore.runtime.SurfaceEntity.ColorTransfer]. This value is used if
     * [contentColorMetadataSet] is `true`.
     */
    override val colorTransfer: Int
        get() = mockSurfaceFeature?.colorTransfer ?: _colorTransfer

    private var _colorRange: Int = SurfaceEntity.ColorRange.FULL

    /**
     * The active color range of the media asset drawn on the surface. Use constants from
     * [androidx.xr.scenecore.runtime.SurfaceEntity.ColorRange]. This value is used if
     * [contentColorMetadataSet] is `true`.
     */
    override val colorRange: Int
        get() = mockSurfaceFeature?.colorRange ?: _colorRange

    private var _maxCLL: Int = 0

    /**
     * The active maximum content light level (MaxCLL) in nits. A value of 0 indicates that MaxCLL
     * is not set or is unknown. This value is used if [contentColorMetadataSet] is `true`.
     */
    override val maxContentLightLevel: Int
        get() = mockSurfaceFeature?.maxContentLightLevel ?: _maxCLL

    /**
     * Sets the explicit color information for the surface content. This will also set
     * [contentColorMetadataSet] to `true`.
     *
     * @param colorSpace The runtime color space value (e.g., [SurfaceEntity.ColorSpace.BT709]).
     * @param colorTransfer The runtime color transfer value (e.g.,
     *   [SurfaceEntity.ColorTransfer.SRGB]).
     * @param colorRange The runtime color range value (e.g., [SurfaceEntity.ColorRange.FULL]).
     * @param maxCLL The maximum content light level in nits.
     */
    override fun setContentColorMetadata(
        colorSpace: Int,
        colorTransfer: Int,
        colorRange: Int,
        maxCLL: Int,
    ) {
        if (mockSurfaceFeature == null) {
            _colorSpace = colorSpace
            _colorTransfer = colorTransfer
            _colorRange = colorRange
            _maxCLL = maxCLL
        } else {
            mockSurfaceFeature!!.setContentColorMetadata(
                colorSpace,
                colorTransfer,
                colorRange,
                maxCLL,
            )
        }
    }

    /**
     * Resets the color information to the runtime's default handling. This will set
     * [contentColorMetadataSet] to `false` and typically involves reverting [colorSpace],
     * [colorTransfer], [colorRange], and [maxContentLightLevel] to their default runtime values.
     */
    override fun resetContentColorMetadata() {
        if (mockSurfaceFeature == null) {
            _colorSpace = SurfaceEntity.ColorSpace.BT709
            _colorTransfer = SurfaceEntity.ColorTransfer.LINEAR
            _colorRange = SurfaceEntity.ColorRange.FULL
            _maxCLL = 0
        } else {
            mockSurfaceFeature!!.resetContentColorMetadata()
        }
    }

    private var _edgeFeather: SurfaceEntity.EdgeFeather =
        SurfaceEntity.EdgeFeather.RectangleFeather(0.1f, 0.1f)

    /**
     * The edge feathering effect for the spatialized geometry.
     *
     * @throws IllegalStateException if the Entity has been disposed.
     */
    override var edgeFeather: SurfaceEntity.EdgeFeather
        get() = mockSurfaceFeature?.edgeFeather ?: _edgeFeather
        set(value) {
            if (mockSurfaceFeature == null) {
                _edgeFeather = value
            } else {
                mockSurfaceFeature!!.edgeFeather = value
            }
        }

    public companion object {
        public fun createWithMockFeature(
            feature: SurfaceFeature,
            nodeHolder: NodeHolder<*>,
        ): SurfaceFeature {
            val fakeSurfaceFeature = FakeSurfaceFeature(nodeHolder)
            fakeSurfaceFeature.mockSurfaceFeature = feature
            return fakeSurfaceFeature
        }
    }
}
