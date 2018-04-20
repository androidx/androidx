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
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;
import android.util.Log;

import androidx.work.impl.ExecutionListener;
import androidx.work.impl.constraints.WorkConstraintsCallback;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.WakeLocks;

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
        mWorkConstraintsTracker = new WorkConstraintsTracker(mContext, this);
        mHasConstraints = false;
        mHasPendingStopWorkCommand = false;
        mLock = new Object();
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> ignored) {
        Log.d(TAG, String.format("onAllConstraintsMet for %s", mWorkSpecId));
        // Constraints met, schedule execution

        // Not using WorkManagerImpl#startWork() here because we need to know if the processor
        // actually enqueued the work here.
        // TODO(rahulrav@) Once WorkManagerImpl provides a callback for acknowledging if
        // work was enqueued, call WorkManagerImpl#startWork().
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

        Log.d(TAG, String.format(
                "onExecuted %s, %s, %s", workSpecId, isSuccessful, needsReschedule));

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
        Log.d(TAG, String.format("Exceeded time limits on execution for %s", workSpecId));
        stopWork();
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> ignored) {
        stopWork();
    }

    @WorkerThread
    void handleProcessWork() {
        mWakeLock = WakeLocks.newWakeLock(
                mContext,
                String.format("%s (%s)", mWorkSpecId, mStartId));
        Log.d(TAG, String.format("Acquiring wakelock %s for WorkSpec %s", mWakeLock, mWorkSpecId));
        mWakeLock.acquire();

        WorkSpec workSpec = mDispatcher.getWorkManager()
                .getWorkDatabase()
                .workSpecDao()
                .getWorkSpec(mWorkSpecId);

        // Keep track of whether the WorkSpec had constraints. This is useful for updating the
        // state of constraint proxies when onExecuted().
        mHasConstraints = workSpec.hasConstraints();

        if (!mHasConstraints) {
            Log.d(TAG, String.format("No constraints for %s", mWorkSpecId));
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
                Log.d(TAG, String.format("Stopping work for workspec %s", mWorkSpecId));
                Intent stopWork = CommandHandler.createStopWorkIntent(mContext, mWorkSpecId);
                mDispatcher.postOnMainThread(
                        new SystemAlarmDispatcher.AddRunnable(mDispatcher, stopWork, mStartId));
                // There are cases where the work may not have been enqueued at all, and therefore
                // the processor is completely unaware of such a workSpecId in which case a
                // reschedule should not happen. For e.g. DELAY_MET when constraints are not met,
                // should not result in a reschedule.
                if (mDispatcher.getProcessor().isEnqueued(mWorkSpecId)) {
                    Log.d(TAG, String.format("WorkSpec %s needs to be rescheduled", mWorkSpecId));
                    Intent reschedule = CommandHandler.createScheduleWorkIntent(mContext,
                            mWorkSpecId);
                    mDispatcher.postOnMainThread(
                            new SystemAlarmDispatcher.AddRunnable(mDispatcher, reschedule,
                                    mStartId));
                } else {
                    Log.d(TAG, String.format(
                            "Processor does not have WorkSpec %s. No need to reschedule ",
                            mWorkSpecId));
                }
                mHasPendingStopWorkCommand = true;
            } else {
                Log.d(TAG, String.format("Already stopped work for %s", mWorkSpecId));
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

            // release wake locks
            if (mWakeLock != null && mWakeLock.isHeld()) {
                Log.d(TAG, String.format(
                        "Releasing wakelock %s for WorkSpec %s", mWakeLock, mWorkSpecId));
                mWakeLock.release();
            }
        }
    }
}
