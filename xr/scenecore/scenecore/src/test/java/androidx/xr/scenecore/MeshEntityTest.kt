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

package androidx.xr.scenecore

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.testing.MemoryUtils
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCustomMeshApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class MeshEntityTest {
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session
    private lateinit var meshBuffer: MeshBuffer
    private lateinit var customMesh: CustomMesh
    private lateinit var material: KhronosPbrMaterial

    @RequiresApi(Build.VERSION_CODES.O)
    @Before
    fun setUp() = runBlocking {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(context = activity, coroutineContext = testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session

        val vertexLayout =
            VertexLayout(
                listOf(
                    VertexAttributeDescriptor(
                        VertexAttribute.POSITION,
                        VertexAttributeType.FLOAT3,
                        0,
                    )
                )
            )
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
    }

    @Test
    fun create_withValidArguments_createsMeshEntity() {
        val entity = MeshEntity.create(session, customMesh, listOf(material))

        assertThat(entity).isNotNull()
        assertThat(entity.mesh).isEqualTo(customMesh)
        assertThat(entity.materials).containsExactly(material)
        assertThat(entity.getPose()).isEqualTo(Pose.Identity)
    }

    @Test
    fun create_withMismatchedMaterialCount_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            MeshEntity.create(session, customMesh, emptyList())
        }
    }

    @Test
    fun create_withNullMaterial_throwsException() {
        @Suppress("UNCHECKED_CAST") val materialsWithNull = listOf(null) as List<Material>
        assertThrows(IllegalArgumentException::class.java) {
            MeshEntity.create(session, customMesh, materialsWithNull)
        }
    }

    @Test
    fun create_withBoneCountAndPose_createsMeshEntity() {
        val pose = Pose(Vector3(1f, 2f, 3f))
        val entity =
            MeshEntity.create(session, customMesh, listOf(material), boneCount = 10, pose = pose)

        assertThat(entity).isNotNull()
        assertThat(entity.mesh).isEqualTo(customMesh)
        assertThat(entity.materials).containsExactly(material)
        assertThat(entity.getPose()).isEqualTo(pose)
    }

    @Test
    fun create_withParent_createsMeshEntity() {
        val parentEntity = Entity.create(session)
        val entity = MeshEntity.create(session, customMesh, listOf(material), parent = parentEntity)

        assertThat(entity).isNotNull()
        assertThat(entity.mesh).isEqualTo(customMesh)
        assertThat(entity.materials).containsExactly(material)
        assertThat(entity.parent).isEqualTo(parentEntity)
    }

    @Test
    fun setMaterial_withValidSubsetIndex_setsMaterial() {
        val entity = MeshEntity.create(session, customMesh, listOf(material))
        val newMaterial = runBlocking { KhronosPbrMaterial.create(session, AlphaMode.OPAQUE) }

        entity.setMaterial(newMaterial, 0)

        assertThat(entity.materials).containsExactly(newMaterial)
    }

    @Test
    fun setMaterial_withInvalidSubsetIndex_throwsException() {
        val entity = MeshEntity.create(session, customMesh, listOf(material))
        val newMaterial = runBlocking { KhronosPbrMaterial.create(session, AlphaMode.OPAQUE) }

        assertThrows(IllegalArgumentException::class.java) { entity.setMaterial(newMaterial, 1) }
        assertThrows(IllegalArgumentException::class.java) { entity.setMaterial(newMaterial, -1) }
    }

    @Test
    fun setBoneTransforms_withZeroBoneCount_throwsException() {
        val entity = MeshEntity.create(session, customMesh, listOf(material), boneCount = 0)

        assertThrows(IllegalStateException::class.java) {
            entity.setBoneTransforms(listOf(Matrix4.Identity))
        }
    }

    @Test
    fun setBoneTransforms_withPositiveBoneCount_succeeds() {
        val entity = MeshEntity.create(session, customMesh, listOf(material), boneCount = 1)

        entity.setBoneTransforms(listOf(Matrix4.Identity))

        // Execution succeeded without exception
    }

    @Test
    fun garbageCollection_disposesEntity() {
        fun createMeshEntity(): WeakReference<MeshEntity> {
            val entity = MeshEntity.create(session, customMesh, listOf(material), parent = null)
            return WeakReference(entity)
        }

        val entityRef = createMeshEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }
}
