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
package androidx.constraintlayout.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 *
 * This defines the internally defined Constraint set
 * It allows you to have a group of References which point to other views and provide them with
 * constraint attributes
 *
 */
public class Constraints extends ViewGroup {

    public static final String TAG = "Constraints";
    ConstraintSet mConstraintSet;

    public Constraints(Context context) {
        super(context);
        super.setVisibility(View.GONE);
    }

    public Constraints(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        super.setVisibility(View.GONE);
    }

    public Constraints(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        super.setVisibility(View.GONE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends ConstraintLayout.LayoutParams {

        public float alpha = 1;
        public boolean applyElevation = false;
        public float elevation = 0;
        public float rotation = 0;
        public float rotationX = 0;
        public float rotationY = 0;
        public float scaleX = 1;
        public float scaleY = 1;
        public float transformPivotX = 0;
        public float transformPivotY = 0;
        public float translationX = 0;
        public float translationY = 0;
        public float translationZ = 0;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ConstraintSet);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintSet_android_alpha) {
                    alpha = a.getFloat(attr, alpha);
                } else if (attr == R.styleable.ConstraintSet_android_elevation) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        elevation = a.getFloat(attr, elevation);
                        applyElevation = true;
                    }
                } else if (attr == R.styleable.ConstraintSet_android_rotationX) {
                    rotationX = a.getFloat(attr, rotationX);
                } else if (attr == R.styleable.ConstraintSet_android_rotationY) {
                    rotationY = a.getFloat(attr, rotationY);
                } else if (attr == R.styleable.ConstraintSet_android_rotation) {
                    rotation = a.getFloat(attr, rotation);
                } else if (attr == R.styleable.ConstraintSet_android_scaleX) {
                    scaleX = a.getFloat(attr, scaleX);
                } else if (attr == R.styleable.ConstraintSet_android_scaleY) {
                    scaleY = a.getFloat(attr, scaleY);
                } else if (attr == R.styleable.ConstraintSet_android_transformPivotX) {
                    transformPivotX = a.getFloat(attr, transformPivotX);
                } else if (attr == R.styleable.ConstraintSet_android_transformPivotY) {
                    transformPivotY = a.getFloat(attr, transformPivotY);
                } else if (attr == R.styleable.ConstraintSet_android_translationX) {
                    translationX = a.getFloat(attr, translationX);
                } else if (attr == R.styleable.ConstraintSet_android_translationY) {
                    translationY = a.getFloat(attr, translationY);
                } else if (attr == R.styleable.ConstraintSet_android_translationZ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        translationZ = a.getFloat(attr, translationZ);
                    }
                }
            }
            a.recycle();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    private void init() {
        Log.v(TAG, " ################# init");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new ConstraintLayout.LayoutParams(p);
    }

    /**
     * get the Constraints associated with this constraint
     *
     * @return
     */
    public ConstraintSet getConstraintSet() {
        if (mConstraintSet == null) {
            mConstraintSet = new ConstraintSet();
        }
        // TODO -- could be more efficient...
        mConstraintSet.clone(this);
        return mConstraintSet;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }
}
