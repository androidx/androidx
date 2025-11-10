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
import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import androidx.compose.runtime.retain.RetainedValuesStore
import androidx.lifecycle.ViewModel
import kotlin.coroutines.cancellation.CancellationException

internal class LifecycleRetainedValuesStoreOwner : ViewModel() {

    private val scopes = mutableIntObjectMapOf<MutableObjectList<RetainedValuesStoreEntry>>()

    fun getOrCreateRetainedValuesStoreEntry(viewId: Int): RetainedValuesStoreEntry {
        val entries = scopes.getOrPut(viewId) { MutableObjectList(initialCapacity = 1) }
        val entry =
            entries.firstOrNull { !it.isInUse }
                ?: RetainedValuesStoreEntry().also { entries.add(it) }

        entry.isInUse = true
        return entry
    }

    override fun onCleared() {
        scopes.forEach { _, value -> value.forEach { it.onCleared() } }
    }

    class RetainedValuesStoreEntry {
        private val _retainedValuesStore = LifecycleRetainedValuesStore()
        val retainedValuesStore: RetainedValuesStore = _retainedValuesStore

        var isInUse = false

        private var endRetainCancellationHandle: CancellationHandle? = null
            set(value) {
                field?.cancel()
                field = value
            }

        fun startRetainingExitedValues() {
            if (!_retainedValuesStore.isRetainingExitedValues) {
                _retainedValuesStore.startLifecycleTransition()
            } else {
                endRetainCancellationHandle = null
            }
        }

        fun stopRetainingExitedValues(frameEndScheduler: FrameEndScheduler) {
            if (_retainedValuesStore.isRetainingExitedValues) {
                endRetainCancellationHandle =
                    try {
                        frameEndScheduler.scheduleFrameEndCallback {
                            _retainedValuesStore.endLifecycleTransition()
                        }
                    } catch (_: CancellationException) {
                        // The Recomposer is shutting down, and we can't schedule work for the next
                        // frame. Stop retaining exited values now. This should only happen during
                        // tests where the Recomposer is explicitly cancelled by the testing
                        // framework before this callback can be dispatched.
                        _retainedValuesStore.endLifecycleTransition()
                        null
                    }
            }
        }

        fun onCleared() {
            endRetainCancellationHandle = null
            _retainedValuesStore.dispose()
        }

        fun release() {
            isInUse = false
        }
    }

    fun interface FrameEndScheduler {
        fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle
    }
}

// Lifecycle retention state can't be directly driven by the content's composition state because
// it doesn't cleanly follow the window attachment state that owns the store. Instead, we puppet a
// `ManagedRetainedValuesStore` to match our lifecycle state. Completing "onResume" is the semantic
// equivalent to an `onContentEntered` callback, and "onStop" an `onContentExit` callback.
//
// The delegation and repurposing behavior lets us use the runtime's stashing implementation for
// the out-of-the-box RetainedStore.
// TODO(anbailey): Consider exposing an AbstractRetainedValuesStore that only defines the retained
//  value stashing logic of a RetainedValuesStore and no retention lifespan policy.
internal class LifecycleRetainedValuesStore(
    val delegate: ManagedRetainedValuesStore = ManagedRetainedValuesStore()
) : RetainedValuesStore by delegate {
    val isRetainingExitedValues: Boolean
        get() = delegate.isRetainingExitedValues

    init {
        // Initialize in the "resumed" (not retaining) state
        delegate.onContentEnteredComposition()
    }

    fun startLifecycleTransition() {
        delegate.onContentExitComposition()
    }

    fun endLifecycleTransition() {
        delegate.onContentEnteredComposition()
    }

    fun dispose() {
        delegate.dispose()
    }

    // Don't subscribe to the composition content lifecycle.
    override fun onContentEnteredComposition() {}

    override fun onContentExitComposition() {}
}
