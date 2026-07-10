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

import android.media.SoundPool
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import com.android.extensions.xr.media.SoundPoolExtensions

/** Implementation of [SoundPoolExtensionsWrapper]. */
internal class SoundPoolExtensionsWrapperImpl(private val mExtensions: SoundPoolExtensions) :
    SoundPoolExtensionsWrapper {
    override fun play(
        soundPool: SoundPool,
        soundId: Int,
        params: PointSourceParams,
        entity: Entity?,
        volume: Float,
        priority: Int,
        loop: Int,
        rate: Float,
    ): Int {
        val extParams = MediaUtils.convertPointSourceParamsToExtensions(params, entity)
        return mExtensions.playAsPointSource(
            soundPool,
            soundId,
            extParams,
            volume,
            priority,
            loop,
            rate,
        )
    }

    override fun play(
        soundPool: SoundPool,
        soundId: Int,
        attributes: SoundFieldAttributes,
        volume: Float,
        priority: Int,
        loop: Int,
        rate: Float,
    ): Int {
        val extAttributes = MediaUtils.convertSoundFieldAttributesToExtensions(attributes)

        return mExtensions.playAsSoundField(
            soundPool,
            soundId,
            extAttributes,
            volume,
            priority,
            loop,
            rate,
        )
    }

    override fun getSpatialSourceType(soundPool: SoundPool, streamId: Int): Int {
        return MediaUtils.convertExtensionsToSourceType(
            mExtensions.getSpatialSourceType(soundPool, streamId)
        )
    }
}
