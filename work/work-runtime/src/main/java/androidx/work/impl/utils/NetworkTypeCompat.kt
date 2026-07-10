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

package androidx.work.impl.utils

import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_VPN
import android.net.NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkRequest
import android.net.NetworkRequest.*
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.NetworkType

/** Convert [NetworkType] to equivalent [NetworkRequest] logic. */
@RequiresApi(28)
public fun NetworkType.toNetworkRequest(): NetworkRequest? {
    if (this == NetworkType.NOT_REQUIRED) {
        return null
    }
    val networkRequestBuilder =
        Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .addCapability(NET_CAPABILITY_VALIDATED)
            .removeCapability(NET_CAPABILITY_NOT_VPN)
            .removeCapability(NET_CAPABILITY_NOT_RESTRICTED)
    return if (Build.VERSION.SDK_INT >= 30 && this == NetworkType.TEMPORARILY_UNMETERED) {
        networkRequestBuilder.addCapability(NET_CAPABILITY_TEMPORARILY_NOT_METERED).build()
    } else {
        when (this) {
            NetworkType.METERED -> networkRequestBuilder.addTransportType(TRANSPORT_CELLULAR)
            NetworkType.UNMETERED -> networkRequestBuilder.addCapability(NET_CAPABILITY_NOT_METERED)

            NetworkType.NOT_ROAMING ->
                networkRequestBuilder.addCapability(NET_CAPABILITY_NOT_ROAMING)

            else -> networkRequestBuilder
        }.build()
    }
}
