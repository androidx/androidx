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
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.Node
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SubspaceNodeEntityImplTest : AndroidXrEntityImplTest() {
    override val xrExtensions = SpatialCoreXrExtensionsHolderProvider.extensionsLegacy
    override val activity: Activity = Robolectric.buildActivity(Activity::class.java).create().get()
    override val sceneNodeRegistry = SceneNodeRegistry()
    override val fakeExecutor = FakeScheduledExecutorService()
    private lateinit var subspaceNodeEntity: SubspaceNodeEntityImpl

    override fun createEntity(node: Node): AndroidXrEntity {
        return SubspaceNodeEntityImpl(activity, xrExtensions, node, sceneNodeRegistry, fakeExecutor)
    }

    @Before
    fun setUp() {
        val activitySpace =
            ActivitySpaceImpl(
                xrExtensions.createNode(),
                activity,
                xrExtensions,
                sceneNodeRegistry,
                { xrExtensions.getSpatialState(activity) },
                fakeExecutor,
            )

        subspaceNodeEntity =
            SubspaceNodeEntityImpl(
                activity,
                xrExtensions,
                xrExtensions.createNode(),
                sceneNodeRegistry,
                fakeExecutor,
            )
        subspaceNodeEntity.parent = activitySpace
    }

    @Test
    fun setSize_sizeIsUpdated() {
        val size = Dimensions(3.0f, 4.0f, 5.0f)

        subspaceNodeEntity.size = size

        Truth.assertThat(subspaceNodeEntity.size).isEqualTo(size)
    }

    @Test
    fun setScale_scaleIsUpdated() {
        val size = Dimensions(3.0f, 4.0f, 5.0f)
        val scale = Vector3(1.0f, 2.0f, 3.0f)

        subspaceNodeEntity.size = size
        subspaceNodeEntity.setScale(scale)

        Truth.assertThat(subspaceNodeEntity.getScale()).isEqualTo(scale)
    }

    @Test
    fun setAlpha_alphaIsUpdated() {
        val alpha = 0.5f

        subspaceNodeEntity.setAlpha(alpha)

        Truth.assertThat(subspaceNodeEntity.getAlpha()).isEqualTo(alpha)
    }

    @Test
    fun setHidden_visibilityIsUpdated() {
        val hidden = true

        subspaceNodeEntity.setHidden(hidden)

        Truth.assertThat(subspaceNodeEntity.isHidden(false)).isEqualTo(hidden)
    }
}
