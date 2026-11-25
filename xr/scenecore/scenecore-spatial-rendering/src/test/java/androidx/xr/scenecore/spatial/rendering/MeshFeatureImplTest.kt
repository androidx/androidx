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

package androidx.xr.scenecore.spatial.rendering

import android.app.Activity
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertRotation
import androidx.xr.scenecore.runtime.GeometryAffordanceState
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.ReformAffordanceFlag
import androidx.xr.scenecore.spatial.core.SpatialSceneRuntime
import androidx.xr.scenecore.spatial.rendering.impress.FakeImpressApiImpl
import androidx.xr.scenecore.spatial.rendering.impress.ImpressApi
import androidx.xr.scenecore.spatial.rendering.impress.ImpressNode
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.ShadowInputEvent
import com.android.extensions.xr.node.ShadowNode
import com.android.extensions.xr.node.Vec3
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.androidxr.splitengine.SubspaceNode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class MeshFeatureImplTest {
    private val xrExtensions = XrExtensions()
    private val executor = FakeScheduledExecutorService()
    private val mockImpressApi = mock(ImpressApi::class.java)
    private val fakeImpressApi = FakeImpressApiImpl()
    private val splitEngineSubspaceManager = mock(SplitEngineSubspaceManager::class.java)
    private val subspaceNode: Node = xrExtensions.createNode()
    private val meshImpressNode: ImpressNode = fakeImpressApi.createImpressNode()
    private lateinit var meshFeature: MeshFeatureImpl

    @Before
    fun setUp() {
        `when`(splitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
            .thenReturn(SubspaceNode(SUBSPACE_ID, subspaceNode))
        `when`(mockImpressApi.createImpressNode()).thenReturn(fakeImpressApi.createImpressNode())
        ShadowXrExtensions.extract(xrExtensions)
            .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        meshFeature =
            MeshFeatureImpl(
                mockImpressApi,
                splitEngineSubspaceManager,
                xrExtensions,
                meshImpressNode,
                BoundingBox.fromMinMax(Vector3.Zero, Vector3.One),
            )
    }

    @After
    fun tearDown() {
        meshFeature.dispose()
        Dispatchers.resetMain()
    }

    @Test
    fun setReformAffordanceEnabled_forwardsRecommendedAffordanceRotationOnInputEvent() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val spatialRuntime = SpatialSceneRuntime.create(activity, executor)
        val entity =
            spatialRuntime.createMeshEntity(
                meshFeature,
                Pose.Identity,
                spatialRuntime.activitySpace,
            )
        val expectedRotation = Quaternion.fromAxisAngle(Vector3.Up, 60f)
        val expectedScale = Vector3(1.5f, 1.5f, 1.5f)
        `when`(mockImpressApi.getReformAffordanceState(meshImpressNode)).thenReturn(3) // ROTATION
        `when`(mockImpressApi.getRecommendedAffordanceTransform(meshImpressNode))
            .thenReturn(Matrix4.fromTrs(Vector3.Zero, expectedRotation, expectedScale))

        var listenerObservedRotation: Quaternion? = null
        entity.addInputEventListener(
            executor,
            InputEventListener { listenerObservedRotation = entity.recommendedAffordanceRotation },
        )

        meshFeature.setReformAffordanceEnabled(entity, true, ReformAffordanceFlag.ROTATABLE)
        val shadowSubspaceNode = ShadowNode.extract(subspaceNode)
        val xrInputEvent = ShadowInputEvent.create(Vec3(0f, 0f, 0f), Vec3(0f, 0f, -1f))
        shadowSubspaceNode.inputExecutor.execute {
            shadowSubspaceNode.inputListener.accept(xrInputEvent)
        }
        executor.runAll()

        verify(splitEngineSubspaceManager).forwardInputEvent(anyNotNull(), eq(SUBSPACE_ID))
        assertThat(entity.affordanceState).isEqualTo(GeometryAffordanceState.ROTATION)
        assertRotation(entity.recommendedAffordanceRotation, expectedRotation)
        assertRotation(checkNotNull(listenerObservedRotation), expectedRotation)
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
