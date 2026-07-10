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
import androidx.xr.scenecore.PointerCaptureComponent
import androidx.xr.scenecore.testing.internal.FakePointerCaptureComponent as InternalFakePointerCaptureComponent

/**
 * A test-only accessor for [PointerCaptureComponent] that enables direct manipulation and
 * inspection of its internal state.
 */
public class PointerCaptureComponentTester
internal constructor(
    private val rtPointerCaptureComponent: InternalFakePointerCaptureComponent,
    internal val pointerCaptureComponent: PointerCaptureComponent,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [PointerCaptureComponent].
         *
         * This function provides a [PointerCaptureComponentTester] instance, which can be used to
         * inspect and manipulate its underlying data in the test environment.
         *
         * @param pointerCaptureComponent The component for which to retrieve the test data
         *   accessor.
         * @return A [PointerCaptureComponentTester] instance for the given component.
         */
        internal fun create(
            pointerCaptureComponent: PointerCaptureComponent
        ): PointerCaptureComponentTester {
            @Suppress("DEPRECATION")
            return PointerCaptureComponentTester(
                (pointerCaptureComponent.rtComponent as FakePointerCaptureComponent).fakeInternal,
                pointerCaptureComponent,
            )
        }
    }

    /**
     * Simulates a pointer capture state change event.
     *
     * This triggers the stateListener passed to [PointerCaptureComponent.create] on the
     * [java.util.concurrent.Executor] specified there.
     *
     * @param newState The new [PointerCaptureComponent.PointerCaptureState] of pointer capture.
     */
    public fun triggerOnStateChanged(newState: PointerCaptureComponent.PointerCaptureState) {
        rtPointerCaptureComponent.onStateChanged(newState.toRtPointerCaptureState())
    }

    /**
     * Simulates an input event from the runtime.
     *
     * This triggers the inputListener passed to [PointerCaptureComponent.create] on the
     * [java.util.concurrent.Executor] specified there.
     *
     * @param event The input event that occurred.
     */
    public fun triggerOnInputEvent(event: InputEvent) {
        rtPointerCaptureComponent.onInputEvent(event.toRtInputEvent())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointerCaptureComponentTester) return false

        if (rtPointerCaptureComponent != other.rtPointerCaptureComponent) return false
        if (pointerCaptureComponent != other.pointerCaptureComponent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtPointerCaptureComponent.hashCode()
        result = 31 * result + pointerCaptureComponent.hashCode()
        return result
    }
}
