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
import static androidx.work.impl.model.WorkSpecKt.generationalId;
import static androidx.work.ListenableFutureKt.executeAsync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.ForegroundInfo;
import androidx.work.ForegroundUpdater;
import androidx.work.Logger;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.foreground.ForegroundProcessor;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.UUID;


/**
 * Transitions a {@link androidx.work.ListenableWorker} to run in the context of a foreground
 * {@link android.app.Service}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkForegroundUpdater implements ForegroundUpdater {

    private static final String TAG = Logger.tagWithPrefix("WMFgUpdater");

    private final TaskExecutor mTaskExecutor;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final ForegroundProcessor mForegroundProcessor;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final WorkSpecDao mWorkSpecDao;

    @SuppressLint("LambdaLast")
    public WorkForegroundUpdater(
            @NonNull WorkDatabase workDatabase,
            @NonNull ForegroundProcessor foregroundProcessor,
            @NonNull TaskExecutor taskExecutor) {

        mForegroundProcessor = foregroundProcessor;
        mTaskExecutor = taskExecutor;
        mWorkSpecDao = workDatabase.workSpecDao();
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setForegroundAsync(
            @NonNull final Context context,
            @NonNull final UUID id,
            @NonNull final ForegroundInfo foregroundInfo) {
        return executeAsync(mTaskExecutor.getSerialTaskExecutor(), "setForegroundAsync",
                () -> {
                    String workSpecId = id.toString();
                    WorkSpec workSpec = mWorkSpecDao.getWorkSpec(workSpecId);
                    if (workSpec == null || workSpec.state.isFinished()) {
                        // state == null would mean that the WorkSpec was replaced.
                        String message =
                                "Calls to setForegroundAsync() must complete before a "
                                        + "ListenableWorker signals completion of work by "
                                        + "returning an instance of Result.";
                        throw new IllegalStateException(message);
                    }
                    // startForeground() is idempotent
                    // NOTE: This will fail when the process is subject to foreground service
                    // restrictions. Propagate the exception to the caller.
                    mForegroundProcessor.startForeground(workSpecId, foregroundInfo);
                    // it is safe to take generation from this workspec, because only
                    // one generation of the same work can run at a time.
                    Intent intent = createNotifyIntent(context, generationalId(workSpec),
                            foregroundInfo);
                    context.startService(intent);
                    return null;
                });
    }
}
