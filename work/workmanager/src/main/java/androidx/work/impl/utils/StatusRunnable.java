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

package androidx.work.impl.utils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.work.WorkInfo;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.UUID;

/**
 * A {@link Runnable} to get {@link WorkInfo}es.
 *
 * @param <T> The expected return type for the {@link ListenableFuture}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class StatusRunnable<T> implements Runnable {
    private final SettableFuture<T> mFuture = SettableFuture.create();

    @Override
    public void run() {
        try {
            final T value = runInternal();
            mFuture.set(value);
        } catch (Throwable throwable) {
            mFuture.setException(throwable);
        }
    }

    @WorkerThread
    abstract T runInternal();

    public ListenableFuture<T> getFuture() {
        return mFuture;
    }

    /**
     * Creates a {@link StatusRunnable} which can get statuses for a given {@link List} of
     * {@link String} workSpec ids.
     *
     * @param workManager The {@link WorkManagerImpl} to use
     * @param ids         The {@link List} of {@link String} ids
     * @return an instance of {@link StatusRunnable}
     */
    public static StatusRunnable<List<WorkInfo>> forStringIds(
            @NonNull final WorkManagerImpl workManager,
            @NonNull final List<String> ids) {

        return new StatusRunnable<List<WorkInfo>>() {
            @Override
            public List<WorkInfo> runInternal() {
                WorkDatabase workDatabase = workManager.getWorkDatabase();
                List<WorkSpec.WorkInfoPojo> workInfoPojos =
                        workDatabase.workSpecDao().getWorkStatusPojoForIds(ids);

                return WorkSpec.WORK_INFO_MAPPER.apply(workInfoPojos);
            }
        };
    }

    /**
     * Creates a {@link StatusRunnable} which can get statuses for a specific {@link UUID}
     * workSpec id.
     *
     * @param workManager The {@link WorkManagerImpl} to use
     * @param id          The workSpec {@link UUID}
     * @return an instance of {@link StatusRunnable}
     */
    public static StatusRunnable<WorkInfo> forUUID(
            @NonNull final WorkManagerImpl workManager,
            @NonNull final UUID id) {

        return new StatusRunnable<WorkInfo>() {
            @Override
            WorkInfo runInternal() {
                WorkDatabase workDatabase = workManager.getWorkDatabase();
                WorkSpec.WorkInfoPojo workInfoPojo =
                        workDatabase.workSpecDao().getWorkStatusPojoForId(id.toString());

                return workInfoPojo != null ? workInfoPojo.toWorkInfo() : null;
            }
        };
    }

    /**
     * Creates a {@link StatusRunnable} which can get statuses for {@link WorkSpec}s annotated with
     * the given {@link String} tag.
     *
     * @param workManager The {@link WorkManagerImpl} to use
     * @param tag The {@link String} tag
     * @return an instance of {@link StatusRunnable}
     */
    public static StatusRunnable<List<WorkInfo>> forTag(
            @NonNull final WorkManagerImpl workManager,
            @NonNull final String tag) {

        return new StatusRunnable<List<WorkInfo>>() {
            @Override
            List<WorkInfo> runInternal() {
                WorkDatabase workDatabase = workManager.getWorkDatabase();
                List<WorkSpec.WorkInfoPojo> workInfoPojos =
                        workDatabase.workSpecDao().getWorkStatusPojoForTag(tag);

                return WorkSpec.WORK_INFO_MAPPER.apply(workInfoPojos);
            }
        };
    }

    /**
     * Creates a {@link StatusRunnable} which can get statuses for {@link WorkSpec}s annotated with
     * the given {@link String} unique name.
     *
     * @param workManager The {@link WorkManagerImpl} to use
     * @param name The {@link String} unique name
     * @return an instance of {@link StatusRunnable}
     */
    public static StatusRunnable<List<WorkInfo>> forUniqueWork(
            @NonNull final WorkManagerImpl workManager,
            @NonNull final String name) {

        return new StatusRunnable<List<WorkInfo>>() {
            @Override
            List<WorkInfo> runInternal() {
                WorkDatabase workDatabase = workManager.getWorkDatabase();
                List<WorkSpec.WorkInfoPojo> workInfoPojos =
                        workDatabase.workSpecDao().getWorkStatusPojoForName(name);

                return WorkSpec.WORK_INFO_MAPPER.apply(workInfoPojos);
            }
        };
    }
}
