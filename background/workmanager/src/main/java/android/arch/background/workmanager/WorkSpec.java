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
 * Stores all {@link WorkItem}s that constitute one logical chain of work.
 */
public class WorkSpec {
    private List<WorkItem> mWorkItems;

    private WorkSpec(List<WorkItem> workItems) {
        this.mWorkItems = workItems;
    }

    List<WorkItem> getWorkItems() {
        return mWorkItems;
    }

    /**
     * Builder for {@link WorkSpec} class.
     */
    public static class Builder {
        private List<WorkItem> mWorkItems = new ArrayList<>(1);

        public Builder(Class<? extends Worker> workerClass) {
            addNewWorkItem(workerClass);
        }

        /**
         * Chain this {@link WorkSpec} to another {@link Worker} Class.
         *
         * @param workerClass the next {@link Worker} class to chain this WorkSpec to
         * @return current builder
         */
        public Builder then(Class<? extends Worker> workerClass) {
            addNewWorkItem(workerClass);
            return this;
        }

        /**
         * Generates the {@link WorkSpec} from this builder
         *
         * @return new {@link WorkSpec} containing all {@link WorkItem}s
         */
        public WorkSpec build() {
            return new WorkSpec(mWorkItems);
        }

        private void addNewWorkItem(Class<? extends Worker> workerClass) {
            WorkItem workItem = new WorkItem();
            workItem.mWorkerClassName = workerClass.getName();
            mWorkItems.add(workItem);
        }
    }
}
