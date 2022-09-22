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

import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.FIXED;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_PARENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_PERCENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_RATIO;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;
import static androidx.constraintlayout.core.widgets.analyzer.WidgetRun.RunType.CENTER;

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.Helper;

public class VerticalWidgetRun extends WidgetRun {
    private static final boolean FORCE_USE = true;
    public DependencyNode baseline = new DependencyNode(this);
    androidx.constraintlayout.core.widgets.analyzer.DimensionDependency mBaselineDimension = null;

    public VerticalWidgetRun(ConstraintWidget widget) {
        super(widget);
        start.mType = DependencyNode.Type.TOP;
        end.mType = DependencyNode.Type.BOTTOM;
        baseline.mType = DependencyNode.Type.BASELINE;
        this.orientation = VERTICAL;
    }

    @Override
    public String toString() {
        return "VerticalRun " + mWidget.getDebugName();
    }

    @Override
    void clear() {
        mRunGroup = null;
        start.clear();
        end.clear();
        baseline.clear();
        mDimension.clear();
        mResolved = false;
    }

    @Override
    void reset() {
        mResolved = false;
        start.clear();
        start.resolved = false;
        end.clear();
        end.resolved = false;
        baseline.clear();
        baseline.resolved = false;
        mDimension.resolved = false;
    }

