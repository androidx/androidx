/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.viewfinder.internal.utils.executor;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Utility class for generating specific implementations of {@link Executor}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CameraExecutors {

    // Should not be instantiated
    private CameraExecutors() {
    }

    /** Returns a cached {@link ScheduledExecutorService} which posts to the main thread. */
    @NonNull
    public static ScheduledExecutorService mainThreadExecutor() {
        return MainThreadExecutor.getInstance();
    }

    /** Returns a cached executor that runs tasks directly from the calling thread. */
    @NonNull
    public static Executor directExecutor() {
        return DirectExecutor.getInstance();
    }
}
