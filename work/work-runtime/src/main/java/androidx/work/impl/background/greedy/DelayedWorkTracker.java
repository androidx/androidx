/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Clock;
import androidx.work.Logger;
import androidx.work.RunnableScheduler;
import androidx.work.impl.model.WorkSpec;

import java.util.HashMap;
import java.util.Map;


/**
 * Keeps track of {@link androidx.work.WorkRequest}s that have a timing component in a
 * {@link GreedyScheduler}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DelayedWorkTracker {

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    static final String TAG = Logger.tagWithPrefix("DelayedWorkTracker");

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final GreedyScheduler mGreedyScheduler;

    private final RunnableScheduler mRunnableScheduler;
    private final Clock mClock;
    private final Map<String, Runnable> mRunnables;

    public DelayedWorkTracker(
            @NonNull GreedyScheduler scheduler,
            @NonNull RunnableScheduler runnableScheduler,
            @NonNull Clock clock) {

        mGreedyScheduler = scheduler;
        mRunnableScheduler = runnableScheduler;
        mClock = clock;
        mRunnables = new HashMap<>();
    }

    /**
     * Cancels the existing instance of a {@link Runnable} if any, and schedules a new
     * {@link Runnable}; which eventually calls {@link GreedyScheduler#schedule(WorkSpec...)} at
     * the {@link WorkSpec}'s scheduled run time.
     *
     * @param workSpec The {@link WorkSpec} corresponding to the {@link androidx.work.WorkRequest}
     * @param nextRunTime time when work should be executed
     */
    public void schedule(@NonNull final WorkSpec workSpec, long nextRunTime) {
        Runnable existing = mRunnables.remove(workSpec.id);
        if (existing != null) {
            mRunnableScheduler.cancel(existing);
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Logger.get().debug(TAG, "Scheduling work " + workSpec.id);
                mGreedyScheduler.schedule(workSpec);
            }
        };

        mRunnables.put(workSpec.id, runnable);
        long now = mClock.currentTimeMillis();
        long delay = nextRunTime - now;
        mRunnableScheduler.scheduleWithDelay(delay, runnable);
    }

    /**
     * Cancels the existing instance of a {@link Runnable} if any.
     *
     * @param workSpecId The {@link androidx.work.WorkRequest} id
     */
    public void unschedule(@NonNull String workSpecId) {
        Runnable runnable = mRunnables.remove(workSpecId);
        if (runnable != null) {
            mRunnableScheduler.cancel(runnable);
        }
    }
}
