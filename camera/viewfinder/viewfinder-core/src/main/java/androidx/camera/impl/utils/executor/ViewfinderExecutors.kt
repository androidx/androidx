/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.impl.utils.executor

import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService

/** Utility class for generating specific implementations of [Executor]. */
object ViewfinderExecutors {
    /** Returns a cached [ScheduledExecutorService] which posts to the main thread. */
    @JvmStatic
    fun mainThreadExecutor(): ScheduledExecutorService {
        return MainThreadExecutor.instance
    }

    /** Returns a cached executor that runs tasks directly from the calling thread. */
    @JvmStatic
    fun directExecutor(): Executor {
        return DirectExecutor.instance
    }
}
