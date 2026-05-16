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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import android.view.Surface
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.SurfaceFeature
import androidx.xr.scenecore.runtime.TextureResource
import androidx.xr.scenecore.testing.internal.FakeSurfaceFeature as InternalFakeSurfaceFeature

/** Test-only implementation of [androidx.xr.scenecore.runtime.SurfaceFeature] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeSurfaceFeature
internal constructor(
    nodeHolder: NodeHolder<*>,
    internal val fakeInternal: InternalFakeSurfaceFeature,
) : FakeBaseRenderingFeature(nodeHolder), SurfaceFeature {

    public constructor(
        nodeHolder: NodeHolder<*>
    ) : this(nodeHolder, InternalFakeSurfaceFeature(nodeHolder))

    @SurfaceEntity.StereoMode
    override var stereoMode: Int
        get() = fakeInternal.stereoMode
        set(value) {
            fakeInternal.stereoMode = value
        }

    @SurfaceEntity.MediaBlendingMode
    override var mediaBlendingMode: Int
        get() = fakeInternal.mediaBlendingMode
        set(value) {
            fakeInternal.mediaBlendingMode = value
        }

    override var shape: SurfaceEntity.Shape
        get() = fakeInternal.shape
        set(value) {
            fakeInternal.shape = value
        }

    override val dimensions: Dimensions
        get() = shape.dimensions

    override val surface: Surface
        get() = fakeInternal.surface

    /** For test purposes only. Caches the input of [setSurfacePixelDimensions]. */
    public val surfacePixelDimensions: IntSize2d
        get() = fakeInternal.surfacePixelDimensions

    override fun setSurfacePixelDimensions(width: Int, height: Int) {
        fakeInternal.setSurfacePixelDimensions(width, height)
    }

    /**
     * For test purposes only. Caches the most recent value passed to [setColliderEnabled].
     *
     * This allows tests to verify whether the collider for the surface's geometry was enabled or
     * disabled.
     */
    public val colliderEnabled: Boolean
        get() = fakeInternal.colliderEnabled

    override fun setColliderEnabled(enableCollider: Boolean) {
        fakeInternal.setColliderEnabled(enableCollider)
    }

    /** For test purposes only. Represents the result of [setPrimaryAlphaMaskTexture]. */
    public val primaryAlphaMask: TextureResource?
        get() = fakeInternal.primaryAlphaMask

    override fun setPrimaryAlphaMaskTexture(alphaMask: TextureResource?) {
        fakeInternal.setPrimaryAlphaMaskTexture(alphaMask)
    }

    /**
     * For test purposes only. Represents the result of [setAuxiliaryAlphaMaskTexture].
     *
     * This allows tests to inspect the `TextureResource` that was set as the auxiliary alpha mask.
     */
    public val auxiliaryAlphaMask: TextureResource?
        get() = fakeInternal.auxiliaryAlphaMask

    override fun setAuxiliaryAlphaMaskTexture(alphaMask: TextureResource?) {
        fakeInternal.setAuxiliaryAlphaMaskTexture(alphaMask)
    }

    override var contentColorMetadataSet: Boolean
        get() = fakeInternal.contentColorMetadataSet
        set(value) {
            fakeInternal.contentColorMetadataSet = value
        }

    override val colorSpace: Int
        get() = fakeInternal.colorSpace

    override val colorTransfer: Int
        get() = fakeInternal.colorTransfer

    override val colorRange: Int
        get() = fakeInternal.colorRange

    override val maxContentLightLevel: Int
        get() = fakeInternal.maxContentLightLevel

    override fun setContentColorMetadata(
        colorSpace: Int,
        colorTransfer: Int,
        colorRange: Int,
        maxCLL: Int,
    ) {
        fakeInternal.setContentColorMetadata(colorSpace, colorTransfer, colorRange, maxCLL)
    }

    override fun resetContentColorMetadata() {
        fakeInternal.resetContentColorMetadata()
    }

    override var edgeFeather: SurfaceEntity.EdgeFeather
        get() = fakeInternal.edgeFeather
        set(value) {
            fakeInternal.edgeFeather = value
        }

    override fun dispose() {
        super.dispose()
        fakeInternal.dispose()
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
        fakeInternal.setSurface(surface)
    }
}
