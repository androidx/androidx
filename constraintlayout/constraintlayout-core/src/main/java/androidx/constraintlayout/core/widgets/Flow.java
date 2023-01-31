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

import static androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.AT_MOST;
import static androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.EXACTLY;
import static androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.UNSPECIFIED;

import androidx.constraintlayout.core.LinearSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Implements the Flow virtual layout.
 */
public class Flow extends VirtualLayout {

    public static final int HORIZONTAL_ALIGN_START = 0;
    public static final int HORIZONTAL_ALIGN_END = 1;
    public static final int HORIZONTAL_ALIGN_CENTER = 2;

    public static final int VERTICAL_ALIGN_TOP = 0;
    public static final int VERTICAL_ALIGN_BOTTOM = 1;
    public static final int VERTICAL_ALIGN_CENTER = 2;
    public static final int VERTICAL_ALIGN_BASELINE = 3;

    public static final int WRAP_NONE = 0;
    public static final int WRAP_CHAIN = 1;
    public static final int WRAP_ALIGNED = 2;
    public static final int WRAP_CHAIN_NEW = 3;

    private int mHorizontalStyle = UNKNOWN;
    private int mVerticalStyle = UNKNOWN;
    private int mFirstHorizontalStyle = UNKNOWN;
    private int mFirstVerticalStyle = UNKNOWN;
    private int mLastHorizontalStyle = UNKNOWN;
    private int mLastVerticalStyle = UNKNOWN;

    private float mHorizontalBias = 0.5f;
    private float mVerticalBias = 0.5f;
    private float mFirstHorizontalBias = 0.5f;
    private float mFirstVerticalBias = 0.5f;
    private float mLastHorizontalBias = 0.5f;
    private float mLastVerticalBias = 0.5f;

    private int mHorizontalGap = 0;
    private int mVerticalGap = 0;

    private int mHorizontalAlign = HORIZONTAL_ALIGN_CENTER;
    private int mVerticalAlign = VERTICAL_ALIGN_CENTER;
    private int mWrapMode = WRAP_NONE;

    private int mMaxElementsWrap = UNKNOWN;

    private int mOrientation = HORIZONTAL;

    private ArrayList<WidgetsList> mChainList = new ArrayList<>();

    // Aligned management

    private ConstraintWidget[] mAlignedBiggestElementsInRows = null;
    private ConstraintWidget[] mAlignedBiggestElementsInCols = null;
    private int[] mAlignedDimensions = null;
    private ConstraintWidget[] mDisplayedWidgets;
    private int mDisplayedWidgetsCount = 0;


