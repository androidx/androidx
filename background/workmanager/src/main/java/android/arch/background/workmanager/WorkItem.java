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

package android.arch.background.workmanager;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores all {@link Blueprint}s that constitute one item of work.
 */
public class WorkItem {
    private List<Blueprint> mBlueprints;

    private WorkItem(List<Blueprint> blueprints) {
        this.mBlueprints = blueprints;
    }

    List<Blueprint> getBlueprints() {
        return mBlueprints;
    }

    /**
     * Builder for {@link WorkItem} class.
     */
    public static class Builder {
        private List<Blueprint> mBlueprints = new ArrayList<>(1);

        public Builder(Class<? extends Worker> workerClass) {
            addNewBlueprint(workerClass);
        }

        /**
         * Chain this {@link WorkItem} to another {@link Worker} Class.
         *
         * @param workerClass the next {@link Worker} class to chain this WorkItem to
         * @return current builder
         */
        public Builder then(Class<? extends Worker> workerClass) {
            addNewBlueprint(workerClass);
            return this;
        }

        /**
         * Generates the {@link WorkItem} from this builder
         *
         * @return new {@link WorkItem} containing all {@link Blueprint}s
         */
        public WorkItem build() {
            return new WorkItem(mBlueprints);
        }

        private void addNewBlueprint(Class<? extends Worker> workerClass) {
            Blueprint blueprint = new Blueprint();
            blueprint.mWorkerClassName = workerClass.getName();
            mBlueprints.add(blueprint);
        }
    }
}
