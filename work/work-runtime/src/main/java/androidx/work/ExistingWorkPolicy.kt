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
 * [OneTimeWorkRequest]s in case of a collision.
 */
enum class ExistingWorkPolicy {
    /**
     * If there is existing pending (uncompleted) work with the same unique name, cancel and delete
     * it. Then, insert the newly-specified work.
     */
    REPLACE,

    /**
     * If there is existing pending (uncompleted) work with the same unique name, do nothing.
     * Otherwise, insert the newly-specified work.
     */
    KEEP,

    /**
     * If there is existing pending (uncompleted) work with the same unique name, append the
     * newly-specified work as a child of all the leaves of that work sequence.  Otherwise, insert
     * the newly-specified work as the start of a new sequence.
     *
     * **Note:** When using APPEND with failed or cancelled prerequisites, newly enqueued work
     * will also be marked as failed or cancelled respectively. Use
     * [ExistingWorkPolicy.APPEND_OR_REPLACE] to create a new chain of work.
     */
    APPEND,

    /**
     * If there is existing pending (uncompleted) work with the same unique name, append the
     * newly-specified work as the child of all the leaves of that work sequence. Otherwise, insert
     * the newly-specified work as the start of a new sequence.
     *
     * **Note:** If there are failed or cancelled prerequisites, these prerequisites are
     * *dropped* and the newly-specified work is the start of a new sequence.
     */
    APPEND_OR_REPLACE
}
