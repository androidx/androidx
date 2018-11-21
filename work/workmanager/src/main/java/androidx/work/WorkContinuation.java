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

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;

/**
 * A class that allows you to chain together {@link OneTimeWorkRequest}s.  WorkContinuations allow
 * the user to create arbitrary acyclic graphs of work dependencies.  You can add dependent work to
 * a WorkContinuation by invoking {@link #then(OneTimeWorkRequest)} or its variants.  This returns a
 * new WorkContinuation.
 * <p>
 * To construct more complex graphs, {@link WorkContinuation#combine(List)} or its
 * variants can be used to return a WorkContinuation with the input WorkContinuations as
 * prerequisites.  To create a graph like this:
 *
 * <pre>
 *     A       C
 *     |       |
 *     B       D
 *     |       |
 *     +-------+
 *         |
 *         E    </pre>
 *
 * you would write the following:
 *
 * <pre>
 * {@code
 *  WorkContinuation left = workManager.beginWith(A).then(B);
 *  WorkContinuation right = workManager.beginWith(C).then(D);
 *  WorkContinuation final = WorkContinuation.combine(Arrays.asList(left, right));
 *  final.enqueue();}</pre>
 *
 * Not that enqueuing a WorkContinuation enqueues all previously-unenqueued prerequisites.  You must
 * call {@link #enqueue()} to inform WorkManager to actually enqueue the work graph.  As usual,
 * enqueues are asynchronous - you can observe or block on the returned {@link Operation} if you
 * need to be informed about its completion.
 * <p>
 * Because of the fluent nature of this class, its existence should be invisible in most cases.
 */
public abstract class WorkContinuation {

    /**
     * Adds new {@link OneTimeWorkRequest} items that depend on the successful completion of
     * all previously added {@link OneTimeWorkRequest}s.
     *
     * @param work One or more {@link OneTimeWorkRequest}s to add as dependents
     * @return A {@link WorkContinuation} that allows for further chaining of dependent
     *         {@link OneTimeWorkRequest}s
     */
    public final @NonNull WorkContinuation then(@NonNull OneTimeWorkRequest work) {
        return then(Collections.singletonList(work));
    }

    /**
     * Adds new {@link OneTimeWorkRequest} items that depend on the successful completion
     * of all previously added {@link OneTimeWorkRequest}s.
     *
     * @param work One or more {@link OneTimeWorkRequest} to add as dependents
     * @return A {@link WorkContinuation} that allows for further chaining of dependent
     *         {@link OneTimeWorkRequest}s
     */
    public abstract @NonNull WorkContinuation then(@NonNull List<OneTimeWorkRequest> work);

    /**
     * Returns a {@link LiveData} list of {@link WorkInfo}s that provide information about the
     * status of each {@link OneTimeWorkRequest} in this {@link WorkContinuation}, as well as their
     * prerequisites.  If the state or outputs of any of the work changes, any attached
     * {@link android.arch.lifecycle.Observer}s will trigger.
     *
     * @return A {@link LiveData} containing a list of {@link WorkInfo}s; you must use
     *         {@link LiveData#observe(LifecycleOwner, Observer)} to receive updates
     */
    public abstract @NonNull LiveData<List<WorkInfo>> getWorkInfosLiveData();

    /**
     * Returns a {@link ListenableFuture} of a {@link List} of {@link WorkInfo}s that provides
     * information about the status of each {@link OneTimeWorkRequest} in this
     * {@link WorkContinuation}, as well as their prerequisites.
     *
     * @return A {@link  ListenableFuture} of a {@link List} of {@link WorkInfo}s
     */
    public abstract @NonNull ListenableFuture<List<WorkInfo>> getWorkInfos();

    /**
     * Enqueues the instance of {@link WorkContinuation} on the background thread.
     *
     * @return An {@link Operation} that can be used to determine when the enqueue has completed
     */
    public abstract @NonNull Operation enqueue();

    /**
     * Combines multiple {@link WorkContinuation}s as prerequisites for a new WorkContinuation to
     * allow for complex chaining.  For example, to create a graph like this:
     *
     * <pre>
     *     A       C
     *     |       |
     *     B       D
     *     |       |
     *     +-------+
     *         |
     *         E    </pre>
     *
     * you would write the following:
     *
     * <pre>
     * {@code
     *  WorkContinuation left = workManager.beginWith(A).then(B);
     *  WorkContinuation right = workManager.beginWith(C).then(D);
     *  WorkContinuation final = WorkContinuation.combine(Arrays.asList(left, right));
     *  final.enqueue();}</pre>
     *
     * @param continuations One or more {@link WorkContinuation}s that are prerequisites for the
     *                      return value
     * @return A {@link WorkContinuation} that allows further chaining
     */
    public static @NonNull WorkContinuation combine(@NonNull List<WorkContinuation> continuations) {
        return continuations.get(0).combineInternal(continuations);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected abstract @NonNull WorkContinuation combineInternal(
            @NonNull List<WorkContinuation> continuations);
}
