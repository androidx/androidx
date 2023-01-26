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

package androidx.constraintlayout.core.state;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.UNKNOWN;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

import androidx.annotation.Nullable;
import androidx.constraintlayout.core.motion.utils.TypedBundle;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.constraintlayout.core.state.helpers.Facade;
import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;

import java.util.ArrayList;
import java.util.HashMap;

public class ConstraintReference implements Reference {

    private Object mKey;

    @Override
    public void setKey(Object key) {
        this.mKey = key;
    }

    @Override
    public Object getKey() {
        return mKey;
    }

    public void setTag(String tag) {
        mTag = tag;
    }

    public String getTag() {
        return mTag;
    }

    public interface ConstraintReferenceFactory {
        // @TODO: add description
        ConstraintReference create(State state);
    }

    final State mState;

    String mTag = null;

    Facade mFacade = null;

    int mHorizontalChainStyle = ConstraintWidget.CHAIN_SPREAD;
    int mVerticalChainStyle = ConstraintWidget.CHAIN_SPREAD;

    float mHorizontalChainWeight = UNKNOWN;
    float mVerticalChainWeight = UNKNOWN;

    protected float mHorizontalBias = 0.5f;
    protected float mVerticalBias = 0.5f;

    protected int mMarginLeft = 0;
    protected int mMarginRight = 0;
    protected int mMarginStart = 0;
    protected int mMarginEnd = 0;
    protected int mMarginTop = 0;
    protected int mMarginBottom = 0;

    protected int mMarginLeftGone = 0;
    protected int mMarginRightGone = 0;
    protected int mMarginStartGone = 0;
    protected int mMarginEndGone = 0;
    protected int mMarginTopGone = 0;
    protected int mMarginBottomGone = 0;

    int mMarginBaseline = 0;
    int mMarginBaselineGone = 0;

    float mPivotX = Float.NaN;
    float mPivotY = Float.NaN;

    float mRotationX = Float.NaN;
    float mRotationY = Float.NaN;
    float mRotationZ = Float.NaN;

    float mTranslationX = Float.NaN;
    float mTranslationY = Float.NaN;
    float mTranslationZ = Float.NaN;

    float mAlpha = Float.NaN;

    float mScaleX = Float.NaN;
    float mScaleY = Float.NaN;

    int mVisibility = ConstraintWidget.VISIBLE;

    protected Object mLeftToLeft = null;
    protected Object mLeftToRight = null;
    protected Object mRightToLeft = null;
    protected Object mRightToRight = null;
    protected Object mStartToStart = null;
    protected Object mStartToEnd = null;
    protected Object mEndToStart = null;
    protected Object mEndToEnd = null;
    protected Object mTopToTop = null;
    protected Object mTopToBottom = null;
    @Nullable Object mTopToBaseline = null;
    protected Object mBottomToTop = null;
    protected Object mBottomToBottom = null;
    @Nullable Object mBottomToBaseline = null;
    Object mBaselineToBaseline = null;
    Object mBaselineToTop = null;
    Object mBaselineToBottom = null;
    Object mCircularConstraint = null;
    private float mCircularAngle;
    private float mCircularDistance;

    State.Constraint mLast = null;

    Dimension mHorizontalDimension = Dimension.createFixed(Dimension.WRAP_DIMENSION);
    Dimension mVerticalDimension = Dimension.createFixed(Dimension.WRAP_DIMENSION);

    private Object mView;
    private ConstraintWidget mConstraintWidget;

    private HashMap<String, Integer> mCustomColors = new HashMap<>();
    private HashMap<String, Float> mCustomFloats = new HashMap<>();

    TypedBundle mMotionProperties = null;

    // @TODO: add description
    public void setView(Object view) {
        mView = view;
        if (mConstraintWidget != null) {
            mConstraintWidget.setCompanionWidget(mView);
        }
    }

    public Object getView() {
        return mView;
    }

    // @TODO: add description
    public void setFacade(Facade facade) {
        mFacade = facade;
        if (facade != null) {
            setConstraintWidget(facade.getConstraintWidget());
        }
    }

    @Override
    public Facade getFacade() {
        return mFacade;
    }

