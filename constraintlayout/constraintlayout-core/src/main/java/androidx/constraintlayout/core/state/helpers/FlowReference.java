/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.constraintlayout.core.state.helpers;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.UNKNOWN;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;
import static androidx.constraintlayout.core.widgets.Flow.HORIZONTAL_ALIGN_CENTER;
import static androidx.constraintlayout.core.widgets.Flow.VERTICAL_ALIGN_CENTER;
import static androidx.constraintlayout.core.widgets.Flow.WRAP_NONE;

import androidx.constraintlayout.core.state.HelperReference;
import androidx.constraintlayout.core.state.State;
import androidx.constraintlayout.core.widgets.Flow;
import androidx.constraintlayout.core.widgets.HelperWidget;

import java.util.HashMap;

/**
 * The FlowReference class can be used to store the relevant properties of a Flow Helper
 * when parsing the Flow Helper information in a JSON representation.
 *
 */
public class FlowReference extends HelperReference {
    protected Flow mFlow;

    protected HashMap<String, Float> mMapWeights;
    protected HashMap<String, Float> mMapPreMargin;
    protected HashMap<String, Float> mMapPostMargin;

    protected int mWrapMode = WRAP_NONE;

    protected int mVerticalStyle = UNKNOWN;
    protected int mFirstVerticalStyle = UNKNOWN;
    protected int mLastVerticalStyle = UNKNOWN;
    protected int mHorizontalStyle = UNKNOWN;
    protected int mFirstHorizontalStyle = UNKNOWN;
    protected int mLastHorizontalStyle = UNKNOWN;

    protected int mVerticalAlign = HORIZONTAL_ALIGN_CENTER;
    protected int mHorizontalAlign = VERTICAL_ALIGN_CENTER;

    protected int mVerticalGap = 0;
    protected int mHorizontalGap = 0;

    protected int mPaddingLeft = 0;
    protected int mPaddingRight = 0;
    protected int mPaddingTop = 0;
    protected int mPaddingBottom = 0;

    protected int mMaxElementsWrap = UNKNOWN;

    protected int mOrientation = HORIZONTAL;

    protected float mFirstVerticalBias = 0.5f;
    protected float mLastVerticalBias = 0.5f;
    protected float mFirstHorizontalBias = 0.5f;
    protected float mLastHorizontalBias = 0.5f;

    public FlowReference(State state, State.Helper type) {
        super(state, type);
        if (type == State.Helper.VERTICAL_FLOW) {
            mOrientation = VERTICAL;
        }
    }

    /**
     * Relate widgets to the FlowReference
     *
     * @param id id of a widget
     * @param weight weight of a widget
     * @param preMargin preMargin of a widget
     * @param postMargin postMargin of a widget
     */
    public void addFlowElement(String id, float weight, float preMargin, float postMargin) {
        super.add(id);
        if (!Float.isNaN(weight)) {
            if (mMapWeights == null) {
                mMapWeights = new HashMap<>();
            }
            mMapWeights.put(id, weight);
        }
        if (!Float.isNaN(preMargin)) {
            if (mMapPreMargin == null) {
                mMapPreMargin = new HashMap<>();
            }
            mMapPreMargin.put(id, preMargin);
        }
        if (!Float.isNaN(postMargin)) {
            if (mMapPostMargin == null) {
                mMapPostMargin = new HashMap<>();
            }
            mMapPostMargin.put(id, postMargin);
        }
    }

    /**
     * Get the weight of a widget
     *
     * @param id id of a widget
     * @return the weight of a widget
     */
    protected float getWeight(String id) {
        if (mMapWeights == null) {
            return UNKNOWN;
        }
        if (mMapWeights.containsKey(id)) {
            return mMapWeights.get(id);
        }
        return UNKNOWN;
    }

    /**
     * Get the post margin of a widget
     *
     * @param id id id of a widget
     * @return the post margin of a widget
     */
    protected float getPostMargin(String id) {
        if (mMapPreMargin != null  && mMapPreMargin.containsKey(id)) {
            return mMapPreMargin.get(id);
        }
        return 0;
    }

