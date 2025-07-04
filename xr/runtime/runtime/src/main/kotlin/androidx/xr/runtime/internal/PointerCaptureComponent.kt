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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/** Component to enable pointer capture. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface PointerCaptureComponent : Component {
    /** The possible states of pointer capture. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public annotation class PointerCaptureState {
        public companion object {
            public const val POINTER_CAPTURE_STATE_PAUSED: Int = 0
            public const val POINTER_CAPTURE_STATE_ACTIVE: Int = 1
            public const val POINTER_CAPTURE_STATE_STOPPED: Int = 2
        }
    }

    /** Functional interface for receiving updates about the state of pointer capture. */
    public fun interface StateListener {
        /**
         * Called when the state of pointer capture changes.
         *
         * @param newState The new state of pointer capture.
         */
        public fun onStateChanged(@PointerCaptureState newState: Int)
    }
}
