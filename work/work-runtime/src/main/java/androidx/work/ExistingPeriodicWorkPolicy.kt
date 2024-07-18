/*
 * Copyright 2018 The Android Open Source Project
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

/**
 * An enumeration of the conflict resolution policies available to unique
 * [PeriodicWorkRequest]s in case of a collision.
 */
enum class ExistingPeriodicWorkPolicy {
    /**
     * If there is existing pending (uncompleted) work with the same unique name, cancel and delete
     * it. Then, insert the newly-specified work.
     */
    @Deprecated(
        message = "Deprecated in favor of the UPDATE policy. UPDATE policy has " +
            "very similar behavior: next run of the worker with the same unique name, " +
            "going to have new specification. However, UPDATE has better defaults: " +
            "unlike REPLACE policy UPDATE won't cancel the worker if it is currently running and " +
            "new worker specification will be used only on the next run. " +
            "Also it preserves original enqueue time, so unlike REPLACE period isn't reset. " +
            "If you want to preserve previous behavior, CANCEL_AND_REENQUEUE should be used.",
        replaceWith = ReplaceWith("UPDATE"),
    )
    REPLACE,

    /**
     * If there is existing pending (uncompleted) work with the same unique name, do nothing.
     * Otherwise, insert the newly-specified work.
     */
    KEEP,

    /**
     * If there is existing pending (uncompleted) work with the same unique name,
     * it will be updated the new specification. Otherwise, new work with the given name will be
     * enqueued.
     *
     * It preserves enqueue time, e.g. if a work was run 3 hours ago and had 8 hours long
     * period, after the update it would be still eligible for run in 5 hours, assuming
     * that periodicity wasn't updated.
     *
     * If the work being updated is currently running the current run won't
     * be interrupted and will continue to rely on previous state of the request, e.g. using
     * old constraints, tags etc. However, on the next iteration of periodic worker,
     * the new worker specification will be used.
     *
     * If the work was previously cancelled (via [WorkManager.cancelWorkById] or similar),
     * it will be deleted and then the newly-specified work will be enqueued.
     */
    UPDATE,

    /**
     * If there is existing pending (uncompleted) work with the same unique name, cancel and delete
     * it. Then, insert the newly-specified work.
     *
     * It is identical for `REPLACE`. But for readability reasons it is better to use
     * `CANCEL_AND_REENQUEUE`, because for a reader the difference between `REPLACE` vs `UPDATED`
     * is unclear.
     */
    CANCEL_AND_REENQUEUE,
}
