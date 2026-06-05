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

package androidx.xr.compose.subspace.media

import androidx.collection.MutableObjectIntMap
import androidx.collection.mutableObjectIntMapOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CoreEntityNode
import androidx.xr.compose.subspace.layout.CoreEntityScope
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.coreEntity
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.runtime.Session
import androidx.xr.scenecore.Component
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.PositionalAudioComponent
import androidx.xr.scenecore.SoundEffectPoolComponent
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SoundFieldAudioComponent

/**
 * Signifies that a class is capable of configuring spatialized audio output.
 *
 * A [SpatializedAudioOutput] should only be attached to one Composable at a time. If it is attached
 * multiple times, its associated audio output will become head locked.
 */
public sealed class SpatializedAudioOutput {
    private val attachedEntities: MutableObjectIntMap<CoreEntity> = mutableObjectIntMapOf()

    internal fun onAttach(entity: CoreEntity) {
        val newCount = attachedEntities.getOrDefault(entity, 0) + 1
        attachedEntities[entity] = newCount
        if (newCount == 1) {
            // This ensures that in the case where the same output is accidentally added multiple
            // times to the same Composable, we only attach a component on the first instance.
            sync()
        }
    }

    internal fun onDetach(entity: CoreEntity) {
        // This ensures that in the case where the same output is accidentally added multiple times
        // to the same Composable, detaching one will still keep spatial audio active, until all
        // instances are detached.
        val count = attachedEntities.getOrDefault(entity, 0)
        if (count <= 1) {
            attachedEntities.remove(entity)
            entity.removeComponent(component)
            sync()
        } else {
            attachedEntities[entity] = count - 1
        }
    }

    private fun sync() {
        if (attachedEntities.size == 1) {
            attachedEntities.forEachKey { it.addComponent(component) }
        } else {
            // Detach from all if more than one
            attachedEntities.forEachKey { it.removeComponent(component) }
        }
    }

    internal abstract val component: Component

    override fun equals(other: Any?): Boolean {
        // Check if the object is equal or the component backing the object is equal. The latter
        // preserves equality for SoundEffectPoolComponent.asSpatializedAudioOutput() instances that
        // aren't remembered.
        if (this === other) return true
        if (other !is SpatializedAudioOutput) return false
        return this.javaClass == other.javaClass && this.component === other.component
    }

    override fun hashCode(): Int {
        return component.hashCode()
    }
}

/**
 * Allows spatializing Exoplayer audio output. This needs to be attached to a Composable via
 * [spatializedAudioOutput]. The [AudioOutputProvider] contained in this class should be passed to
 * an Exoplayer.Builder instance.
 */
public abstract class SpatializedExoplayerAudioOutput internal constructor() :
    SpatializedAudioOutput() {

    /** An [AudioOutputProvider] that may be passed to an ExoPlayer.Builder instance. */
    public abstract val audioOutputProvider: AudioOutputProvider
}

/**
 * Creates a [SpatializedExoplayerAudioOutput] with [PointSourceParams]. When used with an
 * ExoPlayer.Builder instance and [spatializedAudioOutput], audio will be spatialized from the
 * Composable it is attached to.
 */
public class PointSourceExoplayerAudioOutput
@RememberInComposition
constructor(session: Session, params: PointSourceParams) : SpatializedExoplayerAudioOutput() {
    internal val positionalAudioComponent = PositionalAudioComponent.create(session, params)

    /** An [AudioOutputProvider] that be passed to an ExoPlayer.Builder instance. */
    public override val audioOutputProvider: AudioOutputProvider
        get() = positionalAudioComponent.audioOutputProvider

    /**
     * A [PointSourceParams] to modify the audio spatialization. These pointSourceParams will apply
     * to currently playing audio and future playback requests.
     */
    public var params: PointSourceParams
        get() = positionalAudioComponent.pointSourceParams
        set(value) {
            positionalAudioComponent.pointSourceParams = value
        }

    override val component: Component
        get() = positionalAudioComponent
}

/**
 * Creates a [SpatializedExoplayerAudioOutput] from [SoundFieldAttributes]. When used with an
 * ExoPlayer.Builder instance and [spatializedAudioOutput], audio will be spatialized from the
 * Composable it is attached to.
 */
