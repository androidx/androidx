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

@RunWith(JUnit4::class)
class VertexAttributeDescriptorTest {

    @Test
    fun create_withValidCombinations_succeeds() {
        val position =
            VertexAttributeDescriptor(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
        assertThat(position.attribute).isEqualTo(VertexAttribute.POSITION)
        assertThat(position.type).isEqualTo(VertexAttributeType.FLOAT3)

        val normalFloat3 =
            VertexAttributeDescriptor(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3)
        assertThat(normalFloat3.attribute).isEqualTo(VertexAttribute.NORMAL)

        val normalFloat4 =
            VertexAttributeDescriptor(VertexAttribute.NORMAL, VertexAttributeType.FLOAT4)
        assertThat(normalFloat4.type).isEqualTo(VertexAttributeType.FLOAT4)

        val color =
            VertexAttributeDescriptor(VertexAttribute.COLOR, VertexAttributeType.UBYTE4_NORM)
        assertThat(color.type).isEqualTo(VertexAttributeType.UBYTE4_NORM)

        val uv0 = VertexAttributeDescriptor(VertexAttribute.UV0, VertexAttributeType.FLOAT2)
        assertThat(uv0.type).isEqualTo(VertexAttributeType.FLOAT2)

        val uv1 = VertexAttributeDescriptor(VertexAttribute.UV1, VertexAttributeType.FLOAT2)
        assertThat(uv1.type).isEqualTo(VertexAttributeType.FLOAT2)

        val boneIndices =
            VertexAttributeDescriptor(VertexAttribute.BONE_INDICES, VertexAttributeType.UBYTE4)
        assertThat(boneIndices.type).isEqualTo(VertexAttributeType.UBYTE4)

        val boneWeights =
            VertexAttributeDescriptor(VertexAttribute.BONE_WEIGHTS, VertexAttributeType.UBYTE4_NORM)
        assertThat(boneWeights.type).isEqualTo(VertexAttributeType.UBYTE4_NORM)
    }

    @Test
    fun create_withInvalidCombinations_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            VertexAttributeDescriptor(VertexAttribute.POSITION, VertexAttributeType.FLOAT2)
        }

        assertThrows(IllegalArgumentException::class.java) {
            VertexAttributeDescriptor(VertexAttribute.NORMAL, VertexAttributeType.FLOAT2)
        }

        assertThrows(IllegalArgumentException::class.java) {
            VertexAttributeDescriptor(VertexAttribute.COLOR, VertexAttributeType.UBYTE4)
        }

        assertThrows(IllegalArgumentException::class.java) {
            VertexAttributeDescriptor(VertexAttribute.UV0, VertexAttributeType.FLOAT3)
        }

        assertThrows(IllegalArgumentException::class.java) {
            VertexAttributeDescriptor(VertexAttribute.UV1, VertexAttributeType.FLOAT4)
        }

        assertThrows(IllegalArgumentException::class.java) {
            VertexAttributeDescriptor(VertexAttribute.BONE_INDICES, VertexAttributeType.UBYTE4_NORM)
        }

        assertThrows(IllegalArgumentException::class.java) {
            VertexAttributeDescriptor(VertexAttribute.BONE_WEIGHTS, VertexAttributeType.UBYTE4)
        }
    }

    @Test
    fun create_withInvalidOffset_throwsIllegalArgumentException() {
        var exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexAttributeDescriptor(
                    VertexAttribute.POSITION,
                    VertexAttributeType.FLOAT3,
                    offset = -2,
                )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("offset must be AUTO_OFFSET or between 0 and 32767")

        exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexAttributeDescriptor(
                    VertexAttribute.POSITION,
                    VertexAttributeType.FLOAT3,
                    offset = 32768,
                )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("offset must be AUTO_OFFSET or between 0 and 32767")
    }
}
