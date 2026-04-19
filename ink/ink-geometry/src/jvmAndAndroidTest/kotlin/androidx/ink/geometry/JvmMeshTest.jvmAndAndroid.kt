/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertFails

class JvmMeshTest {

    @Test
    fun rawVertexData_emptyIsReadOnly() {
        val mesh = Mesh()
        val rawVertexData = mesh.getMeshOwnedRawVertexBuffer()
        assertThat(rawVertexData.isReadOnly).isTrue()
        // Fails with different exception type on different API levels.
        assertFails { rawVertexData.put(5.toByte()) }
        assertThat(rawVertexData.limit()).isEqualTo(0)
        assertThat(rawVertexData.capacity()).isEqualTo(0)
    }

    @Test
    fun rawTriangleIndexData_usesNativeByteOrder() {
        val mesh = Mesh()

        assertThat(mesh.getMeshOwnedRawTriangleIndexBuffer().order())
            .isEqualTo(java.nio.ByteOrder.nativeOrder())
    }

    @Test
    fun rawIndexData_emptyIsReadOnly() {
        val mesh = Mesh()

        val rawTriangleIndexData = mesh.getMeshOwnedRawTriangleIndexBuffer()
        assertThat(rawTriangleIndexData.isReadOnly).isTrue()
        // Fails with different exception type on different API levels.
        assertFails { rawTriangleIndexData.put(5) }
        assertThat(rawTriangleIndexData.limit()).isEqualTo(0)
        assertThat(rawTriangleIndexData.capacity()).isEqualTo(0)
    }
}
