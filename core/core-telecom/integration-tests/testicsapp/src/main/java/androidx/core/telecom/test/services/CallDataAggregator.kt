/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.test.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Watches for [CallData] flow changes for each active call.
 *
 * Each call should call [watch] so this aggregator can track and listen to changes in its
 * [CallData]. When there is a change in the [CallData] state for any call, regenerate all of the
 * data in [callDataState]
 */
class CallDataAggregator {
    private val mCallDataProducers: MutableStateFlow<List<StateFlow<CallData>>> =
        MutableStateFlow(emptyList())
    private val mCallDataState: MutableStateFlow<List<CallData>> = MutableStateFlow(emptyList())
    /** Contains the current state of all active calls */
    val callDataState: StateFlow<List<CallData>> = mCallDataState.asStateFlow()

    /**
     * Watch the [CallData] flow for changes related to a new call until the [scope] completes when
     * the call ends.
     */
    suspend fun watch(scope: CoroutineScope, dataFlow: Flow<CallData>) {
        val dataStateFlow = dataFlow.stateIn(scope)
        mCallDataProducers.update { oldList -> ArrayList(oldList).apply { add(dataStateFlow) } }
        dataStateFlow
            .onEach { onCallDataUpdated() }
            .onCompletion {
                mCallDataProducers.update { oldList ->
                    ArrayList(oldList).apply { remove(dataStateFlow) }
                }
                onCallDataUpdated()
            }
            .launchIn(scope)
    }

    private fun onCallDataUpdated() {
        mCallDataState.value = mCallDataProducers.value.map { it.value }
    }
}
