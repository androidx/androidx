/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.testing.internal

import android.view.Surface
import androidx.xr.runtime.math.FieldOfView
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.SurfaceEntity.Shape
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
internal class FakeSurfaceEntity(
    private val feature: FakeSurfaceFeature =
        FakeSurfaceFeature(NodeHolder<FakeNode>(object : FakeNode {}, FakeNode::class.java))
) : FakeEntity(), SurfaceEntity {

    /**
     * Specifies how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     */
    @SurfaceEntity.StereoMode
    override var stereoMode: Int
        get() = feature.stereoMode
        set(value) {
            feature.stereoMode = value
        }

    /** Specifies the blending mode of the content. */
    @SurfaceEntity.MediaBlendingMode
    override var mediaBlendingMode: Int
        get() = feature.mediaBlendingMode
        set(value) {
            feature.mediaBlendingMode = value
        }

    /** Specifies the shape of the spatial canvas which the surface is texture mapped to. */
    override var shape: Shape
        get() = feature.shape
        set(value) {
            feature.shape = value
        }

    /**
     * Retrieves the dimensions of the "spatial canvas" which the surface is mapped to. These values
     * are not impacted by scale.
     */
    override val dimensions: Dimensions
        get() = feature.shape.dimensions

    /**
     * Retrieves the surface that the Entity will display. The app can write into this surface
     * however it wants, i.e. MediaPlayer, ExoPlayer, or custom rendering.
     *
     * @return an Android [Surface]
     */
    override val surface: Surface
        get() = feature.surface

    /** For test purposes only. Caches the input of [setSurfacePixelDimensions]. */
    val surfacePixelDimensions: IntSize2d
        get() = feature.surfacePixelDimensions

    /**
     * Sets the dimensions of the Surface in pixels.
     *
     * @param width The width of the Surface in pixels.
     * @param height The height of the Surface in pixels.
     * @throws IllegalArgumentException if the dimensions are invalid.
     */
    override fun setSurfacePixelDimensions(width: Int, height: Int) {
        feature.setSurfacePixelDimensions(width, height)
    }

    /**
     * For test purposes only. Sets or replaces the underlying [Surface] for this fake entity.
     *
     * <p>This allows tests to provide a specific [Surface] instance, such as one connected to a
     * test-controlled producer, to verify rendering behavior.
     *
     * @param surface The new [Surface] to associate with this entity.
     */
    fun setSurface(surface: Surface) {
        feature.setSurface(surface)
    }

    /** For test purposes only. Represents the result of [setPrimaryAlphaMaskTexture]. */
    val primaryAlphaMask: TextureResource?
        get() = feature.primaryAlphaMask

    /**
     * The texture to be composited into the alpha channel of the surface. If null, the alpha mask
     * will be disabled.
     *
     * @param alphaMask The primary alpha mask texture.
     */
    override fun setPrimaryAlphaMaskTexture(alphaMask: TextureResource?) {
        feature.setPrimaryAlphaMaskTexture(alphaMask)
    }

    /** For test purposes only. Represents the result of [setAuxiliaryAlphaMaskTexture] */
    val auxiliaryAlphaMask: TextureResource?
        get() = feature.auxiliaryAlphaMask

    /**
     * The texture to be composited into the alpha channel of the auxiliary view of the surface.
     * This is only used for interleaved stereo content. If null, the alpha mask will be disabled.
     *
     * @param alphaMask The auxiliary alpha mask texture.
     */
    override fun setAuxiliaryAlphaMaskTexture(alphaMask: TextureResource?) {
        feature.setAuxiliaryAlphaMaskTexture(alphaMask)
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
    fun setPerceivedResolution(perceivedResolution: PerceivedResolutionResult) {
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
     *     - [PerceivedResolutionResult.Success] containing the
     *       [androidx.xr.scenecore.runtime.PixelDimensions] if the calculation is successful.
     *     - [PerceivedResolutionResult.EntityTooClose] if the entity is too close to the camera.
     *     - [PerceivedResolutionResult.InvalidRenderViewpoint] if the camera information required
     *       for the calculation is invalid or unavailable.
     *
     * @see androidx.xr.scenecore.runtime.PerceivedResolutionResult
     */
    override fun getPerceivedResolution(
        renderViewScenePose: ScenePose,
        renderViewFov: FieldOfView,
    ): PerceivedResolutionResult {
        return perceivedResolutionResult
    }

    /**
     * The active color space of the media asset drawn on the surface. Use constants from
     * [androidx.xr.scenecore.runtime.SurfaceEntity.ColorSpace]. This value is used if
     * [contentColorMetadataSet] is `true`.
     */
    override val colorSpace: Int
        get() = feature.colorSpace

    /**
     * The active color transfer function of the media asset drawn on the surface. Use constants
     * from [androidx.xr.scenecore.runtime.SurfaceEntity.ColorTransfer]. This value is used if
     * [contentColorMetadataSet] is `true`.
     */
    override val colorTransfer: Int
        get() = feature.colorTransfer

    /**
     * The active color range of the media asset drawn on the surface. Use constants from
     * [androidx.xr.scenecore.runtime.SurfaceEntity.ColorRange]. This value is used if
     * [contentColorMetadataSet] is `true`.
     */
    override val colorRange: Int
        get() = feature.colorRange

    /**
     * The active maximum content light level (MaxCLL) in nits. A value of 0 indicates that MaxCLL
     * is not set or is unknown. This value is used if [contentColorMetadataSet] is `true`.
     */
    override val maxContentLightLevel: Int
        get() = feature.maxContentLightLevel

    /**
     * Indicates whether explicit color information has been set for the surface content. If
     * `false`, the runtime should signal the backend to use its best effort color correction and
     * tone mapping. If `true`, the runtime should inform the backend to use the values specified in
     * [colorSpace], [colorTransfer], [colorRange], and [maxContentLightLevel] for color correction
     * and tone mapping of the surface content.
     *
     * This property is typically managed by the `setContentColorMetadata` and
     * `resetContentColorMetadata` methods.
     */
    override val contentColorMetadataSet: Boolean
        get() = feature.contentColorMetadataSet

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
        feature.setContentColorMetadata(colorSpace, colorTransfer, colorRange, maxContentLightLevel)
    }

    /**
     * Resets the color information to the runtime's default handling. This will set
     * [contentColorMetadataSet] to `false` and typically involves reverting [colorSpace],
     * [colorTransfer], [colorRange], and [maxContentLightLevel] to their default runtime values.
     */
    override fun resetContentColorMetadata() {
        feature.resetContentColorMetadata()
    }

    /**
     * The edge feathering effect for the spatialized geometry.
     *
     * @throws IllegalStateException if the Entity has been disposed.
     */
    override var edgeFeather: SurfaceEntity.EdgeFeather
        get() = feature.edgeFeather
        set(value) {
            feature.edgeFeather = value
        }
}
