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

package androidx.xr.scenecore.impl;

import android.content.Context;
import android.util.Log;
import android.view.Surface;

import androidx.xr.runtime.internal.CameraViewActivityPose;
import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.PerceivedResolutionResult;
import androidx.xr.runtime.internal.Space;
import androidx.xr.runtime.internal.SurfaceEntity;
import androidx.xr.runtime.internal.SurfaceEntity.CanvasShape;
import androidx.xr.runtime.internal.TextureResource;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.impress.ImpressApi;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.NodeTransaction;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of a RealityCore StereoSurfaceEntitySplitEngine.
 *
 * <p>This is used to create an entity that contains a StereoSurfacePanel using the Split Engine
 * route.
 */
final class SurfaceEntityImpl extends AndroidXrEntity implements SurfaceEntity {
    private final ImpressApi mImpressApi;
    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager;
    private final SubspaceNode mSubspace;
    // TODO: b/362520810 - Wrap impress nodes w/ Java class.
    private final int mEntityImpressNode;
    private final int mSubspaceImpressNode;
    @StereoMode private int mStereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE;

    @ContentSecurityLevel
    private int mContentSecurityLevel = SurfaceEntity.ContentSecurityLevel.NONE;

    @SuperSampling private int mSuperSampling = SurfaceEntity.SuperSampling.DEFAULT;

    private CanvasShape mCanvasShape;
    private EdgeFeather mEdgeFeather;
    private boolean mContentColorMetadataSet = false;
    @ColorSpace private int mColorSpace = SurfaceEntity.ColorSpace.BT709;
    @ColorTransfer private int mColorTransfer = SurfaceEntity.ColorTransfer.SRGB;
    @ColorRange private int mColorRange = SurfaceEntity.ColorRange.FULL;
    private int mMaxContentLightLevel = 0;

    // Converts SurfaceEntity's ContentSecurityLevel to an Impress ContentSecurityLevel.
    private static int toImpressContentSecurityLevel(
            @ContentSecurityLevel int contentSecurityLevel) {
        switch (contentSecurityLevel) {
            case ContentSecurityLevel.NONE:
                return ImpressApi.ContentSecurityLevel.NONE;
            case ContentSecurityLevel.PROTECTED:
                return ImpressApi.ContentSecurityLevel.PROTECTED;
            default:
                Log.e(
                        "SurfaceEntityImpl",
                        "Unsupported content security level: "
                                + contentSecurityLevel
                                + ". Defaulting to NONE.");
                return ImpressApi.ContentSecurityLevel.NONE;
        }
    }

    // Converts SurfaceEntity's SuperSampling to a boolean for Impress.
    private static boolean toImpressSuperSampling(@SuperSampling int superSampling) {
        switch (superSampling) {
            case SuperSampling.NONE:
                return false;
            case SuperSampling.DEFAULT:
                return true;
            default:
                Log.e(
                        "SurfaceEntityImpl",
                        "Unsupported super sampling value: "
                                + superSampling
                                + ". Defaulting to true (DEFAULT).");
                return true;
        }
    }

    SurfaceEntityImpl(
            Context context,
            Entity parentEntity,
            ImpressApi impressApi,
            SplitEngineSubspaceManager splitEngineSubspaceManager,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            @StereoMode int stereoMode,
            CanvasShape canvasShape,
            @ContentSecurityLevel int contentSecurityLevel,
            @SuperSampling int superSampling) {
        super(context, extensions.createNode(), extensions, entityManager, executor);
        mImpressApi = impressApi;
        mSplitEngineSubspaceManager = splitEngineSubspaceManager;
        mStereoMode = stereoMode;
        mContentSecurityLevel = contentSecurityLevel;
        mSuperSampling = superSampling;
        mCanvasShape = canvasShape;
        setParent(parentEntity);

        // System will only render Impress nodes that are parented by this subspace node.
        mSubspaceImpressNode = impressApi.createImpressNode();
        String subspaceName = "stereo_surface_panel_entity_subspace_" + mSubspaceImpressNode;

        mSubspace = splitEngineSubspaceManager.createSubspace(subspaceName, mSubspaceImpressNode);

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            // Make the Entity node a parent of the subspace node.
            transaction.setParent(mSubspace.getSubspaceNode(), mNode).apply();
        }

