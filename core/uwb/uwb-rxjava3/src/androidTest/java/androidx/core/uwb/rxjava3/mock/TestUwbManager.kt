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

package androidx.core.uwb.rxjava3.mock

import androidx.core.uwb.UwbManager
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.UwbControllerSessionScope
import com.google.android.gms.nearby.uwb.UwbComplexChannel

/** A default implementation of [UwbManager] used in testing. */
class TestUwbManager : UwbManager {
    companion object {
        @JvmField
        val DEVICE_ADDRESS = byteArrayOf(0xB0.toByte())
    }
    @Deprecated("Renamed to controleeSessionScope")
    override suspend fun clientSessionScope(): UwbClientSessionScope {
        return createClientSessionSCope(false)
    }

    override suspend fun controleeSessionScope(): UwbControleeSessionScope {
        return createClientSessionSCope(false) as UwbControleeSessionScope
    }

    override suspend fun controllerSessionScope(): UwbControllerSessionScope {
        return createClientSessionSCope(true) as UwbControllerSessionScope
    }

    private fun createClientSessionSCope(isController: Boolean): UwbClientSessionScope {
        val complexChannel = UwbComplexChannel.Builder()
            .setPreambleIndex(10)
            .setChannel(10)
            .build()
        val localAddress = com.google.android.gms.nearby.uwb.UwbAddress(DEVICE_ADDRESS)

        val rangingCapabilities =
            com.google.android.gms.nearby.uwb.RangingCapabilities(true, false, false, 200,
                listOf(9), listOf(1, 2, 3), 2F)
        val uwbClient = TestUwbClient(complexChannel, localAddress, rangingCapabilities, true)
        return if (isController) {
             TestUwbControllerSessionScope(
                uwbClient, RangingCapabilities(
                    rangingCapabilities.supportsDistance(),
                    rangingCapabilities.supportsAzimuthalAngle(),
                    rangingCapabilities.supportsElevationAngle(),
                    rangingCapabilities.getMinRangingInterval(),
                    rangingCapabilities.getSupportedChannels().toSet(),
                    rangingCapabilities.getSupportedConfigIds().toSet()
                ),
                UwbAddress(localAddress.address),
                androidx.core.uwb.UwbComplexChannel(
                    complexChannel.channel, complexChannel.preambleIndex
                )
            )
        } else {
            TestUwbControleeSessionScope(
                uwbClient, RangingCapabilities(
                    rangingCapabilities.supportsDistance(),
                    rangingCapabilities.supportsAzimuthalAngle(),
                    rangingCapabilities.supportsElevationAngle(),
                    rangingCapabilities.getMinRangingInterval(),
                    rangingCapabilities.getSupportedChannels().toSet(),
                    rangingCapabilities.getSupportedConfigIds().toSet()
                ),
                UwbAddress(localAddress.address)
            )
        }
    }
}