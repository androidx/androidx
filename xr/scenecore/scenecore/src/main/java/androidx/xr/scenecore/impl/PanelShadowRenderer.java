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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceControlViewHost.SurfacePackage;
import android.view.View;

import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransaction;

import java.util.Objects;

/** Class for rendering the border of a panel onto a perception plane. */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
class PanelShadowRenderer {
    private static final float STROKE_WIDTH = 20f;
    private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
    private static final float CORNER_RADIUS = 20f;
    private static final float PANEL_BORDER_ADDED_MARGIN = 50f;
    private final ActivitySpaceImpl mActivitySpaceImpl;
    private final PerceptionSpaceActivityPoseImpl mPerceptionSpaceActivityPose;
    private final XrExtensions mExtensions;
    private SurfaceControlViewHost mSurfaceControlViewHost;
    private final Handler mHandler;
    private boolean mIsVisible;
    private final Activity mActivity;
    Node mPanelShadowNode;

    /** PanelShadowView is a view with a blue border to enable the shadow effect. */
    private static class PanelShadowView extends View {

        PanelShadowView(Context context) {
            super(context);
        }

        @Override
        public void onDrawForeground(Canvas canvas) {
            super.onDrawForeground(canvas);
            Path border = new Path();
            border.addRoundRect(
                    HALF_STROKE_WIDTH,
                    HALF_STROKE_WIDTH,
                    canvas.getWidth() - HALF_STROKE_WIDTH,
                    canvas.getHeight() - HALF_STROKE_WIDTH,
                    CORNER_RADIUS,
                    CORNER_RADIUS,
                    Path.Direction.CW);
            Paint paint = new Paint();
            paint.setColor(0xFF54639F);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(STROKE_WIDTH);
            canvas.drawPath(border, paint);
        }
    }

    PanelShadowRenderer(
            ActivitySpaceImpl activitySpaceImpl,
            PerceptionSpaceActivityPoseImpl perceptionSpaceActivityPose,
            Activity activity,
            XrExtensions extensions) {
        mActivitySpaceImpl = activitySpaceImpl;
        mPerceptionSpaceActivityPose = perceptionSpaceActivityPose;
        mExtensions = extensions;
        mActivity = activity;
        mHandler = new Handler(Looper.getMainLooper());
    }

    void updatePanelPose(
            Pose openXrToProposedPanel, Pose openXrtoPlane, BasePanelEntity panelEntity) {
        // If there is no panel shadow node, create it.
        if (mPanelShadowNode == null) {
            createPanelShadow(openXrToProposedPanel, openXrtoPlane, panelEntity);
            return;
        }

        Pose panelPoseInActivitySpace =
                getUpdatedPanelPoseInActivitySpace(openXrToProposedPanel, openXrtoPlane);
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            if (!mIsVisible) {
                NodeTransaction unused = transaction.setVisibility(mPanelShadowNode, true);
                mIsVisible = true;
            }
            transaction
                    .setPosition(
                            mPanelShadowNode,
                            panelPoseInActivitySpace.getTranslation().getX(),
                            panelPoseInActivitySpace.getTranslation().getY(),
                            panelPoseInActivitySpace.getTranslation().getZ())
                    .setOrientation(
                            mPanelShadowNode,
                            panelPoseInActivitySpace.getRotation().getX(),
                            panelPoseInActivitySpace.getRotation().getY(),
                            panelPoseInActivitySpace.getRotation().getZ(),
                            panelPoseInActivitySpace.getRotation().getW())
                    .apply();
        }
    }

    void hidePlane() {
        if (!mIsVisible || mPanelShadowNode == null) {
            return;
        }
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setVisibility(mPanelShadowNode, false).apply();
        }
        mIsVisible = false;
    }

    void destroy() {
        if (mSurfaceControlViewHost != null) {
            mHandler.post(() -> mSurfaceControlViewHost.release());
        }
        if (mPanelShadowNode != null) {
            try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
                transaction.setParent(mPanelShadowNode, null).apply();
            }
        }
        mPanelShadowNode = null;
    }

    private void createPanelShadow(
            Pose openXrToProposedPanel, Pose openXrtoPlane, BasePanelEntity panelEntity) {
        View view = new PanelShadowView(mActivity);

        // Scale the panel shadow to the size of the PanelEntity in the activity space.
        Vector3 entityScale = panelEntity.getWorldSpaceScale();
        float sizeX =
                panelEntity.getSizeInPixels().width
                        * entityScale.getX()
                        / mActivitySpaceImpl.getWorldSpaceScale().getX()
                        + PANEL_BORDER_ADDED_MARGIN;
        float sizeZ =
                panelEntity.getSizeInPixels().height
                        * entityScale.getZ()
                        / mActivitySpaceImpl.getWorldSpaceScale().getX()
                        + PANEL_BORDER_ADDED_MARGIN;

        Pose panelPoseInActivitySpace =
                getUpdatedPanelPoseInActivitySpace(openXrToProposedPanel, openXrtoPlane);

        mPanelShadowNode = mExtensions.createNode();

        // The surfaceControlViewHost needs to be created on the main thread.
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        mHandler.post(
                () -> {
                    mSurfaceControlViewHost =
                            new SurfaceControlViewHost(
                                    mActivity,
                                    Objects.requireNonNull(mActivity.getDisplay()),
                                    new Binder());
                    mSurfaceControlViewHost.setView(view, (int) sizeX, (int) sizeZ);
                    SurfacePackage surfacePackage =
                            Objects.requireNonNull(mSurfaceControlViewHost.getSurfacePackage());
                    try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
                        transaction
                                .setName(mPanelShadowNode, "PanelRenderer")
                                .setSurfacePackage(mPanelShadowNode, surfacePackage)
                                .setWindowBounds(surfacePackage, (int) sizeX, (int) sizeZ)
                                .setVisibility(mPanelShadowNode, true)
                                .setPosition(
                                        mPanelShadowNode,
                                        panelPoseInActivitySpace.getTranslation().getX(),
                                        panelPoseInActivitySpace.getTranslation().getY(),
                                        panelPoseInActivitySpace.getTranslation().getZ())
                                .setOrientation(
                                        mPanelShadowNode,
                                        panelPoseInActivitySpace.getRotation().getX(),
                                        panelPoseInActivitySpace.getRotation().getY(),
                                        panelPoseInActivitySpace.getRotation().getZ(),
                                        panelPoseInActivitySpace.getRotation().getW())
                                .setParent(mPanelShadowNode, mActivitySpaceImpl.getNode())
                                .apply();
                    }
                    surfacePackage.release();
                });
        mIsVisible = true;
    }

    private Pose getUpdatedPanelPoseInActivitySpace(
            Pose openXrToProposedPanel, Pose openXrtoPlane) {
        Pose planeToOpenXr = openXrtoPlane.getInverse();
        Pose planeToPanel = planeToOpenXr.compose(openXrToProposedPanel);
        Pose planeToProjectedPanel =
                new Pose(
                        new Vector3(
                                planeToPanel.getTranslation().getX(),
                                0f,
                                planeToPanel.getTranslation().getZ()),
                        planeToOpenXr
                                .getRotation()
                                .times(
                                        PlaneUtils.rotateEntityToPlane(
                                                openXrToProposedPanel.getRotation(),
                                                openXrtoPlane.getRotation())));
        Pose panelInOxr = openXrtoPlane.compose(planeToProjectedPanel);
        return mPerceptionSpaceActivityPose.transformPoseTo(panelInOxr, mActivitySpaceImpl);
    }
}
