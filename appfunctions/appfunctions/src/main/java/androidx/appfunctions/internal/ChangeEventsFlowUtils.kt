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

package androidx.appfunctions.internal

import androidx.appfunctions.ObserveAppFunctionsEvent
import androidx.appfunctions.metadata.AppFunctionName
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

/** Utility methods for working with [ObserveAppFunctionsEvent] [Flow]s. */
internal object ChangeEventsFlowUtils {

    /**
     * Debounces and merges [ObserveAppFunctionsEvent]s.
     *
     * It aggregates events that occur within the [debounceMillis] window and emits them as
     * consolidated [ObserveAppFunctionsEvent.MetadataChanged] and
     * [ObserveAppFunctionsEvent.StatesChanged] events.
     */
    @OptIn(FlowPreview::class)
    public fun Flow<ObserveAppFunctionsEvent>.debounceAndMerge(
        debounceMillis: kotlin.time.Duration
    ): Flow<ObserveAppFunctionsEvent> = flow {
        var pendingPackages = mutableSetOf<String>()
        var pendingFunctions = mutableSetOf<AppFunctionName>()
        val pendingEventsLock = Any()

        this@debounceAndMerge.onEach { event ->
                synchronized(pendingEventsLock) {
                    when (event) {
                        is ObserveAppFunctionsEvent.MetadataChanged -> {
                            pendingPackages.addAll(event.changedPackageNames)
                        }

                        is ObserveAppFunctionsEvent.StatesChanged -> {
                            pendingFunctions.addAll(event.changedFunctionNames)
                        }
                    }
                }
            }
            .debounce(debounceMillis)
            .collect {
                val packagesToEmit: Set<String>
                val functionsToEmit: Set<AppFunctionName>

                synchronized(pendingEventsLock) {
                    packagesToEmit = pendingPackages
                    pendingPackages = mutableSetOf()

                    functionsToEmit = pendingFunctions
                    pendingFunctions = mutableSetOf()
                }

                if (packagesToEmit.isNotEmpty()) {
                    emit(ObserveAppFunctionsEvent.MetadataChanged(packagesToEmit))
                }
                if (functionsToEmit.isNotEmpty()) {
                    emit(ObserveAppFunctionsEvent.StatesChanged(functionsToEmit))
                }
            }
    }
}
