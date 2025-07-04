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

package androidx.xr.runtime.internal

import android.media.SoundPool
import androidx.annotation.RestrictTo

/** Interface for a XR Runtime SoundPoolExtensionsWrapper. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SoundPoolExtensionsWrapper {
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
     * @return The result of the play operation.
     */
    public fun play(
        soundPool: SoundPool,
        soundId: Int,
        params: PointSourceParams,
        volume: Float,
        priority: Int,
        loop: Int,
        rate: Float,
    ): Int

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
     * @return The result of the play operation.
     */
    public fun play(
        soundPool: SoundPool,
        soundId: Int,
        attributes: SoundFieldAttributes,
        volume: Float,
        priority: Int,
        loop: Int,
        rate: Float,
    ): Int

    /**
     * Returns the spatial source type of the sound.
     *
     * @param soundPool The SoundPool to use.
     * @param streamId The stream ID of the sound.
     * @return The spatial source type of the sound.
     */
    @SpatializerConstants.SourceType
    public fun getSpatialSourceType(soundPool: SoundPool, streamId: Int): Int
}
