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

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class to create a logical chain of work, which may contain several dependent steps.
 */

public class Work {

    private WorkSpec mWorkSpec;
    private List<WorkItem> mWorkItems;

    private Work(WorkSpec workSpec, List<WorkItem> workItems) {
        mWorkSpec = workSpec;
        mWorkItems = workItems;
    }

    /**
     * @return The id for this set of work.
     */
    public String getId() {
        return mWorkSpec.mId;
    }

    /**
     * @return A list of ids for each stage of execution.
     */
    public List<String> getWorkItemIds() {
        List<String> ids = new ArrayList<>(mWorkItems.size());
        for (WorkItem workItem : mWorkItems) {
            ids.add(workItem.mId);
        }
        return ids;
    }

    WorkSpec getWorkSpec() {
        return mWorkSpec;
    }

    List<WorkItem> getWorkItems() {
        return mWorkItems;
    }

    List<Dependency> generateDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        for (int i = 1; i < mWorkItems.size(); i++) {
            Dependency dependency = new Dependency();
            dependency.mWorkItemId = mWorkItems.get(i).mId;
            dependency.mPrerequisiteId = mWorkItems.get(i - 1).mId;
            dependencies.add(dependency);
        }
        return dependencies;
    }
    /**
     * Builder for {@link Work} class.
     */
    public static class Builder {
        private WorkSpec mWorkSpec = new WorkSpec(UUID.randomUUID().toString());
        private List<WorkItem> mWorkItems = new ArrayList<>(1);

        public Builder(Class<? extends Worker> workerClass) {
            addNewWorkItem(workerClass);
        }

        private WorkItem getCurrentWorkItem() {
            return mWorkItems.get(mWorkItems.size() - 1);
        }

        /**
         * Add constraints to the current {@link WorkItem}.
         *
         * @param constraints the constraints to attach to the work item
         * @return current builder
         */
        public Builder withConstraints(@NonNull Constraints constraints) {
            getCurrentWorkItem().mConstraints = constraints;
            return this;
        }

        /**
         * Chain this {@link Work} to another {@link Worker} Class.
         *
         * @param workerClass the next {@link Worker} class to chain this Work to
         * @return current {@link Builder}
         */
        public Builder then(Class<? extends Worker> workerClass) {
            addNewWorkItem(workerClass);
            return this;
        }

        /**
         * Change backoff policy and delay for the current {@link WorkItem}.
         * Default is {@value WorkItem#BACKOFF_POLICY_EXPONENTIAL} and 30 seconds.
         *
         * @param backoffPolicy Backoff Policy to use for current {@link WorkItem}
         * @param backoffDelayDuration Time to wait before restarting {@link Worker}
         *                             (in milliseconds)
         * @return current builder
         */
        public Builder withBackoffCriteria(@WorkItem.BackoffPolicy int backoffPolicy,
                                           long backoffDelayDuration) {
            // TODO(xbhatnag): Enforce restrictions on backoff delay. 30 seconds?
            getCurrentWorkItem().mBackoffPolicy = backoffPolicy;
            getCurrentWorkItem().mBackoffDelayDuration = backoffDelayDuration;
            return this;
        }

        /**
         * Add arguments to the current {@link WorkItem}.
         * @param arguments key/value pairs that will be provided to the {@link Worker} class
         * @return current builder
         */
        public Builder withArguments(Arguments arguments) {
            getCurrentWorkItem().mArguments = arguments;
            return this;
        }

        /**
         * Generates the {@link Work} from this builder
         *
         * @return new {@link Work}
         */
        public Work build() {
            return new Work(mWorkSpec, mWorkItems);
        }

        private void addNewWorkItem(Class<? extends Worker> workerClass) {
            WorkItem workItem = new WorkItem(UUID.randomUUID().toString(), mWorkSpec.mId);
            workItem.mWorkerClassName = workerClass.getName();
            mWorkItems.add(workItem);
        }
    }
}
