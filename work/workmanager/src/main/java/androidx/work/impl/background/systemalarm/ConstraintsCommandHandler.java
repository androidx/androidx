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
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;
import android.util.Log;

import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;

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

    private static final String TAG = "ConstraintsCmdHandler";

    private final Context mContext;
    private final int mStartId;
    private final SystemAlarmDispatcher mDispatcher;
    private final List<WorkSpec> mEligibleWorkSpecs;
    private final WorkConstraintsTracker mWorkConstraintsTracker;

    ConstraintsCommandHandler(
            @NonNull Context context,
            int startId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        mContext = context;
        mStartId = startId;
        mDispatcher = dispatcher;
        mWorkConstraintsTracker = new WorkConstraintsTracker(mContext, null);
        mEligibleWorkSpecs = new ArrayList<>();
    }

    @WorkerThread
    void handleConstraintsChanged() {
        int schedulerLimit = mDispatcher
                .getWorkManager()
                .getConfiguration()
                .getMaxSchedulerLimit();

        List<WorkSpec> candidates = mDispatcher.getWorkManager().getWorkDatabase()
                .workSpecDao()
                .getEligibleWorkForScheduling(schedulerLimit);

        // Filter candidates that are marked as SCHEDULE_NOT_REQUESTED_AT
        List<WorkSpec> eligibleWorkSpecs = new ArrayList<>(candidates.size());
        for (WorkSpec candidate: candidates) {
            if (candidate.scheduleRequestedAt != WorkSpec.SCHEDULE_NOT_REQUESTED_YET) {
                eligibleWorkSpecs.add(candidate);
            }
        }

        // Update constraint proxy to potentially disable proxies for previously
        // completed WorkSpecs.
        ConstraintProxy.updateAll(mContext, eligibleWorkSpecs);
        // This needs to be done to populate matching WorkSpec ids in every constraint
        // controller.
        mWorkConstraintsTracker.replace(eligibleWorkSpecs);

        for (WorkSpec workSpec : eligibleWorkSpecs) {
            String workSpecId = workSpec.id;
            if (!workSpec.hasConstraints()
                    || mWorkConstraintsTracker.areAllConstraintsMet(workSpecId)) {
                mEligibleWorkSpecs.add(workSpec);
            }
        }

        for (WorkSpec workSpec : mEligibleWorkSpecs) {
            String workSpecId = workSpec.id;
            Intent intent = CommandHandler.createDelayMetIntent(mContext, workSpecId);
            Log.d(TAG, String.format(
                    "Creating a delay_met command for workSpec with id (%s)", workSpecId));
            mDispatcher.postOnMainThread(
                    new SystemAlarmDispatcher.AddRunnable(mDispatcher, intent, mStartId));
        }
        mWorkConstraintsTracker.reset();
    }
}
