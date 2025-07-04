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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.AnchorEntity
import androidx.xr.runtime.internal.AnchorEntity.OnStateChangedListener

/** Test-only implementation of [AnchorEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeAnchorEntity : FakeSystemSpaceEntity(), AnchorEntity {
    private var onStateChangedListener: OnStateChangedListener =
        OnStateChangedListener { newState ->
            _state = newState
        }

    private var _state: @AnchorEntity.State Int = AnchorEntity.State.UNANCHORED

    /** The current state of the anchor. */
    override val state: @AnchorEntity.State Int
        get() = _state

    /** Registers a listener to be called when the state of the anchor changes. */
    @Suppress("ExecutorRegistration")
    override fun setOnStateChangedListener(onStateChangedListener: OnStateChangedListener) {
        this.onStateChangedListener = onStateChangedListener
    }

    /** Returns the native pointer of the anchor. */
    // TODO(b/373711152) : Remove this property once the Jetpack XR Runtime API migration is done.
    override val nativePointer: Long
        get() = 0L

    /**
     * Test function to invoke the onStateChanged listener callback.
     *
     * This function is used to simulate the update of the underlying [AnchorEntity.State],
     * triggering the registered listener. In tests, you can call this function to manually trigger
     * the listener and verify that your code responds correctly to state updates.
     */
    public fun onStateChanged(newState: @AnchorEntity.State Int) {
        onStateChangedListener.onStateChanged(newState)
    }
}
