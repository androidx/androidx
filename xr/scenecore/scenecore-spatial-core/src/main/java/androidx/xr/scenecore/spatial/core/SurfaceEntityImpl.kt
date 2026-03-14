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
package androidx.xr.scenecore.spatial.core

import android.content.Context
import android.view.Surface
import androidx.xr.runtime.FieldOfView
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.SurfaceFeature
import androidx.xr.scenecore.runtime.TextureResource
import com.android.extensions.xr.XrExtensions
import java.util.concurrent.ScheduledExecutorService

/**
 * Implementation of a SceneCore SurfaceEntity.
 *
 * This is used to create an Entity that uses SplitEngine to render an Android Surface with support
 * for stereoscopic rendering.
 */
internal class SurfaceEntityImpl(
    context: Context,
    private val surfaceFeature: SurfaceFeature,
    parent: Entity?,
    extensions: XrExtensions,
    entityManager: EntityManager,
    executor: ScheduledExecutorService,
) :
    BaseRenderingEntity(context, surfaceFeature, extensions, entityManager, executor),
    SurfaceEntity {

    init {
        this.parent = parent
    }

    @SurfaceEntity.StereoMode
    override var stereoMode: Int
        get() = surfaceFeature.stereoMode
        set(value) {
            surfaceFeature.stereoMode = value
        }

    @SurfaceEntity.MediaBlendingMode
    override var mediaBlendingMode: Int
        get() = surfaceFeature.mediaBlendingMode
        set(value) {
            surfaceFeature.mediaBlendingMode = value
        }

    override var shape: SurfaceEntity.Shape
        get() = surfaceFeature.shape
        set(shape) {
            surfaceFeature.shape = shape
        }

    override fun setSurfacePixelDimensions(width: Int, height: Int) {
        surfaceFeature.setSurfacePixelDimensions(width, height)
    }

    override var edgeFeather: SurfaceEntity.EdgeFeather
        get() = surfaceFeature.edgeFeather
        set(value) {
            surfaceFeature.edgeFeather = value
        }

    @SurfaceEntity.ColorRange
    override val colorRange: Int
        get() = surfaceFeature.colorRange

    @SurfaceEntity.ColorTransfer
    override val colorTransfer: Int
        get() = surfaceFeature.colorTransfer

    @SurfaceEntity.ColorSpace
    override val colorSpace: Int
        get() = surfaceFeature.colorSpace

    override val maxContentLightLevel: Int
        get() = surfaceFeature.maxContentLightLevel

    override val contentColorMetadataSet: Boolean
        get() = surfaceFeature.contentColorMetadataSet

    override val dimensions: Dimensions
        get() = surfaceFeature.dimensions

    override val surface: Surface
        get() = surfaceFeature.surface

    override fun dispose() {
        surfaceFeature.dispose()
        super.dispose()
    }

    fun setColliderEnabled(enableCollider: Boolean) {
        surfaceFeature.setColliderEnabled(enableCollider)
    }

    override fun setPrimaryAlphaMaskTexture(alphaMask: TextureResource?) {
        surfaceFeature.setPrimaryAlphaMaskTexture(alphaMask)
    }

    override fun setAuxiliaryAlphaMaskTexture(alphaMask: TextureResource?) {
        surfaceFeature.setAuxiliaryAlphaMaskTexture(alphaMask)
    }

    override fun setContentColorMetadata(
        @SurfaceEntity.ColorSpace colorSpace: Int,
        @SurfaceEntity.ColorTransfer colorTransfer: Int,
        @SurfaceEntity.ColorRange colorRange: Int,
        maxContentLightLevel: Int,
    ) {
        surfaceFeature.setContentColorMetadata(
            colorSpace,
            colorTransfer,
            colorRange,
            maxContentLightLevel,
        )
    }

    override fun resetContentColorMetadata() {
        surfaceFeature.resetContentColorMetadata()
    }

    override fun getPerceivedResolution(
        renderViewScenePose: ScenePose,
        renderViewFov: FieldOfView,
    ): PerceivedResolutionResult {
        // Compute the width, height, and depth in activity space units
        val dimensionsInLocalUnits = dimensions
        val activitySpaceScale = getScale(Space.ACTIVITY)
        val dimensionsInActivitySpace =
            Dimensions(
                dimensionsInLocalUnits.width * activitySpaceScale.x,
                dimensionsInLocalUnits.height * activitySpaceScale.y,
                dimensionsInLocalUnits.depth * activitySpaceScale.z,
            )

        return getPerceivedResolutionOf3DBox(
            renderViewScenePose,
            renderViewFov,
            /* viewPlaneInPixels= */ getDisplayResolutionInPixels(checkNotNull(context)),
            /* boxDimensionsInActivitySpace= */ dimensionsInActivitySpace,
            /* boxPositionInActivitySpace= */ getPose(Space.ACTIVITY).translation,
        )
    }
}
