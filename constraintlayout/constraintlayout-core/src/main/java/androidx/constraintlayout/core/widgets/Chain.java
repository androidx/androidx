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

import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.GONE;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD;

import androidx.constraintlayout.core.ArrayRow;
import androidx.constraintlayout.core.LinearSystem;
import androidx.constraintlayout.core.SolverVariable;
import androidx.constraintlayout.core.widgets.analyzer.Direct;

import java.util.ArrayList;

/**
 * Chain management and constraints creation
 */
public class Chain {

    private static final boolean DEBUG = false;
    public static final boolean USE_CHAIN_OPTIMIZATION = false;

    /**
     * Apply specific rules for dealing with chains of widgets.
     * Chains are defined as a list of widget linked together with bi-directional connections
     *
     * @param constraintWidgetContainer root container
     * @param system                    the linear system we add the equations to
     * @param orientation               HORIZONTAL or VERTICAL
     */
    public static void applyChainConstraints(
            ConstraintWidgetContainer constraintWidgetContainer,
            LinearSystem system,
            ArrayList<ConstraintWidget> widgets,
            int orientation) {
        // what to do:
        // Don't skip things. Either the element is GONE or not.
        int offset = 0;
        int chainsSize = 0;
        ChainHead[] chainsArray = null;
        if (orientation == ConstraintWidget.HORIZONTAL) {
            offset = 0;
            chainsSize = constraintWidgetContainer.mHorizontalChainsSize;
            chainsArray = constraintWidgetContainer.mHorizontalChainsArray;
        } else {
            offset = 2;
            chainsSize = constraintWidgetContainer.mVerticalChainsSize;
            chainsArray = constraintWidgetContainer.mVerticalChainsArray;
        }

        for (int i = 0; i < chainsSize; i++) {
            ChainHead first = chainsArray[i];
            // we have to make sure we define the ChainHead here,
            // otherwise the values we use may not be correctly initialized
            // (as we initialize them in the ConstraintWidget.addToSolver())
            first.define();
            if (widgets == null || widgets.contains(first.mFirst)) {
                applyChainConstraints(constraintWidgetContainer,
                        system, orientation, offset, first);
            }
        }
    }

