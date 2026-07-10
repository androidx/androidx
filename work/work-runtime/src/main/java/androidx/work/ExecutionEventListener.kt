/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.work

import androidx.work.ListenableWorker.Result

/**
 * Listener interface that is called for events related to a worker executing.
 *
 * Note that a given work request may have multiple executions due to stops and retries.
 */
@ExperimentalEventsApi
public interface ExecutionEventListener {

    /**
     * Called when a work request starts executing i.e. [ListenableWorker.startWork].
     *
     * @param workInfo Snapshot of the work info
     */
    public suspend fun onStarted(workInfo: WorkInfo)

    /**
     * Called when a work request is stopped by the system after it has started.
     *
     * @param stopReason Reason why work stopped
     * @param workInfo Snapshot of the work info
     */
    public suspend fun onStopped(@StopReason stopReason: Int, workInfo: WorkInfo)

    /**
     * Called when the worker finishes and returns a value.
     *
     * @param result [Result] from work finishing
     * @param workInfo Snapshot of the work info
     */
    public suspend fun onFinished(result: Result, workInfo: WorkInfo)

    /**
     * Called when the work request fails from an exception after it has started.
     *
     * @param throwable Throwable thrown by worker
     * @param workInfo Snapshot of the work info
     */
    public suspend fun onException(throwable: Throwable, workInfo: WorkInfo)
}
