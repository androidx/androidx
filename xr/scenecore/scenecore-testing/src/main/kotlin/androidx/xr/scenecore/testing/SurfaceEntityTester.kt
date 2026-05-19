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

import android.view.Surface
import androidx.xr.runtime.FieldOfView
import androidx.xr.scenecore.PerceivedResolutionResult
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.testing.internal.FakeScenePose as InternalFakeScenePose
import androidx.xr.scenecore.testing.internal.FakeSurfaceEntity as InternalFakeSurfaceEntity
import androidx.xr.scenecore.toPerceivedResolutionResult

/**
 * A test-only accessor for [SurfaceEntity] that enables direct manipulation and inspection of its
 * internal state.
 */
public class SurfaceEntityTester
internal constructor(
    private val rtEntity: InternalFakeSurfaceEntity,
    internal val surfaceEntity: SurfaceEntity,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [SurfaceEntity].
         *
         * This function provides a [SurfaceEntityTester] instance, which can be used to inspect and
         * manipulate its underlying data in the test environment.
         *
         * @param surfaceEntity The entity for which to retrieve the test data accessor.
         * @return A [SurfaceEntityTester] instance for the given entity.
         */
        internal fun create(surfaceEntity: SurfaceEntity): SurfaceEntityTester {
            return SurfaceEntityTester(
                @Suppress("DEPRECATION") (surfaceEntity.rtEntity as FakeSurfaceEntity).fakeInternal
                    as InternalFakeSurfaceEntity,
                surfaceEntity,
            )
        }
    }

    /**
     * The [Surface] used by the [SurfaceEntity].
     *
     * Setting this property replaces the current [Surface] associated with the [SurfaceEntity]. In
     * this testing implementation, the [Surface] can be set at any time and can be retrieved by
     * calling [SurfaceEntity.getSurface]. This allows tests to provide a specific [Surface]
     * instance (such as one connected to a test-controlled producer) to verify rendering behavior.
     */
    public var surface: Surface
        get() = rtEntity.surface
        set(value) {
            rtEntity.setSurface(value)
        }

    /**
     * Configures the perceived resolution of the entity, to be retrieved by
     * [SurfaceEntity.getPerceivedResolution].
     *
     * Note: Unlike the real [SurfaceEntity.getPerceivedResolution] method, the value returned by
     * this getter is independent of any specific viewpoint or scene pose. It simply returns the
     * last value that was set via this property.
     */
    public var perceivedResolutionResult: PerceivedResolutionResult
        get() =
            rtEntity
                .getPerceivedResolution(
                    InternalFakeScenePose(),
                    FieldOfView(1.0f, 1.0f, 1.0f, 1.0f),
                )
                .toPerceivedResolutionResult()
        set(value) {
            rtEntity.setPerceivedResolution(value.toRtPerceivedResolutionResult())
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SurfaceEntityTester

        if (rtEntity != other.rtEntity) return false
        if (surfaceEntity != other.surfaceEntity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtEntity.hashCode()
        result = 31 * result + surfaceEntity.hashCode()
        return result
    }
}
