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
 *
 * @param <W> The actual subtype of {@link Worker}
 */
public class TestWorkerBuilder<W extends Worker> extends TestListenableWorkerBuilder<W> {

    private TestWorkerBuilder(
            @NonNull Context context,
            @NonNull Class<W> workerClass,
            @Nullable Executor executor) {
        super(context, workerClass);
        if (executor != null) {
            setExecutor(executor);
        }
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
    @SuppressWarnings("unchecked")
    public static TestWorkerBuilder<? extends Worker> from(
            @NonNull Context context,
            @NonNull WorkRequest workRequest,
            @NonNull Executor executor) {
        WorkSpec workSpec = workRequest.getWorkSpec();
        String name = workSpec.workerClassName;

        Class<Worker> workerClass = getWorkerClass(name);
        if (workerClass == null) {
            throw new IllegalArgumentException(
                    "Invalid worker class name or class does not extend Worker (" + name + ")");
        }

        List<String> tags = new ArrayList<>(workRequest.getTags().size());
        tags.addAll(workRequest.getTags());

        TestWorkerBuilder<Worker> builder =
                new TestWorkerBuilder<>(context, workerClass, executor);

        builder.setId(workRequest.getId())
                .setTags(tags)
                .setInputData(workSpec.input);

        return builder;
    }

    /**
     * Creates a new instance of a {@link TestWorkerBuilder} with the worker {@link Class} that
     * runs on the given {@link Executor}.
     *
     * @param context     The {@link Context}
     * @param workerClass The subtype of {@link Worker} being built
     * @param executor    The {@link Executor}
     * @return The new instance of a {@link TestWorkerBuilder}
     */
    @NonNull
    public static <W extends Worker> TestWorkerBuilder<W> from(
            @NonNull Context context,
            @NonNull Class<W> workerClass,
            @NonNull Executor executor) {
        return new TestWorkerBuilder<>(context, workerClass, executor);
    }

    @SuppressWarnings("unchecked")
    private static Class<Worker> getWorkerClass(String className) {
        try {
            Class<?> klass = Class.forName(className);
            if (!Worker.class.isAssignableFrom(klass)) {
                return null;
            } else {
                return (Class<Worker>) klass;
            }
        } catch (Throwable ignore) {
            return null;
        }
    }
}
