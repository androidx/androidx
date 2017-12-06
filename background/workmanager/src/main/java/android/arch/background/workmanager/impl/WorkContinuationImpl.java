/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager.impl;

import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkContinuation;
import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.utils.BaseWorkHelper;
import android.support.annotation.RestrictTo;

/**
 * A concrete implementation of {@link WorkContinuation}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkContinuationImpl extends WorkContinuation {

    private WorkManagerImpl mWorkManagerImpl;
    private String[] mPrerequisiteIds;

    WorkContinuationImpl(WorkManagerImpl workManagerImpl, Work[] prerequisiteWork) {
        mWorkManagerImpl = workManagerImpl;
        mPrerequisiteIds = new String[prerequisiteWork.length];
        for (int i = 0; i < prerequisiteWork.length; ++i) {
            mPrerequisiteIds[i] = prerequisiteWork[i].getId();
        }
    }

    @Override
    public WorkContinuation then(Work... work) {
        return mWorkManagerImpl.enqueue(work, mPrerequisiteIds);
    }

    @SafeVarargs
    @Override
    public final WorkContinuation then(Class<? extends Worker>... workerClasses) {
        return mWorkManagerImpl.enqueue(
                BaseWorkHelper.convertWorkerClassArrayToWorkArray(workerClasses),
                mPrerequisiteIds);
    }
}
