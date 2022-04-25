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
import androidx.core.uwb.UwbManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import kotlinx.coroutines.tasks.await
import androidx.core.uwb.helper.checkSystemFeature
import androidx.core.uwb.helper.handleApiException

internal class UwbManagerImpl(val context: Context) : UwbManager {
    override suspend fun <R> clientSessionScope(
        sessionHandler: suspend UwbClientSessionScope.() -> R
    ): R {
        // Check whether UWB hardware is available on the device.
        checkSystemFeature(context)
        val uwbClient = Nearby.getUwbControleeClient(context)
        val localAddress: com.google.android.gms.nearby.uwb.UwbAddress
        val rangingCapabilities: com.google.android.gms.nearby.uwb.RangingCapabilities
        try {
            localAddress = uwbClient.localAddress.await()
            rangingCapabilities = uwbClient.rangingCapabilities.await()
        } catch (e: ApiException) {
            handleApiException(e)
            throw RuntimeException("Unexpected error. This indicates that the library is not " +
                "up-to-date with the service backend.")
        }
        return UwbClientSessionScopeImpl(
            uwbClient,
            RangingCapabilities(
                rangingCapabilities.supportsDistance(),
                rangingCapabilities.supportsAzimuthalAngle(),
                rangingCapabilities.supportsElevationAngle()),
            UwbAddress(localAddress.address)
        ).sessionHandler()
    }
}