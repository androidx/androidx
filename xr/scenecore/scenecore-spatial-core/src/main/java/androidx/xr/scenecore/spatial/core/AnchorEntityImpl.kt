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

import android.annotation.SuppressLint
import android.content.Context
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.runtime.ExportableAnchor
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpaceValue
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import java.util.concurrent.ScheduledExecutorService

/**
 * Implementation of AnchorEntity.
 *
 * This entity creates trackable anchors in space.
 */
@SuppressLint(
    "NewApi",
    "WrongConstant",
) // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
internal class AnchorEntityImpl(
    context: Context,
    node: Node,
    private val activitySpace: ActivitySpaceImpl,
    extensions: XrExtensions,
    entityManager: EntityManager,
    executor: ScheduledExecutorService,
) : SystemSpaceEntityImpl(context, node, extensions, entityManager, executor), AnchorEntity {
    private val openXrScenePoseHelper = OpenXrScenePoseHelper(activitySpace)
    private var onStateChangedListener: AnchorEntity.OnStateChangedListener? = null
    private var _state: @AnchorEntity.State Int = AnchorEntity.State.UNANCHORED
    override val state: @AnchorEntity.State Int
        get() {
            synchronized(this) {
                return _state
            }
        }

    init {
        extensions.createNodeTransaction().use { transaction ->
            transaction.setName(node, ANCHOR_NODE_NAME).apply()
        }
    }

    override fun setAnchor(anchor: Anchor): Boolean {
        synchronized(this) {
            if (_state == AnchorEntity.State.ERROR || anchor.runtimeAnchor !is ExportableAnchor) {
                return false
            }
            val exportableAnchor = anchor.runtimeAnchor as ExportableAnchor
            mExtensions.createNodeTransaction().use { transaction ->
                // Attach to the root CPM node. This will enable the anchored content to be visible.
                // Note that the parent of the Entity is null, but the CPM Node is still attached.
                transaction
                    .setParent(mNode, activitySpace.mNode)
                    .setAnchorId(mNode, exportableAnchor.anchorToken)
                    .apply()
            }
            updateState(AnchorEntity.State.ANCHORED)
            return true
        }
    }

    private fun updateState(newState: @AnchorEntity.State Int) {
        synchronized(this) {
            if (newState != _state) {
                _state = newState
                onStateChangedListener?.onStateChanged(_state)
            }
        }
    }

    override fun setOnStateChangedListener(
        onStateChangedListener: AnchorEntity.OnStateChangedListener
    ) {
        this.onStateChangedListener = onStateChangedListener
        mExecutor.execute { onStateChangedListener.onStateChanged(_state) }
    }

    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        return when (relativeTo) {
            Space.PARENT ->
                throw UnsupportedOperationException(
                    "AnchorEntity is a root space and it does not have a parent."
                )

            Space.ACTIVITY -> poseInActivitySpace
            Space.REAL_WORLD -> getPoseInPerceptionSpace()
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        throw UnsupportedOperationException("Cannot set 'pose' on an AnchorEntity.")
    }

    override fun setScale(scale: Vector3, @SpaceValue relativeTo: Int) {
        throw UnsupportedOperationException("Cannot set 'scale' on an AnchorEntity.")
    }

    override fun getScale(@SpaceValue relativeTo: Int): Vector3 {
        return when (relativeTo) {
            Space.PARENT ->
                throw UnsupportedOperationException(
                    "AnchorEntity is a root space and it does not have a parent."
                )

            Space.ACTIVITY -> activitySpaceScale
            Space.REAL_WORLD -> super.worldSpaceScale
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override val poseInActivitySpace: Pose
        get() {
            synchronized(this) {
                if (_state != AnchorEntity.State.ANCHORED) {
                    return Pose()
                }
                return openXrScenePoseHelper.getPoseInActivitySpace(poseInOpenXrReferenceSpace)
            }
        }

    fun getPoseInPerceptionSpace(): Pose {
        val perceptionSpaceScenePose =
            mEntityManager.getSystemSpaceActivityPoseOfType(PerceptionSpaceScenePose::class.java)[0]
        return transformPoseTo(Pose(), perceptionSpaceScenePose)
    }

    override val activitySpacePose: Pose
        get() = openXrScenePoseHelper.getActivitySpacePose(poseInOpenXrReferenceSpace)

    override val activitySpaceScale: Vector3
        get() = openXrScenePoseHelper.getActivitySpaceScale(worldSpaceScale)

    override var parent: Entity? = null
        set(_) {
            throw UnsupportedOperationException("Cannot set 'parent' on an  AnchorEntity.")
        }

    override fun dispose() {
        synchronized(this) {
            // Return early if it is already in the error state.
            if (_state == AnchorEntity.State.ERROR) {
                return
            }
            updateState(AnchorEntity.State.ERROR)
        }

        mExtensions.createNodeTransaction().use { transaction ->
            transaction.setAnchorId(mNode, null).setParent(mNode, null).apply()
        }
        super.dispose()
    }

    companion object {
        const val ANCHOR_NODE_NAME: String = "AnchorNode"

        @JvmStatic
        fun create(
            context: Context,
            node: Node,
            activitySpace: ActivitySpaceImpl,
            extensions: XrExtensions,
            entityManager: EntityManager,
            executor: ScheduledExecutorService,
        ): AnchorEntityImpl {
            return AnchorEntityImpl(
                context,
                node,
                activitySpace,
                extensions,
                entityManager,
                executor,
            )
        }
    }
}
