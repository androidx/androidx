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

package androidx.work.impl.background.systemalarm;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.work.Logger;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a command handler which handles the constraints changed event.
 * Typically this happens for WorkSpec's for which we have pending alarms.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConstraintsCommandHandler {

    private static final String TAG = Logger.tagWithPrefix("ConstraintsCmdHandler");

    private final Context mContext;
    private final int mStartId;
    private final SystemAlarmDispatcher mDispatcher;
    private final WorkConstraintsTracker mWorkConstraintsTracker;

    ConstraintsCommandHandler(
            @NonNull Context context,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        mContext = context;
        mStartId = startId;
        mDispatcher = dispatcher;
        TaskExecutor taskExecutor = mDispatcher.getTaskExecutor();
        mWorkConstraintsTracker = new WorkConstraintsTracker(mContext, taskExecutor, null);
    }

    @WorkerThread
    void handleConstraintsChanged() {
        List<WorkSpec> candidates = mDispatcher.getWorkManager().getWorkDatabase()
                .workSpecDao()
                .getScheduledWork();

        // Update constraint proxy to potentially disable proxies for previously
        // completed WorkSpecs.
        ConstraintProxy.updateAll(mContext, candidates);

        // This needs to be done to populate matching WorkSpec ids in every constraint controller.
        mWorkConstraintsTracker.replace(candidates);

        List<WorkSpec> eligibleWorkSpecs = new ArrayList<>(candidates.size());
        // Filter candidates should have already been scheduled.
        long now = System.currentTimeMillis();
        for (WorkSpec workSpec : candidates) {
            String workSpecId = workSpec.id;
            long triggerAt = workSpec.calculateNextRunTime();
            if (now >= triggerAt && (!workSpec.hasConstraints()
                    || mWorkConstraintsTracker.areAllConstraintsMet(workSpecId))) {
                eligibleWorkSpecs.add(workSpec);
            }
        }

        for (WorkSpec workSpec : eligibleWorkSpecs) {
            String workSpecId = workSpec.id;
            Intent intent = CommandHandler.createDelayMetIntent(mContext, workSpecId);
            Logger.get().debug(TAG, "Creating a delay_met command for workSpec with id (" + workSpecId + ")");
            mDispatcher.postOnMainThread(
                    new SystemAlarmDispatcher.AddRunnable(mDispatcher, intent, mStartId));
        }

        mWorkConstraintsTracker.reset();
    }
}
