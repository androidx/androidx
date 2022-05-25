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
package androidx.work.impl

// Impl note: it is **not** a data class on purpose.
// Multiple schedulers can create `WorkRunId`-s for the same workSpecId, and `WorkRunId`
// objects should be different. If two schedulers request to start a work with the same
// `workSpecId`, Processor dedups those requests. However, if the work is cancelled, both
// of the schedules will request `Processor` to stop the work as well. That can lead to tricky race
// when one scheduler quickly cancels the work and schedules it again, while another stalls
// and tries to cancel the work afterwards. In this situation the freshly scheduled work shouldn't
// be cancelled because the second scheduler tries to cancel already cancelled work.
// So Processor class relies on WorkRunId-s being different and stores WorkRunId with the same
// workSpecId in the set to differentiate between past and future run requests.
class WorkRunId(val workSpecId: String)

class WorkRunIds {
    private val lock = Any()
    private val runs = mutableMapOf<String, WorkRunId>()

    fun workRunIdFor(workSpecId: String): WorkRunId {
        return synchronized(lock) {
            val workRunId = WorkRunId(workSpecId)
            runs.getOrPut(workSpecId) { workRunId }
        }
    }

    fun remove(workSpecId: String): WorkRunId? {
        return synchronized(lock) {
            runs.remove(workSpecId)
        }
    }
}