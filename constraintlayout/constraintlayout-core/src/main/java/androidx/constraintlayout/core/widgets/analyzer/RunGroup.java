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
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;

import java.util.ArrayList;

class RunGroup {
    public static final int START = 0;
    public static final int END = 1;
    public static final int BASELINE = 2;

    public static int index;

    public int position = 0;
    public boolean dual = false;

    WidgetRun mFirstRun = null;
    WidgetRun mLastRun = null;
    ArrayList<WidgetRun> mRuns = new ArrayList<>();

    int mGroupIndex = 0;
    int mDirection;

    RunGroup(WidgetRun run, int dir) {
        mGroupIndex = index;
        index++;
        mFirstRun = run;
        mLastRun = run;
        mDirection = dir;
    }

    public void add(WidgetRun run) {
        mRuns.add(run);
        mLastRun = run;
    }

    private long traverseStart(DependencyNode node, long startPosition) {
        WidgetRun run = node.mRun;
        if (run instanceof HelperReferences) {
            return startPosition;
        }
        long position = startPosition;

        // first, compute stuff dependent on this node.

        final int count = node.mDependencies.size();
        for (int i = 0; i < count; i++) {
            Dependency dependency = node.mDependencies.get(i);
            if (dependency instanceof DependencyNode) {
                DependencyNode nextNode = (DependencyNode) dependency;
                if (nextNode.mRun == run) {
                    // skip our own sibling node
                    continue;
                }
                position = Math.max(position,
                        traverseStart(nextNode, startPosition + nextNode.mMargin));
            }
        }

        if (node == run.start) {
            // let's go for our sibling
            long dimension = run.getWrapDimension();
            position = Math.max(position, traverseStart(run.end, startPosition + dimension));
            position = Math.max(position, startPosition + dimension - run.end.mMargin);
        }

        return position;
    }

    private long traverseEnd(DependencyNode node, long startPosition) {
        WidgetRun run = node.mRun;
        if (run instanceof HelperReferences) {
            return startPosition;
        }
        long position = startPosition;

        // first, compute stuff dependent on this node.

        final int count = node.mDependencies.size();
        for (int i = 0; i < count; i++) {
            Dependency dependency = node.mDependencies.get(i);
            if (dependency instanceof DependencyNode) {
                DependencyNode nextNode = (DependencyNode) dependency;
                if (nextNode.mRun == run) {
                    // skip our own sibling node
                    continue;
                }
                position = Math.min(position,
                        traverseEnd(nextNode, startPosition + nextNode.mMargin));
            }
        }

        if (node == run.end) {
            // let's go for our sibling
            long dimension = run.getWrapDimension();
            position = Math.min(position, traverseEnd(run.start, startPosition - dimension));
            position = Math.min(position, startPosition - dimension - run.start.mMargin);
        }

        return position;
    }

