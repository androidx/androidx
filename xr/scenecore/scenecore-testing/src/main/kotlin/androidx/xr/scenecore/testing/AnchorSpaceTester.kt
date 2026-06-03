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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.xr.scenecore.AnchorSpace
import androidx.xr.scenecore.runtime.AnchorEntity.State as RtState
import androidx.xr.scenecore.testing.internal.FakeAnchorEntity as InternalFakeAnchorEntity

/**
 * A test-only accessor for [AnchorSpace] that enables direct manipulation and inspection of its
 * internal state.
 */
public class AnchorSpaceTester
internal constructor(
    private val rtAnchorEntity: InternalFakeAnchorEntity,
    internal val anchorSpace: AnchorSpace,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [AnchorSpace].
         *
         * This function provides a [AnchorSpaceTester] instance, which can be used to inspect and
         * manipulate its underlying data in the test environment.
         *
         * @param anchorSpace The AnchorSpace for which to retrieve the test data accessor.
         * @return An [AnchorSpaceTester] instance for the given AnchorSpace.
         */
        internal fun create(anchorSpace: AnchorSpace): AnchorSpaceTester {
            return AnchorSpaceTester(
                (anchorSpace.rtEntity as FakeEntity).fakeInternal as InternalFakeAnchorEntity,
                anchorSpace,
            )
        }
    }

    /**
     * The state of the AnchorSpace.
     *
     * Setting this property simulates a system-level state change (e.g., from
     * [AnchorSpace.State.UNANCHORED] to [AnchorSpace.State.ANCHORED]) and triggers the listener set
     * by [AnchorSpace.addStateChangedListener].
     */
    public var state: AnchorSpace.State?
        get() = rtAnchorEntity.state.toState()
        set(value) {
            rtAnchorEntity.onStateChanged(value?.toRtState() ?: RtState.ERROR)
        }

    /**
     * Simulates a change to the underlying space's origin.
     *
     * This function manually triggers any listeners registered via
     * [AnchorSpace.addOriginChangedListener], allowing tests to verify that the application
     * correctly responds to spatial updates from the system.
     */
    public fun triggerOnOriginChanged() {
        rtAnchorEntity.onOriginChanged()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnchorSpaceTester

        if (rtAnchorEntity != other.rtAnchorEntity) return false
        if (anchorSpace != other.anchorSpace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtAnchorEntity.hashCode()
        result = 31 * result + anchorSpace.hashCode()

        return result
    }
}
