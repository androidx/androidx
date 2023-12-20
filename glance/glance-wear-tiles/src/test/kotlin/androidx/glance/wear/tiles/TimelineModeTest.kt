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

package androidx.glance.wear.tiles

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class TimelineModeTest {

    @Test
    fun timeboundEntriesTest() {
        val time1 = Instant.parse("2021-11-12T13:15:30.00Z")
        val time2 = Instant.parse("2021-11-12T13:45:30.00Z")
        val time3 = Instant.parse("2021-11-12T17:45:30.00Z")
        val time4 = Instant.parse("2021-11-12T18:30:30.00Z")

        val timeBoundEntries = TimelineMode.TimeBoundEntries(setOf(
            TimeInterval(),
            TimeInterval(time1, time2),
            TimeInterval(time2, time3),
            TimeInterval(time4)
        ))
        val intervals = timeBoundEntries.timeIntervals

        assertThat(intervals.size).isEqualTo(4)

        assertThat(intervals.elementAt(0).start.toEpochMilli()).isEqualTo(0)
        assertThat(intervals.elementAt(0).end.toEpochMilli()).isEqualTo(Long.MAX_VALUE)

        assertThat(intervals.elementAt(1).start).isEqualTo(time1)
        assertThat(intervals.elementAt(1).end).isEqualTo(time2)

        assertThat(intervals.elementAt(2).start).isEqualTo(time2)
        assertThat(intervals.elementAt(2).end).isEqualTo(time3)

        assertThat(intervals.elementAt(3).start).isEqualTo(time4)
        assertThat(intervals.elementAt(3).end.toEpochMilli()).isEqualTo(Long.MAX_VALUE)
    }
}
