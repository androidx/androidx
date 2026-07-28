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

package androidx.camera.camera2.pipe.core

import android.os.SystemClock
import androidx.camera.camera2.pipe.core.SystemClockOffsets.Companion.monotonicNsToRealtimeNs
import androidx.camera.camera2.pipe.core.SystemClockOffsets.Companion.realtimeNsToMonotonicNs
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 29)
class SystemClockOffsetsTest {

    @Test
    fun fixedSystemClockOffsets_translatesRealtimeToMonotonic() {
        val offsets =
            SystemClockOffsets.fixed(
                realtimeNsToUtcMs = 0L,
                realtimeNsToMonotonicNs = -100_000_000L,
            )
        val monotonicNs = offsets.realtimeNsToMonotonicNs(realtimeNs = 500_000_000L)

        assertThat(monotonicNs).isEqualTo(400_000_000L)
    }

    @Test
    fun fixedSystemClockOffsets_translatesMonotonicToRealtime() {
        val offsets =
            SystemClockOffsets.fixed(
                realtimeNsToUtcMs = 0L,
                realtimeNsToMonotonicNs = -100_000_000L,
            )
        val realtimeNs = offsets.monotonicNsToRealtimeNs(monotonicNs = 400_000_000L)

        assertThat(realtimeNs).isEqualTo(500_000_000L)
    }

    @Test
    fun estimateSystemClockOffsets_translatesRealtimeToMonotonic() {
        val offsets = SystemClockOffsets.estimate()
        val currentRealtimeNs = SystemClock.elapsedRealtimeNanos()
        val currentMonotonicNs = System.nanoTime()

        val translatedMonotonicNs = offsets.realtimeNsToMonotonicNs(currentRealtimeNs)

        val diff = abs(translatedMonotonicNs - currentMonotonicNs)
        assertThat(diff).isLessThan(10_000_000L)
    }

    @Test
    fun estimateSystemClockOffsets_translatesMonotonicToRealtime() {
        val offsets = SystemClockOffsets.estimate()
        val currentRealtimeNs = SystemClock.elapsedRealtimeNanos()
        val currentMonotonicNs = System.nanoTime()

        val translatedRealtimeNs = offsets.monotonicNsToRealtimeNs(currentMonotonicNs)

        val diff = abs(translatedRealtimeNs - currentRealtimeNs)
        assertThat(diff).isLessThan(10_000_000L)
    }
}
