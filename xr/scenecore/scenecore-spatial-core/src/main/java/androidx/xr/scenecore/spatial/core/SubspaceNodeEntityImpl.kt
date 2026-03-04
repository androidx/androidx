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

import android.content.Context
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpaceValue
import androidx.xr.scenecore.runtime.SubspaceNodeEntity
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import java.util.concurrent.ScheduledExecutorService

/**
 * Represents an entity that manages a subspace node.
 *
 * This class manages the pose, scale, alpha, size and visibility of the subspace node enclosed by
 * this entity, and allows the entity to be user interactable. This entity doesn't have access to
 * underlying impress nodes like the [SurfaceEntityImpl], so it treats the subspace node as sibling
 * disjointed from scene graph and applies all transformations to it explicitly.
 */
internal class SubspaceNodeEntityImpl(
    context: Context,
    xrExtensions: XrExtensions,
    private val subspaceNode: Node,
    entityManager: EntityManager,
    executor: ScheduledExecutorService,
) :
    AndroidXrEntity(context, xrExtensions.createNode(), xrExtensions, entityManager, executor),
    SubspaceNodeEntity {

    private var internalScale = Vector3(1f, 1f, 1f)

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        super<AndroidXrEntity>.setPose(pose, relativeTo)
        mExtensions.createNodeTransaction().use { transaction ->
            transaction
                .setPosition(
                    subspaceNode,
                    pose.translation.x,
                    pose.translation.y,
                    pose.translation.z,
                )
                .setOrientation(
                    subspaceNode,
                    pose.rotation.x,
                    pose.rotation.y,
                    pose.rotation.z,
                    pose.rotation.w,
                )
                .apply()
        }
    }

    override fun setScale(scale: Vector3, @SpaceValue relativeTo: Int) {
        super<AndroidXrEntity>.setScale(scale, relativeTo)
        internalScale = super<AndroidXrEntity>.getScale(Space.ACTIVITY)
        val scaledSize =
            Dimensions(
                size.width * internalScale.x,
                size.height * internalScale.y,
                size.depth * internalScale.z,
            )
        mExtensions.createNodeTransaction().use { transaction ->
            transaction
                .setScale(subspaceNode, scaledSize.width, scaledSize.height, scaledSize.depth)
                .apply()
        }
    }

    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
        mExtensions.createNodeTransaction().use { transaction ->
            // Get the final clamped alpha value from the inheritance chain (from BaseEntity).
            // This is then applied to mSubspaceNode to ensure its alpha is consistent with mNode's.
            transaction.setAlpha(subspaceNode, super<AndroidXrEntity>.getAlpha()).apply()
        }
    }

    override var size = Dimensions(0f, 0f, 0f)
        set(size) {
            field = size
            mExtensions.createNodeTransaction().use { transaction ->
                transaction
                    .setScale(
                        subspaceNode,
                        size.width * internalScale.x,
                        size.height * internalScale.y,
                        size.depth * internalScale.z,
                    )
                    .apply()
            }
        }

    override fun setHidden(hidden: Boolean) {
        super.setHidden(hidden)
        mExtensions.createNodeTransaction().use { transaction ->
            transaction.setVisibility(subspaceNode, !hidden).apply()
        }
    }
}
