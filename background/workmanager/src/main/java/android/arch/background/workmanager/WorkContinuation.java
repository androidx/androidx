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

/**
 * An opaque class that allows you to chain together {@link Work}.
 */

public class WorkContinuation {

    WorkManager mWorkManager;
    String mPrerequisiteId;

    WorkContinuation(WorkManager workManager, String prerequisiteId) {
        mWorkManager = workManager;
        mPrerequisiteId = prerequisiteId;
    }

    /**
     * Add new {@link Work} that depends on the previous one.
     *
     * @param work The {@link Work} to add
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public WorkContinuation then(Work work) {
        return mWorkManager.enqueue(work, mPrerequisiteId);
    }

    /**
     * Add new {@link Work} that depends on the previous one.
     *
     * @param workBuilder The {@link Work.Builder} to add; internally {@code build} is called on it
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public WorkContinuation then(Work.Builder workBuilder) {
        return mWorkManager.enqueue(workBuilder.build(), mPrerequisiteId);
    }

    /**
     * Add new {@link Work} that depends on the previous one.
     *
     * @param workerClass The {@link Worker} to enqueue; this is a convenience method that makes a
     *                    {@link Work} object with default arguments using this Worker
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public WorkContinuation then(Class<? extends Worker> workerClass) {
        return mWorkManager.enqueue(new Work.Builder(workerClass).build(), mPrerequisiteId);
    }
}
