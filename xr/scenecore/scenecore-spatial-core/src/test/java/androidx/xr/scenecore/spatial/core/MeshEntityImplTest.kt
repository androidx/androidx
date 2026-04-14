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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.spatial.core

import android.app.Activity
import androidx.xr.runtime.NodeHolder
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl
import androidx.xr.scenecore.testing.FakeMeshFeature
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.node.Node
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class MeshEntityImplTest {
    private val xrExtensions = SpatialCoreXrExtensionsHolderProvider.extensionsLegacy
    private val sceneNodeRegistry = SceneNodeRegistry()
    private val fakeScheduledExecutorService = FakeScheduledExecutorService()
    private lateinit var activitySpace: ActivitySpaceImpl
    private lateinit var meshEntityImpl: MeshEntityImpl
    private lateinit var fakeMeshFeature: FakeMeshFeature

    @Before
    fun setUp() {
        val activityController = Robolectric.buildActivity(Activity::class.java)
        val activity = activityController.create().start().get()

        assertThat(xrExtensions).isNotNull()

        ShadowXrExtensions.extract(xrExtensions)
            .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE)

        val taskNode = xrExtensions.createNode()
        activitySpace =
            ActivitySpaceImpl(
                taskNode,
                activity,
                xrExtensions,
                sceneNodeRegistry,
                { xrExtensions.getSpatialState(activity) },
                fakeScheduledExecutorService,
            )
        sceneNodeRegistry.addSystemSpaceScenePose(PerceptionSpaceScenePoseImpl(activitySpace))

        meshEntityImpl = createMeshEntity(activity)
    }

    @After
    fun tearDown() {
        if (::meshEntityImpl.isInitialized) {
            meshEntityImpl.dispose()
        }
    }

    private fun createMeshEntity(activity: Activity): MeshEntityImpl {
        val nodeHolder = NodeHolder<Node>(xrExtensions.createNode(), Node::class.java)
        fakeMeshFeature = spy(FakeMeshFeature(nodeHolder))

        return MeshEntityImpl(
            activity,
            fakeMeshFeature,
            activitySpace,
            xrExtensions,
            sceneNodeRegistry,
            fakeScheduledExecutorService,
        )
    }

    @Test
    fun dispose_featureDisposed() {
        meshEntityImpl.dispose()

        verify(fakeMeshFeature).dispose()
    }

    @Test
    fun setMaterial_delegatesToMeshFeature() {
        val material: MaterialResource = mock()
        val subsetIndex = 0

        meshEntityImpl.setMaterial(material, subsetIndex)

        verify(fakeMeshFeature).setMaterial(material, subsetIndex)
    }

    companion object {
        private const val OPEN_XR_REFERENCE_SPACE_TYPE = 1
    }
}
