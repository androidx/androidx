/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.macro

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PowerRailTest {

    @Test
    fun hasMetrics_Pixel6() {
        assumeTrue(Build.VERSION.SDK_INT > 31 && Build.MODEL.lowercase() == "oriole")

        assertTrue(PowerRail.hasMetrics(throwOnMissingMetrics = true))
        assertTrue(PowerRail.hasMetrics(throwOnMissingMetrics = false))
    }

    @Test
    fun hasMetrics_false() {
        // The test is using a mocked output of `dumpsys powerstats`
        val output = """
                kernel_uid_readers_throttle_time=1000
                external_stats_collection_rate_limit_ms=600000
                battery_level_collection_delay_ms=300000
                procstate_change_collection_delay_ms=60000
                max_history_files=32
                max_history_buffer_kb=128
                battery_charged_delay_ms=900000

            On battery measured charge stats (microcoulombs)
                Not supported on this device.
        """.trimIndent()

        assertFailsWith<UnsupportedOperationException> {
            PowerRail.hasMetrics(output, throwOnMissingMetrics = true)
        }

        assertFalse(PowerRail.hasMetrics(output, throwOnMissingMetrics = false))
    }

    @Test
    fun hasMetrics() {
        // The test is using a mocked output of `dumpsys powerstats`
        val output = """
            ChannelId: 10, ChannelName: S9S_VDD_AOC, ChannelSubsystem: AOC
            PowerStatsService dumpsys: available Channels
            ChannelId: 0, ChannelName: S10M_VDD_TPU, ChannelSubsystem: TPU
            ChannelId: 1, ChannelName: VSYS_PWR_MODEM, ChannelSubsystem: Modem
            ChannelId: 2, ChannelName: VSYS_PWR_RFFE, ChannelSubsystem: Cellular
            ChannelId: 3, ChannelName: S2M_VDD_CPUCL2, ChannelSubsystem: CPU(BIG)
            ChannelId: 4, ChannelName: S3M_VDD_CPUCL1, ChannelSubsystem: CPU(MID)
        """.trimIndent()

        assertTrue(PowerRail.hasMetrics(output, throwOnMissingMetrics = false))
    }
}
