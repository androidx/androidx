/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.scenecore.spatial.rendering

import androidx.xr.runtime.NodeHolder
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.runtime.RenderingFeature
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.androidxr.splitengine.SubspaceNode

internal abstract class BaseRenderingFeature(
    protected val impressApi: ImpressApi,
    protected val splitEngineSubspaceManager: SplitEngineSubspaceManager,
    protected val extensions: XrExtensions,
) : RenderingFeature {

    internal val node: Node = extensions.createNode()

    // The SubspaceNode isn't final so that we can support setting it to null in dispose(), while
    // still allowing the application to hold a reference to this Entity.
    internal var subspace: SubspaceNode? = null

    override fun getNodeHolder(): NodeHolder<*> {
        return NodeHolder(node, Node::class.java)
    }

    override fun getSubspaceNodeHolder(): NodeHolder<*>? {
        return subspace?.let { NodeHolder(it.subspaceNode, Node::class.java) }
    }

    protected fun bindImpressNodeToSubspace(subspaceNamePrefix: String, impressNode: ImpressNode) {
        // System will only render Impress nodes that are parented by this subspace node.
        val subspaceImpressNode = impressApi.createImpressNode()
        val fullSubspaceName = subspaceNamePrefix + subspaceImpressNode.handle

        subspace =
            splitEngineSubspaceManager.createSubspace(fullSubspaceName, subspaceImpressNode.handle)

        // If splitEngineSubspaceManager is mock version, createSubspace might return null.
        subspace?.let { validSubspace ->
            extensions.createNodeTransaction().use { transaction ->
                // Make the Entity node a parent of the subspace node.
                transaction.setParent(validSubspace.subspaceNode, node).apply()
            }
        }

        // The CPM node hierarchy is: Entity CPM node --- parent of ---> Subspace CPM node.
        // The Impress node hierarchy is: Subspace Impress node --- parent of ---> Entity Impress
        // node.
        try {
            impressApi.setImpressNodeParent(impressNode, subspaceImpressNode)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(e)
        }
    }

    override fun dispose() {
        subspace?.let {
            // The subspace impress node will be destroyed when the subspace is deleted.
            splitEngineSubspaceManager.deleteSubspace(it.subspaceId)
            subspace = null
        }
    }
}
