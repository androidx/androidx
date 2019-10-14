/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TimestampTest {
    @Test
    fun durationArithmetic() {
        val fiveDaysNanos = NanosecondsPerDay * 5
        val startTime: Timestamp = fiveDaysNanos.nanosecondsToTimestamp()
        val endTime: Timestamp = startTime + 25.minutes
        assertEquals("end - start", 25.minutes, endTime - startTime)
        assertEquals("end - 25.minutes", startTime, endTime - 25.minutes)
    }
}