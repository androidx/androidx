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

import androidx.xr.scenecore.projected.ProjectedNodeTransaction
import androidx.xr.scenecore.projected.SceneResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class FakeProjectedSceneCoreServiceTest {

    private val service = FakeProjectedSceneCoreService()

    @Test
    fun createNode_returnsNewNodeAndAddsToCreatedNodes() {
        val node = service.createNode()
        assertThat(node).isInstanceOf(FakeProjectedNode::class.java)
        assertThat(service.createdNodes).containsExactly(node)
    }

    @Test
    fun attachSpatialScene_callsOnSceneResultWithSuccess() {
        val callback = FakeSceneResultCallback()
        val node = FakeProjectedNode()
        service.attachSpatialScene(node, callback)
        assertThat(callback.lastSceneResult?.result).isEqualTo(SceneResult.ResultType.SUCCESS)
    }

    @Test
    fun setSpatialStateChangedCallback_setsCallback() {
        val callback = FakeSpatialStateChangedCallback()
        service.setSpatialStateChangedCallback(callback)
        assertThat(service.spatialStateChangedCallbackReceived).isEqualTo(callback)
    }

    @Test
    fun clearSpatialStateChangedCallback_clearsCallback() {
        val callback = FakeSpatialStateChangedCallback()
        service.setSpatialStateChangedCallback(callback)
        service.clearSpatialStateChangedCallback()
        assertThat(service.spatialStateChangedCallbackReceived).isNull()
    }

    @Test
    fun applyNodeTransactions_addsToList() {
        val transactions = listOf(ProjectedNodeTransaction())
        service.applyNodeTransactions(transactions)
        assertThat(service.transactionList).containsExactly(transactions)
    }

    @Test
    fun getTransactions_filtersByNode() {
        val node1 = FakeProjectedNode()
        val node2 = FakeProjectedNode()
        val tx1 = ProjectedNodeTransaction().apply { node = node1 }
        val tx2 = ProjectedNodeTransaction().apply { node = node2 }
        service.applyNodeTransactions(listOf(tx1, tx2))

        val filtered = service.getTransactions(node1) { true }
        assertThat(filtered).containsExactly(tx1)
    }

    @Test
    fun getTransactions_filtersByPredicate() {
        val tx1 = ProjectedNodeTransaction().apply { name = "tx1" }
        val tx2 = ProjectedNodeTransaction().apply { name = "tx2" }
        service.applyNodeTransactions(listOf(tx1, tx2))

        val filtered = service.getTransactions(null) { it.name == "tx1" }
        assertThat(filtered).containsExactly(tx1)
    }
}
