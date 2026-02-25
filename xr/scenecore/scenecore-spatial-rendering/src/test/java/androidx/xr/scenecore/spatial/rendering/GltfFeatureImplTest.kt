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

package androidx.xr.scenecore.spatial.rendering

import androidx.concurrent.futures.ResolvableFuture
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl
import androidx.xr.scenecore.impl.impress.GltfModel
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.impl.impress.WaterMaterial
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.node.Node
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.androidxr.splitengine.SubspaceNode
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class GltfFeatureImplTest {
    private val xrExtensions = XrExtensionsProvider.getXrExtensions()!!
    private val executor = FakeScheduledExecutorService()
    private val mockImpressApi = mock(ImpressApi::class.java)
    private val fakeImpressApi = FakeImpressApiImpl()
    private val splitEngineSubspaceManager = mock(SplitEngineSubspaceManager::class.java)
    private val mockRenderer = mock(ImpSplitEngineRenderer::class.java)
    private lateinit var modelImpressNode: ImpressNode

    private val subspaceNode: Node = xrExtensions.createNode()
    private val expectedSubspace = SubspaceNode(SUBSPACE_ID, subspaceNode)

    private lateinit var gltfFeature: GltfFeature
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        `when`(splitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
            .thenReturn(expectedSubspace)

        assertThat(xrExtensions).isNotNull()
        ShadowXrExtensions.extract(xrExtensions)
            .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE)
        gltfFeature = createGltfFeature()
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        gltfFeature.dispose()
        Dispatchers.resetMain()
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun createGltfFeature(): GltfFeature {
        val model: GltfModel = runBlocking { fakeImpressApi.loadGltfAsset("FakeGltfAsset.glb") }
        val modelToken = model.nativeHandle
        modelImpressNode = fakeImpressApi.instanceGltfModel(modelToken)
        `when`(mockImpressApi.createImpressNode()).thenReturn(fakeImpressApi.createImpressNode())
        `when`(mockImpressApi.instanceGltfModel(modelToken)).thenReturn(modelImpressNode)
        `when`(mockImpressApi.getGltfModelBoundingBox(modelImpressNode))
            .thenReturn(fakeImpressApi.getGltfModelBoundingBox(modelImpressNode))
        `when`(mockImpressApi.getImpressNodeChildCount(anyNotNull())).thenAnswer {
            fakeImpressApi.getImpressNodeChildCount(it.getArgument(0))
        }
        `when`(mockImpressApi.getImpressNodeChildAt(anyNotNull(), anyInt())).thenAnswer {
            fakeImpressApi.getImpressNodeChildAt(it.getArgument(0), it.getArgument(1))
        }
        `when`(mockImpressApi.getImpressNodeName(anyNotNull())).thenAnswer {
            fakeImpressApi.getImpressNodeName(it.getArgument(0))
        }

        return GltfFeatureImpl(
            model,
            mockImpressApi,
            splitEngineSubspaceManager,
            xrExtensions,
            mockRenderer,
        )
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun createWaterMaterial(isAlphaMapVersion: Boolean): MaterialResource {
        val materialResourceFuture = ResolvableFuture.create<MaterialResource>()
        val material: WaterMaterial = runBlocking {
            fakeImpressApi.createWaterMaterial(isAlphaMapVersion)
        }

        materialResourceFuture.set(material)

        return materialResourceFuture.get()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startAnimation_startsAnimation() =
        runTest(testDispatcher) {
            val animationName = "test_animation"

            runBlocking {
                Mockito.doAnswer {
                        assertThat(gltfFeature.animationState)
                            .isEqualTo(GltfEntity.AnimationState.PLAYING)
                        null
                    }
                    .`when`(mockImpressApi)
                    .animateGltfModel(modelImpressNode, animationName, true)
            }
            gltfFeature.startAnimation(/* looping= */ true, animationName, executor)
            executor.runAll()

            runBlocking {
                verify(mockImpressApi).animateGltfModel(modelImpressNode, animationName, true)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startAnimation_afterImpressCallback_stopsAnimation() =
        runTest(testDispatcher) {
            val animationName = "test_animation"

            runBlocking {
                Mockito.doAnswer {
                        assertThat(gltfFeature.animationState)
                            .isEqualTo(GltfEntity.AnimationState.PLAYING)
                        null
                    }
                    .`when`(mockImpressApi)
                    .animateGltfModel(modelImpressNode, animationName, false)
            }
            gltfFeature.startAnimation(/* looping= */ false, animationName, executor)
            assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
            executor.runAll()
            assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.STOPPED)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun stopAnimation_stopsAnimation() =
        runTest(testDispatcher) {
            val animationName = "test_animation"

            runBlocking {
                Mockito.doAnswer { COROUTINE_SUSPENDED }
                    .`when`(mockImpressApi)
                    .animateGltfModel(modelImpressNode, animationName, true)
            }
            gltfFeature.startAnimation(
                /* looping= */ true,
                animationName,
                testDispatcher.asExecutor(),
            )
            executor.runAll()

            assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)

            // TODO(b/461899032): Add more robust logic to fake Impress APIs
            gltfFeature.stopAnimation()

            assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.STOPPED)
            verify(mockImpressApi).stopGltfModelAnimation(modelImpressNode)
        }

    @Test
    fun pauseAnimation_pauseAnimation() = runBlocking {
        val animationName = "test_animation"
        `when`(mockImpressApi.animateGltfModel(modelImpressNode, animationName, true))
            .thenReturn(fakeImpressApi.animateGltfModel(modelImpressNode, animationName, true))
        gltfFeature.startAnimation(/* looping= */ true, animationName, executor)

        assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)

        gltfFeature.pauseAnimation()
        fakeImpressApi.toggleGltfModelAnimation(modelImpressNode, false)

        assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.PAUSED)
        verify(mockImpressApi).toggleGltfModelAnimation(modelImpressNode, false)
    }

    @Test
    fun resumeAnimation_resumeAnimation() = runBlocking {
        val animationName = "test_animation"
        `when`(mockImpressApi.animateGltfModel(modelImpressNode, animationName, true))
            .thenReturn(fakeImpressApi.animateGltfModel(modelImpressNode, animationName, true))
        gltfFeature.startAnimation(/* looping= */ true, animationName, executor)

        assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)

        gltfFeature.pauseAnimation()
        fakeImpressApi.toggleGltfModelAnimation(modelImpressNode, false)

        assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.PAUSED)
        verify(mockImpressApi).toggleGltfModelAnimation(modelImpressNode, false)

        gltfFeature.resumeAnimation()
        fakeImpressApi.toggleGltfModelAnimation(modelImpressNode, true)

        assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
        verify(mockImpressApi).toggleGltfModelAnimation(modelImpressNode, true)
    }

    @Test
    @Throws(Exception::class)
    fun animationStateListener_isTriggeredOnAnimationStateChanges() {
        val animationName = "test_animation"

        runBlocking {
            Mockito.doAnswer {
                    assertThat(gltfFeature.animationState)
                        .isEqualTo(GltfEntity.AnimationState.PLAYING)
                    null
                }
                .`when`(mockImpressApi)
                .animateGltfModel(modelImpressNode, animationName, /* isAlphaMapVersion= */ true)
        }

        val latestValue = AtomicReference(GltfEntity.AnimationState.STOPPED)
        gltfFeature.addAnimationStateListener(Runnable::run, latestValue::set)
        gltfFeature.startAnimation(/* looping= */ true, animationName, executor)
        executor.runAll()

        gltfFeature.stopAnimation()

        assertThat(latestValue.get()).isEqualTo(GltfEntity.AnimationState.STOPPED)
    }

    @Test
    fun addOnBoundsUpdateListener_setsFrameListener() {
        val listener = Consumer<BoundingBox> {}
        gltfFeature.addOnBoundsUpdateListener(listener)
        verify(mockRenderer).frameListener = Mockito.any()
    }

    @Test
    fun removeOnBoundsUpdateListener_clearsFrameListener() {
        val listener = Consumer<BoundingBox> {}
        gltfFeature.addOnBoundsUpdateListener(listener)
        gltfFeature.removeOnBoundsUpdateListener(listener)
        verify(mockRenderer).frameListener = null
    }

    @Test
    @Throws(Exception::class)
    fun dispose_clearsOverridesAndDeletesSubspace() {
        val childNode = fakeImpressApi.createImpressNode()
        fakeImpressApi.setImpressNodeParent(childNode, modelImpressNode)
        val nodeFeature = gltfFeature.nodes.first()
        val material = createWaterMaterial(false)
        nodeFeature.setMaterialOverride(material, 0)

        gltfFeature.dispose()

        verify(mockImpressApi, atLeastOnce())
            .clearGltfModelNodeMaterialOverride(eq(childNode) ?: childNode, anyInt())
        verify(splitEngineSubspaceManager).deleteSubspace(SUBSPACE_ID)
        verify(mockRenderer).frameListener = null
    }

    @Test
    fun nodes_returnsFlattenedListOfNodesFromImpress() {
        val node1 = fakeImpressApi.createImpressNode()
        val node2 = fakeImpressApi.createImpressNode()
        fakeImpressApi.setImpressNodeParent(node1, modelImpressNode)
        fakeImpressApi.setImpressNodeParent(node2, modelImpressNode)

        val resultNodes = gltfFeature.nodes

        assertThat(resultNodes).hasSize(2)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNotNull(): T {
        val mockitoAny = any<T>()
        return mockitoAny ?: (null as T)
    }

    companion object {
        private const val OPEN_XR_REFERENCE_SPACE_TYPE = 1
        private const val SUBSPACE_ID = 5
    }
}
