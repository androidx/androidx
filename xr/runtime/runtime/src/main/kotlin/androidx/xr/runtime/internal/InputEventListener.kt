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

/** Interface for an Input Listener. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun interface InputEventListener {
    /**
     * Called when an input event occurs.
     *
     * @param event The input event that occurred.
     */
    public fun onInputEvent(event: InputEvent)
}
