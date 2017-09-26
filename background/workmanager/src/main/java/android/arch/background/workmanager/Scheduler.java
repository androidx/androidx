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

import android.arch.background.workmanager.model.WorkSpec;

import java.util.List;

/**
 * Schedules {@link Work} depending on completion of {@link Dependency}s.
 */
abstract class Scheduler {
    protected WorkDatabase mWorkDatabase;

    protected Scheduler(WorkDatabase workDatabase) {
        mWorkDatabase = workDatabase;
    }

    /**
     * Schedules the {@link WorkSpec}.
     *
     * @param workSpec {@link WorkSpec} to schedule.
     */
    abstract void schedule(WorkSpec workSpec);

    /**
     * Cancels the {@link WorkSpec}.
     *
     * @param workId The ID of {@link WorkSpec} to cancel.
     * @return boolean indicating if the work was canceled.
     */
    abstract boolean cancel(String workId);

    /**
     * Handles scheduling the {@link WorkSpec}s that were dependent on the {@link WorkSpec} with
     * {@code workId}. Also removes {@link Dependency}s with the {@code prerequisiteWorkId}.
     *
     * @param workId The ID of the {@link WorkSpec} that was completed.
     */
    void onWorkFinished(String workId) {
        DependencyDao dependencyDao = mWorkDatabase.dependencyDao();
        List<String> dependentWorkIds = dependencyDao.getWorkSpecIdsWithSinglePrerequisite(workId);
        for (WorkSpec workSpec : mWorkDatabase.workSpecDao().getWorkSpecs(dependentWorkIds)) {
            schedule(workSpec);
        }
        dependencyDao.deleteDependenciesWithPrerequisite(workId);
    }
}
