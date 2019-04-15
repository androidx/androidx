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

package androidx.camera.core.impl.utils.executor;

import java.util.concurrent.Executor;

/**
 * Utility class for generating specific implementations of {@link Executor}.
 */
public final class CameraXExecutors {

    /** Returns a cached {@link Executor} which posts to the main thread. */
    public static Executor mainThreadExecutor() {
        return MainThreadExecutor.getInstance();
    }

    /** Returns a cached {@link Executor} suitable for disk I/O. */
    public static Executor ioExecutor() {
        return IoExecutor.getInstance();
    }

    /** Returns a cached executor that runs tasks directly from the calling thread. */
    public static Executor directExecutor() {
        return DirectExecutor.getInstance();
    }

    // Should not be instantiated
    private CameraXExecutors() {}
}