    @Override
    public void copy(ConstraintWidget src, HashMap<ConstraintWidget, ConstraintWidget> map) {
        super.copy(src, map);
        Flow srcFLow = (Flow) src;

        mHorizontalStyle = srcFLow.mHorizontalStyle;
        mVerticalStyle = srcFLow.mVerticalStyle;
        mFirstHorizontalStyle = srcFLow.mFirstHorizontalStyle;
        mFirstVerticalStyle = srcFLow.mFirstVerticalStyle;
        mLastHorizontalStyle = srcFLow.mLastHorizontalStyle;
        mLastVerticalStyle = srcFLow.mLastVerticalStyle;

        mHorizontalBias = srcFLow.mHorizontalBias;
        mVerticalBias = srcFLow.mVerticalBias;
        mFirstHorizontalBias = srcFLow.mFirstHorizontalBias;
        mFirstVerticalBias = srcFLow.mFirstVerticalBias;
        mLastHorizontalBias = srcFLow.mLastHorizontalBias;
        mLastVerticalBias = srcFLow.mLastVerticalBias;

        mHorizontalGap = srcFLow.mHorizontalGap;
        mVerticalGap = srcFLow.mVerticalGap;

        mHorizontalAlign = srcFLow.mHorizontalAlign;
        mVerticalAlign = srcFLow.mVerticalAlign;
        mWrapMode = srcFLow.mWrapMode;

        mMaxElementsWrap = srcFLow.mMaxElementsWrap;

        mOrientation = srcFLow.mOrientation;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Accessors
    /////////////////////////////////////////////////////////////////////////////////////////////

    public void setOrientation(int value) {
        mOrientation = value;
    }

    public void setFirstHorizontalStyle(int value) {
        mFirstHorizontalStyle = value;
    }

    public void setFirstVerticalStyle(int value) {
        mFirstVerticalStyle = value;
    }

    public void setLastHorizontalStyle(int value) {
        mLastHorizontalStyle = value;
    }

    public void setLastVerticalStyle(int value) {
        mLastVerticalStyle = value;
    }

    public void setHorizontalStyle(int value) {
        mHorizontalStyle = value;
    }

    public void setVerticalStyle(int value) {
        mVerticalStyle = value;
    }

    public void setHorizontalBias(float value) {
        mHorizontalBias = value;
    }

    public void setVerticalBias(float value) {
        mVerticalBias = value;
    }

    public void setFirstHorizontalBias(float value) {
        mFirstHorizontalBias = value;
    }

    public void setFirstVerticalBias(float value) {
        mFirstVerticalBias = value;
    }

    public void setLastHorizontalBias(float value) {
        mLastHorizontalBias = value;
    }

    public void setLastVerticalBias(float value) {
        mLastVerticalBias = value;
    }

    public void setHorizontalAlign(int value) {
        mHorizontalAlign = value;
    }

    public void setVerticalAlign(int value) {
        mVerticalAlign = value;
    }

    public void setWrapMode(int value) {
        mWrapMode = value;
    }

    public void setHorizontalGap(int value) {
        mHorizontalGap = value;
    }

    public void setVerticalGap(int value) {
        mVerticalGap = value;
    }

    public void setMaxElementsWrap(int value) {
        mMaxElementsWrap = value;
    }

    public float getMaxElementsWrap() {
        return mMaxElementsWrap;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////
    // Utility methods
    /////////////////////////////////////////////////////////////////////////////////////////////

    private int getWidgetWidth(ConstraintWidget widget, int max) {
        if (widget == null) {
            return 0;
        }
        if (widget.getHorizontalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT) {
            if (widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD) {
                return 0;
            } else if (widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_PERCENT) {
                int value = (int) (widget.mMatchConstraintPercentWidth * max);
                if (value != widget.getWidth()) {
                    widget.setMeasureRequested(true);
                    measure(widget, DimensionBehaviour.FIXED, value,
                            widget.getVerticalDimensionBehaviour(), widget.getHeight());
                }
                return value;
            } else if (widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_WRAP) {
                return widget.getWidth();
            } else if (widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_RATIO) {
                return (int) (widget.getHeight() * widget.mDimensionRatio + 0.5f);
            }
        }
        return widget.getWidth();
    }

    private int getWidgetHeight(ConstraintWidget widget, int max) {
        if (widget == null) {
            return 0;
        }
        if (widget.getVerticalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT) {
            if (widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD) {
                return 0;
            } else if (widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_PERCENT) {
                int value = (int) (widget.mMatchConstraintPercentHeight * max);
                if (value != widget.getHeight()) {
                    widget.setMeasureRequested(true);
                    measure(widget, widget.getHorizontalDimensionBehaviour(),
                            widget.getWidth(), DimensionBehaviour.FIXED, value);
                }
                return value;
            } else if (widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_WRAP) {
                return widget.getHeight();
            } else if (widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_RATIO) {
                return (int) (widget.getWidth() * widget.mDimensionRatio + 0.5f);
            }
        }
        return widget.getHeight();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Measure
    /////////////////////////////////////////////////////////////////////////////////////////////

    // @TODO: add description
    @Override
    public void measure(int widthMode, int widthSize, int heightMode, int heightSize) {
        if (mWidgetsCount > 0 && !measureChildren()) {
            setMeasure(0, 0);
            needsCallbackFromSolver(false);
            return;
        }

        @SuppressWarnings("unused") int width = 0;
        @SuppressWarnings("unused") int height = 0;
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int[] measured = new int[2];
        int max = widthSize - paddingLeft - paddingRight;
        if (mOrientation == VERTICAL) {
            max = heightSize - paddingTop - paddingBottom;
        }

        if (mOrientation == HORIZONTAL) {
            if (mHorizontalStyle == UNKNOWN) {
                mHorizontalStyle = CHAIN_SPREAD;
            }
            if (mVerticalStyle == UNKNOWN) {
                mVerticalStyle = CHAIN_SPREAD;
            }
        } else {
            if (mHorizontalStyle == UNKNOWN) {
                mHorizontalStyle = CHAIN_SPREAD;
            }
            if (mVerticalStyle == UNKNOWN) {
                mVerticalStyle = CHAIN_SPREAD;
            }
        }

        ConstraintWidget[] widgets = mWidgets;

        int gone = 0;
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (widget.getVisibility() == GONE) {
                gone++;
            }
        }
        int count = mWidgetsCount;
        if (gone > 0) {
            widgets = new ConstraintWidget[mWidgetsCount - gone];
            int j = 0;
            for (int i = 0; i < mWidgetsCount; i++) {
                ConstraintWidget widget = mWidgets[i];
                if (widget.getVisibility() != GONE) {
                    widgets[j] = widget;
                    j++;
                }
            }
            count = j;
        }
        mDisplayedWidgets = widgets;
        mDisplayedWidgetsCount = count;
        switch (mWrapMode) {
            case WRAP_ALIGNED: {
                measureAligned(widgets, count, mOrientation, max, measured);
            }
            break;
            case WRAP_CHAIN: {
                measureChainWrap(widgets, count, mOrientation, max, measured);
            }
            break;
            case WRAP_NONE: {
                measureNoWrap(widgets, count, mOrientation, max, measured);
            }
            break;
            case WRAP_CHAIN_NEW: {
                measureChainWrap_new(widgets, count, mOrientation, max, measured);
            }
            break;

        }

        width = measured[HORIZONTAL] + paddingLeft + paddingRight;
        height = measured[VERTICAL] + paddingTop + paddingBottom;

        int measuredWidth = 0;
        int measuredHeight = 0;

        if (widthMode == EXACTLY) {
            measuredWidth = widthSize;
        } else if (widthMode == AT_MOST) {
            measuredWidth = Math.min(width, widthSize);
        } else if (widthMode == UNSPECIFIED) {
            measuredWidth = width;
        }

        if (heightMode == EXACTLY) {
            measuredHeight = heightSize;
        } else if (heightMode == AT_MOST) {
            measuredHeight = Math.min(height, heightSize);
        } else if (heightMode == UNSPECIFIED) {
            measuredHeight = height;
        }

        setMeasure(measuredWidth, measuredHeight);
        setWidth(measuredWidth);
        setHeight(measuredHeight);
        needsCallbackFromSolver(mWidgetsCount > 0);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Utility class representing a single chain
    /////////////////////////////////////////////////////////////////////////////////////////////

    private class WidgetsList {
        private int mOrientation = HORIZONTAL;
        private ConstraintWidget mBiggest = null;
        int mBiggestDimension = 0;
        private ConstraintAnchor mLeft;
        private ConstraintAnchor mTop;
        private ConstraintAnchor mRight;
        private ConstraintAnchor mBottom;
        private int mPaddingLeft = 0;
        private int mPaddingTop = 0;
        private int mPaddingRight = 0;
        private int mPaddingBottom = 0;
        private int mWidth = 0;
        private int mHeight = 0;
        private int mStartIndex = 0;
        private int mCount = 0;
        private int mNbMatchConstraintsWidgets = 0;
        private int mMax = 0;

        WidgetsList(int orientation,
                ConstraintAnchor left, ConstraintAnchor top,
                ConstraintAnchor right, ConstraintAnchor bottom,
                int max) {
            mOrientation = orientation;
            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;
            mPaddingLeft = getPaddingLeft();
            mPaddingTop = getPaddingTop();
            mPaddingRight = getPaddingRight();
            mPaddingBottom = getPaddingBottom();
            mMax = max;
        }

        public void setup(int orientation, ConstraintAnchor left, ConstraintAnchor top,
                ConstraintAnchor right, ConstraintAnchor bottom,
                int paddingLeft, int paddingTop, int paddingRight, int paddingBottom,
                int max) {
            mOrientation = orientation;
            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;
            mPaddingLeft = paddingLeft;
            mPaddingTop = paddingTop;
            mPaddingRight = paddingRight;
            mPaddingBottom = paddingBottom;
            mMax = max;
        }

        public void clear() {
            mBiggestDimension = 0;
            mBiggest = null;
            mWidth = 0;
            mHeight = 0;
            mStartIndex = 0;
            mCount = 0;
            mNbMatchConstraintsWidgets = 0;
        }

        public void setStartIndex(int value) {
            mStartIndex = value;
        }

        public int getWidth() {
            if (mOrientation == HORIZONTAL) {
                return mWidth - mHorizontalGap;
            }
            return mWidth;
        }

        public int getHeight() {
            if (mOrientation == VERTICAL) {
                return mHeight - mVerticalGap;
            }
            return mHeight;
        }

        public void add(ConstraintWidget widget) {
            if (mOrientation == HORIZONTAL) {
                int width = getWidgetWidth(widget, mMax);
                if (widget.getHorizontalDimensionBehaviour()
                        == DimensionBehaviour.MATCH_CONSTRAINT) {
                    mNbMatchConstraintsWidgets++;
                    width = 0;
                }
                int gap = mHorizontalGap;
                if (widget.getVisibility() == GONE) {
                    gap = 0;
                }
                mWidth += width + gap;
                int height = getWidgetHeight(widget, mMax);
                if (mBiggest == null || mBiggestDimension < height) {
                    mBiggest = widget;
                    mBiggestDimension = height;
                    mHeight = height;
                }
            } else {
                int width = getWidgetWidth(widget, mMax);
                int height = getWidgetHeight(widget, mMax);
                if (widget.getVerticalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT) {
                    mNbMatchConstraintsWidgets++;
                    height = 0;
                }
                int gap = mVerticalGap;
                if (widget.getVisibility() == GONE) {
                    gap = 0;
                }
                mHeight += height + gap;
                if (mBiggest == null || mBiggestDimension < width) {
                    mBiggest = widget;
                    mBiggestDimension = width;
                    mWidth = width;
                }
            }
            mCount++;
        }

        public void createConstraints(boolean isInRtl, int chainIndex, boolean isLastChain) {
            final int count = mCount;
            for (int i = 0; i < count; i++) {
                if (mStartIndex + i >= mDisplayedWidgetsCount) {
                    break;
                }
                ConstraintWidget widget = mDisplayedWidgets[mStartIndex + i];
                if (widget != null) {
                    widget.resetAnchors();
                }
            }
            if (count == 0 || mBiggest == null) {
                return;
            }

            boolean singleChain = isLastChain && chainIndex == 0;
            int firstVisible = -1;
            int lastVisible = -1;
            for (int i = 0; i < count; i++) {
                int index = i;
                if (isInRtl) {
                    index = count - 1 - i;
                }
                if (mStartIndex + index >= mDisplayedWidgetsCount) {
                    break;
                }
                ConstraintWidget widget = mDisplayedWidgets[mStartIndex + index];
                if (widget != null && widget.getVisibility() == VISIBLE) {
                    if (firstVisible == -1) {
                        firstVisible = i;
                    }
                    lastVisible = i;
                }
            }

            ConstraintWidget previous = null;
            if (mOrientation == HORIZONTAL) {
                ConstraintWidget verticalWidget = mBiggest;
                verticalWidget.setVerticalChainStyle(mVerticalStyle);
                int padding = mPaddingTop;
                if (chainIndex > 0) {
                    padding += mVerticalGap;
                }
                verticalWidget.mTop.connect(mTop, padding);
                if (isLastChain) {
                    verticalWidget.mBottom.connect(mBottom, mPaddingBottom);
                }
                if (chainIndex > 0) {
                    ConstraintAnchor bottom = mTop.mOwner.mBottom;
                    bottom.connect(verticalWidget.mTop, 0);
                }

                ConstraintWidget baselineVerticalWidget = verticalWidget;
                if (mVerticalAlign == VERTICAL_ALIGN_BASELINE && !verticalWidget.hasBaseline()) {
                    for (int i = 0; i < count; i++) {
                        int index = i;
                        if (isInRtl) {
                            index = count - 1 - i;
                        }
                        if (mStartIndex + index >= mDisplayedWidgetsCount) {
                            break;
                        }
                        ConstraintWidget widget = mDisplayedWidgets[mStartIndex + index];
                        if (widget.hasBaseline()) {
                            baselineVerticalWidget = widget;
                            break;
                        }
                    }
                }

                for (int i = 0; i < count; i++) {
                    int index = i;
                    if (isInRtl) {
                        index = count - 1 - i;
                    }
                    if (mStartIndex + index >= mDisplayedWidgetsCount) {
                        break;
                    }
                    ConstraintWidget widget = mDisplayedWidgets[mStartIndex + index];
                    if (widget == null) {
                        continue;
                    }
                    if (i == 0) {
                        widget.connect(widget.mLeft, mLeft, mPaddingLeft);
                    }

                    // ChainHead is always based on index, not i.
                    // E.g. RTL would have head at the right most widget.
                    if (index == 0) {
                        int style = mHorizontalStyle;
                        float bias = isInRtl ? (1 - mHorizontalBias) : mHorizontalBias;
                        if (mStartIndex == 0 && mFirstHorizontalStyle != UNKNOWN) {
                            style = mFirstHorizontalStyle;
                            bias = isInRtl ? (1 - mFirstHorizontalBias) : mFirstHorizontalBias;
                        } else if (isLastChain && mLastHorizontalStyle != UNKNOWN) {
                            style = mLastHorizontalStyle;
                            bias = isInRtl ? (1 - mLastHorizontalBias) : mLastHorizontalBias;
                        }
                        widget.setHorizontalChainStyle(style);
                        widget.setHorizontalBiasPercent(bias);
                    }
                    if (i == count - 1) {
                        widget.connect(widget.mRight, mRight, mPaddingRight);
                    }
                    if (previous != null) {
                        widget.mLeft.connect(previous.mRight, mHorizontalGap);
                        if (i == firstVisible) {
                            widget.mLeft.setGoneMargin(mPaddingLeft);
                        }
                        previous.mRight.connect(widget.mLeft, 0);
                        if (i == lastVisible + 1) {
                            previous.mRight.setGoneMargin(mPaddingRight);
                        }
                    }
                    if (widget != verticalWidget) {
                        if (mVerticalAlign == VERTICAL_ALIGN_BASELINE
                                && baselineVerticalWidget.hasBaseline()
                                && widget != baselineVerticalWidget
                                && widget.hasBaseline()) {
                            widget.mBaseline.connect(baselineVerticalWidget.mBaseline, 0);
                        } else {
                            switch (mVerticalAlign) {
                                case VERTICAL_ALIGN_TOP: {
                                    widget.mTop.connect(verticalWidget.mTop, 0);
                                }
                                break;
                                case VERTICAL_ALIGN_BOTTOM: {
                                    widget.mBottom.connect(verticalWidget.mBottom, 0);
                                }
                                break;
                                case VERTICAL_ALIGN_CENTER:
                                default: {
                                    if (singleChain) {
                                        widget.mTop.connect(mTop, mPaddingTop);
                                        widget.mBottom.connect(mBottom, mPaddingBottom);
                                    } else {
                                        widget.mTop.connect(verticalWidget.mTop, 0);
                                        widget.mBottom.connect(verticalWidget.mBottom, 0);
                                    }
                                }
                            }
                        }
                    }
                    previous = widget;
                }
            } else {
                ConstraintWidget horizontalWidget = mBiggest;
                horizontalWidget.setHorizontalChainStyle(mHorizontalStyle);
                int padding = mPaddingLeft;
                if (chainIndex > 0) {
                    padding += mHorizontalGap;
                }
                if (isInRtl) {
                    horizontalWidget.mRight.connect(mRight, padding);
                    if (isLastChain) {
                        horizontalWidget.mLeft.connect(mLeft, mPaddingRight);
                    }
                    if (chainIndex > 0) {
                        ConstraintAnchor left = mRight.mOwner.mLeft;
                        left.connect(horizontalWidget.mRight, 0);
                    }
                } else {
                    horizontalWidget.mLeft.connect(mLeft, padding);
                    if (isLastChain) {
                        horizontalWidget.mRight.connect(mRight, mPaddingRight);
                    }
                    if (chainIndex > 0) {
                        ConstraintAnchor right = mLeft.mOwner.mRight;
                        right.connect(horizontalWidget.mLeft, 0);
                    }
                }
                for (int i = 0; i < count; i++) {
                    if (mStartIndex + i >= mDisplayedWidgetsCount) {
                        break;
                    }
                    ConstraintWidget widget = mDisplayedWidgets[mStartIndex + i];
                    if (widget == null) {
                        continue;
                    }
                    if (i == 0) {
                        widget.connect(widget.mTop, mTop, mPaddingTop);
                        int style = mVerticalStyle;
                        float bias = mVerticalBias;
                        if (mStartIndex == 0 && mFirstVerticalStyle != UNKNOWN) {
                            style = mFirstVerticalStyle;
                            bias = mFirstVerticalBias;
                        } else if (isLastChain && mLastVerticalStyle != UNKNOWN) {
                            style = mLastVerticalStyle;
                            bias = mLastVerticalBias;
                        }
                        widget.setVerticalChainStyle(style);
                        widget.setVerticalBiasPercent(bias);
                    }
                    if (i == count - 1) {
                        widget.connect(widget.mBottom, mBottom, mPaddingBottom);
                    }
                    if (previous != null) {
                        widget.mTop.connect(previous.mBottom, mVerticalGap);
                        if (i == firstVisible) {
                            widget.mTop.setGoneMargin(mPaddingTop);
                        }
                        previous.mBottom.connect(widget.mTop, 0);
                        if (i == lastVisible + 1) {
                            previous.mBottom.setGoneMargin(mPaddingBottom);
                        }
                    }
                    if (widget != horizontalWidget) {
                        if (isInRtl) {
                            switch (mHorizontalAlign) {
                                case HORIZONTAL_ALIGN_START: {
                                    widget.mRight.connect(horizontalWidget.mRight, 0);
                                }
                                break;
                                case HORIZONTAL_ALIGN_CENTER: {
                                    widget.mLeft.connect(horizontalWidget.mLeft, 0);
                                    widget.mRight.connect(horizontalWidget.mRight, 0);
                                }
                                break;
                                case HORIZONTAL_ALIGN_END: {
                                    widget.mLeft.connect(horizontalWidget.mLeft, 0);
                                }
                                break;
                            }
                        } else {
                            switch (mHorizontalAlign) {
                                case HORIZONTAL_ALIGN_START: {
                                    widget.mLeft.connect(horizontalWidget.mLeft, 0);
                                }
                                break;
                                case HORIZONTAL_ALIGN_CENTER: {
                                    if (singleChain) {
                                        widget.mLeft.connect(mLeft, mPaddingLeft);
                                        widget.mRight.connect(mRight, mPaddingRight);
                                    } else {
                                        widget.mLeft.connect(horizontalWidget.mLeft, 0);
                                        widget.mRight.connect(horizontalWidget.mRight, 0);
                                    }
                                }
                                break;
                                case HORIZONTAL_ALIGN_END: {
                                    widget.mRight.connect(horizontalWidget.mRight, 0);
                                }
                                break;
                            }
                        }
                    }
                    previous = widget;
                }
            }
        }

        public void measureMatchConstraints(int availableSpace) {
            if (mNbMatchConstraintsWidgets == 0) {
                return;
            }
            final int count = mCount;

            // that's completely incorrect and only works for spread with no weights?
            int widgetSize = availableSpace / mNbMatchConstraintsWidgets;
            for (int i = 0; i < count; i++) {
                if (mStartIndex + i >= mDisplayedWidgetsCount) {
                    break;
                }
                ConstraintWidget widget = mDisplayedWidgets[mStartIndex + i];
                if (mOrientation == HORIZONTAL) {
                    if (widget != null && widget.getHorizontalDimensionBehaviour()
                            == DimensionBehaviour.MATCH_CONSTRAINT) {
                        if (widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD) {
                            measure(widget, DimensionBehaviour.FIXED, widgetSize,
                                    widget.getVerticalDimensionBehaviour(), widget.getHeight());
                        }
                    }
                } else {
                    if (widget != null && widget.getVerticalDimensionBehaviour()
                            == DimensionBehaviour.MATCH_CONSTRAINT) {
                        if (widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD) {
                            measure(widget, widget.getHorizontalDimensionBehaviour(),
                                    widget.getWidth(), DimensionBehaviour.FIXED, widgetSize);
                        }
                    }
                }
            }
            recomputeDimensions();
        }

        private void recomputeDimensions() {
            mWidth = 0;
            mHeight = 0;
            mBiggest = null;
            mBiggestDimension = 0;
            final int count = mCount;
            for (int i = 0; i < count; i++) {
                if (mStartIndex + i >= mDisplayedWidgetsCount) {
                    break;
                }
                ConstraintWidget widget = mDisplayedWidgets[mStartIndex + i];
                if (mOrientation == HORIZONTAL) {
                    int width = widget.getWidth();
                    int gap = mHorizontalGap;
                    if (widget.getVisibility() == GONE) {
                        gap = 0;
                    }
                    mWidth += width + gap;
                    int height = getWidgetHeight(widget, mMax);
                    if (mBiggest == null || mBiggestDimension < height) {
                        mBiggest = widget;
                        mBiggestDimension = height;
                        mHeight = height;
                    }
                } else {
                    int width = getWidgetWidth(widget, mMax);
                    int height = getWidgetHeight(widget, mMax);
                    int gap = mVerticalGap;
                    if (widget.getVisibility() == GONE) {
                        gap = 0;
                    }
                    mHeight += height + gap;
                    if (mBiggest == null || mBiggestDimension < width) {
                        mBiggest = widget;
                        mBiggestDimension = width;
                        mWidth = width;
                    }
                }
            }
        }

    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Measure Chain Wrap
    /////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Measure the virtual layout using a list of chains for the children
     *
     * @param widgets     list of widgets
     * @param orientation the layout orientation (horizontal or vertical)
     * @param max         the maximum available space
     * @param measured    output parameters -- will contain the resulting measure
     */
    private void measureChainWrap(ConstraintWidget[] widgets,
            int count,
            int orientation,
            int max,
            int[] measured) {
        if (count == 0) {
            return;
        }

        mChainList.clear();
        WidgetsList list = new WidgetsList(orientation, mLeft, mTop, mRight, mBottom, max);
        mChainList.add(list);

        int nbMatchConstraintsWidgets = 0;

        if (orientation == HORIZONTAL) {
            int width = 0;
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = widgets[i];
                int w = getWidgetWidth(widget, max);
                if (widget.getHorizontalDimensionBehaviour()
                        == DimensionBehaviour.MATCH_CONSTRAINT) {
                    nbMatchConstraintsWidgets++;
                }
                boolean doWrap = (width == max || (width + mHorizontalGap + w) > max)
                        && list.mBiggest != null;
                if (!doWrap && i > 0 && mMaxElementsWrap > 0 && (i % mMaxElementsWrap == 0)) {
                    doWrap = true;
                }
                if (doWrap) {
                    width = w;
                    list = new WidgetsList(orientation, mLeft, mTop, mRight, mBottom, max);
                    list.setStartIndex(i);
                    mChainList.add(list);
                } else {
                    if (i > 0) {
                        width += mHorizontalGap + w;
                    } else {
                        width = w;
                    }
                }
                list.add(widget);
            }
        } else {
            int height = 0;
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = widgets[i];
                int h = getWidgetHeight(widget, max);
                if (widget.getVerticalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT) {
                    nbMatchConstraintsWidgets++;
                }
                boolean doWrap = (height == max || (height + mVerticalGap + h) > max)
                        && list.mBiggest != null;
                if (!doWrap && i > 0 && mMaxElementsWrap > 0 && (i % mMaxElementsWrap == 0)) {
                    doWrap = true;
                }
                if (doWrap) {
                    height = h;
                    list = new WidgetsList(orientation, mLeft, mTop, mRight, mBottom, max);
                    list.setStartIndex(i);
                    mChainList.add(list);
                } else {
                    if (i > 0) {
                        height += mVerticalGap + h;
                    } else {
                        height = h;
                    }
                }
                list.add(widget);
            }
        }
        final int listCount = mChainList.size();

        ConstraintAnchor left = mLeft;
        ConstraintAnchor top = mTop;
        ConstraintAnchor right = mRight;
        ConstraintAnchor bottom = mBottom;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int maxWidth = 0;
        int maxHeight = 0;

        boolean needInternalMeasure =
                getHorizontalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT
                        || getVerticalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT;

        if (nbMatchConstraintsWidgets > 0 && needInternalMeasure) {
            // we have to remeasure them.
            for (int i = 0; i < listCount; i++) {
                WidgetsList current = mChainList.get(i);
                if (orientation == HORIZONTAL) {
                    current.measureMatchConstraints(max - current.getWidth());
                } else {
                    current.measureMatchConstraints(max - current.getHeight());
                }
            }
        }

        for (int i = 0; i < listCount; i++) {
            WidgetsList current = mChainList.get(i);
            if (orientation == HORIZONTAL) {
                if (i < listCount - 1) {
                    WidgetsList next = mChainList.get(i + 1);
                    bottom = next.mBiggest.mTop;
                    paddingBottom = 0;
                } else {
                    bottom = mBottom;
                    paddingBottom = getPaddingBottom();
                }
                ConstraintAnchor currentBottom = current.mBiggest.mBottom;
                current.setup(orientation, left, top, right, bottom,
                        paddingLeft, paddingTop, paddingRight, paddingBottom, max);
                top = currentBottom;
                paddingTop = 0;
                maxWidth = Math.max(maxWidth, current.getWidth());
                maxHeight += current.getHeight();
                if (i > 0) {
                    maxHeight += mVerticalGap;
                }
            } else {
                if (i < listCount - 1) {
                    WidgetsList next = mChainList.get(i + 1);
                    right = next.mBiggest.mLeft;
                    paddingRight = 0;
                } else {
                    right = mRight;
                    paddingRight = getPaddingRight();
                }
                ConstraintAnchor currentRight = current.mBiggest.mRight;
                current.setup(orientation, left, top, right, bottom,
                        paddingLeft, paddingTop, paddingRight, paddingBottom, max);
                left = currentRight;
                paddingLeft = 0;
                maxWidth += current.getWidth();
                maxHeight = Math.max(maxHeight, current.getHeight());
                if (i > 0) {
                    maxWidth += mHorizontalGap;
                }
            }
        }
        measured[HORIZONTAL] = maxWidth;
        measured[VERTICAL] = maxHeight;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////
    // Measure Chain Wrap new
    /////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Measure the virtual layout using a list of chains for the children in new "fixed way"
     *
     * @param widgets     list of widgets
     * @param orientation the layout orientation (horizontal or vertical)
     * @param max         the maximum available space
     * @param measured    output parameters -- will contain the resulting measure
     */
    private void measureChainWrap_new(ConstraintWidget[] widgets,
            int count,
            int orientation,
            int max,
            int[] measured) {
        if (count == 0) {
            return;
        }

        mChainList.clear();
        WidgetsList list = new WidgetsList(orientation, mLeft, mTop, mRight, mBottom, max);
        mChainList.add(list);

        int nbMatchConstraintsWidgets = 0;

        if (orientation == HORIZONTAL) {
            int width = 0;
            int col = 0;
            for (int i = 0; i < count; i++) {
                col++;
                ConstraintWidget widget = widgets[i];
                int w = getWidgetWidth(widget, max);
                if (widget.getHorizontalDimensionBehaviour()
                        == DimensionBehaviour.MATCH_CONSTRAINT) {
                    nbMatchConstraintsWidgets++;
                }
                boolean doWrap = (width == max || (width + mHorizontalGap + w) > max)
                        && list.mBiggest != null;
                if (!doWrap && i > 0 && mMaxElementsWrap > 0 && (col > mMaxElementsWrap)) {
                    doWrap = true;
                }
                if (doWrap) {
                    width = w;
                    list = new WidgetsList(orientation, mLeft, mTop, mRight, mBottom, max);
                    list.setStartIndex(i);
                    mChainList.add(list);
                } else {
                    col = 0;
                    if (i > 0) {
                        width += mHorizontalGap + w;
                    } else {
                        width = w;
                    }
                }
                list.add(widget);
            }
        } else {
            int height = 0;
            int row = 0;
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = widgets[i];
                int h = getWidgetHeight(widget, max);
                if (widget.getVerticalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT) {
                    nbMatchConstraintsWidgets++;
                }
                boolean doWrap = (height == max || (height + mVerticalGap + h) > max)
                        && list.mBiggest != null;
                if (!doWrap && i > 0 && mMaxElementsWrap > 0 && (row > mMaxElementsWrap)) {
                    doWrap = true;
                }
                if (doWrap) {
                    height = h;
                    list = new WidgetsList(orientation, mLeft, mTop, mRight, mBottom, max);
                    list.setStartIndex(i);
                    mChainList.add(list);
                } else {
                    row = 0;
                    if (i > 0) {
                        height += mVerticalGap + h;
                    } else {
                        height = h;
                    }
                }
                list.add(widget);
            }
        }
        final int listCount = mChainList.size();

        ConstraintAnchor left = mLeft;
        ConstraintAnchor top = mTop;
        ConstraintAnchor right = mRight;
        ConstraintAnchor bottom = mBottom;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int maxWidth = 0;
        int maxHeight = 0;

        boolean needInternalMeasure =
                getHorizontalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT
                        || getVerticalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT;

        if (nbMatchConstraintsWidgets > 0 && needInternalMeasure) {
            // we have to remeasure them.
            for (int i = 0; i < listCount; i++) {
                WidgetsList current = mChainList.get(i);
                if (orientation == HORIZONTAL) {
                    current.measureMatchConstraints(max - current.getWidth());
                } else {
                    current.measureMatchConstraints(max - current.getHeight());
                }
            }
        }

        for (int i = 0; i < listCount; i++) {
            WidgetsList current = mChainList.get(i);
            if (orientation == HORIZONTAL) {
                if (i < listCount - 1) {
                    WidgetsList next = mChainList.get(i + 1);
                    bottom = next.mBiggest.mTop;
                    paddingBottom = 0;
                } else {
                    bottom = mBottom;
                    paddingBottom = getPaddingBottom();
                }
                ConstraintAnchor currentBottom = current.mBiggest.mBottom;
                current.setup(orientation, left, top, right, bottom,
                        paddingLeft, paddingTop, paddingRight, paddingBottom, max);
                top = currentBottom;
                paddingTop = 0;
                maxWidth = Math.max(maxWidth, current.getWidth());
                maxHeight += current.getHeight();
                if (i > 0) {
                    maxHeight += mVerticalGap;
                }
            } else {
                if (i < listCount - 1) {
                    WidgetsList next = mChainList.get(i + 1);
                    right = next.mBiggest.mLeft;
                    paddingRight = 0;
                } else {
                    right = mRight;
                    paddingRight = getPaddingRight();
                }
                ConstraintAnchor currentRight = current.mBiggest.mRight;
                current.setup(orientation, left, top, right, bottom,
                        paddingLeft, paddingTop, paddingRight, paddingBottom, max);
                left = currentRight;
                paddingLeft = 0;
                maxWidth += current.getWidth();
                maxHeight = Math.max(maxHeight, current.getHeight());
                if (i > 0) {
                    maxWidth += mHorizontalGap;
                }
            }
        }
        measured[HORIZONTAL] = maxWidth;
        measured[VERTICAL] = maxHeight;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Measure No Wrap
    /////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Measure the virtual layout using a single chain for the children
     *
     * @param widgets     list of widgets
     * @param orientation the layout orientation (horizontal or vertical)
     * @param max         the maximum available space
     * @param measured    output parameters -- will contain the resulting measure
     */
    private void measureNoWrap(ConstraintWidget[] widgets,
            int count,
            int orientation,
            int max,
            int[] measured) {
        if (count == 0) {
            return;
        }
        WidgetsList list = null;
        if (mChainList.size() == 0) {
            list = new WidgetsList(orientation, mLeft, mTop, mRight, mBottom, max);
            mChainList.add(list);
        } else {
            list = mChainList.get(0);
            list.clear();
            list.setup(orientation, mLeft, mTop, mRight, mBottom,
                    getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom(), max);
        }

        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = widgets[i];
            list.add(widget);
        }

        measured[HORIZONTAL] = list.getWidth();
        measured[VERTICAL] = list.getHeight();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Measure Aligned
    /////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Measure the virtual layout arranging the children in a regular grid
     *
     * @param widgets     list of widgets
     * @param orientation the layout orientation (horizontal or vertical)
     * @param max         the maximum available space
     * @param measured    output parameters -- will contain the resulting measure
     */
    private void measureAligned(ConstraintWidget[] widgets,
            int count,
            int orientation,
            int max,
            int[] measured) {
        boolean done = false;
        int rows = 0;
        int cols = 0;

        if (orientation == HORIZONTAL) {
            cols = mMaxElementsWrap;
            if (cols <= 0) {
                // let's initialize cols with an acceptable value
                int w = 0;
                cols = 0;
                for (int i = 0; i < count; i++) {
                    if (i > 0) {
                        w += mHorizontalGap;
                    }
                    ConstraintWidget widget = widgets[i];
                    if (widget == null) {
                        continue;
                    }
                    w += getWidgetWidth(widget, max);
                    if (w > max) {
                        break;
                    }
                    cols++;
                }
            }
        } else {
            rows = mMaxElementsWrap;
            if (rows <= 0) {
                // let's initialize rows with an acceptable value
                int h = 0;
                rows = 0;
                for (int i = 0; i < count; i++) {
                    if (i > 0) {
                        h += mVerticalGap;
                    }
                    ConstraintWidget widget = widgets[i];
                    if (widget == null) {
                        continue;
                    }
                    h += getWidgetHeight(widget, max);
                    if (h > max) {
                        break;
                    }
                    rows++;
                }
            }
        }

        if (mAlignedDimensions == null) {
            mAlignedDimensions = new int[2];
        }

        if ((rows == 0 && orientation == VERTICAL)
                || (cols == 0 && orientation == HORIZONTAL)) {
            done = true;
        }

        while (!done) {
            // get a num of rows (or cols)
            // get for each row and cols the chain of biggest elements

            if (orientation == HORIZONTAL) {
                rows = (int) Math.ceil(count / (float) cols);
            } else {
                cols = (int) Math.ceil(count / (float) rows);
            }

            if (mAlignedBiggestElementsInCols == null
                    || mAlignedBiggestElementsInCols.length < cols) {
                mAlignedBiggestElementsInCols = new ConstraintWidget[cols];
            } else {
                Arrays.fill(mAlignedBiggestElementsInCols, null);
            }
            if (mAlignedBiggestElementsInRows == null
                    || mAlignedBiggestElementsInRows.length < rows) {
                mAlignedBiggestElementsInRows = new ConstraintWidget[rows];
            } else {
                Arrays.fill(mAlignedBiggestElementsInRows, null);
            }

            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    int index = j * cols + i;
                    if (orientation == VERTICAL) {
                        index = i * rows + j;
                    }
                    if (index >= widgets.length) {
                        continue;
                    }
                    ConstraintWidget widget = widgets[index];
                    if (widget == null) {
                        continue;
                    }
                    int w = getWidgetWidth(widget, max);
                    if (mAlignedBiggestElementsInCols[i] == null
                            || mAlignedBiggestElementsInCols[i].getWidth() < w) {
                        mAlignedBiggestElementsInCols[i] = widget;
                    }
                    int h = getWidgetHeight(widget, max);
                    if (mAlignedBiggestElementsInRows[j] == null
                            || mAlignedBiggestElementsInRows[j].getHeight() < h) {
                        mAlignedBiggestElementsInRows[j] = widget;
                    }
                }
            }

            int w = 0;
            for (int i = 0; i < cols; i++) {
                ConstraintWidget widget = mAlignedBiggestElementsInCols[i];
                if (widget != null) {
                    if (i > 0) {
                        w += mHorizontalGap;
                    }
                    w += getWidgetWidth(widget, max);
                }
            }
            int h = 0;
            for (int j = 0; j < rows; j++) {
                ConstraintWidget widget = mAlignedBiggestElementsInRows[j];
                if (widget != null) {
                    if (j > 0) {
                        h += mVerticalGap;
                    }
                    h += getWidgetHeight(widget, max);
                }
            }
            measured[HORIZONTAL] = w;
            measured[VERTICAL] = h;

            if (orientation == HORIZONTAL) {
                if (w > max) {
                    if (cols > 1) {
                        cols--;
                    } else {
                        done = true;
                    }
                } else {
                    done = true;
                }
            } else { // VERTICAL
                if (h > max) {
                    if (rows > 1) {
                        rows--;
                    } else {
                        done = true;
                    }
                } else {
                    done = true;
                }
            }
        }
        mAlignedDimensions[HORIZONTAL] = cols;
        mAlignedDimensions[VERTICAL] = rows;
    }

    private void createAlignedConstraints(boolean isInRtl) {
        if (mAlignedDimensions == null
                || mAlignedBiggestElementsInCols == null
                || mAlignedBiggestElementsInRows == null) {
            return;
        }

        for (int i = 0; i < mDisplayedWidgetsCount; i++) {
            ConstraintWidget widget = mDisplayedWidgets[i];
            widget.resetAnchors();
        }

        int cols = mAlignedDimensions[HORIZONTAL];
        int rows = mAlignedDimensions[VERTICAL];

        ConstraintWidget previous = null;
        float horizontalBias = mHorizontalBias;
        for (int i = 0; i < cols; i++) {
            int index = i;
            if (isInRtl) {
                index = cols - i - 1;
                horizontalBias = 1 - mHorizontalBias;
            }
            ConstraintWidget widget = mAlignedBiggestElementsInCols[index];
            if (widget == null || widget.getVisibility() == GONE) {
                continue;
            }
            if (i == 0) {
                widget.connect(widget.mLeft, mLeft, getPaddingLeft());
                widget.setHorizontalChainStyle(mHorizontalStyle);
                widget.setHorizontalBiasPercent(horizontalBias);
            }
            if (i == cols - 1) {
                widget.connect(widget.mRight, mRight, getPaddingRight());
            }
            if (i > 0 && previous != null) {
                widget.connect(widget.mLeft, previous.mRight, mHorizontalGap);
                previous.connect(previous.mRight, widget.mLeft, 0);
            }
            previous = widget;
        }
        for (int j = 0; j < rows; j++) {
            ConstraintWidget widget = mAlignedBiggestElementsInRows[j];
            if (widget == null || widget.getVisibility() == GONE) {
                continue;
            }
            if (j == 0) {
                widget.connect(widget.mTop, mTop, getPaddingTop());
                widget.setVerticalChainStyle(mVerticalStyle);
                widget.setVerticalBiasPercent(mVerticalBias);
            }
            if (j == rows - 1) {
                widget.connect(widget.mBottom, mBottom, getPaddingBottom());
            }
            if (j > 0 && previous != null) {
                widget.connect(widget.mTop, previous.mBottom, mVerticalGap);
                previous.connect(previous.mBottom, widget.mTop, 0);
            }
            previous = widget;
        }

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                int index = j * cols + i;
                if (mOrientation == VERTICAL) {
                    index = i * rows + j;
                }
                if (index >= mDisplayedWidgets.length) {
                    continue;
                }
                ConstraintWidget widget = mDisplayedWidgets[index];
                if (widget == null || widget.getVisibility() == GONE) {
                    continue;
                }
                ConstraintWidget biggestInCol = mAlignedBiggestElementsInCols[i];
                ConstraintWidget biggestInRow = mAlignedBiggestElementsInRows[j];
                if (widget != biggestInCol) {
                    widget.connect(widget.mLeft, biggestInCol.mLeft, 0);
                    widget.connect(widget.mRight, biggestInCol.mRight, 0);
                }
                if (widget != biggestInRow) {
                    widget.connect(widget.mTop, biggestInRow.mTop, 0);
                    widget.connect(widget.mBottom, biggestInRow.mBottom, 0);
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Add constraints to solver
    /////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add this widget to the solver
     *
     * @param system   the solver we want to add the widget to
     * @param optimize true if {@link Optimizer#OPTIMIZATION_GRAPH} is on
     */
    @Override
    public void addToSolver(LinearSystem system, boolean optimize) {
        super.addToSolver(system, optimize);

        boolean isInRtl = getParent() != null && ((ConstraintWidgetContainer) getParent()).isRtl();
        switch (mWrapMode) {
            case WRAP_CHAIN: {
                final int count = mChainList.size();
                for (int i = 0; i < count; i++) {
                    WidgetsList list = mChainList.get(i);
                    list.createConstraints(isInRtl, i, i == count - 1);
                }
            }
            break;
            case WRAP_NONE: {
                if (mChainList.size() > 0) {
                    WidgetsList list = mChainList.get(0);
                    list.createConstraints(isInRtl, 0, true);
                }
            }
            break;
            case WRAP_ALIGNED: {
                createAlignedConstraints(isInRtl);
            }
            break;
            case WRAP_CHAIN_NEW: {
                final int count = mChainList.size();
                for (int i = 0; i < count; i++) {
                    WidgetsList list = mChainList.get(i);
                    list.createConstraints(isInRtl, i, i == count - 1);
                }
            }
            break;
        }
        needsCallbackFromSolver(false);
    }
}
