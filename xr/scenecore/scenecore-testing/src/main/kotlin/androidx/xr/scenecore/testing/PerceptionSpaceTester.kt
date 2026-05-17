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

import androidx.xr.scenecore.HitTestResult
import androidx.xr.scenecore.PerceptionSpace
import androidx.xr.scenecore.runtime.HitTestResult as RtHitTestResult
import androidx.xr.scenecore.testing.internal.FakePerceptionSpaceScenePose
import androidx.xr.scenecore.toHitTestResult

/**
 * A test-only data accessor for perception space that enables direct manipulation and inspection of
 * its internal state.
 */
public class PerceptionSpaceTester
internal constructor(private val rtInstance: FakePerceptionSpaceScenePose) {

    /**
     * The [HitTestResult] to be returned by subsequent calls to [PerceptionSpace.hitTest].
     *
     * This property is typically used for testing or simulation purposes, allowing you to define
     * the outcome of hit tests performed within the [PerceptionSpace].
     *
     * Setting a non-null value describes the location and normal of the closest object hit,
     * relative to the origin of the hit test ray. Set to `null` to simulate the hit test not
     * intersecting with any objects.
     */
    public var hitTestResult: HitTestResult?
        get() = rtInstance.hitTestResult.toHitTestResult()
        set(value) {
            rtInstance.hitTestResult =
                value?.toRtHitTestResult()
                    ?: RtHitTestResult(
                        null,
                        null,
                        RtHitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN,
                        0f,
                    )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PerceptionSpaceTester

        return rtInstance == other.rtInstance
    }

    override fun hashCode(): Int {
        return rtInstance.hashCode()
    }
}
