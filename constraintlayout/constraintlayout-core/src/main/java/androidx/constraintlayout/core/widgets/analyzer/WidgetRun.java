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

import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_PERCENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_RATIO;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;

public abstract class WidgetRun implements Dependency {
    public int matchConstraintsType;
    ConstraintWidget mWidget;
    RunGroup mRunGroup;
    protected ConstraintWidget.DimensionBehaviour mDimensionBehavior;
    DimensionDependency mDimension = new DimensionDependency(this);

    public int orientation = HORIZONTAL;
    boolean mResolved = false;
    public DependencyNode start = new DependencyNode(this);
    public DependencyNode end = new DependencyNode(this);

    @SuppressWarnings("HiddenTypeParameter")
    protected RunType mRunType = RunType.NONE;

    public WidgetRun(ConstraintWidget widget) {
        this.mWidget = widget;
    }

    @SuppressWarnings("HiddenAbstractMethod")
    abstract void clear();

    @SuppressWarnings("HiddenAbstractMethod")
    abstract void apply();

    @SuppressWarnings("HiddenAbstractMethod")
    abstract void applyToWidget();

    @SuppressWarnings("HiddenAbstractMethod")
    abstract void reset();

    @SuppressWarnings("HiddenAbstractMethod")
    abstract boolean supportsWrapComputation();

    public boolean isDimensionResolved() {
        return mDimension.resolved;
    }

    // @TODO: add description
    public boolean isCenterConnection() {
        int connections = 0;
        int count = start.mTargets.size();
        for (int i = 0; i < count; i++) {
            DependencyNode dependency = start.mTargets.get(i);
            if (dependency.mRun != this) {
                connections++;
            }
        }
        count = end.mTargets.size();
        for (int i = 0; i < count; i++) {
            DependencyNode dependency = end.mTargets.get(i);
            if (dependency.mRun != this) {
                connections++;
            }
        }
        return connections >= 2;
    }

    // @TODO: add description
    public long wrapSize(int direction) {
        if (mDimension.resolved) {
            long size = mDimension.value;
            if (isCenterConnection()) { //start.targets.size() > 0 && end.targets.size() > 0) {
                size += start.mMargin - end.mMargin;
            } else {
                if (direction == RunGroup.START) {
                    size += start.mMargin;
                } else {
                    size -= end.mMargin;
                }
            }
            return size;
        }
        return 0;
    }

    protected final DependencyNode getTarget(ConstraintAnchor anchor) {
        if (anchor.mTarget == null) {
            return null;
        }
        DependencyNode target = null;
        ConstraintWidget targetWidget = anchor.mTarget.mOwner;
        ConstraintAnchor.Type targetType = anchor.mTarget.mType;
        switch (targetType) {
            case LEFT: {
                HorizontalWidgetRun run = targetWidget.mHorizontalRun;
                target = run.start;
            }
            break;
            case RIGHT: {
                HorizontalWidgetRun run = targetWidget.mHorizontalRun;
                target = run.end;
            }
            break;
            case TOP: {
                VerticalWidgetRun run = targetWidget.mVerticalRun;
                target = run.start;
            }
            break;
            case BASELINE: {
                VerticalWidgetRun run = targetWidget.mVerticalRun;
                target = run.baseline;
            }
            break;
            case BOTTOM: {
                VerticalWidgetRun run = targetWidget.mVerticalRun;
                target = run.end;
            }
            break;
            default:
                break;
        }
        return target;
    }

    protected void updateRunCenter(Dependency dependency,
            ConstraintAnchor startAnchor,
            ConstraintAnchor endAnchor,
            int orientation) {
        DependencyNode startTarget = getTarget(startAnchor);
        DependencyNode endTarget = getTarget(endAnchor);

        if (!(startTarget.resolved && endTarget.resolved)) {
            return;
        }

        int startPos = startTarget.value + startAnchor.getMargin();
        int endPos = endTarget.value - endAnchor.getMargin();
        int distance = endPos - startPos;

        if (!mDimension.resolved
                && mDimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            resolveDimension(orientation, distance);
        }

        if (!mDimension.resolved) {
            return;
        }

        if (mDimension.value == distance) {
            start.resolve(startPos);
            end.resolve(endPos);
            return;
        }

        // Otherwise, we have to center
        float bias = orientation == HORIZONTAL ? mWidget.getHorizontalBiasPercent()
                : mWidget.getVerticalBiasPercent();

        if (startTarget == endTarget) {
            startPos = startTarget.value;
            endPos = endTarget.value;
            // TODO: taking advantage of bias here would be a nice feature to support,
            // but for now let's stay compatible with 1.1
            bias = 0.5f;
        }

        int availableDistance = (endPos - startPos - mDimension.value);
        start.resolve((int) (0.5f + startPos + availableDistance * bias));
        end.resolve(start.value + mDimension.value);
    }