    @Override
    boolean supportsWrapComputation() {
        if (super.mDimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            if (super.mWidget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD) {
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public void update(Dependency dependency) {
        switch (mRunType) {
            case START: {
                updateRunStart(dependency);
            }
            break;
            case END: {
                updateRunEnd(dependency);
            }
            break;
            case CENTER: {
                updateRunCenter(dependency, mWidget.mTop, mWidget.mBottom, VERTICAL);
                return;
            }
            default:
                break;
        }
        if (FORCE_USE || dependency == mDimension) {
            if (mDimension.readyToSolve && !mDimension.resolved) {
                if (mDimensionBehavior == MATCH_CONSTRAINT) {
                    switch (mWidget.mMatchConstraintDefaultHeight) {
                        case MATCH_CONSTRAINT_RATIO: {
                            if (mWidget.mHorizontalRun.mDimension.resolved) {
                                int size = 0;
                                int ratioSide = mWidget.getDimensionRatioSide();
                                switch (ratioSide) {
                                    case ConstraintWidget.HORIZONTAL: {
                                        size = (int) (0.5f + mWidget.mHorizontalRun.mDimension.value
                                                * mWidget.getDimensionRatio());
                                    }
                                    break;
                                    case ConstraintWidget.VERTICAL: {
                                        size = (int) (0.5f + mWidget.mHorizontalRun.mDimension.value
                                                / mWidget.getDimensionRatio());
                                    }
                                    break;
                                    case ConstraintWidget.UNKNOWN: {
                                        size = (int) (0.5f + mWidget.mHorizontalRun.mDimension.value
                                                / mWidget.getDimensionRatio());
                                    }
                                    break;
                                    default:
                                        break;
                                }
                                mDimension.resolve(size);
                            }
                        }
                        break;
                        case MATCH_CONSTRAINT_PERCENT: {
                            ConstraintWidget parent = mWidget.getParent();
                            if (parent != null) {
                                if (parent.mVerticalRun.mDimension.resolved) {
                                    float percent = mWidget.mMatchConstraintPercentHeight;
                                    int targetDimensionValue = parent.mVerticalRun.mDimension.value;
                                    int size = (int) (0.5f + targetDimensionValue * percent);
                                    mDimension.resolve(size);
                                }
                            }
                        }
                        break;
                        default:
                            break;
                    }
                }
            }
        }
        if (!(start.readyToSolve && end.readyToSolve)) {
            return;
        }
        if (start.resolved && end.resolved && mDimension.resolved) {
            return;
        }

        if (!mDimension.resolved
                && mDimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                && mWidget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD
                && !mWidget.isInVerticalChain()) {

            DependencyNode startTarget = start.mTargets.get(0);
            DependencyNode endTarget = end.mTargets.get(0);
            int startPos = startTarget.value + start.mMargin;
            int endPos = endTarget.value + end.mMargin;

            int distance = endPos - startPos;
            start.resolve(startPos);
            end.resolve(endPos);
            mDimension.resolve(distance);
            return;
        }

        if (!mDimension.resolved
                && mDimensionBehavior == MATCH_CONSTRAINT
                && matchConstraintsType == MATCH_CONSTRAINT_WRAP) {
            if (start.mTargets.size() > 0 && end.mTargets.size() > 0) {
                DependencyNode startTarget = start.mTargets.get(0);
                DependencyNode endTarget = end.mTargets.get(0);
                int startPos = startTarget.value + start.mMargin;
                int endPos = endTarget.value + end.mMargin;
                int availableSpace = endPos - startPos;
                if (availableSpace < mDimension.wrapValue) {
                    mDimension.resolve(availableSpace);
                } else {
                    mDimension.resolve(mDimension.wrapValue);
                }
            }
        }

        if (!mDimension.resolved) {
            return;
        }
        // ready to solve, centering.
        if (start.mTargets.size() > 0 && end.mTargets.size() > 0) {
            DependencyNode startTarget = start.mTargets.get(0);
            DependencyNode endTarget = end.mTargets.get(0);
            int startPos = startTarget.value + start.mMargin;
            int endPos = endTarget.value + end.mMargin;
            float bias = mWidget.getVerticalBiasPercent();
            if (startTarget == endTarget) {
                startPos = startTarget.value;
                endPos = endTarget.value;
                // TODO: this might be a nice feature to support, but I guess for now let's stay
                // compatible with 1.1
                bias = 0.5f;
            }
            int distance = (endPos - startPos - mDimension.value);
            start.resolve((int) (0.5f + startPos + distance * bias));
            end.resolve(start.value + mDimension.value);
        }
    }

    @Override
    void apply() {
        if (mWidget.measured) {
            mDimension.resolve(mWidget.getHeight());
        }
        if (!mDimension.resolved) {
            super.mDimensionBehavior = mWidget.getVerticalDimensionBehaviour();
            if (mWidget.hasBaseline()) {
                mBaselineDimension = new BaselineDimensionDependency(this);
            }
            if (super.mDimensionBehavior != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                if (mDimensionBehavior == MATCH_PARENT) {
                    ConstraintWidget parent = mWidget.getParent();
                    if (parent != null && parent.getVerticalDimensionBehaviour() == FIXED) {
                        int resolvedDimension = parent.getHeight()
                                - mWidget.mTop.getMargin() - mWidget.mBottom.getMargin();
                        addTarget(start, parent.mVerticalRun.start, mWidget.mTop.getMargin());
                        addTarget(end, parent.mVerticalRun.end, -mWidget.mBottom.getMargin());
                        mDimension.resolve(resolvedDimension);
                        return;
                    }
                }
                if (mDimensionBehavior == FIXED) {
                    mDimension.resolve(mWidget.getHeight());
                }
            }
        } else {
            if (mDimensionBehavior == MATCH_PARENT) {
                ConstraintWidget parent = mWidget.getParent();
                if (parent != null && parent.getVerticalDimensionBehaviour() == FIXED) {
                    addTarget(start, parent.mVerticalRun.start, mWidget.mTop.getMargin());
                    addTarget(end, parent.mVerticalRun.end, -mWidget.mBottom.getMargin());
                    return;
                }
            }
        }
        // three basic possibilities:
        // <-s-e->
        // <-s-e
        //   s-e->
        // and a variation if the dimension is not yet known:
        // <-s-d-e->
        // <-s<-d<-e
        //   s->d->e->

        if (mDimension.resolved && mWidget.measured) {
            if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP].mTarget != null
                    && mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM].mTarget
                    != null) { // <-s-e->
                if (mWidget.isInVerticalChain()) {
                    start.mMargin = mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP].getMargin();
                    end.mMargin = -mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM].getMargin();
                } else {
                    DependencyNode startTarget =
                            getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP]);
                    if (startTarget != null) {
                        addTarget(start, startTarget,
                                mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP].getMargin());
                    }
                    DependencyNode endTarget =
                            getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM]);
                    if (endTarget != null) {
                        addTarget(end, endTarget,
                                -mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM].getMargin());
                    }
                    start.delegateToWidgetRun = true;
                    end.delegateToWidgetRun = true;
                }
                if (mWidget.hasBaseline()) {
                    addTarget(baseline, start, mWidget.getBaselineDistance());
                }
            } else if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP].mTarget != null) { // <-s-e
                DependencyNode target =
                        getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP]);
                if (target != null) {
                    addTarget(start, target,
                            mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP].getMargin());
                    addTarget(end, start, mDimension.value);
                    if (mWidget.hasBaseline()) {
                        addTarget(baseline, start, mWidget.getBaselineDistance());
                    }
                }
            } else if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM].mTarget
                    != null) {   //   s-e->
                DependencyNode target =
                        getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM]);
                if (target != null) {
                    addTarget(end, target,
                            -mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM].getMargin());
                    addTarget(start, end, -mDimension.value);
                }
                if (mWidget.hasBaseline()) {
                    addTarget(baseline, start, mWidget.getBaselineDistance());
                }
            } else if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_BASELINE].mTarget
                    != null) {
                DependencyNode target =
                        getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_BASELINE]);
                if (target != null) {
                    addTarget(baseline, target, 0);
                    addTarget(start, baseline, -mWidget.getBaselineDistance());
                    addTarget(end, start, mDimension.value);
                }
            } else {
                // no connections, nothing to do.
                if (!(mWidget instanceof Helper) && mWidget.getParent() != null
                        && mWidget.getAnchor(ConstraintAnchor.Type.CENTER).mTarget == null) {
                    DependencyNode top = mWidget.getParent().mVerticalRun.start;
                    addTarget(start, top, mWidget.getY());
                    addTarget(end, start, mDimension.value);
                    if (mWidget.hasBaseline()) {
                        addTarget(baseline, start, mWidget.getBaselineDistance());
                    }
                }
            }
        } else {
            if (!mDimension.resolved && mDimensionBehavior == MATCH_CONSTRAINT) {
                switch (mWidget.mMatchConstraintDefaultHeight) {
                    case MATCH_CONSTRAINT_RATIO: {
                        if (!mWidget.isInVerticalChain()) {
                            if (mWidget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_RATIO) {
                                // need to look into both side
                                // do nothing here --
                                //    let the HorizontalWidgetRun::update() deal with it.
                                break;
                            }
                            // we have a ratio, but we depend on the other side computation
                            DependencyNode targetDimension = mWidget.mHorizontalRun.mDimension;
                            mDimension.mTargets.add(targetDimension);
                            targetDimension.mDependencies.add(mDimension);
                            mDimension.delegateToWidgetRun = true;
                            mDimension.mDependencies.add(start);
                            mDimension.mDependencies.add(end);
                        }
                    }
                    break;
                    case MATCH_CONSTRAINT_PERCENT: {
                        // we need to look up the parent dimension
                        ConstraintWidget parent = mWidget.getParent();
                        if (parent == null) {
                            break;
                        }
                        DependencyNode targetDimension = parent.mVerticalRun.mDimension;
                        mDimension.mTargets.add(targetDimension);
                        targetDimension.mDependencies.add(mDimension);
                        mDimension.delegateToWidgetRun = true;
                        mDimension.mDependencies.add(start);
                        mDimension.mDependencies.add(end);
                    }
                    break;
                    case MATCH_CONSTRAINT_SPREAD: {
                        // the work is done in the update()
                    }
                    break;
                    default:
                        break;
                }
            } else {
                mDimension.addDependency(this);
            }
            if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP].mTarget != null
                    && mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM].mTarget
                    != null) { // <-s-d-e->
                if (mWidget.isInVerticalChain()) {
                    start.mMargin = mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP].getMargin();
                    end.mMargin = -mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM].getMargin();
                } else {
                    DependencyNode startTarget =
                            getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP]);
                    DependencyNode endTarget =
                            getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM]);
                    if (false) {
                        if (startTarget != null) {
                            addTarget(start, startTarget,
                                    mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP].getMargin());
                        }
                        if (endTarget != null) {
                            addTarget(end, endTarget,
                                    -mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM]
                                            .getMargin());
                        }
                    } else {
                        if (startTarget != null) {
                            startTarget.addDependency(this);
                        }
                        if (endTarget != null) {
                            endTarget.addDependency(this);
                        }
                    }
                    mRunType = CENTER;
                }
                if (mWidget.hasBaseline()) {
                    addTarget(baseline, start, 1, mBaselineDimension);
                }
            } else if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP].mTarget
                    != null) { // <-s<-d<-e
                DependencyNode target =
                        getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP]);
                if (target != null) {
                    addTarget(start, target,
                            mWidget.mListAnchors[ConstraintWidget.ANCHOR_TOP].getMargin());
                    addTarget(end, start, 1, mDimension);
                    if (mWidget.hasBaseline()) {
                        addTarget(baseline, start, 1, mBaselineDimension);
                    }
                    if (mDimensionBehavior == MATCH_CONSTRAINT) {
                        if (mWidget.getDimensionRatio() > 0) {
                            if (mWidget.mHorizontalRun.mDimensionBehavior == MATCH_CONSTRAINT) {
                                mWidget.mHorizontalRun.mDimension.mDependencies.add(mDimension);
                                mDimension.mTargets.add(mWidget.mHorizontalRun.mDimension);
                                mDimension.updateDelegate = this;
                            }
                        }
                    }
                }
            } else if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM].mTarget
                    != null) {   //   s->d->e->
                DependencyNode target =
                        getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM]);
                if (target != null) {
                    addTarget(end, target,
                            -mWidget.mListAnchors[ConstraintWidget.ANCHOR_BOTTOM].getMargin());
                    addTarget(start, end, -1, mDimension);
                    if (mWidget.hasBaseline()) {
                        addTarget(baseline, start, 1, mBaselineDimension);
                    }
                }
            } else if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_BASELINE].mTarget != null) {
                DependencyNode target =
                        getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_BASELINE]);
                if (target != null) {
                    addTarget(baseline, target, 0);
                    addTarget(start, baseline, -1, mBaselineDimension);
                    addTarget(end, start, 1, mDimension);
                }
            } else {
                // no connections, nothing to do.
                if (!(mWidget instanceof Helper) && mWidget.getParent() != null) {
                    DependencyNode top = mWidget.getParent().mVerticalRun.start;
                    addTarget(start, top, mWidget.getY());
                    addTarget(end, start, 1, mDimension);
                    if (mWidget.hasBaseline()) {
                        addTarget(baseline, start, 1, mBaselineDimension);
                    }
                    if (mDimensionBehavior == MATCH_CONSTRAINT) {
                        if (mWidget.getDimensionRatio() > 0) {
                            if (mWidget.mHorizontalRun.mDimensionBehavior == MATCH_CONSTRAINT) {
                                mWidget.mHorizontalRun.mDimension.mDependencies.add(mDimension);
                                mDimension.mTargets.add(mWidget.mHorizontalRun.mDimension);
                                mDimension.updateDelegate = this;
                            }
                        }
                    }
                }
            }

            // if dimension has no dependency, mark it as ready to solve
            if (mDimension.mTargets.size() == 0) {
                mDimension.readyToSolve = true;
            }
        }
    }

    // @TODO: add description
    @Override
    public void applyToWidget() {
        if (start.resolved) {
            mWidget.setY(start.value);
        }
    }
}
