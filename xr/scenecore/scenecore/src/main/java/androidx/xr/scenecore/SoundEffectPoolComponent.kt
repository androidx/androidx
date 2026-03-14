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

package androidx.xr.scenecore

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.SceneRuntime

/**
 * Provides positional sound pool audio playback for an [Entity].
 *
 * This component provides a method to play short sounds loaded by a [SoundEffectPool]. The audio is
 * spatialized based on the [Entity]'s transform and the provided [PointSourceParams].
 *
 * This component can only be attached to one [Entity] at a time. If the component is detached from
 * an [Entity], the audio will become head-locked until re-attached.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Suppress("NotCloseable")
public class SoundEffectPoolComponent
private constructor(
    sceneRuntime: SceneRuntime,
    soundEffectPool: SoundEffectPool,
    private val params: PointSourceParams,
) : Component, SoundEffectPlayer {

    private var attachedEntity: Entity? = null

    internal val rtComponent =
        sceneRuntime.createSoundEffectPoolComponent(soundEffectPool.rtSoundEffectPool)

    override fun onAttach(entity: Entity): Boolean {
        if (attachedEntity != null) {
            return false
        }
        if ((entity as BaseEntity<*>).rtEntity!!.addComponent(rtComponent)) {
            attachedEntity = entity
            return true
        }
        return false
    }

    override fun onDetach(entity: Entity) {
        if (entity != attachedEntity) {
            return
        }
        (entity as BaseEntity<*>).rtEntity!!.removeComponent(rtComponent)
        attachedEntity = null
    }

    override fun play(
        soundEffect: SoundEffect,
        volume: Float,
        priority: Int,
        isLooping: Boolean,
    ): Stream {
        val rtEntity = (attachedEntity as? BaseEntity<*>)?.rtEntity
        return rtComponent
            .play(
                soundEffect.toRtSoundEffect(),
                params.rtPointSourceParams,
                rtEntity,
                volume,
                priority,
                isLooping,
            )
            .toStream()
    }

    override fun pause(stream: Stream) {
        rtComponent.pause(stream.toRtStream())
    }

    override fun resume(stream: Stream) {
        rtComponent.resume(stream.toRtStream())
    }

    override fun stop(stream: Stream) {
        rtComponent.stop(stream.toRtStream())
    }

    override fun setVolume(stream: Stream, volume: Float) {
        rtComponent.setVolume(stream.toRtStream(), volume)
    }

    override fun setLooping(stream: Stream, isLooping: Boolean) {
        rtComponent.setLooping(stream.toRtStream(), isLooping)
    }

    public companion object {
        /**
         * Creates a [SoundEffectPoolComponent].
         *
         * @param session The active XR session.
         * @param soundEffectPool The pool that manages the loaded sound assets.
         * @param params The initial spatial audio parameters for this source.
         * @return A new instance of [SoundEffectPoolComponent].
         */
        @JvmStatic
        public fun create(
            session: Session,
            soundEffectPool: SoundEffectPool,
            params: PointSourceParams,
        ): SoundEffectPoolComponent {
            return SoundEffectPoolComponent(session.sceneRuntime, soundEffectPool, params)
        }
    }
}