    // @TODO: add description
    @Override
    public void setConstraintWidget(ConstraintWidget widget) {
        if (widget == null) {
            return;
        }
        mConstraintWidget = widget;
        mConstraintWidget.setCompanionWidget(mView);
    }

    @Override
    public ConstraintWidget getConstraintWidget() {
        if (mConstraintWidget == null) {
            mConstraintWidget = createConstraintWidget();
            mConstraintWidget.setCompanionWidget(mView);
        }
        return mConstraintWidget;
    }

    // @TODO: add description
    public ConstraintWidget createConstraintWidget() {
        return new ConstraintWidget(
                getWidth().getValue(),
                getHeight().getValue());
    }

    static class IncorrectConstraintException extends Exception {

        private final ArrayList<String> mErrors;

        IncorrectConstraintException(ArrayList<String> errors) {
            mErrors = errors;
        }

        public ArrayList<String> getErrors() {
            return mErrors;
        }

        @Override
        public String  getMessage() { return toString(); }

        @Override
        public String toString() {
            return "IncorrectConstraintException: " + mErrors.toString();
        }
    }

    /**
     * Validate the constraints
     */
    public void validate() throws IncorrectConstraintException {
        ArrayList<String> errors = new ArrayList<>();
        if (mLeftToLeft != null && mLeftToRight != null) {
            errors.add("LeftToLeft and LeftToRight both defined");
        }
        if (mRightToLeft != null && mRightToRight != null) {
            errors.add("RightToLeft and RightToRight both defined");
        }
        if (mStartToStart != null && mStartToEnd != null) {
            errors.add("StartToStart and StartToEnd both defined");
        }
        if (mEndToStart != null && mEndToEnd != null) {
            errors.add("EndToStart and EndToEnd both defined");
        }
        if ((mLeftToLeft != null || mLeftToRight != null
                || mRightToLeft != null || mRightToRight != null)
                && (mStartToStart != null || mStartToEnd != null
                || mEndToStart != null || mEndToEnd != null)) {
            errors.add("Both left/right and start/end constraints defined");
        }
        if (errors.size() > 0) {
            throw new IncorrectConstraintException(errors);
        }
    }

    private Object get(Object reference) {
        if (reference == null) {
            return null;
        }
        if (!(reference instanceof ConstraintReference)) {
            return mState.reference(reference);
        }
        return reference;
    }

    public ConstraintReference(State state) {
        mState = state;
    }

    public void setHorizontalChainStyle(int chainStyle) {
        mHorizontalChainStyle = chainStyle;
    }

    public int getHorizontalChainStyle() {
        return mHorizontalChainStyle;
    }

    public void setVerticalChainStyle(int chainStyle) {
        mVerticalChainStyle = chainStyle;
    }

    // @TODO: add description
    public int getVerticalChainStyle(int chainStyle) {
        return mVerticalChainStyle;
    }

    public float getHorizontalChainWeight() {
        return mHorizontalChainWeight;
    }

    public void setHorizontalChainWeight(float weight) {
        mHorizontalChainWeight = weight;
    }

    public float getVerticalChainWeight() {
        return mVerticalChainWeight;
    }

    public void setVerticalChainWeight(float weight) {
        mVerticalChainWeight = weight;
    }

    // @TODO: add description
    public ConstraintReference clearVertical() {
        top().clear();
        baseline().clear();
        bottom().clear();
        return this;
    }

    // @TODO: add description
    public ConstraintReference clearHorizontal() {
        start().clear();
        end().clear();
        left().clear();
        right().clear();
        return this;
    }

    public float getTranslationX() {
        return mTranslationX;
    }

    public float getTranslationY() {
        return mTranslationY;
    }

    public float getTranslationZ() {
        return mTranslationZ;
    }

    public float getScaleX() {
        return mScaleX;
    }

