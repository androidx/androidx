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
    fun setUp() = runBlocking {
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
}
