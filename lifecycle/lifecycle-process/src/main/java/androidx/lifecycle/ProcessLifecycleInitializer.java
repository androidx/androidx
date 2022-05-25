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

package androidx.lifecycle;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.AppInitializer;
import androidx.startup.Initializer;

import java.util.Collections;
import java.util.List;

/**
 * Initializes {@link ProcessLifecycleOwner} using {@code androidx.startup}.
 */
public final class ProcessLifecycleInitializer implements Initializer<LifecycleOwner> {

    @NonNull
    @Override
    public LifecycleOwner create(@NonNull Context context) {
        AppInitializer appInitializer = AppInitializer.getInstance(context);
        if (!appInitializer.isEagerlyInitialized(getClass())) {
            throw new IllegalStateException(
                    "ProcessLifecycleInitializer cannot be initialized lazily. \n"
                            + "Please ensure that you have: \n"
                            + "<meta-data\n"
                            + "    android:name='androidx.lifecycle.ProcessLifecycleInitializer' \n"
                            + "    android:value='androidx.startup' /> \n"
                            + "under InitializationProvider in your AndroidManifest.xml");
        }
        LifecycleDispatcher.init(context);
        ProcessLifecycleOwner.init(context);
        return ProcessLifecycleOwner.get();
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
