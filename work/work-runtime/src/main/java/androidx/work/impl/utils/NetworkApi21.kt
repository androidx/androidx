/*
 * Copyright 2021 The Android Open Source Project
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
@file:RequiresApi(21)
@file:JvmName("NetworkApi21")

package androidx.work.impl.utils

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

@DoNotInline
internal fun ConnectivityManager.unregisterNetworkCallbackCompat(
    networkCallback: NetworkCallback
) = unregisterNetworkCallback(networkCallback)

@DoNotInline
internal fun ConnectivityManager.getNetworkCapabilitiesCompat(
    network: Network?
) = getNetworkCapabilities(network)

@DoNotInline
internal fun NetworkCapabilities.hasCapabilityCompat(capability: Int) = hasCapability(capability)
