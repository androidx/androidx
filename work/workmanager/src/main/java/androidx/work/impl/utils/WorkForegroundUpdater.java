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

package androidx.work.impl.utils;

import static androidx.work.impl.foreground.SystemForegroundDispatcher.createNotifyIntent;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.ForegroundInfo;
import androidx.work.ForegroundUpdater;
import androidx.work.impl.foreground.ForegroundProcessor;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.UUID;


/**
 * Transitions a {@link androidx.work.ListenableWorker} to run in the context of a foreground
 * {@link android.app.Service}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkForegroundUpdater implements ForegroundUpdater {

    private final TaskExecutor mTaskExecutor;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final ForegroundProcessor mForegroundProcessor;


    public WorkForegroundUpdater(
            @NonNull ForegroundProcessor foregroundProcessor,
            @NonNull TaskExecutor taskExecutor) {

        mForegroundProcessor = foregroundProcessor;
        mTaskExecutor = taskExecutor;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setForegroundAsync(
            @NonNull final Context context,
            @NonNull final UUID id,
            @NonNull final ForegroundInfo foregroundInfo) {

        final SettableFuture<Void> future = SettableFuture.create();
        mTaskExecutor.executeOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!future.isCancelled()) {
                        String workSpecId = id.toString();
                        // startForeground() is idempotent
                        mForegroundProcessor.startForeground(workSpecId, foregroundInfo);
                        Intent intent = createNotifyIntent(context, workSpecId, foregroundInfo);
                        context.startService(intent);
                    }
                    future.set(null);
                } catch (Throwable throwable) {
                    future.setException(throwable);
                }
            }
        });

        return future;
    }
}
