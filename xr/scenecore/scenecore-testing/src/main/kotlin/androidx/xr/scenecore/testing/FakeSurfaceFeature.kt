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

    @SurfaceEntity.StereoMode override var stereoMode: Int = SurfaceEntity.StereoMode.MONO

    @SurfaceEntity.MediaBlendingMode
    override var mediaBlendingMode: Int = SurfaceEntity.MediaBlendingMode.TRANSPARENT

    override var shape: SurfaceEntity.Shape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f))

    override val dimensions: Dimensions
        get() = shape.dimensions

    private var internalImageReader: ImageReader? =
        ImageReader.newInstance(1, 1, ImageFormat.YUV_420_888, 1)

    override var surface: Surface = internalImageReader!!.surface
        private set

    /** For test purposes only. Caches the input of [setSurfacePixelDimensions]. */
    public var surfacePixelDimensions: IntSize2d = IntSize2d(0, 0)
        private set

    override fun setSurfacePixelDimensions(width: Int, height: Int) {
        surfacePixelDimensions = IntSize2d(width, height)
    }

    /**
     * For test purposes only. Caches the most recent value passed to [setColliderEnabled].
     *
     * This allows tests to verify whether the collider for the surface's geometry was enabled or
     * disabled.
     */
    public var colliderEnabled: Boolean = false
        private set

    override fun setColliderEnabled(enableCollider: Boolean) {
        colliderEnabled = enableCollider
    }

    /** For test purposes only. Represents the result of [setPrimaryAlphaMaskTexture]. */
    public var primaryAlphaMask: TextureResource? = null
        private set

    override fun setPrimaryAlphaMaskTexture(alphaMask: TextureResource?) {
        // TODO: b/471066885 - Implements FakeTexture
        primaryAlphaMask = alphaMask
    }

    /**
     * For test purposes only. Represents the result of [setAuxiliaryAlphaMaskTexture].
     *
     * This allows tests to inspect the `TextureResource` that was set as the auxiliary alpha mask.
     */
    public var auxiliaryAlphaMask: TextureResource? = null
        private set

    override fun setAuxiliaryAlphaMaskTexture(alphaMask: TextureResource?) {
        // TODO: b/471066885 - Implements FakeTexture
        auxiliaryAlphaMask = alphaMask
    }

    override var contentColorMetadataSet: Boolean = false
        private set

    override var colorSpace: Int = SurfaceEntity.ColorSpace.BT709
        private set

    override var colorTransfer: Int = SurfaceEntity.ColorTransfer.LINEAR
        private set

    override var colorRange: Int = SurfaceEntity.ColorRange.FULL
        private set

    override var maxContentLightLevel: Int = 0
        private set

    override fun setContentColorMetadata(
        colorSpace: Int,
        colorTransfer: Int,
        colorRange: Int,
        maxCLL: Int,
    ) {
        this.colorSpace = colorSpace
        this.colorTransfer = colorTransfer
        this.colorRange = colorRange
        maxContentLightLevel = maxCLL

        contentColorMetadataSet = true
    }

    override fun resetContentColorMetadata() {
        colorSpace = SurfaceEntity.ColorSpace.BT709
        colorTransfer = SurfaceEntity.ColorTransfer.LINEAR
        colorRange = SurfaceEntity.ColorRange.FULL
        maxContentLightLevel = 0

        contentColorMetadataSet = false
    }

    override var edgeFeather: SurfaceEntity.EdgeFeather = SurfaceEntity.EdgeFeather.NoFeathering()

    override fun dispose() {
        super.dispose()

        surface.release()
        internalImageReader?.close()
        internalImageReader = null
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
        internalImageReader?.close()
        internalImageReader = null
        this.surface = surface
    }
}
