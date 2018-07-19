/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.worker;

import static androidx.work.Worker.Result.SUCCESS;

import android.support.annotation.NonNull;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

import java.util.List;

public class CheckLimitsWorker extends Worker {
    /* The limit to enforce */
    public static final String KEY_RECURSIVE = "recursive";
    public static final String KEY_LIMIT_TO_ENFORCE = "limit";

    /* The output key which tells us if we exceeded the scheduler limits. */
    public static final String KEY_EXCEEDS_SCHEDULER_LIMIT = "exceed_scheduler_limit";

    @NonNull
    @Override
    public Result doWork() {
        Data input = getInputData();
        boolean isRecursive = input.getBoolean(KEY_RECURSIVE, false);
        int limitToEnforce = input.getInt(KEY_LIMIT_TO_ENFORCE, Scheduler.MAX_SCHEDULER_LIMIT);
        WorkManagerImpl workManager = WorkManagerImpl.getInstance();
        List<WorkSpec> eligibleWorkSpecs = workManager.getWorkDatabase()
                .workSpecDao()
                .getEligibleWorkForScheduling(limitToEnforce);
        int size = eligibleWorkSpecs != null ? eligibleWorkSpecs.size() : 0;
        boolean exceedsLimits = size > limitToEnforce;
        Data output = new Data.Builder()
                .putBoolean(KEY_EXCEEDS_SCHEDULER_LIMIT, exceedsLimits)
                .build();
        setOutputData(output);
        if (isRecursive) {
            // kick off another Worker, which is not recursive.
            Data newRequestData = new Data.Builder()
                    .putAll(getInputData())
                    .putBoolean(KEY_RECURSIVE, false)
                    .build();

            OneTimeWorkRequest newRequest = new OneTimeWorkRequest.Builder(CheckLimitsWorker.class)
                    .setInputData(newRequestData)
                    .build();
            workManager.enqueue(newRequest);
        }
        return SUCCESS;
    }
}
