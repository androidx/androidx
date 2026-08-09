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

package androidx.xr.compose.subspace.animation.follow

import androidx.annotation.RestrictTo
import androidx.xr.arcore.ArDevice
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorSpace
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import java.lang.Runnable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

/**
 * A FollowTarget can be used with [androidx.xr.compose.spatial.Subspace] to have a set of content
 * follow a target such as an anchor or AR device.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class FollowTarget
internal constructor(
    public val behavior: FollowBehavior,
    public val dimensions: TrackedDimensions,
) {
    internal suspend fun start(trailingEntity: CoreGroupEntity) {
        behavior.start(trailingEntity = trailingEntity, target = this, dimensions = dimensions)
    }

    public companion object {
        /**
         * By designating content to follow the AR device, it will keep that content near the device
         * camera and typically within the field of view, even as the device moves around.
         *
         * The [Session] is required to access the device's tracking state and to perform pose
         * transformations between coordinate spaces.
         *
         * @param session the current [Session] instance used to track the device and transform
         *   poses.
         * @param behavior determines how the [androidx.xr.compose.spatial.Subspace] follows the
         *   target. It can be made to move faster and be more responsive. The default is
         *   [FollowBehavior.Soft()].
         * @param dimensions set of boolean flags to determine the dimensions of movement that are
         *   tracked. Three rotation and three translation dimensions are available to be tracked.
         *   By default, all dimensions are tracked. Any dimensions not listed will not be tracked.
         *   For example if translationY is not listed, this means the content will not move as the
         *   user moves vertically up and down.
         */
        public fun ArDevice(
            session: Session,
            behavior: FollowBehavior = FollowBehavior.Soft(),
            dimensions: TrackedDimensions = TrackedDimensions.All,
        ): FollowTarget = ArDeviceTarget(session, behavior, dimensions)

        /**
         * Targeting an anchor allows content to be positioned relative to that anchor's location.
         *
         * @param anchorSpace represents the anchor which this
         *   [androidx.xr.compose.spatial.Subspace] will be tethered to. As the anchor moves, so
         *   will the [androidx.xr.compose.spatial.Subspace]
         * @param behavior determines how the [androidx.xr.compose.spatial.Subspace] follows the
         *   target. It can be made to move faster and be more responsive. The default is
         *   [FollowBehavior.Tight].
         * @param dimensions set of boolean flags to determine the dimensions of movement that are
         *   tracked. Three rotation and three translation dimensions are available to be tracked.
         *   By default, all dimensions are tracked. Any dimensions not listed will not be tracked.
         *   For example if translationY is not listed, this means the content will not move as the
         *   user moves vertically up and down.
         */
        public fun Anchor(
            anchorSpace: AnchorSpace,
            behavior: FollowBehavior = FollowBehavior.Tight,
            dimensions: TrackedDimensions = TrackedDimensions.All,
        ): FollowTarget = AnchorTarget(anchorSpace, behavior, dimensions)
    }
}

internal interface FollowTargetFlow {
    val poseUpdates: Flow<Pose>
}

/** A concrete [FollowTarget] that wraps the head pose updates from [ArDevice]. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ArDeviceTarget(
    private val session: Session,
    behavior: FollowBehavior = FollowBehavior.Soft(),
    dimensions: TrackedDimensions = TrackedDimensions.All,
) : FollowTarget(behavior, dimensions), FollowTargetFlow {
    // Distance to stay away from the target when following it.
    val offset: Pose = DEFAULT_OFFSET

    override val poseUpdates: Flow<Pose> =
        ArDevice.getInstance(session = session).state.map { state ->
            session.scene.perceptionSpace.transformPoseTo(
                pose = state.devicePose,
                destination = session.scene.activitySpace,
            )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArDeviceTarget) return false

        if (session != other.session) return false
        if (behavior != other.behavior) return false
        if (dimensions != other.dimensions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = session.hashCode()
        result = 31 * result + behavior.hashCode()
        result = 31 * result + dimensions.hashCode()
        return result
    }

    internal companion object {
        // Distance to stay away from the target in meters.
        val DEFAULT_OFFSET: Pose = Pose(translation = Vector3(x = 0f, y = 0f, z = -.5f))
    }
}

/**
 * A Trackable Anchor entity that wraps an [AnchorSpace] from SceneCore and implements
 * [FollowTarget] to provide a stream of pose updates.
 *
 * This implementation is designed to be constructed directly from an existing [AnchorSpace]
 * instance provided by the developer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class AnchorTarget(
    val anchorSpace: AnchorSpace,
    behavior: FollowBehavior = FollowBehavior.Tight,
    dimensions: TrackedDimensions = TrackedDimensions.All,
) : FollowTarget(behavior, dimensions), FollowTargetFlow {
    private val pose: Pose
        get() = anchorSpace.getPose(Space.ACTIVITY)

    /**
     * A Flow that emits the latest pose updates whenever the underlying [AnchorSpace] is updated by
     * the system's perception stack.
     */
    override val poseUpdates: Flow<Pose> = callbackFlow {
        // Send the initial pose immediately upon collection.
        trySend(element = pose)

        val updateListener = Runnable { trySend(pose) }
        anchorSpace.addOriginChangedListener(updateListener)

        // Unregister the listener when the collector cancels or finishes.
        awaitClose {
            try {
                if (!anchorSpace.isDisposed) {
                    anchorSpace.removeOriginChangedListener(updateListener)
                }
            } catch (_: RuntimeException) {
                // The anchor was disposed before we could remove the listener.
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnchorTarget) return false

        if (anchorSpace != other.anchorSpace) return false
        if (behavior != other.behavior) return false
        if (dimensions != other.dimensions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = anchorSpace.hashCode()
        result = 31 * result + behavior.hashCode()
        result = 31 * result + dimensions.hashCode()
        return result
    }
}
