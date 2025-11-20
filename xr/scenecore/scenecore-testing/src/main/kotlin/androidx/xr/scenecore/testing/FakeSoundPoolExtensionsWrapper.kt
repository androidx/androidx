/*
 * Copyright 2024 The Android Open Source Project
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

import android.media.SoundPool
import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import androidx.xr.scenecore.runtime.SpatializerConstants

/** Test-only implementation of [androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSoundPoolExtensionsWrapper : SoundPoolExtensionsWrapper {

    private var playAsPointSourceResult: Int = 0

    /**
     * For test purposes only. Sets the value that will be returned by the [play] method for point
     * source audio.
     *
     * This allows tests to simulate both successful and failed attempts to play a sound.
     *
     * @param result The stream ID to return from the `play` call. A non-zero value simulates a
     *   successful playback, while `0` simulates a failure (e.g., because no more streams are
     *   available).
     */
    public fun setPlayAsPointSourceResult(result: Int) {
        playAsPointSourceResult = result
    }

    /**
     * Plays a sound as a point source.
     *
     * @param soundPool The SoundPool to use.
     * @param soundId The ID of the sound to play.
     * @param params The PointSourceParams to use.
     * @param volume The volume of the sound.
     * @param priority The priority of the sound.
     * @param loop Whether to loop the sound.
     * @param rate The playback rate of the sound.
     * @return Non-zero streamID if successful, zero if failed.
     */
    override fun play(
        soundPool: SoundPool,
        soundId: Int,
        params: PointSourceParams,
        volume: Float,
        priority: Int,
        loop: Int,
        rate: Float,
    ): Int {
        return playAsPointSourceResult
    }

    private var playAsSoundFieldResult: Int = 0

    /**
     * For test purposes only. Sets the value that will be returned by the [play] method for sound
     * field audio.
     *
     * This allows tests to simulate both successful and failed attempts to play a sound.
     *
     * @param result The stream ID to return from the `play` call. A non-zero value simulates a
     *   successful playback, while `0` simulates a failure (e.g., because no more streams are
     *   available).
     */
    public fun setPlayAsSoundFieldResult(result: Int) {
        playAsSoundFieldResult = result
    }

    /**
     * Plays a sound as a sound field.
     *
     * @param soundPool The SoundPool to use.
     * @param soundId The ID of the sound to play.
     * @param attributes The SoundFieldAttributes to use.
     * @param volume The volume of the sound.
     * @param priority The priority of the sound.
     * @param loop Whether to loop the sound.
     * @param rate The playback rate of the sound.
     * @return Non-zero streamID if successful, zero if failed.
     */
    override fun play(
        soundPool: SoundPool,
        soundId: Int,
        attributes: SoundFieldAttributes,
        volume: Float,
        priority: Int,
        loop: Int,
        rate: Float,
    ): Int {
        return playAsSoundFieldResult
    }

    /**
     * For test purposes only.
     *
     * Sets the result of a call to
     * [androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper.getSpatialSourceType] like the
     * setSourceType does in scenecore unit tests.
     */
    @SpatializerConstants.SourceType public var sourceType: Int = 0

    /**
     * Returns the spatial source type of the sound.
     *
     * @param soundPool The SoundPool to use.
     * @param streamId The stream ID of the sound.
     * @return The spatial source type of the sound.
     */
    @SpatializerConstants.SourceType
    override fun getSpatialSourceType(soundPool: SoundPool, streamId: Int): Int {
        return sourceType
    }
}
