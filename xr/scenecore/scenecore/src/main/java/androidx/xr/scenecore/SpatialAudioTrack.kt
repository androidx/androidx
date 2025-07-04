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

package androidx.xr.scenecore

import android.media.AudioTrack
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session

@Suppress("ClassShouldBeObject")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SpatialAudioTrack {

    public companion object {
        /**
         * Gets the [SourceType] of the provided [AudioTrack]. If [setPointSourceParams] has not yet
         * been called, this value is implicitly set by which type of attributes was used to
         * configure the builder. Will return [SpatializerConstants.DEFAULT] for tracks that didn't
         * use spatial audio attributes.
         *
         * If [setPointSourceParams] is called and the [SourceType] was either
         * [SpatializerConstants.DEFAULT] or [SpatializerConstants.POINT_SOURCE], then the return
         * value will be [SpatializerConstants.POINT_SOURCE]. If the [SourceType] was
         * [SpatializerConstants.SOUND_FIELD] then the return value will remain
         * [SpatializerConstants.SOUND_FIELD].
         *
         * @param session The current SceneCore [Session] instance.
         * @param track The [AudioTrack] from which to get the [SpatializerConstants.SourceType].
         * @return The [SpatializerConstants.SourceType] of the provided track.
         */
        @JvmStatic
        @SpatializerConstants.SourceType
        public fun getSpatialSourceType(session: Session, track: AudioTrack): Int {
            return session.platformAdapter.audioTrackExtensionsWrapper
                .getSpatialSourceType(track)
                .sourceTypeToJxr()
        }

        /**
         * Gets the [PointSourceParams] of the provided [AudioTrack].
         *
         * @param session The current SceneCore [Session] instance.
         * @param track The [AudioTrack] from which to get the [PointSourceParams].
         * @return The [PointSourceParams] of the provided track, null if not set.
         */
        @JvmStatic
        public fun getPointSourceParams(session: Session, track: AudioTrack): PointSourceParams? {
            val rtAttributes =
                session.platformAdapter.audioTrackExtensionsWrapper.getPointSourceParams(track)
            return rtAttributes?.toPointSourceParams(session)
        }

        /**
         * Gets the [SoundFieldAttributes] of the provided [AudioTrack].
         *
         * @param session The current SceneCore [Session] instance.
         * @param track The [AudioTrack] from which to get the [SoundFieldAttributes].
         * @return The [SoundFieldAttributes] of the provided track, null if not set.
         */
        @JvmStatic
        public fun getSoundFieldAttributes(
            session: Session,
            track: AudioTrack,
        ): SoundFieldAttributes? {
            val rtAttributes =
                session.platformAdapter.audioTrackExtensionsWrapper.getSoundFieldAttributes(track)
            return rtAttributes?.toSoundFieldAttributes()
        }

        /**
         * Sets a new [PointSourceParams] on the provided [AudioTrack].
         *
         * The new [PointSourceParams] will be applied if the [SourceType] of the AudioTrack was
         * either [SpatializerConstants.DEFAULT] or [SpatializerConstants.POINT_SOURCE]. If the
         * [SourceType] was [SpatializerConstants.SOUND_FIELD], then this method will throw an
         * [IllegalStateException].
         *
         * @param session The current SceneCore [Session] instance.
         * @param track The [AudioTrack] on which to set the [PointSourceParams].
         * @param params The [PointSourceParams] to be set.
         * @throws IllegalStateException if the [SpatializerConstants.SourceType] of the
         *   [AudioTrack] is [SpatializerConstants.SOUND_FIELD].
         * @throws IllegalArgumentException if the [PointSourceParams] is not able to be set.
         */
        @JvmStatic
        public fun setPointSourceParams(
            session: Session,
            track: AudioTrack,
            params: PointSourceParams,
        ) {
            session.platformAdapter.audioTrackExtensionsWrapper.setPointSourceParams(
                track,
                params.rtPointSourceParams,
            )
        }
    }
}

/** Provides spatial audio extensions on the platform [AudioTrack.Builder] class. */
@Suppress("ClassShouldBeObject", "MissingBuildMethod", "TopLevelBuilder")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SpatialAudioTrackBuilder private constructor() {

    public companion object {
        /**
         * Sets the [PointSourceParams] on the provided [AudioTrack.Builder].
         *
         * @param session The current SceneCore [Session] instance.
         * @param builder The Builder on which to set the attributes.
         * @param params The source params to be set.
         * @return The same [AudioTrack.Builder] instance provided.
         */
        @Suppress("SetterReturnsThis")
        @JvmStatic
        public fun setPointSourceParams(
            session: Session,
            builder: AudioTrack.Builder,
            params: PointSourceParams,
        ): AudioTrack.Builder {

            return session.platformAdapter.audioTrackExtensionsWrapper.setPointSourceParams(
                builder,
                params.rtPointSourceParams,
            )
        }

        /**
         * Sets the [SoundFieldAttributes] on the provided [AudioTrack.Builder].
         *
         * @param session The current SceneCore [Session] instance.
         * @param builder The Builder on which to set the attributes.
         * @param attributes The source attributes to be set.
         * @return The same [AudioTrack.Builder] instance provided.
         */
        @Suppress("SetterReturnsThis")
        @JvmStatic
        public fun setSoundFieldAttributes(
            session: Session,
            builder: AudioTrack.Builder,
            attributes: SoundFieldAttributes,
        ): AudioTrack.Builder {
            return session.platformAdapter.audioTrackExtensionsWrapper.setSoundFieldAttributes(
                builder,
                attributes.rtSoundFieldAttributes,
            )
        }
    }
}
