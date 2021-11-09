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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Data;
import androidx.work.Logger;
import androidx.work.ProgressUpdater;
import androidx.work.WorkInfo.State;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.model.WorkProgress;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.UUID;

/**
 * Persists {@link androidx.work.ListenableWorker} progress in a {@link WorkDatabase}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkProgressUpdater implements ProgressUpdater {

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    static final String TAG = Logger.tagWithPrefix("WorkProgressUpdater");

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final WorkDatabase mWorkDatabase;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final TaskExecutor mTaskExecutor;

    public WorkProgressUpdater(
            @NonNull WorkDatabase workDatabase,
            @NonNull TaskExecutor taskExecutor) {
        mWorkDatabase = workDatabase;
        mTaskExecutor = taskExecutor;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> updateProgress(
            @NonNull final Context context,
            @NonNull final UUID id,
            @NonNull final Data data) {
        final SettableFuture<Void> future = SettableFuture.create();
        mTaskExecutor.executeOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                String workSpecId = id.toString();
                Logger.get().debug(TAG, "Updating progress for " + id + " (" + data + ")");
                mWorkDatabase.beginTransaction();
                try {
                    WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
                    WorkSpec workSpec = workSpecDao.getWorkSpec(workSpecId);
                    if (workSpec != null) {
                        State state = workSpec.state;
                        // Update Progress
                        if (state == State.RUNNING) {
                            WorkProgress progress = new WorkProgress(workSpecId, data);
                            mWorkDatabase.workProgressDao().insert(progress);
                        } else {
                            Logger.get().warning(TAG,
                                    "Ignoring setProgressAsync(...). WorkSpec (" +
                                            workSpecId +
                                            ") is not in a RUNNING state.");
                        }
                    } else {
                        String message =
                                "Calls to setProgressAsync() must complete before a "
                                        + "ListenableWorker signals completion of work by "
                                        + "returning an instance of Result.";
                        throw new IllegalStateException(message);
                    }
                    future.set(null);
                    mWorkDatabase.setTransactionSuccessful();
                } catch (Throwable throwable) {
                    Logger.get().error(TAG, "Error updating Worker progress", throwable);
                    future.setException(throwable);
                } finally {
                    mWorkDatabase.endTransaction();
                }
            }
        });
        return future;
    }
}
