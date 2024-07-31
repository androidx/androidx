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
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MeshAttributeUnpackingParamsTest {

    @Test
    fun create1_hasExpectedComponentFields() {
        val transform = MeshAttributeUnpackingParams.create(1F, 2F)

        assertThat(transform.components).hasSize(1)
        assertThat(transform.components[0].offset).isEqualTo(1F)
        assertThat(transform.components[0].scale).isEqualTo(2F)
    }

    @Test
    fun createWithArrays_hasExpectedComponentFields() {
        val transform =
            MeshAttributeUnpackingParams.create(
                floatArrayOf(1F, 2F, 3F, 4F),
                floatArrayOf(5F, 6F, 7F, 8F),
            )

        assertThat(transform.components).hasSize(4)
        assertThat(transform.components[0].offset).isEqualTo(1F)
        assertThat(transform.components[1].offset).isEqualTo(2F)
        assertThat(transform.components[2].offset).isEqualTo(3F)
        assertThat(transform.components[3].offset).isEqualTo(4F)
        assertThat(transform.components[0].scale).isEqualTo(5F)
        assertThat(transform.components[1].scale).isEqualTo(6F)
        assertThat(transform.components[2].scale).isEqualTo(7F)
        assertThat(transform.components[3].scale).isEqualTo(8F)
    }

    @Test
    fun createWithArrays_whenTooSmall_throws() {
        assertFailsWith<IllegalArgumentException> {
            MeshAttributeUnpackingParams.create(floatArrayOf(), floatArrayOf())
        }
    }

    @Test
    fun createWithArrays_whenTooBig_throws() {
        assertFailsWith<IllegalArgumentException> {
            MeshAttributeUnpackingParams.create(
                floatArrayOf(1F, 2F, 3F, 4F, 5F),
                floatArrayOf(6F, 7F, 8F, 9F, 10F),
            )
        }
    }

    @Test
    fun createWithArrays_whenMixedSizes_throws() {
        assertFailsWith<IllegalArgumentException> {
            MeshAttributeUnpackingParams.create(floatArrayOf(1F, 2F), floatArrayOf(3F, 4F, 5F))
        }
    }

    @Test
    fun equals_whenSameInstance_returnsTrueAndHasSameHashCode() {
        val transforms =
            listOf(
                MeshAttributeUnpackingParams.create(1F, 2F),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(7F, 8F),
                    )
                ),
            )

        for (transform in transforms) {
            assertThat(transform).isEqualTo(transform)
            assertThat(transform.hashCode()).isEqualTo(transform.hashCode())
        }
    }

    @Test
    fun equals_whenDifferentClass_returnsFalse() {
        val transforms =
            listOf(
                MeshAttributeUnpackingParams.create(1F, 2F),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(7F, 8F),
                    )
                ),
            )
        val notATransform =
            ImmutableBox.fromTwoPoints(ImmutablePoint(2F, 4F), ImmutablePoint(1F, 3F))

        for (transform in transforms) {
            assertThat(transform).isNotEqualTo(notATransform)
            assertThat(notATransform).isNotEqualTo(transform)
        }
    }

    @Test
    fun equals_whenDifferentComponentCount_returnsFalse() {
        val transforms =
            listOf(
                MeshAttributeUnpackingParams.create(1F, 2F),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(7F, 8F),
                    )
                ),
            )

        for (transformA in transforms) {
            for (transformB in transforms) {
                if (transformA === transformB) continue // Same instance, would be equal.
                assertThat(transformA).isNotEqualTo(transformB)
                assertThat(transformB).isNotEqualTo(transformA)
            }
        }
    }

    @Test
    fun equals_whenDifferentOffset_returnsFalse() {
        val transformsA =
            listOf(
                MeshAttributeUnpackingParams.create(1F, 2F),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(7F, 8F),
                    )
                ),
            )
        val transformsB =
            listOf(
                MeshAttributeUnpackingParams.create(10F, 2F),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(30F, 4F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(50F, 6F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(70F, 8F),
                    )
                ),
            )

        for ((transformA, transformB) in transformsA.zip(transformsB)) {
            assertThat(transformA).isNotEqualTo(transformB)
            assertThat(transformB).isNotEqualTo(transformA)
        }
    }

    @Test
    fun equals_whenDifferentScale_returnsFalse() {
        val transformsA =
            listOf(
                MeshAttributeUnpackingParams.create(1F, 2F),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(7F, 8F),
                    )
                ),
            )
        val transformsB =
            listOf(
                MeshAttributeUnpackingParams.create(1F, 20F),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 40F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 60F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(7F, 80F),
                    )
                ),
            )

        for ((transformA, transformB) in transformsA.zip(transformsB)) {
            assertThat(transformA).isNotEqualTo(transformB)
            assertThat(transformB).isNotEqualTo(transformA)
        }
    }

    @Test
    fun equals_whenDifferentInstanceButEquivalent_returnsTrueAndHasSameHashCode() {
        val transformsA =
            listOf(
                MeshAttributeUnpackingParams.create(1F, 2F),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(7F, 8F),
                    )
                ),
            )
        val transformsB =
            listOf(
                MeshAttributeUnpackingParams.create(1F, 2F),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                    )
                ),
                MeshAttributeUnpackingParams(
                    listOf(
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(1F, 2F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(3F, 4F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(5F, 6F),
                        MeshAttributeUnpackingParams.ComponentUnpackingParams(7F, 8F),
                    )
                ),
            )

        for ((transformA, transformB) in transformsA.zip(transformsB)) {
            assertThat(transformA).isEqualTo(transformB)
            assertThat(transformB).isEqualTo(transformA)
            assertThat(transformA.hashCode()).isEqualTo(transformB.hashCode())
        }
    }
}
