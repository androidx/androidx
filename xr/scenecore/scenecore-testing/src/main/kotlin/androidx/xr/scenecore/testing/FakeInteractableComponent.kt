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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.InputEvent
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.InteractableComponent
import java.util.concurrent.Executor

/** Test-only implementation of [androidx.xr.scenecore.runtime.InteractableComponent] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeInteractableComponent() : FakeComponent(), InteractableComponent {
    internal val inputEventListenersMap: MutableMap<InputEventListener, Executor> = mutableMapOf()

    /**
     * Simulates an input event from the runtime, notifying all registered listeners.
     *
     * This function is intended for testing purposes to allow manual triggering of the update
     * mechanism. It iterates through all currently registered listeners and invokes their
     * `onInputEvent` method on their respective [Executor]s.
     *
     * @param event The new [InputEvent] to be sent in the simulated event.
     */
    public fun onInputEvent(event: InputEvent) {
        for (entry in inputEventListenersMap.entries) {
            val executor = entry.value
            val listener = entry.key
            executor.execute { listener.onInputEvent(event) }
        }
    }
}
