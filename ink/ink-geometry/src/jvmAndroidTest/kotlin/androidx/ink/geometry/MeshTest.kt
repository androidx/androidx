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

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MeshTest {

    @Test
    fun rawVertexData_emptyIsReadOnly() {
        val mesh = Mesh()

        assertThat(mesh.rawVertexData.isReadOnly).isTrue()
        // Fails with different exception type on different API levels.
        assertFails { mesh.rawVertexData.put(5.toByte()) }
        assertThat(mesh.rawVertexData.limit()).isEqualTo(0)
        assertThat(mesh.rawVertexData.capacity()).isEqualTo(0)
    }

    @Test
    fun vertexStride_matchesDefault() {
        val mesh = Mesh()

        assertThat(mesh.vertexStride).isEqualTo(8)
    }

    @Test
    fun vertexCount_isZero() {
        val mesh = Mesh()

        assertThat(mesh.vertexCount).isEqualTo(0)
    }

    @Test
    fun rawIndexData_emptyIsReadOnly() {
        val mesh = Mesh()

        assertThat(mesh.rawTriangleIndexData.isReadOnly).isTrue()
        // Fails with different exception type on different API levels.
        assertFails { mesh.rawTriangleIndexData.put(5) }
        assertThat(mesh.rawTriangleIndexData.limit()).isEqualTo(0)
        assertThat(mesh.rawTriangleIndexData.capacity()).isEqualTo(0)
    }

    @Test
    fun triangleCount_isZero() {
        val mesh = Mesh()

        assertThat(mesh.triangleCount).isEqualTo(0)
    }

    @Test
    fun bounds_hasNoBounds() {
        val mesh = Mesh()

        assertThat(mesh.bounds).isNull()
    }

    @Test
    fun vertexAttributeUnpackingParams_hasValues() {
        val mesh = Mesh()

        assertThat(mesh.vertexAttributeUnpackingParams)
            .containsExactly(
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(0f, 1f),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(0f, 1f),
                    )
                )
            )
    }

    @Test
    fun fillPosition_shouldThrow() {
        val mesh = Mesh()

        assertFailsWith<IllegalArgumentException> { mesh.fillPosition(-1, MutableVec()) }
        assertFailsWith<IllegalArgumentException> { mesh.fillPosition(0, MutableVec()) }
        assertFailsWith<IllegalArgumentException> { mesh.fillPosition(1, MutableVec()) }
    }

    @Test
    fun toString_returnsAString() {
        val string = Mesh().toString()

        // Not elaborate checks - this test mainly exists to ensure that toString doesn't crash.
        assertThat(string).contains("Mesh")
        assertThat(string).contains("bounds")
        assertThat(string).contains("vertexCount")
        assertThat(string).contains("nativeAddress")
    }
}
