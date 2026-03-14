/*
 * Copyright 2025 The Android Open Source Project
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
// TODO: b/438801100 - Move this to a spatial-only library prior to the beta release.

package androidx.xr.scenecore

import android.media.SoundPool
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.xr.runtime.Session

/** Provides spatial audio extensions on the framework [SoundPool] class. */
public object SpatialSoundPool {

    /** Indicates the failure of a [play] method. */
    public const val PLAY_FAILED: Int = 0

    /**
     * Plays a spatialized sound effect emitted from the [Entity] in the [PointSourceParams].
     *
     * @param session The current [Session] instance.
     * @param soundPool The [SoundPool] to use to the play the sound.
     * @param soundID a soundId returned by the [SoundPool.load] function.
     * @param params [PointSourceParams] to configure the sound source.
     * @param entity The [Entity] from which the sound will be played.
     * @param volume value (range = 0.0 to 1.0)
     * @param priority stream priority (0 = lowest priority)
     * @param loop loop mode (0 = no loop, -1 = loop forever, N = loop N times)
     * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
     * @return non-zero streamID if successful, [PLAY_FAILED] if failed.
     */
    @JvmStatic
    @JvmOverloads
    public fun play(
        session: Session,
        soundPool: SoundPool,
        soundID: Int,
        params: PointSourceParams,
        entity: Entity,
        @FloatRange(from = 0.0, to = 1.0) volume: Float = 1f,
        @IntRange(from = 0) priority: Int = 0,
        @IntRange(from = -1) loop: Int = 0,
        @FloatRange(from = 0.5, to = 2.0) rate: Float = 1f,
    ): Int {
        return session.sceneRuntime.soundPoolExtensionsWrapper.play(
            soundPool,
            soundID,
            params.rtPointSourceParams,
            (entity as BaseEntity<*>).rtEntity,
            volume,
            priority,
            loop,
            rate,
        )
    }

    /**
     * Plays a spatialized sound effect as a sound field.
     *
     * @param session The current [Session] instance.
     * @param soundPool The [SoundPool] to use to the play the sound.
     * @param soundID a soundId returned by the [SoundPool.load] function.
     * @param attributes [SoundFieldAttributes] to configure the sound source.
     * @param volume value (range = 0.0 to 1.0)
     * @param priority stream priority (0 = lowest priority)
     * @param loop loop mode (0 = no loop, -1 = loop forever, N = loop N times)
     * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
     * @return non-zero streamID if successful, [PLAY_FAILED] if failed
     */
    @JvmStatic
    @JvmOverloads
    public fun play(
        session: Session,
        soundPool: SoundPool,
        soundID: Int,
        attributes: SoundFieldAttributes,
        @FloatRange(from = 0.0, to = 1.0) volume: Float = 1f,
        @IntRange(from = 0) priority: Int = 0,
        @IntRange(from = -1) loop: Int = 0,
        @FloatRange(from = 0.5, to = 2.0) rate: Float = 1f,
    ): Int {
        return session.sceneRuntime.soundPoolExtensionsWrapper.play(
            soundPool,
            soundID,
            attributes.rtSoundFieldAttributes,
            volume,
            priority,
            loop,
            rate,
        )
    }

    /**
     * @param session The current [Session] instance.
     * @param soundPool The [SoundPool] to use to get its SourceType.
     * @param streamId a streamID returned by the [SoundPool.play] or either [play] method in this
     *   object.
     * @return The [SpatializerConstants.SourceType] for the given streamID.
     */
    public fun getSpatialSourceType(
        session: Session,
        soundPool: SoundPool,
        streamId: Int,
    ): SpatializerConstants.SourceType {
        return session.sceneRuntime.soundPoolExtensionsWrapper
            .getSpatialSourceType(soundPool, streamId)
            .sourceTypeToJxr()
    }
}
