/*
 * Copyright 2023 The Android Open Source Project
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

import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

internal data class NetworkRequestCompat(val wrapped: Any? = null) {
    @get:RequiresApi(21)
    val networkRequest: NetworkRequest?
        get() = wrapped as NetworkRequest?
}

@get:RequiresApi(28)
val NetworkRequest.transportTypesCompat: IntArray
    get() = if (Build.VERSION.SDK_INT >= 31) {
        NetworkRequest31.transportTypes(this)
    } else {
        intArrayOf(
            NetworkCapabilities.TRANSPORT_BLUETOOTH,
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.TRANSPORT_ETHERNET,
            NetworkCapabilities.TRANSPORT_LOWPAN,
            NetworkCapabilities.TRANSPORT_THREAD,
            NetworkCapabilities.TRANSPORT_USB,
            NetworkCapabilities.TRANSPORT_VPN,
            NetworkCapabilities.TRANSPORT_WIFI,
            NetworkCapabilities.TRANSPORT_WIFI_AWARE,
        ).filter { NetworkRequest28.hasTransport(this, it) }.toIntArray()
    }

@get:RequiresApi(28)
val NetworkRequest.capabilitiesCompat: IntArray
    get() = if (Build.VERSION.SDK_INT >= 31) {
        NetworkRequest31.capabilities(this)
    } else {
        intArrayOf(
            NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL,
            NetworkCapabilities.NET_CAPABILITY_CBS,
            NetworkCapabilities.NET_CAPABILITY_DUN,
            NetworkCapabilities.NET_CAPABILITY_EIMS,
            NetworkCapabilities.NET_CAPABILITY_ENTERPRISE,
            NetworkCapabilities.NET_CAPABILITY_FOREGROUND,
            NetworkCapabilities.NET_CAPABILITY_FOTA,
            NetworkCapabilities.NET_CAPABILITY_HEAD_UNIT,
            NetworkCapabilities.NET_CAPABILITY_IA,
            NetworkCapabilities.NET_CAPABILITY_IMS,
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
            NetworkCapabilities.NET_CAPABILITY_MCX,
            NetworkCapabilities.NET_CAPABILITY_MMS,
            NetworkCapabilities.NET_CAPABILITY_MMTEL,
            NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED,
            NetworkCapabilities.NET_CAPABILITY_NOT_METERED,
            NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED,
            NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING,
            NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED,
            NetworkCapabilities.NET_CAPABILITY_NOT_VPN,
            NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_BANDWIDTH,
            NetworkCapabilities.NET_CAPABILITY_PRIORITIZE_LATENCY,
            NetworkCapabilities.NET_CAPABILITY_RCS,
            NetworkCapabilities.NET_CAPABILITY_SUPL,
            NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED,
            NetworkCapabilities.NET_CAPABILITY_TRUSTED,
            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
            NetworkCapabilities.NET_CAPABILITY_WIFI_P2P,
            NetworkCapabilities.NET_CAPABILITY_XCAP,
        ).filter { NetworkRequest28.hasCapability(this, it) }.toIntArray()
    }

@RequiresApi(28)
object NetworkRequest28 {
    @DoNotInline
    internal fun hasCapability(request: NetworkRequest, capability: Int) =
        request.hasCapability(capability)

    @DoNotInline
    internal fun hasTransport(request: NetworkRequest, transport: Int) =
        request.hasTransport(transport)

    @JvmStatic
    @DoNotInline
    fun createNetworkRequest(capabilities: IntArray, transports: IntArray): NetworkRequest {
        val networkRequest = NetworkRequest.Builder()
        capabilities.forEach { networkRequest.addCapability(it) }
        transports.forEach { networkRequest.addTransportType(it) }
        return networkRequest.build()
    }

    @DoNotInline
    internal fun createNetworkRequestCompat(
        capabilities: IntArray,
        transports: IntArray
    ): NetworkRequestCompat {
        return NetworkRequestCompat(createNetworkRequest(capabilities, transports))
    }
}

@RequiresApi(31)
private object NetworkRequest31 {
    @DoNotInline
    fun capabilities(request: NetworkRequest) = request.capabilities

    @DoNotInline
    fun transportTypes(request: NetworkRequest) = request.transportTypes
}
