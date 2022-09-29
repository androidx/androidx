/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.uwb.impl

import android.content.Context
import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import kotlinx.coroutines.tasks.await
import androidx.core.uwb.helper.checkSystemFeature
import androidx.core.uwb.helper.handleApiException

internal class UwbManagerImpl(private val context: Context) : UwbManager {
    @Deprecated("Renamed to controleeSessionScope")
    override suspend fun clientSessionScope(): UwbClientSessionScope {
        return createClientSessionScope(false)
    }

    override suspend fun controleeSessionScope(): UwbControleeSessionScope {
        return createClientSessionScope(false) as UwbControleeSessionScope
    }

    override suspend fun controllerSessionScope(): UwbControllerSessionScope {
        return createClientSessionScope(true) as UwbControllerSessionScope
    }

    private suspend fun createClientSessionScope(isController: Boolean): UwbClientSessionScope {
        checkSystemFeature(context)
        val uwbClient = if (isController)
            Nearby.getUwbControllerClient(context) else Nearby.getUwbControleeClient(context)
        try {
            val nearbyLocalAddress = uwbClient.localAddress.await()
            val nearbyRangingCapabilities = uwbClient.rangingCapabilities.await()
            val localAddress = UwbAddress(nearbyLocalAddress.address)
            val rangingCapabilities = RangingCapabilities(
                nearbyRangingCapabilities.supportsDistance(),
                nearbyRangingCapabilities.supportsAzimuthalAngle(),
                nearbyRangingCapabilities.supportsElevationAngle())
            return if (isController) {
                val uwbComplexChannel = uwbClient.complexChannel.await()
                UwbControllerSessionScopeImpl(
                    uwbClient,
                    rangingCapabilities,
                    localAddress,
                    UwbComplexChannel(uwbComplexChannel.channel, uwbComplexChannel.preambleIndex)
                )
            } else {
                UwbControleeSessionScopeImpl(
                    uwbClient,
                    rangingCapabilities,
                    localAddress
                )
            }
        } catch (e: ApiException) {
            handleApiException(e)
            throw RuntimeException("Unexpected error. This indicates that the library is not " +
                "up-to-date with the service backend.")
        }
    }
}