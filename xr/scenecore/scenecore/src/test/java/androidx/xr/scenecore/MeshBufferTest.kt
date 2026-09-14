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

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class MeshBufferTest {
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session
    private lateinit var vertexLayout: VertexLayout

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
                .startNextBuffer()
                .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3)
                .build()
    }

    @Test
    fun create_missingBufferForLayoutIndex_throwsException() {
        val vertexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val indexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                MeshBuffer.create(
                    session,
                    vertexLayout,
                    listOf(ByteBufferRegion(vertexBuffer, 0, 12)),
                    ByteBufferRegion(indexBuffer, 0, 12),
                )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("vertexData size must match the number of buffers in VertexLayout")
    }

    @Test
    fun create_emptyVertexData_throwsException() {
        val vertexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val indexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                MeshBuffer.create(
                    session,
                    vertexLayout,
                    listOf(
                        ByteBufferRegion(vertexBuffer, 0, 12),
                        ByteBufferRegion(vertexBuffer, 0, 0),
                    ),
                    ByteBufferRegion(indexBuffer, 0, 12),
                )
            }
        assertThat(exception).hasMessageThat().contains("vertexData[1] must be non-empty")
    }

    @Test
    fun create_emptyIndexData_throwsException() {
        val vertexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val indexBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                MeshBuffer.create(
                    session,
                    vertexLayout,
                    listOf(
                        ByteBufferRegion(vertexBuffer, 0, 12),
                        ByteBufferRegion(vertexBuffer, 0, 12),
                    ),
                    ByteBufferRegion(indexBuffer, 0, 0),
                )
            }
        assertThat(exception).hasMessageThat().contains("indexData must be non-empty")
    }

    @Test
    fun create_withValidArguments_createsStaticMeshBuffer() {
        val vertexBuffer1 = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val vertexBuffer2 = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val indexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())

        val meshBuffer =
            MeshBuffer.create(
                session,
                vertexLayout,
                listOf(
                    ByteBufferRegion(vertexBuffer1, 0, 12),
                    ByteBufferRegion(vertexBuffer2, 0, 12),
                ),
                ByteBufferRegion(indexBuffer, 0, 12),
            )

        assertThat(meshBuffer.meshUsage).isEqualTo(MeshBuffer.MeshUsage.STATIC)
        assertThat(meshBuffer.vertexLayout).isEqualTo(vertexLayout)
    }

    @Test
    fun createDynamic_withPositiveCounts_createsDynamicMeshBuffer() {
        val meshBuffer =
            MeshBuffer.createDynamic(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 10,
                indexCount = 30,
            )

        assertThat(meshBuffer.meshUsage).isEqualTo(MeshBuffer.MeshUsage.DYNAMIC)
        assertThat(meshBuffer.vertexLayout).isEqualTo(vertexLayout)
    }

    @Test
    fun createDynamic_withInitialData_succeeds() {
        val vertexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val indexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())

        val meshBuffer =
            MeshBuffer.createDynamic(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 10,
                indexCount = 30,
                vertexData = listOf(ByteBufferRegion(vertexBuffer, 0, 12), null),
                indexData = ByteBufferRegion(indexBuffer, 0, 12),
            )

        assertThat(meshBuffer.meshUsage).isEqualTo(MeshBuffer.MeshUsage.DYNAMIC)
    }

    @Test
    fun createDynamic_withNonPositiveVertexCount_throwsException() {
        val exceptionZero =
            assertThrows(IllegalArgumentException::class.java) {
                MeshBuffer.createDynamic(session, vertexLayout, vertexCount = 0, indexCount = 10)
            }
        assertThat(exceptionZero).hasMessageThat().contains("vertexCount must be positive")

        val exceptionNegative =
            assertThrows(IllegalArgumentException::class.java) {
                MeshBuffer.createDynamic(session, vertexLayout, vertexCount = -1, indexCount = 10)
            }
        assertThat(exceptionNegative).hasMessageThat().contains("vertexCount must be positive")
    }

    @Test
    fun createDynamic_withNonPositiveIndexCount_throwsException() {
        val exceptionZero =
            assertThrows(IllegalArgumentException::class.java) {
                MeshBuffer.createDynamic(session, vertexLayout, vertexCount = 10, indexCount = 0)
            }
        assertThat(exceptionZero).hasMessageThat().contains("indexCount must be positive")

        val exceptionNegative =
            assertThrows(IllegalArgumentException::class.java) {
                MeshBuffer.createDynamic(session, vertexLayout, vertexCount = 10, indexCount = -1)
            }
        assertThat(exceptionNegative).hasMessageThat().contains("indexCount must be positive")
    }

    @Test
    fun createDynamic_withMismatchedVertexDataSize_throwsException() {
        val vertexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                MeshBuffer.createDynamic(
                    session = session,
                    vertexLayout = vertexLayout,
                    vertexCount = 10,
                    indexCount = 30,
                    vertexData = listOf(ByteBufferRegion(vertexBuffer, 0, 12)),
                )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("vertexData size must match the number of buffers in VertexLayout.")
    }

    @Test
    fun updateVertexData_onDynamicMeshBuffer_succeeds() {
        val meshBuffer =
            MeshBuffer.createDynamic(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 10,
                indexCount = 30,
            )
        val updateBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val region = ByteBufferRegion(updateBuffer, 0, 12)

        meshBuffer.updateVertexData(0, region, 0)
        meshBuffer.updateVertexData(1, region, 12)
    }

    @Test
    fun updateVertexData_onStaticMeshBuffer_throwsException() {
        val vertexBuffer1 = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val vertexBuffer2 = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val indexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())

        val meshBuffer =
            MeshBuffer.create(
                session,
                vertexLayout,
                listOf(
                    ByteBufferRegion(vertexBuffer1, 0, 12),
                    ByteBufferRegion(vertexBuffer2, 0, 12),
                ),
                ByteBufferRegion(indexBuffer, 0, 12),
            )
        val updateBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())

        val exception =
            assertThrows(IllegalStateException::class.java) {
                meshBuffer.updateVertexData(0, ByteBufferRegion(updateBuffer, 0, 12))
            }
        assertThat(exception).hasMessageThat().contains("MeshBuffer is not dynamic.")
    }

    @Test
    fun updateVertexData_withOutOfBoundsBufferIndex_throwsException() {
        val meshBuffer =
            MeshBuffer.createDynamic(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 10,
                indexCount = 30,
            )
        val updateBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val region = ByteBufferRegion(updateBuffer, 0, 12)

        val exceptionNegative =
            assertThrows(IllegalArgumentException::class.java) {
                meshBuffer.updateVertexData(-1, region)
            }
        assertThat(exceptionNegative).hasMessageThat().contains("bufferIndex out of bounds.")

        val exceptionTooLarge =
            assertThrows(IllegalArgumentException::class.java) {
                meshBuffer.updateVertexData(2, region)
            }
        assertThat(exceptionTooLarge).hasMessageThat().contains("bufferIndex out of bounds.")
    }

    @Test
    fun updateVertexData_withNegativeOffset_throwsException() {
        val meshBuffer =
            MeshBuffer.createDynamic(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 10,
                indexCount = 30,
            )
        val updateBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val region = ByteBufferRegion(updateBuffer, 0, 12)

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                meshBuffer.updateVertexData(0, region, offsetInBytes = -1)
            }
        assertThat(exception).hasMessageThat().contains("offsetInBytes must be non-negative.")
    }

    @Test
    fun updateVertexData_exceedingCapacity_throwsException() {
        val meshBuffer =
            MeshBuffer.createDynamic(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 2,
                indexCount = 6,
            )
        val updateBuffer = ByteBuffer.allocateDirect(24).order(ByteOrder.nativeOrder())

        val exceptionOffset =
            assertThrows(IllegalArgumentException::class.java) {
                meshBuffer.updateVertexData(
                    0,
                    ByteBufferRegion(updateBuffer, 0, 24),
                    offsetInBytes = 1,
                )
            }
        assertThat(exceptionOffset)
            .hasMessageThat()
            .contains("Update exceeds pre-allocated capacity.")

        val largeUpdateBuffer = ByteBuffer.allocateDirect(36).order(ByteOrder.nativeOrder())
        val exceptionSize =
            assertThrows(IllegalArgumentException::class.java) {
                meshBuffer.updateVertexData(
                    0,
                    ByteBufferRegion(largeUpdateBuffer, 0, 36),
                    offsetInBytes = 0,
                )
            }
        assertThat(exceptionSize)
            .hasMessageThat()
            .contains("Update exceeds pre-allocated capacity.")
    }

    @Test
    fun updateIndexData_onDynamicMeshBuffer_succeeds() {
        val meshBuffer =
            MeshBuffer.createDynamic(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 10,
                indexCount = 30,
            )
        val updateBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val region = ByteBufferRegion(updateBuffer, 0, 12)

        meshBuffer.updateIndexData(region, 0)
        meshBuffer.updateIndexData(region, 12)
    }

    @Test
    fun updateIndexData_onStaticMeshBuffer_throwsException() {
        val vertexBuffer1 = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val vertexBuffer2 = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val indexBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())

        val meshBuffer =
            MeshBuffer.create(
                session,
                vertexLayout,
                listOf(
                    ByteBufferRegion(vertexBuffer1, 0, 12),
                    ByteBufferRegion(vertexBuffer2, 0, 12),
                ),
                ByteBufferRegion(indexBuffer, 0, 12),
            )
        val updateBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())

        val exception =
            assertThrows(IllegalStateException::class.java) {
                meshBuffer.updateIndexData(ByteBufferRegion(updateBuffer, 0, 12))
            }
        assertThat(exception).hasMessageThat().contains("MeshBuffer is not dynamic.")
    }

    @Test
    fun updateIndexData_withNegativeOffset_throwsException() {
        val meshBuffer =
            MeshBuffer.createDynamic(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 10,
                indexCount = 30,
            )
        val updateBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val region = ByteBufferRegion(updateBuffer, 0, 12)

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                meshBuffer.updateIndexData(region, offsetInBytes = -1)
            }
        assertThat(exception).hasMessageThat().contains("offsetInBytes must be non-negative.")
    }

    @Test
    fun updateIndexData_exceedingCapacity_throwsException() {
        val meshBuffer =
            MeshBuffer.createDynamic(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 2,
                indexCount = 2,
            )
        val updateBuffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())

        val exceptionOffset =
            assertThrows(IllegalArgumentException::class.java) {
                meshBuffer.updateIndexData(ByteBufferRegion(updateBuffer, 0, 8), offsetInBytes = 4)
            }
        assertThat(exceptionOffset)
            .hasMessageThat()
            .contains("Update exceeds pre-allocated index capacity.")

        val largeUpdateBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder())
        val exceptionSize =
            assertThrows(IllegalArgumentException::class.java) {
                meshBuffer.updateIndexData(
                    ByteBufferRegion(largeUpdateBuffer, 0, 12),
                    offsetInBytes = 0,
                )
            }
        assertThat(exceptionSize)
            .hasMessageThat()
            .contains("Update exceeds pre-allocated index capacity.")
    }

    @Test
    fun close_destroysMeshBuffer() {
        val meshBuffer =
            MeshBuffer.createDynamic(
                session = session,
                vertexLayout = vertexLayout,
                vertexCount = 10,
                indexCount = 30,
            )
        meshBuffer.close()
    }
}
