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

package androidx.work.testing;

import android.content.Context;
import android.net.Network;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkRequest;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Builds instances of {@link androidx.work.ListenableWorker} which can be used for testing.
 *
 * @param <B> The actual {@link TestListenableWorkerBuilder} subtype being built.
 */
public class TestListenableWorkerBuilder<B extends TestListenableWorkerBuilder> {
    private Context mContext;
    private String mWorkerName;
    private UUID mId;
    private Data mInputData;
    private List<String> mTags;
    private int mRunAttemptCount;
    private WorkerParameters.RuntimeExtras mRuntimeExtras;
    private WorkerFactory mWorkerFactory;
    private TaskExecutor mTaskExecutor;
    private Executor mExecutor;

    TestListenableWorkerBuilder(@NonNull Context applicationContext, @NonNull String workerName) {
        mContext = applicationContext;
        mWorkerName = workerName;
        mId = UUID.randomUUID();
        mInputData = Data.EMPTY;
        mTags = Collections.emptyList();
        mRunAttemptCount = 1;
        mRuntimeExtras = new WorkerParameters.RuntimeExtras();
        mWorkerFactory = WorkerFactory.getDefaultWorkerFactory();
        mTaskExecutor = new InstantWorkTaskExecutor();
        mExecutor = mTaskExecutor.getBackgroundExecutor();
    }

    /**
     * @return The application {@link Context}.
     */
    @NonNull
    Context getApplicationContext() {
        return mContext;
    }

    /**
     * @return The {@link String} name of the unit of work.
     */
    @NonNull
    String getWorkerName() {
        return mWorkerName;
    }

    /**
     * @return The {@link UUID} id associated with this unit of work.
     */
    @NonNull
    UUID getId() {
        return mId;
    }

    /**
     * @return The input {@link Data} associated with this unit of work.
     */
    @NonNull
    Data getInputData() {
        return mInputData;
    }

    /**
     * @return The {@link List<String>} tags associated with this unit of work.
     */
    @NonNull
    List<String> getTags() {
        return mTags;
    }

    /**
     * @return The unit of work's current run attempt count.
     */
    int getRunAttemptCount() {
        return mRunAttemptCount;
    }

    /**
     * @return The {@link androidx.work.WorkerParameters.RuntimeExtras} associated with this unit
     * of work.
     */
    @NonNull
    WorkerParameters.RuntimeExtras getRuntimeExtras() {
        return mRuntimeExtras;
    }

    /**
     * @return The {@link WorkerFactory} associated with this unit of work.
     */
    @NonNull
    WorkerFactory getWorkerFactory() {
        return mWorkerFactory;
    }

    /**
     * @return The {@link TaskExecutor} associated with this unit of work.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    TaskExecutor getTaskExecutor() {
        return mTaskExecutor;
    }

    /**
     * @return The {@link TaskExecutor} associated with this unit of work.
     */
    @NonNull
    Executor getExecutor() {
        return mExecutor;
    }

    /**
     * @return The instance thats being built.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    B getThis() {
        // B = TestListenableWorkerBuilder here. Java :(
        return (B) this;
    }

    /**
     * Sets the id for this unit of work.
     *
     * @param id The {@link UUID}
     * @return The current {@link TestListenableWorkerBuilder}
     */
    @NonNull
    public B setId(@NonNull UUID id) {
        mId = id;
        return getThis();
    }

    /**
     * Adds input {@link Data} to the work.
     *
     * @param inputData key/value pairs that will be provided to the worker
     * @return The current {@link TestListenableWorkerBuilder}
     */
    @NonNull
    public B setInputData(@NonNull Data inputData) {
        mInputData = inputData;
        return getThis();
    }

    /**
     * Sets the tags associated with this unit of work.
     *
     * @param tags The {@link List} of tags to be used
     * @return The current {@link TestListenableWorkerBuilder}
     */
    @NonNull
    public B setTags(@NonNull List<String> tags) {
        mTags = tags;
        return getThis();
    }

