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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link RemoteWorkContinuation}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteWorkContinuationImpl extends RemoteWorkContinuation {
    private final RemoteWorkManagerClient mClient;
    private final WorkContinuation mContinuation;

    public RemoteWorkContinuationImpl(
            @NonNull RemoteWorkManagerClient client,
            @NonNull WorkContinuation continuation) {
        mClient = client;
        mContinuation = continuation;
    }

    @NonNull
    @Override
    @SuppressLint("EnqueueWork")
    public RemoteWorkContinuation then(@NonNull List<OneTimeWorkRequest> work) {
        return new RemoteWorkContinuationImpl(mClient, mContinuation.then(work));
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enqueue() {
        return mClient.enqueue(mContinuation);
    }

    @NonNull
    @Override
    @SuppressLint("EnqueueWork")
    protected RemoteWorkContinuation combineInternal(
            @NonNull List<RemoteWorkContinuation> continuations) {

        int size = continuations.size();
        List<WorkContinuation> workContinuations = new ArrayList<>(size);
        for (RemoteWorkContinuation continuation : continuations) {
            workContinuations.add(((RemoteWorkContinuationImpl) continuation).mContinuation);
        }
        WorkContinuation result = WorkContinuation.combine(workContinuations);
        return new RemoteWorkContinuationImpl(mClient, result);
    }
}
