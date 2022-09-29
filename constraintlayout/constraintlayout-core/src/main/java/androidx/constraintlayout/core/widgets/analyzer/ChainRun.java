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

package androidx.constraintlayout.core.widgets.analyzer;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.GONE;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;

import java.util.ArrayList;

public class ChainRun extends WidgetRun {
    ArrayList<WidgetRun> mWidgets = new ArrayList<>();
    private int mChainStyle;

    public ChainRun(ConstraintWidget widget, int orientation) {
        super(widget);
        this.orientation = orientation;
        build();
    }

    @Override
    public String toString() {
        StringBuilder log = new StringBuilder("ChainRun ");
        log.append((orientation == HORIZONTAL ? "horizontal : " : "vertical : "));
        for (WidgetRun run : mWidgets) {
            log.append("<");
            log.append(run);
            log.append("> ");
        }
        return log.toString();
    }

    @Override
    boolean supportsWrapComputation() {
        final int count = mWidgets.size();
        for (int i = 0; i < count; i++) {
            WidgetRun run = mWidgets.get(i);
            if (!run.supportsWrapComputation()) {
                return false;
            }
        }
        return true;
    }

    // @TODO: add description
    @Override
    public long getWrapDimension() {
        final int count = mWidgets.size();
        long wrapDimension = 0;
        for (int i = 0; i < count; i++) {
            WidgetRun run = mWidgets.get(i);
            wrapDimension += run.start.mMargin;
            wrapDimension += run.getWrapDimension();
            wrapDimension += run.end.mMargin;
        }
        return wrapDimension;
    }

    private void build() {
        ConstraintWidget current = mWidget;
        ConstraintWidget previous = current.getPreviousChainMember(orientation);
        while (previous != null) {
            current = previous;
            previous = current.getPreviousChainMember(orientation);
        }
        mWidget = current; // first element of the chain
        mWidgets.add(current.getRun(orientation));
        ConstraintWidget next = current.getNextChainMember(orientation);
        while (next != null) {
            current = next;
            mWidgets.add(current.getRun(orientation));
            next = current.getNextChainMember(orientation);
        }
        for (WidgetRun run : mWidgets) {
            if (orientation == HORIZONTAL) {
                run.mWidget.horizontalChainRun = this;
            } else if (orientation == ConstraintWidget.VERTICAL) {
                run.mWidget.verticalChainRun = this;
            }
        }
        boolean isInRtl = (orientation == HORIZONTAL)
                && ((ConstraintWidgetContainer) mWidget.getParent()).isRtl();
        if (isInRtl && mWidgets.size() > 1) {
            mWidget = mWidgets.get(mWidgets.size() - 1).mWidget;
        }
        mChainStyle = orientation == HORIZONTAL
                ? mWidget.getHorizontalChainStyle() : mWidget.getVerticalChainStyle();
    }


    @Override
    void clear() {
        mRunGroup = null;
        for (WidgetRun run : mWidgets) {
            run.clear();
        }
    }

    @Override
    void reset() {
        start.resolved = false;
        end.resolved = false;
    }

