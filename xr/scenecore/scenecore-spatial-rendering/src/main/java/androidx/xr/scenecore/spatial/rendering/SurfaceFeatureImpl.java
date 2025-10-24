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

package androidx.xr.scenecore.spatial.rendering;

import android.view.Surface;

import androidx.xr.scenecore.impl.impress.ImpressApi;
import androidx.xr.scenecore.impl.impress.ImpressNode;
import androidx.xr.scenecore.impl.impress.Texture;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.SurfaceEntity;
import androidx.xr.scenecore.runtime.SurfaceEntity.ColorRange;
import androidx.xr.scenecore.runtime.SurfaceEntity.ColorSpace;
import androidx.xr.scenecore.runtime.SurfaceEntity.ColorTransfer;
import androidx.xr.scenecore.runtime.SurfaceEntity.StereoMode;
import androidx.xr.scenecore.runtime.SurfaceEntity.SuperSampling;
import androidx.xr.scenecore.runtime.SurfaceEntity.SurfaceProtection;
import androidx.xr.scenecore.runtime.SurfaceFeature;
import androidx.xr.scenecore.runtime.TextureResource;

import com.android.extensions.xr.XrExtensions;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;

import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of a RealityCore StereoSurfaceEntitySplitEngine.
 *
 * <p>This is used to create an entity that contains a StereoSurfacePanel using the Split Engine
 * route.
 */
final class SurfaceFeatureImpl extends BaseRenderingFeature implements SurfaceFeature {
    private final ImpressNode mEntityImpressNode;

    @StereoMode private int mStereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE;

    @SurfaceProtection private int mSurfaceProtection = SurfaceEntity.SurfaceProtection.NONE;

    @SuperSampling private int mSuperSampling = SurfaceEntity.SuperSampling.DEFAULT;

    private SurfaceEntity.Shape mShape;
    private SurfaceEntity.EdgeFeather mEdgeFeather;
    private boolean mContentColorMetadataSet = false;
    @ColorSpace private int mColorSpace = SurfaceEntity.ColorSpace.BT709;
    @ColorTransfer private int mColorTransfer = SurfaceEntity.ColorTransfer.SRGB;
    @ColorRange private int mColorRange = SurfaceEntity.ColorRange.FULL;
    private int mMaxContentLightLevel = 0;

    // Converts SurfaceEntity's SurfaceProtection to an Impress ContentSecurityLevel.
    private static int toImpressContentSecurityLevel(@SurfaceProtection int contentSecurityLevel) {
        switch (contentSecurityLevel) {
            case SurfaceEntity.SurfaceProtection.NONE:
                return ImpressApi.ContentSecurityLevel.NONE;
            case SurfaceEntity.SurfaceProtection.PROTECTED:
                return ImpressApi.ContentSecurityLevel.PROTECTED;
            default:
                return ImpressApi.ContentSecurityLevel.NONE;
        }
    }

    // Converts SurfaceEntity's SuperSampling to a boolean for Impress.
    private static boolean toImpressSuperSampling(@SuperSampling int superSampling) {
        switch (superSampling) {
            case SurfaceEntity.SuperSampling.NONE:
                return false;
            case SurfaceEntity.SuperSampling.DEFAULT:
                return true;
            default:
                return true;
        }
    }

