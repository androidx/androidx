/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl.ipc.internal;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation of {@link ExecutionTracker}.
 *
 * @hide
 */
@SuppressWarnings("ExecutorTaskName")
@RestrictTo(Scope.LIBRARY)
public class DefaultExecutionTracker implements ExecutionTracker {
    private final Set<SettableFuture<?>> mFuturesInProgress = new HashSet<>();

    @Override
    public void track(SettableFuture<?> future) {
        mFuturesInProgress.add(future);
        future.addListener(() -> mFuturesInProgress.remove(future), MoreExecutors.directExecutor());
    }

    @Override
    public void cancelPendingFutures(Throwable throwable) {
        for (SettableFuture<?> future : mFuturesInProgress) {
            future.setException(throwable);
        }
        mFuturesInProgress.clear();
    }
}
