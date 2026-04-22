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

/** Listener interface that is called for events related to a worker's scheduling. */
@ExperimentalEventsApi
public interface ScheduleEventListener {

    /**
     * Called when a work request is enqueued from the app e.g. [WorkManager.enqueue].
     *
     * @param workInfo Snapshot of the work info
     */
    public suspend fun onEnqueued(workInfo: WorkInfo) {}

    /**
     * Called when a work request is updated from the app e.g. [WorkManager.updateWork].
     *
     * @param oldWorkInfo Snapshot of the work info before the update
     * @param updatedWorkInfo Snapshot of the work info after the update
     */
    public suspend fun onUpdated(oldWorkInfo: WorkInfo, updatedWorkInfo: WorkInfo) {}

    /**
     * Called when a work request is no longer waiting on any of its prerequisite work. If the work
     * has no prerequisite work, this is called immediately after [onEnqueued].
     *
     * @param workInfo Snapshot of the work info
     */
    public suspend fun onUnblocked(workInfo: WorkInfo) {}

    /**
     * Called when a work request is canceled from the app e.g. [WorkManager.cancelWorkById].
     *
     * @param workInfo Snapshot of the work info
     */
    public suspend fun onCancelled(workInfo: WorkInfo) {}

    /**
     * Called when a work request fails because a prerequisite work fails.
     *
     * @param workInfo Snapshot of the work info
     */
    public suspend fun onPrerequisiteFailed(workInfo: WorkInfo) {}
}
