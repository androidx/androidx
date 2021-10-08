/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import java.util.Collections;
import java.util.List;

/**
 * Initializes {@link androidx.work.WorkManager} using {@code androidx.startup}.
 */
public final class WorkManagerInitializer implements Initializer<WorkManager> {

    private static final String TAG = Logger.tagWithPrefix("WrkMgrInitializer");

    @NonNull
    @Override
    public WorkManager create(@NonNull Context context) {
        // Initialize WorkManager with the default configuration.
        Logger.get().debug(TAG, "Initializing WorkManager with default configuration.");
        WorkManager.initialize(context, new Configuration.Builder().build());
        return WorkManager.getInstance(context);
    }

    @NonNull
    @Override
    public List<Class<? extends androidx.startup.Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