    private void resolveDimension(int orientation, int distance) {
        switch (matchConstraintsType) {
            case MATCH_CONSTRAINT_SPREAD: {
                mDimension.resolve(getLimitedDimension(distance, orientation));
            }
            break;
            case MATCH_CONSTRAINT_PERCENT: {
                ConstraintWidget parent = mWidget.getParent();
                if (parent != null) {
                    WidgetRun run = orientation == HORIZONTAL
                            ? parent.mHorizontalRun
                            : parent.mVerticalRun;
                    if (run.mDimension.resolved) {
                        float percent = orientation == HORIZONTAL
                                ? mWidget.mMatchConstraintPercentWidth
                                : mWidget.mMatchConstraintPercentHeight;
                        int targetDimensionValue = run.mDimension.value;
                        int size = (int) (0.5f + targetDimensionValue * percent);
                        mDimension.resolve(getLimitedDimension(size, orientation));
                    }
                }
            }
            break;
            case MATCH_CONSTRAINT_WRAP: {
                int wrapValue = getLimitedDimension(mDimension.wrapValue, orientation);
                mDimension.resolve(Math.min(wrapValue, distance));
            }
            break;
            case MATCH_CONSTRAINT_RATIO: {
                if (mWidget.mHorizontalRun.mDimensionBehavior
                        == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                        && mWidget.mHorizontalRun.matchConstraintsType == MATCH_CONSTRAINT_RATIO
                        && mWidget.mVerticalRun.mDimensionBehavior
                        == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                        && mWidget.mVerticalRun.matchConstraintsType == MATCH_CONSTRAINT_RATIO) {
                    // pof
                } else {
                    WidgetRun run = (orientation == HORIZONTAL)
                            ? mWidget.mVerticalRun : mWidget.mHorizontalRun;
                    if (run.mDimension.resolved) {
                        float ratio = mWidget.getDimensionRatio();
                        int value;
                        if (orientation == VERTICAL) {
                            value = (int) (0.5f + run.mDimension.value / ratio);
                        } else {
                            value = (int) (0.5f + ratio * run.mDimension.value);
                        }
                        mDimension.resolve(value);
                    }
                }
            }
            break;
            default:
                break;
        }
    }

    protected void updateRunStart(Dependency dependency) {

    }

    protected void updateRunEnd(Dependency dependency) {

    }

    // @TODO: add description
    @Override
    public void update(Dependency dependency) {
    }

    protected final int getLimitedDimension(int dimension, int orientation) {
        if (orientation == HORIZONTAL) {
            int max = mWidget.mMatchConstraintMaxWidth;
            int min = mWidget.mMatchConstraintMinWidth;
            int value = Math.max(min, dimension);
            if (max > 0) {
                value = Math.min(max, dimension);
            }
            if (value != dimension) {
                dimension = value;
            }
        } else {
            int max = mWidget.mMatchConstraintMaxHeight;
            int min = mWidget.mMatchConstraintMinHeight;
            int value = Math.max(min, dimension);
            if (max > 0) {
                value = Math.min(max, dimension);
            }
            if (value != dimension) {
                dimension = value;
            }
        }
        return dimension;
    }

    protected final DependencyNode getTarget(ConstraintAnchor anchor, int orientation) {
        if (anchor.mTarget == null) {
            return null;
        }
        DependencyNode target = null;
        ConstraintWidget targetWidget = anchor.mTarget.mOwner;
        WidgetRun run = (orientation == ConstraintWidget.HORIZONTAL)
                ? targetWidget.mHorizontalRun : targetWidget.mVerticalRun;
        ConstraintAnchor.Type targetType = anchor.mTarget.mType;
        switch (targetType) {
            case TOP:
            case LEFT: {
                target = run.start;
            }
            break;
            case BOTTOM:
            case RIGHT: {
                target = run.end;
            }
            break;
            default:
                break;
        }
        return target;
    }

    protected final void addTarget(DependencyNode node,
            DependencyNode target,
            int margin) {
        node.mTargets.add(target);
        node.mMargin = margin;
        target.mDependencies.add(node);
    }

    protected final void addTarget(DependencyNode node,
            DependencyNode target,
            int marginFactor,
            @SuppressWarnings("HiddenTypeParameter") DimensionDependency
                    dimensionDependency) {
        node.mTargets.add(target);
        node.mTargets.add(mDimension);
        node.mMarginFactor = marginFactor;
        node.mMarginDependency = dimensionDependency;
        target.mDependencies.add(node);
        dimensionDependency.mDependencies.add(node);
    }

    // @TODO: add description
    public long getWrapDimension() {
        if (mDimension.resolved) {
            return mDimension.value;
        }
        return 0;
    }

    public boolean isResolved() {
        return mResolved;
    }

    enum RunType {NONE, START, END, CENTER}
}
