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

package androidx.work;

import android.support.annotation.WorkerThread;

import java.util.concurrent.TimeUnit;

/**
 * Blocking methods for {@link WorkManager} operations.  These methods are expected to be called
 * from a background thread.
 */
public interface SynchronousWorkManager {

    /**
     * Prunes all eligible finished work from the internal database in a synchronous fashion.
     * Eligible work must be finished ({@link State#SUCCEEDED}, {@link State#FAILED}, or
     * {@link State#CANCELLED}), with zero unfinished dependents.
     * <p>
     * <b>Use this method with caution</b>; by invoking it, you (and any modules and libraries in
     * your codebase) will no longer be able to observe the {@link WorkStatus} of the pruned work.
     * You do not normally need to call this method - WorkManager takes care to auto-prune its work
     * after a sane period of time.  This method also ignores the
     * {@link OneTimeWorkRequest.Builder#keepResultsForAtLeast(long, TimeUnit)} policy.
     */
    @WorkerThread
    void pruneWorkSync();
}
