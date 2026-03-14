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

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.SceneRuntime

/**
 * Provides spatial audio playback for a sound field associated with an [Entity].
 *
 * It provides an [AudioOutputProvider] which can be used to configure an ExoPlayer.Builder instance
 * for sound field audio playback. The characteristics of the sound field are defined by
 * [SoundFieldAttributes].
 *
 * This component can only be attached to one [Entity] at a time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SoundFieldAudioComponent
internal constructor(
    context: Context,
    sceneRuntime: SceneRuntime,
    attributes: SoundFieldAttributes,
) : Component {

    internal val rtSoundFieldAudioComponent =
        sceneRuntime.createSoundFieldAudioComponent(context, attributes.rtSoundFieldAttributes)

    private var attachedEntity: Entity? = null

    override fun onAttach(entity: Entity): Boolean {
        if (attachedEntity != null) {
            return false
        }
        if ((entity as BaseEntity<*>).rtEntity!!.addComponent(rtSoundFieldAudioComponent)) {
            attachedEntity = entity
            return true
        }
        return false
    }

    override fun onDetach(entity: Entity) {
        if (entity != attachedEntity) {
            return
        }
        (entity as BaseEntity<*>).rtEntity!!.removeComponent(rtSoundFieldAudioComponent)
        attachedEntity = null
    }

    /**
     * Returns an [AudioOutputProvider] that can be used to configure an
     * [androidx.media3.exoplayer.ExoPlayer.Builder] for ambisonics audio playback.
     */
    public fun getAudioOutputProvider(): AudioOutputProvider {
        return rtSoundFieldAudioComponent.getAudioOutputProvider()
    }

    public companion object {
        /**
         * Creates a [SoundFieldAudioComponent] for the given [session] and initializes it with the
         * provided [attributes].
         */
        @JvmStatic
        public fun create(
            session: Session,
            attributes: SoundFieldAttributes,
        ): SoundFieldAudioComponent {
            return SoundFieldAudioComponent(session.context, session.sceneRuntime, attributes)
        }
    }
}
