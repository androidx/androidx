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
}
