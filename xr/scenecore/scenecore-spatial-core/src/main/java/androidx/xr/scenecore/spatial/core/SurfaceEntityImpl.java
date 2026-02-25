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

package androidx.xr.scenecore.spatial.core;

import static androidx.xr.scenecore.spatial.core.PerceivedResolutionUtils.getDisplayResolutionInPixels;

import android.content.Context;
import android.view.Surface;

import androidx.xr.runtime.FieldOfView;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.PerceivedResolutionResult;
import androidx.xr.scenecore.runtime.ScenePose;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.SurfaceEntity;
import androidx.xr.scenecore.runtime.SurfaceFeature;
import androidx.xr.scenecore.runtime.TextureResource;

import com.android.extensions.xr.XrExtensions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of a SceneCore SurfaceEntity.
 *
 * <p>This is used to create an Entity that uses SplitEngine to render an Android Surface with
 * support for stereoscopic rendering.
 */
final class SurfaceEntityImpl extends BaseRenderingEntity implements SurfaceEntity {
    private final SurfaceFeature mSurfaceFeature;

    SurfaceEntityImpl(
            Context context,
            SurfaceFeature surfaceFeature,
            Entity parentEntity,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(context, surfaceFeature, extensions, entityManager, executor);
        mSurfaceFeature = surfaceFeature;
        setParent(parentEntity);
    }

    @Override
    public @NonNull Shape getShape() {
        return mSurfaceFeature.getShape();
    }

    @Override
    public void setShape(@NonNull Shape shape) {
        mSurfaceFeature.setShape(shape);
    }

    @Override
    public void setSurfacePixelDimensions(int width, int height) {
        mSurfaceFeature.setSurfacePixelDimensions(width, height);
    }

    @Override
    public @NonNull EdgeFeather getEdgeFeather() {
        return mSurfaceFeature.getEdgeFeather();
    }

    @Override
    public void setEdgeFeather(@NonNull EdgeFeather edgeFeather) {
        mSurfaceFeature.setEdgeFeather(edgeFeather);
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        mSurfaceFeature.dispose();
        super.dispose();
    }

    @Override
    @NonNull
    public Dimensions getDimensions() {
        return mSurfaceFeature.getDimensions();
    }

    @Override
    @StereoMode
    public int getStereoMode() {
        return mSurfaceFeature.getStereoMode();
    }

    @Override
    public void setStereoMode(@StereoMode int mode) {
        mSurfaceFeature.setStereoMode(mode);
    }

    @Override
    @MediaBlendingMode
    public int getMediaBlendingMode() {
        return mSurfaceFeature.getMediaBlendingMode();
    }

    @Override
    public void setMediaBlendingMode(@MediaBlendingMode int mode) {
        mSurfaceFeature.setMediaBlendingMode(mode);
    }

    public void setColliderEnabled(boolean enableCollider) {
        mSurfaceFeature.setColliderEnabled(enableCollider);
    }

    @Override
    public void setPrimaryAlphaMaskTexture(@Nullable TextureResource alphaMask) {
        mSurfaceFeature.setPrimaryAlphaMaskTexture(alphaMask);
    }

    @Override
    public void setAuxiliaryAlphaMaskTexture(@Nullable TextureResource alphaMask) {
        mSurfaceFeature.setAuxiliaryAlphaMaskTexture(alphaMask);
    }

    @Override
    public @NonNull Surface getSurface() {
        return mSurfaceFeature.getSurface();
    }

    @Override
    @ColorSpace
    public int getColorSpace() {
        return mSurfaceFeature.getColorSpace();
    }

    @Override
    @ColorTransfer
    public int getColorTransfer() {
        return mSurfaceFeature.getColorTransfer();
    }

    @Override
    @ColorRange
    public int getColorRange() {
        return mSurfaceFeature.getColorRange();
    }

    @Override
    public int getMaxContentLightLevel() {
        return mSurfaceFeature.getMaxContentLightLevel();
    }

    @Override
    public boolean getContentColorMetadataSet() {
        return mSurfaceFeature.getContentColorMetadataSet();
    }

    @Override
    public void setContentColorMetadata(
            @ColorSpace int colorSpace,
            @ColorTransfer int colorTransfer,
            @ColorRange int colorRange,
            int maxCLL) {
        mSurfaceFeature.setContentColorMetadata(colorSpace, colorTransfer, colorRange, maxCLL);
    }

    @Override
    public void resetContentColorMetadata() {
        mSurfaceFeature.resetContentColorMetadata();
    }

    @Override
    public @NonNull PerceivedResolutionResult getPerceivedResolution(
            @NonNull ScenePose renderViewScenePose, @NonNull FieldOfView renderViewFov) {
        // Compute the width, height, and depth in activity space units
        Dimensions dimensionsInLocalUnits = getDimensions();
        Vector3 activitySpaceScale = getScale(Space.ACTIVITY);
        Dimensions dimensionsInActivitySpace =
                new Dimensions(
                        dimensionsInLocalUnits.width * activitySpaceScale.getX(),
                        dimensionsInLocalUnits.height * activitySpaceScale.getY(),
                        dimensionsInLocalUnits.depth * activitySpaceScale.getZ());

        return PerceivedResolutionUtils.getPerceivedResolutionOf3DBox(
                renderViewScenePose,
                renderViewFov,
                /* viewPlaneInPixels= */ getDisplayResolutionInPixels(
                        Objects.requireNonNull(getContext())),
                /* boxDimensionsInActivitySpace= */ dimensionsInActivitySpace,
                /* boxPositionInActivitySpace= */ getPose(Space.ACTIVITY).getTranslation());
    }
}
