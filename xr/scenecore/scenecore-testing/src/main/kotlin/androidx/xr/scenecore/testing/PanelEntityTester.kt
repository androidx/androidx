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

import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PerceivedResolutionResult
import androidx.xr.scenecore.testing.internal.FakePanelEntity as InternalFakePanelEntity
import androidx.xr.scenecore.toPerceivedResolutionResult

/**
 * A test-only accessor for [PanelEntity] that enables direct manipulation and inspection of its
 * internal state.
 */
public open class PanelEntityTester
internal constructor(internal val rtPanelEntity: InternalFakePanelEntity) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [PanelEntity].
         *
         * This function provides a [PanelEntityTester] instance, which can be used to inspect and
         * manipulate its underlying data in the test environment.
         *
         * @param panelEntity The entity for which to retrieve the test data accessor.
         * @return A [PanelEntityTester] instance for the given entity.
         */
        internal fun create(panelEntity: PanelEntity): PanelEntityTester {
            @Suppress("DEPRECATION")
            return PanelEntityTester(
                (panelEntity.rtEntity as FakeEntity).fakeInternal as InternalFakePanelEntity
            )
        }
    }

    /**
     * The perceived resolution result which can be retrieved by
     * [PanelEntity.getPerceivedResolution].
     *
     * Setting this property allows tests to simulate various outcomes for perceived resolution
     * queries, including success with specific dimensions or failure due to entity distance or
     * invalid viewpoints.
     */
    public var perceivedResolutionResult: PerceivedResolutionResult
        get() = rtPanelEntity.perceivedResolutionResult.toPerceivedResolutionResult()
        set(value) {
            rtPanelEntity.perceivedResolutionResult = value.toRtPerceivedResolutionResult()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PanelEntityTester

        return rtPanelEntity == other.rtPanelEntity
    }

    override fun hashCode(): Int {
        return rtPanelEntity.hashCode()
    }
}
