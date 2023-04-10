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

import androidx.constraintlayout.core.widgets.Barrier;
import androidx.constraintlayout.core.widgets.ConstraintWidget;

class HelperReferences extends WidgetRun {
    HelperReferences(ConstraintWidget widget) {
        super(widget);
    }

    @Override
    void clear() {
        mRunGroup = null;
        start.clear();
    }

    @Override
    void reset() {
        start.resolved = false;
    }

    @Override
    boolean supportsWrapComputation() {
        return false;
    }

    private void addDependency(DependencyNode node) {
        start.mDependencies.add(node);
        node.mTargets.add(start);
    }

    @Override
    void apply() {
        if (mWidget instanceof Barrier) {
            start.delegateToWidgetRun = true;
            Barrier barrier = (Barrier) mWidget;
            int type = barrier.getBarrierType();
            boolean allowsGoneWidget = barrier.getAllowsGoneWidget();
            switch (type) {
                case Barrier.LEFT: {
                    start.mType = DependencyNode.Type.LEFT;
                    for (int i = 0; i < barrier.mWidgetsCount; i++) {
                        ConstraintWidget refWidget = barrier.mWidgets[i];
                        if (!allowsGoneWidget
                                && refWidget.getVisibility() == ConstraintWidget.GONE) {
                            continue;
                        }
                        DependencyNode target = refWidget.mHorizontalRun.start;
                        target.mDependencies.add(start);
                        start.mTargets.add(target);
                        // FIXME -- if we move the DependencyNode directly
                        //          in the ConstraintAnchor we'll be good.
                    }
                    addDependency(mWidget.mHorizontalRun.start);
                    addDependency(mWidget.mHorizontalRun.end);
                }
                break;
                case Barrier.RIGHT: {
                    start.mType = DependencyNode.Type.RIGHT;
                    for (int i = 0; i < barrier.mWidgetsCount; i++) {
                        ConstraintWidget refWidget = barrier.mWidgets[i];
                        if (!allowsGoneWidget
                                && refWidget.getVisibility() == ConstraintWidget.GONE) {
                            continue;
                        }
                        DependencyNode target = refWidget.mHorizontalRun.end;
                        target.mDependencies.add(start);
                        start.mTargets.add(target);
                        // FIXME -- if we move the DependencyNode directly
                        //              in the ConstraintAnchor we'll be good.
                    }
                    addDependency(mWidget.mHorizontalRun.start);
                    addDependency(mWidget.mHorizontalRun.end);
                }
                break;
                case Barrier.TOP: {
                    start.mType = DependencyNode.Type.TOP;
                    for (int i = 0; i < barrier.mWidgetsCount; i++) {
                        ConstraintWidget refwidget = barrier.mWidgets[i];
                        if (!allowsGoneWidget
                                && refwidget.getVisibility() == ConstraintWidget.GONE) {
                            continue;
                        }
                        DependencyNode target = refwidget.mVerticalRun.start;
                        target.mDependencies.add(start);
                        start.mTargets.add(target);
                        // FIXME -- if we move the DependencyNode directly
                        //              in the ConstraintAnchor we'll be good.
                    }
                    addDependency(mWidget.mVerticalRun.start);
                    addDependency(mWidget.mVerticalRun.end);
                }
                break;
                case Barrier.BOTTOM: {
                    start.mType = DependencyNode.Type.BOTTOM;
                    for (int i = 0; i < barrier.mWidgetsCount; i++) {
                        ConstraintWidget refwidget = barrier.mWidgets[i];
                        if (!allowsGoneWidget
                                && refwidget.getVisibility() == ConstraintWidget.GONE) {
                            continue;
                        }
                        DependencyNode target = refwidget.mVerticalRun.end;
                        target.mDependencies.add(start);
                        start.mTargets.add(target);
                        // FIXME -- if we move the DependencyNode directly
                        //              in the ConstraintAnchor we'll be good.
                    }
                    addDependency(mWidget.mVerticalRun.start);
                    addDependency(mWidget.mVerticalRun.end);
                }
                break;
            }
        }
    }

    @Override
    public void update(Dependency dependency) {
        Barrier barrier = (Barrier) mWidget;
        int type = barrier.getBarrierType();

        int min = -1;
        int max = 0;
        for (DependencyNode node : start.mTargets) {
            int value = node.value;
            if (min == -1 || value < min) {
                min = value;
            }
            if (max < value) {
                max = value;
            }
        }
        if (type == Barrier.LEFT || type == Barrier.TOP) {
            start.resolve(min + barrier.getMargin());
        } else {
            start.resolve(max + barrier.getMargin());
        }
    }

    @Override
    public void applyToWidget() {
        if (mWidget instanceof Barrier) {
            Barrier barrier = (Barrier) mWidget;
            int type = barrier.getBarrierType();
            if (type == Barrier.LEFT
                    || type == Barrier.RIGHT) {
                mWidget.setX(start.value);
            } else {
                mWidget.setY(start.value);
            }
        }
    }
}
