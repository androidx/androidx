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

package androidx.work.impl.background.greedy;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Configuration;
import androidx.work.Logger;
import androidx.work.WorkInfo;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.constraints.WorkConstraintsCallback;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.ProcessUtils;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A greedy {@link Scheduler} that schedules unconstrained, non-timed work.  It intentionally does
 * not acquire any WakeLocks, instead trying to brute-force them as time allows before the process
 * gets killed.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GreedyScheduler implements Scheduler, WorkConstraintsCallback, ExecutionListener {

    private static final String TAG = Logger.tagWithPrefix("GreedyScheduler");

    private final Context mContext;
    private final WorkManagerImpl mWorkManagerImpl;
    private final WorkConstraintsTracker mWorkConstraintsTracker;
    private final Set<WorkSpec> mConstrainedWorkSpecs = new HashSet<>();
    private DelayedWorkTracker mDelayedWorkTracker;
    private boolean mRegisteredExecutionListener;
    private final Object mLock;

    // Internal State
    Boolean mInDefaultProcess;

    public GreedyScheduler(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor taskExecutor,
            @NonNull WorkManagerImpl workManagerImpl) {
        mContext = context;
        mWorkManagerImpl = workManagerImpl;
        mWorkConstraintsTracker = new WorkConstraintsTracker(context, taskExecutor, this);
        mDelayedWorkTracker = new DelayedWorkTracker(this, configuration.getRunnableScheduler());
        mLock = new Object();
    }

    @VisibleForTesting
    public GreedyScheduler(
            @NonNull Context context,
            @NonNull WorkManagerImpl workManagerImpl,
            @NonNull WorkConstraintsTracker workConstraintsTracker) {
        mContext = context;
        mWorkManagerImpl = workManagerImpl;
        mWorkConstraintsTracker = workConstraintsTracker;
        mLock = new Object();
    }

    @VisibleForTesting
    public void setDelayedWorkTracker(@NonNull DelayedWorkTracker delayedWorkTracker) {
        mDelayedWorkTracker = delayedWorkTracker;
    }

    @Override
    public boolean hasLimitedSchedulingSlots() {
        return false;
    }

    @Override
    public void schedule(@NonNull WorkSpec... workSpecs) {
        if (mInDefaultProcess == null) {
            checkDefaultProcess();
        }

        if (!mInDefaultProcess) {
            Logger.get().info(TAG, "Ignoring schedule request in a secondary process");
            return;
        }

        registerExecutionListenerIfNeeded();

        // Keep track of the list of new WorkSpecs whose constraints need to be tracked.
        // Add them to the known list of constrained WorkSpecs and call replace() on
        // WorkConstraintsTracker. That way we only need to synchronize on the part where we
        // are updating mConstrainedWorkSpecs.
        Set<WorkSpec> constrainedWorkSpecs = new HashSet<>();
        Set<String> constrainedWorkSpecIds = new HashSet<>();

        for (WorkSpec workSpec : workSpecs) {
            long nextRunTime = workSpec.calculateNextRunTime();
            long now = System.currentTimeMillis();
            if (workSpec.state == WorkInfo.State.ENQUEUED) {
                if (now < nextRunTime) {
                    // Future work
                    if (mDelayedWorkTracker != null) {
                        mDelayedWorkTracker.schedule(workSpec);
                    }
                } else if (workSpec.hasConstraints()) {
                    if (SDK_INT >= 23 && workSpec.constraints.requiresDeviceIdle()) {
                        // Ignore requests that have an idle mode constraint.
                        Logger.get().debug(TAG,
                                "Ignoring " + workSpec + ". Requires device idle.");
                    } else if (SDK_INT >= 24 && workSpec.constraints.hasContentUriTriggers()) {
                        // Ignore requests that have content uri triggers.
                        Logger.get().debug(TAG,
                                "Ignoring " + workSpec + ". Requires ContentUri triggers.");
                    } else {
                        constrainedWorkSpecs.add(workSpec);
                        constrainedWorkSpecIds.add(workSpec.id);
                    }
                } else {
                    Logger.get().debug(TAG, "Starting work for " + workSpec.id);
                    mWorkManagerImpl.startWork(workSpec.id);
                }
            }
        }

        // onExecuted() which is called on the main thread also modifies the list of mConstrained
        // WorkSpecs. Therefore we need to lock here.
        synchronized (mLock) {
            if (!constrainedWorkSpecs.isEmpty()) {
                String formattedIds = TextUtils.join(",", constrainedWorkSpecIds);
                Logger.get().debug(TAG, "Starting tracking for " + formattedIds);
                mConstrainedWorkSpecs.addAll(constrainedWorkSpecs);
                mWorkConstraintsTracker.replace(mConstrainedWorkSpecs);
            }
        }
    }

    private void checkDefaultProcess() {
        Configuration configuration = mWorkManagerImpl.getConfiguration();
        mInDefaultProcess = ProcessUtils.isDefaultProcess(mContext, configuration);
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        if (mInDefaultProcess == null) {
            checkDefaultProcess();
        }

        if (!mInDefaultProcess) {
            Logger.get().info(TAG, "Ignoring schedule request in non-main process");
            return;
        }

        registerExecutionListenerIfNeeded();
        Logger.get().debug(TAG, "Cancelling work ID " + workSpecId);
        if (mDelayedWorkTracker != null) {
            mDelayedWorkTracker.unschedule(workSpecId);
        }
        // onExecutionCompleted does the cleanup.
        mWorkManagerImpl.stopWork(workSpecId);
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            Logger.get().debug(TAG, "Constraints met: Scheduling work ID " + workSpecId);
            mWorkManagerImpl.startWork(workSpecId);
        }
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            Logger.get().debug(TAG, "Constraints not met: Cancelling work ID " + workSpecId);
            mWorkManagerImpl.stopWork(workSpecId);
        }
    }

    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
        removeConstraintTrackingFor(workSpecId);
        // onExecuted does not need to worry about unscheduling WorkSpecs with the mDelayedTracker.
        // This is because, after onExecuted(), all schedulers are asked to cancel.
    }

    private void removeConstraintTrackingFor(@NonNull String workSpecId) {
        synchronized (mLock) {
            // This is synchronized because onExecuted is on the main thread but
            // Schedulers#schedule() can modify the list of mConstrainedWorkSpecs on the task
            // executor thread.
            for (WorkSpec constrainedWorkSpec : mConstrainedWorkSpecs) {
                if (constrainedWorkSpec.id.equals(workSpecId)) {
                    Logger.get().debug(TAG, "Stopping tracking for " + workSpecId);
                    mConstrainedWorkSpecs.remove(constrainedWorkSpec);
                    mWorkConstraintsTracker.replace(mConstrainedWorkSpecs);
                    break;
                }
            }
        }
    }

    private void registerExecutionListenerIfNeeded() {
        // This method needs to be called *after* Processor is created, since Processor needs
        // Schedulers and is created after this class.
        if (!mRegisteredExecutionListener) {
            mWorkManagerImpl.getProcessor().addExecutionListener(this);
            mRegisteredExecutionListener = true;
        }
    }
}