    @Override
    public void update(Dependency dependency) {
        if (!(start.resolved && end.resolved)) {
            return;
        }

        ConstraintWidget parent = mWidget.getParent();
        boolean isInRtl = false;
        if (parent instanceof ConstraintWidgetContainer) {
            isInRtl = ((ConstraintWidgetContainer) parent).isRtl();
        }
        int distance = end.value - start.value;
        int size = 0;
        int numMatchConstraints = 0;
        float weights = 0;
        int numVisibleWidgets = 0;
        final int count = mWidgets.size();
        // let's find the first visible widget...
        int firstVisibleWidget = -1;
        for (int i = 0; i < count; i++) {
            WidgetRun run = mWidgets.get(i);
            if (run.mWidget.getVisibility() == GONE) {
                continue;
            }
            firstVisibleWidget = i;
            break;
        }
        // now the last visible widget...
        int lastVisibleWidget = -1;
        for (int i = count - 1; i >= 0; i--) {
            WidgetRun run = mWidgets.get(i);
            if (run.mWidget.getVisibility() == GONE) {
                continue;
            }
            lastVisibleWidget = i;
            break;
        }
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < count; i++) {
                WidgetRun run = mWidgets.get(i);
                if (run.mWidget.getVisibility() == GONE) {
                    continue;
                }
                numVisibleWidgets++;
                if (i > 0 && i >= firstVisibleWidget) {
                    size += run.start.mMargin;
                }
                int dimension = run.mDimension.value;
                boolean treatAsFixed = run.mDimensionBehavior != MATCH_CONSTRAINT;
                if (treatAsFixed) {
                    if (orientation == HORIZONTAL
                            && !run.mWidget.mHorizontalRun.mDimension.resolved) {
                        return;
                    }
                    if (orientation == VERTICAL && !run.mWidget.mVerticalRun.mDimension.resolved) {
                        return;
                    }
                } else if (run.matchConstraintsType == MATCH_CONSTRAINT_WRAP && j == 0) {
                    treatAsFixed = true;
                    dimension = run.mDimension.wrapValue;
                    numMatchConstraints++;
                } else if (run.mDimension.resolved) {
                    treatAsFixed = true;
                }
                if (!treatAsFixed) { // only for the first pass
                    numMatchConstraints++;
                    float weight = run.mWidget.mWeight[orientation];
                    if (weight >= 0) {
                        weights += weight;
                    }
                } else {
                    size += dimension;
                }
                if (i < count - 1 && i < lastVisibleWidget) {
                    size += -run.end.mMargin;
                }
            }
            if (size < distance || numMatchConstraints == 0) {
                break; // we are good to go!
            }
            // otherwise, let's do another pass with using match_constraints
            numVisibleWidgets = 0;
            numMatchConstraints = 0;
            size = 0;
            weights = 0;
        }

        int position = start.value;
        if (isInRtl) {
            position = end.value;
        }
        if (size > distance) {
            if (isInRtl) {
                position += (int) (0.5f + (size - distance) / 2f);
            } else {
                position -= (int) (0.5f + (size - distance) / 2f);
            }
        }
        int matchConstraintsDimension = 0;
        if (numMatchConstraints > 0) {
            matchConstraintsDimension =
                    (int) (0.5f + (distance - size) / (float) numMatchConstraints);

            int appliedLimits = 0;
            for (int i = 0; i < count; i++) {
                WidgetRun run = mWidgets.get(i);
                if (run.mWidget.getVisibility() == GONE) {
                    continue;
                }
                if (run.mDimensionBehavior == MATCH_CONSTRAINT && !run.mDimension.resolved) {
                    int dimension = matchConstraintsDimension;
                    if (weights > 0) {
                        float weight = run.mWidget.mWeight[orientation];
                        dimension = (int) (0.5f + weight * (distance - size) / weights);
                    }
                    int max;
                    int min;
                    int value = dimension;
                    if (orientation == HORIZONTAL) {
                        max = run.mWidget.mMatchConstraintMaxWidth;
                        min = run.mWidget.mMatchConstraintMinWidth;
                    } else {
                        max = run.mWidget.mMatchConstraintMaxHeight;
                        min = run.mWidget.mMatchConstraintMinHeight;
                    }
                    if (run.matchConstraintsType == MATCH_CONSTRAINT_WRAP) {
                        value = Math.min(value, run.mDimension.wrapValue);
                    }
                    value = Math.max(min, value);
                    if (max > 0) {
                        value = Math.min(max, value);
                    }
                    if (value != dimension) {
                        appliedLimits++;
                        dimension = value;
                    }
                    run.mDimension.resolve(dimension);
                }
            }
            if (appliedLimits > 0) {
                numMatchConstraints -= appliedLimits;
                // we have to recompute the sizes
                size = 0;
                for (int i = 0; i < count; i++) {
                    WidgetRun run = mWidgets.get(i);
                    if (run.mWidget.getVisibility() == GONE) {
                        continue;
                    }
                    if (i > 0 && i >= firstVisibleWidget) {
                        size += run.start.mMargin;
                    }
                    size += run.mDimension.value;
                    if (i < count - 1 && i < lastVisibleWidget) {
                        size += -run.end.mMargin;
                    }
                }
            }
            if (mChainStyle == ConstraintWidget.CHAIN_PACKED && appliedLimits == 0) {
                mChainStyle = ConstraintWidget.CHAIN_SPREAD;
            }
        }

        if (size > distance) {
            mChainStyle = ConstraintWidget.CHAIN_PACKED;
        }

        if (numVisibleWidgets > 0 && numMatchConstraints == 0
                && firstVisibleWidget == lastVisibleWidget) {
            // only one widget of fixed size to display...
            mChainStyle = ConstraintWidget.CHAIN_PACKED;
        }

        if (mChainStyle == ConstraintWidget.CHAIN_SPREAD_INSIDE) {
            int gap = 0;
            if (numVisibleWidgets > 1) {
                gap = (distance - size) / (numVisibleWidgets - 1);
            } else if (numVisibleWidgets == 1) {
                gap = (distance - size) / 2;
            }
            if (numMatchConstraints > 0) {
                gap = 0;
            }
            for (int i = 0; i < count; i++) {
                int index = i;
                if (isInRtl) {
                    index = count - (i + 1);
                }
                WidgetRun run = mWidgets.get(index);
                if (run.mWidget.getVisibility() == GONE) {
                    run.start.resolve(position);
                    run.end.resolve(position);
                    continue;
                }
                if (i > 0) {
                    if (isInRtl) {
                        position -= gap;
                    } else {
                        position += gap;
                    }
                }
                if (i > 0 && i >= firstVisibleWidget) {
                    if (isInRtl) {
                        position -= run.start.mMargin;
                    } else {
                        position += run.start.mMargin;
                    }
                }

                if (isInRtl) {
                    run.end.resolve(position);
                } else {
                    run.start.resolve(position);
                }

                int dimension = run.mDimension.value;
                if (run.mDimensionBehavior == MATCH_CONSTRAINT
                        && run.matchConstraintsType == MATCH_CONSTRAINT_WRAP) {
                    dimension = run.mDimension.wrapValue;
                }
                if (isInRtl) {
                    position -= dimension;
                } else {
                    position += dimension;
                }

                if (isInRtl) {
                    run.start.resolve(position);
                } else {
                    run.end.resolve(position);
                }
                run.mResolved = true;
                if (i < count - 1 && i < lastVisibleWidget) {
                    if (isInRtl) {
                        position -= -run.end.mMargin;
                    } else {
                        position += -run.end.mMargin;
                    }
                }
            }
        } else if (mChainStyle == ConstraintWidget.CHAIN_SPREAD) {
            int gap = (distance - size) / (numVisibleWidgets + 1);
            if (numMatchConstraints > 0) {
                gap = 0;
            }
            for (int i = 0; i < count; i++) {
                int index = i;
                if (isInRtl) {
                    index = count - (i + 1);
                }
                WidgetRun run = mWidgets.get(index);
                if (run.mWidget.getVisibility() == GONE) {
                    run.start.resolve(position);
                    run.end.resolve(position);
                    continue;
                }
                if (isInRtl) {
                    position -= gap;
                } else {
                    position += gap;
                }
                if (i > 0 && i >= firstVisibleWidget) {
                    if (isInRtl) {
                        position -= run.start.mMargin;
                    } else {
                        position += run.start.mMargin;
                    }
                }

                if (isInRtl) {
                    run.end.resolve(position);
                } else {
                    run.start.resolve(position);
                }

                int dimension = run.mDimension.value;
                if (run.mDimensionBehavior == MATCH_CONSTRAINT
                        && run.matchConstraintsType == MATCH_CONSTRAINT_WRAP) {
                    dimension = Math.min(dimension, run.mDimension.wrapValue);
                }

                if (isInRtl) {
                    position -= dimension;
                } else {
                    position += dimension;
                }

                if (isInRtl) {
                    run.start.resolve(position);
                } else {
                    run.end.resolve(position);
                }
                if (i < count - 1 && i < lastVisibleWidget) {
                    if (isInRtl) {
                        position -= -run.end.mMargin;
                    } else {
                        position += -run.end.mMargin;
                    }
                }
            }
        } else if (mChainStyle == ConstraintWidget.CHAIN_PACKED) {
            float bias = (orientation == HORIZONTAL) ? mWidget.getHorizontalBiasPercent()
                    : mWidget.getVerticalBiasPercent();
            if (isInRtl) {
                bias = 1 - bias;
            }
            int gap = (int) (0.5f + (distance - size) * bias);
            if (gap < 0 || numMatchConstraints > 0) {
                gap = 0;
            }
            if (isInRtl) {
                position -= gap;
            } else {
                position += gap;
            }
            for (int i = 0; i < count; i++) {
                int index = i;
                if (isInRtl) {
                    index = count - (i + 1);
                }
                WidgetRun run = mWidgets.get(index);
                if (run.mWidget.getVisibility() == GONE) {
                    run.start.resolve(position);
                    run.end.resolve(position);
                    continue;
                }
                if (i > 0 && i >= firstVisibleWidget) {
                    if (isInRtl) {
                        position -= run.start.mMargin;
                    } else {
                        position += run.start.mMargin;
                    }
                }
                if (isInRtl) {
                    run.end.resolve(position);
                } else {
                    run.start.resolve(position);
                }

                int dimension = run.mDimension.value;
                if (run.mDimensionBehavior == MATCH_CONSTRAINT
                        && run.matchConstraintsType == MATCH_CONSTRAINT_WRAP) {
                    dimension = run.mDimension.wrapValue;
                }
                if (isInRtl) {
                    position -= dimension;
                } else {
                    position += dimension;
                }

                if (isInRtl) {
                    run.start.resolve(position);
                } else {
                    run.end.resolve(position);
                }
                if (i < count - 1 && i < lastVisibleWidget) {
                    if (isInRtl) {
                        position -= -run.end.mMargin;
                    } else {
                        position += -run.end.mMargin;
                    }
                }
            }
        }
    }

    // @TODO: add description
    @Override
    public void applyToWidget() {
        for (int i = 0; i < mWidgets.size(); i++) {
            WidgetRun run = mWidgets.get(i);
            run.applyToWidget();
        }
    }

    private ConstraintWidget getFirstVisibleWidget() {
        for (int i = 0; i < mWidgets.size(); i++) {
            WidgetRun run = mWidgets.get(i);
            if (run.mWidget.getVisibility() != GONE) {
                return run.mWidget;
            }
        }
        return null;
    }

    private ConstraintWidget getLastVisibleWidget() {
        for (int i = mWidgets.size() - 1; i >= 0; i--) {
            WidgetRun run = mWidgets.get(i);
            if (run.mWidget.getVisibility() != GONE) {
                return run.mWidget;
            }
        }
        return null;
    }


    @Override
    void apply() {
        for (WidgetRun run : mWidgets) {
            run.apply();
        }
        int count = mWidgets.size();
        if (count < 1) {
            return;
        }

        // get the first and last element of the chain
        ConstraintWidget firstWidget = mWidgets.get(0).mWidget;
        ConstraintWidget lastWidget = mWidgets.get(count - 1).mWidget;

        if (orientation == HORIZONTAL) {
            ConstraintAnchor startAnchor = firstWidget.mLeft;
            ConstraintAnchor endAnchor = lastWidget.mRight;
            DependencyNode startTarget = getTarget(startAnchor, HORIZONTAL);
            int startMargin = startAnchor.getMargin();
            ConstraintWidget firstVisibleWidget = getFirstVisibleWidget();
            if (firstVisibleWidget != null) {
                startMargin = firstVisibleWidget.mLeft.getMargin();
            }
            if (startTarget != null) {
                addTarget(start, startTarget, startMargin);
            }
            DependencyNode endTarget = getTarget(endAnchor, HORIZONTAL);
            int endMargin = endAnchor.getMargin();
            ConstraintWidget lastVisibleWidget = getLastVisibleWidget();
            if (lastVisibleWidget != null) {
                endMargin = lastVisibleWidget.mRight.getMargin();
            }
            if (endTarget != null) {
                addTarget(end, endTarget, -endMargin);
            }
        } else {
            ConstraintAnchor startAnchor = firstWidget.mTop;
            ConstraintAnchor endAnchor = lastWidget.mBottom;
            DependencyNode startTarget = getTarget(startAnchor, VERTICAL);
            int startMargin = startAnchor.getMargin();
            ConstraintWidget firstVisibleWidget = getFirstVisibleWidget();
            if (firstVisibleWidget != null) {
                startMargin = firstVisibleWidget.mTop.getMargin();
            }
            if (startTarget != null) {
                addTarget(start, startTarget, startMargin);
            }
            DependencyNode endTarget = getTarget(endAnchor, VERTICAL);
            int endMargin = endAnchor.getMargin();
            ConstraintWidget lastVisibleWidget = getLastVisibleWidget();
            if (lastVisibleWidget != null) {
                endMargin = lastVisibleWidget.mBottom.getMargin();
            }
            if (endTarget != null) {
                addTarget(end, endTarget, -endMargin);
            }
        }
        start.updateDelegate = this;
        end.updateDelegate = this;
    }

}
