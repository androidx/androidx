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

package androidx.xr.scenecore.projected.testing

import androidx.xr.scenecore.projected.IProjectedNode
import androidx.xr.scenecore.projected.IProjectedSceneCoreService
import androidx.xr.scenecore.projected.ISceneResultCallback
import androidx.xr.scenecore.projected.ISpatialStateChangedCallback
import androidx.xr.scenecore.projected.ProjectedNodeTransaction
import androidx.xr.scenecore.projected.SceneResult

public class FakeProjectedSceneCoreService : IProjectedSceneCoreService.Stub() {

    public val transactionList: MutableList<List<ProjectedNodeTransaction?>?> = mutableListOf()
    public val createdNodes: MutableList<IProjectedNode> = mutableListOf()
    public var spatialStateChangedCallbackReceived: ISpatialStateChangedCallback? = null

    // Helper function to get all transactions based on a predicate
    public fun getTransactions(
        node: IProjectedNode? = null,
        predicate: (ProjectedNodeTransaction) -> Boolean = { true },
    ): List<ProjectedNodeTransaction> {
        return this.transactionList
            .flatMap { it ?: emptyList() } // Flatten the list of lists and handle null inner lists
            .filterNotNull()
            .filter { ((node == null) || (it.node == node)) && predicate(it) }
    }

    override fun createNode(): IProjectedNode? {
        val node = FakeProjectedNode()
        createdNodes.add(node)
        return node
    }

    override fun attachSpatialScene(
        sceneNode: IProjectedNode?,
        resultCallback: ISceneResultCallback?,
    ) {
        val resultStatus = SceneResult()
        resultStatus.result = SceneResult.ResultType.SUCCESS
        resultCallback?.onSceneResult(resultStatus)
    }

    override fun setSpatialStateChangedCallback(callback: ISpatialStateChangedCallback?) {
        spatialStateChangedCallbackReceived = callback
    }

    override fun clearSpatialStateChangedCallback() {
        spatialStateChangedCallbackReceived = null
    }

    override fun applyNodeTransactions(transactions: List<ProjectedNodeTransaction?>?) {
        transactionList.add(transactions)
    }

    override fun getInterfaceVersion(): Int {
        return 0
    }
}
