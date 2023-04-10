/*
 * Copyright (C) 2019 The Android Open Source Project
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
package androidx.constraintlayout.core.widgets;

import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure;

import java.util.HashSet;

/**
 * Base class for Virtual layouts
 */
public class VirtualLayout extends HelperWidget {

    private int mPaddingTop = 0;
    private int mPaddingBottom = 0;
    @SuppressWarnings("unused") private int mPaddingLeft = 0;
    @SuppressWarnings("unused") private int mPaddingRight = 0;
    @SuppressWarnings("unused") private int mPaddingStart = 0;
    private int mPaddingEnd = 0;
    private int mResolvedPaddingLeft = 0;
    private int mResolvedPaddingRight = 0;

    private boolean mNeedsCallFromSolver = false;
    private int mMeasuredWidth = 0;
    private int mMeasuredHeight = 0;

    protected BasicMeasure.Measure mMeasure = new BasicMeasure.Measure();

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Accessors
    /////////////////////////////////////////////////////////////////////////////////////////////

    // @TODO: add description
    public void setPadding(int value) {
        mPaddingLeft = value;
        mPaddingTop = value;
        mPaddingRight = value;
        mPaddingBottom = value;
        mPaddingStart = value;
        mPaddingEnd = value;
    }

    // @TODO: add description
    public void setPaddingStart(int value) {
        mPaddingStart = value;
        mResolvedPaddingLeft = value;
        mResolvedPaddingRight = value;
    }

    public void setPaddingEnd(int value) {
        mPaddingEnd = value;
    }

    // @TODO: add description
    public void setPaddingLeft(int value) {
        mPaddingLeft = value;
        mResolvedPaddingLeft = value;
    }

    // @TODO: add description
    public void applyRtl(boolean isRtl) {
        if (mPaddingStart > 0 || mPaddingEnd > 0) {
            if (isRtl) {
                mResolvedPaddingLeft = mPaddingEnd;
                mResolvedPaddingRight = mPaddingStart;
            } else {
                mResolvedPaddingLeft = mPaddingStart;
                mResolvedPaddingRight = mPaddingEnd;
            }
        }
    }

    public void setPaddingTop(int value) {
        mPaddingTop = value;
    }

    // @TODO: add description
    public void setPaddingRight(int value) {
        mPaddingRight = value;
        mResolvedPaddingRight = value;
    }

    public void setPaddingBottom(int value) {
        mPaddingBottom = value;
    }

    public int getPaddingTop() {
        return mPaddingTop;
    }

    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    public int getPaddingLeft() {
        return mResolvedPaddingLeft;
    }

    public int getPaddingRight() {
        return mResolvedPaddingRight;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Solver callback
    /////////////////////////////////////////////////////////////////////////////////////////////

    protected void needsCallbackFromSolver(boolean value) {
        mNeedsCallFromSolver = value;
    }

    // @TODO: add description
    public boolean needSolverPass() {
        return mNeedsCallFromSolver;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Measure
    /////////////////////////////////////////////////////////////////////////////////////////////

    // @TODO: add description
    public void measure(int widthMode, int widthSize, int heightMode, int heightSize) {
        // nothing
    }

    @Override
    public void updateConstraints(ConstraintWidgetContainer container) {
        captureWidgets();
    }

    // @TODO: add description
    public void captureWidgets() {
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (widget != null) {
                widget.setInVirtualLayout(true);
            }
        }
    }

    public int getMeasuredWidth() {
        return mMeasuredWidth;
    }

    public int getMeasuredHeight() {
        return mMeasuredHeight;
    }

    // @TODO: add description
    public void setMeasure(int width, int height) {
        mMeasuredWidth = width;
        mMeasuredHeight = height;
    }

    protected boolean measureChildren() {
        BasicMeasure.Measurer measurer = null;
        if (mParent != null) {
            measurer = ((ConstraintWidgetContainer) mParent).getMeasurer();
        }
        if (measurer == null) {
            return false;
        }

        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (widget == null) {
                continue;
            }

            if (widget instanceof Guideline) {
                continue;
            }

            DimensionBehaviour widthBehavior = widget.getDimensionBehaviour(HORIZONTAL);
            DimensionBehaviour heightBehavior = widget.getDimensionBehaviour(VERTICAL);

            boolean skip = widthBehavior == DimensionBehaviour.MATCH_CONSTRAINT
                    && widget.mMatchConstraintDefaultWidth != MATCH_CONSTRAINT_WRAP
                    && heightBehavior == DimensionBehaviour.MATCH_CONSTRAINT
                    && widget.mMatchConstraintDefaultHeight != MATCH_CONSTRAINT_WRAP;

            if (skip) {
                // we don't need to measure here as the dimension of the widget
                // will be completely computed by the solver.
                continue;
            }

            if (widthBehavior == DimensionBehaviour.MATCH_CONSTRAINT) {
                widthBehavior = DimensionBehaviour.WRAP_CONTENT;
            }
            if (heightBehavior == DimensionBehaviour.MATCH_CONSTRAINT) {
                heightBehavior = DimensionBehaviour.WRAP_CONTENT;
            }
            mMeasure.horizontalBehavior = widthBehavior;
            mMeasure.verticalBehavior = heightBehavior;
            mMeasure.horizontalDimension = widget.getWidth();
            mMeasure.verticalDimension = widget.getHeight();
            measurer.measure(widget, mMeasure);
            widget.setWidth(mMeasure.measuredWidth);
            widget.setHeight(mMeasure.measuredHeight);
            widget.setBaselineDistance(mMeasure.measuredBaseline);
        }
        return true;
    }

    BasicMeasure.Measurer mMeasurer = null;

    protected void measure(ConstraintWidget widget,
            ConstraintWidget.DimensionBehaviour horizontalBehavior,
            int horizontalDimension,
            ConstraintWidget.DimensionBehaviour verticalBehavior,
            int verticalDimension) {
        while (mMeasurer == null && getParent() != null) {
            ConstraintWidgetContainer parent = (ConstraintWidgetContainer) getParent();
            mMeasurer = parent.getMeasurer();
        }
        mMeasure.horizontalBehavior = horizontalBehavior;
        mMeasure.verticalBehavior = verticalBehavior;
        mMeasure.horizontalDimension = horizontalDimension;
        mMeasure.verticalDimension = verticalDimension;
        mMeasurer.measure(widget, mMeasure);
        widget.setWidth(mMeasure.measuredWidth);
        widget.setHeight(mMeasure.measuredHeight);
        widget.setHasBaseline(mMeasure.measuredHasBaseline);
        widget.setBaselineDistance(mMeasure.measuredBaseline);
    }

    // @TODO: add description
    public boolean contains(HashSet<ConstraintWidget> widgets) {
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (widgets.contains(widget)) {
                return true;
            }
        }
        return false;
    }
}
