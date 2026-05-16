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

import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.runtime.AnchorEntity.State as RtState
import androidx.xr.scenecore.testing.internal.FakeAnchorEntity as InternalFakeAnchorEntity

/**
 * A test-only accessor for [AnchorEntity] that enables direct manipulation and inspection of its
 * internal state.
 */
public class AnchorEntityTester
internal constructor(
    private val rtAnchorEntity: InternalFakeAnchorEntity,
    internal val anchorEntity: AnchorEntity,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [AnchorEntity].
         *
         * This function provides a [AnchorEntityTester] instance, which can be used to inspect and
         * manipulate its underlying data in the test environment.
         *
         * @param anchorEntity The entity for which to retrieve the test data accessor.
         * @return A [AnchorEntityTester] instance for the given entity.
         */
        internal fun create(anchorEntity: AnchorEntity): AnchorEntityTester {
            return AnchorEntityTester(
                (anchorEntity.rtEntity as FakeEntity).fakeInternal as InternalFakeAnchorEntity,
                anchorEntity,
            )
        }
    }

    /**
     * The state of the AnchorEntity.
     *
     * Setting this property simulates a system-level state change (e.g., from
     * [AnchorEntity.State.UNANCHORED] to [AnchorEntity.State.ANCHORED]) and triggers the listener
     * set by [AnchorEntity.addStateChangedListener].
     */
    public var state: AnchorEntity.State?
        get() = rtAnchorEntity.state.toState()
        set(value) {
            rtAnchorEntity.onStateChanged(value?.toRtState() ?: RtState.ERROR)
        }

    /**
     * Simulates a change to the underlying space's origin.
     *
     * This function manually triggers any listeners registered via
     * [AnchorEntity.addOriginChangedListener], allowing tests to verify that the application
     * correctly responds to spatial updates from the system.
     */
    public fun triggerOnOriginChanged() {
        rtAnchorEntity.onOriginChanged()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnchorEntityTester

        if (rtAnchorEntity != other.rtAnchorEntity) return false
        if (anchorEntity != other.anchorEntity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtAnchorEntity.hashCode()
        result = 31 * result + anchorEntity.hashCode()

        return result
    }
}
