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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.PointerCaptureComponent
import androidx.xr.runtime.internal.PointerCaptureComponent.PointerCaptureState
import androidx.xr.runtime.internal.PointerCaptureComponent.StateListener
import java.util.concurrent.Executor

/** Test-only implementation of [FakePointerCaptureComponent] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakePointerCaptureComponent(
    /**
     * The executor on which to invoke the [StateListener] callbacks.
     *
     * If this is non-null, listener callbacks will be dispatched via this executor. If it is null,
     * callbacks will be invoked synchronously on the thread that calls [onStateChanged]. This can
     * be set in tests to simulate different threading behaviors.
     */
    internal val executor: Executor? = null,
    /**
     * The [StateListener] that receives callbacks upon a simulated pointer capture state change.
     *
     * Tests can provide a listener at construction to verify that state changes, triggered via the
     * [onStateChanged] function, are dispatched correctly. If this is null, calls to
     * [onStateChanged] will be ignored.
     */
    internal val stateListener: StateListener? = null,
) : FakeComponent(), PointerCaptureComponent {

    /**
     * Simulates a pointer capture state change event, invoking the registered [stateListener].
     *
     * This function is a test utility to manually trigger the state change callback. It respects
     * the provided [executor], dispatching the callback to it if non-null, or invoking it
     * synchronously otherwise.
     *
     * @param newState The new [PointerCaptureState] to propagate to the listener.
     */
    internal fun onStateChanged(@PointerCaptureState newState: Int) {
        if (stateListener != null) {
            executor?.let { currentExecutor ->
                currentExecutor.execute { stateListener.onStateChanged(newState) }
            } ?: run { stateListener!!.onStateChanged(newState) }
        }
    }
}
