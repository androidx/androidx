/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.wear.widget;

import android.content.Context;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.support.wear.R;
import android.view.View;

/**
 * An implementation of the {@link WearableLinearLayoutManager.LayoutCallback} aligning the children
 * of the associated {@link WearableRecyclerView} along a pre-defined vertical curve.
 */
public class CurvingLayoutCallback extends WearableLinearLayoutManager.LayoutCallback {
    private static final float EPSILON = 0.001f;

    private final Path mCurvePath;
    private final PathMeasure mPathMeasure;
    private int mCurvePathHeight;
    private int mXCurveOffset;
    private float mPathLength;
    private float mCurveBottom;
    private float mCurveTop;
    private float mLineGradient;
    private final float[] mPathPoints = new float[2];
    private final float[] mPathTangent = new float[2];
    private final float[] mAnchorOffsetXY = new float[2];

    private RecyclerView mParentView;
    private boolean mIsScreenRound;
    private int mLayoutWidth;
    private int mLayoutHeight;

    public CurvingLayoutCallback(Context context) {
        mCurvePath = new Path();
        mPathMeasure = new PathMeasure();
        mIsScreenRound = context.getResources().getConfiguration().isScreenRound();
        mXCurveOffset = context.getResources().getDimensionPixelSize(
                R.dimen.ws_wrv_curve_default_x_offset);
    }

    @Override
    public void onLayoutFinished(View child, RecyclerView parent) {
        if (mParentView != parent || (mParentView != null && (
                mParentView.getWidth() != parent.getWidth()
                        || mParentView.getHeight() != parent.getHeight()))) {
            mParentView = parent;
            mLayoutWidth = mParentView.getWidth();
            mLayoutHeight = mParentView.getHeight();
        }
        if (mIsScreenRound) {
            maybeSetUpCircularInitialLayout(mLayoutWidth, mLayoutHeight);
            mAnchorOffsetXY[0] = mXCurveOffset;
            mAnchorOffsetXY[1] = child.getHeight() / 2.0f;
            adjustAnchorOffsetXY(child, mAnchorOffsetXY);
            float minCenter = -(float) child.getHeight() / 2;
            float maxCenter = mLayoutHeight + (float) child.getHeight() / 2;
            float range = maxCenter - minCenter;
            float verticalAnchor = (float) child.getTop() + mAnchorOffsetXY[1];
            float mYScrollProgress = (verticalAnchor + Math.abs(minCenter)) / range;

            mPathMeasure.getPosTan(mYScrollProgress * mPathLength, mPathPoints, mPathTangent);

            boolean topClusterRisk =
                    Math.abs(mPathPoints[1] - mCurveBottom) < EPSILON
                            && minCenter < mPathPoints[1];
            boolean bottomClusterRisk =
                    Math.abs(mPathPoints[1] - mCurveTop) < EPSILON
                            && maxCenter > mPathPoints[1];
            // Continue offsetting the child along the straight-line part of the curve, if it
            // has not gone off the screen when it reached the end of the original curve.
            if (topClusterRisk || bottomClusterRisk) {
                mPathPoints[1] = verticalAnchor;
                mPathPoints[0] = (Math.abs(verticalAnchor) * mLineGradient);
            }

            // Offset the View to match the provided anchor point.
            int newLeft = (int) (mPathPoints[0] - mAnchorOffsetXY[0]);
            child.offsetLeftAndRight(newLeft - child.getLeft());
            float verticalTranslation = mPathPoints[1] - verticalAnchor;
            child.setTranslationY(verticalTranslation);
        } else {
            child.setTranslationY(0);
        }
    }

    /**
     * Override this method if you wish to adjust the anchor coordinates for each child view
     * during a layout pass. In the override set the new desired anchor coordinates in
     * the provided array. The coordinates should be provided in relation to the child view.
     *
     * @param child          The child view to which the anchor coordinates will apply.
     * @param anchorOffsetXY The anchor coordinates for the provided child view, by default set
     *                       to a pre-defined constant on the horizontal axis and half of the
     *                       child height on the vertical axis (vertical center).
     */
    public void adjustAnchorOffsetXY(View child, float[] anchorOffsetXY) {
        return;
    };

    @VisibleForTesting
    void setRound(boolean isScreenRound) {
        mIsScreenRound = isScreenRound;
    }

    @VisibleForTesting
    void setOffset(int offset) {
        mXCurveOffset = offset;
    }

    /** Set up the initial layout for round screens. */
    private void maybeSetUpCircularInitialLayout(int width, int height) {
        // The values in this function are custom to the curve we use.
        if (mCurvePathHeight != height) {
            mCurvePathHeight = height;
            mCurveBottom = -0.048f * height;
            mCurveTop = 1.048f * height;
            mLineGradient = 0.5f / 0.048f;
            mCurvePath.reset();
            mCurvePath.moveTo(0.5f * width, mCurveBottom);
            mCurvePath.lineTo(0.34f * width, 0.075f * height);
            mCurvePath.cubicTo(
                    0.22f * width, 0.17f * height, 0.13f * width, 0.32f * height, 0.13f * width,
                    height / 2);
            mCurvePath.cubicTo(
                    0.13f * width,
                    0.68f * height,
                    0.22f * width,
                    0.83f * height,
                    0.34f * width,
                    0.925f * height);
            mCurvePath.lineTo(width / 2, mCurveTop);
            mPathMeasure.setPath(mCurvePath, false);
            mPathLength = mPathMeasure.getLength();
        }
    }
}
