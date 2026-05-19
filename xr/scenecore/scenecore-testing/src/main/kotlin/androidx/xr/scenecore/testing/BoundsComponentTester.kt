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

import androidx.xr.runtime.math.BoundingBox
import androidx.xr.scenecore.BoundsComponent
import androidx.xr.scenecore.testing.internal.FakeBoundsComponent as InternalFakeBoundsComponent

/**
 * A test data accessor for a [BoundsComponent] to inspect and manipulate underlying fake data.
 *
 * In a test environment, each entity created via [BoundsComponent.create] is backed by
 * corresponding fake data in the fake runtime. This class provides a way to bridge the public
 * [BoundsComponent] API with its internal fake state.
 */
public class BoundsComponentTester
internal constructor(
    private val rtBoundsComponent: InternalFakeBoundsComponent,
    internal val boundsComponent: BoundsComponent,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [BoundsComponent].
         *
         * This function provides a [BoundsComponentTester] instance, which can be used to inspect
         * and manipulate its underlying data in the test environment.
         *
         * @param boundsComponent The component for which to retrieve the test data accessor.
         * @return A [BoundsComponentTester] instance for the given component.
         */
        internal fun create(boundsComponent: BoundsComponent): BoundsComponentTester {
            @Suppress("DEPRECATION")
            return BoundsComponentTester(
                (boundsComponent.rtBoundsComponent as FakeBoundsComponent).fakeInternal,
                boundsComponent,
            )
        }
    }

    /**
     * Simulates a bounds update event from the runtime, notifying all registered listeners.
     *
     * This triggers callbacks registered via [BoundsComponent.addBoundsUpdateListener].
     *
     * @param boundingBox The new [BoundingBox].
     */
    public fun triggerOnBoundsUpdate(boundingBox: BoundingBox) {
        rtBoundsComponent.onBoundsUpdate(boundingBox)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoundsComponentTester

        if (rtBoundsComponent != other.rtBoundsComponent) return false
        if (boundsComponent != other.boundsComponent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtBoundsComponent.hashCode()
        result = 31 * result + boundsComponent.hashCode()
        return result
    }
}
