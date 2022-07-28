/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.motion.widget.MotionLayout;

/**
 * Utility class representing a reactive Guideline helper object for {@link ConstraintLayout}.
 */
public class ReactiveGuide extends View implements SharedValues.SharedValuesListener {
    private int mAttributeId = -1;
    private boolean mAnimateChange = false;
    private int mApplyToConstraintSetId = 0;
    private boolean mApplyToAllConstraintSets = true;

    public ReactiveGuide(Context context) {
        super(context);
        super.setVisibility(View.GONE);
        init(null);
    }

    public ReactiveGuide(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setVisibility(View.GONE);
        init(attrs);
    }

    public ReactiveGuide(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setVisibility(View.GONE);
        init(attrs);
    }

    public ReactiveGuide(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        super.setVisibility(View.GONE);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.ConstraintLayout_ReactiveGuide);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_ReactiveGuide_reactiveGuide_valueId) {
                    mAttributeId = a.getResourceId(attr, mAttributeId);
                } else if (attr == R.styleable
                        .ConstraintLayout_ReactiveGuide_reactiveGuide_animateChange) {
                    mAnimateChange = a.getBoolean(attr, mAnimateChange);
                } else if (attr == R.styleable
                        .ConstraintLayout_ReactiveGuide_reactiveGuide_applyToConstraintSet) {
                    mApplyToConstraintSetId = a.getResourceId(attr, mApplyToConstraintSetId);
                } else if (attr == R.styleable
                        .ConstraintLayout_ReactiveGuide_reactiveGuide_applyToAllConstraintSets) {
                    mApplyToAllConstraintSets = a.getBoolean(attr, mApplyToAllConstraintSets);
                }
            }
            a.recycle();
        }
        if (mAttributeId != -1) {
            SharedValues sharedValues = ConstraintLayout.getSharedValues();
            sharedValues.addListener(mAttributeId, this);
        }
    }

    public int getAttributeId() {
        return mAttributeId;
    }

    // @TODO: add description

    /**
     *
     * @param id
     */
    public void setAttributeId(int id) {
        SharedValues sharedValues = ConstraintLayout.getSharedValues();
        if (mAttributeId != -1) {
            sharedValues.removeListener(mAttributeId, this);
        }
        mAttributeId = id;
        if (mAttributeId != -1) {
            sharedValues.addListener(mAttributeId, this);
        }
    }

    public int getApplyToConstraintSetId() {
        return mApplyToConstraintSetId;
    }

    public void setApplyToConstraintSetId(int id) {
        mApplyToConstraintSetId = id;
    }

    public boolean isAnimatingChange() {
        return mAnimateChange;
    }

    public void setAnimateChange(boolean animate) {
        mAnimateChange = animate;
    }

    /**
     *
     */
    @Override
    public void setVisibility(int visibility) {
    }

    /**
     *
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void draw(Canvas canvas) {
    }

    /**
     *
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    /**
     * Set the guideline's distance from the top or left edge.
     *
     * @param margin the distance to the top or left edge
     */
    public void setGuidelineBegin(int margin) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        params.guideBegin = margin;
        setLayoutParams(params);
    }

    /**
     * Set a guideline's distance to end.
     *
     * @param margin the margin to the right or bottom side of container
     */
    public void setGuidelineEnd(int margin) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        params.guideEnd = margin;
        setLayoutParams(params);
    }

    /**
     * Set a Guideline's percent.
     * @param ratio the ratio between the gap on the left and right 0.0 is top/left 0.5 is middle
     */
    public void setGuidelinePercent(float ratio) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        params.guidePercent = ratio;
        setLayoutParams(params);
    }

    @Override
    public void onNewValue(int key, int newValue, int oldValue) {
        setGuidelineBegin(newValue);
        int id = getId();
        if (id <= 0) {
            return;
        }
        if (getParent() instanceof MotionLayout) {
            MotionLayout motionLayout = (MotionLayout) getParent();
            int currentState = motionLayout.getCurrentState();
            if (mApplyToConstraintSetId != 0) {
                currentState = mApplyToConstraintSetId;
            }
            if (mAnimateChange) {
                if (mApplyToAllConstraintSets) {
                    int []ids = motionLayout.getConstraintSetIds();
                    for (int i = 0; i < ids.length; i++) {
                        int cs = ids[i];
                        if (cs != currentState) {
                            changeValue(newValue, id, motionLayout, cs);
                        }
                    }
                }
                ConstraintSet constraintSet = motionLayout.cloneConstraintSet(currentState);
                constraintSet.setGuidelineEnd(id, newValue);
                motionLayout.updateStateAnimate(currentState, constraintSet, 1000);
            } else {
                if (mApplyToAllConstraintSets) {
                    int []ids = motionLayout.getConstraintSetIds();
                    for (int i = 0; i < ids.length; i++) {
                        int cs = ids[i];
                        changeValue(newValue, id, motionLayout, cs);
                    }
                } else {
                    changeValue(newValue, id, motionLayout, currentState);
                }
            }
        }
    }

    private void changeValue(int newValue, int id, MotionLayout motionLayout, int currentState) {
        ConstraintSet constraintSet = motionLayout.getConstraintSet(currentState);
        constraintSet.setGuidelineEnd(id, newValue);
        motionLayout.updateState(currentState, constraintSet);
    }
}
