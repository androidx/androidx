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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import android.media.SoundPool
import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import androidx.xr.scenecore.runtime.SpatializerConstants
import androidx.xr.scenecore.testing.internal.FakeEntity as InternalFakeEntity
import androidx.xr.scenecore.testing.internal.FakeSoundPoolExtensionsWrapper as InternalFakeSoundPoolExtensionsWrapper
import java.util.Collections
import java.util.WeakHashMap

/** Test-only implementation of [androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeSoundPoolExtensionsWrapper
internal constructor(internal var fakeInternal: InternalFakeSoundPoolExtensionsWrapper) :
    SoundPoolExtensionsWrapper {
    public constructor() : this(InternalFakeSoundPoolExtensionsWrapper())

    /** Mapping between [InternalFakeEntity] and [FakeEntity] */
    internal val entityMap = Collections.synchronizedMap(WeakHashMap<Entity, Entity>())
    public val lastPlayedSoundPool: SoundPool?
        get() = fakeInternal.lastPlayedSoundPool

    public val lastPlayedSoundId: Int?
        get() = fakeInternal.lastPlayedSoundId

    public val lastPlayedParams: PointSourceParams?
        get() = fakeInternal.lastPlayedParams

    public val lastPlayedEntity: Entity?
        get() = fakeInternal.lastPlayedEntity?.let { entityMap[it] }

    public val lastPlayedVolume: Float?
        get() = fakeInternal.lastPlayedVolume

    public val lastPlayedPriority: Int?
        get() = fakeInternal.lastPlayedPriority

    public val lastPlayedLoop: Int?
        get() = fakeInternal.lastPlayedLoop

    public val lastPlayedRate: Float?
        get() = fakeInternal.lastPlayedRate

    public val lastPlayedSoundFieldAttributes: SoundFieldAttributes?
        get() = fakeInternal.lastPlayedSoundFieldAttributes

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
        fakeInternal.setPlayAsPointSourceResult(result)
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
        entity: Entity?,
        volume: Float,
        priority: Int,
        loop: Int,
        rate: Float,
    ): Int {
        val internalFakeEntity = (entity as? FakeEntity)?.fakeInternal as? InternalFakeEntity
        internalFakeEntity?.let { entityMap[it] = entity }

        return fakeInternal.play(
            soundPool,
            soundId,
            params,
            internalFakeEntity,
            volume,
            priority,
            loop,
            rate,
        )
    }

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
        fakeInternal.setPlayAsSoundFieldResult(result)
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
        return fakeInternal.play(soundPool, soundId, attributes, volume, priority, loop, rate)
    }

    /**
     * For test purposes only.
     *
     * Sets the result of a call to
     * [androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper.getSpatialSourceType] like the
     * setSourceType does in scenecore unit tests.
     */
    @SpatializerConstants.SourceType
    public var sourceType: Int
        get() = fakeInternal.sourceType
        set(value) {
            fakeInternal.sourceType = value
        }

    /**
     * Returns the spatial source type of the sound.
     *
     * @param soundPool The SoundPool to use.
     * @param streamId The stream ID of the sound.
     * @return The spatial source type of the sound.
     */
    @SpatializerConstants.SourceType
    override fun getSpatialSourceType(soundPool: SoundPool, streamId: Int): Int {
        return fakeInternal.getSpatialSourceType(soundPool, streamId)
    }
}
