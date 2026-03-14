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

import android.app.Activity
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.BoundingBox.Companion.fromMinMax
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeGltfFeature.Companion.createWithMockFeature
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.node.Node
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class GltfEntityImplTest {
    private val xrExtensions = requireNotNull(getXrExtensions())
    private val entityManager = EntityManager()
    private val fakeScheduledExecutorService = FakeScheduledExecutorService()
    private val mockGltfFeature: GltfFeature = mock<GltfFeature>()
    private lateinit var activitySpace: ActivitySpaceImpl
    private lateinit var gltfEntityImpl: GltfEntityImpl

    @Before
    fun setUp() {
        val activityController = Robolectric.buildActivity(Activity::class.java)
        val activity = activityController.create().start().get()

        Truth.assertThat(xrExtensions).isNotNull()

        ShadowXrExtensions.extract(xrExtensions)
            .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE)

        val taskNode = xrExtensions.createNode()
        activitySpace =
            ActivitySpaceImpl(
                taskNode,
                activity,
                xrExtensions,
                entityManager,
                { xrExtensions.getSpatialState(activity) },
                fakeScheduledExecutorService,
            )
        entityManager.addSystemSpaceActivityPose(PerceptionSpaceScenePoseImpl(activitySpace))

        gltfEntityImpl = createGltfEntity(activity)
    }

    @After
    fun tearDown() {
        gltfEntityImpl.dispose()
    }

    private fun createGltfEntity(activity: Activity): GltfEntityImpl {
        val nodeHolder = NodeHolder<Node>(xrExtensions.createNode(), Node::class.java)
        val fakeGltfFeature = createWithMockFeature(mockGltfFeature, nodeHolder)

        return GltfEntityImpl(
            activity,
            fakeGltfFeature,
            activitySpace,
            xrExtensions,
            entityManager,
            fakeScheduledExecutorService,
        )
    }

    @Test
    fun getGltfModelBoundingBox_returnsBoundingBox() {
        val expectedResult = fromMinMax(Vector3.Zero, Vector3.One)
        whenever(mockGltfFeature.getGltfModelBoundingBox()).thenReturn(expectedResult)

        val boundingBox = gltfEntityImpl.gltfModelBoundingBox

        verify(mockGltfFeature).getGltfModelBoundingBox()
        Truth.assertThat(boundingBox).isEqualTo(expectedResult)
    }

    @Test
    fun dispose_featureDisposed() {
        gltfEntityImpl.dispose()

        verify(mockGltfFeature).dispose()
    }

    @Test
    fun getParent_nullParent_returnsNull() {
        gltfEntityImpl.parent = null
        Truth.assertThat(gltfEntityImpl.parent).isEqualTo(null)
    }

    @Test
    fun getPoseInParentSpace_nullParent_returnsIdentity() {
        gltfEntityImpl.parent = null
        gltfEntityImpl.setPose(Pose.Identity)
        Truth.assertThat(gltfEntityImpl.getPose(Space.PARENT)).isEqualTo(Pose.Identity)
    }

    @Test
    fun getPoseInActivitySpace_nullParent_throwsException() {
        gltfEntityImpl.parent = null
        Assert.assertThrows(IllegalStateException::class.java) {
            gltfEntityImpl.getPose(Space.ACTIVITY)
        }
    }

    @Test
    fun getPoseInRealWorldSpace_nullParent_throwsException() {
        gltfEntityImpl.parent = null
        Assert.assertThrows(IllegalStateException::class.java) {
            gltfEntityImpl.getPose(Space.REAL_WORLD)
        }
    }

    @Test
    fun getNodes_returnsNodesFromFeature() {
        val fakeNodes: MutableList<GltfModelNodeFeature> = ArrayList()
        val mockNodeFeature = mock<GltfModelNodeFeature>()
        fakeNodes.add(mockNodeFeature)
        whenever(mockGltfFeature.nodes).thenReturn(fakeNodes)

        val result = gltfEntityImpl.nodes

        verify(mockGltfFeature).nodes
        Truth.assertThat(result).isSameInstanceAs(fakeNodes)
        Truth.assertThat(result).hasSize(1)
    }

    @Test
    fun getNodes_returnsEmptyList_whenFeatureHasNoNodes() {
        whenever(mockGltfFeature.nodes).thenReturn(mutableListOf())

        val result = gltfEntityImpl.nodes

        Truth.assertThat(result).isEmpty()
    }

    companion object {
        private const val OPEN_XR_REFERENCE_SPACE_TYPE = 1
    }
}
