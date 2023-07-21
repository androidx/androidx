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

import static androidx.work.impl.model.WorkSpecKt.generationalId;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.work.Clock;
import androidx.work.Logger;
import androidx.work.impl.constraints.WorkConstraintsTrackerImpl;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.WorkSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a command handler which handles the constraints changed event.
 * Typically this happens for WorkSpec's for which we have pending alarms.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConstraintsCommandHandler {

    private static final String TAG = Logger.tagWithPrefix("ConstraintsCmdHandler");

    private final Context mContext;
    private final Clock mClock;
    private final int mStartId;
    private final SystemAlarmDispatcher mDispatcher;
    private final WorkConstraintsTrackerImpl mWorkConstraintsTracker;

    ConstraintsCommandHandler(
            @NonNull Context context,
            Clock clock,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {
        mContext = context;
        mClock = clock;
        mStartId = startId;
        mDispatcher = dispatcher;
        Trackers trackers = mDispatcher.getWorkManager().getTrackers();
        mWorkConstraintsTracker = new WorkConstraintsTrackerImpl(trackers, null);
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
        long now = mClock.currentTimeMillis();
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
            Intent intent = CommandHandler.createDelayMetIntent(mContext, generationalId(workSpec));
            Logger.get().debug(TAG,
                    "Creating a delay_met command for workSpec with id (" + workSpecId + ")");
            mDispatcher.getTaskExecutor().getMainThreadExecutor().execute(
                    new SystemAlarmDispatcher.AddRunnable(mDispatcher, intent, mStartId));
        }

        mWorkConstraintsTracker.reset();
    }
}
