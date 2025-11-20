/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;

/**
 *  <b>Added in 2.0</b>
 *  <p>
 *  </p>
 *
 */
public abstract class VirtualLayout extends ConstraintHelper {
    private boolean mApplyVisibilityOnAttach;
    private boolean mApplyElevationOnAttach;

    public VirtualLayout(Context context) {
        super(context);
    }

    public VirtualLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VirtualLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.ConstraintLayout_Layout);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
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

    /**
     * called to measure layout
     * @param layout
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    public void onMeasure(androidx.constraintlayout.core.widgets.VirtualLayout layout,
                          int widthMeasureSpec,
                          int heightMeasureSpec) {
        // nothing
    }

    /**
     *
     */
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mApplyVisibilityOnAttach || mApplyElevationOnAttach) {
            ViewParent parent = getParent();
            if (parent instanceof ConstraintLayout) {
                ConstraintLayout container = (ConstraintLayout) parent;
                int visibility = getVisibility();
                float elevation = getElevation();
                for (int i = 0; i < mCount; i++) {
                    int id = mIds[i];
                    View view = container.getViewById(id);
                    if (view != null) {
                        if (mApplyVisibilityOnAttach) {
                            view.setVisibility(visibility);
                        }
                        if (mApplyElevationOnAttach) {
                            if (elevation > 0) {
                                view.setTranslationZ(view.getTranslationZ() + elevation);
                            }
                        }
                    }
                }
            }
        }
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
     *
     * @param container
     */
    @Override
    protected void applyLayoutFeaturesInConstraintSet(ConstraintLayout container) {
        applyLayoutFeatures(container);
    }
}
