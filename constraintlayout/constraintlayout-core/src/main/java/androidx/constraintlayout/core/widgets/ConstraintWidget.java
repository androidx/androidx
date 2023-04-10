/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static androidx.constraintlayout.core.LinearSystem.DEBUG;
import static androidx.constraintlayout.core.LinearSystem.FULL_DEBUG;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;

import androidx.constraintlayout.core.Cache;
import androidx.constraintlayout.core.LinearSystem;
import androidx.constraintlayout.core.SolverVariable;
import androidx.constraintlayout.core.state.WidgetFrame;
import androidx.constraintlayout.core.widgets.analyzer.ChainRun;
import androidx.constraintlayout.core.widgets.analyzer.HorizontalWidgetRun;
import androidx.constraintlayout.core.widgets.analyzer.VerticalWidgetRun;
import androidx.constraintlayout.core.widgets.analyzer.WidgetRun;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Implements a constraint Widget model supporting constraints relations between other widgets.
 * <p>
 * The widget has various anchors (i.e. Left, Top, Right, Bottom, representing their respective
 * sides, as well as Baseline, Center_X and Center_Y). Connecting anchors from one widget to another
 * represents a constraint relation between the two anchors; the {@link LinearSystem} will then
 * be able to use this model to try to minimize the distances between connected anchors.
 * </p>
 * <p>
 * If opposite anchors are connected (e.g. Left and Right anchors), if they have the same strength,
 * the widget will be equally pulled toward their respective target anchor positions; if the widget
 * has a fixed size, this means that the widget will be centered between the two target anchors. If
 * the widget's size is allowed to adjust, the size of the widget will change to be as large as
 * necessary so that the widget's anchors and the target anchors' distances are zero.
 * </p>
 * Constraints are set by connecting a widget's anchor to another via the
 * {@link #connect} function.
 */
public class ConstraintWidget {
    private static final boolean AUTOTAG_CENTER = false;
    private static final boolean DO_NOT_USE = false;
    protected static final int SOLVER = 1;
    protected static final int DIRECT = 2;

    // apply an intrinsic size when wrap content for spread dimensions
    private static final boolean USE_WRAP_DIMENSION_FOR_SPREAD = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Graph measurements
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean measured = false;
    public WidgetRun[] run = new WidgetRun[2];
    public ChainRun horizontalChainRun;
    public ChainRun verticalChainRun;

    public HorizontalWidgetRun mHorizontalRun = null;
    public VerticalWidgetRun mVerticalRun = null;

    public boolean[] isTerminalWidget = {true, true};
    boolean mResolvedHasRatio = false;
    private boolean mMeasureRequested = true;
    private boolean mOptimizeWrapO = false;
    private boolean mOptimizeWrapOnResolved = true;

    private int mWidthOverride = -1;
    private int mHeightOverride = -1;

    public WidgetFrame frame = new WidgetFrame(this);

    public String stringId;

    // @TODO: add description
    public WidgetRun getRun(int orientation) {
        if (orientation == HORIZONTAL) {
            return mHorizontalRun;
        } else if (orientation == VERTICAL) {
            return mVerticalRun;
        }
        return null;
    }

    private boolean mResolvedHorizontal = false;
    private boolean mResolvedVertical = false;

    private boolean mHorizontalSolvingPass = false;
    private boolean mVerticalSolvingPass = false;

    // @TODO: add description
    public void setFinalFrame(int left,
            int top,
            int right,
            int bottom,
            int baseline,
            int orientation) {
        setFrame(left, top, right, bottom);
        setBaselineDistance(baseline);
        if (orientation == HORIZONTAL) {
            mResolvedHorizontal = true;
            mResolvedVertical = false;
        } else if (orientation == VERTICAL) {
            mResolvedHorizontal = false;
            mResolvedVertical = true;
        } else if (orientation == BOTH) {
            mResolvedHorizontal = true;
            mResolvedVertical = true;
        } else {
            mResolvedHorizontal = false;
            mResolvedVertical = false;
        }
    }

    // @TODO: add description
    public void setFinalLeft(int x1) {
        mLeft.setFinalValue(x1);
        mX = x1;
    }

    // @TODO: add description
    public void setFinalTop(int y1) {
        mTop.setFinalValue(y1);
        mY = y1;
    }

    // @TODO: add description
    public void resetSolvingPassFlag() {
        mHorizontalSolvingPass = false;
        mVerticalSolvingPass = false;
    }

    public boolean isHorizontalSolvingPassDone() {
        return mHorizontalSolvingPass;
    }

    public boolean isVerticalSolvingPassDone() {
        return mVerticalSolvingPass;
    }

    // @TODO: add description
    public void markHorizontalSolvingPassDone() {
        mHorizontalSolvingPass = true;
    }

    // @TODO: add description
    public void markVerticalSolvingPassDone() {
        mVerticalSolvingPass = true;
    }

    // @TODO: add description
    public void setFinalHorizontal(int x1, int x2) {
        if (mResolvedHorizontal) {
            return;
        }
        mLeft.setFinalValue(x1);
        mRight.setFinalValue(x2);
        mX = x1;
        mWidth = x2 - x1;
        mResolvedHorizontal = true;
        if (LinearSystem.FULL_DEBUG) {
            System.out.println("*** SET FINAL HORIZONTAL FOR " + getDebugName()
                    + " : " + x1 + " -> " + x2 + " (width: " + mWidth + ")");
        }
    }

    // @TODO: add description
    public void setFinalVertical(int y1, int y2) {
        if (mResolvedVertical) {
            return;
        }
        mTop.setFinalValue(y1);
        mBottom.setFinalValue(y2);
        mY = y1;
        mHeight = y2 - y1;
        if (mHasBaseline) {
            mBaseline.setFinalValue(y1 + mBaselineDistance);
        }
        mResolvedVertical = true;
        if (LinearSystem.FULL_DEBUG) {
            System.out.println("*** SET FINAL VERTICAL FOR " + getDebugName()
                    + " : " + y1 + " -> " + y2 + " (height: " + mHeight + ")");
        }
    }

    // @TODO: add description
    public void setFinalBaseline(int baselineValue) {
        if (!mHasBaseline) {
            return;
        }
        int y1 = baselineValue - mBaselineDistance;
        int y2 = y1 + mHeight;
        mY = y1;
        mTop.setFinalValue(y1);
        mBottom.setFinalValue(y2);
        mBaseline.setFinalValue(baselineValue);
        mResolvedVertical = true;
    }

    public boolean isResolvedHorizontally() {
        return mResolvedHorizontal || (mLeft.hasFinalValue() && mRight.hasFinalValue());
    }

    public boolean isResolvedVertically() {
        return mResolvedVertical || (mTop.hasFinalValue() && mBottom.hasFinalValue());
    }

    // @TODO: add description
    public void resetFinalResolution() {
        mResolvedHorizontal = false;
        mResolvedVertical = false;
        mHorizontalSolvingPass = false;
        mVerticalSolvingPass = false;
        for (int i = 0, mAnchorsSize = mAnchors.size(); i < mAnchorsSize; i++) {
            final ConstraintAnchor anchor = mAnchors.get(i);
            anchor.resetFinalResolution();
        }
    }

    // @TODO: add description
    public void ensureMeasureRequested() {
        mMeasureRequested = true;
    }

    // @TODO: add description
    public boolean hasDependencies() {
        for (int i = 0, mAnchorsSize = mAnchors.size(); i < mAnchorsSize; i++) {
            final ConstraintAnchor anchor = mAnchors.get(i);
            if (anchor.hasDependents()) {
                return true;
            }
        }
        return false;
    }

    // @TODO: add description
    public boolean hasDanglingDimension(int orientation) {
        if (orientation == HORIZONTAL) {
            int horizontalTargets =
                    (mLeft.mTarget != null ? 1 : 0) + (mRight.mTarget != null ? 1 : 0);
            return horizontalTargets < 2;
        } else {
            int verticalTargets = (mTop.mTarget != null ? 1 : 0)
                    + (mBottom.mTarget != null ? 1 : 0) + (mBaseline.mTarget != null ? 1 : 0);
            return verticalTargets < 2;
        }
    }

    // @TODO: add description
    public boolean hasResolvedTargets(int orientation, int size) {
        if (orientation == HORIZONTAL) {
            if (mLeft.mTarget != null && mLeft.mTarget.hasFinalValue()
                    && mRight.mTarget != null && mRight.mTarget.hasFinalValue()) {
                return ((mRight.mTarget.getFinalValue() - mRight.getMargin())
                        - (mLeft.mTarget.getFinalValue() + mLeft.getMargin())) >= size;
            }
        } else {
            if (mTop.mTarget != null && mTop.mTarget.hasFinalValue()
                    && mBottom.mTarget != null && mBottom.mTarget.hasFinalValue()) {
                return ((mBottom.mTarget.getFinalValue() - mBottom.getMargin())
                        - (mTop.mTarget.getFinalValue() + mTop.getMargin())) >= size;
            }
        }
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static final int MATCH_CONSTRAINT_SPREAD = 0;
    public static final int MATCH_CONSTRAINT_WRAP = 1;
    public static final int MATCH_CONSTRAINT_PERCENT = 2;
    public static final int MATCH_CONSTRAINT_RATIO = 3;
    public static final int MATCH_CONSTRAINT_RATIO_RESOLVED = 4;

    public static final int UNKNOWN = -1;
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    public static final int BOTH = 2;

    public static final int VISIBLE = 0;
    public static final int INVISIBLE = 4;
    public static final int GONE = 8;

    // Values of the chain styles
    public static final int CHAIN_SPREAD = 0;
    public static final int CHAIN_SPREAD_INSIDE = 1;
    public static final int CHAIN_PACKED = 2;

    // Values of the wrap behavior in parent
    public static final int WRAP_BEHAVIOR_INCLUDED = 0; // default
    public static final int WRAP_BEHAVIOR_HORIZONTAL_ONLY = 1;
    public static final int WRAP_BEHAVIOR_VERTICAL_ONLY = 2;
    public static final int WRAP_BEHAVIOR_SKIPPED = 3;

    // Support for direct resolution
    public int mHorizontalResolution = UNKNOWN;
    public int mVerticalResolution = UNKNOWN;

    private static final int WRAP = -2;

    private int mWrapBehaviorInParent = WRAP_BEHAVIOR_INCLUDED;

    public int mMatchConstraintDefaultWidth = MATCH_CONSTRAINT_SPREAD;
    public int mMatchConstraintDefaultHeight = MATCH_CONSTRAINT_SPREAD;
    public int[] mResolvedMatchConstraintDefault = new int[2];

    public int mMatchConstraintMinWidth = 0;
    public int mMatchConstraintMaxWidth = 0;
    public float mMatchConstraintPercentWidth = 1;
    public int mMatchConstraintMinHeight = 0;
    public int mMatchConstraintMaxHeight = 0;
    public float mMatchConstraintPercentHeight = 1;
    public boolean mIsWidthWrapContent;
    public boolean mIsHeightWrapContent;

    int mResolvedDimensionRatioSide = UNKNOWN;
    float mResolvedDimensionRatio = 1.0f;

    private int[] mMaxDimension = {Integer.MAX_VALUE, Integer.MAX_VALUE};
    public float mCircleConstraintAngle = Float.NaN;
    private boolean mHasBaseline = false;
    private boolean mInPlaceholder;

    private boolean mInVirtualLayout = false;

    public boolean isInVirtualLayout() {
        return mInVirtualLayout;
    }

    public void setInVirtualLayout(boolean inVirtualLayout) {
        mInVirtualLayout = inVirtualLayout;
    }

    public int getMaxHeight() {
        return mMaxDimension[VERTICAL];
    }

    public int getMaxWidth() {
        return mMaxDimension[HORIZONTAL];
    }

    public void setMaxWidth(int maxWidth) {
        mMaxDimension[HORIZONTAL] = maxWidth;
    }

    public void setMaxHeight(int maxHeight) {
        mMaxDimension[VERTICAL] = maxHeight;
    }

    public boolean isSpreadWidth() {
        return mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD
                && mDimensionRatio == 0
                && mMatchConstraintMinWidth == 0
                && mMatchConstraintMaxWidth == 0
                && mListDimensionBehaviors[HORIZONTAL] == MATCH_CONSTRAINT;
    }

    public boolean isSpreadHeight() {
        return mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD
                && mDimensionRatio == 0
                && mMatchConstraintMinHeight == 0
                && mMatchConstraintMaxHeight == 0
                && mListDimensionBehaviors[VERTICAL] == MATCH_CONSTRAINT;
    }

    public void setHasBaseline(boolean hasBaseline) {
        this.mHasBaseline = hasBaseline;
    }

    public boolean getHasBaseline() {
        return mHasBaseline;
    }

    public boolean isInPlaceholder() {
        return mInPlaceholder;
    }

    public void setInPlaceholder(boolean inPlaceholder) {
        this.mInPlaceholder = inPlaceholder;
    }

    protected void setInBarrier(int orientation, boolean value) {
        mIsInBarrier[orientation] = value;
    }

    // @TODO: add description
    public boolean isInBarrier(int orientation) {
        return mIsInBarrier[orientation];
    }

    public void setMeasureRequested(boolean measureRequested) {
        mMeasureRequested = measureRequested;
    }

    public boolean isMeasureRequested() {
        return mMeasureRequested && mVisibility != GONE;
    }

    // @TODO: add description
    public void setWrapBehaviorInParent(int behavior) {
        if (behavior >= 0 && behavior <= WRAP_BEHAVIOR_SKIPPED) {
            mWrapBehaviorInParent = behavior;
        }
    }

    public int getWrapBehaviorInParent() {
        return mWrapBehaviorInParent;
    }

    /**
     * Keep a cache of the last measure cache as we can bypass remeasures during the onMeasure...
     * the View's measure cache will only be reset in onLayout, so too late for us.
     */
    private int mLastHorizontalMeasureSpec = 0;
    private int mLastVerticalMeasureSpec = 0;

    public int getLastHorizontalMeasureSpec() {
        return mLastHorizontalMeasureSpec;
    }

    public int getLastVerticalMeasureSpec() {
        return mLastVerticalMeasureSpec;
    }

    // @TODO: add description
    public void setLastMeasureSpec(int horizontal, int vertical) {
        mLastHorizontalMeasureSpec = horizontal;
        mLastVerticalMeasureSpec = vertical;
        setMeasureRequested(false);
    }

    /**
     * Define how the widget will resize
     */
    public enum DimensionBehaviour {
        FIXED, WRAP_CONTENT, MATCH_CONSTRAINT, MATCH_PARENT
    }

    // The anchors available on the widget
    // note: all anchors should be added to the mAnchors array (see addAnchors())
    public ConstraintAnchor mLeft = new ConstraintAnchor(this, ConstraintAnchor.Type.LEFT);
    public ConstraintAnchor mTop = new ConstraintAnchor(this, ConstraintAnchor.Type.TOP);
    public ConstraintAnchor mRight = new ConstraintAnchor(this, ConstraintAnchor.Type.RIGHT);
    public ConstraintAnchor mBottom = new ConstraintAnchor(this, ConstraintAnchor.Type.BOTTOM);
    public ConstraintAnchor mBaseline = new ConstraintAnchor(this, ConstraintAnchor.Type.BASELINE);
    ConstraintAnchor mCenterX = new ConstraintAnchor(this, ConstraintAnchor.Type.CENTER_X);
    ConstraintAnchor mCenterY = new ConstraintAnchor(this, ConstraintAnchor.Type.CENTER_Y);
    public ConstraintAnchor mCenter = new ConstraintAnchor(this, ConstraintAnchor.Type.CENTER);

    public static final int ANCHOR_LEFT = 0;
    public static final int ANCHOR_RIGHT = 1;
    public static final int ANCHOR_TOP = 2;
    public static final int ANCHOR_BOTTOM = 3;
    public static final int ANCHOR_BASELINE = 4;

    public ConstraintAnchor[] mListAnchors = {mLeft, mRight, mTop, mBottom, mBaseline, mCenter};
    protected ArrayList<ConstraintAnchor> mAnchors = new ArrayList<>();

    private boolean[] mIsInBarrier = new boolean[2];

    // The horizontal and vertical behaviour for the widgets' dimensions
    static final int DIMENSION_HORIZONTAL = 0;
    static final int DIMENSION_VERTICAL = 1;
    public DimensionBehaviour[] mListDimensionBehaviors =
            {DimensionBehaviour.FIXED, DimensionBehaviour.FIXED};

    // Parent of this widget
    public ConstraintWidget mParent = null;

    // Dimensions of the widget
    int mWidth = 0;
    int mHeight = 0;
    public float mDimensionRatio = 0;
    protected int mDimensionRatioSide = UNKNOWN;

    // Origin of the widget
    protected int mX = 0;
    protected int mY = 0;
    int mRelX = 0;
    int mRelY = 0;

    // Root offset
    protected int mOffsetX = 0;
    protected int mOffsetY = 0;

    // Baseline distance relative to the top of the widget
    int mBaselineDistance = 0;

    // Minimum sizes for the widget
    protected int mMinWidth;
    protected int mMinHeight;

    // Percentages used for biasing one connection over another when dual connections
    // of the same strength exist
    public static float DEFAULT_BIAS = 0.5f;
    float mHorizontalBiasPercent = DEFAULT_BIAS;
    float mVerticalBiasPercent = DEFAULT_BIAS;

    // The companion widget (typically, the real widget we represent)
    private Object mCompanionWidget;

    // This is used to possibly "skip" a position while inside a container. For example,
    // a container like Table can use this to implement empty cells
    // (the item positioned after the empty cell will have a skip value of 1)
    private int mContainerItemSkip = 0;

    // Contains the visibility status of the widget (VISIBLE, INVISIBLE, or GONE)
    private int mVisibility = VISIBLE;
    // Contains if this widget is animated. Currently only affects gone behaviour
    private boolean mAnimated = false;
    private String mDebugName = null;
    private String mType = null;

    int mDistToTop;
    int mDistToLeft;
    int mDistToRight;
    int mDistToBottom;
    boolean mLeftHasCentered;
    boolean mRightHasCentered;
    boolean mTopHasCentered;
    boolean mBottomHasCentered;
    boolean mHorizontalWrapVisited;
    boolean mVerticalWrapVisited;
    boolean mGroupsToSolver = false;

    // Chain support
    int mHorizontalChainStyle = CHAIN_SPREAD;
    int mVerticalChainStyle = CHAIN_SPREAD;
    boolean mHorizontalChainFixedPosition;
    boolean mVerticalChainFixedPosition;

    public float[] mWeight = {UNKNOWN, UNKNOWN};

    protected ConstraintWidget[] mListNextMatchConstraintsWidget = {null, null};
    protected ConstraintWidget[] mNextChainWidget = {null, null};

    ConstraintWidget mHorizontalNextWidget = null;
    ConstraintWidget mVerticalNextWidget = null;

    // TODO: see if we can make this simpler

    // @TODO: add description
    public void reset() {
        mLeft.reset();
        mTop.reset();
        mRight.reset();
        mBottom.reset();
        mBaseline.reset();
        mCenterX.reset();
        mCenterY.reset();
        mCenter.reset();
        mParent = null;
        mCircleConstraintAngle = Float.NaN;
        mWidth = 0;
        mHeight = 0;
        mDimensionRatio = 0;
        mDimensionRatioSide = UNKNOWN;
        mX = 0;
        mY = 0;
        mOffsetX = 0;
        mOffsetY = 0;
        mBaselineDistance = 0;
        mMinWidth = 0;
        mMinHeight = 0;
        mHorizontalBiasPercent = DEFAULT_BIAS;
        mVerticalBiasPercent = DEFAULT_BIAS;
        mListDimensionBehaviors[DIMENSION_HORIZONTAL] = DimensionBehaviour.FIXED;
        mListDimensionBehaviors[DIMENSION_VERTICAL] = DimensionBehaviour.FIXED;
        mCompanionWidget = null;
        mContainerItemSkip = 0;
        mVisibility = VISIBLE;
        mType = null;
        mHorizontalWrapVisited = false;
        mVerticalWrapVisited = false;
        mHorizontalChainStyle = CHAIN_SPREAD;
        mVerticalChainStyle = CHAIN_SPREAD;
        mHorizontalChainFixedPosition = false;
        mVerticalChainFixedPosition = false;
        mWeight[DIMENSION_HORIZONTAL] = UNKNOWN;
        mWeight[DIMENSION_VERTICAL] = UNKNOWN;
        mHorizontalResolution = UNKNOWN;
        mVerticalResolution = UNKNOWN;
        mMaxDimension[HORIZONTAL] = Integer.MAX_VALUE;
        mMaxDimension[VERTICAL] = Integer.MAX_VALUE;
        mMatchConstraintDefaultWidth = MATCH_CONSTRAINT_SPREAD;
        mMatchConstraintDefaultHeight = MATCH_CONSTRAINT_SPREAD;
        mMatchConstraintPercentWidth = 1;
        mMatchConstraintPercentHeight = 1;
        mMatchConstraintMaxWidth = Integer.MAX_VALUE;
        mMatchConstraintMaxHeight = Integer.MAX_VALUE;
        mMatchConstraintMinWidth = 0;
        mMatchConstraintMinHeight = 0;
        mResolvedHasRatio = false;
        mResolvedDimensionRatioSide = UNKNOWN;
        mResolvedDimensionRatio = 1f;
        mGroupsToSolver = false;
        isTerminalWidget[HORIZONTAL] = true;
        isTerminalWidget[VERTICAL] = true;
        mInVirtualLayout = false;
        mIsInBarrier[HORIZONTAL] = false;
        mIsInBarrier[VERTICAL] = false;
        mMeasureRequested = true;
        mResolvedMatchConstraintDefault[HORIZONTAL] = 0;
        mResolvedMatchConstraintDefault[VERTICAL] = 0;
        mWidthOverride = -1;
        mHeightOverride = -1;
    }

    ///////////////////////////////////SERIALIZE///////////////////////////////////////////////

    private void serializeAnchor(StringBuilder ret, String side, ConstraintAnchor a) {
        if (a.mTarget == null) {
            return;
        }
        ret.append(side);
        ret.append(" : [ '");
        ret.append(a.mTarget);
        ret.append("',");
        ret.append(a.mMargin);
        ret.append(",");
        ret.append(a.mGoneMargin);
        ret.append(",");
        ret.append(" ] ,\n");
    }

    private void serializeCircle(StringBuilder ret, ConstraintAnchor a, float angle) {
        if (a.mTarget == null || Float.isNaN(angle)) {
            return;
        }

        ret.append("circle : [ '");
        ret.append(a.mTarget);
        ret.append("',");
        ret.append(a.mMargin);
        ret.append(",");
        ret.append(angle);
        ret.append(",");
        ret.append(" ] ,\n");
    }

    private void serializeAttribute(StringBuilder ret, String type, float value, float def) {
        if (value == def) {
            return;
        }
        ret.append(type);
        ret.append(" :   ");
        ret.append(value);
        ret.append(",\n");
    }

    private void serializeAttribute(StringBuilder ret, String type, int value, int def) {
        if (value == def) {
            return;
        }
        ret.append(type);
        ret.append(" :   ");
        ret.append(value);
        ret.append(",\n");
    }

    private void serializeAttribute(StringBuilder ret, String type, String value, String def) {
        if (def.equals(value)) {
            return;
        }
        ret.append(type);
        ret.append(" :   ");
        ret.append(value);
        ret.append(",\n");
    }

    private void serializeDimensionRatio(StringBuilder ret,
            String type,
            float value,
            int whichSide) {
        if (value == 0) {
            return;
        }
        ret.append(type);
        ret.append(" :  [");
        ret.append(value);
        ret.append(",");
        ret.append(whichSide);
        ret.append("");
        ret.append("],\n");
    }

    private void serializeSize(StringBuilder ret, String type, int size,
            int min, int max, int override,
            int matchConstraintMin, int matchConstraintDefault,
            float matchConstraintPercent,
            float weight) {
        ret.append(type);
        ret.append(" :  {\n");
        serializeAttribute(ret, "size", size, Integer.MIN_VALUE);
        serializeAttribute(ret, "min", min, 0);
        serializeAttribute(ret, "max", max, Integer.MAX_VALUE);
        serializeAttribute(ret, "matchMin", matchConstraintMin, 0);
        serializeAttribute(ret, "matchDef", matchConstraintDefault, MATCH_CONSTRAINT_SPREAD);
        serializeAttribute(ret, "matchPercent", matchConstraintDefault, 1);
        serializeAttribute(ret, "matchConstraintPercent", matchConstraintPercent, 1);
        serializeAttribute(ret, "weight", weight, 1);
        serializeAttribute(ret, "override", override, 1);

        ret.append("},\n");
    }

    /**
     * Serialize the anchors for JSON5 output
     * @param ret StringBuilder to be populated
     * @return the same string builder to alow chaining
     */
    public StringBuilder serialize(StringBuilder ret) {
        ret.append("{\n");
        serializeAnchor(ret, "left", mLeft);
        serializeAnchor(ret, "top", mTop);
        serializeAnchor(ret, "right", mRight);
        serializeAnchor(ret, "bottom", mBottom);
        serializeAnchor(ret, "baseline", mBaseline);
        serializeAnchor(ret, "centerX", mCenterX);
        serializeAnchor(ret, "centerY", mCenterY);
        serializeCircle(ret, mCenter, mCircleConstraintAngle);

        serializeSize(ret, "width",
                mWidth,
                mMinWidth,
                mMaxDimension[HORIZONTAL],
                mWidthOverride,
                mMatchConstraintMinWidth,
                mMatchConstraintDefaultWidth,
                mMatchConstraintPercentWidth,
                mWeight[DIMENSION_HORIZONTAL]
        );

        serializeSize(ret, "height",
                mHeight,
                mMinHeight,
                mMaxDimension[VERTICAL],
                mHeightOverride,
                mMatchConstraintMinHeight,
                mMatchConstraintDefaultHeight,
                mMatchConstraintPercentHeight,
                mWeight[DIMENSION_VERTICAL]);

        serializeDimensionRatio(ret, "dimensionRatio", mDimensionRatio, mDimensionRatioSide);
        serializeAttribute(ret, "horizontalBias", mHorizontalBiasPercent, DEFAULT_BIAS);
        serializeAttribute(ret, "verticalBias", mVerticalBiasPercent, DEFAULT_BIAS);
        ret.append("}\n");

        return ret;
    }
    ///////////////////////////////////END SERIALIZE///////////////////////////////////////////

    public int horizontalGroup = -1;
    public int verticalGroup = -1;

    // @TODO: add description
    public boolean oppositeDimensionDependsOn(int orientation) {
        int oppositeOrientation = (orientation == HORIZONTAL) ? VERTICAL : HORIZONTAL;
        DimensionBehaviour dimensionBehaviour = mListDimensionBehaviors[orientation];
        DimensionBehaviour oppositeDimensionBehaviour =
                mListDimensionBehaviors[oppositeOrientation];
        return dimensionBehaviour == MATCH_CONSTRAINT
                && oppositeDimensionBehaviour == MATCH_CONSTRAINT;
        //&& mDimensionRatio != 0;
    }

    // @TODO: add description
    public boolean oppositeDimensionsTied() {
        return /* isInHorizontalChain() || isInVerticalChain() || */
                (mListDimensionBehaviors[HORIZONTAL] == MATCH_CONSTRAINT
                        && mListDimensionBehaviors[VERTICAL] == MATCH_CONSTRAINT);
    }

    // @TODO: add description
    public boolean hasDimensionOverride() {
        return mWidthOverride != -1 || mHeightOverride != -1;
    }

    /*-----------------------------------------------------------------------*/
    // Creation
    /*-----------------------------------------------------------------------*/

    /**
     * Default constructor
     */
    public ConstraintWidget() {
        addAnchors();
    }

    public ConstraintWidget(String debugName) {
        addAnchors();
        setDebugName(debugName);
    }

    /**
     * Constructor
     *
     * @param x      x position
     * @param y      y position
     * @param width  width of the layout
     * @param height height of the layout
     */
    public ConstraintWidget(int x, int y, int width, int height) {
        mX = x;
        mY = y;
        mWidth = width;
        mHeight = height;
        addAnchors();
    }

    public ConstraintWidget(String debugName, int x, int y, int width, int height) {
        this(x, y, width, height);
        setDebugName(debugName);
    }

    /**
     * Constructor
     *
     * @param width  width of the layout
     * @param height height of the layout
     */
    public ConstraintWidget(int width, int height) {
        this(0, 0, width, height);
    }

    // @TODO: add description
    public void ensureWidgetRuns() {
        if (mHorizontalRun == null) {
            mHorizontalRun = new HorizontalWidgetRun(this);
        }
        if (mVerticalRun == null) {
            mVerticalRun = new VerticalWidgetRun(this);
        }
    }

    public ConstraintWidget(String debugName, int width, int height) {
        this(width, height);
        setDebugName(debugName);
    }

    /**
     * Reset the solver variables of the anchors
     */
    public void resetSolverVariables(Cache cache) {
        mLeft.resetSolverVariable(cache);
        mTop.resetSolverVariable(cache);
        mRight.resetSolverVariable(cache);
        mBottom.resetSolverVariable(cache);
        mBaseline.resetSolverVariable(cache);
        mCenter.resetSolverVariable(cache);
        mCenterX.resetSolverVariable(cache);
        mCenterY.resetSolverVariable(cache);
    }

    /**
     * Add all the anchors to the mAnchors array
     */
    private void addAnchors() {
        mAnchors.add(mLeft);
        mAnchors.add(mTop);
        mAnchors.add(mRight);
        mAnchors.add(mBottom);
        mAnchors.add(mCenterX);
        mAnchors.add(mCenterY);
        mAnchors.add(mCenter);
        mAnchors.add(mBaseline);
    }

    /**
     * Returns true if the widget is the root widget
     *
     * @return true if root widget, false otherwise
     */
    public boolean isRoot() {
        return mParent == null;
    }

    /**
     * Returns the parent of this widget if there is one
     *
     * @return parent
     */
    public ConstraintWidget getParent() {
        return mParent;
    }

    /**
     * Set the parent of this widget
     *
     * @param widget parent
     */
    public void setParent(ConstraintWidget widget) {
        mParent = widget;
    }

    /**
     * Keep track of wrap_content for width
     */
    public void setWidthWrapContent(boolean widthWrapContent) {
        this.mIsWidthWrapContent = widthWrapContent;
    }

    /**
     * Returns true if width is set to wrap_content
     */
    public boolean isWidthWrapContent() {
        return mIsWidthWrapContent;
    }

    /**
     * Keep track of wrap_content for height
     */
    public void setHeightWrapContent(boolean heightWrapContent) {
        this.mIsHeightWrapContent = heightWrapContent;
    }

    /**
     * Returns true if height is set to wrap_content
     */
    public boolean isHeightWrapContent() {
        return mIsHeightWrapContent;
    }

    /**
     * Set a circular constraint
     *
     * @param target the target widget we will use as the center of the circle
     * @param angle  the angle (from 0 to 360)
     * @param radius the radius used
     */
    public void connectCircularConstraint(ConstraintWidget target, float angle, int radius) {
        immediateConnect(ConstraintAnchor.Type.CENTER, target, ConstraintAnchor.Type.CENTER,
                radius, 0);
        mCircleConstraintAngle = angle;
    }

    /**
     * Returns the type string if set
     *
     * @return type (null if not set)
     */
    public String getType() {
        return mType;
    }

    /**
     * Set the type of the widget (as a String)
     *
     * @param type type of the widget
     */
    public void setType(String type) {
        mType = type;
    }

    /**
     * Set the visibility for this widget
     *
     * @param visibility either VISIBLE, INVISIBLE, or GONE
     */
    public void setVisibility(int visibility) {
        mVisibility = visibility;
    }

    /**
     * Returns the current visibility value for this widget
     *
     * @return the visibility (VISIBLE, INVISIBLE, or GONE)
     */
    public int getVisibility() {
        return mVisibility;
    }

    /**
     * Set if this widget is animated. Currently only affects gone behaviour
     *
     * @param animated if true the widget must be positioned correctly when not visible
     */
    public void setAnimated(boolean animated) {
        mAnimated = animated;
    }

    /**
     * Returns if this widget is animated. Currently only affects gone behaviour
     *
     * @return true if ConstraintWidget is used in Animation
     */
    public boolean isAnimated() {
        return mAnimated;
    }

    /**
     * Returns the name of this widget (used for debug purposes)
     *
     * @return the debug name
     */
    public String getDebugName() {
        return mDebugName;
    }

    /**
     * Set the debug name of this widget
     */
    public void setDebugName(String name) {
        mDebugName = name;
    }

    /**
     * Utility debug function. Sets the names of the anchors in the solver given
     * a widget's name. The given name is used as a prefix, resulting in anchors' names
     * of the form:
     * <p/>
     * <ul>
     * <li>{name}.left</li>
     * <li>{name}.top</li>
     * <li>{name}.right</li>
     * <li>{name}.bottom</li>
     * <li>{name}.baseline</li>
     * </ul>
     *
     * @param system solver used
     * @param name   name of the widget
     */
    public void setDebugSolverName(LinearSystem system, String name) {
        mDebugName = name;
        SolverVariable left = system.createObjectVariable(mLeft);
        SolverVariable top = system.createObjectVariable(mTop);
        SolverVariable right = system.createObjectVariable(mRight);
        SolverVariable bottom = system.createObjectVariable(mBottom);
        left.setName(name + ".left");
        top.setName(name + ".top");
        right.setName(name + ".right");
        bottom.setName(name + ".bottom");
        SolverVariable baseline = system.createObjectVariable(mBaseline);
        baseline.setName(name + ".baseline");
    }

    /**
     * Create all the system variables for this widget
     *
     *
     */
    public void createObjectVariables(LinearSystem system) {
        system.createObjectVariable(mLeft);
        system.createObjectVariable(mTop);
        system.createObjectVariable(mRight);
        system.createObjectVariable(mBottom);
        if (mBaselineDistance > 0) {
            system.createObjectVariable(mBaseline);
        }
    }

    /**
     * Returns a string representation of the ConstraintWidget
     *
     * @return string representation of the widget
     */
    @Override
    public String toString() {
        return (mType != null ? "type: " + mType + " " : "")
                + (mDebugName != null ? "id: " + mDebugName + " " : "")
                + "(" + mX + ", " + mY + ") - (" + mWidth + " x " + mHeight + ")";
    }

    /*-----------------------------------------------------------------------*/
    // Position
    /*-----------------------------------------------------------------------*/
    // The widget position is expressed in two ways:
    // - relative to its direct parent container (getX(), getY())
    // - relative to the root container (getDrawX(), getDrawY())
    // Additionally, getDrawX()/getDrawY() are used when animating the
    // widget position on screen
    /*-----------------------------------------------------------------------*/

    /**
     * Return the x position of the widget, relative to its container
     *
     * @return x position
     */
    public int getX() {
        if (mParent != null && mParent instanceof ConstraintWidgetContainer) {
            return ((ConstraintWidgetContainer) mParent).mPaddingLeft + mX;
        }
        return mX;
    }

    /**
     * Return the y position of the widget, relative to its container
     *
     * @return y position
     */
    public int getY() {
        if (mParent != null && mParent instanceof ConstraintWidgetContainer) {
            return ((ConstraintWidgetContainer) mParent).mPaddingTop + mY;
        }
        return mY;
    }

    /**
     * Return the width of the widget
     *
     * @return width width
     */
    public int getWidth() {
        if (mVisibility == ConstraintWidget.GONE) {
            return 0;
        }
        return mWidth;
    }

    // @TODO: add description
    public int getOptimizerWrapWidth() {
        int w = mWidth;
        if (mListDimensionBehaviors[DIMENSION_HORIZONTAL] == MATCH_CONSTRAINT) {
            if (mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_WRAP) {
                w = Math.max(mMatchConstraintMinWidth, w);
            } else if (mMatchConstraintMinWidth > 0) {
                w = mMatchConstraintMinWidth;
                mWidth = w;
            } else {
                w = 0;
            }
            if (mMatchConstraintMaxWidth > 0 && mMatchConstraintMaxWidth < w) {
                w = mMatchConstraintMaxWidth;
            }
        }
        return w;
    }

    // @TODO: add description
    public int getOptimizerWrapHeight() {
        int h = mHeight;
        if (mListDimensionBehaviors[DIMENSION_VERTICAL] == MATCH_CONSTRAINT) {
            if (mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_WRAP) {
                h = Math.max(mMatchConstraintMinHeight, h);
            } else if (mMatchConstraintMinHeight > 0) {
                h = mMatchConstraintMinHeight;
                mHeight = h;
            } else {
                h = 0;
            }
            if (mMatchConstraintMaxHeight > 0 && mMatchConstraintMaxHeight < h) {
                h = mMatchConstraintMaxHeight;
            }
        }
        return h;
    }

    /**
     * Return the height of the widget
     *
     * @return height height
     */
    public int getHeight() {
        if (mVisibility == ConstraintWidget.GONE) {
            return 0;
        }
        return mHeight;
    }

    /**
     * Get a dimension of the widget in a particular orientation.
     *
     * @return The dimension of the specified orientation.
     */
    public int getLength(int orientation) {
        if (orientation == HORIZONTAL) {
            return getWidth();
        } else if (orientation == VERTICAL) {
            return getHeight();
        } else {
            return 0;
        }
    }

    /**
     * Return the x position of the widget, relative to the root
     * (without animation)
     *
     * @return x position
     */
    protected int getRootX() {
        return mX + mOffsetX;
    }

    /**
     * Return the y position of the widget, relative to the root
     * (without animation)
     */
    protected int getRootY() {
        return mY + mOffsetY;
    }

    /**
     * Return the minimum width of the widget
     *
     * @return minimum width
     */
    public int getMinWidth() {
        return mMinWidth;
    }

    /**
     * Return the minimum height of the widget
     *
     * @return minimum height
     */
    public int getMinHeight() {
        return mMinHeight;
    }

    /**
     * Return the left position of the widget (similar to {@link #getX()})
     *
     * @return left position of the widget
     */
    public int getLeft() {
        return getX();
    }

    /**
     * Return the top position of the widget (similar to {@link #getY()})
     *
     * @return top position of the widget
     */
    public int getTop() {
        return getY();
    }

    /**
     * Return the right position of the widget
     *
     * @return right position of the widget
     */
    public int getRight() {
        return getX() + mWidth;
    }

    /**
     * Return the bottom position of the widget
     *
     * @return bottom position of the widget
     */
    public int getBottom() {
        return getY() + mHeight;
    }

    /**
     * Returns all the horizontal margin of the widget.
     */
    public int getHorizontalMargin() {
        int margin = 0;
        if (mLeft != null) {
            margin += mLeft.mMargin;
        }
        if (mRight != null) {
            margin += mRight.mMargin;
        }
        return margin;
    }

    /**
     * Returns all the vertical margin of the widget
     */
    public int getVerticalMargin() {
        int margin = 0;
        if (mLeft != null) {
            margin += mTop.mMargin;
        }
        if (mRight != null) {
            margin += mBottom.mMargin;
        }
        return margin;
    }

    /**
     * Return the horizontal percentage bias that is used when two opposite connections
     * exist of the same strength.
     *
     * @return horizontal percentage bias
     */
    public float getHorizontalBiasPercent() {
        return mHorizontalBiasPercent;
    }

    /**
     * Return the vertical percentage bias that is used when two opposite connections
     * exist of the same strength.
     *
     * @return vertical percentage bias
     */
    public float getVerticalBiasPercent() {
        return mVerticalBiasPercent;
    }

    /**
     * Return the percentage bias that is used when two opposite connections exist of the same
     * strength in a particular orientation.
     *
     * @param orientation Orientation {@link #HORIZONTAL}/{@link #VERTICAL}.
     * @return Respective percentage bias.
     */
    public float getBiasPercent(int orientation) {
        if (orientation == HORIZONTAL) {
            return mHorizontalBiasPercent;
        } else if (orientation == VERTICAL) {
            return mVerticalBiasPercent;
        } else {
            return UNKNOWN;
        }
    }

    /**
     * Return true if this widget has a baseline
     *
     * @return true if the widget has a baseline, false otherwise
     */
    public boolean hasBaseline() {
        return mHasBaseline;
    }

    /**
     * Return the baseline distance relative to the top of the widget
     *
     * @return baseline
     */
    public int getBaselineDistance() {
        return mBaselineDistance;
    }

    /**
     * Return the companion widget. Typically, this would be the real
     * widget we represent with this instance of ConstraintWidget.
     *
     * @return the companion widget, if set.
     */
    public Object getCompanionWidget() {
        return mCompanionWidget;
    }

    /**
     * Return the array of anchors of this widget
     *
     * @return array of anchors
     */
    public ArrayList<ConstraintAnchor> getAnchors() {
        return mAnchors;
    }

    /**
     * Set the x position of the widget, relative to its container
     *
     * @param x x position
     */
    public void setX(int x) {
        mX = x;
    }

    /**
     * Set the y position of the widget, relative to its container
     *
     * @param y y position
     */
    public void setY(int y) {
        mY = y;
    }

    /**
     * Set both the origin in (x, y) of the widget, relative to its container
     *
     * @param x x position
     * @param y y position
     */
    public void setOrigin(int x, int y) {
        mX = x;
        mY = y;
    }

    /**
     * Set the offset of this widget relative to the root widget
     *
     * @param x horizontal offset
     * @param y vertical offset
     */
    public void setOffset(int x, int y) {
        mOffsetX = x;
        mOffsetY = y;
    }

    /**
     * Set the margin to be used when connected to a widget with a visibility of GONE
     *
     * @param type       the anchor to set the margin on
     * @param goneMargin the margin value to use
     */
    public void setGoneMargin(ConstraintAnchor.Type type, int goneMargin) {
        switch (type) {
            case LEFT: {
                mLeft.mGoneMargin = goneMargin;
            }
            break;
            case TOP: {
                mTop.mGoneMargin = goneMargin;
            }
            break;
            case RIGHT: {
                mRight.mGoneMargin = goneMargin;
            }
            break;
            case BOTTOM: {
                mBottom.mGoneMargin = goneMargin;
            }
            break;
            case BASELINE: {
                mBaseline.mGoneMargin = goneMargin;
            }
            break;
            case CENTER:
            case CENTER_X:
            case CENTER_Y:
            case NONE:
                break;
        }
    }

    /**
     * Set the width of the widget
     *
     * @param w width
     */
    public void setWidth(int w) {
        mWidth = w;
        if (mWidth < mMinWidth) {
            mWidth = mMinWidth;
        }
    }

    /**
     * Set the height of the widget
     *
     * @param h height
     */
    public void setHeight(int h) {
        mHeight = h;
        if (mHeight < mMinHeight) {
            mHeight = mMinHeight;
        }
    }

    /**
     * Set the dimension of a widget in a particular orientation.
     *
     * @param length      Size of the dimension.
     * @param orientation HORIZONTAL or VERTICAL
     */
    public void setLength(int length, int orientation) {
        if (orientation == HORIZONTAL) {
            setWidth(length);
        } else if (orientation == VERTICAL) {
            setHeight(length);
        }
    }

    /**
     * Set the horizontal style when MATCH_CONSTRAINT is set
     *
     * @param horizontalMatchStyle MATCH_CONSTRAINT_SPREAD or MATCH_CONSTRAINT_WRAP
     * @param min                  minimum value
     * @param max                  maximum value
     * @param percent              Percent width
     */
    public void setHorizontalMatchStyle(int horizontalMatchStyle, int min, int max, float percent) {
        mMatchConstraintDefaultWidth = horizontalMatchStyle;
        mMatchConstraintMinWidth = min;
        mMatchConstraintMaxWidth = (max == Integer.MAX_VALUE) ? 0 : max;
        mMatchConstraintPercentWidth = percent;
        if (percent > 0 && percent < 1 && mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD) {
            mMatchConstraintDefaultWidth = MATCH_CONSTRAINT_PERCENT;
        }
    }

    /**
     * Set the vertical style when MATCH_CONSTRAINT is set
     *
     * @param verticalMatchStyle MATCH_CONSTRAINT_SPREAD or MATCH_CONSTRAINT_WRAP
     * @param min                minimum value
     * @param max                maximum value
     * @param percent            Percent height
     */
    public void setVerticalMatchStyle(int verticalMatchStyle, int min, int max, float percent) {
        mMatchConstraintDefaultHeight = verticalMatchStyle;
        mMatchConstraintMinHeight = min;
        mMatchConstraintMaxHeight = (max == Integer.MAX_VALUE) ? 0 : max;
        mMatchConstraintPercentHeight = percent;
        if (percent > 0 && percent < 1
                && mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD) {
            mMatchConstraintDefaultHeight = MATCH_CONSTRAINT_PERCENT;
        }
    }

    /**
     * Set the ratio of the widget
     *
     * @param ratio given string of format [H|V],[float|x:y] or [float|x:y]
     */
    public void setDimensionRatio(String ratio) {
        if (ratio == null || ratio.length() == 0) {
            mDimensionRatio = 0;
            return;
        }
        int dimensionRatioSide = UNKNOWN;
        float dimensionRatio = 0;
        int len = ratio.length();
        int commaIndex = ratio.indexOf(',');
        if (commaIndex > 0 && commaIndex < len - 1) {
            String dimension = ratio.substring(0, commaIndex);
            if (dimension.equalsIgnoreCase("W")) {
                dimensionRatioSide = HORIZONTAL;
            } else if (dimension.equalsIgnoreCase("H")) {
                dimensionRatioSide = VERTICAL;
            }
            commaIndex++;
        } else {
            commaIndex = 0;
        }
        int colonIndex = ratio.indexOf(':');

        if (colonIndex >= 0 && colonIndex < len - 1) {
            String nominator = ratio.substring(commaIndex, colonIndex);
            String denominator = ratio.substring(colonIndex + 1);
            if (nominator.length() > 0 && denominator.length() > 0) {
                try {
                    float nominatorValue = Float.parseFloat(nominator);
                    float denominatorValue = Float.parseFloat(denominator);
                    if (nominatorValue > 0 && denominatorValue > 0) {
                        if (dimensionRatioSide == VERTICAL) {
                            dimensionRatio = Math.abs(denominatorValue / nominatorValue);
                        } else {
                            dimensionRatio = Math.abs(nominatorValue / denominatorValue);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        } else {
            String r = ratio.substring(commaIndex);
            if (r.length() > 0) {
                try {
                    dimensionRatio = Float.parseFloat(r);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        if (dimensionRatio > 0) {
            mDimensionRatio = dimensionRatio;
            mDimensionRatioSide = dimensionRatioSide;
        }
    }

    /**
     * Set the ratio of the widget
     * The ratio will be applied if at least one of the dimension
     * (width or height) is set to a behaviour
     * of DimensionBehaviour.MATCH_CONSTRAINT
     * -- the dimension's value will be set to the other dimension * ratio.
     *
     * @param ratio              A float value that describes W/H or H/W depending
     *                           on the provided dimensionRatioSide
     * @param dimensionRatioSide The side the ratio should be calculated on,
     *                           HORIZONTAL, VERTICAL, or UNKNOWN
     */
    public void setDimensionRatio(float ratio, int dimensionRatioSide) {
        mDimensionRatio = ratio;
        mDimensionRatioSide = dimensionRatioSide;
    }

    /**
     * Return the current ratio of this widget
     *
     * @return the dimension ratio (HORIZONTAL, VERTICAL, or UNKNOWN)
     */
    public float getDimensionRatio() {
        return mDimensionRatio;
    }

    /**
     * Return the current side on which ratio will be applied
     *
     * @return HORIZONTAL, VERTICAL, or UNKNOWN
     */
    public int getDimensionRatioSide() {
        return mDimensionRatioSide;
    }

    /**
     * Set the horizontal bias percent to apply when we have two opposite constraints of
     * equal strength
     *
     * @param horizontalBiasPercent the percentage used
     */
    public void setHorizontalBiasPercent(float horizontalBiasPercent) {
        mHorizontalBiasPercent = horizontalBiasPercent;
    }

    /**
     * Set the vertical bias percent to apply when we have two opposite constraints of
     * equal strength
     *
     * @param verticalBiasPercent the percentage used
     */
    public void setVerticalBiasPercent(float verticalBiasPercent) {
        mVerticalBiasPercent = verticalBiasPercent;
    }

    /**
     * Set the minimum width of the widget
     *
     * @param w minimum width
     */
    public void setMinWidth(int w) {
        if (w < 0) {
            mMinWidth = 0;
        } else {
            mMinWidth = w;
        }
    }

    /**
     * Set the minimum height of the widget
     *
     * @param h minimum height
     */
    public void setMinHeight(int h) {
        if (h < 0) {
            mMinHeight = 0;
        } else {
            mMinHeight = h;
        }
    }

    /**
     * Set both width and height of the widget
     *
     * @param w width
     * @param h height
     */
    public void setDimension(int w, int h) {
        mWidth = w;
        if (mWidth < mMinWidth) {
            mWidth = mMinWidth;
        }
        mHeight = h;
        if (mHeight < mMinHeight) {
            mHeight = mMinHeight;
        }
    }

    /**
     * Set the position+dimension of the widget given left/top/right/bottom
     *
     * @param left   left side position of the widget
     * @param top    top side position of the widget
     * @param right  right side position of the widget
     * @param bottom bottom side position of the widget
     */
    public void setFrame(int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;

        mX = left;
        mY = top;

        if (mVisibility == ConstraintWidget.GONE) {
            mWidth = 0;
            mHeight = 0;
            return;
        }

        // correct dimensional instability caused by rounding errors
        if (mListDimensionBehaviors[DIMENSION_HORIZONTAL]
                == DimensionBehaviour.FIXED && w < mWidth) {
            w = mWidth;
        }
        if (mListDimensionBehaviors[DIMENSION_VERTICAL]
                == DimensionBehaviour.FIXED && h < mHeight) {
            h = mHeight;
        }

        mWidth = w;
        mHeight = h;

        if (mHeight < mMinHeight) {
            mHeight = mMinHeight;
        }
        if (mWidth < mMinWidth) {
            mWidth = mMinWidth;
        }
        if (mMatchConstraintMaxWidth > 0
                && mListDimensionBehaviors[HORIZONTAL] == MATCH_CONSTRAINT) {
            mWidth = Math.min(mWidth, mMatchConstraintMaxWidth);
        }
        if (mMatchConstraintMaxHeight > 0
                && mListDimensionBehaviors[VERTICAL] == MATCH_CONSTRAINT) {
            mHeight = Math.min(mHeight, mMatchConstraintMaxHeight);
        }
        if (w != mWidth) {
            mWidthOverride = mWidth;
        }
        if (h != mHeight) {
            mHeightOverride = mHeight;
        }

        if (LinearSystem.FULL_DEBUG) {
            System.out.println("update from solver " + mDebugName
                    + " " + mX + ":" + mY + " - " + mWidth + " x " + mHeight);
        }
    }

    /**
     * Set the position+dimension of the widget based on starting/ending positions on one dimension.
     *
     * @param start       Left/Top side position of the widget.
     * @param end         Right/Bottom side position of the widget.
     * @param orientation Orientation being set (HORIZONTAL/VERTICAL).
     */
    public void setFrame(int start, int end, int orientation) {
        if (orientation == HORIZONTAL) {
            setHorizontalDimension(start, end);
        } else if (orientation == VERTICAL) {
            setVerticalDimension(start, end);
        }
    }

    /**
     * Set the positions for the horizontal dimension only
     *
     * @param left  left side position of the widget
     * @param right right side position of the widget
     */
    public void setHorizontalDimension(int left, int right) {
        mX = left;
        mWidth = right - left;
        if (mWidth < mMinWidth) {
            mWidth = mMinWidth;
        }
    }

    /**
     * Set the positions for the vertical dimension only
     *
     * @param top    top side position of the widget
     * @param bottom bottom side position of the widget
     */
    public void setVerticalDimension(int top, int bottom) {
        mY = top;
        mHeight = bottom - top;
        if (mHeight < mMinHeight) {
            mHeight = mMinHeight;
        }
    }

    /**
     * Get the left/top position of the widget relative to
     * the outer side of the container (right/bottom).
     *
     * @param orientation Orientation by which to find the relative positioning of the widget.
     * @return The relative position of the widget.
     */
    int getRelativePositioning(int orientation) {
        if (orientation == HORIZONTAL) {
            return mRelX;
        } else if (orientation == VERTICAL) {
            return mRelY;
        } else {
            return 0;
        }
    }

    /**
     * Set the left/top position of the widget relative to
     * the outer side of the container (right/bottom).
     *
     * @param offset      Offset of the relative position.
     * @param orientation Orientation of the offset being set.
     */
    void setRelativePositioning(int offset, int orientation) {
        if (orientation == HORIZONTAL) {
            mRelX = offset;
        } else if (orientation == VERTICAL) {
            mRelY = offset;
        }
    }

    /**
     * Set the baseline distance relative to the top of the widget
     *
     * @param baseline the distance of the baseline relative to the widget's top
     */
    public void setBaselineDistance(int baseline) {
        mBaselineDistance = baseline;
        mHasBaseline = baseline > 0;
    }

    /**
     * Set the companion widget. Typically, this would be the real widget we
     * represent with this instance of ConstraintWidget.
     */
    public void setCompanionWidget(Object companion) {
        mCompanionWidget = companion;
    }

    /**
     * Set the skip value for this widget. This can be used when a widget is in a container,
     * so that container can position the widget as if it was positioned further in the list
     * of widgets. For example, with Table, this is used to skip empty cells
     * (the widget after an empty cell will have a skip value of one)
     */
    public void setContainerItemSkip(int skip) {
        if (skip >= 0) {
            mContainerItemSkip = skip;
        } else {
            mContainerItemSkip = 0;
        }
    }

    /**
     * Accessor for the skip value
     *
     * @return skip value
     */
    public int getContainerItemSkip() {
        return mContainerItemSkip;
    }

    /**
     * Set the horizontal weight (only used in chains)
     *
     * @param horizontalWeight Floating point value weight
     */
    public void setHorizontalWeight(float horizontalWeight) {
        mWeight[DIMENSION_HORIZONTAL] = horizontalWeight;
    }

    /**
     * Set the vertical weight (only used in chains)
     *
     * @param verticalWeight Floating point value weight
     */
    public void setVerticalWeight(float verticalWeight) {
        mWeight[DIMENSION_VERTICAL] = verticalWeight;
    }

    /**
     * Set the chain starting from this widget to be packed.
     * The horizontal bias will control how elements of the chain are positioned.
     *
     * @param horizontalChainStyle (CHAIN_SPREAD, CHAIN_SPREAD_INSIDE, CHAIN_PACKED)
     */
    public void setHorizontalChainStyle(int horizontalChainStyle) {
        mHorizontalChainStyle = horizontalChainStyle;
    }

    /**
     * get the chain starting from this widget to be packed.
     * The horizontal bias will control how elements of the chain are positioned.
     *
     * @return Horizontal Chain Style
     */
    public int getHorizontalChainStyle() {
        return mHorizontalChainStyle;
    }

    /**
     * Set the chain starting from this widget to be packed.
     * The vertical bias will control how elements of the chain are positioned.
     *
     * @param verticalChainStyle (CHAIN_SPREAD, CHAIN_SPREAD_INSIDE, CHAIN_PACKED)
     */
    public void setVerticalChainStyle(int verticalChainStyle) {
        mVerticalChainStyle = verticalChainStyle;
    }

    /**
     * Set the chain starting from this widget to be packed.
     * The vertical bias will control how elements of the chain are positioned.
     */
    public int getVerticalChainStyle() {
        return mVerticalChainStyle;
    }

    /**
     * Returns true if this widget should be used in a barrier
     */
    public boolean allowedInBarrier() {
        return mVisibility != GONE;
    }

    /*-----------------------------------------------------------------------*/
    // Connections
    /*-----------------------------------------------------------------------*/

    /**
     * Immediate connection to an anchor without any checks.
     *
     * @param startType  The type of anchor on this widget
     * @param target     The target widget
     * @param endType    The type of anchor on the target widget
     * @param margin     How much margin we want to keep as
     *                   a minimum distance between the two anchors
     * @param goneMargin How much margin we want to keep if the target is set to {@code View.GONE}
     */
    public void immediateConnect(ConstraintAnchor.Type startType, ConstraintWidget target,
            ConstraintAnchor.Type endType, int margin, int goneMargin) {
        ConstraintAnchor startAnchor = getAnchor(startType);
        ConstraintAnchor endAnchor = target.getAnchor(endType);
        startAnchor.connect(endAnchor, margin, goneMargin, true);
    }

    /**
     * Connect the given anchors together (the from anchor should be owned by this widget)
     *
     * @param from   the anchor we are connecting from (of this widget)
     * @param to     the anchor we are connecting to
     * @param margin how much margin we want to have
     */
    public void connect(ConstraintAnchor from, ConstraintAnchor to, int margin) {
        if (from.getOwner() == this) {
            connect(from.getType(), to.getOwner(), to.getType(), margin);
        }
    }

    /**
     * Connect a given anchor of this widget to another anchor of a target widget
     *
     * @param constraintFrom which anchor of this widget to connect from
     * @param target         the target widget
     * @param constraintTo   the target anchor on the target widget
     */
    public void connect(ConstraintAnchor.Type constraintFrom,
            ConstraintWidget target,
            ConstraintAnchor.Type constraintTo) {
        if (DEBUG) {
            System.out.println(this.getDebugName() + " connect "
                    + constraintFrom + " to " + target + " " + constraintTo);
        }
        connect(constraintFrom, target, constraintTo, 0);
    }

    /**
     * Connect a given anchor of this widget to another anchor of a target widget
     *
     * @param constraintFrom which anchor of this widget to connect from
     * @param target         the target widget
     * @param constraintTo   the target anchor on the target widget
     * @param margin         how much margin we want to keep as
     *                       a minimum distance between the two anchors
     */
    public void connect(ConstraintAnchor.Type constraintFrom,
            ConstraintWidget target,
            ConstraintAnchor.Type constraintTo, int margin) {
        if (constraintFrom == ConstraintAnchor.Type.CENTER) {
            // If we have center, we connect instead to the corresponding
            // left/right or top/bottom pairs
            if (constraintTo == ConstraintAnchor.Type.CENTER) {
                ConstraintAnchor left = getAnchor(ConstraintAnchor.Type.LEFT);
                ConstraintAnchor right = getAnchor(ConstraintAnchor.Type.RIGHT);
                ConstraintAnchor top = getAnchor(ConstraintAnchor.Type.TOP);
                ConstraintAnchor bottom = getAnchor(ConstraintAnchor.Type.BOTTOM);
                boolean centerX = false;
                boolean centerY = false;
                if ((left != null && left.isConnected())
                        || (right != null && right.isConnected())) {
                    // don't apply center here
                } else {
                    connect(ConstraintAnchor.Type.LEFT, target,
                            ConstraintAnchor.Type.LEFT, 0);
                    connect(ConstraintAnchor.Type.RIGHT, target,
                            ConstraintAnchor.Type.RIGHT, 0);
                    centerX = true;
                }
                if ((top != null && top.isConnected())
                        || (bottom != null && bottom.isConnected())) {
                    // don't apply center here
                } else {
                    connect(ConstraintAnchor.Type.TOP, target,
                            ConstraintAnchor.Type.TOP, 0);
                    connect(ConstraintAnchor.Type.BOTTOM, target,
                            ConstraintAnchor.Type.BOTTOM, 0);
                    centerY = true;
                }
                if (centerX && centerY) {
                    ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
                    center.connect(target.getAnchor(ConstraintAnchor.Type.CENTER), 0);
                } else if (centerX) {
                    ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER_X);
                    center.connect(target.getAnchor(ConstraintAnchor.Type.CENTER_X), 0);
                } else if (centerY) {
                    ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER_Y);
                    center.connect(target.getAnchor(ConstraintAnchor.Type.CENTER_Y), 0);
                }
            } else if ((constraintTo == ConstraintAnchor.Type.LEFT)
                    || (constraintTo == ConstraintAnchor.Type.RIGHT)) {
                connect(ConstraintAnchor.Type.LEFT, target,
                        constraintTo, 0);
                connect(ConstraintAnchor.Type.RIGHT, target,
                        constraintTo, 0);
                ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
                center.connect(target.getAnchor(constraintTo), 0);
            } else if ((constraintTo == ConstraintAnchor.Type.TOP)
                    || (constraintTo == ConstraintAnchor.Type.BOTTOM)) {
                connect(ConstraintAnchor.Type.TOP, target,
                        constraintTo, 0);
                connect(ConstraintAnchor.Type.BOTTOM, target,
                        constraintTo, 0);
                ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
                center.connect(target.getAnchor(constraintTo), 0);
            }
        } else if (constraintFrom == ConstraintAnchor.Type.CENTER_X
                && (constraintTo == ConstraintAnchor.Type.LEFT
                || constraintTo == ConstraintAnchor.Type.RIGHT)) {
            ConstraintAnchor left = getAnchor(ConstraintAnchor.Type.LEFT);
            ConstraintAnchor targetAnchor = target.getAnchor(constraintTo);
            ConstraintAnchor right = getAnchor(ConstraintAnchor.Type.RIGHT);
            left.connect(targetAnchor, 0);
            right.connect(targetAnchor, 0);
            ConstraintAnchor centerX = getAnchor(ConstraintAnchor.Type.CENTER_X);
            centerX.connect(targetAnchor, 0);
        } else if (constraintFrom == ConstraintAnchor.Type.CENTER_Y
                && (constraintTo == ConstraintAnchor.Type.TOP
                || constraintTo == ConstraintAnchor.Type.BOTTOM)) {
            ConstraintAnchor targetAnchor = target.getAnchor(constraintTo);
            ConstraintAnchor top = getAnchor(ConstraintAnchor.Type.TOP);
            top.connect(targetAnchor, 0);
            ConstraintAnchor bottom = getAnchor(ConstraintAnchor.Type.BOTTOM);
            bottom.connect(targetAnchor, 0);
            ConstraintAnchor centerY = getAnchor(ConstraintAnchor.Type.CENTER_Y);
            centerY.connect(targetAnchor, 0);
        } else if (constraintFrom == ConstraintAnchor.Type.CENTER_X
                && constraintTo == ConstraintAnchor.Type.CENTER_X) {
            // Center X connection will connect left & right
            ConstraintAnchor left = getAnchor(ConstraintAnchor.Type.LEFT);
            ConstraintAnchor leftTarget = target.getAnchor(ConstraintAnchor.Type.LEFT);
            left.connect(leftTarget, 0);
            ConstraintAnchor right = getAnchor(ConstraintAnchor.Type.RIGHT);
            ConstraintAnchor rightTarget = target.getAnchor(ConstraintAnchor.Type.RIGHT);
            right.connect(rightTarget, 0);
            ConstraintAnchor centerX = getAnchor(ConstraintAnchor.Type.CENTER_X);
            centerX.connect(target.getAnchor(constraintTo), 0);
        } else if (constraintFrom == ConstraintAnchor.Type.CENTER_Y
                && constraintTo == ConstraintAnchor.Type.CENTER_Y) {
            // Center Y connection will connect top & bottom.
            ConstraintAnchor top = getAnchor(ConstraintAnchor.Type.TOP);
            ConstraintAnchor topTarget = target.getAnchor(ConstraintAnchor.Type.TOP);
            top.connect(topTarget, 0);
            ConstraintAnchor bottom = getAnchor(ConstraintAnchor.Type.BOTTOM);
            ConstraintAnchor bottomTarget = target.getAnchor(ConstraintAnchor.Type.BOTTOM);
            bottom.connect(bottomTarget, 0);
            ConstraintAnchor centerY = getAnchor(ConstraintAnchor.Type.CENTER_Y);
            centerY.connect(target.getAnchor(constraintTo), 0);
        } else {
            ConstraintAnchor fromAnchor = getAnchor(constraintFrom);
            ConstraintAnchor toAnchor = target.getAnchor(constraintTo);
            if (fromAnchor.isValidConnection(toAnchor)) {
                // make sure that the baseline takes precedence over top/bottom
                // and reversely, reset the baseline if we are connecting top/bottom
                if (constraintFrom == ConstraintAnchor.Type.BASELINE) {
                    ConstraintAnchor top = getAnchor(ConstraintAnchor.Type.TOP);
                    ConstraintAnchor bottom = getAnchor(ConstraintAnchor.Type.BOTTOM);
                    if (top != null) {
                        top.reset();
                    }
                    if (bottom != null) {
                        bottom.reset();
                    }
                } else if ((constraintFrom == ConstraintAnchor.Type.TOP)
                        || (constraintFrom == ConstraintAnchor.Type.BOTTOM)) {
                    ConstraintAnchor baseline = getAnchor(ConstraintAnchor.Type.BASELINE);
                    if (baseline != null) {
                        baseline.reset();
                    }
                    ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
                    if (center.getTarget() != toAnchor) {
                        center.reset();
                    }
                    ConstraintAnchor opposite = getAnchor(constraintFrom).getOpposite();
                    ConstraintAnchor centerY = getAnchor(ConstraintAnchor.Type.CENTER_Y);
                    if (centerY.isConnected()) {
                        opposite.reset();
                        centerY.reset();
                    } else {
                        if (AUTOTAG_CENTER) {
                            // let's see if we need to mark center_y as connected
                            if (opposite.isConnected() && opposite.getTarget().getOwner()
                                    == toAnchor.getOwner()) {
                                ConstraintAnchor targetCenterY = toAnchor.getOwner().getAnchor(
                                        ConstraintAnchor.Type.CENTER_Y);
                                centerY.connect(targetCenterY, 0);
                            }
                        }
                    }
                } else if ((constraintFrom == ConstraintAnchor.Type.LEFT)
                        || (constraintFrom == ConstraintAnchor.Type.RIGHT)) {
                    ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
                    if (center.getTarget() != toAnchor) {
                        center.reset();
                    }
                    ConstraintAnchor opposite = getAnchor(constraintFrom).getOpposite();
                    ConstraintAnchor centerX = getAnchor(ConstraintAnchor.Type.CENTER_X);
                    if (centerX.isConnected()) {
                        opposite.reset();
                        centerX.reset();
                    } else {
                        if (AUTOTAG_CENTER) {
                            // let's see if we need to mark center_x as connected
                            if (opposite.isConnected() && opposite.getTarget().getOwner()
                                    == toAnchor.getOwner()) {
                                ConstraintAnchor targetCenterX = toAnchor.getOwner().getAnchor(
                                        ConstraintAnchor.Type.CENTER_X);
                                centerX.connect(targetCenterX, 0);
                            }
                        }
                    }

                }
                fromAnchor.connect(toAnchor, margin);
            }
        }
    }

    /**
     * Reset all the constraints set on this widget
     */
    public void resetAllConstraints() {
        resetAnchors();
        setVerticalBiasPercent(DEFAULT_BIAS);
        setHorizontalBiasPercent(DEFAULT_BIAS);
    }

    /**
     * Reset the given anchor
     *
     * @param anchor the anchor we want to reset
     */
    public void resetAnchor(ConstraintAnchor anchor) {
        if (getParent() != null) {
            if (getParent() instanceof ConstraintWidgetContainer) {
                ConstraintWidgetContainer parent = (ConstraintWidgetContainer) getParent();
                if (parent.handlesInternalConstraints()) {
                    return;
                }
            }
        }
        ConstraintAnchor left = getAnchor(ConstraintAnchor.Type.LEFT);
        ConstraintAnchor right = getAnchor(ConstraintAnchor.Type.RIGHT);
        ConstraintAnchor top = getAnchor(ConstraintAnchor.Type.TOP);
        ConstraintAnchor bottom = getAnchor(ConstraintAnchor.Type.BOTTOM);
        ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
        ConstraintAnchor centerX = getAnchor(ConstraintAnchor.Type.CENTER_X);
        ConstraintAnchor centerY = getAnchor(ConstraintAnchor.Type.CENTER_Y);

        if (anchor == center) {
            if (left.isConnected() && right.isConnected()
                    && left.getTarget() == right.getTarget()) {
                left.reset();
                right.reset();
            }
            if (top.isConnected() && bottom.isConnected()
                    && top.getTarget() == bottom.getTarget()) {
                top.reset();
                bottom.reset();
            }
            mHorizontalBiasPercent = 0.5f;
            mVerticalBiasPercent = 0.5f;
        } else if (anchor == centerX) {
            if (left.isConnected() && right.isConnected()
                    && left.getTarget().getOwner() == right.getTarget().getOwner()) {
                left.reset();
                right.reset();
            }
            mHorizontalBiasPercent = 0.5f;
        } else if (anchor == centerY) {
            if (top.isConnected() && bottom.isConnected()
                    && top.getTarget().getOwner() == bottom.getTarget().getOwner()) {
                top.reset();
                bottom.reset();
            }
            mVerticalBiasPercent = 0.5f;
        } else if (anchor == left || anchor == right) {
            if (left.isConnected() && left.getTarget() == right.getTarget()) {
                center.reset();
            }
        } else if (anchor == top || anchor == bottom) {
            if (top.isConnected() && top.getTarget() == bottom.getTarget()) {
                center.reset();
            }
        }
        anchor.reset();
    }

    /**
     * Reset all connections
     */
    public void resetAnchors() {
        ConstraintWidget parent = getParent();
        if (parent != null && parent instanceof ConstraintWidgetContainer) {
            ConstraintWidgetContainer parentContainer = (ConstraintWidgetContainer) getParent();
            if (parentContainer.handlesInternalConstraints()) {
                return;
            }
        }
        for (int i = 0, mAnchorsSize = mAnchors.size(); i < mAnchorsSize; i++) {
            final ConstraintAnchor anchor = mAnchors.get(i);
            anchor.reset();
        }
    }

    /**
     * Given a type of anchor, returns the corresponding anchor.
     *
     * @param anchorType type of the anchor (LEFT, TOP, RIGHT, BOTTOM, BASELINE, CENTER_X, CENTER_Y)
     * @return the matching anchor
     */
    public ConstraintAnchor getAnchor(ConstraintAnchor.Type anchorType) {
        switch (anchorType) {
            case LEFT: {
                return mLeft;
            }
            case TOP: {
                return mTop;
            }
            case RIGHT: {
                return mRight;
            }
            case BOTTOM: {
                return mBottom;
            }
            case BASELINE: {
                return mBaseline;
            }
            case CENTER_X: {
                return mCenterX;
            }
            case CENTER_Y: {
                return mCenterY;
            }
            case CENTER: {
                return mCenter;
            }
            case NONE:
                return null;
        }
        throw new AssertionError(anchorType.name());
    }

    /**
     * Accessor for the horizontal dimension behaviour
     *
     * @return dimension behaviour
     */
    public DimensionBehaviour getHorizontalDimensionBehaviour() {
        return mListDimensionBehaviors[DIMENSION_HORIZONTAL];
    }

    /**
     * Accessor for the vertical dimension behaviour
     *
     * @return dimension behaviour
     */
    public DimensionBehaviour getVerticalDimensionBehaviour() {
        return mListDimensionBehaviors[DIMENSION_VERTICAL];
    }

    /**
     * Get the widget's {@link DimensionBehaviour} in an specific orientation.
     *
     * @return The {@link DimensionBehaviour} of the widget.
     */
    public DimensionBehaviour getDimensionBehaviour(int orientation) {
        if (orientation == HORIZONTAL) {
            return getHorizontalDimensionBehaviour();
        } else if (orientation == VERTICAL) {
            return getVerticalDimensionBehaviour();
        } else {
            return null;
        }
    }

    /**
     * Set the widget's behaviour for the horizontal dimension
     *
     * @param behaviour the horizontal dimension's behaviour
     */
    public void setHorizontalDimensionBehaviour(DimensionBehaviour behaviour) {
        mListDimensionBehaviors[DIMENSION_HORIZONTAL] = behaviour;
    }

    /**
     * Set the widget's behaviour for the vertical dimension
     *
     * @param behaviour the vertical dimension's behaviour
     */
    public void setVerticalDimensionBehaviour(DimensionBehaviour behaviour) {
        mListDimensionBehaviors[DIMENSION_VERTICAL] = behaviour;
    }

    /**
     * Test if you are in a Horizontal chain
     *
     * @return true if in a horizontal chain
     */
    public boolean isInHorizontalChain() {
        if ((mLeft.mTarget != null && mLeft.mTarget.mTarget == mLeft)
                || (mRight.mTarget != null && mRight.mTarget.mTarget == mRight)) {
            return true;
        }
        return false;
    }

    /**
     * Return the previous chain member if one exists
     *
     * @param orientation HORIZONTAL or VERTICAL
     * @return the previous chain member or null if we are the first chain element
     */
    public ConstraintWidget getPreviousChainMember(int orientation) {
        if (orientation == HORIZONTAL) {
            if (mLeft.mTarget != null && mLeft.mTarget.mTarget == mLeft) {
                return mLeft.mTarget.mOwner;
            }
        } else if (orientation == VERTICAL) {
            if (mTop.mTarget != null && mTop.mTarget.mTarget == mTop) {
                return mTop.mTarget.mOwner;
            }
        }
        return null;
    }

    /**
     * Return the next chain member if one exists
     *
     * @param orientation HORIZONTAL or VERTICAL
     * @return the next chain member or null if we are the last chain element
     */
    public ConstraintWidget getNextChainMember(int orientation) {
        if (orientation == HORIZONTAL) {
            if (mRight.mTarget != null && mRight.mTarget.mTarget == mRight) {
                return mRight.mTarget.mOwner;
            }
        } else if (orientation == VERTICAL) {
            if (mBottom.mTarget != null && mBottom.mTarget.mTarget == mBottom) {
                return mBottom.mTarget.mOwner;
            }
        }
        return null;
    }

    /**
     * if in a horizontal chain return the left most widget in the chain.
     *
     * @return left most widget in chain or null
     */
    public ConstraintWidget getHorizontalChainControlWidget() {
        ConstraintWidget found = null;
        if (isInHorizontalChain()) {
            ConstraintWidget tmp = this;

            while (found == null && tmp != null) {
                ConstraintAnchor anchor = tmp.getAnchor(ConstraintAnchor.Type.LEFT);
                ConstraintAnchor targetOwner = (anchor == null) ? null : anchor.getTarget();
                ConstraintWidget target = (targetOwner == null) ? null : targetOwner.getOwner();
                if (target == getParent()) {
                    found = tmp;
                    break;
                }
                ConstraintAnchor targetAnchor = (target == null)
                        ? null : target.getAnchor(ConstraintAnchor.Type.RIGHT).getTarget();
                if (targetAnchor != null && targetAnchor.getOwner() != tmp) {
                    found = tmp;
                } else {
                    tmp = target;
                }
            }
        }
        return found;
    }


    /**
     * Test if you are in a vertical chain
     *
     * @return true if in a vertical chain
     */
    public boolean isInVerticalChain() {
        if ((mTop.mTarget != null && mTop.mTarget.mTarget == mTop)
                || (mBottom.mTarget != null && mBottom.mTarget.mTarget == mBottom)) {
            return true;
        }
        return false;
    }

    /**
     * if in a vertical chain return the top most widget in the chain.
     *
     * @return top most widget in chain or null
     */
    public ConstraintWidget getVerticalChainControlWidget() {
        ConstraintWidget found = null;
        if (isInVerticalChain()) {
            ConstraintWidget tmp = this;
            while (found == null && tmp != null) {
                ConstraintAnchor anchor = tmp.getAnchor(ConstraintAnchor.Type.TOP);
                ConstraintAnchor targetOwner = (anchor == null) ? null : anchor.getTarget();
                ConstraintWidget target = (targetOwner == null) ? null : targetOwner.getOwner();
                if (target == getParent()) {
                    found = tmp;
                    break;
                }
                ConstraintAnchor targetAnchor = (target == null)
                        ? null : target.getAnchor(ConstraintAnchor.Type.BOTTOM).getTarget();
                if (targetAnchor != null && targetAnchor.getOwner() != tmp) {
                    found = tmp;
                } else {
                    tmp = target;
                }
            }

        }
        return found;
    }

    /**
     * Determine if the widget is the first element of a chain in a given orientation.
     *
     * @param orientation Either {@link #HORIZONTAL} or {@link #VERTICAL}
     * @return if the widget is the head of a chain
     */
    private boolean isChainHead(int orientation) {
        int offset = orientation * 2;
        return (mListAnchors[offset].mTarget != null
                && mListAnchors[offset].mTarget.mTarget != mListAnchors[offset])
                && (mListAnchors[offset + 1].mTarget != null
                && mListAnchors[offset + 1].mTarget.mTarget == mListAnchors[offset + 1]);
    }


    /*-----------------------------------------------------------------------*/
    // Constraints
    /*-----------------------------------------------------------------------*/

    /**
     * Add this widget to the solver
     *
     * @param system   the solver we want to add the widget to
     * @param optimize true if {@link Optimizer#OPTIMIZATION_GRAPH} is on
     */
    public void addToSolver(LinearSystem system, boolean optimize) {
        if (LinearSystem.FULL_DEBUG) {
            System.out.println("\n----------------------------------------------");
            System.out.println("-- adding " + getDebugName() + " to the solver");
            if (isInVirtualLayout()) {
                System.out.println("-- note: is in virtual layout");
            }
            System.out.println("----------------------------------------------\n");
        }

        SolverVariable left = system.createObjectVariable(mLeft);
        SolverVariable right = system.createObjectVariable(mRight);
        SolverVariable top = system.createObjectVariable(mTop);
        SolverVariable bottom = system.createObjectVariable(mBottom);
        SolverVariable baseline = system.createObjectVariable(mBaseline);

        boolean horizontalParentWrapContent = false;
        boolean verticalParentWrapContent = false;
        if (mParent != null) {
            horizontalParentWrapContent = mParent != null
                    ? mParent.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == WRAP_CONTENT : false;
            verticalParentWrapContent = mParent != null
                    ? mParent.mListDimensionBehaviors[DIMENSION_VERTICAL] == WRAP_CONTENT : false;

            switch (mWrapBehaviorInParent) {
                case WRAP_BEHAVIOR_SKIPPED: {
                    horizontalParentWrapContent = false;
                    verticalParentWrapContent = false;
                }
                break;
                case WRAP_BEHAVIOR_HORIZONTAL_ONLY: {
                    verticalParentWrapContent = false;
                }
                break;
                case WRAP_BEHAVIOR_VERTICAL_ONLY: {
                    horizontalParentWrapContent = false;
                }
                break;
            }
        }

        if (!(mVisibility != GONE || mAnimated || hasDependencies()
                || mIsInBarrier[HORIZONTAL] || mIsInBarrier[VERTICAL])) {
            return;
        }

        if (mResolvedHorizontal || mResolvedVertical) {
            if (LinearSystem.FULL_DEBUG) {
                System.out.println("\n----------------------------------------------");
                System.out.println("-- setting " + getDebugName()
                        + " to " + mX + ", " + mY + " " + mWidth + " x " + mHeight);
                System.out.println("----------------------------------------------\n");
            }
            // For now apply all, but that won't work for wrap/wrap layouts.
            if (mResolvedHorizontal) {
                system.addEquality(left, mX);
                system.addEquality(right, mX + mWidth);
                if (horizontalParentWrapContent && mParent != null) {
                    if (mOptimizeWrapOnResolved) {
                        ConstraintWidgetContainer container = (ConstraintWidgetContainer) mParent;
                        container.addHorizontalWrapMinVariable(mLeft);
                        container.addHorizontalWrapMaxVariable(mRight);
                    } else {
                        int wrapStrength = SolverVariable.STRENGTH_EQUALITY;
                        system.addGreaterThan(system.createObjectVariable(mParent.mRight),
                                right, 0, wrapStrength);
                    }
                }
            }
            if (mResolvedVertical) {
                system.addEquality(top, mY);
                system.addEquality(bottom, mY + mHeight);
                if (mBaseline.hasDependents()) {
                    system.addEquality(baseline, mY + mBaselineDistance);
                }
                if (verticalParentWrapContent && mParent != null) {
                    if (mOptimizeWrapOnResolved) {
                        ConstraintWidgetContainer container = (ConstraintWidgetContainer) mParent;
                        container.addVerticalWrapMinVariable(mTop);
                        container.addVerticalWrapMaxVariable(mBottom);
                    } else {
                        int wrapStrength = SolverVariable.STRENGTH_EQUALITY;
                        system.addGreaterThan(system.createObjectVariable(mParent.mBottom),
                                bottom, 0, wrapStrength);
                    }
                }
            }
            if (mResolvedHorizontal && mResolvedVertical) {
                mResolvedHorizontal = false;
                mResolvedVertical = false;
                if (LinearSystem.FULL_DEBUG) {
                    System.out.println("\n----------------------------------------------");
                    System.out.println("-- setting COMPLETED for " + getDebugName());
                    System.out.println("----------------------------------------------\n");
                }
                return;
            }
        }

        if (LinearSystem.sMetrics != null) {
            LinearSystem.sMetrics.widgets++;
        }
        if (FULL_DEBUG) {
            if (optimize && mHorizontalRun != null && mVerticalRun != null) {
                System.out.println("-- horizontal run : "
                        + mHorizontalRun.start + " : " + mHorizontalRun.end);
                System.out.println("-- vertical run : "
                        + mVerticalRun.start + " : " + mVerticalRun.end);
            }
        }
        if (optimize && mHorizontalRun != null && mVerticalRun != null
                && mHorizontalRun.start.resolved && mHorizontalRun.end.resolved
                && mVerticalRun.start.resolved && mVerticalRun.end.resolved) {

            if (LinearSystem.sMetrics != null) {
                LinearSystem.sMetrics.graphSolved++;
            }
            system.addEquality(left, mHorizontalRun.start.value);
            system.addEquality(right, mHorizontalRun.end.value);
            system.addEquality(top, mVerticalRun.start.value);
            system.addEquality(bottom, mVerticalRun.end.value);
            system.addEquality(baseline, mVerticalRun.baseline.value);
            if (mParent != null) {
                if (horizontalParentWrapContent
                        && isTerminalWidget[HORIZONTAL] && !isInHorizontalChain()) {
                    SolverVariable parentMax = system.createObjectVariable(mParent.mRight);
                    system.addGreaterThan(parentMax, right, 0, SolverVariable.STRENGTH_FIXED);
                }
                if (verticalParentWrapContent
                        && isTerminalWidget[VERTICAL] && !isInVerticalChain()) {
                    SolverVariable parentMax = system.createObjectVariable(mParent.mBottom);
                    system.addGreaterThan(parentMax, bottom, 0, SolverVariable.STRENGTH_FIXED);
                }
            }
            mResolvedHorizontal = false;
            mResolvedVertical = false;
            return; // we are done here
        }
        if (LinearSystem.sMetrics != null) {
            LinearSystem.sMetrics.linearSolved++;
        }

        boolean inHorizontalChain = false;
        boolean inVerticalChain = false;

        if (mParent != null) {
            // Add this widget to a horizontal chain if it is the Head of it.
            if (isChainHead(HORIZONTAL)) {
                ((ConstraintWidgetContainer) mParent).addChain(this, HORIZONTAL);
                inHorizontalChain = true;
            } else {
                inHorizontalChain = isInHorizontalChain();
            }

            // Add this widget to a vertical chain if it is the Head of it.
            if (isChainHead(VERTICAL)) {
                ((ConstraintWidgetContainer) mParent).addChain(this, VERTICAL);
                inVerticalChain = true;
            } else {
                inVerticalChain = isInVerticalChain();
            }

            if (!inHorizontalChain && horizontalParentWrapContent && mVisibility != GONE
                    && mLeft.mTarget == null && mRight.mTarget == null) {
                if (FULL_DEBUG) {
                    System.out.println("<>1 ADDING H WRAP GREATER FOR " + getDebugName());
                }
                SolverVariable parentRight = system.createObjectVariable(mParent.mRight);
                system.addGreaterThan(parentRight, right, 0, SolverVariable.STRENGTH_LOW);
            }

            if (!inVerticalChain && verticalParentWrapContent && mVisibility != GONE
                    && mTop.mTarget == null && mBottom.mTarget == null && mBaseline == null) {
                if (FULL_DEBUG) {
                    System.out.println("<>1 ADDING V WRAP GREATER FOR " + getDebugName());
                }
                SolverVariable parentBottom = system.createObjectVariable(mParent.mBottom);
                system.addGreaterThan(parentBottom, bottom, 0, SolverVariable.STRENGTH_LOW);
            }
        }

        int width = mWidth;
        if (width < mMinWidth) {
            width = mMinWidth;
        }
        int height = mHeight;
        if (height < mMinHeight) {
            height = mMinHeight;
        }

        // Dimensions can be either fixed (a given value)
        // or dependent on the solver if set to MATCH_CONSTRAINT
        boolean horizontalDimensionFixed =
                mListDimensionBehaviors[DIMENSION_HORIZONTAL] != MATCH_CONSTRAINT;
        boolean verticalDimensionFixed =
                mListDimensionBehaviors[DIMENSION_VERTICAL] != MATCH_CONSTRAINT;

        // We evaluate the dimension ratio here as the connections can change.
        // TODO: have a validation pass after connection instead
        boolean useRatio = false;
        mResolvedDimensionRatioSide = mDimensionRatioSide;
        mResolvedDimensionRatio = mDimensionRatio;

        int matchConstraintDefaultWidth = mMatchConstraintDefaultWidth;
        int matchConstraintDefaultHeight = mMatchConstraintDefaultHeight;

        if (mDimensionRatio > 0 && mVisibility != GONE) {
            useRatio = true;
            if (mListDimensionBehaviors[DIMENSION_HORIZONTAL] == MATCH_CONSTRAINT
                    && matchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD) {
                matchConstraintDefaultWidth = MATCH_CONSTRAINT_RATIO;
            }
            if (mListDimensionBehaviors[DIMENSION_VERTICAL] == MATCH_CONSTRAINT
                    && matchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD) {
                matchConstraintDefaultHeight = MATCH_CONSTRAINT_RATIO;
            }

            if (mListDimensionBehaviors[DIMENSION_HORIZONTAL] == MATCH_CONSTRAINT
                    && mListDimensionBehaviors[DIMENSION_VERTICAL] == MATCH_CONSTRAINT
                    && matchConstraintDefaultWidth == MATCH_CONSTRAINT_RATIO
                    && matchConstraintDefaultHeight == MATCH_CONSTRAINT_RATIO) {
                setupDimensionRatio(horizontalParentWrapContent, verticalParentWrapContent,
                        horizontalDimensionFixed, verticalDimensionFixed);
            } else if (mListDimensionBehaviors[DIMENSION_HORIZONTAL] == MATCH_CONSTRAINT
                    && matchConstraintDefaultWidth == MATCH_CONSTRAINT_RATIO) {
                mResolvedDimensionRatioSide = HORIZONTAL;
                width = (int) (mResolvedDimensionRatio * mHeight);
                if (mListDimensionBehaviors[DIMENSION_VERTICAL] != MATCH_CONSTRAINT) {
                    matchConstraintDefaultWidth = MATCH_CONSTRAINT_RATIO_RESOLVED;
                    useRatio = false;
                }
            } else if (mListDimensionBehaviors[DIMENSION_VERTICAL] == MATCH_CONSTRAINT
                    && matchConstraintDefaultHeight == MATCH_CONSTRAINT_RATIO) {
                mResolvedDimensionRatioSide = VERTICAL;
                if (mDimensionRatioSide == UNKNOWN) {
                    // need to reverse the ratio as the parsing is done in horizontal mode
                    mResolvedDimensionRatio = 1 / mResolvedDimensionRatio;
                }
                height = (int) (mResolvedDimensionRatio * mWidth);
                if (mListDimensionBehaviors[DIMENSION_HORIZONTAL] != MATCH_CONSTRAINT) {
                    matchConstraintDefaultHeight = MATCH_CONSTRAINT_RATIO_RESOLVED;
                    useRatio = false;
                }
            }
        }

        mResolvedMatchConstraintDefault[HORIZONTAL] = matchConstraintDefaultWidth;
        mResolvedMatchConstraintDefault[VERTICAL] = matchConstraintDefaultHeight;
        mResolvedHasRatio = useRatio;

        boolean useHorizontalRatio = useRatio && (mResolvedDimensionRatioSide == HORIZONTAL
                || mResolvedDimensionRatioSide == UNKNOWN);

        boolean useVerticalRatio = useRatio && (mResolvedDimensionRatioSide == VERTICAL
                || mResolvedDimensionRatioSide == UNKNOWN);

        // Horizontal resolution
        boolean wrapContent = (mListDimensionBehaviors[DIMENSION_HORIZONTAL] == WRAP_CONTENT)
                && (this instanceof ConstraintWidgetContainer);
        if (wrapContent) {
            width = 0;
        }

        boolean applyPosition = true;
        if (mCenter.isConnected()) {
            applyPosition = false;
        }

        boolean isInHorizontalBarrier = mIsInBarrier[HORIZONTAL];
        boolean isInVerticalBarrier = mIsInBarrier[VERTICAL];

        if (mHorizontalResolution != DIRECT && !mResolvedHorizontal) {
            if (!optimize || !(mHorizontalRun != null
                    && mHorizontalRun.start.resolved && mHorizontalRun.end.resolved)) {
                SolverVariable parentMax = mParent != null
                        ? system.createObjectVariable(mParent.mRight) : null;
                SolverVariable parentMin = mParent != null
                        ? system.createObjectVariable(mParent.mLeft) : null;
                applyConstraints(system, true, horizontalParentWrapContent,
                        verticalParentWrapContent, isTerminalWidget[HORIZONTAL], parentMin,
                        parentMax, mListDimensionBehaviors[DIMENSION_HORIZONTAL], wrapContent,
                        mLeft, mRight, mX, width,
                        mMinWidth, mMaxDimension[HORIZONTAL],
                        mHorizontalBiasPercent, useHorizontalRatio,
                        mListDimensionBehaviors[VERTICAL] == MATCH_CONSTRAINT,
                        inHorizontalChain, inVerticalChain, isInHorizontalBarrier,
                        matchConstraintDefaultWidth, matchConstraintDefaultHeight,
                        mMatchConstraintMinWidth, mMatchConstraintMaxWidth,
                        mMatchConstraintPercentWidth, applyPosition);
            } else if (optimize) {
                system.addEquality(left, mHorizontalRun.start.value);
                system.addEquality(right, mHorizontalRun.end.value);
                if (mParent != null) {
                    if (horizontalParentWrapContent
                            && isTerminalWidget[HORIZONTAL] && !isInHorizontalChain()) {
                        if (FULL_DEBUG) {
                            System.out.println("<>2 ADDING H WRAP GREATER FOR " + getDebugName());
                        }
                        SolverVariable parentMax = system.createObjectVariable(mParent.mRight);
                        system.addGreaterThan(parentMax, right, 0, SolverVariable.STRENGTH_FIXED);
                    }
                }
            }
        }

        boolean applyVerticalConstraints = true;
        if (optimize && mVerticalRun != null
                && mVerticalRun.start.resolved && mVerticalRun.end.resolved) {
            system.addEquality(top, mVerticalRun.start.value);
            system.addEquality(bottom, mVerticalRun.end.value);
            system.addEquality(baseline, mVerticalRun.baseline.value);
            if (mParent != null) {
                if (!inVerticalChain && verticalParentWrapContent && isTerminalWidget[VERTICAL]) {
                    if (FULL_DEBUG) {
                        System.out.println("<>2 ADDING V WRAP GREATER FOR " + getDebugName());
                    }
                    SolverVariable parentMax = system.createObjectVariable(mParent.mBottom);
                    system.addGreaterThan(parentMax, bottom, 0, SolverVariable.STRENGTH_FIXED);
                }
            }
            applyVerticalConstraints = false;
        }
        if (mVerticalResolution == DIRECT) {
            if (LinearSystem.FULL_DEBUG) {
                System.out.println("\n----------------------------------------------");
                System.out.println("-- DONE adding " + getDebugName() + " to the solver");
                System.out.println("-- SKIP VERTICAL RESOLUTION");
                System.out.println("----------------------------------------------\n");
            }
            applyVerticalConstraints = false;
        }
        if (applyVerticalConstraints && !mResolvedVertical) {
            // Vertical Resolution
            wrapContent = (mListDimensionBehaviors[DIMENSION_VERTICAL] == WRAP_CONTENT)
                    && (this instanceof ConstraintWidgetContainer);
            if (wrapContent) {
                height = 0;
            }

            SolverVariable parentMax = mParent != null
                    ? system.createObjectVariable(mParent.mBottom) : null;
            SolverVariable parentMin = mParent != null
                    ? system.createObjectVariable(mParent.mTop) : null;

            if (mBaselineDistance > 0 || mVisibility == GONE) {
                // if we are GONE we might still have to deal with baseline,
                // even if our baseline distance would be zero
                if (mBaseline.mTarget != null) {
                    system.addEquality(baseline, top, getBaselineDistance(),
                            SolverVariable.STRENGTH_FIXED);
                    SolverVariable baselineTarget = system.createObjectVariable(mBaseline.mTarget);
                    int baselineMargin = mBaseline.getMargin();
                    system.addEquality(baseline, baselineTarget, baselineMargin,
                            SolverVariable.STRENGTH_FIXED);
                    applyPosition = false;
                    if (verticalParentWrapContent) {
                        if (FULL_DEBUG) {
                            System.out.println("<>3 ADDING V WRAP GREATER FOR " + getDebugName());
                        }
                        SolverVariable end = system.createObjectVariable(mBottom);
                        int wrapStrength = SolverVariable.STRENGTH_EQUALITY;
                        system.addGreaterThan(parentMax, end, 0, wrapStrength);
                    }
                } else if (mVisibility == GONE) {
                    // TODO: use the constraints graph here to help
                    system.addEquality(baseline, top, mBaseline.getMargin(),
                            SolverVariable.STRENGTH_FIXED);
                } else {
                    system.addEquality(baseline, top, getBaselineDistance(),
                            SolverVariable.STRENGTH_FIXED);
                }
            }

            applyConstraints(system, false, verticalParentWrapContent,
                    horizontalParentWrapContent, isTerminalWidget[VERTICAL], parentMin,
                    parentMax, mListDimensionBehaviors[DIMENSION_VERTICAL],
                    wrapContent, mTop, mBottom, mY, height,
                    mMinHeight, mMaxDimension[VERTICAL], mVerticalBiasPercent, useVerticalRatio,
                    mListDimensionBehaviors[HORIZONTAL] == MATCH_CONSTRAINT,
                    inVerticalChain, inHorizontalChain, isInVerticalBarrier,
                    matchConstraintDefaultHeight, matchConstraintDefaultWidth,
                    mMatchConstraintMinHeight, mMatchConstraintMaxHeight,
                    mMatchConstraintPercentHeight, applyPosition);
        }

        if (useRatio) {
            int strength = SolverVariable.STRENGTH_FIXED;
            if (mResolvedDimensionRatioSide == VERTICAL) {
                system.addRatio(bottom, top, right, left, mResolvedDimensionRatio, strength);
            } else {
                system.addRatio(right, left, bottom, top, mResolvedDimensionRatio, strength);
            }
        }

        if (mCenter.isConnected()) {
            system.addCenterPoint(this, mCenter.getTarget().getOwner(),
                    (float) Math.toRadians(mCircleConstraintAngle + 90), mCenter.getMargin());
        }

        if (LinearSystem.FULL_DEBUG) {
            System.out.println("\n----------------------------------------------");
            System.out.println("-- DONE adding " + getDebugName() + " to the solver");
            System.out.println("----------------------------------------------\n");
        }
        mResolvedHorizontal = false;
        mResolvedVertical = false;
        if (LinearSystem.sMetrics != null) {
            LinearSystem.sMetrics.mEquations = system.getNumEquations();
            LinearSystem.sMetrics.mVariables = system.getNumVariables();
        }

    }

    /**
     * Used to select which widgets should be added to the solver first
     */
    boolean addFirst() {
        return this instanceof VirtualLayout || this instanceof Guideline;
    }

    /**
     * Resolves the dimension ratio parameters
     * (mResolvedDimensionRatioSide & mDimensionRatio)
     *
     * @param hParentWrapContent       true if parent is in wrap content horizontally
     * @param vParentWrapContent       true if parent is in wrap content vertically
     * @param horizontalDimensionFixed true if this widget horizontal dimension is fixed
     * @param verticalDimensionFixed   true if this widget vertical dimension is fixed
     */
    public void setupDimensionRatio(boolean hParentWrapContent,
            boolean vParentWrapContent,
            boolean horizontalDimensionFixed,
            boolean verticalDimensionFixed) {
        if (mResolvedDimensionRatioSide == UNKNOWN) {
            if (horizontalDimensionFixed && !verticalDimensionFixed) {
                mResolvedDimensionRatioSide = HORIZONTAL;
            } else if (!horizontalDimensionFixed && verticalDimensionFixed) {
                mResolvedDimensionRatioSide = VERTICAL;
                if (mDimensionRatioSide == UNKNOWN) {
                    // need to reverse the ratio as the parsing is done in horizontal mode
                    mResolvedDimensionRatio = 1 / mResolvedDimensionRatio;
                }
            }
        }

        if (mResolvedDimensionRatioSide == HORIZONTAL
                && !(mTop.isConnected() && mBottom.isConnected())) {
            mResolvedDimensionRatioSide = VERTICAL;
        } else if (mResolvedDimensionRatioSide == VERTICAL
                && !(mLeft.isConnected() && mRight.isConnected())) {
            mResolvedDimensionRatioSide = HORIZONTAL;
        }

        // if dimension is still unknown... check parentWrap
        if (mResolvedDimensionRatioSide == UNKNOWN) {
            if (!(mTop.isConnected() && mBottom.isConnected()
                    && mLeft.isConnected() && mRight.isConnected())) {
                // only do that if not all connections are set
                if (mTop.isConnected() && mBottom.isConnected()) {
                    mResolvedDimensionRatioSide = HORIZONTAL;
                } else if (mLeft.isConnected() && mRight.isConnected()) {
                    mResolvedDimensionRatio = 1 / mResolvedDimensionRatio;
                    mResolvedDimensionRatioSide = VERTICAL;
                }
            }
        }

        if (DO_NOT_USE && mResolvedDimensionRatioSide == UNKNOWN) {
            if (hParentWrapContent && !vParentWrapContent) {
                mResolvedDimensionRatioSide = HORIZONTAL;
            } else if (!hParentWrapContent && vParentWrapContent) {
                mResolvedDimensionRatio = 1 / mResolvedDimensionRatio;
                mResolvedDimensionRatioSide = VERTICAL;
            }
        }

        if (mResolvedDimensionRatioSide == UNKNOWN) {
            if (mMatchConstraintMinWidth > 0 && mMatchConstraintMinHeight == 0) {
                mResolvedDimensionRatioSide = HORIZONTAL;
            } else if (mMatchConstraintMinWidth == 0 && mMatchConstraintMinHeight > 0) {
                mResolvedDimensionRatio = 1 / mResolvedDimensionRatio;
                mResolvedDimensionRatioSide = VERTICAL;
            }
        }

        if (DO_NOT_USE && mResolvedDimensionRatioSide == UNKNOWN
                && hParentWrapContent && vParentWrapContent) {
            mResolvedDimensionRatio = 1 / mResolvedDimensionRatio;
            mResolvedDimensionRatioSide = VERTICAL;
        }
    }

    /**
     * Apply the constraints in the system depending on the existing anchors, in one dimension
     *
     * @param system                the linear system we are adding constraints to
     * @param wrapContent           is the widget trying to wrap its content
     *                              (i.e. its size will depends on its content)
     * @param beginAnchor           the first anchor
     * @param endAnchor             the second anchor
     * @param beginPosition         the original position of the anchor
     * @param dimension             the dimension
     * @param matchPercentDimension the percentage relative to the parent,
     *                              applied if in match constraint and percent mode
     */
    private void applyConstraints(LinearSystem system, boolean isHorizontal,
            boolean parentWrapContent, boolean oppositeParentWrapContent,
            boolean isTerminal, SolverVariable parentMin,
            SolverVariable parentMax,
            DimensionBehaviour dimensionBehaviour, boolean wrapContent,
            ConstraintAnchor beginAnchor, ConstraintAnchor endAnchor,
            int beginPosition, int dimension, int minDimension,
            int maxDimension, float bias, boolean useRatio,
            boolean oppositeVariable, boolean inChain,
            boolean oppositeInChain, boolean inBarrier,
            int matchConstraintDefault,
            int oppositeMatchConstraintDefault,
            int matchMinDimension, int matchMaxDimension,
            float matchPercentDimension, boolean applyPosition) {
        SolverVariable begin = system.createObjectVariable(beginAnchor);
        SolverVariable end = system.createObjectVariable(endAnchor);
        SolverVariable beginTarget = system.createObjectVariable(beginAnchor.getTarget());
        SolverVariable endTarget = system.createObjectVariable(endAnchor.getTarget());

        if (system.getMetrics() != null) {
            system.getMetrics().nonresolvedWidgets++;
        }

        boolean isBeginConnected = beginAnchor.isConnected();
        boolean isEndConnected = endAnchor.isConnected();
        boolean isCenterConnected = mCenter.isConnected();

        boolean variableSize = false;

        int numConnections = 0;
        if (isBeginConnected) {
            numConnections++;
        }
        if (isEndConnected) {
            numConnections++;
        }
        if (isCenterConnected) {
            numConnections++;
        }

        if (useRatio) {
            matchConstraintDefault = MATCH_CONSTRAINT_RATIO;
        }
        switch (dimensionBehaviour) {
            case FIXED: {
                variableSize = false;
            }
            break;
            case WRAP_CONTENT: {
                variableSize = false;
            }
            break;
            case MATCH_PARENT: {
                variableSize = false;
            }
            break;
            case MATCH_CONSTRAINT: {
                variableSize = matchConstraintDefault != MATCH_CONSTRAINT_RATIO_RESOLVED;
            }
            break;
        }


        if (mWidthOverride != -1 && isHorizontal) {
            if (FULL_DEBUG) {
                System.out.println("OVERRIDE WIDTH to " + mWidthOverride);
            }
            variableSize = false;
            dimension = mWidthOverride;
            mWidthOverride = -1;
        }
        if (mHeightOverride != -1 && !isHorizontal) {
            if (FULL_DEBUG) {
                System.out.println("OVERRIDE HEIGHT to " + mHeightOverride);
            }
            variableSize = false;
            dimension = mHeightOverride;
            mHeightOverride = -1;
        }

        if (mVisibility == ConstraintWidget.GONE) {
            dimension = 0;
            variableSize = false;
        }

        // First apply starting direct connections (more solver-friendly)
        if (applyPosition) {
            if (!isBeginConnected && !isEndConnected && !isCenterConnected) {
                system.addEquality(begin, beginPosition);
            } else if (isBeginConnected && !isEndConnected) {
                system.addEquality(begin, beginTarget,
                        beginAnchor.getMargin(), SolverVariable.STRENGTH_FIXED);
            }
        }

        // Then apply the dimension
        if (!variableSize) {
            if (wrapContent) {
                system.addEquality(end, begin, 0, SolverVariable.STRENGTH_HIGH);
                if (minDimension > 0) {
                    system.addGreaterThan(end, begin, minDimension, SolverVariable.STRENGTH_FIXED);
                }
                if (maxDimension < Integer.MAX_VALUE) {
                    system.addLowerThan(end, begin, maxDimension, SolverVariable.STRENGTH_FIXED);
                }
            } else {
                system.addEquality(end, begin, dimension, SolverVariable.STRENGTH_FIXED);
            }
        } else {
            if (numConnections != 2
                    && !useRatio
                    && ((matchConstraintDefault == MATCH_CONSTRAINT_WRAP)
                    || (matchConstraintDefault == MATCH_CONSTRAINT_SPREAD))) {
                variableSize = false;
                int d = Math.max(matchMinDimension, dimension);
                if (matchMaxDimension > 0) {
                    d = Math.min(matchMaxDimension, d);
                }
                system.addEquality(end, begin, d, SolverVariable.STRENGTH_FIXED);
            } else {
                if (matchMinDimension == WRAP) {
                    matchMinDimension = dimension;
                }
                if (matchMaxDimension == WRAP) {
                    matchMaxDimension = dimension;
                }
                if (dimension > 0
                        && matchConstraintDefault != MATCH_CONSTRAINT_WRAP) {
                    if (USE_WRAP_DIMENSION_FOR_SPREAD
                            && (matchConstraintDefault == MATCH_CONSTRAINT_SPREAD)) {
                        system.addGreaterThan(end, begin, dimension,
                                SolverVariable.STRENGTH_HIGHEST);
                    }
                    dimension = 0;
                }

                if (matchMinDimension > 0) {
                    system.addGreaterThan(end, begin, matchMinDimension,
                            SolverVariable.STRENGTH_FIXED);
                    dimension = Math.max(dimension, matchMinDimension);
                }
                if (matchMaxDimension > 0) {
                    boolean applyLimit = true;
                    if (parentWrapContent && matchConstraintDefault == MATCH_CONSTRAINT_WRAP) {
                        applyLimit = false;
                    }
                    if (applyLimit) {
                        system.addLowerThan(end, begin,
                                matchMaxDimension, SolverVariable.STRENGTH_FIXED);
                    }
                    dimension = Math.min(dimension, matchMaxDimension);
                }
                if (matchConstraintDefault == MATCH_CONSTRAINT_WRAP) {
                    if (parentWrapContent) {
                        system.addEquality(end, begin, dimension, SolverVariable.STRENGTH_FIXED);
                    } else if (inChain) {
                        system.addEquality(end, begin, dimension, SolverVariable.STRENGTH_EQUALITY);
                        system.addLowerThan(end, begin, dimension, SolverVariable.STRENGTH_FIXED);
                    } else {
                        system.addEquality(end, begin, dimension, SolverVariable.STRENGTH_EQUALITY);
                        system.addLowerThan(end, begin, dimension, SolverVariable.STRENGTH_FIXED);
                    }
                } else if (matchConstraintDefault == MATCH_CONSTRAINT_PERCENT) {
                    SolverVariable percentBegin = null;
                    SolverVariable percentEnd = null;
                    if (beginAnchor.getType() == ConstraintAnchor.Type.TOP
                            || beginAnchor.getType() == ConstraintAnchor.Type.BOTTOM) {
                        // vertical
                        percentBegin = system.createObjectVariable(
                                mParent.getAnchor(ConstraintAnchor.Type.TOP));
                        percentEnd = system.createObjectVariable(
                                mParent.getAnchor(ConstraintAnchor.Type.BOTTOM));
                    } else {
                        percentBegin = system.createObjectVariable(
                                mParent.getAnchor(ConstraintAnchor.Type.LEFT));
                        percentEnd = system.createObjectVariable(
                                mParent.getAnchor(ConstraintAnchor.Type.RIGHT));
                    }
                    system.addConstraint(system.createRow().createRowDimensionRatio(end,
                            begin, percentEnd, percentBegin, matchPercentDimension));
                    if (parentWrapContent) {
                        variableSize = false;
                    }
                } else {
                    isTerminal = true;
                }
            }
        }

        if (!applyPosition || inChain) {
            // If we don't need to apply the position, let's finish now.
            if (LinearSystem.FULL_DEBUG) {
                System.out.println("only deal with dimension for " + mDebugName
                        + ", not positioning (applyPosition: "
                        + applyPosition + " inChain: " + inChain + ")");
            }
            if (numConnections < 2 && parentWrapContent && isTerminal) {
                system.addGreaterThan(begin, parentMin, 0, SolverVariable.STRENGTH_FIXED);
                boolean applyEnd = isHorizontal || (mBaseline.mTarget == null);
                if (!isHorizontal && mBaseline.mTarget != null) {
                    // generally we wouldn't take the current widget in the wrap content,
                    // but if the connected element is a ratio widget,
                    // then we can contribute (as the ratio widget may not be enough by itself)
                    // to it.
                    ConstraintWidget target = mBaseline.mTarget.mOwner;
                    if (target.mDimensionRatio != 0
                            && target.mListDimensionBehaviors[0] == MATCH_CONSTRAINT
                            && target.mListDimensionBehaviors[1] == MATCH_CONSTRAINT) {
                        applyEnd = true;
                    } else {
                        applyEnd = false;
                    }
                }
                if (applyEnd) {
                    if (FULL_DEBUG) {
                        System.out.println("<>4 ADDING WRAP GREATER FOR " + getDebugName());
                    }
                    system.addGreaterThan(parentMax, end, 0, SolverVariable.STRENGTH_FIXED);
                }
            }
            return;
        }

        // Ok, we are dealing with single or centered constraints, let's apply them

        int wrapStrength = SolverVariable.STRENGTH_EQUALITY;

        if (!isBeginConnected && !isEndConnected && !isCenterConnected) {
            // note we already applied the start position before, no need to redo it...
        } else if (isBeginConnected && !isEndConnected) {
            // note we already applied the start position before, no need to redo it...

            // If we are constrained to a barrier, make sure that we are not bypassed in the wrap
            ConstraintWidget beginWidget = beginAnchor.mTarget.mOwner;
            if (parentWrapContent && beginWidget instanceof Barrier) {
                wrapStrength = SolverVariable.STRENGTH_FIXED;
            }
        } else if (!isBeginConnected && isEndConnected) {
            system.addEquality(end, endTarget,
                    -endAnchor.getMargin(), SolverVariable.STRENGTH_FIXED);
            if (parentWrapContent) {
                if (mOptimizeWrapO && begin.isFinalValue && mParent != null) {
                    ConstraintWidgetContainer container = (ConstraintWidgetContainer) mParent;
                    if (isHorizontal) {
                        container.addHorizontalWrapMinVariable(beginAnchor);
                    } else {
                        container.addVerticalWrapMinVariable(beginAnchor);
                    }
                } else {
                    if (FULL_DEBUG) {
                        System.out.println("<>5 ADDING WRAP GREATER FOR " + getDebugName());
                    }
                    system.addGreaterThan(begin, parentMin, 0, SolverVariable.STRENGTH_EQUALITY);
                }
            }
        } else if (isBeginConnected && isEndConnected) {
            boolean applyBoundsCheck = true;
            boolean applyCentering = false;
            boolean applyStrongChecks = false;
            boolean applyRangeCheck = false;
            int rangeCheckStrength = SolverVariable.STRENGTH_EQUALITY;

            // TODO: might not need it here (it's overridden)
            int boundsCheckStrength = SolverVariable.STRENGTH_HIGHEST;
            int centeringStrength = SolverVariable.STRENGTH_BARRIER;

            if (parentWrapContent) {
                rangeCheckStrength = SolverVariable.STRENGTH_EQUALITY;
            }
            ConstraintWidget beginWidget = beginAnchor.mTarget.mOwner;
            ConstraintWidget endWidget = endAnchor.mTarget.mOwner;
            ConstraintWidget parent = getParent();

            if (variableSize) {
                if (matchConstraintDefault == MATCH_CONSTRAINT_SPREAD) {
                    if (matchMaxDimension == 0 && matchMinDimension == 0) {
                        applyStrongChecks = true;
                        rangeCheckStrength = SolverVariable.STRENGTH_FIXED;
                        boundsCheckStrength = SolverVariable.STRENGTH_FIXED;
                        // Optimization in case of centering in parent
                        if (beginTarget.isFinalValue && endTarget.isFinalValue) {
                            system.addEquality(begin, beginTarget,
                                    beginAnchor.getMargin(), SolverVariable.STRENGTH_FIXED);
                            system.addEquality(end, endTarget,
                                    -endAnchor.getMargin(), SolverVariable.STRENGTH_FIXED);
                            return;
                        }
                    } else {
                        applyCentering = true;
                        rangeCheckStrength = SolverVariable.STRENGTH_EQUALITY;
                        boundsCheckStrength = SolverVariable.STRENGTH_EQUALITY;
                        applyBoundsCheck = true;
                        applyRangeCheck = true;
                    }
                    if (beginWidget instanceof Barrier || endWidget instanceof Barrier) {
                        boundsCheckStrength = SolverVariable.STRENGTH_HIGHEST;
                    }
                } else if (matchConstraintDefault == MATCH_CONSTRAINT_PERCENT) {
                    applyCentering = true;
                    rangeCheckStrength = SolverVariable.STRENGTH_EQUALITY;
                    boundsCheckStrength = SolverVariable.STRENGTH_EQUALITY;
                    applyBoundsCheck = true;
                    applyRangeCheck = true;
                    if (beginWidget instanceof Barrier || endWidget instanceof Barrier) {
                        boundsCheckStrength = SolverVariable.STRENGTH_HIGHEST;
                    }
                } else if (matchConstraintDefault == MATCH_CONSTRAINT_WRAP) {
                    applyCentering = true;
                    applyRangeCheck = true;
                    rangeCheckStrength = SolverVariable.STRENGTH_FIXED;
                } else if (matchConstraintDefault == MATCH_CONSTRAINT_RATIO) {
                    if (mResolvedDimensionRatioSide == UNKNOWN) {
                        applyCentering = true;
                        applyRangeCheck = true;
                        applyStrongChecks = true;
                        rangeCheckStrength = SolverVariable.STRENGTH_FIXED;
                        boundsCheckStrength = SolverVariable.STRENGTH_EQUALITY;
                        if (oppositeInChain) {
                            boundsCheckStrength = SolverVariable.STRENGTH_EQUALITY;
                            centeringStrength = SolverVariable.STRENGTH_HIGHEST;
                            if (parentWrapContent) {
                                centeringStrength = SolverVariable.STRENGTH_EQUALITY;
                            }
                        } else {
                            centeringStrength = SolverVariable.STRENGTH_FIXED;
                        }
                    } else {
                        applyCentering = true;
                        applyRangeCheck = true;
                        applyStrongChecks = true;
                        if (useRatio) {
                            // useRatio is true
                            // if the side we base ourselves on for the ratio is this one
                            // if that's not the case, we need to have a stronger constraint.
                            boolean otherSideInvariable =
                                    oppositeMatchConstraintDefault == MATCH_CONSTRAINT_PERCENT
                                            || oppositeMatchConstraintDefault
                                            == MATCH_CONSTRAINT_WRAP;
                            if (!otherSideInvariable) {
                                rangeCheckStrength = SolverVariable.STRENGTH_FIXED;
                                boundsCheckStrength = SolverVariable.STRENGTH_EQUALITY;
                            }
                        } else {
                            rangeCheckStrength = SolverVariable.STRENGTH_EQUALITY;
                            if (matchMaxDimension > 0) {
                                boundsCheckStrength = SolverVariable.STRENGTH_EQUALITY;
                            } else if (matchMaxDimension == 0 && matchMinDimension == 0) {
                                if (!oppositeInChain) {
                                    boundsCheckStrength = SolverVariable.STRENGTH_FIXED;
                                } else {
                                    if (beginWidget != parent && endWidget != parent) {
                                        rangeCheckStrength = SolverVariable.STRENGTH_HIGHEST;
                                    } else {
                                        rangeCheckStrength = SolverVariable.STRENGTH_EQUALITY;
                                    }
                                    boundsCheckStrength = SolverVariable.STRENGTH_HIGHEST;
                                }
                            }
                        }
                    }
                }
            } else {
                applyCentering = true;
                applyRangeCheck = true;

                // Let's optimize away if we can...
                if (beginTarget.isFinalValue && endTarget.isFinalValue) {
                    system.addCentering(begin, beginTarget, beginAnchor.getMargin(),
                            bias, endTarget, end, endAnchor.getMargin(),
                            SolverVariable.STRENGTH_FIXED);
                    if (parentWrapContent && isTerminal) {
                        int margin = 0;
                        if (endAnchor.mTarget != null) {
                            margin = endAnchor.getMargin();
                        }
                        if (endTarget != parentMax) { // if not already applied
                            if (FULL_DEBUG) {
                                System.out.println("<>6 ADDING WRAP GREATER FOR " + getDebugName());
                            }
                            system.addGreaterThan(parentMax, end, margin, wrapStrength);
                        }
                    }
                    return;
                }
            }

            if (applyRangeCheck && beginTarget == endTarget && beginWidget != parent) {
                // no need to apply range / bounds check if we are centered on the same anchor
                applyRangeCheck = false;
                applyBoundsCheck = false;
            }

            if (applyCentering) {
                if (!variableSize && !oppositeVariable && !oppositeInChain
                        && beginTarget == parentMin && endTarget == parentMax) {
                    // for fixed size widgets, we can simplify the constraints
                    centeringStrength = SolverVariable.STRENGTH_FIXED;
                    rangeCheckStrength = SolverVariable.STRENGTH_FIXED;
                    applyBoundsCheck = false;
                    parentWrapContent = false;
                }

                system.addCentering(begin, beginTarget, beginAnchor.getMargin(),
                        bias, endTarget, end, endAnchor.getMargin(), centeringStrength);
            }

            if (mVisibility == GONE && !endAnchor.hasDependents()) {
                return;
            }

            if (applyRangeCheck) {
                if (parentWrapContent && beginTarget != endTarget
                        && !variableSize) {
                    if (beginWidget instanceof Barrier || endWidget instanceof Barrier) {
                        rangeCheckStrength = SolverVariable.STRENGTH_BARRIER;
                    }
                }
                system.addGreaterThan(begin, beginTarget,
                        beginAnchor.getMargin(), rangeCheckStrength);
                system.addLowerThan(end, endTarget, -endAnchor.getMargin(), rangeCheckStrength);
            }

            if (parentWrapContent && inBarrier // if we are referenced by a barrier
                    && !(beginWidget instanceof Barrier || endWidget instanceof Barrier)
                    && !(endWidget == parent)) {
                // ... but not directly constrained by it
                // ... then make sure we can hold our own
                boundsCheckStrength = SolverVariable.STRENGTH_BARRIER;
                rangeCheckStrength = SolverVariable.STRENGTH_BARRIER;
                applyBoundsCheck = true;
            }

            if (applyBoundsCheck) {
                if (applyStrongChecks && (!oppositeInChain || oppositeParentWrapContent)) {
                    int strength = boundsCheckStrength;
                    if (beginWidget == parent || endWidget == parent) {
                        strength = SolverVariable.STRENGTH_BARRIER;
                    }
                    if (beginWidget instanceof Guideline || endWidget instanceof Guideline) {
                        strength = SolverVariable.STRENGTH_EQUALITY;
                    }
                    if (beginWidget instanceof Barrier || endWidget instanceof Barrier) {
                        strength = SolverVariable.STRENGTH_EQUALITY;
                    }
                    if (oppositeInChain) {
                        strength = SolverVariable.STRENGTH_EQUALITY;
                    }
                    boundsCheckStrength = Math.max(strength, boundsCheckStrength);
                }

                if (parentWrapContent) {
                    boundsCheckStrength = Math.min(rangeCheckStrength, boundsCheckStrength);
                    if (useRatio && !oppositeInChain
                            && (beginWidget == parent || endWidget == parent)) {
                        // When using ratio, relax some strength to allow other parts of the system
                        // to take precedence rather than driving it
                        boundsCheckStrength = SolverVariable.STRENGTH_HIGHEST;
                    }
                }
                system.addEquality(begin, beginTarget,
                        beginAnchor.getMargin(), boundsCheckStrength);
                system.addEquality(end, endTarget, -endAnchor.getMargin(), boundsCheckStrength);
            }

            if (parentWrapContent) {
                int margin = 0;
                if (parentMin == beginTarget) {
                    margin = beginAnchor.getMargin();
                }
                if (beginTarget != parentMin) { // already done otherwise
                    if (FULL_DEBUG) {
                        System.out.println("<>7 ADDING WRAP GREATER FOR " + getDebugName());
                    }
                    system.addGreaterThan(begin, parentMin, margin, wrapStrength);
                }
            }

            if (parentWrapContent && variableSize && minDimension == 0 && matchMinDimension == 0) {
                if (FULL_DEBUG) {
                    System.out.println("<>8 ADDING WRAP GREATER FOR " + getDebugName());
                }
                if (variableSize && matchConstraintDefault == MATCH_CONSTRAINT_RATIO) {
                    system.addGreaterThan(end, begin, 0, SolverVariable.STRENGTH_FIXED);
                } else {
                    system.addGreaterThan(end, begin, 0, wrapStrength);
                }
            }
        }

        if (parentWrapContent && isTerminal) {
            int margin = 0;
            if (endAnchor.mTarget != null) {
                margin = endAnchor.getMargin();
            }
            if (endTarget != parentMax) { // if not already applied
                if (mOptimizeWrapO && end.isFinalValue && mParent != null) {
                    ConstraintWidgetContainer container = (ConstraintWidgetContainer) mParent;
                    if (isHorizontal) {
                        container.addHorizontalWrapMaxVariable(endAnchor);
                    } else {
                        container.addVerticalWrapMaxVariable(endAnchor);
                    }
                    return;
                }
                if (FULL_DEBUG) {
                    System.out.println("<>9 ADDING WRAP GREATER FOR " + getDebugName());
                }
                system.addGreaterThan(parentMax, end, margin, wrapStrength);
            }
        }
    }

    /**
     * Update the widget from the values generated by the solver
     *
     * @param system   the solver we get the values from.
     * @param optimize true if {@link Optimizer#OPTIMIZATION_GRAPH} is on
     */
    public void updateFromSolver(LinearSystem system, boolean optimize) {
        int left = system.getObjectVariableValue(mLeft);
        int top = system.getObjectVariableValue(mTop);
        int right = system.getObjectVariableValue(mRight);
        int bottom = system.getObjectVariableValue(mBottom);

        if (optimize && mHorizontalRun != null
                && mHorizontalRun.start.resolved && mHorizontalRun.end.resolved) {
            left = mHorizontalRun.start.value;
            right = mHorizontalRun.end.value;
        }
        if (optimize && mVerticalRun != null
                && mVerticalRun.start.resolved && mVerticalRun.end.resolved) {
            top = mVerticalRun.start.value;
            bottom = mVerticalRun.end.value;
        }

        int w = right - left;
        int h = bottom - top;
        if (w < 0 || h < 0
                || left == Integer.MIN_VALUE || left == Integer.MAX_VALUE
                || top == Integer.MIN_VALUE || top == Integer.MAX_VALUE
                || right == Integer.MIN_VALUE || right == Integer.MAX_VALUE
                || bottom == Integer.MIN_VALUE || bottom == Integer.MAX_VALUE) {
            left = 0;
            top = 0;
            right = 0;
            bottom = 0;
        }
        setFrame(left, top, right, bottom);
        if (DEBUG) {
            System.out.println(" *** UPDATE FROM SOLVER " + this);
        }
    }

    // @TODO: add description
    public void copy(ConstraintWidget src, HashMap<ConstraintWidget, ConstraintWidget> map) {
        // Support for direct resolution
        mHorizontalResolution = src.mHorizontalResolution;
        mVerticalResolution = src.mVerticalResolution;

        mMatchConstraintDefaultWidth = src.mMatchConstraintDefaultWidth;
        mMatchConstraintDefaultHeight = src.mMatchConstraintDefaultHeight;

        mResolvedMatchConstraintDefault[0] = src.mResolvedMatchConstraintDefault[0];
        mResolvedMatchConstraintDefault[1] = src.mResolvedMatchConstraintDefault[1];

        mMatchConstraintMinWidth = src.mMatchConstraintMinWidth;
        mMatchConstraintMaxWidth = src.mMatchConstraintMaxWidth;
        mMatchConstraintMinHeight = src.mMatchConstraintMinHeight;
        mMatchConstraintMaxHeight = src.mMatchConstraintMaxHeight;
        mMatchConstraintPercentHeight = src.mMatchConstraintPercentHeight;
        mIsWidthWrapContent = src.mIsWidthWrapContent;
        mIsHeightWrapContent = src.mIsHeightWrapContent;

        mResolvedDimensionRatioSide = src.mResolvedDimensionRatioSide;
        mResolvedDimensionRatio = src.mResolvedDimensionRatio;

        mMaxDimension = Arrays.copyOf(src.mMaxDimension, src.mMaxDimension.length);
        mCircleConstraintAngle = src.mCircleConstraintAngle;

        mHasBaseline = src.mHasBaseline;
        mInPlaceholder = src.mInPlaceholder;

        // The anchors available on the widget
        // note: all anchors should be added to the mAnchors array (see addAnchors())

        mLeft.reset();
        mTop.reset();
        mRight.reset();
        mBottom.reset();
        mBaseline.reset();
        mCenterX.reset();
        mCenterY.reset();
        mCenter.reset();
        mListDimensionBehaviors = Arrays.copyOf(mListDimensionBehaviors, 2);
        mParent = (mParent == null) ? null : map.get(src.mParent);

        mWidth = src.mWidth;
        mHeight = src.mHeight;
        mDimensionRatio = src.mDimensionRatio;
        mDimensionRatioSide = src.mDimensionRatioSide;

        mX = src.mX;
        mY = src.mY;
        mRelX = src.mRelX;
        mRelY = src.mRelY;

        mOffsetX = src.mOffsetX;
        mOffsetY = src.mOffsetY;

        mBaselineDistance = src.mBaselineDistance;
        mMinWidth = src.mMinWidth;
        mMinHeight = src.mMinHeight;

        mHorizontalBiasPercent = src.mHorizontalBiasPercent;
        mVerticalBiasPercent = src.mVerticalBiasPercent;

        mCompanionWidget = src.mCompanionWidget;
        mContainerItemSkip = src.mContainerItemSkip;
        mVisibility = src.mVisibility;
        mAnimated = src.mAnimated;
        mDebugName = src.mDebugName;
        mType = src.mType;

        mDistToTop = src.mDistToTop;
        mDistToLeft = src.mDistToLeft;
        mDistToRight = src.mDistToRight;
        mDistToBottom = src.mDistToBottom;
        mLeftHasCentered = src.mLeftHasCentered;
        mRightHasCentered = src.mRightHasCentered;

        mTopHasCentered = src.mTopHasCentered;
        mBottomHasCentered = src.mBottomHasCentered;

        mHorizontalWrapVisited = src.mHorizontalWrapVisited;
        mVerticalWrapVisited = src.mVerticalWrapVisited;

        mHorizontalChainStyle = src.mHorizontalChainStyle;
        mVerticalChainStyle = src.mVerticalChainStyle;
        mHorizontalChainFixedPosition = src.mHorizontalChainFixedPosition;
        mVerticalChainFixedPosition = src.mVerticalChainFixedPosition;
        mWeight[0] = src.mWeight[0];
        mWeight[1] = src.mWeight[1];

        mListNextMatchConstraintsWidget[0] = src.mListNextMatchConstraintsWidget[0];
        mListNextMatchConstraintsWidget[1] = src.mListNextMatchConstraintsWidget[1];

        mNextChainWidget[0] = src.mNextChainWidget[0];
        mNextChainWidget[1] = src.mNextChainWidget[1];

        mHorizontalNextWidget = (src.mHorizontalNextWidget == null)
                ? null : map.get(src.mHorizontalNextWidget);
        mVerticalNextWidget = (src.mVerticalNextWidget == null)
                ? null : map.get(src.mVerticalNextWidget);
    }

    // @TODO: add description
    public void updateFromRuns(boolean updateHorizontal, boolean updateVertical) {
        updateHorizontal &= mHorizontalRun.isResolved();
        updateVertical &= mVerticalRun.isResolved();
        int left = mHorizontalRun.start.value;
        int top = mVerticalRun.start.value;
        int right = mHorizontalRun.end.value;
        int bottom = mVerticalRun.end.value;
        int w = right - left;
        int h = bottom - top;
        if (w < 0 || h < 0
                || left == Integer.MIN_VALUE || left == Integer.MAX_VALUE
                || top == Integer.MIN_VALUE || top == Integer.MAX_VALUE
                || right == Integer.MIN_VALUE || right == Integer.MAX_VALUE
                || bottom == Integer.MIN_VALUE || bottom == Integer.MAX_VALUE) {
            left = 0;
            top = 0;
            right = 0;
            bottom = 0;
        }

        w = right - left;
        h = bottom - top;

        if (updateHorizontal) {
            mX = left;
        }
        if (updateVertical) {
            mY = top;
        }

        if (mVisibility == ConstraintWidget.GONE) {
            mWidth = 0;
            mHeight = 0;
            return;
        }

        // correct dimensional instability caused by rounding errors
        if (updateHorizontal) {
            if (mListDimensionBehaviors[DIMENSION_HORIZONTAL]
                    == DimensionBehaviour.FIXED && w < mWidth) {
                w = mWidth;
            }
            mWidth = w;
            if (mWidth < mMinWidth) {
                mWidth = mMinWidth;
            }
        }

        if (updateVertical) {
            if (mListDimensionBehaviors[DIMENSION_VERTICAL]
                    == DimensionBehaviour.FIXED && h < mHeight) {
                h = mHeight;
            }
            mHeight = h;
            if (mHeight < mMinHeight) {
                mHeight = mMinHeight;
            }
        }

    }

    // @TODO: add description
    public void addChildrenToSolverByDependency(ConstraintWidgetContainer container,
            LinearSystem system,
            HashSet<ConstraintWidget> widgets,
            int orientation,
            boolean addSelf) {
        if (addSelf) {
            if (!widgets.contains(this)) {
                return;
            }
            Optimizer.checkMatchParent(container, system, this);
            widgets.remove(this);
            addToSolver(system, container.optimizeFor(Optimizer.OPTIMIZATION_GRAPH));
        }
        if (orientation == HORIZONTAL) {
            HashSet<ConstraintAnchor> dependents = mLeft.getDependents();
            if (dependents != null) {
                for (ConstraintAnchor anchor : dependents) {
                    anchor.mOwner.addChildrenToSolverByDependency(container,
                            system, widgets, orientation, true);
                }
            }
            dependents = mRight.getDependents();
            if (dependents != null) {
                for (ConstraintAnchor anchor : dependents) {
                    anchor.mOwner.addChildrenToSolverByDependency(container,
                            system, widgets, orientation, true);
                }
            }
        } else {
            HashSet<ConstraintAnchor> dependents = mTop.getDependents();
            if (dependents != null) {
                for (ConstraintAnchor anchor : dependents) {
                    anchor.mOwner.addChildrenToSolverByDependency(container,
                            system, widgets, orientation, true);
                }
            }
            dependents = mBottom.getDependents();
            if (dependents != null) {
                for (ConstraintAnchor anchor : dependents) {
                    anchor.mOwner.addChildrenToSolverByDependency(container,
                            system, widgets, orientation, true);
                }
            }
            dependents = mBaseline.getDependents();
            if (dependents != null) {
                for (ConstraintAnchor anchor : dependents) {
                    anchor.mOwner.addChildrenToSolverByDependency(container,
                            system, widgets, orientation, true);
                }
            }
        }
        // horizontal
    }

    // @TODO: add description
    public void getSceneString(StringBuilder ret) {

        ret.append("  " + stringId + ":{\n");
        ret.append("    actualWidth:" + mWidth);
        ret.append("\n");
        ret.append("    actualHeight:" + mHeight);
        ret.append("\n");
        ret.append("    actualLeft:" + mX);
        ret.append("\n");
        ret.append("    actualTop:" + mY);
        ret.append("\n");
        getSceneString(ret, "left", mLeft);
        getSceneString(ret, "top", mTop);
        getSceneString(ret, "right", mRight);
        getSceneString(ret, "bottom", mBottom);
        getSceneString(ret, "baseline", mBaseline);
        getSceneString(ret, "centerX", mCenterX);
        getSceneString(ret, "centerY", mCenterY);
        getSceneString(ret, "    width",
                mWidth,
                mMinWidth,
                mMaxDimension[HORIZONTAL],
                mWidthOverride,
                mMatchConstraintMinWidth,
                mMatchConstraintDefaultWidth,
                mMatchConstraintPercentWidth,
                mListDimensionBehaviors[HORIZONTAL],
                mWeight[DIMENSION_HORIZONTAL]
        );

        getSceneString(ret, "    height",
                mHeight,
                mMinHeight,
                mMaxDimension[VERTICAL],
                mHeightOverride,
                mMatchConstraintMinHeight,
                mMatchConstraintDefaultHeight,
                mMatchConstraintPercentHeight,
                mListDimensionBehaviors[VERTICAL],
                mWeight[DIMENSION_VERTICAL]);
        serializeDimensionRatio(ret, "    dimensionRatio", mDimensionRatio, mDimensionRatioSide);
        serializeAttribute(ret, "    horizontalBias", mHorizontalBiasPercent, DEFAULT_BIAS);
        serializeAttribute(ret, "    verticalBias", mVerticalBiasPercent, DEFAULT_BIAS);
        serializeAttribute(ret, "    horizontalChainStyle", mHorizontalChainStyle, CHAIN_SPREAD);
        serializeAttribute(ret, "    verticalChainStyle", mVerticalChainStyle, CHAIN_SPREAD);

        ret.append("  }");

    }

    private void getSceneString(StringBuilder ret, String type, int size,
            int min, int max,
            @SuppressWarnings("unused") int override,
            int matchConstraintMin, int matchConstraintDefault,
            float matchConstraintPercent,
            DimensionBehaviour behavior,
            @SuppressWarnings("unused") float weight) {
        ret.append(type);
        ret.append(" :  {\n");
        serializeAttribute(ret, "      behavior", behavior.toString(),
                DimensionBehaviour.FIXED.toString());
        serializeAttribute(ret, "      size", size, 0);
        serializeAttribute(ret, "      min", min, 0);
        serializeAttribute(ret, "      max", max, Integer.MAX_VALUE);
        serializeAttribute(ret, "      matchMin", matchConstraintMin, 0);
        serializeAttribute(ret, "      matchDef", matchConstraintDefault, MATCH_CONSTRAINT_SPREAD);
        serializeAttribute(ret, "      matchPercent", matchConstraintPercent, 1);
        ret.append("    },\n");
    }

    private void getSceneString(StringBuilder ret, String side, ConstraintAnchor a) {
        if (a.mTarget == null) {
            return;
        }
        ret.append("    ");
        ret.append(side);
        ret.append(" : [ '");
        ret.append(a.mTarget);
        ret.append("'");
        if (a.mGoneMargin != Integer.MIN_VALUE || a.mMargin != 0) {
            ret.append(",");
            ret.append(a.mMargin);
            if (a.mGoneMargin != Integer.MIN_VALUE) {
                ret.append(",");
                ret.append(a.mGoneMargin);
                ret.append(",");
            }
        }
        ret.append(" ] ,\n");
    }
}
