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

@file:Suppress("UNUSED_PARAMETER")

package androidx.xr.scenecore

import android.media.SoundPool

/** Provides spatial audio extensions on the framework [SoundPool] class. */
@Suppress("ClassShouldBeObject")
public class SpatialSoundPool private constructor() {

    public companion object {
        /**
         * Plays a spatialized sound effect emitted relative [Node] in the [PointSourceAttributes].
         *
         * @param session The current SceneCore [Session] instance.
         * @param soundPool The [SoundPool] to use to the play the sound.
         * @param soundID a soundId returned by the load() function.
         * @param attributes attributes to specify sound source. [PointSourceAttributes]
         * @param volume value (range = 0.0 to 1.0)
         * @param priority stream priority (0 = lowest priority)
         * @param loop loop mode (0 = no loop, -1 = loop forever, N = loop N times)
         * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
         * @return non-zero streamID if successful, zero if failed
         */
        @JvmStatic
        public fun play(
            session: Session,
            soundPool: SoundPool,
            soundID: Int,
            attributes: PointSourceAttributes,
            volume: Float,
            priority: Int,
            loop: Int,
            rate: Float,
        ): Int {

            return session.platformAdapter.soundPoolExtensionsWrapper.play(
                soundPool,
                soundID,
                attributes.rtPointSourceAttributes,
                volume,
                priority,
                loop,
                rate,
            )
        }

        /**
         * Plays a spatialized sound effect as a sound field.
         *
         * @param session The current SceneCore [Session] instance.
         * @param soundPool The [SoundPool] to use to the play the sound.
         * @param soundID a soundId returned by the load() function.
         * @param attributes attributes to specify sound source. [SoundFieldAttributes]
         * @param volume value (range = 0.0 to 1.0)
         * @param priority stream priority (0 = lowest priority)
         * @param loop loop mode (0 = no loop, -1 = loop forever, N = loop N times)
         * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
         * @return non-zero streamID if successful, zero if failed
         */
        @JvmStatic
        public fun play(
            session: Session,
            soundPool: SoundPool,
            soundID: Int,
            attributes: SoundFieldAttributes,
            volume: Float,
            priority: Int,
            loop: Int,
            rate: Float,
        ): Int {

            return session.platformAdapter.soundPoolExtensionsWrapper.play(
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
         * @param session The current SceneCore [Session] instance.
         * @param soundPool The [SoundPool] to use to get its SourceType.
         * @param streamId a streamID returned by the play(), playAsPointSource(), or
         *   playAsSoundField().
         * @return The [SpatializerConstants.SourceType] for the given streamID.
         */
        @JvmStatic
        @SpatializerConstants.SourceType
        public fun getSpatialSourceType(
            session: Session,
            soundPool: SoundPool,
            streamId: Int
        ): Int {
            return session.platformAdapter.soundPoolExtensionsWrapper
                .getSpatialSourceType(soundPool, streamId)
                .sourceTypeToJxr()
        }
    }
}