    /**
     * Apply specific rules for dealing with chains of widgets.
     * Chains are defined as a list of widget linked together with bi-directional connections
     *
     * @param container   the root container
     * @param system      the linear system we add the equations to
     * @param orientation HORIZONTAL or VERTICAL
     * @param offset      0 or 2 to accommodate for HORIZONTAL / VERTICAL
     * @param chainHead   a chain represented by its main elements
     */
    static void applyChainConstraints(ConstraintWidgetContainer container, LinearSystem system,
            int orientation, int offset, ChainHead chainHead) {
        ConstraintWidget first = chainHead.mFirst;
        ConstraintWidget last = chainHead.mLast;
        ConstraintWidget firstVisibleWidget = chainHead.mFirstVisibleWidget;
        ConstraintWidget lastVisibleWidget = chainHead.mLastVisibleWidget;
        ConstraintWidget head = chainHead.mHead;

        ConstraintWidget widget = first;
        ConstraintWidget next = null;
        boolean done = false;

        float totalWeights = chainHead.mTotalWeight;
        @SuppressWarnings("unused") ConstraintWidget firstMatchConstraintsWidget =
                chainHead.mFirstMatchConstraintWidget;
        @SuppressWarnings("unused") ConstraintWidget previousMatchConstraintsWidget =
                chainHead.mLastMatchConstraintWidget;

        boolean isWrapContent = container.mListDimensionBehaviors[orientation]
                == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
        boolean isChainSpread = false;
        boolean isChainSpreadInside = false;
        boolean isChainPacked = false;

        if (orientation == ConstraintWidget.HORIZONTAL) {
            isChainSpread = head.mHorizontalChainStyle == ConstraintWidget.CHAIN_SPREAD;
            isChainSpreadInside =
                    head.mHorizontalChainStyle == ConstraintWidget.CHAIN_SPREAD_INSIDE;
            isChainPacked = head.mHorizontalChainStyle == ConstraintWidget.CHAIN_PACKED;
        } else {
            isChainSpread = head.mVerticalChainStyle == ConstraintWidget.CHAIN_SPREAD;
            isChainSpreadInside = head.mVerticalChainStyle == ConstraintWidget.CHAIN_SPREAD_INSIDE;
            isChainPacked = head.mVerticalChainStyle == ConstraintWidget.CHAIN_PACKED;
        }

        if (USE_CHAIN_OPTIMIZATION && !isWrapContent
                && Direct.solveChain(container, system, orientation, offset, chainHead,
                isChainSpread, isChainSpreadInside, isChainPacked)) {
            if (LinearSystem.FULL_DEBUG) {
                System.out.println("### CHAIN FULLY SOLVED! ###");
            }
            return; // done with the chain!
        } else if (LinearSystem.FULL_DEBUG) {
            System.out.println("### CHAIN WASN'T SOLVED DIRECTLY... ###");
        }

        // This traversal will:
        // - set up some basic ordering constraints
        // - build a linked list of matched constraints widgets
        while (!done) {
            ConstraintAnchor begin = widget.mListAnchors[offset];

            int strength = SolverVariable.STRENGTH_HIGHEST;
            if (isChainPacked) {
                strength = SolverVariable.STRENGTH_LOW;
            }
            int margin = begin.getMargin();
            boolean isSpreadOnly = widget.mListDimensionBehaviors[orientation]
                    == DimensionBehaviour.MATCH_CONSTRAINT
                    && widget.mResolvedMatchConstraintDefault[orientation]
                    == MATCH_CONSTRAINT_SPREAD;

            if (begin.mTarget != null && widget != first) {
                margin += begin.mTarget.getMargin();
            }

            if (isChainPacked && widget != first && widget != firstVisibleWidget) {
                strength = SolverVariable.STRENGTH_FIXED;
            }

            if (begin.mTarget != null) {
                if (widget == firstVisibleWidget) {
                    system.addGreaterThan(begin.mSolverVariable, begin.mTarget.mSolverVariable,
                            margin, SolverVariable.STRENGTH_BARRIER);
                } else {
                    system.addGreaterThan(begin.mSolverVariable, begin.mTarget.mSolverVariable,
                            margin, SolverVariable.STRENGTH_FIXED);
                }
                if (isSpreadOnly && !isChainPacked) {
                    strength = SolverVariable.STRENGTH_EQUALITY;
                }
                if (widget == firstVisibleWidget && isChainPacked
                        && widget.isInBarrier(orientation)) {
                    strength = SolverVariable.STRENGTH_EQUALITY;
                }
                system.addEquality(begin.mSolverVariable, begin.mTarget.mSolverVariable, margin,
                        strength);
            }

            if (isWrapContent) {
                if (widget.getVisibility() != ConstraintWidget.GONE
                        && widget.mListDimensionBehaviors[orientation]
                        == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    system.addGreaterThan(widget.mListAnchors[offset + 1].mSolverVariable,
                            widget.mListAnchors[offset].mSolverVariable, 0,
                            SolverVariable.STRENGTH_EQUALITY);
                }
                system.addGreaterThan(widget.mListAnchors[offset].mSolverVariable,
                        container.mListAnchors[offset].mSolverVariable,
                        0, SolverVariable.STRENGTH_FIXED);
            }

            // go to the next widget
            ConstraintAnchor nextAnchor = widget.mListAnchors[offset + 1].mTarget;
            if (nextAnchor != null) {
                next = nextAnchor.mOwner;
                if (next.mListAnchors[offset].mTarget == null
                        || next.mListAnchors[offset].mTarget.mOwner != widget) {
                    next = null;
                }
            } else {
                next = null;
            }
            if (next != null) {
                widget = next;
            } else {
                done = true;
            }
        }

        // Make sure we have constraints for the last anchors / targets
        if (lastVisibleWidget != null && last.mListAnchors[offset + 1].mTarget != null) {
            ConstraintAnchor end = lastVisibleWidget.mListAnchors[offset + 1];
            boolean isSpreadOnly = lastVisibleWidget.mListDimensionBehaviors[orientation]
                    == DimensionBehaviour.MATCH_CONSTRAINT
                    && lastVisibleWidget.mResolvedMatchConstraintDefault[orientation]
                    == MATCH_CONSTRAINT_SPREAD;
            if (isSpreadOnly && !isChainPacked && end.mTarget.mOwner == container) {
                system.addEquality(end.mSolverVariable, end.mTarget.mSolverVariable,
                        -end.getMargin(), SolverVariable.STRENGTH_EQUALITY);
            } else if (isChainPacked && end.mTarget.mOwner == container) {
                system.addEquality(end.mSolverVariable, end.mTarget.mSolverVariable,
                        -end.getMargin(), SolverVariable.STRENGTH_HIGHEST);
            }
            system.addLowerThan(end.mSolverVariable,
                    last.mListAnchors[offset + 1].mTarget.mSolverVariable, -end.getMargin(),
                    SolverVariable.STRENGTH_BARRIER);
        }

        // ... and make sure the root end is constrained in wrap content.
        if (isWrapContent) {
            system.addGreaterThan(container.mListAnchors[offset + 1].mSolverVariable,
                    last.mListAnchors[offset + 1].mSolverVariable,
                    last.mListAnchors[offset + 1].getMargin(), SolverVariable.STRENGTH_FIXED);
        }

        // Now, let's apply the centering / spreading for matched constraints widgets
        ArrayList<ConstraintWidget> listMatchConstraints =
                chainHead.mWeightedMatchConstraintsWidgets;
        if (listMatchConstraints != null) {
            final int count = listMatchConstraints.size();
            if (count > 1) {
                ConstraintWidget lastMatch = null;
                float lastWeight = 0;

                if (chainHead.mHasUndefinedWeights && !chainHead.mHasComplexMatchWeights) {
                    totalWeights = chainHead.mWidgetsMatchCount;
                }

                for (int i = 0; i < count; i++) {
                    ConstraintWidget match = listMatchConstraints.get(i);
                    float currentWeight = match.mWeight[orientation];

                    if (currentWeight < 0) {
                        if (chainHead.mHasComplexMatchWeights) {
                            system.addEquality(match.mListAnchors[offset + 1].mSolverVariable,
                                    match.mListAnchors[offset].mSolverVariable,
                                    0, SolverVariable.STRENGTH_HIGHEST);
                            continue;
                        }
                        currentWeight = 1;
                    }
                    if (currentWeight == 0) {
                        system.addEquality(match.mListAnchors[offset + 1].mSolverVariable,
                                match.mListAnchors[offset].mSolverVariable,
                                0, SolverVariable.STRENGTH_FIXED);
                        continue;
                    }

                    if (lastMatch != null) {
                        SolverVariable begin = lastMatch.mListAnchors[offset].mSolverVariable;
                        SolverVariable end = lastMatch.mListAnchors[offset + 1].mSolverVariable;
                        SolverVariable nextBegin = match.mListAnchors[offset].mSolverVariable;
                        SolverVariable nextEnd = match.mListAnchors[offset + 1].mSolverVariable;
                        ArrayRow row = system.createRow();
                        row.createRowEqualMatchDimensions(lastWeight, totalWeights, currentWeight,
                                begin, end, nextBegin, nextEnd);
                        system.addConstraint(row);
                    }

                    lastMatch = match;
                    lastWeight = currentWeight;
                }
            }
        }

        if (DEBUG) {
            widget = firstVisibleWidget;
            while (widget != null) {
                next = widget.mNextChainWidget[orientation];
                widget.mListAnchors[offset].mSolverVariable
                        .setName("" + widget.getDebugName() + ".left");
                widget.mListAnchors[offset + 1].mSolverVariable
                        .setName("" + widget.getDebugName() + ".right");
                widget = next;
            }
        }

        // Finally, let's apply the specific rules dealing with the different chain types

        if (firstVisibleWidget != null
                && (firstVisibleWidget == lastVisibleWidget || isChainPacked)) {
            ConstraintAnchor begin = first.mListAnchors[offset];
            ConstraintAnchor end = last.mListAnchors[offset + 1];
            SolverVariable beginTarget = begin.mTarget != null
                    ? begin.mTarget.mSolverVariable : null;
            SolverVariable endTarget = end.mTarget != null ? end.mTarget.mSolverVariable : null;
            begin = firstVisibleWidget.mListAnchors[offset];
            if (lastVisibleWidget != null) {
                end = lastVisibleWidget.mListAnchors[offset + 1];
            }
            if (beginTarget != null && endTarget != null) {
                float bias = 0.5f;
                if (orientation == ConstraintWidget.HORIZONTAL) {
                    bias = head.mHorizontalBiasPercent;
                } else {
                    bias = head.mVerticalBiasPercent;
                }
                int beginMargin = begin.getMargin();
                int endMargin = end.getMargin();
                system.addCentering(begin.mSolverVariable, beginTarget,
                        beginMargin, bias, endTarget, end.mSolverVariable,
                        endMargin, SolverVariable.STRENGTH_CENTERING);
            }
        } else if (isChainSpread && firstVisibleWidget != null) {
            // for chain spread, we need to add equal dimensions in between *visible* widgets
            widget = firstVisibleWidget;
            ConstraintWidget previousVisibleWidget = firstVisibleWidget;
            boolean applyFixedEquality = chainHead.mWidgetsMatchCount > 0
                    && (chainHead.mWidgetsCount == chainHead.mWidgetsMatchCount);
            while (widget != null) {
                next = widget.mNextChainWidget[orientation];
                while (next != null && next.getVisibility() == GONE) {
                    next = next.mNextChainWidget[orientation];
                }
                if (next != null || widget == lastVisibleWidget) {
                    ConstraintAnchor beginAnchor = widget.mListAnchors[offset];
                    SolverVariable begin = beginAnchor.mSolverVariable;
                    SolverVariable beginTarget = beginAnchor.mTarget != null
                            ? beginAnchor.mTarget.mSolverVariable : null;
                    if (previousVisibleWidget != widget) {
                        beginTarget =
                                previousVisibleWidget.mListAnchors[offset + 1].mSolverVariable;
                    } else if (widget == firstVisibleWidget) {
                        beginTarget = first.mListAnchors[offset].mTarget != null
                                ? first.mListAnchors[offset].mTarget.mSolverVariable : null;
                    }

                    ConstraintAnchor beginNextAnchor = null;
                    SolverVariable beginNext = null;
                    @SuppressWarnings("unused") SolverVariable beginNextTarget = null;
                    int beginMargin = beginAnchor.getMargin();
                    int nextMargin = widget.mListAnchors[offset + 1].getMargin();

                    if (next != null) {
                        beginNextAnchor = next.mListAnchors[offset];
                        beginNext = beginNextAnchor.mSolverVariable;
                    } else {
                        beginNextAnchor = last.mListAnchors[offset + 1].mTarget;
                        if (beginNextAnchor != null) {
                            beginNext = beginNextAnchor.mSolverVariable;
                        }
                    }
                    beginNextTarget = widget.mListAnchors[offset + 1].mSolverVariable;

                    if (beginNextAnchor != null) {
                        nextMargin += beginNextAnchor.getMargin();
                    }
                    beginMargin += previousVisibleWidget.mListAnchors[offset + 1].getMargin();
                    if (begin != null && beginTarget != null
                            && beginNext != null && beginNextTarget != null) {
                        int margin1 = beginMargin;
                        if (widget == firstVisibleWidget) {
                            margin1 = firstVisibleWidget.mListAnchors[offset].getMargin();
                        }
                        int margin2 = nextMargin;
                        if (widget == lastVisibleWidget) {
                            margin2 = lastVisibleWidget.mListAnchors[offset + 1].getMargin();
                        }
                        int strength = SolverVariable.STRENGTH_EQUALITY;
                        if (applyFixedEquality) {
                            strength = SolverVariable.STRENGTH_FIXED;
                        }
                        system.addCentering(begin, beginTarget, margin1, 0.5f,
                                beginNext, beginNextTarget, margin2,
                                strength);
                    }
                }
                if (widget.getVisibility() != GONE) {
                    previousVisibleWidget = widget;
                }
                widget = next;
            }
        } else if (isChainSpreadInside && firstVisibleWidget != null) {
            // for chain spread inside, we need to add equal dimensions in between *visible* widgets
            widget = firstVisibleWidget;
            ConstraintWidget previousVisibleWidget = firstVisibleWidget;
            boolean applyFixedEquality = chainHead.mWidgetsMatchCount > 0
                    && (chainHead.mWidgetsCount == chainHead.mWidgetsMatchCount);
            while (widget != null) {
                next = widget.mNextChainWidget[orientation];
                while (next != null && next.getVisibility() == GONE) {
                    next = next.mNextChainWidget[orientation];
                }
                if (widget != firstVisibleWidget && widget != lastVisibleWidget && next != null) {
                    if (next == lastVisibleWidget) {
                        next = null;
                    }
                    ConstraintAnchor beginAnchor = widget.mListAnchors[offset];
                    SolverVariable begin = beginAnchor.mSolverVariable;
                    @SuppressWarnings("unused") SolverVariable beginTarget =
                            beginAnchor.mTarget != null
                            ? beginAnchor.mTarget.mSolverVariable : null;
                    beginTarget = previousVisibleWidget.mListAnchors[offset + 1].mSolverVariable;
                    ConstraintAnchor beginNextAnchor = null;
                    SolverVariable beginNext = null;
                    SolverVariable beginNextTarget = null;
                    int beginMargin = beginAnchor.getMargin();
                    int nextMargin = widget.mListAnchors[offset + 1].getMargin();

                    if (next != null) {
                        beginNextAnchor = next.mListAnchors[offset];
                        beginNext = beginNextAnchor.mSolverVariable;
                        beginNextTarget = beginNextAnchor.mTarget != null
                                ? beginNextAnchor.mTarget.mSolverVariable : null;
                    } else {
                        beginNextAnchor = lastVisibleWidget.mListAnchors[offset];
                        if (beginNextAnchor != null) {
                            beginNext = beginNextAnchor.mSolverVariable;
                        }
                        beginNextTarget = widget.mListAnchors[offset + 1].mSolverVariable;
                    }

                    if (beginNextAnchor != null) {
                        nextMargin += beginNextAnchor.getMargin();
                    }
                    beginMargin += previousVisibleWidget.mListAnchors[offset + 1].getMargin();
                    int strength = SolverVariable.STRENGTH_HIGHEST;
                    if (applyFixedEquality) {
                        strength = SolverVariable.STRENGTH_FIXED;
                    }
                    if (begin != null && beginTarget != null
                            && beginNext != null && beginNextTarget != null) {
                        system.addCentering(begin, beginTarget, beginMargin, 0.5f,
                                beginNext, beginNextTarget, nextMargin,
                                strength);
                    }
                }
                if (widget.getVisibility() != GONE) {
                    previousVisibleWidget = widget;
                }
                widget = next;
            }
            ConstraintAnchor begin = firstVisibleWidget.mListAnchors[offset];
            ConstraintAnchor beginTarget = first.mListAnchors[offset].mTarget;
            ConstraintAnchor end = lastVisibleWidget.mListAnchors[offset + 1];
            ConstraintAnchor endTarget = last.mListAnchors[offset + 1].mTarget;
            int endPointsStrength = SolverVariable.STRENGTH_EQUALITY;
            if (beginTarget != null) {
                if (firstVisibleWidget != lastVisibleWidget) {
                    system.addEquality(begin.mSolverVariable, beginTarget.mSolverVariable,
                            begin.getMargin(), endPointsStrength);
                } else if (endTarget != null) {
                    system.addCentering(begin.mSolverVariable, beginTarget.mSolverVariable,
                            begin.getMargin(), 0.5f, end.mSolverVariable, endTarget.mSolverVariable,
                            end.getMargin(), endPointsStrength);
                }
            }
            if (endTarget != null && (firstVisibleWidget != lastVisibleWidget)) {
                system.addEquality(end.mSolverVariable,
                        endTarget.mSolverVariable, -end.getMargin(), endPointsStrength);
            }

        }

        // final centering, necessary if the chain is larger than the available space...
        if ((isChainSpread || isChainSpreadInside) && firstVisibleWidget
                != null && firstVisibleWidget != lastVisibleWidget) {
            ConstraintAnchor begin = firstVisibleWidget.mListAnchors[offset];
            if (lastVisibleWidget == null) {
                lastVisibleWidget = firstVisibleWidget;
            }
            ConstraintAnchor end = lastVisibleWidget.mListAnchors[offset + 1];
            SolverVariable beginTarget =
                    begin.mTarget != null ? begin.mTarget.mSolverVariable : null;
            SolverVariable endTarget = end.mTarget != null ? end.mTarget.mSolverVariable : null;
            if (last != lastVisibleWidget) {
                ConstraintAnchor realEnd = last.mListAnchors[offset + 1];
                endTarget = realEnd.mTarget != null ? realEnd.mTarget.mSolverVariable : null;
            }
            if (firstVisibleWidget == lastVisibleWidget) {
                begin = firstVisibleWidget.mListAnchors[offset];
                end = firstVisibleWidget.mListAnchors[offset + 1];
            }
            if (beginTarget != null && endTarget != null) {
                float bias = 0.5f;
                int beginMargin = begin.getMargin();
                int endMargin = lastVisibleWidget.mListAnchors[offset + 1].getMargin();
                system.addCentering(begin.mSolverVariable, beginTarget, beginMargin,
                        bias, endTarget, end.mSolverVariable, endMargin,
                        SolverVariable.STRENGTH_EQUALITY);
            }
        }
    }
}
