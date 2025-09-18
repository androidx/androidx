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

package androidx.compose.ui.platform

import androidx.collection.MutableObjectList
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.runtime.CancellationHandle
import androidx.compose.runtime.ControlledRetainScope
import androidx.compose.runtime.RetainScope
import androidx.lifecycle.ViewModel
import kotlin.coroutines.cancellation.CancellationException

internal class LifecycleRetainScopeOwner : ViewModel() {

    private val scopes = mutableIntObjectMapOf<MutableObjectList<RetainScopeEntry>>()

    fun getOrCreateRetainScopeEntry(viewId: Int): RetainScopeEntry {
        val entries = scopes.getOrPut(viewId) { MutableObjectList(initialCapacity = 1) }
        val entry =
            entries.firstOrNull { !it.isInUse } ?: RetainScopeEntry().also { entries.add(it) }

        entry.isInUse = true
        return entry
    }

    override fun onCleared() {
        scopes.forEach { _, value -> value.forEach { it.onCleared() } }
    }

    class RetainScopeEntry {
        private val controlledRetainScope = ControlledRetainScope()
        val retainScope: RetainScope = controlledRetainScope

        var isInUse = false

        private var endRetainCancellationHandle: CancellationHandle? = null
            set(value) {
                field?.cancel()
                field = value
            }

        fun startKeepingExitedValues() {
            if (!controlledRetainScope.isKeepingExitedValues) {
                controlledRetainScope.startKeepingExitedValues()
            }
        }

        fun stopKeepingExitedValues(frameEndScheduler: FrameEndScheduler) {
            if (controlledRetainScope.isKeepingExitedValues) {
                endRetainCancellationHandle =
                    try {
                        frameEndScheduler.scheduleFrameEndCallback {
                            controlledRetainScope.stopKeepingExitedValues()
                        }
                    } catch (_: CancellationException) {
                        // The Recomposer is shutting down, and we can't schedule work for the next
                        // frame. Stop keeping exited values now. This should only happen during
                        // tests where the Recomposer is explicitly cancelled by the testing
                        // framework before this callback can be dispatched.
                        controlledRetainScope.stopKeepingExitedValues()
                        null
                    }
            }
        }

        fun onCleared() {
            if (controlledRetainScope.isKeepingExitedValues) {
                controlledRetainScope.stopKeepingExitedValues()
            }
        }

        fun release() {
            isInUse = false
        }
    }

    fun interface FrameEndScheduler {
        fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle
    }
}
