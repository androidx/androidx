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

package androidx.constraintlayout.motion.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintHelper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.R;

import java.util.HashMap;

/**
 *
 */
public class MotionHelper extends ConstraintHelper implements MotionHelperInterface {

    private boolean mUseOnShow = false;
    private boolean mUseOnHide = false;
    private float mProgress;
    protected View[] views;

    public MotionHelper(Context context) {
        super(context);
    }

    public MotionHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MotionHelper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    /**
     *
     */
    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MotionHelper);
            final int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.MotionHelper_onShow) {
                    mUseOnShow = a.getBoolean(attr, mUseOnShow);
                } else if (attr == R.styleable.MotionHelper_onHide) {
                    mUseOnHide = a.getBoolean(attr, mUseOnHide);
                }
            }
            a.recycle();
        }
    }

    /**
     *
     * @return
     *
     */
    @Override
    public boolean isUsedOnShow() {
        return mUseOnShow;
    }

    /**
     *
     * @return
     *
     */
    @Override
    public boolean isUseOnHide() {
        return mUseOnHide;
    }

    @Override
    public float getProgress() {
        return mProgress;
    }

    @Override
    public void setProgress(float progress) {
        mProgress = progress;
        if (this.mCount > 0) {
            this.views = this.getViews((ConstraintLayout) this.getParent());

            for (int i = 0; i < this.mCount; ++i) {
                View view = this.views[i];
                this.setProgress(view, progress);
            }
        } else {
            ViewGroup group = (ViewGroup) this.getParent();
            int count = group.getChildCount();

            for (int i = 0; i < count; ++i) {
                View view = group.getChildAt(i);
                if (view instanceof MotionHelper) {
                    continue;
                }
                this.setProgress(view, progress);
            }
        }
    }

    /**
     *
     * @param view
     * @param progress
     *
     */
    public void setProgress(View view, float progress) {

    }

    @Override
    public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
    }

    @Override
    public void onTransitionChange(MotionLayout motionLayout,
                                   int startId,
                                   int endId,
                                   float progress) {
    }

    @Override
    public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
    }

    @Override
    public void onTransitionTrigger(MotionLayout motionLayout,
                                    int triggerId,
                                    boolean positive,
                                    float progress) {
    }

    @Override
    public boolean isDecorator() {
        return false;
    }

    @Override
    public void onPreDraw(Canvas canvas) {

    }
    @Override
    public void onFinishedMotionScene(MotionLayout motionLayout) {

    }

    @Override
    public void onPostDraw(Canvas canvas) {

    }

    @Override
    public void onPreSetup(MotionLayout motionLayout,
                           HashMap<View, MotionController> controllerMap) {

    }

}
