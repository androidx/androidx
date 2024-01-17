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

package androidx.core.uwb.common

import androidx.core.uwb.RangingParameters
import androidx.core.uwb.UwbDevice
import com.google.android.gms.internal.nearby.zzpe
import com.google.android.gms.nearby.uwb.RangingCapabilities
import com.google.android.gms.nearby.uwb.UwbAddress
import com.google.android.gms.nearby.uwb.UwbComplexChannel

internal class TestCommons {
    companion object {
        val COMPLEX_CHANNEL = UwbComplexChannel.Builder()
            .setPreambleIndex(10)
            .setChannel(10)
            .build()
        val LOCAL_ADDRESS = UwbAddress(byteArrayOf(0xB0.toByte()))
        val RANGING_CAPABILITIES = RangingCapabilities(true, false, false, false,
            200, zzpe.zzl(9), zzpe.zzl(1), zzpe.zzn(1, 2, 3), zzpe.zzl(2), zzpe.zzl(1), false)
        val NEIGHBOR_1 = byteArrayOf(0xA1.toByte())
        val NEIGHBOR_2 = byteArrayOf(0xA5.toByte())
        val UWB_DEVICE = UwbDevice.createForAddress(NEIGHBOR_1)
        val RANGING_PARAMETERS = RangingParameters(
            RangingParameters.CONFIG_UNICAST_DS_TWR,
            sessionId = 0,
            subSessionId = 0,
            sessionKeyInfo = byteArrayOf(
                /* Vendor ID */ 0x07, 0x08,
                /* Static STS IV */ 0x01, 0x02, 0x03, 0x04, 0x05, 0x06),
            subSessionKeyInfo = null,
            complexChannel = null,
            listOf(UWB_DEVICE),
            RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC,
            uwbRangeDataNtfConfig = null,
            RangingParameters.RANGING_SLOT_DURATION_2_MILLIS,
            isAoaDisabled = false
        )
    }
}
