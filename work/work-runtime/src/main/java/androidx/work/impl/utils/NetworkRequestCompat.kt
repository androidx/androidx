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
import androidx.annotation.RequiresApi
import androidx.work.Logger

internal data class NetworkRequestCompat(val wrapped: Any? = null) {

    companion object {
        val TAG = Logger.tagWithPrefix("NetworkRequestCompat")
    }

    val networkRequest: NetworkRequest?
        get() = wrapped as NetworkRequest?
}

@get:RequiresApi(28)
public val NetworkRequest.transportTypesCompat: IntArray
    get() =
        if (Build.VERSION.SDK_INT >= 31) {
            NetworkRequest31.transportTypes(this)
        } else {
            intArrayOf(
                    NetworkCapabilities.TRANSPORT_BLUETOOTH,
                    NetworkCapabilities.TRANSPORT_CELLULAR,
                    NetworkCapabilities.TRANSPORT_ETHERNET,
                    NetworkCapabilities.TRANSPORT_LOWPAN,
                    NetworkCapabilities.TRANSPORT_SATELLITE,
                    NetworkCapabilities.TRANSPORT_THREAD,
                    NetworkCapabilities.TRANSPORT_USB,
                    NetworkCapabilities.TRANSPORT_VPN,
                    NetworkCapabilities.TRANSPORT_WIFI,
                    NetworkCapabilities.TRANSPORT_WIFI_AWARE,
                )
                .filter { NetworkRequest28.hasTransport(this, it) }
                .toIntArray()
        }

@get:RequiresApi(28)
public val NetworkRequest.capabilitiesCompat: IntArray
    get() =
        if (Build.VERSION.SDK_INT >= 31) {
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
                    NetworkCapabilities.NET_CAPABILITY_LOCAL_NETWORK,
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
                )
                .filter { NetworkRequest28.hasCapability(this, it) }
                .toIntArray()
        }

// List of default capabilities that are set when a network request is constructed.
// https://cs.android.com/android/platform/superproject/main/+/main:packages/modules/Connectivity/framework/src/android/net/NetworkCapabilities.java;?q=DEFAULT_CAPABILITIES
private val defaultCapabilities =
    intArrayOf(
        NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED,
        NetworkCapabilities.NET_CAPABILITY_NOT_VPN,
        NetworkCapabilities.NET_CAPABILITY_TRUSTED,
    )

@RequiresApi(28)
public object NetworkRequest28 {
    internal fun hasCapability(request: NetworkRequest, capability: Int) =
        request.hasCapability(capability)

    internal fun hasTransport(request: NetworkRequest, transport: Int) =
        request.hasTransport(transport)

    @JvmStatic
    public fun createNetworkRequest(capabilities: IntArray, transports: IntArray): NetworkRequest {
        val networkRequest = NetworkRequest.Builder()
        capabilities.forEach {
            try {
                networkRequest.addCapability(it)
            } catch (ex: IllegalArgumentException) {
                // b/351180465 - Ignoring the IAE that addCapability() can throw on SDK < 35 and
                // aligning with newer SDK behaviour. Capabilities are persisted in the database
                // and the framework can by default add new ones. Catching this exception mitigates
                // the case where decoding the capabilities from the database fails across OS
                // changes.
                Logger.get()
                    .warning(NetworkRequestCompat.TAG, "Ignoring adding capability '$it'", ex)
            }
        }
        // b/409716532 - There is a list of capabilities that are set by default when the network
        // request is constructed and must be explicitly removed if they were not persisted as
        // otherwise the request to exclude those capabilities will be lost between
        // encoding and decoding.
        defaultCapabilities.forEach {
            if (!capabilities.contains(it)) {
                try {
                    networkRequest.removeCapability(it)
                } catch (ex: IllegalArgumentException) {
                    // Ignoring the IAE that removeCapability() can throw in the case that default
                    // capabilities changes across OS versions.
                    Logger.get()
                        .warning(
                            NetworkRequestCompat.TAG,
                            "Ignoring removing default capability '$it'",
                            ex,
                        )
                }
            }
        }

        transports.forEach { networkRequest.addTransportType(it) }
        return networkRequest.build()
    }

    internal fun createNetworkRequestCompat(
        capabilities: IntArray,
        transports: IntArray,
    ): NetworkRequestCompat {
        return NetworkRequestCompat(createNetworkRequest(capabilities, transports))
    }
}

@RequiresApi(31)
private object NetworkRequest31 {
    fun capabilities(request: NetworkRequest) = request.capabilities

    fun transportTypes(request: NetworkRequest) = request.transportTypes
}

@RequiresApi(30)
internal object NetworkRequest30 {
    fun getNetworkSpecifier(request: NetworkRequest) = request.networkSpecifier
}
