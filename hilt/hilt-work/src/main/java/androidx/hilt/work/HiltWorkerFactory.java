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

package androidx.hilt.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import java.util.Map;

import javax.inject.Provider;

/**
 * Worker Factory for the Hilt Extension
 * <p>
 * A provider for this factory will be installed in the
 * {@link dagger.hilt.android.components.ApplicationComponent}.
 */
public final class HiltWorkerFactory extends androidx.work.WorkerFactory {

    private final Map<String,
            Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mWorkerFactories;

    public HiltWorkerFactory(@NonNull Map<String,
            Provider<WorkerAssistedFactory<? extends ListenableWorker>>> workerFactories) {
        mWorkerFactories = workerFactories;
    }

    @Nullable
    @Override
    public ListenableWorker createWorker(@NonNull Context appContext,
            @NonNull String workerClassName, @NonNull WorkerParameters workerParameters) {
        Provider<WorkerAssistedFactory<? extends ListenableWorker>> factoryProvider =
                mWorkerFactories.get(workerClassName);
        if (factoryProvider == null) {
            return null;
        }
        return factoryProvider.get().create(appContext, workerParameters);
    }
}