    SurfaceFeatureImpl(
            ImpressApi impressApi,
            SplitEngineSubspaceManager splitEngineSubspaceManager,
            XrExtensions extensions,
            @StereoMode int stereoMode,
            SurfaceEntity.Shape canvasShape,
            @SurfaceProtection int surfaceProtection,
            @SuperSampling int superSampling) {
        super(impressApi, splitEngineSubspaceManager, extensions);
        mStereoMode = stereoMode;
        mSurfaceProtection = surfaceProtection;
        mSuperSampling = superSampling;
        mShape = canvasShape;

        try {
            // This is broken up into two steps to limit the size of the Impress Surface
            mEntityImpressNode =
                    mImpressApi.createStereoSurface(
                            stereoMode,
                            toImpressContentSecurityLevel(mSurfaceProtection),
                            toImpressSuperSampling(mSuperSampling));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
        setShape(mShape);

        bindImpressNodeToSubspace("stereo_surface_panel_entity_subspace_", mEntityImpressNode);
    }

    @Override
    public SurfaceEntity.@NonNull Shape getShape() {
        return mShape;
    }

    @Override
    public void setShape(SurfaceEntity.@NonNull Shape canvasShape) {
        mShape = canvasShape;

        if (mShape instanceof SurfaceEntity.Shape.Quad) {
            SurfaceEntity.Shape.Quad q = (SurfaceEntity.Shape.Quad) mShape;
            try {
                mImpressApi.setStereoSurfaceEntityCanvasShapeQuad(
                        mEntityImpressNode, q.getExtents().getWidth(), q.getExtents().getHeight());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            }
        } else if (mShape instanceof SurfaceEntity.Shape.Sphere) {
            SurfaceEntity.Shape.Sphere s = (SurfaceEntity.Shape.Sphere) mShape;
            try {
                mImpressApi.setStereoSurfaceEntityCanvasShapeSphere(
                        mEntityImpressNode, s.getRadius());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            }
        } else if (mShape instanceof SurfaceEntity.Shape.Hemisphere) {
            SurfaceEntity.Shape.Hemisphere h = (SurfaceEntity.Shape.Hemisphere) mShape;
            try {
                mImpressApi.setStereoSurfaceEntityCanvasShapeHemisphere(
                        mEntityImpressNode, h.getRadius());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported canvas shape: " + mShape);
        }
    }

    @Override
    public void setColliderEnabled(boolean enableCollider) {
        mImpressApi.setStereoSurfaceEntityColliderEnabled(mEntityImpressNode, enableCollider);
    }

    @Override
    public void setEdgeFeather(SurfaceEntity.@NonNull EdgeFeather edgeFeather) {
        mEdgeFeather = edgeFeather;
        if (mEdgeFeather instanceof SurfaceEntity.EdgeFeather.RectangleFeather) {
            SurfaceEntity.EdgeFeather.RectangleFeather s =
                    (SurfaceEntity.EdgeFeather.RectangleFeather) mEdgeFeather;
            try {
                mImpressApi.setFeatherRadiusForStereoSurface(
                        mEntityImpressNode, s.getLeftRight(), s.getTopBottom());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            }
        } else if (mEdgeFeather instanceof SurfaceEntity.EdgeFeather.NoFeathering) {
            try {
                mImpressApi.setFeatherRadiusForStereoSurface(mEntityImpressNode, 0.0f, 0.0f);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported edge feather: " + mEdgeFeather);
        }
    }

    @Override
    public SurfaceEntity.@NonNull EdgeFeather getEdgeFeather() {
        return mEdgeFeather;
    }

    @Override
    public void setStereoMode(@StereoMode int mode) {
        try {
            mImpressApi.setStereoModeForStereoSurface(mEntityImpressNode, mode);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
        mStereoMode = mode;
    }

    @Override
    public @NonNull Dimensions getDimensions() {
        return mShape.getDimensions();
    }

    @Override
    @StereoMode
    public int getStereoMode() {
        return mStereoMode;
    }

    @Override
    public void setPrimaryAlphaMaskTexture(@Nullable TextureResource alphaMask) {
        long alphaMaskToken = -1;
        if (alphaMask != null) {
            if (!(alphaMask instanceof Texture)) {
                throw new IllegalArgumentException("TextureResource is not a Texture");
            }
            alphaMaskToken = ((Texture) alphaMask).getNativeHandle();
        }
        try {
            mImpressApi.setPrimaryAlphaMaskForStereoSurface(mEntityImpressNode, alphaMaskToken);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setAuxiliaryAlphaMaskTexture(@Nullable TextureResource alphaMask) {
        long alphaMaskToken = -1;
        if (alphaMask != null) {
            if (!(alphaMask instanceof Texture)) {
                throw new IllegalArgumentException("TextureResource is not a Texture");
            }
            alphaMaskToken = ((Texture) alphaMask).getNativeHandle();
        }
        try {
            mImpressApi.setAuxiliaryAlphaMaskForStereoSurface(mEntityImpressNode, alphaMaskToken);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public @NonNull Surface getSurface() {
        // TODO Either cache the surface in the constructor, or change this interface to return a
        //  Future.
        try {
            return mImpressApi.getSurfaceFromStereoSurface(mEntityImpressNode);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setSurfacePixelDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Surface dimensions must be positive.");
        }
        try {
            mImpressApi.setStereoSurfaceEntitySurfaceSize(mEntityImpressNode, width, height);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @ColorSpace
    public int getColorSpace() {
        return mColorSpace;
    }

    @Override
    @ColorTransfer
    public int getColorTransfer() {
        return mColorTransfer;
    }

    @Override
    @ColorRange
    public int getColorRange() {
        return mColorRange;
    }

    @Override
    public int getMaxContentLightLevel() {
        return mMaxContentLightLevel;
    }

    @Override
    public boolean getContentColorMetadataSet() {
        return mContentColorMetadataSet;
    }

    @Override
    public void setContentColorMetadata(
            @ColorSpace int colorSpace,
            @ColorTransfer int colorTransfer,
            @ColorRange int colorRange,
            int maxCLL) {
        mColorSpace = colorSpace;
        mColorTransfer = colorTransfer;
        mColorRange = colorRange;
        mMaxContentLightLevel = maxCLL;
        mContentColorMetadataSet = true;
        try {
            mImpressApi.setContentColorMetadataForStereoSurface(
                    mEntityImpressNode, colorSpace, colorTransfer, colorRange, maxCLL);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void resetContentColorMetadata() {
        mColorSpace = SurfaceEntity.ColorSpace.BT709;
        mColorTransfer = SurfaceEntity.ColorTransfer.SRGB;
        mColorRange = SurfaceEntity.ColorRange.FULL;
        mMaxContentLightLevel = 0;
        mContentColorMetadataSet = false;
        mImpressApi.resetContentColorMetadataForStereoSurface(mEntityImpressNode);
    }

    // Note this returns the Impress node for the entity, not the subspace. The subspace Impress
    // node is the parent of the entity Impress node.
    @VisibleForTesting
    ImpressNode getEntityImpressNode() {
        return mEntityImpressNode;
    }
}
