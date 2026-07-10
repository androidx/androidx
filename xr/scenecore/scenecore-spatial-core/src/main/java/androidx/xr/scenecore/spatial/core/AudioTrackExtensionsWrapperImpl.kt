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

import android.media.AudioTrack
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import com.android.extensions.xr.media.AudioTrackExtensions

/** Implementation of the [AudioTrackExtensionsWrapper] */
internal class AudioTrackExtensionsWrapperImpl(
    private val audioTrackExtensions: AudioTrackExtensions
) : AudioTrackExtensionsWrapper {
    override fun getPointSourceParams(track: AudioTrack): PointSourceParams? {
        audioTrackExtensions.getPointSourceParams(track) ?: return null

        return PointSourceParams()
    }

    override fun getSoundFieldAttributes(track: AudioTrack): SoundFieldAttributes? {
        val extAttributes = audioTrackExtensions.getSoundFieldAttributes(track) ?: return null

        return SoundFieldAttributes(extAttributes.ambisonicsOrder)
    }

    override fun getSpatialSourceType(track: AudioTrack): Int {
        return MediaUtils.convertExtensionsToSourceType(
            audioTrackExtensions.getSpatialSourceType(track)
        )
    }

    override fun setPointSourceParams(
        track: AudioTrack,
        params: PointSourceParams,
        entity: Entity?,
    ) {
        val extParams = MediaUtils.convertPointSourceParamsToExtensions(params, entity)

        audioTrackExtensions.setPointSourceParams(track, extParams)
    }

    override fun setPointSourceParams(
        builder: AudioTrack.Builder,
        params: PointSourceParams,
        entity: Entity?,
    ): AudioTrack.Builder {
        val extParams = MediaUtils.convertPointSourceParamsToExtensions(params, entity)

        return audioTrackExtensions.setPointSourceParams(builder, extParams)
    }

    override fun setSoundFieldAttributes(
        builder: AudioTrack.Builder,
        attributes: SoundFieldAttributes,
    ): AudioTrack.Builder {
        val extAttributes = MediaUtils.convertSoundFieldAttributesToExtensions(attributes)

        return audioTrackExtensions.setSoundFieldAttributes(builder, extAttributes)
    }
}
