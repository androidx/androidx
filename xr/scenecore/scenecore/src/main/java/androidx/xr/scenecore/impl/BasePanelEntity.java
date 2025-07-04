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

import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.core.util.TypedValueCompat;
import androidx.xr.runtime.internal.CameraViewActivityPose;
import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.PanelEntity;
import androidx.xr.runtime.internal.PerceivedResolutionResult;
import androidx.xr.runtime.internal.PixelDimensions;
import androidx.xr.runtime.internal.Space;
import androidx.xr.runtime.math.Vector3;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransaction;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.ScheduledExecutorService;

/** BasePanelEntity provides implementations of capabilities common to PanelEntities. */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
abstract class BasePanelEntity extends AndroidXrEntity implements PanelEntity {
    private static final float DEFAULT_CORNER_RADIUS_DP = 32.0f;
    protected PixelDimensions mPixelDimensions;
    private float mCornerRadius;

    BasePanelEntity(
            Context context,
            Node node,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(context, node, extensions, entityManager, executor);
    }

    protected float getDefaultPixelDensity() {
        return mExtensions
                .getConfig()
                .defaultPixelsPerMeter(Resources.getSystem().getDisplayMetrics().density);
    }

    protected float getDefaultCornerRadiusInMeters() {
        // Get the width and height of the panel in DP.
        float widthDp =
                TypedValueCompat.deriveDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        mPixelDimensions.width,
                        Resources.getSystem().getDisplayMetrics());
        float heightDp =
                TypedValueCompat.deriveDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        mPixelDimensions.height,
                        Resources.getSystem().getDisplayMetrics());
        float radiusDp = DEFAULT_CORNER_RADIUS_DP;

        // If the pixel dimensions are smaller than the default corner radius, use the smaller of
        // the
        // two dimensions as the corner radius.
        if (mPixelDimensions != null
                && (widthDp < DEFAULT_CORNER_RADIUS_DP * 2
                        || heightDp < DEFAULT_CORNER_RADIUS_DP * 2)) {
            radiusDp = min(widthDp / 2, heightDp / 2);
        }

        // Convert the updated corner radius to pixels.
        float radiusPixels =
                TypedValueCompat.dpToPx(radiusDp, Resources.getSystem().getDisplayMetrics());

        // Convert the pixel radius to meters.
        return radiusPixels / getDefaultPixelDensity();
    }

    @Override
    public @NonNull Dimensions getSize() {
        float pixelDensity = getDefaultPixelDensity();
        return new Dimensions(
                mPixelDimensions.width / pixelDensity, mPixelDimensions.height / pixelDensity, 0);
    }

    @Override
    public void setSize(@NonNull Dimensions dimensions) {
        float pixelDensity = getDefaultPixelDensity();
        setSizeInPixels(
                new PixelDimensions(
                        (int) (dimensions.width * pixelDensity),
                        (int) (dimensions.height * pixelDensity)));
    }

    @Override
    public @NonNull PixelDimensions getSizeInPixels() {
        return mPixelDimensions;
    }

    @Override
    public void setSizeInPixels(@NonNull PixelDimensions dimensions) {
        mPixelDimensions = dimensions;
    }

    @Override
    public @NonNull PerceivedResolutionResult getPerceivedResolution() {
        // Get the Camera View with which to compute Perceived Resolution
        CameraViewActivityPose cameraView =
                PerceivedResolutionUtils.getPerceivedResolutionCameraView(mEntityManager);
        if (cameraView == null) {
            return new PerceivedResolutionResult.InvalidCameraView();
        }

        // Compute the width, height, and distance to camera, of the panel in activity space units
        float panelWidthInActivitySpace = getSize().width * getScale(Space.ACTIVITY).getX();
        float panelHeightInActivitySpace = getSize().height * getScale(Space.ACTIVITY).getY();
        Vector3 cameraPositionInActivitySpace = cameraView.getActivitySpacePose().getTranslation();
        float PanelDistanceToCameraInActivitySpace =
                Vector3.distance(
                        cameraPositionInActivitySpace, getPose(Space.ACTIVITY).getTranslation());

        return PerceivedResolutionUtils.getPerceivedResolutionOfPanel(
                cameraView,
                panelWidthInActivitySpace,
                panelHeightInActivitySpace,
                PanelDistanceToCameraInActivitySpace);
    }

    @Override
    public void setCornerRadius(float value) {
        if (value < 0.0f) {
            throw new IllegalArgumentException("Corner radius can't be negative: " + value);
        }
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setCornerRadius(mNode, value).apply();
            mCornerRadius = value;
        }
    }

    // Sets just the value of the corner radius, without updating the node. This should be only be
    // used when constructing the entity so that the stored value is consistent with the value set
    // in
    // the node transaction.
    public void setCornerRadiusValue(float value) {
        mCornerRadius = value;
    }

    @Override
    public float getCornerRadius() {
        return mCornerRadius;
    }
}
