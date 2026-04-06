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

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCustomMeshApi::class)
@RunWith(JUnit4::class)
class VertexLayoutTest {

    @Test
    fun create_withValidAttributes_succeeds() {
        val attributes =
            listOf(
                VertexAttributeDescriptor(VertexAttribute.POSITION, VertexAttributeType.FLOAT3),
                VertexAttributeDescriptor(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3),
                VertexAttributeDescriptor(VertexAttribute.UV0, VertexAttributeType.FLOAT2),
            )
        val layout = VertexLayout(attributes)
        assertThat(layout.attributes).isEqualTo(attributes)
    }

    @Test
    fun create_withoutPosition_throwsIllegalArgumentException() {
        val attributes =
            listOf(
                VertexAttributeDescriptor(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3),
                VertexAttributeDescriptor(VertexAttribute.UV0, VertexAttributeType.FLOAT2),
            )
        assertThrows(IllegalArgumentException::class.java) { VertexLayout(attributes) }
    }

    @Test
    fun create_withDuplicateAttributes_throwsIllegalArgumentException() {
        val attributes =
            listOf(
                VertexAttributeDescriptor(VertexAttribute.POSITION, VertexAttributeType.FLOAT3),
                VertexAttributeDescriptor(VertexAttribute.UV0, VertexAttributeType.FLOAT2),
                VertexAttributeDescriptor(
                    VertexAttribute.UV0,
                    VertexAttributeType.FLOAT2,
                    bufferIndex = 1,
                ),
            )
        val exception =
            assertThrows(IllegalArgumentException::class.java) { VertexLayout(attributes) }
        assertThat(exception)
            .hasMessageThat()
            .contains("VertexLayout cannot contain duplicate attributes: UV0")
    }
}
