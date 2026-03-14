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
import androidx.annotation.RestrictTo.Scope
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.SceneRuntime

/**
 * A Component that provides positional spatial audio playback for an [Entity].
 *
 * It provides an [AudioOutputProvider] which can be used to configure an
 * [androidx.media3.exoplayer.ExoPlayer.Builder] instance for positional audio playback.
 *
 * This component can only be attached to one [Entity] at a time. If the component is detached from
 * an [Entity], the audio will become head-locked.
 */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public class PositionalAudioComponent
internal constructor(context: Context, sceneRuntime: SceneRuntime, params: PointSourceParams) :
    Component {

    internal val rtComponent =
        sceneRuntime.createPositionalAudioComponent(context, params.rtPointSourceParams)

    private var attachedEntity: Entity? = null

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

    /**
     * Updates the [PointSourceParams] used by the spatial audio source.
     *
     * These params will apply to currently playing audio and future playback requests.
     */
    public fun setPointSourceParams(params: PointSourceParams) {
        rtComponent.setPointSourceParams(params.rtPointSourceParams)
    }

    /**
     * Returns an [AudioOutputProvider] that can be used to configure an
     * [androidx.media3.exoplayer.ExoPlayer.Builder] for positional audio playback.
     */
    public fun getAudioOutputProvider(): AudioOutputProvider {
        return rtComponent.getAudioOutputProvider()
    }

    public companion object {
        /**
         * Creates a [PositionalAudioComponent] for the given [session] and initializes it with the
         * provided [params].
         */
        @JvmStatic
        public fun create(session: Session, params: PointSourceParams): PositionalAudioComponent {
            return PositionalAudioComponent(session.context, session.sceneRuntime, params)
        }
    }
}
