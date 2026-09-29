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

package androidx.xr.scenecore.openxr

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SystemSpaceEntity
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference

/**
 * Implementation of SceneCore's [SystemSpaceEntity] for OpenXR.
 *
 * This is a parentless, system-controlled Entity that defines its own coordinate space, and is
 * expected to be the root of its own parent-child entity hierarchy.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class OpenXrSystemSpaceEntity
internal constructor(
    context: Context?,
    entityHandle: Long,
    nativeWrapper: SceneCoreOpenXrNative,
    sceneNodeRegistry: OpenXrSceneNodeRegistry,
    executor: ScheduledExecutorService,
) :
    OpenXrEntity(context, entityHandle, nativeWrapper, sceneNodeRegistry, executor),
    SystemSpaceEntity {

    private class ListenerHolder(val listener: Runnable, val executor: Executor)

    private val originListener = AtomicReference<ListenerHolder?>()

    /**
     * Backing storage for [poseInPlatformReferenceSpace], updated via
     * [setPlatformReferenceSpacePose]. Defaults to [Pose.Identity] until the runtime reports the
     * space's origin.
     */
    protected val _poseInPlatformReferenceSpace: AtomicReference<Pose> =
        AtomicReference(Pose.Identity)

    /**
     * The pose of this space's origin relative to the underlying OpenXR platform reference space.
     *
     * This is returned directly from stored state rather than computed through [transformPoseTo].
     * [androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl] derives its own pose from
     * the ActivitySpace's value of this property, so computing it via the perception space would
     * recurse infinitely.
     */
    override val poseInPlatformReferenceSpace: Pose
        get() = _poseInPlatformReferenceSpace.get()

    /**
     * Updates the pose of this system space relative to the underlying platform reference space and
     * notifies registered origin change listeners if the pose changed.
     */
    public fun setPlatformReferenceSpacePose(pose: Pose) {
        val previousPose = _poseInPlatformReferenceSpace.getAndSet(pose)
        if (previousPose != pose) {
            onOriginChanged()
        }
    }

    override fun setOnOriginChangedListener(listener: Runnable?, executor: Executor?) {
        if (listener != null) {
            originListener.set(ListenerHolder(listener, executor ?: this.executor))
        } else {
            originListener.set(null)
        }
    }

    /** Called when the underlying space's origin is updated. */
    // TODO: b/538951394 - Connect the native OpenXR event bridge to trigger onOriginChanged
    // when the underlying OpenXR reference space origin updates.
    public fun onOriginChanged() {
        originListener.get()?.let { holder -> holder.executor.execute(holder.listener) }
    }

    override val worldSpaceScale: Vector3
        get() = super<OpenXrEntity>.getScale(Space.PARENT)
}
