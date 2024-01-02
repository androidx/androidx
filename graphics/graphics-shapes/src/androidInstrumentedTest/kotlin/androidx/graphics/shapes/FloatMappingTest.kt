/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.shapes

import androidx.test.filters.SmallTest
import org.junit.Test

@SmallTest
class FloatMappingTest {
    @Test
    fun identityMappingTest() = validateMapping(DoubleMapper.Identity) { it }

    @Test
    fun simpleMappingTest() = validateMapping(
        // Map the first half of the start source to the first quarter of the target.
        mapper = DoubleMapper(
            0f to 0f,
            0.5f to 0.25f
        )
    ) { x ->
        if (x < 0.5f) x / 2
        else (3 * x - 1) / 2
    }

    @Test
    fun targetWrapsTest() = validateMapping(
        // mapping applies a "+ 0.5f"
        mapper = DoubleMapper(
            0f to 0.5f,
            0.1f to 0.6f
        )
    ) { x -> (x + 0.5f) % 1f }

    @Test
    fun sourceWrapsTest() = validateMapping(
        // Values on the source wrap (this is still the "+ 0.5f" function)
        mapper = DoubleMapper(
            0.5f to 0f,
            0.1f to 0.6f
        )
    ) { x -> (x + 0.5f) % 1f }

    @Test
    fun bothWrapTest() = validateMapping(
        // Just the identity function
        mapper = DoubleMapper(
            0.5f to 0.5f,
            0.75f to 0.75f,
            0.1f to 0.1f,
            0.49f to 0.49f
        )
    ) { it }

    @Test
    fun multiplePointTes() = validateMapping(
        mapper = DoubleMapper(
            0.4f to 0.2f,
            0.5f to 0.22f,
            0f to 0.8f
        )
    ) { x ->
        if (x < 0.4f) {
            (0.8f + x) % 1f
        } else if (x < 0.5f) {
            0.2f + (x - 0.4f) / 5
        } else {
            // maps a change of 0.5 in the source to a change 0.58 in the target, hence the 1.16
            0.22f + (x - 0.5f) * 1.16f
        }
    }

    private fun validateMapping(mapper: DoubleMapper, expectedFunction: (Float) -> Float) {
        (0..9999).forEach { i ->
            val source = i / 10000f
            val target = expectedFunction(source)

            assertEqualish(target, mapper.map(source))
            assertEqualish(source, mapper.mapBack(target))
        }
    }
}
