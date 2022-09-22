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

import java.util.ArrayList;
import java.util.List;

public class DependencyNode implements Dependency {
    public Dependency updateDelegate = null;
    public boolean delegateToWidgetRun = false;
    public boolean readyToSolve = false;

    enum Type {
        UNKNOWN, HORIZONTAL_DIMENSION, VERTICAL_DIMENSION,
        LEFT, RIGHT, TOP, BOTTOM, BASELINE
    }

    WidgetRun mRun;
    Type mType = Type.UNKNOWN;
    int mMargin;
    public int value;
    int mMarginFactor = 1;
    DimensionDependency mMarginDependency = null;
    public boolean resolved = false;

    public DependencyNode(WidgetRun run) {
        this.mRun = run;
    }

    List<Dependency> mDependencies = new ArrayList<>();
    List<DependencyNode> mTargets = new ArrayList<>();

    @Override
    public String toString() {
        return mRun.mWidget.getDebugName() + ":" + mType + "("
                + (resolved ? value : "unresolved") + ") <t="
                + mTargets.size() + ":d=" + mDependencies.size() + ">";
    }

    // @TODO: add description
    public void resolve(int value) {
        if (resolved) {
            return;
        }

        this.resolved = true;
        this.value = value;
        for (Dependency node : mDependencies) {
            node.update(node);
        }
    }

    // @TODO: add description
    @Override
    public void update(Dependency node) {
        for (DependencyNode target : mTargets) {
            if (!target.resolved) {
                return;
            }
        }
        readyToSolve = true;
        if (updateDelegate != null) {
            updateDelegate.update(this);
        }
        if (delegateToWidgetRun) {
            mRun.update(this);
            return;
        }
        DependencyNode target = null;
        int numTargets = 0;
        for (DependencyNode t : mTargets) {
            if (t instanceof DimensionDependency) {
                continue;
            }
            target = t;
            numTargets++;
        }
        if (target != null && numTargets == 1 && target.resolved) {
            if (mMarginDependency != null) {
                if (mMarginDependency.resolved) {
                    mMargin = mMarginFactor * mMarginDependency.value;
                } else {
                    return;
                }
            }
            resolve(target.value + mMargin);
        }
        if (updateDelegate != null) {
            updateDelegate.update(this);
        }
    }

    // @TODO: add description
    public void addDependency(Dependency dependency) {
        mDependencies.add(dependency);
        if (resolved) {
            dependency.update(dependency);
        }
    }

    // @TODO: add description
    public String name() {
        String definition = mRun.mWidget.getDebugName();
        if (mType == Type.LEFT
                || mType == Type.RIGHT) {
            definition += "_HORIZONTAL";
        } else {
            definition += "_VERTICAL";
        }
        definition += ":" + mType.name();
        return definition;
    }

    // @TODO: add description
    public void clear() {
        mTargets.clear();
        mDependencies.clear();
        resolved = false;
        value = 0;
        readyToSolve = false;
        delegateToWidgetRun = false;
    }
}
