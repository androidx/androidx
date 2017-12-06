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

import android.arch.background.workmanager.utils.BaseWorkHelper;

/**
 * An opaque class that allows you to chain together {@link Work}.
 */

public class WorkContinuation {

    private WorkManagerImpl mWorkManagerImpl;
    private String[] mPrerequisiteIds;

    WorkContinuation(WorkManagerImpl workManagerImpl, Work[] prerequisiteWork) {
        mWorkManagerImpl = workManagerImpl;
        mPrerequisiteIds = new String[prerequisiteWork.length];
        for (int i = 0; i < prerequisiteWork.length; ++i) {
            mPrerequisiteIds[i] = prerequisiteWork[i].getId();
        }
    }

    /**
     * Add new {@link Work} items that depend on the items added in the previous step.
     *
     * @param work One or more {@link Work} to enqueue
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input work
     */
    public final WorkContinuation then(Work... work) {
        return mWorkManagerImpl.enqueue(work, mPrerequisiteIds);
    }

    /**
     * Add new {@link Work} items that depend on the items added in the previous step.
     *
     * @param workerClasses One or more {@link Worker}s to enqueue; this is a convenience method
     *                      that makes a {@link Work} object with default arguments for each Worker
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input workerClasses
     */
    @SafeVarargs
    public final WorkContinuation then(Class<? extends Worker>... workerClasses) {
        return mWorkManagerImpl.enqueue(
                BaseWorkHelper.convertWorkerClassArrayToWorkArray(workerClasses),
                mPrerequisiteIds);
    }
}