    public long computeWrapSize(ConstraintWidgetContainer container, int orientation) {
        if (mFirstRun instanceof ChainRun) {
            ChainRun chainRun = (ChainRun) mFirstRun;
            if (chainRun.orientation != orientation) {
                return 0;
            }
        } else {
            if (orientation == HORIZONTAL) {
                if (!(mFirstRun instanceof HorizontalWidgetRun)) {
                    return 0;
                }
            } else {
                if (!(mFirstRun instanceof VerticalWidgetRun)) {
                    return 0;
                }
            }
        }
        DependencyNode containerStart = orientation == HORIZONTAL
                ? container.mHorizontalRun.start : container.mVerticalRun.start;
        DependencyNode containerEnd = orientation == HORIZONTAL
                ? container.mHorizontalRun.end : container.mVerticalRun.end;

        boolean runWithStartTarget = mFirstRun.start.mTargets.contains(containerStart);
        boolean runWithEndTarget = mFirstRun.end.mTargets.contains(containerEnd);

        long dimension = mFirstRun.getWrapDimension();

        if (runWithStartTarget && runWithEndTarget) {
            long maxPosition = traverseStart(mFirstRun.start, 0);
            long minPosition = traverseEnd(mFirstRun.end, 0);

            // to compute the gaps, we subtract the margins
            long endGap = maxPosition - dimension;
            if (endGap >= -mFirstRun.end.mMargin) {
                endGap += mFirstRun.end.mMargin;
            }
            long startGap = -minPosition - dimension - mFirstRun.start.mMargin;
            if (startGap >= mFirstRun.start.mMargin) {
                startGap -= mFirstRun.start.mMargin;
            }
            float bias = mFirstRun.mWidget.getBiasPercent(orientation);
            long gap = 0;
            if (bias > 0) {
                gap = (long) ((startGap / bias) + (endGap / (1f - bias)));
            }

            startGap = (long) (0.5f + (gap * bias));
            endGap = (long) (0.5f + (gap * (1f - bias)));

            long runDimension = startGap + dimension + endGap;
            dimension = mFirstRun.start.mMargin + runDimension - mFirstRun.end.mMargin;

        } else if (runWithStartTarget) {
            long maxPosition = traverseStart(mFirstRun.start, mFirstRun.start.mMargin);
            long runDimension = mFirstRun.start.mMargin + dimension;
            dimension = Math.max(maxPosition, runDimension);
        } else if (runWithEndTarget) {
            long minPosition = traverseEnd(mFirstRun.end, mFirstRun.end.mMargin);
            long runDimension = -mFirstRun.end.mMargin + dimension;
            dimension = Math.max(-minPosition, runDimension);
        } else {
            dimension = mFirstRun.start.mMargin
                    + mFirstRun.getWrapDimension() - mFirstRun.end.mMargin;
        }

        return dimension;
    }

    private boolean defineTerminalWidget(WidgetRun run, int orientation) {
        if (!run.mWidget.isTerminalWidget[orientation]) {
            return false;
        }
        for (Dependency dependency : run.start.mDependencies) {
            if (dependency instanceof DependencyNode) {
                DependencyNode node = (DependencyNode) dependency;
                if (node.mRun == run) {
                    continue;
                }
                if (node == node.mRun.start) {
                    if (run instanceof ChainRun) {
                        ChainRun chainRun = (ChainRun) run;
                        for (WidgetRun widgetChainRun : chainRun.mWidgets) {
                            defineTerminalWidget(widgetChainRun, orientation);
                        }
                    } else {
                        if (!(run instanceof HelperReferences)) {
                            run.mWidget.isTerminalWidget[orientation] = false;
                        }
                    }
                    defineTerminalWidget(node.mRun, orientation);
                }
            }
        }
        for (Dependency dependency : run.end.mDependencies) {
            if (dependency instanceof DependencyNode) {
                DependencyNode node = (DependencyNode) dependency;
                if (node.mRun == run) {
                    continue;
                }
                if (node == node.mRun.start) {
                    if (run instanceof ChainRun) {
                        ChainRun chainRun = (ChainRun) run;
                        for (WidgetRun widgetChainRun : chainRun.mWidgets) {
                            defineTerminalWidget(widgetChainRun, orientation);
                        }
                    } else {
                        if (!(run instanceof HelperReferences)) {
                            run.mWidget.isTerminalWidget[orientation] = false;
                        }
                    }
                    defineTerminalWidget(node.mRun, orientation);
                }
            }
        }
        return false;
    }


    public void defineTerminalWidgets(boolean horizontalCheck, boolean verticalCheck) {
        if (horizontalCheck && mFirstRun instanceof HorizontalWidgetRun) {
            defineTerminalWidget(mFirstRun, HORIZONTAL);
        }
        if (verticalCheck && mFirstRun instanceof VerticalWidgetRun) {
            defineTerminalWidget(mFirstRun, VERTICAL);
        }
    }
}
