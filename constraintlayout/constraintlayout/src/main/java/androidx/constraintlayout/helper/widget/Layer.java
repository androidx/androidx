/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.helper.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.widget.ConstraintHelper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.R;

/**
 * Layer adds the ability to move and rotate a group of views as if they were contained
 * in a viewGroup
 * <b>Added in 2.0</b>
 * Methods such as setRotation(float) rotate all views about a common center.
 * For simple visibility manipulation use Group
 *
 */
public class Layer extends ConstraintHelper {
    private static final String TAG = "Layer";
    private float mRotationCenterX = Float.NaN;
    private float mRotationCenterY = Float.NaN;
    private float mGroupRotateAngle = Float.NaN;
    ConstraintLayout mContainer;
    private float mScaleX = 1;
    private float mScaleY = 1;
    protected float mComputedCenterX = Float.NaN;
    protected float mComputedCenterY = Float.NaN;

    protected float mComputedMaxX = Float.NaN;
    protected float mComputedMaxY = Float.NaN;
    protected float mComputedMinX = Float.NaN;
    protected float mComputedMinY = Float.NaN;
    boolean mNeedBounds = true;
    View[] mViews = null; // used to reduce the getViewById() cost
    private float mShiftX = 0;
    private float mShiftY = 0;

    private boolean mApplyVisibilityOnAttach;
    private boolean mApplyElevationOnAttach;

    public Layer(Context context) {
        super(context);
    }

