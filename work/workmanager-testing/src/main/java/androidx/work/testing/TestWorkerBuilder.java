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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.impl.model.WorkSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Builds instances of {@link Worker} which can be used for testing.
 */
public class TestWorkerBuilder extends TestListenableWorkerBuilder<TestWorkerBuilder> {

    private TestWorkerBuilder(
            @NonNull Context context,
            @NonNull String workerName,
            @Nullable Executor executor) {
        super(context, workerName);
        if (executor != null) {
            setExecutor(executor);
        }
    }

    @NonNull
    @Override
    TestWorkerBuilder getThis() {
        return this;
    }

    /**
     * Builds the {@link Worker}.
     *
     * @return the instance of a {@link Worker}.
     */
    @NonNull
    public Worker build() {
        return (Worker) super.build();
    }

    /**
     * Creates a new instance of a {@link TestWorkerBuilder} from a {@link WorkRequest}.
     *
     * @param context     The {@link Context}
     * @param workRequest The {@link WorkRequest}
     * @return The new instance of a {@link TestWorkerBuilder}
     */
    @NonNull
    public static TestWorkerBuilder from(
            @NonNull Context context,
            @NonNull WorkRequest workRequest) {
        return from(context, workRequest, null);
    }

    /**
     * Sets the {@link Executor} that can be used to execute this unit of work.
     *
     * @param executor The {@link Executor}
     * @return The current {@link TestWorkerBuilder}
     */
    @NonNull
    @Override
    public TestWorkerBuilder setExecutor(@NonNull Executor executor) {
        return super.setExecutor(executor);
    }

    /**
     * Creates a new instance of a {@link TestWorkerBuilder} from a {@link WorkRequest} that runs on
     * the given {@link Executor}.
     *
     * @param context     The {@link Context}
     * @param workRequest The {@link WorkRequest}
     * @param executor    The {@link Executor}
     * @return The new instance of a {@link TestWorkerBuilder}
     */
    @NonNull
    public static TestWorkerBuilder from(
            @NonNull Context context,
            @NonNull WorkRequest workRequest,
            @Nullable Executor executor) {
        WorkSpec workSpec = workRequest.getWorkSpec();
        String name = workSpec.workerClassName;
        if (!isValidWorker(name)) {
            throw new IllegalArgumentException(
                    String.format("Invalid worker class name or class does not extend Worker (%s)",
                            name));
        }
        List<String> tags = new ArrayList<>(workRequest.getTags().size());
        tags.addAll(workRequest.getTags());
        return new TestWorkerBuilder(context, name, executor)
                .setId(workRequest.getId())
                .setTags(tags)
                .setInputData(workSpec.input);
    }

    private static boolean isValidWorker(@NonNull String className) {
        try {
            Class<?> klass = Class.forName(className);
            return Worker.class.isAssignableFrom(klass);
        } catch (Throwable ignore) {
            return false;
        }
    }
}