    /**
     * Get the pre margin of a widget
     *
     * @param id id id of a widget
     * @return the pre margin of a widget
     */
    protected float getPreMargin(String id) {
        if (mMapPostMargin != null  && mMapPostMargin.containsKey(id)) {
            return mMapPostMargin.get(id);
        }
        return 0;
    }

    /**
     * Get wrap mode
     *
     * @return wrap mode
     */
    public int getWrapMode() {
        return mWrapMode;
    }

    /**
     * Set wrap Mode
     *
     * @param wrap wrap Mode
     */
    public void setWrapMode(int wrap) {
        this.mWrapMode = wrap;
    }

    /**
     * Get paddingLeft
     *
     * @return paddingLeft value
     */
    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    /**
     * Set paddingLeft
     *
     * @param padding paddingLeft value
     */
    public void setPaddingLeft(int padding) {
        this.mPaddingLeft = padding;
    }

    /**
     * Get paddingTop
     *
     * @return paddingTop value
     */
    public int getPaddingTop() {
        return mPaddingTop;
    }

    /**
     * Set paddingTop
     *
     * @param padding paddingTop value
     */
    public void setPaddingTop(int padding) {
        this.mPaddingTop = padding;
    }

    /**
     * Get paddingRight
     *
     * @return paddingRight value
     */
    public int getPaddingRight() {
        return mPaddingRight;
    }

    /**
     * Set paddingRight
     *
     * @param padding paddingRight value
     */
    public void setPaddingRight(int padding) {
        this.mPaddingRight = padding;
    }

    /**
     * Get paddingBottom
     *
     * @return paddingBottom value
     */
    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    /**
     * Set padding
     *
     * @param padding paddingBottom value
     */
    public void setPaddingBottom(int padding) {
        this.mPaddingBottom = padding;
    }

    /**
     * Get vertical style
     *
     * @return vertical style
     */
    public int getVerticalStyle() {
        return mVerticalStyle;
    }

    /**
     * set vertical style
     *
     * @param verticalStyle Flow vertical style
     */
    public void setVerticalStyle(int verticalStyle) {
        this.mVerticalStyle = verticalStyle;
    }

    /**
     * Get first vertical style
     *
     * @return first vertical style
     */
    public int getFirstVerticalStyle() {
        return mFirstVerticalStyle;
    }

    /**
     * Set first vertical style
     *
     * @param firstVerticalStyle Flow first vertical style
     */
    public void setFirstVerticalStyle(int firstVerticalStyle) {
        this.mFirstVerticalStyle = firstVerticalStyle;
    }

    /**
     * Get last vertical style
     *
     * @return last vertical style
     */
    public int getLastVerticalStyle() {
        return mLastVerticalStyle;
    }

    /**
     * Set last vertical style
     *
     * @param lastVerticalStyle Flow last vertical style
     */
    public void setLastVerticalStyle(int lastVerticalStyle) {
        this.mLastVerticalStyle = lastVerticalStyle;
    }

    /**
     * Get horizontal style
     *
     * @return horizontal style
     */
    public int getHorizontalStyle() {
        return mHorizontalStyle;
    }

    /**
     * Set horizontal style
     *
     * @param horizontalStyle Flow horizontal style
     */
    public void setHorizontalStyle(int horizontalStyle) {
        this.mHorizontalStyle = horizontalStyle;
    }

    /**
     * Get first horizontal style
     *
     * @return first horizontal style
     */
    public int getFirstHorizontalStyle() {
        return mFirstHorizontalStyle;
    }

    /**
     * Set first horizontal style
     *
     * @param firstHorizontalStyle Flow first horizontal style
     */
    public void setFirstHorizontalStyle(int firstHorizontalStyle) {
        this.mFirstHorizontalStyle = firstHorizontalStyle;
    }

