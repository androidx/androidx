/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.multiprocess;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.OneTimeWorkRequest;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;

/**
 * Provides a subset of {@link androidx.work.WorkContinuation} APIs that are available for apps
 * that use multiple processes.
 */
public abstract class RemoteWorkContinuation {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected RemoteWorkContinuation() {
        // Does nothing
    }

    /**
     * Adds new {@link OneTimeWorkRequest} items that depend on the successful completion of
     * all previously added {@link OneTimeWorkRequest}s.
     *
     * @param work One or more {@link OneTimeWorkRequest}s to add as dependents
     * @return A {@link RemoteWorkContinuation} that allows for further chaining of dependent
     *         {@link OneTimeWorkRequest}s
     */
    public final @NonNull RemoteWorkContinuation then(@NonNull OneTimeWorkRequest work) {
        return then(Collections.singletonList(work));
    }

    /**
     * Adds new {@link OneTimeWorkRequest} items that depend on the successful completion
     * of all previously added {@link OneTimeWorkRequest}s.
     *
     * @param work One or more {@link OneTimeWorkRequest} to add as dependents
     * @return A {@link RemoteWorkContinuation} that allows for further chaining of dependent
     *         {@link OneTimeWorkRequest}s
     */
    @NonNull
    public abstract RemoteWorkContinuation then(@NonNull List<OneTimeWorkRequest> work);

    /**
     * Enqueues the instance of {@link RemoteWorkContinuation} on the background thread.
     *
     * @return An {@link ListenableFuture} that can be used to determine when the enqueue
     * has completed
     */
    @NonNull
    public abstract ListenableFuture<Void> enqueue();

    /**
     * Combines multiple {@link RemoteWorkContinuation}s as prerequisites for a new
     * RemoteWorkContinuation to allow for complex chaining.
     *
     * @param continuations One or more {@link RemoteWorkContinuation}s that are
     *                      prerequisites for the return value
     * @return A {@link RemoteWorkContinuation} that allows further chaining
     */
    @NonNull
    public static RemoteWorkContinuation combine(
            @NonNull List<RemoteWorkContinuation> continuations) {

        return continuations.get(0).combineInternal(continuations);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    protected abstract RemoteWorkContinuation combineInternal(
            @NonNull List<RemoteWorkContinuation> continuations);
}
