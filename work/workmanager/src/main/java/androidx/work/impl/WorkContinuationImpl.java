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

import android.arch.lifecycle.LiveData;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.work.BaseWork;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Work;
import androidx.work.WorkContinuation;
import androidx.work.WorkStatus;
import androidx.work.impl.logger.Logger;
import androidx.work.impl.utils.EnqueueRunnable;
import androidx.work.impl.workers.JoinWorker;

/**
 * A concrete implementation of {@link WorkContinuation}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkContinuationImpl extends WorkContinuation {

    private static final String TAG = "WorkContinuationImpl";

    private final WorkManagerImpl mWorkManagerImpl;
    private final String mUniqueTag;
    private final ExistingWorkPolicy mExistingWorkPolicy;
    private final List<? extends BaseWork> mWork;
    private final List<String> mIds;
    private final List<String> mAllIds;
    private final List<WorkContinuationImpl> mParents;
    private boolean mEnqueued;

    @NonNull
    public WorkManagerImpl getWorkManagerImpl() {
        return mWorkManagerImpl;
    }

    @Nullable
    public String getUniqueTag() {
        return mUniqueTag;
    }

    public ExistingWorkPolicy getExistingWorkPolicy() {
        return mExistingWorkPolicy;
    }

    @NonNull
    public List<? extends BaseWork> getWork() {
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

    WorkContinuationImpl(
            @NonNull WorkManagerImpl workManagerImpl,
            @NonNull List<? extends BaseWork> work) {
        this(
                workManagerImpl,
                null,
                ExistingWorkPolicy.KEEP,
                work,
                null);
    }

    WorkContinuationImpl(@NonNull WorkManagerImpl workManagerImpl,
            String uniqueTag,
            ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<? extends BaseWork> work) {
        this(workManagerImpl, uniqueTag, existingWorkPolicy, work, null);
    }

    WorkContinuationImpl(@NonNull WorkManagerImpl workManagerImpl,
            String uniqueTag,
            ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<? extends BaseWork> work,
            @Nullable List<WorkContinuationImpl> parents) {
        mWorkManagerImpl = workManagerImpl;
        mUniqueTag = uniqueTag;
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
            String id = work.get(i).getId();
            mIds.add(id);
            mAllIds.add(id);
        }
    }

    @Override
    public WorkContinuation then(List<Work> work) {
        // TODO (rahulrav@) We need to decide if we want to allow chaining of continuations after
        // an initial call to enqueue()
        return new WorkContinuationImpl(mWorkManagerImpl,
                mUniqueTag,
                ExistingWorkPolicy.KEEP,
                work,
                Collections.singletonList(this));
    }

    @Override
    public LiveData<List<WorkStatus>> getStatuses() {
        return mWorkManagerImpl.getStatusesById(mAllIds);
    }

    @Override
    public void enqueue() {
        // Only enqueue if not already enqueued.
        if (!mEnqueued) {
            // The runnable walks the hierarchy of the continuations
            // and marks them enqueued using the markEnqueued() method, parent first.
            mWorkManagerImpl.getTaskExecutor()
                    .executeOnBackgroundThread(new EnqueueRunnable(this));
        } else {
            Logger.warn(TAG, "Already enqueued work ids (%s)", TextUtils.join(", ", mIds));
        }
    }

    @Override
    @WorkerThread
    public void enqueueSync() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            throw new IllegalStateException("Cannot enqueueSync on main thread!");
        }

        if (!mEnqueued) {
            // The runnable walks the hierarchy of the continuations
            // and marks them enqueued using the markEnqueued() method, parent first.
            new EnqueueRunnable(this).run();
        } else {
            Logger.warn(TAG, "Already enqueued work ids (%s)", TextUtils.join(", ", mIds));
        }
    }

    @Override
    protected WorkContinuation joinInternal(
            @Nullable Work work,
            @NonNull List<WorkContinuation> continuations) {

        if (work == null) {
            work = new Work.Builder(JoinWorker.class).build();
        }

        List<WorkContinuationImpl> parents = new ArrayList<>(continuations.size());
        for (WorkContinuation continuation : continuations) {
            parents.add((WorkContinuationImpl) continuation);
        }

        return new WorkContinuationImpl(mWorkManagerImpl,
                null,
                ExistingWorkPolicy.KEEP,
                Collections.singletonList(work),
                parents);
    }
}