    /**
     * Get last horizontal style
     *
     * @return last horizontal style
     */
    public int getLastHorizontalStyle() {
        return mLastHorizontalStyle;
    }

    /**
     * Set last horizontal style
     *
     * @param lastHorizontalStyle Flow last horizontal style
     */
    public void setLastHorizontalStyle(int lastHorizontalStyle) {
        this.mLastHorizontalStyle = lastHorizontalStyle;
    }

    /**
     * Get vertical align
     * @return vertical align value
     */
    public int getVerticalAlign() {
        return mVerticalAlign;
    }

    /**
     * Set vertical align
     *
     * @param verticalAlign vertical align value
     */
    public void setVerticalAlign(int verticalAlign) {
        this.mVerticalAlign = verticalAlign;
    }

    /**
     * Get horizontal align
     *
     * @return horizontal align value
     */
    public int getHorizontalAlign() {
        return mHorizontalAlign;
    }

    /**
     * Set horizontal align
     *
     * @param horizontalAlign horizontal align value
     */
    public void setHorizontalAlign(int horizontalAlign) {
        this.mHorizontalAlign = horizontalAlign;
    }

    /**
     * Get vertical gap
     *
     * @return vertical gap value
     */
    public int getVerticalGap() {
        return mVerticalGap;
    }

    /**
     * Set vertical gap
     *
     * @param verticalGap vertical gap value
     */
    public void setVerticalGap(int verticalGap) {
        this.mVerticalGap = verticalGap;
    }

    /**
     * Get horizontal gap
     *
     * @return horizontal gap value
     */
    public int getHorizontalGap() {
        return mHorizontalGap;
    }

    /**
     * Set horizontal gap
     *
     * @param horizontalGap horizontal gap value
     */
    public void setHorizontalGap(int horizontalGap) {
        mHorizontalGap = horizontalGap;
    }

    /**
     * Get max element wrap
     *
     * @return max element wrap value
     */
    public int getMaxElementsWrap() {
        return mMaxElementsWrap;
    }

    /**
     * Set max element wrap
     *
     * @param maxElementsWrap max element wrap value
     */
    public void setMaxElementsWrap(int maxElementsWrap) {
        this.mMaxElementsWrap = maxElementsWrap;
    }

    /**
     * Get the orientation of a Flow
     *
     * @return orientation value
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * Set the orientation of a Flow
     *
     * @param mOrientation orientation value
     */
    public void setOrientation(int mOrientation) {
        this.mOrientation = mOrientation;
    }

    /**
     * Get vertical bias
     *
     * @return vertical bias value
     */
    public float getVerticalBias() {
        return mVerticalBias;
    }


    /**
     * Get first vertical bias
     *
     * @return first vertical bias value
     */
    public float getFirstVerticalBias() {
        return mFirstVerticalBias;
    }

    /**
     * Set first vertical bias
     *
     * @param firstVerticalBias first vertical bias value
     */
    public void setFirstVerticalBias(float firstVerticalBias) {
        this.mFirstVerticalBias = firstVerticalBias;
    }

    /**
     * Get last vertical bias
     *
     * @return last vertical bias
     */
    public float getLastVerticalBias() {
        return mLastVerticalBias;
    }

    /**
     * Set last vertical bias
     *
     * @param lastVerticalBias last vertical bias value
     */
    public void setLastVerticalBias(float lastVerticalBias) {
        this.mLastVerticalBias = lastVerticalBias;
    }

    /**
     * Get horizontal bias
     * @return horizontal bias value
     */
    public float getHorizontalBias() {
        return mHorizontalBias;
    }

    /**
     * Get first horizontal bias
     *
     * @return first horizontal bias
     */
    public float getFirstHorizontalBias() {
        return mFirstHorizontalBias;
    }

    /**
     * Set first horizontal bias
     *
     * @param firstHorizontalBias first horizontal bias value
     */
    public void setFirstHorizontalBias(float firstHorizontalBias) {
        this.mFirstHorizontalBias = firstHorizontalBias;
    }

