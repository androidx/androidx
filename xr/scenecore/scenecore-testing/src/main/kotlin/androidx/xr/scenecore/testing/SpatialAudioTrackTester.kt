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

import android.media.AudioTrack
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SpatialAudioTrack
import androidx.xr.scenecore.SpatializerConstants
import androidx.xr.scenecore.SpatializerConstants.SourceType
import androidx.xr.scenecore.testing.internal.FakeAudioTrackExtensionsWrapper as InternalFakeAudioTrackExtensionsWrapper

/**
 * Test utility class for spatial audio extensions.
 *
 * This class provides a mechanism for tests to inspect and verify spatial audio attributes
 * associated with an [AudioTrack] that are otherwise encapsulated within the SceneCore runtime.
 */
public class SpatialAudioTrackTester
internal constructor(private val rtInstance: InternalFakeAudioTrackExtensionsWrapper) {

    // TODO: b/515202507 - Consider removing the SpatialAudioTrackTester setters of
    // SpatialSourceType and SoundFieldAttributes after fake runtime implemented the implicit
    // relationship of SpatialAudioTrackBuilder.
    /**
     * Sets the [SourceType] of this audio track.
     *
     * This value needs to be set in the [SpatialAudioTrackTester] in order to test
     * [SpatialAudioTrack.getSpatialSourceType] because there is currently no way to internally
     * associate an [AudioTrack] with the [AudioTrack.Builder] used to create it.
     *
     * @param track The [AudioTrack] on which to set the [SpatializerConstants.SourceType].
     * @param sourceType The [SpatializerConstants.SourceType] of the provided track.
     */
    public fun setSpatialSourceType(track: AudioTrack, sourceType: SourceType) {
        rtInstance.spatialSourceTypeMap[track] = sourceType.toRtSourceType()
    }

    // TODO: b/515202507 - Consider removing the SpatialAudioTrackTester setters of
    // SpatialSourceType and SoundFieldAttributes after fake runtime implemented the implicit
    // relationship of SpatialAudioTrackBuilder.
    /**
     * Sets the [SoundFieldAttributes] associated with this audio track.
     *
     * This value needs to be set in the [SpatialAudioTrackTester] in order to test
     * [SpatialAudioTrack.getSoundFieldAttributes] because there is currently no way to internally
     * associate an [AudioTrack] with the [AudioTrack.Builder] used to create it.
     *
     * @param track The [AudioTrack] on which to set the [SoundFieldAttributes].
     * @param soundFieldAttributes The [SoundFieldAttributes] to be set.
     */
    public fun setSoundFieldAttributes(
        track: AudioTrack,
        soundFieldAttributes: SoundFieldAttributes,
    ) {
        rtInstance.soundFieldAttributesMap[track] = soundFieldAttributes.toRtSoundFieldAttributes()
    }

    /**
     * Checks whether the given [entity] is the current point source for this [AudioTrack].
     *
     * This returns `true` if the [entity] was passed to [SpatialAudioTrack.setPointSourceParams] as
     * a point source.
     *
     * @param track The [AudioTrack] from [SpatialAudioTrack.setPointSourceParams].
     * @param entity The target [Entity] to check against.
     * @return `true` if the [entity] is the current point source for this track, `false` otherwise.
     */
    public fun isCurrentPointSource(track: AudioTrack, entity: Entity): Boolean {
        @Suppress("DEPRECATION")
        return rtInstance.entityMap[track] == (entity.rtEntity as FakeEntity).fakeInternal
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpatialAudioTrackTester

        return rtInstance == other.rtInstance
    }

    override fun hashCode(): Int {
        return rtInstance.hashCode()
    }
}
