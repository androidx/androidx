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

import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.Guideline;

class GuidelineReference extends WidgetRun {

    GuidelineReference(ConstraintWidget widget) {
        super(widget);
        widget.mHorizontalRun.clear();
        widget.mVerticalRun.clear();
        this.orientation = ((Guideline) widget).getOrientation();
    }

    @Override
    void clear() {
        start.clear();
    }

    @Override
    void reset() {
        start.resolved = false;
        end.resolved = false;
    }

    @Override
    boolean supportsWrapComputation() {
        return false;
    }

    private void addDependency(
            androidx.constraintlayout.core.widgets.analyzer.DependencyNode node) {
        start.mDependencies.add(node);
        node.mTargets.add(start);
    }

    @Override
    public void update(Dependency dependency) {
        if (!start.readyToSolve) {
            return;
        }
        if (start.resolved) {
            return;
        }
        // ready to solve, centering.
        androidx.constraintlayout.core.widgets.analyzer.DependencyNode startTarget =
                start.mTargets.get(0);
        Guideline guideline = (Guideline) mWidget;
        int startPos = (int) (0.5f + startTarget.value * guideline.getRelativePercent());
        start.resolve(startPos);
    }

    @Override
    void apply() {
        Guideline guideline = (Guideline) mWidget;
        int relativeBegin = guideline.getRelativeBegin();
        int relativeEnd = guideline.getRelativeEnd();
        @SuppressWarnings("unused") float percent = guideline.getRelativePercent();
        if (guideline.getOrientation() == ConstraintWidget.VERTICAL) {
            if (relativeBegin != -1) {
                start.mTargets.add(mWidget.mParent.mHorizontalRun.start);
                mWidget.mParent.mHorizontalRun.start.mDependencies.add(start);
                start.mMargin = relativeBegin;
            } else if (relativeEnd != -1) {
                start.mTargets.add(mWidget.mParent.mHorizontalRun.end);
                mWidget.mParent.mHorizontalRun.end.mDependencies.add(start);
                start.mMargin = -relativeEnd;
            } else {
                start.delegateToWidgetRun = true;
                start.mTargets.add(mWidget.mParent.mHorizontalRun.end);
                mWidget.mParent.mHorizontalRun.end.mDependencies.add(start);
            }
            // FIXME -- if we move the DependencyNode directly
            //              in the ConstraintAnchor we'll be good.
            addDependency(mWidget.mHorizontalRun.start);
            addDependency(mWidget.mHorizontalRun.end);
        } else {
            if (relativeBegin != -1) {
                start.mTargets.add(mWidget.mParent.mVerticalRun.start);
                mWidget.mParent.mVerticalRun.start.mDependencies.add(start);
                start.mMargin = relativeBegin;
            } else if (relativeEnd != -1) {
                start.mTargets.add(mWidget.mParent.mVerticalRun.end);
                mWidget.mParent.mVerticalRun.end.mDependencies.add(start);
                start.mMargin = -relativeEnd;
            } else {
                start.delegateToWidgetRun = true;
                start.mTargets.add(mWidget.mParent.mVerticalRun.end);
                mWidget.mParent.mVerticalRun.end.mDependencies.add(start);
            }
            // FIXME -- if we move the DependencyNode directly
            //              in the ConstraintAnchor we'll be good.
            addDependency(mWidget.mVerticalRun.start);
            addDependency(mWidget.mVerticalRun.end);
        }
    }

    @Override
    public void applyToWidget() {
        Guideline guideline = (Guideline) mWidget;
        if (guideline.getOrientation() == ConstraintWidget.VERTICAL) {
            mWidget.setX(start.value);
        } else {
            mWidget.setY(start.value);
        }
    }
}