    /**
     * Sets the initial run attempt count for this work.
     *
     * @param runAttemptCount The initial run attempt count
     * @return The current {@link TestListenableWorkerBuilder}
     */
    @NonNull
    public B setRunAttemptCount(int runAttemptCount) {
        mRunAttemptCount = runAttemptCount;
        return getThis();
    }

    /**
     * Sets the list of Content {@link Uri}s associated with this unit of work.
     *
     * @param contentUris The list of content {@link Uri}s
     * @return The current {@link TestListenableWorkerBuilder}
     */
    @RequiresApi(24)
    @NonNull
    public B setTriggeredContentUris(@NonNull List<Uri> contentUris) {
        mRuntimeExtras.triggeredContentUris = contentUris;
        return getThis();
    }

    /**
     * Sets the authorities for content {@link Uri}'s associated with this unit of work.
     *
     * @param authorities The {@link List} of authorities
     * @return The current {@link TestListenableWorkerBuilder}
     */
    @RequiresApi(24)
    @NonNull
    public B setTriggeredContentAuthorities(
            @NonNull List<String> authorities) {
        mRuntimeExtras.triggeredContentAuthorities = authorities;
        return getThis();
    }

    /**
     * Sets the network associated with this unit of work.
     *
     * @param network The {@link Network} associated with this unit of work
     * @return The current {@link TestListenableWorkerBuilder}
     */
    @RequiresApi(28)
    @NonNull
    public B setNetwork(@NonNull Network network) {
        mRuntimeExtras.network = network;
        return getThis();
    }

    /**
     * Sets the {@link WorkerFactory} to be used to construct the
     * {@link androidx.work.ListenableWorker}.
     *
     * @param workerFactory The {@link WorkerFactory} to use to construct the
     *                      {@link androidx.work.ListenableWorker}
     * @return The current {@link TestListenableWorkerBuilder}
     */
    @NonNull
    public B setWorkerFactory(@NonNull WorkerFactory workerFactory) {
        mWorkerFactory = workerFactory;
        return getThis();
    }

    /**
     * Sets the {@link Executor} that can be used to execute this unit of work.
     *
     * @param executor The {@link Executor}
     * @return The current {@link TestListenableWorkerBuilder}
     */
    @NonNull
    B setExecutor(@NonNull Executor executor) {
        mExecutor = executor;
        return getThis();
    }

    /**
     * Builds the {@link ListenableWorker}.
     *
     * @return the instance of a {@link ListenableWorker}.
     */
    @NonNull
    public ListenableWorker build() {
        WorkerParameters parameters =
                new WorkerParameters(
                        getId(),
                        getInputData(),
                        getTags(),
                        getRuntimeExtras(),
                        getRunAttemptCount(),
                        // This is unused for ListenableWorker
                        getExecutor(),
                        getTaskExecutor(),
                        getWorkerFactory()
                );

        WorkerFactory defaultFactory = WorkerFactory.getDefaultWorkerFactory();
        ListenableWorker worker =
                defaultFactory.createWorkerWithDefaultFallback(
                        getApplicationContext(),
                        getWorkerName(),
                        parameters);

        if (worker == null) {
            throw new IllegalStateException(
                    String.format("Could not create an instance of ListenableWorker %s",
                            getWorkerName()));
        }
        return worker;
    }

    /**
     * Creates a new instance of a {@link TestListenableWorkerBuilder} from a {@link WorkRequest}.
     *
     * @param context     The {@link Context}
     * @param workRequest The {@link WorkRequest}
     * @return The new instance of a {@link ListenableWorker}
     */
    @NonNull
    public static TestListenableWorkerBuilder from(
            @NonNull Context context,
            @NonNull WorkRequest workRequest) {
        WorkSpec workSpec = workRequest.getWorkSpec();
        String name = workSpec.workerClassName;
        List<String> tags = new ArrayList<>(workRequest.getTags().size());
        tags.addAll(workRequest.getTags());
        return new TestListenableWorkerBuilder(context.getApplicationContext(), name)
                .setId(workRequest.getId())
                .setTags(tags)
                .setInputData(workSpec.input);
    }
}
