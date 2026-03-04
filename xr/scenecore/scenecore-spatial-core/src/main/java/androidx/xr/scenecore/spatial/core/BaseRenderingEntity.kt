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
import androidx.xr.runtime.TypeHolder.Companion.assertGetValue
import androidx.xr.scenecore.runtime.RenderingFeature
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import java.util.concurrent.ScheduledExecutorService

/**
 * A base class for entities that rely on a [RenderingFeature] for rendering functionality.
 *
 * This class provides common logic for managing the lifecycle of a [RenderingFeature], which
 * supplies the underlying rendering implementation and provides a [Node] for initialization.
 */
internal abstract class BaseRenderingEntity(
    context: Context?,
    private val renderingFeature: RenderingFeature,
    xrExtensions: XrExtensions,
    entityManager: EntityManager,
    executor: ScheduledExecutorService,
) :
    AndroidXrEntity(
        context,
        assertGetValue(renderingFeature.getNodeHolder(), Node::class.java),
        xrExtensions,
        entityManager,
        executor,
    ) {
    private val subspaceNode: Node? =
        renderingFeature.getSubspaceNodeHolder()?.let { subspaceNodeHolder ->
            // Establish an alias from the primary node to the subspace node. This is crucial for
            // ensuring that input events, such as hit tests, which may be reported against the
            // subspace node, can be correctly resolved back to this entity. Without this alias,
            // getEntityForNode(subspaceNode) would fail.
            assertGetValue(subspaceNodeHolder, Node::class.java).also {
                entityManager.setEntityForNode(it, this)
            }
        }

    override fun dispose() {
        subspaceNode?.let { mEntityManager.removeEntityForNode(it) }
        renderingFeature.dispose()
        super.dispose()
    }
}
