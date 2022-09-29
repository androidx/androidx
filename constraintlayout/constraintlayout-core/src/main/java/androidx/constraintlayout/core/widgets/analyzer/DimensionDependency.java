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

class DimensionDependency extends DependencyNode {

    public int wrapValue;

    DimensionDependency(WidgetRun run) {
        super(run);
        if (run instanceof HorizontalWidgetRun) {
            mType = Type.HORIZONTAL_DIMENSION;
        } else {
            mType = Type.VERTICAL_DIMENSION;
        }
    }

    @Override
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

}
