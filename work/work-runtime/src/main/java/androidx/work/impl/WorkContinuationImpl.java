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

package androidx.work.impl;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LiveData;
import androidx.work.ArrayCreatingInputMerger;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Logger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkRequest;
import androidx.work.impl.utils.EnqueueRunnable;
import androidx.work.impl.utils.StatusRunnable;
import androidx.work.impl.workers.CombineContinuationsWorker;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A concrete implementation of {@link WorkContinuation}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkContinuationImpl extends WorkContinuation {

    private static final String TAG = Logger.tagWithPrefix("WorkContinuationImpl");

    private final WorkManagerImpl mWorkManagerImpl;
    private final String mName;
    private final ExistingWorkPolicy mExistingWorkPolicy;
    private final List<? extends WorkRequest> mWork;
    private final List<String> mIds;
    private final List<String> mAllIds;
    private final List<WorkContinuationImpl> mParents;

    private boolean mEnqueued;
    private Operation mOperation;

    @NonNull
    public WorkManagerImpl getWorkManagerImpl() {
        return mWorkManagerImpl;
    }

    @Nullable
    public String getName() {
        return mName;
    }

    public ExistingWorkPolicy getExistingWorkPolicy() {
        return mExistingWorkPolicy;
    }

    @NonNull
    public List<? extends WorkRequest> getWork() {
        return mWork;
    }

    @NonNull
    public List<String> getIds() {
        return mIds;
    }

    public List<String> getAllIds() {
        return mAllIds;
    }

    public boolean isEnqueued() {
        return mEnqueued;
    }

    /**
     * Marks the {@link WorkContinuationImpl} as enqueued.
     */
    public void markEnqueued() {
        mEnqueued = true;
    }

    public List<WorkContinuationImpl> getParents() {
        return mParents;
    }

    public WorkContinuationImpl(
            @NonNull WorkManagerImpl workManagerImpl,
            @NonNull List<? extends WorkRequest> work) {
        this(
                workManagerImpl,
                null,
                ExistingWorkPolicy.KEEP,
                work,
                null);
    }

    public WorkContinuationImpl(
            @NonNull WorkManagerImpl workManagerImpl,
            @Nullable String name,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<? extends WorkRequest> work) {
        this(workManagerImpl, name, existingWorkPolicy, work, null);
    }

    public WorkContinuationImpl(@NonNull WorkManagerImpl workManagerImpl,
            @Nullable String name,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<? extends WorkRequest> work,
            @Nullable List<WorkContinuationImpl> parents) {
        mWorkManagerImpl = workManagerImpl;
        mName = name;
        mExistingWorkPolicy = existingWorkPolicy;
        mWork = work;
        mParents = parents;
        mIds = new ArrayList<>(mWork.size());
        mAllIds = new ArrayList<>();
        if (parents != null) {
            for (WorkContinuationImpl parent : parents) {
                mAllIds.addAll(parent.mAllIds);
            }
        }
        for (int i = 0; i < work.size(); i++) {
            String id = work.get(i).getStringId();
            mIds.add(id);
            mAllIds.add(id);
        }
    }

    @Override
    public @NonNull WorkContinuation then(@NonNull List<OneTimeWorkRequest> work) {
        if (work.isEmpty()) {
            return this;
        } else {
            return new WorkContinuationImpl(mWorkManagerImpl,
                    mName,
                    ExistingWorkPolicy.KEEP,
                    work,
                    Collections.singletonList(this));
        }
    }

    @Override
    public @NonNull LiveData<List<WorkInfo>> getWorkInfosLiveData() {
        return mWorkManagerImpl.getWorkInfosById(mAllIds);
    }

    @NonNull
    @Override
    public ListenableFuture<List<WorkInfo>> getWorkInfos() {
        StatusRunnable<List<WorkInfo>> runnable =
                StatusRunnable.forStringIds(mWorkManagerImpl, mAllIds);

        mWorkManagerImpl.getWorkTaskExecutor().executeOnBackgroundThread(runnable);
        return runnable.getFuture();
    }

    @Override
    public @NonNull Operation enqueue() {
        // Only enqueue if not already enqueued.
        if (!mEnqueued) {
            // The runnable walks the hierarchy of the continuations
            // and marks them enqueued using the markEnqueued() method, parent first.
            EnqueueRunnable runnable = new EnqueueRunnable(this);
            mWorkManagerImpl.getWorkTaskExecutor().executeOnBackgroundThread(runnable);
            mOperation = runnable.getOperation();
        } else {
            Logger.get().warning(TAG,
                    "Already enqueued work ids (" + TextUtils.join(", ", mIds) + ")");
        }
        return mOperation;
    }

    @Override
    protected @NonNull WorkContinuation combineInternal(
            @NonNull List<WorkContinuation> continuations) {
        OneTimeWorkRequest combinedWork =
                new OneTimeWorkRequest.Builder(CombineContinuationsWorker.class)
                        .setInputMerger(ArrayCreatingInputMerger.class)
                        .build();

        List<WorkContinuationImpl> parents = new ArrayList<>(continuations.size());
        for (WorkContinuation continuation : continuations) {
            parents.add((WorkContinuationImpl) continuation);
        }

        return new WorkContinuationImpl(mWorkManagerImpl,
                null,
                ExistingWorkPolicy.KEEP,
                Collections.singletonList(combinedWork),
                parents);
    }

    /**
     * @return {@code true} If there are cycles in the {@link WorkContinuationImpl}.

     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean hasCycles() {
        return hasCycles(this, new HashSet<String>());
    }

    /**
     * @param continuation The {@link WorkContinuationImpl} instance.
     * @param visited      The {@link Set} of {@link androidx.work.impl.model.WorkSpec} ids
     *                     marked as visited.
     * @return {@code true} if the {@link WorkContinuationImpl} has a cycle.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    private static boolean hasCycles(
            @NonNull WorkContinuationImpl continuation,
            @NonNull Set<String> visited) {

        // mark the ids of this workContinuation as visited
        // before we check if the parents have cycles.
        visited.addAll(continuation.getIds());

        Set<String> prerequisiteIds = prerequisitesFor(continuation);
        for (String id : visited) {
            if (prerequisiteIds.contains(id)) {
                // This prerequisite has already been visited before.
                // There is a cycle.
                return true;
            }
        }

        List<WorkContinuationImpl> parents = continuation.getParents();
        if (parents != null && !parents.isEmpty()) {
            for (WorkContinuationImpl parent : parents) {
                // if any of the parent has a cycle, then bail out
                if (hasCycles(parent, visited)) {
                    return true;
                }
            }
        }

        // Un-mark the ids of the workContinuation as visited for the next parent.
        // This is because we don't want to change the state of visited ids for subsequent parents
        // This is being done to avoid allocations. Ideally we would check for a
        // hasCycles(parent, new HashSet<>(visited)) instead.
        visited.removeAll(continuation.getIds());
        return false;
    }

    /**
     * @return the {@link Set} of pre-requisites for a given {@link WorkContinuationImpl}.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Set<String> prerequisitesFor(WorkContinuationImpl continuation) {
        Set<String> preRequisites = new HashSet<>();
        List<WorkContinuationImpl> parents = continuation.getParents();
        if (parents != null && !parents.isEmpty()) {
            for (WorkContinuationImpl parent : parents) {
                preRequisites.addAll(parent.getIds());
            }
        }
        return preRequisites;
    }
}
