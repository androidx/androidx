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

package androidx.constraintlayout.core.widgets;

import androidx.constraintlayout.core.LinearSystem;
import androidx.constraintlayout.core.SolverVariable;

import java.util.HashMap;

/**
 * A Barrier takes multiple widgets
 */
public class Barrier extends HelperWidget {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int TOP = 2;
    public static final int BOTTOM = 3;
    private static final boolean USE_RESOLUTION = true;
    private static final boolean USE_RELAX_GONE = false;

    private int mBarrierType = LEFT;

    private boolean mAllowsGoneWidget = true;
    private int mMargin = 0;
    boolean mResolved = false;

    public Barrier() {
    }

    public Barrier(String debugName) {
        setDebugName(debugName);
    }

    @Override
    public boolean allowedInBarrier() {
        return true;
    }

    public int getBarrierType() {
        return mBarrierType;
    }

    public void setBarrierType(int barrierType) {
        mBarrierType = barrierType;
    }

    public void setAllowsGoneWidget(boolean allowsGoneWidget) {
        mAllowsGoneWidget = allowsGoneWidget;
    }

    /**
     * Find if this barrier supports gone widgets.
     *
     * @return true if this barrier supports gone widgets, otherwise false
     * @deprecated This method should be called {@code getAllowsGoneWidget}
     * such that {@code allowsGoneWidget}
     * can be accessed as a property from Kotlin; {@see https://android.github
     * .io/kotlin-guides/interop.html#property-prefixes}.
     * Use {@link #getAllowsGoneWidget()} instead.
     */
    @Deprecated
    public boolean allowsGoneWidget() {
        return mAllowsGoneWidget;
    }

    /**
     * Find if this barrier supports gone widgets.
     *
     * @return true if this barrier supports gone widgets, otherwise false
     */
    public boolean getAllowsGoneWidget() {
        return mAllowsGoneWidget;
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
    public void copy(ConstraintWidget src, HashMap<ConstraintWidget, ConstraintWidget> map) {
        super.copy(src, map);
        Barrier srcBarrier = (Barrier) src;
        mBarrierType = srcBarrier.mBarrierType;
        mAllowsGoneWidget = srcBarrier.mAllowsGoneWidget;
        mMargin = srcBarrier.mMargin;
    }

    @Override
    public String toString() {
        String debug = "[Barrier] " + getDebugName() + " {";
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (i > 0) {
                debug += ", ";
            }
            debug += widget.getDebugName();
        }
        debug += "}";
        return debug;
    }

