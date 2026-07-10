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

package androidx.xr.scenecore.testing

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Matrix4
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.ByteBufferRegion
import androidx.xr.scenecore.CustomMesh
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.MeshBuffer
import androidx.xr.scenecore.MeshEntity
import androidx.xr.scenecore.MeshSubset
import androidx.xr.scenecore.MeshSubsetTopology
import androidx.xr.scenecore.VertexAttribute
import androidx.xr.scenecore.VertexAttributeType
import androidx.xr.scenecore.VertexLayout
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class MeshEntityTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()
    private val boneCount = 2
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session
    private lateinit var meshBuffer: MeshBuffer
    private lateinit var customMesh: CustomMesh
    private lateinit var material: KhronosPbrMaterial
    private lateinit var meshEntity: MeshEntity
    private lateinit var underTest: MeshEntityTester

    @RequiresApi(Build.VERSION_CODES.O)
    @Before
    fun setUp() =
        runTest(testDispatcher) {
            activityController = Robolectric.buildActivity(ComponentActivity::class.java)
            activity = activityController.create().start().get()
            val result =
                Session.create(
                    context = activity,
                    coroutineContext = testDispatcher,
                    lifecycleOwner = activity as LifecycleOwner,
                )

            assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

            session = (result as SessionCreateSuccess).session

            val vertexLayout =
                VertexLayout.Builder()
                    .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                    .build()
            val vertexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
            val indexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
            meshBuffer =
                MeshBuffer.create(
                    session,
                    vertexLayout,
                    listOf(ByteBufferRegion(vertexBuffer, 0, 12)),
                    ByteBufferRegion(indexBuffer, 0, 12),
                )
            customMesh =
                CustomMesh.BuilderFromMeshBuffer(session, meshBuffer)
                    .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))
                    .build()
            material = KhronosPbrMaterial.create(session, AlphaMode.OPAQUE)
            meshEntity = MeshEntity.create(session, customMesh, listOf(material), boneCount)

            assertThat(meshEntity).isNotNull()

            underTest = testRule.createTester<MeshEntityTester>(meshEntity)

            assertThat(underTest.boneTransforms).isEmpty()
        }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun equalsAndHashCode_behaveCorrectly() {
        val tester1 = MeshEntityTester.create(meshEntity)
        val tester2 = MeshEntityTester.create(meshEntity)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun setBoneTransforms_withSameNumberOfTransformsAsBoneCount_updatesTransforms() {
        val transform1 =
            Matrix4(floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 1f, 1f))
        val transform2 =
            Matrix4(floatArrayOf(2f, 0f, 0f, 0f, 0f, 2f, 0f, 0f, 0f, 0f, 2f, 0f, 2f, 2f, 2f, 1f))
        val exactSizeList = listOf(transform1, transform2)

        meshEntity.setBoneTransforms(exactSizeList)

        assertThat(underTest.boneTransforms).hasSize(boneCount) // boneCount = 2
        assertThat(underTest.boneTransforms[0]).isEqualTo(transform1)
        assertThat(underTest.boneTransforms[1]).isEqualTo(transform2)
    }

    @Test
    fun setBoneTransforms_withFewerTransformsThanBoneCount_updatesOnlyProvidedBones() {
        val transform1 =
            Matrix4(floatArrayOf(3f, 0f, 0f, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 3f, 0f, 3f, 3f, 3f, 1f))
        val partialList = listOf(transform1)

        meshEntity.setBoneTransforms(partialList)

        assertThat(underTest.boneTransforms).hasSize(1)
        assertThat(underTest.boneTransforms[0]).isEqualTo(transform1)
    }

    @Test
    fun setBoneTransforms_withMoreTransformsThanBoneCount_ignoresExtraTransforms() {
        val transform1 = Matrix4.Identity
        val transform2 =
            Matrix4(floatArrayOf(2f, 0f, 0f, 0f, 0f, 2f, 0f, 0f, 0f, 0f, 2f, 0f, 5f, 5f, 5f, 1f))
        val transform3 =
            Matrix4(floatArrayOf(4f, 0f, 0f, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 4f, 0f, 9f, 9f, 9f, 1f))
        val overSizedList = listOf(transform1, transform2, transform3)

        meshEntity.setBoneTransforms(overSizedList)

        assertThat(underTest.boneTransforms).hasSize(boneCount) // boneCount = 2
        assertThat(underTest.boneTransforms[0]).isEqualTo(transform1)
        assertThat(underTest.boneTransforms[1]).isEqualTo(transform2)
    }
}
