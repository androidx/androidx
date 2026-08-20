/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.work.impl

import androidx.annotation.RestrictTo
import androidx.work.impl.model.WorkGenerationalId

/**
 * A listener that is notified when work is promoted to or demoted from running under a Foreground
 * Service.
 *
 * Listeners are notified when a worker calls [androidx.work.ListenableWorker.setForegroundAsync].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface ForegroundListener {
    /**
     * Called when the foreground state of a work changes. Guaranteed to run on
     * [androidx.work.impl.utils.taskexecutor.SerialExecutor].
     *
     * @param id The [WorkGenerationalId] of the work
     * @param isForeground True if the work is now running under a Foreground Service, false
     *   otherwise
     */
    public fun onForegroundChanged(id: WorkGenerationalId, isForeground: Boolean)
}
