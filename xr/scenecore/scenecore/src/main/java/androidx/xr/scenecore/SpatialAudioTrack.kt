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

import android.media.AudioTrack
import androidx.xr.runtime.Session

/** Provides spatial audio extensions on the framework [AudioTrack] class. */
public object SpatialAudioTrack {

    /**
     * Gets the [SpatializerConstants.SourceType] of the provided [AudioTrack]. If
     * [setPointSourceParams] has not yet been called, this value is determined by how the
     * [SpatialAudioTrackBuilder] was constructed. Will return
     * [SpatializerConstants.SourceType.DEFAULT] for tracks that didn't use spatial audio
     * attributes.
     *
     * @param session The current [Session] instance.
     * @param track The [AudioTrack] from which to get the [SpatializerConstants.SourceType].
     * @return The [SpatializerConstants.SourceType] of the provided track.
     */
    @JvmStatic
    public fun getSpatialSourceType(
        session: Session,
        track: AudioTrack,
    ): SpatializerConstants.SourceType {
        return session.sceneRuntime.audioTrackExtensionsWrapper
            .getSpatialSourceType(track)
            .sourceTypeToJxr()
    }

    /**
     * Gets the [PointSourceParams] of the provided [AudioTrack].
     *
     * @param session The current [Session] instance.
     * @param track The [AudioTrack] from which to get the [PointSourceParams].
     * @return The [PointSourceParams] of the provided track, null if not set.
     */
    @JvmStatic
    public fun getPointSourceParams(session: Session, track: AudioTrack): PointSourceParams? {
        val rtAttributes =
            session.sceneRuntime.audioTrackExtensionsWrapper.getPointSourceParams(track)
        return rtAttributes?.toPointSourceParams(session)
    }

    /**
     * Gets the [SoundFieldAttributes] of the provided [AudioTrack].
     *
     * @param session The current [Session] instance.
     * @param track The [AudioTrack] from which to get the [SoundFieldAttributes].
     * @return The [SoundFieldAttributes] of the provided track, null if not set.
     */
    @JvmStatic
    public fun getSoundFieldAttributes(session: Session, track: AudioTrack): SoundFieldAttributes? {
        val rtAttributes =
            session.sceneRuntime.audioTrackExtensionsWrapper.getSoundFieldAttributes(track)
        return rtAttributes?.toSoundFieldAttributes()
    }

    /**
     * Sets a new [PointSourceParams] on the provided [AudioTrack].
     *
     * The new [PointSourceParams] will be applied if the [SpatializerConstants.SourceType] of the
     * AudioTrack was either [SpatializerConstants.SourceType.DEFAULT] or
     * [SpatializerConstants.SourceType.POINT_SOURCE]. If the [SpatializerConstants.SourceType] was
     * [SpatializerConstants.SourceType.POINT_SOURCE], then this method will throw an
     * [IllegalStateException].
     *
     * @param session The current [Session] instance.
     * @param track The [AudioTrack] on which to set the [PointSourceParams].
     * @param params The [PointSourceParams] to be set.
     * @param entity The [Entity] from which the sound will be played.
     * @throws IllegalStateException if the [SpatializerConstants.SourceType] of the [AudioTrack] is
     *   [SpatializerConstants.SourceType.SOUND_FIELD].
     * @throws IllegalArgumentException if the [PointSourceParams] cannot be set on this
     *   [AudioTrack] instance.
     */
    @JvmStatic
    public fun setPointSourceParams(
        session: Session,
        track: AudioTrack,
        params: PointSourceParams,
        entity: Entity,
    ) {
        session.sceneRuntime.audioTrackExtensionsWrapper.setPointSourceParams(
            track,
            params.rtPointSourceParams,
            (entity as BaseEntity<*>).rtEntity,
        )
    }
}

/** Provides spatial audio extensions on the platform [AudioTrack.Builder] class. */
@Suppress("MissingBuildMethod", "TopLevelBuilder")
public object SpatialAudioTrackBuilder {

    /**
     * Sets the [PointSourceParams] on the provided [AudioTrack.Builder].
     *
     * @param session The current [Session] instance.
     * @param builder The Builder on which to set the attributes.
     * @param params The source params to be set.
     * @param entity The [Entity] from which the sound will be played.
     */
    @Suppress("SetterReturnsThis")
    @JvmStatic
    public fun setPointSourceParams(
        session: Session,
        builder: AudioTrack.Builder,
        params: PointSourceParams,
        entity: Entity,
    ) {
        session.sceneRuntime.audioTrackExtensionsWrapper.setPointSourceParams(
            builder,
            params.rtPointSourceParams,
            (entity as BaseEntity<*>).rtEntity,
        )
    }

    /**
     * Sets the [SoundFieldAttributes] on the provided [AudioTrack.Builder].
     *
     * @param session The current [Session] instance.
     * @param builder The Builder on which to set the attributes.
     * @param attributes The source attributes to be set.
     */
    @Suppress("SetterReturnsThis")
    @JvmStatic
    public fun setSoundFieldAttributes(
        session: Session,
        builder: AudioTrack.Builder,
        attributes: SoundFieldAttributes,
    ) {
        session.sceneRuntime.audioTrackExtensionsWrapper.setSoundFieldAttributes(
            builder,
            attributes.rtSoundFieldAttributes,
        )
    }
}
