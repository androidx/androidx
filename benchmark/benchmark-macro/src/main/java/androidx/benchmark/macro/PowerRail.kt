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

import androidx.annotation.RestrictTo
import androidx.benchmark.Shell

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object PowerRail {

    private const val DUMPSYS_POWERSTATS = "dumpsys powerstats"

    /**
     * Looking for something like this:
     *
     * ChannelId: 10, ChannelName: S9S_VDD_AOC, ChannelSubsystem: AOC
     * PowerStatsService dumpsys: available Channels
     * ChannelId: 0, ChannelName: S10M_VDD_TPU, ChannelSubsystem: TPU
     * ChannelId: 1, ChannelName: VSYS_PWR_MODEM, ChannelSubsystem: Modem
     * ChannelId: 2, ChannelName: VSYS_PWR_RFFE, ChannelSubsystem: Cellular
     * ChannelId: 3, ChannelName: S2M_VDD_CPUCL2, ChannelSubsystem: CPU(BIG)
     * ChannelId: 4, ChannelName: S3M_VDD_CPUCL1, ChannelSubsystem: CPU(MID)
     */
    private val CHANNEL_ID_REGEX = "ChannelId:(.*)".toRegex()

    /**
     * Checks if rail metrics are generated on specified device.
     *
     * @Throws UnsupportedOperationException if `hasException == true` and no rail metrics are found.
     */
    fun hasMetrics(throwOnMissingMetrics: Boolean = false): Boolean {
        val output = Shell.executeCommand(DUMPSYS_POWERSTATS)
        return hasMetrics(output, throwOnMissingMetrics)
    }

    internal fun hasMetrics(output: String, throwOnMissingMetrics: Boolean = false): Boolean {
        val line = output.splitToSequence("\r?\n".toRegex()).find {
            it.contains(CHANNEL_ID_REGEX)
        }
        if (!line.isNullOrBlank()) {
            return true
        }
        if (throwOnMissingMetrics) {
            throw UnsupportedOperationException(
                """
                Rail metrics are not available on this device.
                To check a device for power/energy measurement support, the following command's
                output must contain rows underneath the "available Channels" section:

                adb shell $DUMPSYS_POWERSTATS

                To check at runtime for this, use PowerRail.hasMetrics()

                """.trimIndent()
            )
        }
        return false
    }
}
