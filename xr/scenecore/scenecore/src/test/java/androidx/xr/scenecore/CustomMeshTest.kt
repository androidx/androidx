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
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
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

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class CustomMeshTest {

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session
    private lateinit var meshBuffer: MeshBuffer
    private lateinit var vertexLayout: VertexLayout
    private lateinit var vertexBufferRegion: ByteBufferRegion
    private lateinit var indexBufferRegion: ByteBufferRegion

    @RequiresApi(Build.VERSION_CODES.O)
    @Before
    fun setUp(): Unit = runBlocking {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(context = activity, coroutineContext = testDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session

        vertexLayout =
            VertexLayout.Builder()
                .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                .build()

        val vertexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val indexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        vertexBufferRegion = ByteBufferRegion(vertexBuffer, 0, 12)
        indexBufferRegion = ByteBufferRegion(indexBuffer, 0, 12)

        meshBuffer =
            MeshBuffer.create(session, vertexLayout, listOf(vertexBufferRegion), indexBufferRegion)
    }

    @Test
    fun builder_addSubsetAfterSetTopology_throwsException() {
        val builder =
            CustomMesh.BuilderFromMeshData(session, vertexLayout)
                .setTopology(MeshSubsetTopology.TRIANGLES)
        val exception =
            assertThrows(IllegalStateException::class.java) {
                builder.addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot add subset after setting a single topology")
    }

    @Test
    fun builder_addSubsetOverloadAfterSetTopology_throwsException() {
        val builder =
            CustomMesh.BuilderFromMeshData(session, vertexLayout)
                .setTopology(MeshSubsetTopology.TRIANGLES)
        val exception =
            assertThrows(IllegalStateException::class.java) {
                builder.addSubset(MeshSubsetTopology.TRIANGLES, 0, 3)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot add subset after setting a single topology")
    }

    @Test
    fun builder_addSubset_withNegativeIndexOffset_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.addSubset(MeshSubsetTopology.TRIANGLES, -1, 3)
            }
        assertThat(exception).hasMessageThat().contains("indexOffset must not be negative.")
    }

    @Test
    fun builder_addSubset_withNegativeIndexCount_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.addSubset(MeshSubsetTopology.TRIANGLES, 0, -1)
            }
        assertThat(exception).hasMessageThat().contains("indexCount must not be negative.")
    }

    @Test
    fun meshBufferBuilder_addSubset_withNegativeIndexOffset_throwsException() {
        val builder = CustomMesh.BuilderFromMeshBuffer(session, meshBuffer)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.addSubset(MeshSubsetTopology.TRIANGLES, -1, 3)
            }
        assertThat(exception).hasMessageThat().contains("indexOffset must not be negative.")
    }

    @Test
    fun meshBufferBuilder_addSubset_withNegativeIndexCount_throwsException() {
        val builder = CustomMesh.BuilderFromMeshBuffer(session, meshBuffer)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.addSubset(MeshSubsetTopology.TRIANGLES, 0, -1)
            }
        assertThat(exception).hasMessageThat().contains("indexCount must not be negative.")
    }

    @Test
    fun builder_setTopologyAfterAddSubset_throwsException() {
        val builder =
            CustomMesh.BuilderFromMeshData(session, vertexLayout)
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))
        val exception =
            assertThrows(IllegalStateException::class.java) {
                builder.setTopology(MeshSubsetTopology.TRIANGLES)
            }
        assertThat(exception).hasMessageThat().contains("Cannot set topology after adding subsets")
    }

    @Test
    fun meshBufferBuilder_withoutSubsets_throwsException() {
        val builder = CustomMesh.BuilderFromMeshBuffer(session, meshBuffer)
        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }
        assertThat(exception).hasMessageThat().contains("CustomMesh requires at least one subset")
    }

    @Test
    fun meshBufferBuilder_withSubsets_succeeds() {
        val builder =
            CustomMesh.BuilderFromMeshBuffer(session, meshBuffer)
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))
        assertThat(builder.build()).isNotNull()
    }

    @Test
    fun meshBufferBuilder_withSubsetsOverload_succeeds() {
        val builder =
            CustomMesh.BuilderFromMeshBuffer(session, meshBuffer)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, 3)
        assertThat(builder.build()).isNotNull()
    }

    @Test
    fun build_withOverloads_succeeds() {
        val builder =
            CustomMesh.BuilderFromMeshData(session, vertexLayout)
                .addVertexData(vertexBufferRegion.buffer, 0, 12)
                .setIndexData(indexBufferRegion.buffer, 0, 12)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, 3)

        assertThat(builder.build()).isNotNull()
    }

    @Test
    fun builder_addVertexData_withNegativeOffset_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.addVertexData(vertexBufferRegion.buffer, offset = -1, size = 5)
            }
        assertThat(exception).hasMessageThat().contains("offset must not be negative.")
    }

    @Test
    fun builder_addVertexData_withZeroSize_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.addVertexData(vertexBufferRegion.buffer, offset = 0, size = 0)
            }
        assertThat(exception).hasMessageThat().contains("size must be greater than zero")
    }

    @Test
    fun builder_addVertexData_withNegativeSize_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.addVertexData(vertexBufferRegion.buffer, offset = 0, size = -1)
            }
        assertThat(exception).hasMessageThat().contains("size must be greater than zero")
    }

    @Test
    fun builder_addVertexData_withOutOfBounds_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.addVertexData(vertexBufferRegion.buffer, offset = 10, size = 5)
            }
        assertThat(exception).hasMessageThat().contains("size + offset must not exceed capacity")
    }

    @Test
    fun builder_addVertexData_withOffsetGreaterThanCapacity_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.addVertexData(vertexBufferRegion.buffer, offset = 20, size = 5)
            }
        assertThat(exception).hasMessageThat().contains("offset must not exceed buffer capacity")
    }

    @Test
    fun builder_setIndexData_withNegativeOffset_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.setIndexData(indexBufferRegion.buffer, offset = -1, size = 5)
            }
        assertThat(exception).hasMessageThat().contains("offset must not be negative.")
    }

    @Test
    fun builder_setIndexData_withZeroSize_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.setIndexData(indexBufferRegion.buffer, offset = 0, size = 0)
            }
        assertThat(exception).hasMessageThat().contains("size must be greater than zero")
    }

    @Test
    fun builder_setIndexData_withNegativeSize_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.setIndexData(indexBufferRegion.buffer, offset = 0, size = -1)
            }
        assertThat(exception).hasMessageThat().contains("size must be greater than zero")
    }

    @Test
    fun builder_setIndexData_withOutOfBounds_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.setIndexData(indexBufferRegion.buffer, offset = 10, size = 5)
            }
        assertThat(exception).hasMessageThat().contains("size + offset must not exceed capacity")
    }

    @Test
    fun builder_setIndexData_withOffsetGreaterThanCapacity_throwsException() {
        val builder = CustomMesh.BuilderFromMeshData(session, vertexLayout)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                builder.setIndexData(indexBufferRegion.buffer, offset = 20, size = 5)
            }
        assertThat(exception).hasMessageThat().contains("offset must not exceed buffer capacity")
    }

    @Test
    fun build_withMissingIndexData_throwsException() {
        val builder =
            CustomMesh.BuilderFromMeshData(session, vertexLayout)
                .addVertexData(vertexBufferRegion)
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))

        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }
        assertThat(exception).hasMessageThat().contains("Index data must be provided")
    }

    @Test
    fun build_withMissingVertexData_throwsException() {
        val builder =
            CustomMesh.BuilderFromMeshData(session, vertexLayout)
                .setIndexData(indexBufferRegion)
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))

        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }
        assertThat(exception)
            .hasMessageThat()
            .contains("At least one vertex buffer data region must be provided")
    }

    @Test
    fun build_withNeitherSubsetsNorTopology_throwsException() {
        val builder =
            CustomMesh.BuilderFromMeshData(session, vertexLayout)
                .addVertexData(vertexBufferRegion)
                .setIndexData(indexBufferRegion)

        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }
        assertThat(exception)
            .hasMessageThat()
            .contains("CustomMesh requires either subsets or a single topology, but not both")
    }

    @Test
    fun setBounds_updatesRuntime() {
        val builder =
            CustomMesh.BuilderFromMeshData(session, vertexLayout)
                .addVertexData(vertexBufferRegion)
                .setIndexData(indexBufferRegion)
                .setTopology(MeshSubsetTopology.TRIANGLES)

        val customMesh = builder.build()

        val newBounds =
            BoundingBox.fromCenterAndHalfExtents(Vector3(1f, 2f, 3f), FloatSize3d(4f, 5f, 6f))
        customMesh.bounds = newBounds
        assertThat(customMesh.bounds).isEqualTo(newBounds)
    }

    @Test
    fun meshBufferBuilder_withBounds_succeeds() {
        val bounds =
            BoundingBox.fromCenterAndHalfExtents(Vector3(1f, 2f, 3f), FloatSize3d(4f, 5f, 6f))
        val customMesh =
            CustomMesh.BuilderFromMeshBuffer(session, meshBuffer)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, 3)
                .setBounds(bounds)
                .build()

        assertThat(customMesh).isNotNull()
    }

    @Test
    fun buildStatic_withNullVertexData_throwsException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                CustomMesh.BuilderFromMeshData(session, vertexLayout).addVertexData(null)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Null vertex data is only allowed when constructing a dynamic mesh.")
    }

    @Test
    fun buildStatic_withNullIndexData_throwsException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                CustomMesh.BuilderFromMeshData(session, vertexLayout).setIndexData(null)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Null index data is only allowed when constructing a dynamic mesh.")
    }

    @Test
    fun constructor_withNonPositiveMaxVertices_throwsException() {
        val exceptionZero =
            assertThrows(IllegalArgumentException::class.java) {
                CustomMesh.BuilderFromMeshData(
                    session,
                    vertexLayout,
                    maxVertices = 0,
                    maxIndices = 10,
                )
            }
        assertThat(exceptionZero).hasMessageThat().contains("maxVertices must be positive.")

        val exceptionNegative =
            assertThrows(IllegalArgumentException::class.java) {
                CustomMesh.BuilderFromMeshData(
                    session,
                    vertexLayout,
                    maxVertices = -1,
                    maxIndices = 10,
                )
            }
        assertThat(exceptionNegative).hasMessageThat().contains("maxVertices must be positive.")
    }

    @Test
    fun constructor_withNonPositiveMaxIndices_throwsException() {
        val exceptionZero =
            assertThrows(IllegalArgumentException::class.java) {
                CustomMesh.BuilderFromMeshData(
                    session,
                    vertexLayout,
                    maxVertices = 10,
                    maxIndices = 0,
                )
            }
        assertThat(exceptionZero).hasMessageThat().contains("maxIndices must be positive.")

        val exceptionNegative =
            assertThrows(IllegalArgumentException::class.java) {
                CustomMesh.BuilderFromMeshData(
                    session,
                    vertexLayout,
                    maxVertices = 10,
                    maxIndices = -1,
                )
            }
        assertThat(exceptionNegative).hasMessageThat().contains("maxIndices must be positive.")
    }

    @Test
    fun buildDynamic_withOmittedVertexAndIndexData_succeeds() {
        val customMesh =
            CustomMesh.BuilderFromMeshData(session, vertexLayout, maxVertices = 10, maxIndices = 30)
                .setTopology(MeshSubsetTopology.TRIANGLES)
                .build()

        assertThat(customMesh.meshBuffer.meshUsage).isEqualTo(MeshBuffer.MeshUsage.DYNAMIC)
        assertThat(customMesh.subsets).hasSize(1)
        assertThat(customMesh.subsets[0].topology).isEqualTo(MeshSubsetTopology.TRIANGLES)
        assertThat(customMesh.subsets[0].indexOffset).isEqualTo(0)
        assertThat(customMesh.subsets[0].indexCount).isEqualTo(30)
    }

    @Test
    fun buildDynamic_withTopology_succeeds() {
        val customMesh =
            CustomMesh.BuilderFromMeshData(session, vertexLayout, maxVertices = 10, maxIndices = 30)
                .addVertexData(null)
                .setIndexData(null)
                .setTopology(MeshSubsetTopology.TRIANGLES)
                .build()

        assertThat(customMesh.meshBuffer.meshUsage).isEqualTo(MeshBuffer.MeshUsage.DYNAMIC)
        assertThat(customMesh.subsets).hasSize(1)
        assertThat(customMesh.subsets[0].topology).isEqualTo(MeshSubsetTopology.TRIANGLES)
        assertThat(customMesh.subsets[0].indexOffset).isEqualTo(0)
        assertThat(customMesh.subsets[0].indexCount).isEqualTo(30)
    }

    @Test
    fun buildDynamic_withSubsets_succeeds() {
        val customMesh =
            CustomMesh.BuilderFromMeshData(session, vertexLayout, maxVertices = 10, maxIndices = 30)
                .addVertexData(vertexBufferRegion)
                .setIndexData(indexBufferRegion)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, 3)
                .build()

        assertThat(customMesh.meshBuffer.meshUsage).isEqualTo(MeshBuffer.MeshUsage.DYNAMIC)
        assertThat(customMesh.subsets).hasSize(1)
        assertThat(customMesh.subsets[0].topology).isEqualTo(MeshSubsetTopology.TRIANGLES)
        assertThat(customMesh.subsets[0].indexOffset).isEqualTo(0)
        assertThat(customMesh.subsets[0].indexCount).isEqualTo(3)
    }

    @Test
    fun buildDynamic_withInitialVertexDataExceedingCapacity_throwsException() {
        val largeVertexBuffer = ByteBuffer.allocateDirect(24).order(ByteOrder.nativeOrder())
        val builder =
            CustomMesh.BuilderFromMeshData(session, vertexLayout, maxVertices = 1, maxIndices = 3)
                .addVertexData(ByteBufferRegion(largeVertexBuffer, 0, 24))
                .setIndexData(null)
                .setTopology(MeshSubsetTopology.TRIANGLES)

        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }
        assertThat(exception)
            .hasMessageThat()
            .contains("Provided initial dynamic vertex data exceeds maxVertices.")
    }

    @Test
    fun buildDynamic_withInitialIndexDataExceedingCapacity_throwsException() {
        val builder =
            CustomMesh.BuilderFromMeshData(session, vertexLayout, maxVertices = 3, maxIndices = 1)
                .addVertexData(null)
                .setIndexData(indexBufferRegion)
                .setTopology(MeshSubsetTopology.TRIANGLES)

        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }
        assertThat(exception)
            .hasMessageThat()
            .contains("Provided initial dynamic index data exceeds maxIndices.")
    }
}
