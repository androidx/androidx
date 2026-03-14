/*
 * Copyright 2026 The Android Open Source Project
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

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.SoundPool
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundEffect
import androidx.xr.scenecore.runtime.SoundEffectPlayer
import androidx.xr.scenecore.runtime.SoundEffectPool
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import androidx.xr.scenecore.runtime.Stream
import java.util.concurrent.Executor

internal class SoundEffectPoolImpl(
    maxStreams: Int,
    soundPoolExtensionsWrapper: SoundPoolExtensionsWrapper,
    soundEffectPlayer: SoundEffectPlayer?,
) : SoundEffectPool {

    internal val soundPool = SoundPool.Builder().setMaxStreams(maxStreams).build()
    private val _soundEffectPlayer: SoundEffectPlayer =
        soundEffectPlayer ?: SoundEffectPlayerImpl(soundPool, soundPoolExtensionsWrapper)

    public override fun setOnLoadCompleteListener(
        executor: Executor,
        listener: SoundEffectPool.LoadCompleteListener,
    ) {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            executor.execute { listener.onLoadComplete(SoundEffect(sampleId), status == 0) }
        }
    }

    override fun load(context: Context, resId: Int): SoundEffect {
        return SoundEffect(soundPool.load(context, resId, /* priority= */ 1))
    }

    override fun load(assetFileDescriptor: AssetFileDescriptor): SoundEffect {
        return SoundEffect(soundPool.load(assetFileDescriptor, /* priority= */ 1))
    }

    override fun unload(soundEffect: SoundEffect): Boolean {
        val soundId = soundEffect.id
        return soundPool.unload(soundId)
    }

    override fun release() {
        soundPool.release()
    }

    internal fun getSoundEffectPlayer(): SoundEffectPlayer {
        return _soundEffectPlayer
    }

    private class SoundEffectPlayerImpl(
        private val soundPool: SoundPool,
        private val soundPoolExtensionsWrapper: SoundPoolExtensionsWrapper,
    ) : SoundEffectPlayer {
        override fun play(
            soundEffect: SoundEffect,
            pointSourceParams: PointSourceParams,
            entity: Entity?,
            volume: Float,
            priority: Int,
            isLooping: Boolean,
        ): Stream {
            val loopCount = if (isLooping) -1 else 0

            val playbackId =
                soundPoolExtensionsWrapper.play(
                    soundPool,
                    soundEffect.id,
                    pointSourceParams,
                    entity,
                    volume,
                    priority,
                    loopCount,
                    PLAYBACK_RATE,
                )
            return Stream(playbackId)
        }

        override fun pause(stream: Stream) {
            soundPool.pause(stream.streamId)
        }

        override fun resume(stream: Stream) {
            soundPool.resume(stream.streamId)
        }

        override fun stop(stream: Stream) {
            soundPool.stop(stream.streamId)
        }

        override fun setVolume(stream: Stream, volume: Float) {
            soundPool.setVolume(stream.streamId, volume, volume)
        }

        override fun setLooping(stream: Stream, isLooping: Boolean) {
            val loopCount = if (isLooping) -1 else 0
            soundPool.setLoop(stream.streamId, loopCount)
        }

        companion object {
            const val PLAYBACK_RATE = 1.0f
        }
    }
}
