/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.scenecore.spatial.rendering

import android.view.Surface
import androidx.annotation.VisibleForTesting
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.impl.impress.Texture
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.SurfaceFeature
import androidx.xr.scenecore.runtime.TextureResource
import com.android.extensions.xr.XrExtensions
import com.google.androidxr.splitengine.SplitEngineSubspaceManager

/**
 * Implementation of a RealityCore StereoSurfaceEntitySplitEngine.
 *
 * <p>This is used to create an entity that contains a StereoSurfacePanel using the Split Engine
 * route.
 */
internal class SurfaceFeatureImpl(
    impressApi: ImpressApi,
    splitEngineSubspaceManager: SplitEngineSubspaceManager,
    extensions: XrExtensions,
    @SurfaceEntity.StereoMode stereoMode: Int,
    @SurfaceEntity.MediaBlendingMode mediaBlendingMode: Int,
    canvasShape: SurfaceEntity.Shape,
    @SurfaceEntity.SurfaceProtection surfaceProtection: Int,
    @SurfaceEntity.SuperSampling superSampling: Int,
) : BaseRenderingFeature(impressApi, splitEngineSubspaceManager, extensions), SurfaceFeature {

    @get:VisibleForTesting internal val entityImpressNode: ImpressNode

    @get:SurfaceEntity.StereoMode
    override var stereoMode: Int = stereoMode
        set(value) {
            try {
                impressApi.setStereoModeForStereoSurface(entityImpressNode, value)
            } catch (e: IllegalArgumentException) {
                throw IllegalStateException(e)
            }
            field = value
        }

    @get:SurfaceEntity.MediaBlendingMode
    override var mediaBlendingMode: Int = SurfaceEntity.MediaBlendingMode.TRANSPARENT
        set(value) {
            try {
                impressApi.setBlendingModeForStereoSurfaceEntity(entityImpressNode, value)
            } catch (e: IllegalArgumentException) {
                throw IllegalStateException(e)
            }
            field = value
        }

    @SurfaceEntity.SurfaceProtection private val surfaceProtection: Int = surfaceProtection

    @SurfaceEntity.SuperSampling private val superSampling: Int = superSampling

    override var shape: SurfaceEntity.Shape = canvasShape
        set(value) {
            field = value
            when (value) {
                is SurfaceEntity.Shape.Quad -> {
                    try {
                        impressApi.setStereoSurfaceEntityCanvasShapeQuad(
                            entityImpressNode,
                            value.extents.width,
                            value.extents.height,
                            value.cornerRadius,
                        )
                    } catch (e: IllegalArgumentException) {
                        throw IllegalStateException(e)
                    }
                }
                is SurfaceEntity.Shape.Sphere -> {
                    try {
                        impressApi.setStereoSurfaceEntityCanvasShapeSphere(
                            entityImpressNode,
                            value.radius,
                        )
                    } catch (e: IllegalArgumentException) {
                        throw IllegalStateException(e)
                    }
                }
                is SurfaceEntity.Shape.Hemisphere -> {
                    try {
                        impressApi.setStereoSurfaceEntityCanvasShapeHemisphere(
                            entityImpressNode,
                            value.radius,
                        )
                    } catch (e: IllegalArgumentException) {
                        throw IllegalStateException(e)
                    }
                }
                is SurfaceEntity.Shape.CustomMesh -> {
                    try {
                        impressApi.setStereoSurfaceEntityCanvasShapeCustomMesh(
                            entityImpressNode,
                            value.leftEye.positions,
                            value.leftEye.texCoords,
                            value.leftEye?.indices,
                            value.rightEye?.positions,
                            value.rightEye?.texCoords,
                            value.rightEye?.indices,
                            value.drawMode,
                        )
                    } catch (e: IllegalArgumentException) {
                        throw IllegalStateException(e)
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unsupported canvas shape: $value")
                }
            }
        }

    override var edgeFeather: SurfaceEntity.EdgeFeather = SurfaceEntity.EdgeFeather.NoFeathering()
        set(value) {
            field = value
            when (value) {
                is SurfaceEntity.EdgeFeather.RectangleFeather -> {
                    try {
                        impressApi.setFeatherRadiusForStereoSurface(
                            entityImpressNode,
                            value.leftRight,
                            value.topBottom,
                        )
                    } catch (e: IllegalArgumentException) {
                        throw IllegalStateException(e)
                    }
                }
                is SurfaceEntity.EdgeFeather.NoFeathering -> {
                    try {
                        impressApi.setFeatherRadiusForStereoSurface(entityImpressNode, 0.0f, 0.0f)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalStateException(e)
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unsupported edge feather: $value")
                }
            }
        }

    override var contentColorMetadataSet: Boolean = false
        private set

    @get:SurfaceEntity.ColorSpace
    override var colorSpace: Int = SurfaceEntity.ColorSpace.BT709
        private set

    @get:SurfaceEntity.ColorTransfer
    override var colorTransfer: Int = SurfaceEntity.ColorTransfer.SRGB
        private set

    @get:SurfaceEntity.ColorRange
    override var colorRange: Int = SurfaceEntity.ColorRange.FULL
        private set

    override var maxContentLightLevel: Int = 0
        private set

    init {
        try {
            // This is broken up into two steps to limit the size of the Impress Surface
            entityImpressNode =
                impressApi.createStereoSurface(
                    stereoMode,
                    toImpressMediaBlendingMode(mediaBlendingMode),
                    toImpressContentSecurityLevel(surfaceProtection),
                    toImpressSuperSampling(superSampling),
                )
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(e)
        }
        this.shape = canvasShape

        bindImpressNodeToSubspace("stereo_surface_panel_entity_subspace_", entityImpressNode)
    }

    override fun setColliderEnabled(enableCollider: Boolean) =
        impressApi.setStereoSurfaceEntityColliderEnabled(entityImpressNode, enableCollider)

    override val dimensions: Dimensions
        get() = shape.dimensions

    override fun setPrimaryAlphaMaskTexture(alphaMask: TextureResource?) {
        var alphaMaskToken: Long = -1
        if (alphaMask != null) {
            require(alphaMask is Texture) { "TextureResource is not a Texture" }
            alphaMaskToken = alphaMask.nativeHandle
        }
        try {
            impressApi.setPrimaryAlphaMaskForStereoSurface(entityImpressNode, alphaMaskToken)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(e)
        }
    }

    override fun setAuxiliaryAlphaMaskTexture(alphaMask: TextureResource?) {
        var alphaMaskToken: Long = -1
        if (alphaMask != null) {
            require(alphaMask is Texture) { "TextureResource is not a Texture" }
            alphaMaskToken = alphaMask.nativeHandle
        }
        try {
            impressApi.setAuxiliaryAlphaMaskForStereoSurface(entityImpressNode, alphaMaskToken)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(e)
        }
    }

    override val surface: Surface
        get() {
            // TODO Either cache the surface in the constructor, or change this interface to return
            // a Future.
            try {
                return impressApi.getSurfaceFromStereoSurface(entityImpressNode)
            } catch (e: IllegalArgumentException) {
                throw IllegalStateException(e)
            }
        }

    override fun setSurfacePixelDimensions(width: Int, height: Int) {
        require(width > 0 && height > 0) { "Surface dimensions must be positive." }
        if (surfaceProtection == SurfaceEntity.SurfaceProtection.PROTECTED) {
            throw IllegalStateException(
                "Cannot set surface pixel dimensions for protected surfaces."
            )
        }
        try {
            impressApi.setStereoSurfaceEntitySurfaceSize(entityImpressNode, width, height)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(e)
        }
    }

    override fun setContentColorMetadata(
        @SurfaceEntity.ColorSpace colorSpace: Int,
        @SurfaceEntity.ColorTransfer colorTransfer: Int,
        @SurfaceEntity.ColorRange colorRange: Int,
        maxCLL: Int,
    ) {
        this.colorSpace = colorSpace
        this.colorTransfer = colorTransfer
        this.colorRange = colorRange
        this.maxContentLightLevel = maxCLL
        this.contentColorMetadataSet = true
        try {
            impressApi.setContentColorMetadataForStereoSurface(
                entityImpressNode,
                colorSpace,
                colorTransfer,
                colorRange,
                maxCLL,
            )
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(e)
        }
    }

    override fun resetContentColorMetadata() {
        colorSpace = SurfaceEntity.ColorSpace.BT709
        colorTransfer = SurfaceEntity.ColorTransfer.SRGB
        colorRange = SurfaceEntity.ColorRange.FULL
        maxContentLightLevel = 0
        contentColorMetadataSet = false
        impressApi.resetContentColorMetadataForStereoSurface(entityImpressNode)
    }

    companion object {
        // Converts SurfaceEntity's MediaBlendingMode to an Impress MediaBlendingMode.
        private fun toImpressMediaBlendingMode(
            @SurfaceEntity.MediaBlendingMode mediaBlendingMode: Int
        ): Int {
            return when (mediaBlendingMode) {
                SurfaceEntity.MediaBlendingMode.TRANSPARENT ->
                    ImpressApi.MediaBlendingMode.TRANSPARENT
                SurfaceEntity.MediaBlendingMode.OPAQUE -> ImpressApi.MediaBlendingMode.OPAQUE
                else -> ImpressApi.MediaBlendingMode.TRANSPARENT
            }
        }

        // Converts SurfaceEntity's SurfaceProtection to an Impress ContentSecurityLevel.
        private fun toImpressContentSecurityLevel(
            @SurfaceEntity.SurfaceProtection contentSecurityLevel: Int
        ): Int {
            return when (contentSecurityLevel) {
                SurfaceEntity.SurfaceProtection.NONE -> ImpressApi.ContentSecurityLevel.NONE
                SurfaceEntity.SurfaceProtection.PROTECTED ->
                    ImpressApi.ContentSecurityLevel.PROTECTED
                else -> ImpressApi.ContentSecurityLevel.NONE
            }
        }

        // Converts SurfaceEntity's SuperSampling to a boolean for Impress.
        private fun toImpressSuperSampling(
            @SurfaceEntity.SuperSampling superSampling: Int
        ): Boolean {
            return when (superSampling) {
                SurfaceEntity.SuperSampling.NONE -> false
                SurfaceEntity.SuperSampling.DEFAULT -> true
                else -> true
            }
        }
    }
}
