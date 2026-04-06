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

@OptIn(ExperimentalCustomMeshApi::class)
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
    fun setUp() = runBlocking {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session

        vertexLayout =
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
        vertexBufferRegion = ByteBufferRegion(vertexBuffer, 0, 12)
        indexBufferRegion = ByteBufferRegion(indexBuffer, 0, 12)

        meshBuffer =
            MeshBuffer.create(session, vertexLayout, listOf(vertexBufferRegion), indexBufferRegion)
    }

    @Test
    fun builder_setMeshBufferAfterVertexLayout_throwsException() {
        val builder = CustomMesh.Builder(session).setVertexLayout(vertexLayout)
        val exception =
            assertThrows(IllegalStateException::class.java) { builder.setMeshBuffer(meshBuffer) }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot set MeshBuffer after setting vertex, index data, or topology")
    }

    @Test
    fun builder_setMeshBufferAfterAddVertexBufferData_throwsException() {
        val builder = CustomMesh.Builder(session).addVertexBufferData(vertexBufferRegion)
        val exception =
            assertThrows(IllegalStateException::class.java) { builder.setMeshBuffer(meshBuffer) }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot set MeshBuffer after setting vertex, index data, or topology")
    }

    @Test
    fun builder_setMeshBufferAfterSetIndexData_throwsException() {
        val builder = CustomMesh.Builder(session).setIndexData(indexBufferRegion)
        val exception =
            assertThrows(IllegalStateException::class.java) { builder.setMeshBuffer(meshBuffer) }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot set MeshBuffer after setting vertex, index data, or topology")
    }

    @Test
    fun builder_setVertexLayoutAfterMeshBuffer_throwsException() {
        val builder = CustomMesh.Builder(session).setMeshBuffer(meshBuffer)
        val exception =
            assertThrows(IllegalStateException::class.java) {
                builder.setVertexLayout(vertexLayout)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot set vertex layout after setting MeshBuffer")
    }

    @Test
    fun builder_addVertexBufferDataAfterMeshBuffer_throwsException() {
        val builder = CustomMesh.Builder(session).setMeshBuffer(meshBuffer)
        val exception =
            assertThrows(IllegalStateException::class.java) {
                builder.addVertexBufferData(vertexBufferRegion)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot set vertex data after setting MeshBuffer")
    }

    @Test
    fun builder_addSubsetAfterSetSingleSubsetTopology_throwsException() {
        val builder =
            CustomMesh.Builder(session).setSingleSubsetTopology(MeshSubsetTopology.TRIANGLES)
        val exception =
            assertThrows(IllegalStateException::class.java) {
                builder.addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot add subset after setting a single topology")
    }

    @Test
    fun builder_setSingleSubsetTopologyAfterAddSubset_throwsException() {
        val builder =
            CustomMesh.Builder(session).addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))
        val exception =
            assertThrows(IllegalStateException::class.java) {
                builder.setSingleSubsetTopology(MeshSubsetTopology.TRIANGLES)
            }
        assertThat(exception).hasMessageThat().contains("Cannot set topology after adding subsets")
    }

    @Test
    fun builder_setSingleSubsetTopologyAfterMeshBuffer_throwsException() {
        val builder = CustomMesh.Builder(session).setMeshBuffer(meshBuffer)
        val exception =
            assertThrows(IllegalStateException::class.java) {
                builder.setSingleSubsetTopology(MeshSubsetTopology.TRIANGLES)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot set topology after setting MeshBuffer")
    }

    @Test
    fun builder_setMeshBufferAfterSingleSubsetTopology_throwsException() {
        val builder =
            CustomMesh.Builder(session).setSingleSubsetTopology(MeshSubsetTopology.TRIANGLES)
        val exception =
            assertThrows(IllegalStateException::class.java) { builder.setMeshBuffer(meshBuffer) }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot set MeshBuffer after setting vertex, index data, or topology")
    }

    @Test
    fun builder_setMeshBufferAfterAddSubset_succeeds() {
        val builder =
            CustomMesh.Builder(session).addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))
        builder.setMeshBuffer(meshBuffer)
        assertThat(builder.build()).isNotNull()
    }

    @Test
    fun builder_setIndexDataAfterMeshBuffer_throwsException() {
        val builder = CustomMesh.Builder(session).setMeshBuffer(meshBuffer)
        val exception =
            assertThrows(IllegalStateException::class.java) {
                builder.setIndexData(indexBufferRegion)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot set index data after setting MeshBuffer")
    }

    @Test
    fun build_withMissingVertexLayout_throwsException() {
        val builder =
            CustomMesh.Builder(session)
                .addVertexBufferData(vertexBufferRegion)
                .setIndexData(indexBufferRegion)
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))

        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }
        assertThat(exception).hasMessageThat().contains("VertexLayout must be provided")
    }

    @Test
    fun build_withMissingIndexData_throwsException() {
        val builder =
            CustomMesh.Builder(session)
                .setVertexLayout(vertexLayout)
                .addVertexBufferData(vertexBufferRegion)
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))

        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }
        assertThat(exception).hasMessageThat().contains("Index data must be provided")
    }

    @Test
    fun build_withMissingVertexData_throwsException() {
        val builder =
            CustomMesh.Builder(session)
                .setVertexLayout(vertexLayout)
                .setIndexData(indexBufferRegion)
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 3))

        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }
        assertThat(exception)
            .hasMessageThat()
            .contains("At least one vertex buffer data region must be provided")
    }

    @Test
    fun build_withNeitherSubsetsNorTopology_throwsException() {
        val builder = CustomMesh.Builder(session).setMeshBuffer(meshBuffer)

        val exception = assertThrows(IllegalStateException::class.java) { builder.build() }
        assertThat(exception)
            .hasMessageThat()
            .contains("CustomMesh requires either subsets or a single topology, but not both")
    }
}
