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

import androidx.xr.runtime.TypeHolder
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.NodeRepository
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.androidxr.splitengine.SubspaceNode
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class BaseRenderingFeatureTest {

    private val nodeRepository = NodeRepository.getInstance()
    private val xrExtensions = XrExtensionsProvider.getXrExtensions()!!
    private val fakeImpressApi = FakeImpressApiImpl()
    private val splitEngineSubspaceManager = Mockito.mock(SplitEngineSubspaceManager::class.java)
    private val subspaceNode = xrExtensions.createNode()
    private val expectedSubspace = SubspaceNode(SUBSPACE_ID, subspaceNode)
    private lateinit var renderingFeature: BaseRenderingFeatureImpl

    // Internal implementation for test only
    internal class BaseRenderingFeatureImpl(
        impressApi: ImpressApi,
        splitEngineSubspaceManager: SplitEngineSubspaceManager,
        extensions: XrExtensions,
    ) : BaseRenderingFeature(impressApi, splitEngineSubspaceManager, extensions) {

        // Expose the function to test it.
        fun bindImpressNodeToSubspace(impressNode: ImpressNode) {
            bindImpressNodeToSubspace("test_node_", impressNode)
        }
    }

    @Before
    fun setUp() {
        `when`(splitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
            .thenReturn(expectedSubspace)
        ShadowXrExtensions.extract(xrExtensions)
            .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE)

        renderingFeature =
            BaseRenderingFeatureImpl(fakeImpressApi, splitEngineSubspaceManager, xrExtensions)
    }

    @After
    fun tearDown() {
        renderingFeature.dispose()
    }

    @Test
    fun getNodeHolder_hasCorrectHierarchy() {
        val nodeHolder = renderingFeature.getNodeHolder()
        val node = TypeHolder.assertGetValue(nodeHolder, Node::class.java)
        val impressNode = fakeImpressApi.createImpressNode()

        renderingFeature.bindImpressNodeToSubspace(impressNode)

        // The CPM node hierarchy is: Entity CPM node --- parent of ---> Subspace CPM node.
        assertThat(nodeRepository.getParent(subspaceNode)).isEqualTo(node)
        // The Impress node hierarchy is: Subspace Impress node --- parent of ---> Entity Impress
        // node. The subspace impress node is not recorded anywhere. We can only know that it has a
        // parent.
        assertThat(fakeImpressApi.impressNodeHasParent(impressNode)).isTrue()
    }

    companion object {
        private const val OPEN_XR_REFERENCE_SPACE_TYPE = 1
        private const val SUBSPACE_ID = 5
    }
}