public class SoundFieldExoplayerAudioOutput
@RememberInComposition
constructor(session: Session, soundFieldAttributes: SoundFieldAttributes) :
    SpatializedExoplayerAudioOutput() {
    internal val soundFieldAudioComponent =
        SoundFieldAudioComponent.create(session, soundFieldAttributes)

    /** An [AudioOutputProvider] that be passed to an ExoPlayer.Builder instance. */
    public override val audioOutputProvider: AudioOutputProvider
        get() = soundFieldAudioComponent.audioOutputProvider

    override val component: Component
        get() = soundFieldAudioComponent
}

/**
 * Adds spatialized audio output to this Composable. Use [PointSourceExoplayerAudioOutput],
 * [SoundFieldExoplayerAudioOutput] or [SoundEffectPoolComponent.asSpatializedAudioOutput] to create
 * an object capable of spatializing audio.
 *
 * @sample androidx.xr.compose.samples.SpatializedAudioOutputSample
 * @sample androidx.xr.compose.samples.SoundFieldSpatializedAudioOutputSample
 * @sample androidx.xr.compose.samples.SoundEffectPlayerSample
 * @param spatializedAudioOutput A [SpatializedAudioOutput] to be added to this Composable. A
 *   [SpatializedAudioOutput] should only be attached to one Composable at a time. If it is attached
 *   multiple times, its associated audio output will become head locked.
 */
public fun SubspaceModifier.spatializedAudioOutput(
    spatializedAudioOutput: SpatializedAudioOutput
): SubspaceModifier = this.then(SpatializedAudioOutputElement(spatializedAudioOutput))

private class SpatializedAudioOutputElement(
    private val spatializedAudioOutput: SpatializedAudioOutput
) : SubspaceModifierNodeElement<SpatializedAudioOutputNode>() {
    override fun create(): SpatializedAudioOutputNode =
        SpatializedAudioOutputNode(spatializedAudioOutput)

    override fun update(node: SpatializedAudioOutputNode) {
        node.spatializedAudioOutput = spatializedAudioOutput
    }

    override fun hashCode(): Int {
        return spatializedAudioOutput.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatializedAudioOutputElement) return false

        return spatializedAudioOutput == other.spatializedAudioOutput
    }
}

private class SpatializedAudioOutputNode(initialOutput: SpatializedAudioOutput) :
    SubspaceModifier.Node(), CoreEntityNode {

    var spatializedAudioOutput: SpatializedAudioOutput = initialOutput
        set(value) {
            if (field !== value) {
                // For handling the case when a developer swaps the SpatializedAudioOutput instance.
                if (isAudioAttached) {
                    field.onDetach(coreEntity)
                    value.onAttach(coreEntity)
                }
                field = value
            }
        }

    private var isAudioAttached = false

    override fun onAttach() {
        super.onAttach()
        if (!isAudioAttached) {
            spatializedAudioOutput.onAttach(coreEntity)
            isAudioAttached = true
        }
    }

    override fun onDetach() {
        if (isAudioAttached) {
            spatializedAudioOutput.onDetach(coreEntity)
            isAudioAttached = false
        }
        super.onDetach()
    }

    override fun CoreEntityScope.modifyCoreEntity() {
        // No-op. We just need the node to be a CoreEntityNode to get access to coreEntity.
    }
}

/**
 * Creates and remembers a [PointSourceExoplayerAudioOutput] with [PointSourceParams]. When used
 * with an ExoPlayer.Builder instance and [spatializedAudioOutput], audio will be spatialized from
 * the Composable it is attached to.
 *
 * @param params A [PointSourceParams] to modify the audio spatialization. Updating this will affect
 *   the spatialization currently playing audio and future playback requests.
 */
@Composable
public fun rememberPointSourceExoplayerAudioOutput(
    params: PointSourceParams
): PointSourceExoplayerAudioOutput {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    return remember(session) { PointSourceExoplayerAudioOutput(session, params) }
        .apply { this.params = params }
}

/**
 * Returns a [SpatializedAudioOutput] that wraps this [SoundEffectPoolComponent]. This allows the
 * component's audio to be spatialized from the Composable it is attached to via
 * [spatializedAudioOutput].
 */
public fun SoundEffectPoolComponent.asSpatializedAudioOutput(): SpatializedAudioOutput {
    return ComponentSpatializedAudioOutput(this)
}

private class ComponentSpatializedAudioOutput(override val component: Component) :
    SpatializedAudioOutput()
