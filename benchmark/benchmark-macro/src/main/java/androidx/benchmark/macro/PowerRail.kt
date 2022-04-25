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

    private const val commandHal2 = "dumpsys android.hardware.power.stats.IPowerStats/default delta"
    private const val commandHal1 =
        "lshal debug android.hardware.power.stats@1.0::IPowerStats/default delta"

    private const val hal2Header = "============= PowerStats HAL 2.0 energy meter =============="
    private const val hal1Header =
        "============= PowerStats HAL 1.0 rail energy data =============="

    /**
     * Checks if rail metrics are generated on specified device.
     *
     * @Throws UnsupportedOperationException if `hasException == true` and no rail metrics are found.
     */
    fun hasMetrics(throwOnMissingMetrics: Boolean = false): Boolean {
        val resultHal2 = Shell.executeCommand(commandHal2)
        val resultHal1 = Shell.executeCommand(commandHal1)

        if ((resultHal2.contains(hal2Header)) || (resultHal1.contains(hal1Header))) {
            return true
        }
        if (throwOnMissingMetrics) {
            throw UnsupportedOperationException(
                """
                Rail metrics are not available on this device.
                To check a device for power/energy measurement support, it must output rail metrics
                for one of the following commands:

                adb shell $commandHal2
                adb shell $commandHal1

                To check at runtime for this, use PowerRail.hasMetrics()

                """.trimIndent()
            )
        }
        return false
    }
}