/*
 * Copyright 2017 The Android Open Source Project
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
package androidx.work.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.impl.model.WorkSpec;

/**
 * An interface for classes responsible for scheduling background work.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Scheduler {

    /**
     * The maximum number of {@link WorkSpec}s that can be scheduled at a given point in time.
     */
    int MAX_SCHEDULER_LIMIT = 50;

    /**
     * The maximum number of {@link WorkSpec}s that are considered for execution by the greedy
     * scheduler.
     */
    int MAX_GREEDY_SCHEDULER_LIMIT = 200;

    /**
     * Schedule the given {@link WorkSpec}s for background execution.  The Scheduler does NOT need
     * to check if there are any dependencies.
     *
     * @param workSpecs The array of {@link WorkSpec}s to schedule
     */
    void schedule(@NonNull WorkSpec... workSpecs);

    /**
     * Cancel the work identified by the given {@link WorkSpec} id.
     *
     * @param workSpecId The id of the work to stopWork
     */
    void cancel(@NonNull String workSpecId);

    /**
     * This is <code>true</code> when a {@link Scheduler} has a fixed number of slots.
     *
     * @return <code>true</code> if a {@link Scheduler} has limited scheduling slots.
     */
    boolean hasLimitedSchedulingSlots();
}
