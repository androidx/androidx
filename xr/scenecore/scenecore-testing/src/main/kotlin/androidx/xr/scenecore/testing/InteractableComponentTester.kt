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

package androidx.xr.scenecore.testing

import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.testing.internal.FakeInteractableComponent as InternalFakeInteractableComponent

/**
 * A test-only accessor for [InteractableComponent] that enables direct manipulation and inspection
 * of its internal state.
 */
public class InteractableComponentTester
internal constructor(
    private val rtComponent: InternalFakeInteractableComponent,
    internal val interactableComponent: InteractableComponent,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [InteractableComponent].
         *
         * This function provides a [InteractableComponentTester] instance, which can be used to
         * inspect and manipulate its underlying data in the test environment.
         *
         * @param interactableComponent The entity for which to retrieve the test data accessor.
         * @return A [InteractableComponentTester] instance for the given entity.
         */
        internal fun create(
            interactableComponent: InteractableComponent
        ): InteractableComponentTester {
            return InteractableComponentTester(
                @Suppress("DEPRECATION")
                (interactableComponent.rtInteractableComponent as FakeInteractableComponent)
                    .fakeInternal,
                interactableComponent,
            )
        }
    }

    /**
     * Simulates an input event from the runtime, notifying the listener registered via
     * [InteractableComponent.create].
     *
     * @param inputEvent The [InputEvent] to be triggered.
     */
    public fun triggerOnInputEvent(inputEvent: InputEvent) {
        rtComponent.onInputEvent(inputEvent.toRtInputEvent())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InteractableComponentTester

        if (rtComponent != other.rtComponent) return false
        if (interactableComponent != other.interactableComponent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtComponent.hashCode()
        result = 31 * result + interactableComponent.hashCode()
        return result
    }
}