    protected void markWidgets() {
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (!mAllowsGoneWidget && !widget.allowedInBarrier()) {
                continue;
            }
            if (mBarrierType == LEFT || mBarrierType == RIGHT) {
                widget.setInBarrier(HORIZONTAL, true);
            } else if (mBarrierType == TOP || mBarrierType == BOTTOM) {
                widget.setInBarrier(VERTICAL, true);
            }
        }
    }

    /**
     * Add this widget to the solver
     *
     * @param system   the solver we want to add the widget to
     * @param optimize true if {@link Optimizer#OPTIMIZATION_GRAPH} is on
     */
    @Override
    public void addToSolver(LinearSystem system, boolean optimize) {
        if (LinearSystem.FULL_DEBUG) {
            System.out.println("\n----------------------------------------------");
            System.out.println("-- adding " + getDebugName() + " to the solver");
            System.out.println("----------------------------------------------\n");
        }

        ConstraintAnchor position;
        mListAnchors[LEFT] = mLeft;
        mListAnchors[TOP] = mTop;
        mListAnchors[RIGHT] = mRight;
        mListAnchors[BOTTOM] = mBottom;
        for (int i = 0; i < mListAnchors.length; i++) {
            mListAnchors[i].mSolverVariable = system.createObjectVariable(mListAnchors[i]);
        }
        if (mBarrierType >= 0 && mBarrierType < 4) {
            position = mListAnchors[mBarrierType];
        } else {
            return;
        }

        if (USE_RESOLUTION) {
            if (!mResolved) {
                allSolved();
            }
            if (mResolved) {
                mResolved = false;
                if (mBarrierType == LEFT || mBarrierType == RIGHT) {
                    system.addEquality(mLeft.mSolverVariable, mX);
                    system.addEquality(mRight.mSolverVariable, mX);
                } else if (mBarrierType == TOP || mBarrierType == BOTTOM) {
                    system.addEquality(mTop.mSolverVariable, mY);
                    system.addEquality(mBottom.mSolverVariable, mY);
                }
                return;
            }
        }

        // We have to handle the case where some of the elements
        //  referenced in the barrier are set as
        // match_constraint; we have to take it in account to set the strength of the barrier.
        boolean hasMatchConstraintWidgets = false;
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (!mAllowsGoneWidget && !widget.allowedInBarrier()) {
                continue;
            }
            if ((mBarrierType == LEFT || mBarrierType == RIGHT)
                    && (widget.getHorizontalDimensionBehaviour()
                    == DimensionBehaviour.MATCH_CONSTRAINT)
                    && widget.mLeft.mTarget != null && widget.mRight.mTarget != null) {
                hasMatchConstraintWidgets = true;
                break;
            } else if ((mBarrierType == TOP || mBarrierType == BOTTOM)
                    && (widget.getVerticalDimensionBehaviour()
                    == DimensionBehaviour.MATCH_CONSTRAINT)
                    && widget.mTop.mTarget != null && widget.mBottom.mTarget != null) {
                hasMatchConstraintWidgets = true;
                break;
            }
        }

        boolean mHasHorizontalCenteredDependents =
                mLeft.hasCenteredDependents() || mRight.hasCenteredDependents();
        boolean mHasVerticalCenteredDependents =
                mTop.hasCenteredDependents() || mBottom.hasCenteredDependents();
        boolean applyEqualityOnReferences = !hasMatchConstraintWidgets
                && ((mBarrierType == LEFT && mHasHorizontalCenteredDependents)
                || (mBarrierType == TOP && mHasVerticalCenteredDependents)
                || (mBarrierType == RIGHT && mHasHorizontalCenteredDependents)
                || (mBarrierType == BOTTOM && mHasVerticalCenteredDependents));

        int equalityOnReferencesStrength = SolverVariable.STRENGTH_EQUALITY;
        if (!applyEqualityOnReferences) {
            equalityOnReferencesStrength = SolverVariable.STRENGTH_HIGHEST;
        }
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (!mAllowsGoneWidget && !widget.allowedInBarrier()) {
                continue;
            }
            SolverVariable target = system.createObjectVariable(widget.mListAnchors[mBarrierType]);
            widget.mListAnchors[mBarrierType].mSolverVariable = target;
            int margin = 0;
            if (widget.mListAnchors[mBarrierType].mTarget != null
                    && widget.mListAnchors[mBarrierType].mTarget.mOwner == this) {
                margin += widget.mListAnchors[mBarrierType].mMargin;
            }
            if (mBarrierType == LEFT || mBarrierType == TOP) {
                system.addLowerBarrier(position.mSolverVariable, target,
                        mMargin - margin, hasMatchConstraintWidgets);
            } else {
                system.addGreaterBarrier(position.mSolverVariable, target,
                        mMargin + margin, hasMatchConstraintWidgets);
            }
            if (USE_RELAX_GONE) {
                if (widget.getVisibility() != GONE
                        || widget instanceof Guideline || widget instanceof Barrier) {
                    system.addEquality(position.mSolverVariable, target,
                            mMargin + margin, equalityOnReferencesStrength);
                }
            } else {
                system.addEquality(position.mSolverVariable, target,
                        mMargin + margin, equalityOnReferencesStrength);
            }
        }

        int barrierParentStrength = SolverVariable.STRENGTH_HIGHEST;
        int barrierParentStrengthOpposite = SolverVariable.STRENGTH_NONE;

        if (mBarrierType == LEFT) {
            system.addEquality(mRight.mSolverVariable,
                    mLeft.mSolverVariable, 0, SolverVariable.STRENGTH_FIXED);
            system.addEquality(mLeft.mSolverVariable,
                    mParent.mRight.mSolverVariable, 0, barrierParentStrength);
            system.addEquality(mLeft.mSolverVariable,
                    mParent.mLeft.mSolverVariable, 0, barrierParentStrengthOpposite);
        } else if (mBarrierType == RIGHT) {
            system.addEquality(mLeft.mSolverVariable,
                    mRight.mSolverVariable, 0, SolverVariable.STRENGTH_FIXED);
            system.addEquality(mLeft.mSolverVariable,
                    mParent.mLeft.mSolverVariable, 0, barrierParentStrength);
            system.addEquality(mLeft.mSolverVariable,
                    mParent.mRight.mSolverVariable, 0, barrierParentStrengthOpposite);
        } else if (mBarrierType == TOP) {
            system.addEquality(mBottom.mSolverVariable,
                    mTop.mSolverVariable, 0, SolverVariable.STRENGTH_FIXED);
            system.addEquality(mTop.mSolverVariable,
                    mParent.mBottom.mSolverVariable, 0, barrierParentStrength);
            system.addEquality(mTop.mSolverVariable,
                    mParent.mTop.mSolverVariable, 0, barrierParentStrengthOpposite);
        } else if (mBarrierType == BOTTOM) {
            system.addEquality(mTop.mSolverVariable,
                    mBottom.mSolverVariable, 0, SolverVariable.STRENGTH_FIXED);
            system.addEquality(mTop.mSolverVariable,
                    mParent.mTop.mSolverVariable, 0, barrierParentStrength);
            system.addEquality(mTop.mSolverVariable,
                    mParent.mBottom.mSolverVariable, 0, barrierParentStrengthOpposite);
        }
    }

    public void setMargin(int margin) {
        mMargin = margin;
    }

    public int getMargin() {
        return mMargin;
    }

    // @TODO: add description
    public int getOrientation() {
        switch (mBarrierType) {
            case LEFT:
            case RIGHT:
                return HORIZONTAL;
            case TOP:
            case BOTTOM:
                return VERTICAL;
        }
        return UNKNOWN;
    }

    // @TODO: add description
    public boolean allSolved() {
        if (!USE_RESOLUTION) {
            return false;
        }
        boolean hasAllWidgetsResolved = true;
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (!mAllowsGoneWidget && !widget.allowedInBarrier()) {
                continue;
            }
            if ((mBarrierType == LEFT || mBarrierType == RIGHT)
                    && !widget.isResolvedHorizontally()) {
                hasAllWidgetsResolved = false;
            } else if ((mBarrierType == TOP || mBarrierType == BOTTOM)
                    && !widget.isResolvedVertically()) {
                hasAllWidgetsResolved = false;
            }
        }

        if (hasAllWidgetsResolved && mWidgetsCount > 0) {
            // we're done!
            int barrierPosition = 0;
            boolean initialized = false;
            for (int i = 0; i < mWidgetsCount; i++) {
                ConstraintWidget widget = mWidgets[i];
                if (!mAllowsGoneWidget && !widget.allowedInBarrier()) {
                    continue;
                }
                if (!initialized) {
                    if (mBarrierType == LEFT) {
                        barrierPosition =
                                widget.getAnchor(ConstraintAnchor.Type.LEFT).getFinalValue();
                    } else if (mBarrierType == RIGHT) {
                        barrierPosition =
                                widget.getAnchor(ConstraintAnchor.Type.RIGHT).getFinalValue();
                    } else if (mBarrierType == TOP) {
                        barrierPosition =
                                widget.getAnchor(ConstraintAnchor.Type.TOP).getFinalValue();
                    } else if (mBarrierType == BOTTOM) {
                        barrierPosition =
                                widget.getAnchor(ConstraintAnchor.Type.BOTTOM).getFinalValue();
                    }
                    initialized = true;
                }
                if (mBarrierType == LEFT) {
                    barrierPosition = Math.min(barrierPosition,
                            widget.getAnchor(ConstraintAnchor.Type.LEFT).getFinalValue());
                } else if (mBarrierType == RIGHT) {
                    barrierPosition = Math.max(barrierPosition,
                            widget.getAnchor(ConstraintAnchor.Type.RIGHT).getFinalValue());
                } else if (mBarrierType == TOP) {
                    barrierPosition = Math.min(barrierPosition,
                            widget.getAnchor(ConstraintAnchor.Type.TOP).getFinalValue());
                } else if (mBarrierType == BOTTOM) {
                    barrierPosition = Math.max(barrierPosition,
                            widget.getAnchor(ConstraintAnchor.Type.BOTTOM).getFinalValue());
                }
            }
            barrierPosition += mMargin;
            if (mBarrierType == LEFT || mBarrierType == RIGHT) {
                setFinalHorizontal(barrierPosition, barrierPosition);
            } else {
                setFinalVertical(barrierPosition, barrierPosition);
            }
            if (LinearSystem.FULL_DEBUG) {
                System.out.println("*** BARRIER " + getDebugName()
                        + " SOLVED TO " + barrierPosition + " ***");
            }
            mResolved = true;
            return true;
        }
        return false;
    }
}
