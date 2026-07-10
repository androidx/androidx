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
package androidx.xr.scenecore.spatial.core

import android.media.MediaPlayer
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.MediaPlayerExtensionsWrapper
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import com.android.extensions.xr.media.MediaPlayerExtensions

/** Implementation of the [MediaPlayerExtensionsWrapper]. */
internal class MediaPlayerExtensionsWrapperImpl(
    private val mediaPlayerExtensions: MediaPlayerExtensions
) : MediaPlayerExtensionsWrapper {
    override fun setPointSourceParams(
        mediaPlayer: MediaPlayer,
        params: PointSourceParams,
        entity: Entity,
    ) {
        mediaPlayerExtensions.setPointSourceParams(
            mediaPlayer,
            MediaUtils.convertPointSourceParamsToExtensions(params, entity),
        )
    }

    override fun setSoundFieldAttributes(
        mediaPlayer: MediaPlayer,
        attributes: SoundFieldAttributes,
    ) {
        val extAttributes = MediaUtils.convertSoundFieldAttributesToExtensions(attributes)

        mediaPlayerExtensions.setSoundFieldAttributes(mediaPlayer, extAttributes)
    }
}
