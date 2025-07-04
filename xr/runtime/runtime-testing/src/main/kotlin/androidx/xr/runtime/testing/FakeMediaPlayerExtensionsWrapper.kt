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

package androidx.xr.runtime.testing

import android.media.MediaPlayer
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.MediaPlayerExtensionsWrapper
import androidx.xr.runtime.internal.PointSourceParams
import androidx.xr.runtime.internal.SoundFieldAttributes

/** Test-only implementation of [MediaPlayerExtensionsWrapper] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeMediaPlayerExtensionsWrapper : MediaPlayerExtensionsWrapper {
    private var _pointSourceParams: MutableMap<MediaPlayer, PointSourceParams> = mutableMapOf()

    /**
     * For test purposes only.
     *
     * This read-only map stores the [PointSourceParams] that were last set for each [MediaPlayer]
     * instance via the [setPointSourceParams] method.
     *
     * Tests can inspect this map to verify that the code under test correctly applies the intended
     * `PointSourceParams` to the `MediaPlayer`.
     */
    public val pointSourceParams: Map<MediaPlayer, PointSourceParams>
        get() = _pointSourceParams

    /**
     * Sets the PointSourceParams of the MediaPlayer.
     *
     * @param mediaPlayer The MediaPlayer to set the PointSourceParams on.
     * @param params The PointSourceParams to set.
     */
    override fun setPointSourceParams(mediaPlayer: MediaPlayer, params: PointSourceParams) {
        _pointSourceParams[mediaPlayer] = params
    }

    private var _soundFieldAttributes: MutableMap<MediaPlayer, SoundFieldAttributes> =
        mutableMapOf()

    /**
     * For test purposes only.
     *
     * This read-only map stores the [SoundFieldAttributes] that were last set for each
     * [MediaPlayer] instance via the [setSoundFieldAttributes] method.
     *
     * Tests can inspect this map to verify that the code under test correctly applies the intended
     * `SoundFieldAttributes` to the `MediaPlayer`.
     */
    public val soundFieldAttributes: Map<MediaPlayer, SoundFieldAttributes>
        get() = _soundFieldAttributes

    /**
     * Sets the SoundFieldAttributes of the MediaPlayer.
     *
     * @param mediaPlayer The MediaPlayer to set the SoundFieldAttributes on.
     * @param attributes The SoundFieldAttributes to set.
     */
    override fun setSoundFieldAttributes(
        mediaPlayer: MediaPlayer,
        attributes: SoundFieldAttributes,
    ) {
        _soundFieldAttributes[mediaPlayer] = attributes
    }
}
