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

import android.media.MediaPlayer

@Suppress("ClassShouldBeObject")
public class SpatialMediaPlayer {

    public companion object {
        /**
         * Sets a [PointSourceAttributes] on a [MediaPlayer] instance.
         *
         * Must be called before prepare(), not compatible with instances created by
         * MediaPlayer#create(). Only the attributes from the most recent call to
         * setPointSourceAttributes or [setSoundFieldAttributes] will apply.
         *
         * @param session The current SceneCore [Session] instance.
         * @param mediaPlayer The [MediaPlayer] instance on which to set the attributes
         * @param attributes The source attributes to be set.
         */
        @JvmStatic
        public fun setPointSourceAttributes(
            session: Session,
            mediaPlayer: MediaPlayer,
            attributes: PointSourceAttributes,
        ) {
            session.platformAdapter.mediaPlayerExtensionsWrapper.setPointSourceAttributes(
                mediaPlayer,
                attributes.rtPointSourceAttributes,
            )
        }

        /**
         * Sets a [SoundFieldAttributes] on a [MediaPlayer] instance.
         *
         * Must be called before prepare(), not compatible with instances created by
         * MediaPlayer#create(). Only the attributes from the most recent call to
         * setSoundFieldAttributes or [setPointSourceAttributes] will apply.
         *
         * @param session The current SceneCore [Session] instance.
         * @param mediaPlayer The [MediaPlayer] instance on which to set the attributes
         * @param attributes The source attributes to be set.
         */
        @JvmStatic
        public fun setSoundFieldAttributes(
            session: Session,
            mediaPlayer: MediaPlayer,
            attributes: SoundFieldAttributes,
        ) {
            session.platformAdapter.mediaPlayerExtensionsWrapper.setSoundFieldAttributes(
                mediaPlayer,
                attributes.rtSoundFieldAttributes,
            )
        }
    }
}
