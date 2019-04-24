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

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * Utility class for generating specific implementations of {@link Executor}.
 */
public final class CameraXExecutors {

    // Should not be instantiated
    private CameraXExecutors() {
    }

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

    /**
     * Returns a new executor which will perform all tasks sequentially.
     *
     * <p>The returned executor delegates all tasks to the provided delegate Executor, but will
     * ensure all tasks are run in order and without overlapping. Note this can only be
     * guaranteed for tasks that are submitted via the same sequential executor. Tasks submitted
     * directly to the delegate or to different instances of the sequential executor do not have
     * any ordering guarantees.
     */
    public static Executor newSequentialExecutor(@NonNull Executor delegate) {
        return new SequentialExecutor(delegate);
    }
}