    public Layer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Layer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @param attrs
     *
     */
    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        mUseViewMeasure = false;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.ConstraintLayout_Layout);
            final int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_android_visibility) {
                    mApplyVisibilityOnAttach = true;
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_elevation) {
                    mApplyElevationOnAttach = true;
                }
            }
            a.recycle();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mContainer = (ConstraintLayout) getParent();
        if (mApplyVisibilityOnAttach || mApplyElevationOnAttach) {
            int visibility = getVisibility();
            float elevation = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                elevation = getElevation();
            }
            for (int i = 0; i < mCount; i++) {
                int id = mIds[i];
                View view = mContainer.getViewById(id);
                if (view != null) {
                    if (mApplyVisibilityOnAttach) {
                        view.setVisibility(visibility);
                    }
                    if (mApplyElevationOnAttach) {
                        if (elevation > 0
                                && android.os.Build.VERSION.SDK_INT
                                >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            view.setTranslationZ(view.getTranslationZ() + elevation);
                        }
                    }
                }

            }
        }
    }

    /**
     * @param container
     *
     */
    @Override
    public void updatePreDraw(ConstraintLayout container) {
        mContainer = container;
        float rotate = getRotation();
        if (rotate == 0) {
            if (!Float.isNaN(mGroupRotateAngle)) {
                mGroupRotateAngle = rotate;
            }
        } else {
            mGroupRotateAngle = rotate;
        }
    }

    /**
     * Rotates all associated views around a single point post layout..
     * The point is the middle of the bounding box or set by setPivotX,setPivotX;
     * @param angle
     */
    @Override
    public void setRotation(float angle) {
        mGroupRotateAngle = angle;
        transform();
    }
    /**
     * Scales all associated views around a single point post layout..
     * The point is the middle of the bounding box or set by setPivotX,setPivotX;
     * @param scaleX The value to scale in X.
     */
    @Override
    public void setScaleX(float scaleX) {
        mScaleX = scaleX;
        transform();
    }

    /**
     * Scales all associated views around a single point post layout..
     * The point is the middle of the bounding box or set by setPivotX,setPivotX;
     * @param scaleY The value to scale in X.
     */
    @Override
    public void setScaleY(float scaleY) {
        mScaleY = scaleY;
        transform();
    }

    /**
     * Sets the pivot point for scale operations.
     * Setting it to Float.NaN (default) results in the center of the group being used.
     * @param pivotX The X location of the pivot point
     */
    @Override
    public void setPivotX(float pivotX) {
        mRotationCenterX = pivotX;
        transform();
    }

    /**
     * Sets the pivot point for scale operations.
     * Setting it to Float.NaN (default) results in the center of the group being used.
     * @param pivotY The Y location of the pivot point
     */
    @Override
    public void setPivotY(float pivotY) {
        mRotationCenterY = pivotY;
        transform();
    }

    /**
     * Shift all the views in the X direction post layout.
     * @param dx number of pixes to shift
     */
    @Override
    public void setTranslationX(float dx) {
        mShiftX = dx;
        transform();

    }
    /**
     * Shift all the views in the Y direction post layout.
     * @param dy number of pixes to shift
     */
    @Override
    public void setTranslationY(float dy) {
        mShiftY = dy;
        transform();
    }

    /**
     *
     */
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        applyLayoutFeatures();
    }

    /**
     *
     */
    @Override
    public void setElevation(float elevation) {
        super.setElevation(elevation);
        applyLayoutFeatures();
    }

    /**
     * @param container
     *
     */
    @Override
    public void updatePostLayout(ConstraintLayout container) {
        reCacheViews();

        mComputedCenterX = Float.NaN;
        mComputedCenterY = Float.NaN;
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        ConstraintWidget widget = params.getConstraintWidget();
        widget.setWidth(0);
        widget.setHeight(0);
        calcCenters();
        int left = (int) mComputedMinX - getPaddingLeft();
        int top = (int) mComputedMinY - getPaddingTop();
        int right = (int) mComputedMaxX + getPaddingRight();
        int bottom = (int) mComputedMaxY + getPaddingBottom();
        layout(left, top, right, bottom);
        transform();
    }

    private void reCacheViews() {
        if (mContainer == null) {
            return;
        }
        if (mCount == 0) {
            return;
        }

        if (mViews == null || mViews.length != mCount) {
            mViews = new View[mCount];
        }
        for (int i = 0; i < mCount; i++) {
            int id = mIds[i];
            mViews[i] = mContainer.getViewById(id);
        }
    }

    protected void calcCenters() {
        if (mContainer == null) {
            return;
        }
        if (!mNeedBounds) {
            if (!(Float.isNaN(mComputedCenterX) || Float.isNaN(mComputedCenterY))) {
                return;
            }
        }
        if (Float.isNaN(mRotationCenterX) || Float.isNaN(mRotationCenterY)) {
            View[] views = getViews(mContainer);

            int minx = views[0].getLeft();
            int miny = views[0].getTop();
            int maxx = views[0].getRight();
            int maxy = views[0].getBottom();

            for (int i = 0; i < mCount; i++) {
                View view = views[i];
                minx = Math.min(minx, view.getLeft());
                miny = Math.min(miny, view.getTop());
                maxx = Math.max(maxx, view.getRight());
                maxy = Math.max(maxy, view.getBottom());
            }

            mComputedMaxX = maxx;
            mComputedMaxY = maxy;
            mComputedMinX = minx;
            mComputedMinY = miny;

            if (Float.isNaN(mRotationCenterX)) {
                mComputedCenterX = (minx + maxx) / 2;
            } else {
                mComputedCenterX = mRotationCenterX;
            }
            if (Float.isNaN(mRotationCenterY)) {
                mComputedCenterY = (miny + maxy) / 2;

            } else {
                mComputedCenterY = mRotationCenterY;
            }

        } else {
            mComputedCenterY = mRotationCenterY;
            mComputedCenterX = mRotationCenterX;
        }

    }

    private void transform() {
        if (mContainer == null) {
            return;
        }
        if (mViews == null) {
            reCacheViews();
        }
        calcCenters();

        double rad = Float.isNaN(mGroupRotateAngle) ? 0.0 : Math.toRadians(mGroupRotateAngle);
        float sin = (float) Math.sin(rad);
        float cos = (float) Math.cos(rad);
        float m11 = mScaleX * cos;
        float m12 = -mScaleY * sin;
        float m21 = mScaleX * sin;
        float m22 = mScaleY * cos;

        for (int i = 0; i < mCount; i++) {
            View view = mViews[i];
            int x = (view.getLeft() + view.getRight()) / 2;
            int y = (view.getTop() + view.getBottom()) / 2;
            float dx = x - mComputedCenterX;
            float dy = y - mComputedCenterY;
            float shiftx = m11 * dx + m12 * dy - dx + mShiftX;
            float shifty = m21 * dx + m22 * dy - dy + mShiftY;

            view.setTranslationX(shiftx);
            view.setTranslationY(shifty);
            view.setScaleY(mScaleY);
            view.setScaleX(mScaleX);
            if (!Float.isNaN(mGroupRotateAngle)) {
                view.setRotation(mGroupRotateAngle);
            }
        }
    }

    /**
     *
     * @param container
     */
    @Override
    protected void applyLayoutFeaturesInConstraintSet(ConstraintLayout container) {
        applyLayoutFeatures(container);
    }
}
