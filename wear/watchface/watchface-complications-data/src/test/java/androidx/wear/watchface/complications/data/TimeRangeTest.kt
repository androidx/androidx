/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.complications.data

import androidx.wear.watchface.complications.SharedRobolectricTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(SharedRobolectricTestRunner::class)
public class TimeRangeTest {
    @Test
    public fun beforeGivenTime() {
        val range = TimeRange.before(Instant.ofEpochMilli(1000))
        assertThat(range.contains(Instant.ofEpochMilli(100))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(999))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(1000))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(1001))).isFalse()
        assertThat(range.contains(Instant.ofEpochMilli(10000))).isFalse()
    }

    @Test
    public fun afterGivenTime() {
        val range = TimeRange.after(Instant.ofEpochMilli(1000))
        assertThat(range.contains(Instant.ofEpochMilli(100))).isFalse()
        assertThat(range.contains(Instant.ofEpochMilli(999))).isFalse()
        assertThat(range.contains(Instant.ofEpochMilli(1000))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(1001))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(10000))).isTrue()
    }

    @Test
    public fun betweenTwoTimes() {
        val range = TimeRange.between(
            Instant.ofEpochMilli(1000),
            Instant.ofEpochMilli(2000)
        )
        assertThat(range.contains(Instant.ofEpochMilli(100))).isFalse()
        assertThat(range.contains(Instant.ofEpochMilli(999))).isFalse()
        assertThat(range.contains(Instant.ofEpochMilli(1000))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(1001))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(1999))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(2000))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(2001))).isFalse()
        assertThat(range.contains(Instant.ofEpochMilli(10000))).isFalse()
    }

    @Test
    public fun always() {
        var range = TimeRange.ALWAYS
        assertThat(range.contains(Instant.EPOCH)).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(100))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(999))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(1000))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(1001))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(1999))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(2000))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(2001))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(10000))).isTrue()
        assertThat(range.contains(Instant.ofEpochMilli(Long.MAX_VALUE))).isTrue()
    }
}