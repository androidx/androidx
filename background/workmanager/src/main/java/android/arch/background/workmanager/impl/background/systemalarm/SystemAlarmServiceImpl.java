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

package android.arch.background.workmanager.impl.background.systemalarm;

import static android.arch.background.workmanager.State.ENQUEUED;

import android.app.Service;
import android.arch.background.workmanager.Constraints;
import android.arch.background.workmanager.impl.background.BackgroundProcessor;
import android.arch.background.workmanager.impl.constraints.WorkConstraintsTracker;
import android.arch.background.workmanager.impl.logger.Logger;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SystemAlarmServiceImpl {
    private static final String TAG = "SystemAlarmServiceImpl";

    static final String EXTRA_WORK_ID = "EXTRA_WORK_ID";
    static final String ACTION_DELAY_MET = "DELAY_MET";
    static final String ACTION_CANCEL_WORK = "CANCEL_WORK";
    static final String ACTION_CONSTRAINT_CHANGED = "CONSTRAINT_CHANGED";

    private final List<WorkSpec> mDelayMetWorkSpecs = new ArrayList<>();
    private final List<WorkSpec> mDelayNotMetWorkSpecs = new ArrayList<>();
    private final Object mLock = new Object();
    private final Context mContext;
    private final AllWorkExecutedCallback mCallback;
    private final BackgroundProcessor mProcessor;
    private final WorkConstraintsTracker mWorkConstraintsTracker;

    SystemAlarmServiceImpl(
            @NonNull Context context,
            @NonNull BackgroundProcessor processor,
            @NonNull WorkConstraintsTracker workConstraintsTracker,
            @NonNull AllWorkExecutedCallback callback) {
        mContext = context.getApplicationContext();
        mProcessor = processor;
        mWorkConstraintsTracker = workConstraintsTracker;
        mCallback = callback;
    }

    int onStartCommand(@Nullable Intent intent) {
        // TODO(janclarin): Keep wakelock.
        if (intent == null || intent.getAction() == null) {
            Logger.error(TAG, "Null intent/action. Likely caused by sticky service");
            return Service.START_STICKY;
        }

        final String workSpecId = intent.getStringExtra(EXTRA_WORK_ID);

        switch (intent.getAction()) {
            case ACTION_DELAY_MET:
                onDelayMet(workSpecId);
                break;
            case ACTION_CANCEL_WORK:
                Logger.debug(TAG, "Cancelling work %s", workSpecId);
                mProcessor.cancel(workSpecId, true);
                break;
            case ACTION_CONSTRAINT_CHANGED:
                Logger.debug(TAG, "Constraint Changed. Service woken");
                break;
        }

        return Service.START_STICKY;
    }

    private void updateConstraintsTrackerAndProxy() {
        mWorkConstraintsTracker.replace(mDelayMetWorkSpecs);
        ConstraintProxy.updateAll(mContext, mDelayMetWorkSpecs);
    }

    /**
     * May be invoked before the LiveData is ready and mDelayNotMetWorkSpecs would be empty. When
     * LiveData is ready, the delays of all WorkSpecs will be checked against current time.
     *
     * @param workSpecId ID of {@link WorkSpec} whose delay has been met
     */
    private void onDelayMet(@NonNull String workSpecId) {
        Logger.debug(TAG, "Delay Met intent received for %s", workSpecId);
        synchronized (mLock) {
            WorkSpec workSpec = removeWorkSpecWithId(workSpecId, mDelayNotMetWorkSpecs);
            if (workSpec == null) {
                Logger.debug(TAG, "Could not find %s in Delay Not Met list."
                        + " Is LiveData initialized right now?", workSpecId);
            } else if (hasConstraints(workSpec)) {
                Logger.debug(TAG, "Observing constraints for %s", workSpec);
                mDelayMetWorkSpecs.add(workSpec);
                updateConstraintsTrackerAndProxy();
            } else if (isEnqueued(workSpec)) {
                Logger.debug(TAG, "Processing %s immediately", workSpec);
                mProcessor.process(workSpec.getId());
            } else {
                Logger.debug(TAG, "%s is unconstrained and currently running", workSpec);
            }
        }
    }

    void onEligibleWorkChanged(@NonNull List<WorkSpec> workSpecs) {
        synchronized (mLock) {
            mDelayMetWorkSpecs.clear();
            mDelayNotMetWorkSpecs.clear();

            for (WorkSpec workSpec : workSpecs) {
                if (!isDelayMet(workSpec)) {
                    Logger.debug(TAG, "Delay not met for %s", workSpec);
                    mDelayNotMetWorkSpecs.add(workSpec);
                } else if (hasConstraints(workSpec)) {
                    Logger.debug(TAG, "Observing constraints for %s", workSpec);
                    mDelayMetWorkSpecs.add(workSpec);
                } else if (isEnqueued(workSpec)) {
                    Logger.debug(TAG, "Processing %s immediately", workSpec);
                    mProcessor.process(workSpec.getId());
                } else {
                    Logger.debug(TAG, "%s is unconstrained and currently running", workSpec);
                }
            }

            updateConstraintsTrackerAndProxy();
        }
    }

    void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            mProcessor.process(workSpecId);
        }
    }

    void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            mProcessor.cancel(workSpecId, true);
        }
    }

    void onExecuted(@NonNull String workSpecId, boolean isSuccessful, boolean needsReschedule) {
        // TODO(janclarin): Handle rescheduling if needed or if periodic.

        synchronized (mLock) {
            WorkSpec executedWorkSpec = removeWorkSpecWithId(workSpecId, mDelayMetWorkSpecs);
            if (executedWorkSpec != null) {
                updateConstraintsTrackerAndProxy();
            } else {
                Logger.debug(TAG, "%s not found in DelayMetWorkSpecs. May not have delay",
                        workSpecId);
            }
        }

        if (!mProcessor.hasWork()) {
            // TODO(janclarin): Release wakelock.
            mCallback.onAllWorkExecuted();
        }
    }

    @VisibleForTesting
    List<WorkSpec> getDelayMetWorkSpecs() {
        return mDelayMetWorkSpecs;
    }

    @VisibleForTesting
    List<WorkSpec> getDelayNotMetWorkSpecs() {
        return mDelayNotMetWorkSpecs;
    }

    private static boolean isDelayMet(@NonNull WorkSpec workSpec) {
        return workSpec.calculateNextRunTime() <= System.currentTimeMillis();
    }

    private static boolean hasConstraints(@NonNull WorkSpec workSpec) {
        return !Constraints.NONE.equals(workSpec.getConstraints());
    }

    private static boolean isEnqueued(@NonNull WorkSpec workSpec) {
        return workSpec.getState() == ENQUEUED;
    }

    private static WorkSpec removeWorkSpecWithId(String workSpecId, List<WorkSpec> workSpecs) {
        Iterator<WorkSpec> iterator = workSpecs.iterator();
        while (iterator.hasNext()) {
            WorkSpec workSpec = iterator.next();
            if (workSpec.getId().equals(workSpecId)) {
                iterator.remove();
                return workSpec;
            }
        }
        return null;
    }

    interface AllWorkExecutedCallback {
        void onAllWorkExecuted();
    }
}
