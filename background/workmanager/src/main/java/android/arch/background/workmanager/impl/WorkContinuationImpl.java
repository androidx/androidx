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

package android.arch.background.workmanager.impl;

import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkContinuation;
import android.arch.background.workmanager.WorkManager;
import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.impl.utils.BaseWorkHelper;
import android.arch.background.workmanager.impl.utils.EnqueueRunnable;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A concrete implementation of {@link WorkContinuation}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkContinuationImpl extends WorkContinuation {

    private static final String TAG = "LzyWrkContinuationImpl";

    @VisibleForTesting
    final WorkManagerImpl mWorkManagerImpl;

    final String mUniqueTag;

    final @WorkManager.ExistingWorkPolicy int mExistingWorkPolicy;

    @VisibleForTesting
    final Work[] mWork;

    @VisibleForTesting
    final String[] mIds;

    @VisibleForTesting
    final List<String> mAllIds;

    @VisibleForTesting
    boolean mEnqueued;

    @VisibleForTesting
    WorkContinuationImpl mParent;

    WorkContinuationImpl(@NonNull WorkManagerImpl workManagerImpl, @NonNull Work... work) {
        this(workManagerImpl, null, WorkManager.KEEP_EXISTING_WORK, work, null);
    }

    WorkContinuationImpl(@NonNull WorkManagerImpl workManagerImpl,
            @NonNull String uniqueTag,
            @WorkManager.ExistingWorkPolicy int existingWorkPolicy,
            @NonNull Work... work) {
        this(workManagerImpl, uniqueTag, existingWorkPolicy, work, null);
    }

    private WorkContinuationImpl(@NonNull WorkManagerImpl workManagerImpl,
            String uniqueTag,
            @WorkManager.ExistingWorkPolicy int existingWorkPolicy,
            @NonNull Work[] work,
            @Nullable WorkContinuationImpl parent) {
        mWorkManagerImpl = workManagerImpl;
        mUniqueTag = uniqueTag;
        mExistingWorkPolicy = existingWorkPolicy;
        mWork = work;
        final int parentSize = mParent != null ? mParent.mWork.length : 0;
        mIds = new String[mWork.length];
        mAllIds = new ArrayList<>(mWork.length + parentSize);
        if (parent != null) {
            mParent = parent;
            mAllIds.addAll(mParent.mAllIds);
        }
        for (int i = 0; i < work.length; i++) {
            mIds[i] = work[i].getId();
            mAllIds.add(work[i].getId());
        }
    }

    @Override
    public WorkContinuation then(Work... work) {
        // TODO (rahulrav@) We need to decide if we want to allow chaining of continuations after
        // an initial call to enqueue()
        return new WorkContinuationImpl(mWorkManagerImpl,
                mUniqueTag,
                WorkManager.KEEP_EXISTING_WORK,
                work,
                this);
    }

    @Override
    public WorkContinuation then(Class<? extends Worker>[] workerClasses) {
        return new WorkContinuationImpl(mWorkManagerImpl,
                mUniqueTag,
                WorkManager.KEEP_EXISTING_WORK,
                BaseWorkHelper.convertWorkerClassArrayToWorkArray(workerClasses),
                this);
    }

    @Override
    public LiveData<Map<String, Integer>> getStatuses() {
        return mWorkManagerImpl.getStatusesFor(mAllIds);
    }

    @Override
    public void enqueue() {
        // TODO(sumir): Change enqueuing to be transactional, where everything gets enqueued
        // together in one pass instead of spawning multiple runnables.

        // Only enqueue if not already enqueued.
        if (!mEnqueued) {
            if (mParent == null) {
                mWorkManagerImpl.getTaskExecutor()
                        .executeOnBackgroundThread(
                                new EnqueueRunnable(
                                        mWorkManagerImpl,
                                        mWork,
                                        null /* no prerequisites*/,
                                        mUniqueTag,
                                        mExistingWorkPolicy));
            } else {
                // has dependencies which need to be enqueued first
                mParent.enqueue();
                // now enqueue the work continuation, given the dependencies have been enqueued.
                mWorkManagerImpl.getTaskExecutor()
                        .executeOnBackgroundThread(
                                new EnqueueRunnable(
                                        mWorkManagerImpl,
                                        mWork,
                                        mParent.mIds,
                                        mUniqueTag,
                                        mExistingWorkPolicy));
            }
            mEnqueued = true;
        } else {
            Log.w(TAG,
                    String.format("Already enqueued work ids (%s).", TextUtils.join(", ", mIds)));
        }
    }
}
