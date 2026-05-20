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
import java.util.WeakHashMap
import kotlin.test.Test
import kotlin.test.assertFails

class JvmMeshTest {

    @Test
    fun rawVertexData_emptyIsReadOnly() {
        val mesh = Mesh()
        val rawVertexData = mesh.getRawVertexBuffer()
        assertThat(rawVertexData.isReadOnly).isTrue()
        // Fails with different exception type on different API levels.
        assertFails { rawVertexData.put(5.toByte()) }
        assertThat(rawVertexData.limit()).isEqualTo(0)
        assertThat(rawVertexData.capacity()).isEqualTo(0)
    }

    @Test
    fun rawTriangleIndexData_usesNativeByteOrder() {
        val mesh = Mesh()

        assertThat(mesh.getRawTriangleIndexBuffer().order())
            .isEqualTo(java.nio.ByteOrder.nativeOrder())
    }

    @Test
    fun rawIndexData_emptyIsReadOnly() {
        val mesh = Mesh()

        val rawTriangleIndexData = mesh.getRawTriangleIndexBuffer()
        assertThat(rawTriangleIndexData.isReadOnly).isTrue()
        // Fails with different exception type on different API levels.
        assertFails { rawTriangleIndexData.put(5) }
        assertThat(rawTriangleIndexData.limit()).isEqualTo(0)
        assertThat(rawTriangleIndexData.capacity()).isEqualTo(0)
    }

    @Test
    fun rawVertexData_retainsWeakReferenceToMeshFromOriginalDirectBuffer() {
        val mesh = Mesh()
        val unused = mesh.getRawVertexBuffer()
        assertThat(meshesReferencedByBuffers).isInstanceOf<WeakHashMap<*, *>>()
        // Unfortunately, we need to map from the _original_ direct buffer to the mesh, not the
        // wrapped
        // buffer that's ultimately returned by this getter. The internals of DirectByteBuffer
        // ensure
        // that methods which slice or duplicate the buffer retain a reference to the original
        // buffer,
        // but not any intermediate copies. So retaining a weak reference to the original ensures
        // that
        // any further copies/slices also do the right thing. But this reference back to the
        // original
        // buffer isn't in the public API, so we can't assert about it.
        assertThat(meshesReferencedByBuffers.values).contains(mesh)
        val reversedMap = meshesReferencedByBuffers.entries.associate { (k, v) -> v to k }
        val originalDirectBuffer = reversedMap[mesh]!!
        assertThat(originalDirectBuffer.isDirect()).isTrue()
    }

    @Test
    fun rawIndexData_retainsWeakReferenceToMeshFromOriginalDirectBuffer() {
        val mesh = Mesh()
        val unused = mesh.getRawTriangleIndexBuffer()
        assertThat(meshesReferencedByBuffers).isInstanceOf<WeakHashMap<*, *>>()
        // See comment above about why this entry maps to the original direct buffer, not the
        // wrapped
        // one.
        assertThat(meshesReferencedByBuffers.values).contains(mesh)
        val reversedMap = meshesReferencedByBuffers.entries.associate { (k, v) -> v to k }
        val originalDirectBuffer = reversedMap[mesh]!!
        assertThat(originalDirectBuffer.isDirect()).isTrue()
    }
}
