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
import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_PERCENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_RATIO;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.UNKNOWN;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;
import static androidx.constraintlayout.core.widgets.analyzer.WidgetRun.RunType.CENTER;

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.Helper;

public class HorizontalWidgetRun extends WidgetRun {

    private static int[] sTempDimensions = new int[2];

    public HorizontalWidgetRun(ConstraintWidget widget) {
        super(widget);
        start.mType = DependencyNode.Type.LEFT;
        end.mType = DependencyNode.Type.RIGHT;
        this.orientation = HORIZONTAL;
    }

    @Override
    public String toString() {
        return "HorizontalRun " + mWidget.getDebugName();
    }

    @Override
    void clear() {
        mRunGroup = null;
        start.clear();
        end.clear();
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
        mDimension.resolved = false;
    }

    @Override
    boolean supportsWrapComputation() {
        if (super.mDimensionBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            if (super.mWidget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD) {
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    void apply() {
        if (mWidget.measured) {
            mDimension.resolve(mWidget.getWidth());
        }
        if (!mDimension.resolved) {
            super.mDimensionBehavior = mWidget.getHorizontalDimensionBehaviour();
            if (super.mDimensionBehavior != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                if (mDimensionBehavior == MATCH_PARENT) {
                    ConstraintWidget parent = mWidget.getParent();
                    if (parent != null
                            && (parent.getHorizontalDimensionBehaviour() == FIXED
                            || parent.getHorizontalDimensionBehaviour() == MATCH_PARENT)) {
                        int resolvedDimension = parent.getWidth()
                                - mWidget.mLeft.getMargin() - mWidget.mRight.getMargin();
                        addTarget(start, parent.mHorizontalRun.start, mWidget.mLeft.getMargin());
                        addTarget(end, parent.mHorizontalRun.end, -mWidget.mRight.getMargin());
                        mDimension.resolve(resolvedDimension);
                        return;
                    }
                }
                if (mDimensionBehavior == FIXED) {
                    mDimension.resolve(mWidget.getWidth());
                }
            }
        } else {
            if (mDimensionBehavior == MATCH_PARENT) {
                ConstraintWidget parent = mWidget.getParent();
                if (parent != null
                        && (parent.getHorizontalDimensionBehaviour() == FIXED
                        || parent.getHorizontalDimensionBehaviour() == MATCH_PARENT)) {
                    addTarget(start, parent.mHorizontalRun.start, mWidget.mLeft.getMargin());
                    addTarget(end, parent.mHorizontalRun.end, -mWidget.mRight.getMargin());
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
            if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].mTarget != null
                    && mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].mTarget
                    != null) { // <-s-e->
                if (mWidget.isInHorizontalChain()) {
                    start.mMargin = mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin();
                    end.mMargin = -mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin();
                } else {
                    DependencyNode startTarget =
                            getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT]);
                    if (startTarget != null) {
                        addTarget(start, startTarget,
                                mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin());
                    }
                    DependencyNode endTarget =
                            getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT]);
                    if (endTarget != null) {
                        addTarget(end, endTarget,
                                -mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin());
                    }
                    start.delegateToWidgetRun = true;
                    end.delegateToWidgetRun = true;
                }
            } else if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].mTarget
                    != null) { // <-s-e
                DependencyNode target =
                        getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT]);
                if (target != null) {
                    addTarget(start, target,
                            mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin());
                    addTarget(end, start, mDimension.value);
                }
            } else if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].mTarget
                    != null) {   //   s-e->
                DependencyNode target =
                        getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT]);
                if (target != null) {
                    addTarget(end, target,
                            -mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin());
                    addTarget(start, end, -mDimension.value);
                }
            } else {
                // no connections, nothing to do.
                if (!(mWidget instanceof Helper) && mWidget.getParent() != null
                        && mWidget.getAnchor(ConstraintAnchor.Type.CENTER).mTarget == null) {
                    DependencyNode left = mWidget.getParent().mHorizontalRun.start;
                    addTarget(start, left, mWidget.getX());
                    addTarget(end, start, mDimension.value);
                }
            }
        } else {
            if (mDimensionBehavior == MATCH_CONSTRAINT) {
                switch (mWidget.mMatchConstraintDefaultWidth) {
                    case MATCH_CONSTRAINT_RATIO: {
                        if (mWidget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_RATIO
                        ) {
                            // need to look into both side
                            start.updateDelegate = this;
                            end.updateDelegate = this;
                            mWidget.mVerticalRun.start.updateDelegate = this;
                            mWidget.mVerticalRun.end.updateDelegate = this;
                            mDimension.updateDelegate = this;

                            if (mWidget.isInVerticalChain()) {
                                mDimension.mTargets.add(mWidget.mVerticalRun.mDimension);
                                mWidget.mVerticalRun.mDimension.mDependencies.add(mDimension);
                                mWidget.mVerticalRun.mDimension.updateDelegate = this;
                                mDimension.mTargets.add(mWidget.mVerticalRun.start);
                                mDimension.mTargets.add(mWidget.mVerticalRun.end);
                                mWidget.mVerticalRun.start.mDependencies.add(mDimension);
                                mWidget.mVerticalRun.end.mDependencies.add(mDimension);
                            } else if (mWidget.isInHorizontalChain()) {
                                mWidget.mVerticalRun.mDimension.mTargets.add(mDimension);
                                mDimension.mDependencies.add(mWidget.mVerticalRun.mDimension);
                            } else {
                                mWidget.mVerticalRun.mDimension.mTargets.add(mDimension);
                            }
                            break;
                        }
                        // we have a ratio, but we depend on the other side computation
                        DependencyNode targetDimension = mWidget.mVerticalRun.mDimension;
                        mDimension.mTargets.add(targetDimension);
                        targetDimension.mDependencies.add(mDimension);
                        mWidget.mVerticalRun.start.mDependencies.add(mDimension);
                        mWidget.mVerticalRun.end.mDependencies.add(mDimension);
                        mDimension.delegateToWidgetRun = true;
                        mDimension.mDependencies.add(start);
                        mDimension.mDependencies.add(end);
                        start.mTargets.add(mDimension);
                        end.mTargets.add(mDimension);
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
            }
            if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].mTarget != null
                    && mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].mTarget
                    != null) { // <-s-d-e->

                if (mWidget.isInHorizontalChain()) {
                    start.mMargin = mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin();
                    end.mMargin = -mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin();
                } else {
                    DependencyNode startTarget =
                            getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT]);
                    DependencyNode endTarget =
                            getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT]);
                    if (false) {
                        if (startTarget != null) {
                            addTarget(start, startTarget,
                                    mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin());
                        }
                        if (endTarget != null) {
                            addTarget(end, endTarget,
                                    -mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT]
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
            } else if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].mTarget
                    != null) { // <-s<-d<-e
                DependencyNode target =
                        getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT]);
                if (target != null) {
                    addTarget(start, target,
                            mWidget.mListAnchors[ConstraintWidget.ANCHOR_LEFT].getMargin());
                    addTarget(end, start, 1, mDimension);
                }
            } else if (mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].mTarget
                    != null) {   //   s->d->e->
                DependencyNode target =
                        getTarget(mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT]);
                if (target != null) {
                    addTarget(end, target,
                            -mWidget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT].getMargin());
                    addTarget(start, end, -1, mDimension);
                }
            } else {
                // no connections, nothing to do.
                if (!(mWidget instanceof Helper) && mWidget.getParent() != null) {
                    DependencyNode left = mWidget.getParent().mHorizontalRun.start;
                    addTarget(start, left, mWidget.getX());
                    addTarget(end, start, 1, mDimension);
                }
            }
        }
    }

    private void computeInsetRatio(int[] dimensions,
            int x1,
            int x2,
            int y1,
            int y2,
            float ratio,
            int side) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        switch (side) {
            case UNKNOWN: {
                int candidateX1 = (int) (0.5f + dy * ratio);
                int candidateY1 = dy;
                int candidateX2 = dx;
                int candidateY2 = (int) (0.5f + dx / ratio);
                if (candidateX1 <= dx && candidateY1 <= dy) {
                    dimensions[HORIZONTAL] = candidateX1;
                    dimensions[VERTICAL] = candidateY1;
                } else if (candidateX2 <= dx && candidateY2 <= dy) {
                    dimensions[HORIZONTAL] = candidateX2;
                    dimensions[VERTICAL] = candidateY2;
                }
            }
            break;
            case HORIZONTAL: {
                int horizontalSide = (int) (0.5f + dy * ratio);
                dimensions[HORIZONTAL] = horizontalSide;
                dimensions[VERTICAL] = dy;
            }
            break;
            case VERTICAL: {
                int verticalSide = (int) (0.5f + dx * ratio);
                dimensions[HORIZONTAL] = dx;
                dimensions[VERTICAL] = verticalSide;
            }
            break;
            default:
                break;
        }
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
                updateRunCenter(dependency, mWidget.mLeft, mWidget.mRight, HORIZONTAL);
                return;
            }
            default:
                break;
        }

        if (!mDimension.resolved) {
            if (mDimensionBehavior == MATCH_CONSTRAINT) {
                switch (mWidget.mMatchConstraintDefaultWidth) {
                    case MATCH_CONSTRAINT_RATIO: {
                        if (mWidget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD
                                || mWidget.mMatchConstraintDefaultHeight
                                == MATCH_CONSTRAINT_RATIO) {
                            DependencyNode secondStart = mWidget.mVerticalRun.start;
                            DependencyNode secondEnd = mWidget.mVerticalRun.end;
                            boolean s1 = mWidget.mLeft.mTarget != null;
                            boolean s2 = mWidget.mTop.mTarget != null;
                            boolean e1 = mWidget.mRight.mTarget != null;
                            boolean e2 = mWidget.mBottom.mTarget != null;

                            int definedSide = mWidget.getDimensionRatioSide();

                            if (s1 && s2 && e1 && e2) {
                                float ratio = mWidget.getDimensionRatio();
                                if (secondStart.resolved && secondEnd.resolved) {
                                    if (!(start.readyToSolve && end.readyToSolve)) {
                                        return;
                                    }
                                    int x1 = start.mTargets.get(0).value + start.mMargin;
                                    int x2 = end.mTargets.get(0).value - end.mMargin;
                                    int y1 = secondStart.value + secondStart.mMargin;
                                    int y2 = secondEnd.value - secondEnd.mMargin;
                                    computeInsetRatio(sTempDimensions,
                                            x1, x2, y1, y2, ratio, definedSide);
                                    mDimension.resolve(sTempDimensions[HORIZONTAL]);
                                    mWidget.mVerticalRun.mDimension
                                            .resolve(sTempDimensions[VERTICAL]);
                                    return;
                                }
                                if (start.resolved && end.resolved) {
                                    if (!(secondStart.readyToSolve && secondEnd.readyToSolve)) {
                                        return;
                                    }
                                    int x1 = start.value + start.mMargin;
                                    int x2 = end.value - end.mMargin;
                                    int y1 = secondStart.mTargets.get(0).value
                                            + secondStart.mMargin;
                                    int y2 = secondEnd.mTargets.get(0).value - secondEnd.mMargin;
                                    computeInsetRatio(sTempDimensions,
                                            x1, x2, y1, y2, ratio, definedSide);
                                    mDimension.resolve(sTempDimensions[HORIZONTAL]);
                                    mWidget.mVerticalRun.mDimension
                                            .resolve(sTempDimensions[VERTICAL]);
                                }
                                if (!(start.readyToSolve && end.readyToSolve
                                        && secondStart.readyToSolve
                                        && secondEnd.readyToSolve)) {
                                    return;
                                }
                                int x1 = start.mTargets.get(0).value + start.mMargin;
                                int x2 = end.mTargets.get(0).value - end.mMargin;
                                int y1 = secondStart.mTargets.get(0).value + secondStart.mMargin;
                                int y2 = secondEnd.mTargets.get(0).value - secondEnd.mMargin;
                                computeInsetRatio(sTempDimensions,
                                        x1, x2, y1, y2, ratio, definedSide);
                                mDimension.resolve(sTempDimensions[HORIZONTAL]);
                                mWidget.mVerticalRun.mDimension.resolve(sTempDimensions[VERTICAL]);
                            } else if (s1 && e1) {
                                if (!(start.readyToSolve && end.readyToSolve)) {
                                    return;
                                }
                                float ratio = mWidget.getDimensionRatio();
                                int x1 = start.mTargets.get(0).value + start.mMargin;
                                int x2 = end.mTargets.get(0).value - end.mMargin;

                                switch (definedSide) {
                                    case UNKNOWN:
                                    case HORIZONTAL: {
                                        int dx = x2 - x1;
                                        int ldx = getLimitedDimension(dx, HORIZONTAL);
                                        int dy = (int) (0.5f + ldx * ratio);
                                        int ldy = getLimitedDimension(dy, VERTICAL);
                                        if (dy != ldy) {
                                            ldx = (int) (0.5f + ldy / ratio);
                                        }
                                        mDimension.resolve(ldx);
                                        mWidget.mVerticalRun.mDimension.resolve(ldy);
                                    }
                                    break;
                                    case VERTICAL: {
                                        int dx = x2 - x1;
                                        int ldx = getLimitedDimension(dx, HORIZONTAL);
                                        int dy = (int) (0.5f + ldx / ratio);
                                        int ldy = getLimitedDimension(dy, VERTICAL);
                                        if (dy != ldy) {
                                            ldx = (int) (0.5f + ldy * ratio);
                                        }
                                        mDimension.resolve(ldx);
                                        mWidget.mVerticalRun.mDimension.resolve(ldy);
                                    }
                                    break;
                                    default:
                                        break;
                                }
                            } else if (s2 && e2) {
                                if (!(secondStart.readyToSolve && secondEnd.readyToSolve)) {
                                    return;
                                }
                                float ratio = mWidget.getDimensionRatio();
                                int y1 = secondStart.mTargets.get(0).value + secondStart.mMargin;
                                int y2 = secondEnd.mTargets.get(0).value - secondEnd.mMargin;

                                switch (definedSide) {
                                    case UNKNOWN:
                                    case VERTICAL: {
                                        int dy = y2 - y1;
                                        int ldy = getLimitedDimension(dy, VERTICAL);
                                        int dx = (int) (0.5f + ldy / ratio);
                                        int ldx = getLimitedDimension(dx, HORIZONTAL);
                                        if (dx != ldx) {
                                            ldy = (int) (0.5f + ldx * ratio);
                                        }
                                        mDimension.resolve(ldx);
                                        mWidget.mVerticalRun.mDimension.resolve(ldy);
                                    }
                                    break;
                                    case HORIZONTAL: {
                                        int dy = y2 - y1;
                                        int ldy = getLimitedDimension(dy, VERTICAL);
                                        int dx = (int) (0.5f + ldy * ratio);
                                        int ldx = getLimitedDimension(dx, HORIZONTAL);
                                        if (dx != ldx) {
                                            ldy = (int) (0.5f + ldx / ratio);
                                        }
                                        mDimension.resolve(ldx);
                                        mWidget.mVerticalRun.mDimension.resolve(ldy);
                                    }
                                    break;
                                    default:
                                        break;
                                }
                            }
                        } else {
                            int size = 0;
                            int ratioSide = mWidget.getDimensionRatioSide();
                            switch (ratioSide) {
                                case HORIZONTAL: {
                                    size = (int) (0.5f + mWidget.mVerticalRun.mDimension.value
                                            / mWidget.getDimensionRatio());
                                }
                                break;
                                case ConstraintWidget.VERTICAL: {
                                    size = (int) (0.5f + mWidget.mVerticalRun.mDimension.value
                                            * mWidget.getDimensionRatio());
                                }
                                break;
                                case ConstraintWidget.UNKNOWN: {
                                    size = (int) (0.5f + mWidget.mVerticalRun.mDimension.value
                                            * mWidget.getDimensionRatio());
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
                            if (parent.mHorizontalRun.mDimension.resolved) {
                                float percent = mWidget.mMatchConstraintPercentWidth;
                                int targetDimensionValue = parent.mHorizontalRun.mDimension.value;
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

        if (!(start.readyToSolve && end.readyToSolve)) {
            return;
        }

        if (start.resolved && end.resolved && mDimension.resolved) {
            return;
        }

        if (!mDimension.resolved
                && mDimensionBehavior == MATCH_CONSTRAINT
                && mWidget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD
                && !mWidget.isInHorizontalChain()) {

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
                int value = Math.min(availableSpace, mDimension.wrapValue);
                int max = mWidget.mMatchConstraintMaxWidth;
                int min = mWidget.mMatchConstraintMinWidth;
                value = Math.max(min, value);
                if (max > 0) {
                    value = Math.min(max, value);
                }
                mDimension.resolve(value);
            }
        }

        if (!mDimension.resolved) {
            return;
        }
        // ready to solve, centering.
        DependencyNode startTarget = start.mTargets.get(0);
        DependencyNode endTarget = end.mTargets.get(0);
        int startPos = startTarget.value + start.mMargin;
        int endPos = endTarget.value + end.mMargin;
        float bias = mWidget.getHorizontalBiasPercent();
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

    // @TODO: add description
    @Override
    public void applyToWidget() {
        if (start.resolved) {
            mWidget.setX(start.value);
        }
    }

}
