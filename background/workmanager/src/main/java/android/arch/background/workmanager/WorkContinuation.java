/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.arch.background.workmanager;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An opaque class that allows you to chain together {@link Work}.
 */

public abstract class WorkContinuation {

    /**
     * Adds new {@link Work} items that depend on the successful completion of all previously added
     * {@link Work}.
     *
     * @param work One or more {@link Work} to add to the {@link WorkContinuation}
     * @return A {@link WorkContinuation} that allows for further chaining of dependent {@link Work}
     */
    public abstract WorkContinuation then(Work... work);

    /**
     * Adds new {@link Work} items, created with the {@link Worker} classes, that depend on the
     * successful completion of all previously added {@link Work}.
     *
     * Each {@link Work} is created with no {@link Arguments} or {@link Constraints}.
     *
     * @param workerClasses One or more {@link Worker}s to add to the {@link WorkContinuation}
     * @return A {@link WorkContinuation} that allows for further chaining of dependent {@link Work}
     */
    @SafeVarargs
    public final WorkContinuation then(Class<? extends Worker>... workerClasses) {
        return then(Arrays.asList(workerClasses));
    }

    /**
     * Returns a {@link LiveData} mapping of work identifiers to their statuses for all work in this
     * chain.  Whenever the status of one of the work enqueued in this chain changes, any attached
     * {@link android.arch.lifecycle.Observer}s will trigger.
     *
     * @return A {@link LiveData} containing a map of work identifiers to their corresponding
     * {@link BaseWork.WorkStatus}
     */
    public abstract LiveData<Map<String, Integer>> getStatuses();

    /**
     * Enqueues the instance of {@link WorkContinuation} on the background thread.
     */
    public abstract void enqueue();

    /**
     * Joins multiple {@link WorkContinuation}s to allow for complex chaining.
     *
     * @param continuations Two or more {@link WorkContinuation}s that are prerequisites for the
     *                      return value
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public static WorkContinuation join(@NonNull WorkContinuation... continuations) {
        if (continuations.length < 2) {
            throw new IllegalArgumentException(
                    "WorkContinuation.join() needs at least 2 continuations.");
        }

        return continuations[0].joinInternal(null, continuations);
    }

    /**
     * Joins multiple {@link WorkContinuation}s to allow for complex chaining using the
     * {@link Work} provided.
     *
     * @param work The {@link Work} which depends on the successful completion of the
     *             provided {@link WorkContinuation}s
     * @param continuations Two or more {@link WorkContinuation}s that are prerequisites for the
     *                      {@link Work} provided.
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public static WorkContinuation join(
            @NonNull Work work,
            @NonNull WorkContinuation... continuations) {

        if (continuations.length < 2) {
            throw new IllegalArgumentException(
                    "WorkContinuation.join() needs at least 2 continuations.");
        }

        return continuations[0].joinInternal(work, continuations);
    }

    protected abstract WorkContinuation then(List<Class<? extends Worker>> workerClasses);

    protected abstract WorkContinuation joinInternal(
            @Nullable Work work,
            @NonNull WorkContinuation... continuations);
}
