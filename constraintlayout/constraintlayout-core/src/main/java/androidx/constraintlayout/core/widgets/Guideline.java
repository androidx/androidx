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

import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;

import androidx.constraintlayout.core.LinearSystem;
import androidx.constraintlayout.core.SolverVariable;

import java.util.HashMap;

/**
 * Guideline
 */
public class Guideline extends ConstraintWidget {
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public static final int RELATIVE_PERCENT = 0;
    public static final int RELATIVE_BEGIN = 1;
    public static final int RELATIVE_END = 2;
    public static final int RELATIVE_UNKNOWN = -1;

    protected float mRelativePercent = -1;
    protected int mRelativeBegin = -1;
    protected int mRelativeEnd = -1;
    protected boolean mGuidelineUseRtl = true;

    private ConstraintAnchor mAnchor = mTop;
    private int mOrientation = HORIZONTAL;
    private int mMinimumPosition = 0;
    private boolean mResolved;

    public Guideline() {
        mAnchors.clear();
        mAnchors.add(mAnchor);
        final int count = mListAnchors.length;
        for (int i = 0; i < count; i++) {
            mListAnchors[i] = mAnchor;
        }
    }

    @Override
    public void copy(ConstraintWidget src, HashMap<ConstraintWidget, ConstraintWidget> map) {
        super.copy(src, map);
        Guideline srcGuideline = (Guideline) src;
        mRelativePercent = srcGuideline.mRelativePercent;
        mRelativeBegin = srcGuideline.mRelativeBegin;
        mRelativeEnd = srcGuideline.mRelativeEnd;
        mGuidelineUseRtl = srcGuideline.mGuidelineUseRtl;
        setOrientation(srcGuideline.mOrientation);
    }

    @Override
    public boolean allowedInBarrier() {
        return true;
    }

    // @TODO: add description
    public int getRelativeBehaviour() {
        if (mRelativePercent != -1) {
            return RELATIVE_PERCENT;
        }
        if (mRelativeBegin != -1) {
            return RELATIVE_BEGIN;
        }
        if (mRelativeEnd != -1) {
            return RELATIVE_END;
        }
        return RELATIVE_UNKNOWN;
    }

    // @TODO: add description
    public void setOrientation(int orientation) {
        if (mOrientation == orientation) {
            return;
        }
        mOrientation = orientation;
        mAnchors.clear();
        if (mOrientation == VERTICAL) {
            mAnchor = mLeft;
        } else {
            mAnchor = mTop;
        }
        mAnchors.add(mAnchor);
        final int count = mListAnchors.length;
        for (int i = 0; i < count; i++) {
            mListAnchors[i] = mAnchor;
        }
    }

    public ConstraintAnchor getAnchor() {
        return mAnchor;
    }

    /**
     * Specify the xml type for the container
     */
    @Override
    public String getType() {
        return "Guideline";
    }

    /**
     * get the orientation VERTICAL or HORIZONTAL
     * @return orientation
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * set the minimum position
     * @param minimum
     */
    public void setMinimumPosition(int minimum) {
        mMinimumPosition = minimum;
    }

    /**
     * Get the Minimum Position
     * @return the Minimum Position
     */
    public int getMinimumPosition() {
        return mMinimumPosition;
    }

    @Override
    public ConstraintAnchor getAnchor(ConstraintAnchor.Type anchorType) {
        switch (anchorType) {
            case LEFT:
            case RIGHT: {
                if (mOrientation == VERTICAL) {
                    return mAnchor;
                }
            }
            break;
            case TOP:
            case BOTTOM: {
                if (mOrientation == HORIZONTAL) {
                    return mAnchor;
                }
            }
            break;
            case BASELINE:
            case CENTER:
            case CENTER_X:
            case CENTER_Y:
            case NONE:
                return null;
        }
        return null;
    }

    // @TODO: add description
    public void setGuidePercent(int value) {
        setGuidePercent(value / 100f);
    }

    // @TODO: add description
    public void setGuidePercent(float value) {
        if (value > -1) {
            mRelativePercent = value;
            mRelativeBegin = -1;
            mRelativeEnd = -1;
        }
    }

    // @TODO: add description
    public void setGuideBegin(int value) {
        if (value > -1) {
            mRelativePercent = -1;
            mRelativeBegin = value;
            mRelativeEnd = -1;
        }
    }

    // @TODO: add description
    public void setGuideEnd(int value) {
        if (value > -1) {
            mRelativePercent = -1;
            mRelativeBegin = -1;
            mRelativeEnd = value;
        }
    }

    public float getRelativePercent() {
        return mRelativePercent;
    }

    public int getRelativeBegin() {
        return mRelativeBegin;
    }

    public int getRelativeEnd() {
        return mRelativeEnd;
    }

    // @TODO: add description
    public void setFinalValue(int position) {
        if (LinearSystem.FULL_DEBUG) {
            System.out.println("*** SET FINAL GUIDELINE VALUE "
                    + position + " FOR " + getDebugName());
        }
        mAnchor.setFinalValue(position);
        mResolved = true;
    }

    @Override
    public boolean isResolvedHorizontally() {
        return mResolved;
    }

    @Override
    public boolean isResolvedVertically() {
        return mResolved;
    }

