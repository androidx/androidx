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

import androidx.xr.scenecore.SpatialSoundPool
import androidx.xr.scenecore.SpatializerConstants.SourceType
import androidx.xr.scenecore.testing.internal.FakeSceneRuntime as InternalFakeSceneRuntime
import androidx.xr.scenecore.testing.internal.FakeSoundPoolExtensionsWrapper as InternalFakeSoundPoolExtensionsWrapper

/**
 * A test utility for accessing and inspecting the spatial data associated with the
 * [SpatialSoundPool].
 */
public class SpatialSoundPoolTester
internal constructor(private val runtime: InternalFakeSceneRuntime) {

    private val fakeSoundPoolExtensions: InternalFakeSoundPoolExtensionsWrapper
        get() = (runtime.soundPoolExtensionsWrapper as InternalFakeSoundPoolExtensionsWrapper)

    /**
     * The value that will be returned by the [SpatialSoundPool.play] method for point source audio.
     *
     * Setting this property allows tests to simulate both successful and failed attempts to play a
     * sound. A non-zero value simulates a successful playback, while `0` simulates a failure (e.g.,
     * because no more streams are available).
     */
    public var playAsPointSourceResult: Int
        get() = fakeSoundPoolExtensions.playAsPointSourceResult
        set(value) {
            fakeSoundPoolExtensions.playAsPointSourceResult = value
        }

    /**
     * The value that will be returned by the [SpatialSoundPool.play] method for sound field audio.
     *
     * Setting this property allows tests to simulate both successful and failed attempts to play a
     * sound. A non-zero value simulates a successful playback, while `0` simulates a failure (e.g.,
     * because no more streams are available).
     */
    public var playAsSoundFieldResult: Int
        get() = fakeSoundPoolExtensions.playAsSoundFieldResult
        set(value) {
            fakeSoundPoolExtensions.playAsSoundFieldResult = value
        }

    /**
     * The spatial source type which can be retrieved from [SpatialSoundPool.getSpatialSourceType]
     * of the sound pool.
     */
    public var spatialSourceType: SourceType
        get() = fakeSoundPoolExtensions.sourceType.toSourceType()
        set(value) {
            fakeSoundPoolExtensions.sourceType = value.toRtSourceType()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpatialSoundPoolTester

        if (runtime != other.runtime) return false

        return true
    }

    override fun hashCode(): Int {
        return runtime.hashCode()
    }
}
