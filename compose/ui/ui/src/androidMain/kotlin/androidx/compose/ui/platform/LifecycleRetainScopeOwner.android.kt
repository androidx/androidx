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

import androidx.compose.runtime.CancellationHandle
import androidx.compose.runtime.ControlledRetainScope
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.RetainScope
import androidx.compose.ui.internal.requirePrecondition
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel

internal class LifecycleRetainScopeOwner : ViewModel(), DefaultLifecycleObserver {

    private val controlledRetainScope = ControlledRetainScope()
    val retainScope: RetainScope
        get() = controlledRetainScope

    private var installedIn: LifecycleOwner? = null
    private var recomposer: Recomposer? = null

    private var endRetainCancellationHandle: CancellationHandle? = null
        set(value) {
            field?.cancel()
            field = value
        }

    fun installIn(lifecycleOwner: LifecycleOwner, recomposer: Recomposer) {
        if (installedIn === lifecycleOwner) return
        requirePrecondition(installedIn == null) {
            "Attempted to install a RetainScope into a different lifecycle before " +
                "the previously used context was destroyed."
        }

        installedIn = lifecycleOwner
        this.recomposer = recomposer
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (controlledRetainScope.isKeepingExitedValues) {
            val recomposer =
                checkNotNull(recomposer) {
                    "Received onResume() while not attached to a Recomposer."
                }

            if (recomposer.currentState.value <= Recomposer.State.ShuttingDown) {
                // The Recomposer is shutting down, and we can't schedule work for the next frame.
                // Stop keeping exited values now. This should only happen during tests where the
                // Recomposer is explicitly cancelled by the testing framework before this lifecycle
                // callback is dispatched by the testing framework.
                controlledRetainScope.stopKeepingExitedValues()
            } else {
                endRetainCancellationHandle =
                    recomposer.scheduleFrameEndCallback {
                        controlledRetainScope.stopKeepingExitedValues()
                    }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        checkNotNull(installedIn) { "Received onStop() while not installed in a lifecycle." }
        controlledRetainScope.startKeepingExitedValues()
        endRetainCancellationHandle = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
        endRetainCancellationHandle = null
        installedIn = null
        recomposer = null
    }

    override fun onCleared() {
        if (controlledRetainScope.isKeepingExitedValues)
            controlledRetainScope.stopKeepingExitedValues()
    }
}