    @Override
    public void addToSolver(LinearSystem system, boolean optimize) {
        if (LinearSystem.FULL_DEBUG) {
            System.out.println("\n----------------------------------------------");
            System.out.println("-- adding " + getDebugName() + " to the solver");
            System.out.println("----------------------------------------------\n");
        }

        ConstraintWidgetContainer parent = (ConstraintWidgetContainer) getParent();
        if (parent == null) {
            return;
        }
        ConstraintAnchor begin = parent.getAnchor(ConstraintAnchor.Type.LEFT);
        ConstraintAnchor end = parent.getAnchor(ConstraintAnchor.Type.RIGHT);
        boolean parentWrapContent = mParent != null
                ? mParent.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == WRAP_CONTENT : false;
        if (mOrientation == HORIZONTAL) {
            begin = parent.getAnchor(ConstraintAnchor.Type.TOP);
            end = parent.getAnchor(ConstraintAnchor.Type.BOTTOM);
            parentWrapContent = mParent != null
                    ? mParent.mListDimensionBehaviors[DIMENSION_VERTICAL] == WRAP_CONTENT : false;
        }
        if (mResolved && mAnchor.hasFinalValue()) {
            SolverVariable guide = system.createObjectVariable(mAnchor);
            if (LinearSystem.FULL_DEBUG) {
                System.out.println("*** SET FINAL POSITION FOR GUIDELINE "
                        + getDebugName() + " TO " + mAnchor.getFinalValue());
            }
            system.addEquality(guide, mAnchor.getFinalValue());
            if (mRelativeBegin != -1) {
                if (parentWrapContent) {
                    system.addGreaterThan(system.createObjectVariable(end), guide,
                            0, SolverVariable.STRENGTH_EQUALITY);
                }
            } else if (mRelativeEnd != -1) {
                if (parentWrapContent) {
                    SolverVariable parentRight = system.createObjectVariable(end);
                    system.addGreaterThan(guide, system.createObjectVariable(begin),
                            0, SolverVariable.STRENGTH_EQUALITY);
                    system.addGreaterThan(parentRight, guide, 0, SolverVariable.STRENGTH_EQUALITY);
                }
            }
            mResolved = false;
            return;
        }
        if (mRelativeBegin != -1) {
            SolverVariable guide = system.createObjectVariable(mAnchor);
            SolverVariable parentLeft = system.createObjectVariable(begin);
            system.addEquality(guide, parentLeft, mRelativeBegin, SolverVariable.STRENGTH_FIXED);
            if (parentWrapContent) {
                system.addGreaterThan(system.createObjectVariable(end),
                        guide, 0, SolverVariable.STRENGTH_EQUALITY);
            }
        } else if (mRelativeEnd != -1) {
            SolverVariable guide = system.createObjectVariable(mAnchor);
            SolverVariable parentRight = system.createObjectVariable(end);
            system.addEquality(guide, parentRight, -mRelativeEnd, SolverVariable.STRENGTH_FIXED);
            if (parentWrapContent) {
                system.addGreaterThan(guide, system.createObjectVariable(begin),
                        0, SolverVariable.STRENGTH_EQUALITY);
                system.addGreaterThan(parentRight, guide, 0, SolverVariable.STRENGTH_EQUALITY);
            }
        } else if (mRelativePercent != -1) {
            SolverVariable guide = system.createObjectVariable(mAnchor);
            SolverVariable parentRight = system.createObjectVariable(end);
            system.addConstraint(LinearSystem
                    .createRowDimensionPercent(system, guide, parentRight,
                            mRelativePercent));
        }
    }

    @Override
    public void updateFromSolver(LinearSystem system, boolean optimize) {
        if (getParent() == null) {
            return;
        }
        int value = system.getObjectVariableValue(mAnchor);
        if (mOrientation == VERTICAL) {
            setX(value);
            setY(0);
            setHeight(getParent().getHeight());
            setWidth(0);
        } else {
            setX(0);
            setY(value);
            setWidth(getParent().getWidth());
            setHeight(0);
        }
    }

    void inferRelativePercentPosition() {
        float percent = (getX() / (float) getParent().getWidth());
        if (mOrientation == HORIZONTAL) {
            percent = (getY() / (float) getParent().getHeight());
        }
        setGuidePercent(percent);
    }

    void inferRelativeBeginPosition() {
        int position = getX();
        if (mOrientation == HORIZONTAL) {
            position = getY();
        }
        setGuideBegin(position);
    }

    void inferRelativeEndPosition() {
        int position = getParent().getWidth() - getX();
        if (mOrientation == HORIZONTAL) {
            position = getParent().getHeight() - getY();
        }
        setGuideEnd(position);
    }

    // @TODO: add description
    public void cyclePosition() {
        if (mRelativeBegin != -1) {
            // cycle to percent-based position
            inferRelativePercentPosition();
        } else if (mRelativePercent != -1) {
            // cycle to end-based position
            inferRelativeEndPosition();
        } else if (mRelativeEnd != -1) {
            // cycle to begin-based position
            inferRelativeBeginPosition();
        }
    }

    public boolean isPercent() {
        return mRelativePercent != -1 && mRelativeBegin == -1 && mRelativeEnd == -1;
    }
}
