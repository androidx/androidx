/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.complications.data

import androidx.wear.complications.SharedRobolectricTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SharedRobolectricTestRunner::class)
public class TimeRangeTest {
    @Test
    public fun beforeGivenTime() {
        val range = TimeRange.before(1000)
        assertThat(range.contains(100)).isTrue()
        assertThat(range.contains(999)).isTrue()
        assertThat(range.contains(1000)).isTrue()
        assertThat(range.contains(1001)).isFalse()
        assertThat(range.contains(10000)).isFalse()
    }

    @Test
    public fun afterGivenTime() {
        val range = TimeRange.after(1000)
        assertThat(range.contains(100)).isFalse()
        assertThat(range.contains(999)).isFalse()
        assertThat(range.contains(1000)).isTrue()
        assertThat(range.contains(1001)).isTrue()
        assertThat(range.contains(10000)).isTrue()
    }

    @Test
    public fun betweenTwoTimes() {
        val range = TimeRange.between(1000, 2000)
        assertThat(range.contains(100)).isFalse()
        assertThat(range.contains(999)).isFalse()
        assertThat(range.contains(1000)).isTrue()
        assertThat(range.contains(1001)).isTrue()
        assertThat(range.contains(1999)).isTrue()
        assertThat(range.contains(2000)).isTrue()
        assertThat(range.contains(2001)).isFalse()
        assertThat(range.contains(10000)).isFalse()
    }

    @Test
    public fun always() {
        var range = TimeRange.ALWAYS
        assertThat(range.contains(0)).isTrue()
        assertThat(range.contains(100)).isTrue()
        assertThat(range.contains(999)).isTrue()
        assertThat(range.contains(1000)).isTrue()
        assertThat(range.contains(1001)).isTrue()
        assertThat(range.contains(1999)).isTrue()
        assertThat(range.contains(2000)).isTrue()
        assertThat(range.contains(2001)).isTrue()
        assertThat(range.contains(10000)).isTrue()
        assertThat(range.contains(Long.MAX_VALUE)).isTrue()
    }
}

@RunWith(SharedRobolectricTestRunner::class)
public class TimeReferenceTest {
    @Test
    public fun startingAtTime() {
        val reference = TimeReference.starting(1000L)
        assertThat(reference.hasStartDateTimeMillis()).isTrue()
        assertThat(reference.startDateTimeMillis).isEqualTo(1000L)
        assertThat(reference.hasEndDateTimeMillis()).isFalse()
    }

    @Test
    public fun endingAtTime() {
        val reference = TimeReference.ending(1000L)
        assertThat(reference.hasStartDateTimeMillis()).isFalse()
        assertThat(reference.hasEndDateTimeMillis()).isTrue()
        assertThat(reference.endDateTimeMillis).isEqualTo(1000L)
    }
}
