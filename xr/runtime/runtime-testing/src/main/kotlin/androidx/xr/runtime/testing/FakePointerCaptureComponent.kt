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

/** Test-only implementation of [FakePointerCaptureComponent] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakePointerCaptureComponent() : FakeComponent(), PointerCaptureComponent {
    /**
     * For internal test only.
     *
     * The [PointerCaptureState] of pointer capture can be changed by [StateListener].
     */
    internal var pointerCaptureState: Int = PointerCaptureState.POINTER_CAPTURE_STATE_PAUSED

    /**
     * For internal test only.
     *
     * The fake version of [StateListener] that demonstrates how to update [pointerCaptureState].
     */
    internal val stateListener: StateListener =
        object : StateListener {
            override fun onStateChanged(@PointerCaptureState newState: Int) {
                pointerCaptureState = newState
            }
        }
}
