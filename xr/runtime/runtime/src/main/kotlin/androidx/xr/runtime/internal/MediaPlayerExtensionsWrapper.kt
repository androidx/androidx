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

package androidx.xr.runtime.internal

import android.media.MediaPlayer
import androidx.annotation.RestrictTo

/** Interface for a XR Runtime MediaPlayerExtensionsWrapper */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface MediaPlayerExtensionsWrapper {
    /**
     * Sets the PointSourceParams of the MediaPlayer.
     *
     * @param mediaPlayer The MediaPlayer to set the PointSourceParams on.
     * @param params The PointSourceParams to set.
     */
    public fun setPointSourceParams(mediaPlayer: MediaPlayer, params: PointSourceParams)

    /**
     * Sets the SoundFieldAttributes of the MediaPlayer.
     *
     * @param mediaPlayer The MediaPlayer to set the SoundFieldAttributes on.
     * @param attributes The SoundFieldAttributes to set.
     */
    public fun setSoundFieldAttributes(mediaPlayer: MediaPlayer, attributes: SoundFieldAttributes)
}
