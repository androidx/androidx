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
class VertexLayoutTest {

    @Test
    fun create_withValidAttributes_succeeds() {
        val layout =
            VertexLayout.Builder()
                .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3)
                .addAttribute(VertexAttribute.UV0, VertexAttributeType.FLOAT2)
                .build()
        assertThat(layout.buffers.size).isEqualTo(1)
        assertThat(layout.buffers[0].attributes.size).isEqualTo(3)
        assertThat(layout.buffers[0].attributes[0].attribute).isEqualTo(VertexAttribute.POSITION)
        assertThat(layout.buffers[0].attributes[1].attribute).isEqualTo(VertexAttribute.NORMAL)
        assertThat(layout.buffers[0].attributes[2].attribute).isEqualTo(VertexAttribute.UV0)
    }

    @Test
    fun create_withoutPosition_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexLayout.Builder()
                    .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3)
                    .addAttribute(VertexAttribute.UV0, VertexAttributeType.FLOAT2)
                    .build()
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("VertexLayout must contain a POSITION attribute.")
    }

    @Test
    fun create_withBoneIndicesWithoutBoneWeights_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexLayout.Builder()
                    .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                    .addAttribute(VertexAttribute.BONE_INDICES, VertexAttributeType.UBYTE4)
                    .build()
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("VertexLayout must contain both BONE_INDICES and BONE_WEIGHTS, or neither.")
    }

    @Test
    fun create_withBoneWeightsWithoutBoneIndices_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexLayout.Builder()
                    .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                    .addAttribute(VertexAttribute.BONE_WEIGHTS, VertexAttributeType.UBYTE4_NORM)
                    .build()
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("VertexLayout must contain both BONE_INDICES and BONE_WEIGHTS, or neither.")
    }

    @Test
    fun create_withBothBoneIndicesAndBoneWeights_succeeds() {
        val layout =
            VertexLayout.Builder()
                .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                .addAttribute(VertexAttribute.BONE_INDICES, VertexAttributeType.UBYTE4)
                .addAttribute(VertexAttribute.BONE_WEIGHTS, VertexAttributeType.UBYTE4_NORM)
                .build()
        assertThat(layout.buffers.size).isEqualTo(1)
        assertThat(layout.buffers[0].attributes.size).isEqualTo(3)
        assertThat(layout.buffers[0].attributes[0].attribute).isEqualTo(VertexAttribute.POSITION)
        assertThat(layout.buffers[0].attributes[1].attribute)
            .isEqualTo(VertexAttribute.BONE_INDICES)
        assertThat(layout.buffers[0].attributes[2].attribute)
            .isEqualTo(VertexAttribute.BONE_WEIGHTS)
    }

    @Test
    fun addAttribute_withDuplicateAttributeParams_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexLayout.Builder()
                    .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                    .addAttribute(VertexAttribute.UV0, VertexAttributeType.FLOAT2)
                    .addAttribute(VertexAttribute.UV0, VertexAttributeType.FLOAT2)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("VertexLayout cannot contain duplicate attributes: UV0")
    }

    @Test
    fun addAttribute_withDuplicateAttributeDescriptor_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexLayout.Builder()
                    .addAttribute(
                        VertexAttributeDescriptor(
                            VertexAttribute.POSITION,
                            VertexAttributeType.FLOAT3,
                        )
                    )
                    .addAttribute(
                        VertexAttributeDescriptor(VertexAttribute.UV0, VertexAttributeType.FLOAT2)
                    )
                    .addAttribute(
                        VertexAttributeDescriptor(VertexAttribute.UV0, VertexAttributeType.FLOAT2)
                    )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("VertexLayout cannot contain duplicate attributes: UV0")
    }

    @Test
    fun addAttribute_withDuplicateAttributeAcrossBuffers_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexLayout.Builder()
                    .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                    .startNextBuffer()
                    .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("VertexLayout cannot contain duplicate attributes: POSITION")
    }

    @Test
    fun createBufferLayout_withOverlappingAttributes_throwsIllegalArgumentException() {
        var exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexBufferLayout(
                    listOf(
                        VertexAttributeDescriptor(
                            VertexAttribute.POSITION,
                            VertexAttributeType.FLOAT3,
                            offset = 0,
                        ),
                        VertexAttributeDescriptor(
                            VertexAttribute.NORMAL,
                            VertexAttributeType.FLOAT3,
                            offset = 4,
                        ),
                    )
                )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains(
                "Attribute NORMAL overlaps with existing attribute POSITION in the same buffer."
            )

        exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexBufferLayout(
                    listOf(
                        VertexAttributeDescriptor(
                            VertexAttribute.POSITION,
                            VertexAttributeType.FLOAT3,
                            offset = 4,
                        ),
                        VertexAttributeDescriptor(
                            VertexAttribute.NORMAL,
                            VertexAttributeType.FLOAT3,
                            offset = 0,
                        ),
                    )
                )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains(
                "Attribute NORMAL overlaps with existing attribute POSITION in the same buffer."
            )

        exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexBufferLayout(
                    listOf(
                        VertexAttributeDescriptor(
                            VertexAttribute.POSITION,
                            VertexAttributeType.FLOAT3,
                            offset = 12,
                        ),
                        VertexAttributeDescriptor(
                            VertexAttribute.NORMAL,
                            VertexAttributeType.FLOAT3,
                            offset = 0,
                        ),
                        VertexAttributeDescriptor(
                            VertexAttribute.UV0,
                            VertexAttributeType.FLOAT2,
                        ), // AUTO_OFFSET = 12
                    )
                )
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Attribute UV0 overlaps with existing attribute POSITION in the same buffer.")
    }

    @Test
    fun create_withEmptyBuffers_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) { VertexLayout.Builder().build() }
        assertThat(exception)
            .hasMessageThat()
            .contains("VertexLayout must contain at least one buffer")
    }

    @Test
    fun create_withNextBufferWithoutAttributes_throwsIllegalStateException() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                VertexLayout.Builder()
                    .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                    .startNextBuffer()
                    .startNextBuffer()
                    .build()
            }
        assertThat(exception)
            .hasMessageThat()
            .contains(
                "Cannot call startNextBuffer() with no attributes added to the current buffer."
            )
    }

    @Test
    fun createBufferLayout_withEmptyAttributes_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) { VertexBufferLayout(emptyList()) }
        assertThat(exception)
            .hasMessageThat()
            .contains("VertexBufferLayout must contain at least one attribute")
    }

    @Test
    fun builder_setInvalidStride_throwsIllegalArgumentException() {
        var exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexLayout.Builder().setStride(0)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("stride must be AUTO_STRIDE or between 1 and 32767")

        exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexLayout.Builder().setStride(-2)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("stride must be AUTO_STRIDE or between 1 and 32767")

        exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexLayout.Builder().setStride(32768)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("stride must be AUTO_STRIDE or between 1 and 32767")
    }

    @Test
    fun createBufferLayout_withInvalidStride_throwsIllegalArgumentException() {
        val attributes =
            listOf(VertexAttributeDescriptor(VertexAttribute.POSITION, VertexAttributeType.FLOAT3))

        var exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexBufferLayout(attributes, stride = 0)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("stride must be AUTO_STRIDE or between 1 and 32767")

        exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexBufferLayout(attributes, stride = -2)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("stride must be AUTO_STRIDE or between 1 and 32767")

        exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexBufferLayout(attributes, stride = 32768)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("stride must be AUTO_STRIDE or between 1 and 32767")
    }

    @Test
    fun createBufferLayout_withStrideTooSmall_throwsIllegalArgumentException() {
        val attributes =
            listOf(
                VertexAttributeDescriptor(
                    VertexAttribute.POSITION,
                    VertexAttributeType.FLOAT3,
                    offset = 0,
                ),
                VertexAttributeDescriptor(
                    VertexAttribute.NORMAL,
                    VertexAttributeType.FLOAT3,
                    offset = 12,
                ),
            )

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexBufferLayout(attributes, stride = 20) // Normal ends at 12 + 12 = 24
            }
        assertThat(exception)
            .hasMessageThat()
            .contains(
                "stride (20) must be at least the minimum byte stride required to encompass all attributes in the buffer (24)."
            )
    }

    @Test
    fun builder_withStrideTooSmall_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                VertexLayout.Builder()
                    .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3, offset = 0)
                    .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3, offset = 12)
                    .setStride(20) // Normal ends at 12 + 12 = 24
                    .build()
            }
        assertThat(exception)
            .hasMessageThat()
            .contains(
                "stride (20) must be at least the minimum byte stride required to encompass all attributes in the buffer (24)."
            )
    }
}
