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
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpaceValue
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference

/**
 * Implementation of SceneCore's [ActivitySpace] for OpenXR.
 *
 * This Entity represents the origin of the Scene, and is positioned by the system.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OpenXrActivitySpace
internal constructor(
    context: Context?,
    entityHandle: Long,
    nativeWrapper: SceneCoreOpenXrNative,
    sceneNodeRegistry: OpenXrSceneNodeRegistry,
    executor: ScheduledExecutorService,
) :
    OpenXrSystemSpaceEntity(context, entityHandle, nativeWrapper, sceneNodeRegistry, executor),
    ActivitySpace {

    private val boundsListeners = CopyOnWriteArraySet<ActivitySpace.OnBoundsChangedListener>()
    // TODO: b/538951394 - Query initial spatial container bounds from native during initialization
    // and route OpenXR container bounds change events to onBoundsChanged.
    private val _bounds = AtomicReference<Dimensions?>()

    override val bounds: Dimensions
        get() =
            _bounds.get()
                ?: throw UnsupportedOperationException(
                    "ActivitySpace bounds are not yet supported in OpenXR."
                )

    override val activitySpacePose: Pose
        get() = Pose.Identity

    override val activitySpaceScale: Vector3
        get() = Vector3.One

    override val worldSpaceScale: Vector3
        get() = Vector3.One

    override var parent: Entity?
        get() = super.parent
        set(_) = throw UnsupportedOperationException("Cannot set 'parent' on an ActivitySpace.")

    // TODO: b/535276430 - Implement recommended content box calculations for ActivitySpace in
    // Vitreous.
    override val recommendedContentBoxInFullSpace: BoundingBox
        get() = TODO("Not yet implemented for OpenXR ActivitySpace")

    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        return when (relativeTo) {
            Space.PARENT ->
                throw UnsupportedOperationException(
                    "ActivitySpace is a root space and does not have a parent."
                )
            Space.ACTIVITY -> activitySpacePose
            Space.REAL_WORLD -> poseInPerceptionSpace
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        throw UnsupportedOperationException("Cannot set 'pose' on an ActivitySpace.")
    }

    override fun getScale(@SpaceValue relativeTo: Int): Vector3 {
        return when (relativeTo) {
            Space.PARENT ->
                throw UnsupportedOperationException(
                    "ActivitySpace is a root space and does not have a parent."
                )
            Space.ACTIVITY -> activitySpaceScale
            Space.REAL_WORLD -> worldSpaceScale
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setScale(scale: Vector3, @SpaceValue relativeTo: Int) {
        throw UnsupportedOperationException("Cannot set 'scale' on an ActivitySpace.")
    }

    override fun addOnBoundsChangedListener(listener: ActivitySpace.OnBoundsChangedListener) {
        boundsListeners.add(listener)
    }

    override fun removeOnBoundsChangedListener(listener: ActivitySpace.OnBoundsChangedListener) {
        boundsListeners.remove(listener)
    }

    /** Called when the bounds of the ActivitySpace change to dispatch updates to listeners. */
    // TODO: b/538951394 - Route OpenXR spatial container dimension change updates from runtime
    // polling thread.
    public fun onBoundsChanged(newBounds: Dimensions) {
        _bounds.set(newBounds)
        for (listener in boundsListeners) {
            listener.onBoundsChanged(newBounds)
        }
    }

    // TODO: b/535276460 - Implement raycast hit testing relative to activity space pose for
    // Vitreous.
    override suspend fun hitTestRelativeToActivityPose(
        origin: Vector3,
        direction: Vector3,
        hitTestFilter: Int,
        scenePose: ScenePose,
    ): HitTestResult = TODO("Not yet implemented for OpenXR ActivitySpace")
}
