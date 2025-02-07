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

@Suppress("ClassShouldBeObject")
public class SpatialAudioTrack {

    public companion object {
        /**
         * Gets the [SourceType] of the provided [AudioTrack]. This value is implicitly set
         * depending one which type of attributes was used to configure the builder. Will return
         * [SpatializerExtensions.NOT_SPATIALIZED] for tracks that didn't use spatial audio
         * attributes.
         *
         * @param session The current SceneCore [Session] instance.
         * @param track The [AudioTrack] from which to get the [SpatializerConstants.SourceType].
         * @return The [SpatializerExtensions.SourceType] of the provided track.
         */
        @JvmStatic
        @SpatializerConstants.SourceType
        public fun getSpatialSourceType(session: Session, track: AudioTrack): Int {
            return session.platformAdapter.audioTrackExtensionsWrapper
                .getSpatialSourceType(track)
                .sourceTypeToJxr()
        }

        /**
         * Gets the [PointSourceAttributes] of the provided [AudioTrack].
         *
         * @param session The current SceneCore [Session] instance.
         * @param track The [AudioTrack] from which to get the [PointSourceAttributes].
         * @return The [PointSourceAttributes] of the provided track, null if not set.
         */
        @JvmStatic
        public fun getPointSourceAttributes(
            session: Session,
            track: AudioTrack,
        ): PointSourceAttributes? {
            val rtAttributes =
                session.platformAdapter.audioTrackExtensionsWrapper.getPointSourceAttributes(track)
            return rtAttributes?.toPointSourceAttributes(session)
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
            track: AudioTrack
        ): SoundFieldAttributes? {
            val rtAttributes =
                session.platformAdapter.audioTrackExtensionsWrapper.getSoundFieldAttributes(track)
            return rtAttributes?.toSoundFieldAttributes()
        }
    }
}

/** Provides spatial audio extensions on the platform [AudioTrack.Builder] class. */
@Suppress("ClassShouldBeObject", "MissingBuildMethod", "TopLevelBuilder")
public class SpatialAudioTrackBuilder private constructor() {

    public companion object {
        /**
         * Sets the [PointSourceAttributes] on the provided [AudioTrack.Builder].
         *
         * @param session The current SceneCore [Session] instance.
         * @param builder The Builder on which to set the attributes.
         * @param attributes The source attributes to be set.
         * @return The same [AudioTrack.Builder] instance provided.
         */
        @Suppress("SetterReturnsThis")
        @JvmStatic
        public fun setPointSourceAttributes(
            session: Session,
            builder: AudioTrack.Builder,
            attributes: PointSourceAttributes,
        ): AudioTrack.Builder {

            return session.platformAdapter.audioTrackExtensionsWrapper.setPointSourceAttributes(
                builder,
                attributes.rtPointSourceAttributes,
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
