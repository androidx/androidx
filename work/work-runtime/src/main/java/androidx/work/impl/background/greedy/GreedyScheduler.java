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

import static androidx.work.WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS;
import static androidx.work.impl.constraints.WorkConstraintsTrackerKt.listen;
import static androidx.work.impl.model.WorkSpecKt.generationalId;

import static java.lang.Math.max;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.Logger;
import androidx.work.RunnableScheduler;
import androidx.work.WorkInfo;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Processor;
import androidx.work.impl.Scheduler;
import androidx.work.impl.StartStopToken;
import androidx.work.impl.StartStopTokens;
import androidx.work.impl.WorkLauncher;
import androidx.work.impl.constraints.ConstraintsState;
import androidx.work.impl.constraints.OnConstraintsStateChangedListener;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.WorkGenerationalId;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.ProcessUtils;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import kotlinx.coroutines.Job;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A greedy {@link Scheduler} that schedules unconstrained, non-timed work.  It intentionally does
 * not acquire any WakeLocks, instead trying to brute-force them as time allows before the process
 * gets killed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GreedyScheduler implements Scheduler, OnConstraintsStateChangedListener,
        ExecutionListener {

    private static final String TAG = Logger.tagWithPrefix("GreedyScheduler");

    /**
     * GreedyScheduler will start throttle workspec if it sees the same work being retried
     * within process's lifetime.
     */
    private static final int NON_THROTTLE_RUN_ATTEMPT_COUNT = 5;

    private final Context mContext;
    private final Map<WorkGenerationalId, Job> mConstrainedWorkSpecs = new HashMap<>();
    private DelayedWorkTracker mDelayedWorkTracker;
    private boolean mRegisteredExecutionListener;
    private final Object mLock = new Object();
    private final StartStopTokens mStartStopTokens = StartStopTokens.create();
    private final Processor mProcessor;
    private final WorkLauncher mWorkLauncher;

    private final Configuration mConfiguration;

    private final Map<WorkGenerationalId, AttemptData> mFirstRunAttempts = new HashMap<>();
    // Internal State
    Boolean mInDefaultProcess;

    private final WorkConstraintsTracker mConstraintsTracker;
    private final TaskExecutor mTaskExecutor;
    private final TimeLimiter mTimeLimiter;

    public GreedyScheduler(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull Trackers trackers,
            @NonNull Processor processor,
            @NonNull WorkLauncher workLauncher,
            @NonNull TaskExecutor taskExecutor
    ) {
        mContext = context;
        RunnableScheduler runnableScheduler = configuration.getRunnableScheduler();
        mDelayedWorkTracker = new DelayedWorkTracker(this, runnableScheduler,
                configuration.getClock());
        mTimeLimiter = new TimeLimiter(runnableScheduler, workLauncher);
        mTaskExecutor = taskExecutor;
        mConstraintsTracker = new WorkConstraintsTracker(trackers);
        mConfiguration = configuration;
        mProcessor = processor;
        mWorkLauncher = workLauncher;
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
    public void schedule(WorkSpec @NonNull ... workSpecs) {
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
            // it doesn't help against races, but reduces useless load in the system
            WorkGenerationalId id = generationalId(workSpec);
            if (mStartStopTokens.contains(id)) {
                continue;
            }
            long throttled = throttleIfNeeded(workSpec);
            long nextRunTime = max(workSpec.calculateNextRunTime(), throttled);
            long now = mConfiguration.getClock().currentTimeMillis();
            if (workSpec.state == WorkInfo.State.ENQUEUED) {
                if (now < nextRunTime) {
                    // Future work
                    if (mDelayedWorkTracker != null) {
                        mDelayedWorkTracker.schedule(workSpec, nextRunTime);
                    }
                } else if (workSpec.hasConstraints()) {
                    Constraints constraints = workSpec.constraints;
                    if (constraints.requiresDeviceIdle()) {
                        // Ignore requests that have an idle mode constraint.
                        Logger.get().debug(TAG,
                                "Ignoring " + workSpec + ". Requires device idle.");
                    } else if (SDK_INT >= 24 && constraints.hasContentUriTriggers()) {
                        // Ignore requests that have content uri triggers.
                        Logger.get().debug(TAG,
                                "Ignoring " + workSpec + ". Requires ContentUri triggers.");
                    } else {
                        constrainedWorkSpecs.add(workSpec);
                        constrainedWorkSpecIds.add(workSpec.id);
                    }
                } else {
                    // it doesn't help against races, but reduces useless load in the system
                    if (!mStartStopTokens.contains(generationalId(workSpec))) {
                        Logger.get().debug(TAG, "Starting work for " + workSpec.id);
                        StartStopToken token = mStartStopTokens.tokenFor(workSpec);
                        mTimeLimiter.track(token);
                        mWorkLauncher.startWork(token);
                    }
                }
            }
        }

        // onExecuted() which is called on the main thread also modifies the list of mConstrained
        // WorkSpecs. Therefore we need to lock here.
        synchronized (mLock) {
            if (!constrainedWorkSpecs.isEmpty()) {
                String formattedIds = TextUtils.join(",", constrainedWorkSpecIds);
                Logger.get().debug(TAG, "Starting tracking for " + formattedIds);
                for (WorkSpec spec: constrainedWorkSpecs) {
                    WorkGenerationalId id = generationalId(spec);
                    if (!mConstrainedWorkSpecs.containsKey(id)) {
                        Job job = listen(mConstraintsTracker, spec,
                                mTaskExecutor.getTaskCoroutineDispatcher(), this);
                        mConstrainedWorkSpecs.put(id, job);
                    }
                }
            }
        }
    }

    private void checkDefaultProcess() {
        mInDefaultProcess = ProcessUtils.isDefaultProcess(mContext, mConfiguration);
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
        for (StartStopToken id : mStartStopTokens.remove(workSpecId)) {
            mTimeLimiter.cancel(id);
            mWorkLauncher.stopWork(id);
        }
    }

    @Override
    public void onConstraintsStateChanged(@NonNull WorkSpec workSpec,
            @NonNull ConstraintsState state) {
        WorkGenerationalId id = generationalId(workSpec);
        if (state instanceof ConstraintsState.ConstraintsMet) {
            // it doesn't help against races, but reduces useless load in the system
            if (!mStartStopTokens.contains(id)) {
                Logger.get().debug(TAG, "Constraints met: Scheduling work ID " + id);
                StartStopToken token = mStartStopTokens.tokenFor(id);
                mTimeLimiter.track(token);
                mWorkLauncher.startWork(token);
            }
        } else {
            Logger.get().debug(TAG, "Constraints not met: Cancelling work ID " + id);
            StartStopToken runId = mStartStopTokens.remove(id);
            if (runId != null) {
                mTimeLimiter.cancel(runId);
                int reason = ((ConstraintsState.ConstraintsNotMet) state).getReason();
                mWorkLauncher.stopWorkWithReason(runId, reason);
            }
        }
    }

    @Override
    public void onExecuted(@NonNull WorkGenerationalId id, boolean needsReschedule) {
        StartStopToken token = mStartStopTokens.remove(id);
        if (token != null) {
            mTimeLimiter.cancel(token);
        }
        removeConstraintTrackingFor(id);

        if (!needsReschedule) {
            // finished execution rather than being interrupted
            synchronized (mLock) {
                mFirstRunAttempts.remove(id);
            }
        }
        // onExecuted does not need to worry about unscheduling WorkSpecs with the mDelayedTracker.
        // This is because, after onExecuted(), all schedulers are asked to cancel.
    }

    private void removeConstraintTrackingFor(@NonNull WorkGenerationalId id) {
        Job job;
        synchronized (mLock) {
            // This is synchronized because onExecuted is on the main thread but
            // Schedulers#schedule() can modify the list of mConstrainedWorkSpecs on the task
            // executor thread.
            job = mConstrainedWorkSpecs.remove(id);
        }
        if (job != null) {
            Logger.get().debug(TAG, "Stopping tracking for " + id);
            job.cancel(null);
        }
    }

    private void registerExecutionListenerIfNeeded() {
        // This method needs to be called *after* Processor is created, since Processor needs
        // Schedulers and is created after this class.
        if (!mRegisteredExecutionListener) {
            mProcessor.addExecutionListener(this);
            mRegisteredExecutionListener = true;
        }
    }

    private long throttleIfNeeded(WorkSpec workSpec) {
        synchronized (mLock) {
            WorkGenerationalId id = generationalId(workSpec);
            AttemptData firstRunAttempt = mFirstRunAttempts.get(id);
            if (firstRunAttempt == null) {
                firstRunAttempt = new AttemptData(workSpec.runAttemptCount,
                        mConfiguration.getClock().currentTimeMillis());
                mFirstRunAttempts.put(id, firstRunAttempt);
            }
            return firstRunAttempt.mTimeStamp
                    + max(workSpec.runAttemptCount - firstRunAttempt.mRunAttemptCount
                    - NON_THROTTLE_RUN_ATTEMPT_COUNT, 0) * DEFAULT_BACKOFF_DELAY_MILLIS;
        }
    }

    private static class AttemptData {
        final int mRunAttemptCount;
        final long mTimeStamp;

        private AttemptData(int runAttemptCount, long timeStamp) {
            this.mRunAttemptCount = runAttemptCount;
            this.mTimeStamp = timeStamp;
        }
    }
}
