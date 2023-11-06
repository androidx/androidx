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

package androidx.window.embedding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The unit tests for [EmbeddingAspectRatio].
 */
class EmbeddingAspectRatioTest {

    @Test
    fun testRatio() {
        // ratio must > 1.
        assertThrows(IllegalArgumentException::class.java) {
            EmbeddingAspectRatio.ratio(0f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EmbeddingAspectRatio.ratio(1f)
        }

        // test equals and value.
        assertEquals(EmbeddingAspectRatio.ratio(1.1f), EmbeddingAspectRatio.ratio(1.1f))
        assertEquals(1.1f, EmbeddingAspectRatio.ratio(1.1f).value)
    }

    @Test
    fun testAlwaysAllow() {
        assertEquals(0f, EmbeddingAspectRatio.ALWAYS_ALLOW.value)
    }

    @Test
    fun testAlwaysDisallow() {
        assertEquals(-1f, EmbeddingAspectRatio.ALWAYS_DISALLOW.value)
    }

    @Test
    fun testBuildAspectRatioFromValue() {
        assertEquals(
            EmbeddingAspectRatio.ALWAYS_ALLOW,
            EmbeddingAspectRatio.buildAspectRatioFromValue(0f)
        )
        assertEquals(
            EmbeddingAspectRatio.ALWAYS_DISALLOW,
            EmbeddingAspectRatio.buildAspectRatioFromValue(-1f)
        )
        assertEquals(
            EmbeddingAspectRatio.ratio(1.1f),
            EmbeddingAspectRatio.buildAspectRatioFromValue(1.1f)
        )
    }
}
