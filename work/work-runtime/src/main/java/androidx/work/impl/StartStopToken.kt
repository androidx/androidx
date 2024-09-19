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

import androidx.work.impl.model.WorkGenerationalId
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.model.generationalId

// Impl note: it is **not** a data class on purpose.
// Multiple schedulers can create `StartStopToken`-s for the same workSpecId, and `StartStopToken`
// objects should be different. If two schedulers request to start a work with the same
// `workSpecId`, Processor dedups those requests. However, if the work is cancelled, both
// of the schedules will request `Processor` to stop the work as well. That can lead to tricky race
// when one scheduler quickly cancels the work and schedules it again, while another stalls
// and tries to cancel the work afterwards. In this situation the freshly scheduled work shouldn't
// be cancelled because the second scheduler tries to cancel already cancelled work.
// So Processor class relies on StartStopToken-s being different and stores StartStopToken
// with the same workSpecId in the set to differentiate between past and future run requests.
class StartStopToken(val id: WorkGenerationalId)

interface StartStopTokens {
    fun tokenFor(id: WorkGenerationalId): StartStopToken

    fun remove(id: WorkGenerationalId): StartStopToken?

    fun remove(workSpecId: String): List<StartStopToken>

    fun contains(id: WorkGenerationalId): Boolean

    fun tokenFor(spec: WorkSpec) = tokenFor(spec.generationalId())

    fun remove(spec: WorkSpec) = remove(spec.generationalId())

    companion object {
        @JvmStatic
        @JvmOverloads
        fun create(synchronized: Boolean = true): StartStopTokens {
            val tokens = StartStopTokensImpl()
            return if (synchronized) {
                SynchronizedStartStopTokensImpl(tokens)
            } else {
                tokens
            }
        }
    }
}

private class StartStopTokensImpl : StartStopTokens {
    private val runs = mutableMapOf<WorkGenerationalId, StartStopToken>()

    override fun tokenFor(id: WorkGenerationalId): StartStopToken {
        return runs.getOrPut(id) { StartStopToken(id) }
    }

    override fun remove(id: WorkGenerationalId): StartStopToken? {
        return runs.remove(id)
    }

    override fun remove(workSpecId: String): List<StartStopToken> {
        val toRemove = runs.filterKeys { it.workSpecId == workSpecId }
        toRemove.keys.forEach { runs.remove(it) }
        return toRemove.values.toList()
    }

    override fun contains(id: WorkGenerationalId): Boolean {
        return runs.contains(id)
    }
}

private class SynchronizedStartStopTokensImpl(private val delegate: StartStopTokens) :
    StartStopTokens {

    private val lock = Any()

    override fun tokenFor(id: WorkGenerationalId): StartStopToken {
        return synchronized(lock) { delegate.tokenFor(id) }
    }

    override fun remove(id: WorkGenerationalId): StartStopToken? {
        return synchronized(lock) { delegate.remove(id) }
    }

    override fun remove(workSpecId: String): List<StartStopToken> {
        return synchronized(lock) { delegate.remove(workSpecId) }
    }

    override fun contains(id: WorkGenerationalId): Boolean {
        return synchronized(lock) { delegate.contains(id) }
    }
}