    /**
     * Get last horizontal bias
     *
     * @return last horizontal bias value
     */
    public float getLastHorizontalBias() {
        return mLastHorizontalBias;
    }

    /**
     * Set last horizontal bias
     *
     * @param lastHorizontalBias last horizontal bias value
     */
    public void setLastHorizontalBias(float lastHorizontalBias) {
        this.mLastHorizontalBias = lastHorizontalBias;
    }

    @Override
    public HelperWidget getHelperWidget() {
        if (mFlow == null) {
            mFlow = new Flow();
        }
        return mFlow;
    }

    @Override
    public void setHelperWidget(HelperWidget widget) {
        if (widget instanceof Flow) {
            mFlow = (Flow) widget;
        } else {
            mFlow = null;
        }
    }

    @Override
    public void apply() {
        getHelperWidget();
        this.setConstraintWidget(mFlow);
        mFlow.setOrientation(mOrientation);
        mFlow.setWrapMode(mWrapMode);

        if (mMaxElementsWrap != UNKNOWN) {
            mFlow.setMaxElementsWrap(mMaxElementsWrap);
        }

        // Padding
        if (mPaddingLeft != 0) {
            mFlow.setPaddingLeft(mPaddingLeft);
        }
        if (mPaddingTop != 0) {
            mFlow.setPaddingTop(mPaddingTop);
        }
        if (mPaddingRight != 0) {
            mFlow.setPaddingRight(mPaddingRight);
        }
        if (mPaddingBottom != 0) {
            mFlow.setPaddingBottom(mPaddingBottom);
        }

        // Gap
        if (mHorizontalGap != 0) {
            mFlow.setHorizontalGap(mHorizontalGap);
        }
        if (mVerticalGap != 0) {
            mFlow.setVerticalGap(mVerticalGap);
        }

        // Bias
        if (mHorizontalBias != 0.5f) {
            mFlow.setHorizontalBias(mHorizontalBias);
        }
        if (mFirstHorizontalBias != 0.5f) {
            mFlow.setFirstHorizontalBias(mFirstHorizontalBias);
        }
        if (mLastHorizontalBias != 0.5f) {
            mFlow.setLastHorizontalBias(mLastHorizontalBias);
        }
        if (mVerticalBias != 0.5f) {
            mFlow.setVerticalBias(mVerticalBias);
        }
        if (mFirstVerticalBias != 0.5f) {
            mFlow.setFirstVerticalBias(mFirstVerticalBias);
        }
        if (mLastVerticalBias != 0.5f) {
            mFlow.setLastVerticalBias(mLastVerticalBias);
        }

        // Align
        if (mHorizontalAlign != HORIZONTAL_ALIGN_CENTER) {
            mFlow.setHorizontalAlign(mHorizontalAlign);
        }
        if (mVerticalAlign != VERTICAL_ALIGN_CENTER) {
            mFlow.setVerticalAlign(mVerticalAlign);
        }

        // Style
        if (mVerticalStyle != UNKNOWN) {
            mFlow.setVerticalStyle(mVerticalStyle);
        }
        if (mFirstVerticalStyle != UNKNOWN) {
            mFlow.setFirstVerticalStyle(mFirstVerticalStyle);
        }
        if (mLastVerticalStyle != UNKNOWN) {
            mFlow.setLastVerticalStyle(mLastVerticalStyle);
        }
        if (mHorizontalStyle != UNKNOWN) {
            mFlow.setHorizontalStyle(mHorizontalStyle);
        }
        if (mFirstHorizontalStyle != UNKNOWN) {
            mFlow.setFirstHorizontalStyle(mFirstHorizontalStyle);
        }
        if (mLastHorizontalStyle!= UNKNOWN) {
            mFlow.setLastHorizontalStyle(mLastHorizontalStyle);
        }

        // General attributes of a widget
        applyBase();
    }
}
