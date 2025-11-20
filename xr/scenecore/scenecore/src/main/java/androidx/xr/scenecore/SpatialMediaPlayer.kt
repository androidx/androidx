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

import android.media.MediaPlayer
import androidx.xr.runtime.Session

/** Provides spatial audio extensions on the framework [MediaPlayer] class. */
public object SpatialMediaPlayer {

    /**
     * Sets a [PointSourceParams] on a [MediaPlayer] instance.
     *
     * Must be called before [MediaPlayer.prepare]. Not compatible with instances created by
     * [MediaPlayer.create]. Only the params or attributes from the most recent call to this method
     * or [setSoundFieldAttributes] will apply since they are mutually exclusive.
     *
     * @param session The current [Session] instance.
     * @param mediaPlayer The [MediaPlayer] instance on which to set the params
     * @param params The source params to be set.
     */
    @JvmStatic
    public fun setPointSourceParams(
        session: Session,
        mediaPlayer: MediaPlayer,
        params: PointSourceParams,
    ) {
        session.sceneRuntime.mediaPlayerExtensionsWrapper.setPointSourceParams(
            mediaPlayer,
            params.rtPointSourceParams,
        )
    }

    /**
     * Sets a [SoundFieldAttributes] on a [MediaPlayer] instance.
     *
     * Must be called before [MediaPlayer.prepare]. Not compatible with instances created by
     * [MediaPlayer.create]. Only the attributes or params from the most recent call to this method
     * or [setPointSourceParams] will apply since they are mutually exclusive.
     *
     * @param session The current [Session] instance.
     * @param mediaPlayer The [MediaPlayer] instance on which to set the attributes
     * @param attributes The source attributes to be set.
     */
    @JvmStatic
    public fun setSoundFieldAttributes(
        session: Session,
        mediaPlayer: MediaPlayer,
        attributes: SoundFieldAttributes,
    ) {
        session.sceneRuntime.mediaPlayerExtensionsWrapper.setSoundFieldAttributes(
            mediaPlayer,
            attributes.rtSoundFieldAttributes,
        )
    }
}