        try {
            // This is broken up into two steps to limit the size of the Impress Surface
            mEntityImpressNode =
                    mImpressApi.createStereoSurface(
                            stereoMode,
                            toImpressContentSecurityLevel(mContentSecurityLevel),
                            toImpressSuperSampling(mSuperSampling));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
        setCanvasShape(mCanvasShape);

        // The CPM node hierarchy is: Entity CPM node --- parent of ---> Subspace CPM node.
        // The Impress node hierarchy is: Subspace Impress node --- parent of ---> Entity Impress
        // node.
        try {
            impressApi.setImpressNodeParent(mEntityImpressNode, mSubspaceImpressNode);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public CanvasShape getCanvasShape() {
        return mCanvasShape;
    }

    @Override
    public void setCanvasShape(CanvasShape canvasShape) {
        mCanvasShape = canvasShape;

        if (mCanvasShape instanceof CanvasShape.Quad) {
            CanvasShape.Quad q = (CanvasShape.Quad) mCanvasShape;
            try {
                mImpressApi.setStereoSurfaceEntityCanvasShapeQuad(
                        mEntityImpressNode, q.getWidth(), q.getHeight());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            }
        } else if (mCanvasShape instanceof CanvasShape.Vr360Sphere) {
            CanvasShape.Vr360Sphere s = (CanvasShape.Vr360Sphere) mCanvasShape;
            try {
                mImpressApi.setStereoSurfaceEntityCanvasShapeSphere(mEntityImpressNode,
                                                                    s.getRadius());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            }
        } else if (mCanvasShape instanceof CanvasShape.Vr180Hemisphere) {
            CanvasShape.Vr180Hemisphere h = (CanvasShape.Vr180Hemisphere) mCanvasShape;
            try {
                mImpressApi.setStereoSurfaceEntityCanvasShapeHemisphere(mEntityImpressNode,
                                                                        h.getRadius());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported canvas shape: " + mCanvasShape);
        }
    }

    @Override
    public void setEdgeFeather(EdgeFeather edgeFeather) {
        mEdgeFeather = edgeFeather;
        if (mEdgeFeather instanceof EdgeFeather.SmoothFeather) {
            EdgeFeather.SmoothFeather s = (EdgeFeather.SmoothFeather) mEdgeFeather;
            try {
                mImpressApi.setFeatherRadiusForStereoSurface(
                        mEntityImpressNode, s.getLeftRight(), s.getTopBottom());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            }
        } else if (mEdgeFeather instanceof EdgeFeather.SolidEdge) {
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
    public EdgeFeather getEdgeFeather() {
        return mEdgeFeather;
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        // The subspace impress node will be destroyed when the subspace is deleted.
        mSplitEngineSubspaceManager.deleteSubspace(mSubspace.subspaceId);
        super.dispose();
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
    public Dimensions getDimensions() {
        return mCanvasShape.getDimensions();
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
            if (!(alphaMask instanceof TextureResourceImpl)) {
                throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
            }
            alphaMaskToken = ((TextureResourceImpl) alphaMask).getTextureToken();
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
            if (!(alphaMask instanceof TextureResourceImpl)) {
                throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
            }
            alphaMaskToken = ((TextureResourceImpl) alphaMask).getTextureToken();
        }
        try {
            mImpressApi.setAuxiliaryAlphaMaskForStereoSurface(mEntityImpressNode, alphaMaskToken);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Surface getSurface() {
        // TODO Either cache the surface in the constructor, or change this interface to
        // return a Future.
        try {
          return mImpressApi.getSurfaceFromStereoSurface(mEntityImpressNode);
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
    public int getMaxCLL() {
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
    public int getEntityImpressNode() {
        return mEntityImpressNode;
    }

    @Override
    public @NonNull PerceivedResolutionResult getPerceivedResolution() {
        // Get the Camera View with which to compute Perceived Resolution
        CameraViewActivityPose cameraView =
                PerceivedResolutionUtils.getPerceivedResolutionCameraView(mEntityManager);
        if (cameraView == null) {
            return new PerceivedResolutionResult.InvalidCameraView();
        }

        // Compute the width, height, and depth in activity space units
        Dimensions dimensionsInLocalUnits = getDimensions();
        Vector3 activitySpaceScale = getScale(Space.ACTIVITY);
        Dimensions dimensionsInActivitySpace =
                new Dimensions(
                        dimensionsInLocalUnits.width * activitySpaceScale.getX(),
                        dimensionsInLocalUnits.height * activitySpaceScale.getY(),
                        dimensionsInLocalUnits.depth * activitySpaceScale.getZ());

        return PerceivedResolutionUtils.getPerceivedResolutionOf3DBox(
                cameraView,
                /* boxDimensionsInActivitySpace= */ dimensionsInActivitySpace,
                /* boxPositionInActivitySpace= */ getPose(Space.ACTIVITY).getTranslation());
    }
}
