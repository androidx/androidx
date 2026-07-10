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

import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.testing.internal.FakeResizableComponent as InternalFakeResizableComponent

/**
 * A test-only accessor for [ResizableComponent] that enables direct manipulation and inspection of
 * its internal state.
 */
public class ResizableComponentTester
internal constructor(
    private val rtResizableComponent: InternalFakeResizableComponent,
    internal val resizableComponent: ResizableComponent,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [ResizableComponent].
         *
         * This function provides a [ResizableComponentTester] instance, which can be used to
         * inspect and manipulate its underlying data in the test environment.
         *
         * @param resizableComponent The component for which to retrieve the test data accessor.
         * @return A [ResizableComponentTester] instance for the given component.
         */
        internal fun create(resizableComponent: ResizableComponent): ResizableComponentTester {
            @Suppress("DEPRECATION")
            return ResizableComponentTester(
                (resizableComponent.rtResizableComponent as FakeResizableComponent).fakeInternal,
                resizableComponent,
            )
        }
    }

    /**
     * Simulates a resize event from the runtime, notifying all registered listeners.
     *
     * This triggers callbacks registered via [ResizableComponent.addResizeEventListener].
     *
     * @param resizeEvent The [ResizeEvent] to be sent.
     */
    public fun triggerOnResizeEvent(resizeEvent: ResizeEvent) {
        rtResizableComponent.onResizeEvent(resizeEvent.toRtResizeEvent())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResizableComponentTester

        if (rtResizableComponent != other.rtResizableComponent) return false
        if (resizableComponent != other.resizableComponent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtResizableComponent.hashCode()
        result = 31 * result + resizableComponent.hashCode()
        return result
    }
}