    public float getScaleY() {
        return mScaleY;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public float getPivotX() {
        return mPivotX;
    }

    public float getPivotY() {
        return mPivotY;
    }

    public float getRotationX() {
        return mRotationX;
    }

    public float getRotationY() {
        return mRotationY;
    }

    public float getRotationZ() {
        return mRotationZ;
    }

    // @TODO: add description
    public ConstraintReference pivotX(float x) {
        mPivotX = x;
        return this;
    }

    // @TODO: add description
    public ConstraintReference pivotY(float y) {
        mPivotY = y;
        return this;
    }

    // @TODO: add description
    public ConstraintReference rotationX(float x) {
        mRotationX = x;
        return this;
    }

    // @TODO: add description
    public ConstraintReference rotationY(float y) {
        mRotationY = y;
        return this;
    }

    // @TODO: add description
    public ConstraintReference rotationZ(float z) {
        mRotationZ = z;
        return this;
    }

    // @TODO: add description
    public ConstraintReference translationX(float x) {
        mTranslationX = x;
        return this;
    }

    // @TODO: add description
    public ConstraintReference translationY(float y) {
        mTranslationY = y;
        return this;
    }

    // @TODO: add description
    public ConstraintReference translationZ(float z) {
        mTranslationZ = z;
        return this;
    }

    // @TODO: add description
    public ConstraintReference scaleX(float x) {
        mScaleX = x;
        return this;
    }

    // @TODO: add description
    public ConstraintReference scaleY(float y) {
        mScaleY = y;
        return this;
    }

    // @TODO: add description
    public ConstraintReference alpha(float alpha) {
        mAlpha = alpha;
        return this;
    }

    // @TODO: add description
    public ConstraintReference visibility(int visibility) {
        mVisibility = visibility;
        return this;
    }

    // @TODO: add description
    public ConstraintReference left() {
        if (mLeftToLeft != null) {
            mLast = State.Constraint.LEFT_TO_LEFT;
        } else {
            mLast = State.Constraint.LEFT_TO_RIGHT;
        }
        return this;
    }

    // @TODO: add description
    public ConstraintReference right() {
        if (mRightToLeft != null) {
            mLast = State.Constraint.RIGHT_TO_LEFT;
        } else {
            mLast = State.Constraint.RIGHT_TO_RIGHT;
        }
        return this;
    }

    // @TODO: add description
    public ConstraintReference start() {
        if (mStartToStart != null) {
            mLast = State.Constraint.START_TO_START;
        } else {
            mLast = State.Constraint.START_TO_END;
        }
        return this;
    }

    // @TODO: add description
    public ConstraintReference end() {
        if (mEndToStart != null) {
            mLast = State.Constraint.END_TO_START;
        } else {
            mLast = State.Constraint.END_TO_END;
        }
        return this;
    }

    // @TODO: add description
    public ConstraintReference top() {
        if (mTopToTop != null) {
            mLast = State.Constraint.TOP_TO_TOP;
        } else {
            mLast = State.Constraint.TOP_TO_BOTTOM;
        }
        return this;
    }

    // @TODO: add description
    public ConstraintReference bottom() {
        if (mBottomToTop != null) {
            mLast = State.Constraint.BOTTOM_TO_TOP;
        } else {
            mLast = State.Constraint.BOTTOM_TO_BOTTOM;
        }
        return this;
    }

    // @TODO: add description
    public ConstraintReference baseline() {
        mLast = State.Constraint.BASELINE_TO_BASELINE;
        return this;
    }

    // @TODO: add description
    public void addCustomColor(String name, int color) {
        mCustomColors.put(name, color);
    }

    // @TODO: add description
    public void addCustomFloat(String name, float value) {
        if (mCustomFloats == null) {
            mCustomFloats = new HashMap<>();
        }
        mCustomFloats.put(name, value);
    }

    private void dereference() {
        mLeftToLeft = get(mLeftToLeft);
        mLeftToRight = get(mLeftToRight);
        mRightToLeft = get(mRightToLeft);
        mRightToRight = get(mRightToRight);
        mStartToStart = get(mStartToStart);
        mStartToEnd = get(mStartToEnd);
        mEndToStart = get(mEndToStart);
        mEndToEnd = get(mEndToEnd);
        mTopToTop = get(mTopToTop);
        mTopToBottom = get(mTopToBottom);
        mBottomToTop = get(mBottomToTop);
        mBottomToBottom = get(mBottomToBottom);
        mBaselineToBaseline = get(mBaselineToBaseline);
        mBaselineToTop = get(mBaselineToTop);
        mBaselineToBottom = get(mBaselineToBottom);
    }

    // @TODO: add description
    public ConstraintReference leftToLeft(Object reference) {
        mLast = State.Constraint.LEFT_TO_LEFT;
        mLeftToLeft = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference leftToRight(Object reference) {
        mLast = State.Constraint.LEFT_TO_RIGHT;
        mLeftToRight = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference rightToLeft(Object reference) {
        mLast = State.Constraint.RIGHT_TO_LEFT;
        mRightToLeft = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference rightToRight(Object reference) {
        mLast = State.Constraint.RIGHT_TO_RIGHT;
        mRightToRight = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference startToStart(Object reference) {
        mLast = State.Constraint.START_TO_START;
        mStartToStart = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference startToEnd(Object reference) {
        mLast = State.Constraint.START_TO_END;
        mStartToEnd = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference endToStart(Object reference) {
        mLast = State.Constraint.END_TO_START;
        mEndToStart = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference endToEnd(Object reference) {
        mLast = State.Constraint.END_TO_END;
        mEndToEnd = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference topToTop(Object reference) {
        mLast = State.Constraint.TOP_TO_TOP;
        mTopToTop = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference topToBottom(Object reference) {
        mLast = State.Constraint.TOP_TO_BOTTOM;
        mTopToBottom = reference;
        return this;
    }

    ConstraintReference topToBaseline(Object reference) {
        mLast = State.Constraint.TOP_TO_BASELINE;
        mTopToBaseline = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference bottomToTop(Object reference) {
        mLast = State.Constraint.BOTTOM_TO_TOP;
        mBottomToTop = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference bottomToBottom(Object reference) {
        mLast = State.Constraint.BOTTOM_TO_BOTTOM;
        mBottomToBottom = reference;
        return this;
    }

    ConstraintReference bottomToBaseline(Object reference) {
        mLast = State.Constraint.BOTTOM_TO_BASELINE;
        mBottomToBaseline = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference baselineToBaseline(Object reference) {
        mLast = State.Constraint.BASELINE_TO_BASELINE;
        mBaselineToBaseline = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference baselineToTop(Object reference) {
        mLast = State.Constraint.BASELINE_TO_TOP;
        mBaselineToTop = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference baselineToBottom(Object reference) {
        mLast = State.Constraint.BASELINE_TO_BOTTOM;
        mBaselineToBottom = reference;
        return this;
    }

    // @TODO: add description
    public ConstraintReference centerHorizontally(Object reference) {
        Object ref = get(reference);
        mStartToStart = ref;
        mEndToEnd = ref;
        mLast = State.Constraint.CENTER_HORIZONTALLY;
        mHorizontalBias = 0.5f;
        return this;
    }

    // @TODO: add description
    public ConstraintReference centerVertically(Object reference) {
        Object ref = get(reference);
        mTopToTop = ref;
        mBottomToBottom = ref;
        mLast = State.Constraint.CENTER_VERTICALLY;
        mVerticalBias = 0.5f;
        return this;
    }

    // @TODO: add description
    public ConstraintReference circularConstraint(Object reference, float angle, float distance) {
        Object ref = get(reference);
        mCircularConstraint = ref;
        mCircularAngle = angle;
        mCircularDistance = distance;
        mLast = State.Constraint.CIRCULAR_CONSTRAINT;
        return this;
    }

    // @TODO: add description
    public ConstraintReference width(Dimension dimension) {
        return setWidth(dimension);
    }

    // @TODO: add description
    public ConstraintReference height(Dimension dimension) {
        return setHeight(dimension);
    }

    public Dimension getWidth() {
        return mHorizontalDimension;
    }

    // @TODO: add description
    public ConstraintReference setWidth(Dimension dimension) {
        mHorizontalDimension = dimension;
        return this;
    }

    public Dimension getHeight() {
        return mVerticalDimension;
    }

    // @TODO: add description
    public ConstraintReference setHeight(Dimension dimension) {
        mVerticalDimension = dimension;
        return this;
    }

    // @TODO: add description
    public ConstraintReference margin(Object marginValue) {
        return margin(mState.convertDimension(marginValue));
    }

    // @TODO: add description
    public ConstraintReference marginGone(Object marginGoneValue) {
        return marginGone(mState.convertDimension(marginGoneValue));
    }

    // @TODO: add description
    public ConstraintReference margin(int value) {
        if (mLast != null) {
            switch (mLast) {
                case LEFT_TO_LEFT:
                case LEFT_TO_RIGHT: {
                    mMarginLeft = value;
                }
                break;
                case RIGHT_TO_LEFT:
                case RIGHT_TO_RIGHT: {
                    mMarginRight = value;
                }
                break;
                case START_TO_START:
                case START_TO_END: {
                    mMarginStart = value;
                }
                break;
                case END_TO_START:
                case END_TO_END: {
                    mMarginEnd = value;
                }
                break;
                case TOP_TO_TOP:
                case TOP_TO_BOTTOM:
                case TOP_TO_BASELINE: {
                    mMarginTop = value;
                }
                break;
                case BOTTOM_TO_TOP:
                case BOTTOM_TO_BOTTOM:
                case BOTTOM_TO_BASELINE: {
                    mMarginBottom = value;
                }
                break;
                case BASELINE_TO_BOTTOM:
                case BASELINE_TO_TOP:
                case BASELINE_TO_BASELINE: {
                    mMarginBaseline = value;
                }
                break;
                case CIRCULAR_CONSTRAINT: {
                    mCircularDistance = value;
                }
                break;
                default:
                    break;
            }
        } else {
            mMarginLeft = value;
            mMarginRight = value;
            mMarginStart = value;
            mMarginEnd = value;
            mMarginTop = value;
            mMarginBottom = value;
        }
        return this;
    }

    // @TODO: add description
    public ConstraintReference marginGone(int value) {
        if (mLast != null) {
            switch (mLast) {
                case LEFT_TO_LEFT:
                case LEFT_TO_RIGHT: {
                    mMarginLeftGone = value;
                }
                break;
                case RIGHT_TO_LEFT:
                case RIGHT_TO_RIGHT: {
                    mMarginRightGone = value;
                }
                break;
                case START_TO_START:
                case START_TO_END: {
                    mMarginStartGone = value;
                }
                break;
                case END_TO_START:
                case END_TO_END: {
                    mMarginEndGone = value;
                }
                break;
                case TOP_TO_TOP:
                case TOP_TO_BOTTOM:
                case TOP_TO_BASELINE: {
                    mMarginTopGone = value;
                }
                break;
                case BOTTOM_TO_TOP:
                case BOTTOM_TO_BOTTOM:
                case BOTTOM_TO_BASELINE: {
                    mMarginBottomGone = value;
                }
                break;
                case BASELINE_TO_TOP:
                case BASELINE_TO_BOTTOM:
                case BASELINE_TO_BASELINE: {
                    mMarginBaselineGone = value;
                }
                break;
                default:
                    break;
            }
        } else {
            mMarginLeftGone = value;
            mMarginRightGone = value;
            mMarginStartGone = value;
            mMarginEndGone = value;
            mMarginTopGone = value;
            mMarginBottomGone = value;
        }
        return this;
    }

    // @TODO: add description
    public ConstraintReference horizontalBias(float value) {
        mHorizontalBias = value;
        return this;
    }

    // @TODO: add description
    public ConstraintReference verticalBias(float value) {
        mVerticalBias = value;
        return this;
    }

    // @TODO: add description
    public ConstraintReference bias(float value) {
        if (mLast == null) {
            return this;
        }
        switch (mLast) {
            case CENTER_HORIZONTALLY:
            case LEFT_TO_LEFT:
            case LEFT_TO_RIGHT:
            case RIGHT_TO_LEFT:
            case RIGHT_TO_RIGHT:
            case START_TO_START:
            case START_TO_END:
            case END_TO_START:
            case END_TO_END: {
                mHorizontalBias = value;
            }
            break;
            case CENTER_VERTICALLY:
            case TOP_TO_TOP:
            case TOP_TO_BOTTOM:
            case TOP_TO_BASELINE:
            case BOTTOM_TO_TOP:
            case BOTTOM_TO_BOTTOM:
            case BOTTOM_TO_BASELINE: {
                mVerticalBias = value;
            }
            break;
            default:
                break;
        }
        return this;
    }

    /**
     * Clears all constraints.
     */
    public ConstraintReference clearAll() {
        mLeftToLeft = null;
        mLeftToRight = null;
        mMarginLeft = 0;
        mRightToLeft = null;
        mRightToRight = null;
        mMarginRight = 0;
        mStartToStart = null;
        mStartToEnd = null;
        mMarginStart = 0;
        mEndToStart = null;
        mEndToEnd = null;
        mMarginEnd = 0;
        mTopToTop = null;
        mTopToBottom = null;
        mMarginTop = 0;
        mBottomToTop = null;
        mBottomToBottom = null;
        mMarginBottom = 0;
        mBaselineToBaseline = null;
        mCircularConstraint = null;
        mHorizontalBias = 0.5f;
        mVerticalBias = 0.5f;
        mMarginLeftGone = 0;
        mMarginRightGone = 0;
        mMarginStartGone = 0;
        mMarginEndGone = 0;
        mMarginTopGone = 0;
        mMarginBottomGone = 0;
        return this;
    }

    // @TODO: add description
    public ConstraintReference clear() {
        if (mLast != null) {
            switch (mLast) {
                case LEFT_TO_LEFT:
                case LEFT_TO_RIGHT: {
                    mLeftToLeft = null;
                    mLeftToRight = null;
                    mMarginLeft = 0;
                    mMarginLeftGone = 0;
                }
                break;
                case RIGHT_TO_LEFT:
                case RIGHT_TO_RIGHT: {
                    mRightToLeft = null;
                    mRightToRight = null;
                    mMarginRight = 0;
                    mMarginRightGone = 0;
                }
                break;
                case START_TO_START:
                case START_TO_END: {
                    mStartToStart = null;
                    mStartToEnd = null;
                    mMarginStart = 0;
                    mMarginStartGone = 0;
                }
                break;
                case END_TO_START:
                case END_TO_END: {
                    mEndToStart = null;
                    mEndToEnd = null;
                    mMarginEnd = 0;
                    mMarginEndGone = 0;
                }
                break;
                case TOP_TO_TOP:
                case TOP_TO_BOTTOM:
                case TOP_TO_BASELINE: {
                    mTopToTop = null;
                    mTopToBottom = null;
                    mTopToBaseline = null;
                    mMarginTop = 0;
                    mMarginTopGone = 0;
                }
                break;
                case BOTTOM_TO_TOP:
                case BOTTOM_TO_BOTTOM:
                case BOTTOM_TO_BASELINE: {
                    mBottomToTop = null;
                    mBottomToBottom = null;
                    mBottomToBaseline = null;
                    mMarginBottom = 0;
                    mMarginBottomGone = 0;
                }
                break;
                case BASELINE_TO_BASELINE: {
                    mBaselineToBaseline = null;
                }
                break;
                case CIRCULAR_CONSTRAINT: {
                    mCircularConstraint = null;
                }
                break;
                default:
                    break;
            }
        } else {
            clearAll();
        }
        return this;
    }

    private ConstraintWidget getTarget(Object target) {
        if (target instanceof Reference) {
            Reference referenceTarget = (Reference) target;
            return referenceTarget.getConstraintWidget();
        }
        return null;
    }

    private void applyConnection(ConstraintWidget widget,
            Object opaqueTarget,
            State.Constraint type) {
        ConstraintWidget target = getTarget(opaqueTarget);
        if (target == null) {
            return;
        }
        switch (type) {
            // TODO: apply RTL
            default:
                break;
        }
        switch (type) {
            case START_TO_START: {
                widget.getAnchor(ConstraintAnchor.Type.LEFT).connect(target.getAnchor(
                        ConstraintAnchor.Type.LEFT), mMarginStart, mMarginStartGone, false);
            }
            break;
            case START_TO_END: {
                widget.getAnchor(ConstraintAnchor.Type.LEFT).connect(target.getAnchor(
                        ConstraintAnchor.Type.RIGHT), mMarginStart, mMarginStartGone, false);
            }
            break;
            case END_TO_START: {
                widget.getAnchor(ConstraintAnchor.Type.RIGHT).connect(target.getAnchor(
                        ConstraintAnchor.Type.LEFT), mMarginEnd, mMarginEndGone, false);
            }
            break;
            case END_TO_END: {
                widget.getAnchor(ConstraintAnchor.Type.RIGHT).connect(target.getAnchor(
                        ConstraintAnchor.Type.RIGHT), mMarginEnd, mMarginEndGone, false);
            }
            break;
            case LEFT_TO_LEFT: {
                widget.getAnchor(ConstraintAnchor.Type.LEFT).connect(target.getAnchor(
                        ConstraintAnchor.Type.LEFT), mMarginLeft, mMarginLeftGone, false);
            }
            break;
            case LEFT_TO_RIGHT: {
                widget.getAnchor(ConstraintAnchor.Type.LEFT).connect(target.getAnchor(
                        ConstraintAnchor.Type.RIGHT), mMarginLeft, mMarginLeftGone, false);
            }
            break;
            case RIGHT_TO_LEFT: {
                widget.getAnchor(ConstraintAnchor.Type.RIGHT).connect(target.getAnchor(
                        ConstraintAnchor.Type.LEFT), mMarginRight, mMarginRightGone, false);
            }
            break;
            case RIGHT_TO_RIGHT: {
                widget.getAnchor(ConstraintAnchor.Type.RIGHT).connect(target.getAnchor(
                        ConstraintAnchor.Type.RIGHT), mMarginRight, mMarginRightGone, false);
            }
            break;
            case TOP_TO_TOP: {
                widget.getAnchor(ConstraintAnchor.Type.TOP).connect(target.getAnchor(
                        ConstraintAnchor.Type.TOP), mMarginTop, mMarginTopGone, false);
            }
            break;
            case TOP_TO_BOTTOM: {
                widget.getAnchor(ConstraintAnchor.Type.TOP).connect(target.getAnchor(
                        ConstraintAnchor.Type.BOTTOM), mMarginTop, mMarginTopGone, false);
            }
            break;
            case TOP_TO_BASELINE: {
                widget.immediateConnect(ConstraintAnchor.Type.TOP, target,
                        ConstraintAnchor.Type.BASELINE, mMarginTop, mMarginTopGone);
            }
            break;
            case BOTTOM_TO_TOP: {
                widget.getAnchor(ConstraintAnchor.Type.BOTTOM).connect(target.getAnchor(
                        ConstraintAnchor.Type.TOP), mMarginBottom, mMarginBottomGone, false);
            }
            break;
            case BOTTOM_TO_BOTTOM: {
                widget.getAnchor(ConstraintAnchor.Type.BOTTOM).connect(target.getAnchor(
                        ConstraintAnchor.Type.BOTTOM), mMarginBottom, mMarginBottomGone, false);
            }
            break;
            case BOTTOM_TO_BASELINE: {
                widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target,
                        ConstraintAnchor.Type.BASELINE, mMarginBottom, mMarginBottomGone);
            }
            break;
            case BASELINE_TO_BASELINE: {
                widget.immediateConnect(ConstraintAnchor.Type.BASELINE, target,
                        ConstraintAnchor.Type.BASELINE, mMarginBaseline, mMarginBaselineGone);
            }
            break;
            case BASELINE_TO_TOP: {
                widget.immediateConnect(ConstraintAnchor.Type.BASELINE,
                        target, ConstraintAnchor.Type.TOP, mMarginBaseline, mMarginBaselineGone);
            }
            break;
            case BASELINE_TO_BOTTOM: {
                widget.immediateConnect(ConstraintAnchor.Type.BASELINE, target,
                        ConstraintAnchor.Type.BOTTOM, mMarginBaseline, mMarginBaselineGone);
            }
            break;
            case CIRCULAR_CONSTRAINT: {
                widget.connectCircularConstraint(target, mCircularAngle, (int) mCircularDistance);
            }
            break;
            default:
                break;
        }
    }

    /**
     * apply all the constraints attributes of the mConstraintWidget
     */
    public void applyWidgetConstraints() {
        applyConnection(mConstraintWidget, mLeftToLeft, State.Constraint.LEFT_TO_LEFT);
        applyConnection(mConstraintWidget, mLeftToRight, State.Constraint.LEFT_TO_RIGHT);
        applyConnection(mConstraintWidget, mRightToLeft, State.Constraint.RIGHT_TO_LEFT);
        applyConnection(mConstraintWidget, mRightToRight, State.Constraint.RIGHT_TO_RIGHT);
        applyConnection(mConstraintWidget, mStartToStart, State.Constraint.START_TO_START);
        applyConnection(mConstraintWidget, mStartToEnd, State.Constraint.START_TO_END);
        applyConnection(mConstraintWidget, mEndToStart, State.Constraint.END_TO_START);
        applyConnection(mConstraintWidget, mEndToEnd, State.Constraint.END_TO_END);
        applyConnection(mConstraintWidget, mTopToTop, State.Constraint.TOP_TO_TOP);
        applyConnection(mConstraintWidget, mTopToBottom, State.Constraint.TOP_TO_BOTTOM);
        applyConnection(mConstraintWidget, mTopToBaseline, State.Constraint.TOP_TO_BASELINE);
        applyConnection(mConstraintWidget, mBottomToTop, State.Constraint.BOTTOM_TO_TOP);
        applyConnection(mConstraintWidget, mBottomToBottom, State.Constraint.BOTTOM_TO_BOTTOM);
        applyConnection(mConstraintWidget, mBottomToBaseline, State.Constraint.BOTTOM_TO_BASELINE);
        applyConnection(mConstraintWidget, mBaselineToBaseline,
                State.Constraint.BASELINE_TO_BASELINE);
        applyConnection(mConstraintWidget, mBaselineToTop, State.Constraint.BASELINE_TO_TOP);
        applyConnection(mConstraintWidget, mBaselineToBottom, State.Constraint.BASELINE_TO_BOTTOM);
        applyConnection(mConstraintWidget, mCircularConstraint,
                State.Constraint.CIRCULAR_CONSTRAINT);
    }

    // @TODO: add description
    @Override
    public void apply() {
        if (mConstraintWidget == null) {
            return;
        }
        if (mFacade != null) {
            mFacade.apply();
        }
        mHorizontalDimension.apply(mState, mConstraintWidget, HORIZONTAL);
        mVerticalDimension.apply(mState, mConstraintWidget, VERTICAL);
        dereference();

        applyWidgetConstraints();

        if (mHorizontalChainStyle != ConstraintWidget.CHAIN_SPREAD) {
            mConstraintWidget.setHorizontalChainStyle(mHorizontalChainStyle);
        }
        if (mVerticalChainStyle != ConstraintWidget.CHAIN_SPREAD) {
            mConstraintWidget.setVerticalChainStyle(mVerticalChainStyle);
        }
        if (mHorizontalChainWeight != UNKNOWN) {
            mConstraintWidget.setHorizontalWeight(mHorizontalChainWeight);
        }
        if (mVerticalChainWeight != UNKNOWN) {
            mConstraintWidget.setVerticalWeight(mVerticalChainWeight);
        }

        mConstraintWidget.setHorizontalBiasPercent(mHorizontalBias);
        mConstraintWidget.setVerticalBiasPercent(mVerticalBias);

        mConstraintWidget.frame.pivotX = mPivotX;
        mConstraintWidget.frame.pivotY = mPivotY;
        mConstraintWidget.frame.rotationX = mRotationX;
        mConstraintWidget.frame.rotationY = mRotationY;
        mConstraintWidget.frame.rotationZ = mRotationZ;
        mConstraintWidget.frame.translationX = mTranslationX;
        mConstraintWidget.frame.translationY = mTranslationY;
        mConstraintWidget.frame.translationZ = mTranslationZ;
        mConstraintWidget.frame.scaleX = mScaleX;
        mConstraintWidget.frame.scaleY = mScaleY;
        mConstraintWidget.frame.alpha = mAlpha;
        mConstraintWidget.frame.visibility = mVisibility;
        mConstraintWidget.setVisibility(mVisibility);
        mConstraintWidget.frame.setMotionAttributes(mMotionProperties);
        if (mCustomColors != null) {
            for (String key : mCustomColors.keySet()) {
                Integer color = mCustomColors.get(key);
                mConstraintWidget.frame.setCustomAttribute(key,
                        TypedValues.Custom.TYPE_COLOR, color);
            }
        }
        if (mCustomFloats != null) {
            for (String key : mCustomFloats.keySet()) {
                float value = mCustomFloats.get(key);
                mConstraintWidget.frame.setCustomAttribute(key,
                        TypedValues.Custom.TYPE_FLOAT, value);
            }
        }
    }
}
