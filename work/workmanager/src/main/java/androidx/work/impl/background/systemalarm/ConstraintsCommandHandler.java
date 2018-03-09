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

import java.util.ArrayList;
import java.util.List;

import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.logger.Logger;
import androidx.work.impl.model.WorkSpec;

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
        long maxStartTime = System.currentTimeMillis();

        List<WorkSpec> eligibleWorkSpecs = mDispatcher.getWorkManager().getWorkDatabase()
                .workSpecDao()
                .getEligibleWorkSpecs(maxStartTime);

        // Update constraint proxy to potentially disable proxies for previously
        // completed WorkSpecs.
        ConstraintProxy.updateAll(mContext, eligibleWorkSpecs);
        // This needs to be done to populate matching WorkSpec ids in every constraint
        // controller.
        mWorkConstraintsTracker.replace(eligibleWorkSpecs);

        for (WorkSpec workSpec : eligibleWorkSpecs) {
            String workSpecId = workSpec.getId();
            if (!workSpec.hasConstraints()
                    || mWorkConstraintsTracker.areAllConstraintsMet(workSpecId)) {
                mEligibleWorkSpecs.add(workSpec);
            }
        }

        for (WorkSpec workSpec : mEligibleWorkSpecs) {
            String workSpecId = workSpec.getId();
            Intent intent = CommandHandler.createDelayMetIntent(mContext, workSpecId);
            Logger.debug(TAG, "Creating a delay_met command for workSpec with id (%s)", workSpecId);
            mDispatcher.postOnMainThread(
                    new SystemAlarmDispatcher.AddRunnable(mDispatcher, intent, mStartId));
        }
        mWorkConstraintsTracker.reset();
    }
}
