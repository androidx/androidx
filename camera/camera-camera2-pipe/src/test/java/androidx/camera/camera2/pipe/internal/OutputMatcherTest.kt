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

package androidx.camera.camera2.pipe.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [OutputMatcher]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.NEWEST_SDK])
class OutputMatcherTest {

    @Test
    fun matcher_fuzzyEqual() {
        val matcher = OutputMatcher(initialOffset = 105, errorDelta = 6)

        assertThat(matcher.fuzzyEqual(100, 0)).isFalse()
        assertThat(matcher.fuzzyEqual(100, 198)).isFalse()
        assertThat(matcher.fuzzyEqual(100, 205)).isTrue()
        assertThat(matcher.fuzzyEqual(100, 207)).isTrue()
        assertThat(matcher.fuzzyEqual(100, 215)).isFalse()
    }

    @Test
    fun matcher_fuzzyLessThan() {
        // Example:
        // img   ssr   off   error   v  <
        // 0   - 100 - 105 + 6 =  -199  true
        // 198 - 100 - 105 + 6 =    -1  true  // less than
        // 205 - 100 - 105 + 6 =     6  false // exact equals
        // 207 - 100 - 105 + 6 =     8  false // fuzzy equals
        // 215 - 100 - 105 + 6 =    16  false // greater than

        val matcher = OutputMatcher(initialOffset = 105, errorDelta = 6)

        assertThat(matcher.fuzzyLessThan(100, 0)).isTrue()
        assertThat(matcher.fuzzyLessThan(100, 198)).isTrue()
        assertThat(matcher.fuzzyLessThan(100, 205)).isFalse()
        assertThat(matcher.fuzzyLessThan(100, 207)).isFalse()
        assertThat(matcher.fuzzyLessThan(100, 215)).isFalse()
    }

    @Test
    fun matcher_fuzzyLessThanEquals() {
        // Example:
        // img   ssr   off   error   v  <=
        // 0   - 100 - 105 - 6 =  -211  true
        // 198 - 100 - 105 - 6 =   -13  true  // less than
        // 205 - 100 - 105 - 6 =    -6  true  // exact equals
        // 207 - 100 - 105 - 6 =    -4  true  // fuzzy equals
        // 215 - 100 - 105 - 6 =     4  false // greater than

        val matcher = OutputMatcher(initialOffset = 105, errorDelta = 6)

        assertThat(matcher.fuzzyLessThanOrEqual(100, 0)).isTrue()
        assertThat(matcher.fuzzyLessThanOrEqual(100, 198)).isTrue()
        assertThat(matcher.fuzzyLessThanOrEqual(100, 205)).isTrue()
        assertThat(matcher.fuzzyLessThanOrEqual(100, 207)).isTrue()
        assertThat(matcher.fuzzyLessThanOrEqual(100, 215)).isFalse()
    }

    @Test
    fun matcher_realExampleTest() {
        val matcher = OutputMatcher.forTimestampsWithOffset(initialOffset = -36480495215790L)

        assertThat(matcher.fuzzyEqual(86161121748100L, 49680626532310L)).isTrue()
        assertThat(matcher.fuzzyEqual(86161121748100L, 49680626500000L)).isTrue()
        assertThat(matcher.fuzzyEqual(86161121748100L, 49680610000000L)).isFalse()
        assertThat(matcher.fuzzyLessThan(86161121748100L, 49680660307860L)).isFalse()
    }

    @Test
    fun matcher_exactBehavesTheSameAsNormalCompare() {
        val matcher = OutputMatcher.EXACT

        assertThat(matcher.fuzzyEqual(42, 41)).isFalse()
        assertThat(matcher.fuzzyEqual(42, 42)).isTrue()
        assertThat(matcher.fuzzyEqual(42, 43)).isFalse()

        assertThat(matcher.fuzzyLessThan(42, 41)).isTrue()
        assertThat(matcher.fuzzyLessThan(42, 42)).isFalse()
        assertThat(matcher.fuzzyLessThan(42, 43)).isFalse()

        assertThat(matcher.fuzzyLessThanOrEqual(42, 41)).isTrue()
        assertThat(matcher.fuzzyLessThanOrEqual(42, 42)).isTrue()
        assertThat(matcher.fuzzyLessThanOrEqual(42, 43)).isFalse()

        assertThat(matcher.fuzzyGreaterThan(42, 41)).isFalse()
        assertThat(matcher.fuzzyGreaterThan(42, 42)).isFalse()
        assertThat(matcher.fuzzyGreaterThan(42, 43)).isTrue()

        assertThat(matcher.fuzzyGreaterThanOrEqual(42, 41)).isFalse()
        assertThat(matcher.fuzzyGreaterThanOrEqual(42, 42)).isTrue()
        assertThat(matcher.fuzzyGreaterThanOrEqual(42, 43)).isTrue()
    }

    @Test
    fun fuzzyEqual_updatesOffsetOnInexactMatch() {
        // Start with offset 100, error delta 10.
        // Formula: delta = cameraOutputNumber - outputNumber + currentOffset
        val matcher = OutputMatcher(initialOffset = 100L, errorDelta = 10L)

        // 1. Exact match: delta is 0. Offset should NOT change.
        // 500 - 600 + 100 = 0
        assertThat(matcher.fuzzyEqual(500, 600)).isTrue()

        // 2. Inexact match within error delta (10).
        // 500 - 605 + 100 = -5 (which is > -10 and < 10)
        // New offset should become: offset - delta => 100 - (-5) = 105
        assertThat(matcher.fuzzyEqual(500, 605)).isTrue()

        // 3. Verify offset updated to 105 by checking an exact match with the new offset.
        // 500 - 614 + 105 = 0 (Fuzzy match succeeds because internal delta was updated)
        assertThat(matcher.fuzzyEqual(500, 614)).isTrue()
    }

    @Test
    fun fuzzyEqual_doesNotUpdateOffsetOnFailedMatch() {
        val matcher = OutputMatcher(initialOffset = 100L, errorDelta = 10L)

        // Match fails (delta = 20, which is > errorDelta 10)
        // 500 - 580 + 100 = 20
        assertThat(matcher.fuzzyEqual(500, 580)).isFalse()

        // Verify offset is still 100 by performing an exact match
        assertThat(matcher.fuzzyEqual(500, 600)).isTrue()
    }

    @Test
    fun fuzzyEqual_handlesLargeDriftUpdates() {
        // Simulate a scenario where the clocks are slowly drifting
        val matcher = OutputMatcher(initialOffset = 0L, errorDelta = 100L)

        // First match at 1000, but image is at 1050.
        // Delta = 1000 - 1050 + 0 = -50. New Offset = 50.
        assertThat(matcher.fuzzyEqual(1000, 1050)).isTrue()

        // Second match: sensor at 2000.
        // If offset was still 0, 2000 - 2050 + 0 = -50 (fuzzy)
        // Since offset is now 50, 2000 - 2050 + 50 = 0 (exact)
        assertThat(matcher.fuzzyEqual(2000, 2050)).isTrue()
    }
}
