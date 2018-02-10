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

package android.arch.background.workmanager.impl.background.systemalarm;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

import android.arch.background.workmanager.impl.ExecutionListener;
import android.arch.background.workmanager.impl.constraints.WorkConstraintsCallback;
import android.arch.background.workmanager.impl.constraints.WorkConstraintsTracker;
import android.arch.background.workmanager.impl.logger.Logger;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.Collections;
import java.util.List;

/**
 * This is a command handler which attempts to run a work spec given its id.
 * Also handles constraints gracefully.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DelayMetCommandHandler implements
        WorkConstraintsCallback,
        ExecutionListener,
        WorkTimer.TimeLimitExceededListener {

    private static final String TAG = "DelayMetCommandHandler";

    private final Context mContext;
    private final int mStartId;
    private final String mWorkSpecId;
    private final PowerManager mPowerManager;
    private final SystemAlarmDispatcher mDispatcher;
    private final WorkConstraintsTracker mWorkConstraintsTracker;
    private final Object mLock;
    private boolean mHasPendingStopWorkCommand;

    @Nullable private PowerManager.WakeLock mWakeLock;
    private boolean mHasConstraints;

    DelayMetCommandHandler(
            @NonNull Context context,
            int startId,
            @NonNull String workSpecId,
            @NonNull SystemAlarmDispatcher dispatcher) {

        mContext = context;
        mStartId = startId;
        mDispatcher = dispatcher;
        mWorkSpecId = workSpecId;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWorkConstraintsTracker = new WorkConstraintsTracker(mContext, this);
        mHasConstraints = false;
        mHasPendingStopWorkCommand = false;
        mLock = new Object();
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> ignored) {
        Logger.debug(TAG, "onAllConstraintsMet for %s", mWorkSpecId);
        // constraints met, schedule execution
        boolean isEnqueued = mDispatcher.getProcessor().startWork(mWorkSpecId);

        if (isEnqueued) {
            // setup timers to enforce quotas on workers that have
            // been enqueued
            mDispatcher.getWorkTimer()
                    .startTimer(mWorkSpecId, CommandHandler.WORK_PROCESSING_TIME_IN_MS, this);
        } else {
            // if we did not actually enqueue the work, it was enqueued before
            // cleanUp and pretend this never happened.
            cleanUp();
        }
    }

    @Override
    public void onExecuted(
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {

        Logger.debug(TAG, "onExecuted %s, %s, %s", workSpecId, isSuccessful, needsReschedule);
        cleanUp();

        if (mHasConstraints) {
            // The WorkSpec had constraints. Once the execution of the worker is complete,
            // we might need to disable constraint proxies which were previously enabled for
            // this WorkSpec. Hence, trigger a constraints changed command.
            Intent intent = CommandHandler.createConstraintsChangedIntent(mContext);
            mDispatcher.postOnMainThread(
                    new SystemAlarmDispatcher.AddRunnable(mDispatcher, intent, mStartId));
        }
    }

    @Override
    public void onTimeLimitExceeded(@NonNull String workSpecId) {
        //TODO (rahulrav@) Check if we need to re-schedule
        Logger.debug(TAG, "Exceeded time limits on execution for %s", workSpecId);
        stopWork();
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> ignored) {
        stopWork();
    }

    @WorkerThread
    void handleProcessWork() {
        mWakeLock = newWakeLock();
        Logger.debug(TAG, "Acquiring wakelock %s for WorkSpec %s", mWakeLock, mWorkSpecId);
        mWakeLock.acquire();

        WorkSpec workSpec = mDispatcher.getWorkManager()
                .getWorkDatabase()
                .workSpecDao()
                .getWorkSpec(mWorkSpecId);

        // Keep track of whether the WorkSpec had constraints. This is useful for updating the
        // state of constraint proxies when onExecuted().
        mHasConstraints = workSpec.hasConstraints();

        if (!mHasConstraints) {
            Logger.debug(TAG, "No constraints for %s", mWorkSpecId);
            onAllConstraintsMet(Collections.singletonList(mWorkSpecId));
        } else {
            // Allow tracker to report constraint changes
            mWorkConstraintsTracker.replace(Collections.singletonList(workSpec));
        }
    }

    private void stopWork() {
        // No need to release the wake locks here. The stopWork command will eventually call
        // onExecuted() if there is a corresponding pending delay met command handler; which in
        // turn calls cleanUp().

        // Needs to be synchronized, as the stopWork() request can potentially come from the
        // WorkTimer thread as well as the command executor service in SystemAlarmDispatcher.
        synchronized (mLock) {
            if (!mHasPendingStopWorkCommand) {
                Logger.debug(TAG, "Stopping work for workspec %s", mWorkSpecId);
                Intent stopWork = CommandHandler.createStopWorkIntent(mContext, mWorkSpecId);
                mDispatcher.postOnMainThread(
                        new SystemAlarmDispatcher.AddRunnable(mDispatcher, stopWork, mStartId));
                mHasPendingStopWorkCommand = true;
            } else {
                Logger.debug(TAG, "Already stopped work for %s", mWorkSpecId);
            }
        }
    }

    private void cleanUp() {
        // cleanUp() may occur from one of 2 threads.
        // * In the call to bgProcessor.startWork() returns false,
        //   it probably means that the worker is already being processed
        //   so we just need to call cleanUp to release wakelocks on the command processor thread.
        // * It could also happen on the onExecutionCompleted() pass of the bgProcessor.
        // To avoid calling mWakeLock.release() twice, we are synchronizing here.
        synchronized (mLock) {
            // stop timers
            mDispatcher.getWorkTimer().stopTimer(mWorkSpecId);

            // reset trackers
            mWorkConstraintsTracker.reset();

            // release wake locks
            if (mWakeLock != null && mWakeLock.isHeld()) {
                Logger.debug(TAG, "Releasing wakelock %s for WorkSpec %s", mWakeLock, mWorkSpecId);
                mWakeLock.release();
            }
        }
    }

    private PowerManager.WakeLock newWakeLock() {
        String tag = String.format("%s (%s)", mWorkSpecId, mStartId);
        return mPowerManager.newWakeLock(PARTIAL_WAKE_LOCK, tag);
    }
}
