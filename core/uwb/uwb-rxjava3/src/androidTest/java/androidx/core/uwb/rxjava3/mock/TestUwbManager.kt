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

import android.content.Context
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbManager
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.RangingCapabilities
import com.google.android.gms.nearby.uwb.UwbComplexChannel

/** A default implementation of [UwbManager] used in testing. */
class TestUwbManager(val context: Context) : UwbManager {
    override suspend fun clientSessionScope(): UwbClientSessionScope {
        val complexChannel = UwbComplexChannel.Builder()
            .setPreambleIndex(0)
            .setChannel(0)
            .build()
        val localAddress = com.google.android.gms.nearby.uwb.UwbAddress(ByteArray(0))
        val rangingCapabilities =
            com.google.android.gms.nearby.uwb.RangingCapabilities(true, false, false)
        val uwbClient = TestUwbClient(complexChannel, localAddress, rangingCapabilities, true)
        return TestUwbClientSessionScope(
            uwbClient, RangingCapabilities(
                rangingCapabilities.supportsDistance(),
                rangingCapabilities.supportsAzimuthalAngle(),
                rangingCapabilities.supportsElevationAngle()
            ),
            UwbAddress(localAddress.address)
        )
    }
}