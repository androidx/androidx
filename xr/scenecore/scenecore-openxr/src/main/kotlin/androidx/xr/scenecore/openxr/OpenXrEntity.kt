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
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.CleanupAction
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpaceValue
import androidx.xr.scenecore.runtime.impl.BaseEntity
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService

/**
 * Implementation of a JXR SceneCore Entity that wraps an OpenXR scene entity.
 *
 * This should not be created on its own but should be inherited by objects that need to wrap an
 * OpenXR scene entity handle.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class OpenXrEntity
internal constructor(
    context: Context?,
    entityHandle: Long,
    @JvmField internal val nativeWrapper: SceneCoreOpenXrNative,
    @JvmField internal val sceneNodeRegistry: OpenXrSceneNodeRegistry,
    @JvmField internal val executor: ScheduledExecutorService,
) : BaseEntity(context), Entity {

    public open var entityHandle: Long = entityHandle
        internal set

    init {
        bindEntityHandle(entityHandle)
    }

    internal fun bindEntityHandle(handle: Long) {
        this.entityHandle = handle
        if (handle != INVALID_HANDLE) {
            sceneNodeRegistry.setEntityForNode(handle, this)
            registerCleanup(
                executor,
                OpenXrEntityCleanupAction(handle, nativeWrapper, sceneNodeRegistry),
            )
        }
    }

    private class OpenXrEntityCleanupAction(
        private val entityHandle: Long,
        private val nativeWrapper: SceneCoreOpenXrNative,
        private val sceneNodeRegistry: OpenXrSceneNodeRegistry,
    ) :
        CleanupAction({
            sceneNodeRegistry.removeEntityForNode(entityHandle)
            if (nativeWrapper.nativeScenecore != INVALID_HANDLE && entityHandle != INVALID_HANDLE) {
                nativeWrapper.destroySceneEntity(entityHandle)
            }
        })

    override var parent: Entity?
        get() = super<BaseEntity>.parent
        set(newParent) {
            require(newParent != this) { "Cannot set an entity as its own parent." }
            if (newParent != null && newParent !is OpenXrEntity) {
                throw IllegalArgumentException(
                    "Cannot set parent with a non-OpenXrEntity parent: $newParent"
                )
            }
            super<BaseEntity>.parent = newParent

            if (entityHandle != INVALID_HANDLE) {
                OpenXrTransaction(nativeWrapper, nativeWrapper.createSceneTransaction()).use { tx ->
                    if (tx.isAvailable) {
                        tx.setParent(
                            entityHandle,
                            (newParent as? OpenXrEntity)?.entityHandle ?: INVALID_HANDLE,
                        )
                        tx.commit()
                    }
                }
            }
        }

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        val targetPose =
            when (relativeTo) {
                Space.PARENT -> pose
                Space.ACTIVITY -> getLocalPoseForActivitySpacePose(pose)
                Space.REAL_WORLD -> getLocalPoseForPerceptionSpacePose(pose)
                else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
            }
        super<BaseEntity>.setPose(targetPose, Space.PARENT)

        if (entityHandle != INVALID_HANDLE) {
            OpenXrTransaction(nativeWrapper, nativeWrapper.createSceneTransaction()).use { tx ->
                if (tx.isAvailable) {
                    tx.setTransform(
                        entityHandle,
                        targetPose,
                        super<BaseEntity>.getScale(Space.PARENT),
                    )
                    tx.commit()
                }
            }
        }
    }

    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        return when (relativeTo) {
            Space.PARENT -> super<BaseEntity>.getPose(Space.PARENT)
            Space.ACTIVITY -> activitySpacePose
            Space.REAL_WORLD -> poseInPerceptionSpace
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setScale(scale: Vector3, @SpaceValue relativeTo: Int) {
        super<BaseEntity>.setScale(scale, relativeTo)
        val localScale = super<BaseEntity>.getScale(Space.PARENT)
        if (entityHandle != INVALID_HANDLE) {
            OpenXrTransaction(nativeWrapper, nativeWrapper.createSceneTransaction()).use { tx ->
                if (tx.isAvailable) {
                    tx.setTransform(
                        entityHandle,
                        super<BaseEntity>.getPose(Space.PARENT),
                        localScale,
                    )
                    tx.commit()
                }
            }
        }
    }

    // TODO: b/535276460 - Implement native alpha transparency and hierarchical alpha propagation
    // down scene entities.
    override fun setAlpha(alpha: Float): Unit = TODO("Not yet implemented for OpenXR")

    // TODO: b/535276460 - Implement entity visibility control and transaction-based hide/show
    // propagation.
    override fun setHidden(hidden: Boolean): Unit = TODO("Not yet implemented for OpenXR")

    // TODO: b/535276460 - Support InteractableComponent, PointerCaptureComponent, and spatial input
    // event listeners.
    override fun addInputEventListener(executor: Executor?, listener: InputEventListener): Unit =
        TODO("Not yet implemented for OpenXR")

    override fun removeInputEventListener(listener: InputEventListener): Unit =
        TODO("Not yet implemented for OpenXR")

    // TODO: b/535276460 - Implement 3D-to-2D spatial raycast hit testing against entity bounds.
    override suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        hitTestFilter: Int,
    ): HitTestResult = TODO("Not yet implemented for OpenXR")

    override fun dispose() {
        entityHandle = INVALID_HANDLE
        super<BaseEntity>.dispose()
    }

    internal val poseInPerceptionSpace: Pose
        get() {
            val perceptionSpaceScenePose =
                sceneNodeRegistry
                    .getSystemSpaceScenePoseOfType(PerceptionSpaceScenePose::class.java)
                    .firstOrNull()
                    ?: throw IllegalStateException(
                        "Cannot get pose in Real World Space without PerceptionSpaceScenePose"
                    )
            return transformPoseTo(Pose(), perceptionSpaceScenePose)
        }

    private fun getLocalPoseForActivitySpacePose(pose: Pose): Pose {
        if (parent !is OpenXrEntity) {
            throw IllegalStateException(
                "Cannot get pose in Activity Space with a non-OpenXrEntity parent"
            )
        }
        val xrParent = parent as OpenXrEntity
        val activitySpace =
            sceneNodeRegistry.getSystemSpaceScenePoseOfType(ActivitySpace::class.java).firstOrNull()
                ?: throw IllegalStateException(
                    "Cannot get pose in Activity Space without ActivitySpace"
                )
        return activitySpace.transformPoseTo(pose, xrParent)
    }

    private fun getLocalPoseForPerceptionSpacePose(pose: Pose): Pose {
        if (parent !is OpenXrEntity) {
            throw IllegalStateException(
                "Cannot get pose in Perception Space with a non-OpenXrEntity parent"
            )
        }
        val xrParent = parent as OpenXrEntity
        val perceptionSpaceScenePose =
            sceneNodeRegistry
                .getSystemSpaceScenePoseOfType(PerceptionSpaceScenePose::class.java)
                .firstOrNull()
                ?: throw IllegalStateException(
                    "Cannot get pose in Perception Space without PerceptionSpaceScenePose"
                )
        return perceptionSpaceScenePose.transformPoseTo(pose, xrParent)
    }
}
