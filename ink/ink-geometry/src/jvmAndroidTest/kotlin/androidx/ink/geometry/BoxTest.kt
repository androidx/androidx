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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BoxTest {

    @Test
    fun isAlmostEqual_withToleranceGiven_returnsCorrectValue() {
        val box = ImmutableBox.fromTwoPoints(ImmutableVec(1f, 2f), ImmutableVec(3f, 4f))

        assertThat(box.isAlmostEqual(box, tolerance = 0.00000001f)).isTrue()
        assertThat(
                box.isAlmostEqual(
                    ImmutableBox.fromTwoPoints(ImmutableVec(1f, 2f), ImmutableVec(3f, 4f)),
                    tolerance = 0.00000001f,
                )
            )
            .isTrue()
        assertThat(
                box.isAlmostEqual(
                    ImmutableBox.fromTwoPoints(
                        ImmutableVec(1.00001f, 1.99999f),
                        ImmutableVec(3f, 4f)
                    ),
                    tolerance = 0.000001f,
                )
            )
            .isFalse()
        assertThat(
                box.isAlmostEqual(
                    ImmutableBox.fromTwoPoints(
                        ImmutableVec(1f, 2f),
                        ImmutableVec(3.00001f, 3.99999f)
                    ),
                    tolerance = 0.000001f,
                )
            )
            .isFalse()
        assertThat(
                box.isAlmostEqual(
                    ImmutableBox.fromTwoPoints(ImmutableVec(1f, 1.99f), ImmutableVec(3f, 4f)),
                    tolerance = 0.02f,
                )
            )
            .isTrue()
        assertThat(
                box.isAlmostEqual(
                    ImmutableBox.fromTwoPoints(ImmutableVec(1f, 2f), ImmutableVec(3.01f, 4f)),
                    tolerance = 0.02f,
                )
            )
            .isTrue()
    }

    @Test
    fun isAlmostEqual_whenSameInterface_returnsTrue() {
        val box = MutableBox().populateFromTwoPoints(ImmutableVec(1f, 2f), ImmutableVec(3f, 4f))
        val other =
            ImmutableBox.fromTwoPoints(
                ImmutableVec(0.99999f, 2.00001f),
                ImmutableVec(3.00001f, 3.99999f)
            )
        assertThat(box.isAlmostEqual(other, tolerance = 0.0001f)).isTrue()
    }
}
