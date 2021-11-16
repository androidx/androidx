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

package androidx.work.impl.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.os.BuildCompat;
import androidx.work.ForegroundInfo;
import androidx.work.ForegroundUpdater;
import androidx.work.ListenableWorker;
import androidx.work.Logger;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkForegroundRunnable implements Runnable {

    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("WorkForegroundRunnable");

    final SettableFuture<Void> mFuture;

    final Context mContext;
    final WorkSpec mWorkSpec;
    final ListenableWorker mWorker;
    final ForegroundUpdater mForegroundUpdater;
    final TaskExecutor mTaskExecutor;

    @SuppressLint("LambdaLast")
    public WorkForegroundRunnable(
            @NonNull Context context,
            @NonNull WorkSpec workSpec,
            @NonNull ListenableWorker worker,
            @NonNull ForegroundUpdater foregroundUpdater,
            @NonNull TaskExecutor taskExecutor) {

        mFuture = SettableFuture.create();
        mContext = context;
        mWorkSpec = workSpec;
        mWorker = worker;
        mForegroundUpdater = foregroundUpdater;
        mTaskExecutor = taskExecutor;
    }

    @NonNull
    public ListenableFuture<Void> getFuture() {
        return mFuture;
    }

    @Override
    @SuppressLint("UnsafeExperimentalUsageError")
    public void run() {
        if (!mWorkSpec.expedited || BuildCompat.isAtLeastS()) {
            mFuture.set(null);
            return;
        }

        final SettableFuture<ForegroundInfo> foregroundFuture = SettableFuture.create();
        mTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                foregroundFuture.setFuture(mWorker.getForegroundInfoAsync());
            }
        });

        foregroundFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ForegroundInfo foregroundInfo = foregroundFuture.get();
                    if (foregroundInfo == null) {
                        String message = "Worker was marked important (" +
                                mWorkSpec.workerClassName +
                                ") but did not provide ForegroundInfo";
                        throw new IllegalStateException(message);
                    }
                    Logger.get().debug(TAG,
                            "Updating notification for " + mWorkSpec.workerClassName);
                    // Mark as running in the foreground
                    mWorker.setRunInForeground(true);
                    mFuture.setFuture(
                            mForegroundUpdater.setForegroundAsync(
                                    mContext, mWorker.getId(), foregroundInfo));
                } catch (Throwable throwable) {
                    mFuture.setException(throwable);
                }
            }
        }, mTaskExecutor.getMainThreadExecutor());
    }
}
